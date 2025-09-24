package com.alert.system.repository;

import com.alert.system.entity.FlinkConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlinkConfigRepository extends JpaRepository<FlinkConfig, UUID> {

    Optional<FlinkConfig> findByName(String name);

    boolean existsByName(String name);

    Optional<FlinkConfig> findByIsDefaultTrue();

    @Modifying
    @Query("UPDATE FlinkConfig f SET f.isDefault = false WHERE f.isDefault = true AND f.id != :id")
    void clearOtherDefaults(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE FlinkConfig f SET f.isActive = false WHERE f.id != :id")
    void deactivateOthers(@Param("id") UUID id);
}