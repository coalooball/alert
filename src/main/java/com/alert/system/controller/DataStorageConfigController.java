package com.alert.system.controller;

import com.alert.system.dto.DataStorageConfigDTO;
import com.alert.system.enums.DatabaseType;
import com.alert.system.service.DataStorageConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data-storage-config")
@PreAuthorize("hasRole('ADMIN')")
public class DataStorageConfigController {

    @Autowired
    private DataStorageConfigService service;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllConfigs() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.getAllConfigs());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveConfigs() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.getActiveConfigs());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getConfigById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            DataStorageConfigDTO config = service.getConfigById(id);
            response.put("success", true);
            response.put("data", config);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/default")
    public ResponseEntity<Map<String, Object>> getDefaultConfig() {
        Map<String, Object> response = new HashMap<>();
        DataStorageConfigDTO config = service.getDefaultConfig();
        response.put("success", true);
        response.put("data", config);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createConfig(@RequestBody DataStorageConfigDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            DataStorageConfigDTO created = service.createConfig(dto);
            response.put("success", true);
            response.put("data", created);
            response.put("message", "Data storage configuration created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateConfig(
            @PathVariable Long id,
            @RequestBody DataStorageConfigDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            DataStorageConfigDTO updated = service.updateConfig(id, dto);
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "Data storage configuration updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteConfig(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            service.deleteConfig(id);
            response.put("success", true);
            response.put("message", "Data storage configuration deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody DataStorageConfigDTO dto) {
        Map<String, Object> result = service.testConnection(dto);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/database-types")
    public ResponseEntity<Map<String, Object>> getSupportedDatabaseTypes() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> types = service.getSupportedDatabaseTypes().stream()
                .map(type -> {
                    Map<String, String> item = new HashMap<>();
                    item.put("value", type.name());
                    item.put("label", type.getDisplayName());
                    return item;
                })
                .toList();
        response.put("success", true);
        response.put("data", types);
        return ResponseEntity.ok(response);
    }
}