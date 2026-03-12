<template>
  <div class="admin-layout">
    <el-container class="h-screen">
      <!-- 顶部导航 -->
      <el-header height="60px" class="bg-white border-b border-gray-200 flex items-center justify-between px-4 shadow-sm">
        <div class="flex items-center">
          <div class="logo mr-4">
            <h1 class="text-xl font-medium text-[#2A9D8F]">智教黔行管理系统</h1>
          </div>
          
          <!-- 顶部导航菜单 -->
          <div class="hidden lg:block" v-if="isAdmin">
            <el-menu mode="horizontal" :default-active="activeMenu" router background-color="transparent" text-color="#606266">
              <el-menu-item index="/admin/dashboard">控制台</el-menu-item>
              <el-menu-item index="/admin/data">数据中心</el-menu-item>
              <el-menu-item index="/admin/business">商户管理</el-menu-item>
              <el-menu-item index="/admin/user">用户管理</el-menu-item>
              <el-menu-item index="/admin/content">内容管理</el-menu-item>
              <el-menu-item index="/admin/system">系统设置</el-menu-item>
            </el-menu>
          </div>
          
          <!-- 移动端菜单按钮 -->
          <div class="block lg:hidden" v-if="isAdmin">
            <el-button text @click="drawerVisible = true">
              <el-icon class="text-gray-600 text-xl"><Menu /></el-icon>
            </el-button>
          </div>
        </div>
        
        <div class="flex items-center space-x-4">
          <!-- 搜索框 -->
          <div class="hidden md:block">
            <el-input placeholder="搜索..." prefix-icon="Search" class="w-48" />
          </div>
          
          <!-- 用户头像 -->
          <el-dropdown trigger="click">
            <div class="flex items-center cursor-pointer">
              <el-avatar :size="32" :src="userInfo.avatar || 'https://via.placeholder.com/40'" />
              <span class="ml-2 text-gray-700 hidden md:inline">{{ userInfo.nickname || userInfo.username }}</span>
              <el-icon class="ml-1 text-gray-500"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goToAccount">
                  <el-icon><Setting /></el-icon>账号设置
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-container>
        <!-- 侧边栏 -->
        <el-aside width="220px" class="bg-white border-r border-gray-200" v-if="isAdmin">
          <!-- 二级菜单 -->
          <el-menu
            :default-active="activeSubMenu"
            class="h-full border-none bg-transparent"
            router
            unique-opened
          >
            <div class="py-4 px-4 text-gray-500 text-sm">
              {{ currentMainMenuTitle }}
            </div>
            
            <template v-if="activeMenu === '/admin/dashboard'">
              <el-menu-item index="/admin/dashboard/overview">
                <el-icon><DataBoard /></el-icon>
                <span>总览</span>
              </el-menu-item>
              <el-menu-item index="/admin/dashboard/analytics">
                <el-icon><Histogram /></el-icon>
                <span>数据分析</span>
              </el-menu-item>
              <el-menu-item index="/admin/dashboard/monitor">
                <el-icon><Monitor /></el-icon>
                <span>系统监控</span>
              </el-menu-item>
            </template>

            <template v-else-if="activeMenu === '/admin/data'">
              <el-menu-item index="/admin/data/statistics">
                <el-icon><DataLine /></el-icon>
                <span>统计报表</span>
              </el-menu-item>
              <el-menu-item index="/admin/data/prediction">
                <el-icon><DataAnalysis /></el-icon>
                <span>客流预测</span>
              </el-menu-item>
              <el-menu-item index="/admin/data/policy-sandbox">
                <el-icon><Notebook /></el-icon>
                <span>政策沙盘</span>
              </el-menu-item>
              <el-menu-item index="/admin/data/export">
                <el-icon><DocumentCopy /></el-icon>
                <span>数据导出</span>
              </el-menu-item>
            </template>
            
            <template v-else-if="activeMenu === '/admin/business'">
              <el-menu-item index="/admin/business/list">
                <el-icon><Shop /></el-icon>
                <span>商户列表</span>
              </el-menu-item>
              <el-menu-item index="/admin/business/audit">
                <el-icon><Checked /></el-icon>
                <span>审核管理</span>
              </el-menu-item>
              <el-menu-item index="/admin/business/contract">
                <el-icon><Document /></el-icon>
                <span>合同管理</span>
              </el-menu-item>
              <el-menu-item index="/admin/business/audit-logs">
                <el-icon><Document /></el-icon>
                <span>审核日志</span>
              </el-menu-item>
              <el-menu-item index="/admin/business/merchant-scenics">
                <el-icon><PictureFilled /></el-icon>
                <span>可申请景区管理</span>
              </el-menu-item>
            </template>
            
            <template v-else-if="activeMenu === '/admin/user'">
              <el-menu-item index="/admin/user/list">
                <el-icon><UserFilled /></el-icon>
                <span>用户列表</span>
              </el-menu-item>
              <el-menu-item index="/admin/user/behavior">
                <el-icon><View /></el-icon>
                <span>行为分析</span>
              </el-menu-item>
            </template>
            
            <template v-else-if="activeMenu === '/admin/content'">
              <el-menu-item index="/admin/content/landing">
                <el-icon><Monitor /></el-icon>
                <span>首页配置</span>
              </el-menu-item>
              <el-menu-item index="/admin/content/scenic">
                <el-icon><PictureFilled /></el-icon>
                <span>景区管理</span>
              </el-menu-item>
              <el-menu-item index="/admin/content/banner">
                <el-icon><Picture /></el-icon>
                <span>轮播广告</span>
              </el-menu-item>
              <el-menu-item index="/admin/content/showcase">
                <el-icon><VideoCamera /></el-icon>
                <span>实景预览</span>
              </el-menu-item>
              <div class="py-2 px-4 text-gray-400 text-xs">小程序管理</div>
              <el-menu-item index="/admin/content/mp-banner">
                <el-icon><Picture /></el-icon>
                <span>小程序轮播图</span>
              </el-menu-item>
              <el-menu-item index="/admin/content/mp-product">
                <el-icon><Goods /></el-icon>
                <span>文创商品管理</span>
              </el-menu-item>
            </template>
            
            <template v-else-if="activeMenu === '/admin/system'">
              <el-menu-item index="/admin/system/role">
                <el-icon><Lock /></el-icon>
                <span>角色权限</span>
              </el-menu-item>
              <el-menu-item index="/admin/system/settings">
                <el-icon><Setting /></el-icon>
                <span>系统配置</span>
              </el-menu-item>
              <el-menu-item index="/admin/system/notifications">
                <el-icon><Bell /></el-icon>
                <span>系统通知</span>
              </el-menu-item>
            </template>
          </el-menu>
        </el-aside>
        
        <!-- 主体内容 -->
        <el-main class="p-6">
          <!-- 面包屑导航 -->
          <div class="mb-6">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item>{{ currentMainMenuTitle }}</el-breadcrumb-item>
              <el-breadcrumb-item v-if="currentSubMenuTitle">{{ currentSubMenuTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          
          <!-- 路由视图 -->
          <router-view v-slot="{ Component, route: childRoute }">
            <transition name="page-slide" mode="out-in">
              <component :is="Component" :key="childRoute.path" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
    
    <!-- 移动端菜单抽屉 -->
    <el-drawer v-model="drawerVisible" title="导航菜单" direction="ltr" size="70%" v-if="isAdmin">
      <el-menu :default-active="activeMenu" router @select="drawerVisible = false">
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>控制台</span>
        </el-menu-item>
        <el-menu-item index="/admin/business">
          <el-icon><Shop /></el-icon>
          <span>商户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/user">
          <el-icon><UserFilled /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/content">
          <el-icon><PictureFilled /></el-icon>
          <span>内容管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/system">
          <el-icon><Setting /></el-icon>
          <span>系统设置</span>
        </el-menu-item>
      </el-menu>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Menu, ArrowDown, Setting, SwitchButton,
  DataBoard, Histogram, Monitor, DataLine, DataAnalysis, DocumentCopy,
  Shop, Checked, Document, UserFilled, View, PictureFilled,
  Picture, Lock, Notebook, VideoCamera, Bell, Goods
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const drawerVisible = ref(false)

// 用户信息
const userInfo = computed(() => {
  return userStore.userInfo || {
    username: '管理员',
    nickname: '管理员',
    role: 'admin',
    avatar: ''
  }
})

const isAdmin = computed(() => userStore.userInfo?.role === 'admin')

// 当前菜单
const activeMenu = computed(() => {
  const path = route.path
  const parts = path.split('/').filter(p => p)
  if (parts.length >= 2) {
    return `/${parts[0]}/${parts[1]}`
  }
  return path
})

// 当前子菜单
const activeSubMenu = computed(() => {
  return route.path
})

// 当前主菜单标题
const currentMainMenuTitle = computed(() => {
  const menuMap: Record<string, string> = {
    '/admin/dashboard': '控制台',
    '/admin/data': '数据中心',
    '/admin/business': '商户管理',
    '/admin/user': '用户管理',
    '/admin/content': '内容管理',
    '/admin/system': '系统设置'
  }
  
  return menuMap[activeMenu.value] || '首页'
})

// 当前子菜单标题
const currentSubMenuTitle = computed(() => {
  const path = route.path.split('/')
  const subPath = path[path.length - 1]
  
  const subMenuMap: Record<string, Record<string, string>> = {
    '/admin/dashboard': {
      'overview': '总览',
      'analytics': '数据分析',
      'monitor': '系统监控'
    },
    '/admin/data': {
      'statistics': '统计报表',
      'prediction': '客流预测',
      'export': '数据导出'
    },
    '/admin/business': {
      'list': '商户列表',
      'audit': '审核管理',
      'contract': '合同管理',
      'audit-logs': '审核日志',
      'merchant-scenics': '可申请景区管理'
    },
    '/admin/user': {
      'list': '用户列表',
      'behavior': '行为分析'
    },
    '/admin/content': {
      'landing': '首页配置',
      'scenic': '景区管理',
      'banner': '轮播广告',
      'showcase': '实景预览',
      'mp-banner': '小程序轮播图',
      'mp-product': '文创商品管理'
    },
    '/admin/system': {
      'role': '角色权限',
      'settings': '系统配置',
      'notifications': '系统通知'
    }
  }
  
  const currentSubMenus = subMenuMap[activeMenu.value] || {}
  return currentSubMenus[subPath] || ''
})

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

// 顶部下拉：账号设置
const goToAccount = () => {
  router.push('/admin/system/account')
}
</script>

<style scoped>
::deep(.el-menu--horizontal) {
  background-color: transparent;
  border-bottom: none;
}

::deep(.el-menu--horizontal > .el-menu-item) {
  height: 60px;
  line-height: 60px;
  color: #606266;
}

::deep(.el-menu--horizontal > .el-menu-item.is-active) {
  border-bottom: 2px solid #2A9D8F;
  color: #2A9D8F;
}

::deep(.el-menu--horizontal > .el-menu-item:not(.is-disabled):focus),
::deep(.el-menu--horizontal > .el-menu-item:not(.is-disabled):hover) {
  background-color: #F5F7FA;
  color: #2A9D8F;
}

::deep(.el-menu-item.is-active) {
  background: rgba(42, 157, 143, 0.1);
  color: #2A9D8F;
  border-right: 2px solid #2A9D8F;
}

::deep(.el-menu-item),
::deep(.el-sub-menu__title) {
  color: #606266;
  height: 50px;
  line-height: 50px;
}

::deep(.el-menu-item:hover),
::deep(.el-sub-menu__title:hover) {
  background: #F5F7FA;
  color: #2A9D8F;
}
</style> 