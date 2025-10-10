# Frontend - Kafka Producer Service

## 技术栈

- **Vue 3** - 渐进式JavaScript框架
- **Element Plus** - Vue 3 UI组件库
- **Vite** - 下一代前端构建工具
- **Axios** - HTTP客户端

## 开发

### 安装依赖
```bash
npm install
```

### 开发模式运行
```bash
npm run dev
```
访问: http://localhost:5173

### 构建生产版本
```bash
npm run build
```

构建产物会输出到 `../src/main/resources/static/` 目录，Spring Boot会自动服务这些静态文件。

## 项目结构

```
frontend/
├── src/
│   ├── App.vue          # 主应用组件
│   ├── main.js          # 应用入口
│   └── assets/          # 静态资源
├── index.html           # HTML模板
├── vite.config.js       # Vite配置
├── package.json         # 依赖配置
└── README.md           # 本文件
```

## 功能模块

### 1. 从服务器文件发送
- 选择服务器上的JSON文件
- 配置Kafka参数
- 实时发送进度
- 详细统计信息

### 2. 从文件上传发送
- 拖拽上传JSON文件
- 自动解析文件内容
- 上传并发送到Kafka

### 3. 服务信息
- API端点列表
- 系统信息展示
- 服务状态监控

## 集成到Spring Boot

前端构建过程已集成到Maven构建流程中：

1. Maven 构建时自动安装 Node.js 和 npm
2. 自动执行 `npm install` 安装依赖
3. 自动执行 `npm run build` 构建前端
4. 构建产物输出到 `src/main/resources/static/`
5. Spring Boot 启动时自动服务前端页面

## API代理配置

开发模式下，Vite配置了API代理：

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8888',
      changeOrigin: true
    }
  }
}
```

这样在开发时可以直接访问后端API而不需要CORS配置。

## 部署

### 开发环境
前端和后端分离运行：
- 前端: `npm run dev` (端口 5173)
- 后端: `mvn spring-boot:run` (端口 8888)

### 生产环境
前端构建到后端静态资源：
- 运行: `mvn clean package` 会自动构建前端和后端
- 访问: http://localhost:8888/ 即可看到前端界面

