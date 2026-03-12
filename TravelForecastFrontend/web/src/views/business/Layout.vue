<template>
  <div class="business-layout">
    <el-container class="h-screen">
      <!-- 侧边栏 -->
      <el-aside width="220px" class="bg-white border-r border-gray-200">
        <div class="h-full flex flex-col">
          <!-- Logo -->
          <div class="h-16 flex items-center justify-center border-b border-gray-200">
            <h1 class="text-xl font-medium text-[#2A9D8F]">智教黔行商户平台</h1>
          </div>
          
          <!-- 菜单 -->
          <el-menu
            :default-active="activeMenu"
            class="h-full border-none bg-transparent"
            router
            unique-opened
          >
            <template v-if="canSeeBusiness">
              <el-menu-item index="/business/dashboard">
                <el-icon><DataBoard /></el-icon>
                <span>数据看板</span>
              </el-menu-item>
              
              <el-menu-item index="/business/monitor">
                <el-icon><Monitor /></el-icon>
                <span>实时监控</span>
              </el-menu-item>
              
              <el-menu-item index="/business/analysis">
                <el-icon><DataAnalysis /></el-icon>
                <span>数据分析</span>
              </el-menu-item>
              
              <el-menu-item index="/business/resource">
                <el-icon><Management /></el-icon>
                <span>资源管理</span>
              </el-menu-item>
              
              <el-menu-item index="/business/review">
                <el-icon><StarFilled /></el-icon>
                <span>评价管理</span>
              </el-menu-item>
              
              <el-menu-item index="/business/tickets">
                <el-icon><Tickets /></el-icon>
                <span>门票订单</span>
              </el-menu-item>
              
              <el-menu-item index="/business/rescue">
                <el-icon><Warning /></el-icon>
                <span>紧急救援</span>
              </el-menu-item>
              
              <el-menu-item index="/business/news">
                <el-icon><Document /></el-icon>
                <span>新闻资讯</span>
              </el-menu-item>
              
              <!-- 数据中心 -->
              <el-sub-menu index="data-center">
                <template #title>
                  <el-icon><TrendCharts /></el-icon>
                  <span>数据中心</span>
                </template>
                <el-menu-item index="/business/data/statistics">
                  <el-icon><PieChart /></el-icon>
                  <span>统计报表</span>
                </el-menu-item>
                <el-menu-item index="/business/data/prediction">
                  <el-icon><Histogram /></el-icon>
                  <span>客流预测</span>
                </el-menu-item>
                <el-menu-item index="/business/data/policy-sandbox">
                  <el-icon><SetUp /></el-icon>
                  <span>政策沙盘</span>
                </el-menu-item>
                <el-menu-item index="/business/data/export">
                  <el-icon><Download /></el-icon>
                  <span>数据导出</span>
                </el-menu-item>
              </el-sub-menu>
              
              <el-menu-item index="/business/profile">
                <el-icon><User /></el-icon>
                <span>商家资料</span>
              </el-menu-item>
            </template>
          </el-menu>
          
          <div class="mt-auto border-t border-gray-200 p-4">
            <el-dropdown trigger="click" class="w-full">
              <div class="flex items-center cursor-pointer p-2 rounded-lg hover:bg-gray-100">
                <el-avatar :size="32" :src="userInfo.avatar" icon="User" class="bg-[#2A9D8F]" />
                <div class="ml-2">
                  <div class="text-sm text-gray-800">{{ userInfo.name }}</div>
                  <div class="text-xs text-gray-500">{{ userInfo.role }}</div>
                </div>
                <el-icon class="ml-auto text-gray-500"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="goToProfile">账号设置</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-aside>
      
      <!-- 主体内容 -->
      <el-container>
        <el-header height="64px" class="border-b border-gray-200 bg-white bg-opacity-95 flex items-center justify-between px-6">
          <div class="flex items-center">
            <el-icon class="text-xl cursor-pointer text-gray-500" @click="toggleSidebar">
              <Fold v-if="isCollapse" />
              <Expand v-else />
            </el-icon>
            <el-breadcrumb separator="/" class="ml-4">
              <el-breadcrumb-item :to="{ path: '/business/dashboard' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="currentRoute">{{ currentRouteName }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          
          <div class="flex items-center space-x-4">
            <el-tooltip content="帮助文档" placement="bottom">
              <el-icon class="text-xl cursor-pointer"><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
        </el-header>
        
        <el-main class="p-6">
          <router-view v-slot="{ Component, route: childRoute }">
            <transition name="page-slide" mode="out-in">
              <component :is="Component" :key="childRoute.path" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { 
  DataBoard, DataAnalysis, Monitor, StarFilled, Management, 
  QuestionFilled, ArrowDown, Fold, Expand, User, Document, Tickets, Warning,
  TrendCharts, PieChart, Histogram, SetUp, Download
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isCollapse = ref(false)
const userInfo = computed(() => {
  const u = userStore.userInfo
  return u ? { 
    name: u.nickname || u.username || '用户', 
    role: u.role,
    avatar: u.avatar || ''
  } : { 
    name: '商户用户', 
    role: 'business',
    avatar: ''
  }
})

const canSeeBusiness = computed(() => {
  const role = userStore.userInfo?.role
  return role === 'business' || role === 'merchant' || role === 'admin'
})

const activeMenu = computed(() => route.path)
const currentRoute = computed(() => route.matched[route.matched.length - 1])
const currentRouteName = computed(() => {
  const nameMap: Record<string, string> = {
    'dashboard': '数据看板',
    'monitor': '实时监控',
    'analysis': '数据分析',
    'resource': '资源管理',
    'review': '评价管理',
    'tickets': '门票订单',
    'rescue': '紧急救援',
    'news': '新闻资讯',
    'profile': '商家资料',
    'statistics': '统计报表',
    'prediction': '客流预测',
    'policy-sandbox': '政策沙盘',
    'export': '数据导出'
  }
  
  const path = route.path.split('/').filter(Boolean)
  return path.length > 1 ? nameMap[path[path.length - 1]] || '未知页面' : '首页'
})

// 切换侧边栏
const toggleSidebar = () => {
  isCollapse.value = !isCollapse.value
}

// 前往账号设置
const goToProfile = () => {
  router.push('/business/account')
}

// 退出登录
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
</script>

<style scoped>
::deep(.el-menu) {
  background-color: transparent;
}

::deep(.el-menu-item),
::deep(.el-sub-menu__title) {
  color: #606266;
  height: 50px;
  line-height: 50px;
}

::deep(.el-menu-item.is-active) {
  background: rgba(42, 157, 143, 0.1);
  color: #2A9D8F;
  border-right: 2px solid #2A9D8F;
}

::deep(.el-menu-item:hover),
::deep(.el-sub-menu__title:hover) {
  background: #F5F7FA;
  color: #2A9D8F;
}

::deep(.el-sub-menu.is-active .el-sub-menu__title) {
  color: #2A9D8F;
}
</style> 