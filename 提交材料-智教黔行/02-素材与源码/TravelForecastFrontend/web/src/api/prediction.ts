import request from '@/utils/request'
import { cachedRequest, generateCacheKey } from '@/utils/requestCache'

/**
 * 获取未来7天总客流预测（管理员端）- 带缓存，5分钟内不重复请求
 * @param model 预测模型类型
 */
export function getNext7DaysTotal(model?: string) {
  const cacheKey = generateCacheKey('/admin/prediction/next-7-days-total', { model })
  return cachedRequest(
    cacheKey,
    () => request({
      url: '/admin/prediction/next-7-days-total',
      method: 'get',
      params: { model }
    }),
    300000 // 缓存5分钟
  )
}

/**
 * 获取景区客流趋势预测（管理员端、商家端、用户端共用）- 带缓存，5分钟内不重复请求
 * @param scenicId 景区ID，null表示全部景区
 * @param model 预测模型类型（dual_stream/lstm/arima）
 * @param days 预测天数（默认7天）
 * @param factors 预测因子列表
 */
export function getScenicTrend(scenicId?: number, model?: string, days?: number, factors?: string[]) {
  const cacheKey = generateCacheKey('/admin/prediction/scenic-trend', { scenicId, model, days, factors })
  return cachedRequest(
    cacheKey,
    () => request({
      url: '/admin/prediction/scenic-trend',
      method: 'get',
      params: { scenicId, model, days, factors: factors?.join(',') }
    }),
    5000 // 缓存5秒（开发调试阶段缩短缓存，避免降级数据被长期锁定）
  )
}

/**
 * 获取小时客流分布预测（所有端共用）- 带缓存，5分钟内不重复请求
 * @param date 日期
 * @param scenicId 景区ID
 * @param model 预测模型类型
 */
export function getHourlyDistribution(date?: string, scenicId?: number, model?: string) {
  const cacheKey = generateCacheKey('/admin/prediction/hourly-distribution', { date, scenicId, model })
  return cachedRequest(
    cacheKey,
    () => request({
      url: '/admin/prediction/hourly-distribution',
      method: 'get',
      params: { date, scenicId, model }
    }),
    300000 // 缓存5分钟
  )
}

/**
 * 获取高峰预警 - 带缓存，2分钟内不重复请求
 */
export function getPeakWarning() {
  const cacheKey = generateCacheKey('/admin/prediction/peak-warning')
  return cachedRequest(
    cacheKey,
    () => request({
      url: '/admin/prediction/peak-warning',
      method: 'get'
    }),
    120000 // 缓存2分钟
  )
}

/**
 * 获取预测详情列表 - 带缓存，2分钟内不重复请求
 * @param page 页码
 * @param pageSize 每页大小
 */
export function getPredictionDetails(page: number = 1, pageSize: number = 10) {
  const cacheKey = generateCacheKey('/admin/prediction/details', { page, pageSize })
  return cachedRequest(
    cacheKey,
    () => request({
      url: '/admin/prediction/details',
      method: 'get',
      params: { page, pageSize }
    }),
    120000 // 缓存2分钟
  )
}

/**
 * 获取最佳游览时段 - 带缓存，5分钟内不重复请求
 */
export function getBestVisitTime() {
  const cacheKey = generateCacheKey('/admin/prediction/best-visit-time')
  return cachedRequest(
    cacheKey,
    () => request({
      url: '/admin/prediction/best-visit-time',
      method: 'get'
    }),
    300000 // 缓存5分钟
  )
}

// ============= 用户端和商家端专用API =============

/**
 * 获取客流预测（用户端/商家端）
 * 返回未来7天的详细预测数据
 * @param scenicId 景区ID
 * @param model 预测模型
 * @param days 预测天数
 * @param factors 预测因子
 */
export function getFlowPrediction(scenicId: number, model?: string, days?: number, factors?: string[]) {
  return request({
    url: '/admin/prediction/scenic-trend',
    method: 'get',
    params: { scenicId, model, days, factors: factors?.join(',') }
  }).then((res: any) => {
    // 转换数据格式以适应用户端/商家端的需求
    if (res.code === 200 && res.data) {
      const data = res.data
      const predictions = data.dates?.map((date: string, index: number) => ({
        date: date,
        expectedFlow: data.predictions[index],
        confidence: data.confidences[index]
      })) || []

      // 计算平均置信度
      const avgConfidence = data.confidences?.length > 0
        ? data.confidences.reduce((sum: number, c: number) => sum + c, 0) / data.confidences.length
        : 0.94

      return {
        predictions,
        confidence: avgConfidence
      }
    }
    return { predictions: [], confidence: 0.94 }
  })
}

/**
 * 获取小时预测（用户端/商家端）
 * @param date 日期
 * @param scenicId 景区ID
 */
export function getHourlyPrediction(date?: string, scenicId?: number) {
  return getHourlyDistribution(date, scenicId)
}
