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
import java.util.UUID;

@Entity
@Table(name = "observables", indexes = {
    @Index(name = "idx_observable_type", columnList = "observable_type"),
    @Index(name = "idx_observable_value", columnList = "observable_value"),
    @Index(name = "idx_observable_type_value", columnList = "observable_type,observable_value", unique = true)
})
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class Observable {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "observable_type", nullable = false, length = 50)
    private String observableType;

    @Column(name = "observable_value", nullable = false, length = 500)
    private String observableValue;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "is_malicious")
    private Boolean isMalicious;

    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "first_seen")
    private LocalDateTime firstSeen;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "occurrence_count")
    private Integer occurrenceCount = 1;

    @Column(name = "tags", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> tags;

    @Column(name = "attributes", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String attributes;

    @Column(name = "threat_intel", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String threatIntel;

    @Column(name = "geo_location", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String geoLocation;

    @Column(name = "whois_info", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String whoisInfo;

    @Column(name = "related_observables", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String relatedObservables;

    @Column(name = "enrichment_data", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String enrichmentData;

    @Column(name = "metadata", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    @Column(name = "status", length = 20)
    private String status = "ACTIVE";

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (firstSeen == null) {
            firstSeen = LocalDateTime.now();
        }
        lastSeen = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        lastSeen = LocalDateTime.now();
        if (occurrenceCount != null) {
            occurrenceCount++;
        }
    }
}