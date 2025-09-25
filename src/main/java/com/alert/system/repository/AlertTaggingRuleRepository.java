package com.alert.system.repository;

import com.alert.system.entity.AlertTaggingRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertTaggingRuleRepository extends JpaRepository<AlertTaggingRule, UUID> {

    /**
     * Find rules by alert type and enabled status
     */
    Page<AlertTaggingRule> findByAlertTypeAndIsEnabled(Integer alertType, Boolean isEnabled, Pageable pageable);

    /**
     * Find rules by enabled status only
     */
    Page<AlertTaggingRule> findByIsEnabled(Boolean isEnabled, Pageable pageable);


    /**
     * Find rules by rule name containing (case insensitive)
     */
    Page<AlertTaggingRule> findByRuleNameContainingIgnoreCase(String ruleName, Pageable pageable);

    /**
     * Complex search with multiple criteria
     */
    @Query("SELECT r FROM AlertTaggingRule r WHERE " +
           "(:ruleName IS NULL OR LOWER(r.ruleName) LIKE LOWER(CONCAT('%', :ruleName, '%'))) AND " +
           "(:alertType IS NULL OR r.alertType = :alertType) AND " +
           "(:isEnabled IS NULL OR r.isEnabled = :isEnabled)")
    Page<AlertTaggingRule> findBySearchCriteria(
        @Param("ruleName") String ruleName,
        @Param("alertType") Integer alertType,
        @Param("isEnabled") Boolean isEnabled,
        Pageable pageable
    );

    /**
     * Find active rules for alert processing (ordered by priority)
     */
    @Query("SELECT r FROM AlertTaggingRule r WHERE " +
           "r.isEnabled = true AND " +
           "r.alertType = :alertType AND " +
           "(:alertSubtype IS NULL OR r.alertSubtype = :alertSubtype) " +
           "ORDER BY r.priority DESC, r.createdAt ASC")
    List<AlertTaggingRule> findActiveRulesForAlert(
        @Param("alertType") Integer alertType,
        @Param("alertSubtype") String alertSubtype
    );

    /**
     * Count enabled rules
     */
    long countByIsEnabled(Boolean isEnabled);
}