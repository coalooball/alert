package com.alert.system.service;

import com.alert.system.entity.*;
import com.alert.system.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertCorrelationService {

    private final AlertRepository alertRepository;
    private final EventRepository eventRepository;
    private final EventCorrelationRuleRepository correlationRuleRepository;
    private final AlertObservableMappingRepository observableMappingRepository;
    private final FlinkComputeService flinkComputeService;
    private final ObjectMapper objectMapper;

    private final Map<String, List<Alert>> correlationWindows = new ConcurrentHashMap<>();

    @Async
    @Transactional
    public void correlateAlert(Alert alert) {
        try {
            List<EventCorrelationRule> rules = getApplicableRules(alert);

            for (EventCorrelationRule rule : rules) {
                processCorrelationRule(alert, rule);
            }

        } catch (Exception e) {
            log.error("Error correlating alert: {}", alert.getAlertUuid(), e);
        }
    }

    private List<EventCorrelationRule> getApplicableRules(Alert alert) {
        List<EventCorrelationRule> allRules = correlationRuleRepository.findByIsEnabledOrderByPriority(true);

        return allRules.stream()
            .filter(rule -> isRuleApplicable(alert, rule))
            .collect(Collectors.toList());
    }

    private boolean isRuleApplicable(Alert alert, EventCorrelationRule rule) {
        if (rule.getAlertType() != null &&
            !rule.getAlertType().getId().equals(alert.getAlertType().getId())) {
            return false;
        }

        if (rule.getAlertSubtype() != null &&
            !rule.getAlertSubtype().equals(alert.getAlertSubtype())) {
            return false;
        }

        return true;
    }

    private void processCorrelationRule(Alert alert, EventCorrelationRule rule) {
        try {
            String correlationLevel = rule.getCorrelationLevel();

            if ("SUBTYPE".equalsIgnoreCase(correlationLevel)) {
                correlateWithinSubtype(alert, rule);
            } else if ("CROSS_TYPE".equalsIgnoreCase(correlationLevel)) {
                correlateCrossType(alert, rule);
            } else {
                correlateWithinType(alert, rule);
            }

        } catch (Exception e) {
            log.error("Error processing correlation rule: {}", rule.getRuleName(), e);
        }
    }

    private void correlateWithinSubtype(Alert alert, EventCorrelationRule rule) {
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(rule.getTimeWindowSeconds());

        List<Alert> relatedAlerts = alertRepository.findByAlertTypeAndSubtypeInTimeWindow(
            alert.getAlertType().getId(),
            alert.getAlertSubtype(),
            startTime,
            LocalDateTime.now()
        );

        relatedAlerts = filterByCorrelationFields(relatedAlerts, alert, rule);

        if (relatedAlerts.size() >= rule.getMinAlertCount()) {
            createOrUpdateEvent(relatedAlerts, rule, "SUBTYPE");
        }
    }

    private void correlateWithinType(Alert alert, EventCorrelationRule rule) {
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(rule.getTimeWindowSeconds());

        List<Alert> relatedAlerts = alertRepository.findByAlertTypeInTimeWindow(
            alert.getAlertType().getId(),
            startTime,
            LocalDateTime.now()
        );

        relatedAlerts = filterByCorrelationFields(relatedAlerts, alert, rule);

        if (relatedAlerts.size() >= rule.getMinAlertCount()) {
            createOrUpdateEvent(relatedAlerts, rule, "TYPE");
        }
    }

    private void correlateCrossType(Alert alert, EventCorrelationRule rule) {
        if (flinkComputeService.isFlinkAvailable()) {
            flinkComputeService.correlateCrossTypeAlerts(alert, rule);
            return;
        }

        LocalDateTime startTime = LocalDateTime.now().minusSeconds(rule.getTimeWindowSeconds());

        List<Alert> relatedAlerts = alertRepository.findAlertsInTimeWindow(
            startTime,
            LocalDateTime.now()
        );

        relatedAlerts = filterByCorrelationFields(relatedAlerts, alert, rule);
        relatedAlerts = filterByObservables(relatedAlerts, alert);

        if (relatedAlerts.size() >= rule.getMinAlertCount()) {
            createOrUpdateEvent(relatedAlerts, rule, "CROSS_TYPE");
        }
    }

    private List<Alert> filterByCorrelationFields(List<Alert> alerts, Alert targetAlert, EventCorrelationRule rule) {
        if (rule.getCorrelationFields() == null || rule.getCorrelationFields().length == 0) {
            return alerts;
        }

        return alerts.stream()
            .filter(a -> !a.getIsFiltered())
            .filter(a -> matchesCorrelationFields(a, targetAlert, rule.getCorrelationFields()))
            .collect(Collectors.toList());
    }

    private boolean matchesCorrelationFields(Alert alert1, Alert alert2, String[] fields) {
        for (String field : fields) {
            String value1 = getFieldValue(alert1, field);
            String value2 = getFieldValue(alert2, field);

            if (value1 == null || value2 == null) {
                continue;
            }

            if (value1.equals(value2)) {
                return true;
            }
        }
        return false;
    }

    private List<Alert> filterByObservables(List<Alert> alerts, Alert targetAlert) {
        Set<UUID> targetObservables = observableMappingRepository
            .findByAlertId(targetAlert.getId())
            .stream()
            .map(m -> m.getObservable().getId())
            .collect(Collectors.toSet());

        if (targetObservables.isEmpty()) {
            return alerts;
        }

        return alerts.stream()
            .filter(alert -> {
                Set<UUID> alertObservables = observableMappingRepository
                    .findByAlertId(alert.getId())
                    .stream()
                    .map(m -> m.getObservable().getId())
                    .collect(Collectors.toSet());

                alertObservables.retainAll(targetObservables);
                return !alertObservables.isEmpty();
            })
            .collect(Collectors.toList());
    }

    @Transactional
    private void createOrUpdateEvent(List<Alert> alerts, EventCorrelationRule rule, String correlationType) {
        try {
            String correlationKey = generateCorrelationKey(alerts, rule);

            Event existingEvent = eventRepository.findByCorrelationKey(correlationKey).orElse(null);

            if (existingEvent != null) {
                updateEvent(existingEvent, alerts);
            } else {
                Event newEvent = createEvent(alerts, rule, correlationType);
                newEvent.setCorrelationRuleId(rule.getId());
                eventRepository.save(newEvent);

                for (Alert alert : alerts) {
                    alert.setEvent(newEvent);
                    alert.setStatus("CORRELATED");
                    alert.setCorrelationKey(correlationKey);
                    alertRepository.save(alert);
                }

                log.info("Created new event: {} with {} alerts", newEvent.getSystemCode(), alerts.size());
            }

            rule.setExecutionCount(rule.getExecutionCount() + 1);
            rule.setSuccessCount(rule.getSuccessCount() + 1);
            rule.setLastExecutionTime(LocalDateTime.now());
            correlationRuleRepository.save(rule);

        } catch (Exception e) {
            log.error("Error creating/updating event", e);
        }
    }

    private Event createEvent(List<Alert> alerts, EventCorrelationRule rule, String correlationType) {
        Event event = new Event();

        Alert firstAlert = alerts.stream()
            .min(Comparator.comparing(Alert::getAlertTime))
            .orElse(alerts.get(0));

        Alert lastAlert = alerts.stream()
            .max(Comparator.comparing(Alert::getAlertTime))
            .orElse(alerts.get(alerts.size() - 1));

        event.setName(generateEventName(alerts, rule));
        event.setDescription(generateEventDescription(alerts, rule));
        event.setType(determineEventType(alerts));

        event.setStartTime(firstAlert.getAlertTime());
        event.setEndTime(lastAlert.getAlertTime());
        event.setFoundTime(LocalDateTime.now());
        event.setFirstFoundTime(firstAlert.getAlertTime());

        aggregateEventFields(event, alerts);

        event.setAlertCount(alerts.size());
        event.setStatus("OPEN");
        event.setIsAutoGenerated(true);

        calculateScores(event, alerts);

        ObjectNode mergeAlerts = objectMapper.createObjectNode();
        ArrayNode alertArray = mergeAlerts.putArray("alerts");

        for (Alert alert : alerts) {
            ObjectNode alertNode = objectMapper.createObjectNode();
            alertNode.put("alert_id", alert.getAlertUuid());
            alertNode.put("alert_type", alert.getAlertType().getTypeName());
            alertNode.put("alert_subtype", alert.getAlertSubtype());
            alertNode.put("alert_time", alert.getAlertTime().toString());
            alertArray.add(alertNode);
        }

        event.setMergeAlerts(mergeAlerts.toString());

        return event;
    }

    private void updateEvent(Event event, List<Alert> newAlerts) {
        for (Alert alert : newAlerts) {
            if (alert.getEvent() == null) {
                alert.setEvent(event);
                alert.setStatus("CORRELATED");
                alertRepository.save(alert);

                event.setAlertCount(event.getAlertCount() + 1);
            }
        }

        Alert lastAlert = newAlerts.stream()
            .max(Comparator.comparing(Alert::getAlertTime))
            .orElse(null);

        if (lastAlert != null && lastAlert.getAlertTime().isAfter(event.getEndTime())) {
            event.setEndTime(lastAlert.getAlertTime());
        }

        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
    }

    private String generateCorrelationKey(List<Alert> alerts, EventCorrelationRule rule) {
        if (rule.getGroupingFields() != null && rule.getGroupingFields().length > 0) {
            StringBuilder key = new StringBuilder(rule.getId().toString());

            for (String field : rule.getGroupingFields()) {
                Set<String> values = alerts.stream()
                    .map(a -> getFieldValue(a, field))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

                key.append("_").append(String.join(",", values));
            }

            return key.toString();
        }

        return rule.getId().toString() + "_" + System.currentTimeMillis();
    }

    private String generateEventName(List<Alert> alerts, EventCorrelationRule rule) {
        if (rule.getEventTemplate() != null) {
            try {
                JsonNode template = objectMapper.readTree(rule.getEventTemplate());
                if (template.has("name")) {
                    return template.get("name").asText();
                }
            } catch (Exception ignored) {}
        }

        Map<String, Long> typeCount = alerts.stream()
            .collect(Collectors.groupingBy(
                a -> a.getAlertType().getTypeLabel(),
                Collectors.counting()
            ));

        return "Security Event: " + typeCount.entrySet().stream()
            .map(e -> e.getValue() + " " + e.getKey())
            .collect(Collectors.joining(", "));
    }

    private String generateEventDescription(List<Alert> alerts, EventCorrelationRule rule) {
        StringBuilder description = new StringBuilder();

        description.append("Correlated ").append(alerts.size()).append(" alerts");
        description.append(" using rule: ").append(rule.getRuleName());

        Map<String, Long> subtypeCount = alerts.stream()
            .filter(a -> a.getAlertSubtype() != null)
            .collect(Collectors.groupingBy(
                Alert::getAlertSubtype,
                Collectors.counting()
            ));

        if (!subtypeCount.isEmpty()) {
            description.append(". Subtypes: ");
            description.append(subtypeCount.entrySet().stream()
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .collect(Collectors.joining(", ")));
        }

        return description.toString();
    }

    private String determineEventType(List<Alert> alerts) {
        Map<String, Long> typeCount = alerts.stream()
            .collect(Collectors.groupingBy(
                a -> a.getAlertType().getTypeName(),
                Collectors.counting()
            ));

        return typeCount.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("Unknown");
    }

    private void aggregateEventFields(Event event, List<Alert> alerts) {
        Set<String> attackIps = new HashSet<>();
        Set<String> victimIps = new HashSet<>();
        Set<String> attackDomains = new HashSet<>();
        Set<String> victimDomains = new HashSet<>();

        String highestSeverity = "LOW";
        String highestPriority = "LOW";

        for (Alert alert : alerts) {
            if (alert.getSourceIp() != null) attackIps.add(alert.getSourceIp());
            if (alert.getDestIp() != null) victimIps.add(alert.getDestIp());

            if (compareSeverity(alert.getSeverity(), highestSeverity) > 0) {
                highestSeverity = alert.getSeverity();
            }
            if (comparePriority(alert.getPriority(), highestPriority) > 0) {
                highestPriority = alert.getPriority();
            }

            extractDomainsFromAlert(alert, attackDomains, victimDomains);
        }

        event.setAttackAssetIps(new ArrayList<>(attackIps));
        event.setVictimAssetIps(new ArrayList<>(victimIps));
        event.setAttackAssetDomains(new ArrayList<>(attackDomains));
        event.setVictimAssetDomains(new ArrayList<>(victimDomains));
        event.setSeverity(highestSeverity);
        event.setPriority(highestPriority);
    }

    private void extractDomainsFromAlert(Alert alert, Set<String> attackDomains, Set<String> victimDomains) {
        try {
            if (alert.getParsedData() != null) {
                JsonNode parsed = objectMapper.readTree(alert.getParsedData());

                extractFieldToSet(parsed, "attack_domain", attackDomains);
                extractFieldToSet(parsed, "victim_domain", victimDomains);
                extractFieldToSet(parsed, "domain", attackDomains);
            }
        } catch (Exception e) {
            log.debug("Error extracting domains from alert", e);
        }
    }

    private void extractFieldToSet(JsonNode node, String fieldName, Set<String> targetSet) {
        JsonNode field = node.get(fieldName);
        if (field != null) {
            if (field.isArray()) {
                field.forEach(item -> targetSet.add(item.asText()));
            } else {
                targetSet.add(field.asText());
            }
        }
    }

    private void calculateScores(Event event, List<Alert> alerts) {
        double totalRisk = 0;
        double totalConfidence = 0;
        int count = 0;

        for (Alert alert : alerts) {
            totalRisk += calculateAlertRisk(alert);
            totalConfidence += 0.8;
            count++;
        }

        event.setRiskScore(totalRisk / count * 1.5);
        event.setConfidenceScore(Math.min(totalConfidence / count, 1.0));
    }

    private double calculateAlertRisk(Alert alert) {
        double risk = 0.5;

        String severity = alert.getSeverity();
        if (severity != null) {
            switch (severity.toUpperCase()) {
                case "CRITICAL": risk = 1.0; break;
                case "HIGH": risk = 0.8; break;
                case "MEDIUM": risk = 0.5; break;
                case "LOW": risk = 0.3; break;
            }
        }

        return risk;
    }

    private int compareSeverity(String sev1, String sev2) {
        return getSeverityLevel(sev1) - getSeverityLevel(sev2);
    }

    private int comparePriority(String pri1, String pri2) {
        return getPriorityLevel(pri1) - getPriorityLevel(pri2);
    }

    private int getSeverityLevel(String severity) {
        if (severity == null) return 0;
        switch (severity.toUpperCase()) {
            case "CRITICAL": return 4;
            case "HIGH": return 3;
            case "MEDIUM": return 2;
            case "LOW": return 1;
            default: return 0;
        }
    }

    private int getPriorityLevel(String priority) {
        if (priority == null) return 0;
        switch (priority.toUpperCase()) {
            case "CRITICAL": return 4;
            case "HIGH": return 3;
            case "MEDIUM": return 2;
            case "LOW": return 1;
            default: return 0;
        }
    }

    private String getFieldValue(Alert alert, String fieldName) {
        try {
            if (alert.getParsedData() != null) {
                JsonNode parsed = objectMapper.readTree(alert.getParsedData());
                JsonNode value = parsed.get(fieldName);
                if (value != null && !value.isNull()) {
                    return value.asText();
                }
            }

            switch (fieldName.toLowerCase()) {
                case "source_ip": return alert.getSourceIp();
                case "dest_ip": return alert.getDestIp();
                case "alert_type": return alert.getAlertType().getTypeName();
                case "alert_subtype": return alert.getAlertSubtype();
                default: return null;
            }
        } catch (Exception e) {
            log.debug("Error getting field value: {}", fieldName, e);
            return null;
        }
    }
}