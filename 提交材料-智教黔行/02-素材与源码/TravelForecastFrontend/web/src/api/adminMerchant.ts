import request from '@/utils/request'

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface MerchantUser {
  userId: number
  username: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  role: string
  status: 'pending' | 'active' | 'inactive'
  createdAt?: string
}

export interface MerchantProfile {
  id?: number
  userId: number
  businessName?: string
  category?: string
  contact?: string
  phone?: string
  licenseNo?: string
  address?: string
  createdAt?: string
  updatedAt?: string
}

export function getPendingMerchants(params: { pageNum?: number; pageSize?: number }) {
  return request<{ records: MerchantUser[]; total: number; size: number; current: number; pages: number }>({
    url: '/admin/merchant/pending',
    method: 'get',
    params
  })
}

export function pageMerchantProfiles(params: { pageNum?: number; pageSize?: number; keyword?: string }) {
  return request<PageResult<MerchantProfile>>({
    url: '/merchant/profiles',
    method: 'get',
    params
  })
}

export function approveMerchant(userId: number, reason?: string) {
  return request({
    url: `/admin/merchant/approve/${userId}`,
    method: 'post',
    data: reason ? { reason } : undefined
  })
}

export function rejectMerchant(userId: number, reason?: string) {
  return request({
    url: `/admin/merchant/reject/${userId}`,
    method: 'post',
    data: reason ? { reason } : undefined
  })
}

export function getMerchantStats() {
  return request<{ pending: number; approved: number; rejected: number; today: number }>({
    url: '/admin/merchant/stats',
    method: 'get'
  })
}

export function pageMerchantList(params: { page?: number; size?: number; keyword?: string }) {
  return request<PageResult<any>>({
    url: '/admin/merchant/list',
    method: 'get',
    params
  })
}

export function pageAuditLogs(params: { page?: number; size?: number; userId?: number }) {
  return request<PageResult<any>>({
    url: '/admin/merchant/audit-logs',
    method: 'get',
    params
  })
}


