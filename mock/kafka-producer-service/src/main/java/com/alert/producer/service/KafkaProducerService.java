package com.alert.producer.service;

import com.alert.producer.dto.KafkaProducerRequest;
import com.alert.producer.dto.KafkaProducerResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kafka Producer 服务
 */
@Slf4j
@Service
public class KafkaProducerService {

    @Value("${app.data-directory:/app/data}")
    private String dataDirectory;

    @Value("${app.upload-directory:/app/uploads}")
    private String uploadDirectory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从文件发送消息到Kafka
     */
    public KafkaProducerResponse sendFromFile(KafkaProducerRequest request) {
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            log.info("开始发送Kafka消息 - Broker: {}, Topic: {}, File: {}", 
                    request.getBrokerAddress(), request.getTopic(), request.getJsonFilePath());

            // 读取JSON文件
            List<Map<String, Object>> messages = loadJsonFile(request.getJsonFilePath());
            
            if (messages == null || messages.isEmpty()) {
                return buildErrorResponse(startTime, "JSON文件为空或格式错误");
            }

            // 应用限制
            if (request.getLimit() != null && request.getLimit() > 0) {
                messages = messages.subList(0, Math.min(request.getLimit(), messages.size()));
            }

            // 发送消息
            return sendMessagesToKafka(request, messages, startTime);

        } catch (Exception e) {
            log.error("发送Kafka消息失败", e);
            return buildErrorResponse(startTime, "发送失败: " + e.getMessage());
        }
    }

    /**
     * 从上传的文件发送消息到Kafka
     */
    public KafkaProducerResponse sendFromUpload(MultipartFile file, KafkaProducerRequest request) {
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            log.info("开始发送上传文件到Kafka - Broker: {}, Topic: {}, File: {}", 
                    request.getBrokerAddress(), request.getTopic(), file.getOriginalFilename());

            // 读取上传的JSON文件
            List<Map<String, Object>> messages = objectMapper.readValue(
                    file.getInputStream(), 
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            
            if (messages == null || messages.isEmpty()) {
                return buildErrorResponse(startTime, "上传的JSON文件为空");
            }

            // 应用限制
            if (request.getLimit() != null && request.getLimit() > 0) {
                messages = messages.subList(0, Math.min(request.getLimit(), messages.size()));
            }

            // 发送消息
            return sendMessagesToKafka(request, messages, startTime);

        } catch (Exception e) {
            log.error("发送上传文件到Kafka失败", e);
            return buildErrorResponse(startTime, "发送失败: " + e.getMessage());
        }
    }

    /**
     * 发送消息到Kafka
     */
    private KafkaProducerResponse sendMessagesToKafka(
            KafkaProducerRequest request,
            List<Map<String, Object>> messages,
            LocalDateTime startTime) {

        KafkaProducer<String, String> producer = null;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        try {
            // 创建Kafka Producer
            producer = createKafkaProducer(request);

            int total = messages.size();
            log.info("准备发送 {} 条消息到 topic: {}", total, request.getTopic());

            // 批量发送消息
            for (int i = 0; i < messages.size(); i++) {
                Map<String, Object> message = messages.get(i);
                
                try {
                    // 提取消息key
                    String key = extractMessageKey(message, request.getMessageKeyField());
                    
                    // 转换为JSON字符串
                    String value = objectMapper.writeValueAsString(message);
                    
                    // 创建ProducerRecord
                    ProducerRecord<String, String> record = 
                            new ProducerRecord<>(request.getTopic(), key, value);
                    
                    // 异步发送
                    Future<RecordMetadata> future = producer.send(record, (metadata, exception) -> {
                        if (exception == null) {
                            successCount.incrementAndGet();
                            if (successCount.get() % 100 == 0) {
                                log.info("已成功发送 {} 条消息", successCount.get());
                            }
                        } else {
                            failedCount.incrementAndGet();
                            log.error("消息发送失败: {}", exception.getMessage());
                        }
                    });

                    // 添加延迟
                    if (request.getDelayMillis() != null && request.getDelayMillis() > 0) {
                        Thread.sleep(request.getDelayMillis());
                    }

                    // 定期打印进度
                    if ((i + 1) % 1000 == 0) {
                        log.info("进度: {}/{} 消息已提交", i + 1, total);
                    }

                } catch (Exception e) {
                    failedCount.incrementAndGet();
                    log.error("发送消息失败: {}", e.getMessage());
                }
            }

            // 等待所有消息发送完成
            producer.flush();
            
            LocalDateTime endTime = LocalDateTime.now();
            double durationSeconds = java.time.Duration.between(startTime, endTime).toMillis() / 1000.0;

            // 构建统计信息
            KafkaProducerResponse.Statistics statistics = KafkaProducerResponse.Statistics.builder()
                    .total(messages.size())
                    .success(successCount.get())
                    .failed(failedCount.get())
                    .successRate(successCount.get() * 100.0 / messages.size())
                    .throughput(messages.size() / durationSeconds)
                    .build();

            log.info("发送完成 - 总数: {}, 成功: {}, 失败: {}, 耗时: {}秒", 
                    messages.size(), successCount.get(), failedCount.get(), String.format("%.2f", durationSeconds));

            return KafkaProducerResponse.builder()
                    .success(true)
                    .message("消息发送完成")
                    .statistics(statistics)
                    .startTime(startTime)
                    .endTime(endTime)
                    .durationSeconds(durationSeconds)
                    .build();

        } catch (Exception e) {
            log.error("Kafka发送过程出错", e);
            return buildErrorResponse(startTime, "发送过程出错: " + e.getMessage());
        } finally {
            if (producer != null) {
                producer.close();
            }
        }
    }

    /**
     * 创建Kafka Producer
     */
    private KafkaProducer<String, String> createKafkaProducer(KafkaProducerRequest request) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, request.getBrokerAddress());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, request.getAcks());
        props.put(ProducerConfig.RETRIES_CONFIG, request.getRetries());
        
        if (request.getCompressionEnabled()) {
            props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
        }
        
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);

        return new KafkaProducer<>(props);
    }

    /**
     * 从消息中提取key
     */
    private String extractMessageKey(Map<String, Object> message, String keyField) {
        if (keyField != null && message.containsKey(keyField)) {
            Object keyValue = message.get(keyField);
            return keyValue != null ? keyValue.toString() : null;
        }
        return null;
    }

    /**
     * 加载JSON文件（从数据目录或上传目录）
     */
    private List<Map<String, Object>> loadJsonFile(String filePath) throws IOException {
        // 先尝试从上传目录读取
        Path uploadPath = Paths.get(uploadDirectory, filePath);
        if (Files.exists(uploadPath)) {
            log.info("从上传目录加载文件: {}", uploadPath);
            return objectMapper.readValue(uploadPath.toFile(), new TypeReference<List<Map<String, Object>>>() {});
        }
        
        // 再尝试从数据目录读取
        Path dataPath = Paths.get(dataDirectory, filePath);
        if (Files.exists(dataPath)) {
            log.info("从数据目录加载文件: {}", dataPath);
            return objectMapper.readValue(dataPath.toFile(), new TypeReference<List<Map<String, Object>>>() {});
        }
        
        throw new IOException("文件不存在: " + filePath + " (已搜索上传目录和数据目录)");
    }

    /**
     * 构建错误响应
     */
    private KafkaProducerResponse buildErrorResponse(LocalDateTime startTime, String errorMessage) {
        LocalDateTime endTime = LocalDateTime.now();
        double durationSeconds = java.time.Duration.between(startTime, endTime).toMillis() / 1000.0;
        
        return KafkaProducerResponse.builder()
                .success(false)
                .message("发送失败")
                .errorDetails(errorMessage)
                .startTime(startTime)
                .endTime(endTime)
                .durationSeconds(durationSeconds)
                .build();
    }

    /**
     * 测试Kafka连接
     */
    public Map<String, Object> testKafkaConnection(String brokerAddress) {
        Map<String, Object> result = new java.util.HashMap<>();
        long startTime = System.currentTimeMillis();
        
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddress);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        
        KafkaProducer<String, String> producer = null;
        
        try {
            log.info("尝试连接Kafka Broker: {}", brokerAddress);
            producer = new KafkaProducer<>(props);
            
            // 尝试获取metadata来测试连接
            producer.partitionsFor("test-connection-topic");
            
            long duration = System.currentTimeMillis() - startTime;
            
            result.put("success", true);
            result.put("message", "连接成功");
            result.put("brokerAddress", brokerAddress);
            result.put("responseTime", duration + "ms");
            result.put("timestamp", LocalDateTime.now().toString());
            
            log.info("Kafka连接成功 - Broker: {}, 响应时间: {}ms", brokerAddress, duration);
            
        } catch (org.apache.kafka.common.errors.TimeoutException e) {
            long duration = System.currentTimeMillis() - startTime;
            result.put("success", false);
            result.put("message", "连接超时，请检查Broker地址是否正确");
            result.put("error", "连接超时");
            result.put("responseTime", duration + "ms");
            log.error("Kafka连接超时 - Broker: {}", brokerAddress, e);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            result.put("success", false);
            result.put("message", "连接失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            result.put("responseTime", duration + "ms");
            log.error("Kafka连接失败 - Broker: {}", brokerAddress, e);
            
        } finally {
            if (producer != null) {
                try {
                    producer.close();
                } catch (Exception e) {
                    log.warn("关闭Kafka Producer时出错", e);
                }
            }
        }
        
        return result;
    }

    /**
     * 保存上传的文件（不发送到Kafka）
     */
    public String saveUploadedFile(MultipartFile file) throws IOException {
        log.info("开始保存上传文件: {}", file.getOriginalFilename());
        
        Path uploadPath = Paths.get(uploadDirectory);
        
        // 确保上传目录存在
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("创建上传目录: {}", uploadPath);
        }

        // 生成文件名（防止覆盖）
        String originalFilename = file.getOriginalFilename();
        String fileName = originalFilename;
        Path targetPath = uploadPath.resolve(fileName);
        
        // 如果文件已存在，添加时间戳
        if (Files.exists(targetPath)) {
            String baseName = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            fileName = baseName + "_" + System.currentTimeMillis() + extension;
            targetPath = uploadPath.resolve(fileName);
        }

        // 保存文件
        file.transferTo(targetPath.toFile());
        
        log.info("文件已保存: {} (大小: {} bytes)", targetPath, file.getSize());
        
        return fileName;
    }

    /**
     * 列出数据目录和上传目录中的所有文件
     */
    public List<String> listDataFiles() throws IOException {
        java.util.Set<String> fileSet = new java.util.HashSet<>();
        
        // 列出数据目录的文件
        Path dataPath = Paths.get(dataDirectory);
        if (Files.exists(dataPath) && Files.isDirectory(dataPath)) {
            Files.list(dataPath)
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(path -> path.getFileName().toString())
                    .forEach(fileSet::add);
        }
        
        // 列出上传目录的文件
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("创建上传目录: {}", uploadPath);
        }
        if (Files.isDirectory(uploadPath)) {
            Files.list(uploadPath)
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(path -> path.getFileName().toString())
                    .forEach(fileSet::add);
        }
        
        // 返回排序后的列表
        return fileSet.stream().sorted().toList();
    }
}

