<template>
  <div class="monitor-container">
    <div class="page-header mb-6">
      <h2 class="text-2xl font-bold text-gray-800">系统监控</h2>
      <p class="text-gray-500 mt-1">实时监控系统运行状态</p>
    </div>

    <!-- 系统状态总览 -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
      <holographic-card 
        v-for="status in systemStatus" 
        :key="status.label"
        class="status-card"
      >
        <div class="flex items-center justify-between">
          <div class="flex-1">
            <p class="text-gray-500 text-sm mb-1">{{ status.label }}</p>
            <p class="text-2xl font-bold text-gray-800">{{ status.value }}</p>
            <div class="mt-2">
              <el-progress 
                :percentage="status.percentage" 
                :color="getStatusColor(status.percentage)"
                :show-text="false"
              />
            </div>
          </div>
          <div 
            class="text-3xl ml-4"
            :class="status.percentage > 80 ? 'text-red-400' : 'text-green-400'"
          >
            {{ status.icon }}
          </div>
        </div>
      </holographic-card>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
      <!-- 实时请求监控 -->
      <holographic-card title="实时请求监控" icon="pulse-line">
        <div ref="requestChart" class="chart-container"></div>
        <div class="flex justify-around mt-4">
          <div class="text-center">
            <p class="text-gray-500 text-xs">当前QPS</p>
            <p class="text-[#2A9D8F] text-xl font-bold">{{ currentQPS }}</p>
          </div>
          <div class="text-center">
            <p class="text-gray-500 text-xs">峰值QPS</p>
            <p class="text-orange-400 text-xl font-bold">{{ peakQPS }}</p>
          </div>
          <div class="text-center">
            <p class="text-gray-500 text-xs">错误率</p>
            <p class="text-red-400 text-xl font-bold">{{ errorRate }}%</p>
          </div>
        </div>
      </holographic-card>

      <!-- 响应时间分布 -->
      <holographic-card title="响应时间分布" icon="time-line">
        <div ref="responseChart" class="chart-container"></div>
      </holographic-card>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
      <!-- 服务状态 -->
      <holographic-card title="服务状态" icon="server-line">
        <div class="space-y-3">
          <div 
            v-for="service in services" 
            :key="service.name"
            class="flex items-center justify-between p-3 rounded bg-gray-50"
          >
            <div class="flex items-center">
              <div 
                class="w-2 h-2 rounded-full mr-3"
                :class="service.status === 'running' ? 'bg-green-400 animate-pulse' : 'bg-red-400'"
              ></div>
              <span class="text-gray-800">{{ service.name }}</span>
            </div>
            <el-tag 
              :type="service.status === 'running' ? 'success' : 'danger'"
              size="small"
            >
              {{ service.status === 'running' ? '运行中' : '已停止' }}
            </el-tag>
          </div>
        </div>
      </holographic-card>

      <!-- 数据库连接池 -->
      <holographic-card title="数据库连接池" icon="database-2-line">
        <div class="space-y-4">
          <div>
            <div class="flex justify-between text-sm mb-2">
              <span class="text-gray-500">活跃连接</span>
              <span class="text-[#2A9D8F]">{{ dbPool.active }}/{{ dbPool.max }}</span>
            </div>
            <el-progress 
              :percentage="dbPool.max > 0 ? (dbPool.active / dbPool.max) * 100 : 0" 
              color="#2A9D8F"
            />
          </div>
          <div>
            <div class="flex justify-between text-sm mb-2">
              <span class="text-gray-500">空闲连接</span>
              <span class="text-green-400">{{ dbPool.idle }}</span>
            </div>
            <el-progress 
              :percentage="dbPool.max > 0 ? (dbPool.idle / dbPool.max) * 100 : 0" 
              color="#4ade80"
            />
          </div>
          <div class="grid grid-cols-2 gap-4 mt-4">
            <div class="text-center p-2 bg-gray-50 rounded">
              <p class="text-gray-500 text-xs">平均等待</p>
              <p class="text-gray-800 text-lg">{{ dbPool.avgWait }}ms</p>
            </div>
            <div class="text-center p-2 bg-gray-50 rounded">
              <p class="text-gray-500 text-xs">超时次数</p>
              <p class="text-gray-800 text-lg">{{ dbPool.timeouts }}</p>
            </div>
          </div>
        </div>
      </holographic-card>

      <!-- 缓存命中率 -->
      <holographic-card title="缓存命中率" icon="flashlight-line">
        <div ref="cacheChart" class="chart-container-small"></div>
        <div class="mt-4 space-y-2">
          <div class="flex justify-between text-sm">
            <span class="text-gray-500">命中次数</span>
            <span class="text-green-400">{{ formatNumber(cache.hits) }}</span>
          </div>
          <div class="flex justify-between text-sm">
            <span class="text-gray-500">未命中</span>
            <span class="text-red-400">{{ formatNumber(cache.misses) }}</span>
          </div>
          <div class="flex justify-between text-sm">
            <span class="text-gray-500">总请求</span>
            <span class="text-gray-800">{{ formatNumber(cache.total) }}</span>
          </div>
        </div>
      </holographic-card>
    </div>

    <!-- 告警日志 -->
    <holographic-card title="系统告警" icon="alarm-warning-line">
      <el-table :data="alerts" style="width: 100%">
        <el-table-column prop="time" label="时间" width="180">
          <template #default="{ row }">
            <span class="text-gray-500">{{ row.time }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="100">
          <template #default="{ row }">
            <el-tag 
              :type="row.level === 'critical' ? 'danger' : row.level === 'warning' ? 'warning' : 'info'"
              size="small"
            >
              {{ row.levelText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="150">
          <template #default="{ row }">
            <span class="text-gray-800">{{ row.module }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="告警信息">
          <template #default="{ row }">
            <span class="text-gray-500">{{ row.message }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default>
            <el-button type="primary" size="small" text>查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </holographic-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import HolographicCard from '@/components/HolographicCard.vue'
import * as echarts from 'echarts'
import {
  getSystemStatus,
  getServicesStatus,
  getDbPoolStatus,
  getQpsData,
  getResponseTimeDistribution,
  getCacheStats,
  getAlerts
} from '@/api/system-monitor'

const systemStatus = ref([
  { label: 'CPU使用率', value: '加载中...', percentage: 0, icon: '💻' },
  { label: '内存使用率', value: '加载中...', percentage: 0, icon: '🧠' },
  { label: '磁盘使用率', value: '加载中...', percentage: 0, icon: '💾' },
  { label: '网络带宽', value: '加载中...', percentage: 0, icon: '🌐' }
])

const currentQPS = ref(0)
const peakQPS = ref(0)
const errorRate = ref('0.00')

const services = ref<any[]>([])

const dbPool = ref({
  active: 0,
  idle: 0,
  max: 20,
  avgWait: 0,
  timeouts: 0
})

const cache = ref({
  hits: 0,
  misses: 0,
  total: 0
})

const alerts = ref<any[]>([])

const requestChart = ref()
const responseChart = ref()
const cacheChart = ref()

let requestChartInstance: any = null
let cacheChartInstance: any = null
let responseChartInstance: any = null
let refreshTimer: any = null
let qpsTimer: any = null
let qpsHistory: number[] = []
let qpsTimeLabels: string[] = []

const getStatusColor = (percentage: number) => {
  if (percentage > 80) return '#ef4444'
  if (percentage > 60) return '#f59e0b'
  return '#10b981'
}

const formatNumber = (num: number) => {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

// 辅助函数：Hex转RGBA
const hexToRgba = (hex: string, alpha: number) => {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}


// 加载系统状态
const loadSystemStatus = async () => {
  try {
    const res: any = await getSystemStatus()
    if (res.code === 200 && res.data?.status) {
      systemStatus.value = res.data.status
    }
  } catch (error) {
    console.error('加载系统状态失败:', error)
  }
}

// 加载服务状态
const loadServicesStatus = async () => {
  try {
    const res: any = await getServicesStatus()
    if (res.code === 200 && res.data) {
      services.value = res.data
    }
  } catch (error) {
    console.error('加载服务状态失败:', error)
  }
}

// 加载数据库连接池状态
const loadDbPoolStatus = async () => {
  try {
    const res: any = await getDbPoolStatus()
    if (res.code === 200 && res.data) {
      dbPool.value = res.data
    }
  } catch (error) {
    console.error('加载数据库连接池状态失败:', error)
  }
}

// 加载QPS数据
const loadQpsData = async () => {
  try {
    const res: any = await getQpsData()
    if (res.code === 200 && res.data) {
      currentQPS.value = res.data.current
      peakQPS.value = res.data.peak
      errorRate.value = res.data.errorRate
      
      // 初始化历史数据（只在首次加载时）
      if (res.data.history && qpsHistory.length === 0) {
        // 清空数组再赋值
        qpsHistory.length = 0
        qpsTimeLabels.length = 0
        res.data.history.forEach((p: any) => {
          qpsHistory.push(p.value)
          qpsTimeLabels.push(p.time)
        })
      }
    }
  } catch (error) {
    console.error('加载QPS数据失败:', error)
  }
}

// 加载响应时间分布
const loadResponseTimeDistribution = async () => {
  try {
    const res: any = await getResponseTimeDistribution()
    if (res.code === 200 && res.data) {
      initResponseChart(res.data.labels, res.data.data)
    }
  } catch (error) {
    console.error('加载响应时间分布失败:', error)
  }
}

// 加载缓存统计
const loadCacheStats = async () => {
  try {
    const res: any = await getCacheStats()
    if (res.code === 200 && res.data) {
      cache.value = res.data
      updateCacheChart()
    }
  } catch (error) {
    console.error('加载缓存统计失败:', error)
  }
}

// 加载告警
const loadAlerts = async () => {
  try {
    const res: any = await getAlerts()
    if (res.code === 200 && res.data) {
      alerts.value = res.data
    }
  } catch (error) {
    console.error('加载告警失败:', error)
  }
}

// 初始化QPS图表
const initQpsChart = () => {
  if (!requestChart.value) return
  
  requestChartInstance = echarts.init(requestChart.value)
  
  // 只有在没有历史数据时才初始化（避免重复）
  if (qpsHistory.length === 0) {
    const now = Date.now()
    for (let i = 60; i >= 0; i--) {
      const time = new Date(now - i * 1000)
      qpsTimeLabels.push(`${time.getHours()}:${String(time.getMinutes()).padStart(2, '0')}:${String(time.getSeconds()).padStart(2, '0')}`)
      qpsHistory.push(0)
    }
  }
  
  updateQpsChart()
}

// 更新QPS图表
const updateQpsChart = () => {
  if (!requestChartInstance) return
  
  requestChartInstance.setOption({
    backgroundColor: 'transparent',
    grid: { left: '3%', right: '4%', bottom: '15%', top: '5%', containLabel: true },
    xAxis: {
      type: 'category',
      data: qpsTimeLabels,
      axisLine: { lineStyle: { color: '#E5E7EB' } },
      axisLabel: { 
        color: '#909399', 
        interval: 14,
        rotate: 30,
        fontSize: 10
      }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#E5E7EB' } },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#F0F2F5' } }
    },
    series: [{
      name: 'QPS',
      type: 'line',
      data: qpsHistory,
      smooth: true,
      lineStyle: { color: '#2A9D8F', width: 2 },
      itemStyle: { color: '#2A9D8F' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(42, 157, 143, 0.3)' },
            { offset: 1, color: 'rgba(42, 157, 143, 0)' }
          ]
        }
      },
      showSymbol: false
    }]
  })
}

// 初始化响应时间图表
const initResponseChart = (labels: string[], data: number[]) => {
  if (!responseChart.value) return
  
  if (responseChartInstance) {
    responseChartInstance.dispose()
  }
  
  responseChartInstance = echarts.init(responseChart.value)
  responseChartInstance.setOption({
    backgroundColor: 'transparent',
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#E5E7EB' } },
      axisLabel: { color: '#909399' }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#E5E7EB' } },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#F0F2F5' } }
    },
    series: [{
      name: '请求数',
      type: 'bar',
      data: data,
      itemStyle: {
        color: function(params: any) {
          const colors = ['#2A9D8F', '#00C9A7', '#FFD700', '#FF6B35', '#E76F51', '#457B9D']
          return new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: colors[params.dataIndex % colors.length] },
            { offset: 1, color: hexToRgba(colors[params.dataIndex % colors.length], 0.3) }
          ])
        },
        borderRadius: [4, 4, 0, 0]
      },
      barWidth: '50%'
    }]
  })
}

// 初始化缓存图表
const initCacheChart = () => {
  if (!cacheChart.value) return
  
  cacheChartInstance = echarts.init(cacheChart.value)
  updateCacheChart()
}

// 更新缓存图表
const updateCacheChart = () => {
  if (!cacheChartInstance) return
  
  const hitRate = cache.value.total > 0 
    ? ((cache.value.hits / cache.value.total) * 100).toFixed(1) 
    : '0.0'
  
  cacheChartInstance.setOption({
    backgroundColor: 'transparent',
    series: [{
      type: 'gauge',
      startAngle: 180,
      endAngle: 0,
      min: 0,
      max: 100,
      splitNumber: 10,
      radius: '90%',
      center: ['50%', '65%'],
      axisLine: {
        lineStyle: {
          width: 15,
          color: [
            [0.7, '#E76F51'],
            [0.9, '#FFD700'],
            [1, '#2A9D8F']
          ]
        }
      },
      pointer: {
        itemStyle: { color: '#2A9D8F' },
        length: '70%',
        width: 4
      },
      axisTick: { show: false },
      splitLine: {
        length: 15,
        lineStyle: { color: '#E5E7EB', width: 2 }
      },
      axisLabel: {
        color: '#909399',
        fontSize: 10,
        distance: -40
      },
      title: {
        show: false
      },
      detail: {
        valueAnimation: true,
        formatter: '{value}%',
        color: '#2A9D8F',
        fontSize: 22,
        fontWeight: 'bold',
        offsetCenter: [0, '30%']
      },
      data: [{ value: parseFloat(hitRate) }]
    }]
  })
}

// 实时更新QPS
const startQpsRealtime = () => {
  qpsTimer = setInterval(async () => {
    try {
      const res: any = await getQpsData()
      if (res.code === 200 && res.data) {
        currentQPS.value = res.data.current
        
        // 更新图表数据
        const now = new Date()
        const timeStr = `${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
        
        qpsTimeLabels.shift()
        qpsTimeLabels.push(timeStr)
        qpsHistory.shift()
        qpsHistory.push(res.data.current)
        
        updateQpsChart()
      }
    } catch (error) {
      // 静默处理错误
    }
  }, 2000) // 每2秒更新一次
}

// 定时刷新所有数据
const startRefreshTimer = () => {
  refreshTimer = setInterval(() => {
    loadSystemStatus()
    loadServicesStatus()
    loadDbPoolStatus()
    loadCacheStats()
    loadAlerts()
    loadResponseTimeDistribution()
  }, 10000) // 每10秒刷新一次
}

onMounted(async () => {
  // 先加载QPS数据获取历史记录
  await loadQpsData()
  
  // 加载其他数据
  await Promise.all([
    loadSystemStatus(),
    loadServicesStatus(),
    loadDbPoolStatus(),
    loadCacheStats(),
    loadAlerts()
  ])
  
  // 初始化图表
  nextTick(() => {
    initQpsChart()
    initCacheChart()
    loadResponseTimeDistribution()
    
    // 启动实时更新
    startQpsRealtime()
    startRefreshTimer()
  })
})

onUnmounted(() => {
  if (qpsTimer) clearInterval(qpsTimer)
  if (refreshTimer) clearInterval(refreshTimer)
  if (requestChartInstance) requestChartInstance.dispose()
  if (responseChartInstance) responseChartInstance.dispose()
  if (cacheChartInstance) cacheChartInstance.dispose()
})
</script>

<style scoped>
.monitor-container {
  @apply p-6;
}

.status-card {
  @apply transition-transform hover:scale-105;
}

.chart-container {
  width: 100%;
  height: 280px;
}

.chart-container-small {
  width: 100%;
  height: 180px;
}

:deep(.el-table) {
  background: transparent !important;
}

:deep(.el-table tr) {
  background: transparent !important;
}

:deep(.el-table td),
:deep(.el-table th) {
  border: none !important;
  background: transparent !important;
}

:deep(.el-table__body tr:hover > td) {
  background: rgba(42, 157, 143, 0.03) !important;
}
</style>
