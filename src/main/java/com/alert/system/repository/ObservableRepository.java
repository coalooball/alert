package com.alert.system.repository;

import com.alert.system.entity.Observable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ObservableRepository extends JpaRepository<Observable, UUID> {

    @Query("SELECT o FROM Observable o WHERE o.observableType = :type AND o.observableValue = :value")
    Optional<Observable> findByTypeAndValue(@Param("type") String type,
                                           @Param("value") String value);

    List<Observable> findByObservableType(String observableType);

    List<Observable> findByIsMalicious(Boolean isMalicious);

    @Query("SELECT o FROM Observable o WHERE o.riskScore >= :minScore")
    List<Observable> findByRiskScoreGreaterThanEqual(@Param("minScore") Double minScore);

    @Query("SELECT o FROM Observable o WHERE o.lastSeen BETWEEN :startTime AND :endTime")
    List<Observable> findByLastSeenBetween(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    @Query("SELECT o FROM Observable o WHERE o.status = :status ORDER BY o.lastSeen DESC")
    List<Observable> findByStatusOrderByLastSeenDesc(@Param("status") String status);

    @Query("UPDATE Observable o SET o.occurrenceCount = o.occurrenceCount + 1, o.lastSeen = :lastSeen WHERE o.id = :id")
    void incrementOccurrence(@Param("id") UUID id, @Param("lastSeen") LocalDateTime lastSeen);

    @Query("SELECT COUNT(o) FROM Observable o WHERE o.observableType = :type")
    Long countByType(@Param("type") String type);

    @Query("SELECT o FROM Observable o WHERE o.category = :category")
    List<Observable> findByCategory(@Param("category") String category);
}