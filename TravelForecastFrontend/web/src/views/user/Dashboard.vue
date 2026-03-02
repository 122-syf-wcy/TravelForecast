<template>
  <div class="dashboard-container">
    <div class="grid grid-cols-1 md:grid-cols-12 gap-6">
      <!-- 欢迎卡片 -->
      <div class="col-span-12 md:col-span-8">
        <div class="welcome-banner scenic-banner">
          <div class="banner-content">
            <div class="banner-left">
              <div class="banner-header">
                <div class="banner-icon">
                  <el-icon :size="24"><DataAnalysis /></el-icon>
                </div>
                <div class="banner-badge">
                  <span class="badge-dot"></span>
                  <span>智能预测</span>
                </div>
              </div>
              <h2 class="banner-title">欢迎来到游韵华章</h2>
              <p class="banner-subtitle">基于多源数据的六盘水智慧旅游预测平台，为您提供科学的行程规划与实时服务。</p>
              <div class="banner-features">
                <div class="feature-item" @click="router.push('/user/prediction')">
                  <span class="feature-icon">📊</span>
                  <span>客流预测</span>
                </div>
                <div class="feature-item" @click="router.push('/user/planning')">
                  <span class="feature-icon">🗺️</span>
                  <span>智能规划</span>
                </div>
                <div class="feature-item" @click="router.push('/user/service')">
                  <span class="feature-icon">⚡</span>
                  <span>实时服务</span>
                </div>
              </div>
              <div class="banner-actions">
                <el-button type="primary" round class="start-btn" size="large" @click="navigateToPlanning">
                  开始规划旅程 <el-icon class="ml-2"><ArrowRight /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
          <!-- 背景装饰：简单的山水波纹或图片 -->
          <div class="banner-decoration-image"></div>
        </div>
      </div>

      <!-- 天气预报卡片 -->
      <div class="col-span-12 md:col-span-4">
        <div class="weather-banner scenic-weather" :class="getWeatherBannerClass()">
          <div class="weather-content">
            <div class="weather-header">
              <h3 class="weather-title">六盘水天气</h3>
              <div class="weather-icon-wrapper">
                <span class="weather-emoji">{{ getWeatherEmoji() }}</span>
              </div>
            </div>
            <div class="weather-main">
              <div class="temp-display">
                <span class="temp-value">{{ weather.temperature.toFixed(0) }}</span>
                <span class="temp-unit">°C</span>
              </div>
              <div class="weather-info">
                <div class="condition-tag">{{ weather.condition }}</div>
                <div class="humidity-info">
                  <span class="humidity-icon">💧</span>
                  <span>湿度: {{ Math.round(weather.humidity) }}%</span>
                </div>
              </div>
            </div>
            <div class="weather-footer">
              <div class="temp-range">
                <span class="range-label">今日温度：</span>
                <span class="range-value">{{ Math.round(weather.minTemp) }}°C - {{ Math.round(weather.maxTemp) }}°C</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 景区流量统计 -->
      <div class="col-span-12">
        <scenic-card class="flow-chart-card">
          <template #header>
            <div class="flex justify-between items-center">
              <div class="flex items-center gap-3">
                <span class="header-icon-box">
                  <el-icon><PieChart /></el-icon>
                </span>
                <h3 class="text-xl font-bold text-gray-800">景区实时流量</h3>
              </div>
              <div class="header-badge-simple">
                <span class="pulse-dot-green"></span>
                <span>实时更新</span>
              </div>
            </div>
          </template>
          
          <div class="flow-section-layout">
            <!-- 左侧统计卡片 -->
            <div class="flow-stats-left">
              <div class="flow-stat-item">
                <div class="stat-icon-box blue">
                  <span class="stat-icon">👥</span>
                </div>
                <div class="stat-info">
                  <span class="stat-label">今日总游客</span>
                  <span class="stat-value">{{ totalVisitorsToday.toLocaleString() }}</span>
                </div>
                <div class="stat-trend" :class="flowGrowthRate >= 0 ? 'text-red-500' : 'text-green-500'">
                  <span>{{ flowGrowthRate >= 0 ? '↑' : '↓' }} {{ Math.abs(flowGrowthRate) }}%</span>
                </div>
              </div>
              
              <div class="flow-stat-item">
                <div class="stat-icon-box purple">
                  <span class="stat-icon">📈</span>
                </div>
                <div class="stat-info">
                  <span class="stat-label">峰值时段</span>
                  <span class="stat-value">{{ peakTimeRange }}</span>
                </div>
                <div class="stat-tag" v-if="isPeakTime">高峰</div>
              </div>
              
              <div class="flow-stat-item">
                <div class="stat-icon-box orange">
                  <span class="stat-icon">⏱️</span>
                </div>
                <div class="stat-info">
                  <span class="stat-label">平均停留</span>
                  <span class="stat-value">{{ avgStayTime }} 小时</span>
                </div>
              </div>
            </div>
            
            <!-- 中间图表 -->
            <div class="flow-chart-wrapper">
              <div class="h-full w-full" ref="flowChartRef"></div>
            </div>
            
            <!-- 右侧景区列表 -->
            <div class="flow-stats-right">
              <div class="scenic-flow-list">
                <div 
                  v-for="(item, index) in scenicFlowData" 
                  :key="item.id" 
                  class="scenic-flow-item"
                >
                  <div class="flow-item-rank" :class="'rank-' + (index + 1)">
                    {{ index + 1 }}
                  </div>
                  <div class="flow-item-info">
                    <span class="flow-item-name">{{ item.name }}</span>
                    <div class="flow-item-bar">
                      <div 
                        class="flow-item-progress" 
                        :style="{ 
                          width: getFlowPercentage(item.currentFlow) + '%',
                          background: getFlowColor(index)
                        }"
                      ></div>
                    </div>
                  </div>
                  <div class="flow-item-count">
                    <span class="count-value">{{ item.currentFlow }}</span>
                    <span class="count-unit">人</span>
                  </div>
                </div>
              </div>
              
              <div class="flow-summary">
                <div class="summary-item">
                  <span class="summary-dot green"></span>
                  <span class="summary-text">舒适 {{ getFlowStatusCount('low') }} 个</span>
                </div>
                <div class="summary-item">
                  <span class="summary-dot yellow"></span>
                  <span class="summary-text">适中 {{ getFlowStatusCount('medium') }} 个</span>
                </div>
                <div class="summary-item">
                  <span class="summary-dot red"></span>
                  <span class="summary-text">拥挤 {{ getFlowStatusCount('high') }} 个</span>
                </div>
              </div>
            </div>
          </div>
        </scenic-card>
      </div>

      <!-- 景点推荐 -->
      <div class="col-span-12 md:col-span-6">
        <scenic-card class="h-full" title="热门景点推荐" icon="Location">
          <template #header>
            <el-button type="primary" link @click="viewAllSpots">
              查看更多 <el-icon class="ml-1"><ArrowRight /></el-icon>
            </el-button>
          </template>
          <div class="spots-list">
            <div v-for="(spot, index) in popularSpots" :key="index" class="spot-item-card">
              <div class="spot-image-wrapper">
                <el-image 
                  :src="normalizeImageUrl(spot.image)" 
                  fit="cover" 
                  class="spot-image" 
                />
              </div>
              <div class="spot-content">
                <div class="flex justify-between items-start">
                  <h4 class="spot-name">{{ spot.name }}</h4>
                  <div class="spot-rating">
                    <el-icon class="text-yellow-400 mr-1"><StarFilled /></el-icon>
                    <span>{{ spot.rating }}</span>
                  </div>
                </div>
                <p class="spot-desc">{{ spot.description }}</p>
                <div class="spot-footer">
                  <el-button 
                    :type="spot.isFavorited ? 'warning' : 'default'"
                    size="small" 
                    circle
                    @click="toggleFavorite(spot)"
                    :loading="spot.favoriteLoading"
                  >
                    <el-icon><component :is="spot.isFavorited ? 'StarFilled' : 'Star'" /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </scenic-card>
      </div>

      <!-- 新闻通知 -->
      <div class="col-span-12 md:col-span-6">
        <scenic-card class="h-full" title="旅游资讯" icon="Bell">
           <template #header>
            <el-button type="primary" link @click="viewAllNews">
              查看更多 <el-icon class="ml-1"><ArrowRight /></el-icon>
            </el-button>
          </template>
          <div class="news-list-simple">
            <div v-for="(news, index) in tourismNews" :key="index" class="news-item-simple" @click="viewNewsDetail(news)">
              <div class="news-date-box">
                <span class="day">{{ new Date(news.publishTime).getDate() }}</span>
                <span class="month">{{ new Date(news.publishTime).getMonth() + 1 }}月</span>
              </div>
              <div class="news-content">
                <h4 class="news-title">{{ news.title }}</h4>
                <p class="news-desc">{{ news.summary }}</p>
              </div>
            </div>
          </div>
        </scenic-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { PieChart, Location, Sunny, Bell, DataAnalysis, ArrowRight, Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ScenicCard from '@/components/ScenicCard.vue'
import * as echarts from 'echarts'
import { getPopularSpots, getScenicFlowStats, getWeatherInfo, type PopularSpot, type ScenicFlow, type TourismNews } from '@/api/dashboard'
import { getHotNews } from '@/api/news'
import { addFavorite, removeFavorite, checkFavoriteBatch } from '@/api/favorite'
import { normalizeImageUrl } from '@/utils/imageProxy'

const router = useRouter()
const userStore = useUserStore()
const flowChartRef = ref(null)
let flowChart: any = null

const navigateToPlanning = () => {
  router.push('/user/planning')
}

// 响应式数据
const popularSpots = ref<PopularSpot[]>([])
const tourismNews = ref<Array<TourismNews & { summary?: string; date?: string }>>([])
const scenicFlowData = ref<ScenicFlow[]>([])
const loading = ref(false)
const weather = ref({ city: '六盘水', temperature: 22, condition: '晴朗', humidity: 65, minTemp: 18, maxTemp: 25 })

// 计算今日总游客数
const totalVisitorsToday = computed(() => {
  return scenicFlowData.value.reduce((sum, item) => sum + item.currentFlow, 0)
})

// 获取流量百分比（用于进度条）
const getFlowPercentage = (flow: number) => {
  const maxFlow = Math.max(...scenicFlowData.value.map(item => item.currentFlow), 1)
  return (flow / maxFlow) * 100
}

// 获取流量颜色 - 自然色系
const getFlowColor = (index: number) => {
  const colors = [
    'linear-gradient(90deg, #2A9D8F, #4DB6AC)', // 青绿
    'linear-gradient(90deg, #457B9D, #669BBC)', // 蓝灰
    'linear-gradient(90deg, #A8DADC, #C4E6EB)', // 浅青 (替代土黄)
    'linear-gradient(90deg, #F4A261, #FAB882)', // 橙色 (保留强调)
    'linear-gradient(90deg, #E76F51, #FF8A65)'  // 赭石
  ]
  return colors[index % colors.length]
}

// 获取各状态景区数量
const getFlowStatusCount = (status: string) => {
  return scenicFlowData.value.filter(item => {
    const percentage = (item.currentFlow / (item.maxCapacity || 1000)) * 100
    if (status === 'low') return percentage < 30
    if (status === 'medium') return percentage >= 30 && percentage < 70
    return percentage >= 70
  }).length
}

// 计算流量增长率
const flowGrowthRate = computed(() => {
  if (scenicFlowData.value.length === 0) return 0
  const avgRate = scenicFlowData.value.reduce((sum, item) => {
    return sum + (item.flowRate || 0)
  }, 0) / scenicFlowData.value.length
  if (avgRate > 0.5) return Math.round((avgRate - 0.4) * 50 * 10) / 10
  return Math.round((avgRate - 0.3) * 30 * 10) / 10
})

// 获取峰值时段
const peakTimeRange = computed(() => {
  const hour = new Date().getHours()
  if (hour >= 10 && hour < 12) return '10:00-12:00'
  if (hour >= 14 && hour < 16) return '14:00-16:00'
  if (hour >= 16 && hour < 18) return '16:00-18:00'
  return '14:00-16:00'
})

// 判断当前是否为高峰时段
const isPeakTime = computed(() => {
  const hour = new Date().getHours()
  return (hour >= 10 && hour < 12) || (hour >= 14 && hour < 18)
})

// 计算平均停留时间
const avgStayTime = computed(() => {
  if (scenicFlowData.value.length === 0) return '2.5'
  const avgRate = scenicFlowData.value.reduce((sum, item) => {
    return sum + (item.flowRate || 0)
  }, 0) / scenicFlowData.value.length
  const baseTime = 3.5 - avgRate * 1.5
  return Math.max(2, Math.min(4.5, baseTime)).toFixed(1)
})

// 根据天气状况获取emoji图标
const getWeatherEmoji = () => {
  const condition = weather.value.condition || ''
  if (condition.includes('晴')) return '☀️'
  if (condition.includes('多云')) return '⛅'
  if (condition.includes('阴')) return '☁️'
  if (condition.includes('雨')) return '🌧️'
  if (condition.includes('雪')) return '❄️'
  return '🌤️'
}

// 根据天气状况获取图标容器样式类
const getWeatherBannerClass = () => {
  const condition = weather.value.condition || ''
  if (condition.includes('晴')) return 'weather-sunny'
  if (condition.includes('雨')) return 'weather-rainy'
  if (condition.includes('阴') || condition.includes('多云')) return 'weather-cloudy'
  return ''
}

// 加载热门景点
const loadPopularSpots = async () => {
  try {
    const res: any = await getPopularSpots(3)
    const data = res?.data || res
    
    if (data && Array.isArray(data)) {
      popularSpots.value = data.map((spot: any) => {
        const imgUrl = spot.imageUrl || spot.image_url || spot.image || ''
        return {
          ...spot,
          image: imgUrl,
          isFavorited: false,
          favoriteLoading: false
        }
      })
      if (userStore.isLoggedIn && popularSpots.value.length > 0) {
        try {
          const scenicIds = popularSpots.value.map(spot => spot.id)
          const checkRes: any = await checkFavoriteBatch(scenicIds)
          const favoriteMap = checkRes?.data || checkRes || {}
          popularSpots.value.forEach(spot => {
            spot.isFavorited = favoriteMap[spot.id] === true
          })
        } catch (error) {
          console.error('批量检查收藏状态失败:', error)
        }
      }
    }
  } catch (error) {
    console.error('加载热门景点失败:', error)
  }
}

// 切换收藏状态
const toggleFavorite = async (spot: any) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  spot.favoriteLoading = true
  try {
    if (spot.isFavorited) {
      await removeFavorite(spot.id)
      spot.isFavorited = false
    } else {
      await addFavorite(spot.id)
      spot.isFavorited = true
    }
    ElMessage.success(spot.isFavorited ? '收藏成功' : '已取消收藏')
  } catch (error: any) {
    ElMessage.error(error?.message || '操作失败')
  } finally {
    spot.favoriteLoading = false
  }
}

// 加载旅游资讯
const loadTourismNews = async () => {
  try {
    const res: any = await getHotNews({ page: 1, size: 3 })
    if (res.data && res.data.records) {
      tourismNews.value = res.data.records.map((item: any) => ({
        ...item,
        summary: item.summary || (item.content ? item.content.substring(0, 50) + '...' : ''),
        date: item.publishTime
      }))
    }
  } catch (error) {
    console.error('加载资讯失败:', error)
  }
}

const viewAllSpots = () => router.push('/user/popular-spots')
const viewAllNews = () => router.push('/user/news')
const viewNewsDetail = (news: any) => router.push(`/user/news?id=${news.id}`)

const formatNewsDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

// 加载景区流量数据
const loadScenicFlow = async () => {
  try {
    const res: any = await getScenicFlowStats()
    const data = res?.data || res
    if (data && Array.isArray(data)) scenicFlowData.value = data
    updateFlowChart()
  } catch (error) {
    console.error('加载流量失败:', error)
    // Mock data needed if api fails? logic kept simple here
  }
}

const initFlowChart = () => {
  if (flowChartRef.value && !flowChart) {
    flowChart = echarts.init(flowChartRef.value)
    updateFlowChart()
    window.addEventListener('resize', handleResize)
  }
}

const loadWeather = async () => {
  try {
    const res: any = await getWeatherInfo('六盘水')
    const data: any = res && typeof res === 'object' && 'data' in res ? res.data : res
    if (data && typeof data === 'object') weather.value = { ...weather.value, ...data }
  } catch (e) { console.warn('获取天气失败', e) }
}

const updateFlowChart = () => {
  if (!flowChart || scenicFlowData.value.length === 0) return
  
  const totalVisitors = scenicFlowData.value.reduce((sum, item) => sum + item.currentFlow, 0)
  
  const option = {
    color: ['#2A9D8F', '#457B9D', '#A8DADC', '#F4A261', '#E76F51'],
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#E4E7ED',
      borderWidth: 1,
      borderRadius: 8,
      padding: [12, 16],
      textStyle: {
        color: '#1D2129',
        fontSize: 14
      },
      formatter: (params: any) => {
        const percent = ((params.value / totalVisitors) * 100).toFixed(1)
        return `
          <div style="min-width: 140px;">
            <div style="font-size: 14px; font-weight: 600; margin-bottom: 8px; color: #1D2129; display: flex; align-items: center; gap: 8px;">
              <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: ${params.color};"></span>
              ${params.name}
            </div>
            <div style="display: flex; align-items: baseline; gap: 6px; margin-bottom: 6px;">
              <span style="color: #86909C; font-size: 12px;">实时游客</span>
              <span style="color: #2A9D8F; font-weight: 700; font-size: 20px; font-family: -apple-system, sans-serif;">${params.value.toLocaleString()}</span>
            </div>
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="color: #86909C; font-size: 12px;">占比</span>
              <span style="color: #1D2129; font-weight: 600; font-size: 13px;">${percent}%</span>
            </div>
          </div>
        `
      },
      extraCssText: 'box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);'
    },
    legend: {
      orient: 'horizontal',
      bottom: '0%',
      left: 'center',
      itemGap: 16,
      itemWidth: 10,
      itemHeight: 10,
      icon: 'circle',
      textStyle: {
        color: '#4E5969',
        fontSize: 12
      }
    },
    series: [
      {
        name: '游客数量',
        type: 'pie',
        radius: ['52%', '72%'],
        center: ['50%', '42%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          scale: true,
          scaleSize: 10,
          label: {
            show: false // 禁用 hover 时的中心文字显示，避免与固定文字重叠
          }
        },
        labelLine: { show: false },
        data: scenicFlowData.value.map(item => ({
          value: item.currentFlow,
          name: item.name
        }))
      },
      // 内层装饰环
      {
        type: 'pie',
        radius: ['45%', '46%'],
        center: ['50%', '42%'],
        silent: true,
        label: { show: false },
        data: [{ value: 1, name: '' }],
        itemStyle: {
          color: 'rgba(42, 157, 143, 0.05)'
        }
      },
       // 中心文字
       {
        type: 'pie',
        radius: ['0%', '40%'],
        center: ['50%', '42%'],
        silent: true,
        label: {
          show: true,
          position: 'center',
          formatter: () => {
            return `{title|实时总览}\n\n{value|${totalVisitors.toLocaleString()}}`
          },
          rich: {
            title: {
              fontSize: 13,
              color: '#86909C',
              lineHeight: 20,
              fontWeight: '400'
            },
            value: {
              fontSize: 28,
              color: '#1D2129',
              lineHeight: 36,
              fontWeight: '600',
              fontFamily: '-apple-system, sans-serif'
            }
          }
        },
        data: [{ value: 0, name: '' }],
        itemStyle: {
          color: 'transparent'
        }
      }
    ]
  }
  
  flowChart.setOption(option)
}

const handleResize = () => flowChart?.resize()

onMounted(async () => {
  loading.value = true
  await Promise.all([loadPopularSpots(), loadTourismNews(), loadScenicFlow(), loadWeather()])
  loading.value = false
  nextTick(() => initFlowChart())
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (flowChart) {
    flowChart.dispose()
    flowChart = null
  }
})
</script>

<style scoped>
.dashboard-container {
  padding: 24px;
  min-height: 100vh;
  background-color: #F0F2F5;
}

/* 欢迎横幅 - 自然全景风格 */
.scenic-banner {
  position: relative;
  height: 240px;
  border-radius: 16px;
  overflow: hidden;
  background: white;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  display: flex;
}

.banner-decoration-image {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 50%;
  background-image: url('https://smarttourism717.oss-cn-beijing.aliyuncs.com/scenic/images/1/gallery/202510/1760618845010.jpg'); /* 使用真实风景图 */
  background-size: cover;
  background-position: center;
  mask-image: linear-gradient(to right, transparent, black 20%);
  -webkit-mask-image: linear-gradient(to right, transparent, black 20%);
  opacity: 0.9;
}

.banner-content {
  position: relative;
  z-index: 10;
  height: 100%;
  padding: 32px 40px;
  width: 60%;
}

.banner-left {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.banner-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.banner-icon {
  width: 40px;
  height: 40px;
  background: rgba(42, 157, 143, 0.1);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-color-primary);
}

.banner-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: #E8F5E9;
  border-radius: 20px;
  color: #2E7D32;
  font-size: 12px;
  font-weight: 500;
}

.badge-dot {
  width: 6px;
  height: 6px;
  background: #2E7D32;
  border-radius: 50%;
}

.banner-title {
  font-size: 28px;
  font-weight: 700;
  color: #1D2129;
  margin-bottom: 12px;
  letter-spacing: 0.5px;
}

.banner-subtitle {
  font-size: 14px;
  color: #4E5969;
  line-height: 1.6;
  margin-bottom: 24px;
  max-width: 90%;
}

.banner-features {
  display: flex;
  gap: 12px;
  margin-bottom: auto;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #F7F8FA;
  border-radius: 8px;
  color: #4E5969;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.feature-item:hover {
  background: #E6FFFA;
  color: #2A9D8F;
}

.banner-actions {
  margin-top: 20px;
}

/* 天气卡片 - 清新风格 */
.scenic-weather {
  height: 240px;
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.scenic-weather.weather-sunny { background: linear-gradient(135deg, #FFF9C4 0%, #FFFFFF 100%); }
.scenic-weather.weather-cloudy { background: linear-gradient(135deg, #E3F2FD 0%, #FFFFFF 100%); }
.scenic-weather.weather-rainy { background: linear-gradient(135deg, #E1F5FE 0%, #FFFFFF 100%); }

.weather-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.weather-title {
  font-size: 18px;
  font-weight: 600;
  color: #1D2129;
}

.weather-emoji {
  font-size: 32px;
}

.weather-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.temp-display {
  display: flex;
  align-items: flex-start;
}

.temp-value {
  font-size: 56px;
  font-weight: 600;
  color: #1D2129;
  line-height: 1;
}

.temp-unit {
  font-size: 20px;
  color: #86909C;
  margin-top: 8px;
  margin-left: 4px;
}

.weather-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-end;
}

.condition-tag {
  padding: 4px 12px;
  background: rgba(0,0,0,0.05);
  border-radius: 12px;
  color: #4E5969;
  font-size: 14px;
}

.humidity-info {
  font-size: 13px;
  color: #86909C;
  display: flex;
  align-items: center;
  gap: 4px;
}

.weather-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(0,0,0,0.05);
}

.temp-range {
  font-size: 14px;
  color: #4E5969;
}

.range-value {
  font-weight: 600;
  color: #1D2129;
}

/* 流量卡片 */
.flow-chart-card {
  --el-card-padding: 0;
}

.header-icon-box {
  width: 32px;
  height: 32px;
  background: #E6FFFA;
  border-radius: 8px;
  color: #2A9D8F;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-badge-simple {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #10B981;
  background: #ECFDF5;
  padding: 4px 10px;
  border-radius: 12px;
}

.pulse-dot-green {
  width: 6px;
  height: 6px;
  background: #10B981;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4); }
  70% { box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
  100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}

.flow-section-layout {
  display: grid;
  grid-template-columns: 200px 1fr 260px;
  gap: 24px;
  height: 360px;
}

.flow-stats-left {
  display: flex;
  flex-direction: column;
  gap: 16px;
  justify-content: center;
}

.flow-stat-item {
  background: #F7F8FA;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}
.stat-icon-box.blue { background: #E3F2FD; color: #1976D2; }
.stat-icon-box.purple { background: #F3E5F5; color: #7B1FA2; }
.stat-icon-box.orange { background: #FFF3E0; color: #F57C00; }

.stat-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.stat-label { font-size: 12px; color: #86909C; }
.stat-value { font-size: 18px; font-weight: 600; color: #1D2129; }
.stat-trend { font-size: 12px; font-weight: 500; }
.stat-tag { font-size: 12px; color: #F56C6C; background: #FEF0F0; padding: 2px 6px; border-radius: 4px; }

.flow-stats-right {
  display: flex;
  flex-direction: column;
}

.scenic-flow-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
}

.scenic-flow-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #F2F3F5;
}

.flow-item-rank {
  width: 20px;
  height: 20px;
  background: #F2F3F5;
  color: #86909C;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}
.rank-1 { background: #FFF7E6; color: #FA8C16; }
.rank-2 { background: #E6F7FF; color: #1890FF; }
.rank-3 { background: #FFF0F6; color: #EB2F96; }

.flow-item-info { flex: 1; }
.flow-item-name { font-size: 14px; color: #1D2129; display: block; margin-bottom: 4px; }
.flow-item-bar { height: 4px; background: #F2F3F5; border-radius: 2px; overflow: hidden; }
.flow-item-progress { height: 100%; border-radius: 2px; }

.flow-item-count { font-size: 14px; color: #1D2129; font-weight: 500; text-align: right; min-width: 60px; }
.count-unit { font-size: 12px; color: #86909C; margin-left: 2px; }

.flow-summary {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  padding-top: 16px;
  border-top: 1px solid #F2F3F5;
}

.summary-item { display: flex; align-items: center; gap: 6px; }
.summary-dot { width: 8px; height: 8px; border-radius: 50%; }
.summary-dot.green { background: #52C41A; }
.summary-dot.yellow { background: #FAAD14; }
.summary-dot.red { background: #F5222D; }
.summary-text { font-size: 12px; color: #4E5969; }

/* 推荐景点 */
.spots-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.spot-item-card {
  display: flex;
  gap: 16px;
  padding: 12px;
  background: #F7F8FA;
  border-radius: 12px;
  transition: all 0.2s;
}

.spot-item-card:hover { background: #E6FFFA; }

.spot-image-wrapper {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.spot-image { width: 100%; height: 100%; object-fit: cover; }

.spot-content { flex: 1; display: flex; flex-direction: column; justify-content: space-between; }
.spot-name { font-size: 16px; font-weight: 600; color: #1D2129; margin: 0; }
.spot-desc { font-size: 13px; color: #86909C; margin: 4px 0 0; line-clamp: 2; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.spot-footer { display: flex; justify-content: flex-end; margin-top: 8px; }

.spot-rating {
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: #1D2129;
}

/* 资讯简略列表 */
.news-list-simple {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.news-item-simple {
  display: flex;
  gap: 16px;
  cursor: pointer;
  padding-bottom: 16px;
  border-bottom: 1px solid #F2F3F5;
}

.news-item-simple:last-child { border-bottom: none; padding-bottom: 0; }

.news-date-box {
  width: 50px;
  height: 50px;
  background: #E6FFFA;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #2A9D8F;
  flex-shrink: 0;
}

.news-date-box .day { font-size: 20px; font-weight: 700; line-height: 1; }
.news-date-box .month { font-size: 12px; }

.news-content { flex: 1; }
.news-title { font-size: 15px; font-weight: 600; color: #1D2129; margin: 0 0 6px; line-height: 1.4; }
.news-desc { font-size: 13px; color: #86909C; margin: 0; line-clamp: 2; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

@media (max-width: 768px) {
  .flow-section-layout { grid-template-columns: 1fr; height: auto; }
  .flow-chart-wrapper { height: 300px; }
  .welcome-banner { height: auto; flex-direction: column; }
  .banner-decoration-image { width: 100%; height: 160px; position: relative; }
  .banner-content { width: 100%; }
}
</style>