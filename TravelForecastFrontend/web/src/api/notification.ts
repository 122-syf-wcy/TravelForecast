/**
 * 系统通知API
 */
import request from '@/utils/request'
import { cachedRequest, generateCacheKey } from '@/utils/requestCache'

/**
 * 获取用户端系统通知列表 - 带缓存，60秒内不重复请求
 */
export function getUserNotifications() {
  const cacheKey = generateCacheKey('/user/notifications')
  return cachedRequest(
    cacheKey,
    () => request({
      url: '/user/notifications',
      method: 'get'
    }),
    60000 // 缓存60秒
  )
}

/**
 * 获取商家系统通知列表
 */
export function getMerchantNotifications() {
  return request({
    url: '/merchant/notifications',
    method: 'get'
  })
}

/**
 * 标记通知为已读（商家端）
 */
export function markNotificationAsRead(id: number) {
  return request({
    url: `/merchant/notifications/${id}/read`,
    method: 'post'
  })
}

/**
 * 标记通知为已读（用户端）
 */
export function markUserNotificationAsRead(id: number) {
  return request({
    url: `/user/notifications/${id}/read`,
    method: 'post'
  })
}

/**
 * 获取所有系统通知（管理员）
 */
export function getAdminNotifications() {
  return request({
    url: '/admin/notifications',
    method: 'get'
  })
}

/**
 * 创建系统通知（管理员）
 */
export function createNotification(data: {
  title: string
  content: string
  type: string
  targetRole: string
  priority?: number
  expiresAt?: string
}) {
  return request({
    url: '/admin/notifications',
    method: 'post',
    data
  })
}

/**
 * 更新系统通知（管理员）
 */
export function updateNotification(id: number, data: {
  title: string
  content: string
  type: string
  targetRole: string
  priority?: number
  status?: string
  expiresAt?: string
}) {
  return request({
    url: `/admin/notifications/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除系统通知（管理员）
 */
export function deleteNotification(id: number) {
  return request({
    url: `/admin/notifications/${id}`,
    method: 'delete'
  })
}

