package com.alert.system.controller;

import com.alert.system.dto.AlertFilterRuleRequest;
import com.alert.system.dto.AlertFilterRuleResponse;
import com.alert.system.service.AlertFilterRuleService;
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
@RequestMapping("/api/alert/filter-rules")
@PreAuthorize("hasRole('ADMIN')")
public class AlertFilterRuleController {

    @Autowired
    private AlertFilterRuleService filterRuleService;

    @GetMapping
    public ResponseEntity<List<AlertFilterRuleResponse>> getAllRules() {
        return ResponseEntity.ok(filterRuleService.getAllRules());
    }

    @GetMapping("/active")
    public ResponseEntity<List<AlertFilterRuleResponse>> getActiveRules() {
        return ResponseEntity.ok(filterRuleService.getActiveRules());
    }

    @GetMapping("/type/{alertType}")
    public ResponseEntity<List<AlertFilterRuleResponse>> getRulesByType(
            @PathVariable Integer alertType,
            @RequestParam(required = false) String alertSubtype) {
        return ResponseEntity.ok(filterRuleService.getRulesByType(alertType, alertSubtype));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertFilterRuleResponse> getRuleById(@PathVariable UUID id) {
        return ResponseEntity.ok(filterRuleService.getRuleById(id));
    }

    @PostMapping
    public ResponseEntity<?> createRule(@Valid @RequestBody AlertFilterRuleRequest request) {
        try {
            AlertFilterRuleResponse response = filterRuleService.createRule(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRule(
            @PathVariable UUID id,
            @Valid @RequestBody AlertFilterRuleRequest request) {
        try {
            AlertFilterRuleResponse response = filterRuleService.updateRule(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRule(@PathVariable UUID id) {
        try {
            filterRuleService.deleteRule(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Filter rule deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<AlertFilterRuleResponse>> searchRules(@RequestParam String keyword) {
        return ResponseEntity.ok(filterRuleService.searchRules(keyword));
    }
}