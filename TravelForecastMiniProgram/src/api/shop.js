import { request } from './request'

/**
 * 获取商品列表
 */
export const fetchProducts = (params = {}) =>
  request({ url: '/shop/products', data: params })

/**
 * 获取商品详情
 */
export const fetchProductDetail = (id) =>
  request({ url: `/shop/products/${id}` })

/**
 * 获取购物车
 */
export const fetchCart = (userId) =>
  request({ url: '/shop/cart', data: { userId } })

/**
 * 添加到购物车
 */
export const addToCart = (userId, productId, quantity = 1) =>
  request({ url: '/shop/cart/add', method: 'POST', data: { userId, productId, quantity } })

/**
 * 从购物车移除
 */
export const removeFromCart = (cartId) =>
  request({ url: `/shop/cart/${cartId}`, method: 'DELETE' })

/**
 * 获取订单列表
 */
export const fetchOrders = (userId, status) =>
  request({ url: '/shop/orders', data: { userId, status } })

/**
 * 创建订单
 */
export const createOrder = (userId, items) =>
  request({ url: '/shop/orders', method: 'POST', data: { userId, items } })

/**
 * 更新订单状态
 */
export const updateOrderStatus = (orderId, status) =>
  request({ url: `/shop/orders/${orderId}/status`, method: 'PUT', data: { status } })

/**
 * 删除订单
 */
export const deleteOrder = (orderId) =>
  request({ url: `/shop/orders/${orderId}`, method: 'DELETE' })
