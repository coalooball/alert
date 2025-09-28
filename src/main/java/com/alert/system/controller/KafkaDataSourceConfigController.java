package com.alert.system.controller;

import com.alert.system.dto.KafkaDataSourceConfigResponse;
import com.alert.system.entity.KafkaDataSourceConfig;
import com.alert.system.repository.KafkaDataSourceConfigRepository;
import com.alert.system.repository.AlertTypeRepository;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.SaslConfigs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/kafka-datasource-config")
@CrossOrigin
public class KafkaDataSourceConfigController {

    @Autowired
    private KafkaDataSourceConfigRepository repository;

    @Autowired
    private AlertTypeRepository alertTypeRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getAllConfigs() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<KafkaDataSourceConfig> configs = repository.findAll();
            List<KafkaDataSourceConfigResponse> configResponses = new ArrayList<>();

            for (KafkaDataSourceConfig config : configs) {
                configResponses.add(KafkaDataSourceConfigResponse.fromEntity(config));
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
    public ResponseEntity<Map<String, Object>> createConfig(@RequestBody KafkaDataSourceConfig config) {
        Map<String, Object> response = new HashMap<>();
        try {
            KafkaDataSourceConfig saved = repository.save(config);
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
    public ResponseEntity<Map<String, Object>> updateConfig(@PathVariable Long id, @RequestBody KafkaDataSourceConfig config) {
        Map<String, Object> response = new HashMap<>();
        return repository.findById(id)
            .map(existing -> {
                config.setId(id);
                config.setCreateTime(existing.getCreateTime());
                KafkaDataSourceConfig updated = repository.save(config);
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
        AdminClient adminClient = null;

        try {
            System.out.println("=== Kafka Test Connection Debug ===");
            System.out.println("Received config: " + config);

            String brokers = config.get("brokers");
            String topicName = config.get("topicName");
            String securityProtocol = config.get("securityProtocol");
            String saslMechanism = config.get("saslMechanism");
            String username = config.get("username");
            String password = config.get("password");

            System.out.println("brokers: " + brokers);
            System.out.println("topicName: " + topicName);
            System.out.println("securityProtocol: " + securityProtocol);

            if (brokers == null || brokers.isEmpty()) {
                response.put("success", false);
                response.put("message", "Kafka集群地址不能为空");
                System.out.println("Error: brokers is null or empty");
                return ResponseEntity.badRequest().body(response);
            }

            if (topicName == null || topicName.isEmpty()) {
                response.put("success", false);
                response.put("message", "Topic名称不能为空");
                System.out.println("Error: topicName is null or empty");
                return ResponseEntity.badRequest().body(response);
            }

            Properties props = new Properties();
            props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
            props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
            props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);

            if (securityProtocol != null && !"PLAINTEXT".equals(securityProtocol)) {
                props.put("security.protocol", securityProtocol);

                if (securityProtocol.contains("SASL")) {
                    if (saslMechanism != null && !saslMechanism.isEmpty()) {
                        props.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
                    }

                    if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                        String jaasConfig = String.format(
                            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                            username, password
                        );
                        props.put(SaslConfigs.SASL_JAAS_CONFIG, jaasConfig);
                    }
                }
            }

            adminClient = AdminClient.create(props);

            DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(Collections.singleton(topicName));
            KafkaFuture<Map<String, org.apache.kafka.clients.admin.TopicDescription>> future = describeTopicsResult.allTopicNames();
            future.get(5, TimeUnit.SECONDS);

            response.put("success", true);
            response.put("message", "Kafka连接测试成功");
            return ResponseEntity.ok(response);

        } catch (org.apache.kafka.common.errors.UnknownTopicOrPartitionException e) {
            response.put("success", false);
            response.put("message", "Topic不存在: " + config.get("topicName"));
            return ResponseEntity.badRequest().body(response);
        } catch (java.util.concurrent.TimeoutException e) {
            response.put("success", false);
            response.put("message", "连接超时，请检查Kafka集群地址是否正确");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "连接失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } finally {
            if (adminClient != null) {
                adminClient.close();
            }
        }
    }
}