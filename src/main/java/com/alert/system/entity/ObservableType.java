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
@Table(name = "observable_types")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class ObservableType {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "type_name", nullable = false, unique = true, length = 50)
    private String typeName;

    @Column(name = "type_label", nullable = false, length = 100)
    private String typeLabel;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "validation_regex", length = 500)
    private String validationRegex;

    @Column(name = "extraction_regex", length = 500)
    private String extractionRegex;

    @Column(name = "normalization_rules", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String normalizationRules;

    @Column(name = "enrichment_config", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String enrichmentConfig;

    @Column(name = "display_template", length = 500)
    private String displayTemplate;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Column(name = "is_system_type")
    private Boolean isSystemType = false;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}