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
@Table(name = "alert_convergence_rules")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class AlertConvergenceRule {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "rule_name", nullable = false, length = 200)
    private String ruleName;

    @Column(name = "rule_description", columnDefinition = "TEXT")
    private String ruleDescription;

    @Column(name = "alert_type", nullable = false)
    private Integer alertType;

    @Column(name = "alert_subtype", nullable = false, length = 20)
    private String alertSubtype;

    @Column(name = "convergence_type", nullable = false, length = 50)
    private String convergenceType; // field_match, ml_algorithm

    // 计算引擎收敛字段
    @Column(name = "engine_fields", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> engineFields;

    // 机器学习收敛字段
    @Column(name = "ml_fields", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> mlFields;

    // 机器学习相关配置
    @Column(name = "use_ml_model")
    private Boolean useMlModel = false;

    @Column(name = "ml_model_name", length = 100)
    private String mlModelName;

    @Column(name = "ml_model_config", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> mlModelConfig;

    @Column(name = "convergence_config", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> convergenceConfig;

    @Column(name = "time_window")
    private Integer timeWindow = 3600;

    @Column(name = "min_count")
    private Integer minCount = 2;


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