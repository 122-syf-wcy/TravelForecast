import { request } from './request'

export const fetchServiceCategories = () => request({ url: '/life-services/categories' })

export const fetchServiceItems = (category, keyword = '') => {
  const data = { category }
  if (keyword) data.keyword = keyword
  return request({ url: '/life-services/items', data })
}
