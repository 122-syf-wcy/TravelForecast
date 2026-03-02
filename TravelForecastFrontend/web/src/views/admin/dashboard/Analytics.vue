<template>
  <div class="analytics-container">
    <div class="page-header mb-6">
      <h2 class="text-2xl font-bold text-gray-800">数据分析</h2>
      <p class="text-gray-500 mt-1">深度数据挖掘与智能分析</p>
    </div>

    <!-- 时间范围选择 -->
    <div class="mb-6">
      <el-radio-group v-model="timeRange" @change="loadAnalytics">
        <el-radio-button label="today">今日</el-radio-button>
        <el-radio-button label="week">本周</el-radio-button>
        <el-radio-button label="month">本月</el-radio-button>
        <el-radio-button label="year">本年</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 核心指标对比 -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
      <holographic-card 
        v-for="metric in metrics" 
        :key="metric.label"
        class="metric-card"
      >
        <div class="flex items-center justify-between">
          <div>
            <p class="text-gray-500 text-sm mb-1">{{ metric.label }}</p>
            <p class="text-2xl font-bold text-gray-800">{{ metric.value }}</p>
            <div class="flex items-center mt-2">
              <span 
                :class="metric.trend > 0 ? 'text-green-400' : 'text-red-400'"
                class="text-xs"
              >
                <i :class="metric.trend > 0 ? 'ri-arrow-up-line' : 'ri-arrow-down-line'"></i>
                {{ Math.abs(metric.trend) }}%
              </span>
              <span class="text-gray-500 text-xs ml-2">vs 上期</span>
            </div>
          </div>
          <div class="text-4xl opacity-20">{{ metric.icon }}</div>
        </div>
      </holographic-card>
    </div>

    <!-- 图表区域 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
      <!-- 用户增长趋势 -->
      <holographic-card title="用户增长趋势" icon="line-chart-line">
        <div ref="userTrendChart" class="chart-container"></div>
      </holographic-card>

      <!-- 收入分析 -->
      <holographic-card title="收入分析" icon="money-cny-circle-line">
        <div ref="revenueChart" class="chart-container"></div>
      </holographic-card>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 景区热度排行 -->
      <holographic-card title="景区热度排行 TOP10" icon="fire-line">
        <el-table :data="hotScenics" style="width: 100%" :show-header="false">
          <el-table-column type="index" width="50" />
          <el-table-column prop="name" label="景区名称">
            <template #default="{ row }">
              <span class="text-gray-800">{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="visits" label="访问量" width="100">
            <template #default="{ row }">
              <span class="text-[#2A9D8F]">{{ row.visits }}</span>
            </template>
          </el-table-column>
          <el-table-column label="热度" width="150">
            <template #default="{ row }">
              <el-progress 
                :percentage="row.heat" 
                :color="getHeatColor(row.heat)"
                :show-text="false"
              />
            </template>
          </el-table-column>
        </el-table>
      </holographic-card>

      <!-- 用户活跃度分布 -->
      <holographic-card title="用户活跃度分布" icon="pie-chart-line">
        <div ref="activeChart" class="chart-container"></div>
      </holographic-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import HolographicCard from '@/components/HolographicCard.vue'
import * as echarts from 'echarts'
import { getScenicHotRankings, getUserActivityDistribution, getAnalyticsMetrics } from '@/api/admin-dashboard'

const timeRange = ref('month')

const metrics = ref<any[]>([])

const hotScenics = ref<any[]>([])

const userTrendChart = ref()
const revenueChart = ref()
const activeChart = ref()

const getHeatColor = (heat: number) => {
  if (heat >= 80) return '#2A9D8F'
  if (heat >= 60) return '#FFA500'
  return '#888'
}

// 加载核心指标数据
const loadMetrics = async () => {
  try {
    const res: any = await getAnalyticsMetrics(timeRange.value)
    if (res.code === 200 && res.data && res.data.metrics) {
      metrics.value = res.data.metrics
    }
  } catch (error) {
    console.error('加载核心指标失败:', error)
    ElMessage.error('加载核心指标失败')
  }
}

// 加载景区热度排行
const loadHotScenics = async () => {
  try {
    const res: any = await getScenicHotRankings()
    if (res.code === 200 && res.data && res.data.data) {
      hotScenics.value = res.data.data
    }
  } catch (error) {
    console.error('加载景区热度排行失败:', error)
    ElMessage.error('加载景区热度排行失败')
  }
}

// 加载用户活跃度分布
const loadUserActivity = async () => {
  try {
    const res: any = await getUserActivityDistribution()
    if (res.code === 200 && res.data && res.data.data) {
      updateActiveChart(res.data.data)
    }
  } catch (error) {
    console.error('加载用户活跃度分布失败:', error)
    ElMessage.error('加载用户活跃度分布失败')
  }
}

const loadAnalytics = () => {
  console.log('加载数据分析:', timeRange.value)
  loadMetrics()
  loadHotScenics()
  loadUserActivity()
}

const initCharts = async () => {
  // 用户增长趋势图 - 从后端获取数据
  if (userTrendChart.value) {
    const chart1 = echarts.init(userTrendChart.value)
    let months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月']
    let userData = [750, 890, 920, 1100, 1254, 1380, 1520]

    try {
      const { getTrendData } = await import('@/api/admin-dashboard')
      const res: any = await getTrendData('month')
      if (res.code === 200 && res.data) {
        const dates = res.data.dates || res.data.labels || []
        const visitors = res.data.visitors || res.data.values || []
        if (dates.length > 0) {
          months = dates.slice(-7)
          userData = visitors.slice(-7)
        }
      }
    } catch (e) {
      console.warn('用户趋势API不可用，使用默认数据')
    }

    chart1.setOption({
      backgroundColor: 'transparent',
      grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
      xAxis: {
        type: 'category',
        data: months,
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
        name: '新增用户',
        type: 'line',
        data: userData,
        smooth: true,
        lineStyle: { color: '#2A9D8F', width: 3 },
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
        }
      }]
    })
  }

  // 收入分析图 - 从后端获取数据
  if (revenueChart.value) {
    const chart2 = echarts.init(revenueChart.value)
    let revenueMonths = ['1月', '2月', '3月', '4月', '5月', '6月', '7月']
    let revenueData = [85, 92, 101, 115, 126, 138, 152]

    try {
      const { getTourismIncome } = await import('@/api/statistics')
      const res: any = await getTourismIncome('6m')
      if (res.code === 200 && res.data) {
        const incomeData = res.data.data || res.data.list || res.data
        if (Array.isArray(incomeData) && incomeData.length > 0) {
          revenueMonths = incomeData.map((d: any) => d.month || d.date || d.label || '')
          revenueData = incomeData.map((d: any) => d.income || d.revenue || d.value || 0)
        }
      }
    } catch (e) {
      console.warn('收入统计API不可用，使用默认数据')
    }

    chart2.setOption({
      backgroundColor: 'transparent',
      grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
      xAxis: {
        type: 'category',
        data: revenueMonths,
        axisLine: { lineStyle: { color: '#E5E7EB' } },
        axisLabel: { color: '#909399' }
      },
      yAxis: {
        type: 'value',
        axisLine: { lineStyle: { color: '#E5E7EB' } },
        axisLabel: { color: '#909399', formatter: '¥{value}万' },
        splitLine: { lineStyle: { color: '#F0F2F5' } }
      },
      series: [{
        name: '收入',
        type: 'bar',
        data: revenueData,
        itemStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: '#2A9D8F' },
              { offset: 1, color: '#0066FF' }
            ]
          }
        },
        barWidth: '60%'
      }]
    })
  }

  // 用户活跃度分布（初始化空图表）
  if (activeChart.value) {
    const chart3 = echarts.init(activeChart.value)
    chart3.setOption({
      backgroundColor: 'transparent',
      tooltip: { trigger: 'item' },
      legend: {
        orient: 'vertical',
        right: '10%',
        top: 'center',
        textStyle: { color: '#909399' }
      },
      series: [{
        name: '活跃度',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['35%', '50%'],
        data: [],
        label: {
          color: '#909399',
          formatter: '{b}: {d}%'
        }
      }]
    })
  }
}

// 更新用户活跃度图表
const updateActiveChart = (data: any[]) => {
  if (!activeChart.value) return
  
  const chart = echarts.getInstanceByDom(activeChart.value)
  if (!chart) return
  
  // 设置颜色映射
  const colorMap: Record<string, string> = {
    '高活跃': '#2A9D8F',
    '中活跃': '#0066FF',
    '低活跃': '#888'
  }
  
  // 转换数据格式
  const chartData = data.map((item: any) => ({
    value: item.value,
    name: item.name,
    itemStyle: { color: colorMap[item.name] || '#888' }
  }))
  
  chart.setOption({
    series: [{
      data: chartData
    }]
  })
}

onMounted(() => {
  nextTick(() => {
    initCharts()
    loadAnalytics()
  })
})

onBeforeUnmount(() => {
  [userTrendChart, revenueChart, activeChart].forEach(ref => {
    if (ref.value) {
      const inst = echarts.getInstanceByDom(ref.value)
      if (inst) inst.dispose()
    }
  })
})
</script>

<style scoped>
.analytics-container {
  @apply p-6;
}

.metric-card {
  @apply transition-transform hover:scale-105;
}

.chart-container {
  width: 100%;
  height: 300px;
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

:deep(.el-radio-button__inner) {
  background: rgba(42, 157, 143, 0.03);
  border-color: #333;
  color: #888;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #2A9D8F;
  border-color: #2A9D8F;
  color: #000;
}
</style>






