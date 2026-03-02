import request from '@/utils/request'

/**
 * 获取景区客流热力图数据
 */
export function getScenicHeatmapData(scenicId: number | string) {
  return request({
    url: `/scenic/heatmap/${scenicId}`,
    method: 'get'
  })
}






