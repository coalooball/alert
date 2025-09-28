package com.alert.system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rest_http_datasource_config")
public class RestHttpDataSourceConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_name", nullable = false, length = 200)
    private String configName;

    @Column(name = "alert_type_id", nullable = false)
    private Long alertTypeId;

    @Column(name = "endpoint_path", nullable = false, length = 500)
    private String endpointPath;

    @Column(name = "method", nullable = false, length = 10)
    private String method = "POST";

    @Column(name = "auth_type", length = 50)
    private String authType = "none";

    @Column(name = "api_key_name", length = 100)
    private String apiKeyName;

    @Column(name = "api_key_value", length = 500)
    private String apiKeyValue;

    @Column(name = "bearer_token", length = 1000)
    private String bearerToken;

    @Column(name = "basic_username", length = 200)
    private String basicUsername;

    @Column(name = "basic_password", length = 500)
    private String basicPassword;

    @Column(name = "custom_headers", columnDefinition = "JSON")
    private String customHeaders;

    @Column(name = "content_type", length = 100)
    private String contentType = "application/json";

    @Column(name = "data_validation", columnDefinition = "TEXT")
    private String dataValidation;

    @Column(name = "field_mapping", columnDefinition = "JSON")
    private String fieldMapping;

    @Column(name = "rate_limit_enabled", nullable = false)
    private Boolean rateLimitEnabled = false;

    @Column(name = "rate_limit_requests")
    private Integer rateLimitRequests;

    @Column(name = "rate_limit_window")
    private Integer rateLimitWindow;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    // 关联alert_types表
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_type_id", insertable = false, updatable = false)
    private AlertType alertType;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createTime = now;
        updateTime = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
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

    public String getApiKeyValue() {
        return apiKeyValue;
    }

    public void setApiKeyValue(String apiKeyValue) {
        this.apiKeyValue = apiKeyValue;
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

    public String getBasicPassword() {
        return basicPassword;
    }

    public void setBasicPassword(String basicPassword) {
        this.basicPassword = basicPassword;
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

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }
}