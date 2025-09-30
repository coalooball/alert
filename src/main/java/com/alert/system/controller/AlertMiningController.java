package com.alert.system.controller;

import com.alert.system.entity.AlertStorageMapping;
import com.alert.system.service.AlertMiningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/alert-mining")
public class AlertMiningController {

    private static final Logger logger = LoggerFactory.getLogger(AlertMiningController.class);

    @Autowired
    private AlertMiningService alertMiningService;

    @GetMapping("/alerts")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getAllAlertsWithTags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("Fetching alerts with tags - page: {}, size: {}", page, size);

        List<Map<String, Object>> alerts = alertMiningService.getAllAlertsWithTags(page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("alerts", alerts);
        response.put("page", page);
        response.put("size", size);
        response.put("totalItems", alerts.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/alerts/by-type/{alertTypeId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getAlertsByType(
            @PathVariable Integer alertTypeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("Fetching alerts by type {} with tags - page: {}, size: {}", alertTypeId, page, size);

        List<Map<String, Object>> alerts = alertMiningService.getAlertsByTypeWithTags(alertTypeId, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("alerts", alerts);
        response.put("alertTypeId", alertTypeId);
        response.put("page", page);
        response.put("size", size);
        response.put("totalItems", alerts.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/alerts/by-time-range")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getAlertsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("Fetching alerts by time range from {} to {} - page: {}, size: {}", startTime, endTime, page, size);

        List<Map<String, Object>> alerts = alertMiningService.getAlertsByTimeRange(startTime, endTime, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("alerts", alerts);
        response.put("startTime", startTime);
        response.put("endTime", endTime);
        response.put("page", page);
        response.put("size", size);
        response.put("totalItems", alerts.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/alerts/{alertId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getAlertById(@PathVariable UUID alertId) {
        logger.info("Fetching alert by ID: {}", alertId);

        Map<String, Object> alert = alertMiningService.getAlertById(alertId);

        if (alert == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(alert);
    }

    @GetMapping("/storage-mappings")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<AlertStorageMapping>> getAllStorageMappings() {
        logger.info("Fetching all storage mappings");

        List<AlertStorageMapping> mappings = alertMiningService.getAllStorageMappings();

        return ResponseEntity.ok(mappings);
    }

    @GetMapping("/storage-mappings/by-type/{alertTypeId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AlertStorageMapping> getStorageMappingByType(@PathVariable Integer alertTypeId) {
        logger.info("Fetching storage mapping for alert type: {}", alertTypeId);

        AlertStorageMapping mapping = alertMiningService.getStorageMappingByAlertType(alertTypeId);

        if (mapping == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(mapping);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getAlertStatistics() {
        logger.info("Fetching alert statistics");

        Map<String, Object> stats = alertMiningService.getAlertStatistics();

        return ResponseEntity.ok(stats);
    }
}