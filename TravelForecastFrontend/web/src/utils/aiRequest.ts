import axios, { AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

/**
 * AI智能服务专用请求工具
 * 指向 AI 后端 (Port 8081, 路径前缀 /ai-api)
 * 与业务后端 (Port 8080, 路径前缀 /api) 分离
 */

let isErrorShowing = false
const showError = (msg: string) => {
    if (isErrorShowing) return
    isErrorShowing = true
    ElMessage.error(msg)
    setTimeout(() => { isErrorShowing = false }, 2000)
}

const aiRequest = axios.create({
    baseURL: '/ai-api',
    timeout: 60000,  // AI服务默认60秒超时（比业务接口更长）
    headers: {
        'Content-Type': 'application/json',
    },
})

// 请求拦截器 - 复用同一个Token
aiRequest.interceptors.request.use(
    (config: AxiosRequestConfig) => {
        const token = localStorage.getItem('token') || useUserStore().token

        if (token && config.headers) {
            config.headers['Authorization'] = `Bearer ${token}`
        }

        if (config.headers) {
            const traceId = `ai-${Date.now()}-${Math.random().toString(16).slice(2)}`
            config.headers['X-Trace-Id'] = traceId
        }

        return config
    },
    (error: AxiosError) => {
        console.error('AI请求错误:', error)
        return Promise.reject(error)
    }
)

// 响应拦截器
aiRequest.interceptors.response.use(
    (response: AxiosResponse) => {
        const data: any = response.data

        if (data && typeof data === 'object' && 'code' in data) {
            if (data.code !== 200) {
                showError(data.message || 'AI服务请求失败')
                return Promise.reject(new Error(data.message || 'AI服务请求失败'))
            }
            return data
        }

        return data
    },
    (error: AxiosError) => {
        console.error('AI服务响应错误:', error)

        const status = error.response?.status
        let message = ''

        switch (status) {
            case 503:
                message = 'AI服务暂时不可用，请稍后重试'
                break
            case 504:
                message = 'AI服务响应超时，请稍后重试'
                break
            case 500:
                message = 'AI服务内部错误'
                break
            default:
                message = error.code === 'ECONNABORTED'
                    ? 'AI响应超时，请稍后重试'
                    : 'AI服务连接失败，请检查网络'
        }

        showError(message)
        return Promise.reject(error)
    }
)

export default aiRequest
