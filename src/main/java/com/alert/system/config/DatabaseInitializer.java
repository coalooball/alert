package com.alert.system.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private DataSource dataSource;

    @Value("${app.init-db:false}")
    private boolean initDb;

    @Override
    public void run(String... args) throws Exception {
        if (System.getProperty("app.init-db") != null) {
            System.out.println("🔄 Initializing database (drop and recreate)...");
            initializeDatabase();
        }
    }

    private void initializeDatabase() {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            // Execute SQL files in order
            String[] sqlFiles = new String[]{
                    "sql/001_create_users_table.sql",
                    "sql/002_create_users_indexes.sql"
            };

            for (String sqlFile : sqlFiles) {
                System.out.println("📄 Executing SQL file: " + sqlFile);
                String sql = new String(Files.readAllBytes(Paths.get(sqlFile)), StandardCharsets.UTF_8);

                // Remove comments and execute statements
                String[] statements = sql.split(";");
                for (String statement : statements) {
                    String trimmed = statement.trim()
                            .replaceAll("--.*", "")
                            .replaceAll("/\\*.*?\\*/", "")
                            .trim();
                    if (!trimmed.isEmpty()) {
                        System.out.println("  Executing: " + trimmed.split("\n")[0]);
                        jdbcTemplate.execute(trimmed);
                    }
                }
            }

            System.out.println("✅ Database tables created successfully");

        } catch (Exception e) {
            System.err.println("❌ Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}