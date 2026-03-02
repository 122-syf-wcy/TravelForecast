import request from '@/utils/request'

const isDev = typeof window !== 'undefined' && /localhost|127\.0\.0\.1/.test(window.location.hostname)

export type ExportPayload = {
  resource: string
  format: 'csv' | 'xlsx'
  filters?: Record<string, any>
}

export const adminApi = {
  exportData(data: ExportPayload) {
    return request({ url: '/v1/admin/exports', method: 'post', data, responseType: 'json' })
  },
  // 预留：通知列表与已读接口
  getNotifications(params?: { page?: number; size?: number; onlyUnread?: boolean }) {
    return request({ url: '/v1/notifications', method: 'get', params })
  },
  markAllNotificationsRead() {
    return request({ url: '/v1/notifications/read-all', method: 'post' })
  },
  markNotificationRead(id: string | number) {
    return request({ url: `/v1/notifications/${id}/read`, method: 'post' })
  },
  deleteNotification(id: string | number) {
    return request({ url: `/v1/notifications/${id}`, method: 'delete' })
  },
  // 待办事项管理
  getPendingTasks(params?: { status?: 'all' | 'pending' | 'processed' }) {
    return request({ url: '/v1/admin/pending-tasks', method: 'get', params })
  },
  updatePendingTask(id: string | number, data: { processed: boolean }) {
    return request({ url: `/v1/admin/pending-tasks/${id}`, method: 'put', data })
  },
  createPendingTask(data: { title: string; description: string; submitter: string }) {
    return request({ url: '/v1/admin/pending-tasks', method: 'post', data })
  },
  deletePendingTask(id: string | number) {
    return request({ url: `/v1/admin/pending-tasks/${id}`, method: 'delete' })
  },

  // 用户管理
  getUserList(params: {
    page: number
    size: number
    username?: string
    phone?: string
    status?: string
    startDate?: string
    endDate?: string
  }) {
    return request({
      url: '/admin/users',
      method: 'get',
      params
    })
  },

  getUserStatistics() {
    return request({
      url: '/admin/users/statistics',
      method: 'get'
    })
  },

  getUserGrowth() {
    return request({
      url: '/admin/users/growth',
      method: 'get'
    })
  },

  getUserDistribution() {
    return request({
      url: '/admin/users/distribution',
      method: 'get'
    })
  },

  updateUserStatus(userId: number, status: string) {
    return request({
      url: `/admin/users/${userId}/status`,
      method: 'put',
      data: { status }
    })
  },

  updateUser(userId: number, data: any) {
    return request({
      url: `/admin/users/${userId}`,
      method: 'put',
      data
    })
  },

  deleteUser(userId: number) {
    return request({
      url: `/admin/users/${userId}`,
      method: 'delete'
    })
  },

  // 用户行为分析
  getBehaviorStatistics() {
    return request({
      url: '/admin/users/behavior/statistics',
      method: 'get'
    })
  },

  getTimeDistribution() {
    return request({
      url: '/admin/users/behavior/time-distribution',
      method: 'get'
    })
  },

  getModuleRanking() {
    return request({
      url: '/admin/users/behavior/module-ranking',
      method: 'get'
    })
  },

  getDurationDistribution() {
    return request({
      url: '/admin/users/behavior/duration-distribution',
      method: 'get'
    })
  },

  getHotPages() {
    return request({
      url: '/admin/users/behavior/hot-pages',
      method: 'get'
    })
  }
}



