package com.alert.system.repository;

import com.alert.system.entity.AlertField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertFieldRepository extends JpaRepository<AlertField, Integer> {

    List<AlertField> findByAlertTypeIdAndIsActiveTrueOrderByDisplayOrderAsc(Integer alertTypeId);

    AlertField findByAlertTypeIdAndFieldName(Integer alertTypeId, String fieldName);
}