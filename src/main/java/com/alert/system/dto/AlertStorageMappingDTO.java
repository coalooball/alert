package com.alert.system.dto;

import java.time.LocalDateTime;

public class AlertStorageMappingDTO {
    private Long id;
    private Integer alertTypeId;
    private String alertTypeName;
    private String alertTypeLabel;
    private Long storageConfigId;
    private String storageConfigName;
    private String storageDatabase;
    private String storageHost;
    private String tableName;
    private Integer retentionDays;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getAlertTypeId() {
        return alertTypeId;
    }

    public void setAlertTypeId(Integer alertTypeId) {
        this.alertTypeId = alertTypeId;
    }

    public String getAlertTypeName() {
        return alertTypeName;
    }

    public void setAlertTypeName(String alertTypeName) {
        this.alertTypeName = alertTypeName;
    }

    public String getAlertTypeLabel() {
        return alertTypeLabel;
    }

    public void setAlertTypeLabel(String alertTypeLabel) {
        this.alertTypeLabel = alertTypeLabel;
    }

    public Long getStorageConfigId() {
        return storageConfigId;
    }

    public void setStorageConfigId(Long storageConfigId) {
        this.storageConfigId = storageConfigId;
    }

    public String getStorageConfigName() {
        return storageConfigName;
    }

    public void setStorageConfigName(String storageConfigName) {
        this.storageConfigName = storageConfigName;
    }

    public String getStorageDatabase() {
        return storageDatabase;
    }

    public void setStorageDatabase(String storageDatabase) {
        this.storageDatabase = storageDatabase;
    }

    public String getStorageHost() {
        return storageHost;
    }

    public void setStorageHost(String storageHost) {
        this.storageHost = storageHost;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Integer getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(Integer retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}