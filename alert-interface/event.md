# 威胁事件接口文档

## 1.1 F3GJ-F3SJ原始威胁事件推送

### 1.1.1 通信方法特性

- **通信协议**: 消息队列
- **服务器访问地址**: ip:port
- **请求方式**: Kafka
- **消息队列数据类型**: json
- **接口调用频率**: 实时

### 1.1.2 协议特性

该接口使用消息队列Kafka传递数据，数据格式为json类型。

### 1.1.3 数据元素特性

#### 1. 消息队列数据

##### 数据样例

```json
{
  "id": 1000001,
  "system_code": "SYS-2025-001",
  "name": "APT攻击事件样例",
  "description": "模拟APT组织通过钓鱼邮件投递恶意文档，利用系统漏洞入侵受害者网络。",
  "type": "网络攻击",
  "attacker": "APT-XYZ组织",
  "victimer": "某能源企业",
  "start_time": "2025-09-10 08:30:00",
  "end_time": "2025-09-10 12:45:00",
  "found_time": "2025-09-10 09:00:00",
  "source": "IDS/威胁情报平台",
  "mitre_technique_id": "T1566,T1203,T1059",
  "attsck_list": "鱼叉式钓鱼,利用漏洞,脚本执行",
  "attack_tool": "Cobalt Strike, Metasploit",
  "first_found_time": "2025-09-10 08:45:00",
  "priority": "高",
  "severity": "严重",
  "dispose_status": "处置中",
  "app": "Microsoft Word, Apache Tomcat",
  "impact_assessment": "可能导致核心业务系统数据泄露",

  "merge_alerts": [
    {
      "alert_id": "AL-001",
      "alert_type": "钓鱼邮件",
      "alert_time": "2025-09-10 08:35:00"
    },
    {
      "alert_id": "AL-002",
      "alert_type": "漏洞利用",
      "alert_time": "2025-09-10 08:50:00"
    }
  ],
  "threat_actor": [
    {
      "name": "APT-XYZ",
      "country": "未知",
      "group": "APT组织"
    }
  ],
  "org": [
    {
      "name": "某能源企业总部",
      "location": "北京"
    }
  ],

  "attack_asset_ip": ["192.168.10.15", "45.67.89.101"],
  "victim_asset_ip": ["10.0.5.20", "172.16.3.45"],
  "attack_asset_ip_port": ["192.168.10.15:443", "45.67.89.101:80"],
  "victim_asset_ip_port": ["10.0.5.20:8080", "172.16.3.45:445"],
  "attack_asset_domain": ["evil-apt.com", "malicious.cn"],
  "victim_asset_domain": ["victim-energy.com"],
  "attack_url": ["http://evil-apt.com/phishing.doc", "http://malicious.cn/exploit"],
  "victim_url": ["http://victim-energy.com/login"],
  "attack_malware": ["Trojan.Win32.APTXYZ", "Backdoor.Linux.Cobalt"],
  "attack_malware_sample": ["hash:123abc...", "hash:456def..."],
  "attack_malware_sample_family": ["Cobalt Strike", "PlugX"],
  "attack_email_address": ["aptxyz@evil-apt.com", "attacker@phish.cn"],
  "victim_email_address": ["admin@victim-energy.com", "it@victim-energy.com"],
  "attack_email": ["钓鱼邮件主题：紧急安全更新"],
  "victim_email": ["回复邮件：确认收到安全更新"],
  "attack_software": ["Cobalt Strike", "Mimikatz"],
  "victim_software": ["Windows Server 2019", "Oracle Database 12c"],
  "attack_vulnerability": ["CVE-2023-21768", "CVE-2024-12345"],
  "attack_certificate": ["恶意证书 SHA1: abcd1234efgh5678"],
  "victim_certificate": ["企业证书 SHA1: xyz9876abcd5432"]
}
```

##### 数据元素特征

| 序号 | 名称/标识 | 数据类型 | 大小和格式/单位 | 范围/枚举 | 准确性/精度 | 是否必填 | 说明 |
|------|-----------|----------|----------------|-----------|-------------|----------|------|
| 1 | id | bigint | 64/字节 | — | — | 是 | |
| 2 | 系统编号system_code | varchar | 32/字节 | — | — | 是 | |
| 3 | 名称name | varchar | 256/字节 | — | — | 是 | |
| 4 | 描述description | varchar | 256/字节 | — | — | 是 | |
| 5 | 分类type | varchar | 32/字节 | — | — | 是 | |
| 6 | 攻击者 attacker | varchar | 32/字节 | — | — | 是 | |
| 7 | 受害者victimer | varchar | 32/字节 | — | — | 是 | |
| 8 | 事件开始时间start_time | datetime | — | — | — | 是 | |
| 9 | 事件结束时间end_time | datetime | — | — | — | 是 | |
| 10 | 监测时间found_time | datetime | — | — | — | 是 | |
| 11 | 数据来源source | varchar | — | — | — | 是 | |
| 12 | att&ck_id mitre_technique_id | text | — | — | — | 是 | |
| 13 | 技战法列表 attsck_list | varchar | — | — | — | 是 | |
| 14 | 攻击工具 attack_tool | varchar | — | — | — | 是 | |
| 15 | 首次发现时间first_found_time | datetime | — | — | — | 是 | |
| 16 | 优先级 priority | varchar | — | — | — | 是 | |
| 17 | 等级severity | varchar | — | — | — | 是 | |
| 18 | 处置状态 dispose_status | varchar | — | — | — | 是 | |
| 19 | 应用程序app | varchar | — | — | — | 否 | |
| 20 | 影响评估impact_assessment | varchar | — | — | — | 否 | |
| 21 | 收敛告警列表merge_alerts | | — | — | — | 否 | |
| 22 | 威胁主体列表threat_actor | json数组 | — | — | — | 否 | |
| 23 | 威胁目标org | json数组 | — | — | — | 否 | |
| 24 | 主要攻击者IP列表attack_asset_ip | json数组 | — | — | — | 否 | |
| 25 | 主要被攻击者IP列表victim_asset_ip | json数组 | — | — | — | 否 | |
| 26 | 主要攻击者端口列表attack_asset_ip_port | json数组 | — | — | — | 否 | |
| 27 | 主要被攻击者端口列表victim_asset_ip_port | json数组 | — | — | — | 否 | |
| 28 | 主要攻击者域名列表attack_asset_domain | json数组 | — | — | — | 否 | |
| 29 | 主要被攻击者域名列表victim_asset_domain | json数组 | — | — | — | 否 | |
| 30 | 主要攻击者URL列表attack_url | json数组 | — | — | — | 否 | |
| 31 | 主要被攻击者URL列表victim_url | json数组 | — | — | — | 否 | |
| 32 | 主要攻击者恶意代码列表attack_malware | json数组 | — | — | — | 否 | |
| 33 | 主要攻击者样本列表attack_malware_sample | json数组 | — | — | — | 否 | |
| 34 | 主要攻击者样本家族列表attack_malware_sample_family | json数组 | — | — | — | 否 | |
| 35 | 主要攻击者邮箱列表attack_email_address | json数组 | — | — | — | 否 | |
| 36 | 主要被攻击者邮箱列表victim_email_address | json数组 | — | — | — | 否 | |
| 37 | 主要攻击者邮件列表attack_email | json数组 | — | — | — | 否 | |
| 38 | 主要被攻击者邮件列表victim_email | json数组 | — | — | — | 否 | |
| 39 | 主要攻击者软件列表attack_software | json数组 | — | — | — | 否 | |
| 40 | 主要被攻击者软件列表victim_software | json数组 | — | — | — | 否 | |
| 41 | 主要攻击者漏洞列表attack_vulnerability | json数组 | — | — | — | 否 | |
| 42 | 主要攻击者证书列表attack_certificate | json数组 | — | — | — | 否 | |
| 43 | 主要被攻击者证书列表victim_certificate | json数组 | — | — | — | 否 | |