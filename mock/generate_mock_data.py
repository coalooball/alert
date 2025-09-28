#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成三种告警类型的模拟数据
每种类型生成 10000 条数据
"""

import json
import random
import time
from datetime import datetime, timedelta

# 基础配置
NUM_RECORDS = 10000
BASE_TIMESTAMP = int(datetime(2024, 1, 1).timestamp() * 1000)
TIME_RANGE_DAYS = 365

# 网络攻击相关数据池
NETWORK_ATTACK_NAMES = [
    "APT组织Lazarus后门通信检测", "SQL注入漏洞利用尝试", "大规模端口扫描行为",
    "钓鱼邮件链接访问", "DDoS攻击-SYN洪水", "XSS跨站脚本攻击",
    "远程代码执行尝试", "暴力破解SSH服务", "DNS隧道通信检测",
    "Webshell后门检测", "命令注入攻击", "SSRF服务端请求伪造",
    "XML外部实体注入", "反序列化漏洞利用", "目录遍历攻击",
    "CSRF跨站请求伪造", "HTTP请求走私", "Redis未授权访问",
    "MySQL注入攻击", "文件包含漏洞利用"
]

NETWORK_SUBTYPES = [1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010]
APT_GROUPS = ["Lazarus Group", "APT28", "APT29", "APT41", "OceanLotus", ""]
PROTOCOLS = ["HTTP", "HTTPS", "TCP", "UDP", "DNS", "SSH", "FTP", "SMTP"]
ATTACK_STAGES = ["Reconnaissance", "Initial Access", "Execution", "Persistence",
                 "Privilege Escalation", "Defense Evasion", "Credential Access",
                 "Discovery", "Lateral Movement", "Collection", "Command and Control", "Exfiltration", "Impact"]

# 恶意样本相关数据池
MALWARE_NAMES = [
    "Emotet银行木马变种检测", "WannaCry勒索软件检测", "Mirai僵尸网络样本",
    "XMRig挖矿木马", "通用蠕虫病毒检测", "TrickBot木马检测",
    "Ryuk勒索软件", "Zeus银行木马", "CobaltStrike后门",
    "Meterpreter后门", "PlugX远控木马", "DarkComet远控",
    "njRAT远程木马", "QuasarRAT远控", "NanoCore远控木马",
    "Agent Tesla键盘记录", "FormBook信息窃取", "LokiBot木马",
    "Dridex银行木马", "IcedID恶意软件"
]

MALWARE_SUBTYPES = [2001, 2002, 2003, 2004, 2005, 2006, 2007]
MALWARE_FAMILIES = [
    "Emotet", "WannaCry", "Mirai", "XMRig", "Generic.Worm", "TrickBot",
    "Ryuk", "Zeus", "CobaltStrike", "Meterpreter", "PlugX", "DarkComet",
    "njRAT", "QuasarRAT", "NanoCore", "AgentTesla", "FormBook", "LokiBot"
]

PLATFORMS = ["Windows x86", "Windows x64", "Linux x64", "Linux ARM", "macOS", "Android"]
FILE_TYPES = ["PE32 executable", "PE32+ executable", "ELF 32-bit", "ELF 64-bit", "Mach-O", "APK"]
LANGUAGES = ["C", "C++", "C#", "Visual Basic", "Python", "Go", "Rust", "Assembly"]

# 主机行为相关数据池
HOST_BEHAVIOR_NAMES = [
    "XMRig挖矿进程检测", "勒索软件文件加密行为", "Cobalt Strike Beacon远控检测",
    "SSH暴力破解攻击", "Mimikatz凭证窃取", "PowerShell注入攻击",
    "敏感数据外泄", "后门程序持久化", "进程注入检测",
    "内存马注入", "计划任务持久化", "注册表启动项修改",
    "系统服务创建", "DLL劫持检测", "提权漏洞利用",
    "Pass-the-Hash攻击", "WMI横向移动", "PsExec远程执行",
    "SMB横向传播", "异常网络连接"
]

HOST_SUBTYPES = [3001, 3002, 3003, 3004, 3005, 3006, 3007, 3008, 3009]
OS_SYSTEMS = [
    "Windows 10", "Windows 11", "Windows Server 2016", "Windows Server 2019",
    "Ubuntu 20.04 LTS", "Ubuntu 22.04 LTS", "CentOS 7.9", "CentOS 8",
    "Red Hat Enterprise Linux 8", "Debian 11"
]

def generate_random_ip():
    """生成随机IP地址"""
    return f"{random.randint(1, 223)}.{random.randint(0, 255)}.{random.randint(0, 255)}.{random.randint(1, 254)}"

def generate_internal_ip():
    """生成内网IP地址"""
    subnet = random.choice(["192.168", "10.0", "172.16"])
    return f"{subnet}.{random.randint(0, 255)}.{random.randint(1, 254)}"

def generate_external_ip():
    """生成外网IP地址"""
    return f"{random.randint(1, 223)}.{random.randint(0, 255)}.{random.randint(0, 255)}.{random.randint(1, 254)}"

def generate_hash(length=32):
    """生成随机哈希值"""
    return ''.join(random.choices('0123456789abcdef', k=length))

def generate_timestamp():
    """生成随机时间戳"""
    days_offset = random.randint(0, TIME_RANGE_DAYS)
    return BASE_TIMESTAMP + (days_offset * 24 * 3600 * 1000) + random.randint(0, 86400000)

def generate_network_attack_record(index):
    """生成网络攻击告警记录"""
    severity = random.choices([1, 2, 3], weights=[20, 35, 45])[0]
    subtype = random.choice(NETWORK_SUBTYPES)
    has_apt = random.random() < 0.1
    has_vuln = random.random() < 0.3

    record = {
        "alarm_id": f"NA-2024-{index:06d}-{generate_hash(6).upper()}",
        "alarm_date": generate_timestamp(),
        "alarm_severity": severity,
        "alarm_name": random.choice(NETWORK_ATTACK_NAMES),
        "alarm_description": f"检测到网络攻击行为，告警编号：{index}",
        "alarm_type": 1,
        "alarm_subtype": subtype,
        "source": random.randint(0, 10),
        "control_rule_id": f"RULE-NET-2024-{random.randint(1, 999):03d}" if random.random() > 0.2 else "0",
        "control_task_id": f"TASK-SEC-2024-{random.randint(1, 500):03d}" if random.random() > 0.3 else "0",
        "procedure_technique_id": [f"T{random.randint(1000, 1600)}.{random.randint(1, 9):03d}" for _ in range(random.randint(1, 3))],
        "session_id": f"SESSION-{datetime.fromtimestamp(generate_timestamp()/1000).strftime('%Y%m%d-%H%M%S')}-{random.randint(1, 999):03d}",
        "ip_version": 4,
        "src_ip": generate_external_ip() if random.random() > 0.3 else generate_internal_ip(),
        "src_port": random.randint(1024, 65535),
        "dst_ip": generate_internal_ip(),
        "dst_port": random.choice([22, 80, 443, 3306, 3389, 8080, 21, 25]),
        "protocol": random.choice(PROTOCOLS),
        "terminal_id": f"TERM-{random.choice(['PC', 'SVR'])}-{random.randint(1, 999):03d}" if random.random() > 0.5 else "",
        "source_file_path": f"/data/traffic/2024/{random.randint(1, 12):02d}/{random.randint(1, 28):02d}/capture_{random.randint(0, 235959):06d}.pcap",
        "signature_id": f"SIG-{random.choice(['APT', 'SQLI', 'XSS', 'SCAN', 'DDOS'])}-{random.randint(1, 999):03d}",
        "attack_payload": json.dumps({"data": f"payload_{generate_hash(16)}"}),
        "attack_stage": random.choice(ATTACK_STAGES),
        "attack_ip": generate_external_ip(),
        "attacked_ip": generate_internal_ip(),
        "apt_group": random.choice(APT_GROUPS) if has_apt else "",
        "vul_type": random.choice(["SQL注入", "XSS", "命令注入", "文件包含", "反序列化", ""]) if has_vuln else "",
        "CVE_id": f"CVE-2024-{random.randint(1000, 9999)}" if has_vuln and random.random() > 0.5 else "",
        "vul_desc": "存在安全漏洞，可能被攻击者利用" if has_vuln else ""
    }
    return record

def generate_malware_record(index):
    """生成恶意样本告警记录"""
    severity = random.choices([1, 2, 3], weights=[15, 40, 45])[0]
    subtype = random.choice(MALWARE_SUBTYPES)
    has_apt = random.random() < 0.05
    has_network = random.random() < 0.4

    record = {
        "alarm_id": f"MS-2024-{index:06d}-{generate_hash(6).upper()}",
        "alarm_date": generate_timestamp(),
        "alarm_severity": severity,
        "alarm_name": random.choice(MALWARE_NAMES),
        "alarm_description": f"检测到恶意样本，告警编号：{index}",
        "alarm_type": 2,
        "alarm_subtype": subtype,
        "source": random.randint(0, 10),
        "control_rule_id": f"RULE-MAL-2024-{random.randint(1, 999):03d}" if random.random() > 0.2 else "0",
        "control_task_id": f"TASK-MAL-2024-{random.randint(1, 500):03d}" if random.random() > 0.4 else "0",
        "procedure_technique_id": [f"T{random.randint(1000, 1600)}.{random.randint(1, 9):03d}" for _ in range(random.randint(1, 3))],
        "session_id": f"SESSION-{datetime.fromtimestamp(generate_timestamp()/1000).strftime('%Y%m%d-%H%M%S')}-{random.randint(1, 999):03d}" if has_network else "",
        "ip_version": 4 if has_network else None,
        "src_ip": generate_internal_ip() if has_network else "",
        "src_port": random.randint(1024, 65535) if has_network else None,
        "dst_ip": generate_external_ip() if has_network else "",
        "dst_port": random.choice([80, 443, 8080, 4444]) if has_network else None,
        "protocol": random.choice(["HTTP", "HTTPS", "TCP"]) if has_network else "",
        "terminal_id": f"TERM-{random.choice(['PC', 'SVR'])}-{random.randint(1, 999):03d}",
        "source_file_path": f"/data/samples/2024/{random.randint(1, 12):02d}/{random.randint(1, 28):02d}/sample_{random.randint(0, 235959):06d}.{random.choice(['exe', 'dll', 'elf', 'so'])}",
        "sample_source": random.randint(0, 5),
        "md5": generate_hash(32),
        "sha1": generate_hash(40),
        "sha256": generate_hash(64),
        "sha512": generate_hash(128),
        "ssdeep": f"{random.randint(96, 12288)}:{generate_hash(random.randint(20, 40))}",
        "sample_original_name": f"{random.choice(['setup', 'update', 'install', 'invoice', 'document', 'svchost', 'explorer'])}.{random.choice(['exe', 'dll', 'scr', 'bat'])}",
        "sample_description": "",
        "sample_family": random.choice(MALWARE_FAMILIES),
        "apt_group": random.choice(APT_GROUPS) if has_apt else "",
        "sample_alarm_engine": random.sample([1, 2, 3, 4, 5], k=random.randint(1, 3)),
        "target_platform": random.choice(PLATFORMS),
        "file_type": random.choice(FILE_TYPES),
        "file_size": random.randint(10240, 10485760),
        "language": random.choice(LANGUAGES),
        "rule": f"YARA:{random.choice(MALWARE_FAMILIES)}_{random.randint(1, 100)}, AV:Trojan.{random.choice(['Win32', 'Win64', 'Linux'])}.{random.choice(MALWARE_FAMILIES)}",
        "target_content": random.choice(["", f"String: {generate_hash(20)}", "Suspicious API calls detected"]),
        "compile_date": generate_timestamp() - random.randint(86400000, 31536000000),
        "last_analy_date": generate_timestamp(),
        "sample_alarm_detail": json.dumps([{"av_engine_name": f"Engine{i}", "av_label": f"Trojan.{random.choice(MALWARE_FAMILIES)}"} for i in range(random.randint(1, 3))])
    }
    return record

def generate_host_behavior_record(index):
    """生成主机行为告警记录"""
    severity = random.choices([1, 2, 3], weights=[25, 35, 40])[0]
    subtype = random.choice(HOST_SUBTYPES)
    is_windows = random.random() > 0.3
    has_network = random.random() < 0.5
    has_registry = is_windows and random.random() < 0.3

    record = {
        "alarm_id": f"HB-2024-{index:06d}-{generate_hash(6).upper()}",
        "alarm_date": generate_timestamp(),
        "alarm_severity": severity,
        "alarm_name": random.choice(HOST_BEHAVIOR_NAMES),
        "alarm_description": f"检测到主机异常行为，告警编号：{index}",
        "alarm_type": 3,
        "alarm_subtype": subtype,
        "source": random.randint(0, 10),
        "control_rule_id": f"RULE-HOST-2024-{random.randint(1, 999):03d}" if random.random() > 0.3 else "0",
        "control_task_id": f"TASK-HOST-2024-{random.randint(1, 500):03d}" if random.random() > 0.4 else "0",
        "procedure_technique_id": [f"T{random.randint(1000, 1600)}.{random.randint(1, 9):03d}" for _ in range(random.randint(1, 3))],
        "session_id": "",
        "ip_version": 4 if has_network else None,
        "src_ip": generate_internal_ip() if has_network and random.random() > 0.5 else "",
        "src_port": random.randint(1024, 65535) if has_network and random.random() > 0.5 else None,
        "dst_ip": generate_external_ip() if has_network else "",
        "dst_port": random.choice([80, 443, 4444, 8080, 21]) if has_network else None,
        "protocol": random.choice(["HTTP", "HTTPS", "TCP", "SSH", "FTP"]) if has_network else "",
        "terminal_id": f"TERM-{random.choice(['PC', 'SVR', 'DC'])}-{random.randint(1, 999):03d}",
        "source_file_path": f"/data/logs/2024/{random.randint(1, 12):02d}/{random.randint(1, 28):02d}/host_{random.randint(0, 235959):06d}.log",
        "host_name": f"{random.choice(['WEB', 'DB', 'APP', 'DC', 'FILE'])}-SERVER-{random.randint(1, 99):02d}" if not is_windows or random.random() > 0.5 else f"{random.choice(['HR', 'FIN', 'IT', 'SALES'])}-PC-{random.randint(1, 999):03d}",
        "terminal_ip": generate_internal_ip(),
        "user_account": random.choice(["root", "admin", "www-data", "SYSTEM"]) if random.random() > 0.3 else f"user{random.randint(1, 999):03d}",
        "terminal_os": random.choice(OS_SYSTEMS),
        "dst_process_md5": generate_hash(32) if random.random() > 0.3 else "",
        "dst_process_path": f"C:\\Windows\\System32\\{random.choice(['cmd.exe', 'powershell.exe', 'rundll32.exe', 'svchost.exe'])}" if is_windows else f"/usr/bin/{random.choice(['bash', 'python3', 'curl', 'wget'])}",
        "dst_process_cli": f"Process command line {generate_hash(20)}",
        "src_process_md5": generate_hash(32) if random.random() > 0.5 else "",
        "src_process_path": f"C:\\Program Files\\{random.choice(['Microsoft Office', 'Common Files'])}\\app.exe" if is_windows else f"/usr/sbin/{random.choice(['sshd', 'httpd', 'nginx'])}",
        "src_process_cli": "" if random.random() > 0.5 else f"Parent process {generate_hash(10)}",
        "register_key_name": f"SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\{random.choice(['Run', 'RunOnce'])}" if has_registry else "",
        "register_key_value": f"C:\\ProgramData\\{random.choice(['update', 'service', 'system'])}.exe" if has_registry else "",
        "register_path": f"HKEY_{random.choice(['CURRENT_USER', 'LOCAL_MACHINE'])}\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run" if has_registry else "",
        "file_name": f"{random.choice(['update', 'config', 'data', 'temp'])}.{random.choice(['exe', 'dll', 'dat', 'tmp'])}" if random.random() > 0.3 else "",
        "file_md5": generate_hash(32) if random.random() > 0.4 else "",
        "file_path": f"C:\\Temp\\{random.choice(['file', 'data', 'temp'])}{random.randint(1, 999)}.exe" if is_windows else f"/tmp/.{random.choice(['cache', 'system', 'update'])}/{generate_hash(8)}"
    }
    return record

def generate_data(data_type, num_records):
    """生成指定类型的数据"""
    print(f"开始生成 {data_type} 数据，共 {num_records} 条...")

    if data_type == "network_attack":
        generator = generate_network_attack_record
        filename = "network_attack_mock_data.json"
    elif data_type == "malicious_sample":
        generator = generate_malware_record
        filename = "malicious_sample_mock_data.json"
    elif data_type == "host_behavior":
        generator = generate_host_behavior_record
        filename = "host_behavior_mock_data.json"
    else:
        raise ValueError(f"Unknown data type: {data_type}")

    records = []
    batch_size = 1000

    for i in range(1, num_records + 1):
        records.append(generator(i))

        if i % batch_size == 0:
            print(f"  已生成 {i}/{num_records} 条记录...")

    output_path = f"/home/why/code/alerts/mock/{filename}"
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(records, f, ensure_ascii=False, indent=2)

    print(f"✓ {data_type} 数据生成完成，保存到: {output_path}")
    print(f"  文件大小: {len(json.dumps(records, ensure_ascii=False)) / 1024 / 1024:.2f} MB\n")

def main():
    """主函数"""
    print("="*60)
    print("告警数据模拟生成工具")
    print("="*60)
    print()

    start_time = time.time()

    # 生成三种类型的数据
    generate_data("network_attack", NUM_RECORDS)
    generate_data("malicious_sample", NUM_RECORDS)
    generate_data("host_behavior", NUM_RECORDS)

    elapsed_time = time.time() - start_time
    print("="*60)
    print(f"全部数据生成完成！")
    print(f"总计: {NUM_RECORDS * 3} 条记录")
    print(f"耗时: {elapsed_time:.2f} 秒")
    print("="*60)

if __name__ == "__main__":
    main()