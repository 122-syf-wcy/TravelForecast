import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'

// 定义路由
const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'Root',
    redirect: () => {
      // 根据用户角色动态重定向
      const userStore = useUserStore()
      if (!userStore.isLoggedIn) {
        // 未登录时跳转到 landing 页面
        return { path: '/landing' }
      }

      const roleMap = {
        'user': '/user/dashboard',
        'business': '/business/dashboard',
        'merchant': '/business/dashboard',
        'admin': '/admin/dashboard'
      }

      return { path: roleMap[userStore.userInfo?.role || 'user'] }
    }
  },
  {
    path: '/landing',
    name: 'Landing',
    component: () => import('@/views/Landing.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/ForgotPassword.vue')
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('@/views/ResetPassword.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue')
  },
  // 用户端路由
  {
    path: '/user',
    name: 'UserLayout',
    component: () => import('@/views/user/Layout.vue'),
    redirect: '/user/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'UserDashboard',
        component: () => import('@/views/user/Dashboard.vue'),
        meta: { title: '智慧游六盘水' }
      },
      {
        path: 'scenic',
        name: 'UserScenic',
        component: () => import('@/views/user/ScenicExplore.vue'),
        meta: { title: '景区探索' }
      },
      {
        path: 'prediction',
        name: 'UserPrediction',
        component: () => import('@/views/user/FlowPrediction.vue'),
        meta: { title: '客流预测' }
      },
      {
        path: 'planning',
        name: 'UserPlanning',
        component: () => import('@/views/user/TripPlanning.vue'),
        meta: { title: '行程规划' }
      },
      {
        path: 'service',
        name: 'UserService',
        component: () => import('@/views/user/RealTimeService.vue'),
        meta: { title: '实时服务' }
      },
      {
        path: 'ai',
        name: 'UserAIService',
        component: () => import('@/views/user/AIService.vue'),
        meta: { title: '智能服务' }
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { title: '用户信息管理' }
      },
      {
        path: 'news',
        name: 'TourismNews',
        component: () => import('@/views/user/TourismNews.vue'),
        meta: { title: '旅游资讯' }
      },
      {
        path: 'popular-spots',
        name: 'PopularSpots',
        component: () => import('@/views/user/PopularSpots.vue'),
        meta: { title: '热门景点' }
      }
    ]
  },
  // 商户端路由
  {
    path: '/business',
    name: 'BusinessLayout',
    component: () => import('@/views/business/Layout.vue'),
    redirect: '/business/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'BusinessDashboard',
        component: () => import('@/views/business/Dashboard.vue'),
        meta: { title: '景区智慧管理中心' }
      },
      {
        path: 'monitor',
        name: 'BusinessMonitor',
        component: () => import('@/views/business/RealtimeMonitor.vue'),
        meta: { title: '实时监测' }
      },
      {
        path: 'analysis',
        name: 'BusinessAnalysis',
        component: () => import('@/views/business/DataAnalysis.vue'),
        meta: { title: '数据分析' }
      },
      {
        path: 'resource',
        name: 'BusinessResource',
        component: () => import('@/views/business/ResourceManagement.vue'),
        meta: { title: '资源管理' }
      },
      {
        path: 'profile',
        name: 'BusinessProfile',
        component: () => import('@/views/business/Profile.vue'),
        meta: { title: '商家资料' }
      },
      {
        path: 'account',
        name: 'BusinessAccount',
        component: () => import('@/views/business/AccountSettings.vue'),
        meta: { title: '账号设置' }
      },
      {
        path: 'news',
        name: 'BusinessNews',
        component: () => import('@/views/business/News.vue'),
        meta: { title: '新闻资讯管理' }
      },
      {
        path: 'review',
        name: 'BusinessReview',
        component: () => import('@/views/business/ReviewSystem.vue'),
        meta: { title: '评价系统' }
      },
      {
        path: 'tickets',
        name: 'BusinessTickets',
        component: () => import('@/views/business/TicketOrders.vue'),
        meta: { title: '门票订单管理' }
      },
      {
        path: 'rescue',
        name: 'BusinessRescue',
        component: () => import('@/views/business/EmergencyRescue.vue'),
        meta: { title: '紧急救援管理' }
      },
      // 数据中心 - 商家端专用
      {
        path: 'data',
        name: 'BusinessData',
        redirect: '/business/data/statistics'
      },
      {
        path: 'data/statistics',
        name: 'BusinessDataStatistics',
        component: () => import('@/views/business/data/Statistics.vue'),
        meta: { title: '统计报表' }
      },
      {
        path: 'data/prediction',
        name: 'BusinessDataPrediction',
        component: () => import('@/views/business/data/Prediction.vue'),
        meta: { title: '客流预测' }
      },
      {
        path: 'data/policy-sandbox',
        name: 'BusinessDataPolicySandbox',
        component: () => import('@/views/business/data/PolicySandbox.vue'),
        meta: { title: '政策沙盘' }
      },
      {
        path: 'data/export',
        name: 'BusinessDataExport',
        component: () => import('@/views/business/data/Export.vue'),
        meta: { title: '数据导出' }
      }
    ]
  },
  // 管理员端路由 - 暂时注释掉缺少的组件
  {
    path: '/admin',
    name: 'AdminLayout',
    component: () => import('@/views/admin/Layout.vue'),
    redirect: '/admin/dashboard/overview',
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        redirect: '/admin/dashboard/overview'
      },
      {
        path: 'dashboard/overview',
        name: 'AdminDashboardOverview',
        component: () => import('@/views/admin/dashboard/Overview.vue'),
        meta: { title: '总览' }
      },
      {
        path: 'dashboard/analytics',
        name: 'AdminDashboardAnalytics',
        component: () => import('@/views/admin/dashboard/Analytics.vue'),
        meta: { title: '数据分析' }
      },
      {
        path: 'dashboard/monitor',
        name: 'AdminDashboardMonitor',
        component: () => import('@/views/admin/dashboard/Monitor.vue'),
        meta: { title: '系统监控' }
      },
      {
        path: 'data',
        name: 'AdminData',
        redirect: '/admin/data/statistics'
      },
      {
        path: 'data/statistics',
        name: 'AdminDataStatistics',
        component: () => import('@/views/admin/data/Statistics.vue'),
        meta: { title: '统计报表' }
      },
      {
        path: 'data/prediction',
        name: 'AdminDataPrediction',
        component: () => import('@/views/admin/data/Prediction.vue'),
        meta: { title: '客流预测' }
      },
      {
        path: 'data/policy-sandbox',
        name: 'AdminDataPolicySandbox',
        component: () => import('@/views/admin/data/PolicySandbox.vue'),
        meta: { title: '政策沙盘' }
      },
      {
        path: 'data/export',
        name: 'AdminDataExport',
        component: () => import('@/views/admin/data/Export.vue'),
        meta: { title: '数据导出' }
      },
      {
        path: 'business',
        name: 'AdminBusiness',
        redirect: '/admin/business/list'
      },
      {
        path: 'business/list',
        name: 'AdminBusinessList',
        component: () => import('@/views/admin/business/List.vue'),
        meta: { title: '商户列表' }
      },
      {
        path: 'business/audit',
        name: 'AdminBusinessAudit',
        component: () => import('@/views/admin/business/Audit.vue'),
        meta: { title: '审核管理' }
      },
      {
        path: 'business/contract',
        name: 'AdminBusinessContract',
        component: () => import('@/views/admin/business/Contract.vue'),
        meta: { title: '合同管理' }
      },
      {
        path: 'business/audit-logs',
        name: 'AdminBusinessAuditLogs',
        component: () => import('@/views/admin/business/AuditLogs.vue'),
        meta: { title: '审核日志' }
      },
      {
        path: 'business/merchant-scenics',
        name: 'AdminMerchantScenics',
        component: () => import('@/views/admin/business/MerchantScenics.vue'),
        meta: { title: '可申请景区管理' }
      },
      {
        path: 'user',
        name: 'AdminUser',
        redirect: '/admin/user/list'
      },
      {
        path: 'user/list',
        name: 'AdminUserList',
        component: () => import('@/views/admin/user/List.vue'),
        meta: { title: '用户列表' }
      },
      {
        path: 'user/behavior',
        name: 'AdminUserBehavior',
        component: () => import('@/views/admin/user/Behavior.vue'),
        meta: { title: '行为分析' }
      },
      {
        path: 'content',
        name: 'AdminContent',
        redirect: '/admin/content/landing'
      },
      {
        path: 'content/landing',
        name: 'AdminContentLanding',
        component: () => import('@/views/admin/content/Landing.vue'),
        meta: { title: '首页配置' }
      },
      {
        path: 'content/scenic',
        name: 'AdminContentScenic',
        component: () => import('@/views/admin/content/Scenic.vue'),
        meta: { title: '景区管理' }
      },
      {
        path: 'content/banner',
        name: 'AdminContentBanner',
        component: () => import('@/views/admin/content/Banner.vue'),
        meta: { title: '轮播广告' }
      },
      {
        path: 'content/showcase',
        name: 'AdminContentShowcase',
        component: () => import('@/views/admin/content/Showcase.vue'),
        meta: { title: '实景预览' }
      },
      {
        path: 'content/mp-banner',
        name: 'AdminContentMpBanner',
        component: () => import('@/views/admin/content/MpBanner.vue'),
        meta: { title: '小程序轮播图' }
      },
      {
        path: 'content/mp-product',
        name: 'AdminContentMpProduct',
        component: () => import('@/views/admin/content/MpProduct.vue'),
        meta: { title: '文创商品管理' }
      },
      {
        path: 'system',
        name: 'AdminSystem',
        redirect: '/admin/system/account'
      },
      {
        path: 'system/account',
        name: 'AdminSystemAccount',
        component: () => import('@/views/admin/system/Account.vue'),
        meta: { title: '账号设置' }
      },
      {
        path: 'system/role',
        name: 'AdminSystemRole',
        component: () => import('@/views/admin/system/Role.vue'),
        meta: { title: '角色权限' }
      },
      {
        path: 'system/settings',
        name: 'AdminSystemSettings',
        component: () => import('@/views/admin/system/Settings.vue'),
        meta: { title: '系统配置' }
      },
      {
        path: 'system/notifications',
        name: 'AdminSystemNotifications',
        component: () => import('@/views/admin/SystemNotifications.vue'),
        meta: { title: '系统通知' }
      }
    ]
  },
  // 404页面
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0, behavior: 'smooth' }
  }
})

// 全局前置守卫
router.beforeEach((to, _from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 智教黔行`
  }

  // 初始化用户状态
  const userStore = useUserStore()
  userStore.initUserFromStorage()

  // 路由权限控制
  const isLoggedIn = userStore.isLoggedIn
  const userRole = userStore.userInfo?.role

  // 如果是登录页，已登录则重定向到对应的首页
  if (to.path === '/login' && isLoggedIn) {
    const roleMap = {
      'user': '/user/dashboard',
      'business': '/business/dashboard',
      'merchant': '/business/dashboard',
      'admin': '/admin/dashboard'
    }
    next({ path: roleMap[userRole || 'user'] })
    return
  }

  // 白名单页面：未登录也可访问
  const whiteList = ['/landing', '/login', '/register', '/forgot-password', '/reset-password']
  
  // 访客模式：允许访问的页面（无需登录）
  const guestAllowedPaths = [
    '/user/scenic',      // 景区探索
    '/user/prediction',  // 客流预测
    '/user/planning',    // 行程规划
    '/user/ai'          // AI助手
  ]
  
  const isGuestAllowed = guestAllowedPaths.includes(to.path)
  const isGuestMode = sessionStorage.getItem('guestMode') === 'true'
  
  if (!whiteList.includes(to.path) && !isLoggedIn && !isGuestAllowed) {
    next({ path: '/login' })
    return
  }
  
  // 如果是访客模式访问允许的页面，直接放行
  if (!isLoggedIn && isGuestAllowed && isGuestMode) {
    next()
    return
  }

  // 根据角色层级控制：user < business < admin
  if (isLoggedIn && userRole) {
    const roleRank = { user: 1, business: 2, merchant: 2, admin: 3 } as const
    const needRank = to.path.startsWith('/admin/') ? 3
                   : to.path.startsWith('/business/') ? 2
                   : to.path.startsWith('/user/') ? 1
                   : 1
    const currentRank = roleRank[userRole as keyof typeof roleRank] || 1

    if (currentRank < needRank) {
      const roleMap = {
        'user': '/user/dashboard',
        'business': '/business/dashboard',
        'merchant': '/business/dashboard',
        'admin': '/admin/dashboard'
      }
      next({ path: roleMap[userRole] })
      return
    }
  }

  next()
})

export default router 