package com.alert.system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlinkTestResult {
    private boolean success;
    private String message;

    @JsonProperty("test_time")
    private LocalDateTime testTime;

    @JsonProperty("response_time_ms")
    private Long responseTimeMs;

    @JsonProperty("flink_version")
    private String flinkVersion;

    @JsonProperty("cluster_id")
    private String clusterId;

    @JsonProperty("job_manager_address")
    private String jobManagerAddress;

    @JsonProperty("task_managers")
    private Integer taskManagers;

    @JsonProperty("total_task_slots")
    private Integer totalTaskSlots;

    @JsonProperty("available_task_slots")
    private Integer availableTaskSlots;

    @JsonProperty("running_jobs")
    private Integer runningJobs;

    @JsonProperty("additional_info")
    private Map<String, Object> additionalInfo;

    private String error;

    @JsonProperty("error_detail")
    private String errorDetail;
}