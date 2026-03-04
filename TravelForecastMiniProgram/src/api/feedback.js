import { request } from './request'

/**
 * 提交意见反馈
 */
export const submitFeedback = (data) =>
  request({ url: '/feedback/submit', method: 'POST', data })
