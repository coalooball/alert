package com.alert.system.repository;

import com.alert.system.entity.AlertFilterRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertFilterRuleRepository extends JpaRepository<AlertFilterRule, UUID> {

    List<AlertFilterRule> findByAlertType(Integer alertType);

    List<AlertFilterRule> findByAlertTypeAndAlertSubtype(Integer alertType, String alertSubtype);

    List<AlertFilterRule> findByIsEnabledTrue();

    List<AlertFilterRule> findByIsEnabledTrueOrderByPriorityDesc();

    @Query("SELECT r FROM AlertFilterRule r WHERE r.alertType = :alertType AND " +
           "(r.alertSubtype IS NULL OR r.alertSubtype = :alertSubtype) AND " +
           "r.isEnabled = true ORDER BY r.priority DESC")
    List<AlertFilterRule> findActiveRulesByTypeAndSubtype(
        @Param("alertType") Integer alertType,
        @Param("alertSubtype") String alertSubtype
    );

    @Query("SELECT r FROM AlertFilterRule r WHERE r.ruleName LIKE %:keyword% OR " +
           "r.ruleDescription LIKE %:keyword%")
    List<AlertFilterRule> searchByKeyword(@Param("keyword") String keyword);

    boolean existsByRuleName(String ruleName);
}