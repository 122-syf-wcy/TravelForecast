<template>
  <div class="data-prediction">
    <div class="page-header">
      <h1 class="text-2xl text-gray-800 mb-4">客流预测</h1>
      <p class="text-gray-500">基于LSTM×ARIMA混合时序算法的六盘水景区客流预测系统</p>
    </div>
    
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
      <el-card class="bg-white border border-gray-200 shadow-lg">
        <div class="holographic-card">
          <div class="text-center mb-4">
            <el-icon class="text-4xl text-[#2A9D8F]"><DataAnalysis /></el-icon>
            <h3 class="text-xl text-[#2A9D8F] mt-2">未来7天总客流</h3>
          </div>
          <div class="text-center mb-6">
            <div class="text-4xl font-bold text-gray-800">{{ totalPrediction.total?.toLocaleString() || '0' }}</div>
            <div class="text-sm text-gray-500">预计访问人数</div>
          </div>
          <div class="flex justify-between items-center">
            <div>
              <div class="text-sm text-gray-500">较上周同期</div>
              <div :class="totalPrediction.growthRate >= 0 ? 'text-green-400' : 'text-red-400'" class="flex items-center">
                <el-icon><ArrowUp v-if="totalPrediction.growthRate >= 0" /></el-icon> 
                {{ totalPrediction.growthRate >= 0 ? '+' : '' }}{{ totalPrediction.growthRate }}%
              </div>
            </div>
            <div>
              <div class="text-sm text-gray-500">准确率</div>
              <div class="text-[#2A9D8F]">{{ totalPrediction.accuracy }}%</div>
            </div>
          </div>
        </div>
      </el-card>
      
      <el-card class="bg-white border border-gray-200 shadow-lg">
        <div class="holographic-card">
          <div class="text-center mb-4">
            <el-icon class="text-4xl text-[#457B9D]"><Timer /></el-icon>
            <h3 class="text-xl text-[#457B9D] mt-2">最佳游览时段</h3>
          </div>
          <div class="text-center mb-6">
            <div class="text-4xl font-bold text-gray-800">{{ bestVisitTime.bestTimeRange }}</div>
            <div class="text-sm text-gray-500">{{ bestVisitTime.reason || '平均客流最少时段' }}</div>
          </div>
          <div class="flex justify-between items-center">
            <div>
              <div class="text-sm text-gray-500">拥挤度</div>
              <div class="text-green-400">{{ bestVisitTime.crowdLevel }}</div>
            </div>
            <div>
              <div class="text-sm text-gray-500">等待时间</div>
              <div class="text-[#2A9D8F]">{{ bestVisitTime.waitTime }}</div>
            </div>
          </div>
        </div>
      </el-card>
      
      <el-card class="bg-white border border-gray-200 shadow-lg">
        <div class="holographic-card">
          <div class="text-center mb-4">
            <el-icon class="text-4xl text-orange-400"><Warning /></el-icon>
            <h3 class="text-xl text-orange-400 mt-2">高峰预警</h3>
          </div>
          <div class="text-center mb-6">
            <div class="text-4xl font-bold text-gray-800">{{ peakWarning.warningCount }} 景区</div>
            <div class="text-sm text-gray-500">存在高峰风险</div>
          </div>
          <div class="flex justify-between items-center">
            <div>
              <div class="text-sm text-gray-500">最高风险</div>
              <div class="text-red-400">{{ peakWarning.highestRisk || '无' }}</div>
            </div>
            <div>
              <div class="text-sm text-gray-500">预警等级</div>
              <div :class="getWarningLevelClass(peakWarning.warningLevel)">{{ peakWarning.warningLevel }}</div>
            </div>
          </div>
        </div>
      </el-card>
    </div>
    
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
      <el-card class="bg-white border border-gray-200 shadow-lg">
        <template #header>
          <div class="card-header flex items-center justify-between">
            <h3 class="text-lg text-[#2A9D8F] font-bold">未来7天预测趋势</h3>
            <el-select v-model="selectedScenic" placeholder="选择景区" size="small" @change="updateWeekTrend">
              <el-option v-for="item in scenicOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
            </el-select>
          </div>
        </template>
        <div ref="weekTrendChartRef" class="h-80"></div>
      </el-card>
      
      <el-card class="bg-white border border-gray-200 shadow-lg">
        <template #header>
          <div class="card-header flex items-center justify-between">
            <h3 class="text-lg text-[#457B9D] font-bold">日内客流分布</h3>
            <el-select v-model="selectedDay" placeholder="选择日期" size="small" @change="updateDayDistribution">
              <el-option v-for="day in nextSevenDays" :key="day.value" :label="day.label" :value="day.value"></el-option>
            </el-select>
          </div>
        </template>
        <div ref="dayDistributionChartRef" class="h-80"></div>
      </el-card>
    </div>
    
      <holographic-card class="mb-6">
      <template #header>
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg text-green-400 font-bold">{{ getModelTitle() }}</h3>
          <div class="flex items-center space-x-2">
            <el-select v-model="selectedModel" placeholder="选择模型" size="small" @change="handleModelChange">
              <el-option v-for="model in modelOptions" :key="model.value" :label="model.label" :value="model.value"></el-option>
            </el-select>
          </div>
        </div>
      </template>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <div ref="modelAccuracyChartRef" class="h-60"></div>
          <div class="text-center mt-2 text-gray-500">模型准确度历史（96%+目标线）</div>
        </div>
        <div>
          <div class="mb-4">
            <div class="flex justify-between mb-1">
              <span class="text-gray-500">MAE (平均绝对误差)</span>
              <span class="text-[#2A9D8F]">142.8</span>
            </div>
            <el-progress :percentage="88" color="#2A9D8F" :show-text="false" />
          </div>
          <div class="mb-4">
            <div class="flex justify-between mb-1">
              <span class="text-gray-500">RMSE (均方根误差)</span>
              <span class="text-[#2A9D8F]">215.3</span>
            </div>
            <el-progress :percentage="85" color="#2A9D8F" :show-text="false" />
          </div>
          <div class="mb-4">
            <div class="flex justify-between mb-1">
              <span class="text-gray-500">MAPE (平均绝对百分比误差)</span>
              <span class="text-[#2A9D8F]">5.4%</span>
            </div>
            <el-progress :percentage="96" color="#2A9D8F" :show-text="false" />
          </div>
          <div>
            <div class="flex justify-between mb-1">
              <span class="text-gray-500">R² (决定系数)</span>
              <span class="text-[#2A9D8F]">0.92</span>
            </div>
            <el-progress :percentage="92" color="#2A9D8F" :show-text="false" />
          </div>
        </div>
      </div>
    </holographic-card>
    
    <holographic-card>
      <template #header>
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg text-orange-400 font-bold">景区客流预测详情</h3>
          <el-button type="primary" size="small" @click="exportForecastCSV">导出预测数据</el-button>
        </div>
      </template>
      <el-table :data="forecastData" style="width: 100%" class="bg-transparent">
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="scenic" label="景区" />
        <el-table-column prop="forecast" label="预测客流" width="120">
          <template #default="scope">
            {{ scope.row.forecast?.toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column prop="confidence" label="置信度" width="200">
          <template #default="scope">
            <div class="flex items-center">
              <el-progress :percentage="scope.row.confidence * 100" :color="getConfidenceColor(scope.row.confidence)" :show-text="false" class="flex-grow mr-2" />
              <span>{{ (scope.row.confidence * 100).toFixed(0) }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-center mt-4">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="totalRecords"
          :page-size="pageSize"
          :current-page="currentPage"
          @current-change="handlePageChange"
        />
      </div>
    </holographic-card>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { DataAnalysis, ArrowUp, Timer, Warning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  getNext7DaysTotal,
  getScenicTrend,
  getHourlyDistribution,
  getPeakWarning,
  getPredictionDetails,
  getBestVisitTime
} from '@/api/prediction'
import { getScenicSpots } from '@/api/scenic'
import HolographicCard from '@/components/HolographicCard.vue'

const weekTrendChartRef = ref<HTMLElement | null>(null)
const dayDistributionChartRef = ref<HTMLElement | null>(null)
const modelAccuracyChartRef = ref<HTMLElement | null>(null)
const selectedScenic = ref<number | undefined>(undefined)
const selectedDay = ref<string>()
const selectedModel = ref('dual_stream')

// 预测数据状态
const totalPrediction = ref({
  total: 0,
  growthRate: 0,
  accuracy: 94.2
})

const peakWarning = ref({
  warningCount: 0,
  highestRisk: '无',
  warningLevel: '绿色'
})

const bestVisitTime = ref({
  bestTimeRange: '09:00-11:00',
  crowdLevel: '低',
  waitTime: '<15分钟',
  reason: '平均客流最少时段'
})

// 景区选项（动态加载）
const scenicOptions = ref<any[]>([
  { value: undefined, label: '全部景区' }
])

// 日期选项（动态生成未来7天）
const nextSevenDays = computed(() => {
  const days: any[] = []
  const today = new Date()
  const labels = ['今天', '明天', '后天']
  
  for (let i = 0; i < 7; i++) {
    const date = new Date(today)
    date.setDate(today.getDate() + i)
    const dateStr = date.toISOString().split('T')[0]
    const label = i < 3 ? labels[i] : `${date.getMonth() + 1}月${date.getDate()}日`
    days.push({ value: dateStr, label })
  }
  
  return days
})

// 模型选项
const modelOptions = [
  { value: 'dual_stream', label: '双流融合模型 (ARIMA+LSTM)' },
  { value: 'hybrid', label: '混合模型 (旧版)' },
  { value: 'lstm', label: 'LSTM深度学习模型' },
  { value: 'arima', label: 'ARIMA时序模型' }
]

// 预测数据
const forecastData = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const totalRecords = ref(0)

const getConfidenceColor = (confidence: number) => {
  if (confidence >= 0.95) return '#67C23A'
  if (confidence >= 0.9) return '#409EFF'
  if (confidence >= 0.8) return '#E6A23C'
  return '#F56C6C'
}

const getStatusType = (status: string) => {
  if (status === '正常') return 'success'
  if (status === '偏低') return 'info'
  if (status === '高峰') return 'warning'
  if (status === '爆满') return 'danger'
  return 'info'
}

const getWarningLevelClass = (level: string) => {
  if (level === '红色') return 'text-red-400'
  if (level === '橙色') return 'text-orange-400'
  if (level === '黄色') return 'text-yellow-400'
  return 'text-green-400'
}

const loadScenicList = async () => {
  try {
    const res: any = await getScenicSpots({ city: '六盘水' })
    if (res.code === 200 && res.data) {
      const scenicList = Array.isArray(res.data) 
        ? res.data.map((scenic: any) => ({ value: scenic.id, label: scenic.name }))
        : res.data.list?.map((scenic: any) => ({ value: scenic.id, label: scenic.name })) || []
      scenicOptions.value = [{ value: undefined, label: '全部景区' }, ...scenicList]
    }
  } catch (error) {
    console.error('加载景区列表失败', error)
  }
}

const loadAllData = async () => {
  try {
    await Promise.all([
      loadScenicList(),
      loadTotalPrediction(),
      loadPeakWarning(),
      loadBestVisitTime(),
      loadPredictionDetails()
    ])
    await initWeekTrendChart()
    await initDayDistributionChart()
    initModelAccuracyChart()
  } catch (error) {
    console.error('加载预测数据失败', error)
    ElMessage.error('加载预测数据失败')
  }
}

const loadTotalPrediction = async () => {
  try {
    const res: any = await getNext7DaysTotal(selectedModel.value)
    if (res.code === 200 && res.data) {
      totalPrediction.value = res.data
    }
  } catch (error) {
    console.error('加载总预测失败', error)
  }
}

const loadPeakWarning = async () => {
  try {
    const res: any = await getPeakWarning()
    if (res.code === 200 && res.data) {
      peakWarning.value = res.data
    }
  } catch (error) {
    console.error('加载高峰预警失败', error)
  }
}

const loadBestVisitTime = async () => {
  try {
    const res: any = await getBestVisitTime()
    if (res.code === 200 && res.data) {
      bestVisitTime.value = res.data
    }
  } catch (error) {
    console.error('加载最佳时段失败', error)
  }
}

const loadPredictionDetails = async () => {
  try {
    const res: any = await getPredictionDetails(currentPage.value, pageSize.value)
    if (res.code === 200 && res.data) {
      forecastData.value = res.data.data || []
      totalRecords.value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载预测详情失败', error)
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadPredictionDetails()
}

const initWeekTrendChart = async () => {
  const chartDom = weekTrendChartRef.value
  if (!chartDom) return
  
  try {
    const res: any = await getScenicTrend(selectedScenic.value, selectedModel.value)
    if (res.code !== 200 || !res.data) {
      throw new Error('获取趋势数据失败')
    }
    
    const trendData = res.data
    const myChart = echarts.init(chartDom)
    const option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params: any) => {
          let result = `${params[0].axisValue}<br/>`
          params.forEach((item: any) => {
            const confidence = trendData.confidences?.[item.dataIndex] || 0
            result += `${item.marker} ${item.seriesName}: ${item.value} 人<br/>`
            result += `置信度: ${(confidence * 100).toFixed(1)}%<br/>`
          })
          return result
        }
      },
      legend: { data: ['预测客流'], textStyle: { color: '#303133' } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: trendData.dates || [],
        axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.15)' } },
        axisLabel: { color: '#303133' }
      },
      yAxis: {
        type: 'value',
        name: '预测客流量',
        nameTextStyle: { color: '#303133' },
        axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.15)' } },
        axisLabel: { color: '#303133' },
        splitLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.06)' } }
      },
      series: [{
        name: '预测客流',
        type: 'line',
        smooth: true,
        data: trendData.predictions || [],
        lineStyle: { width: 3, color: '#2A9D8F' },
        areaStyle: {
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(42, 157, 143, 0.3)' },
              { offset: 1, color: 'rgba(42, 157, 143, 0.05)' }
            ]
          }
        },
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: { color: '#2A9D8F', borderColor: '#303133', borderWidth: 2 }
      }]
    }
    
    myChart.setOption(option)
    window.addEventListener('resize', () => myChart.resize())
    ;(window as any).__weekTrendChart = myChart
  } catch (error) {
    console.error('初始化趋势图表失败', error)
  }
}

const initDayDistributionChart = async () => {
  const chartDom = dayDistributionChartRef.value
  if (!chartDom) return
  
  try {
    const defaultDate = nextSevenDays.value[0]?.value
    if (!selectedDay.value && defaultDate) {
      selectedDay.value = defaultDate
    }
    
    const res: any = await getHourlyDistribution(selectedDay.value, selectedScenic.value)
    if (res.code !== 200 || !res.data) {
      throw new Error('获取小时分布数据失败')
    }
    
    const hourlyData = res.data
    const myChart = echarts.init(chartDom)
    const option = {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: hourlyData.hours || [],
        axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.15)' } },
        axisLabel: { color: '#303133' }
      },
      yAxis: {
        type: 'value',
        name: '小时客流量',
        nameTextStyle: { color: '#303133' },
        axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.15)' } },
        axisLabel: { color: '#303133' },
        splitLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.06)' } }
      },
      series: [{
        data: hourlyData.visitors || [],
        type: 'bar',
        barWidth: '60%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#457B9D' },
            { offset: 1, color: '#2A9D8F' }
          ])
        }
      }]
    }
    
    myChart.setOption(option)
    window.addEventListener('resize', () => myChart.resize())
    ;(window as any).__dayDistributionChart = myChart
  } catch (error) {
    console.error('初始化日内分布图表失败', error)
  }
}

const initModelAccuracyChart = () => {
  const chartDom = modelAccuracyChartRef.value
  if (!chartDom) return
  
  const myChart = echarts.init(chartDom)
  const option = {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月'],
      axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.15)' } },
      axisLabel: { color: '#303133' }
    },
    yAxis: {
      type: 'value',
      name: '准确率(%)',
      min: 80,
      max: 100,
      nameTextStyle: { color: '#303133' },
      axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.15)' } },
      axisLabel: { color: '#303133' },
      splitLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.06)' } }
    },
    series: [{
      data: [92.3, 93.1, 94.5, 95.2, 94.8, 96.1],
      type: 'line',
      smooth: true,
      lineStyle: { width: 3, color: '#67C23A' },
      areaStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
            { offset: 1, color: 'rgba(103, 194, 58, 0.05)' }
          ]
        }
      },
      symbol: 'circle',
      symbolSize: 8,
      itemStyle: { color: '#67C23A', borderColor: '#303133', borderWidth: 2 },
      markLine: {
        data: [{ yAxis: 96, label: { formatter: '96%目标线' } }],
        lineStyle: { type: 'dashed', color: '#E6A23C' }
      }
    }]
  }
  
  myChart.setOption(option)
  window.addEventListener('resize', () => myChart.resize())
}

const updateWeekTrend = () => { initWeekTrendChart() }
const updateDayDistribution = () => { initDayDistributionChart() }

const exportForecastCSV = () => {
  if (forecastData.value.length === 0) {
    ElMessage.warning('没有可导出的数据')
    return
  }
  
  let csv = 'data:text/csv;charset=utf-8,'
  csv += '日期,景区,预测客流,置信度,状态\n'
  
  forecastData.value.forEach(row => {
    csv += `${row.date},${row.scenic},${row.forecast},${(row.confidence * 100).toFixed(0)}%,${row.status}\n`
  })
  
  const encodedUri = encodeURI(csv)
  const link = document.createElement('a')
  link.setAttribute('href', encodedUri)
  link.setAttribute('download', `客流预测数据_${new Date().toISOString().split('T')[0]}.csv`)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  ElMessage.success('数据导出成功')
}

onMounted(() => {
  loadAllData()
})

onBeforeUnmount(() => {
  [weekTrendChartRef, dayDistributionChartRef, modelAccuracyChartRef].forEach(r => {
    if (r.value) {
      const inst = echarts.getInstanceByDom(r.value)
      if (inst) inst.dispose()
    }
  })
})

const getModelTitle = () => {
  const model = modelOptions.find(m => m.value === selectedModel.value)
  return model ? `${model.label}性能` : 'LSTM×ARIMA混合模型性能'
}

const handleModelChange = () => {
  ElMessage.success(`已切换到${getModelTitle()}`)
  loadAllData()
}

watch(selectedScenic, () => {
  updateWeekTrend()
  updateDayDistribution()
})
</script>

<style scoped>
.data-prediction {
  padding: 1.5rem;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 2rem;
}

.holographic-card {
  padding: 1rem;
  background: linear-gradient(135deg, rgba(42, 157, 143, 0.05), rgba(69, 123, 157, 0.05));
  border-radius: 0.5rem;
  border: 1px solid rgba(42, 157, 143, 0.2);
}

.card-header {
  color: #2C3E50;
}

:deep(.el-card) {
  background-color: #F9FAFB;
  backdrop-filter: blur(10px);
}

:deep(.el-card__header) {
  border-bottom-color: rgba(0, 0, 0, 0.04);
}

:deep(.el-table) {
  background-color: transparent !important;
  color: #303133;
}

:deep(.el-table tr) {
  background-color: transparent !important;
}

:deep(.el-table th.el-table__cell) {
  background-color: rgba(42, 157, 143, 0.1) !important;
  color: #2A9D8F !important;
  border-bottom: 1px solid rgba(42, 157, 143, 0.3);
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

:deep(.el-table__body tr:hover > td) {
  background-color: rgba(42, 157, 143, 0.05) !important;
}

:deep(.el-select .el-input__wrapper) {
  background-color: #F9FAFB;
  border-color: rgba(42, 157, 143, 0.3);
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #2A9D8F, #457B9D);
  border: none;
}

:deep(.el-pagination) {
  justify-content: center;
}

:deep(.el-pagination.is-background .el-pager li) {
  background-color: #F9FAFB;
  color: #303133;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background: linear-gradient(135deg, #2A9D8F, #457B9D);
}
</style>

