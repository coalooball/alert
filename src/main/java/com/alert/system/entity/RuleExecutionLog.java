package com.alert.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "rule_execution_logs")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class RuleExecutionLog {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType; // filter, convergence, tagging, correlation

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(name = "execution_time", nullable = false)
    private LocalDateTime executionTime;

    @Column(name = "input_count")
    private Integer inputCount = 0;

    @Column(name = "output_count")
    private Integer outputCount = 0;

    @Column(name = "success")
    private Boolean success = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "execution_details", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> executionDetails;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}