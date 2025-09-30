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
@Table(name = "events")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class Event {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "system_code", nullable = false, unique = true, length = 100)
    private String systemCode;

    @Column(name = "name", nullable = false, length = 500)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "attacker", length = 200)
    private String attacker;

    @Column(name = "victim", length = 200)
    private String victim;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "found_time")
    private LocalDateTime foundTime;

    @Column(name = "first_found_time")
    private LocalDateTime firstFoundTime;

    @Column(name = "source", length = 200)
    private String source;

    @Column(name = "mitre_technique_id", columnDefinition = "TEXT")
    private String mitreTechniqueId;

    @Column(name = "attack_list", columnDefinition = "TEXT")
    private String attackList;

    @Column(name = "attack_tool", columnDefinition = "TEXT")
    private String attackTool;

    @Column(name = "priority", length = 20)
    private String priority;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "dispose_status", length = 50)
    private String disposeStatus;

    @Column(name = "app", columnDefinition = "TEXT")
    private String app;

    @Column(name = "impact_assessment", columnDefinition = "TEXT")
    private String impactAssessment;

    @Column(name = "merge_alerts", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String mergeAlerts;

    @Column(name = "threat_actors", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String threatActors;

    @Column(name = "organizations", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String organizations;

    @Column(name = "attack_asset_ips", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> attackAssetIps;

    @Column(name = "victim_asset_ips", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> victimAssetIps;

    @Column(name = "attack_asset_ip_ports", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> attackAssetIpPorts;

    @Column(name = "victim_asset_ip_ports", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> victimAssetIpPorts;

    @Column(name = "attack_asset_domains", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> attackAssetDomains;

    @Column(name = "victim_asset_domains", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> victimAssetDomains;

    @Column(name = "attack_urls", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> attackUrls;

    @Column(name = "victim_urls", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> victimUrls;

    @Column(name = "attack_malwares", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> attackMalwares;

    @Column(name = "attack_malware_samples", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> attackMalwareSamples;

    @Column(name = "attack_malware_sample_families", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> attackMalwareSampleFamilies;

    @Column(name = "attack_email_addresses", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> attackEmailAddresses;

    @Column(name = "victim_email_addresses", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> victimEmailAddresses;

    @Column(name = "attack_emails", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> attackEmails;

    @Column(name = "victim_emails", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> victimEmails;

    @Column(name = "attack_softwares", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> attackSoftwares;

    @Column(name = "victim_softwares", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> victimSoftwares;

    @Column(name = "attack_vulnerabilities", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> attackVulnerabilities;

    @Column(name = "attack_certificates", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> attackCertificates;

    @Column(name = "victim_certificates", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> victimCertificates;

    @Column(name = "correlation_rule_id")
    private UUID correlationRuleId;

    @Column(name = "correlation_key", length = 500)
    private String correlationKey;

    @Column(name = "alert_count")
    private Integer alertCount = 0;

    @Column(name = "status", length = 20)
    private String status = "OPEN";

    @Column(name = "is_auto_generated")
    private Boolean isAutoGenerated = true;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "additional_data", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String additionalData;

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
        if (systemCode == null) {
            systemCode = generateSystemCode();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateSystemCode() {
        return String.format("EVT-%s-%s",
            LocalDateTime.now().toString().replace("-", "").replace(":", "").replace(".", "").substring(0, 14),
            UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}