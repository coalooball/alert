package com.alert.system.repository;

import com.alert.system.entity.KafkaDataSourceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KafkaDataSourceConfigRepository extends JpaRepository<KafkaDataSourceConfig, Long> {
    List<KafkaDataSourceConfig> findAll();
    Optional<KafkaDataSourceConfig> findByAlertTypeId(Long alertTypeId);
}