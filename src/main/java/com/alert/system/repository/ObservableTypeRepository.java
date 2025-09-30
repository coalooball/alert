package com.alert.system.repository;

import com.alert.system.entity.ObservableType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ObservableTypeRepository extends JpaRepository<ObservableType, UUID> {

    Optional<ObservableType> findByTypeName(String typeName);

    List<ObservableType> findByIsEnabled(Boolean isEnabled);

    List<ObservableType> findByCategory(String category);

    @Query("SELECT t FROM ObservableType t WHERE t.isEnabled = true ORDER BY t.priority DESC")
    List<ObservableType> findEnabledTypesOrderByPriority();

    List<ObservableType> findByIsSystemType(Boolean isSystemType);

    List<ObservableType> findByIsEnabledAndCategory(Boolean isEnabled, String category);

    @Query("SELECT DISTINCT o.category FROM ObservableType o WHERE o.category IS NOT NULL")
    List<String> findDistinctCategories();
}