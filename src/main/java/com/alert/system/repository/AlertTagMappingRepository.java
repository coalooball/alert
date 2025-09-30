package com.alert.system.repository;

import com.alert.system.entity.AlertTagMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertTagMappingRepository extends JpaRepository<AlertTagMapping, UUID> {

    @Query("SELECT m FROM AlertTagMapping m WHERE m.alert.id = :alertId")
    List<AlertTagMapping> findByAlertId(@Param("alertId") UUID alertId);

    @Query("SELECT m FROM AlertTagMapping m WHERE m.tag.id = :tagId")
    List<AlertTagMapping> findByTagId(@Param("tagId") UUID tagId);

    @Query("SELECT m FROM AlertTagMapping m WHERE m.alert.id = :alertId AND m.tag.id = :tagId")
    List<AlertTagMapping> findByAlertIdAndTagId(@Param("alertId") UUID alertId,
                                                @Param("tagId") UUID tagId);

    @Query("SELECT m FROM AlertTagMapping m WHERE m.taggingRule.id = :ruleId")
    List<AlertTagMapping> findByTaggingRuleId(@Param("ruleId") UUID ruleId);

    @Query("SELECT COUNT(m) FROM AlertTagMapping m WHERE m.tag.id = :tagId")
    Long countByTagId(@Param("tagId") UUID tagId);

    @Query("SELECT COUNT(DISTINCT m.alert.id) FROM AlertTagMapping m WHERE m.isAutoTagged = true")
    Long countAutoTaggedAlerts();
}