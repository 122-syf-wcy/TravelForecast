import request from '@/utils/request'
import { cachedRequest, generateCacheKey } from '@/utils/requestCache'

// 新闻资讯接口

/**
 * 商家端 - 获取新闻列表
 */
export function getMerchantNewsList(params: {
  page: number
  size: number
  keyword?: string
  status?: string
}) {
  return request({
    url: '/merchant/news',
    method: 'get',
    params
  })
}

/**
 * 商家端 - 获取新闻详情
 */
export function getMerchantNewsDetail(id: number) {
  return request({
    url: `/merchant/news/${id}`,
    method: 'get'
  })
}

/**
 * 商家端 - 发布新闻
 */
export function publishNews(data: any) {
  return request({
    url: '/merchant/news',
    method: 'post',
    data
  })
}

/**
 * 商家端 - 更新新闻
 */
export function updateNews(id: number, data: any) {
  return request({
    url: `/merchant/news/${id}`,
    method: 'put',
    data
  })
}

/**
 * 商家端 - 删除新闻
 */
export function deleteNews(id: number) {
  return request({
    url: `/merchant/news/${id}`,
    method: 'delete'
  })
}

/**
 * 用户端 - 获取新闻列表（分页）- 带缓存，60秒内不重复请求
 */
export function getNewsList(params: {
  page: number
  size: number
  keyword?: string
  scenicId?: number
}) {
  const cacheKey = generateCacheKey('/news', params)
  return cachedRequest(
    cacheKey,
    () => request({
      url: '/news',
      method: 'get',
      params
    }),
    60000 // 缓存60秒
  )
}

/**
 * 用户端 - 获取新闻详情 - 带缓存，60秒内不重复请求
 */
export function getNewsDetail(id: number) {
  const cacheKey = generateCacheKey(`/news/${id}`)
  return cachedRequest(
    cacheKey,
    () => request({
      url: `/news/${id}`,
      method: 'get'
    }),
    60000 // 缓存60秒
  )
}

/**
 * 用户端 - 获取热门新闻 - 带缓存，60秒内不重复请求
 */
export function getHotNews(params: {
  page: number
  size: number
}) {
  const cacheKey = generateCacheKey('/news/hot', params)
  return cachedRequest(
    cacheKey,
    () => request({
      url: '/news/hot',
      method: 'get',
      params
    }),
    60000 // 缓存60秒
  )
}

