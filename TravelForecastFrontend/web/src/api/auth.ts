import request from '@/utils/request'

function devCaptchaSvg(code: string) {
  const svg = `<svg xmlns='http://www.w3.org/2000/svg' width='100' height='40'>
    <rect width='100' height='40' fill='#0A1127'/>
    <text x='50' y='26' font-size='22' text-anchor='middle' fill='#00FEFC' font-family='monospace'>${code}</text>
  </svg>`
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
}

// 暂时禁用开发模式假数据，直接调用后端
// const isDev = typeof window !== 'undefined' && /localhost|127\.0\.0\.1/.test(window.location.hostname)

export const authApi = {
  getLoginConfig() {
    return request({ url: '/auth/login-config', method: 'get' })
  },
  getCaptcha() {
    // if (isDev) {
    //   // 开发阶段返回固定验证码 1234（字段名与后端保持一致）
    //   return Promise.resolve({ data: { imageBase64: devCaptchaSvg('1234'), captchaId: 'dev-1234' } })
    // }
    return request({ url: '/captcha', method: 'get' })
  },
  registerUser(data: any) {
    return request({ url: '/auth/register', method: 'post', data })
  },
  registerMerchant(data: any) {
    return request({ url: '/auth/register/merchant', method: 'post', data })
  },
  login(data: any) {
    return request({ url: '/auth/login', method: 'post', data })
  },
  status() {
    return request({ url: '/auth/status', method: 'get' })
  },
  logout() {
    return request({ url: '/auth/logout', method: 'post' })
  },
  logoutAll() {
    return request({ url: '/auth/logout-all', method: 'post' })
  },
  refreshToken(refreshToken: string) {
    return request({ url: '/auth/refresh-token', method: 'post', data: { refreshToken } })
  },
  forgotPassword(usernameOrEmail: string) {
    return request({ url: '/auth/forgot-password', method: 'post', data: { usernameOrEmail } })
  },
  resetPassword(token: string, newPassword: string) {
    return request({ url: '/auth/reset-password', method: 'post', data: { token, newPassword } })
  }
}


