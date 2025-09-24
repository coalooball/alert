package com.alert.system.repository;

import com.alert.system.entity.AlertSubtype;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertSubtypeRepository extends JpaRepository<AlertSubtype, Integer> {

    List<AlertSubtype> findByAlertTypeIdAndIsActiveTrueOrderByDisplayOrderAsc(Integer alertTypeId);

    AlertSubtype findByAlertTypeIdAndSubtypeCode(Integer alertTypeId, String subtypeCode);
}