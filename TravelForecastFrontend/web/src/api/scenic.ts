import request from '@/utils/request'
import { cachedRequest, generateCacheKey, clearCacheByPrefix } from '@/utils/requestCache'

// 景区信息接口
export interface ScenicSpot {
    id: number
    name: string
    location: {
        lat: number
        lng: number
    }
    description: string
    imageUrl: string
    rating: number
    currentFlow: number
    maxCapacity: number
    openingHours: {
        open: string
        close: string
    }
    ticketPrice: number
    tags: string[]
    weather: {
        temperature: number
        condition: string
        windSpeed: number
        humidity: number
    }
}

export interface CreateScenicPayload {
    name: string
    address: string
    level: string
    type: string
    image?: string
    description?: string
    location?: { lat: number; lng: number }
    openingHours?: { open: string; close: string }
    ticketPrice?: number
    status?: string
}

// 获取六盘水所有景区列表（带缓存，60秒内不重复请求）
export function getScenicSpots(params?: { city?: string; page?: number; size?: number }) {
    const cacheKey = generateCacheKey('/spots', params)
    return cachedRequest(
        cacheKey,
        () => request<{ list: ScenicSpot[]; total?: number }>({
            url: '/spots',
            method: 'get',
            params
        }),
        60000 // 缓存60秒
    )
}

// 获取单个景区详情（带缓存，30秒内不重复请求）
export function getScenicDetail(id: number) {
    const cacheKey = generateCacheKey(`/spots/${id}`)
    return cachedRequest(
        cacheKey,
        () => request<ScenicSpot>({
            url: `/spots/${id}`,
            method: 'get'
        }),
        30000 // 缓存30秒
    )
}

// 获取景区实时客流数据（带缓存，15秒内不重复请求，实时数据缓存时间短）
export function getScenicFlow(id: number) {
    const cacheKey = generateCacheKey(`/spots/${id}/realtime`)
    return cachedRequest(
        cacheKey,
        () => request<{
            currentFlow: number
            maxCapacity: number
            flowRate: number
            updateTime: string
            areaDistribution: Record<string, number>
        }>({
            url: `/spots/${id}/realtime`,
            method: 'get'
        }),
        15000 // 缓存15秒（实时数据缓存时间短）
    )
}

// 清除景区相关缓存（用于数据更新后强制刷新）
export function clearScenicCache() {
    clearCacheByPrefix('/spots')
}

// 获取景区热力图数据
export function getScenicHeatmap(id: number) {
    return request<{
        heatmapData: Array<{
            lat: number
            lng: number
            weight: number
        }>
        updateTime: string
    }>({
        url: `/spots/${id}/heatmap`,
        method: 'get'
    })
}

// 获取景区周边服务点
export function getScenicServices(id: number, type?: 'restaurant' | 'hotel' | 'parking' | 'toilet' | 'emergency') {
    return request<Array<{
        id: number
        name: string
        type: string
        location: {
            lat: number
            lng: number
        }
        distance: number
        rating: number
        status: 'open' | 'closed' | 'busy'
    }>>({
        url: `/spots/${id}/services`,
        method: 'get',
        params: { type }
    })
}

export function searchScenic(keyword: string, category?: string) {
    return request<{ list: ScenicSpot[] }>({
        url: '/spots/search',
        method: 'get',
        params: { keyword, category }
    })
}

// 新增景区
export function createScenic(payload: CreateScenicPayload) {
    return request<{ id: number } | { success: boolean; id?: number }>({
        url: '/spots',
        method: 'post',
        data: payload
    })
}

// 更新景区
export function updateScenic(id: number | string, payload: Partial<CreateScenicPayload>) {
    return request<{ message?: string }>({
        url: `/spots/${id}`,
        method: 'put',
        data: payload
    })
}

// 删除景区
export function deleteScenic(id: number | string) {
    return request<{ message?: string }>({
        url: `/spots/${id}`,
        method: 'delete'
    })
}

// 景区视频接口
export interface ScenicVideo {
    id: number
    scenicId: number
    title: string
    videoUrl: string
    coverUrl?: string
    duration?: number
    createdAt: string
}

// 获取景区视频列表
export function getScenicVideos(scenicId: number) {
    return request<ScenicVideo[]>({
        url: `/spots/${scenicId}/videos`,
        method: 'get'
    })
}

// ==========================================
// 管理员端接口 (AdminScenicController)
// ==========================================
export const adminScenicApi = {
    // 获取列表
    getList(params: { page: number; size: number; status?: string; keyword?: string }) {
        return request<{ records: any[]; total: number }>({
            url: '/admin/scenic/list',
            method: 'get',
            params
        })
    },
    // 获取详情
    getDetail(idOrCode: number | string) {
        return request<any>({
            url: `/admin/scenic/${idOrCode}`,
            method: 'get'
        })
    },
    // 创建
    create(data: any) {
        return request<any>({
            url: '/admin/scenic',
            method: 'post',
            data
        })
    },
    // 更新
    update(id: number | string, data: any) {
        return request<any>({
            url: `/admin/scenic/${id}`,
            method: 'put',
            data
        })
    },
    // 删除
    delete(id: number | string) {
        return request<void>({
            url: `/admin/scenic/${id}`,
            method: 'delete'
        })
    },
    // 更新状态
    updateStatus(id: number | string, status: string) {
        return request<void>({
            url: `/admin/scenic/${id}/status`,
            method: 'put',
            data: { status }
        })
    },
    // 上传图片
    uploadImage(idOrCode: number | string, file: File, imageType: string = 'GALLERY', sortOrder: number = 0) {
        const formData = new FormData()
        formData.append('file', file)
        formData.append('imageType', imageType)
        formData.append('sortOrder', String(sortOrder))
        return request<any>({
            url: `/admin/scenic/${idOrCode}/images/upload`,
            method: 'post',
            data: formData,
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    },
    // 上传视频
    uploadVideo(idOrCode: number | string, file: File, title: string, cover?: File, duration: number = 0) {
        const formData = new FormData()
        formData.append('file', file)
        formData.append('title', title)
        if (cover) formData.append('cover', cover)
        formData.append('duration', String(duration))
        return request<any>({
            url: `/admin/scenic/${idOrCode}/videos/upload`,
            method: 'post',
            data: formData,
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    },
    // 获取图片列表
    getImages(idOrCode: number | string) {
        return request<any[]>({
            url: `/admin/scenic/${idOrCode}/images`,
            method: 'get'
        })
    },
    // 获取视频列表
    getVideos(idOrCode: number | string) {
        return request<any[]>({
            url: `/admin/scenic/${idOrCode}/videos`,
            method: 'get'
        })
    },
    // 删除图片
    deleteImage(imageId: number) {
        return request<void>({
            url: `/admin/scenic/images/${imageId}`,
            method: 'delete'
        })
    },
    // 删除视频
    deleteVideo(videoId: number) {
        return request<void>({
            url: `/admin/scenic/videos/${videoId}`,
            method: 'delete'
        })
    }
}