# 文件上传功能更新说明

## 更新日期
2025-10-10

## 修改概述
为 Kafka Producer Service 添加了纯文件上传功能，将文件上传和发送到Kafka的功能分离。

## 修改内容

### 1. 后端修改

#### KafkaProducerController.java
- 新增 `POST /api/kafka/upload-file` 接口
  - 功能：仅上传文件到服务器，不发送到Kafka
  - 参数：只需要上传的文件
  - 返回：文件名、保存路径、文件大小等信息

#### KafkaProducerService.java
- 新增 `saveUploadedFile()` 方法
  - 功能：保存上传的文件到数据目录
  - 特性：
    - 自动创建数据目录
    - 文件名冲突时自动添加时间戳
    - 返回保存后的文件名

### 2. 前端修改

#### App.vue

**功能调整：**
- 保留"从服务器文件发送"功能（需要Kafka参数）
- 新增"文件上传"功能（纯上传，无Kafka参数）
- 移除了原来的"从文件上传发送"（合并上传和发送的功能）

**新增Tab：文件上传**
- 只需选择文件
- 点击"上传"按钮即可
- 上传成功后显示：
  - 文件名
  - 保存路径
  - 文件大小
  - 状态信息
- 自动刷新文件列表

**新增功能：**
- `uploadFileOnly()` - 纯文件上传函数
- `formatFileSize()` - 格式化文件大小显示（B/KB/MB/GB）
- 上传成功后自动刷新服务器文件列表

## 使用场景

### 场景1：先上传，后发送
1. 在"文件上传"Tab上传文件到服务器
2. 切换到"从服务器文件发送"Tab
3. 选择刚上传的文件进行发送

### 场景2：直接从服务器发送
1. 使用"从服务器文件发送"Tab
2. 选择已存在的文件
3. 配置Kafka参数后发送

### 场景3：通过API上传
```bash
curl -X POST http://localhost:8888/api/kafka/upload-file \
  -F "file=@/path/to/data.json"
```

## API端点

### 新增端点
- **POST /api/kafka/upload-file**
  - 功能：上传文件（不发送到Kafka）
  - 请求：multipart/form-data，文件字段名为 `file`
  - 响应示例：
    ```json
    {
      "code": 200,
      "message": "文件上传成功",
      "data": {
        "success": true,
        "fileName": "my_data.json",
        "savedPath": "my_data.json",
        "fileSize": 1024000,
        "message": "文件上传成功"
      }
    }
    ```

### 保留端点
- **POST /api/kafka/send-from-file** - 从服务器文件发送
- **POST /api/kafka/send-from-upload** - 上传并发送（API层面保留）
- **GET /api/kafka/list-files** - 列出文件
- **GET /api/kafka/health** - 健康检查
- **GET /api/kafka/info** - 服务信息

## 技术细节

### 文件保存逻辑
1. 文件保存到配置的数据目录（默认：`/app/data`）
2. 如果文件名已存在，自动添加时间戳后缀
3. 示例：`data.json` → `data_1728567890123.json`

### 文件大小格式化
- 自动根据大小选择合适的单位（B/KB/MB/GB）
- 保留两位小数
- 示例：1024000 bytes → 1000 KB

### 文件列表刷新
- 上传成功后自动刷新文件列表
- 用户可以立即看到新上传的文件

## 优势

1. **功能分离**：上传和发送功能分离，更灵活
2. **简化操作**：上传时不需要配置Kafka参数
3. **批量准备**：可以先上传多个文件，再逐个发送
4. **更安全**：上传的文件可以先验证，再决定是否发送
5. **API友好**：提供独立的上传API，易于集成

## 兼容性

- ✅ 完全向后兼容
- ✅ 保留所有原有功能
- ✅ 原有API接口不变
- ✅ 仅新增功能，不影响现有使用

## 测试建议

1. 测试纯文件上传
2. 测试文件名冲突时的处理
3. 测试上传后文件列表刷新
4. 测试大文件上传（如果需要）
5. 测试不同格式文件的上传限制

---

**修改者**: AI Assistant  
**版本**: 1.1.0


