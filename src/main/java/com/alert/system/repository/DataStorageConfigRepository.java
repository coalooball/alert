package com.alert.system.repository;

import com.alert.system.entity.DataStorageConfig;
import com.alert.system.enums.DatabaseType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataStorageConfigRepository extends JpaRepository<DataStorageConfig, Long> {

    @Override
    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    List<DataStorageConfig> findAll();

    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    Optional<DataStorageConfig> findByName(String name);

    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    List<DataStorageConfig> findByIsActiveTrue();

    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    Optional<DataStorageConfig> findByIsDefaultTrue();

    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    List<DataStorageConfig> findByDbType(DatabaseType dbType);

    @Modifying
    @Query("UPDATE DataStorageConfig d SET d.isDefault = false WHERE d.id != :id")
    void clearDefaultExcept(@Param("id") Long id);

    boolean existsByNameAndIdNot(String name, Long id);

    @Override
    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    Optional<DataStorageConfig> findById(Long id);

    List<DataStorageConfig> findByIsDefault(Boolean isDefault);
}