package com.alert.system.service;

import com.alert.system.entity.*;
import com.alert.system.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class ObservableService {

    private final ObservableRepository observableRepository;
    private final ObservableTypeRepository observableTypeRepository;
    private final AlertObservableMappingRepository alertObservableMappingRepository;
    private final ObjectMapper objectMapper;

    private static final Map<String, Pattern> OBSERVABLE_PATTERNS = new HashMap<>();

    static {
        OBSERVABLE_PATTERNS.put("IP", Pattern.compile(
            "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"));
        OBSERVABLE_PATTERNS.put("DOMAIN", Pattern.compile(
            "\\b(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}\\b"));
        OBSERVABLE_PATTERNS.put("URL", Pattern.compile(
            "https?://[^\\s]+"));
        OBSERVABLE_PATTERNS.put("EMAIL", Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"));
        OBSERVABLE_PATTERNS.put("MD5", Pattern.compile(
            "\\b[a-fA-F0-9]{32}\\b"));
        OBSERVABLE_PATTERNS.put("SHA1", Pattern.compile(
            "\\b[a-fA-F0-9]{40}\\b"));
        OBSERVABLE_PATTERNS.put("SHA256", Pattern.compile(
            "\\b[a-fA-F0-9]{64}\\b"));
        OBSERVABLE_PATTERNS.put("CVE", Pattern.compile(
            "CVE-\\d{4}-\\d{4,7}"));
        OBSERVABLE_PATTERNS.put("FILE_PATH", Pattern.compile(
            "(?:[A-Za-z]:)?[\\\\/](?:[^\\\\/:\\*\\?\"<>|\\r\\n]+[\\\\/])*[^\\\\/:\\*\\?\"<>|\\r\\n]*"));
    }

    @Transactional
    public void extractAndSaveObservables(Alert alert) {
        try {
            Set<ExtractedObservable> extractedObservables = new HashSet<>();

            extractFromRawData(alert, extractedObservables);

            extractFromParsedData(alert, extractedObservables);

            extractFromSpecificFields(alert, extractedObservables);

            for (ExtractedObservable extracted : extractedObservables) {
                com.alert.system.entity.Observable observable = findOrCreateObservable(extracted);

                createAlertObservableMapping(alert, observable, extracted);
            }

            log.debug("Extracted {} observables from alert {}",
                extractedObservables.size(), alert.getAlertUuid());

        } catch (Exception e) {
            log.error("Error extracting observables from alert: {}", alert.getAlertUuid(), e);
        }
    }

    private void extractFromRawData(Alert alert, Set<ExtractedObservable> observables) {
        if (alert.getRawData() == null) return;

        try {
            String rawData = alert.getRawData();

            for (Map.Entry<String, Pattern> entry : OBSERVABLE_PATTERNS.entrySet()) {
                String type = entry.getKey();
                Pattern pattern = entry.getValue();

                Matcher matcher = pattern.matcher(rawData);
                while (matcher.find()) {
                    String value = matcher.group();
                    if (isValidObservable(type, value)) {
                        observables.add(new ExtractedObservable(type, value, "raw_data", null));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error extracting observables from raw data", e);
        }
    }

    private void extractFromParsedData(Alert alert, Set<ExtractedObservable> observables) {
        if (alert.getParsedData() == null) return;

        try {
            JsonNode parsedData = objectMapper.readTree(alert.getParsedData());

            extractFromJsonNode(parsedData, "", observables);

        } catch (Exception e) {
            log.debug("Error extracting observables from parsed data", e);
        }
    }

    private void extractFromJsonNode(JsonNode node, String path, Set<ExtractedObservable> observables) {
        if (node.isTextual()) {
            String value = node.asText();
            detectAndAddObservable(value, path, observables);
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                extractFromJsonNode(node.get(i), path + "[" + i + "]", observables);
            }
        } else if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldPath = path.isEmpty() ? field.getKey() : path + "." + field.getKey();
                extractFromJsonNode(field.getValue(), fieldPath, observables);

                String fieldName = field.getKey().toLowerCase();
                if (fieldName.contains("ip") || fieldName.contains("address")) {
                    extractTypedObservable(field.getValue(), "IP", fieldPath, observables);
                } else if (fieldName.contains("domain") || fieldName.contains("host")) {
                    extractTypedObservable(field.getValue(), "DOMAIN", fieldPath, observables);
                } else if (fieldName.contains("url")) {
                    extractTypedObservable(field.getValue(), "URL", fieldPath, observables);
                } else if (fieldName.contains("email") || fieldName.contains("mail")) {
                    extractTypedObservable(field.getValue(), "EMAIL", fieldPath, observables);
                } else if (fieldName.contains("hash") || fieldName.contains("md5") ||
                          fieldName.contains("sha")) {
                    detectHashType(field.getValue(), fieldPath, observables);
                } else if (fieldName.contains("cve") || fieldName.contains("vulnerability")) {
                    extractTypedObservable(field.getValue(), "CVE", fieldPath, observables);
                }
            }
        }
    }

    private void extractTypedObservable(JsonNode node, String type, String path,
                                       Set<ExtractedObservable> observables) {
        if (node.isTextual()) {
            String value = node.asText();
            if (isValidObservable(type, value)) {
                observables.add(new ExtractedObservable(type, value, path, null));
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual()) {
                    String value = item.asText();
                    if (isValidObservable(type, value)) {
                        observables.add(new ExtractedObservable(type, value, path, null));
                    }
                }
            }
        }
    }

    private void detectAndAddObservable(String value, String path, Set<ExtractedObservable> observables) {
        for (Map.Entry<String, Pattern> entry : OBSERVABLE_PATTERNS.entrySet()) {
            String type = entry.getKey();
            Pattern pattern = entry.getValue();

            Matcher matcher = pattern.matcher(value);
            while (matcher.find()) {
                String matchedValue = matcher.group();
                if (isValidObservable(type, matchedValue)) {
                    observables.add(new ExtractedObservable(type, matchedValue, path, null));
                }
            }
        }
    }

    private void detectHashType(JsonNode node, String path, Set<ExtractedObservable> observables) {
        if (node.isTextual()) {
            String value = node.asText().trim();
            String hashType = null;

            if (value.matches("[a-fA-F0-9]{32}")) {
                hashType = "MD5";
            } else if (value.matches("[a-fA-F0-9]{40}")) {
                hashType = "SHA1";
            } else if (value.matches("[a-fA-F0-9]{64}")) {
                hashType = "SHA256";
            }

            if (hashType != null) {
                observables.add(new ExtractedObservable(hashType, value, path, null));
            }
        }
    }

    private void extractFromSpecificFields(Alert alert, Set<ExtractedObservable> observables) {
        if (alert.getSourceIp() != null && isValidIp(alert.getSourceIp())) {
            observables.add(new ExtractedObservable("IP", alert.getSourceIp(),
                "source_ip", "ATTACKER"));
        }

        if (alert.getDestIp() != null && isValidIp(alert.getDestIp())) {
            observables.add(new ExtractedObservable("IP", alert.getDestIp(),
                "dest_ip", "VICTIM"));
        }

        if (alert.getTitle() != null) {
            detectAndAddObservable(alert.getTitle(), "title", observables);
        }

        if (alert.getDescription() != null) {
            detectAndAddObservable(alert.getDescription(), "description", observables);
        }
    }

    private boolean isValidObservable(String type, String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        switch (type) {
            case "IP":
                return isValidIp(value);
            case "DOMAIN":
                return isValidDomain(value);
            case "URL":
                return isValidUrl(value);
            case "EMAIL":
                return isValidEmail(value);
            case "MD5":
            case "SHA1":
            case "SHA256":
                return isValidHash(value, type);
            case "CVE":
                return value.matches("CVE-\\d{4}-\\d{4,7}");
            default:
                return true;
        }
    }

    private boolean isValidIp(String ip) {
        if (ip == null) return false;

        if (ip.equals("0.0.0.0") || ip.equals("127.0.0.1") ||
            ip.equals("255.255.255.255") || ip.startsWith("169.254.")) {
            return false;
        }

        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;

        for (String part : parts) {
            try {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidDomain(String domain) {
        if (domain == null || domain.length() > 253) return false;

        if (domain.equals("localhost") || domain.endsWith(".local") ||
            domain.endsWith(".localdomain")) {
            return false;
        }

        return domain.matches("^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]?(\\.[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]?)*$");
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.length() < 10) return false;

        return url.matches("^https?://.*");
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;

        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isValidHash(String hash, String type) {
        if (hash == null) return false;

        switch (type) {
            case "MD5":
                return hash.matches("^[a-fA-F0-9]{32}$");
            case "SHA1":
                return hash.matches("^[a-fA-F0-9]{40}$");
            case "SHA256":
                return hash.matches("^[a-fA-F0-9]{64}$");
            default:
                return false;
        }
    }

    @Transactional
    private com.alert.system.entity.Observable findOrCreateObservable(ExtractedObservable extracted) {
        return observableRepository.findByTypeAndValue(extracted.type, extracted.value)
            .map(observable -> {
                observable.setLastSeen(LocalDateTime.now());
                observable.setOccurrenceCount(observable.getOccurrenceCount() + 1);
                return observableRepository.save(observable);
            })
            .orElseGet(() -> {
                com.alert.system.entity.Observable newObservable = new com.alert.system.entity.Observable();
                newObservable.setObservableType(extracted.type);
                newObservable.setObservableValue(extracted.value);
                newObservable.setDisplayName(generateDisplayName(extracted.type, extracted.value));
                newObservable.setCategory(determineCategory(extracted.type));
                newObservable.setFirstSeen(LocalDateTime.now());
                newObservable.setLastSeen(LocalDateTime.now());
                newObservable.setOccurrenceCount(1);
                newObservable.setStatus("ACTIVE");

                enrichObservable(newObservable);

                return observableRepository.save(newObservable);
            });
    }

    private void createAlertObservableMapping(Alert alert, com.alert.system.entity.Observable observable,
                                             ExtractedObservable extracted) {
        try {
            List<AlertObservableMapping> existingMappings =
                alertObservableMappingRepository.findByAlertIdAndObservableId(
                    alert.getId(), observable.getId());

            if (existingMappings.isEmpty()) {
                AlertObservableMapping mapping = new AlertObservableMapping();
                mapping.setAlert(alert);
                mapping.setObservable(observable);
                mapping.setRole(extracted.role);
                mapping.setContext(extracted.context);
                mapping.setExtractedFrom(extracted.source);
                mapping.setCreatedAt(LocalDateTime.now());

                alertObservableMappingRepository.save(mapping);
            }
        } catch (Exception e) {
            log.error("Error creating alert-observable mapping", e);
        }
    }

    private String generateDisplayName(String type, String value) {
        switch (type) {
            case "IP":
                return "IP Address: " + value;
            case "DOMAIN":
                return "Domain: " + value;
            case "URL":
                return "URL: " + (value.length() > 50 ? value.substring(0, 50) + "..." : value);
            case "EMAIL":
                return "Email: " + value;
            case "MD5":
            case "SHA1":
            case "SHA256":
                return type + " Hash: " + value.substring(0, Math.min(value.length(), 16)) + "...";
            case "CVE":
                return "Vulnerability: " + value;
            default:
                return type + ": " + value;
        }
    }

    private String determineCategory(String type) {
        switch (type) {
            case "IP":
            case "DOMAIN":
            case "URL":
                return "NETWORK";
            case "EMAIL":
                return "IDENTITY";
            case "MD5":
            case "SHA1":
            case "SHA256":
                return "FILE";
            case "CVE":
                return "VULNERABILITY";
            case "FILE_PATH":
                return "HOST";
            default:
                return "OTHER";
        }
    }

    private void enrichObservable(com.alert.system.entity.Observable observable) {
        // TODO: Implement threat intelligence enrichment
        // TODO: Implement GeoIP lookup for IP addresses
        // TODO: Implement WHOIS lookup for domains
        // TODO: Implement VirusTotal/other threat intel API checks
    }

    public List<com.alert.system.entity.Observable> findObservablesByType(String type) {
        return observableRepository.findByObservableType(type);
    }

    public List<com.alert.system.entity.Observable> findMaliciousObservables() {
        return observableRepository.findByIsMalicious(true);
    }

    public List<com.alert.system.entity.Observable> findHighRiskObservables(double minRiskScore) {
        return observableRepository.findByRiskScoreGreaterThanEqual(minRiskScore);
    }

    @Transactional
    public com.alert.system.entity.Observable updateObservableRisk(UUID observableId, double riskScore, boolean isMalicious) {
        com.alert.system.entity.Observable observable = observableRepository.findById(observableId)
            .orElseThrow(() -> new RuntimeException("Observable not found"));

        observable.setRiskScore(riskScore);
        observable.setIsMalicious(isMalicious);
        observable.setUpdatedAt(LocalDateTime.now());

        return observableRepository.save(observable);
    }

    private static class ExtractedObservable {
        final String type;
        final String value;
        final String source;
        final String role;
        final String context;

        ExtractedObservable(String type, String value, String source, String role) {
            this.type = type;
            this.value = value;
            this.source = source;
            this.role = role;
            this.context = source;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExtractedObservable that = (ExtractedObservable) o;
            return type.equals(that.type) && value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, value);
        }
    }
}