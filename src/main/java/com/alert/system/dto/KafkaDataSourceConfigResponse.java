package com.alert.system.dto;

import com.alert.system.entity.KafkaDataSourceConfig;
import java.time.LocalDateTime;

public class KafkaDataSourceConfigResponse {
    private Long id;
    private String configName;
    private Long alertTypeId;
    private String alertTypeName;
    private String alertTypeLabel;
    private String brokers;
    private String topicName;
    private String consumerGroup;
    private String securityProtocol;
    private String saslMechanism;
    private String username;
    private Integer sessionTimeout;
    private Integer maxPollRecords;
    private String autoOffsetReset;
    private String dataFormat;
    private String fieldMapping;
    private String description;
    private Boolean isEnabled;
    private String connectionStatus;
    private String createdBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static KafkaDataSourceConfigResponse fromEntity(KafkaDataSourceConfig config) {
        KafkaDataSourceConfigResponse response = new KafkaDataSourceConfigResponse();
        response.setId(config.getId());
        response.setConfigName(config.getConfigName());
        response.setAlertTypeId(config.getAlertTypeId());

        if (config.getAlertType() != null) {
            response.setAlertTypeName(config.getAlertType().getTypeName());
            response.setAlertTypeLabel(config.getAlertType().getTypeLabel());
        }

        response.setBrokers(config.getBrokers());
        response.setTopicName(config.getTopicName());
        response.setConsumerGroup(config.getConsumerGroup());
        response.setSecurityProtocol(config.getSecurityProtocol());
        response.setSaslMechanism(config.getSaslMechanism());
        response.setUsername(config.getUsername());
        response.setSessionTimeout(config.getSessionTimeout());
        response.setMaxPollRecords(config.getMaxPollRecords());
        response.setAutoOffsetReset(config.getAutoOffsetReset());
        response.setDataFormat(config.getDataFormat());
        response.setFieldMapping(config.getFieldMapping());
        response.setDescription(config.getDescription());
        response.setIsEnabled(config.getIsEnabled());
        response.setConnectionStatus(config.getConnectionStatus());
        response.setCreatedBy(config.getCreatedBy());
        response.setCreateTime(config.getCreateTime());
        response.setUpdateTime(config.getUpdateTime());

        return response;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public Long getAlertTypeId() {
        return alertTypeId;
    }

    public void setAlertTypeId(Long alertTypeId) {
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

    public String getBrokers() {
        return brokers;
    }

    public void setBrokers(String brokers) {
        this.brokers = brokers;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getSecurityProtocol() {
        return securityProtocol;
    }

    public void setSecurityProtocol(String securityProtocol) {
        this.securityProtocol = securityProtocol;
    }

    public String getSaslMechanism() {
        return saslMechanism;
    }

    public void setSaslMechanism(String saslMechanism) {
        this.saslMechanism = saslMechanism;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(Integer sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public Integer getMaxPollRecords() {
        return maxPollRecords;
    }

    public void setMaxPollRecords(Integer maxPollRecords) {
        this.maxPollRecords = maxPollRecords;
    }

    public String getAutoOffsetReset() {
        return autoOffsetReset;
    }

    public void setAutoOffsetReset(String autoOffsetReset) {
        this.autoOffsetReset = autoOffsetReset;
    }

    public String getDataFormat() {
        return dataFormat;
    }

    public void setDataFormat(String dataFormat) {
        this.dataFormat = dataFormat;
    }

    public String getFieldMapping() {
        return fieldMapping;
    }

    public void setFieldMapping(String fieldMapping) {
        this.fieldMapping = fieldMapping;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}