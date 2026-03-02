import request from '@/utils/request'

/**
 * 数据库备份管理API
 */
export const backupApi = {
  // 获取备份列表
  getList() {
    return request({
      url: '/admin/backup/list',
      method: 'get'
    })
  },

  // 创建手动备份
  create() {
    return request({
      url: '/admin/backup/create',
      method: 'post'
    })
  },

  // 下载备份
  download(filename: string) {
    // 直接返回下载URL，让浏览器处理下载
    const token = localStorage.getItem('token')
    return `${import.meta.env.VITE_API_BASE_URL || ''}/admin/backup/download/${filename}?token=${token}`
  },

  // 删除备份
  delete(filename: string) {
    return request({
      url: `/admin/backup/${filename}`,
      method: 'delete'
    })
  }
}

