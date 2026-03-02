# 旅游预测系统 - 前端重复请求修复方案

## 问题诊断

### 1. **主要问题：前端大量重复和错误请求**

#### 问题来源
用户反馈：前端控制台有"很多错误请求和重复请求"

#### 根本原因分析
1. **401 错误处理导致请求重复发送**
   - 原因：当 token 过期时，响应拦截器会刷新 token 后重新发送原请求
   - 但没有使用请求队列机制，多个并发请求都在各自刷新 token
   - 导致同一个请求被多个线程重试，造成重复发送

2. **Vite 代理配置复杂且冗余**
   - 原有配置：包含 `/images/proxy`, `/images`, `/api/v1`, `/api` 等多个规则
   - 问题：多个代理规则可能造成请求被重复转发或错误转发

3. **某些组件的数据加载逻辑**
   - Dashboard 在 onMounted 中使用 `Promise.all` 并行加载多个数据源
   - 但这本身不会导致重复，除非被多次触发

## 实施的修复方案

### 修复 1：优化 401 错误处理 ✅

**文件**: `src/utils/request.ts`

**修改内容**:
- 添加 `isRefreshingToken` 标志和 `refreshTokenQueue` 队列
- 实现请求队列机制：当 token 刷新中时，其他请求加入队列
- token 刷新完成后，批量处理队列中的请求
- **效果**：同一时刻只有一个刷新请求发送，避免重复

**核心逻辑**:
```typescript
if (isRefreshingToken) {
    // 已在刷新中，加入队列等待
    return new Promise((resolve) => {
        refreshTokenQueue.push((newToken: string) => {
            const original = error.config as any
            original.headers = original.headers || {}
            original.headers['Authorization'] = `Bearer ${newToken}`
            resolve(request(original))
        })
    })
}

// 标记为正在刷新，发送唯一的刷新请求
isRefreshingToken = true
// ... 刷新逻辑 ...
// 刷新完成后处理队列
queue.forEach(cb => cb(data.token))
```

### 修复 2：简化 Vite 代理配置 ✅

**文件**: `vite.config.ts`

**修改内容**:
- 删除冗余的代理规则（`/images`, `/images/proxy`, `/api/v1`）
- 只保留必要的 `/api` 代理规则
- 图片处理完全由后端 `ImageProxyController` 负责
- **效果**：减少代理层数，提高请求效率，避免代理冲突

**修改前**:
```typescript
proxy: {
    '/images/proxy': { ... },
    '/images': { ... },
    '/api/v1': { ... },
    '/api': { ... }
}
```

**修改后**:
```typescript
proxy: {
    '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path
    }
}
```

## 工作流程总结

### 后端处理流程
```
前端请求 (/api/scenic/list)
    ↓
Vite 代理 (/api → http://localhost:8080)
    ↓
后端接收 (http://localhost:8080/api/scenic/list)
    ↓
返回数据（包含 OSS URL）
    ↓
前端接收并通过 imageProxy.ts 转换为 /images/proxy?url=...
    ↓
后端 ImageProxyController 代理访问 OSS
    ↓
图片显示成功
```

### 请求队列机制
```
请求 1 发起
  ↓
获得 401 错误
  ↓
isRefreshingToken = true
发送刷新 token 请求
  ↓
请求 2、3、4 同时发起 → 也获得 401 错误
  ↓
检测 isRefreshingToken = true
加入 refreshTokenQueue
  ↓
请求 1 的 token 刷新完成
  ↓
批量处理队列 (请求 2、3、4)
更新新 token
重新发送
```

## 环境配置

### 本地开发环境
- `APP_ENV=local`
- `APP_FRONTEND_URL=http://localhost:3000`
- 后端 CORS 配置允许 localhost:3000

### 生产环境
- `APP_ENV=prod`
- `APP_FRONTEND_URL=<实际前端域名>`
- 后端 CORS 配置只允许配置的前端域名

## 启动脚本

### Windows 开发启动脚本
**文件**: `run-dev.bat`

**用法**:
```bash
cd f:\网页系统开发\旅游预测
run-dev.bat
```

**功能**:
- 自动检查 Node.js 环境
- 设置必要的环境变量
- 启动后端服务 (http://localhost:8080)
- 启动前端开发服务 (http://localhost:3000)
- 自动打开两个服务窗口

## 后端关键文件

### 1. CorsConfig.java
- **功能**: 环境感知的 CORS 配置
- **实现**: 根据 `APP_ENV` 环境变量动态允许不同的源

### 2. ImageProxyController.java
- **功能**: 处理 `/images` 和 `/images/proxy` 请求
- **实现**:
  - `proxyImage()`: 代理 OSS 图片请求
  - `getImage()`: 获取本地图片

### 3. application.yml
- **关键配置**:
  ```yaml
  app:
    env: ${APP_ENV:local}
    frontend-url: ${APP_FRONTEND_URL:http://localhost:3000}
  ```

## 前端关键文件

### 1. utils/request.ts
- 优化了 401 错误处理
- 实现请求队列机制

### 2. utils/imageProxy.ts
- 自动将 OSS URL 转换为后端代理 URL

### 3. vite.config.ts
- 简化的代理配置
- 只转发 `/api` 请求

## 测试步骤

1. **启动系统**
   ```bash
   cd f:\网页系统开发\旅游预测
   run-dev.bat
   ```

2. **检查后端日志**
   - 确保 Spring Boot 启动成功
   - 检查 CORS 配置输出

3. **打开浏览器开发者工具**
   - 打开 Network 选项卡
   - 访问 http://localhost:3000
   - 查看请求列表，确认没有重复的请求

4. **检查控制台日志**
   - 不应该看到重复的 ERROR 信息
   - 图片应该正常加载

## 可能需要进一步检查的项目

1. **后端日志**: 检查是否有多余的请求被处理
   ```
   tail -f logs/travel-prediction.log | grep "GET /api"
   ```

2. **前端组件**: 验证所有 Vue 组件的生命周期是否正确
   - 检查是否有组件被重复挂载
   - 检查是否有 watch 未正确清理依赖

3. **缓存设置**: 确保浏览器缓存设置正确
   - Network 选项卡中禁用缓存进行测试

## 常见问题

### Q1: 仍然看到重复的请求？
**A**: 
- 清除浏览器缓存和本地存储
- 重启前后端服务
- 检查浏览器控制台是否有 JavaScript 错误

### Q2: 图片仍然无法显示？
**A**:
- 确保后端 ImageProxyController 已编译并运行
- 检查后端日志中是否有 /images/proxy 请求
- 检查 vite.config.ts 中的代理是否正确

### Q3: 401 错误循环？
**A**:
- 确保 refreshToken 有效
- 检查 `authApi.refreshToken` 是否正确实现
- 查看浏览器存储中是否有有效的 token 和 refreshToken

## 后续优化建议

1. **请求去重**: 在某些场景下添加防抖或节流
2. **缓存策略**: 对不经常变化的数据实现本地缓存
3. **离线支持**: 考虑添加 Service Worker 支持
4. **性能监控**: 添加 APM 工具监控请求性能

---

最后更新: 2024年
