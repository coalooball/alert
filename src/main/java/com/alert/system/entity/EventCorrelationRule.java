package com.alert.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_correlation_rules")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class EventCorrelationRule {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "rule_name", nullable = false, unique = true, length = 200)
    private String ruleName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType;

    @Column(name = "correlation_level", length = 20)
    private String correlationLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_type_id")
    private AlertType alertType;

    @Column(name = "alert_subtype", length = 50)
    private String alertSubtype;

    @Column(name = "time_window_seconds")
    private Integer timeWindowSeconds = 300;

    @Column(name = "time_window_type", length = 20)
    private String timeWindowType = "SLIDING";

    @Column(name = "time_window_slide_seconds")
    private Integer timeWindowSlideSeconds = 60;

    @Column(name = "min_alert_count")
    private Integer minAlertCount = 2;

    @Column(name = "max_alert_count")
    private Integer maxAlertCount = 100;

    @Column(name = "correlation_fields", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] correlationFields;

    @Column(name = "grouping_fields", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] groupingFields;

    @Column(name = "aggregation_rules", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String aggregationRules;

    @Column(name = "condition_expression", columnDefinition = "TEXT")
    private String conditionExpression;

    @Column(name = "flink_job_config", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String flinkJobConfig;

    @Column(name = "event_template", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String eventTemplate;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Column(name = "is_cross_type")
    private Boolean isCrossType = false;

    @Column(name = "execution_count")
    private Long executionCount = 0L;

    @Column(name = "success_count")
    private Long successCount = 0L;

    @Column(name = "last_execution_time")
    private LocalDateTime lastExecutionTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}