package com.alert.system.controller;

import com.alert.system.dto.AlertTaggingRuleRequest;
import com.alert.system.dto.AlertTaggingRuleResponse;
import com.alert.system.dto.ApiResponse;
import com.alert.system.service.AlertTaggingRuleService;
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
@RequestMapping("/api/alert/tagging-rules")
@PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
public class AlertTaggingRuleController {

    @Autowired
    private AlertTaggingRuleService taggingRuleService;

    /**
     * Get tagging rules with pagination and search
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

            Page<AlertTaggingRuleResponse> rulesPage;
            if (ruleName != null || alertType != null || isEnabled != null) {
                rulesPage = taggingRuleService.searchRules(ruleName, alertType, isEnabled, pageable);
            } else {
                rulesPage = taggingRuleService.getAllRules(pageable);
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
            errorResponse.put("error", "Failed to fetch tagging rules: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get tagging rule by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AlertTaggingRuleResponse>> getRuleById(@PathVariable UUID id) {
        try {
            AlertTaggingRuleResponse rule = taggingRuleService.getRuleById(id);
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
     * Create a new tagging rule
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AlertTaggingRuleResponse>> createRule(
            @Valid @RequestBody AlertTaggingRuleRequest request) {
        try {
            AlertTaggingRuleResponse createdRule = taggingRuleService.createRule(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Tagging rule created successfully", createdRule));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to create rule: " + e.getMessage()));
        }
    }

    /**
     * Update an existing tagging rule
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AlertTaggingRuleResponse>> updateRule(
            @PathVariable UUID id,
            @Valid @RequestBody AlertTaggingRuleRequest request) {
        try {
            AlertTaggingRuleResponse updatedRule = taggingRuleService.updateRule(id, request);
            return ResponseEntity.ok(ApiResponse.success("Tagging rule updated successfully", updatedRule));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Rule not found: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to update rule: " + e.getMessage()));
        }
    }

    /**
     * Delete a tagging rule
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteRule(@PathVariable UUID id) {
        try {
            taggingRuleService.deleteRule(id);
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
    public ResponseEntity<ApiResponse<AlertTaggingRuleResponse>> toggleRule(@PathVariable UUID id) {
        try {
            AlertTaggingRuleResponse toggledRule = taggingRuleService.toggleRule(id);
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
    public ResponseEntity<List<AlertTaggingRuleResponse>> getActiveRulesForAlert(
            @RequestParam Integer alertType,
            @RequestParam(required = false) String alertSubtype) {
        try {
            List<AlertTaggingRuleResponse> activeRules =
                taggingRuleService.getActiveRulesForAlert(alertType, alertSubtype);
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
            stats.put("enabled", taggingRuleService.getEnabledRuleCount());
            stats.put("disabled", taggingRuleService.getDisabledRuleCount());
            stats.put("total", stats.get("enabled") + stats.get("disabled"));
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}