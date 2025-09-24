package com.alert.system.service;

import com.alert.system.dto.FlinkConfigRequest;
import com.alert.system.dto.FlinkConfigResponse;
import com.alert.system.dto.FlinkTestResult;
import com.alert.system.entity.FlinkConfig;
import com.alert.system.repository.FlinkConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class FlinkConfigService {

    @Autowired
    private FlinkConfigRepository flinkConfigRepository;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    public List<FlinkConfigResponse> getAllConfigs() {
        return flinkConfigRepository.findAll().stream()
                .map(FlinkConfigResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public FlinkConfigResponse getConfigById(UUID id) {
        FlinkConfig config = flinkConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flink configuration not found"));
        return FlinkConfigResponse.fromEntity(config);
    }

    public FlinkConfigResponse createConfig(FlinkConfigRequest request) {
        if (flinkConfigRepository.existsByName(request.getName())) {
            throw new RuntimeException("Configuration with name '" + request.getName() + "' already exists");
        }

        FlinkConfig config = new FlinkConfig();
        config.setName(request.getName());
        config.setJobManagerUrl(normalizeUrl(request.getJobManagerUrl()));
        config.setPort(request.getPort());
        config.setDescription(request.getDescription());
        config.setIsActive(request.getIsActive());
        config.setIsDefault(request.getIsDefault());
        config.setUsername(request.getUsername());
        config.setPassword(request.getPassword());
        config.setConnectionTimeout(request.getConnectionTimeout());
        config.setReadTimeout(request.getReadTimeout());

        // If this is set as default, clear other defaults
        if (request.getIsDefault()) {
            flinkConfigRepository.clearOtherDefaults(UUID.randomUUID());
        }

        // If this is set as active, deactivate others
        if (request.getIsActive()) {
            flinkConfigRepository.deactivateOthers(UUID.randomUUID());
        }

        FlinkConfig saved = flinkConfigRepository.save(config);
        return FlinkConfigResponse.fromEntity(saved);
    }

    public FlinkConfigResponse updateConfig(UUID id, FlinkConfigRequest request) {
        FlinkConfig config = flinkConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flink configuration not found"));

        // Check if name is being changed and if new name already exists
        if (!config.getName().equals(request.getName()) &&
            flinkConfigRepository.existsByName(request.getName())) {
            throw new RuntimeException("Configuration with name '" + request.getName() + "' already exists");
        }

        config.setName(request.getName());
        config.setJobManagerUrl(normalizeUrl(request.getJobManagerUrl()));
        config.setPort(request.getPort());
        config.setDescription(request.getDescription());
        config.setIsActive(request.getIsActive());
        config.setIsDefault(request.getIsDefault());
        config.setUsername(request.getUsername());
        config.setPassword(request.getPassword());
        config.setConnectionTimeout(request.getConnectionTimeout());
        config.setReadTimeout(request.getReadTimeout());

        // If this is set as default, clear other defaults
        if (request.getIsDefault()) {
            flinkConfigRepository.clearOtherDefaults(id);
        }

        // If this is set as active, deactivate others
        if (request.getIsActive()) {
            flinkConfigRepository.deactivateOthers(id);
        }

        FlinkConfig saved = flinkConfigRepository.save(config);
        return FlinkConfigResponse.fromEntity(saved);
    }

    public void deleteConfig(UUID id) {
        if (!flinkConfigRepository.existsById(id)) {
            throw new RuntimeException("Flink configuration not found");
        }
        flinkConfigRepository.deleteById(id);
    }

    public FlinkTestResult testConnection(UUID id) {
        FlinkConfig config = flinkConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flink configuration not found"));

        FlinkTestResult result = testFlinkConnection(config);

        // Update the test result in the config
        config.setLastTestTime(LocalDateTime.now());
        config.setLastTestStatus(result.isSuccess() ? "SUCCESS" : "FAILED");
        config.setLastTestMessage(result.getMessage());
        flinkConfigRepository.save(config);

        return result;
    }

    public FlinkTestResult testConnectionByRequest(FlinkConfigRequest request) {
        FlinkConfig config = new FlinkConfig();
        config.setJobManagerUrl(normalizeUrl(request.getJobManagerUrl()));
        config.setPort(request.getPort());
        config.setUsername(request.getUsername());
        config.setPassword(request.getPassword());
        config.setConnectionTimeout(request.getConnectionTimeout());
        config.setReadTimeout(request.getReadTimeout());

        return testFlinkConnection(config);
    }

    private FlinkTestResult testFlinkConnection(FlinkConfig config) {
        long startTime = System.currentTimeMillis();
        FlinkTestResult.FlinkTestResultBuilder resultBuilder = FlinkTestResult.builder()
                .testTime(LocalDateTime.now());

        try {
            // Build the Flink REST API URL
            String baseUrl = buildFlinkUrl(config);

            // Create RestTemplate with timeout settings
            RestTemplate restTemplate = restTemplateBuilder
                    .setConnectTimeout(Duration.ofMillis(config.getConnectionTimeout()))
                    .setReadTimeout(Duration.ofMillis(config.getReadTimeout()))
                    .build();

            // Add authentication headers if needed
            HttpHeaders headers = new HttpHeaders();
            if (config.getUsername() != null && config.getPassword() != null) {
                String auth = config.getUsername() + ":" + config.getPassword();
                byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
                String authHeader = "Basic " + new String(encodedAuth);
                headers.set("Authorization", authHeader);
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Test connection by getting cluster overview
            String overviewUrl = baseUrl + "/overview";
            ResponseEntity<String> response = restTemplate.exchange(
                    overviewUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Parse the response
                JsonNode jsonNode = objectMapper.readTree(response.getBody());

                long responseTime = System.currentTimeMillis() - startTime;

                resultBuilder.success(true)
                        .message("Successfully connected to Flink cluster")
                        .responseTimeMs(responseTime)
                        .flinkVersion(jsonNode.path("flink-version").asText(null))
                        .taskManagers(jsonNode.path("taskmanagers").asInt(0))
                        .totalTaskSlots(jsonNode.path("slots-total").asInt(0))
                        .availableTaskSlots(jsonNode.path("slots-available").asInt(0))
                        .runningJobs(jsonNode.path("jobs-running").asInt(0));

                // Try to get additional cluster info
                try {
                    String configUrl = baseUrl + "/config";
                    ResponseEntity<String> configResponse = restTemplate.exchange(
                            configUrl,
                            HttpMethod.GET,
                            entity,
                            String.class
                    );

                    if (configResponse.getStatusCode().is2xxSuccessful() && configResponse.getBody() != null) {
                        JsonNode configNode = objectMapper.readTree(configResponse.getBody());
                        Map<String, Object> additionalInfo = new HashMap<>();

                        // Extract some key configuration
                        configNode.fields().forEachRemaining(entry -> {
                            String key = entry.getKey();
                            if (key.contains("cluster-id") || key.contains("rest.address") ||
                                key.contains("jobmanager.rpc.address")) {
                                additionalInfo.put(key, entry.getValue().asText());
                            }
                        });

                        resultBuilder.additionalInfo(additionalInfo);
                    }
                } catch (Exception e) {
                    // Ignore additional info errors
                }

                return resultBuilder.build();
            } else {
                return resultBuilder
                        .success(false)
                        .message("Failed to connect to Flink cluster: Invalid response")
                        .error("HTTP " + response.getStatusCode())
                        .build();
            }

        } catch (RestClientException e) {
            return resultBuilder
                    .success(false)
                    .message("Failed to connect to Flink cluster")
                    .error(e.getClass().getSimpleName())
                    .errorDetail(e.getMessage())
                    .build();
        } catch (Exception e) {
            return resultBuilder
                    .success(false)
                    .message("Unexpected error during connection test")
                    .error(e.getClass().getSimpleName())
                    .errorDetail(e.getMessage())
                    .build();
        }
    }

    private String normalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }
        url = url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        // Remove trailing slashes
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String buildFlinkUrl(FlinkConfig config) {
        String baseUrl = config.getJobManagerUrl();
        // Check if port is already specified in the URL (after the host)
        // Parse URL to check if port is already included
        if (baseUrl.matches(".*:\\d+$")) {
            // URL already has a port number at the end
            return baseUrl;
        } else {
            // Add port to the URL
            return baseUrl + ":" + config.getPort();
        }
    }
}