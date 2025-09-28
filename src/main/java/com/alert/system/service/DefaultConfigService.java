package com.alert.system.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DefaultConfigService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void restoreDefaultConfig() {
        log.info("开始恢复默认配置...");

        log.info("清除现有配置数据...");
        entityManager.createNativeQuery("DELETE FROM alert_storage_mapping").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM alert_fields").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM alert_subtypes").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM alert_types").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM tags").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM rule_execution_logs").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM alert_tagging_rules").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM alert_filter_rules").executeUpdate();

        log.info("执行默认数据 SQL 脚本...");
        executeSqlFile("sql/006_create_alert_metadata_tables.sql");
        executeSqlFile("sql/007_create_alert_storage_mapping.sql");
        executeSqlFile("sql/008_create_tags_table.sql");

        log.info("默认配置恢复完成！");
    }

    private void executeSqlFile(String filePath) {
        try {
            log.info("执行 SQL 文件: {}", filePath);
            ClassPathResource resource = new ClassPathResource(filePath);

            if (!resource.exists()) {
                log.warn("SQL 文件不存在: {}", filePath);
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                StringBuilder sqlBuilder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.isEmpty() || line.startsWith("--")) {
                        continue;
                    }

                    sqlBuilder.append(line).append(" ");

                    if (line.endsWith(";")) {
                        String sql = sqlBuilder.toString().trim();
                        if (!sql.isEmpty()) {
                            try {
                                entityManager.createNativeQuery(sql).executeUpdate();
                                log.debug("执行 SQL: {}", sql.substring(0, Math.min(100, sql.length())));
                            } catch (Exception e) {
                                log.error("执行 SQL 失败: {}", sql, e);
                            }
                        }
                        sqlBuilder.setLength(0);
                    }
                }

            }

            log.info("SQL 文件执行完成: {}", filePath);

        } catch (Exception e) {
            log.error("执行 SQL 文件失败: {}", filePath, e);
            throw new RuntimeException("恢复默认配置失败: " + e.getMessage(), e);
        }
    }
}