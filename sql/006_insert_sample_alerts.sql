-- Insert sample alerts for testing Alert Mining functionality
-- These alerts will be linked to the existing alert types and will have tags associated

-- Insert sample alerts (network-alert type = 1, host-alert type = 2, application-alert type = 3)
INSERT INTO alerts (
    id, alert_uuid, alert_type_id, alert_subtype, alert_time,
    source_ip, dest_ip, source_port, dest_port,
    severity, priority, title, description,
    raw_data, parsed_data, is_filtered, status,
    clickhouse_table, processing_time, storage_time,
    created_at, updated_at
) VALUES
(
    gen_random_uuid(), 'ALERT-001', 1, 'firewall-alert', NOW() - INTERVAL '2 hours',
    '192.168.1.100', '10.0.0.50', 45632, 443,
    'HIGH', 'P1', 'Suspicious Firewall Activity Detected',
    'Multiple failed connection attempts detected from internal IP to external service',
    '{"event_type": "firewall_block", "rule_id": "FW-2023-001", "packets": 150}',
    '{"analyzed": true, "threat_level": "high", "action": "blocked"}',
    false, 'NEW', 'alerts_network', NOW(), NOW(),
    NOW(), NOW()
),
(
    gen_random_uuid(), 'ALERT-002', 1, 'ids-alert', NOW() - INTERVAL '1 hour',
    '203.0.113.45', '192.168.1.20', 8080, 80,
    'CRITICAL', 'P1', 'IDS Alert: Potential SQL Injection Attack',
    'Intrusion Detection System identified potential SQL injection patterns in HTTP request',
    '{"signature_id": "IDS-SQL-001", "confidence": 0.95, "payload_sample": "SELECT * FROM..."}',
    '{"attack_type": "sql_injection", "mitigation": "request_blocked"}',
    false, 'NEW', 'alerts_network', NOW(), NOW(),
    NOW(), NOW()
),
(
    gen_random_uuid(), 'ALERT-003', 2, 'cpu-alert', NOW() - INTERVAL '30 minutes',
    '10.0.1.15', NULL, NULL, NULL,
    'MEDIUM', 'P2', 'High CPU Usage on Production Server',
    'CPU usage exceeded 85% threshold on production database server',
    '{"hostname": "prod-db-01", "cpu_percent": 87.5, "duration_minutes": 15}',
    '{"resource": "cpu", "threshold_exceeded": true, "auto_scaled": false}',
    false, 'NEW', 'alerts_host', NOW(), NOW(),
    NOW(), NOW()
),
(
    gen_random_uuid(), 'ALERT-004', 2, 'memory-alert', NOW() - INTERVAL '45 minutes',
    '10.0.1.20', NULL, NULL, NULL,
    'LOW', 'P3', 'Memory Usage Warning',
    'Memory usage approaching configured threshold on application server',
    '{"hostname": "app-server-02", "memory_percent": 75, "available_gb": 8}',
    '{"resource": "memory", "warning_level": true}',
    false, 'NEW', 'alerts_host', NOW(), NOW(),
    NOW(), NOW()
),
(
    gen_random_uuid(), 'ALERT-005', 3, 'error-alert', NOW() - INTERVAL '15 minutes',
    '172.16.0.100', '172.16.0.200', 5000, 5432,
    'HIGH', 'P1', 'Database Connection Pool Exhausted',
    'Application unable to acquire database connections from pool',
    '{"app_name": "user-service", "pool_size": 100, "active_connections": 100}',
    '{"error_code": "CONN_POOL_EXHAUSTED", "impacted_users": 450}',
    false, 'NEW', 'alerts_application', NOW(), NOW(),
    NOW(), NOW()
);

-- Get the IDs of the inserted alerts for tag mapping
-- We'll use a subquery to get the alert IDs based on alert_uuid

-- Insert tag mappings for the alerts using existing Chinese tags
INSERT INTO alert_tag_mappings (id, alert_id, tag_id, is_auto_tagged, confidence_score, created_at)
SELECT
    gen_random_uuid(),
    a.id,
    t.id,
    true,
    CASE
        WHEN t.tag_name = '严重威胁' THEN 0.95
        WHEN t.tag_name = '高危威胁' THEN 0.90
        ELSE 0.85
    END,
    NOW()
FROM alerts a
CROSS JOIN tags t
WHERE a.alert_uuid = 'ALERT-001'
AND t.tag_name IN ('严重威胁', '网络攻击', 'DDoS攻击');

INSERT INTO alert_tag_mappings (id, alert_id, tag_id, is_auto_tagged, confidence_score, created_at)
SELECT
    gen_random_uuid(),
    a.id,
    t.id,
    true,
    0.92,
    NOW()
FROM alerts a
CROSS JOIN tags t
WHERE a.alert_uuid = 'ALERT-002'
AND t.tag_name IN ('严重威胁', 'SQL注入', '网络攻击');

INSERT INTO alert_tag_mappings (id, alert_id, tag_id, is_auto_tagged, confidence_score, created_at)
SELECT
    gen_random_uuid(),
    a.id,
    t.id,
    true,
    0.88,
    NOW()
FROM alerts a
CROSS JOIN tags t
WHERE a.alert_uuid = 'ALERT-003'
AND t.tag_name IN ('中危威胁', '性能异常', '系统监控');

INSERT INTO alert_tag_mappings (id, alert_id, tag_id, is_auto_tagged, confidence_score, created_at)
SELECT
    gen_random_uuid(),
    a.id,
    t.id,
    true,
    0.75,
    NOW()
FROM alerts a
CROSS JOIN tags t
WHERE a.alert_uuid = 'ALERT-004'
AND t.tag_name IN ('低危威胁', '性能异常', '系统监控');

INSERT INTO alert_tag_mappings (id, alert_id, tag_id, is_auto_tagged, confidence_score, created_at)
SELECT
    gen_random_uuid(),
    a.id,
    t.id,
    true,
    0.93,
    NOW()
FROM alerts a
CROSS JOIN tags t
WHERE a.alert_uuid = 'ALERT-005'
AND t.tag_name IN ('高危威胁', '数据库异常', '应用错误');