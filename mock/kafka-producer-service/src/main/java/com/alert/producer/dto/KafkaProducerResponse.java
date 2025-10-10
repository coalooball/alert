package com.alert.producer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Kafka Producer 响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaProducerResponse {
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 统计信息
     */
    private Statistics statistics;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 耗时（秒）
     */
    private Double durationSeconds;
    
    /**
     * 错误详情
     */
    private String errorDetails;
    
    /**
     * 统计信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statistics {
        /**
         * 总消息数
         */
        private Integer total;
        
        /**
         * 成功数
         */
        private Integer success;
        
        /**
         * 失败数
         */
        private Integer failed;
        
        /**
         * 成功率（百分比）
         */
        private Double successRate;
        
        /**
         * 吞吐量（消息/秒）
         */
        private Double throughput;
    }
}

