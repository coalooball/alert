package com.alert.system.repository;

import com.alert.system.entity.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertTypeRepository extends JpaRepository<AlertType, Integer> {

    List<AlertType> findByIsActiveTrueOrderByDisplayOrderAsc();

    AlertType findByTypeName(String typeName);

    List<AlertType> findAllByOrderByDisplayOrderAsc();

    List<AlertType> findByIsActiveOrderByDisplayOrderAsc(Boolean isActive);
}