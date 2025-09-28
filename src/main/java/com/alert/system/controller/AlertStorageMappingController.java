package com.alert.system.controller;

import com.alert.system.dto.AlertStorageMappingDTO;
import com.alert.system.service.AlertStorageMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alert-storage-mapping")
@PreAuthorize("hasRole('ADMIN')")
public class AlertStorageMappingController {

    @Autowired
    private AlertStorageMappingService service;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllMappings() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.getAllMappings());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveMappings() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.getActiveMappings());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMappingById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            AlertStorageMappingDTO mapping = service.getMappingById(id);
            response.put("success", true);
            response.put("data", mapping);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/alert-type/{alertTypeId}")
    public ResponseEntity<Map<String, Object>> getMappingByAlertType(@PathVariable Integer alertTypeId) {
        Map<String, Object> response = new HashMap<>();
        AlertStorageMappingDTO mapping = service.getMappingByAlertType(alertTypeId);
        response.put("success", true);
        response.put("data", mapping);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-storage/{storageConfigId}")
    public ResponseEntity<Map<String, Object>> getMappingsByStorageConfig(@PathVariable Long storageConfigId) {
        Map<String, Object> response = new HashMap<>();
        List<AlertStorageMappingDTO> mappings = service.getMappingsByStorageConfig(storageConfigId);
        response.put("success", true);
        response.put("data", mappings);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createMapping(@RequestBody AlertStorageMappingDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            AlertStorageMappingDTO created = service.createMapping(dto);
            response.put("success", true);
            response.put("data", created);
            response.put("message", "Alert storage mapping created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateMapping(
            @PathVariable Long id,
            @RequestBody AlertStorageMappingDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            AlertStorageMappingDTO updated = service.updateMapping(id, dto);
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "Alert storage mapping updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMapping(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            service.deleteMapping(id);
            response.put("success", true);
            response.put("message", "Alert storage mapping deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}/ddl")
    public ResponseEntity<Map<String, Object>> generateDDL(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            String ddl = service.generateTableDDL(id);
            response.put("success", true);
            response.put("data", ddl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}/table-exists")
    public ResponseEntity<Map<String, Object>> checkTableExists(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean exists = service.checkTableExists(id);
            response.put("success", true);
            response.put("data", Map.of("exists", exists));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/create-table")
    public ResponseEntity<Map<String, Object>> createTable(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean created = service.createStorageTable(id);
            response.put("success", true);
            if (created) {
                response.put("message", "Storage table created successfully");
            } else {
                response.put("message", "Table already exists");
            }
            response.put("data", Map.of("created", created));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

}