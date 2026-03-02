<template>
  <div class="user-layout">
    <header class="tech-header">
      <div class="container mx-auto px-4 flex items-center justify-between h-16">
        <div class="logo flex items-center">
          <h1 class="text-xl neon-text">游韵华章</h1>
        </div>
        <nav class="main-nav">
          <ul class="flex space-x-6">
            <li v-if="isGuestMode">
              <router-link 
                to="/landing" 
                class="nav-link"
              >
                返回首页
              </router-link>
            </li>
            <li v-for="menu in visibleMenus" :key="menu.path">
              <router-link 
                :to="menu.path" 
                class="nav-link"
                :class="{ active: currentPath.includes(menu.path) }"
              >
                {{ menu.title }}
              </router-link>
            </li>
          </ul>
        </nav>
        <div class="user-controls flex items-center space-x-4">
          <!-- 访客模式 -->
          <template v-if="isGuestMode">
            <span class="text-sm text-yellow-400">访客模式</span>
            <el-button type="primary" size="small" @click="goToRegister">注册</el-button>
            <el-button type="default" size="small" @click="goToLogin">登录</el-button>
          </template>
          <!-- 已登录 -->
          <template v-else>
            <span class="text-sm text-[#2A9D8F]">{{ userInfo.username }}</span>
            <el-dropdown trigger="click">
              <span class="avatar-wrapper cursor-pointer">
                <el-avatar :size="36" :src="userInfo.avatar" icon="User" class="bg-tech-purple" />
                <i class="ri-arrow-down-s-line ml-1"></i>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="goToProfile">
                    <el-icon><User /></el-icon>
                    <span>用户信息管理</span>
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">
                    <el-icon><SwitchButton /></el-icon>
                    <span>退出登录</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>
      </div>
    </header>
    
    <main class="main-content">
      <router-view v-slot="{ Component, route: childRoute }">
        <transition name="fade" mode="out-in">
          <component :is="Component" :key="childRoute.path" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessageBox } from 'element-plus'
import { User, SwitchButton } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const userInfo = computed(() => userStore.userInfo || { username: '游客', role: 'user' })
const currentPath = computed(() => route.path)
const isGuestMode = computed(() => userStore.isGuestMode)

const menus = [
  { path: '/user/dashboard', title: '数据总览' },
  { path: '/user/scenic', title: '景区探索' },
  { path: '/user/prediction', title: '客流预测' },
  { path: '/user/planning', title: '行程规划' },
  { path: '/user/service', title: '实时服务' },
]

// 访客模式下只显示部分菜单
const guestMenus = [
  { path: '/user/scenic', title: '景区探索' },
  { path: '/user/prediction', title: '客流预测' },
  { path: '/user/planning', title: '行程规划' },
  { path: '/user/ai', title: '智能助手' },
]

// user/business/admin 均可见用户端菜单
const visibleMenus = computed(() => {
  return isGuestMode.value ? guestMenus : menus
})

const goToProfile = () => {
  router.push('/user/profile')
}

const handleLogout = () => {
  ElMessageBox.confirm('确认退出登录？', '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    userStore.logout()
    router.push('/login')
  }).catch(() => {})
}

const goToRegister = () => {
  router.push('/register')
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.user-layout {
  min-height: 100vh;
  background-color: #F5F7FA;
}

.tech-header {
  @apply bg-white bg-opacity-95 backdrop-blur-md border-b fixed top-0 left-0 right-0 z-50 shadow-sm;
  border-color: rgba(0, 0, 0, 0.05);
}

.logo h1 {
  background: linear-gradient(90deg, #2A9D8F, #264653);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-weight: 700;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.nav-link {
  font-size: 15px;
  font-weight: 500;
  color: #4E5969;
  transition: all 0.3s;
  position: relative;
  padding: 8px 0;
}

.nav-link:hover {
  color: #2A9D8F;
}

.nav-link::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 3px;
  background: #2A9D8F;
  border-radius: 3px;
  transition: all 0.3s;
}

.nav-link.active {
  color: #2A9D8F;
  font-weight: 600;
}

.nav-link.active::after {
  width: 24px;
}

.main-content {
  padding-top: 64px;
  padding-left: 24px;
  padding-right: 24px;
  padding-bottom: 32px;
  min-height: 100vh;
}

:deep(.bg-tech-purple) {
  background-color: #2A9D8F !important;
}
</style> 