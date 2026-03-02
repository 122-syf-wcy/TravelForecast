import request from '@/utils/request'
import { cachedRequest, generateCacheKey } from '@/utils/requestCache'

// 首页仪表盘相关接口

// 获取热门景点推荐
export interface PopularSpot {
  id: number
  name: string
  image: string
  description: string
  rating: number
  favoritesCount?: number
  growthRate?: number // 收藏增长率（相比昨天）
  isFavorited?: boolean
  favoriteLoading?: boolean
  category: string
}

// 带缓存，60秒内不重复请求
export function getPopularSpots(limit: number = 5) {
  const cacheKey = generateCacheKey('/dashboard/popular-spots', { limit })
  return cachedRequest(
    cacheKey,
    () => request<PopularSpot[]>({
      url: '/dashboard/popular-spots',
      method: 'get',
      params: { limit }
    }),
    60000 // 缓存60秒
  )
}

// 获取景区实时流量数据
export interface ScenicFlow {
  id: number
  name: string
  currentFlow: number
  maxCapacity: number
  flowRate: number
  trend: 'up' | 'down' | 'stable'
}

// 带缓存，30秒内不重复请求
export function getScenicFlowStats() {
  const cacheKey = generateCacheKey('/dashboard/scenic-flow')
  return cachedRequest(
    cacheKey,
    () => request<ScenicFlow[]>({
      url: '/dashboard/scenic-flow',
      method: 'get'
    }),
    30000 // 缓存30秒
  )
}

// 获取天气信息
export interface WeatherInfo {
  city: string
  temperature: number
  condition: string
  humidity: number
  minTemp: number
  maxTemp: number
  windSpeed?: number
  updateTime?: string
}

// 天气数据缓存5分钟（天气变化不频繁）
export function getWeatherInfo(city: string = '六盘水') {
  const cacheKey = generateCacheKey('/dashboard/weather', { city })
  return cachedRequest(
    cacheKey,
    () => request<WeatherInfo>({
      url: '/dashboard/weather',
      method: 'get',
      params: { city }
    }),
    300000 // 缓存5分钟
  )
}

// 获取旅游资讯
export interface TourismNews {
  id: number
  title: string
  content: string
  publishTime: string
  category: 'notice' | 'event' | 'promotion'
  important: boolean
}

export function getTourismNews(limit: number = 10) {
  return request<TourismNews[]>({
    url: '/dashboard/news',
    method: 'get',
    params: { limit }
  })
}

// 获取用户统计数据
export interface UserStats {
  totalVisits: number
  savedPlans: number
  favoriteSpots: number
  reviewsCount: number
}

export function getUserStats() {
  return request<UserStats>({
    url: '/dashboard/user-stats',
    method: 'get'
  })
}

