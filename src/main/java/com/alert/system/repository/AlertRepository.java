package com.alert.system.repository;

import com.alert.system.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Optional<Alert> findByAlertUuid(String alertUuid);

    List<Alert> findByEventId(UUID eventId);

    List<Alert> findByIsFilteredFalse();

    @Query("SELECT a FROM Alert a WHERE a.alertType.id = :typeId AND a.alertSubtype = :subtype " +
           "AND a.alertTime BETWEEN :startTime AND :endTime AND a.isFiltered = false")
    List<Alert> findByAlertTypeAndSubtypeInTimeWindow(@Param("typeId") Integer typeId,
                                                      @Param("subtype") String subtype,
                                                      @Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);

    @Query("SELECT a FROM Alert a WHERE a.alertType.id = :typeId " +
           "AND a.alertTime BETWEEN :startTime AND :endTime AND a.isFiltered = false")
    List<Alert> findByAlertTypeInTimeWindow(@Param("typeId") Integer typeId,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    @Query("SELECT a FROM Alert a WHERE a.alertTime BETWEEN :startTime AND :endTime AND a.isFiltered = false")
    List<Alert> findAlertsInTimeWindow(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    @Query("SELECT a FROM Alert a WHERE a.correlationKey = :correlationKey")
    List<Alert> findByCorrelationKey(@Param("correlationKey") String correlationKey);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.alertTime >= :startTime")
    Long countAlertsAfter(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.isFiltered = true AND a.alertTime >= :startTime")
    Long countFilteredAlertsAfter(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT a FROM Alert a WHERE a.status = :status ORDER BY a.alertTime DESC")
    List<Alert> findByStatus(@Param("status") String status);

    @Query("SELECT DISTINCT a FROM Alert a LEFT JOIN FETCH a.alertType ORDER BY a.alertTime DESC")
    List<Alert> findAllWithAlertType();

    @Query("SELECT a FROM Alert a WHERE a.alertType.id = :typeId ORDER BY a.alertTime DESC")
    List<Alert> findByAlertTypeId(@Param("typeId") Integer typeId);
}