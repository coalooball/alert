package com.alert.system.service;

import com.alert.system.dto.AlertTaggingRuleRequest;
import com.alert.system.dto.AlertTaggingRuleResponse;
import com.alert.system.entity.AlertTaggingRule;
import com.alert.system.entity.User;
import com.alert.system.repository.AlertTaggingRuleRepository;
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
public class AlertTaggingRuleService {

    @Autowired
    private AlertTaggingRuleRepository taggingRuleRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get all tagging rules with pagination
     */
    public Page<AlertTaggingRuleResponse> getAllRules(Pageable pageable) {
        return taggingRuleRepository.findAll(pageable)
                .map(AlertTaggingRuleResponse::fromEntity);
    }

    /**
     * Search tagging rules with criteria
     */
    public Page<AlertTaggingRuleResponse> searchRules(String ruleName, Integer alertType, Boolean isEnabled, Pageable pageable) {
        return taggingRuleRepository.findBySearchCriteria(ruleName, alertType, isEnabled, pageable)
                .map(AlertTaggingRuleResponse::fromEntity);
    }

    /**
     * Get tagging rule by ID
     */
    public AlertTaggingRuleResponse getRuleById(UUID id) {
        AlertTaggingRule rule = taggingRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tagging rule not found with id: " + id));
        return AlertTaggingRuleResponse.fromEntity(rule);
    }

    /**
     * Create a new tagging rule
     */
    public AlertTaggingRuleResponse createRule(AlertTaggingRuleRequest request) {
        AlertTaggingRule rule = new AlertTaggingRule();
        mapRequestToEntity(request, rule);

        // Set created by current user
        User currentUser = getCurrentUser();
        rule.setCreatedBy(currentUser);

        AlertTaggingRule savedRule = taggingRuleRepository.save(rule);
        return AlertTaggingRuleResponse.fromEntity(savedRule);
    }

    /**
     * Update an existing tagging rule
     */
    public AlertTaggingRuleResponse updateRule(UUID id, AlertTaggingRuleRequest request) {
        AlertTaggingRule existingRule = taggingRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tagging rule not found with id: " + id));

        mapRequestToEntity(request, existingRule);

        AlertTaggingRule updatedRule = taggingRuleRepository.save(existingRule);
        return AlertTaggingRuleResponse.fromEntity(updatedRule);
    }

    /**
     * Delete a tagging rule
     */
    public void deleteRule(UUID id) {
        AlertTaggingRule rule = taggingRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tagging rule not found with id: " + id));
        taggingRuleRepository.delete(rule);
    }

    /**
     * Toggle rule enabled status
     */
    public AlertTaggingRuleResponse toggleRule(UUID id) {
        AlertTaggingRule rule = taggingRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tagging rule not found with id: " + id));

        rule.setIsEnabled(!rule.getIsEnabled());
        AlertTaggingRule updatedRule = taggingRuleRepository.save(rule);
        return AlertTaggingRuleResponse.fromEntity(updatedRule);
    }

    /**
     * Get active rules for alert processing
     */
    public List<AlertTaggingRuleResponse> getActiveRulesForAlert(Integer alertType, String alertSubtype) {
        List<AlertTaggingRule> rules = taggingRuleRepository
                .findActiveRulesForAlert(alertType, alertSubtype);
        return rules.stream()
                .map(AlertTaggingRuleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get rule statistics
     */
    public long getEnabledRuleCount() {
        return taggingRuleRepository.countByIsEnabled(true);
    }

    public long getDisabledRuleCount() {
        return taggingRuleRepository.countByIsEnabled(false);
    }

    /**
     * Helper method to map request to entity
     */
    private void mapRequestToEntity(AlertTaggingRuleRequest request, AlertTaggingRule entity) {
        entity.setRuleName(request.getRuleName());
        entity.setRuleDescription(request.getRuleDescription());
        entity.setAlertType(request.getAlertType());
        entity.setAlertSubtype(request.getAlertSubtype());
        entity.setMatchField(request.getMatchField());
        entity.setMatchType(request.getMatchType());
        entity.setMatchValue(request.getMatchValue());
        entity.setTags(request.getTags());
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