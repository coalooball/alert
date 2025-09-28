package com.alert.system.repository;

import com.alert.system.entity.RestHttpDataSourceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestHttpDataSourceConfigRepository extends JpaRepository<RestHttpDataSourceConfig, Long> {
    List<RestHttpDataSourceConfig> findAll();
    Optional<RestHttpDataSourceConfig> findByAlertTypeId(Long alertTypeId);
}