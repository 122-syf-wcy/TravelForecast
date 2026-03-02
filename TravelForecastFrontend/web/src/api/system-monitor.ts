import request from '@/utils/request'

/**
 * 获取系统状态概览（CPU、内存、磁盘、网络）
 */
export function getSystemStatus() {
  return request({
    url: '/admin/monitor/status',
    method: 'get'
  })
}

/**
 * 获取服务状态
 */
export function getServicesStatus() {
  return request({
    url: '/admin/monitor/services',
    method: 'get'
  })
}

/**
 * 获取数据库连接池状态
 */
export function getDbPoolStatus() {
  return request({
    url: '/admin/monitor/db-pool',
    method: 'get'
  })
}

/**
 * 获取JVM信息
 */
export function getJvmInfo() {
  return request({
    url: '/admin/monitor/jvm',
    method: 'get'
  })
}

/**
 * 获取实时QPS数据
 */
export function getQpsData() {
  return request({
    url: '/admin/monitor/qps',
    method: 'get'
  })
}

/**
 * 获取响应时间分布
 */
export function getResponseTimeDistribution() {
  return request({
    url: '/admin/monitor/response-time',
    method: 'get'
  })
}

/**
 * 获取缓存统计
 */
export function getCacheStats() {
  return request({
    url: '/admin/monitor/cache',
    method: 'get'
  })
}

/**
 * 获取系统告警
 */
export function getAlerts() {
  return request({
    url: '/admin/monitor/alerts',
    method: 'get'
  })
}
