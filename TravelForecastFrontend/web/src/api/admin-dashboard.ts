import request from '@/utils/request'

/**
 * 获取总览统计数据
 */
export function getOverviewStatistics() {
  return request({
    url: '/admin/dashboard/overview/statistics',
    method: 'get'
  })
}

/**
 * 获取平台运营趋势数据
 * @param timeRange 时间范围: week/month/quarter
 */
export function getTrendData(timeRange: string = 'week') {
  return request({
    url: '/admin/dashboard/trend',
    method: 'get',
    params: { timeRange }
  })
}

/**
 * 获取用户分布数据
 * @param type 分布类型: region/age/gender
 */
export function getDistributionData(type: string = 'region') {
  return request({
    url: '/admin/dashboard/distribution',
    method: 'get',
    params: { type }
  })
}

/**
 * 获取热门景区排行
 */
export function getHotScenicRankings() {
  return request({
    url: '/admin/dashboard/scenic/rankings',
    method: 'get'
  })
}

/**
 * 获取管理员通知
 */
export function getAdminNotices() {
  return request({
    url: '/admin/dashboard/notices',
    method: 'get'
  })
}

/**
 * 获取待办事项
 */
export function getPendingTasks() {
  return request({
    url: '/admin/dashboard/tasks',
    method: 'get'
  })
}

/**
 * 获取平台活动日志
 */
export function getActivityLogs() {
  return request({
    url: '/admin/dashboard/activity-logs',
    method: 'get'
  })
}

/**
 * 标记待办事项为已处理
 */
export function markTaskAsProcessed(taskId: number) {
  return request({
    url: `/admin/dashboard/tasks/${taskId}/process`,
    method: 'post'
  })
}

/**
 * 创建待办事项
 */
export function createTask(data: { title: string; description?: string }) {
  return request({
    url: '/admin/dashboard/tasks',
    method: 'post',
    data
  })
}

/**
 * 删除待办事项
 */
export function deleteTask(taskId: number) {
  return request({
    url: `/admin/dashboard/tasks/${taskId}`,
    method: 'delete'
  })
}

/**
 * 获取景区热度排行TOP10（数据分析页面）
 */
export function getScenicHotRankings() {
  return request({
    url: '/admin/dashboard/analytics/scenic-hot-rankings',
    method: 'get'
  })
}

/**
 * 获取用户活跃度分布
 */
export function getUserActivityDistribution() {
  return request({
    url: '/admin/dashboard/analytics/user-activity',
    method: 'get'
  })
}

/**
 * 获取核心指标数据
 */
export function getAnalyticsMetrics(timeRange: string) {
  return request({
    url: '/admin/dashboard/analytics/metrics',
    method: 'get',
    params: { timeRange }
  })
}

