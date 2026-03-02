/**
 * 景区状态API
 */
import request from '@/utils/request'
import { cachedRequest, generateCacheKey } from '@/utils/requestCache'

/**
 * 景区状态信息
 */
export interface ScenicStatus {
  scenicId: number
  scenicName: string
  status: 'open' | 'normal' | 'idle' | 'busy' | 'crowded' | 'limited' | 'closed'
  statusDescription: string
  currentFlow: number
  maxCapacity: number
  flowRate: number
  predictedFlow: number
  predictedFlowRate: number
  trend: 'rising' | 'stable' | 'falling'
  suggestedWaitTime: number
  recommended: boolean
  suggestion: string
  updateTime: string
}

/**
 * 获取景区实时状态（基于预测模型）- 带缓存，15秒内不重复请求
 */
export function getScenicStatus(scenicId: number) {
  const cacheKey = generateCacheKey(`/scenic/status/${scenicId}`)
  return cachedRequest(
    cacheKey,
    () => request<ScenicStatus>({
      url: `/scenic/status/${scenicId}`,
      method: 'get'
    }),
    15000 // 缓存15秒（实时状态数据缓存时间短）
  )
}

