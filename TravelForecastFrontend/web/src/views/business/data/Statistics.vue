<template>
  <div class="data-statistics">
    <div class="page-header mb-6">
      <h1 class="text-2xl text-[#2C3E50] mb-2">景区统计报表</h1>
      <p class="text-gray-600">查看【{{ currentScenicName }}】的数据统计报表</p>
    </div>
    
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
      <el-card class="bg-white border border-gray-200 shadow-lg">
        <template #header>
          <div class="card-header flex items-center justify-between">
            <h3 class="text-lg text-[#2A9D8F] font-bold">景区收入统计</h3>
            <el-dropdown @command="changeIncomeRange">
              <el-button type="primary" size="small">
                {{ incomeRangeLabel }} <el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="1m">最近一个月</el-dropdown-item>
                  <el-dropdown-item command="3m">最近三个月</el-dropdown-item>
                  <el-dropdown-item command="6m">最近半年</el-dropdown-item>
                  <el-dropdown-item command="12m">最近一年</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
        <div ref="incomeChartRef" class="h-80"></div>
      </el-card>
      
      <el-card class="bg-white border border-gray-200 shadow-lg">
        <template #header>
          <div class="card-header flex items-center justify-between">
            <h3 class="text-lg text-purple-400 font-bold">游客来源分析</h3>
            <el-dropdown @command="changeSourceRange">
              <el-button type="primary" size="small">
                {{ sourceRangeLabel }} <el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="1m">最近一个月</el-dropdown-item>
                  <el-dropdown-item command="3m">最近三个月</el-dropdown-item>
                  <el-dropdown-item command="6m">最近半年</el-dropdown-item>
                  <el-dropdown-item command="12m">最近一年</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
        <div ref="sourceChartRef" class="h-80"></div>
      </el-card>
    </div>
    
    <el-card class="bg-white border border-gray-200 shadow-lg mb-6">
      <template #header>
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg text-green-400 font-bold">客流趋势对比</h3>
          <div class="flex items-center space-x-2">
            <el-select v-model="selectedYear" placeholder="选择年份" size="small" @change="updateTrendChart">
              <el-option v-for="year in years" :key="year" :label="year" :value="year"></el-option>
            </el-select>
            <el-button type="primary" size="small" @click="exportTrendCSV">导出数据</el-button>
          </div>
        </div>
      </template>
      <div ref="trendChartRef" class="h-96"></div>
    </el-card>
    
    <el-card class="bg-white border border-gray-200 shadow-lg">
      <template #header>
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg text-orange-400 font-bold">详细数据</h3>
          <div class="flex items-center space-x-2">
            <el-input placeholder="搜索..." v-model="searchQuery" size="small" class="w-48" />
            <el-button type="primary" size="small" @click="exportTableCSV">导出表格</el-button>
          </div>
        </div>
      </template>
      <el-table :data="tableData" style="width: 100%" class="bg-transparent">
        <el-table-column prop="date" label="日期" />
        <el-table-column prop="visitors" label="游客数量" />
        <el-table-column prop="ticketIncome" label="门票收入(元)" />
        <el-table-column prop="otherIncome" label="其他收入(元)" />
        <el-table-column prop="totalIncome" label="总收入(元)">
          <template #default="{ row }">
            <span class="text-green-400 font-bold">¥{{ row.totalIncome }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="satisfaction" label="满意度">
          <template #default="scope">
            <el-rate v-model="scope.row.satisfaction" disabled text-color="#ff9900" />
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-center mt-4">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="tableTotal"
          :page-size="pageSize"
          v-model:current-page="currentPage"
          @current-change="loadTableData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getMerchantScenicInfo } from '@/api/merchantScenic'

// 图表引用
const incomeChartRef = ref<HTMLElement | null>(null)
const sourceChartRef = ref<HTMLElement | null>(null)
const trendChartRef = ref<HTMLElement | null>(null)

// 商家景区ID（从API获取）
const merchantScenicId = ref<number>(0)

// 当前景区名称
const currentScenicName = ref('加载中...')

// 时间范围
const selectedYear = ref('2025')
const searchQuery = ref('')
const years = ['2023', '2024', '2025']

const incomeRange = ref<'1m'|'3m'|'6m'|'12m'>('12m')
const sourceRange = ref<'1m'|'3m'|'6m'|'12m'>('12m')
const incomeRangeLabel = ref('最近一年')
const sourceRangeLabel = ref('最近一年')

// 表格数据
const tableData = ref<any[]>([])
const tableTotal = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 从API获取商家对应的景区信息
const loadMerchantScenic = async () => {
  try {
    const res: any = await getMerchantScenicInfo()
    const data = res?.data || res
    
    if (data && data.scenicId) {
      merchantScenicId.value = data.scenicId
      currentScenicName.value = data.scenicName || '我的景区'
      console.log(`商家景区: ${currentScenicName.value} (ID: ${merchantScenicId.value})`)
    } else {
      currentScenicName.value = '未绑定景区'
      ElMessage.warning('您尚未绑定景区，请先在商家资料中绑定景区')
    }
  } catch (error: any) {
    console.error('加载景区信息失败:', error)
    currentScenicName.value = '加载失败'
    ElMessage.error('获取景区信息失败，请刷新重试')
  }
}

// 加载表格数据
const loadTableData = async () => {
  // 生成模拟数据
  const data = []
  const baseDate = new Date()
  for (let i = 0; i < 30; i++) {
    const date = new Date(baseDate)
    date.setDate(date.getDate() - i)
    const visitors = Math.floor(Math.random() * 2000) + 500
    const ticketIncome = visitors * (Math.random() * 30 + 50)
    const otherIncome = visitors * (Math.random() * 20 + 10)
    data.push({
      date: date.toISOString().split('T')[0],
      visitors,
      ticketIncome: ticketIncome.toFixed(2),
      otherIncome: otherIncome.toFixed(2),
      totalIncome: (ticketIncome + otherIncome).toFixed(2),
      satisfaction: Math.floor(Math.random() * 2) + 3.5
    })
  }
  
  // 搜索过滤
  let filtered = data
  if (searchQuery.value) {
    filtered = data.filter(item => item.date.includes(searchQuery.value))
  }
  
  tableTotal.value = filtered.length
  const start = (currentPage.value - 1) * pageSize.value
  tableData.value = filtered.slice(start, start + pageSize.value)
}

onMounted(() => {
  loadMerchantScenic()
  initAllCharts()
  loadTableData()
})

onBeforeUnmount(() => {
  [incomeChartRef, sourceChartRef, trendChartRef].forEach(r => {
    if (r.value) {
      const inst = echarts.getInstanceByDom(r.value)
      if (inst) inst.dispose()
    }
  })
})

const initAllCharts = () => {
  initIncomeChart()
  initSourceChart()
  initTrendChart()
}

// 初始化收入统计图表
const initIncomeChart = () => {
  const chartDom = incomeChartRef.value
  if (!chartDom) return
  
  const myChart = echarts.init(chartDom)
  
  // 生成景区专属数据
  const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
  const ticketIncome = months.map(() => Math.floor(Math.random() * 300) + 200)
  const foodIncome = months.map(() => Math.floor(Math.random() * 150) + 50)
  const shopIncome = months.map(() => Math.floor(Math.random() * 100) + 30)
  const otherIncome = months.map(() => Math.floor(Math.random() * 50) + 20)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['门票收入', '餐饮收入', '购物收入', '其他收入'],
      textStyle: { color: '#606266' }
    },
    grid: {
      left: '3%', right: '4%', bottom: '3%', containLabel: true
    },
    xAxis: [{
      type: 'category',
      data: months,
      axisLine: { lineStyle: { color: '#E4E7ED' } },
      axisLabel: { color: '#909399' }
    }],
    yAxis: [{
      type: 'value',
      name: '收入(万元)',
      nameTextStyle: { color: '#909399' },
      axisLine: { lineStyle: { color: '#E4E7ED' } },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#F0F2F5' } }
    }],
    series: [
      { name: '门票收入', type: 'bar', stack: '收入', data: ticketIncome, itemStyle: { color: '#2A9D8F' } },
      { name: '餐饮收入', type: 'bar', stack: '收入', data: foodIncome, itemStyle: { color: '#9B59B6' } },
      { name: '购物收入', type: 'bar', stack: '收入', data: shopIncome, itemStyle: { color: '#2ECC71' } },
      { name: '其他收入', type: 'bar', stack: '收入', data: otherIncome, itemStyle: { color: '#F39C12' } }
    ]
  }
  
  myChart.setOption(option)
  window.addEventListener('resize', () => myChart.resize())
  ;(window as any).__incomeChart = myChart
}

// 初始化游客来源图表
const initSourceChart = () => {
  const chartDom = sourceChartRef.value
  if (!chartDom) return
  
  const myChart = echarts.init(chartDom)
  
  const sourceData = [
    { value: 35, name: '省内游客' },
    { value: 25, name: '周边省份' },
    { value: 20, name: '东部地区' },
    { value: 12, name: '西部地区' },
    { value: 8, name: '境外游客' }
  ]
  
  const option = {
    tooltip: { trigger: 'item' },
    legend: {
      orient: 'vertical',
      left: 'left',
      textStyle: { color: '#606266' }
    },
    series: [{
      name: '游客来源',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#FFFFFF',
        borderWidth: 2
      },
      label: { show: true, color: '#606266' },
      emphasis: {
        label: { show: true, fontSize: 16, fontWeight: 'bold' }
      },
      labelLine: { show: true },
      data: sourceData
    }]
  }
  
  myChart.setOption(option)
  window.addEventListener('resize', () => myChart.resize())
  ;(window as any).__sourceChart = myChart
}

// 初始化客流趋势图表
const initTrendChart = () => {
  const chartDom = trendChartRef.value
  if (!chartDom) return
  
  const myChart = echarts.init(chartDom)
  
  const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
  const thisYear = months.map(() => Math.floor(Math.random() * 3000) + 1500)
  const lastYear = months.map(() => Math.floor(Math.random() * 2500) + 1200)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: [`${selectedYear.value}年客流`, `${parseInt(selectedYear.value) - 1}年客流`],
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
      name: '游客数量',
      axisLine: { lineStyle: { color: '#E4E7ED' } },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#F0F2F5' } }
    },
    series: [
      {
        name: `${selectedYear.value}年客流`,
        type: 'line',
        smooth: true,
        data: thisYear,
        itemStyle: { color: '#2A9D8F' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(42, 157, 143, 0.3)' },
            { offset: 1, color: 'rgba(42, 157, 143, 0.05)' }
          ])
        }
      },
      {
        name: `${parseInt(selectedYear.value) - 1}年客流`,
        type: 'line',
        smooth: true,
        data: lastYear,
        itemStyle: { color: '#9B59B6' },
        lineStyle: { type: 'dashed' }
      }
    ]
  }
  
  myChart.setOption(option)
  window.addEventListener('resize', () => myChart.resize())
  ;(window as any).__trendChart = myChart
}

// 时间范围切换
const rangeLabelMap: Record<string, string> = { 
  '1m': '最近一个月', '3m': '最近三个月', '6m': '最近半年', '12m': '最近一年' 
}

const changeIncomeRange = (cmd: '1m'|'3m'|'6m'|'12m') => {
  incomeRange.value = cmd
  incomeRangeLabel.value = rangeLabelMap[cmd]
  initIncomeChart()
}

const changeSourceRange = (cmd: '1m'|'3m'|'6m'|'12m') => {
  sourceRange.value = cmd
  sourceRangeLabel.value = rangeLabelMap[cmd]
  initSourceChart()
}

const updateTrendChart = () => {
  initTrendChart()
}

// 导出功能
const exportCSV = (rows: any[], headers: string[], filename: string) => {
  const csv = [headers.join(','), ...rows.map(r => headers.map(h => (r[h] ?? '')).join(','))].join('\n')
  const blob = new Blob(["\ufeff"+csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  link.click()
  URL.revokeObjectURL(link.href)
}

const exportTrendCSV = () => {
  const months = ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月']
  const rows = months.map((m, idx) => ({
    '月份': m,
    '今年客流': Math.floor(Math.random() * 3000) + 1500,
    '去年客流': Math.floor(Math.random() * 2500) + 1200
  }))
  exportCSV(rows, ['月份', '今年客流', '去年客流'], `${currentScenicName.value}_客流趋势_${selectedYear.value}.csv`)
  ElMessage.success('导出成功')
}

const exportTableCSV = () => {
  exportCSV(tableData.value, ['date','visitors','ticketIncome','otherIncome','totalIncome','satisfaction'], 
    `${currentScenicName.value}_详细数据.csv`)
  ElMessage.success('导出成功')
}
</script>

<style scoped>
.data-statistics {
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

:deep(.el-table) {
  background-color: #FFFFFF !important;
}

:deep(.el-table tr), :deep(.el-table th) {
  background-color: #FFFFFF !important;
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) {
  background-color: #F5F7FA !important;
}

:deep(.el-table td), :deep(.el-table th) {
  border-bottom: 1px solid #EBEEF5;
}

:deep(.el-table .cell) {
  color: #606266;
}

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: #606266;
  --el-pagination-button-color: #606266;
}
</style>

