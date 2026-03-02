import request from '@/utils/request'

/**
 * 获取数据概览
 * @param scenicId 景区ID
 * @param startDate 开始日期 (YYYY-MM-DD)
 * @param endDate 结束日期 (YYYY-MM-DD)
 */
export function getAnalysisOverview(scenicId?: number, startDate?: string, endDate?: string) {
  return request({
    url: '/merchant/analysis/overview',
    method: 'get',
    params: { scenicId, startDate, endDate }
  })
}

/**
 * 获取游客量趋势
 * @param scenicId 景区ID
 * @param startDate 开始日期
 * @param endDate 结束日期
 * @param metric 指标 (daily, weekly, monthly)
 */
export function getVisitorTrend(scenicId?: number, startDate?: string, endDate?: string, metric: string = 'daily') {
  return request({
    url: '/merchant/analysis/visitor-trend',
    method: 'get',
    params: { scenicId, startDate, endDate, metric }
  })
}

/**
 * 获取收入分析
 * @param scenicId 景区ID
 * @param startDate 开始日期
 * @param endDate 结束日期
 * @param type 类型 (all, ticket, food, souvenir)
 */
export function getRevenueAnalysis(scenicId?: number, startDate?: string, endDate?: string, type: string = 'all') {
  return request({
    url: '/merchant/analysis/revenue-analysis',
    method: 'get',
    params: { scenicId, startDate, endDate, type }
  })
}

/**
 * 获取游客来源分析
 * @param scenicId 景区ID
 * @param startDate 开始日期
 * @param endDate 结束日期
 */
export function getVisitorSource(scenicId?: number, startDate?: string, endDate?: string) {
  return request({
    url: '/merchant/analysis/visitor-source',
    method: 'get',
    params: { scenicId, startDate, endDate }
  })
}

/**
 * 获取年龄分布
 * @param scenicId 景区ID
 * @param startDate 开始日期
 * @param endDate 结束日期
 */
export function getAgeDistribution(scenicId?: number, startDate?: string, endDate?: string) {
  return request({
    url: '/merchant/analysis/age-distribution',
    method: 'get',
    params: { scenicId, startDate, endDate }
  })
}

/**
 * 获取满意度分析
 * @param scenicId 景区ID
 * @param startDate 开始日期
 * @param endDate 结束日期
 */
export function getSatisfactionAnalysis(scenicId?: number, startDate?: string, endDate?: string) {
  return request({
    url: '/merchant/analysis/satisfaction-analysis',
    method: 'get',
    params: { scenicId, startDate, endDate }
  })
}

/**
 * 获取景点热度排行
 * @param scenicId 景区ID
 * @param startDate 开始日期
 * @param endDate 结束日期
 */
export function getSpotRanking(scenicId?: number, startDate?: string, endDate?: string) {
  return request({
    url: '/merchant/analysis/spot-ranking',
    method: 'get',
    params: { scenicId, startDate, endDate }
  })
}


