package com.alert.system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemInfo {
    @JsonProperty("system_name")
    private String systemName;

    @JsonProperty("system_version")
    private String systemVersion;

    @JsonProperty("kernel_version")
    private String kernelVersion;

    @JsonProperty("hostname")
    private String hostname;

    @JsonProperty("architecture")
    private String architecture;

    @JsonProperty("cpu_cores")
    private int cpuCores;

    @JsonProperty("total_memory")
    private long totalMemory;

    @JsonProperty("available_memory")
    private long availableMemory;

    @JsonProperty("boot_time")
    private long bootTime;

    @JsonProperty("uptime")
    private long uptime;

    @JsonProperty("current_time")
    private String currentTime;

    @JsonProperty("app_version")
    private String appVersion;

    @JsonProperty("database_connected")
    private boolean databaseConnected;

    @JsonProperty("server_address")
    private String serverAddress;

    @JsonProperty("server_port")
    private int serverPort;
}