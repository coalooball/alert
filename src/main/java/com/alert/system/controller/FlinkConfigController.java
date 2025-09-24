package com.alert.system.controller;

import com.alert.system.dto.FlinkConfigRequest;
import com.alert.system.dto.FlinkConfigResponse;
import com.alert.system.dto.FlinkTestResult;
import com.alert.system.service.FlinkConfigService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/flink-configs")
@PreAuthorize("isAuthenticated()")
public class FlinkConfigController {

    @Autowired
    private FlinkConfigService flinkConfigService;

    @GetMapping
    public ResponseEntity<List<FlinkConfigResponse>> getAllConfigs() {
        List<FlinkConfigResponse> configs = flinkConfigService.getAllConfigs();
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlinkConfigResponse> getConfigById(@PathVariable UUID id) {
        try {
            FlinkConfigResponse config = flinkConfigService.getConfigById(id);
            return ResponseEntity.ok(config);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createConfig(@Valid @RequestBody FlinkConfigRequest request) {
        try {
            FlinkConfigResponse config = flinkConfigService.createConfig(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(config);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateConfig(
            @PathVariable UUID id,
            @Valid @RequestBody FlinkConfigRequest request) {
        try {
            FlinkConfigResponse config = flinkConfigService.updateConfig(id, request);
            return ResponseEntity.ok(config);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteConfig(@PathVariable UUID id) {
        try {
            flinkConfigService.deleteConfig(id);
            Map<String, String> result = new HashMap<>();
            result.put("message", "Flink configuration deleted successfully");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<FlinkTestResult> testConnection(@PathVariable UUID id) {
        try {
            FlinkTestResult result = flinkConfigService.testConnection(id);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            FlinkTestResult result = FlinkTestResult.builder()
                    .success(false)
                    .message("Configuration not found")
                    .error(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
    }

    @PostMapping("/test")
    public ResponseEntity<FlinkTestResult> testConnectionByRequest(
            @Valid @RequestBody FlinkConfigRequest request) {
        FlinkTestResult result = flinkConfigService.testConnectionByRequest(request);
        return ResponseEntity.ok(result);
    }
}