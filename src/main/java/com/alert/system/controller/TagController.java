package com.alert.system.controller;

import com.alert.system.dto.ApiResponse;
import com.alert.system.dto.TagRequest;
import com.alert.system.dto.TagResponse;
import com.alert.system.service.TagService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tags")
@PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
public class TagController {

    private static final Logger logger = LoggerFactory.getLogger(TagController.class);

    @Autowired
    private TagService tagService;

    /**
     * 分页查询标签列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getTags(
            @RequestParam(required = false) String tagName,
            @RequestParam(required = false) String tagType,
            @RequestParam(required = false) Boolean isEnabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.info("查询标签列表 - tagName: {}, tagType: {}, isEnabled: {}, page: {}, size: {}",
                tagName, tagType, isEnabled, page, size);

        try {
            Page<TagResponse> tagsPage = tagService.getTags(tagName, tagType, isEnabled, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("records", tagsPage.getContent());
            response.put("total", tagsPage.getTotalElements());
            response.put("page", tagsPage.getNumber());
            response.put("size", tagsPage.getSize());
            response.put("totalPages", tagsPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询标签列表失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "查询标签列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 获取所有启用的标签
     */
    @GetMapping("/enabled")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getEnabledTags() {
        logger.info("查询所有启用的标签");

        try {
            List<TagResponse> tags = tagService.getAllEnabledTags();
            return ResponseEntity.ok(ApiResponse.success("查询成功", tags));
        } catch (Exception e) {
            logger.error("查询启用标签失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 根据ID获取标签详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TagResponse>> getTagById(@PathVariable UUID id) {
        logger.info("查询标签详情 - ID: {}", id);

        try {
            return tagService.getTagById(id)
                    .map(tag -> ResponseEntity.ok(ApiResponse.success("查询成功", tag)))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("标签不存在")));
        } catch (Exception e) {
            logger.error("查询标签详情失败 - ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 根据标签类型获取标签
     */
    @GetMapping("/type/{tagType}")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getTagsByType(@PathVariable String tagType) {
        logger.info("根据类型查询标签 - tagType: {}", tagType);

        try {
            List<TagResponse> tags = tagService.getTagsByType(tagType);
            return ResponseEntity.ok(ApiResponse.success("查询成功", tags));
        } catch (Exception e) {
            logger.error("根据类型查询标签失败 - tagType: {}", tagType, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 创建新标签
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TagResponse>> createTag(@Valid @RequestBody TagRequest request) {
        logger.info("创建标签 - 请求: {}", request);

        try {
            TagResponse createdTag = tagService.createTag(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("标签创建成功", createdTag));
        } catch (IllegalArgumentException e) {
            logger.warn("创建标签失败 - 参数错误: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("创建标签失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("创建失败: " + e.getMessage()));
        }
    }

    /**
     * 更新标签
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TagResponse>> updateTag(@PathVariable UUID id,
                                                              @Valid @RequestBody TagRequest request) {
        logger.info("更新标签 - ID: {}, 请求: {}", id, request);

        try {
            TagResponse updatedTag = tagService.updateTag(id, request);
            return ResponseEntity.ok(ApiResponse.success("标签更新成功", updatedTag));
        } catch (IllegalArgumentException e) {
            logger.warn("更新标签失败 - ID: {}, 错误: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新标签失败 - ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新失败: " + e.getMessage()));
        }
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable UUID id) {
        logger.info("删除标签 - ID: {}", id);

        try {
            tagService.deleteTag(id);
            return ResponseEntity.ok(ApiResponse.success("标签删除成功", null));
        } catch (IllegalArgumentException e) {
            logger.warn("删除标签失败 - ID: {}, 错误: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("删除标签失败 - ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("删除失败: " + e.getMessage()));
        }
    }

    /**
     * 切换标签启用状态
     */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TagResponse>> toggleTagStatus(@PathVariable UUID id) {
        logger.info("切换标签状态 - ID: {}", id);

        try {
            TagResponse updatedTag = tagService.toggleTagStatus(id);
            String statusText = updatedTag.getIsEnabled() ? "启用" : "禁用";
            return ResponseEntity.ok(ApiResponse.success("标签已" + statusText, updatedTag));
        } catch (IllegalArgumentException e) {
            logger.warn("切换标签状态失败 - ID: {}, 错误: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("切换标签状态失败 - ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("切换状态失败: " + e.getMessage()));
        }
    }

    /**
     * 检查标签名称是否可用
     */
    @GetMapping("/check-name")
    public ResponseEntity<ApiResponse<Boolean>> checkTagNameAvailable(@RequestParam String tagName) {
        logger.info("检查标签名称是否可用 - tagName: {}", tagName);

        try {
            boolean exists = tagService.tagNameExists(tagName);
            return ResponseEntity.ok(ApiResponse.success("检查完成", !exists));
        } catch (Exception e) {
            logger.error("检查标签名称失败 - tagName: {}", tagName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("检查失败: " + e.getMessage()));
        }
    }
}