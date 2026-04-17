import request from '@/utils/request'
import { cachedRequest, generateCacheKey, clearCacheByPrefix } from '@/utils/requestCache'

export const contentApi = {
  // 获取首页配置（带缓存，30秒内不重复请求）
  getLanding() {
    const cacheKey = generateCacheKey('/content/landing')
    return cachedRequest(
      cacheKey,
      () => request({ url: '/content/landing', method: 'get' }),
      600000 // 缓存10分钟
    )
  },
  // 管理端 - 轮播图
  pageBanners(params?: any) { return request({ url: '/content/banners', method: 'get', params }) },
  saveBanner(data: any) { return data.id ? request({ url: `/content/banners/${data.id}`, method: 'put', data }) : request({ url: '/content/banners', method: 'post', data }) },
  deleteBanner(id: number|string) { return request({ url: `/content/banners/${id}`, method: 'delete' }) },
  // 获取公开轮播图（带缓存，5分钟内不重复请求）
  getBanners(params?: any) {
    const cacheKey = generateCacheKey('/content/banners/public', params)
    return cachedRequest(
      cacheKey,
      () => request({ url: '/content/banners/public', method: 'get', params }),
      600000 // 缓存10分钟
    )
  },
  
  // 管理端 - 实景预览（支持图片和视频）
  pageShowcases(params?: any) { return request({ url: '/content/showcases', method: 'get', params }) },
  saveShowcase(data: any) { return data.id ? request({ url: `/content/showcases/${data.id}`, method: 'put', data }) : request({ url: '/content/showcases', method: 'post', data }) },
  deleteShowcase(id: number|string) { return request({ url: `/content/showcases/${id}`, method: 'delete' }) },
  // 获取公开实景预览（带缓存）
  getShowcases(params?: any) {
    const cacheKey = generateCacheKey('/content/showcases/public', params)
    return cachedRequest(
      cacheKey,
      () => request({ url: '/content/showcases/public', method: 'get', params }),
      600000 // 缓存10分钟
    )
  }
}

// 清除首页内容相关缓存（管理员修改内容后调用）
export function clearContentCache() {
  clearCacheByPrefix('/content/')
}














