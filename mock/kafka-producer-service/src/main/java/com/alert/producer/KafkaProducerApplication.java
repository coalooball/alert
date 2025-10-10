package com.alert.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Kafka Producer Service Application
 * 独立的Kafka生产者服务，用于将JSON文件数据发送到Kafka
 */
@SpringBootApplication
public class KafkaProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaProducerApplication.class, args);
        System.out.println("\n" +
                "╔═══════════════════════════════════════════════════════════╗\n" +
                "║                                                           ║\n" +
                "║       Kafka Producer Service Started Successfully!        ║\n" +
                "║                                                           ║\n" +
                "║  API Documentation: http://localhost:8888/api/health      ║\n" +
                "║                                                           ║\n" +
                "╚═══════════════════════════════════════════════════════════╝\n");
    }
}

