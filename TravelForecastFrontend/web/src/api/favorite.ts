import request from '@/utils/request'

/**
 * 添加收藏
 */
export function addFavorite(scenicId: number) {
  return request({
    url: `/favorites/${scenicId}`,
    method: 'post'
  })
}

/**
 * 取消收藏
 */
export function removeFavorite(scenicId: number) {
  return request({
    url: `/favorites/${scenicId}`,
    method: 'delete'
  })
}

/**
 * 检查是否已收藏
 */
export function checkFavorite(scenicId: number) {
  return request({
    url: `/favorites/check/${scenicId}`,
    method: 'get'
  })
}

/**
 * 批量检查收藏状态
 * @param scenicIds 景区ID数组
 * @returns Map<scenicId, isFavorited>
 */
export function checkFavoriteBatch(scenicIds: number[]) {
  return request<Record<number, boolean>>({
    url: '/favorites/check-batch',
    method: 'post',
    data: scenicIds
  })
}

/**
 * 获取景区收藏统计
 */
export function getFavoriteStats(scenicId: number) {
  return request({
    url: `/favorites/stats/${scenicId}`,
    method: 'get'
  })
}

