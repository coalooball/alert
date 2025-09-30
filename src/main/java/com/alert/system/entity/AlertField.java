package com.alert.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_fields",
       uniqueConstraints = @UniqueConstraint(columnNames = {"alert_type_id", "field_name"}))
@Data
@EqualsAndHashCode(exclude = {"alertType"})
@ToString(exclude = {"alertType"})
public class AlertField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_type_id", nullable = false)
    @JsonIgnore
    private AlertType alertType;

    @Column(name = "alert_type_id", insertable = false, updatable = false)
    private Integer alertTypeId;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "field_label", nullable = false, length = 100)
    private String fieldLabel;

    @Column(name = "field_type", length = 50)
    private String fieldType = "string";

    @Column(name = "data_type", length = 50)
    private String dataType = "string";

    @Column(name = "field_path", length = 200)
    private String fieldPath;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

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