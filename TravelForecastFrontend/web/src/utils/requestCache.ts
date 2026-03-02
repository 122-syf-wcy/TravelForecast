/**
 * 请求缓存工具
 * 用于避免短时间内重复发送相同的API请求
 */

interface CacheItem<T> {
  data: T
  timestamp: number
  promise?: Promise<T>
}

// 缓存存储
const cache = new Map<string, CacheItem<any>>()

// 正在进行中的请求
const pendingRequests = new Map<string, Promise<any>>()

// 默认缓存时间（毫秒）
const DEFAULT_CACHE_TIME = 30000 // 30秒

/**
 * 生成缓存键
 */
export function generateCacheKey(url: string, params?: Record<string, any>): string {
  const paramStr = params ? JSON.stringify(params) : ''
  return `${url}:${paramStr}`
}

/**
 * 带缓存的请求包装器
 * @param key 缓存键
 * @param requestFn 实际的请求函数
 * @param cacheTime 缓存时间（毫秒），默认30秒
 */
export async function cachedRequest<T>(
  key: string,
  requestFn: () => Promise<T>,
  cacheTime: number = DEFAULT_CACHE_TIME
): Promise<T> {
  const now = Date.now()
  
  // 检查缓存是否有效
  const cached = cache.get(key)
  if (cached && (now - cached.timestamp) < cacheTime) {
    return cached.data
  }
  
  // 检查是否有正在进行的相同请求
  const pending = pendingRequests.get(key)
  if (pending) {
    return pending
  }
  
  // 发起新请求
  const promise = requestFn()
    .then(data => {
      // 缓存结果
      cache.set(key, { data, timestamp: Date.now() })
      pendingRequests.delete(key)
      return data
    })
    .catch(error => {
      pendingRequests.delete(key)
      throw error
    })
  
  // 记录进行中的请求
  pendingRequests.set(key, promise)
  
  return promise
}

/**
 * 清除指定缓存
 */
export function clearCache(key?: string): void {
  if (key) {
    cache.delete(key)
  } else {
    cache.clear()
  }
}

/**
 * 清除匹配前缀的缓存
 */
export function clearCacheByPrefix(prefix: string): void {
  const keysToDelete: string[] = []
  cache.forEach((_, key) => {
    if (key.startsWith(prefix)) {
      keysToDelete.push(key)
    }
  })
  keysToDelete.forEach(key => cache.delete(key))
}

/**
 * 获取缓存统计信息
 */
export function getCacheStats(): { size: number; keys: string[] } {
  return {
    size: cache.size,
    keys: Array.from(cache.keys())
  }
}
