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
  request({ url: '/user/favorites', method: 'DELETE', data: { userId, scenicId } })

/**
 * 检查是否已收藏
 */
export const checkFavorite = (userId, scenicId) =>
  request({ url: '/user/favorites/check', data: { userId, scenicId } })

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
