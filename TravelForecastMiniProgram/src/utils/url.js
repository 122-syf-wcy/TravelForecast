import { AI_API_BASE_URL, ASSET_BASE_URL, SERVER_HOST_URL } from '@/config'

const upgradeTravelUrl = (url) => {
  if (!url) return ''
  return url
    .replace(/^https?:\/\/39\.97\.232\.141:8082(?=\/|$)/, ASSET_BASE_URL)
    .replace(/^https?:\/\/localhost:8082(?=\/|$)/, ASSET_BASE_URL)
    .replace(/^https?:\/\/travel\.dongsiwei\.com:8082(?=\/|$)/, ASSET_BASE_URL)
    .replace(/^https?:\/\/39\.97\.232\.141:8081(?=\/|$)/, AI_API_BASE_URL)
    .replace(/^https?:\/\/localhost:8081(?=\/|$)/, AI_API_BASE_URL)
    .replace(/^https?:\/\/travel\.dongsiwei\.com:8081(?=\/|$)/, AI_API_BASE_URL)
    .replace(/^https?:\/\/39\.97\.232\.141(?=\/|$)/, SERVER_HOST_URL)
    .replace(/^http:\/\/travel\.dongsiwei\.com(?=\/|$)/, SERVER_HOST_URL)
    .replace(/^https:\/\/travel\.dongsiwei\.com(?=\/|$)/, SERVER_HOST_URL)
}

export const resolveAssetUrl = (url) => {
  if (!url) return ''
  const normalizedUrl = upgradeTravelUrl(url)
  if (normalizedUrl.startsWith('http')) return normalizedUrl
  if (normalizedUrl.startsWith('/')) return ASSET_BASE_URL + normalizedUrl
  return normalizedUrl
}
