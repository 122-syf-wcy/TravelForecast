import request from '@/utils/request'

export interface MerchantScenicInfo {
  scenicId: number
  scenicName: string
  businessName?: string
}

/**
 * 获取当前商家的景区信息
 */
export function getMerchantScenicInfo() {
  return request<MerchantScenicInfo>({
    url: '/merchant/profile/my-scenic',
    method: 'get'
  })
}
