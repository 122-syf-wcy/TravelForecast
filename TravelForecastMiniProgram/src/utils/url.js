import { ASSET_BASE_URL } from '@/config'

export const resolveAssetUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/')) return ASSET_BASE_URL + url
  return url
}
