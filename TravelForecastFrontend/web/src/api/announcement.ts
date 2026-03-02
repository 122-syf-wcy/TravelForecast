import request from '@/utils/request'
import { cachedRequest, generateCacheKey } from '@/utils/requestCache'

// 获取景区公告列表（用户端）- 带缓存，60秒内不重复请求
export const getScenicAnnouncements = (params: { scenicId: number }) => {
  const cacheKey = generateCacheKey('/announcements', params)
  return cachedRequest(
    cacheKey,
    () => request({
      url: '/announcements',  // 修复：移除多余的 /api 前缀
      method: 'get',
      params
    }),
    60000 // 缓存60秒
  )
}

// 获取商家公告列表
export const getMerchantAnnouncements = (params: { page?: number; size?: number }) => {
  return request({
    url: '/merchant/announcements',
    method: 'get',
    params
  })
}

// 创建公告
export const createAnnouncement = (data: {
  title: string
  content: string
  type?: string
  priority?: number
  startTime?: string
  endTime?: string
}) => {
  return request({
    url: '/merchant/announcements',
    method: 'post',
    data
  })
}

// 更新公告
export const updateAnnouncement = (id: number, data: {
  title?: string
  content?: string
  type?: string
  status?: string
  priority?: number
  startTime?: string
  endTime?: string
}) => {
  return request({
    url: `/merchant/announcements/${id}`,
    method: 'put',
    data
  })
}

// 删除公告
export const deleteAnnouncement = (id: number) => {
  return request({
    url: `/merchant/announcements/${id}`,
    method: 'delete'
  })
}

// 获取公告详情
export const getAnnouncementDetail = (id: number) => {
  return request({
    url: `/merchant/announcements/${id}`,
    method: 'get'
  })
}

