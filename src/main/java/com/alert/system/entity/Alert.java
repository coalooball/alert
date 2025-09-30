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
@Table(name = "alerts", indexes = {
    @Index(name = "idx_alert_uuid", columnList = "alert_uuid"),
    @Index(name = "idx_alert_type_subtype", columnList = "alert_type_id,alert_subtype"),
    @Index(name = "idx_alert_time", columnList = "alert_time"),
    @Index(name = "idx_alert_status", columnList = "status"),
    @Index(name = "idx_event_id", columnList = "event_id")
})
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class Alert {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "alert_uuid", nullable = false, unique = true)
    private String alertUuid;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "alert_type_id", nullable = false)
    private AlertType alertType;

    @Column(name = "alert_subtype", length = 50)
    private String alertSubtype;

    @Column(name = "alert_time", nullable = false)
    private LocalDateTime alertTime;

    @Column(name = "source_ip", length = 50)
    private String sourceIp;

    @Column(name = "dest_ip", length = 50)
    private String destIp;

    @Column(name = "source_port")
    private Integer sourcePort;

    @Column(name = "dest_port")
    private Integer destPort;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "priority", length = 20)
    private String priority;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "raw_data", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String rawData;

    @Column(name = "parsed_data", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String parsedData;

    @Column(name = "is_filtered")
    private Boolean isFiltered = false;

    @Column(name = "filter_rule_id")
    private UUID filterRuleId;

    @Column(name = "filter_reason", length = 500)
    private String filterReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "correlation_key", length = 500)
    private String correlationKey;

    @Column(name = "status", length = 20)
    private String status = "NEW";

    @Column(name = "clickhouse_table", length = 100)
    private String clickhouseTable;

    @Column(name = "clickhouse_id", length = 100)
    private String clickhouseId;

    @Column(name = "kafka_topic", length = 100)
    private String kafkaTopic;

    @Column(name = "kafka_partition")
    private Integer kafkaPartition;

    @Column(name = "kafka_offset")
    private Long kafkaOffset;

    @Column(name = "processing_time")
    private LocalDateTime processingTime;

    @Column(name = "storage_time")
    private LocalDateTime storageTime;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        processingTime = LocalDateTime.now();
        if (alertUuid == null) {
            alertUuid = UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}