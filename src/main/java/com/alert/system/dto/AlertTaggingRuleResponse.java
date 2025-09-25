package com.alert.system.dto;

import com.alert.system.entity.AlertTaggingRule;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AlertTaggingRuleResponse {

    private UUID id;
    private String ruleName;
    private String ruleDescription;
    private Integer alertType;
    private String alertSubtype;
    private String matchField;
    private String matchType;
    private String matchValue;
    private List<String> tags;
    private Integer priority;
    private Boolean isEnabled;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AlertTaggingRuleResponse fromEntity(AlertTaggingRule entity) {
        return AlertTaggingRuleResponse.builder()
                .id(entity.getId())
                .ruleName(entity.getRuleName())
                .ruleDescription(entity.getRuleDescription())
                .alertType(entity.getAlertType())
                .alertSubtype(entity.getAlertSubtype())
                .matchField(entity.getMatchField())
                .matchType(entity.getMatchType())
                .matchValue(entity.getMatchValue())
                .tags(entity.getTags())
                .priority(entity.getPriority())
                .isEnabled(entity.getIsEnabled())
                .createdBy(entity.getCreatedBy() != null ? entity.getCreatedBy().getUsername() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}