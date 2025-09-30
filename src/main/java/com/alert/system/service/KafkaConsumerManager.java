package com.alert.system.service;

import com.alert.system.entity.*;
import com.alert.system.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerManager {

    private final KafkaDataSourceConfigRepository kafkaConfigRepository;
    private final AlertProcessingService alertProcessingService;
    private final ObjectMapper objectMapper;

    private final Map<Long, KafkaConsumer<String, String>> consumers = new ConcurrentHashMap<>();
    private final Map<Long, Future<?>> consumerTasks = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private volatile boolean running = true;

    @PostConstruct
    public void initialize() {
        log.info("Initializing Kafka Consumer Manager...");
        startAllEnabledConsumers();
        scheduleConfigRefresh();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Kafka Consumer Manager...");
        running = false;
        stopAllConsumers();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void startAllEnabledConsumers() {
        List<KafkaDataSourceConfig> configs = kafkaConfigRepository.findByIsEnabled(true);
        log.info("Found {} enabled Kafka configurations", configs.size());

        for (KafkaDataSourceConfig config : configs) {
            startConsumer(config);
        }
    }

    public void startConsumer(KafkaDataSourceConfig config) {
        if (consumers.containsKey(config.getId())) {
            log.warn("Consumer for config {} already exists", config.getConfigName());
            return;
        }

        try {
            Properties props = createConsumerProperties(config);
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Collections.singletonList(config.getTopicName()));

            consumers.put(config.getId(), consumer);

            Future<?> task = executorService.submit(() -> consumeMessages(config, consumer));
            consumerTasks.put(config.getId(), task);

            log.info("Started Kafka consumer for config: {} on topic: {}",
                config.getConfigName(), config.getTopicName());

            updateConnectionStatus(config.getId(), "connected");
        } catch (Exception e) {
            log.error("Failed to start consumer for config: {}", config.getConfigName(), e);
            updateConnectionStatus(config.getId(), "error: " + e.getMessage());
        }
    }

    public void stopConsumer(Long configId) {
        KafkaConsumer<String, String> consumer = consumers.remove(configId);
        if (consumer != null) {
            try {
                consumer.wakeup();
                consumer.close(Duration.ofSeconds(10));
                log.info("Stopped Kafka consumer for config ID: {}", configId);
            } catch (Exception e) {
                log.error("Error stopping consumer for config ID: {}", configId, e);
            }
        }

        Future<?> task = consumerTasks.remove(configId);
        if (task != null) {
            task.cancel(true);
        }

        updateConnectionStatus(configId, "disconnected");
    }

    private void stopAllConsumers() {
        for (Long configId : new HashSet<>(consumers.keySet())) {
            stopConsumer(configId);
        }
    }

    private void consumeMessages(KafkaDataSourceConfig config, KafkaConsumer<String, String> consumer) {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            processMessage(config, record);
                        } catch (Exception e) {
                            log.error("Error processing message from topic: {}, partition: {}, offset: {}",
                                record.topic(), record.partition(), record.offset(), e);
                        }
                    }

                    if (!records.isEmpty()) {
                        consumer.commitSync();
                    }
                } catch (CommitFailedException e) {
                    log.error("Failed to commit offset for config: {}", config.getConfigName(), e);
                }
            }
        } catch (Exception e) {
            if (!e.getClass().getSimpleName().equals("WakeupException")) {
                log.error("Error in consumer loop for config: {}", config.getConfigName(), e);
                updateConnectionStatus(config.getId(), "error: " + e.getMessage());
            }
        } finally {
            try {
                consumer.close();
            } catch (Exception e) {
                log.error("Error closing consumer for config: {}", config.getConfigName(), e);
            }
            consumers.remove(config.getId());
            consumerTasks.remove(config.getId());
        }
    }

    private void processMessage(KafkaDataSourceConfig config, ConsumerRecord<String, String> record) {
        AlertData alertData = new AlertData();
        alertData.setKafkaConfig(config);
        alertData.setRawData(record.value());
        alertData.setKafkaTopic(record.topic());
        alertData.setKafkaPartition(record.partition());
        alertData.setKafkaOffset(record.offset());
        alertData.setReceivedTime(System.currentTimeMillis());

        alertProcessingService.processAlert(alertData);
    }

    private Properties createConsumerProperties(KafkaDataSourceConfig config) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBrokers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, config.getConsumerGroup());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, config.getAutoOffsetReset());
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, config.getSessionTimeout());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, config.getMaxPollRecords());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        if (config.getSecurityProtocol() != null && !"PLAINTEXT".equals(config.getSecurityProtocol())) {
            props.put("security.protocol", config.getSecurityProtocol());

            if ("SASL_PLAINTEXT".equals(config.getSecurityProtocol()) ||
                "SASL_SSL".equals(config.getSecurityProtocol())) {
                props.put("sasl.mechanism", config.getSaslMechanism());
                props.put("sasl.jaas.config", String.format(
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                    config.getUsername(), config.getPassword()));
            }
        }

        return props;
    }

    @Transactional
    private void updateConnectionStatus(Long configId, String status) {
        kafkaConfigRepository.updateConnectionStatus(configId, status);
    }

    private void scheduleConfigRefresh() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(this::refreshConsumers, 30, 30, TimeUnit.SECONDS);
    }

    private void refreshConsumers() {
        try {
            List<KafkaDataSourceConfig> currentConfigs = kafkaConfigRepository.findByIsEnabled(true);
            Set<Long> currentConfigIds = new HashSet<>();

            for (KafkaDataSourceConfig config : currentConfigs) {
                currentConfigIds.add(config.getId());
                if (!consumers.containsKey(config.getId())) {
                    startConsumer(config);
                }
            }

            for (Long configId : consumers.keySet()) {
                if (!currentConfigIds.contains(configId)) {
                    stopConsumer(configId);
                }
            }
        } catch (Exception e) {
            log.error("Error refreshing consumers", e);
        }
    }

    public Map<Long, String> getConsumerStatuses() {
        Map<Long, String> statuses = new HashMap<>();
        for (Long configId : consumers.keySet()) {
            Future<?> task = consumerTasks.get(configId);
            if (task != null && !task.isDone()) {
                statuses.put(configId, "running");
            } else {
                statuses.put(configId, "stopped");
            }
        }
        return statuses;
    }

    public void restartConsumer(Long configId) {
        stopConsumer(configId);
        KafkaDataSourceConfig config = kafkaConfigRepository.findById(configId).orElse(null);
        if (config != null && config.getIsEnabled()) {
            startConsumer(config);
        }
    }

    public static class AlertData {
        private KafkaDataSourceConfig kafkaConfig;
        private String rawData;
        private String kafkaTopic;
        private Integer kafkaPartition;
        private Long kafkaOffset;
        private Long receivedTime;

        public KafkaDataSourceConfig getKafkaConfig() { return kafkaConfig; }
        public void setKafkaConfig(KafkaDataSourceConfig kafkaConfig) { this.kafkaConfig = kafkaConfig; }
        public String getRawData() { return rawData; }
        public void setRawData(String rawData) { this.rawData = rawData; }
        public String getKafkaTopic() { return kafkaTopic; }
        public void setKafkaTopic(String kafkaTopic) { this.kafkaTopic = kafkaTopic; }
        public Integer getKafkaPartition() { return kafkaPartition; }
        public void setKafkaPartition(Integer kafkaPartition) { this.kafkaPartition = kafkaPartition; }
        public Long getKafkaOffset() { return kafkaOffset; }
        public void setKafkaOffset(Long kafkaOffset) { this.kafkaOffset = kafkaOffset; }
        public Long getReceivedTime() { return receivedTime; }
        public void setReceivedTime(Long receivedTime) { this.receivedTime = receivedTime; }
    }
}