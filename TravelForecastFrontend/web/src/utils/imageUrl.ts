// OSS图片URL转换工具函数
import { normalizeImageUrl } from './imageProxy'

/**
 * 将图片URL转换为可访问的URL
 * 在本地开发环境中，保持相对路径，由vite代理处理
 * 在生产环境中，完整的OSS URL会通过Nginx代理
 * 
 * @param imageUrl 原始图片URL（可能是相对路径或完整URL）
 * @param defaultImage 默认图片URL
 * @returns 可访问的图片URL
 */
// 默认占位图 - 使用本地存在的图片
const DEFAULT_PLACEHOLDER = '/images/placeholder.jpg'

export function getImageUrl(imageUrl: string | null | undefined, defaultImage: string = DEFAULT_PLACEHOLDER): string {
    if (!imageUrl) {
        return defaultImage
    }

    // 使用normalizeImageUrl统一处理
    return normalizeImageUrl(imageUrl)
}

/**
 * 批量转换数据中的图片URL字段
 * @param items 数据数组
 * @param imageFields 图片字段名数组，优先级从高到低
 * @param targetField 目标字段名
 */
export function convertImageUrls<T extends Record<string, any>>(
    items: T[],
    imageFields: string[] = ['imageUrl', 'image_url', 'image'],
    targetField: string = 'image'
): T[] {
    return items.map(item => {
        // 从多个可能的字段中获取图片URL
        let imgUrl = ''
        for (const field of imageFields) {
            if (item[field]) {
                imgUrl = item[field]
                break
            }
        }

        return {
            ...item,
            [targetField]: getImageUrl(imgUrl)
        }
    })
}
