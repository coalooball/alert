package com.alert.system.repository;

import com.alert.system.entity.AlertObservableMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertObservableMappingRepository extends JpaRepository<AlertObservableMapping, UUID> {

    @Query("SELECT m FROM AlertObservableMapping m WHERE m.alert.id = :alertId")
    List<AlertObservableMapping> findByAlertId(@Param("alertId") UUID alertId);

    @Query("SELECT m FROM AlertObservableMapping m WHERE m.observable.id = :observableId")
    List<AlertObservableMapping> findByObservableId(@Param("observableId") UUID observableId);

    @Query("SELECT m FROM AlertObservableMapping m WHERE m.event.id = :eventId")
    List<AlertObservableMapping> findByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT m FROM AlertObservableMapping m WHERE m.alert.id = :alertId AND m.observable.id = :observableId")
    List<AlertObservableMapping> findByAlertIdAndObservableId(@Param("alertId") UUID alertId,
                                                              @Param("observableId") UUID observableId);

    @Query("SELECT m FROM AlertObservableMapping m WHERE m.role = :role")
    List<AlertObservableMapping> findByRole(@Param("role") String role);

    @Query("SELECT COUNT(DISTINCT m.observable.id) FROM AlertObservableMapping m WHERE m.alert.id = :alertId")
    Long countObservablesByAlertId(@Param("alertId") UUID alertId);

    @Query("SELECT COUNT(DISTINCT m.alert.id) FROM AlertObservableMapping m WHERE m.observable.id = :observableId")
    Long countAlertsByObservableId(@Param("observableId") UUID observableId);

}