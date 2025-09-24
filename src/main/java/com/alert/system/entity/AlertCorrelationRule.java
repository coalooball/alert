package com.alert.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "alert_correlation_rules")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class AlertCorrelationRule {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "rule_name", nullable = false, length = 200)
    private String ruleName;

    @Column(name = "rule_description", columnDefinition = "TEXT")
    private String ruleDescription;

    @Column(name = "correlation_type", nullable = false, length = 50)
    private String correlationType; // sequential, parallel, hybrid

    @Column(name = "source_alert_type", nullable = false)
    private Integer sourceAlertType;

    @Column(name = "source_alert_subtype", length = 20)
    private String sourceAlertSubtype;

    @Column(name = "target_alert_type", nullable = false)
    private Integer targetAlertType;

    @Column(name = "target_alert_subtype", length = 20)
    private String targetAlertSubtype;

    @Column(name = "correlation_fields", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> correlationFields;

    @Column(name = "time_window")
    private Integer timeWindow = 3600;

    @Column(name = "correlation_config", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> correlationConfig;

    @Column(name = "threat_level")
    private Integer threatLevel = 1;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

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