package com.alert.system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AlertConvergenceRuleRequest {

    @NotBlank(message = "Rule name is required")
    private String ruleName;

    private String ruleDescription;

    @NotNull(message = "Alert type is required")
    private Integer alertType; // 1:网络攻击 2:恶意样本 3:主机行为

    @NotBlank(message = "Alert subtype is required")
    private String alertSubtype;

    @NotBlank(message = "Convergence type is required")
    private String convergenceType; // field_match, ml_algorithm

    // 计算引擎收敛字段
    private List<String> engineFields;

    // 机器学习收敛字段
    private List<String> mlFields;

    // 机器学习相关配置
    private Boolean useMlModel = false;

    private String mlModelName;

    private Map<String, Object> mlModelConfig;

    private Map<String, Object> convergenceConfig;

    @Min(value = 1, message = "Time window must be at least 1 second")
    private Integer timeWindow = 3600;

    @Min(value = 2, message = "Min count must be at least 2")
    private Integer minCount = 2;

    private Integer priority = 0;

    private Boolean isEnabled = true;
}