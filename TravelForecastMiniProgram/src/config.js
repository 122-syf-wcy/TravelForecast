/**
 * 小程序环境配置
 *
 * 注意：微信小程序对 `request`、`socket` 合法域名有严格要求——
 * 必须在微信公众平台配置已 ICP 备案的 HTTPS 域名，本地调试时可在开发者工具
 * 「详情 -> 不校验合法域名」中临时放开。
 *
 * 开发联调可直连服务器 IP；生产/真机环境必须使用已备案的 HTTPS 域名。
 * 如需临时切换，可通过 Vite/UniApp 环境变量覆盖。
 */

const PROD_SERVER_HOST = 'https://travel.dongsiwei.com'
// 微信小程序要求 request/image 均走 HTTPS（即便开发构建）
// 本地联调请通过 VITE_APP_SERVER_HOST / VUE_APP_SERVER_HOST 覆盖为本机地址
const DEV_SERVER_HOST = 'https://travel.dongsiwei.com'

const rawEnv = typeof process !== 'undefined' && process.env ? process.env : {}

function readEnv(key) {
  const value = rawEnv[key]
  return typeof value === 'string' && value.length > 0 ? value : null
}

function isProduction() {
  const nodeEnv = readEnv('NODE_ENV')
  if (nodeEnv) {
    return nodeEnv === 'production'
  }
  const uniNodeEnv = readEnv('UNI_NODE_ENV')
  if (uniNodeEnv) {
    return uniNodeEnv === 'production'
  }
  return false
}

const SERVER_HOST =
  readEnv('VITE_APP_SERVER_HOST') ||
  readEnv('VUE_APP_SERVER_HOST') ||
  (isProduction() ? PROD_SERVER_HOST : DEV_SERVER_HOST)

export const API_BASE_URL = `${SERVER_HOST}/miniprogram-api/api`

export const AI_API_BASE_URL = `${SERVER_HOST}/ai-api`

export const DIGITAL_HUMAN_API_BASE_URL = `${SERVER_HOST}/digital-human-api/api`

export const ASSET_BASE_URL = `${SERVER_HOST}/miniprogram-api`

export const SERVER_HOST_URL = SERVER_HOST
