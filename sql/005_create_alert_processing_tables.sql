-- =====================================================
-- 告警智能处理系统表结构设计
-- =====================================================

-- 1. 过滤规则表
CREATE TABLE IF NOT EXISTS alert_filter_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_name VARCHAR(200) NOT NULL,
    rule_description TEXT,
    alert_type INTEGER NOT NULL, -- 1:网络攻击 2:恶意样本 3:主机行为
    alert_subtype VARCHAR(20), -- 告警子类编码
    match_field VARCHAR(100) NOT NULL, -- 匹配字段名
    match_type VARCHAR(20) NOT NULL, -- exact:精确匹配, regex:正则匹配, contains:包含
    match_value TEXT NOT NULL, -- 匹配值或正则表达式
    priority INTEGER DEFAULT 0, -- 优先级，数值越大优先级越高
    is_enabled BOOLEAN DEFAULT true,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 收敛规则表
DROP TABLE IF EXISTS alert_convergence_rules CASCADE;
CREATE TABLE alert_convergence_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_name VARCHAR(200) NOT NULL,
    rule_description TEXT,
    alert_type INTEGER NOT NULL,
    alert_subtype VARCHAR(20) NOT NULL, -- 收敛是在子类内进行

    -- 计算引擎收敛字段
    engine_fields TEXT[], -- 系统内部计算引擎处理的字段列表

    -- 机器学习收敛字段
    ml_fields TEXT[], -- 机器学习模型处理的字段列表

    -- 机器学习相关配置
    use_ml_model BOOLEAN DEFAULT false, -- 是否使用机器学习模型
    ml_model_name VARCHAR(100), -- 机器学习模型名称
    ml_model_config JSONB, -- 机器学习模型配置参数

    convergence_config JSONB, -- 收敛配置（如时间窗口、阈值等）
    time_window INTEGER DEFAULT 3600, -- 时间窗口（秒）
    min_count INTEGER DEFAULT 2, -- 最小告警数量
    priority INTEGER DEFAULT 0,
    is_enabled BOOLEAN DEFAULT true,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. 自动标签规则表
-- DROP TABLE IF EXISTS alert_tagging_rules CASCADE;
CREATE TABLE alert_tagging_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_name VARCHAR(200) NOT NULL,
    rule_description TEXT,
    alert_type INTEGER NOT NULL, -- 告警类型（必填）
    alert_subtype VARCHAR(20) NOT NULL, -- 告警子类型（必填）
    match_field VARCHAR(100) NOT NULL, -- 匹配字段
    match_type VARCHAR(20) NOT NULL CHECK (match_type IN ('exact', 'regex')), -- 匹配类型：exact精确匹配, regex正则匹配
    match_value TEXT NOT NULL, -- 匹配值
    tags TEXT[] NOT NULL, -- 要添加的标签列表
    priority INTEGER DEFAULT 0,
    is_enabled BOOLEAN DEFAULT true,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. 关联规则表
CREATE TABLE IF NOT EXISTS alert_correlation_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_name VARCHAR(200) NOT NULL,
    rule_description TEXT,
    correlation_type VARCHAR(50) NOT NULL, -- sequential:顺序关联, parallel:并行关联, hybrid:混合
    source_alert_type INTEGER NOT NULL, -- 源告警类型
    source_alert_subtype VARCHAR(20),
    target_alert_type INTEGER NOT NULL, -- 目标告警类型
    target_alert_subtype VARCHAR(20),
    correlation_fields JSONB NOT NULL, -- 关联字段映射 [{source_field: "src_ip", target_field: "dst_ip", match_type: "exact"}]
    time_window INTEGER DEFAULT 3600, -- 时间窗口（秒）
    correlation_config JSONB, -- 关联配置
    threat_level INTEGER DEFAULT 1, -- 威胁等级 1-5
    priority INTEGER DEFAULT 0,
    is_enabled BOOLEAN DEFAULT true,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. 收敛后的告警表
CREATE TABLE IF NOT EXISTS converged_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    convergence_id VARCHAR(100) UNIQUE NOT NULL, -- 收敛ID
    convergence_rule_id UUID REFERENCES alert_convergence_rules(id),
    alert_type INTEGER NOT NULL,
    alert_subtype VARCHAR(20) NOT NULL,
    alert_count INTEGER NOT NULL, -- 收敛的告警数量
    first_seen_time TIMESTAMP NOT NULL,
    last_seen_time TIMESTAMP NOT NULL,
    severity_max INTEGER, -- 最高严重级别
    convergence_data JSONB NOT NULL, -- 收敛后的数据
    original_alert_ids TEXT[], -- 原始告警ID列表
    tags TEXT[], -- 标签
    is_correlated BOOLEAN DEFAULT false, -- 是否已关联
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. 威胁事件表（简化版，详细设计待定）
CREATE TABLE IF NOT EXISTS threat_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(100) UNIQUE NOT NULL,
    event_name VARCHAR(500) NOT NULL,
    event_description TEXT,
    threat_level INTEGER NOT NULL, -- 1-5
    correlation_rule_id UUID REFERENCES alert_correlation_rules(id),
    converged_alert_ids UUID[], -- 关联的收敛告警ID列表
    event_data JSONB NOT NULL, -- 事件详细数据
    event_timeline JSONB, -- 事件时间线
    tags TEXT[],
    status VARCHAR(50) DEFAULT 'active', -- active, investigating, resolved, false_positive
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. 规则执行日志表
CREATE TABLE IF NOT EXISTS rule_execution_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_type VARCHAR(50) NOT NULL, -- filter, convergence, tagging, correlation
    rule_id UUID NOT NULL,
    execution_time TIMESTAMP NOT NULL,
    input_count INTEGER DEFAULT 0,
    output_count INTEGER DEFAULT 0,
    success BOOLEAN DEFAULT true,
    error_message TEXT,
    execution_details JSONB,
    duration_ms INTEGER, -- 执行时长（毫秒）
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_filter_rules_type ON alert_filter_rules(alert_type, alert_subtype);
CREATE INDEX idx_filter_rules_enabled ON alert_filter_rules(is_enabled);
CREATE INDEX idx_convergence_rules_type ON alert_convergence_rules(alert_type, alert_subtype);
CREATE INDEX idx_convergence_rules_enabled ON alert_convergence_rules(is_enabled);
CREATE INDEX idx_tagging_rules_enabled ON alert_tagging_rules(is_enabled);
CREATE INDEX idx_correlation_rules_types ON alert_correlation_rules(source_alert_type, target_alert_type);
CREATE INDEX idx_correlation_rules_enabled ON alert_correlation_rules(is_enabled);
CREATE INDEX idx_converged_alerts_type ON converged_alerts(alert_type, alert_subtype);
CREATE INDEX idx_converged_alerts_time ON converged_alerts(first_seen_time, last_seen_time);
CREATE INDEX idx_converged_alerts_correlated ON converged_alerts(is_correlated);
CREATE INDEX idx_threat_events_level ON threat_events(threat_level);
CREATE INDEX idx_threat_events_status ON threat_events(status);
CREATE INDEX idx_threat_events_time ON threat_events(created_at);
CREATE INDEX idx_rule_logs_type_time ON rule_execution_logs(rule_type, execution_time);

-- 添加触发器自动更新 updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_filter_rules_updated_at BEFORE UPDATE ON alert_filter_rules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_convergence_rules_updated_at BEFORE UPDATE ON alert_convergence_rules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tagging_rules_updated_at BEFORE UPDATE ON alert_tagging_rules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_correlation_rules_updated_at BEFORE UPDATE ON alert_correlation_rules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_converged_alerts_updated_at BEFORE UPDATE ON converged_alerts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_threat_events_updated_at BEFORE UPDATE ON threat_events
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();