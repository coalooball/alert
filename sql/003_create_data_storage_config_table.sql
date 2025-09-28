-- Create data storage configuration table
CREATE TABLE IF NOT EXISTS data_storage_config (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    db_type VARCHAR(50) NOT NULL, -- POSTGRESQL, MYSQL, CLICKHOUSE, MONGODB, ELASTICSEARCH
    host VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    database_name VARCHAR(100) NOT NULL,
    username VARCHAR(100),
    password VARCHAR(255), -- Should be encrypted in production
    connection_params TEXT, -- JSON string for additional connection parameters
    max_connections INTEGER DEFAULT 10,
    connection_timeout INTEGER DEFAULT 30000, -- milliseconds
    is_active BOOLEAN DEFAULT true,
    is_default BOOLEAN DEFAULT false,
    description TEXT,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES users(id),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_data_storage_name UNIQUE(name)
);

-- Create indexes
CREATE INDEX idx_data_storage_active ON data_storage_config(is_active);
CREATE INDEX idx_data_storage_default ON data_storage_config(is_default);
CREATE INDEX idx_data_storage_type ON data_storage_config(db_type);

-- Add comments
COMMENT ON TABLE data_storage_config IS 'Alert data storage configuration for different database types';
COMMENT ON COLUMN data_storage_config.db_type IS 'Database type: POSTGRESQL, MYSQL, CLICKHOUSE, MONGODB, ELASTICSEARCH';
COMMENT ON COLUMN data_storage_config.connection_params IS 'Additional connection parameters in JSON format';
COMMENT ON COLUMN data_storage_config.is_default IS 'Only one configuration can be default';