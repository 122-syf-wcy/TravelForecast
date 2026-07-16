import { request } from './request'

export const searchSpots = (keyword) =>
  request({ url: '/spots/list', data: { keyword } })

export const searchProducts = (keyword) =>
  request({ url: '/shop/products', data: { keyword } })

export const searchAll = (keyword) =>
  Promise.allSettled([
    searchSpots(keyword),
    searchProducts(keyword)
  ]).then(([spotsRes, productsRes]) => ({
    spots: spotsRes.status === 'fulfilled' && Array.isArray(spotsRes.value) ? spotsRes.value : [],
    products: productsRes.status === 'fulfilled' && Array.isArray(productsRes.value) ? productsRes.value : []
  }))
