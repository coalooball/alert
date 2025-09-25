package com.alert.system.controller;

import com.alert.system.dto.AlertConvergenceRuleRequest;
import com.alert.system.dto.AlertConvergenceRuleResponse;
import com.alert.system.dto.ApiResponse;
import com.alert.system.service.AlertConvergenceRuleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/alert/convergence-rules")
@PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
public class AlertConvergenceRuleController {

    @Autowired
    private AlertConvergenceRuleService convergenceRuleService;

    /**
     * Get convergence rules with pagination and search
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getRules(
            @RequestParam(required = false) String ruleName,
            @RequestParam(required = false) Integer alertType,
            @RequestParam(required = false) Boolean isEnabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        try {
            Sort sort = Sort.by(sortDir.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<AlertConvergenceRuleResponse> rulesPage;
            if (ruleName != null || alertType != null || isEnabled != null) {
                rulesPage = convergenceRuleService.searchRules(ruleName, alertType, isEnabled, pageable);
            } else {
                rulesPage = convergenceRuleService.getAllRules(pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("records", rulesPage.getContent());
            response.put("total", rulesPage.getTotalElements());
            response.put("totalPages", rulesPage.getTotalPages());
            response.put("currentPage", rulesPage.getNumber());
            response.put("pageSize", rulesPage.getSize());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch convergence rules: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get convergence rule by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AlertConvergenceRuleResponse>> getRuleById(@PathVariable UUID id) {
        try {
            AlertConvergenceRuleResponse rule = convergenceRuleService.getRuleById(id);
            return ResponseEntity.ok(ApiResponse.success(rule));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Rule not found: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Create a new convergence rule
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AlertConvergenceRuleResponse>> createRule(
            @Valid @RequestBody AlertConvergenceRuleRequest request) {
        try {
            AlertConvergenceRuleResponse createdRule = convergenceRuleService.createRule(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Convergence rule created successfully", createdRule));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to create rule: " + e.getMessage()));
        }
    }

    /**
     * Update an existing convergence rule
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AlertConvergenceRuleResponse>> updateRule(
            @PathVariable UUID id,
            @Valid @RequestBody AlertConvergenceRuleRequest request) {
        try {
            AlertConvergenceRuleResponse updatedRule = convergenceRuleService.updateRule(id, request);
            return ResponseEntity.ok(ApiResponse.success("Convergence rule updated successfully", updatedRule));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Rule not found: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to update rule: " + e.getMessage()));
        }
    }

    /**
     * Delete a convergence rule
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteRule(@PathVariable UUID id) {
        try {
            convergenceRuleService.deleteRule(id);
            return ResponseEntity.ok(ApiResponse.success("Rule deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Rule not found: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete rule: " + e.getMessage()));
        }
    }

    /**
     * Toggle rule enabled status
     */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AlertConvergenceRuleResponse>> toggleRule(@PathVariable UUID id) {
        try {
            AlertConvergenceRuleResponse toggledRule = convergenceRuleService.toggleRule(id);
            String message = toggledRule.getIsEnabled() ? "Rule enabled successfully" : "Rule disabled successfully";
            return ResponseEntity.ok(ApiResponse.success(message, toggledRule));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Rule not found: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to toggle rule: " + e.getMessage()));
        }
    }

    /**
     * Get active rules for alert processing
     */
    @GetMapping("/active")
    public ResponseEntity<List<AlertConvergenceRuleResponse>> getActiveRulesForAlert(
            @RequestParam Integer alertType,
            @RequestParam String alertSubtype) {
        try {
            List<AlertConvergenceRuleResponse> activeRules =
                convergenceRuleService.getActiveRulesForAlert(alertType, alertSubtype);
            return ResponseEntity.ok(activeRules);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get rule statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getRuleStats() {
        try {
            Map<String, Long> stats = new HashMap<>();
            stats.put("enabled", convergenceRuleService.getEnabledRuleCount());
            stats.put("disabled", convergenceRuleService.getDisabledRuleCount());
            stats.put("total", stats.get("enabled") + stats.get("disabled"));
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}