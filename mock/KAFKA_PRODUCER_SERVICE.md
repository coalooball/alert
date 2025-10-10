# Kafka Producer Service 项目说明

## 项目概述

这是一个独立的Java Web服务，用于将JSON格式的告警数据发送到Kafka。它提供了REST API接口，支持从服务器文件读取或通过HTTP上传的方式发送数据。

**项目位置**: `/home/why/code/alerts/mock/kafka-producer-service/`

## 为什么创建这个服务？

原有的Python脚本 (`kafka_producer.py`) 虽然功能完善，但有以下局限：
1. 需要手动在命令行运行
2. 缺少Web界面
3. 不便于远程调用
4. 难以集成到其他系统

这个Java服务解决了这些问题，提供：
- ✅ REST API接口，可被任何语言调用
- ✅ 友好的Web测试页面
- ✅ Docker容器化，一键部署
- ✅ 完整的Kafka环境（包含Kafka和Zookeeper）
- ✅ 实时统计和监控

## 功能对比

| 功能 | Python脚本 | Java服务 |
|------|-----------|---------|
| 从文件发送 | ✅ | ✅ |
| 文件上传 | ❌ | ✅ |
| REST API | ❌ | ✅ |
| Web界面 | ❌ | ✅ |
| Docker部署 | ❌ | ✅ |
| 内置Kafka | ❌ | ✅ |
| 实时统计 | ✅ | ✅ |
| 远程调用 | ❌ | ✅ |

## 项目结构

```
kafka-producer-service/
├── src/main/java/com/alert/producer/
│   ├── KafkaProducerApplication.java      # 主程序入口
│   ├── config/
│   │   └── WebConfig.java                 # CORS配置
│   ├── controller/
│   │   └── KafkaProducerController.java   # REST API控制器
│   ├── dto/
│   │   ├── ApiResponse.java               # 统一响应格式
│   │   ├── KafkaProducerRequest.java      # 请求DTO
│   │   └── KafkaProducerResponse.java     # 响应DTO
│   └── service/
│       └── KafkaProducerService.java      # 核心业务逻辑
├── src/main/resources/
│   └── application.yml                    # 配置文件
├── Dockerfile                             # Docker镜像构建
├── docker-compose.yml                     # 服务编排
├── build-and-run.sh                       # 便捷启动脚本
├── test.html                              # Web测试页面
├── README.md                              # 完整文档
├── QUICKSTART.md                          # 快速开始
└── pom.xml                                # Maven配置
```

## 核心组件说明

### 1. KafkaProducerService (核心服务)
- 负责读取JSON文件并解析
- 创建Kafka Producer实例
- 批量发送消息到Kafka
- 统计发送结果

### 2. KafkaProducerController (API接口)
提供5个REST端点：
- `GET /api/kafka/health` - 健康检查
- `GET /api/kafka/info` - 服务信息
- `GET /api/kafka/list-files` - 列出可用文件
- `POST /api/kafka/send-from-file` - 从服务器文件发送
- `POST /api/kafka/send-from-upload` - 从上传文件发送

### 3. Docker Compose 编排
包含4个容器：
- `kafka-producer-service` - Java服务 (端口8888)
- `kafka` - Kafka Broker (端口9092)
- `zookeeper` - Zookeeper (端口2181)
- `kafka-ui` - Kafka管理界面 (端口8080)

## 快速开始

### 一键启动
```bash
cd /home/why/code/alerts/mock/kafka-producer-service
./build-and-run.sh
```

选择 `1` 构建并启动所有服务。

### 访问服务
- **API服务**: http://localhost:8888
- **测试页面**: 双击打开 `test.html`
- **Kafka UI**: http://localhost:8080

### 发送测试数据
```bash
curl -X POST http://localhost:8888/api/kafka/send-from-file \
  -H "Content-Type: application/json" \
  -d '{
    "brokerAddress": "localhost:9092",
    "topic": "test-alerts",
    "jsonFilePath": "network_attack_mock_data.json",
    "limit": 10
  }'
```

## API使用示例

### Bash / cURL
```bash
curl -X POST http://localhost:8888/api/kafka/send-from-file \
  -H "Content-Type: application/json" \
  -d '{
    "brokerAddress": "localhost:9092",
    "topic": "network-attack-alerts",
    "jsonFilePath": "network_attack_mock_data.json",
    "limit": 100,
    "delayMillis": 100
  }'
```

### Python
```python
import requests

response = requests.post(
    "http://localhost:8888/api/kafka/send-from-file",
    json={
        "brokerAddress": "localhost:9092",
        "topic": "network-attack-alerts",
        "jsonFilePath": "network_attack_mock_data.json",
        "limit": 100
    }
)
print(response.json())
```

### JavaScript / Node.js
```javascript
fetch('http://localhost:8888/api/kafka/send-from-file', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    brokerAddress: 'localhost:9092',
    topic: 'network-attack-alerts',
    jsonFilePath: 'network_attack_mock_data.json',
    limit: 100
  })
})
.then(res => res.json())
.then(data => console.log(data));
```

### Java
```java
RestTemplate restTemplate = new RestTemplate();
KafkaProducerRequest request = new KafkaProducerRequest();
request.setBrokerAddress("localhost:9092");
request.setTopic("network-attack-alerts");
request.setJsonFilePath("network_attack_mock_data.json");
request.setLimit(100);

ApiResponse<KafkaProducerResponse> response = restTemplate.postForObject(
    "http://localhost:8888/api/kafka/send-from-file",
    request,
    new ParameterizedTypeReference<ApiResponse<KafkaProducerResponse>>() {}
);
```

## 与现有系统集成

### 集成到告警系统后端
可以在现有的告警系统中添加一个控制器，调用这个服务：

```java
@RestController
@RequestMapping("/api/kafka-producer")
public class KafkaProducerProxyController {
    
    private final RestTemplate restTemplate;
    private final String kafkaProducerServiceUrl = "http://localhost:8888";
    
    @PostMapping("/send")
    public ResponseEntity<?> sendToKafka(@RequestBody KafkaProducerRequest request) {
        String url = kafkaProducerServiceUrl + "/api/kafka/send-from-file";
        return restTemplate.postForEntity(url, request, ApiResponse.class);
    }
}
```

### 集成到前端页面
可以在现有的Vue前端添加一个Kafka数据发送页面，调用API接口。

## 数据文件挂载

Docker Compose 默认将父目录（mock目录）挂载到容器的 `/app/data`：

```yaml
volumes:
  - ../:/app/data:ro
```

这样容器就可以访问所有mock数据文件：
- `network_attack_mock_data.json`
- `malicious_sample_mock_data.json`
- `host_behavior_mock_data.json`

## 性能特性

- **批量发送**: 优化的批量发送机制
- **异步处理**: 异步发送提高吞吐量
- **压缩支持**: 可选Gzip压缩减少网络传输
- **可配置延迟**: 控制发送速率避免压垮Kafka
- **实时统计**: 实时反馈发送进度和统计信息

## 监控和管理

### 使用Kafka UI
访问 http://localhost:8080 可以：
- 查看所有Topics
- 查看消息内容
- 监控消费者组
- 查看Broker状态

### 查看日志
```bash
# 查看服务日志
docker-compose logs -f kafka-producer-service

# 查看Kafka日志
docker-compose logs -f kafka
```

### 健康检查
```bash
curl http://localhost:8888/api/kafka/health
```

## 配置选项

### 环境变量
在 `docker-compose.yml` 中配置：

```yaml
environment:
  - SERVER_PORT=8888              # HTTP服务端口
  - KAFKA_BOOTSTRAP_SERVERS=...   # Kafka地址
  - DATA_DIR=/app/data            # 数据目录
  - JAVA_OPTS=-Xmx512m            # JVM参数
```

### 请求参数
发送请求时可配置：
- `brokerAddress`: Kafka Broker地址
- `topic`: 目标Topic
- `limit`: 限制发送数量
- `delayMillis`: 消息间延迟（毫秒）
- `compressionEnabled`: 是否压缩
- `acks`: ACK模式（all/1/0）
- `retries`: 重试次数

## 停止和清理

```bash
# 停止服务
docker-compose down

# 停止并删除数据
docker-compose down -v

# 使用脚本
./build-and-run.sh
# 选择 3 停止服务
# 选择 6 清理所有数据
```

## 技术栈

- **Java**: 17
- **Spring Boot**: 3.2.0
- **Spring Kafka**: Kafka集成
- **Maven**: 项目构建
- **Docker**: 容器化
- **Kafka**: 消息队列
- **Alpine Linux**: 轻量级基础镜像

## 与Python脚本的对比

### Python脚本 (kafka_producer.py)
**优势**:
- 轻量级
- 命令行直接运行
- 简单快速

**劣势**:
- 只能本地运行
- 无法远程调用
- 缺少Web界面
- 难以集成

### Java服务 (kafka-producer-service)
**优势**:
- REST API，任何语言可调用
- Web测试界面
- Docker容器化部署
- 易于集成到其他系统
- 内置完整Kafka环境
- 更好的监控和管理

**劣势**:
- 相对复杂
- 需要更多资源
- 启动时间稍长

## 使用场景建议

**使用Python脚本**:
- 本地快速测试
- 一次性数据导入
- 命令行自动化

**使用Java服务**:
- 生产环境部署
- 远程服务调用
- Web界面操作
- 与其他系统集成
- 需要监控和管理

## 后续扩展

可以考虑添加以下功能：
1. 定时任务自动发送
2. 消息发送历史记录
3. 多个Kafka集群支持
4. 消息转换和过滤
5. 与现有告警系统的深度集成
6. 消息发送调度器
7. WebSocket实时推送进度

## 文档索引

- **完整文档**: [README.md](kafka-producer-service/README.md)
- **快速开始**: [QUICKSTART.md](kafka-producer-service/QUICKSTART.md)
- **本说明**: [KAFKA_PRODUCER_SERVICE.md](KAFKA_PRODUCER_SERVICE.md)

## 相关文件

- Python版本: [kafka_producer.py](kafka_producer.py)
- Python文档: [KAFKA_PRODUCER_README.md](KAFKA_PRODUCER_README.md)
- Mock数据: [README.md](README.md)

---

**创建日期**: 2024-10-10  
**版本**: 1.0.0  
**维护者**: Alert System Team

