package com.alert.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alert_tag_mappings", indexes = {
    @Index(name = "idx_alert_id", columnList = "alert_id"),
    @Index(name = "idx_tag_id", columnList = "tag_id"),
    @Index(name = "idx_alert_tag", columnList = "alert_id,tag_id", unique = true)
})
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class AlertTagMapping {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tagging_rule_id")
    private AlertTaggingRule taggingRule;

    @Column(name = "is_auto_tagged")
    private Boolean isAutoTagged = true;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}