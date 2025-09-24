-- Create flink_configs table
CREATE TABLE IF NOT EXISTS flink_configs (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    job_manager_url VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL DEFAULT 8081,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    username VARCHAR(100),
    password VARCHAR(100),
    connection_timeout INTEGER DEFAULT 5000,
    read_timeout INTEGER DEFAULT 10000,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_test_time TIMESTAMP,
    last_test_status VARCHAR(50),
    last_test_message VARCHAR(1000)
);

-- Create indexes
CREATE INDEX idx_flink_configs_name ON flink_configs(name);
CREATE INDEX idx_flink_configs_is_active ON flink_configs(is_active);
CREATE INDEX idx_flink_configs_is_default ON flink_configs(is_default);