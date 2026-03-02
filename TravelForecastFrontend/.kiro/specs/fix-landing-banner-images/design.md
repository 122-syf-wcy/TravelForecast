# 设计文档

## 概述

本设计旨在修复Landing页面轮播图无法显示的问题。通过分析发现，问题的根本原因是：
1. 数据库中存储的OSS URL可能因为权限或签名过期无法直接访问
2. 前端的图片URL处理逻辑没有正确使用后端的图片代理服务
3. 缺少统一的图片URL转换和错误处理机制

解决方案是修改前端的图片URL处理逻辑，将OSS URL转换为通过后端ImageProxyController代理的URL，确保图片能够正常加载。

## 架构

### 当前架构问题

```
前端 Landing.vue
  ↓ 调用 contentApi.getBanners()
后端 BannerController.listPublic()
  ↓ 返回 OSS URL (https://smarttourism717.oss-cn-beijing.aliyuncs.com/banners/...)
前端 normalizeImageUrl()
  ↓ 直接返回 OSS URL
浏览器
  ↓ 尝试直接访问 OSS URL
  ✗ 失败（权限问题/签名过期）
```

### 目标架构

```
前端 Landing.vue
  ↓ 调用 contentApi.getBanners()
后端 BannerController.listPublic()
  ↓ 返回 OSS URL
前端 normalizeImageUrl()
  ↓ 转换为代理URL (/images/proxy?url=...)
浏览器
  ↓ 请求代理URL
后端 ImageProxyController.proxyImage()
  ↓ 从OSS获取图片
  ✓ 成功返回图片数据
```

## 组件和接口

### 前端组件

#### 1. imageProxy.ts 工具函数

**现有函数：**
- `normalizeImageUrl(url)`: 标准化图片URL

**需要修改：**
- 添加对OSS URL的检测和转换逻辑
- 将OSS URL转换为代理URL格式

**新增函数：**
- `isOssUrl(url)`: 检测是否是OSS URL
- `convertToProxyUrl(url)`: 将OSS URL转换为代理URL

#### 2. Landing.vue 组件

**需要修改：**
- 确保使用`normalizeImageUrl`处理所有图片URL
- 添加图片加载错误处理（@error事件）
- 显示占位图而不是"FAILED"错误

### 后端组件

#### 1. ImageProxyController

**现有功能：**
- `/images/proxy?url=...`: 代理远程OSS URL
- `/images/{filename}`: 提供本地图片

**需要验证：**
- URL解码逻辑是否正确
- OSS访问是否正常
- 错误处理是否完善

#### 2. BannerController

**现有功能：**
- `/content/banners/public`: 返回公开轮播图列表
- 对图片URL进行签名处理

**可能需要修改：**
- 考虑是否需要在后端直接返回代理URL
- 或者添加一个标识让前端知道需要使用代理

## 数据模型

### Banner 实体

```typescript
interface Banner {
  id: number | string
  title?: string
  image: string  // 图片URL（OSS URL或本地路径）
  link?: string
  sort?: number
  enabled?: boolean
}
```

### 图片URL格式

**输入格式（数据库存储）：**
- OSS完整URL: `https://smarttourism717.oss-cn-beijing.aliyuncs.com/banners/202510/1_1_1760789864543.jpg`
- OSS签名URL: `https://smarttourism717.oss-cn-beijing.aliyuncs.com/banners/...?Expires=...&Signature=...`
- 本地相对路径: `/images/meihuashan-1.jpg`

**输出格式（前端使用）：**
- 代理URL: `/images/proxy?url=https%3A%2F%2Fsmarttourism717.oss-cn-beijing.aliyuncs.com%2Fbanners%2F...`
- 本地路径: `/images/meihuashan-1.jpg`

## 正确性属性

*属性是一个特征或行为，应该在系统的所有有效执行中保持为真——本质上是关于系统应该做什么的正式声明。属性作为人类可读规范和机器可验证正确性保证之间的桥梁。*

### 属性反思

在编写属性之前，让我们审查一下是否有冗余：

- 属性1.1（显示所有启用的轮播图）和属性2.4（返回可访问的URL）有重叠，但1.1关注前端渲染，2.4关注后端API，它们验证不同的层面，应该保留
- 属性1.3（处理不同格式URL）和属性1.4（处理特殊字符）可以合并为一个更全面的URL处理属性
- 属性1.2和1.5都是关于错误处理的，可以合并

合并后的属性：

**Property 1: Banner显示完整性**
*对于任何*启用的banner列表，Landing页面应该为每个banner渲染一个图片元素，且没有加载失败状态
**验证: 需求 1.1**

**Property 2: URL处理通用性**
*对于任何*有效的图片URL（OSS URL、OSS签名URL、本地路径、包含特殊字符的URL），normalizeImageUrl函数应该返回一个可访问的URL
**验证: 需求 1.3, 1.4**

**Property 3: 错误降级友好性**
*对于任何*无效或无法访问的图片URL，系统应该显示占位图而不是错误信息
**验证: 需求 1.2, 1.5**

**Property 4: OSS对象名称规范性**
*对于任何*上传的轮播图文件，生成的OSS对象名称应该符合格式：`banners/{yyyyMM}/{sort}_{title}_{timestamp}{suffix}`
**验证: 需求 2.1**

**Property 5: URL格式一致性**
*对于任何*存储到数据库的轮播图URL，应该使用统一的格式（完整的OSS URL或相对路径）
**验证: 需求 2.2**

**Property 6: 签名URL编码正确性**
*对于任何*生成的OSS签名URL，其中的特殊字符应该被正确编码，且能够被后端代理正确解码
**验证: 需求 2.3**

**Property 7: API返回URL可访问性**
*对于任何*通过`/content/banners/public` API返回的图片URL，经过前端处理后应该能够成功加载图片
**验证: 需求 2.4**

## 错误处理

### 前端错误处理

1. **图片加载失败**
   - 监听`<el-image>`的`@error`事件
   - 显示默认占位图
   - 在控制台输出警告日志

2. **API请求失败**
   - 使用默认的本地图片作为fallback
   - 不阻塞页面渲染

3. **URL转换失败**
   - 返回原始URL或占位图URL
   - 记录错误日志

### 后端错误处理

1. **OSS访问失败**
   - 返回404状态码
   - 记录详细的错误日志（包括URL、错误原因）

2. **URL解码失败**
   - 返回400状态码
   - 记录警告日志

3. **代理请求超时**
   - 设置合理的超时时间（10s连接，30s读取）
   - 返回504状态码

## 测试策略

### 单元测试

**前端单元测试（使用Vitest）：**

1. `imageProxy.ts`工具函数测试
   - 测试`isOssUrl()`能正确识别OSS URL
   - 测试`convertToProxyUrl()`能正确转换URL
   - 测试`normalizeImageUrl()`处理各种URL格式
   - 测试特殊字符的编码处理

2. `Landing.vue`组件测试
   - 测试banner数据加载和渲染
   - 测试图片错误处理逻辑
   - 测试占位图显示

**后端单元测试（使用JUnit）：**

1. `ImageProxyController`测试
   - 测试代理URL的解码逻辑
   - 测试OSS图片获取
   - 测试错误处理

2. `BannerController`测试
   - 测试公开banner列表返回
   - 测试URL签名逻辑

### 属性测试

使用**fast-check**（JavaScript/TypeScript的属性测试库）进行属性测试：

**Property 1测试：**
```typescript
// 生成随机的banner列表，验证所有启用的banner都被正确渲染
fc.assert(fc.property(
  fc.array(bannerArbitrary),
  (banners) => {
    const enabledBanners = banners.filter(b => b.enabled)
    const rendered = renderLandingPage(banners)
    return enabledBanners.every(b => 
      rendered.hasImageElement(b.id) && 
      !rendered.hasFailedState(b.id)
    )
  }
))
```

**Property 2测试：**
```typescript
// 生成各种格式的URL，验证normalizeImageUrl能正确处理
fc.assert(fc.property(
  fc.oneof(
    ossUrlArbitrary,
    ossSignedUrlArbitrary,
    localPathArbitrary,
    urlWithSpecialCharsArbitrary
  ),
  (url) => {
    const normalized = normalizeImageUrl(url)
    return isAccessibleUrl(normalized)
  }
))
```

**Property 3测试：**
```typescript
// 生成无效URL，验证显示占位图
fc.assert(fc.property(
  invalidUrlArbitrary,
  (url) => {
    const result = handleImageError(url)
    return result === PLACEHOLDER_URL
  }
))
```

### 集成测试

1. **端到端测试**
   - 启动前后端服务
   - 访问Landing页面
   - 验证轮播图正常显示
   - 验证图片可以正常切换

2. **API集成测试**
   - 调用`/content/banners/public` API
   - 验证返回的URL格式
   - 验证通过代理URL能访问图片

### 测试配置

- 属性测试运行次数：每个属性至少100次迭代
- 测试超时时间：单元测试5秒，集成测试30秒
- 覆盖率目标：核心逻辑80%以上

## 实现细节

### 前端实现

#### 1. 修改 imageProxy.ts

```typescript
// 检测是否是OSS URL
export function isOssUrl(url: string): boolean {
  return url.includes('aliyuncs.com')
}

// 将OSS URL转换为代理URL
export function convertToProxyUrl(url: string): string {
  // 对URL进行编码
  const encodedUrl = encodeURIComponent(url)
  return `/images/proxy?url=${encodedUrl}`
}

// 修改 normalizeImageUrl 函数
export function normalizeImageUrl(url: string | undefined | null): string {
  if (!url) {
    return DEFAULT_PLACEHOLDER
  }

  // 如果是OSS URL，转换为代理URL
  if (isOssUrl(url)) {
    return convertToProxyUrl(url)
  }

  // 如果是本地静态图片，直接返回
  if (isLocalStaticImage(url)) {
    return url.startsWith('/') ? url : '/' + url
  }

  // 其他情况返回原URL
  return url
}
```

#### 2. 修改 Landing.vue

```vue
<template>
  <!-- Banner Carousel -->
  <div v-if="banners.length" class="mt-8 reveal">
    <el-carousel :interval="5000" arrow="always" height="280px">
      <el-carousel-item v-for="b in banners" :key="b.id">
        <a :href="b.link || 'javascript:void(0)'" @click.prevent="handleBannerClick(b)">
          <el-image 
            :src="b.image" 
            fit="cover" 
            class="w-full h-[280px] md:h-[360px] rounded-lg"
            @error="handleImageError"
          >
            <template #error>
              <div class="image-slot">
                <img :src="placeholderImage" alt="加载失败" />
              </div>
            </template>
          </el-image>
        </a>
      </el-carousel-item>
    </el-carousel>
  </div>
</template>

<script setup lang="ts">
const placeholderImage = '/images/placeholder.jpg'

const handleImageError = (event: Event) => {
  console.warn('图片加载失败:', event)
}
</script>
```

### 后端实现

#### 验证 ImageProxyController

确保代理逻辑正确处理URL编码：

```java
@GetMapping("/proxy")
public ResponseEntity<byte[]> proxyImage(@RequestParam String url) {
    try {
        log.info("代理图片请求: {}", url);
        
        // URL解码（Spring已经自动解码一次）
        String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.name());
        log.info("解码后的OSS URL: {}", decodedUrl);
        
        // 从OSS获取图片...
    }
}
```

## 部署考虑

### 开发环境
- 使用ImageProxyController代理所有OSS图片
- 本地图片直接从文件系统加载

### 生产环境
- 考虑使用CDN加速
- 配置OSS的CORS策略
- 使用Nginx反向代理优化性能

## 性能优化

1. **前端缓存**
   - 使用浏览器缓存（Cache-Control）
   - API响应缓存（5分钟）

2. **后端优化**
   - 图片代理添加缓存头
   - 考虑使用Redis缓存图片数据

3. **图片优化**
   - 使用WebP格式
   - 压缩图片大小
   - 使用响应式图片

## 安全考虑

1. **URL验证**
   - 只允许代理aliyuncs.com域名的URL
   - 防止SSRF攻击

2. **访问控制**
   - 验证OSS访问权限
   - 限制代理请求频率

3. **错误信息**
   - 不暴露敏感的系统信息
   - 统一的错误响应格式
