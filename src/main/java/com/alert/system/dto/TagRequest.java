package com.alert.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TagRequest {

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 100, message = "标签名称长度不能超过100个字符")
    private String tagName;

    @Size(max = 500, message = "标签描述长度不能超过500个字符")
    private String description;

    @NotBlank(message = "标签类型不能为空")
    @Pattern(regexp = "^(threat-level|attack-type|source-system|status|business|custom)$",
             message = "标签类型必须是: threat-level, attack-type, source-system, status, business, custom 之一")
    private String tagType;

    @NotBlank(message = "标签颜色不能为空")
    @Pattern(regexp = "^#[0-9a-fA-F]{3,8}$", message = "标签颜色必须是有效的十六进制颜色值，格式如：#FF0000 或 #FFF")
    private String color;

    private Boolean isEnabled = true;

    // Constructors
    public TagRequest() {}

    public TagRequest(String tagName, String description, String tagType, String color, Boolean isEnabled) {
        this.tagName = tagName;
        this.description = description;
        this.tagType = tagType;
        this.color = color;
        this.isEnabled = isEnabled;
    }

    // Getters and Setters
    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTagType() {
        return tagType;
    }

    public void setTagType(String tagType) {
        this.tagType = tagType;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    @Override
    public String toString() {
        return "TagRequest{" +
                "tagName='" + tagName + '\'' +
                ", description='" + description + '\'' +
                ", tagType='" + tagType + '\'' +
                ", color='" + color + '\'' +
                ", isEnabled=" + isEnabled +
                '}';
    }
}