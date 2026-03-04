// 环境判断：开发 vs 生产
const isDev = process.env.NODE_ENV === 'development'

// 服务器地址
const SERVER_HOST = isDev ? 'http://localhost' : 'http://39.97.232.141'

// 小程序后端（所有业务接口统一走8082）
export const API_BASE_URL = `${SERVER_HOST}:8082/api`

// AI服务后端（公用，对话+语音合成+研学教育+行程规划）
export const AI_API_BASE_URL = `${SERVER_HOST}:8081/ai-api`

// 静态资源基础地址（图片等）
export const ASSET_BASE_URL = `${SERVER_HOST}:8082`
