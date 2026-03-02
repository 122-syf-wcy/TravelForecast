import { ref, watch } from 'vue'
import { systemConfigApi } from '@/api/adminSystem'

// 全局系统配置
const systemConfig = ref<Record<string, string>>({})
const systemLogo = ref<string>('')
const systemName = ref<string>('六盘水旅游管理系统')

// 加载系统配置
export async function loadSystemConfig() {
  try {
    const res: any = await systemConfigApi.getConfig()
    if (res.data) {
      systemConfig.value = res.data
      
      // 提取常用配置
      if (res.data.system_logo) {
        systemLogo.value = res.data.system_logo
        updateFavicon(res.data.system_logo)
      }
      
      if (res.data.system_name) {
        systemName.value = res.data.system_name
        updateTitle(res.data.system_name)
      }
    }
  } catch (error) {
    console.error('加载系统配置失败:', error)
  }
}

// 更新网站favicon
export function updateFavicon(logoUrl: string) {
  if (!logoUrl) return
  
  // 查找现有的favicon链接
  let link: HTMLLinkElement | null = document.querySelector("link[rel*='icon']")
  
  if (!link) {
    // 如果不存在，创建新的
    link = document.createElement('link')
    link.rel = 'icon'
    document.head.appendChild(link)
  }
  
  // 更新favicon
  link.href = logoUrl
}

// 更新网站标题
export function updateTitle(title: string) {
  if (title) {
    document.title = title
  }
}

// 使用系统配置的composable
export function useSystemConfig() {
  return {
    systemConfig,
    systemLogo,
    systemName,
    loadSystemConfig,
    updateFavicon,
    updateTitle
  }
}

// 监听logo变化，自动更新favicon
watch(systemLogo, (newLogo) => {
  if (newLogo) {
    updateFavicon(newLogo)
  }
})

// 监听系统名称变化，自动更新标题
watch(systemName, (newName) => {
  if (newName) {
    updateTitle(newName)
  }
})

