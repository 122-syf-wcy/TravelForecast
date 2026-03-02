import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 小程序后端专用请求实例（代理到 8082）
 */
const mpRequest = axios.create({
  baseURL: '/mp-api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

mpRequest.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token && config.headers) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

mpRequest.interceptors.response.use(
  (response) => {
    const data: any = response.data
    if (data && typeof data === 'object' && 'code' in data) {
      if (data.code !== 200) {
        ElMessage.error(data.message || '请求失败')
        return Promise.reject(new Error(data.message || '请求失败'))
      }
      return data
    }
    return data
  },
  (error) => {
    ElMessage.error(error.response?.data?.message || '网络请求失败')
    return Promise.reject(error)
  }
)

// ==================== 轮播图管理 ====================
export const mpBannerApi = {
  /** 查询全部轮播图（含未启用） */
  list() {
    return mpRequest.get('/admin/banners')
  },
  /** 新增轮播图 */
  create(data: any) {
    return mpRequest.post('/admin/banners', data)
  },
  /** 修改轮播图 */
  update(id: number | string, data: any) {
    return mpRequest.put(`/admin/banners/${id}`, data)
  },
  /** 删除轮播图 */
  remove(id: number | string) {
    return mpRequest.delete(`/admin/banners/${id}`)
  },
  /** 启用/禁用切换 */
  toggle(id: number | string) {
    return mpRequest.put(`/admin/banners/${id}/toggle`)
  },
  /** 上传图片（返回 OSS URL） */
  upload(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return mpRequest.post('/admin/banners/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

// ==================== 文创商品管理 ====================
export const mpProductApi = {
  /** 查询全部商品（含下架） */
  list(params?: { category?: string; status?: string; keyword?: string }) {
    return mpRequest.get('/admin/products', { params })
  },
  /** 查询单个商品 */
  detail(id: number | string) {
    return mpRequest.get(`/admin/products/${id}`)
  },
  /** 新增商品 */
  create(data: any) {
    return mpRequest.post('/admin/products', data)
  },
  /** 修改商品 */
  update(id: number | string, data: any) {
    return mpRequest.put(`/admin/products/${id}`, data)
  },
  /** 删除商品 */
  remove(id: number | string) {
    return mpRequest.delete(`/admin/products/${id}`)
  },
  /** 上架/下架切换 */
  toggle(id: number | string) {
    return mpRequest.put(`/admin/products/${id}/toggle`)
  },
  /** 上传商品图片（返回 OSS URL） */
  upload(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return mpRequest.post('/admin/products/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
