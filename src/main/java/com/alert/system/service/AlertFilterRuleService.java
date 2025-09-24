package com.alert.system.service;

import com.alert.system.dto.AlertFilterRuleRequest;
import com.alert.system.dto.AlertFilterRuleResponse;
import com.alert.system.entity.AlertFilterRule;
import com.alert.system.entity.User;
import com.alert.system.repository.AlertFilterRuleRepository;
import com.alert.system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AlertFilterRuleService {

    @Autowired
    private AlertFilterRuleRepository filterRuleRepository;

    @Autowired
    private UserRepository userRepository;

    public List<AlertFilterRuleResponse> getAllRules() {
        return filterRuleRepository.findAll().stream()
                .map(AlertFilterRuleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AlertFilterRuleResponse> getActiveRules() {
        return filterRuleRepository.findByIsEnabledTrueOrderByPriorityDesc().stream()
                .map(AlertFilterRuleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AlertFilterRuleResponse> getRulesByType(Integer alertType, String alertSubtype) {
        List<AlertFilterRule> rules;
        if (alertSubtype != null) {
            rules = filterRuleRepository.findActiveRulesByTypeAndSubtype(alertType, alertSubtype);
        } else {
            rules = filterRuleRepository.findByAlertType(alertType);
        }
        return rules.stream()
                .map(AlertFilterRuleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public AlertFilterRuleResponse getRuleById(UUID id) {
        AlertFilterRule rule = filterRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Filter rule not found"));
        return AlertFilterRuleResponse.fromEntity(rule);
    }

    public AlertFilterRuleResponse createRule(AlertFilterRuleRequest request) {
        // Check if rule name already exists
        if (filterRuleRepository.existsByRuleName(request.getRuleName())) {
            throw new RuntimeException("Rule with name '" + request.getRuleName() + "' already exists");
        }

        // Validate match type
        if (!isValidMatchType(request.getMatchType())) {
            throw new RuntimeException("Invalid match type. Must be one of: exact, regex");
        }

        AlertFilterRule rule = new AlertFilterRule();
        rule.setRuleName(request.getRuleName());
        rule.setRuleDescription(request.getRuleDescription());
        rule.setAlertType(request.getAlertType());
        rule.setAlertSubtype(request.getAlertSubtype());
        rule.setMatchField(request.getMatchField());
        rule.setMatchType(request.getMatchType());
        rule.setMatchValue(request.getMatchValue());
        rule.setPriority(request.getPriority());
        rule.setIsEnabled(request.getIsEnabled());

        // Set created by user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            userRepository.findByUsername(username).ifPresent(rule::setCreatedBy);
        }

        AlertFilterRule saved = filterRuleRepository.save(rule);
        return AlertFilterRuleResponse.fromEntity(saved);
    }

    public AlertFilterRuleResponse updateRule(UUID id, AlertFilterRuleRequest request) {
        AlertFilterRule rule = filterRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Filter rule not found"));

        // Check if rule name is being changed and if new name already exists
        if (!rule.getRuleName().equals(request.getRuleName()) &&
            filterRuleRepository.existsByRuleName(request.getRuleName())) {
            throw new RuntimeException("Rule with name '" + request.getRuleName() + "' already exists");
        }

        // Validate match type
        if (!isValidMatchType(request.getMatchType())) {
            throw new RuntimeException("Invalid match type. Must be one of: exact, regex");
        }

        rule.setRuleName(request.getRuleName());
        rule.setRuleDescription(request.getRuleDescription());
        rule.setAlertType(request.getAlertType());
        rule.setAlertSubtype(request.getAlertSubtype());
        rule.setMatchField(request.getMatchField());
        rule.setMatchType(request.getMatchType());
        rule.setMatchValue(request.getMatchValue());
        rule.setPriority(request.getPriority());
        rule.setIsEnabled(request.getIsEnabled());

        AlertFilterRule saved = filterRuleRepository.save(rule);
        return AlertFilterRuleResponse.fromEntity(saved);
    }

    public void deleteRule(UUID id) {
        if (!filterRuleRepository.existsById(id)) {
            throw new RuntimeException("Filter rule not found");
        }
        filterRuleRepository.deleteById(id);
    }

    public List<AlertFilterRuleResponse> searchRules(String keyword) {
        return filterRuleRepository.searchByKeyword(keyword).stream()
                .map(AlertFilterRuleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private boolean isValidMatchType(String matchType) {
        return "exact".equals(matchType) || "regex".equals(matchType);
    }
}