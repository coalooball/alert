package com.alert.system.repository;

import com.alert.system.entity.AlertStorageMapping;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertStorageMappingRepository extends JpaRepository<AlertStorageMapping, Long> {

    @Override
    @EntityGraph(attributePaths = {"alertType", "storageConfig", "createdBy", "updatedBy"})
    List<AlertStorageMapping> findAll();

    @EntityGraph(attributePaths = {"alertType", "storageConfig", "createdBy", "updatedBy"})
    Optional<AlertStorageMapping> findByAlertTypeId(Integer alertTypeId);

    @EntityGraph(attributePaths = {"alertType", "storageConfig", "createdBy", "updatedBy"})
    List<AlertStorageMapping> findByStorageConfigId(Long storageConfigId);

    @Override
    @EntityGraph(attributePaths = {"alertType", "storageConfig", "createdBy", "updatedBy"})
    Optional<AlertStorageMapping> findById(Long id);

    boolean existsByAlertTypeIdAndIdNot(Integer alertTypeId, Long id);
}