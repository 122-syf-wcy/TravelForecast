import request from '@/utils/request'

/**
 * 模拟政策效果
 * @param discount 联票折扣率（%）
 * @param subsidy 交通补贴（元/人）
 * @param capacity 容量上限（人/日）
 */
export function simulatePolicyEffect(discount: number, subsidy: number, capacity: number) {
  return request({
    url: '/admin/policy/simulate',
    method: 'get',
    params: { discount, subsidy, capacity }
  })
}

