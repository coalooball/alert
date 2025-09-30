package com.alert.system.service;

import com.alert.system.entity.Alert;
import com.alert.system.entity.AlertStorageMapping;
import com.alert.system.entity.DataStorageConfig;
import com.alert.system.repository.AlertStorageMappingRepository;
import com.alert.system.repository.DataStorageConfigRepository;
import com.clickhouse.jdbc.ClickHouseDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClickHouseStorageService {

    private final AlertStorageMappingRepository storageMappingRepository;
    private final DataStorageConfigRepository storageConfigRepository;
    private final ObjectMapper objectMapper;

    private final Map<Long, DataSource> dataSourceCache = new ConcurrentHashMap<>();
    private static final DateTimeFormatter CLICKHOUSE_DATETIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String storeAlert(Alert alert, Long alertTypeId) {
        try {
            AlertStorageMapping mapping = storageMappingRepository
                .findByAlertType_Id(alertTypeId.intValue())
                .orElseThrow(() -> new RuntimeException("No storage mapping found for alert type: " + alertTypeId));

            DataStorageConfig storageConfig = mapping.getStorageConfig();
            if (storageConfig == null || !"clickhouse".equalsIgnoreCase(storageConfig.getStorageType())) {
                log.warn("No ClickHouse storage configured for alert type: {}", alertTypeId);
                return null;
            }

            String tableName = mapping.getTableName();
            ensureTableExists(storageConfig, tableName, alert);

            String clickhouseId = insertAlert(storageConfig, tableName, alert);

            log.debug("Stored alert {} in ClickHouse table {} with ID {}",
                alert.getAlertUuid(), tableName, clickhouseId);

            return clickhouseId;

        } catch (Exception e) {
            log.error("Error storing alert to ClickHouse", e);
            return null;
        }
    }

    private void ensureTableExists(DataStorageConfig config, String tableName, Alert alert) {
        try (Connection conn = getConnection(config)) {
            DatabaseMetaData metadata = conn.getMetaData();
            ResultSet tables = metadata.getTables(null, null, tableName, null);

            if (!tables.next()) {
                createTable(conn, tableName);
                log.info("Created ClickHouse table: {}", tableName);
            }
        } catch (Exception e) {
            log.error("Error checking/creating table: {}", tableName, e);
        }
    }

    private void createTable(Connection conn, String tableName) throws SQLException {
        String createTableSql = String.format("""
            CREATE TABLE IF NOT EXISTS %s (
                id UUID DEFAULT generateUUIDv4(),
                alert_uuid String,
                alert_type_id Int32,
                alert_subtype String,
                alert_time DateTime,
                source_ip String,
                dest_ip String,
                source_port UInt16,
                dest_port UInt16,
                severity String,
                priority String,
                title String,
                description String,
                raw_data String,
                parsed_data String,
                kafka_topic String,
                kafka_partition Int32,
                kafka_offset Int64,
                processing_time DateTime,
                insert_time DateTime DEFAULT now(),
                INDEX idx_alert_uuid alert_uuid TYPE bloom_filter GRANULARITY 1,
                INDEX idx_alert_time alert_time TYPE minmax GRANULARITY 1,
                INDEX idx_source_ip source_ip TYPE bloom_filter GRANULARITY 1,
                INDEX idx_dest_ip dest_ip TYPE bloom_filter GRANULARITY 1
            ) ENGINE = MergeTree()
            PARTITION BY toYYYYMM(alert_time)
            ORDER BY (alert_time, alert_uuid)
            TTL alert_time + INTERVAL 90 DAY
            SETTINGS index_granularity = 8192
            """, tableName);

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
        }
    }

    private String insertAlert(DataStorageConfig config, String tableName, Alert alert) throws Exception {
        // 从parsedData JSON中提取字段值
        Map<String, Object> parsedData = new HashMap<>();
        if (alert.getParsedData() != null && !alert.getParsedData().isEmpty()) {
            parsedData = new ObjectMapper().readValue(alert.getParsedData(), Map.class);
        }

        String insertSql = String.format("""
            INSERT INTO %s (
                alert_uuid, alert_type_id, alert_subtype, alert_time,
                source_ip, dest_ip, source_port, dest_port,
                severity, priority, title, description,
                raw_data, parsed_data, kafka_topic, kafka_partition,
                kafka_offset, processing_time
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, tableName);

        try (Connection conn = getConnection(config);
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {

            pstmt.setString(1, alert.getAlertUuid());
            pstmt.setInt(2, alert.getAlertType().getId());
            pstmt.setString(3, alert.getAlertSubtype());
            pstmt.setTimestamp(4, Timestamp.valueOf(alert.getAlertTime()));

            // 从parsedData中提取字段，如果不存在则设置为null
            pstmt.setString(5, getStringFromMap(parsedData, "src_ip"));
            pstmt.setString(6, getStringFromMap(parsedData, "dst_ip"));
            pstmt.setObject(7, parsedData.get("src_port"));
            pstmt.setObject(8, parsedData.get("dst_port"));
            pstmt.setString(9, getStringFromMap(parsedData, "alarm_severity"));
            pstmt.setString(10, getStringFromMap(parsedData, "alarm_priority"));
            pstmt.setString(11, getStringFromMap(parsedData, "alarm_name"));
            pstmt.setString(12, getStringFromMap(parsedData, "alarm_description"));

            pstmt.setString(13, alert.getRawData());
            pstmt.setString(14, alert.getParsedData());
            pstmt.setString(15, alert.getKafkaTopic());
            pstmt.setObject(16, alert.getKafkaPartition());
            pstmt.setObject(17, alert.getKafkaOffset());
            pstmt.setTimestamp(18, alert.getProcessingTime() != null ?
                Timestamp.valueOf(alert.getProcessingTime()) : Timestamp.valueOf(LocalDateTime.now()));

            pstmt.executeUpdate();

            return alert.getAlertUuid();
        }
    }

    private String getStringFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    public List<Map<String, Object>> queryAlerts(String tableName, String whereClause,
                                                 int limit, int offset) {
        List<Map<String, Object>> results = new ArrayList<>();

        DataStorageConfig defaultConfig = storageConfigRepository
            .findByIsDefault(true)
            .stream()
            .filter(c -> "clickhouse".equalsIgnoreCase(c.getStorageType()))
            .findFirst()
            .orElse(null);

        if (defaultConfig == null) {
            log.error("No default ClickHouse configuration found");
            return results;
        }

        String query = String.format(
            "SELECT * FROM %s %s ORDER BY alert_time DESC LIMIT %d OFFSET %d",
            tableName,
            whereClause != null ? "WHERE " + whereClause : "",
            limit,
            offset
        );

        try (Connection conn = getConnection(defaultConfig);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            ResultSetMetaData metadata = rs.getMetaData();
            int columnCount = metadata.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metadata.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }

        } catch (Exception e) {
            log.error("Error querying alerts from ClickHouse", e);
        }

        return results;
    }

    public long countAlerts(String tableName, String whereClause) {
        DataStorageConfig defaultConfig = storageConfigRepository
            .findByIsDefault(true)
            .stream()
            .filter(c -> "clickhouse".equalsIgnoreCase(c.getStorageType()))
            .findFirst()
            .orElse(null);

        if (defaultConfig == null) {
            log.error("No default ClickHouse configuration found");
            return 0;
        }

        String query = String.format(
            "SELECT COUNT(*) FROM %s %s",
            tableName,
            whereClause != null ? "WHERE " + whereClause : ""
        );

        try (Connection conn = getConnection(defaultConfig);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getLong(1);
            }

        } catch (Exception e) {
            log.error("Error counting alerts in ClickHouse", e);
        }

        return 0;
    }

    public void deleteOldAlerts(String tableName, int retentionDays) {
        DataStorageConfig defaultConfig = storageConfigRepository
            .findByIsDefault(true)
            .stream()
            .filter(c -> "clickhouse".equalsIgnoreCase(c.getStorageType()))
            .findFirst()
            .orElse(null);

        if (defaultConfig == null) {
            log.error("No default ClickHouse configuration found");
            return;
        }

        String deleteSql = String.format(
            "ALTER TABLE %s DELETE WHERE alert_time < now() - INTERVAL %d DAY",
            tableName,
            retentionDays
        );

        try (Connection conn = getConnection(defaultConfig);
             Statement stmt = conn.createStatement()) {

            stmt.execute(deleteSql);
            log.info("Deleted old alerts from {} older than {} days", tableName, retentionDays);

        } catch (Exception e) {
            log.error("Error deleting old alerts from ClickHouse", e);
        }
    }

    private Connection getConnection(DataStorageConfig config) throws SQLException {
        DataSource dataSource = dataSourceCache.computeIfAbsent(config.getId(), id -> {
            try {
                String jdbcUrl = buildJdbcUrl(config);
                Properties properties = new Properties();

                if (config.getUsername() != null) {
                    properties.setProperty("user", config.getUsername());
                }
                if (config.getPassword() != null) {
                    properties.setProperty("password", config.getPassword());
                }

                if (config.getAdditionalConfig() != null) {
                    try {
                        Map<String, Object> additionalConfig = objectMapper.readValue(
                            config.getAdditionalConfig(), Map.class);
                        additionalConfig.forEach((key, value) ->
                            properties.setProperty(key, String.valueOf(value)));
                    } catch (Exception e) {
                        log.warn("Error parsing additional config", e);
                    }
                }

                return new ClickHouseDataSource(jdbcUrl, properties);
            } catch (Exception e) {
                log.error("Error creating ClickHouse data source", e);
                throw new RuntimeException(e);
            }
        });

        return dataSource.getConnection();
    }

    private String buildJdbcUrl(DataStorageConfig config) {
        return String.format("jdbc:clickhouse://%s:%d/%s",
            config.getHost(),
            config.getPort(),
            config.getDatabaseName());
    }

    public boolean testConnection(DataStorageConfig config) {
        try (Connection conn = getConnection(config)) {
            return conn.isValid(5);
        } catch (Exception e) {
            log.error("ClickHouse connection test failed", e);
            return false;
        }
    }
}