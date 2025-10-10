# Kafka Producer Service

独立的Kafka生产者服务，用于将JSON文件数据发送到Kafka Topic。提供REST API接口和精美的Vue3前端界面，支持文件上传和服务器端文件读取两种方式。

## 功能特性

### 后端功能
- ✅ 从服务器端JSON文件读取并发送到Kafka
- ✅ 支持文件上传（HTTP Multipart）后发送到Kafka
- ✅ 可配置Kafka连接参数（Broker地址、Topic等）
- ✅ 支持消息限制数量
- ✅ 可调节消息发送延迟
- ✅ 支持Gzip压缩
- ✅ 批量发送优化
- ✅ 实时统计信息（成功/失败数、吞吐量等）
- ✅ Docker容器化部署
- ✅ 完整的docker-compose编排

### 前端界面
- ✅ Vue 3 + Element Plus 精美UI
- ✅ 渐变色设计，现代化界面
- ✅ 文件拖拽上传
- ✅ 实时服务状态监控
- ✅ 详细的发送统计展示
- ✅ 响应式布局
- ✅ 与后端自动集成

## 快速开始

### 方式一：连接现有 Kafka（推荐）

如果你已经有运行中的Kafka服务（例如主项目的Kafka）：

```bash
# 1. 进入项目目录
cd kafka-producer-service

# 2. 设置Kafka地址并启动服务
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
docker-compose up -d --build

# 3. 查看服务状态
docker-compose ps

# 4. 查看日志
docker-compose logs -f kafka-producer-service
```

服务启动后可以访问：
- **Producer Service API**: http://localhost:8888

### 方式二：启动完整环境（包含 Kafka）

如果需要独立的完整Kafka环境：

```bash
# 使用完整配置文件
docker-compose -f docker-compose-full.yml up -d --build
```

这将启动：
- **Producer Service API**: http://localhost:8888
- **Kafka Broker**: localhost:9092
- **Kafka UI**: http://localhost:8080
- **Zookeeper**: localhost:2181

### 方式三：本地开发运行

需要预装：JDK 17、Maven 3.x

```bash
# 1. 编译项目
mvn clean package

# 2. 运行应用
java -jar target/kafka-producer-service.jar

# 或者直接使用 Maven 运行
mvn spring-boot:run
```

## API 接口文档

### 1. 健康检查

**接口**: `GET /api/kafka/health`

**响应示例**:
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "status": "UP",
    "service": "kafka-producer-service",
    "version": "1.0.0",
    "timestamp": 1234567890
  }
}
```

### 2. 获取服务信息

**接口**: `GET /api/kafka/info`

**响应**: 返回服务的基本信息和可用端点列表

### 3. 列出数据文件

**接口**: `GET /api/kafka/list-files`

**描述**: 列出服务器 `/app/data` 目录下所有的 `.json` 文件

**响应示例**:
```json
{
  "code": 200,
  "message": "获取文件列表成功",
  "data": [
    "network_attack_mock_data.json",
    "malicious_sample_mock_data.json",
    "host_behavior_mock_data.json"
  ]
}
```

### 4. 从服务器文件发送消息

**接口**: `POST /api/kafka/send-from-file`

**请求体**:
```json
{
  "brokerAddress": "localhost:9092",
  "topic": "network-attack-alerts",
  "jsonFilePath": "network_attack_mock_data.json",
  "limit": 1000,
  "delayMillis": 100,
  "messageKeyField": "alarm_id",
  "compressionEnabled": true,
  "acks": "all",
  "retries": 3
}
```

**参数说明**:
- `brokerAddress`: Kafka Broker地址（必填）
- `topic`: Kafka Topic名称（必填）
- `jsonFilePath`: JSON文件路径，相对于 `/app/data` 目录（必填）
- `limit`: 限制发送消息数量（可选，不填则全部发送）
- `delayMillis`: 消息间延迟（毫秒，默认100）
- `messageKeyField`: 用作消息Key的字段名（默认 "alarm_id"）
- `compressionEnabled`: 是否启用Gzip压缩（默认true）
- `acks`: ACK模式，可选值：all, 1, 0（默认all）
- `retries`: 重试次数（默认3）

**响应示例**:
```json
{
  "code": 200,
  "message": "消息发送成功",
  "data": {
    "success": true,
    "message": "消息发送完成",
    "statistics": {
      "total": 1000,
      "success": 1000,
      "failed": 0,
      "successRate": 100.0,
      "throughput": 250.5
    },
    "startTime": "2024-10-10T10:00:00",
    "endTime": "2024-10-10T10:00:04",
    "durationSeconds": 3.99
  }
}
```

### 5. 从上传文件发送消息

**接口**: `POST /api/kafka/send-from-upload`

**Content-Type**: `multipart/form-data`

**请求参数**:
- `file`: 上传的JSON文件（必填）
- `brokerAddress`: Kafka Broker地址（必填）
- `topic`: Kafka Topic名称（必填）
- `limit`: 限制数量（可选）
- `delayMillis`: 延迟毫秒（默认100）
- `messageKeyField`: 消息Key字段（默认 "alarm_id"）
- `compressionEnabled`: 启用压缩（默认true）
- `acks`: ACK模式（默认all）
- `retries`: 重试次数（默认3）

**cURL 示例**:
```bash
curl -X POST http://localhost:8888/api/kafka/send-from-upload \
  -F "file=@network_attack_mock_data.json" \
  -F "brokerAddress=localhost:9092" \
  -F "topic=network-attack-alerts" \
  -F "limit=100"
```

## 使用示例

### 示例1: 发送网络攻击告警数据

```bash
curl -X POST http://localhost:8888/api/kafka/send-from-file \
  -H "Content-Type: application/json" \
  -d '{
    "brokerAddress": "localhost:9092",
    "topic": "network-attack-alerts",
    "jsonFilePath": "network_attack_mock_data.json",
    "limit": 100,
    "delayMillis": 50
  }'
```

### 示例2: 发送恶意样本告警数据

```bash
curl -X POST http://localhost:8888/api/kafka/send-from-file \
  -H "Content-Type: application/json" \
  -d '{
    "brokerAddress": "localhost:9092",
    "topic": "malicious-sample-alerts",
    "jsonFilePath": "malicious_sample_mock_data.json",
    "limit": 500
  }'
```

### 示例3: 上传并发送自定义JSON文件

```bash
curl -X POST http://localhost:8888/api/kafka/send-from-upload \
  -F "file=@my_custom_data.json" \
  -F "brokerAddress=localhost:9092" \
  -F "topic=custom-alerts" \
  -F "limit=1000" \
  -F "delayMillis=100"
```

### 示例4: 使用 Python 调用API

```python
import requests
import json

# 从服务器文件发送
url = "http://localhost:8888/api/kafka/send-from-file"
payload = {
    "brokerAddress": "localhost:9092",
    "topic": "host-behavior-alerts",
    "jsonFilePath": "host_behavior_mock_data.json",
    "limit": 1000,
    "delayMillis": 100
}

response = requests.post(url, json=payload)
result = response.json()

if result['code'] == 200:
    stats = result['data']['statistics']
    print(f"发送成功: {stats['success']}/{stats['total']}")
    print(f"吞吐量: {stats['throughput']:.2f} msg/s")
else:
    print(f"发送失败: {result['message']}")
```

### 示例5: 使用 Python 上传文件发送

```python
import requests

url = "http://localhost:8888/api/kafka/send-from-upload"

files = {
    'file': open('my_data.json', 'rb')
}

data = {
    'brokerAddress': 'localhost:9092',
    'topic': 'my-topic',
    'limit': 500,
    'delayMillis': 100
}

response = requests.post(url, files=files, data=data)
print(response.json())
```

## 配置说明

### 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `SERVER_PORT` | 8888 | HTTP服务端口 |
| `SERVER_HOST` | 0.0.0.0 | 服务监听地址 |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka Broker地址 |
| `DATA_DIR` | /app/data | 数据文件目录 |
| `JAVA_OPTS` | -Xmx512m -Xms256m | JVM参数 |

### 修改配置

**方式1: Docker Compose**

编辑 `docker-compose.yml` 文件中的 `environment` 部分：

```yaml
environment:
  - SERVER_PORT=8888
  - KAFKA_BOOTSTRAP_SERVERS=my-kafka:9092
  - DATA_DIR=/app/data
```

**方式2: Docker Run**

```bash
docker run -d \
  -e SERVER_PORT=8888 \
  -e KAFKA_BOOTSTRAP_SERVERS=my-kafka:9092 \
  -p 8888:8888 \
  kafka-producer-service:latest
```

**方式3: 修改 application.yml**

编辑 `src/main/resources/application.yml` 后重新构建。

## 数据文件格式

JSON文件应该是一个对象数组，例如：

```json
[
  {
    "alarm_id": "550e8400-e29b-41d4-a716-446655440001",
    "alarm_name": "网络扫描探测",
    "alarm_type": 1,
    "alarm_subtype": 1001,
    "alarm_severity": 2,
    "create_time": "2024-01-15T08:30:00Z",
    "source_ip": "192.168.1.100",
    "target_ip": "10.0.0.50",
    ...
  },
  {
    "alarm_id": "550e8400-e29b-41d4-a716-446655440002",
    ...
  }
]
```

## 监控和管理

### 查看 Kafka Topics

使用 Kafka UI 访问: http://localhost:8080

或者使用命令行：

```bash
# 进入 Kafka 容器
docker exec -it kafka-producer-kafka bash

# 列出所有 topics
kafka-topics --bootstrap-server localhost:9092 --list

# 查看 topic 详情
kafka-topics --bootstrap-server localhost:9092 --describe --topic network-attack-alerts

# 消费消息（查看）
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic network-attack-alerts \
  --from-beginning \
  --max-messages 10
```

### 查看服务日志

```bash
# 查看 Producer Service 日志
docker-compose logs -f kafka-producer-service

# 查看 Kafka 日志
docker-compose logs -f kafka

# 查看所有服务日志
docker-compose logs -f
```

### 停止和清理

```bash
# 停止服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

## 性能调优

### 提高吞吐量

1. **减少延迟时间**:
   ```json
   {
     "delayMillis": 0
   }
   ```

2. **增加批量大小**（修改 `application.yml`）:
   ```yaml
   spring:
     kafka:
       producer:
         batch-size: 32768
         linger-ms: 50
   ```

3. **启用压缩**:
   ```json
   {
     "compressionEnabled": true
   }
   ```

4. **调整ACK策略**（降低可靠性换取性能）:
   ```json
   {
     "acks": "1"
   }
   ```

### JVM 内存调优

```bash
# 大文件处理时增加堆内存
docker run -e JAVA_OPTS="-Xmx2g -Xms1g" kafka-producer-service:latest
```

## 故障排查

### 问题1: 连接Kafka失败

**错误**: `Connection to node -1 could not be established`

**解决**:
- 检查 Kafka 是否正常运行: `docker-compose ps`
- 验证 Broker 地址是否正确
- 确认网络连通性

### 问题2: 文件找不到

**错误**: `文件不存在`

**解决**:
- 确认文件已正确挂载到 `/app/data` 目录
- 检查 docker-compose.yml 中的 volumes 配置
- 使用 `/api/kafka/list-files` 查看可用文件

### 问题3: 内存不足

**错误**: `OutOfMemoryError`

**解决**:
- 增加 JVM 堆内存: `-e JAVA_OPTS="-Xmx2g"`
- 减少批处理大小
- 分批发送大文件（使用 `limit` 参数）

## 技术栈

- **Java**: 17
- **Spring Boot**: 3.2.0
- **Spring Kafka**: Kafka客户端封装
- **Maven**: 构建工具
- **Docker**: 容器化
- **Kafka**: 2.8.1 (Confluent Platform 7.5.0)

## 项目结构

```
kafka-producer-service/
├── src/
│   └── main/
│       ├── java/com/alert/producer/
│       │   ├── KafkaProducerApplication.java   # 主程序
│       │   ├── config/
│       │   │   └── WebConfig.java              # Web配置
│       │   ├── controller/
│       │   │   └── KafkaProducerController.java # REST控制器
│       │   ├── dto/
│       │   │   ├── ApiResponse.java            # API响应封装
│       │   │   ├── KafkaProducerRequest.java   # 请求DTO
│       │   │   └── KafkaProducerResponse.java  # 响应DTO
│       │   └── service/
│       │       └── KafkaProducerService.java   # 业务逻辑
│       └── resources/
│           └── application.yml                 # 配置文件
├── Dockerfile                                  # Docker构建文件
├── docker-compose.yml                          # Docker编排文件
├── pom.xml                                     # Maven配置
└── README.md                                   # 本文件
```

## License

MIT License

## 联系方式

如有问题或建议，请提交 Issue。

