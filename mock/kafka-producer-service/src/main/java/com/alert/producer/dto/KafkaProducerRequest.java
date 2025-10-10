package com.alert.producer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Kafka Producer 请求 DTO
 */
@Data
public class KafkaProducerRequest {
    
    /**
     * Kafka Broker 地址 (例如: localhost:9092)
     */
    @NotBlank(message = "Kafka broker地址不能为空")
    private String brokerAddress;
    
    /**
     * Kafka Topic 名称
     */
    @NotBlank(message = "Topic名称不能为空")
    private String topic;
    
    /**
     * JSON 文件路径（服务器端路径，相对于data目录）
     */
    private String jsonFilePath;
    
    /**
     * 限制发送消息数量 (可选，null表示全部发送)
     */
    @Min(value = 1, message = "限制数量必须大于0")
    private Integer limit;
    
    /**
     * 消息之间的延迟时间（毫秒）
     */
    @NotNull(message = "延迟时间不能为空")
    @Min(value = 0, message = "延迟时间不能为负数")
    private Long delayMillis = 100L;
    
    /**
     * 消息键字段名称（用于从JSON中提取作为Kafka消息key）
     */
    private String messageKeyField = "alarm_id";
    
    /**
     * 是否启用压缩 (gzip)
     */
    private Boolean compressionEnabled = true;
    
    /**
     * ACK模式 (all, 1, 0)
     */
    private String acks = "all";
    
    /**
     * 重试次数
     */
    @Min(value = 0, message = "重试次数不能为负数")
    private Integer retries = 3;

    /**
     * 批量大小
     */
    private Integer batchSize = 100;
}

