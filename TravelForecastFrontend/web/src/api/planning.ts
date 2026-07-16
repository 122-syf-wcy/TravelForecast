import request from '@/utils/request'

// 景点接口定义
export interface Attraction {
    id: number
    name: string
    location: {
        lat: number
        lng: number
    }
    visitDuration: number // 分钟
    popularityScore: number
    category: string
    imageUrl: string
    description: string
}

// 创建行程计划
export function createPlan(data: {
    date: string
    startTime: string
    endTime: string
    attractions: number[]
    preferences: {
        avoidCrowded: boolean
        preferIndoor: boolean
        preferOutdoor: boolean
        withChildren: boolean
        withElderly: boolean
    }
}) {
    return request<{
        planId: string
        date: string
        schedule: Array<{
            startTime: string
            endTime: string
            attraction: Attraction
            suggestedDuration: number
            congestionLevel: 'low' | 'medium' | 'high'
            tips: string
        }>
        totalDistance: number
        estimatedCost: number
    }>({
        url: '/itineraries',
        method: 'post',
        data
    })
}

// 获取行程计划
export function getPlan(planId: string) {
    return request<{
        planId: string
        date: string
        schedule: Array<{
            startTime: string
            endTime: string
            attraction: Attraction
            suggestedDuration: number
            congestionLevel: 'low' | 'medium' | 'high'
            tips: string
        }>
        totalDistance: number
        estimatedCost: number
    }>({
        url: `/itineraries/${planId}`,
        method: 'get'
    })
}

// 更新行程计划
export function updatePlan(planId: string, data: {
    attractions: number[]
    date?: string
    startTime?: string
    endTime?: string
}) {
    return request({
        url: `/itineraries/${planId}`,
        method: 'put',
        data
    })
}

// 获取用户所有行程计划
export function getUserPlans() {
    return request<Array<{
        planId: string
        date: string
        attractions: number
        createdAt: string
        status: 'upcoming' | 'completed' | 'cancelled'
    }>>({
        url: '/itineraries',
        method: 'get'
    })
}

// 获取景点间的距离矩阵
export function getDistanceMatrix(attractionIds: number[]) {
    return request<{
        distances: Array<{
            from: number
            to: number
            distance: number // 米
            duration: number // 秒
            transportMode: 'walking' | 'driving' | 'transit'
        }>
    }>({
        url: '/itineraries/distance-matrix',
        method: 'get',
        params: { ids: attractionIds.join(',') }
    })
}

// 获取可用景点列表
export function getAttractions() {
    return request<Attraction[]>({
        url: '/spots/attractions',
        method: 'get'
    })
} 