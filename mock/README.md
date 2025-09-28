# 告警数据模拟数据集

本目录包含三种告警类型的模拟数据，每种类型各 10,000 条记录，用于测试和开发。

## 数据文件

### 1. network_attack_mock_data.json
- **类型**: 网络攻击告警 (alarm_type: 1)
- **记录数**: 10,000 条
- **文件大小**: ~11 MB
- **子类型**: 包含 10 种网络攻击子类型 (1001-1010)
  - 1001: 网络扫描探测
  - 1002: 网络钓鱼
  - 1003: 漏洞利用
  - 1004: 后门通信
  - 1005: 凭据攻击
  - 1006: 拒绝服务
  - 1007: 网页篡改
  - 1008: 失陷主机
  - 1009: APT攻击
  - 1010: 其他网络攻击

**主要特征**:
- 包含源/目标IP地址和端口信息
- 网络协议类型 (HTTP, HTTPS, TCP, UDP, DNS, SSH, FTP, SMTP)
- APT组织信息 (约10%的记录)
- 漏洞信息 (约30%的记录包含CVE编号)
- 攻击阶段 (基于MITRE ATT&CK框架)
- 时间范围: 2024年全年

### 2. malicious_sample_mock_data.json
- **类型**: 恶意样本告警 (alarm_type: 2)
- **记录数**: 10,000 条
- **文件大小**: ~17 MB
- **子类型**: 包含 7 种恶意样本子类型 (2001-2007)
  - 2001: 计算机病毒
  - 2002: 网络蠕虫
  - 2003: 特洛伊木马
  - 2004: 僵尸网络
  - 2005: 勒索软件
  - 2006: 挖矿软件
  - 2007: 其他恶意样本

**主要特征**:
- 完整的哈希值信息 (MD5, SHA1, SHA256, SHA512, SSDEEP)
- 样本家族分类 (Emotet, WannaCry, Mirai, XMRig等)
- 目标平台 (Windows, Linux, macOS, Android)
- 文件类型和大小
- 编程语言
- 检测引擎告警详情
- 网络通信信息 (约40%的记录)
- APT组织关联 (约5%的记录)
- 时间范围: 2024年全年

### 3. host_behavior_mock_data.json
- **类型**: 主机行为告警 (alarm_type: 3)
- **记录数**: 10,000 条
- **文件大小**: ~14 MB
- **子类型**: 包含 9 种主机行为子类型 (3001-3009)
  - 3001: 挖矿攻击
  - 3002: 勒索攻击
  - 3003: 远控攻击
  - 3004: 爆破攻击
  - 3005: 后门攻击
  - 3006: 注入攻击
  - 3007: 横向移动攻击
  - 3008: 数据窃取攻击
  - 3009: 其它异常行为告警

**主要特征**:
- 主机名和终端IP信息
- 操作系统类型 (Windows, Ubuntu, CentOS, RHEL, Debian)
- 用户账户信息
- 进程信息 (路径、命令行、MD5哈希)
- 父子进程关系
- 注册表操作 (Windows系统，约30%的记录)
- 文件操作信息
- 网络连接信息 (约50%的记录)
- 时间范围: 2024年全年

## 数据生成规则

### 告警等级分布
- **等级 1 (低危)**: 15-25%
- **等级 2 (中危)**: 35-40%
- **等级 3 (高危)**: 40-45%

### 时间戳
- 基准时间: 2024-01-01
- 时间范围: 365天
- 随机分布在全年各个时间点

### IP地址
- **内网IP**: 192.168.x.x, 10.0.x.x, 172.16.x.x
- **外网IP**: 公网IP地址段

### 其他特征
- 所有字段符合 `alert-interface/` 目录下的接口规范
- 哈希值、ID等均为随机生成
- MITRE ATT&CK技术ID: T1000-T1600

## 使用方法

### Python 读取示例
```python
import json

# 读取网络攻击数据
with open('network_attack_mock_data.json', 'r', encoding='utf-8') as f:
    network_attacks = json.load(f)

print(f"共有 {len(network_attacks)} 条网络攻击告警")
print(f"第一条记录: {network_attacks[0]}")
```

### 统计分析示例
```python
import json
from collections import Counter

with open('network_attack_mock_data.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

# 统计告警等级分布
severity_dist = Counter(record['alarm_severity'] for record in data)
print("告警等级分布:", severity_dist)

# 统计子类型分布
subtype_dist = Counter(record['alarm_subtype'] for record in data)
print("子类型分布:", subtype_dist)
```

## 重新生成数据

如需重新生成数据，运行以下命令：

```bash
python3 generate_mock_data.py
```

脚本会生成新的 10,000 条记录，覆盖现有文件。

## 注意事项

⚠️ **重要提示**:
- 本数据集仅用于测试和开发目的
- 所有数据均为模拟生成，不包含真实的敏感信息
- 哈希值、IP地址、域名等均为随机生成
- 不应将此数据用于生产环境

## 生成信息

- **生成时间**: 2024-09-28
- **总记录数**: 30,000 条
- **总文件大小**: ~42 MB
- **生成工具**: generate_mock_data.py
- **Python版本**: 3.x

## 数据字段说明

详细的字段说明请参考 `alert-interface/` 目录下的接口文档：
- `network-attack-fields.json` - 网络攻击字段说明
- `malicious-sample-fields.json` - 恶意样本字段说明
- `host-behavior-fields.json` - 主机行为字段说明