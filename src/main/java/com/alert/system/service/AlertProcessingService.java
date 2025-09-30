package com.alert.system.service;

import com.alert.system.entity.*;
import com.alert.system.repository.*;
import com.alert.system.service.KafkaConsumerManager.AlertData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertProcessingService {

    private final AlertRepository alertRepository;
    private final AlertTypeRepository alertTypeRepository;
    private final AlertFieldRepository alertFieldRepository;
    private final AlertFilterRuleRepository filterRuleRepository;
    private final AlertTaggingRuleRepository taggingRuleRepository;
    private final AlertTagMappingRepository tagMappingRepository;
    private final TagRepository tagRepository;
    private final ObservableService observableService;
    private final AlertCorrelationService correlationService;
    private final ClickHouseStorageService clickHouseStorageService;
    private final FlinkComputeService flinkComputeService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter[] DATE_FORMATS = {
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
    };

    @Transactional
    public void processAlert(AlertData alertData) {
        try {
            Alert alert = parseAlert(alertData);

            if (alert == null) {
                log.error("Failed to parse alert data");
                return;
            }

            boolean isFiltered = applyFilterRules(alert);

            if (isFiltered) {
                log.info("Alert {} filtered by rules", alert.getAlertUuid());
                alert.setIsFiltered(true);
                alert.setStatus("FILTERED");
            } else {
                applyTaggingRules(alert);

                extractObservables(alert);

                storeToClickHouse(alert, alertData.getKafkaConfig());

                correlationService.correlateAlert(alert);
            }

            alertRepository.save(alert);

            log.info("Successfully processed alert: {}", alert.getAlertUuid());

        } catch (Exception e) {
            log.error("Error processing alert", e);
        }
    }

    private Alert parseAlert(AlertData alertData) {
        try {
            JsonNode jsonData = objectMapper.readTree(alertData.getRawData());
            Alert alert = new Alert();

            alert.setAlertUuid(UUID.randomUUID().toString());
            alert.setRawData(alertData.getRawData());

            KafkaDataSourceConfig kafkaConfig = alertData.getKafkaConfig();
            AlertType alertType = alertTypeRepository.findById(kafkaConfig.getAlertTypeId().intValue()).orElse(null);

            if (alertType == null) {
                log.error("Alert type not found for config: {}", kafkaConfig.getConfigName());
                return null;
            }

            alert.setAlertType(alertType);

            Map<String, Object> parsedData = new HashMap<>();
            List<AlertField> fields = alertFieldRepository.findByAlertTypeIdOrderByDisplayOrder(alertType.getId());

            for (AlertField field : fields) {
                String fieldPath = field.getFieldPath() != null ? field.getFieldPath() : field.getFieldName();
                JsonNode value = getJsonValue(jsonData, fieldPath);

                if (value != null && !value.isNull()) {
                    Object parsedValue = parseFieldValue(value, field.getDataType());
                    parsedData.put(field.getFieldName(), parsedValue);

                    // 只设置几个核心字段到Alert实体，用于PostgreSQL查询
                    // 其他字段都通过parsedData传递
                    if (field.getFieldName().equalsIgnoreCase("alarm_date")) {
                        if (parsedValue instanceof Long) {
                            alert.setAlertTime(LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli((Long) parsedValue),
                                java.time.ZoneId.systemDefault()
                            ));
                        }
                    } else if (field.getFieldName().equalsIgnoreCase("alarm_subtype")) {
                        alert.setAlertSubtype(String.valueOf(parsedValue));
                    }
                }
            }

            alert.setParsedData(objectMapper.writeValueAsString(parsedData));

            if (alert.getAlertTime() == null) {
                alert.setAlertTime(LocalDateTime.now());
            }

            alert.setKafkaTopic(alertData.getKafkaTopic());
            alert.setKafkaPartition(alertData.getKafkaPartition());
            alert.setKafkaOffset(alertData.getKafkaOffset());

            return alert;

        } catch (Exception e) {
            log.error("Error parsing alert data", e);
            return null;
        }
    }

    private JsonNode getJsonValue(JsonNode root, String path) {
        String[] parts = path.split("\\.");
        JsonNode current = root;

        for (String part : parts) {
            if (current == null || current.isNull()) {
                return null;
            }
            current = current.get(part);
        }

        return current;
    }

    private Object parseFieldValue(JsonNode value, String dataType) {
        try {
            switch (dataType.toLowerCase()) {
                case "string":
                    return value.asText();
                case "integer":
                    return value.asInt();
                case "long":
                    return value.asLong();
                case "double":
                    return value.asDouble();
                case "boolean":
                    return value.asBoolean();
                case "datetime":
                    return parseDateTime(value.asText());
                case "array":
                    List<String> list = new ArrayList<>();
                    if (value.isArray()) {
                        value.forEach(node -> list.add(node.asText()));
                    }
                    return list;
                case "json":
                    return value.toString();
                default:
                    return value.asText();
            }
        } catch (Exception e) {
            log.warn("Error parsing field value of type {}: {}", dataType, e.getMessage());
            return value.asText();
        }
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDateTime.parse(dateStr, formatter);
            } catch (Exception ignored) {
            }
        }

        log.warn("Unable to parse date: {}", dateStr);
        return null;
    }

    // setAlertField方法已删除，所有字段都通过parsedData传递

    private boolean applyFilterRules(Alert alert) {
        try {
            List<AlertFilterRule> rules = filterRuleRepository.findByAlertTypeAndIsEnabledOrderByPriority(
                alert.getAlertType().getId(), true);

            if (flinkComputeService.isFlinkAvailable()) {
                return flinkComputeService.applyFilterRules(alert, rules);
            }

            for (AlertFilterRule rule : rules) {
                if (matchesRule(alert, rule)) {
                    alert.setFilterRuleId(rule.getId());
                    alert.setFilterReason(rule.getRuleName());
                    log.info("Alert {} matched filter rule: {}", alert.getAlertUuid(), rule.getRuleName());
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            log.error("Error applying filter rules", e);
            return false;
        }
    }

    private void applyTaggingRules(Alert alert) {
        try {
            List<AlertTaggingRule> rules = taggingRuleRepository.findByAlertTypeAndIsEnabledOrderByPriority(
                alert.getAlertType().getId(), true);

            if (flinkComputeService.isFlinkAvailable()) {
                List<String> tags = flinkComputeService.applyTaggingRules(alert, rules);
                saveAlertTags(alert, tags, null);
                return;
            }

            for (AlertTaggingRule rule : rules) {
                if (matchesRule(alert, rule)) {
                    saveAlertTags(alert, rule.getTags(), rule);
                    log.debug("Alert {} matched tagging rule: {}", alert.getAlertUuid(), rule.getRuleName());
                }
            }

        } catch (Exception e) {
            log.error("Error applying tagging rules", e);
        }
    }

    private boolean matchesRule(Alert alert, Object rule) {
        try {
            String matchField;
            String matchType;
            String matchValue;

            if (rule instanceof AlertFilterRule) {
                AlertFilterRule filterRule = (AlertFilterRule) rule;
                matchField = filterRule.getMatchField();
                matchType = filterRule.getMatchType();
                matchValue = filterRule.getMatchValue();
            } else if (rule instanceof AlertTaggingRule) {
                AlertTaggingRule taggingRule = (AlertTaggingRule) rule;
                matchField = taggingRule.getMatchField();
                matchType = taggingRule.getMatchType();
                matchValue = taggingRule.getMatchValue();
            } else {
                return false;
            }

            String fieldValue = getAlertFieldValue(alert, matchField);
            if (fieldValue == null) {
                return false;
            }

            switch (matchType.toLowerCase()) {
                case "exact":
                    return fieldValue.equalsIgnoreCase(matchValue);
                case "contains":
                    return fieldValue.toLowerCase().contains(matchValue.toLowerCase());
                case "regex":
                    Pattern pattern = Pattern.compile(matchValue, Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(fieldValue);
                    return matcher.find();
                default:
                    return false;
            }

        } catch (Exception e) {
            log.error("Error matching rule", e);
            return false;
        }
    }

    private String getAlertFieldValue(Alert alert, String fieldName) {
        try {
            if (alert.getParsedData() != null) {
                JsonNode parsed = objectMapper.readTree(alert.getParsedData());
                JsonNode value = parsed.get(fieldName);
                if (value != null && !value.isNull()) {
                    return value.asText();
                }
            }

            switch (fieldName.toLowerCase()) {
                case "source_ip": return alert.getSourceIp();
                case "dest_ip": return alert.getDestIp();
                case "severity": return alert.getSeverity();
                case "priority": return alert.getPriority();
                case "title": return alert.getTitle();
                case "description": return alert.getDescription();
                case "alert_subtype": return alert.getAlertSubtype();
                default: return null;
            }
        } catch (Exception e) {
            log.error("Error getting field value: {}", fieldName, e);
            return null;
        }
    }

    private void saveAlertTags(Alert alert, List<String> tagNames, AlertTaggingRule rule) {
        for (String tagName : tagNames) {
            try {
                Tag tag = tagRepository.findByName(tagName).orElseGet(() -> {
                    Tag newTag = new Tag();
                    newTag.setName(tagName);
                    newTag.setCategory("auto");
                    newTag.setCreatedAt(LocalDateTime.now());
                    return tagRepository.save(newTag);
                });

                AlertTagMapping mapping = new AlertTagMapping();
                mapping.setAlert(alert);
                mapping.setTag(tag);
                mapping.setTaggingRule(rule);
                mapping.setIsAutoTagged(true);
                mapping.setCreatedAt(LocalDateTime.now());

                tagMappingRepository.save(mapping);

            } catch (Exception e) {
                log.error("Error saving tag: {}", tagName, e);
            }
        }
    }

    private void extractObservables(Alert alert) {
        CompletableFuture.runAsync(() -> {
            try {
                observableService.extractAndSaveObservables(alert);
            } catch (Exception e) {
                log.error("Error extracting observables for alert: {}", alert.getAlertUuid(), e);
            }
        });
    }

    private void storeToClickHouse(Alert alert, KafkaDataSourceConfig config) {
        CompletableFuture.runAsync(() -> {
            try {
                String clickhouseId = clickHouseStorageService.storeAlert(alert, config.getAlertTypeId());
                alert.setClickhouseId(clickhouseId);
                alert.setStorageTime(LocalDateTime.now());
                alertRepository.save(alert);
            } catch (Exception e) {
                log.error("Error storing alert to ClickHouse: {}", alert.getAlertUuid(), e);
            }
        });
    }
}