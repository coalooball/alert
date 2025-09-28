package com.alert.system.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Component
@Slf4j
public class DatabaseInitializer implements BeanFactoryPostProcessor, EnvironmentAware, Ordered {

    private Environment environment;
    private static final String DB_NAME = "alert_system";

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        ensureDatabaseExists();
    }

    private void ensureDatabaseExists() {
        String databaseUrl = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");

        if (databaseUrl == null || username == null || password == null) {
            log.warn("⚠️  Database configuration not found, skipping database check");
            return;
        }

        log.info("🔍 Checking if database '{}' exists...", DB_NAME);

        if (!databaseExists(databaseUrl, username, password)) {
            log.info("📦 Database '{}' does not exist. Creating...", DB_NAME);
            createDatabase(databaseUrl, username, password);
            log.info("✅ Database '{}' created successfully", DB_NAME);
        } else {
            log.info("✅ Database '{}' already exists", DB_NAME);
        }
    }

    private boolean databaseExists(String databaseUrl, String username, String password) {
        String postgresUrl = databaseUrl.substring(0, databaseUrl.lastIndexOf('/')) + "/postgres";

        try (Connection conn = java.sql.DriverManager.getConnection(postgresUrl, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT 1 FROM pg_database WHERE datname = '" + DB_NAME + "'")) {
            return rs.next();
        } catch (SQLException e) {
            log.error("❌ Error checking database existence: {}", e.getMessage());
            return false;
        }
    }

    private void createDatabase(String databaseUrl, String username, String password) {
        String postgresUrl = databaseUrl.substring(0, databaseUrl.lastIndexOf('/')) + "/postgres";

        try (Connection conn = java.sql.DriverManager.getConnection(postgresUrl, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE " + DB_NAME);
            log.info("✅ Database created, waiting for it to be ready...");
            Thread.sleep(2000); // Wait for database to be fully ready
        } catch (SQLException e) {
            log.error("❌ Error creating database: {}", e.getMessage());
            throw new RuntimeException("Failed to create database", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for database", e);
        }
    }

    @Component
    @Slf4j
    public static class DataInitializer implements CommandLineRunner {

        private final DataSource dataSource;

        public DataInitializer(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public void run(String... args) throws Exception {
            insertDefaultDataIfNeeded();
        }

        private void insertDefaultDataIfNeeded() {
            try {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

                // Check if default data already exists
                Long count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM alert_types", Long.class);

                if (count != null && count > 0) {
                    log.info("✅ Default data already exists, skipping initialization");
                    return;
                }

                log.info("📝 Inserting default data from SQL files...");
                insertDefaultData(jdbcTemplate);
                log.info("🎉 Default data insertion completed successfully!");

            } catch (Exception e) {
                log.warn("⚠️  Could not check or insert default data: {}", e.getMessage());
            }
        }

        private void insertDefaultData(JdbcTemplate jdbcTemplate) {
            // SQL files to execute in order
            String[] sqlFiles = {
                "sql/001_insert_default_clickhouse_config.sql",
                "sql/002_insert_alert_metadata.sql",
                "sql/003_insert_alert_storage_mapping.sql",
                "sql/004_insert_default_tags.sql",
                "sql/005_insert_default_datasource_config.sql"
            };

            for (String sqlFile : sqlFiles) {
                try {
                    log.info("  📄 Executing: {}", sqlFile);
                    ClassPathResource resource = new ClassPathResource(sqlFile);

                    if (!resource.exists()) {
                        log.warn("  ⚠️  SQL file not found: {}", sqlFile);
                        continue;
                    }

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                        StringBuilder sqlBuilder = new StringBuilder();
                        String line;

                        while ((line = reader.readLine()) != null) {
                            line = line.trim();

                            // Skip comments and empty lines
                            if (line.isEmpty() || line.startsWith("--")) {
                                continue;
                            }

                            sqlBuilder.append(line).append(" ");

                            // Execute statement when semicolon is found
                            if (line.endsWith(";")) {
                                String sql = sqlBuilder.toString().trim();
                                if (!sql.isEmpty()) {
                                    try {
                                        jdbcTemplate.execute(sql);
                                    } catch (Exception e) {
                                        log.warn("  ⚠️  Failed to execute SQL statement: {}", e.getMessage());
                                    }
                                }
                                sqlBuilder.setLength(0);
                            }
                        }
                    }

                    log.info("  ✅ Completed: {}", sqlFile);

                } catch (Exception e) {
                    log.error("  ❌ Error executing SQL file {}: {}", sqlFile, e.getMessage());
                }
            }
        }
    }
}