package com.alert.system.repository;

import com.alert.system.entity.KafkaDataSourceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface KafkaDataSourceConfigRepository extends JpaRepository<KafkaDataSourceConfig, Long> {
    List<KafkaDataSourceConfig> findAll();
    Optional<KafkaDataSourceConfig> findByAlertTypeId(Long alertTypeId);

    List<KafkaDataSourceConfig> findByIsEnabled(Boolean isEnabled);

    @Modifying
    @Transactional
    @Query("UPDATE KafkaDataSourceConfig k SET k.connectionStatus = :status WHERE k.id = :id")
    void updateConnectionStatus(@Param("id") Long id, @Param("status") String status);

    Optional<KafkaDataSourceConfig> findByConfigName(String configName);

    @Query("SELECT k FROM KafkaDataSourceConfig k WHERE k.isEnabled = true AND k.connectionStatus != 'connected'")
    List<KafkaDataSourceConfig> findDisconnectedEnabledConfigs();
}