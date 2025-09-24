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
@Table(name = "converged_alerts")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class ConvergedAlert {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "convergence_id", unique = true, nullable = false, length = 100)
    private String convergenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convergence_rule_id")
    private AlertConvergenceRule convergenceRule;

    @Column(name = "alert_type", nullable = false)
    private Integer alertType;

    @Column(name = "alert_subtype", nullable = false, length = 20)
    private String alertSubtype;

    @Column(name = "alert_count", nullable = false)
    private Integer alertCount;

    @Column(name = "first_seen_time", nullable = false)
    private LocalDateTime firstSeenTime;

    @Column(name = "last_seen_time", nullable = false)
    private LocalDateTime lastSeenTime;

    @Column(name = "severity_max")
    private Integer severityMax;

    @Column(name = "convergence_data", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> convergenceData;

    @Column(name = "original_alert_ids", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> originalAlertIds;

    @Column(name = "tags", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> tags;

    @Column(name = "is_correlated")
    private Boolean isCorrelated = false;

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