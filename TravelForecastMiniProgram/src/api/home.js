import { request } from './request'

export const fetchHomeBanners = () => request({ url: '/content/banners/public' })

export const fetchHotNews = (size = 5) =>
  request({ url: '/news/hot', data: { page: 1, size } })

export const fetchScenicSpots = () => request({ url: '/spots/list' })
