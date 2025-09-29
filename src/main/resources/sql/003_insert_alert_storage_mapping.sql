-- =====================================================
-- Alert storage mapping default data
-- Table is managed by JPA Entity
-- =====================================================

-- Insert default mappings for existing alert types
-- Using WITH clause to get IDs
WITH config AS (
    SELECT id as config_id FROM data_storage_config WHERE is_default = true LIMIT 1
),
admin_user AS (
    SELECT id as user_id FROM users WHERE username = 'admin' LIMIT 1
)
INSERT INTO alert_storage_mapping (
    alert_type_id,
    storage_config_id,
    table_name,
    retention_days,
    description,
    created_by,
    updated_by
)
SELECT
    alert_type_id,
    config.config_id,
    table_name,
    retention_days,
    description,
    admin_user.user_id,
    admin_user.user_id
FROM (
    VALUES
        (1, 'alert_network_attack', 90, '网络攻击类告警数据存储'),
        (2, 'alert_malicious_sample', 180, '恶意样本类告警数据存储'),
        (3, 'alert_host_behavior', 60, '主机行为类告警数据存储')
) AS data(alert_type_id, table_name, retention_days, description)
CROSS JOIN config
CROSS JOIN admin_user
ON CONFLICT (alert_type_id) DO NOTHING;