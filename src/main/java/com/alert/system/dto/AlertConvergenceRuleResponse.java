package com.alert.system.dto;

import com.alert.system.entity.AlertConvergenceRule;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class AlertConvergenceRuleResponse {

    private UUID id;
    private String ruleName;
    private String ruleDescription;
    private Integer alertType;
    private String alertSubtype;
    private String convergenceType;

    // 计算引擎收敛字段
    private List<String> engineFields;

    // 机器学习收敛字段
    private List<String> mlFields;

    // 机器学习相关配置
    private Boolean useMlModel;
    private String mlModelName;
    private Map<String, Object> mlModelConfig;

    private Map<String, Object> convergenceConfig;
    private Integer timeWindow;
    private Integer minCount;
    private Integer priority;
    private Boolean isEnabled;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AlertConvergenceRuleResponse fromEntity(AlertConvergenceRule entity) {
        return AlertConvergenceRuleResponse.builder()
                .id(entity.getId())
                .ruleName(entity.getRuleName())
                .ruleDescription(entity.getRuleDescription())
                .alertType(entity.getAlertType())
                .alertSubtype(entity.getAlertSubtype())
                .convergenceType(entity.getConvergenceType())
                .engineFields(entity.getEngineFields())
                .mlFields(entity.getMlFields())
                .useMlModel(entity.getUseMlModel())
                .mlModelName(entity.getMlModelName())
                .mlModelConfig(entity.getMlModelConfig())
                .convergenceConfig(entity.getConvergenceConfig())
                .timeWindow(entity.getTimeWindow())
                .minCount(entity.getMinCount())
                .priority(entity.getPriority())
                .isEnabled(entity.getIsEnabled())
                .createdBy(entity.getCreatedBy() != null ? entity.getCreatedBy().getUsername() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}