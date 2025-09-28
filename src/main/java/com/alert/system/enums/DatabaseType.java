package com.alert.system.enums;

public enum DatabaseType {
    POSTGRESQL("PostgreSQL"),
    MYSQL("MySQL"),
    CLICKHOUSE("ClickHouse"),
    MONGODB("MongoDB"),
    ELASTICSEARCH("Elasticsearch");

    private final String displayName;

    DatabaseType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}