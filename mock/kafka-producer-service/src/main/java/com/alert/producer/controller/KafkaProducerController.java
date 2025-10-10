package com.alert.producer.controller;

import com.alert.producer.dto.ApiResponse;
import com.alert.producer.dto.KafkaProducerRequest;
import com.alert.producer.dto.KafkaProducerResponse;
import com.alert.producer.service.KafkaProducerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kafka Producer REST API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class KafkaProducerController {

    private final KafkaProducerService kafkaProducerService;

    /**
     * 从服务器文件发送消息
     */
    @PostMapping("/send-from-file")
    public ApiResponse<KafkaProducerResponse> sendFromFile(
            @Valid @RequestBody KafkaProducerRequest request) {
        
        log.info("收到从文件发送请求 - Topic: {}, File: {}", 
                request.getTopic(), request.getJsonFilePath());
        
        try {
            KafkaProducerResponse response = kafkaProducerService.sendFromFile(request);
            
            if (response.getSuccess()) {
                return ApiResponse.success("消息发送成功", response);
            } else {
                return ApiResponse.error(500, response.getErrorDetails());
            }
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return ApiResponse.error("发送失败: " + e.getMessage());
        }
    }

    /**
     * 从上传文件发送消息
     */
    @PostMapping(value = "/send-from-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<KafkaProducerResponse> sendFromUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("brokerAddress") String brokerAddress,
            @RequestParam("topic") String topic,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "delayMillis", defaultValue = "100") Long delayMillis,
            @RequestParam(value = "messageKeyField", defaultValue = "alarm_id") String messageKeyField,
            @RequestParam(value = "compressionEnabled", defaultValue = "true") Boolean compressionEnabled,
            @RequestParam(value = "acks", defaultValue = "all") String acks,
            @RequestParam(value = "retries", defaultValue = "3") Integer retries) {
        
        log.info("收到文件上传发送请求 - Topic: {}, File: {}", topic, file.getOriginalFilename());
        
        try {
            // 构建请求对象
            KafkaProducerRequest request = new KafkaProducerRequest();
            request.setBrokerAddress(brokerAddress);
            request.setTopic(topic);
            request.setLimit(limit);
            request.setDelayMillis(delayMillis);
            request.setMessageKeyField(messageKeyField);
            request.setCompressionEnabled(compressionEnabled);
            request.setAcks(acks);
            request.setRetries(retries);
            
            KafkaProducerResponse response = kafkaProducerService.sendFromUpload(file, request);
            
            if (response.getSuccess()) {
                return ApiResponse.success("消息发送成功", response);
            } else {
                return ApiResponse.error(500, response.getErrorDetails());
            }
        } catch (Exception e) {
            log.error("上传文件发送失败", e);
            return ApiResponse.error("发送失败: " + e.getMessage());
        }
    }

    /**
     * 列出可用的数据文件
     */
    @GetMapping("/list-files")
    public ApiResponse<List<String>> listFiles() {
        try {
            List<String> files = kafkaProducerService.listDataFiles();
            return ApiResponse.success("获取文件列表成功", files);
        } catch (Exception e) {
            log.error("获取文件列表失败", e);
            return ApiResponse.error("获取文件列表失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "kafka-producer-service");
        health.put("version", "1.0.0");
        health.put("timestamp", System.currentTimeMillis());
        return ApiResponse.success(health);
    }

    /**
     * 纯文件上传（不发送到Kafka）
     */
    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file) {
        
        log.info("收到文件上传请求 - File: {}", file.getOriginalFilename());
        
        try {
            String savedFilePath = kafkaProducerService.saveUploadedFile(file);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("fileName", file.getOriginalFilename());
            result.put("savedPath", savedFilePath);
            result.put("fileSize", file.getSize());
            result.put("message", "文件上传成功");
            
            return ApiResponse.success("文件上传成功", result);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return ApiResponse.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 测试Kafka连接
     */
    @PostMapping("/test-connection")
    public ApiResponse<Map<String, Object>> testConnection(
            @RequestParam("brokerAddress") String brokerAddress) {
        
        log.info("测试Kafka连接 - Broker: {}", brokerAddress);
        
        try {
            Map<String, Object> result = kafkaProducerService.testKafkaConnection(brokerAddress);
            
            if ((Boolean) result.get("success")) {
                return ApiResponse.success("Kafka连接成功", result);
            } else {
                return ApiResponse.error(500, (String) result.get("message"));
            }
        } catch (Exception e) {
            log.error("测试Kafka连接失败", e);
            return ApiResponse.error("连接测试失败: " + e.getMessage());
        }
    }

    /**
     * 获取服务信息
     */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "Kafka Producer Service");
        info.put("version", "1.0.0");
        info.put("description", "独立的Kafka生产者服务，用于将JSON文件数据发送到Kafka");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("POST /api/kafka/send-from-file", "从服务器文件发送消息");
        endpoints.put("POST /api/kafka/send-from-upload", "从上传文件发送消息");
        endpoints.put("POST /api/kafka/upload-file", "纯文件上传（不发送）");
        endpoints.put("POST /api/kafka/test-connection", "测试Kafka连接");
        endpoints.put("GET /api/kafka/list-files", "列出可用的数据文件");
        endpoints.put("GET /api/kafka/health", "健康检查");
        endpoints.put("GET /api/kafka/info", "服务信息");
        
        info.put("endpoints", endpoints);
        
        return ApiResponse.success(info);
    }
}

