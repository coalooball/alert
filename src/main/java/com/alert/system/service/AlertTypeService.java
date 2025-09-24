package com.alert.system.service;

import com.alert.system.entity.AlertType;
import com.alert.system.entity.AlertSubtype;
import com.alert.system.entity.AlertField;
import com.alert.system.repository.AlertTypeRepository;
import com.alert.system.repository.AlertSubtypeRepository;
import com.alert.system.repository.AlertFieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AlertTypeService {

    private final AlertTypeRepository alertTypeRepository;
    private final AlertSubtypeRepository alertSubtypeRepository;
    private final AlertFieldRepository alertFieldRepository;

    public List<AlertType> getAllAlertTypes() {
        return alertTypeRepository.findAllByOrderByDisplayOrderAsc();
    }

    public List<AlertType> getActiveAlertTypes() {
        return alertTypeRepository.findByIsActiveOrderByDisplayOrderAsc(true);
    }

    public Optional<AlertType> getAlertTypeById(Integer id) {
        return alertTypeRepository.findById(id);
    }

    @Transactional
    public AlertType createAlertType(AlertType alertType) {
        return alertTypeRepository.save(alertType);
    }

    @Transactional
    public AlertType updateAlertType(Integer id, AlertType alertType) {
        AlertType existingType = alertTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AlertType not found with id: " + id));

        existingType.setTypeName(alertType.getTypeName());
        existingType.setTypeLabel(alertType.getTypeLabel());
        existingType.setDescription(alertType.getDescription());
        existingType.setIsActive(alertType.getIsActive());
        if (alertType.getDisplayOrder() != null) {
            existingType.setDisplayOrder(alertType.getDisplayOrder());
        }

        return alertTypeRepository.save(existingType);
    }

    @Transactional
    public void deleteAlertType(Integer id) {
        alertTypeRepository.deleteById(id);
    }

    public List<AlertSubtype> getAlertSubtypesByTypeId(Integer typeId) {
        return alertSubtypeRepository.findByAlertTypeIdAndIsActiveTrueOrderByDisplayOrderAsc(typeId);
    }

    public Optional<AlertSubtype> getAlertSubtypeById(Integer id) {
        return alertSubtypeRepository.findById(id);
    }

    @Transactional
    public AlertSubtype createAlertSubtype(AlertSubtype alertSubtype) {
        AlertType alertType = alertTypeRepository.findById(alertSubtype.getAlertTypeId())
                .orElseThrow(() -> new RuntimeException("AlertType not found with id: " + alertSubtype.getAlertTypeId()));
        alertSubtype.setAlertType(alertType);
        return alertSubtypeRepository.save(alertSubtype);
    }

    @Transactional
    public AlertSubtype updateAlertSubtype(Integer id, AlertSubtype alertSubtype) {
        AlertSubtype existingSubtype = alertSubtypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AlertSubtype not found with id: " + id));

        existingSubtype.setSubtypeCode(alertSubtype.getSubtypeCode());
        existingSubtype.setSubtypeLabel(alertSubtype.getSubtypeLabel());
        existingSubtype.setDescription(alertSubtype.getDescription());
        existingSubtype.setIsActive(alertSubtype.getIsActive());
        if (alertSubtype.getDisplayOrder() != null) {
            existingSubtype.setDisplayOrder(alertSubtype.getDisplayOrder());
        }

        if (alertSubtype.getAlertTypeId() != null && !alertSubtype.getAlertTypeId().equals(existingSubtype.getAlertTypeId())) {
            AlertType alertType = alertTypeRepository.findById(alertSubtype.getAlertTypeId())
                    .orElseThrow(() -> new RuntimeException("AlertType not found with id: " + alertSubtype.getAlertTypeId()));
            existingSubtype.setAlertType(alertType);
        }

        return alertSubtypeRepository.save(existingSubtype);
    }

    @Transactional
    public void deleteAlertSubtype(Integer id) {
        alertSubtypeRepository.deleteById(id);
    }

    public List<AlertField> getAlertFieldsByTypeId(Integer typeId) {
        return alertFieldRepository.findByAlertTypeIdAndIsActiveTrueOrderByDisplayOrderAsc(typeId);
    }

    public Optional<AlertField> getAlertFieldById(Integer id) {
        return alertFieldRepository.findById(id);
    }

    @Transactional
    public AlertField createAlertField(AlertField alertField) {
        AlertType alertType = alertTypeRepository.findById(alertField.getAlertTypeId())
                .orElseThrow(() -> new RuntimeException("AlertType not found with id: " + alertField.getAlertTypeId()));
        alertField.setAlertType(alertType);
        return alertFieldRepository.save(alertField);
    }

    @Transactional
    public AlertField updateAlertField(Integer id, AlertField alertField) {
        AlertField existingField = alertFieldRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AlertField not found with id: " + id));

        existingField.setFieldName(alertField.getFieldName());
        existingField.setFieldLabel(alertField.getFieldLabel());
        existingField.setFieldType(alertField.getFieldType());
        existingField.setDescription(alertField.getDescription());
        existingField.setIsActive(alertField.getIsActive());
        if (alertField.getDisplayOrder() != null) {
            existingField.setDisplayOrder(alertField.getDisplayOrder());
        }

        if (alertField.getAlertTypeId() != null && !alertField.getAlertTypeId().equals(existingField.getAlertTypeId())) {
            AlertType alertType = alertTypeRepository.findById(alertField.getAlertTypeId())
                    .orElseThrow(() -> new RuntimeException("AlertType not found with id: " + alertField.getAlertTypeId()));
            existingField.setAlertType(alertType);
        }

        return alertFieldRepository.save(existingField);
    }

    @Transactional
    public void deleteAlertField(Integer id) {
        alertFieldRepository.deleteById(id);
    }
}