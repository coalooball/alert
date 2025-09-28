package com.alert.system.service;

import com.alert.system.dto.DataStorageConfigDTO;
import com.alert.system.entity.DataStorageConfig;
import com.alert.system.entity.User;
import com.alert.system.enums.DatabaseType;
import com.alert.system.repository.DataStorageConfigRepository;
import com.alert.system.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;

@Service
public class DataStorageConfigService {

    @Autowired
    private DataStorageConfigRepository repository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<DataStorageConfigDTO> getAllConfigs() {
        List<DataStorageConfig> configs = repository.findAll();
        return configs.stream().map(this::convertToDTO).toList();
    }

    public List<DataStorageConfigDTO> getActiveConfigs() {
        List<DataStorageConfig> configs = repository.findByIsActiveTrue();
        return configs.stream().map(this::convertToDTO).toList();
    }

    public DataStorageConfigDTO getConfigById(Long id) {
        DataStorageConfig config = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Config not found with id: " + id));
        return convertToDTO(config);
    }

    public DataStorageConfigDTO getDefaultConfig() {
        DataStorageConfig config = repository.findByIsDefaultTrue()
                .orElse(null);
        return config != null ? convertToDTO(config) : null;
    }

    @Transactional
    public DataStorageConfigDTO createConfig(DataStorageConfigDTO dto) {
        if (repository.findByName(dto.getName()).isPresent()) {
            throw new RuntimeException("Config with name already exists: " + dto.getName());
        }

        DataStorageConfig config = convertToEntity(dto);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        config.setCreatedBy(currentUser);
        config.setUpdatedBy(currentUser);

        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            repository.clearDefaultExcept(-1L);
        }

        DataStorageConfig saved = repository.save(config);
        return convertToDTO(saved);
    }

    @Transactional
    public DataStorageConfigDTO updateConfig(Long id, DataStorageConfigDTO dto) {
        DataStorageConfig config = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Config not found with id: " + id));

        if (!config.getName().equals(dto.getName()) &&
            repository.existsByNameAndIdNot(dto.getName(), id)) {
            throw new RuntimeException("Config with name already exists: " + dto.getName());
        }

        updateEntityFromDTO(config, dto);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        config.setUpdatedBy(currentUser);

        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            repository.clearDefaultExcept(id);
        }

        DataStorageConfig saved = repository.save(config);
        return convertToDTO(saved);
    }

    @Transactional
    public void deleteConfig(Long id) {
        DataStorageConfig config = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Config not found with id: " + id));

        if (Boolean.TRUE.equals(config.getIsDefault())) {
            throw new RuntimeException("Cannot delete default configuration");
        }

        repository.deleteById(id);
    }

    public Map<String, Object> testConnection(DataStorageConfigDTO dto) {
        Map<String, Object> result = new HashMap<>();

        try {
            String connectionUrl = buildConnectionUrl(dto);

            switch (dto.getDbType()) {
                case POSTGRESQL:
                case MYSQL:
                    testJdbcConnection(connectionUrl, dto.getUsername(), dto.getPassword());
                    break;
                case CLICKHOUSE:
                    testClickHouseConnection(dto);
                    break;
                default:
                    result.put("success", false);
                    result.put("message", "Database type not supported for testing: " + dto.getDbType());
                    return result;
            }

            result.put("success", true);
            result.put("message", "Connection test successful");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Connection failed: " + e.getMessage());
        }

        return result;
    }

    private void testJdbcConnection(String url, String username, String password) throws Exception {
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            conn.isValid(5);
        }
    }

    private void testClickHouseConnection(DataStorageConfigDTO dto) throws Exception {
        String url = String.format("jdbc:clickhouse://%s:%d/%s",
                dto.getHost(), dto.getPort(), dto.getDatabaseName());

        Properties props = new Properties();
        if (dto.getUsername() != null) props.setProperty("user", dto.getUsername());
        if (dto.getPassword() != null) props.setProperty("password", dto.getPassword());

        try (Connection conn = DriverManager.getConnection(url, props)) {
            conn.isValid(5);
        }
    }

    private String buildConnectionUrl(DataStorageConfigDTO dto) {
        switch (dto.getDbType()) {
            case POSTGRESQL:
                return String.format("jdbc:postgresql://%s:%d/%s",
                        dto.getHost(), dto.getPort(), dto.getDatabaseName());
            case MYSQL:
                return String.format("jdbc:mysql://%s:%d/%s",
                        dto.getHost(), dto.getPort(), dto.getDatabaseName());
            case CLICKHOUSE:
                return String.format("jdbc:clickhouse://%s:%d/%s",
                        dto.getHost(), dto.getPort(), dto.getDatabaseName());
            default:
                throw new UnsupportedOperationException("Database type not supported: " + dto.getDbType());
        }
    }

    private DataStorageConfigDTO convertToDTO(DataStorageConfig entity) {
        DataStorageConfigDTO dto = new DataStorageConfigDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDbType(entity.getDbType());
        dto.setHost(entity.getHost());
        dto.setPort(entity.getPort());
        dto.setDatabaseName(entity.getDatabaseName());
        dto.setUsername(entity.getUsername());
        dto.setPassword(entity.getPassword());
        dto.setMaxConnections(entity.getMaxConnections());
        dto.setConnectionTimeout(entity.getConnectionTimeout());
        dto.setIsActive(entity.getIsActive());
        dto.setIsDefault(entity.getIsDefault());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getCreatedBy() != null) {
            dto.setCreatedBy(entity.getCreatedBy().getUsername());
        }
        if (entity.getUpdatedBy() != null) {
            dto.setUpdatedBy(entity.getUpdatedBy().getUsername());
        }

        if (entity.getConnectionParams() != null) {
            try {
                Map<String, Object> params = objectMapper.readValue(
                    entity.getConnectionParams(),
                    new TypeReference<Map<String, Object>>() {}
                );
                dto.setConnectionParams(params);
            } catch (JsonProcessingException e) {
                dto.setConnectionParams(new HashMap<>());
            }
        }

        return dto;
    }

    private DataStorageConfig convertToEntity(DataStorageConfigDTO dto) {
        DataStorageConfig entity = new DataStorageConfig();
        updateEntityFromDTO(entity, dto);
        return entity;
    }

    private void updateEntityFromDTO(DataStorageConfig entity, DataStorageConfigDTO dto) {
        entity.setName(dto.getName());
        entity.setDbType(dto.getDbType());
        entity.setHost(dto.getHost());
        entity.setPort(dto.getPort());
        entity.setDatabaseName(dto.getDatabaseName());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setMaxConnections(dto.getMaxConnections());
        entity.setConnectionTimeout(dto.getConnectionTimeout());
        entity.setIsActive(dto.getIsActive());
        entity.setIsDefault(dto.getIsDefault());
        entity.setDescription(dto.getDescription());

        if (dto.getConnectionParams() != null) {
            try {
                String params = objectMapper.writeValueAsString(dto.getConnectionParams());
                entity.setConnectionParams(params);
            } catch (JsonProcessingException e) {
                entity.setConnectionParams("{}");
            }
        }
    }

    public List<DatabaseType> getSupportedDatabaseTypes() {
        return Arrays.asList(DatabaseType.values());
    }
}