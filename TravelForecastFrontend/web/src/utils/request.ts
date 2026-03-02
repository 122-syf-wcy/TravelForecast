import axios, { AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'
import { authApi } from '@/api/auth'

// 错误消息防抖/节流
let isErrorShowing = false
const showError = (msg: string) => {
    if (isErrorShowing) return
    isErrorShowing = true
    ElMessage.error(msg)
    setTimeout(() => { isErrorShowing = false }, 2000)
}

// 创建axios实例
// 根据当前访问地址自动判断 API 地址
const getBaseURL = () => {
    const baseURL = import.meta.env.VITE_API_BASE_URL
    if (baseURL) return baseURL

    // 使用相对路径，通过 Nginx 代理访问后端
    // 这样可以避免 CORS 问题
    return '/api'
}

const request = axios.create({
    // 统一以 /api 为前缀，对接后端接口
    baseURL: getBaseURL(),
    timeout: 15000,
    headers: {
        'Content-Type': 'application/json',
    },
})

// 请求拦截器
request.interceptors.request.use(
    (config: AxiosRequestConfig) => {
        // 优先从localStorage读取token，确保即使Store未初始化也能获取到
        const token = localStorage.getItem('token') || useUserStore().token

        if (token && config.headers) {
            config.headers['Authorization'] = `Bearer ${token}`
        }

        // 注入 traceId，便于后端串联日志
        if (config.headers) {
            const traceId = `${Date.now()}-${Math.random().toString(16).slice(2)}`
            config.headers['X-Trace-Id'] = traceId
        }

        return config
    },
    (error: AxiosError) => {
        console.error('请求错误:', error)
        return Promise.reject(error)
    }
)

// 防止 401 重试时重复发送请求的标记
let isRefreshingToken = false
let refreshTokenQueue: Array<(token: string) => void> = []

// 响应拦截器
request.interceptors.response.use(
    (response: AxiosResponse) => {
        const data: any = response.data

        // 兼容：若返回含有统一响应模型，则按规范解包；否则直接返回原始数据
        if (data && typeof data === 'object' && 'code' in data) {
            const code = data.code
            if (code !== 200) {
                showError(data.message || '请求失败')
                return Promise.reject(new Error(data.message || '请求失败'))
            }
            // 返回完整的 data 对象，包含 data 字段
            return data
        }

        return data
    },
    async (error: AxiosError) => {
        console.error('响应错误:', error)

        const status = error.response?.status
        let message = ''

        switch (status) {
            case 401:
                message = '登录状态已过期，尝试续签...'
                // 自动尝试刷新令牌一次（使用队列机制防止重复发送）
                try {
                    const userStore401 = useUserStore()
                    const refreshToken = localStorage.getItem('refreshToken')

                    if (refreshToken) {
                        // 如果已经在刷新中，将当前请求加入队列，等待刷新完成后再重试
                        if (isRefreshingToken) {
                            return new Promise((resolve) => {
                                refreshTokenQueue.push((newToken: string) => {
                                    const original = error.config as any
                                    original.headers = original.headers || {}
                                    original.headers['Authorization'] = `Bearer ${newToken}`
                                    resolve(request(original))
                                })
                            })
                        }

                        // 标记为正在刷新
                        isRefreshingToken = true

                        try {
                            const res: any = await authApi.refreshToken(refreshToken)
                            const data: any = res.data || res

                            if (data && data.token) {
                                userStore401.login(userStore401.userInfo as any, data.token)

                                // 刷新成功，处理队列中的所有请求
                                const queue = refreshTokenQueue
                                refreshTokenQueue = []
                                isRefreshingToken = false

                                // 重新发送原请求
                                const original = error.config as any
                                original.headers = original.headers || {}
                                original.headers['Authorization'] = `Bearer ${data.token}`

                                // 处理队列中的请求
                                queue.forEach(cb => cb(data.token))

                                return request(original)
                            }
                        } catch (e) {
                            // 刷新失败则回退到退出登录
                            isRefreshingToken = false
                            refreshTokenQueue = []
                        }
                    }
                } catch (e) {
                    // 异常处理
                }

                // 刷新失败，清除并跳回登录
                const userStore = useUserStore()
                userStore.logout()
                router.push('/')
                break
            case 403:
                message = '您没有权限访问该资源'
                break
            case 404:
                message = '请求的资源不存在'
                break
            case 500:
                message = '服务器内部错误'
                break
            default:
                message = '网络连接失败，请检查网络设置'
        }

        showError(message)
        return Promise.reject(error)
    }
)

// 导出封装的请求方法
export default request 