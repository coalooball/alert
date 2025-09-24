package com.alert.system.controller;

import com.alert.system.dto.ApiResponse;
import com.alert.system.entity.AlertType;
import com.alert.system.entity.AlertSubtype;
import com.alert.system.entity.AlertField;
import com.alert.system.service.AlertTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alert-types")
@RequiredArgsConstructor
public class AlertTypeController {

    private final AlertTypeService alertTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AlertType>>> getAllAlertTypes() {
        List<AlertType> alertTypes = alertTypeService.getAllAlertTypes();
        return ResponseEntity.ok(ApiResponse.success(alertTypes));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<AlertType>>> getActiveAlertTypes() {
        List<AlertType> alertTypes = alertTypeService.getActiveAlertTypes();
        return ResponseEntity.ok(ApiResponse.success(alertTypes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AlertType>> getAlertTypeById(@PathVariable Integer id) {
        return alertTypeService.getAlertTypeById(id)
                .map(alertType -> ResponseEntity.ok(ApiResponse.success(alertType)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AlertType>> createAlertType(@RequestBody AlertType alertType) {
        try {
            AlertType createdAlertType = alertTypeService.createAlertType(alertType);
            return ResponseEntity.ok(ApiResponse.success(createdAlertType));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AlertType>> updateAlertType(@PathVariable Integer id, @RequestBody AlertType alertType) {
        try {
            AlertType updatedAlertType = alertTypeService.updateAlertType(id, alertType);
            return ResponseEntity.ok(ApiResponse.success(updatedAlertType));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAlertType(@PathVariable Integer id) {
        try {
            alertTypeService.deleteAlertType(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{typeId}/subtypes")
    public ResponseEntity<ApiResponse<List<AlertSubtype>>> getAlertSubtypesByTypeId(@PathVariable Integer typeId) {
        List<AlertSubtype> subtypes = alertTypeService.getAlertSubtypesByTypeId(typeId);
        return ResponseEntity.ok(ApiResponse.success(subtypes));
    }

    @GetMapping("/subtypes/{id}")
    public ResponseEntity<ApiResponse<AlertSubtype>> getAlertSubtypeById(@PathVariable Integer id) {
        return alertTypeService.getAlertSubtypeById(id)
                .map(subtype -> ResponseEntity.ok(ApiResponse.success(subtype)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/subtypes")
    public ResponseEntity<ApiResponse<AlertSubtype>> createAlertSubtype(@RequestBody AlertSubtype alertSubtype) {
        try {
            AlertSubtype createdSubtype = alertTypeService.createAlertSubtype(alertSubtype);
            return ResponseEntity.ok(ApiResponse.success(createdSubtype));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/subtypes/{id}")
    public ResponseEntity<ApiResponse<AlertSubtype>> updateAlertSubtype(@PathVariable Integer id, @RequestBody AlertSubtype alertSubtype) {
        try {
            AlertSubtype updatedSubtype = alertTypeService.updateAlertSubtype(id, alertSubtype);
            return ResponseEntity.ok(ApiResponse.success(updatedSubtype));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/subtypes/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAlertSubtype(@PathVariable Integer id) {
        try {
            alertTypeService.deleteAlertSubtype(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{typeId}/fields")
    public ResponseEntity<ApiResponse<List<AlertField>>> getAlertFieldsByTypeId(@PathVariable Integer typeId) {
        List<AlertField> fields = alertTypeService.getAlertFieldsByTypeId(typeId);
        return ResponseEntity.ok(ApiResponse.success(fields));
    }

    @GetMapping("/fields/{id}")
    public ResponseEntity<ApiResponse<AlertField>> getAlertFieldById(@PathVariable Integer id) {
        return alertTypeService.getAlertFieldById(id)
                .map(field -> ResponseEntity.ok(ApiResponse.success(field)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/fields")
    public ResponseEntity<ApiResponse<AlertField>> createAlertField(@RequestBody AlertField alertField) {
        try {
            AlertField createdField = alertTypeService.createAlertField(alertField);
            return ResponseEntity.ok(ApiResponse.success(createdField));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/fields/{id}")
    public ResponseEntity<ApiResponse<AlertField>> updateAlertField(@PathVariable Integer id, @RequestBody AlertField alertField) {
        try {
            AlertField updatedField = alertTypeService.updateAlertField(id, alertField);
            return ResponseEntity.ok(ApiResponse.success(updatedField));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/fields/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAlertField(@PathVariable Integer id) {
        try {
            alertTypeService.deleteAlertField(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}