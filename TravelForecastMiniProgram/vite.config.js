import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// 使用 `npm run build:mp-weixin` 或 `npm run dev:mp-weixin` 由 uni CLI 注入 UNI_PLATFORM。
// 保持 UniApp 标准项目根目录，让 H5 构建从根目录 index.html 进入。
export default defineConfig({
  resolve: {
    alias: {
      '@': '/src'
    }
  },
  plugins: [uni()]
})
