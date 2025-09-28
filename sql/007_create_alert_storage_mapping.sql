-- Drop existing table and recreate with proper structure
DROP TABLE IF EXISTS alert_storage_mapping CASCADE;

-- Create alert data storage mapping table
CREATE TABLE IF NOT EXISTS alert_storage_mapping (
    id BIGSERIAL PRIMARY KEY,
    alert_type_id INTEGER NOT NULL REFERENCES alert_types(id) ON DELETE CASCADE,
    storage_config_id BIGINT REFERENCES data_storage_config(id) ON DELETE RESTRICT,
    table_name VARCHAR(100) NOT NULL,
    retention_days INTEGER DEFAULT 90,
    description TEXT,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES users(id),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_alert_storage_type UNIQUE(alert_type_id)
);

-- Create indexes
CREATE INDEX idx_alert_storage_type_id ON alert_storage_mapping(alert_type_id);
CREATE INDEX idx_alert_storage_config ON alert_storage_mapping(storage_config_id);

-- Add comments
COMMENT ON TABLE alert_storage_mapping IS 'Mapping of alert types to storage configurations';
COMMENT ON COLUMN alert_storage_mapping.alert_type_id IS 'Reference to alert_types table';
COMMENT ON COLUMN alert_storage_mapping.storage_config_id IS 'Reference to data storage configuration';
COMMENT ON COLUMN alert_storage_mapping.table_name IS 'Target table name in the storage database';
COMMENT ON COLUMN alert_storage_mapping.retention_days IS 'Data retention period in days';

-- Insert default mappings for existing alert types
INSERT INTO alert_storage_mapping (
    alert_type_id,
    storage_config_id,
    table_name,
    retention_days,
    description,
    created_by,
    updated_by
) VALUES
(
    1,  -- network_attack
    (SELECT id FROM data_storage_config WHERE is_default = true LIMIT 1),
    'alert_network_attack',
    90,
    '网络攻击类告警数据存储',
    (SELECT id FROM users WHERE username = 'admin' LIMIT 1),
    (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
),
(
    2,  -- malicious_sample
    (SELECT id FROM data_storage_config WHERE is_default = true LIMIT 1),
    'alert_malicious_sample',
    180,
    '恶意样本类告警数据存储',
    (SELECT id FROM users WHERE username = 'admin' LIMIT 1),
    (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
),
(
    3,  -- host_behavior
    (SELECT id FROM data_storage_config WHERE is_default = true LIMIT 1),
    'alert_host_behavior',
    60,
    '主机行为类告警数据存储',
    (SELECT id FROM users WHERE username = 'admin' LIMIT 1),
    (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
)
ON CONFLICT (alert_type_id) DO NOTHING;