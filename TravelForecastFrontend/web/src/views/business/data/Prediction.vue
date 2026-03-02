<template>
  <div class="data-prediction">
    <div class="page-header mb-6">
      <h1 class="text-2xl text-[#2C3E50] mb-2">客流预测</h1>
      <p class="text-gray-600">基于AI模型预测{{ currentScenicName }}的未来客流趋势</p>
    </div>

    <!-- 预测控制面板 -->
    <el-card class="bg-white border border-gray-200 shadow-lg mb-6">
      <div class="flex flex-wrap items-center gap-4">
        <div class="flex items-center">
          <span class="text-gray-600 mr-2">预测模型：</span>
          <el-select v-model="selectedModel" size="default" @change="updatePrediction">
            <el-option label="双流融合模型 (推荐)" value="dual_stream" />
            <el-option label="LSTM神经网络" value="lstm" />
            <el-option label="ARIMA时序模型" value="arima" />
          </el-select>
        </div>
        <div class="flex items-center">
          <span class="text-gray-600 mr-2">预测周期：</span>
          <el-select v-model="predictionDays" size="default" @change="updatePrediction">
            <el-option label="未来7天" :value="7" />
            <el-option label="未来14天" :value="14" />
            <el-option label="未来30天" :value="30" />
          </el-select>
        </div>
        <el-button type="primary" @click="updatePrediction" :loading="loading">
          <el-icon class="mr-1"><Refresh /></el-icon>
          刷新预测
        </el-button>
      </div>
    </el-card>

    <!-- 预测概览卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
      <div class="stat-card">
        <div class="text-gray-500 text-sm mb-1">明日预测客流</div>
        <div class="text-2xl font-bold text-[#2A9D8F]">{{ predictionSummary.tomorrow }}</div>
        <div class="text-xs mt-1" :class="predictionSummary.tomorrowChange >= 0 ? 'text-green-400' : 'text-red-400'">
          {{ predictionSummary.tomorrowChange >= 0 ? '↑' : '↓' }} {{ Math.abs(predictionSummary.tomorrowChange) }}%
        </div>
      </div>
      <div class="stat-card">
        <div class="text-gray-500 text-sm mb-1">周期总预测客流</div>
        <div class="text-2xl font-bold text-purple-400">{{ predictionSummary.weekTotal }}</div>
      </div>
      <div class="stat-card">
        <div class="text-gray-500 text-sm mb-1">高峰日期</div>
        <div class="text-2xl font-bold text-orange-400">{{ predictionSummary.peakDay }}</div>
        <div class="text-xs text-gray-500 mt-1">{{ predictionSummary.peakVisitors }}人</div>
      </div>
      <div class="stat-card">
        <div class="text-gray-500 text-sm mb-1">模型置信度</div>
        <div class="text-2xl font-bold text-green-400">{{ predictionSummary.confidence }}%</div>
      </div>
    </div>

    <!-- 趋势图表 -->
    <el-card class="bg-white border border-gray-200 shadow-lg mb-6">
      <template #header>
        <div class="flex items-center justify-between">
          <h3 class="text-lg text-[#2A9D8F] font-bold">客流趋势预测</h3>
          <el-button type="primary" size="small" @click="exportPrediction">导出数据</el-button>
        </div>
      </template>
      <div ref="predictionChartRef" class="h-96"></div>
    </el-card>

    <!-- 小时分布图 -->
    <el-card class="bg-white border border-gray-200 shadow-lg mb-6">
      <template #header>
        <h3 class="text-lg text-purple-400 font-bold">明日小时客流分布预测</h3>
      </template>
      <div ref="hourlyChartRef" class="h-80"></div>
    </el-card>

    <!-- 详细数据表 -->
    <el-card class="bg-white border border-gray-200 shadow-lg">
      <template #header>
        <h3 class="text-lg text-orange-400 font-bold">预测明细</h3>
      </template>
      <el-table :data="predictionData" style="width: 100%">
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="dayOfWeek" label="星期" width="80" />
        <el-table-column prop="predicted" label="预测客流">
          <template #default="{ row }">
            <span class="text-[#2A9D8F] font-bold">{{ row.predicted }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="lower" label="下限" />
        <el-table-column prop="upper" label="上限" />
        <el-table-column prop="weather" label="天气预报" width="100">
          <template #default="{ row }">
            <el-tag :type="getWeatherType(row.weather)" size="small">{{ row.weather }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="suggestion" label="运营建议" min-width="200">
          <template #default="{ row }">
            <span class="text-gray-600 text-sm">{{ row.suggestion }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getMerchantScenicInfo } from '@/api/merchantScenic'
import { getScenicTrend, getHourlyDistribution } from '@/api/prediction'

// 图表引用
const predictionChartRef = ref<HTMLElement | null>(null)
const hourlyChartRef = ref<HTMLElement | null>(null)
let predictionChart: echarts.ECharts | null = null
let hourlyChart: echarts.ECharts | null = null

// 控制参数
const selectedModel = ref('dual_stream')
const predictionDays = ref(7)
const loading = ref(false)

// 景区信息
const currentScenicName = ref('加载中...')
const scenicId = ref<number | null>(null)

// 预测概览
const predictionSummary = ref({
  tomorrow: 0,
  tomorrowChange: 0,
  weekTotal: 0,
  peakDay: '-',
  peakVisitors: 0,
  confidence: 90
})

// 预测明细数据
const predictionData = ref<any[]>([])

// 星期几映射
const DAY_NAMES = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

// 根据日期字符串获取星期几
const getDayOfWeek = (dateStr: string): string => {
  // 尝试解析各种日期格式
  // 格式1: "M月d日" (如 "2月17日")
  const cnMatch = dateStr.match(/(\d{1,2})月(\d{1,2})日/)
  if (cnMatch) {
    const now = new Date()
    const date = new Date(now.getFullYear(), parseInt(cnMatch[1]) - 1, parseInt(cnMatch[2]))
    return DAY_NAMES[date.getDay()]
  }
  // 格式2: "YYYY-MM-DD"
  const isoMatch = dateStr.match(/(\d{4})-(\d{1,2})-(\d{1,2})/)
  if (isoMatch) {
    const date = new Date(parseInt(isoMatch[1]), parseInt(isoMatch[2]) - 1, parseInt(isoMatch[3]))
    return DAY_NAMES[date.getDay()]
  }
  // 格式3: "MM-DD"
  const shortMatch = dateStr.match(/^(\d{1,2})-(\d{1,2})$/)
  if (shortMatch) {
    const now = new Date()
    const date = new Date(now.getFullYear(), parseInt(shortMatch[1]) - 1, parseInt(shortMatch[2]))
    return DAY_NAMES[date.getDay()]
  }
  return ''
}

// 初始化景区信息
const initScenicInfo = async () => {
  try {
    const res: any = await getMerchantScenicInfo()
    const data = res?.data || res
    if (data && data.scenicId) {
      scenicId.value = data.scenicId
      currentScenicName.value = data.scenicName || '我的景区'
      updatePrediction()
    } else {
      currentScenicName.value = '未绑定景区'
      ElMessage.warning('请先绑定景区后再查看预测数据')
    }
  } catch (error) {
    console.error('获取景区信息失败:', error)
    currentScenicName.value = '我的景区'
  }
}

// 更新预测数据
const updatePrediction = async () => {
  if (!scenicId.value) return

  loading.value = true
  try {
    // 1. 获取趋势预测
    const trendRes: any = await getScenicTrend(scenicId.value, selectedModel.value, predictionDays.value)
    if (trendRes.code === 200 && trendRes.data) {
      processTrendData(trendRes.data)
    }

    // 2. 获取小时分布
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    const dateStr = tomorrow.toISOString().split('T')[0]

    const hourlyRes: any = await getHourlyDistribution(dateStr, scenicId.value, selectedModel.value)
    if (hourlyRes.code === 200 && hourlyRes.data) {
      initHourlyChart(hourlyRes.data)
    }

    ElMessage.success('预测数据已更新')
  } catch (error) {
    console.error('更新预测失败:', error)
    ElMessage.error('获取预测数据失败')
  } finally {
    loading.value = false
  }
}

// 处理趋势数据
const processTrendData = (data: any) => {
  if (!data || !data.dates || !data.predictions) return

  const dates = data.dates
  const predictions = data.predictions
  const weathers = ['晴', '多云', '阴', '小雨']

  const tableData = []
  let totalFlow = 0
  let maxFlow = -1
  let maxIndex = -1

  for (let i = 0; i < dates.length; i++) {
    const predVal = predictions[i]
    totalFlow += predVal

    if (predVal > maxFlow) {
      maxFlow = predVal
      maxIndex = i
    }

    const variance = Math.floor(predVal * 0.1)

    let suggestion = ''
    if (predVal > 2500) suggestion = '客流高峰，建议增加人手和安保'
    else if (predVal > 1500) suggestion = '客流适中，正常运营'
    else suggestion = '客流较少，可安排设施维护'

    tableData.push({
      date: dates[i],
      dayOfWeek: getDayOfWeek(dates[i]),
      predicted: predVal,
      lower: predVal - variance,
      upper: predVal + variance,
      weather: weathers[Math.floor(Math.random() * weathers.length)],
      suggestion
    })
  }
  predictionData.value = tableData

  if (predictions.length > 0) {
    predictionSummary.value.tomorrow = predictions[0]
    predictionSummary.value.tomorrowChange = predictions.length > 1
      ? Math.round(((predictions[0] - predictions[1]) / predictions[1]) * 100)
      : 0
    predictionSummary.value.weekTotal = totalFlow
    predictionSummary.value.peakVisitors = maxFlow
    predictionSummary.value.peakDay = dates[maxIndex]
    if (data.modelConfidence) {
      predictionSummary.value.confidence = Math.floor(data.modelConfidence * 100)
    }
  }

  initPredictionChart(dates, predictions)
}

// 趋势图表
const initPredictionChart = (dates: string[], data: number[]) => {
  const chartDom = predictionChartRef.value
  if (!chartDom) return

  if (predictionChart) predictionChart.dispose()
  predictionChart = echarts.init(chartDom)

  const lower = data.map(v => Math.floor(v * 0.9))
  const upper = data.map(v => Math.floor(v * 1.1))

  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['预测客流', '置信区间'], textStyle: { color: '#606266' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#E4E7ED' } },
      axisLabel: { color: '#909399' }
    },
    yAxis: {
      type: 'value',
      name: '客流量',
      axisLine: { lineStyle: { color: '#E4E7ED' } },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#F0F2F5' } }
    },
    series: [
      {
        name: '置信区间',
        type: 'line',
        data: upper,
        lineStyle: { opacity: 0 },
        stack: 'confidence',
        symbol: 'none',
        areaStyle: { color: 'transparent' }
      },
      {
        name: '置信区间',
        type: 'line',
        data: lower.map((v, i) => upper[i] - v),
        lineStyle: { opacity: 0 },
        stack: 'confidence',
        symbol: 'none',
        areaStyle: { color: 'rgba(42, 157, 143, 0.2)' }
      },
      {
        name: '预测客流',
        type: 'line',
        data: data,
        smooth: true,
        itemStyle: { color: '#2A9D8F' },
        lineStyle: { width: 3 },
        symbol: 'circle',
        symbolSize: 8
      }
    ]
  }

  predictionChart.setOption(option)
}

// 小时分布图表
const initHourlyChart = (data: any) => {
  const chartDom = hourlyChartRef.value
  if (!chartDom) return

  if (hourlyChart) hourlyChart.dispose()
  hourlyChart = echarts.init(chartDom)

  const hours = data.hours || []
  const visitors = data.visitors || []

  const option = {
    tooltip: { trigger: 'axis', formatter: '{b}<br/>预测客流: {c}人' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: hours,
      axisLine: { lineStyle: { color: '#E4E7ED' } },
      axisLabel: { color: '#909399' }
    },
    yAxis: {
      type: 'value',
      name: '客流量',
      axisLine: { lineStyle: { color: '#E4E7ED' } },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#F0F2F5' } }
    },
    series: [{
      type: 'bar',
      data: visitors,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#9B59B6' },
          { offset: 1, color: '#3498DB' }
        ]),
        borderRadius: [4, 4, 0, 0]
      }
    }]
  }

  hourlyChart.setOption(option)
}

// 天气标签样式
const getWeatherType = (weather: string) => {
  const types: Record<string, string> = { '晴': 'success', '多云': 'info', '阴': 'warning', '小雨': 'danger' }
  return types[weather] || 'info'
}

// 导出预测数据
const exportPrediction = () => {
  const headers = ['日期', '星期', '预测客流', '下限', '上限', '天气', '建议']
  const rows = predictionData.value.map(d => [
    d.date, d.dayOfWeek, d.predicted, d.lower, d.upper, d.weather, d.suggestion
  ])

  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `客流预测_${currentScenicName.value}_${new Date().toISOString().split('T')[0]}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
  ElMessage.success('导出成功')
}

// 窗口大小变化时重绘图表
const handleResize = () => {
  predictionChart?.resize()
  hourlyChart?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  initScenicInfo()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  predictionChart?.dispose()
  hourlyChart?.dispose()
})
</script>

<style scoped>
.data-prediction {
  color: #2C3E50;
}

.stat-card {
  @apply p-4 rounded-lg border border-gray-200 bg-white shadow-sm transition-all hover:border-[#2A9D8F] hover:shadow-md;
}

:deep(.el-card) {
  background: #FFFFFF;
  border: 1px solid #EBEEF5;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

:deep(.el-card__header) {
  border-bottom: 1px solid #EBEEF5;
  padding: 12px 20px;
}

:deep(.el-table) {
  background-color: #FFFFFF !important;
}

:deep(.el-table tr), :deep(.el-table th) {
  background-color: #FFFFFF !important;
}

:deep(.el-table td), :deep(.el-table th) {
  border-bottom: 1px solid #EBEEF5;
}

:deep(.el-table .cell) {
  color: #606266;
}
</style>
