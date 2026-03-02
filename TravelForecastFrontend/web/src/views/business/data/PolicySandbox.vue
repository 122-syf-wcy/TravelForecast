<template>
  <div class="policy-sandbox">
    <div class="page-header mb-6">
      <h1 class="text-2xl text-[#2C3E50] mb-2">政策沙盘</h1>
      <p class="text-gray-600">模拟不同政策对【{{ currentScenicName }}】经营的影响</p>
    </div>
    
    <!-- 政策调整面板 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
      <el-card class="bg-white border border-gray-200 shadow-lg lg:col-span-1">
        <template #header>
          <h3 class="text-lg text-[#2A9D8F] font-bold">政策参数调整</h3>
        </template>
        
        <div class="space-y-6">
          <div>
            <div class="flex justify-between mb-2">
              <span class="text-gray-600">门票价格调整</span>
              <span class="text-[#2A9D8F]">{{ policyParams.ticketPriceChange > 0 ? '+' : '' }}{{ policyParams.ticketPriceChange }}%</span>
            </div>
            <el-slider v-model="policyParams.ticketPriceChange" :min="-50" :max="50" @change="simulatePolicy" />
          </div>
          
          <div>
            <div class="flex justify-between mb-2">
              <span class="text-gray-600">营销投入增加</span>
              <span class="text-purple-400">{{ policyParams.marketingIncrease }}%</span>
            </div>
            <el-slider v-model="policyParams.marketingIncrease" :min="0" :max="100" @change="simulatePolicy" />
          </div>
          
          <div>
            <div class="flex justify-between mb-2">
              <span class="text-gray-600">服务人员增加</span>
              <span class="text-green-400">{{ policyParams.staffIncrease }}%</span>
            </div>
            <el-slider v-model="policyParams.staffIncrease" :min="0" :max="50" @change="simulatePolicy" />
          </div>
          
          <div>
            <div class="flex justify-between mb-2">
              <span class="text-gray-600">设施升级投入</span>
              <span class="text-orange-400">{{ policyParams.facilityUpgrade }}万元</span>
            </div>
            <el-slider v-model="policyParams.facilityUpgrade" :min="0" :max="500" :step="10" @change="simulatePolicy" />
          </div>
          
          <el-button type="primary" class="w-full" @click="resetParams">
            <el-icon class="mr-1"><RefreshRight /></el-icon>
            重置参数
          </el-button>
        </div>
      </el-card>
      
      <el-card class="bg-white border border-gray-200 shadow-lg lg:col-span-2">
        <template #header>
          <h3 class="text-lg text-purple-400 font-bold">模拟效果预测</h3>
        </template>
        
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <div class="text-center p-4 rounded-lg bg-gray-50">
            <div class="text-gray-500 text-sm mb-1">预计客流变化</div>
            <div class="text-2xl font-bold" :class="simulationResult.visitorChange >= 0 ? 'text-green-400' : 'text-red-400'">
              {{ simulationResult.visitorChange >= 0 ? '+' : '' }}{{ simulationResult.visitorChange }}%
            </div>
          </div>
          <div class="text-center p-4 rounded-lg bg-gray-50">
            <div class="text-gray-500 text-sm mb-1">预计收入变化</div>
            <div class="text-2xl font-bold" :class="simulationResult.revenueChange >= 0 ? 'text-green-400' : 'text-red-400'">
              {{ simulationResult.revenueChange >= 0 ? '+' : '' }}{{ simulationResult.revenueChange }}%
            </div>
          </div>
          <div class="text-center p-4 rounded-lg bg-gray-50">
            <div class="text-gray-500 text-sm mb-1">预计满意度变化</div>
            <div class="text-2xl font-bold" :class="simulationResult.satisfactionChange >= 0 ? 'text-green-400' : 'text-red-400'">
              {{ simulationResult.satisfactionChange >= 0 ? '+' : '' }}{{ simulationResult.satisfactionChange }}%
            </div>
          </div>
          <div class="text-center p-4 rounded-lg bg-gray-50">
            <div class="text-gray-500 text-sm mb-1">投资回报周期</div>
            <div class="text-2xl font-bold text-[#2A9D8F]">{{ simulationResult.roiMonths }}个月</div>
          </div>
        </div>
        
        <div ref="simulationChartRef" class="h-64"></div>
      </el-card>
    </div>
    
    <!-- 历史政策效果 -->
    <el-card class="bg-white border border-gray-200 shadow-lg">
      <template #header>
        <h3 class="text-lg text-orange-400 font-bold">历史政策效果分析</h3>
      </template>
      
      <el-table :data="historyPolicies" style="width: 100%">
        <el-table-column prop="date" label="实施日期" width="120" />
        <el-table-column prop="policy" label="政策内容" min-width="200" />
        <el-table-column prop="visitorEffect" label="客流影响" width="120">
          <template #default="{ row }">
            <span :class="row.visitorEffect >= 0 ? 'text-green-400' : 'text-red-400'">
              {{ row.visitorEffect >= 0 ? '+' : '' }}{{ row.visitorEffect }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="revenueEffect" label="收入影响" width="120">
          <template #default="{ row }">
            <span :class="row.revenueEffect >= 0 ? 'text-green-400' : 'text-red-400'">
              {{ row.revenueEffect >= 0 ? '+' : '' }}{{ row.revenueEffect }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '进行中' ? 'success' : 'info'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RefreshRight } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getMerchantScenicInfo } from '@/api/merchantScenic'

const simulationChartRef = ref<HTMLElement | null>(null)

// 当前景区名称（从API获取）
const currentScenicName = ref('加载中...')

// 从API获取商家对应的景区信息
const initScenicInfo = async () => {
  try {
    const res: any = await getMerchantScenicInfo()
    const data = res?.data || res
    
    if (data && data.scenicName) {
      currentScenicName.value = data.scenicName
    } else {
      currentScenicName.value = '未绑定景区'
    }
  } catch (error) {
    console.error('加载景区信息失败:', error)
    currentScenicName.value = '加载失败'
  }
}

// 政策参数
const policyParams = ref({
  ticketPriceChange: 0,
  marketingIncrease: 0,
  staffIncrease: 0,
  facilityUpgrade: 0
})

// 模拟结果
const simulationResult = ref({
  visitorChange: 0,
  revenueChange: 0,
  satisfactionChange: 0,
  roiMonths: 0
})

// 历史政策
const historyPolicies = ref([
  { date: '2025-01-01', policy: '春节门票8折优惠活动', visitorEffect: 25, revenueEffect: 15, status: '已结束' },
  { date: '2025-03-15', policy: '增加10名服务人员', visitorEffect: 5, revenueEffect: 8, status: '进行中' },
  { date: '2025-05-01', policy: '五一营销推广活动', visitorEffect: 35, revenueEffect: 28, status: '已结束' },
  { date: '2025-06-01', policy: '景区设施升级改造', visitorEffect: 12, revenueEffect: 18, status: '进行中' }
])

// 模拟政策效果
const simulatePolicy = async () => {
  const { ticketPriceChange, marketingIncrease, staffIncrease, facilityUpgrade } = policyParams.value

  try {
    // 尝试调用后端政策模拟API
    const { simulatePolicyEffect } = await import('@/api/policy')
    const discount = Math.max(0, 100 - ticketPriceChange)
    const subsidy = marketingIncrease
    const capacity = 5000 + facilityUpgrade * 10

    const res: any = await simulatePolicyEffect(discount, subsidy, capacity)
    if (res.code === 200 && res.data) {
      const data = res.data
      simulationResult.value = {
        visitorChange: data.visitorChange ?? data.flowChange ?? 0,
        revenueChange: data.revenueChange ?? data.incomeChange ?? 0,
        satisfactionChange: data.satisfactionChange ?? 0,
        roiMonths: data.roiMonths ?? 0
      }
      updateSimulationChart()
      return
    }
  } catch (error) {
    console.warn('后端政策模拟API不可用，使用本地算法:', error)
  }

  // 降级：本地模拟算法
  let visitorChange = -ticketPriceChange * 0.8 + marketingIncrease * 0.3 + facilityUpgrade * 0.02
  let revenueChange = ticketPriceChange * 0.5 + marketingIncrease * 0.2 - staffIncrease * 0.1 + facilityUpgrade * 0.03
  let satisfactionChange = -ticketPriceChange * 0.1 + staffIncrease * 0.5 + facilityUpgrade * 0.05

  visitorChange = Math.round(visitorChange)
  revenueChange = Math.round(revenueChange)
  satisfactionChange = Math.round(satisfactionChange)

  const totalInvestment = marketingIncrease * 1000 + staffIncrease * 5000 + facilityUpgrade * 10000
  const monthlyReturn = revenueChange > 0 ? revenueChange * 500 : 100
  const roiMonths = totalInvestment > 0 ? Math.ceil(totalInvestment / monthlyReturn) : 0

  simulationResult.value = {
    visitorChange,
    revenueChange,
    satisfactionChange,
    roiMonths: Math.min(roiMonths, 36)
  }

  updateSimulationChart()
}

// 重置参数
const resetParams = () => {
  policyParams.value = {
    ticketPriceChange: 0,
    marketingIncrease: 0,
    staffIncrease: 0,
    facilityUpgrade: 0
  }
  simulatePolicy()
}

// 更新模拟图表
const updateSimulationChart = () => {
  const chartDom = simulationChartRef.value
  if (!chartDom) return
  
  const myChart = echarts.init(chartDom)
  
  const months = ['当前', '1月后', '2月后', '3月后', '4月后', '5月后', '6月后']
  const { visitorChange, revenueChange } = simulationResult.value
  
  const visitorTrend = months.map((_, i) => 100 + (visitorChange * i / 6))
  const revenueTrend = months.map((_, i) => 100 + (revenueChange * i / 6))
  
  const option = {
    tooltip: { trigger: 'axis' },
    legend: {
      data: ['客流指数', '收入指数'],
      textStyle: { color: '#606266' }
    },
    grid: {
      left: '3%', right: '4%', bottom: '3%', containLabel: true
    },
    xAxis: {
      type: 'category',
      data: months,
      axisLine: { lineStyle: { color: '#E4E7ED' } },
      axisLabel: { color: '#909399' }
    },
    yAxis: {
      type: 'value',
      name: '指数',
      min: 50,
      max: 150,
      axisLine: { lineStyle: { color: '#E4E7ED' } },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#F0F2F5' } }
    },
    series: [
      {
        name: '客流指数',
        type: 'line',
        data: visitorTrend,
        smooth: true,
        itemStyle: { color: '#2A9D8F' }
      },
      {
        name: '收入指数',
        type: 'line',
        data: revenueTrend,
        smooth: true,
        itemStyle: { color: '#9B59B6' }
      }
    ]
  }
  
  myChart.setOption(option)
  window.addEventListener('resize', () => myChart.resize())
}

onMounted(() => {
  initScenicInfo()
  simulatePolicy()
})
</script>

<style scoped>
.policy-sandbox {
  color: #2C3E50;
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

:deep(.el-slider__runway) {
  background-color: #E4E7ED;
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

