package com.alert.system.service;

import com.alert.system.entity.Alert;
import com.alert.system.entity.AlertStorageMapping;
import com.alert.system.entity.AlertTagMapping;
import com.alert.system.entity.Tag;
import com.alert.system.repository.AlertRepository;
import com.alert.system.repository.AlertStorageMappingRepository;
import com.alert.system.repository.AlertTagMappingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AlertMiningService {

    private static final Logger logger = LoggerFactory.getLogger(AlertMiningService.class);

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertStorageMappingRepository storageMappingRepository;

    @Autowired
    private AlertTagMappingRepository tagMappingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private ClickHouseStorageService clickHouseStorageService;

    public List<Map<String, Object>> getAllAlertsWithTags(int page, int size) {
        // 获取所有告警类型的数据
        List<AlertStorageMapping> allMappings = storageMappingRepository.findAll();
        List<Map<String, Object>> allAlerts = new ArrayList<>();

        if (clickHouseStorageService == null) {
            logger.error("ClickHouse storage service is not available");
            return allAlerts;
        }

        // 从每个表查询数据
        for (AlertStorageMapping mapping : allMappings) {
            try {
                List<Map<String, Object>> clickhouseData = clickHouseStorageService.queryAlerts(
                    mapping.getTableName(),
                    null, // 无过滤条件
                    size / allMappings.size(), // 平均分配每个表的查询数量
                    page * (size / allMappings.size())
                );

                for (Map<String, Object> row : clickhouseData) {
                    Map<String, Object> alertData = convertClickHouseRowToAlertMap(row, mapping.getAlertType().getId());
                    String alertUuid = (String) row.get("alert_uuid");
                    if (alertUuid != null) {
                        List<Map<String, Object>> tags = getTagsForAlertByUuid(alertUuid);
                        alertData.put("tags", tags);
                    } else {
                        alertData.put("tags", new ArrayList<>());
                    }
                    allAlerts.add(alertData);
                }
            } catch (Exception e) {
                logger.error("Error querying ClickHouse table {}: {}", mapping.getTableName(), e.getMessage());
            }
        }

        // 按时间排序
        allAlerts.sort((a, b) -> {
            LocalDateTime timeA = (LocalDateTime) a.get("alertTime");
            LocalDateTime timeB = (LocalDateTime) b.get("alertTime");
            if (timeA == null || timeB == null) return 0;
            return timeB.compareTo(timeA); // 降序
        });

        // 分页
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, allAlerts.size());
        if (fromIndex >= allAlerts.size()) {
            return new ArrayList<>();
        }

        return allAlerts.subList(fromIndex, toIndex);
    }

    public List<Map<String, Object>> getAlertsByTypeWithTags(Integer alertTypeId, int page, int size) {
        // 从 ClickHouse 读取数据
        Optional<AlertStorageMapping> mappingOpt = storageMappingRepository.findByAlertType_Id(alertTypeId);

        if (!mappingOpt.isPresent()) {
            logger.warn("No storage mapping found for alert type: {}", alertTypeId);
            return new ArrayList<>();
        }

        if (clickHouseStorageService == null) {
            logger.error("ClickHouse storage service is not available");
            return new ArrayList<>();
        }

        AlertStorageMapping mapping = mappingOpt.get();
        String tableName = mapping.getTableName();

        try {
            // 从 ClickHouse 查询数据
            List<Map<String, Object>> clickhouseData = clickHouseStorageService.queryAlerts(
                tableName,
                String.format("alert_type_id = %d", alertTypeId),
                size,
                page * size
            );

            // 转换 ClickHouse 数据格式
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> row : clickhouseData) {
                Map<String, Object> alertData = convertClickHouseRowToAlertMap(row, alertTypeId);
                // 尝试获取标签（如果有 alert_uuid 关联）
                String alertUuid = (String) row.get("alert_uuid");
                if (alertUuid != null) {
                    List<Map<String, Object>> tags = getTagsForAlertByUuid(alertUuid);
                    alertData.put("tags", tags);
                } else {
                    alertData.put("tags", new ArrayList<>());
                }
                result.add(alertData);
            }
            return result;

        } catch (Exception e) {
            logger.error("Error querying ClickHouse for alert type {}: {}", alertTypeId, e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getAlertsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        // 从所有 ClickHouse 表查询指定时间范围的数据
        List<AlertStorageMapping> allMappings = storageMappingRepository.findAll();
        List<Map<String, Object>> allAlerts = new ArrayList<>();

        if (clickHouseStorageService == null) {
            logger.error("ClickHouse storage service is not available");
            return allAlerts;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String whereClause = String.format("alert_time >= '%s' AND alert_time <= '%s'",
            startTime.format(formatter), endTime.format(formatter));

        for (AlertStorageMapping mapping : allMappings) {
            try {
                List<Map<String, Object>> clickhouseData = clickHouseStorageService.queryAlerts(
                    mapping.getTableName(),
                    whereClause,
                    1000, // 查询更多数据，后续再分页
                    0
                );

                for (Map<String, Object> row : clickhouseData) {
                    Map<String, Object> alertData = convertClickHouseRowToAlertMap(row, mapping.getAlertType().getId());
                    String alertUuid = (String) row.get("alert_uuid");
                    if (alertUuid != null) {
                        List<Map<String, Object>> tags = getTagsForAlertByUuid(alertUuid);
                        alertData.put("tags", tags);
                    } else {
                        alertData.put("tags", new ArrayList<>());
                    }
                    allAlerts.add(alertData);
                }
            } catch (Exception e) {
                logger.error("Error querying ClickHouse table {} for time range: {}",
                    mapping.getTableName(), e.getMessage());
            }
        }

        // 按时间排序
        allAlerts.sort((a, b) -> {
            LocalDateTime timeA = (LocalDateTime) a.get("alertTime");
            LocalDateTime timeB = (LocalDateTime) b.get("alertTime");
            if (timeA == null || timeB == null) return 0;
            return timeB.compareTo(timeA); // 降序
        });

        // 分页
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, allAlerts.size());
        if (fromIndex >= allAlerts.size()) {
            return new ArrayList<>();
        }

        return allAlerts.subList(fromIndex, toIndex);
    }

    public Map<String, Object> getAlertById(UUID alertId) {
        Optional<Alert> alertOpt = alertRepository.findById(alertId);
        if (alertOpt.isEmpty()) {
            return null;
        }

        Alert alert = alertOpt.get();
        Map<String, Object> alertData = convertAlertToMap(alert);
        List<Map<String, Object>> tags = getTagsForAlertAsMap(alert.getId());
        alertData.put("tags", tags);

        return alertData;
    }

    public List<AlertStorageMapping> getAllStorageMappings() {
        return storageMappingRepository.findAll();
    }

    public AlertStorageMapping getStorageMappingByAlertType(Integer alertTypeId) {
        return storageMappingRepository.findByAlertTypeId(alertTypeId).orElse(null);
    }

    private List<Map<String, Object>> getTagsForAlertAsMap(UUID alertId) {
        List<AlertTagMapping> mappings = tagMappingRepository.findByAlertId(alertId);
        List<Map<String, Object>> tags = new ArrayList<>();
        for (AlertTagMapping mapping : mappings) {
            Tag tag = mapping.getTag();
            if (tag != null) {
                Map<String, Object> tagMap = new LinkedHashMap<>();
                tagMap.put("id", tag.getId());
                tagMap.put("tagName", tag.getTagName());
                tagMap.put("description", tag.getDescription());
                tagMap.put("tagType", tag.getTagType());
                tagMap.put("color", tag.getColor());
                tagMap.put("isEnabled", tag.getIsEnabled());
                tagMap.put("confidenceScore", mapping.getConfidenceScore());
                tagMap.put("isAutoTagged", mapping.getIsAutoTagged());
                tags.add(tagMap);
            }
        }
        return tags;
    }

    private Map<String, Object> convertAlertToMap(Alert alert) {
        Map<String, Object> alertMap = new LinkedHashMap<>();

        alertMap.put("id", alert.getId());
        alertMap.put("alertUuid", alert.getAlertUuid());
        alertMap.put("alertType", alert.getAlertType() != null ? alert.getAlertType().getTypeName() : null);
        alertMap.put("alertSubtype", alert.getAlertSubtype());
        alertMap.put("alertTime", alert.getAlertTime());
        alertMap.put("sourceIp", alert.getSourceIp());
        alertMap.put("destIp", alert.getDestIp());
        alertMap.put("sourcePort", alert.getSourcePort());
        alertMap.put("destPort", alert.getDestPort());
        alertMap.put("severity", alert.getSeverity());
        alertMap.put("priority", alert.getPriority());
        alertMap.put("title", alert.getTitle());
        alertMap.put("description", alert.getDescription());
        alertMap.put("status", alert.getStatus());

        if (alert.getRawData() != null) {
            try {
                Map<String, Object> rawDataMap = objectMapper.readValue(alert.getRawData(), Map.class);
                alertMap.put("rawData", rawDataMap);
            } catch (Exception e) {
                alertMap.put("rawData", alert.getRawData());
            }
        }

        if (alert.getParsedData() != null) {
            try {
                Map<String, Object> parsedDataMap = objectMapper.readValue(alert.getParsedData(), Map.class);
                alertMap.put("parsedData", parsedDataMap);
            } catch (Exception e) {
                alertMap.put("parsedData", alert.getParsedData());
            }
        }

        alertMap.put("isFiltered", alert.getIsFiltered());
        alertMap.put("correlationKey", alert.getCorrelationKey());
        alertMap.put("clickhouseTable", alert.getClickhouseTable());
        alertMap.put("clickhouseId", alert.getClickhouseId());
        alertMap.put("kafkaTopic", alert.getKafkaTopic());
        alertMap.put("processingTime", alert.getProcessingTime());
        alertMap.put("storageTime", alert.getStorageTime());
        alertMap.put("createdAt", alert.getCreatedAt());
        alertMap.put("updatedAt", alert.getUpdatedAt());

        return alertMap;
    }

    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> stats = new HashMap<>();

        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);
        LocalDateTime last30Days = LocalDateTime.now().minusDays(30);

        stats.put("totalAlerts", alertRepository.count());
        stats.put("last24HoursCount", alertRepository.countAlertsAfter(last24Hours));
        stats.put("last7DaysCount", alertRepository.countAlertsAfter(last7Days));
        stats.put("last30DaysCount", alertRepository.countAlertsAfter(last30Days));
        stats.put("filteredAlertsLast24Hours", alertRepository.countFilteredAlertsAfter(last24Hours));
        stats.put("autoTaggedAlerts", tagMappingRepository.countAutoTaggedAlerts());

        return stats;
    }

    private Map<String, Object> convertClickHouseRowToAlertMap(Map<String, Object> row, Integer alertTypeId) {
        Map<String, Object> alertMap = new LinkedHashMap<>();

        // 映射 ClickHouse 字段到前端期望的格式
        alertMap.put("id", row.get("id"));
        alertMap.put("alertUuid", row.get("alert_uuid"));
        alertMap.put("alertType", getAlertTypeName(alertTypeId));
        alertMap.put("alertSubtype", row.get("alert_subtype"));

        // 转换时间格式
        Object alertTime = row.get("alert_time");
        if (alertTime instanceof Timestamp) {
            alertMap.put("alertTime", ((Timestamp) alertTime).toLocalDateTime());
        } else if (alertTime instanceof String) {
            try {
                alertMap.put("alertTime", LocalDateTime.parse((String) alertTime,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (Exception e) {
                alertMap.put("alertTime", alertTime);
            }
        } else {
            alertMap.put("alertTime", alertTime);
        }

        alertMap.put("sourceIp", row.get("source_ip"));
        alertMap.put("destIp", row.get("dest_ip"));
        alertMap.put("sourcePort", row.get("source_port"));
        alertMap.put("destPort", row.get("dest_port"));
        alertMap.put("severity", row.get("severity"));
        alertMap.put("priority", row.get("priority"));
        alertMap.put("title", row.get("title"));
        alertMap.put("description", row.get("description"));
        alertMap.put("status", row.getOrDefault("status", "NEW"));

        // 处理 JSON 字段
        String rawDataStr = (String) row.get("raw_data");
        if (rawDataStr != null && !rawDataStr.isEmpty()) {
            try {
                Map<String, Object> rawDataMap = objectMapper.readValue(rawDataStr, Map.class);
                alertMap.put("rawData", rawDataMap);
            } catch (Exception e) {
                alertMap.put("rawData", rawDataStr);
            }
        } else {
            alertMap.put("rawData", new HashMap<>());
        }

        String parsedDataStr = (String) row.get("parsed_data");
        if (parsedDataStr != null && !parsedDataStr.isEmpty()) {
            try {
                Map<String, Object> parsedDataMap = objectMapper.readValue(parsedDataStr, Map.class);
                alertMap.put("parsedData", parsedDataMap);
            } catch (Exception e) {
                alertMap.put("parsedData", parsedDataStr);
            }
        } else {
            alertMap.put("parsedData", new HashMap<>());
        }

        alertMap.put("isFiltered", row.getOrDefault("is_filtered", false));
        alertMap.put("correlationKey", row.get("correlation_key"));
        alertMap.put("clickhouseTable", row.get("table_name"));
        alertMap.put("clickhouseId", row.get("id"));
        alertMap.put("kafkaTopic", row.get("kafka_topic"));
        alertMap.put("processingTime", row.get("processing_time"));
        alertMap.put("storageTime", row.get("insert_time"));

        return alertMap;
    }

    private String getAlertTypeName(Integer alertTypeId) {
        // 根据 ID 返回类型名称
        switch (alertTypeId) {
            case 1:
                return "network_attack";
            case 2:
                return "malicious_sample";
            case 3:
                return "host_behavior";
            default:
                return "unknown";
        }
    }

    private List<Map<String, Object>> getTagsForAlertByUuid(String alertUuid) {
        // 根据 alertUuid 查询标签
        Optional<Alert> alertOpt = alertRepository.findByAlertUuid(alertUuid);
        if (alertOpt.isPresent()) {
            return getTagsForAlertAsMap(alertOpt.get().getId());
        }
        return new ArrayList<>();
    }
}