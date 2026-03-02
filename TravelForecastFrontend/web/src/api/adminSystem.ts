import request from '@/utils/request'

/**
 * 管理员账户管理API
 */
export const adminAccountApi = {
  // 获取账户信息
  getInfo() {
    return request({
      url: '/admin/account/info',
      method: 'get'
    })
  },

  // 更新账户信息
  updateInfo(data: {
    username?: string
    nickname?: string
    email?: string
    phone?: string
    avatar?: string
  }) {
    return request({
      url: '/admin/account/info',
      method: 'put',
      data
    })
  },

  // 修改密码
  changePassword(data: {
    currentPassword: string
    newPassword: string
    confirmPassword: string
  }) {
    return request({
      url: '/admin/account/password',
      method: 'put',
      data
    })
  }
}

/**
 * 系统配置API
 */
export const systemConfigApi = {
  // 获取所有配置
  getConfig() {
    return request({
      url: '/admin/system/config',
      method: 'get'
    })
  },

  // 保存配置
  saveConfig(config: Record<string, string>) {
    return request({
      url: '/admin/system/config',
      method: 'post',
      data: config
    })
  }
}

/**
 * 系统日志API
 */
export const systemLogApi = {
  // 分页查询日志
  getLogs(params: {
    page?: number
    size?: number
    level?: string
    module?: string
    startTime?: string
    endTime?: string
  }) {
    return request({
      url: '/admin/system/logs',
      method: 'get',
      params
    })
  },

  // 导出日志
  exportLogs(params: {
    level?: string
    startTime?: string
    endTime?: string
  }) {
    return request({
      url: '/admin/system/logs/export',
      method: 'get',
      params
    })
  },

  // 获取统计信息
  getStats() {
    return request({
      url: '/admin/system/stats',
      method: 'get'
    })
  }
}

