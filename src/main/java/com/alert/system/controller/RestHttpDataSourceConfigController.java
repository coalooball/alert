package com.alert.system.controller;

import com.alert.system.dto.RestHttpDataSourceConfigResponse;
import com.alert.system.entity.RestHttpDataSourceConfig;
import com.alert.system.repository.RestHttpDataSourceConfigRepository;
import com.alert.system.repository.AlertTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.Base64;

@RestController
@RequestMapping("/api/rest-http-datasource-config")
@CrossOrigin
public class RestHttpDataSourceConfigController {

    @Autowired
    private RestHttpDataSourceConfigRepository repository;

    @Autowired
    private AlertTypeRepository alertTypeRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getAllConfigs() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<RestHttpDataSourceConfig> configs = repository.findAll();
            List<RestHttpDataSourceConfigResponse> configResponses = new ArrayList<>();

            for (RestHttpDataSourceConfig config : configs) {
                configResponses.add(RestHttpDataSourceConfigResponse.fromEntity(config));
            }

            response.put("success", true);
            response.put("data", configResponses);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to load configurations: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getConfig(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        return repository.findById(id)
            .map(config -> {
                if (config.getAlertType() != null) {
                    config.getAlertType().getTypeName();
                }
                response.put("success", true);
                response.put("data", config);
                return ResponseEntity.ok(response);
            })
            .orElseGet(() -> {
                response.put("success", false);
                response.put("message", "Configuration not found");
                return ResponseEntity.notFound().build();
            });
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createConfig(@RequestBody RestHttpDataSourceConfig config) {
        Map<String, Object> response = new HashMap<>();
        try {
            RestHttpDataSourceConfig saved = repository.save(config);
            response.put("success", true);
            response.put("data", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create configuration: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateConfig(@PathVariable Long id, @RequestBody RestHttpDataSourceConfig config) {
        Map<String, Object> response = new HashMap<>();
        return repository.findById(id)
            .map(existing -> {
                config.setId(id);
                config.setCreateTime(existing.getCreateTime());
                RestHttpDataSourceConfig updated = repository.save(config);
                response.put("success", true);
                response.put("data", updated);
                return ResponseEntity.ok(response);
            })
            .orElseGet(() -> {
                response.put("success", false);
                response.put("message", "Configuration not found");
                return ResponseEntity.notFound().build();
            });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteConfig(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        return repository.findById(id)
            .map(config -> {
                repository.delete(config);
                response.put("success", true);
                response.put("message", "Configuration deleted successfully");
                return ResponseEntity.ok(response);
            })
            .orElseGet(() -> {
                response.put("success", false);
                response.put("message", "Configuration not found");
                return ResponseEntity.notFound().build();
            });
    }

    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, String> config) {
        Map<String, Object> response = new HashMap<>();
        HttpURLConnection connection = null;

        try {
            String endpointPath = config.get("endpointPath");
            String method = config.get("method");
            String authType = config.get("authType");

            if (endpointPath == null || endpointPath.isEmpty()) {
                response.put("success", false);
                response.put("message", "API端点不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            URL url = new URL(endpointPath);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method != null ? method : "GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if ("basic".equals(authType)) {
                String username = config.get("basicUsername");
                String password = config.get("basicPassword");
                if (username != null && password != null) {
                    String auth = username + ":" + password;
                    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                    connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
                }
            } else if ("bearer".equals(authType)) {
                String token = config.get("bearerToken");
                if (token != null && !token.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }
            } else if ("api_key".equals(authType)) {
                String keyName = config.get("apiKeyName");
                String keyValue = config.get("apiKeyValue");
                if (keyName != null && keyValue != null) {
                    connection.setRequestProperty(keyName, keyValue);
                }
            }

            connection.setRequestProperty("Content-Type", config.getOrDefault("contentType", "application/json"));

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 400) {
                response.put("success", true);
                response.put("message", "连接测试成功 (HTTP " + responseCode + ")");
            } else {
                response.put("success", false);
                response.put("message", "连接失败 (HTTP " + responseCode + ")");
            }
            return ResponseEntity.ok(response);

        } catch (java.net.MalformedURLException e) {
            response.put("success", false);
            response.put("message", "无效的URL地址: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (java.net.UnknownHostException e) {
            response.put("success", false);
            response.put("message", "无法解析主机名，请检查网络连接");
            return ResponseEntity.badRequest().body(response);
        } catch (java.net.SocketTimeoutException e) {
            response.put("success", false);
            response.put("message", "连接超时，请检查端点是否可达");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "连接失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}