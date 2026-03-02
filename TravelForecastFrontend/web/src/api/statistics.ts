import request from '@/utils/request'

/**
 * 获取旅游收入统计
 * @param range 时间范围: 1m/3m/6m/12m
 */
export function getTourismIncome(range: string = '12m') {
  return request({
    url: '/admin/statistics/tourism-income',
    method: 'get',
    params: { range }
  })
}

/**
 * 获取游客来源分析
 * @param range 时间范围: 1m/3m/6m/12m
 */
export function getVisitorSource(range: string = '12m') {
  return request({
    url: '/admin/statistics/visitor-source',
    method: 'get',
    params: { range }
  })
}

/**
 * 获取景区客流对比
 * @param year 年份
 */
export function getScenicComparison(year: string = '2025') {
  return request({
    url: '/admin/statistics/scenic-comparison',
    method: 'get',
    params: { year }
  })
}

/**
 * 获取详细统计数据（分页）
 * @param page 页码
 * @param pageSize 每页大小
 * @param keyword 搜索关键词
 */
export function getDetailedData(page: number = 1, pageSize: number = 10, keyword?: string) {
  return request({
    url: '/admin/statistics/detailed-data',
    method: 'get',
    params: { page, pageSize, keyword }
  })
}

