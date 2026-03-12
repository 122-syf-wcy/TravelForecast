# 智教黔行 · 智游六盘水 - 系统审计报告

> 审计时间：2026-03-02  
> 审计范围：Bug、硬编码、未实现功能、安全隐患

---

## 一、硬编码问题（高优先级）

### 1.1 敏感信息硬编码（安全风险）

| 位置 | 问题 | 建议 |
|------|------|------|
| `TravelForecastBackend/application.yml` | 数据库密码 `TravelDB2025!`、通义千问 API Key `sk-c444b733...`、高德 API Key、OSS AccessKey 明文 | 改为环境变量 `${DB_PASSWORD}`、`${DASHSCOPE_API_KEY}` 等 |
| `TravelForecastBackend/application.yml` | OSS `access-key-id`、`access-key-secret` 明文 | 使用 `${OSS_ACCESS_KEY_ID}` 等 |
| `TravelForecastingAIBackend/application.yml` | 同上，且 `api-key` 默认值包含真实 Key | 移除默认值，强制从环境变量读取 |
| `TravelForecast-DigitalHuman/backend/app/services/auth_service.py` | 默认密码 `admin123`、`guest` 硬编码，JWT secret 硬编码 | 改为环境变量，生产环境强制修改 |
| `TravelForecast-PythonPredictionService/data_generator.py` | 数据库密码 `123456` 硬编码 | 使用 config.env 或环境变量 |
| `TravelForecast-PythonPredictionService/test_db_connection.py` | 同上 | 同上 |
| `TravelForecast-PythonPredictionService/init_database.py` | 数据库配置硬编码 | 使用环境变量 |

### 1.2 IP/URL 硬编码（部署不灵活）

| 位置 | 硬编码值 | 影响 |
|------|----------|------|
| `TravelForecastGateway/application.yml` | Redis `host: 39.97.232.141` | 部署到其他服务器需改配置 |
| `TravelForecastBackend/application.yml` | MySQL `39.97.232.141:3306`、Redis `39.97.232.141` | 同上 |
| `TravelForecastingAIBackend/application.yml` | 同上 | 同上 |
| `TravelForecastMiniProgramBackend/application.yml` | 同上 | 同上 |
| `TravelForecastMiniProgram/src/config.js` | `API_BASE_URL: 'http://localhost:8082/api'` | 真机/生产环境无法使用，需改为可配置 |
| `TravelForecastMiniProgram/src/config.js` | `AI_API_BASE_URL`、`ASSET_BASE_URL` 均为 localhost | 同上 |
| `TravelForecastFrontend/web/src/components/EmbeddedDigitalHuman.vue` | `WS_URL_DIRECT = 'ws://localhost:8083/ws/avatar'` | 生产环境应走网关 `ws://域名:8888/ws/...` |
| `TravelForecastFrontend/web/src/components/DigitalHuman.vue` | 同上 | 同上 |
| `TravelForecastMiniProgram/src/api/digitalHuman.js` | `url: 'http://localhost:5000/api/stt'` | STT 服务地址硬编码，且 5000 端口与文档中数字人 8000 不一致 |
| `TravelForecast-DigitalHuman/backend/app/core/config.py` | `PREDICTION_SERVICE_URL: "http://localhost:8001"` | 容器/多机部署需可配置 |
| `TravelForecast-DigitalHuman/index.html` | `WS_URL = 'ws://localhost:8000/ws/avatar'` | 端口 8000 与 config 中 8083 不一致 |
| `TravelForecast-DigitalHuman/src/main.js` | `wsUrl: 'ws://localhost:8000/ws/avatar'` | 同上 |
| `TravelForecast-DigitalHuman/src/services/WebSocketService.js` | `url: 'ws://localhost:8000/ws/avatar'` | 同上 |

### 1.3 数字人服务端口不一致

- **Gateway** 路由到 `http://localhost:8083`
- **Digital Human config.py** 默认 `server_port: 8083`
- **Digital Human 前端** (index.html, main.js, WebSocketService.js) 使用 `8000`
- **HealthCheckController** 默认 `routes[4]` 为 `http://localhost:8000`（实际配置会覆盖）

**建议**：统一为 8083，并修正数字人前端配置。

---

## 二、Bug 与潜在异常

### 2.1 空 catch 块 / 异常吞没

| 位置 | 问题 |
|------|------|
| `TravelForecastBackend/.../OssController.java:150` | `catch (Exception ignored) {}` 吞掉异常 |
| `TravelForecastBackend/.../OssController.java:196` | `catch (Exception ignored) {}` |
| `TravelForecastBackend/.../AdminMerchantServiceImpl.java:194,206` | `catch (Exception ignored) {}` 邮件发送失败静默 |
| `TravelForecastFrontend/.../request.ts:138-139` | `catch (e) { /* 异常处理 */ }` 空块 |

### 2.2 可能的 IndexOutOfBounds / NPE

| 位置 | 风险 |
|------|------|
| `AiPlanningController.java` | `paths.get(0)`、`coords1.get(0)` 等，若 API 返回空列表会抛异常 |
| `DashboardController.java` | `forecasts.get(0)`、`casts.get(0)`、`weatherList.get(0)` 等 |
| `AdminDashboardServiceImpl.java` | `coverImages.get(0)`、`spots.get(0)` 未判空 |
| `ScenicStatusServiceImpl.java` | `hourlyData.get(0)` |
| `ReviewServiceImpl.java` | `businessReplies.get(0)` 可能空列表 |
| `MerchantReviewController.java` | `existingReplies.get(0)` |
| `AdminScenicController.java` | `videos.get(0)` |
| `ExportServiceImpl.java` | `groupData.get(0)`、`data.get(0)` |

**建议**：在使用 `list.get(0)` 前增加 `!list.isEmpty()` 或 `list != null && list.size() > 0` 判断。

### 2.3 RuntimeException 使用不当

多处使用 `throw new RuntimeException(...)` 而非业务异常，不利于统一错误处理和前端展示：

- `TicketOrderServiceImpl`、`ActivityServiceImpl`、`AnnouncementServiceImpl`、`FacilityServiceImpl`、`EmergencyRescueServiceImpl`、`ExportServiceImpl` 等

**建议**：统一使用 `BusinessException` 或自定义业务异常，由全局异常处理器返回统一格式。

### 2.4 HealthCheckController 与路由顺序耦合

`HealthCheckController` 通过 `spring.cloud.gateway.routes[0]` ~ `[4]` 按索引读取路由，若 yml 中路由顺序调整会导致健康检查错配。

**建议**：改为按 `id` 查找路由，或使用配置项显式指定各服务 URL。

---

## 三、未实现 / 占位功能

### 3.1 返回 null 的接口

| 位置 | 说明 |
|------|------|
| `AuthController.java:229` | 某接口 `return null` |
| `RagServiceImpl.java:115` | 知识库检索失败 `return null` |
| `OssServiceImpl.java` | 多处 `return null`，调用方需处理 |
| `AmapServiceImpl.java:110,123,126` | 高德 API 失败时 `return null` |
| `JwtAuthenticationFilter.java:103` | `return null` |
| `BusinessClient.java`、`DigitalHumanClient.java` | Feign 降级 `return null`，需确认调用方有兜底 |
| `PaymentController.java:236` | `return null` |
| `SpeechServiceImpl.java:26` | `return null` |

### 3.2 注释掉的逻辑

| 位置 | 说明 |
|------|------|
| `AdminMerchantServiceImpl.java:178` | `// throw new BusinessException("商家资料未完善...")` 被注释，可能跳过校验 |

### 3.3 小程序 STT 接口

`TravelForecastMiniProgram/src/api/digitalHuman.js` 中 `uploadVoice` 调用 `http://localhost:5000/api/stt`，但项目中未发现 5000 端口的 STT 服务实现，可能为未完成功能。

---

## 四、配置与环境问题

### 4.1 生产环境配置缺失

- **TravelForecastingAIBackend**、**TravelForecastMiniProgramBackend** 无 `application-prod.yml`，生产环境需通过 JVM 参数覆盖。
- **TravelForecastBackend** 的 `application-prod.yml` 使用 `travel_user` / `Travel@2024`，与主配置 `root` / `TravelDB2025!` 不一致，需确认生产库用户。

### 4.2 Python 预测服务环境变量

`TravelForecast-PythonPredictionService` 的 `db_connector.py` 通过 `os.getenv()` 读取配置，但未显式 `load_dotenv("config.env")`，需在启动脚本中 `export` 或加载 `.env`。

### 4.3 日志级别

多个服务 `logging.level` 为 `debug`，生产环境建议改为 `info` 或 `warn`：

- `TravelForecastGateway`、`TravelForecastBackend`、`TravelForecastingAIBackend`、`TravelForecastMiniProgramBackend`

---

## 五、前端相关问题

### 5.1 环境变量

- `TravelForecastFrontend` 使用 `VITE_API_BASE_URL`，未配置时回退到 `/api`（相对路径），生产需在 Nginx 正确代理。
- `Settings.vue`、`backup.ts` 使用 `import.meta.env.VITE_API_BASE_URL`，需确保构建时注入。

### 5.2 OSS 地址硬编码

`TravelForecastFrontend/web/src/utils/imageProxy.ts` 中 `OSS_BASE_URL` 硬编码为 `https://smarttourism717.oss-cn-beijing.aliyuncs.com/scenic`，换 bucket 需改代码。

### 5.3 开发环境判断

`Login.vue`、`admin.ts` 等通过 `localhost|127.0.0.1` 判断开发环境，逻辑可接受，但需注意生产域名不含这些字符串。

---

## 六、修复优先级建议

| 优先级 | 类型 | 建议行动 |
|--------|------|----------|
| P0 | 安全 | 敏感信息全部改为环境变量，禁止提交到仓库 |
| P0 | 部署 | 小程序 config.js 改为可配置（构建时注入或运行时配置） |
| P1 | Bug | 为所有 `list.get(0)` 增加空检查 |
| P1 | Bug | 数字人端口统一（8083），修正前端 WS URL |
| P1 | 功能 | 确认 STT 服务（5000 端口）是否存在，或移除/替换调用 |
| P2 | 代码质量 | 空 catch 改为日志记录或合理处理 |
| P2 | 代码质量 | RuntimeException 改为业务异常 |
| P2 | 配置 | 为 AI、小程序后端添加 application-prod.yml |
| P3 | 配置 | 生产环境日志级别调整为 info/warn |

---

## 七、快速修复清单

1. **敏感信息**：创建 `.env.example`，所有密钥从环境变量读取。
2. **小程序**：`config.js` 改为 `export const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8082/api'`，或使用构建时替换。
3. **数字人**：统一端口为 8083，修正 index.html、main.js、WebSocketService.js。
4. **空指针**：对 `get(0)` 使用前增加 `CollectionUtils.isEmpty(list) ? defaultValue : list.get(0)`。
5. **生产配置**：各服务增加 `application-prod.yml`，数据库/Redis 使用 `localhost`。
