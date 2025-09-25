package com.alert.system.dto;

import com.alert.system.entity.Tag;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public class TagResponse {

    private UUID id;
    private String tagName;
    private String description;
    private String tagType;
    private String color;
    private Boolean isEnabled;
    private String createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Constructors
    public TagResponse() {}

    public TagResponse(Tag tag) {
        this.id = tag.getId();
        this.tagName = tag.getTagName();
        this.description = tag.getDescription();
        this.tagType = tag.getTagType();
        this.color = tag.getColor();
        this.isEnabled = tag.getIsEnabled();
        this.createdBy = tag.getCreatedBy() != null ? tag.getCreatedBy().getUsername() : null;
        this.createdAt = tag.getCreatedAt();
        this.updatedAt = tag.getUpdatedAt();
    }

    public TagResponse(UUID id, String tagName, String description, String tagType,
                       String color, Boolean isEnabled, String createdBy,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.tagName = tagName;
        this.description = description;
        this.tagType = tagType;
        this.color = color;
        this.isEnabled = isEnabled;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "TagResponse{" +
                "id=" + id +
                ", tagName='" + tagName + '\'' +
                ", description='" + description + '\'' +
                ", tagType='" + tagType + '\'' +
                ", color='" + color + '\'' +
                ", isEnabled=" + isEnabled +
                ", createdBy='" + createdBy + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}