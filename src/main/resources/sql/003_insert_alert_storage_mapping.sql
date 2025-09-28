-- =====================================================
-- Alert storage mapping default data
-- Table is managed by JPA Entity
-- =====================================================

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