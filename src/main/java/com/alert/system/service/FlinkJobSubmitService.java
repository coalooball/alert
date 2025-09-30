package com.alert.system.service;

import com.alert.system.entity.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Flink作业提交服务
 * 负责通过REST API与Flink集群交互
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FlinkJobSubmitService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    /**
     * Flink REST API端点
     */
    public static class FlinkRestEndpoints {
        public static final String OVERVIEW = "/overview";
        public static final String JARS_UPLOAD = "/jars/upload";
        public static final String JARS_LIST = "/jars";
        public static final String JAR_RUN = "/jars/%s/run";
        public static final String JOBS_LIST = "/jobs";
        public static final String JOB_DETAIL = "/jobs/%s";
        public static final String JOB_CANCEL = "/jobs/%s/cancel";
        public static final String JOB_SAVEPOINT = "/jobs/%s/savepoints";
        public static final String JOB_METRICS = "/jobs/%s/metrics";
        public static final String TASKMANAGERS = "/taskmanagers";
        public static final String CONFIG = "/config";
    }

    /**
     * 上传JAR包到Flink集群
     */
    public String uploadJar(FlinkConfig config, String jarPath) throws Exception {
        String url = buildFlinkUrl(config, FlinkRestEndpoints.JARS_UPLOAD);

        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            throw new RuntimeException("JAR file not found: " + jarPath);
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("jarfile", new FileSystemResource(jarFile));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        addAuthHeaders(headers, config);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode result = objectMapper.readTree(response.getBody());
            String jarId = result.get("filename").asText();
            log.info("Successfully uploaded JAR: {}", jarId);
            return jarId;
        } else {
            throw new RuntimeException("Failed to upload JAR: " + response.getStatusCode());
        }
    }

    /**
     * 提交Flink作业
     */
    public String submitJob(FlinkConfig config, String jarId, Map<String, Object> jobConfig) throws Exception {
        String url = buildFlinkUrl(config, String.format(FlinkRestEndpoints.JAR_RUN, jarId));

        Map<String, Object> request = new HashMap<>();

        // 设置入口类
        String entryClass = (String) jobConfig.getOrDefault("entryClass",
            "com.alert.flink.AlertProcessingJob");
        request.put("entryClass", entryClass);

        // 设置并行度
        Integer parallelism = (Integer) jobConfig.getOrDefault("parallelism", 2);
        request.put("parallelism", parallelism);

        // 设置程序参数
        String programArgs = objectMapper.writeValueAsString(jobConfig);
        request.put("programArgs", programArgs);

        // 设置保存点路径（如果有）
        String savepointPath = (String) jobConfig.get("savepointPath");
        if (savepointPath != null) {
            request.put("savepointPath", savepointPath);
            request.put("allowNonRestoredState", true);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addAuthHeaders(headers, config);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            String.class
        );

        if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.ACCEPTED) {
            JsonNode result = objectMapper.readTree(response.getBody());
            String jobId = result.get("jobid").asText();
            log.info("Successfully submitted job: {}", jobId);
            return jobId;
        } else {
            throw new RuntimeException("Failed to submit job: " + response.getBody());
        }
    }

    /**
     * 获取作业状态
     */
    public Map<String, Object> getJobStatus(FlinkConfig config, String jobId) throws Exception {
        String url = buildFlinkUrl(config, String.format(FlinkRestEndpoints.JOB_DETAIL, jobId));

        HttpHeaders headers = new HttpHeaders();
        addAuthHeaders(headers, config);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return objectMapper.readValue(response.getBody(), Map.class);
        } else {
            throw new RuntimeException("Failed to get job status: " + response.getStatusCode());
        }
    }

    /**
     * 取消作业
     */
    public boolean cancelJob(FlinkConfig config, String jobId, boolean withSavepoint) throws Exception {
        String url = buildFlinkUrl(config, String.format(FlinkRestEndpoints.JOB_CANCEL, jobId));

        Map<String, Object> request = new HashMap<>();
        if (withSavepoint) {
            request.put("mode", "savepoint");
            request.put("targetDirectory", "/tmp/savepoints");
        } else {
            request.put("mode", "cancel");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addAuthHeaders(headers, config);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.PATCH,
            entity,
            String.class
        );

        return response.getStatusCode() == HttpStatus.OK ||
               response.getStatusCode() == HttpStatus.ACCEPTED;
    }

    /**
     * 列出所有作业
     */
    public List<Map<String, Object>> listJobs(FlinkConfig config) throws Exception {
        String url = buildFlinkUrl(config, FlinkRestEndpoints.JOBS_LIST);

        HttpHeaders headers = new HttpHeaders();
        addAuthHeaders(headers, config);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode result = objectMapper.readTree(response.getBody());
            List<Map<String, Object>> jobs = new ArrayList<>();

            JsonNode jobsNode = result.get("jobs");
            if (jobsNode != null && jobsNode.isArray()) {
                for (JsonNode jobNode : jobsNode) {
                    Map<String, Object> job = new HashMap<>();
                    job.put("id", jobNode.get("id").asText());
                    job.put("status", jobNode.get("status").asText());
                    jobs.add(job);
                }
            }

            return jobs;
        } else {
            throw new RuntimeException("Failed to list jobs: " + response.getStatusCode());
        }
    }

    /**
     * 获取集群概览
     */
    public Map<String, Object> getClusterOverview(FlinkConfig config) throws Exception {
        String url = buildFlinkUrl(config, FlinkRestEndpoints.OVERVIEW);

        HttpHeaders headers = new HttpHeaders();
        addAuthHeaders(headers, config);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return objectMapper.readValue(response.getBody(), Map.class);
        } else {
            throw new RuntimeException("Failed to get cluster overview: " + response.getStatusCode());
        }
    }

    /**
     * 获取TaskManager列表
     */
    public List<Map<String, Object>> getTaskManagers(FlinkConfig config) throws Exception {
        String url = buildFlinkUrl(config, FlinkRestEndpoints.TASKMANAGERS);

        HttpHeaders headers = new HttpHeaders();
        addAuthHeaders(headers, config);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode result = objectMapper.readTree(response.getBody());
            List<Map<String, Object>> taskManagers = new ArrayList<>();

            JsonNode tmNode = result.get("taskmanagers");
            if (tmNode != null && tmNode.isArray()) {
                for (JsonNode tm : tmNode) {
                    Map<String, Object> tmInfo = new HashMap<>();
                    tmInfo.put("id", tm.get("id").asText());
                    tmInfo.put("path", tm.get("path").asText());
                    tmInfo.put("dataPort", tm.get("dataPort").asInt());
                    tmInfo.put("slotsNumber", tm.get("slotsNumber").asInt());
                    tmInfo.put("freeSlots", tm.get("freeSlots").asInt());
                    taskManagers.add(tmInfo);
                }
            }

            return taskManagers;
        } else {
            throw new RuntimeException("Failed to get task managers: " + response.getStatusCode());
        }
    }

    /**
     * 获取作业指标
     */
    public Map<String, Object> getJobMetrics(FlinkConfig config, String jobId, List<String> metrics) throws Exception {
        String url = buildFlinkUrl(config, String.format(FlinkRestEndpoints.JOB_METRICS, jobId));

        if (metrics != null && !metrics.isEmpty()) {
            url += "?get=" + String.join(",", metrics);
        }

        HttpHeaders headers = new HttpHeaders();
        addAuthHeaders(headers, config);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode result = objectMapper.readTree(response.getBody());
            Map<String, Object> metricsMap = new HashMap<>();

            if (result.isArray()) {
                for (JsonNode metric : result) {
                    String name = metric.get("id").asText();
                    String value = metric.get("value").asText();
                    metricsMap.put(name, value);
                }
            }

            return metricsMap;
        } else {
            throw new RuntimeException("Failed to get job metrics: " + response.getStatusCode());
        }
    }

    /**
     * 提交告警过滤作业
     */
    public String submitAlertFilterJob(FlinkConfig config, Alert alert, List<AlertFilterRule> rules) throws Exception {
        Map<String, Object> jobConfig = new HashMap<>();
        jobConfig.put("operation", "FILTER");
        jobConfig.put("alert", convertAlertToMap(alert));
        jobConfig.put("rules", convertFilterRulesToMaps(rules));
        jobConfig.put("parallelism", 2);
        jobConfig.put("jobName", "alert-filter-" + System.currentTimeMillis());

        String jarId = ensureJarUploaded(config, "alert-processing.jar");
        return submitJob(config, jarId, jobConfig);
    }

    /**
     * 提交告警标签作业
     */
    public String submitAlertTaggingJob(FlinkConfig config, Alert alert, List<AlertTaggingRule> rules) throws Exception {
        Map<String, Object> jobConfig = new HashMap<>();
        jobConfig.put("operation", "TAGGING");
        jobConfig.put("alert", convertAlertToMap(alert));
        jobConfig.put("rules", convertTaggingRulesToMaps(rules));
        jobConfig.put("parallelism", 2);
        jobConfig.put("jobName", "alert-tagging-" + System.currentTimeMillis());

        String jarId = ensureJarUploaded(config, "alert-processing.jar");
        return submitJob(config, jarId, jobConfig);
    }

    /**
     * 提交告警关联作业
     */
    public String submitAlertCorrelationJob(FlinkConfig config, EventCorrelationRule rule) throws Exception {
        Map<String, Object> jobConfig = new HashMap<>();
        jobConfig.put("operation", "CORRELATION");
        jobConfig.put("rule", convertCorrelationRuleToMap(rule));
        jobConfig.put("parallelism", 4);
        jobConfig.put("jobName", "alert-correlation-" + rule.getRuleName());
        jobConfig.put("timeWindowSeconds", rule.getTimeWindowSeconds());

        String jarId = ensureJarUploaded(config, "alert-processing.jar");
        return submitJob(config, jarId, jobConfig);
    }

    /**
     * 确保JAR已上传
     */
    private String ensureJarUploaded(FlinkConfig config, String jarName) throws Exception {
        // 检查JAR是否已经上传
        String url = buildFlinkUrl(config, FlinkRestEndpoints.JARS_LIST);

        HttpHeaders headers = new HttpHeaders();
        addAuthHeaders(headers, config);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode result = objectMapper.readTree(response.getBody());
            JsonNode files = result.get("files");

            if (files != null && files.isArray()) {
                for (JsonNode file : files) {
                    String fileName = file.get("name").asText();
                    if (fileName.contains(jarName)) {
                        return file.get("id").asText();
                    }
                }
            }
        }

        // 如果JAR不存在，上传它
        String jarPath = "/opt/flink-jobs/" + jarName;
        return uploadJar(config, jarPath);
    }

    /**
     * 构建Flink URL
     */
    private String buildFlinkUrl(FlinkConfig config, String endpoint) {
        return String.format("http://%s:%d%s",
            config.getJobManagerUrl(),
            config.getPort(),
            endpoint);
    }

    /**
     * 添加认证头
     */
    private void addAuthHeaders(HttpHeaders headers, FlinkConfig config) {
        if (config.getUsername() != null && config.getPassword() != null) {
            headers.setBasicAuth(config.getUsername(), config.getPassword());
        }
    }

    /**
     * 转换Alert为Map
     */
    private Map<String, Object> convertAlertToMap(Alert alert) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", alert.getId());
        map.put("alertUuid", alert.getAlertUuid());
        map.put("alertType", alert.getAlertType().getId());
        map.put("alertSubtype", alert.getAlertSubtype());
        map.put("sourceIp", alert.getSourceIp());
        map.put("destIp", alert.getDestIp());
        map.put("severity", alert.getSeverity());
        map.put("priority", alert.getPriority());
        map.put("title", alert.getTitle());
        map.put("description", alert.getDescription());
        map.put("alertTime", alert.getAlertTime().toString());
        return map;
    }

    /**
     * 转换过滤规则为Map列表
     */
    private List<Map<String, Object>> convertFilterRulesToMaps(List<AlertFilterRule> rules) {
        List<Map<String, Object>> maps = new ArrayList<>();
        for (AlertFilterRule rule : rules) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", rule.getId());
            map.put("ruleName", rule.getRuleName());
            map.put("matchField", rule.getMatchField());
            map.put("matchType", rule.getMatchType());
            map.put("matchValue", rule.getMatchValue());
            map.put("priority", rule.getPriority());
            maps.add(map);
        }
        return maps;
    }

    /**
     * 转换标签规则为Map列表
     */
    private List<Map<String, Object>> convertTaggingRulesToMaps(List<AlertTaggingRule> rules) {
        List<Map<String, Object>> maps = new ArrayList<>();
        for (AlertTaggingRule rule : rules) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", rule.getId());
            map.put("ruleName", rule.getRuleName());
            map.put("matchField", rule.getMatchField());
            map.put("matchType", rule.getMatchType());
            map.put("matchValue", rule.getMatchValue());
            map.put("tags", rule.getTags());
            map.put("priority", rule.getPriority());
            maps.add(map);
        }
        return maps;
    }

    /**
     * 转换关联规则为Map
     */
    private Map<String, Object> convertCorrelationRuleToMap(EventCorrelationRule rule) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", rule.getId());
        map.put("ruleName", rule.getRuleName());
        map.put("ruleType", rule.getRuleType());
        map.put("correlationLevel", rule.getCorrelationLevel());
        map.put("timeWindowSeconds", rule.getTimeWindowSeconds());
        map.put("minAlertCount", rule.getMinAlertCount());
        map.put("maxAlertCount", rule.getMaxAlertCount());
        map.put("correlationFields", rule.getCorrelationFields());
        map.put("groupingFields", rule.getGroupingFields());
        map.put("conditionExpression", rule.getConditionExpression());
        return map;
    }
}