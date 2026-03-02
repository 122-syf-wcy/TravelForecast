import { request } from './request'

/**
 * 搜索景区
 */
export const searchAll = (keyword) =>
  request({ url: '/spots/list', data: { keyword } })
