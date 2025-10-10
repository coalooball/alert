# 部署说明

## 两种部署模式

本服务提供两种部署模式，适应不同的使用场景。

### 模式一：连接现有Kafka（推荐）

**适用场景**：
- 已有运行中的Kafka环境
- 与主告警系统共用Kafka
- 资源受限的环境
- 只需要Producer功能

**使用文件**：`docker-compose.yml`

**启动方式**：
```bash
# 方式1：使用环境变量
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
docker-compose up -d --build

# 方式2：命令行指定
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 docker-compose up -d --build

# 方式3：使用便捷脚本
./build-and-run.sh
# 选择 1，输入Kafka地址
```

**网络模式**：`host`
- 容器直接使用宿主机网络
- 可直接访问 localhost:9092 的Kafka
- 无需额外网络配置

**特点**：
- ✅ 轻量级（仅1个容器）
- ✅ 快速启动（~10秒）
- ✅ 资源占用少（~512MB）
- ✅ 与现有Kafka集成

**服务列表**：
- kafka-producer-service (端口8888)

---

### 模式二：完整独立环境

**适用场景**：
- 需要完全独立的测试环境
- 开发和演示
- 不想依赖外部Kafka
- 需要Kafka UI管理界面

**使用文件**：`docker-compose-full.yml`

**启动方式**：
```bash
# 方式1：直接启动
docker-compose -f docker-compose-full.yml up -d --build

# 方式2：使用便捷脚本
./build-and-run.sh
# 选择 2
```

**网络模式**：`bridge`（自定义网络 kafka-network）
- 容器之间通过Docker网络通信
- 完全隔离的环境

**特点**：
- ✅ 完全独立
- ✅ 包含Kafka UI管理界面
- ✅ 一键部署全套环境
- ⚠️ 资源占用较多（~2GB）
- ⚠️ 启动较慢（~60秒）

**服务列表**：
- kafka-producer-service (端口8888)
- kafka (端口9092)
- zookeeper (端口2181)
- kafka-ui (端口8080)

---

## 配置对比

| 项目 | 模式一（现有Kafka） | 模式二（完整环境） |
|------|-------------------|------------------|
| 配置文件 | docker-compose.yml | docker-compose-full.yml |
| 容器数量 | 1个 | 4个 |
| 内存占用 | ~512MB | ~2GB |
| 启动时间 | ~10秒 | ~60秒 |
| 网络模式 | host | bridge |
| Kafka依赖 | 外部 | 内置 |
| Kafka UI | ❌ | ✅ |
| 适合场景 | 生产/集成 | 开发/测试 |

---

## 环境变量

### 模式一（连接现有Kafka）

```bash
# 必需
KAFKA_BOOTSTRAP_SERVERS=localhost:9092    # Kafka地址

# 可选
SERVER_PORT=8888                          # HTTP服务端口
SERVER_HOST=0.0.0.0                       # 监听地址
DATA_DIR=/app/data                        # 数据文件目录
JAVA_OPTS=-Xmx512m -Xms256m              # JVM参数
```

### 模式二（完整环境）

无需额外配置，所有服务使用默认配置。

---

## 快速命令参考

### 启动服务
```bash
# 模式一
docker-compose up -d --build

# 模式二
docker-compose -f docker-compose-full.yml up -d --build
```

### 停止服务
```bash
# 模式一
docker-compose down

# 模式二
docker-compose -f docker-compose-full.yml down

# 两种模式都停止
docker-compose down
docker-compose -f docker-compose-full.yml down
```

### 查看状态
```bash
# 模式一
docker-compose ps

# 模式二
docker-compose -f docker-compose-full.yml ps
```

### 查看日志
```bash
# 模式一
docker-compose logs -f kafka-producer-service

# 模式二
docker-compose -f docker-compose-full.yml logs -f
# 只看Producer服务
docker-compose -f docker-compose-full.yml logs -f kafka-producer-service
```

### 重启服务
```bash
# 模式一
docker-compose restart

# 模式二
docker-compose -f docker-compose-full.yml restart
```

### 清理所有数据
```bash
# 模式一
docker-compose down -v

# 模式二
docker-compose -f docker-compose-full.yml down -v
```

---

## 切换模式

### 从模式二切换到模式一

```bash
# 1. 停止完整环境
docker-compose -f docker-compose-full.yml down

# 2. 启动模式一（确保外部Kafka已运行）
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 docker-compose up -d
```

### 从模式一切换到模式二

```bash
# 1. 停止模式一
docker-compose down

# 2. 启动完整环境
docker-compose -f docker-compose-full.yml up -d
```

---

## 与主项目集成

### 使用主项目的Kafka

假设主项目的Kafka运行在 `localhost:9092`：

```bash
cd /home/why/code/alerts/mock/kafka-producer-service

# 使用模式一连接主项目Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 docker-compose up -d --build
```

### 数据文件挂载

两种模式都将父目录（mock目录）挂载到容器：

```yaml
volumes:
  - ../:/app/data:ro
```

这样可以访问所有mock数据文件：
- network_attack_mock_data.json
- malicious_sample_mock_data.json
- host_behavior_mock_data.json

---

## 故障排查

### 问题1：模式一无法连接Kafka

**原因**：Kafka地址配置错误或Kafka未运行

**解决**：
```bash
# 检查Kafka是否运行
netstat -tlnp | grep 9092

# 确认Kafka地址
echo $KAFKA_BOOTSTRAP_SERVERS

# 重新启动并指定正确地址
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 docker-compose up -d
```

### 问题2：模式二启动失败

**原因**：端口被占用

**解决**：
```bash
# 检查端口占用
netstat -tlnp | grep -E '(8888|9092|2181|8080)'

# 停止占用端口的服务或修改docker-compose-full.yml中的端口映射
```

### 问题3：容器无法访问数据文件

**原因**：卷挂载路径错误

**解决**：
```bash
# 确认数据文件存在
ls -la /home/why/code/alerts/mock/*.json

# 进入容器检查
docker exec -it kafka-producer-service ls -la /app/data/
```

---

## 推荐实践

### 开发环境
- 使用**模式二**（完整环境）
- 方便调试和查看Kafka消息
- 使用Kafka UI管理Topics

### 测试环境
- 使用**模式二**（完整环境）
- 独立的测试环境，不影响其他系统

### 生产环境
- 使用**模式一**（连接现有Kafka）
- 轻量级，资源占用少
- 与现有Kafka集群集成

### CI/CD环境
- 使用**模式二**（完整环境）
- 自动化测试需要独立环境
- 测试完成后自动清理

---

## 性能优化

### 模式一优化

```bash
# 增加JVM内存（大文件处理）
JAVA_OPTS="-Xmx1g -Xms512m" docker-compose up -d
```

### 模式二优化

编辑 `docker-compose-full.yml`：

```yaml
# Kafka配置优化
kafka:
  environment:
    KAFKA_NUM_PARTITIONS: 6              # 增加分区数
    KAFKA_LOG_RETENTION_HOURS: 24        # 减少保留时间
```

---

## 总结

- **日常使用**：推荐模式一，连接现有Kafka
- **独立测试**：推荐模式二，完整环境
- **快速切换**：使用 `build-and-run.sh` 脚本
- **生产部署**：仅使用模式一

选择合适的模式可以更好地满足你的需求！

