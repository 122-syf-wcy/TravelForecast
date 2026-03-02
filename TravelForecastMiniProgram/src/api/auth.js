import { request } from './request'
import { API_BASE_URL } from '@/config'

export const wechatLogin = (data) => request({
  url: '/auth/wechat/login',
  method: 'POST',
  data,
  skipAuth: true
})

export const updateUserInfo = (data) => request({
  url: '/auth/wechat/user-info',
  method: 'POST',
  data
})

/**
 * 上传头像文件并更新用户信息
 */
export const uploadAvatar = (filePath, userId, nickname) => {
  const token = uni.getStorageSync('token')
  return new Promise((resolve, reject) => {
    const formData = { userId: String(userId) }
    if (nickname) formData.nickname = nickname

    uni.uploadFile({
      url: API_BASE_URL + '/auth/wechat/upload-avatar',
      filePath,
      name: 'file',
      formData,
      header: token ? { Authorization: `Bearer ${token}` } : {},
      timeout: 30000,
      success: (res) => {
        try {
          const body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
          if (body.code === 200) {
            resolve(body.data)
          } else {
            reject(new Error(body.message || '上传失败'))
          }
        } catch (e) {
          reject(new Error('解析响应失败'))
        }
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}
