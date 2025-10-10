# 快速开始指南

## 5分钟快速上手

### 第一步：启动服务

**选项A：连接现有Kafka（推荐）**

如果你已经有运行中的Kafka（如主项目的Kafka）：

```bash
cd /home/why/code/alerts/mock/kafka-producer-service

# 方式1：使用环境变量
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
docker-compose up -d --build

# 方式2：直接在命令中指定
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 docker-compose up -d --build
```

**选项B：启动完整环境（包含Kafka）**

如果需要独立的Kafka环境：

```bash
cd /home/why/code/alerts/mock/kafka-producer-service

# 使用完整配置
docker-compose -f docker-compose-full.yml up -d --build
```

等待服务启动（约10-60秒），服务访问地址：
```
✅ 服务启动成功！

选项A（仅Producer）：
  - Producer Service API: http://localhost:8888
  - Health Check: http://localhost:8888/api/kafka/health

选项B（完整环境）：
  - Producer Service API: http://localhost:8888
  - Kafka UI: http://localhost:8080
  - Kafka Broker: localhost:9092
```

### 第二步：验证服务

打开浏览器访问：http://localhost:8888/api/kafka/health

你应该看到：
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "status": "UP",
    "service": "kafka-producer-service",
    "version": "1.0.0"
  }
}
```

### 第三步：使用Web测试页面

在浏览器中打开 `test.html` 文件（双击打开）：
```bash
# 在浏览器中打开
xdg-open test.html  # Linux
# open test.html    # macOS
# start test.html   # Windows
```

Web页面提供两种方式：
1. **从服务器文件发送** - 选择预置的JSON文件
2. **上传文件发送** - 上传你自己的JSON文件

### 第四步：发送测试消息

**方式1: 使用Web页面**
- 选择文件：`network_attack_mock_data.json`
- 设置Topic：`network-attack-alerts`
- 限制数量：`100`
- 点击"发送消息"

**方式2: 使用cURL命令**
```bash
curl -X POST http://localhost:8888/api/kafka/send-from-file \
  -H "Content-Type: application/json" \
  -d '{
    "brokerAddress": "localhost:9092",
    "topic": "test-alerts",
    "jsonFilePath": "network_attack_mock_data.json",
    "limit": 10,
    "delayMillis": 100
  }'
```

### 第五步：查看Kafka消息

**方式1: 使用Kafka UI（推荐）**
- 访问：http://localhost:8080
- 导航到 Topics → 选择你的topic
- 查看消息内容

**方式2: 使用命令行**
```bash
# 进入Kafka容器
docker exec -it kafka-producer-kafka bash

# 消费消息
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic test-alerts \
  --from-beginning \
  --max-messages 10
```

## 常用命令

### 查看服务状态
```bash
docker-compose ps
```

### 查看日志
```bash
# 查看所有日志
docker-compose logs -f

# 只看Producer服务日志
docker-compose logs -f kafka-producer-service

# 只看Kafka日志
docker-compose logs -f kafka
```

### 停止服务
```bash
docker-compose down
```

### 重启服务
```bash
docker-compose restart kafka-producer-service
```

### 清理所有数据
```bash
docker-compose down -v
```

## 发送示例数据

### 发送网络攻击告警
```bash
curl -X POST http://localhost:8888/api/kafka/send-from-file \
  -H "Content-Type: application/json" \
  -d '{
    "brokerAddress": "localhost:9092",
    "topic": "network-attack-alerts",
    "jsonFilePath": "network_attack_mock_data.json",
    "limit": 100
  }'
```

### 发送恶意样本告警
```bash
curl -X POST http://localhost:8888/api/kafka/send-from-file \
  -H "Content-Type: application/json" \
  -d '{
    "brokerAddress": "localhost:9092",
    "topic": "malicious-sample-alerts",
    "jsonFilePath": "malicious_sample_mock_data.json",
    "limit": 100
  }'
```

### 发送主机行为告警
```bash
curl -X POST http://localhost:8888/api/kafka/send-from-file \
  -H "Content-Type: application/json" \
  -d '{
    "brokerAddress": "localhost:9092",
    "topic": "host-behavior-alerts",
    "jsonFilePath": "host_behavior_mock_data.json",
    "limit": 100
  }'
```

## 上传自定义文件

```bash
curl -X POST http://localhost:8888/api/kafka/send-from-upload \
  -F "file=@/path/to/your/data.json" \
  -F "brokerAddress=localhost:9092" \
  -F "topic=custom-topic" \
  -F "limit=50"
```

## Python 脚本示例

创建 `send_to_kafka.py`:

```python
import requests
import json

def send_alerts(file_path, topic, limit=None):
    """发送告警数据到Kafka"""
    url = "http://localhost:8888/api/kafka/send-from-file"
    
    payload = {
        "brokerAddress": "localhost:9092",
        "topic": topic,
        "jsonFilePath": file_path,
        "limit": limit,
        "delayMillis": 100,
        "compressionEnabled": True,
        "acks": "all",
        "retries": 3
    }
    
    print(f"发送 {file_path} 到 topic: {topic}")
    response = requests.post(url, json=payload)
    result = response.json()
    
    if result['code'] == 200:
        stats = result['data']['statistics']
        print(f"✅ 成功: {stats['success']}/{stats['total']}")
        print(f"   吞吐量: {stats['throughput']:.2f} msg/s")
        print(f"   耗时: {result['data']['durationSeconds']:.2f}秒")
    else:
        print(f"❌ 失败: {result['message']}")

# 使用示例
send_alerts("network_attack_mock_data.json", "network-attack-alerts", 100)
send_alerts("malicious_sample_mock_data.json", "malicious-sample-alerts", 100)
send_alerts("host_behavior_mock_data.json", "host-behavior-alerts", 100)
```

运行：
```bash
python3 send_to_kafka.py
```

## 故障排查

### 问题：服务无法启动
```bash
# 查看详细日志
docker-compose logs kafka-producer-service

# 检查端口占用
netstat -tlnp | grep 8888
netstat -tlnp | grep 9092
```

### 问题：连接Kafka失败
```bash
# 检查Kafka是否运行
docker-compose ps kafka

# 测试Kafka连接
docker exec -it kafka-producer-kafka kafka-broker-api-versions \
  --bootstrap-server localhost:9092
```

### 问题：文件找不到
```bash
# 查看已挂载的文件
docker exec -it kafka-producer-service ls -la /app/data/

# 确认文件列表
curl http://localhost:8888/api/kafka/list-files
```

## 下一步

- 📖 阅读完整文档：[README.md](README.md)
- 🔧 自定义配置：编辑 `docker-compose.yml`
- 🚀 集成到你的项目：使用REST API
- 📊 监控Kafka：访问 Kafka UI (http://localhost:8080)

## 需要帮助？

查看完整文档或提交Issue。

