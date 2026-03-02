import request from '@/utils/request'
import { cachedRequest, generateCacheKey } from '@/utils/requestCache'

// 评价接口定义
export interface Review {
    id: number
    userId: number
    username: string
    userAvatar: string
    scenicId: number
    scenicName: string
    rating: number
    content: string
    images?: string[]
    tags: string[]
    visitDate: string
    createdAt: string
    likes: number
    hasLiked?: boolean
    sentiment: 'positive' | 'neutral' | 'negative'
}

// 获取景区评价列表 - 带缓存，30秒内不重复请求
export function getReviews(scenicId: number, params?: {
    page?: number
    size?: number
    sortBy?: 'newest' | 'rating' | 'likes'
    filter?: 'all' | 'positive' | 'negative' | 'withImages'
}) {
    const cacheKey = generateCacheKey('/reviews', { scenicId, ...params })
    return cachedRequest(
        cacheKey,
        () => request<{
            total: number
            reviews: Review[]
            averageRating: number
            ratingDistribution: Record<string, number>
            popularTags: Array<{
                name: string
                count: number
            }>
        }>({
            url: `/reviews`,
            method: 'get',
            params: { scenicId, ...params }
        }),
        30000 // 缓存30秒
    )
}

// 提交评价
export function submitReview(data: {
    scenicId: number
    rating: number
    content: string
    images?: string[]
    tags?: string[]
    visitDate: string
}) {
    return request<Review>({
        url: '/reviews',
        method: 'post',
        data
    })
}

// 点赞评价
export function likeReview(reviewId: number) {
    return request({
        url: `/reviews/${reviewId}/like`,
        method: 'post'
    })
}

// 取消点赞
export function unlikeReview(reviewId: number) {
    return request({
        url: `/reviews/${reviewId}/unlike`,
        method: 'post'
    })
}

// 获取评价统计分析（商户端）
export function getReviewAnalysis(scenicId: number, period: 'day' | 'week' | 'month' | 'year') {
    return request<{
        scenicId: number
        scenicName: string
        period: string
        averageRating: number
        totalReviews: number
        sentimentDistribution: {
            positive: number
            neutral: number
            negative: number
        }
        ratingTrend: Array<{
            date: string
            rating: number
            reviewCount: number
        }>
        keywordCloud: Array<{
            keyword: string
            weight: number
            sentiment: 'positive' | 'neutral' | 'negative'
        }>
        topComplaints: Array<{
            issue: string
            count: number
            exampleReviews: string[]
        }>
        recommendedActions: string[]
    }>({
        url: `/admin/reviews/analysis/${scenicId}`,
        method: 'get',
        params: { period }
    })
}

// 回复评价（商户端）
export function replyToReview(reviewId: number, content: string) {
    return request({
        url: `/reviews/${reviewId}/reply`,
        method: 'post',
        data: { content }
    })
} 