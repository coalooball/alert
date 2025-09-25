-- =====================================================
-- 标签管理系统表结构设计
-- =====================================================

-- 标签表
CREATE TABLE IF NOT EXISTS tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tag_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    tag_type VARCHAR(50) NOT NULL, -- threat-level:威胁级别, attack-type:攻击类型, source-system:来源系统, status:处理状态, business:业务分类, custom:自定义
    color VARCHAR(7) NOT NULL DEFAULT '#409eff', -- 标签颜色，格式为十六进制颜色值 #RRGGBB
    is_enabled BOOLEAN DEFAULT true,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_tags_tag_name ON tags(tag_name);
CREATE INDEX IF NOT EXISTS idx_tags_tag_type ON tags(tag_type);
CREATE INDEX IF NOT EXISTS idx_tags_is_enabled ON tags(is_enabled);
CREATE INDEX IF NOT EXISTS idx_tags_created_by ON tags(created_by);
CREATE INDEX IF NOT EXISTS idx_tags_created_at ON tags(created_at);

-- 创建更新时间触发器函数（如果不存在）
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为标签表添加更新时间触发器
CREATE TRIGGER update_tags_updated_at
    BEFORE UPDATE ON tags
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

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

-- 添加表注释
COMMENT ON TABLE tags IS '标签管理表';
COMMENT ON COLUMN tags.id IS '标签唯一标识';
COMMENT ON COLUMN tags.tag_name IS '标签名称';
COMMENT ON COLUMN tags.description IS '标签描述';
COMMENT ON COLUMN tags.tag_type IS '标签类型：threat-level威胁级别/attack-type攻击类型/source-system来源系统/status处理状态/business业务分类/custom自定义';
COMMENT ON COLUMN tags.color IS '标签颜色，十六进制格式#RRGGBB';
COMMENT ON COLUMN tags.is_enabled IS '是否启用';
COMMENT ON COLUMN tags.created_by IS '创建用户ID';
COMMENT ON COLUMN tags.created_at IS '创建时间';
COMMENT ON COLUMN tags.updated_at IS '更新时间';