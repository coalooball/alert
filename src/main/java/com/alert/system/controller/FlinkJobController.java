package com.alert.system.controller;

import com.alert.system.entity.FlinkConfig;
import com.alert.system.repository.FlinkConfigRepository;
import com.alert.system.service.FlinkJobSubmitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Flink作业管理API
 */
@RestController
@RequestMapping("/api/flink/jobs")
@RequiredArgsConstructor
@Slf4j
public class FlinkJobController {

    private final FlinkJobSubmitService flinkJobSubmitService;
    private final FlinkConfigRepository flinkConfigRepository;

    // 获取集群概览
    @GetMapping("/cluster/overview")
    public ResponseEntity<?> getClusterOverview() {
        try {
            FlinkConfig config = getActiveFlinkConfig();
            Map<String, Object> overview = flinkJobSubmitService.getClusterOverview(config);
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            log.error("Failed to get cluster overview", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Flink API
    @GetMapping("/cluster/taskmanagers")
    public ResponseEntity<?> getTaskManagers() {
        try {
            FlinkConfig config = getActiveFlinkConfig();
            List<Map<String, Object>> taskManagers = flinkJobSubmitService.getTaskManagers(config);
            return ResponseEntity.ok(taskManagers);
        } catch (Exception e) {
            log.error("Failed to get task managers", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Flink API
    @PostMapping("/jars/upload")
    public ResponseEntity<?> uploadJar(@RequestParam("file") MultipartFile file) {
        try {
            FlinkConfig config = getActiveFlinkConfig();

            // 保存上传的文件到临时目录
            String tempDir = System.getProperty("java.io.tmpdir");
            Path tempFile = Paths.get(tempDir, file.getOriginalFilename());
            Files.write(tempFile, file.getBytes());

            // 上传到Flink集群
            String jarId = flinkJobSubmitService.uploadJar(config, tempFile.toString());

            // 删除临时文件
            Files.deleteIfExists(tempFile);

            return ResponseEntity.ok(Map.of(
                "jarId", jarId,
                "message", "JAR uploaded successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to upload JAR", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Flink API
    @PostMapping("/submit")
    public ResponseEntity<?> submitJob(@RequestBody Map<String, Object> jobConfig) {
        try {
            FlinkConfig config = getActiveFlinkConfig();

            String jarId = (String) jobConfig.get("jarId");
            if (jarId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "jarId is required"));
            }

            String jobId = flinkJobSubmitService.submitJob(config, jarId, jobConfig);

            return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "message", "Job submitted successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to submit job", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Flink API
    @GetMapping("/list")
    public ResponseEntity<?> listJobs() {
        try {
            FlinkConfig config = getActiveFlinkConfig();
            List<Map<String, Object>> jobs = flinkJobSubmitService.listJobs(config);
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            log.error("Failed to list jobs", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Flink API
    @GetMapping("/{jobId}/status")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId) {
        try {
            FlinkConfig config = getActiveFlinkConfig();
            Map<String, Object> status = flinkJobSubmitService.getJobStatus(config, jobId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to get job status", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Flink API
    @GetMapping("/{jobId}/metrics")
    public ResponseEntity<?> getJobMetrics(
            @PathVariable String jobId,
            @RequestParam(required = false) List<String> metrics) {
        try {
            FlinkConfig config = getActiveFlinkConfig();
            Map<String, Object> metricsData = flinkJobSubmitService.getJobMetrics(config, jobId, metrics);
            return ResponseEntity.ok(metricsData);
        } catch (Exception e) {
            log.error("Failed to get job metrics", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Flink API
    @PostMapping("/{jobId}/cancel")
    public ResponseEntity<?> cancelJob(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "false") boolean withSavepoint) {
        try {
            FlinkConfig config = getActiveFlinkConfig();
            boolean success = flinkJobSubmitService.cancelJob(config, jobId, withSavepoint);

            if (success) {
                return ResponseEntity.ok(Map.of(
                    "message", "Job cancelled successfully",
                    "withSavepoint", withSavepoint
                ));
            } else {
                return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to cancel job"
                ));
            }
        } catch (Exception e) {
            log.error("Failed to cancel job", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Flink API
    @PostMapping("/alert/filter")
    public ResponseEntity<?> submitAlertFilterJob(@RequestBody Map<String, Object> request) {
        try {
            FlinkConfig config = getActiveFlinkConfig();

            // 这里简化处理，实际应该从request中解析Alert和Rules
            Map<String, Object> jobConfig = new HashMap<>();
            jobConfig.put("operation", "FILTER");
            jobConfig.put("alertData", request.get("alert"));
            jobConfig.put("filterRules", request.get("rules"));
            jobConfig.put("parallelism", request.getOrDefault("parallelism", 2));
            jobConfig.put("jobName", "alert-filter-" + System.currentTimeMillis());

            // 使用默认的JAR
            String jarId = "alert-processing.jar";
            String jobId = flinkJobSubmitService.submitJob(config, jarId, jobConfig);

            return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "message", "Alert filter job submitted successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to submit alert filter job", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Flink API
    @PostMapping("/alert/tagging")
    public ResponseEntity<?> submitAlertTaggingJob(@RequestBody Map<String, Object> request) {
        try {
            FlinkConfig config = getActiveFlinkConfig();

            Map<String, Object> jobConfig = new HashMap<>();
            jobConfig.put("operation", "TAGGING");
            jobConfig.put("alertData", request.get("alert"));
            jobConfig.put("taggingRules", request.get("rules"));
            jobConfig.put("parallelism", request.getOrDefault("parallelism", 2));
            jobConfig.put("jobName", "alert-tagging-" + System.currentTimeMillis());

            String jarId = "alert-processing.jar";
            String jobId = flinkJobSubmitService.submitJob(config, jarId, jobConfig);

            return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "message", "Alert tagging job submitted successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to submit alert tagging job", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Flink API
    @PostMapping("/alert/correlation")
    public ResponseEntity<?> submitAlertCorrelationJob(@RequestBody Map<String, Object> request) {
        try {
            FlinkConfig config = getActiveFlinkConfig();

            Map<String, Object> jobConfig = new HashMap<>();
            jobConfig.put("operation", "CORRELATION");
            jobConfig.put("correlationRule", request.get("rule"));
            jobConfig.put("timeWindowSeconds", request.getOrDefault("timeWindowSeconds", 300));
            jobConfig.put("parallelism", request.getOrDefault("parallelism", 4));
            jobConfig.put("jobName", "alert-correlation-" + System.currentTimeMillis());

            String jarId = "alert-processing.jar";
            String jobId = flinkJobSubmitService.submitJob(config, jarId, jobConfig);

            return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "message", "Alert correlation job submitted successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to submit alert correlation job", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Flink API
    @PostMapping("/cep/submit")
    public ResponseEntity<?> submitCEPJob(@RequestBody Map<String, Object> request) {
        try {
            FlinkConfig config = getActiveFlinkConfig();

            Map<String, Object> jobConfig = new HashMap<>();
            jobConfig.put("operation", "CEP");
            jobConfig.put("pattern", request.get("pattern"));
            jobConfig.put("timeWindowMinutes", request.getOrDefault("timeWindowMinutes", 5));
            jobConfig.put("inputTopic", request.get("inputTopic"));
            jobConfig.put("outputTopic", request.get("outputTopic"));
            jobConfig.put("parallelism", request.getOrDefault("parallelism", 2));
            jobConfig.put("jobName", "cep-" + System.currentTimeMillis());

            String jarId = "alert-processing.jar";
            String jobId = flinkJobSubmitService.submitJob(config, jarId, jobConfig);

            return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "message", "CEP job submitted successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to submit CEP job", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取当前激活的Flink配置
     */
    private FlinkConfig getActiveFlinkConfig() {
        return flinkConfigRepository.findByIsActiveTrue()
            .orElseThrow(() -> new RuntimeException("No active Flink configuration found"));
    }
}