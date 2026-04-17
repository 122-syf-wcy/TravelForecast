// 环境判断：开发 vs 生产
const isDev = process.env.NODE_ENV === 'development'

// 服务器地址（统一走 Nginx 80 端口代理）
const SERVER_HOST = 'http://39.97.232.141'

// 小程序后端（通过 Nginx /miniprogram-api/ 代理到 8082）
export const API_BASE_URL = `${SERVER_HOST}/miniprogram-api/api`

// AI服务后端（通过 Nginx /ai-api/ 代理到 8081）
export const AI_API_BASE_URL = `${SERVER_HOST}/ai-api`

// 静态资源基础地址（图片等）
export const ASSET_BASE_URL = `${SERVER_HOST}/miniprogram-api`
