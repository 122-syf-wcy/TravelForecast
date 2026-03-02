<template>
  <div class="page">
    <div class="header">
      <h1>三维政策沙盘（交互模拟）</h1>
      <p class="sub">联票折扣 / 交通补贴 / 容量上限 → 客流 / 收入 / 碳足迹 联动</p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">
      <holographic-card class="controls lg:col-span-1">
        <template #header>
          <div class="flex items-center text-[#2A9D8F] font-bold text-lg mb-2">
            <el-icon class="mr-2"><Operation /></el-icon>
            政策参数调整
          </div>
        </template>
        
        <div class="control-item mb-6">
          <div class="label mb-2 flex justify-between">
            <span class="text-gray-500">联票折扣率</span>
            <span class="text-[#2A9D8F]">{{ discount }}%</span>
          </div>
          <el-slider v-model="discount" :min="0" :max="30" :step="1" :show-tooltip="false" class="neon-slider" />
        </div>
        
        <div class="control-item mb-6">
          <div class="label mb-2 flex justify-between">
            <span class="text-gray-500">交通补贴</span>
            <span class="text-[#457B9D]">¥{{ subsidy }}/人</span>
          </div>
          <el-slider v-model="subsidy" :min="0" :max="100" :step="5" :show-tooltip="false" class="neon-slider purple" />
        </div>
        
        <div class="control-item mb-8">
          <div class="label mb-2 flex justify-between">
            <span class="text-gray-500">日容量上限</span>
            <span class="text-green-400">{{ capacity }}人</span>
          </div>
          <el-slider v-model="capacity" :min="2000" :max="12000" :step="500" :show-tooltip="false" class="neon-slider green" />
        </div>

        <div class="simulation-results space-y-4">
          <div class="result-item bg-black bg-opacity-30 p-3 rounded border border-gray-200">
            <div class="text-xs text-gray-500">预计客单价提升</div>
            <div class="text-xl font-bold text-green-400">+{{ priceBoost.toFixed(1) }}%</div>
          </div>
          <div class="result-item bg-black bg-opacity-30 p-3 rounded border border-gray-200">
            <div class="text-xs text-gray-500">客流指数</div>
            <div class="text-xl font-bold text-[#2A9D8F]">{{ flowIndex.toFixed(2) }}</div>
          </div>
          <div class="result-item bg-black bg-opacity-30 p-3 rounded border border-gray-200">
            <div class="text-xs text-gray-500">收入指数</div>
            <div class="text-xl font-bold text-[#457B9D]">{{ incomeIndex.toFixed(2) }}</div>
          </div>
          <div class="result-item bg-black bg-opacity-30 p-3 rounded border border-gray-200">
            <div class="text-xs text-gray-500">碳足迹指数</div>
            <div class="text-xl font-bold text-red-400">{{ carbonIndex.toFixed(2) }}</div>
          </div>
        </div>
      </holographic-card>

      <holographic-card class="chart-card lg:col-span-3" v-loading="loading" element-loading-background="rgba(255, 255, 255, 0.98)">
        <template #header>
          <div class="flex items-center text-[#457B9D] font-bold text-lg">
            <el-icon class="mr-2"><TrendCharts /></el-icon>
            7日推演结果
          </div>
        </template>
        <div ref="chartRef" class="chart h-full w-full" style="min-height: 500px;"></div>
      </holographic-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { Operation, TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import HolographicCard from '@/components/HolographicCard.vue'
import { simulatePolicyEffect } from '@/api/policy'

const discount = ref(10) // %
const subsidy = ref(20)  // 元/人
const capacity = ref(8000) // 人/日

// 从真实API获取的指标
const flowIndex = ref(1.27)
const incomeIndex = ref(1.35)
const carbonIndex = ref(1.22)
const priceBoost = ref(18.0)

const chartRef = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null
const loading = ref(false)

// 存储7日模拟数据
const dailyData = ref<any[]>([])

/**
 * 调用后端API获取真实的政策模拟数据
 * 如果后端未就绪，使用前端计算（基于真实参数）
 */
async function fetchSimulationData() {
  loading.value = true
  try {
    const res: any = await simulatePolicyEffect(discount.value, subsidy.value, capacity.value)
    console.log('政策模拟响应:', res)
    
    if (res.code === 200 && res.data) {
      flowIndex.value = res.data.flowIndex || 1.0
      incomeIndex.value = res.data.incomeIndex || 1.0
      carbonIndex.value = res.data.carbonIndex || 1.0
      priceBoost.value = res.data.priceBoost || 18.0
      dailyData.value = res.data.dailyData || []
      
      // 更新图表
      renderChart()
    }
  } catch (error) {
    console.warn('后端API未就绪，使用前端计算', error)
    // 使用前端计算作为后备方案
    calculateLocalSimulation()
  } finally {
    loading.value = false
  }
}

/**
 * 前端计算模拟数据（当后端未就绪时）
 * 使用与后端相同的算法逻辑
 */
function calculateLocalSimulation() {
  // 折扣效应：每1%折扣提升2%客流
  const discountEffect = 1 + discount.value * 0.02
  
  // 补贴效应：每5元补贴提升2.5%客流
  const subsidyEffect = 1 + subsidy.value * 0.005
  
  // 容量限制效应
  const baseFlow = 5000 // 默认基准客流
  const predictedFlow = baseFlow * discountEffect * subsidyEffect
  const capacityEffect = Math.min(1, capacity.value / predictedFlow)
  
  // 客流指数
  flowIndex.value = Math.max(0.5, Math.min(2.5, (predictedFlow / baseFlow) * capacityEffect))
  
  // 联票折扣刺激客单价提升
  const priceBoostRatio = 1 + (discount.value / 100) * 0.6
  incomeIndex.value = Math.max(0.5, Math.min(3.0, flowIndex.value * priceBoostRatio))
  
  // 碳足迹指数
  const carbonReduction = 1 - subsidy.value * 0.005
  carbonIndex.value = Math.max(0.2, Math.min(2.0, flowIndex.value * Math.max(0.6, carbonReduction)))
  
  // 客单价提升百分比
  priceBoost.value = 18.0 * (discount.value / 20.0 + 1.0)
  
  // 生成7日模拟数据
  dailyData.value = []
  for (let i = 1; i <= 7; i++) {
    const flowVariation = 0.95 + Math.random() * 0.1
    const incomeVariation = 0.95 + Math.random() * 0.1
    const carbonVariation = 0.95 + Math.random() * 0.1
    
    dailyData.value.push({
      day: `D${i}`,
      flow: Math.round(flowIndex.value * flowVariation * 1000) / 1000,
      income: Math.round(incomeIndex.value * incomeVariation * 1000) / 1000,
      carbon: Math.round(carbonIndex.value * carbonVariation * 1000) / 1000
    })
  }
  
  // 更新图表
  renderChart()
}

/**
 * 渲染图表
 */
function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)

  const days = dailyData.value.map(d => d.day)
  const flow = dailyData.value.map(d => d.flow)
  const income = dailyData.value.map(d => d.income)
  const carbon = dailyData.value.map(d => d.carbon)

  chart.setOption({
    backgroundColor: 'transparent',
    legend: {
      data: ['客流指数', '收入指数', '碳足迹指数'],
      top: 0,
      textStyle: { color: '#909399' },
      icon: 'circle'
    },
    tooltip: { 
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: '#2A9D8F',
      textStyle: { color: '#303133' },
      axisPointer: { type: 'cross', label: { backgroundColor: '#6a7985' } }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: days,
      axisLabel: { color: '#909399' },
      axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.04)' } }
    },
    yAxis: {
      type: 'value',
      name: '指数（相对）',
      nameTextStyle: { color: '#909399' },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: 'rgba(42, 157, 143, 0.03)', type: 'dashed' } },
      axisLine: { show: true, lineStyle: { color: 'rgba(0, 0, 0, 0.04)' } },
      min: 0,
      max: 2.5
    },
    series: [
      { 
        name: '客流指数', 
        type: 'line', 
        smooth: true, 
        data: flow, 
        lineStyle: { width: 3, color: '#2A9D8F' },
        itemStyle: { color: '#2A9D8F' },
        symbol: 'circle',
        symbolSize: 8,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(42, 157, 143, 0.3)' },
            { offset: 1, color: 'rgba(42, 157, 143, 0.05)' }
          ])
        }
      },
      { 
        name: '收入指数', 
        type: 'line', 
        smooth: true, 
        data: income, 
        lineStyle: { width: 3, color: '#457B9D' },
        itemStyle: { color: '#457B9D' },
        symbol: 'circle',
        symbolSize: 8
      },
      { 
        name: '碳足迹指数', 
        type: 'line', 
        smooth: true, 
        data: carbon, 
        lineStyle: { width: 3, color: '#E76F51' },
        itemStyle: { color: '#E76F51' },
        symbol: 'circle',
        symbolSize: 8
      }
    ]
  })
}

// 监听参数变化，实时更新模拟数据
// 使用防抖避免频繁请求
let debounceTimer: any = null
watch([discount, subsidy, capacity], () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    fetchSimulationData()
  }, 300)
})

function onResize() { 
  chart?.resize() 
}

onMounted(() => {
  // 初始加载
  fetchSimulationData()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.page { display: flex; flex-direction: column; min-height: 80vh; }
.header h1 { margin: 0; font-size: 24px; font-weight: 900; color: #303133; margin-bottom: 0.5rem; }
.header .sub { margin: 0; color: #A0AEC0; margin-bottom: 2rem; }

/* 霓虹滑块样式 */
.neon-slider {
  --el-slider-main-bg-color: #2A9D8F;
  --el-slider-runway-bg-color: #E5E7EB;
}

.neon-slider.purple {
  --el-slider-main-bg-color: #457B9D;
}

.neon-slider.green {
  --el-slider-main-bg-color: #2A9D8F;
}

:deep(.el-slider__button) {
  background-color: #F5F7FA;
  border: 2px solid var(--el-slider-main-bg-color);
}
</style>
