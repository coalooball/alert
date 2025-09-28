package com.alert.system.service;

import com.alert.system.dto.AlertStorageMappingDTO;
import com.alert.system.entity.*;
import com.alert.system.enums.DatabaseType;
import com.alert.system.repository.AlertStorageMappingRepository;
import com.alert.system.repository.DataStorageConfigRepository;
import com.alert.system.repository.UserRepository;
import com.alert.system.repository.AlertTypeRepository;
import com.alert.system.repository.AlertFieldRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertStorageMappingService {

    @Autowired
    private AlertStorageMappingRepository repository;

    @Autowired
    private DataStorageConfigRepository storageConfigRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AlertTypeRepository alertTypeRepository;

    @Autowired
    private AlertFieldRepository alertFieldRepository;

    public List<AlertStorageMappingDTO> getAllMappings() {
        List<AlertStorageMapping> mappings = repository.findAll();
        return mappings.stream().map(this::convertToDTO).toList();
    }

    public List<AlertStorageMappingDTO> getActiveMappings() {
        List<AlertStorageMapping> mappings = repository.findAll();
        return mappings.stream().map(this::convertToDTO).toList();
    }

    public AlertStorageMappingDTO getMappingById(Long id) {
        AlertStorageMapping mapping = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mapping not found with id: " + id));
        return convertToDTO(mapping);
    }

    public AlertStorageMappingDTO getMappingByAlertType(Integer alertTypeId) {
        AlertStorageMapping mapping = repository.findByAlertTypeId(alertTypeId)
                .orElse(null);
        return mapping != null ? convertToDTO(mapping) : null;
    }

    @Transactional
    public AlertStorageMappingDTO createMapping(AlertStorageMappingDTO dto) {
        if (repository.findByAlertTypeId(dto.getAlertTypeId()).isPresent()) {
            throw new RuntimeException("Mapping for alert type already exists");
        }

        AlertStorageMapping mapping = convertToEntity(dto);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        mapping.setCreatedBy(currentUser);
        mapping.setUpdatedBy(currentUser);

        if (dto.getAlertTypeId() != null) {
            AlertType alertType = alertTypeRepository.findById(dto.getAlertTypeId())
                    .orElseThrow(() -> new RuntimeException("Alert type not found with id: " + dto.getAlertTypeId()));
            mapping.setAlertType(alertType);
        }

        if (dto.getStorageConfigId() != null) {
            DataStorageConfig storageConfig = storageConfigRepository.findById(dto.getStorageConfigId())
                    .orElseThrow(() -> new RuntimeException("Storage config not found with id: " + dto.getStorageConfigId()));
            mapping.setStorageConfig(storageConfig);
        }

        AlertStorageMapping saved = repository.save(mapping);
        return convertToDTO(saved);
    }

    @Transactional
    public AlertStorageMappingDTO updateMapping(Long id, AlertStorageMappingDTO dto) {
        AlertStorageMapping mapping = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mapping not found with id: " + id));

        if (dto.getAlertTypeId() != null && !mapping.getAlertType().getId().equals(dto.getAlertTypeId()) &&
            repository.existsByAlertTypeIdAndIdNot(dto.getAlertTypeId(), id)) {
            throw new RuntimeException("Mapping for alert type already exists");
        }

        updateEntityFromDTO(mapping, dto);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        mapping.setUpdatedBy(currentUser);

        if (dto.getAlertTypeId() != null) {
            AlertType alertType = alertTypeRepository.findById(dto.getAlertTypeId())
                    .orElseThrow(() -> new RuntimeException("Alert type not found with id: " + dto.getAlertTypeId()));
            mapping.setAlertType(alertType);
        }

        if (dto.getStorageConfigId() != null) {
            DataStorageConfig storageConfig = storageConfigRepository.findById(dto.getStorageConfigId())
                    .orElseThrow(() -> new RuntimeException("Storage config not found with id: " + dto.getStorageConfigId()));
            mapping.setStorageConfig(storageConfig);
        }

        AlertStorageMapping saved = repository.save(mapping);
        return convertToDTO(saved);
    }

    @Transactional
    public void deleteMapping(Long id) {
        repository.deleteById(id);
    }

    public List<AlertStorageMappingDTO> getMappingsByStorageConfig(Long storageConfigId) {
        List<AlertStorageMapping> mappings = repository.findByStorageConfigId(storageConfigId);
        return mappings.stream().map(this::convertToDTO).toList();
    }

    private AlertStorageMappingDTO convertToDTO(AlertStorageMapping entity) {
        AlertStorageMappingDTO dto = new AlertStorageMappingDTO();
        dto.setId(entity.getId());
        dto.setTableName(entity.getTableName());
        dto.setRetentionDays(entity.getRetentionDays());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getAlertType() != null) {
            dto.setAlertTypeId(entity.getAlertType().getId());
            dto.setAlertTypeName(entity.getAlertType().getTypeName());
            dto.setAlertTypeLabel(entity.getAlertType().getTypeLabel());
        }

        if (entity.getStorageConfig() != null) {
            dto.setStorageConfigId(entity.getStorageConfig().getId());
            dto.setStorageConfigName(entity.getStorageConfig().getName());
            dto.setStorageDatabase(entity.getStorageConfig().getDatabaseName());
            dto.setStorageHost(entity.getStorageConfig().getHost());
        }

        if (entity.getCreatedBy() != null) {
            dto.setCreatedBy(entity.getCreatedBy().getUsername());
        }
        if (entity.getUpdatedBy() != null) {
            dto.setUpdatedBy(entity.getUpdatedBy().getUsername());
        }

        return dto;
    }

    private AlertStorageMapping convertToEntity(AlertStorageMappingDTO dto) {
        AlertStorageMapping entity = new AlertStorageMapping();
        updateEntityFromDTO(entity, dto);
        return entity;
    }

    private void updateEntityFromDTO(AlertStorageMapping entity, AlertStorageMappingDTO dto) {
        entity.setTableName(dto.getTableName());
        entity.setRetentionDays(dto.getRetentionDays());
        entity.setDescription(dto.getDescription());
    }

    public String generateTableDDL(Long mappingId) {
        AlertStorageMapping mapping = repository.findById(mappingId)
                .orElseThrow(() -> new RuntimeException("Mapping not found with id: " + mappingId));

        if (mapping.getStorageConfig() == null) {
            throw new RuntimeException("No storage configuration found for this mapping");
        }

        AlertType alertType = mapping.getAlertType();
        List<AlertField> fields = alertFieldRepository.findByAlertTypeIdAndIsActiveTrue(alertType.getId());

        DatabaseType dbType = mapping.getStorageConfig().getDbType();
        String tableName = mapping.getTableName();

        StringBuilder ddl = new StringBuilder();

        if (dbType == DatabaseType.CLICKHOUSE) {
            ddl.append(generateClickHouseDDL(tableName, fields));
        } else if (dbType == DatabaseType.ELASTICSEARCH) {
            ddl.append(generateElasticsearchMapping(tableName, fields));
        } else if (dbType == DatabaseType.MYSQL) {
            ddl.append(generateMySQLDDL(tableName, fields));
        } else if (dbType == DatabaseType.POSTGRESQL) {
            ddl.append(generatePostgreSQLDDL(tableName, fields));
        } else {
            throw new RuntimeException("Unsupported database type: " + dbType);
        }

        return ddl.toString();
    }

    private String generateClickHouseDDL(String tableName, List<AlertField> fields) {
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (\n");

        // System fields
        ddl.append("    id UUID DEFAULT generateUUIDv4(),\n");
        ddl.append("    insert_time DateTime DEFAULT now(),\n");

        // Alert fields
        for (AlertField field : fields) {
            String fieldType = mapToClickHouseType(field.getFieldType());
            ddl.append("    ").append(field.getFieldName()).append(" ").append(fieldType).append(",\n");
        }

        // Remove last comma
        ddl.setLength(ddl.length() - 2);
        ddl.append("\n");

        ddl.append(") ENGINE = MergeTree()\n");
        ddl.append("ORDER BY (insert_time, id)\n");
        ddl.append("PARTITION BY toYYYYMM(insert_time)\n");
        ddl.append("SETTINGS index_granularity = 8192;");

        return ddl.toString();
    }

    private String generateElasticsearchMapping(String indexName, List<AlertField> fields) {
        StringBuilder mapping = new StringBuilder();
        mapping.append("PUT /").append(indexName).append("\n");
        mapping.append("{\n");
        mapping.append("  \"mappings\": {\n");
        mapping.append("    \"properties\": {\n");

        // System fields
        mapping.append("      \"id\": { \"type\": \"keyword\" },\n");
        mapping.append("      \"insert_time\": { \"type\": \"date\" },\n");

        // Alert fields
        for (int i = 0; i < fields.size(); i++) {
            AlertField field = fields.get(i);
            String fieldType = mapToElasticsearchType(field.getFieldType());
            mapping.append("      \"").append(field.getFieldName()).append("\": { \"type\": \"")
                   .append(fieldType).append("\" }");
            if (i < fields.size() - 1) {
                mapping.append(",");
            }
            mapping.append("\n");
        }

        mapping.append("    }\n");
        mapping.append("  }\n");
        mapping.append("}");

        return mapping.toString();
    }

    private String generateMySQLDDL(String tableName, List<AlertField> fields) {
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE IF NOT EXISTS `").append(tableName).append("` (\n");

        // System fields
        ddl.append("    `id` VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),\n");
        ddl.append("    `insert_time` DATETIME DEFAULT CURRENT_TIMESTAMP,\n");

        // Alert fields
        for (AlertField field : fields) {
            String fieldType = mapToMySQLType(field.getFieldType());
            ddl.append("    `").append(field.getFieldName()).append("` ").append(fieldType).append(",\n");
        }

        // Indexes
        ddl.append("    INDEX idx_insert_time (insert_time)\n");

        ddl.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

        return ddl.toString();
    }

    private String generatePostgreSQLDDL(String tableName, List<AlertField> fields) {
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (\n");

        // System fields
        ddl.append("    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),\n");
        ddl.append("    insert_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n");

        // Alert fields
        for (AlertField field : fields) {
            String fieldType = mapToPostgreSQLType(field.getFieldType());
            ddl.append("    ").append(field.getFieldName()).append(" ").append(fieldType).append(",\n");
        }

        // Remove last comma
        ddl.setLength(ddl.length() - 2);
        ddl.append("\n);\n\n");

        // Create indexes
        ddl.append("CREATE INDEX idx_").append(tableName).append("_insert_time ON ")
           .append(tableName).append(" (insert_time);");

        return ddl.toString();
    }

    private String mapToClickHouseType(String fieldType) {
        switch (fieldType) {
            case "string":
                return "String";
            case "number":
                return "Int64";
            case "string[]":
                return "Array(String)";
            case "number[]":
                return "Array(Int64)";
            default:
                return "String";
        }
    }

    private String mapToElasticsearchType(String fieldType) {
        switch (fieldType) {
            case "string":
            case "string[]":
                return "text";
            case "number":
            case "number[]":
                return "long";
            default:
                return "text";
        }
    }

    private String mapToMySQLType(String fieldType) {
        switch (fieldType) {
            case "string":
                return "VARCHAR(500)";
            case "number":
                return "BIGINT";
            case "string[]":
            case "number[]":
                return "JSON";
            default:
                return "VARCHAR(500)";
        }
    }

    private String mapToPostgreSQLType(String fieldType) {
        switch (fieldType) {
            case "string":
                return "VARCHAR(500)";
            case "number":
                return "BIGINT";
            case "string[]":
                return "TEXT[]";
            case "number[]":
                return "BIGINT[]";
            default:
                return "VARCHAR(500)";
        }
    }

    public boolean checkTableExists(Long mappingId) {
        AlertStorageMapping mapping = repository.findById(mappingId)
                .orElseThrow(() -> new RuntimeException("Mapping not found with id: " + mappingId));

        if (mapping.getStorageConfig() == null) {
            throw new RuntimeException("No storage configuration found for this mapping");
        }

        DataStorageConfig config = mapping.getStorageConfig();
        String tableName = mapping.getTableName();

        try {
            DatabaseType dbType = config.getDbType();

            if (dbType == DatabaseType.ELASTICSEARCH) {
                // For Elasticsearch, we would need to check if index exists via REST API
                return false; // Not implemented yet
            }

            DriverManagerDataSource dataSource = createDataSource(config);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            String checkQuery = generateTableExistsQuery(dbType, tableName, config.getDatabaseName());

            try {
                Integer count = jdbcTemplate.queryForObject(checkQuery, Integer.class);
                return count != null && count > 0;
            } catch (Exception e) {
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to check table existence: " + e.getMessage(), e);
        }
    }

    @Transactional
    public boolean createStorageTable(Long mappingId) {
        AlertStorageMapping mapping = repository.findById(mappingId)
                .orElseThrow(() -> new RuntimeException("Mapping not found with id: " + mappingId));

        if (mapping.getStorageConfig() == null) {
            throw new RuntimeException("No storage configuration found for this mapping");
        }

        // Check if table already exists
        if (checkTableExists(mappingId)) {
            return false; // Table already exists, no need to create
        }

        DataStorageConfig config = mapping.getStorageConfig();
        String ddl = generateTableDDL(mappingId);

        try {
            if (config.getDbType() == DatabaseType.ELASTICSEARCH) {
                throw new RuntimeException("Elasticsearch table creation requires REST API implementation");
            }

            DriverManagerDataSource dataSource = createDataSource(config);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.execute(ddl);

            return true; // Table created successfully
        } catch (Exception e) {
            throw new RuntimeException("Failed to create table: " + e.getMessage(), e);
        }
    }

    private DriverManagerDataSource createDataSource(DataStorageConfig config) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        DatabaseType dbType = config.getDbType();

        if (dbType == DatabaseType.CLICKHOUSE) {
            dataSource.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
            dataSource.setUrl("jdbc:clickhouse://" + config.getHost() + ":" + config.getPort() + "/" + config.getDatabaseName());
        } else if (dbType == DatabaseType.MYSQL) {
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource.setUrl("jdbc:mysql://" + config.getHost() + ":" + config.getPort() + "/" + config.getDatabaseName() + "?useSSL=false&serverTimezone=UTC");
        } else if (dbType == DatabaseType.POSTGRESQL) {
            dataSource.setDriverClassName("org.postgresql.Driver");
            dataSource.setUrl("jdbc:postgresql://" + config.getHost() + ":" + config.getPort() + "/" + config.getDatabaseName());
        } else {
            throw new RuntimeException("Unsupported database type: " + dbType);
        }

        dataSource.setUsername(config.getUsername());
        dataSource.setPassword(config.getPassword());
        return dataSource;
    }

    private String generateTableExistsQuery(DatabaseType dbType, String tableName, String databaseName) {
        switch (dbType) {
            case CLICKHOUSE:
                return "SELECT COUNT(*) FROM system.tables WHERE database = '" + databaseName + "' AND name = '" + tableName + "'";
            case MYSQL:
                return "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '" + databaseName + "' AND table_name = '" + tableName + "'";
            case POSTGRESQL:
                return "SELECT COUNT(*) FROM information_schema.tables WHERE table_catalog = '" + databaseName + "' AND table_name = '" + tableName + "'";
            default:
                throw new RuntimeException("Unsupported database type for table existence check: " + dbType);
        }
    }

}