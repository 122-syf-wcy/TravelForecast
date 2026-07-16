import { request } from './request'

/**
 * 签到
 */
export const checkIn = (userId) =>
  request({ url: '/user/checkin', method: 'POST', data: { userId } })

/**
 * 查询签到状态
 */
export const getCheckInStatus = (userId) =>
  request({ url: '/user/checkin/status', data: { userId } })

/**
 * 获取收藏列表
 */
export const getFavorites = (userId) =>
  request({ url: '/user/favorites', data: { userId } })

/**
 * 添加收藏
 */
export const addFavorite = (userId, scenicId) =>
  request({ url: '/user/favorites/add', method: 'POST', data: { userId, scenicId } })

/**
 * 取消收藏
 */
export const removeFavorite = (userId, scenicId) =>
  request({ url: `/user/favorites?userId=${userId}&scenicId=${scenicId}`, method: 'DELETE' })

/**
 * 检查是否已收藏
 */
export const checkFavorite = (userId, scenicId) =>
  request({ url: '/user/favorites/check', data: { userId, scenicId } })

/**
 * 获取用户优惠券列表
 */
export const getCoupons = (userId) =>
  request({ url: '/user/coupons', data: { userId } })

/**
 * 获取用户足迹（浏览历史）
 */
export const getFootprints = (userId) =>
  request({ url: '/user/footprints', data: { userId } })

/**
 * 记录足迹
 */
export const addFootprint = (userId, targetId, targetType, title) =>
  request({ url: '/user/footprints/add', method: 'POST', data: { userId, targetId, targetType, title } })

/**
 * 创建支付
 */
export const createPayment = (orderId, userId) =>
  request({ url: '/pay/create', method: 'POST', data: { orderId, userId } })

/**
 * 黔豆支付
 */
export const payWithPoints = (orderId, userId) =>
  request({ url: '/pay/points', method: 'POST', data: { orderId, userId } })

/**
 * 查询支付状态
 */
export const getPayStatus = (orderId) =>
  request({ url: '/pay/status', data: { orderId } })
