import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [vue()],
    base: './', // 添加这一行，使用相对路径
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
        },
    },
    server: {
        host: true,
        port: 3000,
        proxy: {
            // 注意：代理规则按照在对象中出现的顺序匹配
            // 更具体的规则应该在前面，避免被更宽泛的规则捕获

            // 图片代理接口 - 用于代理外部图片URL（必须在/images之前）
            '/images/proxy': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                rewrite: (path) => '/api' + path
            },
            // 景区图片资源 - 代理到后端静态资源
            '/images': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                rewrite: (path) => '/api' + path
            },
            // 小程序后端API代理（管理端调用小程序后端接口）
            '/mp-api': {
                target: 'http://localhost:8082',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/mp-api/, '/api')
            },
            // AI智能服务代理 (必须在/api之前，否则会被/api捕获)
            '/ai-api': {
                target: 'http://localhost:8081',
                changeOrigin: true,
                rewrite: (path) => path
            },
            // API接口代理
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                rewrite: (path) => path
            }
        }
    }
}) 