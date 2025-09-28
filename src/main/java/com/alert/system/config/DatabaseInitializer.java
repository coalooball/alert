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

            // Get all SQL files in the sql directory
            java.io.File sqlDir = new java.io.File("sql");
            java.io.File[] sqlFiles = sqlDir.listFiles((dir, name) -> name.endsWith(".sql"));

            if (sqlFiles != null && sqlFiles.length > 0) {
                // Sort SQL files by name to ensure proper execution order
                Arrays.sort(sqlFiles, Comparator.comparing(java.io.File::getName));

                for (java.io.File sqlFile : sqlFiles) {
                    System.out.println("📄 Executing SQL file: " + sqlFile.getName());
                    String sql = new String(Files.readAllBytes(sqlFile.toPath()), StandardCharsets.UTF_8);

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
            } else {
                System.out.println("⚠️ No SQL files found in sql directory");
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}