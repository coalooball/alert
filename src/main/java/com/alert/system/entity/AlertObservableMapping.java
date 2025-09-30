package com.alert.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alert_observable_mappings", indexes = {
    @Index(name = "idx_alert_observable_alert", columnList = "alert_id"),
    @Index(name = "idx_alert_observable_observable", columnList = "observable_id"),
    @Index(name = "idx_alert_observable_event", columnList = "event_id")
})
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class AlertObservableMapping {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "observable_id", nullable = false)
    private Observable observable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "role", length = 50)
    private String role;

    @Column(name = "context", length = 200)
    private String context;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @Column(name = "extracted_from", length = 100)
    private String extractedFrom;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}