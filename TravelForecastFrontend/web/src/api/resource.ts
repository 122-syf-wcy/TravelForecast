/**
 * 资源管理API（活动和设施）
 */
import request from '@/utils/request'
import { cachedRequest, generateCacheKey } from '@/utils/requestCache'

// ==================== 活动管理 ====================

/**
 * 获取商家活动列表
 */
export const getMerchantActivities = (params: {
  page?: number
  size?: number
  status?: string
  category?: string
}) => {
  return request({
    url: '/merchant/activities',
    method: 'get',
    params
  })
}

/**
 * 获取活动详情
 */
export const getActivityDetail = (id: number) => {
  return request({
    url: `/merchant/activities/${id}`,
    method: 'get'
  })
}

/**
 * 创建活动
 */
export const createActivity = (data: any) => {
  return request({
    url: '/merchant/activities',
    method: 'post',
    data
  })
}

/**
 * 更新活动
 */
export const updateActivity = (id: number, data: any) => {
  return request({
    url: `/merchant/activities/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除活动
 */
export const deleteActivity = (id: number) => {
  return request({
    url: `/merchant/activities/${id}`,
    method: 'delete'
  })
}

/**
 * 推广活动到新闻资讯
 */
export const promoteActivity = (id: number) => {
  return request({
    url: `/merchant/activities/${id}/promote`,
    method: 'post'
  })
}

/**
 * 取消推广活动
 */
export const unpromoteActivity = (id: number) => {
  return request({
    url: `/merchant/activities/${id}/unpromote`,
    method: 'post'
  })
}

/**
 * 获取活动统计信息
 */
export const getActivityStats = () => {
  return request({
    url: '/merchant/activities/stats',
    method: 'get'
  })
}

// ==================== 设施管理 ====================

/**
 * 获取商家设施列表
 */
export const getMerchantFacilities = (params: {
  page?: number
  size?: number
  status?: string
  category?: string
}) => {
  return request({
    url: '/merchant/facilities',
    method: 'get',
    params
  })
}

/**
 * 获取设施详情
 */
export const getFacilityDetail = (id: number) => {
  return request({
    url: `/merchant/facilities/${id}`,
    method: 'get'
  })
}

/**
 * 创建设施
 */
export const createFacility = (data: any) => {
  return request({
    url: '/merchant/facilities',
    method: 'post',
    data
  })
}

/**
 * 更新设施
 */
export const updateFacility = (id: number, data: any) => {
  return request({
    url: `/merchant/facilities/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除设施
 */
export const deleteFacility = (id: number) => {
  return request({
    url: `/merchant/facilities/${id}`,
    method: 'delete'
  })
}

/**
 * 获取设施统计信息
 */
export const getFacilityStats = () => {
  return request({
    url: '/merchant/facilities/stats',
    method: 'get'
  })
}

// ==================== 用户端API ====================

// ==================== 子景点管理 ====================

/**
 * 获取商家主景区下的子景点列表
 */
export const getMerchantSubSpots = () => {
  return request({
    url: '/merchant/sub-spots',
    method: 'get'
  })
}

/**
 * 新增子景点
 */
export const createSubSpot = (data: {
  name: string
  description?: string
  imageUrl?: string
  latitude?: number
  longitude?: number
  tags?: string
  openingHours?: string
  sortOrder?: number
}) => {
  return request({
    url: '/merchant/sub-spots',
    method: 'post',
    data
  })
}

/**
 * 更新子景点
 */
export const updateSubSpot = (id: number, data: any) => {
  return request({
    url: `/merchant/sub-spots/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除子景点
 */
export const deleteSubSpot = (id: number) => {
  return request({
    url: `/merchant/sub-spots/${id}`,
    method: 'delete'
  })
}

// ==================== 用户端API ====================

/**
 * 用户端获取正在进行的活动 - 带缓存，60秒内不重复请求
 */
export const getOngoingActivities = (params: {
  scenicId?: number
  page?: number
  size?: number
}) => {
  const cacheKey = generateCacheKey('/activities/ongoing', params)
  return cachedRequest(
    cacheKey,
    () => request({
      url: '/activities/ongoing',
      method: 'get',
      params
    }),
    60000 // 缓存60秒
  )
}

/**
 * 用户端查询景区设施 - 带缓存，60秒内不重复请求
 */
export const getScenicFacilities = (params: {
  scenicId?: number
  category?: string
  page?: number
  size?: number
}) => {
  const cacheKey = generateCacheKey('/facilities', params)
  return cachedRequest(
    cacheKey,
    () => request({
      url: '/facilities',
      method: 'get',
      params
    }),
    60000 // 缓存60秒
  )
}
