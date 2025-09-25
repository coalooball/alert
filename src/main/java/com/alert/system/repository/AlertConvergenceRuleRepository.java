package com.alert.system.repository;

import com.alert.system.entity.AlertConvergenceRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertConvergenceRuleRepository extends JpaRepository<AlertConvergenceRule, UUID> {

    /**
     * Find rules by alert type and enabled status
     */
    Page<AlertConvergenceRule> findByAlertTypeAndIsEnabled(Integer alertType, Boolean isEnabled, Pageable pageable);

    /**
     * Find rules by enabled status only
     */
    Page<AlertConvergenceRule> findByIsEnabled(Boolean isEnabled, Pageable pageable);

    /**
     * Find rules by alert type only
     */
    Page<AlertConvergenceRule> findByAlertType(Integer alertType, Pageable pageable);

    /**
     * Find rules by rule name containing (case insensitive)
     */
    Page<AlertConvergenceRule> findByRuleNameContainingIgnoreCase(String ruleName, Pageable pageable);

    /**
     * Complex search with multiple criteria
     */
    @Query("SELECT r FROM AlertConvergenceRule r WHERE " +
           "(:ruleName IS NULL OR LOWER(r.ruleName) LIKE LOWER(CONCAT('%', :ruleName, '%'))) AND " +
           "(:alertType IS NULL OR r.alertType = :alertType) AND " +
           "(:isEnabled IS NULL OR r.isEnabled = :isEnabled)")
    Page<AlertConvergenceRule> findBySearchCriteria(
        @Param("ruleName") String ruleName,
        @Param("alertType") Integer alertType,
        @Param("isEnabled") Boolean isEnabled,
        Pageable pageable
    );

    /**
     * Find active rules by alert type and subtype for processing
     */
    List<AlertConvergenceRule> findByAlertTypeAndAlertSubtypeAndIsEnabledOrderByPriorityDesc(
        Integer alertType, String alertSubtype, Boolean isEnabled
    );

    /**
     * Count enabled rules
     */
    long countByIsEnabled(Boolean isEnabled);
}