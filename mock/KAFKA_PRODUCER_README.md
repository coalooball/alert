# Kafka Producer - 告警数据发送工具

## 功能说明

`kafka_producer.py` 是一个用于读取JSON格式的告警数据并发送到Kafka的Python脚本。

## Kafka配置

脚本使用以下Kafka配置（基于 `005_insert_default_datasource_config.sql`）：

| 告警类型 | Topic名称 | JSON文件 |
|---------|----------|---------|
| 网络攻击 | `network-attack-alerts` | `network_attack_mock_data.json` |
| 恶意样本 | `malicious-sample-alerts` | `malicious_sample_mock_data.json` |
| 主机行为 | `host-behavior-alerts` | `host_behavior_mock_data.json` |

默认Kafka地址: `localhost:9092`
安全协议: `PLAINTEXT`
数据格式: `JSON`

## 依赖安装

```bash
pip install kafka-python
```

或使用requirements.txt:

```bash
# 创建requirements.txt
cat > requirements.txt << EOF
kafka-python==2.0.2
EOF

# 安装依赖
pip install -r requirements.txt
```

## 使用方法

### 基本用法

```bash
# 发送网络攻击告警数据
./kafka_producer.py --type network_attack

# 发送恶意样本告警数据
./kafka_producer.py --type malicious_sample

# 发送主机行为告警数据
./kafka_producer.py --type host_behavior

# 发送所有类型的告警数据
./kafka_producer.py --all
```

### 高级用法

```bash
# 只发送前100条数据
./kafka_producer.py --type network_attack --limit 100

# 指定Kafka服务器地址
./kafka_producer.py --type malicious_sample --broker 192.168.1.100:9092

# 设置消息间延迟（秒）
./kafka_producer.py --type host_behavior --delay 0.5

# 指定数据文件目录
./kafka_producer.py --type network_attack --data-dir /path/to/data

# 组合使用
./kafka_producer.py --all --limit 50 --delay 0.2
```

### 参数说明

| 参数 | 说明 | 默认值 |
|-----|------|--------|
| `--broker` | Kafka服务器地址 | `localhost:9092` |
| `--type` | 告警类型 (`network_attack`, `malicious_sample`, `host_behavior`) | 无 |
| `--all` | 发送所有类型的告警 | False |
| `--limit` | 限制发送的消息数量 | 无限制 |
| `--delay` | 消息间延迟（秒） | 0.1 |
| `--data-dir` | JSON数据文件目录 | 脚本所在目录 |

## 示例输出

```
✅ Connected to Kafka broker: localhost:9092

============================================================
📋 Processing: 网络攻击告警
   Topic: network-attack-alerts
   File: network_attack_mock_data.json
============================================================
✅ Loaded 10000 records from network_attack_mock_data.json

📤 Sending 10000 messages to topic: network-attack-alerts
   Progress: 100/10000 messages sent...
   Progress: 200/10000 messages sent...
   ...

✅ Completed: 10000 success, 0 failed

============================================================
📊 Summary
============================================================
Total messages: 10000
✅ Success: 10000
❌ Failed: 0
Success rate: 100.00%
✅ Kafka producer closed
```

## 测试Kafka连接

在运行脚本前，确保Kafka服务正在运行：

```bash
# 使用Docker启动Kafka（如果尚未启动）
docker-compose -f docker-compose/kafka-compose.yml up -d

# 检查Kafka是否运行
docker ps | grep kafka

# 检查topic是否存在
kafka-topics.sh --bootstrap-server localhost:9092 --list

# 创建所需的topic（如果不存在）
kafka-topics.sh --bootstrap-server localhost:9092 --create --topic network-attack-alerts --partitions 3 --replication-factor 1
kafka-topics.sh --bootstrap-server localhost:9092 --create --topic malicious-sample-alerts --partitions 3 --replication-factor 1
kafka-topics.sh --bootstrap-server localhost:9092 --create --topic host-behavior-alerts --partitions 3 --replication-factor 1
```

## 消费消息验证

发送消息后，可以使用Kafka消费者验证：

```bash
# 消费网络攻击告警
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic network-attack-alerts --from-beginning --max-messages 10

# 消费恶意样本告警
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic malicious-sample-alerts --from-beginning --max-messages 10

# 消费主机行为告警
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic host-behavior-alerts --from-beginning --max-messages 10
```

## 故障排查

### 连接失败

```
❌ Failed to connect to Kafka: NoBrokersAvailable
```

**解决方案**:
- 检查Kafka是否运行: `docker ps | grep kafka`
- 检查网络连接和防火墙设置
- 验证broker地址是否正确

### Topic不存在

```
❌ Failed to send message: UnknownTopicOrPartitionError
```

**解决方案**:
- 创建对应的topic（见上方"测试Kafka连接"部分）
- 或者配置Kafka自动创建topic: `auto.create.topics.enable=true`

### 文件未找到

```
❌ File not found: /path/to/file.json
```

**解决方案**:
- 确认JSON数据文件存在
- 使用`--data-dir`参数指定正确的目录
- 检查文件权限

## 性能调优

### 批量发送

```python
# 修改脚本中的batch_size参数（需要编辑脚本）
producer = KafkaProducer(
    batch_size=16384,  # 增加批量大小
    linger_ms=10,      # 等待更多消息批处理
    buffer_memory=33554432  # 增加缓冲区
)
```

### 并行发送

```bash
# 使用多个终端并行发送不同类型
# Terminal 1
./kafka_producer.py --type network_attack &

# Terminal 2
./kafka_producer.py --type malicious_sample &

# Terminal 3
./kafka_producer.py --type host_behavior &
```

## 注意事项

1. ⚠️ **数据量**: JSON文件较大（10MB+），发送所有数据可能需要几分钟
2. ⚠️ **内存使用**: 脚本会将整个JSON文件加载到内存中
3. ⚠️ **网络流量**: 注意Kafka服务器的网络带宽和存储空间
4. ⚠️ **消息顺序**: 使用message key确保相同告警ID的消息有序

## 集成到告警系统

脚本发送的数据会被告警系统的Kafka Consumer接收并处理：

1. 数据源配置在 `kafka_datasource_config` 表中
2. Consumer Group:
   - 网络攻击: `alert-system-network-group`
   - 恶意样本: `alert-system-malware-group`
   - 主机行为: `alert-system-host-group`
3. 数据格式: JSON
4. 自动偏移量重置: `latest`

## 开发者信息

- 脚本位置: `/home/why/code/alerts/mock/kafka_producer.py`
- 配置参考: `/home/why/code/alerts/src/main/resources/sql/005_insert_default_datasource_config.sql`
- 数据文件: `/home/why/code/alerts/mock/*.json`