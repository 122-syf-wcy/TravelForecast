import { API_BASE_URL } from '@/config'

/**
 * 小程序 HTTP 客户端，提供：
 * - 统一 Base URL 拼接
 * - 401 / token 失效时的静默重新登录（wx.login → /auth/wechat/login → 重试原请求）
 * - 多并发请求共享同一次登录刷新结果，避免重复触发
 */

const buildUrl = (url) => {
  if (!url) return API_BASE_URL
  if (url.startsWith('http')) return url
  if (url.startsWith('/')) return API_BASE_URL + url
  return API_BASE_URL + '/' + url
}

let refreshingPromise = null
let lastRefreshAt = 0

const REFRESH_COOLDOWN_MS = 2000

const wxLogin = () => new Promise((resolve, reject) => {
  uni.login({
    provider: 'weixin',
    success: (res) => {
      if (res.code) resolve(res.code)
      else reject(new Error('wx.login 未返回 code'))
    },
    fail: (err) => reject(err)
  })
})

const exchangeTokenByCode = (code) => new Promise((resolve, reject) => {
  uni.request({
    url: buildUrl('/auth/wechat/login'),
    method: 'POST',
    data: { code },
    header: { 'Content-Type': 'application/json' },
    timeout: 10000,
    success: (res) => {
      const body = res.data || {}
      if (body.code === 200 && body.data && body.data.token) {
        resolve(body.data)
      } else {
        reject(new Error(body.message || '静默登录失败'))
      }
    },
    fail: (err) => reject(err)
  })
})

const refreshToken = () => {
  const now = Date.now()
  if (refreshingPromise) return refreshingPromise
  if (now - lastRefreshAt < REFRESH_COOLDOWN_MS) {
    return Promise.reject(new Error('登录刷新过于频繁，稍后再试'))
  }
  refreshingPromise = wxLogin()
    .then((code) => exchangeTokenByCode(code))
    .then((payload) => {
      if (payload.token) uni.setStorageSync('token', payload.token)
      // WechatLoginResponse 为平铺结构（userId/nickname/avatarUrl 等直接在 payload 上），
      // 合并后端嵌套与平铺两种形态，保持本地 userInfo 始终有最新 userId/昵称。
      const merged = { ...(uni.getStorageSync('userInfo') || {}) }
      if (payload.userInfo && typeof payload.userInfo === 'object') {
        Object.assign(merged, payload.userInfo)
      }
      ;['userId', 'nickname', 'nickName', 'avatarUrl', 'avatar', 'openId', 'sessionKey'].forEach((key) => {
        if (payload[key] != null) merged[key] = payload[key]
      })
      uni.setStorageSync('userInfo', merged)
      lastRefreshAt = Date.now()
      return payload.token
    })
    .finally(() => { refreshingPromise = null })
  return refreshingPromise
}

const rawRequest = ({ url, method = 'GET', data, header, token }) => new Promise((resolve, reject) => {
  const headers = {
    'Content-Type': 'application/json',
    ...header
  }
  if (token) headers.Authorization = `Bearer ${token}`

  uni.request({
    url: buildUrl(url),
    method,
    data,
    header: headers,
    timeout: 15000,
    success: (res) => resolve(res),
    fail: (err) => reject(err)
  })
})

const handleBusiness = (res, resolve, reject) => {
  const body = res.data || {}
  if (body.code === 200) {
    resolve(body.data)
  } else {
    reject(Object.assign(new Error(body.message || '请求失败'), { code: body.code, raw: body }))
  }
}

export const request = async ({ url, method = 'GET', data, header, skipAuth = false }) => {
  const token = skipAuth ? '' : uni.getStorageSync('token')
  const res = await rawRequest({ url, method, data, header, token })

  const httpStatus = res.statusCode
  const body = res.data || {}
  const businessUnauthorized = typeof body === 'object' && (body.code === 401 || body.code === 10401)
  const needsRefresh = !skipAuth && (httpStatus === 401 || businessUnauthorized)

  if (!needsRefresh) {
    return new Promise((resolve, reject) => handleBusiness(res, resolve, reject))
  }

  try {
    const newToken = await refreshToken()
    const retryRes = await rawRequest({ url, method, data, header, token: newToken })
    return new Promise((resolve, reject) => handleBusiness(retryRes, resolve, reject))
  } catch (err) {
    const msg = err && err.message ? err.message : ''
    const isCooldown = msg.includes('过于频繁')
    if (!isCooldown) {
      uni.removeStorageSync('token')
      uni.removeStorageSync('userInfo')
    }
    const error = new Error(isCooldown ? msg : (msg || '登录已过期，请重新登录'))
    error.code = isCooldown ? 429 : 401
    throw error
  }
}
