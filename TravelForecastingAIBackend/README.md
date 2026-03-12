# 智教黔行 · AI智能服务 (TravelForecastingAIBackend)

Java Spring Boot 微服务，提供AI智能聊天、行程规划、研学教育、知识库检索、语音服务代理等功能。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | JDK版本 |
| Spring Boot | 3.2.0 | 核心框架 |
| MyBatis-Plus | 3.5.5 | ORM框架 |
| Redis | - | 缓存AI响应/会话上下文 |
| DashScope SDK | 2.12.0 | 通义千问AI接入 |
| OkHttp | 4.12.0 | WebSocket代理 |
| MySQL | 8.0+ | 共享业务数据库 |

## 架构设计

```
简化微服务架构（不使用Spring Cloud）

Java业务后端 (8080) ←→ Java AI后端 (8081)
                          ↓
                   ┌──────┴──────┐
                   ▼             ▼
            旧数字人后端     Python预测服务
             (8000)          (8001)
```

### 服务间调用

- **BusinessClient** → 调用Java业务后端 (8080) 获取景区/用户等数据
- **DigitalHumanClient** → 调用旧数字人后端 (8000) 的TTS和聊天
- **PredictionClient** → 调用Python预测服务 (8001) 获取客流预测
- **WebSocket代理** → 前端 ←→ AI服务(8081) ←→ 数字人后端(8000)

## 项目结构

```
src/main/java/com/travel/ai/
├── TravelAIApplication.java          # 主启动类
├── common/
│   └── Result.java                   # 统一响应类
├── config/
│   ├── AsyncConfig.java              # 异步线程池
│   ├── DashScopeConfig.java          # 通义千问配置
│   ├── MyBatisPlusConfig.java        # MyBatis-Plus配置
│   ├── RedisConfig.java              # Redis缓存配置
│   ├── RestTemplateConfig.java       # HTTP客户端配置
│   ├── WebConfig.java                # Web MVC配置
│   └── WebSocketConfig.java          # WebSocket配置
├── controller/
│   ├── ChatController.java           # AI聊天 (迁移自业务后端)
│   ├── AiPlanningController.java     # 行程规划 (迁移自业务后端)
│   ├── EducationController.java      # 研学教育 (新增)
│   ├── KnowledgeController.java      # 知识库管理 (新增)
│   ├── SpeechController.java         # 语音服务代理
│   └── HealthController.java         # 健康检查
├── service/
│   ├── AiChatService.java            # 聊天服务接口
│   ├── AiPlanningService.java        # 行程规划接口
│   ├── RagService.java               # 知识库检索接口
│   ├── EducationService.java         # 研学教育接口
│   ├── SpeechService.java            # 语音服务接口
│   └── impl/                         # 实现类
├── client/
│   ├── BusinessClient.java           # 业务后端客户端
│   ├── DigitalHumanClient.java       # 数字人客户端
│   └── PredictionClient.java         # 预测服务客户端
├── entity/                           # 数据实体
├── dto/                              # 数据传输对象
├── mapper/                           # MyBatis Mapper
├── websocket/
│   └── DigitalHumanWebSocket.java    # WebSocket代理
├── interceptor/
│   └── JwtInterceptor.java           # JWT验证
└── exception/                        # 异常处理
```

## API 接口

| 模块 | 路径 | 说明 |
|------|------|------|
| 聊天 | `POST /ai-api/chat/message` | 发送聊天消息 |
| 聊天 | `GET /ai-api/chat/history/{id}` | 获取会话历史 |
| 聊天 | `GET /ai-api/chat/conversations` | 获取会话列表 |
| 行程 | `POST /ai-api/ai-planning/generate` | AI生成行程 |
| 知识 | `POST /ai-api/knowledge/search` | 知识库检索 |
| 知识 | `GET /ai-api/knowledge/answer` | AI知识问答 |
| 知识 | `GET /ai-api/knowledge/list` | 知识文档列表 |
| 研学 | `GET /ai-api/education/routes` | 研学路线列表 |
| 研学 | `POST /ai-api/education/generate` | AI生成研学方案 |
| 语音 | `POST /ai-api/speech/tts` | 文本转语音 |
| WebSocket | `ws://localhost:8081/ai-api/ws/digital-human` | 数字人代理 |
| 健康 | `GET /ai-api/health` | 服务状态检查 |
| 文档 | `GET /ai-api/swagger-ui.html` | Swagger文档 |

## 快速启动

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+ (已有travel_prediction数据库)
- Redis 6.0+ (可选但推荐)

### 2. 初始化数据库

```sql
-- 在 travel_prediction 数据库中执行
source src/main/resources/db/V1__init_ai_tables.sql
```

### 3. 修改配置

编辑 `src/main/resources/application.yml`：

```yaml
# 数据库密码
spring.datasource.password: your_password

# DashScope API Key
dashscope.api-key: your_api_key

# 高德地图 API Key
amap.api.key: your_amap_key
```

### 4. 编译运行

```bash
# 编译
mvn clean package -DskipTests

# 运行
java -jar target/travel-ai-service-1.0.0.jar

# 或使用Maven直接运行
mvn spring-boot:run
```

### 5. 验证

```bash
# 健康检查
curl http://localhost:8081/ai-api/health

# 测试聊天
curl -X POST http://localhost:8081/ai-api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message":"六盘水有什么好玩的景区？"}'

# Swagger文档
打开 http://localhost:8081/ai-api/swagger-ui.html
```

## 配置说明

### 环境变量（推荐用于生产环境）

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `DASHSCOPE_API_KEY` | 通义千问API密钥 | 配置文件中的值 |
| `AMAP_API_KEY` | 高德地图API密钥 | 配置文件中的值 |

### 依赖服务

| 服务 | 端口 | 必需 | 说明 |
|------|------|------|------|
| Java业务后端 | 8080 | 否 | 获取景区/用户数据 |
| 数字人后端 | 8000 | 否 | TTS语音/WebSocket |
| Python预测服务 | 8001 | 否 | 客流预测数据 |
| MySQL | 3306 | 是 | 数据存储 |
| Redis | 6379 | 否 | AI响应缓存 |
