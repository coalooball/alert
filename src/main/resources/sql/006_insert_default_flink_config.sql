-- =====================================================
-- Flink 计算引擎默认配置
-- Tables are managed by JPA Entities
-- =====================================================

-- 插入默认 Flink 配置
INSERT INTO flink_configs (
    id,
    name,
    job_manager_url,
    port,
    description,
    is_active,
    is_default,
    connection_timeout,
    read_timeout,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'Default Flink Cluster',
    'http://localhost',
    8081,
    'Default Flink cluster configuration for alert processing',
    true,
    true,
    5000,
    10000,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (name) DO UPDATE SET
    job_manager_url = EXCLUDED.job_manager_url,
    port = EXCLUDED.port,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;

-- 插入备用 Flink 配置（可选）
INSERT INTO flink_configs (
    id,
    name,
    job_manager_url,
    port,
    description,
    is_active,
    is_default,
    connection_timeout,
    read_timeout,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'Backup Flink Cluster',
    'http://flink-backup',
    8081,
    'Backup Flink cluster configuration for high availability',
    false,
    false,
    5000,
    10000,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (name) DO UPDATE SET
    job_manager_url = EXCLUDED.job_manager_url,
    port = EXCLUDED.port,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;