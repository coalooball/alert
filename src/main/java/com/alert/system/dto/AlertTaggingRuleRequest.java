package com.alert.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class AlertTaggingRuleRequest {

    @NotBlank(message = "Rule name is required")
    private String ruleName;

    private String ruleDescription;

    @NotNull(message = "Alert type is required")
    private Integer alertType;

    @NotBlank(message = "Alert subtype is required")
    private String alertSubtype;

    @NotBlank(message = "Match field is required")
    private String matchField;

    @NotBlank(message = "Match type is required")
    @Pattern(regexp = "^(exact|regex)$", message = "Match type must be 'exact' or 'regex'")
    private String matchType;

    @NotBlank(message = "Match value is required")
    private String matchValue;

    @NotEmpty(message = "Tags are required")
    private List<String> tags;

    private Integer priority = 0;

    private Boolean isEnabled = true;
}