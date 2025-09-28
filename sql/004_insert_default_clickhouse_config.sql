-- Insert default ClickHouse configuration
INSERT INTO data_storage_config (
    name,
    db_type,
    host,
    port,
    database_name,
    username,
    password,
    connection_params,
    max_connections,
    connection_timeout,
    is_active,
    is_default,
    description,
    created_by,
    updated_by
) VALUES (
    'Default ClickHouse',
    'CLICKHOUSE',
    'localhost',
    8123,
    'alert_system',
    'admin',
    'clickhouse123',
    '{"protocol": "http", "compress": true, "max_execution_time": 60}',
    20,
    30000,
    true,
    true,
    'Default ClickHouse database for alert data storage',
    (SELECT id FROM users WHERE username = 'admin' LIMIT 1),
    (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
) ON CONFLICT (name) DO NOTHING;