import { API_BASE_URL } from '@/config'

const buildUrl = (url) => {
  if (!url) return API_BASE_URL
  if (url.startsWith('http')) return url
  if (url.startsWith('/')) return API_BASE_URL + url
  return API_BASE_URL + '/' + url
}

export const request = ({ url, method = 'GET', data, header, skipAuth = false }) => {
  const token = skipAuth ? '' : uni.getStorageSync('token')
  const headers = {
    'Content-Type': 'application/json',
    ...header
  }
  if (token) headers.Authorization = `Bearer ${token}`

  return new Promise((resolve, reject) => {
    uni.request({
      url: buildUrl(url),
      method,
      data,
      header: headers,
      timeout: 15000,
      success: (res) => {
        const body = res.data || {}
        if (body.code === 200) {
          resolve(body.data)
          return
        }
        const message = body.message || '请求失败'
        reject(new Error(message))
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}
