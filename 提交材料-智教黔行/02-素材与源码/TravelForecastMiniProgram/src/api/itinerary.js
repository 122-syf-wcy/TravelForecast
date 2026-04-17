import { request } from './request'

/**
 * 获取行程列表
 */
export const fetchItineraries = (userId) =>
  request({ url: '/itinerary/list', data: { userId } })

/**
 * 创建行程
 */
export const createItinerary = (userId, title, days, items) =>
  request({ url: '/itinerary/create', method: 'POST', data: { userId, title, days, items } })

/**
 * 删除行程
 */
export const deleteItinerary = (id) =>
  request({ url: `/itinerary/${id}`, method: 'DELETE' })
