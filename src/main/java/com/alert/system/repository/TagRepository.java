package com.alert.system.repository;

import com.alert.system.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

    /**
     * 根据标签名称查找标签
     * @param tagName 标签名称
     * @return 标签
     */
    Optional<Tag> findByTagName(String tagName);

    /**
     * 根据标签类型查找标签
     * @param tagType 标签类型
     * @return 标签列表
     */
    List<Tag> findByTagType(String tagType);

    /**
     * 根据启用状态查找标签
     * @param isEnabled 启用状态
     * @return 标签列表
     */
    List<Tag> findByIsEnabled(Boolean isEnabled);

    /**
     * 查询启用的标签
     * @return 启用的标签列表
     */
    List<Tag> findByIsEnabledTrueOrderByTagName();

    /**
     * 根据标签类型和启用状态查找标签
     * @param tagType 标签类型
     * @param isEnabled 启用状态
     * @return 标签列表
     */
    List<Tag> findByTagTypeAndIsEnabled(String tagType, Boolean isEnabled);

    /**
     * 分页查询标签（支持多条件搜索）
     * @param tagName 标签名称（模糊搜索）
     * @param tagType 标签类型
     * @param isEnabled 启用状态
     * @param pageable 分页参数
     * @return 分页标签列表
     */
    @Query("SELECT t FROM Tag t WHERE " +
           "(:tagName IS NULL OR t.tagName LIKE %:tagName%) AND " +
           "(:tagType IS NULL OR t.tagType = :tagType) AND " +
           "(:isEnabled IS NULL OR t.isEnabled = :isEnabled)")
    Page<Tag> findTagsWithFilters(@Param("tagName") String tagName,
                                  @Param("tagType") String tagType,
                                  @Param("isEnabled") Boolean isEnabled,
                                  Pageable pageable);

    /**
     * 检查标签名称是否已存在（排除指定ID）
     * @param tagName 标签名称
     * @param id 排除的标签ID
     * @return 是否存在
     */
    @Query("SELECT COUNT(t) > 0 FROM Tag t WHERE t.tagName = :tagName AND t.id != :id")
    boolean existsByTagNameAndIdNot(@Param("tagName") String tagName, @Param("id") UUID id);

    /**
     * Find tag by name (alias for findByTagName)
     */
    default Optional<Tag> findByName(String name) {
        return findByTagName(name);
    }
}