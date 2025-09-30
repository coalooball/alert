package com.alert.system.service;

import com.alert.system.entity.*;
import com.alert.system.repository.FlinkConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class FlinkComputeService {

    private final FlinkConfigRepository flinkConfigRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    private volatile FlinkConfig activeFlinkConfig = null;

    public boolean isFlinkAvailable() {
        try {
            if (activeFlinkConfig == null) {
                activeFlinkConfig = flinkConfigRepository.findByIsActiveTrue()
                    .stream()
                    .findFirst()
                    .orElse(null);
            }

            if (activeFlinkConfig == null) {
                return false;
            }

            return testFlinkConnection(activeFlinkConfig);
        } catch (Exception e) {
            log.debug("Flink is not available", e);
            return false;
        }
    }

    public boolean applyFilterRules(Alert alert, List<AlertFilterRule> rules) {
        if (!isFlinkAvailable()) {
            log.debug("Flink not available, falling back to local processing");
            return false;
        }

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("alert", convertAlertToMap(alert));
            request.put("rules", convertRulesToMaps(rules));
            request.put("operation", "FILTER");

            String response = submitFlinkJob(request, "alert-filter-job");

            Map<String, Object> result = objectMapper.readValue(response, Map.class);
            return (boolean) result.getOrDefault("filtered", false);

        } catch (Exception e) {
            log.error("Error applying filter rules via Flink", e);
            return false;
        }
    }

    public List<String> applyTaggingRules(Alert alert, List<AlertTaggingRule> rules) {
        if (!isFlinkAvailable()) {
            log.debug("Flink not available, falling back to local processing");
            return new ArrayList<>();
        }

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("alert", convertAlertToMap(alert));
            request.put("rules", convertTaggingRulesToMaps(rules));
            request.put("operation", "TAG");

            String response = submitFlinkJob(request, "alert-tagging-job");

            Map<String, Object> result = objectMapper.readValue(response, Map.class);
            return (List<String>) result.getOrDefault("tags", new ArrayList<>());

        } catch (Exception e) {
            log.error("Error applying tagging rules via Flink", e);
            return new ArrayList<>();
        }
    }

    public void correlateCrossTypeAlerts(Alert alert, EventCorrelationRule rule) {
        if (!isFlinkAvailable()) {
            log.debug("Flink not available for cross-type correlation");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> request = new HashMap<>();
                request.put("alert", convertAlertToMap(alert));
                request.put("rule", convertCorrelationRuleToMap(rule));
                request.put("operation", "CORRELATE_CROSS_TYPE");
                request.put("windowSeconds", rule.getTimeWindowSeconds());

                String jobName = String.format("correlation-%s-%d",
                    rule.getRuleName().replaceAll("[^a-zA-Z0-9]", "-"),
                    System.currentTimeMillis());

                submitFlinkJob(request, jobName);

                log.info("Submitted cross-type correlation job for alert: {} with rule: {}",
                    alert.getAlertUuid(), rule.getRuleName());

            } catch (Exception e) {
                log.error("Error submitting cross-type correlation to Flink", e);
            }
        });
    }

    public void submitComplexEventProcessingJob(String cepPattern, int timeWindowMinutes) {
        if (!isFlinkAvailable()) {
            log.warn("Flink not available for CEP job submission");
            return;
        }

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("operation", "CEP");
            request.put("pattern", cepPattern);
            request.put("timeWindowMinutes", timeWindowMinutes);
            request.put("outputTopic", "cep-output");

            String jobName = String.format("cep-job-%d", System.currentTimeMillis());
            submitFlinkJob(request, jobName);

            log.info("Submitted CEP job: {}", jobName);

        } catch (Exception e) {
            log.error("Error submitting CEP job to Flink", e);
        }
    }

    private String submitFlinkJob(Map<String, Object> jobConfig, String jobName) throws Exception {
        if (activeFlinkConfig == null) {
            throw new RuntimeException("No active Flink configuration");
        }

        String flinkUrl = buildFlinkUrl(activeFlinkConfig);
        String jarId = ensureJobJarUploaded(flinkUrl);

        Map<String, Object> submitRequest = new HashMap<>();
        submitRequest.put("entryClass", "com.alert.flink.AlertProcessingJob");
        submitRequest.put("parallelism", 2);
        submitRequest.put("programArgs", objectMapper.writeValueAsString(jobConfig));
        submitRequest.put("savepointPath", null);
        submitRequest.put("allowNonRestoredState", true);

        String submitUrl = String.format("%s/jars/%s/run", flinkUrl, jarId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (activeFlinkConfig.getUsername() != null && activeFlinkConfig.getPassword() != null) {
            headers.setBasicAuth(activeFlinkConfig.getUsername(), activeFlinkConfig.getPassword());
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(submitRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            submitUrl,
            HttpMethod.POST,
            entity,
            String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            log.debug("Successfully submitted Flink job: {}", jobName);
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to submit Flink job: " + response.getStatusCode());
        }
    }

    private String ensureJobJarUploaded(String flinkUrl) {
        // In a production environment, this would check if the jar is already uploaded
        // and upload it if necessary. For now, we'll assume it's pre-uploaded.
        return "alert-processing-job.jar";
    }

    private Map<String, Object> convertAlertToMap(Alert alert) {
        Map<String, Object> alertMap = new HashMap<>();
        alertMap.put("alertUuid", alert.getAlertUuid());
        alertMap.put("alertType", alert.getAlertType().getId());
        alertMap.put("alertSubtype", alert.getAlertSubtype());
        alertMap.put("sourceIp", alert.getSourceIp());
        alertMap.put("destIp", alert.getDestIp());
        alertMap.put("severity", alert.getSeverity());
        alertMap.put("priority", alert.getPriority());
        alertMap.put("title", alert.getTitle());
        alertMap.put("description", alert.getDescription());
        alertMap.put("parsedData", alert.getParsedData());
        alertMap.put("alertTime", alert.getAlertTime().toString());
        return alertMap;
    }

    private List<Map<String, Object>> convertRulesToMaps(List<AlertFilterRule> rules) {
        List<Map<String, Object>> ruleMaps = new ArrayList<>();
        for (AlertFilterRule rule : rules) {
            Map<String, Object> ruleMap = new HashMap<>();
            ruleMap.put("id", rule.getId().toString());
            ruleMap.put("ruleName", rule.getRuleName());
            ruleMap.put("matchField", rule.getMatchField());
            ruleMap.put("matchType", rule.getMatchType());
            ruleMap.put("matchValue", rule.getMatchValue());
            ruleMap.put("priority", rule.getPriority());
            ruleMaps.add(ruleMap);
        }
        return ruleMaps;
    }

    private List<Map<String, Object>> convertTaggingRulesToMaps(List<AlertTaggingRule> rules) {
        List<Map<String, Object>> ruleMaps = new ArrayList<>();
        for (AlertTaggingRule rule : rules) {
            Map<String, Object> ruleMap = new HashMap<>();
            ruleMap.put("id", rule.getId().toString());
            ruleMap.put("ruleName", rule.getRuleName());
            ruleMap.put("matchField", rule.getMatchField());
            ruleMap.put("matchType", rule.getMatchType());
            ruleMap.put("matchValue", rule.getMatchValue());
            ruleMap.put("tags", rule.getTags());
            ruleMap.put("priority", rule.getPriority());
            ruleMaps.add(ruleMap);
        }
        return ruleMaps;
    }

    private Map<String, Object> convertCorrelationRuleToMap(EventCorrelationRule rule) {
        Map<String, Object> ruleMap = new HashMap<>();
        ruleMap.put("id", rule.getId().toString());
        ruleMap.put("ruleName", rule.getRuleName());
        ruleMap.put("ruleType", rule.getRuleType());
        ruleMap.put("correlationLevel", rule.getCorrelationLevel());
        ruleMap.put("timeWindowSeconds", rule.getTimeWindowSeconds());
        ruleMap.put("minAlertCount", rule.getMinAlertCount());
        ruleMap.put("maxAlertCount", rule.getMaxAlertCount());
        ruleMap.put("correlationFields", rule.getCorrelationFields());
        ruleMap.put("groupingFields", rule.getGroupingFields());
        ruleMap.put("conditionExpression", rule.getConditionExpression());
        return ruleMap;
    }

    private boolean testFlinkConnection(FlinkConfig config) {
        try {
            String url = buildFlinkUrl(config) + "/overview";

            HttpHeaders headers = new HttpHeaders();
            if (config.getUsername() != null && config.getPassword() != null) {
                headers.setBasicAuth(config.getUsername(), config.getPassword());
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            boolean isConnected = response.getStatusCode() == HttpStatus.OK;

            config.setLastTestTime(LocalDateTime.now());
            config.setLastTestStatus(isConnected ? "connected" : "failed");
            config.setLastTestMessage(isConnected ? "Connection successful" : "Connection failed");
            flinkConfigRepository.save(config);

            return isConnected;

        } catch (Exception e) {
            config.setLastTestTime(LocalDateTime.now());
            config.setLastTestStatus("error");
            config.setLastTestMessage(e.getMessage());
            flinkConfigRepository.save(config);

            log.debug("Flink connection test failed: {}", e.getMessage());
            return false;
        }
    }

    private String buildFlinkUrl(FlinkConfig config) {
        return String.format("http://%s:%d", config.getJobManagerUrl(), config.getPort());
    }

    public Map<String, Object> getFlinkClusterStatus() {
        if (!isFlinkAvailable()) {
            Map<String, Object> status = new HashMap<>();
            status.put("available", false);
            return status;
        }

        try {
            String url = buildFlinkUrl(activeFlinkConfig) + "/overview";

            HttpHeaders headers = new HttpHeaders();
            if (activeFlinkConfig.getUsername() != null && activeFlinkConfig.getPassword() != null) {
                headers.setBasicAuth(activeFlinkConfig.getUsername(), activeFlinkConfig.getPassword());
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            Map<String, Object> status = objectMapper.readValue(response.getBody(), Map.class);
            status.put("available", true);
            return status;

        } catch (Exception e) {
            log.error("Error getting Flink cluster status", e);
            Map<String, Object> status = new HashMap<>();
            status.put("available", false);
            status.put("error", e.getMessage());
            return status;
        }
    }
}