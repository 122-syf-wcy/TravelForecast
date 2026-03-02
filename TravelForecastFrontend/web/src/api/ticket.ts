import request from '@/utils/request'

// 门票类型
export type TicketType = 'adult' | 'child' | 'student' | 'elder'

// 门票订单状态
export type TicketOrderStatus = 'pending' | 'paid' | 'used' | 'cancelled' | 'refunded'

// 门票订单接口
export interface TicketOrder {
  id: number
  orderNo: string
  scenicId: number
  scenicName?: string
  userId: number
  userName?: string
  userPhone?: string
  ticketType: TicketType
  ticketCount: number
  unitPrice: number
  totalAmount: number
  visitDate: string
  visitorName: string
  visitorPhone: string
  status: TicketOrderStatus
  barcode?: string  // 条形码（用于景区扫码检票）
  paymentTime?: string
  usedTime?: string
  createdAt: string
  updatedAt?: string
}

// 创建门票订单参数
export interface CreateTicketOrderParams {
  scenicId: number
  ticketType: TicketType
  ticketCount: number
  visitDate: string
  visitorName: string
  visitorPhone: string
}

// 查询订单参数
export interface QueryTicketOrderParams {
  scenicId?: number
  status?: TicketOrderStatus
  startDate?: string
  endDate?: string
  keyword?: string
  page?: number
  size?: number
}

// 用户端：创建门票订单
export function createTicketOrder(params: CreateTicketOrderParams) {
  return request<{
    orderId: number
    orderNo: string
    amount: number
  }>({
    url: '/tickets/order',
    method: 'post',
    data: params
  })
}

// 用户端：获取我的门票订单列表
export function getMyTicketOrders(params?: {
  status?: TicketOrderStatus
  page?: number
  size?: number
}) {
  return request<{
    list: TicketOrder[]
    total: number
  }>({
    url: '/tickets/my-orders',
    method: 'get',
    params
  })
}

// 用户端：获取订单详情
export function getTicketOrderDetail(orderId: number) {
  return request<TicketOrder>({
    url: `/tickets/order/${orderId}`,
    method: 'get'
  })
}

// 用户端：取消订单
export function cancelTicketOrder(orderId: number) {
  return request<{ message: string }>({
    url: `/tickets/order/${orderId}/cancel`,
    method: 'put'
  })
}

// 商家端：获取景区的门票订单列表
export function getMerchantTicketOrders(params?: QueryTicketOrderParams) {
  return request<{
    list: TicketOrder[]
    total: number
  }>({
    url: '/tickets/merchant/orders',
    method: 'get',
    params
  })
}

// 商家端：更新订单状态
export function updateTicketOrderStatus(orderId: number, status: TicketOrderStatus) {
  return request<{ message: string }>({
    url: `/tickets/merchant/order/${orderId}/status`,
    method: 'put',
    data: { status }
  })
}

// 商家端：获取门票订单统计
export function getTicketOrderStatistics(params?: {
  scenicId?: number
  startDate?: string
  endDate?: string
}) {
  return request<{
    totalOrders: number
    totalAmount: number
    totalTickets: number
    pendingOrders: number
    paidOrders: number
    usedOrders: number
    cancelledOrders: number
    todayOrders: number
    todayAmount: number
  }>({
    url: '/tickets/merchant/statistics',
    method: 'get',
    params
  })
}

// 商家端：批量核销门票（扫码核销）
export function verifyTickets(orderIds: number[]) {
  return request<{
    success: number
    failed: number
    message: string
  }>({
    url: '/tickets/merchant/verify',
    method: 'post',
    data: { orderIds }
  })
}

