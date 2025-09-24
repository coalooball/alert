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
@Table(name = "threat_events")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class ThreatEvent {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "event_id", unique = true, nullable = false, length = 100)
    private String eventId;

    @Column(name = "event_name", nullable = false, length = 500)
    private String eventName;

    @Column(name = "event_description", columnDefinition = "TEXT")
    private String eventDescription;

    @Column(name = "threat_level", nullable = false)
    private Integer threatLevel; // 1-5

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "correlation_rule_id")
    private AlertCorrelationRule correlationRule;

    @Column(name = "converged_alert_ids", columnDefinition = "uuid[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<UUID> convergedAlertIds;

    @Column(name = "event_data", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> eventData;

    @Column(name = "event_timeline", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> eventTimeline;

    @Column(name = "tags", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> tags;

    @Column(name = "status", length = 50)
    private String status = "active"; // active, investigating, resolved, false_positive

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