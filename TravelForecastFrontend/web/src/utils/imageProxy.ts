/**
 * 图片代理工具函数
 * 用于处理OSS图片URL
 */

// 默认占位图
const DEFAULT_PLACEHOLDER = '/images/placeholder.jpg'

// OSS 基础 URL
const OSS_BASE_URL = 'https://smarttourism717.oss-cn-beijing.aliyuncs.com/scenic'

// 本地静态图片列表（仅系统UI必需的图片）
const LOCAL_STATIC_IMAGES = [
  'placeholder.jpg',
  'avatar-poster.jpg',
  'default-spot.jpg',
  // 景区图片
  'meihuashan-1.jpg', 'meihuashan-2.jpg', 'meihuashan-3.jpg', 'meihuashan-map.jpg', 'meihuashan-thumb.jpg',
  'minghu-1.jpg', 'minghu-2.jpg', 'minghu-3.jpg', 'minghu-map.jpg', 'minghu-thumb.jpg',
  'shuicheng-1.jpg', 'shuicheng-2.jpg', 'shuicheng-3.jpg', 'shuicheng-map.jpg', 'shuicheng-thumb.jpg',
  'wumeng-1.jpg', 'wumeng-2.jpg', 'wumeng-3.jpg', 'wumeng-map.jpg', 'wumeng-thumb.jpg',
  'yushe-1.jpg', 'yushe-2.jpg', 'yushe-3.jpg', 'yushe-map.jpg', 'yushe-thumb.jpg'
]

/**
 * 检查是否是本地静态图片
 */
function isLocalStaticImage(url: string): boolean {
  if (!url) return false
  const filename = url.split('/').pop() || ''
  return LOCAL_STATIC_IMAGES.some(img => filename === img)
}

/**
 * 检查是否是OSS URL
 */
export function isOssUrl(url: string): boolean {
  if (!url) return false
  return url.includes('aliyuncs.com')
}

/**
 * 处理图片 URL - 核心函数
 * @param url - 原始图片 URL
 * @returns 可访问的图片 URL
 */
export function normalizeImageUrl(url: string | undefined | null): string {
  if (!url) {
    return DEFAULT_PLACEHOLDER
  }

  // 如果已经是完整的 HTTP/HTTPS URL（包括 OSS URL），直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }

  // 如果是本地静态图片，直接返回
  if (isLocalStaticImage(url)) {
    return url.startsWith('/') ? url : '/' + url
  }

  // 如果是 /images/ 开头的相对路径，转换为 OSS URL
  if (url.startsWith('/images/')) {
    // 移除开头的斜杠，拼接 OSS 基础 URL
    const path = url.slice(1) // 去掉开头的 /
    return `${OSS_BASE_URL}/${path}`
  }

  // 如果是 images/ 开头（没有斜杠），也转换为 OSS URL
  if (url.startsWith('images/')) {
    return `${OSS_BASE_URL}/${url}`
  }

  // 其他相对路径直接返回
  if (url.startsWith('/')) {
    return url
  }

  return url
}

/**
 * 处理图片加载错误，提供降级方案
 */
export function handleImageError(event: any, fallbackUrl: string = DEFAULT_PLACEHOLDER) {
  if (event && event.target) {
    const currentUrl = event.target.src

    // 如果已经是占位图，不再处理
    if (currentUrl.includes('placeholder')) {
      return
    }

    console.warn('图片加载失败:', currentUrl)
    event.target.src = fallbackUrl
  }
}

/**
 * 为多个图片URL进行标准化处理
 */
export function normalizeImageUrls(urls: string[]): string[] {
  return urls.map(url => normalizeImageUrl(url))
}

// 兼容旧的函数名
export const getImageProxyUrl = normalizeImageUrl
export const convertToProxyUrl = normalizeImageUrl
