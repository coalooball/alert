package com.alert.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlinkConfigRequest {

    @NotBlank(message = "Configuration name is required")
    private String name;

    @NotBlank(message = "Job Manager URL is required")
    private String jobManagerUrl;

    @NotNull(message = "Port is required")
    @Min(value = 1, message = "Port must be between 1 and 65535")
    @Max(value = 65535, message = "Port must be between 1 and 65535")
    private Integer port = 8081;

    private String description;

    private Boolean isActive = false;

    private Boolean isDefault = false;

    private String username;

    private String password;

    @Min(value = 1000, message = "Connection timeout must be at least 1000ms")
    @Max(value = 60000, message = "Connection timeout must not exceed 60000ms")
    private Integer connectionTimeout = 5000;

    @Min(value = 1000, message = "Read timeout must be at least 1000ms")
    @Max(value = 120000, message = "Read timeout must not exceed 120000ms")
    private Integer readTimeout = 10000;
}