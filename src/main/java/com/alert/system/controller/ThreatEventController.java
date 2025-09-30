package com.alert.system.controller;

import com.alert.system.dto.ApiResponse;
import com.alert.system.entity.Event;
import com.alert.system.service.ThreatEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/threat-events")
@RequiredArgsConstructor
public class ThreatEventController {

    private final ThreatEventService threatEventService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Event> events = threatEventService.getFilteredEvents(
                type, severity, status, startTime, endTime, pageable
            );

            Map<String, Object> response = new HashMap<>();
            response.put("events", events.getContent());
            response.put("currentPage", events.getNumber());
            response.put("totalItems", events.getTotalElements());
            response.put("totalPages", events.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch events: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getEventById(@PathVariable UUID id) {
        try {
            Optional<Event> event = threatEventService.getEventById(id);
            if (event.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(event.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Event not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch event: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createEvent(@RequestBody Event event) {
        try {
            Event createdEvent = threatEventService.createEvent(event);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdEvent));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to create event: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateEvent(
            @PathVariable UUID id,
            @RequestBody Event event
    ) {
        try {
            Event updatedEvent = threatEventService.updateEvent(id, event);
            if (updatedEvent != null) {
                return ResponseEntity.ok(ApiResponse.success(updatedEvent));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Event not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to update event: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteEvent(@PathVariable UUID id) {
        try {
            boolean deleted = threatEventService.deleteEvent(id);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("Event deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Event not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to delete event: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<?>> getStatistics() {
        try {
            Map<String, Object> stats = threatEventService.getStatistics();
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch statistics: " + e.getMessage()));
        }
    }

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<?>> getEventTypes() {
        try {
            List<String> types = Arrays.asList(
                "APT攻击", "DDoS攻击", "Web攻击", "恶意软件",
                "数据泄露", "钓鱼攻击", "内部威胁", "其他"
            );
            return ResponseEntity.ok(ApiResponse.success(types));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch event types: " + e.getMessage()));
        }
    }

    @GetMapping("/severities")
    public ResponseEntity<ApiResponse<?>> getSeverities() {
        try {
            List<String> severities = Arrays.asList("CRITICAL", "HIGH", "MEDIUM", "LOW");
            return ResponseEntity.ok(ApiResponse.success(severities));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch severities: " + e.getMessage()));
        }
    }

    @GetMapping("/statuses")
    public ResponseEntity<ApiResponse<?>> getStatuses() {
        try {
            List<String> statuses = Arrays.asList("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED");
            return ResponseEntity.ok(ApiResponse.success(statuses));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch statuses: " + e.getMessage()));
        }
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<ApiResponse<?>> batchDeleteEvents(@RequestBody List<UUID> ids) {
        try {
            int deletedCount = threatEventService.batchDeleteEvents(ids);
            Map<String, Object> result = new HashMap<>();
            result.put("deletedCount", deletedCount);
            result.put("message", deletedCount + " events deleted successfully");
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to batch delete events: " + e.getMessage()));
        }
    }

    @PostMapping("/batch-update-status")
    public ResponseEntity<ApiResponse<?>> batchUpdateStatus(
            @RequestBody Map<String, Object> request
    ) {
        try {
            @SuppressWarnings("unchecked")
            List<String> idStrings = (List<String>) request.get("ids");
            String status = (String) request.get("status");

            List<UUID> ids = new ArrayList<>();
            for (String idStr : idStrings) {
                ids.add(UUID.fromString(idStr));
            }

            int updatedCount = threatEventService.batchUpdateStatus(ids, status);
            Map<String, Object> result = new HashMap<>();
            result.put("updatedCount", updatedCount);
            result.put("message", updatedCount + " events updated successfully");
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to batch update status: " + e.getMessage()));
        }
    }
}