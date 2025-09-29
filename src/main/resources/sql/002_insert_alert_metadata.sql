-- =====================================================
-- 告警元数据默认数据
-- Tables are managed by JPA Entities
-- =====================================================

-- 插入告警类型数据
INSERT INTO alert_types (id, type_name, type_label, description, display_order, is_active) VALUES
(1, 'network_attack', '网络攻击', '网络攻击类告警', 1, true),
(2, 'malicious_sample', '恶意样本', '恶意样本类告警', 2, true),
(3, 'host_behavior', '主机行为', '主机行为类告警', 3, true)
ON CONFLICT (id) DO UPDATE SET
    type_label = EXCLUDED.type_label,
    is_active = EXCLUDED.is_active,
    updated_at = CURRENT_TIMESTAMP;

-- 插入网络攻击子类型
INSERT INTO alert_subtypes (alert_type_id, subtype_code, subtype_label, display_order, is_active) VALUES
(1, '01001', '网络扫描探测', 1, true),
(1, '01002', '网络钓鱼', 2, true),
(1, '01003', '漏洞利用', 3, true),
(1, '01004', '后门通信', 4, true),
(1, '01005', '凭据攻击', 5, true),
(1, '01006', '拒绝服务', 6, true),
(1, '01007', '网页篡改', 7, true),
(1, '01008', '失陷主机', 8, true),
(1, '01009', 'APT攻击', 9, true),
(1, '01010', '其他网络攻击', 10, true)
ON CONFLICT (alert_type_id, subtype_code) DO UPDATE SET
    subtype_label = EXCLUDED.subtype_label,
    display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active,
    updated_at = CURRENT_TIMESTAMP;

-- 插入恶意样本子类型
INSERT INTO alert_subtypes (alert_type_id, subtype_code, subtype_label, display_order, is_active) VALUES
(2, '02001', '计算机病毒', 1, true),
(2, '02002', '网络蠕虫', 2, true),
(2, '02003', '特洛伊木马', 3, true),
(2, '02004', '僵尸网络', 4, true),
(2, '02005', '勒索软件', 5, true),
(2, '02006', '挖矿软件', 6, true),
(2, '02007', '其他恶意样本', 7, true)
ON CONFLICT (alert_type_id, subtype_code) DO UPDATE SET
    subtype_label = EXCLUDED.subtype_label,
    display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active,
    updated_at = CURRENT_TIMESTAMP;

-- 插入主机行为子类型
INSERT INTO alert_subtypes (alert_type_id, subtype_code, subtype_label, display_order, is_active) VALUES
(3, '03001', '挖矿攻击', 1, true),
(3, '03002', '勒索攻击', 2, true),
(3, '03003', '远控攻击', 3, true),
(3, '03004', '爆破攻击', 4, true),
(3, '03005', '后门攻击', 5, true),
(3, '03006', '注入攻击', 6, true),
(3, '03007', '横向移动攻击', 7, true),
(3, '03008', '数据窃取攻击', 8, true),
(3, '03009', '其它异常行为告警', 9, true)
ON CONFLICT (alert_type_id, subtype_code) DO UPDATE SET
    subtype_label = EXCLUDED.subtype_label,
    display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active,
    updated_at = CURRENT_TIMESTAMP;

-- 插入网络攻击字段（根据API文档完整字段）
INSERT INTO alert_fields (alert_type_id, field_name, field_label, field_type, display_order, is_active) VALUES
(1, 'alarm_id', '告警ID', 'string', 1),
(1, 'alarm_date', '告警时间', 'number', 2),
(1, 'alarm_severity', '告警等级', 'number', 3),
(1, 'alarm_name', '告警名称', 'string', 4),
(1, 'alarm_description', '告警描述', 'string', 5),
(1, 'alarm_type', '告警类型', 'number', 6),
(1, 'alarm_subtype', '告警子类型', 'number', 7),
(1, 'source', '数据源', 'number', 8),
(1, 'control_rule_id', '控制规则ID', 'string', 9),
(1, 'control_task_id', '控制任务ID', 'string', 10),
(1, 'procedure_technique_id', '技术ID列表', 'string[]', 11),
(1, 'session_id', '会话ID', 'string', 12),
(1, 'ip_version', 'IP版本', 'number', 13),
(1, 'src_ip', '源IP', 'string', 14),
(1, 'src_port', '源端口', 'number', 15),
(1, 'dst_ip', '目标IP', 'string', 16),
(1, 'dst_port', '目标端口', 'number', 17),
(1, 'protocol', '协议', 'string', 18),
(1, 'terminal_id', '终端ID', 'string', 19),
(1, 'source_file_path', '源文件路径', 'string', 20),
(1, 'signature_id', '特征ID', 'string', 21),
(1, 'attack_payload', '攻击载荷', 'string', 22),
(1, 'attack_stage', '攻击阶段', 'string', 23),
(1, 'attack_ip', '攻击IP', 'string', 24),
(1, 'attacked_ip', '被攻击IP', 'string', 25),
(1, 'apt_group', 'APT组织', 'string', 26),
(1, 'vul_type', '漏洞类型', 'string', 27),
(1, 'CVE_id', 'CVE编号', 'string', 28),
(1, 'vul_desc', '漏洞描述', 'string', 29)
ON CONFLICT (alert_type_id, field_name) DO UPDATE SET
    field_label = EXCLUDED.field_label,
    field_type = EXCLUDED.field_type,
    display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active,
    updated_at = CURRENT_TIMESTAMP;

-- 插入恶意样本字段（根据API文档完整字段）
INSERT INTO alert_fields (alert_type_id, field_name, field_label, field_type, display_order, is_active) VALUES
(2, 'alarm_id', '告警ID', 'string', 1),
(2, 'alarm_date', '告警时间', 'number', 2),
(2, 'alarm_severity', '告警等级', 'number', 3),
(2, 'alarm_name', '告警名称', 'string', 4),
(2, 'alarm_description', '告警描述', 'string', 5),
(2, 'alarm_type', '告警类型', 'number', 6),
(2, 'alarm_subtype', '告警子类型', 'number', 7),
(2, 'source', '数据源', 'number', 8),
(2, 'control_rule_id', '控制规则ID', 'string', 9),
(2, 'control_task_id', '控制任务ID', 'string', 10),
(2, 'procedure_technique_id', '技术ID列表', 'string[]', 11),
(2, 'session_id', '会话ID', 'string', 12),
(2, 'ip_version', 'IP版本', 'number', 13),
(2, 'src_ip', '源IP', 'string', 14),
(2, 'src_port', '源端口', 'number', 15),
(2, 'dst_ip', '目标IP', 'string', 16),
(2, 'dst_port', '目标端口', 'number', 17),
(2, 'protocol', '协议', 'string', 18),
(2, 'terminal_id', '终端ID', 'string', 19),
(2, 'source_file_path', '源文件路径', 'string', 20),
(2, 'sample_source', '样本来源', 'number', 21),
(2, 'md5', 'MD5', 'string', 22),
(2, 'sha1', 'SHA1', 'string', 23),
(2, 'sha256', 'SHA256', 'string', 24),
(2, 'sha512', 'SHA512', 'string', 25),
(2, 'ssdeep', 'SSDEEP', 'string', 26),
(2, 'sample_original_name', '原始文件名', 'string', 27),
(2, 'sample_description', '样本描述', 'string', 28),
(2, 'sample_family', '样本家族', 'string', 29),
(2, 'apt_group', 'APT组织', 'string', 30),
(2, 'sample_alarm_engine', '告警引擎', 'number[]', 31),
(2, 'target_platform', '目标平台', 'string', 32),
(2, 'file_type', '文件类型', 'string', 33),
(2, 'file_size', '文件大小', 'number', 34),
(2, 'language', '编程语言', 'string', 35),
(2, 'rule', '规则', 'string', 36),
(2, 'target_content', '目标内容', 'string', 37),
(2, 'compile_date', '编译日期', 'number', 38),
(2, 'last_analy_date', '最后分析日期', 'number', 39),
(2, 'sample_alarm_detail', '样本告警详情', 'string', 40)
ON CONFLICT (alert_type_id, field_name) DO UPDATE SET
    field_label = EXCLUDED.field_label,
    field_type = EXCLUDED.field_type,
    display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active,
    updated_at = CURRENT_TIMESTAMP;

-- 插入主机行为字段（根据API文档完整字段）
INSERT INTO alert_fields (alert_type_id, field_name, field_label, field_type, display_order, is_active) VALUES
(3, 'alarm_id', '告警ID', 'string', 1),
(3, 'alarm_date', '告警时间', 'number', 2),
(3, 'alarm_severity', '告警等级', 'number', 3),
(3, 'alarm_name', '告警名称', 'string', 4),
(3, 'alarm_description', '告警描述', 'string', 5),
(3, 'alarm_type', '告警类型', 'number', 6),
(3, 'alarm_subtype', '告警子类型', 'number', 7),
(3, 'source', '数据源', 'number', 8),
(3, 'control_rule_id', '控制规则ID', 'string', 9),
(3, 'control_task_id', '控制任务ID', 'string', 10),
(3, 'procedure_technique_id', '技术ID列表', 'string[]', 11),
(3, 'session_id', '会话ID', 'string', 12),
(3, 'ip_version', 'IP版本', 'number', 13),
(3, 'src_ip', '源IP', 'string', 14),
(3, 'src_port', '源端口', 'number', 15),
(3, 'dst_ip', '目标IP', 'string', 16),
(3, 'dst_port', '目标端口', 'number', 17),
(3, 'protocol', '协议', 'string', 18),
(3, 'terminal_id', '终端ID', 'string', 19),
(3, 'source_file_path', '源文件路径', 'string', 20),
(3, 'host_name', '主机名', 'string', 21),
(3, 'terminal_ip', '终端IP', 'string', 22),
(3, 'user_account', '用户账户', 'string', 23),
(3, 'terminal_os', '操作系统', 'string', 24),
(3, 'dst_process_md5', '目标进程MD5', 'string', 25),
(3, 'dst_process_path', '目标进程路径', 'string', 26),
(3, 'dst_process_cli', '目标进程命令行', 'string', 27),
(3, 'src_process_md5', '源进程MD5', 'string', 28),
(3, 'src_process_path', '源进程路径', 'string', 29),
(3, 'src_process_cli', '源进程命令行', 'string', 30),
(3, 'register_key_name', '注册表键名', 'string', 31),
(3, 'register_key_value', '注册表键值', 'string', 32),
(3, 'register_path', '注册表路径', 'string', 33),
(3, 'file_name', '文件名', 'string', 34),
(3, 'file_md5', '文件MD5', 'string', 35),
(3, 'file_path', '文件路径', 'string', 36)
ON CONFLICT (alert_type_id, field_name) DO UPDATE SET
    field_label = EXCLUDED.field_label,
    field_type = EXCLUDED.field_type,
    display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active,
    updated_at = CURRENT_TIMESTAMP;