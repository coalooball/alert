-- =====================================================
-- 标签默认数据
-- Table is managed by JPA Entity
-- =====================================================

-- 插入默认标签数据
INSERT INTO tags (tag_name, description, tag_type, color, is_enabled, created_by) VALUES
('严重威胁', '表示严重级别的安全威胁', 'threat-level', '#f56c6c', true, (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),
('高危威胁', '表示高危级别的安全威胁', 'threat-level', '#e6a23c', true, (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),
('中危威胁', '表示中危级别的安全威胁', 'threat-level', '#909399', true, (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),
('低危威胁', '表示低危级别的安全威胁', 'threat-level', '#67c23a', true, (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),
('SQL注入', 'SQL注入攻击相关告警', 'attack-type', '#e6a23c', true, (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),
('XSS攻击', '跨站脚本攻击相关告警', 'attack-type', '#f56c6c', true, (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),
('暴力破解', '暴力破解攻击相关告警', 'attack-type', '#409eff', true, (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),
('DDoS攻击', '分布式拒绝服务攻击相关告警', 'attack-type', '#f56c6c', true, (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),
('WAF告警', '来自Web应用防火墙的告警', 'source-system', '#409eff', true, (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),
('IDS告警', '来自入侵检测系统的告警', 'source-system', '#67c23a', true, (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),
('终端告警', '来自终端防护系统的告警', 'source-system', '#e6a23c', true, (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),
('待处理', '尚未处理的告警', 'status', '#e6a23c', true, (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),
('处理中', '正在处理的告警', 'status', '#409eff', true, (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),
('已处理', '已经处理完成的告警', 'status', '#67c23a', true, (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),
('已忽略', '已忽略的告警', 'status', '#909399', true, (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),
('业务系统', '业务系统相关告警', 'business', '#909399', true, (SELECT id FROM users WHERE username = 'admin' LIMIT 1)),
('核心业务', '核心业务系统相关告警', 'business', '#f56c6c', true, (SELECT id FROM users WHERE username = 'admin' LIMIT 1))
ON CONFLICT (tag_name) DO NOTHING;