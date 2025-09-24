package com.alert.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AlertFilterRuleRequest {

    @NotBlank(message = "Rule name is required")
    private String ruleName;

    private String ruleDescription;

    @NotNull(message = "Alert type is required")
    private Integer alertType; // 1:网络攻击 2:恶意样本 3:主机行为

    @NotBlank(message = "Alert subtype is required")
    private String alertSubtype;

    @NotBlank(message = "Match field is required")
    private String matchField;

    @NotBlank(message = "Match type is required")
    private String matchType; // exact, regex, contains

    @NotBlank(message = "Match value is required")
    private String matchValue;

    private Integer priority = 0;

    private Boolean isEnabled = true;
}