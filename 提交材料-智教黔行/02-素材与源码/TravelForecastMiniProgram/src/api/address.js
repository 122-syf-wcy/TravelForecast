import { request } from './request'

/**
 * 获取地址列表
 */
export const fetchAddressList = (userId) =>
  request({ url: '/address/list', data: { userId } })

/**
 * 保存地址（新增或编辑）
 */
export const saveAddress = (data) =>
  request({ url: '/address/save', method: 'POST', data })

/**
 * 删除地址
 */
export const deleteAddress = (id, userId) =>
  request({ url: `/address/${id}?userId=${userId}`, method: 'DELETE' })

/**
 * 设为默认地址
 */
export const setDefaultAddress = (id, userId) =>
  request({ url: `/address/${id}/default?userId=${userId}`, method: 'PUT' })
