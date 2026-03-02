# 游韵华章 - API网关服务

## 📋 项目简介

Travel Forecast Gateway 是游韵华章旅游预测系统的统一API网关服务，作为所有微服务的统一入口，提供路由转发、统一认证、限流熔断、请求日志等功能。

## 🎯 核心功能

### 1. 统一入口
- 所有微服务请求通过网关路由
- 支持动态路由配置
- 支持路径重写和前缀剥离

### 2. 统一认证
- JWT Token验证
- 白名单机制（登录、注册等公开接口）
- 用户信息传递到下游服务

### 3. 限流熔断
- Redis实现的令牌桶限流
- Resilience4j熔断器保护
- 不同服务差异化限流策略

### 4. 请求日志
- 记录所有API调用
- 请求/响应信息记录
- 性能监控（响应时间）

### 5. CORS处理
- 跨域请求支持
- 灵活的域名白名单配置

## 🏗️ 技术栈

- **Spring Cloud Gateway** - 网关框架
- **Spring Boot 3.2.0** - 基础框架
- **Redis** - 限流和缓存
- **JWT** - 统一认证
- **Resilience4j** - 熔断器
- **WebFlux** - 响应式编程

## 📁 项目结构

```
TravelForecastGateway/
├── src/
│   ├── main/
│   │   ├── java/com/travel/gateway/
│   │   │   ├── GatewayApplication.java      # 主启动类
│   │   │   ├── config/                      # 配置类
│   │   │   │   ├── GatewayProperties.java   # 网关配置属性
│   │   │   │   └── CorsConfig.java          # CORS配置
│   │   │   ├── filter/                      # 过滤器
│   │   │   │   ├── JwtAuthenticationFilter.java  # JWT认证过滤器
│   │   │   │   └── GatewayLogFilter.java    # 日志过滤器
│   │   │   ├── util/                        # 工具类
│   │   │   │   └── JwtUtil.java            # JWT工具类
│   │   │   └── exception/                   # 异常处理
│   │   │       └── GatewayExceptionHandler.java
│   │   └── resources/
│   │       └── application.yml              # 配置文件
│   └── test/
└── pom.xml                                   # Maven配置
```

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Redis 6.0+

### 启动步骤

1. **启动Redis**
   ```bash
   redis-server
   ```

2. **修改配置**
   
   编辑 `src/main/resources/application.yml`，根据实际情况修改：
   - Redis连接信息
   - 各微服务的地址和端口
   - JWT密钥（需与业务后端保持一致）

3. **编译运行**
   ```bash
   mvn clean package
   mvn spring-boot:run
   ```

4. **验证启动**
   
   访问健康检查接口：
   ```
   http://localhost:8888/actuator/health
   ```

## 🔧 配置说明

### 路由配置

网关支持以下路由：

| 路由ID | 路径前缀 | 目标服务 | 限流速率 |
|--------|---------|---------|---------|
| business-service | `/api/**` | http://localhost:8080 | 100/秒 |
| ai-service | `/ai-api/**` | http://localhost:8081 | 50/秒 |
| miniprogram-service | `/miniprogram-api/**` | http://localhost:8082 | 80/秒 |
| prediction-service | `/prediction-api/**` | http://localhost:8001 | 30/秒 |
| digital-human-service | `/digital-human-api/**` | http://localhost:8000 | 40/秒 |

### 认证白名单

以下路径不需要JWT认证：

- `/api/auth/login` - 登录
- `/api/auth/register` - 注册
- `/api/auth/captcha` - 验证码
- `/api/auth/wechat/login` - 微信登录
- `/miniprogram-api/**` - 小程序接口（小程序有自己的认证）
- `/actuator/**` - 监控端点

### 限流配置

限流采用Redis令牌桶算法：

- **replenishRate**: 每秒补充的令牌数（允许的请求数）
- **burstCapacity**: 令牌桶容量（突发请求数）
- **requestedTokens**: 每个请求消耗的令牌数（通常为1）

## 📝 API使用示例

### 1. 登录（无需Token）

```bash
curl -X POST http://localhost:8888/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456"
  }'
```

### 2. 调用业务接口（需要Token）

```bash
curl -X GET http://localhost:8888/api/user/info \
  -H "Authorization: Bearer <your-jwt-token>"
```

### 3. 调用AI服务

```bash
curl -X POST http://localhost:8888/ai-api/predict \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "预测明天的天气"
  }'
```

## 🔍 监控端点

网关提供以下监控端点（需要配置访问权限）：

- `/actuator/health` - 健康检查
- `/actuator/gateway/routes` - 查看所有路由
- `/actuator/metrics` - 指标监控

## 🛡️ 安全建议

1. **生产环境配置**
   - 修改JWT密钥为强随机字符串
   - 限制CORS允许的域名（不要使用`*`）
   - 配置HTTPS

2. **限流策略**
   - 根据实际业务调整限流参数
   - 监控限流触发情况
   - 为重要接口设置更高的限流阈值

3. **日志管理**
   - 生产环境建议关闭响应日志（数据量大）
   - 配置日志轮转和清理策略
   - 敏感信息不要记录到日志

## 🐛 常见问题

### 1. 连接被拒绝

**问题**: 网关无法连接到后端服务

**解决**: 
- 检查后端服务是否启动
- 检查`application.yml`中的服务地址和端口是否正确
- 检查防火墙设置

### 2. Token验证失败

**问题**: 返回401未授权错误

**解决**:
- 确认Token格式正确：`Bearer <token>`
- 确认JWT密钥与业务后端一致
- 检查Token是否过期

### 3. 限流触发

**问题**: 返回429 Too Many Requests

**解决**:
- 降低请求频率
- 调整限流配置参数
- 检查是否有异常请求

## 📄 许可证

本项目为游韵华章旅游预测系统的一部分。

## 👥 贡献

欢迎提交Issue和Pull Request！

---

**游韵华章 - 让旅行更智能，让预测更精准** 🌟

