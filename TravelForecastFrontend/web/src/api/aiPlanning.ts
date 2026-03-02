// AI智能行程规划API
import request from '@/utils/aiRequest'

export interface FlowPredictionData {
  date: string
  flow: number
  crowdLevel: string  // 'low' | 'medium' | 'high'
}

export interface AiPlanningRequest {
  startDate: string        // "2025-10-16"
  endDate: string          // "2025-10-19"
  attractions: string[]    // ["梅花山风景区", "乌蒙大草原"]
  transportation: string   // "car" | "public" | "tour"
  peopleCount: number      // 2
  budget: number           // 5000
  preferences: string[]    // ["自然风光", "历史文化"]
  flowPredictions?: Record<string, FlowPredictionData[]>  // 各景区客流预测数据
}

export interface Activity {
  time: string
  title: string
  description: string
  type: 'food' | 'scenic' | 'transit' | 'hotel' | 'other'
  duration: string
  cost?: number
  location?: string
  coordinates?: [number, number]
  nextDistance?: number     // 到下一个地点的距离（米）
  nextDuration?: number     // 到下一个地点的时间（秒）
}

export interface DayItinerary {
  day: number
  date: string
  activities: Activity[]
}

export interface AiPlanningResponse {
  title: string
  summary: string
  totalBudget: number
  budgetBreakdown: {
    ticket: number
    food: number
    transportation: number
    accommodation: number
    other: number
  }
  itinerary: DayItinerary[]
  tips: string[]
  optimized?: boolean
  optimizedBy?: string
}

/**
 * 调用AI生成智能行程规划
 * 注意：AI生成需要较长时间，设置90秒超时
 */
export async function generateAiPlan(planRequest: AiPlanningRequest): Promise<AiPlanningResponse> {
  const response: any = await request({
    url: '/ai-planning/generate',
    method: 'post',
    data: planRequest,
    timeout: 90000  // AI生成需要较长时间，设置90秒超时
  })
  return response?.data || response
}

