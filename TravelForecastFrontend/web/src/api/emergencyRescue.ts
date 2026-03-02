import request from '@/utils/request'

// 紧急救援相关接口 - 已修复API路径问题

export interface EmergencyRescue {
  id?: number
  userId?: number
  scenicId: number
  rescueType: string  // MEDICAL, LOST, ACCIDENT, OTHER
  status?: string  // PENDING, PROCESSING, COMPLETED, CANCELLED
  contactName: string
  contactPhone: string
  location: string
  latitude?: number
  longitude?: number
  description?: string
  emergencyLevel?: string  // URGENT, NORMAL
  handlerId?: number
  handlerName?: string
  handleTime?: string
  completeTime?: string
  handleNotes?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 创建救援请求（用户端）
 */
export const createRescue = (data: EmergencyRescue) => {
  return request({
    url: '/emergency-rescue/create',
    method: 'post',
    data
  })
}

/**
 * 获取用户的救援记录（用户端）
 */
export const getMyRescues = () => {
  return request({
    url: '/emergency-rescue/my-rescues',
    method: 'get'
  })
}

/**
 * 获取景区的救援列表（商家端）
 */
export const getRescuesByScenicId = (scenicId: number) => {
  return request({
    url: `/emergency-rescue/scenic/${scenicId}`,
    method: 'get'
  })
}

/**
 * 商家获取其管理的景区的所有救援列表（商家端）
 */
export const getMerchantRescues = () => {
  return request({
    url: '/emergency-rescue/merchant/list',
    method: 'get'
  })
}

/**
 * 处理救援请求（商家端）
 */
export const handleRescue = (rescueId: number, handlerName: string) => {
  return request({
    url: `/emergency-rescue/handle/${rescueId}`,
    method: 'post',
    data: { handlerName }
  })
}

/**
 * 完成救援请求（商家端）
 */
export const completeRescue = (rescueId: number, handleNotes: string) => {
  return request({
    url: `/emergency-rescue/complete/${rescueId}`,
    method: 'post',
    data: { handleNotes }
  })
}

/**
 * 取消救援请求
 */
export const cancelRescue = (rescueId: number) => {
  return request({
    url: `/emergency-rescue/cancel/${rescueId}`,
    method: 'post'
  })
}

/**
 * 获取景区救援统计信息（商家端）
 */
export const getRescueStats = (scenicId: number) => {
  return request({
    url: `/emergency-rescue/stats/${scenicId}`,
    method: 'get'
  })
}

/**
 * 获取救援详情
 */
export const getRescueDetail = (rescueId: number) => {
  return request({
    url: `/emergency-rescue/detail/${rescueId}`,
    method: 'get'
  })
}

/**
 * 删除救援记录（商家端）
 */
export const deleteRescue = (rescueId: number) => {
  return request({
    url: `/emergency-rescue/delete/${rescueId}`,
    method: 'delete'
  })
}