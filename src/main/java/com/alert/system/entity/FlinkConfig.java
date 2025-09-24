package com.alert.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "flink_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlinkConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String jobManagerUrl;

    @Column(nullable = false)
    private Integer port = 8081;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean isActive = false;

    @Column(nullable = false)
    private Boolean isDefault = false;

    @Column(length = 100)
    private String username;

    @Column(length = 100)
    private String password;

    @Column(name = "connection_timeout")
    private Integer connectionTimeout = 5000;

    @Column(name = "read_timeout")
    private Integer readTimeout = 10000;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_test_time")
    private LocalDateTime lastTestTime;

    @Column(name = "last_test_status")
    private String lastTestStatus;

    @Column(name = "last_test_message", length = 1000)
    private String lastTestMessage;
}