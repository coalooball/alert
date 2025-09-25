package com.alert.system.service;

import com.alert.system.dto.AlertConvergenceRuleRequest;
import com.alert.system.dto.AlertConvergenceRuleResponse;
import com.alert.system.entity.AlertConvergenceRule;
import com.alert.system.entity.User;
import com.alert.system.repository.AlertConvergenceRuleRepository;
import com.alert.system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AlertConvergenceRuleService {

    @Autowired
    private AlertConvergenceRuleRepository convergenceRuleRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get all convergence rules with pagination
     */
    public Page<AlertConvergenceRuleResponse> getAllRules(Pageable pageable) {
        return convergenceRuleRepository.findAll(pageable)
                .map(AlertConvergenceRuleResponse::fromEntity);
    }

    /**
     * Search convergence rules with criteria
     */
    public Page<AlertConvergenceRuleResponse> searchRules(String ruleName, Integer alertType, Boolean isEnabled, Pageable pageable) {
        return convergenceRuleRepository.findBySearchCriteria(ruleName, alertType, isEnabled, pageable)
                .map(AlertConvergenceRuleResponse::fromEntity);
    }

    /**
     * Get convergence rule by ID
     */
    public AlertConvergenceRuleResponse getRuleById(UUID id) {
        AlertConvergenceRule rule = convergenceRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Convergence rule not found with id: " + id));
        return AlertConvergenceRuleResponse.fromEntity(rule);
    }

    /**
     * Create a new convergence rule
     */
    public AlertConvergenceRuleResponse createRule(AlertConvergenceRuleRequest request) {
        AlertConvergenceRule rule = new AlertConvergenceRule();
        mapRequestToEntity(request, rule);

        // Set created by current user
        User currentUser = getCurrentUser();
        rule.setCreatedBy(currentUser);

        AlertConvergenceRule savedRule = convergenceRuleRepository.save(rule);
        return AlertConvergenceRuleResponse.fromEntity(savedRule);
    }

    /**
     * Update an existing convergence rule
     */
    public AlertConvergenceRuleResponse updateRule(UUID id, AlertConvergenceRuleRequest request) {
        AlertConvergenceRule existingRule = convergenceRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Convergence rule not found with id: " + id));

        mapRequestToEntity(request, existingRule);

        AlertConvergenceRule updatedRule = convergenceRuleRepository.save(existingRule);
        return AlertConvergenceRuleResponse.fromEntity(updatedRule);
    }

    /**
     * Delete a convergence rule
     */
    public void deleteRule(UUID id) {
        AlertConvergenceRule rule = convergenceRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Convergence rule not found with id: " + id));
        convergenceRuleRepository.delete(rule);
    }

    /**
     * Toggle rule enabled status
     */
    public AlertConvergenceRuleResponse toggleRule(UUID id) {
        AlertConvergenceRule rule = convergenceRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Convergence rule not found with id: " + id));

        rule.setIsEnabled(!rule.getIsEnabled());
        AlertConvergenceRule updatedRule = convergenceRuleRepository.save(rule);
        return AlertConvergenceRuleResponse.fromEntity(updatedRule);
    }

    /**
     * Get active rules for alert processing
     */
    public List<AlertConvergenceRuleResponse> getActiveRulesForAlert(Integer alertType, String alertSubtype) {
        List<AlertConvergenceRule> rules = convergenceRuleRepository
                .findByAlertTypeAndAlertSubtypeAndIsEnabledOrderByPriorityDesc(alertType, alertSubtype, true);
        return rules.stream()
                .map(AlertConvergenceRuleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get rule statistics
     */
    public long getEnabledRuleCount() {
        return convergenceRuleRepository.countByIsEnabled(true);
    }

    public long getDisabledRuleCount() {
        return convergenceRuleRepository.countByIsEnabled(false);
    }

    /**
     * Helper method to map request to entity
     */
    private void mapRequestToEntity(AlertConvergenceRuleRequest request, AlertConvergenceRule entity) {
        entity.setRuleName(request.getRuleName());
        entity.setRuleDescription(request.getRuleDescription());
        entity.setAlertType(request.getAlertType());
        entity.setAlertSubtype(request.getAlertSubtype());
        entity.setConvergenceType(request.getConvergenceType());
        entity.setConvergenceFields(request.getConvergenceFields());
        entity.setConvergenceConfig(request.getConvergenceConfig());
        entity.setTimeWindow(request.getTimeWindow());
        entity.setMinCount(request.getMinCount());
        entity.setPriority(request.getPriority());
        entity.setIsEnabled(request.getIsEnabled());
    }

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + username));
    }
}