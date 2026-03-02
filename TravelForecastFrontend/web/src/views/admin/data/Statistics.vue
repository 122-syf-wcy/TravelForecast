<template>
  <div class="data-statistics">
    <div class="page-header">
      <h1 class="text-2xl text-gray-800 mb-4">统计报表</h1>
      <p class="text-gray-500">查看六盘水旅游数据统计报表</p>
    </div>
    
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
      <holographic-card>
        <template #header>
          <div class="card-header flex items-center justify-between">
            <h3 class="text-lg text-[#2A9D8F] font-bold">旅游收入统计</h3>
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
      </holographic-card>
      
      <holographic-card>
        <template #header>
          <div class="card-header flex items-center justify-between">
            <h3 class="text-lg text-[#457B9D] font-bold">游客来源分析</h3>
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
      </holographic-card>
    </div>
    
    <holographic-card class="mb-6">
      <template #header>
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg text-green-400 font-bold">景区客流对比</h3>
          <div class="flex items-center space-x-2">
            <el-select v-model="selectedYear" placeholder="选择年份" size="small" @change="updateComparisonChart">
              <el-option v-for="year in years" :key="year" :label="year" :value="year"></el-option>
            </el-select>
            <el-button type="primary" size="small" @click="exportComparisonCSV">导出数据</el-button>
          </div>
        </div>
      </template>
      <div ref="comparisonChartRef" class="h-96"></div>
    </holographic-card>
    
    <holographic-card>
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
        <el-table-column prop="scenic" label="景区" />
        <el-table-column prop="visitors" label="游客数量" />
        <el-table-column prop="income" label="收入(万元)" />
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
    </holographic-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import HolographicCard from '@/components/HolographicCard.vue'
import { getTourismIncome, getVisitorSource, getScenicComparison, getDetailedData } from '@/api/statistics'

const incomeChartRef = ref<HTMLElement | null>(null)
const sourceChartRef = ref<HTMLElement | null>(null)
const comparisonChartRef = ref<HTMLElement | null>(null)
const selectedYear = ref('2025')
const searchQuery = ref('')
const years = ['2022', '2023', '2024', '2025']
const comparisonData = ref<any>(null)

// 下拉切换标签
const incomeRange = ref<'1m'|'3m'|'6m'|'12m'>('12m')
const sourceRange = ref<'1m'|'3m'|'6m'|'12m'>('12m')
const incomeRangeLabel = ref('最近一年')
const sourceRangeLabel = ref('最近一年')

// 表格数据
const tableData = ref<any[]>([])
const tableTotal = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 加载表格数据
const loadTableData = async () => {
  try {
    const res: any = await getDetailedData(currentPage.value, pageSize.value, searchQuery.value)
    if (res.code === 200) {
      tableData.value = res.data.data || []
      tableTotal.value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载表格数据失败:', error)
  }
}

onMounted(() => {
  initIncomeChart()
  initSourceChart()
  initComparisonChart()
})

onBeforeUnmount(() => {
  [incomeChartRef, sourceChartRef, comparisonChartRef].forEach(r => {
    if (r.value) {
      const inst = echarts.getInstanceByDom(r.value)
      if (inst) inst.dispose()
    }
  })
})

// 初始化收入统计图表
const initIncomeChart = async () => {
  const chartDom = incomeChartRef.value
  if (!chartDom) return
  
  const myChart = echarts.init(chartDom)
  
  // 从API加载数据
  try {
    const res: any = await getTourismIncome(incomeRange.value)
    if (res.code === 200) {
      const data = res.data
      
      const option = {
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
          backgroundColor: 'rgba(255, 255, 255, 0.98)',
          textStyle: { color: '#303133' },
          borderColor: '#2A9D8F'
        },
        legend: {
          data: ['门票收入', '餐饮收入', '住宿收入', '购物收入', '其他收入'],
          textStyle: { color: '#909399' },
          bottom: 0
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '10%',
          top: '10%',
          containLabel: true
        },
        xAxis: [{
          type: 'category',
          data: data.months || [],
          axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.04)' } },
          axisLabel: { color: '#909399' }
        }],
        yAxis: [{
          type: 'value',
          name: '收入(万元)',
          nameTextStyle: { color: '#909399' },
          axisLine: { show: true, lineStyle: { color: 'rgba(0, 0, 0, 0.04)' } },
          axisLabel: { color: '#909399' },
          splitLine: { lineStyle: { color: 'rgba(42, 157, 143, 0.03)', type: 'dashed' } }
        }],
        series: [
          {
            name: '门票收入',
            type: 'bar',
            stack: '收入',
            itemStyle: { color: '#2A9D8F' },
            data: data.ticketIncome || []
          },
          {
            name: '餐饮收入',
            type: 'bar',
            stack: '收入',
            itemStyle: { color: '#457B9D' },
            data: data.foodIncome || []
          },
          {
            name: '住宿收入',
            type: 'bar',
            stack: '收入',
            itemStyle: { color: '#FF6B35' },
            data: data.accommodationIncome || []
          },
          {
            name: '购物收入',
            type: 'bar',
            stack: '收入',
            itemStyle: { color: '#FFD700' },
            data: data.shoppingIncome || []
          },
          {
            name: '其他收入',
            type: 'bar',
            stack: '收入',
            itemStyle: { color: '#E76F51', borderRadius: [4, 4, 0, 0] },
            data: data.otherIncome || []
          }
        ]
      }
      
      myChart.setOption(option)
    }
  } catch (error) {
    console.error('加载收入统计数据失败:', error)
    ElMessage.error('加载收入统计数据失败')
  }
  
  window.addEventListener('resize', () => myChart.resize())
  ;(window as any).__incomeChart = myChart
}

// 初始化游客来源图表
const initSourceChart = async () => {
  const chartDom = sourceChartRef.value
  if (!chartDom) return
  
  const myChart = echarts.init(chartDom)
  
  // 从API加载数据
  try {
    const res: any = await getVisitorSource(sourceRange.value)
    if (res.code === 200) {
      const sourceData = res.data.data || []
      
      const option = {
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)',
          backgroundColor: 'rgba(255, 255, 255, 0.98)',
          textStyle: { color: '#303133' },
          borderColor: '#2A9D8F'
        },
        legend: {
          orient: 'vertical',
          left: 'left',
          textStyle: { color: '#909399' }
        },
        series: [
          {
            name: '游客来源',
            type: 'pie',
            radius: ['40%', '70%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 10,
              borderColor: '#FFFFFF',
              borderWidth: 2
            },
            label: {
              show: false,
              position: 'center'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: '20',
                fontWeight: 'bold',
                color: '#303133'
              },
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(42, 157, 143, 0.5)'
              }
            },
            labelLine: { show: false },
            data: sourceData,
            color: ['#2A9D8F', '#457B9D', '#FF6B35', '#FFD700', '#E76F51', '#909399']
          }
        ]
      }
      
      myChart.setOption(option)
    }
  } catch (error) {
    console.error('加载游客来源数据失败:', error)
    ElMessage.error('加载游客来源数据失败')
  }
  
  window.addEventListener('resize', () => myChart.resize())
  ;(window as any).__sourceChart = myChart
}

// 初始化景区对比图表
const initComparisonChart = async () => {
  const chartDom = comparisonChartRef.value
  if (!chartDom) return
  
  const myChart = echarts.init(chartDom)
  
  // 从API加载数据
  try {
    const res: any = await getScenicComparison(selectedYear.value)
    if (res.code === 200) {
      comparisonData.value = res.data // 保存数据供导出使用
      const data = res.data
      const scenicNames = data.scenicNames || []
      const scenicVisitors = data.scenicVisitors || {}
      const scenicSatisfaction = data.scenicSatisfaction || {}
      const months = data.months || []
      
      // 构建series
      const series: any[] = []
      
      // 添加各景区的柱状图
      scenicNames.forEach((name: string) => {
        series.push({
          name: name,
          type: 'bar',
          data: scenicVisitors[name] || []
        })
      })
      
      // 计算平均满意度
      const avgSatisfaction = months.map((_, idx) => {
        let sum = 0
        let count = 0
        scenicNames.forEach((name: string) => {
          if (scenicSatisfaction[name] && scenicSatisfaction[name][idx] !== undefined) {
            sum += Number(scenicSatisfaction[name][idx])
            count++
          }
        })
        return count > 0 ? (sum / count).toFixed(1) : 4.5
      })
      
      // 添加平均满意度折线图
      series.push({
        name: '平均满意度',
        type: 'line',
        yAxisIndex: 1,
        data: avgSatisfaction
      })
      
      const option = {
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'cross', crossStyle: { color: '#909399' } },
          backgroundColor: 'rgba(255, 255, 255, 0.98)',
          textStyle: { color: '#303133' },
          borderColor: '#2A9D8F'
        },
        legend: {
          data: [...scenicNames, '平均满意度'],
          textStyle: { color: '#909399' }
        },
        xAxis: [{
          type: 'category',
          data: months,
          axisPointer: { type: 'shadow' },
          axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.04)' } },
          axisLabel: { color: '#909399' }
        }],
        yAxis: [
          {
            type: 'value',
            name: '游客数量',
            axisLine: { show: true, lineStyle: { color: 'rgba(0, 0, 0, 0.04)' } },
            axisLabel: { formatter: '{value}', color: '#909399' },
            splitLine: { lineStyle: { color: 'rgba(42, 157, 143, 0.03)', type: 'dashed' } }
          },
          {
            type: 'value',
            name: '满意度',
            min: 3,
            max: 5,
            interval: 0.5,
            axisLine: { show: true, lineStyle: { color: 'rgba(0, 0, 0, 0.04)' } },
            axisLabel: { formatter: '{value}', color: '#909399' },
            splitLine: { show: false }
          }
        ],
        series: series.map(s => {
          if (s.type === 'bar') {
            return {
              ...s,
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: '#2A9D8F' },
                  { offset: 1, color: 'rgba(42, 157, 143, 0.1)' }
                ]),
                borderRadius: [4, 4, 0, 0]
              }
            }
          } else {
            return {
              ...s,
              itemStyle: { color: '#FFD700' },
              lineStyle: { color: '#FFD700', width: 3, shadowColor: 'rgba(255, 215, 0, 0.5)', shadowBlur: 10 }
            }
          }
        })
      }
      
      myChart.setOption(option)
    }
  } catch (error) {
    console.error('加载景区对比数据失败:', error)
    ElMessage.error('加载景区对比数据失败')
  }
  
  window.addEventListener('resize', () => myChart.resize())
  ;(window as any).__comparisonChart = myChart
}

// 交互：切换时间范围
const rangeLabelMap: Record<string, string> = { '1m': '最近一个月', '3m': '最近三个月', '6m': '最近半年', '12m': '最近一年' }

const changeIncomeRange = async (cmd: '1m'|'3m'|'6m'|'12m') => {
  incomeRange.value = cmd
  incomeRangeLabel.value = rangeLabelMap[cmd]
  const chart = (window as any).__incomeChart
  if (!chart) return
  
  try {
    const res: any = await getTourismIncome(cmd)
    if (res.code === 200) {
      const data = res.data
      chart.setOption({
        xAxis: [{ data: data.months || [] }],
        series: [
          { data: data.ticketIncome || [] },
          { data: data.foodIncome || [] },
          { data: data.accommodationIncome || [] },
          { data: data.shoppingIncome || [] },
          { data: data.otherIncome || [] }
        ]
      })
    }
  } catch (error) {
    console.error('切换收入统计时间范围失败:', error)
  }
}

const changeSourceRange = async (cmd: '1m'|'3m'|'6m'|'12m') => {
  sourceRange.value = cmd
  sourceRangeLabel.value = rangeLabelMap[cmd]
  const chart = (window as any).__sourceChart
  if (!chart) return
  
  try {
    const res: any = await getVisitorSource(cmd)
    if (res.code === 200) {
      const sourceData = res.data.data || []
      chart.setOption({
        series: [{ data: sourceData }]
      })
    }
  } catch (error) {
    console.error('切换游客来源时间范围失败:', error)
  }
}

// 年份切换-重绘对比图
const updateComparisonChart = async () => {
  const chart = (window as any).__comparisonChart
  if (!chart) return
  
  try {
    const res: any = await getScenicComparison(selectedYear.value)
    if (res.code === 200) {
      comparisonData.value = res.data // 保存数据供导出
      const data = res.data
      const scenicNames = data.scenicNames || []
      const scenicVisitors = data.scenicVisitors || {}
      const scenicSatisfaction = data.scenicSatisfaction || {}
      const months = data.months || []
      
      // 构建series
      const series: any[] = []
      
      // 添加各景区的柱状图
      scenicNames.forEach((name: string) => {
        series.push({
          data: scenicVisitors[name] || []
        })
      })
      
      // 计算平均满意度
      const avgSatisfaction = months.map((_, idx) => {
        let sum = 0
        let count = 0
        scenicNames.forEach((name: string) => {
          if (scenicSatisfaction[name] && scenicSatisfaction[name][idx] !== undefined) {
            sum += Number(scenicSatisfaction[name][idx])
            count++
          }
        })
        return count > 0 ? (sum / count).toFixed(1) : 4.5
      })
      
      series.push({
        data: avgSatisfaction
      })
      
      chart.setOption({
        xAxis: [{ data: months }],
        series: series
      })
    }
  } catch (error) {
    console.error('切换景区对比年份失败:', error)
  }
}

// 导出功能（CSV）
const exportCSV = (rows: any[], headers: string[], filename: string) => {
  const csv = [headers.join(','), ...rows.map(r => headers.map(h => (r[h] ?? '')).join(','))].join('\n')
  const blob = new Blob(["\ufeff"+csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  link.click()
  URL.revokeObjectURL(link.href)
}

const exportComparisonCSV = () => {
  if (!comparisonData.value) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  
  const data = comparisonData.value
  const scenicNames = data.scenicNames || []
  const months = data.months || []
  const scenicVisitors = data.scenicVisitors || {}
  const scenicSatisfaction = data.scenicSatisfaction || {}
  
  const headers = ['月份', ...scenicNames, '平均满意度']
  
  const rows = months.map((m: string, idx: number) => {
    const row: any = { '月份': m }
    
    // 添加各景区游客数
    scenicNames.forEach((name: string) => {
      row[name] = (scenicVisitors[name] && scenicVisitors[name][idx]) || 0
    })
    
    // 计算平均满意度
    let sum = 0
    let count = 0
    scenicNames.forEach((name: string) => {
      if (scenicSatisfaction[name] && scenicSatisfaction[name][idx] !== undefined) {
        sum += Number(scenicSatisfaction[name][idx])
        count++
      }
    })
    row['平均满意度'] = count > 0 ? (sum / count).toFixed(1) : '-'
    
    return row
  })
  
  exportCSV(rows, headers, `景区客流对比_${selectedYear.value}.csv`)
}

const exportTableCSV = () => {
  exportCSV(tableData.value, ['date','scenic','visitors','income','satisfaction'], '统计详细数据.csv')
}
</script>

<style scoped>
.data-statistics {
  color: #2C3E50;
}

:deep(.el-table) {
  background-color: transparent !important;
}

:deep(.el-table tr), :deep(.el-table th) {
  background-color: transparent !important;
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) {
  background-color: rgba(42, 157, 143, 0.1) !important;
}

:deep(.el-table td), :deep(.el-table th) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

:deep(.el-table .cell) {
  color: #303133;
}

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: #606266;
  --el-pagination-button-color: #606266;
  --el-pagination-button-bg-color: transparent;
  --el-pagination-button-disabled-color: #909399;
  --el-pagination-button-disabled-bg-color: transparent;
  --el-pagination-hover-color: var(--el-color-primary);
}
</style>