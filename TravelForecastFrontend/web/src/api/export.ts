import request from '@/utils/request'

/**
 * 导出数据
 * @param params 导出参数
 */
export function exportData(params: {
  dataType: string
  scenicIds: (number | null)[]
  startDate: string
  endDate: string
  granularity: string
  format: string
  includeRawData?: boolean
  includeAnalytics?: boolean
  includePredictions?: boolean
}) {
  return request({
    url: '/admin/export/data',
    method: 'post',
    data: params
  })
}

/**
 * 获取导出统计
 */
export function getExportStats() {
  return request({
    url: '/admin/export/stats',
    method: 'get'
  })
}

/**
 * 获取导出历史
 * @param pageNum 页码
 * @param pageSize 每页大小
 * @param keyword 搜索关键词
 */
export function getExportHistory(pageNum: number = 1, pageSize: number = 10, keyword?: string) {
  return request({
    url: '/admin/export/history',
    method: 'get',
    params: { pageNum, pageSize, keyword }
  })
}

/**
 * 下载导出文件
 * @param exportId 导出ID
 */
export function downloadExportFile(exportId: string) {
  return request({
    url: `/admin/export/download/${exportId}`,
    method: 'get',
    responseType: 'blob'
  })
}

/**
 * 删除导出记录
 * @param exportId 导出ID
 */
export function deleteExportRecord(exportId: string) {
  return request({
    url: `/admin/export/${exportId}`,
    method: 'delete'
  })
}

/**
 * 清理过期记录
 */
export function clearExpiredRecords() {
  return request({
    url: '/admin/export/clear-expired',
    method: 'delete'
  })
}

/**
 * 获取今日下载统计
 */
export function getDownloadStats() {
  return request({
    url: '/admin/export/download-stats',
    method: 'get'
  })
}

/**
 * 获取存储用量统计
 */
export function getStorageStats() {
  return request({
    url: '/admin/export/storage-stats',
    method: 'get'
  })
}

