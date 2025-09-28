package com.alert.system.dto;

import com.alert.system.entity.RestHttpDataSourceConfig;
import java.time.LocalDateTime;

public class RestHttpDataSourceConfigResponse {
    private Long id;
    private String configName;
    private Long alertTypeId;
    private String alertTypeName;
    private String alertTypeLabel;
    private String endpointPath;
    private String method;
    private String authType;
    private String apiKeyName;
    private String bearerToken;
    private String basicUsername;
    private String customHeaders;
    private String contentType;
    private String dataValidation;
    private String fieldMapping;
    private Boolean rateLimitEnabled;
    private Integer rateLimitRequests;
    private Integer rateLimitWindow;
    private String description;
    private Boolean isEnabled;
    private String createdBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static RestHttpDataSourceConfigResponse fromEntity(RestHttpDataSourceConfig config) {
        RestHttpDataSourceConfigResponse response = new RestHttpDataSourceConfigResponse();
        response.setId(config.getId());
        response.setConfigName(config.getConfigName());
        response.setAlertTypeId(config.getAlertTypeId());

        if (config.getAlertType() != null) {
            response.setAlertTypeName(config.getAlertType().getTypeName());
            response.setAlertTypeLabel(config.getAlertType().getTypeLabel());
        }

        response.setEndpointPath(config.getEndpointPath());
        response.setMethod(config.getMethod());
        response.setAuthType(config.getAuthType());
        response.setApiKeyName(config.getApiKeyName());
        response.setBearerToken(config.getBearerToken());
        response.setBasicUsername(config.getBasicUsername());
        response.setCustomHeaders(config.getCustomHeaders());
        response.setContentType(config.getContentType());
        response.setDataValidation(config.getDataValidation());
        response.setFieldMapping(config.getFieldMapping());
        response.setRateLimitEnabled(config.getRateLimitEnabled());
        response.setRateLimitRequests(config.getRateLimitRequests());
        response.setRateLimitWindow(config.getRateLimitWindow());
        response.setDescription(config.getDescription());
        response.setIsEnabled(config.getIsEnabled());
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

    public String getEndpointPath() {
        return endpointPath;
    }

    public void setEndpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getApiKeyName() {
        return apiKeyName;
    }

    public void setApiKeyName(String apiKeyName) {
        this.apiKeyName = apiKeyName;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public String getBasicUsername() {
        return basicUsername;
    }

    public void setBasicUsername(String basicUsername) {
        this.basicUsername = basicUsername;
    }

    public String getCustomHeaders() {
        return customHeaders;
    }

    public void setCustomHeaders(String customHeaders) {
        this.customHeaders = customHeaders;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getDataValidation() {
        return dataValidation;
    }

    public void setDataValidation(String dataValidation) {
        this.dataValidation = dataValidation;
    }

    public String getFieldMapping() {
        return fieldMapping;
    }

    public void setFieldMapping(String fieldMapping) {
        this.fieldMapping = fieldMapping;
    }

    public Boolean getRateLimitEnabled() {
        return rateLimitEnabled;
    }

    public void setRateLimitEnabled(Boolean rateLimitEnabled) {
        this.rateLimitEnabled = rateLimitEnabled;
    }

    public Integer getRateLimitRequests() {
        return rateLimitRequests;
    }

    public void setRateLimitRequests(Integer rateLimitRequests) {
        this.rateLimitRequests = rateLimitRequests;
    }

    public Integer getRateLimitWindow() {
        return rateLimitWindow;
    }

    public void setRateLimitWindow(Integer rateLimitWindow) {
        this.rateLimitWindow = rateLimitWindow;
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