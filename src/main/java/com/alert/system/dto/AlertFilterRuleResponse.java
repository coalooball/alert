package com.alert.system.dto;

import com.alert.system.entity.AlertFilterRule;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AlertFilterRuleResponse {

    private UUID id;
    private String ruleName;
    private String ruleDescription;
    private Integer alertType;
    private String alertSubtype;
    private String matchField;
    private String matchType;
    private String matchValue;
    private Integer priority;
    private Boolean isEnabled;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AlertFilterRuleResponse fromEntity(AlertFilterRule entity) {
        return AlertFilterRuleResponse.builder()
                .id(entity.getId())
                .ruleName(entity.getRuleName())
                .ruleDescription(entity.getRuleDescription())
                .alertType(entity.getAlertType())
                .alertSubtype(entity.getAlertSubtype())
                .matchField(entity.getMatchField())
                .matchType(entity.getMatchType())
                .matchValue(entity.getMatchValue())
                .priority(entity.getPriority())
                .isEnabled(entity.getIsEnabled())
                .createdBy(entity.getCreatedBy() != null ? entity.getCreatedBy().getUsername() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}