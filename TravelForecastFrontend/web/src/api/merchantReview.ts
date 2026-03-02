import request from '@/utils/request'

// 商家端评论管理API

/**
 * 获取商家评论列表
 */
export function getMerchantReviews(params: {
  page?: number
  size?: number
  status?: string
  keyword?: string
  sortBy?: string
}) {
  return request({
    url: '/merchant/reviews',
    method: 'get',
    params
  })
}

/**
 * 获取评论统计
 */
export function getReviewStats() {
  return request({
    url: '/merchant/reviews/stats',
    method: 'get'
  })
}

/**
 * 回复评论
 */
export function replyReview(reviewId: number, content: string) {
  return request({
    url: `/merchant/reviews/${reviewId}/reply`,
    method: 'post',
    data: { content }
  })
}

/**
 * 删除评论
 */
export function deleteReview(reviewId: number) {
  return request({
    url: `/merchant/reviews/${reviewId}`,
    method: 'delete'
  })
}

/**
 * 标记评论为精选
 */
export function featureReview(reviewId: number) {
  return request({
    url: `/merchant/reviews/${reviewId}/feature`,
    method: 'post'
  })
}

/**
 * 取消精选评论
 */
export function unfeatureReview(reviewId: number) {
  return request({
    url: `/merchant/reviews/${reviewId}/unfeature`,
    method: 'post'
  })
}

/**
 * 举报评论
 */
export function reportReview(reviewId: number, reason?: string) {
  return request({
    url: `/merchant/reviews/${reviewId}/report`,
    method: 'post',
    data: { reason }
  })
}

