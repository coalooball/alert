package com.alert.system.dto;

import com.alert.system.entity.FlinkConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlinkConfigResponse {
    private UUID id;
    private String name;

    @JsonProperty("job_manager_url")
    private String jobManagerUrl;

    private Integer port;
    private String description;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("is_default")
    private Boolean isDefault;

    private String username;

    @JsonProperty("connection_timeout")
    private Integer connectionTimeout;

    @JsonProperty("read_timeout")
    private Integer readTimeout;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("last_test_time")
    private LocalDateTime lastTestTime;

    @JsonProperty("last_test_status")
    private String lastTestStatus;

    @JsonProperty("last_test_message")
    private String lastTestMessage;

    public static FlinkConfigResponse fromEntity(FlinkConfig config) {
        FlinkConfigResponse response = new FlinkConfigResponse();
        response.setId(config.getId());
        response.setName(config.getName());
        response.setJobManagerUrl(config.getJobManagerUrl());
        response.setPort(config.getPort());
        response.setDescription(config.getDescription());
        response.setIsActive(config.getIsActive());
        response.setIsDefault(config.getIsDefault());
        response.setUsername(config.getUsername());
        response.setConnectionTimeout(config.getConnectionTimeout());
        response.setReadTimeout(config.getReadTimeout());
        response.setCreatedAt(config.getCreatedAt());
        response.setUpdatedAt(config.getUpdatedAt());
        response.setLastTestTime(config.getLastTestTime());
        response.setLastTestStatus(config.getLastTestStatus());
        response.setLastTestMessage(config.getLastTestMessage());
        return response;
    }
}