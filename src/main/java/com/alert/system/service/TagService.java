package com.alert.system.service;

import com.alert.system.dto.TagRequest;
import com.alert.system.dto.TagResponse;
import com.alert.system.entity.Tag;
import com.alert.system.entity.User;
import com.alert.system.repository.TagRepository;
import com.alert.system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class TagService {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 分页查询标签列表（支持搜索）
     */
    public Page<TagResponse> getTags(String tagName, String tagType, Boolean isEnabled, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Tag> tags = tagRepository.findTagsWithFilters(tagName, tagType, isEnabled, pageable);

        return tags.map(TagResponse::new);
    }

    /**
     * 获取所有启用的标签
     */
    public List<TagResponse> getAllEnabledTags() {
        List<Tag> tags = tagRepository.findByIsEnabledTrueOrderByTagName();
        return tags.stream()
                   .map(TagResponse::new)
                   .collect(Collectors.toList());
    }

    /**
     * 根据ID获取标签
     */
    public Optional<TagResponse> getTagById(UUID id) {
        return tagRepository.findById(id)
                           .map(TagResponse::new);
    }

    /**
     * 根据标签名称获取标签
     */
    public Optional<TagResponse> getTagByName(String tagName) {
        return tagRepository.findByTagName(tagName)
                           .map(TagResponse::new);
    }

    /**
     * 根据标签类型获取标签
     */
    public List<TagResponse> getTagsByType(String tagType) {
        List<Tag> tags = tagRepository.findByTagType(tagType);
        return tags.stream()
                   .map(TagResponse::new)
                   .collect(Collectors.toList());
    }

    /**
     * 创建新标签
     */
    public TagResponse createTag(TagRequest request) {
        // 检查标签名称是否已存在
        if (tagRepository.findByTagName(request.getTagName()).isPresent()) {
            throw new IllegalArgumentException("标签名称 '" + request.getTagName() + "' 已存在");
        }

        // 获取当前用户
        User currentUser = getCurrentUser();

        // 创建新标签
        Tag tag = new Tag();
        tag.setTagName(request.getTagName());
        tag.setDescription(request.getDescription());
        tag.setTagType(request.getTagType());
        tag.setColor(request.getColor());
        tag.setIsEnabled(request.getIsEnabled());
        tag.setCreatedBy(currentUser);

        Tag savedTag = tagRepository.save(tag);
        return new TagResponse(savedTag);
    }

    /**
     * 更新标签
     */
    public TagResponse updateTag(UUID id, TagRequest request) {
        Tag existingTag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("标签不存在，ID: " + id));

        // 检查标签名称是否已被其他标签使用
        if (!existingTag.getTagName().equals(request.getTagName()) &&
            tagRepository.existsByTagNameAndIdNot(request.getTagName(), id)) {
            throw new IllegalArgumentException("标签名称 '" + request.getTagName() + "' 已存在");
        }

        // 更新标签信息
        existingTag.setTagName(request.getTagName());
        existingTag.setDescription(request.getDescription());
        existingTag.setTagType(request.getTagType());
        existingTag.setColor(request.getColor());
        existingTag.setIsEnabled(request.getIsEnabled());

        Tag updatedTag = tagRepository.save(existingTag);
        return new TagResponse(updatedTag);
    }

    /**
     * 删除标签
     */
    public void deleteTag(UUID id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("标签不存在，ID: " + id));

        // TODO: 这里可以添加检查逻辑，确保标签没有被使用
        // 例如：检查是否有告警关联了这个标签

        tagRepository.delete(tag);
    }

    /**
     * 切换标签启用状态
     */
    public TagResponse toggleTagStatus(UUID id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("标签不存在，ID: " + id));

        tag.setIsEnabled(!tag.getIsEnabled());
        Tag updatedTag = tagRepository.save(tag);

        return new TagResponse(updatedTag);
    }

    /**
     * 获取当前登录用户
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("用户未登录");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("用户不存在: " + username));
    }

    /**
     * 检查标签是否存在
     */
    public boolean tagExists(UUID id) {
        return tagRepository.existsById(id);
    }

    /**
     * 检查标签名称是否存在
     */
    public boolean tagNameExists(String tagName) {
        return tagRepository.findByTagName(tagName).isPresent();
    }
}