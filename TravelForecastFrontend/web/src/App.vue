<template>
  <div class="app-container min-h-screen text-gray-800 font-roboto bg-[#F5F7FA]">
    <!-- 顶部路由加载进度条 -->
    <div class="route-progress" :class="{ active: isLoading, done: isDone }">
      <div class="route-progress-bar"></div>
    </div>

    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
    <!-- 只在用户端页面显示数字人助手 -->
    <DigitalHuman v-if="showDigitalHuman" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { loadSystemConfig } from '@/composables/useSystemConfig'
import DigitalHuman from '@/components/DigitalHuman.vue'

// 初始化用户状态
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

// 路由加载进度条状态
const isLoading = ref(false)
const isDone = ref(false)
let loadingTimer: ReturnType<typeof setTimeout> | null = null

router.beforeEach((_to, _from, next) => {
  isDone.value = false
  isLoading.value = true
  next()
})

router.afterEach(() => {
  // 延迟一点完成，让进度条动画看起来更自然
  if (loadingTimer) clearTimeout(loadingTimer)
  loadingTimer = setTimeout(() => {
    isDone.value = true
    setTimeout(() => {
      isLoading.value = false
      isDone.value = false
    }, 400)
  }, 100)
})

// 只在用户端页面显示数字人助手（排除admin、business页面，以及实时服务页面-因为该页面内嵌了数字人）
const showDigitalHuman = computed(() => {
  const path = route.path
  // 排除管理端、商家端、以及实时服务页面（实时服务页面内嵌数字人）
  return !path.startsWith('/admin') && 
         !path.startsWith('/business') && 
         path !== '/user/service'
})

onMounted(async () => {
  // 从localStorage恢复用户登录状态
  userStore.initUserFromStorage()
  
  // 加载系统配置（包括logo和网站标题）
  // 如果用户已登录，尝试加载系统配置
  if (userStore.token) {
    try {
      await loadSystemConfig()
    } catch (error) {
      // 静默失败，不影响应用启动
      console.log('系统配置加载失败，将使用默认配置')
    }
  }
})
</script>

<style>
.app-container {
  min-height: 100vh;
}

/* ========== 顶部路由加载进度条 ========== */
.route-progress {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  z-index: 99999;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.2s;
}

.route-progress.active {
  opacity: 1;
}

.route-progress.done .route-progress-bar {
  width: 100% !important;
  transition: width 0.2s ease;
}

.route-progress-bar {
  height: 100%;
  width: 0;
  background: linear-gradient(90deg, #2A9D8F, #3eb5a5, #2A9D8F);
  background-size: 200% 100%;
  animation: progress-loading 1.5s ease-in-out infinite, progress-width 8s cubic-bezier(0.1, 0.5, 0.3, 1) forwards;
  border-radius: 0 2px 2px 0;
  box-shadow: 0 0 8px rgba(42, 157, 143, 0.5);
}

@keyframes progress-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@keyframes progress-width {
  0% { width: 0; }
  10% { width: 15%; }
  30% { width: 40%; }
  50% { width: 60%; }
  70% { width: 75%; }
  90% { width: 85%; }
  100% { width: 90%; }
}

/* ========== 顶层路由淡入淡出 ========== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ========== 子路由页面滑入动画 ========== */
.page-slide-enter-active {
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.page-slide-leave-active {
  transition: all 0.2s cubic-bezier(0.55, 0.085, 0.68, 0.53);
}

.page-slide-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.page-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ========== 全局入场动画工具类 ========== */
/* 渐入上浮 - 用在页面卡片元素上 */
.animate-fade-in-up {
  animation: fadeInUp 0.5s ease both;
}

.animate-fade-in-up-delay-1 { animation: fadeInUp 0.5s ease 0.1s both; }
.animate-fade-in-up-delay-2 { animation: fadeInUp 0.5s ease 0.2s both; }
.animate-fade-in-up-delay-3 { animation: fadeInUp 0.5s ease 0.3s both; }
.animate-fade-in-up-delay-4 { animation: fadeInUp 0.5s ease 0.4s both; }

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 渐入缩放 - 适合弹窗/卡片 */
.animate-scale-in {
  animation: scaleIn 0.35s cubic-bezier(0.175, 0.885, 0.32, 1.275) both;
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.92);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 骨架屏闪烁动画 */
.skeleton-shimmer {
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 37%, #f0f0f0 63%);
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}

@keyframes shimmer {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}

/* 数字计数上浮 */
.animate-count-up {
  animation: countUp 0.6s ease both;
}

@keyframes countUp {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
</style>
