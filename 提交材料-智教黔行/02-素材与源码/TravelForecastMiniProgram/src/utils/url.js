import { ASSET_BASE_URL } from '@/config'

export const resolveAssetUrl = (url) => {
  if (!url) return ''
  // 修正后端返回的 localhost URL 为 Nginx 代理路径
  if (url.includes('localhost:8082')) {
    const path = url.replace(/https?:\/\/localhost:8082/, '')
    return ASSET_BASE_URL + path
  }
  if (url.includes('localhost:8081')) {
    const path = url.replace(/https?:\/\/localhost:8081/, '')
    return 'http://39.97.232.141/ai-api' + path
  }
  if (url.startsWith('http')) return url
  if (url.startsWith('/')) return ASSET_BASE_URL + url
  return url
}
