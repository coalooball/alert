package com.alert.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alert_filter_rules")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class AlertFilterRule {

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
    private Integer alertType; // 1:网络攻击 2:恶意样本 3:主机行为

    @Column(name = "alert_subtype", length = 20)
    private String alertSubtype;

    @Column(name = "match_field", nullable = false, length = 100)
    private String matchField;

    @Column(name = "match_type", nullable = false, length = 20)
    private String matchType; // exact, regex, contains

    @Column(name = "match_value", nullable = false, columnDefinition = "TEXT")
    private String matchValue;

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