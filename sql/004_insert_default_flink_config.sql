-- Insert default Flink configuration based on docker-compose setup
INSERT INTO flink_configs (
    id,
    name,
    job_manager_url,
    port,
    description,
    is_active,
    is_default,
    username,
    password,
    connection_timeout,
    read_timeout,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'Local Docker Flink',
    'http://localhost',
    8081,
    'Default Flink cluster running in Docker container (flink-jobmanager)',
    true,
    true,
    NULL,
    NULL,
    5000,
    10000,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (name) DO UPDATE SET
    job_manager_url = EXCLUDED.job_manager_url,
    port = EXCLUDED.port,
    description = EXCLUDED.description,
    is_active = EXCLUDED.is_active,
    is_default = EXCLUDED.is_default,
    updated_at = CURRENT_TIMESTAMP;