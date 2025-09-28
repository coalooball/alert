package com.alert.system.controller;

import com.alert.system.entity.AlertType;
import com.alert.system.entity.AlertSubtype;
import com.alert.system.entity.AlertField;
import com.alert.system.repository.AlertTypeRepository;
import com.alert.system.repository.AlertSubtypeRepository;
import com.alert.system.repository.AlertFieldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/alert-metadata")
@CrossOrigin
public class AlertMetadataController {

    @Autowired
    private AlertTypeRepository alertTypeRepository;

    @Autowired
    private AlertSubtypeRepository alertSubtypeRepository;

    @Autowired
    private AlertFieldRepository alertFieldRepository;

    @GetMapping("/types")
    public ResponseEntity<List<AlertType>> getAllAlertTypes() {
        return ResponseEntity.ok(alertTypeRepository.findByIsActiveTrueOrderByDisplayOrderAsc());
    }

    @GetMapping("/types/{typeId}/subtypes")
    public ResponseEntity<List<AlertSubtype>> getAlertSubtypes(@PathVariable Integer typeId) {
        return ResponseEntity.ok(alertSubtypeRepository.findByAlertTypeIdAndIsActiveTrueOrderByDisplayOrderAsc(typeId));
    }

    @GetMapping("/types/{typeId}/fields")
    public ResponseEntity<List<AlertField>> getAlertFields(@PathVariable Integer typeId) {
        return ResponseEntity.ok(alertFieldRepository.findByAlertTypeIdAndIsActiveTrueOrderByDisplayOrderAsc(typeId));
    }

    @GetMapping("/fields")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getAllFields() {
        Map<String, Object> response = new HashMap<>();

        List<AlertType> types = alertTypeRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        Map<Integer, List<AlertField>> fieldsByType = new HashMap<>();

        for (AlertType type : types) {
            fieldsByType.put(type.getId(),
                alertFieldRepository.findByAlertTypeIdAndIsActiveTrueOrderByDisplayOrderAsc(type.getId()));
        }

        response.put("success", true);
        response.put("data", fieldsByType);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getAllMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        List<AlertType> types = alertTypeRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        metadata.put("types", types);

        Map<Integer, List<AlertSubtype>> subtypesByType = new HashMap<>();
        Map<Integer, List<AlertField>> fieldsByType = new HashMap<>();

        for (AlertType type : types) {
            subtypesByType.put(type.getId(),
                alertSubtypeRepository.findByAlertTypeIdAndIsActiveTrueOrderByDisplayOrderAsc(type.getId()));
            fieldsByType.put(type.getId(),
                alertFieldRepository.findByAlertTypeIdAndIsActiveTrueOrderByDisplayOrderAsc(type.getId()));
        }

        metadata.put("subtypes", subtypesByType);
        metadata.put("fields", fieldsByType);

        return ResponseEntity.ok(metadata);
    }
}