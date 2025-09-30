package com.alert.system.repository;

import com.alert.system.entity.EventCorrelationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventCorrelationRuleRepository extends JpaRepository<EventCorrelationRule, UUID> {

    Optional<EventCorrelationRule> findByRuleName(String ruleName);

    @Query("SELECT r FROM EventCorrelationRule r WHERE r.isEnabled = :isEnabled ORDER BY r.priority DESC")
    List<EventCorrelationRule> findByIsEnabledOrderByPriority(@Param("isEnabled") Boolean isEnabled);

    @Query("SELECT r FROM EventCorrelationRule r WHERE r.alertType.id = :alertTypeId AND r.isEnabled = true ORDER BY r.priority DESC")
    List<EventCorrelationRule> findByAlertTypeAndIsEnabled(@Param("alertTypeId") Integer alertTypeId);

    @Query("SELECT r FROM EventCorrelationRule r WHERE r.correlationLevel = :level AND r.isEnabled = true ORDER BY r.priority DESC")
    List<EventCorrelationRule> findByCorrelationLevelAndIsEnabled(@Param("level") String correlationLevel);

    List<EventCorrelationRule> findByIsCrossTypeAndIsEnabled(Boolean isCrossType, Boolean isEnabled);

    @Query("SELECT COUNT(r) FROM EventCorrelationRule r WHERE r.isEnabled = true")
    Long countEnabledRules();

    List<EventCorrelationRule> findByIsEnabled(Boolean isEnabled);

    List<EventCorrelationRule> findByCorrelationLevel(String correlationLevel);

    List<EventCorrelationRule> findByRuleType(String ruleType);
}