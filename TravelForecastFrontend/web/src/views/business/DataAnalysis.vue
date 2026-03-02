<template>
  <div class="data-analysis-container">
    <h1 class="text-2xl font-bold text-[#2C3E50] mb-6">数据分析</h1>
    
    <!-- 时间范围选择 -->
    <div class="flex justify-between items-center mb-6">
      <div class="flex items-center">
        <el-radio-group v-model="timeRange" size="small" class="mr-4">
          <el-radio-button label="week">最近一周</el-radio-button>
          <el-radio-button label="month">最近一月</el-radio-button>
          <el-radio-button label="quarter">最近三月</el-radio-button>
        </el-radio-group>
        
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="small"
        />
      </div>
      
      <el-button type="primary" :icon="Download" size="small" @click="handleExport">
        导出报告
      </el-button>
    </div>
    
    <!-- 数据概览卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
      <stat-card 
        title="总游客量" 
        :value="analysisData.totalVisitors" 
        suffix="人次" 
        :change="analysisData.visitorChange" 
        icon="User" 
        color="bg-gradient-to-r from-blue-500 to-cyan-400"
      />
      
      <stat-card 
        title="总收入" 
        :value="analysisData.totalRevenue" 
        suffix="元" 
        :change="analysisData.revenueChange" 
        icon="Money" 
        color="bg-gradient-to-r from-green-500 to-teal-400"
      />
      
      <stat-card 
        title="平均停留时间" 
        :value="analysisData.avgStayTime" 
        suffix="小时" 
        :change="analysisData.stayTimeChange" 
        icon="Timer" 
        color="bg-gradient-to-r from-purple-500 to-indigo-400"
      />
      
      <stat-card 
        title="回游率" 
        :value="analysisData.returnRate" 
        suffix="%" 
        :change="analysisData.returnRateChange" 
        icon="RefreshRight" 
        color="bg-gradient-to-r from-pink-500 to-rose-400"
      />
    </div>
    
    <!-- 游客量趋势 -->
    <holographic-card class="mb-6">
      <template #header>
        <div class="flex justify-between items-center p-4">
          <h2 class="text-xl font-bold text-[#2A9D8F]">游客量趋势</h2>
          <div>
            <el-radio-group v-model="visitorMetric" size="small">
              <el-radio-button label="daily">日</el-radio-button>
              <el-radio-button label="weekly">周</el-radio-button>
              <el-radio-button label="monthly">月</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>
      <div class="p-4 h-80">
        <div ref="visitorTrendChartRef" class="w-full h-full"></div>
      </div>
    </holographic-card>
    
    <!-- 收入分析 -->
    <holographic-card class="mb-6">
      <template #header>
        <div class="flex justify-between items-center p-4">
          <h2 class="text-xl font-bold text-[#2A9D8F]">收入分析</h2>
          <div>
            <el-radio-group v-model="revenueType" size="small">
              <el-radio-button label="all">全部</el-radio-button>
              <el-radio-button label="ticket">门票</el-radio-button>
              <el-radio-button label="food">餐饮</el-radio-button>
              <el-radio-button label="souvenir">纪念品</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>
      <div class="p-4 h-80">
        <div ref="revenueChartRef" class="w-full h-full"></div>
      </div>
    </holographic-card>
    
    <!-- 游客来源与年龄分布 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
      <holographic-card>
        <template #header>
          <div class="flex justify-between items-center p-4">
            <h2 class="text-xl font-bold text-[#2A9D8F]">游客来源</h2>
          </div>
        </template>
        <div class="p-4 h-80">
          <div ref="visitorSourceChartRef" class="w-full h-full"></div>
        </div>
      </holographic-card>
      
      <holographic-card>
        <template #header>
          <div class="flex justify-between items-center p-4">
            <h2 class="text-xl font-bold text-[#2A9D8F]">年龄分布</h2>
          </div>
        </template>
        <div class="p-4 h-80">
          <div ref="ageDistributionChartRef" class="w-full h-full"></div>
        </div>
      </holographic-card>
    </div>
    
    <!-- 景点分析 -->
    <holographic-card class="mb-6">
      <template #header>
        <div class="flex justify-between items-center p-4">
          <h2 class="text-xl font-bold text-[#2A9D8F]">景点分析</h2>
        </div>
      </template>
      <div class="p-4">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div v-for="(spot, index) in spotData" :key="index" class="bg-white border border-gray-100 shadow-sm rounded-lg p-4">
            <div class="flex items-center mb-4">
              <el-image 
                :src="spot.image" 
                class="w-16 h-16 rounded-lg object-cover mr-3"
              />
              <div>
                <h3 class="text-gray-800 font-medium">{{ spot.name }}</h3>
                <div class="flex items-center mt-1">
                  <span class="text-sm text-gray-500 mr-2">满意度: {{ spot.satisfaction }}</span>
                  <el-rate v-model="spot.satisfaction" disabled text-color="#F9F9F9" />
                </div>
              </div>
            </div>
            
            <div class="grid grid-cols-2 gap-4">
              <div>
                <div class="text-xs text-gray-500">游客量</div>
                <div class="text-gray-800 font-medium">{{ spot.visitors.toLocaleString() }}人</div>
                <div class="text-xs" :class="spot.trend >= 0 ? 'text-green-400' : 'text-red-400'">
                  <span v-if="spot.trend >= 0">↑</span>
                  <span v-else>↓</span>
                  {{ Math.abs(spot.trend) }}%
                </div>
              </div>
              
              <div>
                <div class="text-xs text-gray-500">平均停留</div>
                <div class="text-gray-800 font-medium">{{ spot.avgStayTime }}</div>
              </div>
              
              <div>
                <div class="text-xs text-gray-500">高峰时段</div>
                <div class="text-gray-800 font-medium">{{ spot.peakHours }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </holographic-card>
    
    <!-- 游客满意度分析 -->
    <holographic-card>
      <template #header>
        <div class="flex justify-between items-center p-4">
          <h2 class="text-xl font-bold text-[#2A9D8F]">游客满意度分析</h2>
        </div>
      </template>
      <div class="p-4 h-80">
        <div ref="satisfactionChartRef" class="w-full h-full"></div>
      </div>
    </holographic-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch, computed } from 'vue'
import { ArrowUp, ArrowDown, Download, User, Money, Timer, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import HolographicCard from '@/components/HolographicCard.vue'
import StatCard from '@/components/StatCard.vue'
import * as echarts from 'echarts'
import {
  getAnalysisOverview,
  getVisitorTrend,
  getRevenueAnalysis,
  getVisitorSource,
  getAgeDistribution,
  getSatisfactionAnalysis,
  getSpotRanking
} from '@/api/merchant-analysis'

// 商家景区ID（从localStorage获取）
const merchantScenicId = computed(() => {
  try {
    const userInfo = localStorage.getItem('userInfo')
    if (userInfo) {
      const user = JSON.parse(userInfo)
      if (user.scenicId) {
        return user.scenicId
      }
    }
  } catch (error) {
    console.warn('获取用户信息失败:', error)
  }
  return 1 // 默认梅花山
})

// 时间范围
const timeRange = ref('month')
const dateRange = ref([
  new Date(new Date().setMonth(new Date().getMonth() - 1)),
  new Date()
])

// 辅助函数：根据范围获取日期
const getDateRange = (range: string) => {
  const now = new Date()
  const endDate = now.toISOString().split('T')[0]
  let startDate = ''
  
  if (range === 'week') {
    const weekAgo = new Date(now)
    weekAgo.setDate(weekAgo.getDate() - 7)
    startDate = weekAgo.toISOString().split('T')[0]
  } else if (range === 'month') {
    const monthAgo = new Date(now)
    monthAgo.setMonth(monthAgo.getMonth() - 1)
    startDate = monthAgo.toISOString().split('T')[0]
  } else if (range === 'quarter') {
    const quarterAgo = new Date(now)
    quarterAgo.setMonth(quarterAgo.getMonth() - 3)
    startDate = quarterAgo.toISOString().split('T')[0]
  }
  
  return { startDate, endDate }
}

// 更新分析数据（调用真实API）
const updateAnalysisData = async (range: string) => {
  try {
    const { startDate, endDate } = getDateRange(range)
    console.log('加载数据概览:', merchantScenicId.value, startDate, endDate)
    
    const res: any = await getAnalysisOverview(
      merchantScenicId.value,
      startDate,
      endDate
    )
    
    if (res.code === 200 && res.data) {
      analysisData.value = res.data
      console.log('数据概览加载成功:', res.data)
    }
  } catch (error) {
    console.error('加载数据概览失败', error)
    ElMessage.error('加载数据概览失败')
  }
}

// 监听时间范围变化
watch(timeRange, async (newValue) => {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  
  if (newValue === 'week') {
    const weekAgo = new Date(today)
    weekAgo.setDate(weekAgo.getDate() - 7)
    dateRange.value = [weekAgo, today]
  } else if (newValue === 'month') {
    const monthAgo = new Date(today)
    monthAgo.setMonth(monthAgo.getMonth() - 1)
    dateRange.value = [monthAgo, today]
  } else if (newValue === 'quarter') {
    const quarterAgo = new Date(today)
    quarterAgo.setMonth(quarterAgo.getMonth() - 3)
    dateRange.value = [quarterAgo, today]
  }
  
  // 重新加载所有数据
  await updateAnalysisData(newValue)
  await loadSpotData()
  
  nextTick(() => {
    initVisitorTrendChart()
    initRevenueAnalysisChart()
    initVisitorSourceChart()
    initAgeDistributionChart()
    initSatisfactionChart()
  })
  
  ElMessage.info(`已切换到${newValue === 'week' ? '最近一周' : newValue === 'month' ? '最近一月' : '最近三月'}`)
})

// 监听日期范围选择器变化
watch(dateRange, (newValue) => {
  if (newValue && newValue[0] && newValue[1]) {
    const startDate = newValue[0].toISOString().split('T')[0]
    const endDate = newValue[1].toISOString().split('T')[0]
    console.log('自定义日期范围:', startDate, endDate)
    loadDataWithCustomRange(startDate, endDate)
  }
})

// 自定义日期范围加载
const loadDataWithCustomRange = async (startDate: string, endDate: string) => {
  try {
    const res: any = await getAnalysisOverview(merchantScenicId.value, startDate, endDate)
    if (res.code === 200 && res.data) {
      analysisData.value = res.data
    }
    
    await loadSpotData()
    
    nextTick(() => {
      initVisitorTrendChart()
      initRevenueAnalysisChart()
      initVisitorSourceChart()
      initAgeDistributionChart()
      initSatisfactionChart()
    })
  } catch (error) {
    console.error('加载自定义范围数据失败', error)
    ElMessage.error('加载失败')
  }
}

// 导出报告
const handleExport = () => {
  const startDate = dateRange.value[0]?.toLocaleDateString() || '开始日期'
  const endDate = dateRange.value[1]?.toLocaleDateString() || '结束日期'
  
  try {
    // 准备导出数据
    const exportData = [
      ['数据分析报告'],
      [`时间范围: ${startDate} 至 ${endDate}`],
      [''],
      ['数据概览'],
      ['指标', '数值', '变化'],
      ['总游客量', `${analysisData.value.totalVisitors}人次`, `${analysisData.value.visitorChange > 0 ? '+' : ''}${analysisData.value.visitorChange}%`],
      ['总收入', `${analysisData.value.totalRevenue}元`, `${analysisData.value.revenueChange > 0 ? '+' : ''}${analysisData.value.revenueChange}%`],
      ['平均停留时间', `${analysisData.value.avgStayTime}小时`, `${analysisData.value.stayTimeChange > 0 ? '+' : ''}${analysisData.value.stayTimeChange}%`],
      ['回游率', `${analysisData.value.returnRate}%`, `${analysisData.value.returnRateChange > 0 ? '+' : ''}${analysisData.value.returnRateChange}%`],
      [''],
      ['景点分析'],
      ['景区名称', '游客数', '平均停留时间', '高峰时段', '满意度', '趋势'],
      ...spotData.value.map(spot => [
        spot.name,
        spot.visitors,
        spot.avgStayTime,
        spot.peakHours,
        spot.satisfaction,
        `${spot.trend > 0 ? '+' : ''}${spot.trend}%`
      ])
    ]
    
    // 转换为CSV格式
    const csvContent = exportData.map(row => row.join(',')).join('\n')
    
    // 添加BOM以支持中文
    const BOM = '\uFEFF'
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
    
    // 创建下载链接
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)
    link.setAttribute('href', url)
    link.setAttribute('download', `数据分析报告_${startDate.replace(/\//g, '-')}_${endDate.replace(/\//g, '-')}.csv`)
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    ElMessage.success('报告导出成功！')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请重试')
  }
}

// 分析指标
const visitorMetric = ref('daily')
const revenueType = ref('all')

// 图表引用
const visitorTrendChartRef = ref(null)
const revenueChartRef = ref(null)
const visitorSourceChartRef = ref(null)
const ageDistributionChartRef = ref(null)
const satisfactionChartRef = ref(null)

let visitorTrendChart: any = null
let revenueChart: any = null
let visitorSourceChart: any = null
let ageDistributionChart: any = null
let satisfactionChart: any = null

// 分析数据
const analysisData = ref({
  totalVisitors: 45628,
  visitorChange: 12.5,
  totalRevenue: 2356800,
  revenueChange: 8.3,
  avgStayTime: 3.2,
  stayTimeChange: 5.7,
  returnRate: 35.8,
  returnRateChange: 2.4
})

// 景点数据
const spotData = ref<any[]>([])

// 加载景点数据
const loadSpotData = async () => {
  try {
    const { startDate, endDate } = getDateRange(timeRange.value)
    const res: any = await getSpotRanking(merchantScenicId.value, startDate, endDate)
    
    if (res.code === 200 && res.data && res.data.spots) {
      spotData.value = res.data.spots
      console.log('景点数据加载成功:', spotData.value)
    }
  } catch (error) {
    console.error('加载景点数据失败', error)
  }
}

onMounted(async () => {
  // 首次加载数据
  await updateAnalysisData(timeRange.value)
  await loadSpotData()
  
  setTimeout(() => {
    initVisitorTrendChart()
    initRevenueAnalysisChart()
    initVisitorSourceChart()
    initAgeDistributionChart()
    initSatisfactionChart()
  }, 500)
})

// 监听指标变化
watch(visitorMetric, (newValue) => {
  console.log('游客量指标切换:', newValue)
  if (visitorTrendChart) {
    visitorTrendChart.dispose()
    visitorTrendChart = null
  }
  nextTick(() => {
    initVisitorTrendChart()
  })
})

watch(revenueType, (newValue) => {
  console.log('收入类型切换:', newValue)
  if (revenueChart) {
    revenueChart.dispose()
    revenueChart = null
  }
  nextTick(() => {
    initRevenueAnalysisChart()
  })
})

// 初始化游客量趋势图表（调用真实API）
const initVisitorTrendChart = async () => {
  if (!visitorTrendChartRef.value) return
  
  try {
    const { startDate, endDate } = getDateRange(timeRange.value)
    const res: any = await getVisitorTrend(
      merchantScenicId.value,
      startDate,
      endDate,
      visitorMetric.value
    )
    
    if (res.code === 200 && res.data) {
      const data = res.data
      
      // 初始化或获取图表实例
      if (!visitorTrendChart) {
        visitorTrendChart = echarts.init(visitorTrendChartRef.value)
      }
      
      const xAxisData = data.labels || []
      const visitorData = data.data || []
    
    const option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        },
        formatter: function(params: any[]) {
          return `${params[0].axisValue}<br/>${params[0].marker}游客量: ${params[0].value}人次`;
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: [
        {
          type: 'category',
          data: xAxisData,
          axisLabel: {
            color: '#909399',
            rotate: visitorMetric.value === 'monthly' ? 45 : 0,
            fontSize: 11
          },
          axisLine: {
            lineStyle: {
              color: '#E4E7ED'
            }
          }
        }
      ],
      yAxis: [
        {
          type: 'value',
          name: '游客量',
          axisLabel: {
            color: '#909399',
            formatter: '{value}人'
          },
          axisLine: {
            lineStyle: {
              color: '#E4E7ED'
            }
          },
          splitLine: {
            lineStyle: {
              color: '#F0F2F5'
            }
          }
        }
      ],
      series: [
        {
          name: '游客量',
          type: 'bar',
          barWidth: '60%',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#2A9D8F' },
              { offset: 1, color: '#457B9D' }
            ])
          },
          data: visitorData
        }
      ]
    }
    
      visitorTrendChart.setOption(option)
      console.log('游客量趋势图表加载成功')
    }
  } catch (error) {
    console.error('初始化游客量趋势图表失败', error)
  }
}

// 初始化收入分析图表（调用真实API）
const initRevenueAnalysisChart = async () => {
  if (!revenueChartRef.value) return
  
  try {
    const { startDate, endDate } = getDateRange(timeRange.value)
    const res: any = await getRevenueAnalysis(
      merchantScenicId.value,
      startDate,
      endDate,
      revenueType.value
    )
    
    if (res.code === 200 && res.data) {
      const data = res.data
      
      if (!revenueChart) {
        revenueChart = echarts.init(revenueChartRef.value)
      }
      
      const months = data.labels || []
      
      // 根据类型显示不同的系列
      let seriesData = []
      
      if (revenueType.value === 'all') {
        // 全部收入显示为堆叠区域图
        const totalData = data.data || []
        seriesData.push({
          name: '总收入',
          type: 'line',
          smooth: true,
          lineStyle: { width: 3, color: '#2A9D8F' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(42, 157, 143, 0.3)' },
              { offset: 1, color: 'rgba(42, 157, 143, 0.1)' }
            ])
          },
          data: totalData
        })
      } else {
        // 单一类型显示
        const typeData = data.data || []
        const typeName = revenueType.value === 'ticket' ? '门票收入' : 
                        revenueType.value === 'food' ? '餐饮收入' : '纪念品收入'
        const color = revenueType.value === 'ticket' ? '#2A9D8F' :
                     revenueType.value === 'food' ? '#457B9D' : '#E76F51'
        
        seriesData.push({
          name: typeName,
          type: 'line',
          smooth: true,
          lineStyle: { width: 3, color },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: `${color}4D` },
              { offset: 1, color: `${color}1A` }
            ])
          },
          data: typeData
        })
      }
    
    const option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross',
          label: {
            backgroundColor: '#6a7985'
          }
        },
        formatter: function(params: any[]) {
          let result = `${params[0].axisValue}<br/>`;
          let total = 0;
          
          params.forEach(param => {
            result += `${param.marker} ${param.seriesName}: ${param.value.toLocaleString()}元<br/>`;
            total += param.value;
          });
          
          if (params.length > 1) {
            result += `<br/><b>总收入: ${total.toLocaleString()}元</b>`;
          }
          
          return result;
        }
      },
      legend: {
        data: seriesData.map(item => item.name),
        textStyle: {
          color: '#606266'
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: [
        {
          type: 'category',
          boundaryGap: false,
          data: months,
          axisLine: {
            lineStyle: {
              color: '#E4E7ED'
            }
          },
          axisLabel: {
            color: '#909399'
          }
        }
      ],
      yAxis: [
        {
          type: 'value',
          axisLabel: {
            color: '#909399',
            formatter: '{value}元'
          },
          axisLine: {
            lineStyle: {
              color: '#E4E7ED'
            }
          },
          splitLine: {
            lineStyle: {
              color: '#F0F2F5'
            }
          }
        }
      ],
      series: seriesData
    }
    
      revenueChart.setOption(option)
      console.log('收入分析图表加载成功')
    }
  } catch (error) {
    console.error('初始化收入分析图表失败', error)
  }
}

// 初始化游客来源图表（调用真实API）
const initVisitorSourceChart = async () => {
  if (!visitorSourceChartRef.value) return
  
  try {
    const { startDate, endDate } = getDateRange(timeRange.value)
    const res: any = await getVisitorSource(
      merchantScenicId.value,
      startDate,
      endDate
    )
    
    if (res.code === 200 && res.data) {
      if (!visitorSourceChart) {
        visitorSourceChart = echarts.init(visitorSourceChartRef.value)
      }
      
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c}人 ({d}%)'
        },
        legend: {
          orient: 'vertical',
          right: 10,
          top: 'center',
          textStyle: { color: '#606266' }
        },
        series: [{
          name: '游客来源',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#FFFFFF',
            borderWidth: 2
          },
          label: { show: false },
          emphasis: {
            label: {
              show: true,
              fontSize: '14',
              fontWeight: 'bold'
            }
          },
          labelLine: { show: false },
          data: res.data.data || []
        }]
      }
      
      visitorSourceChart.setOption(option)
      console.log('游客来源图表加载成功')
    }
  } catch (error) {
    console.error('初始化游客来源图表失败', error)
  }
}

// 初始化年龄分布图表（调用真实API）
const initAgeDistributionChart = async () => {
  if (!ageDistributionChartRef.value) return
  
  try {
    const { startDate, endDate } = getDateRange(timeRange.value)
    const res: any = await getAgeDistribution(
      merchantScenicId.value,
      startDate,
      endDate
    )
    
    if (res.code === 200 && res.data) {
      if (!ageDistributionChart) {
        ageDistributionChart = echarts.init(ageDistributionChartRef.value)
      }
      
      const colors = ['#2A9D8F', '#F4A261', '#E76F51', '#457B9D', '#264653', '#67C23A']
      const labels = res.data.labels || []
      const data = (res.data.data || []).map((value: number, index: number) => ({
        value,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
            { offset: 0, color: colors[index % colors.length] },
            { offset: 1, color: colors[index % colors.length] + '80' }
          ])
        }
      }))
      
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          axisLabel: { color: '#909399', formatter: '{value}人' },
          axisLine: { lineStyle: { color: '#E4E7ED' } },
          splitLine: { lineStyle: { color: '#F0F2F5' } }
        },
        yAxis: {
          type: 'category',
          data: labels.reverse(), // 反转标签顺序
          axisLabel: { color: '#909399' },
          axisLine: { lineStyle: { color: '#E4E7ED' } }
        },
        series: [{
          name: '游客数量',
          type: 'bar',
          barWidth: '60%',
          data: data.reverse() // 反转数据顺序
        }]
      }
      
      ageDistributionChart.setOption(option)
      console.log('年龄分布图表加载成功')
    }
  } catch (error) {
    console.error('初始化年龄分布图表失败', error)
  }
}

// 初始化游客满意度图表（调用真实API）
const initSatisfactionChart = async () => {
  if (!satisfactionChartRef.value) return
  
  try {
    const { startDate, endDate } = getDateRange(timeRange.value)
    const res: any = await getSatisfactionAnalysis(
      merchantScenicId.value,
      startDate,
      endDate
    )
    
    if (res.code === 200 && res.data) {
      if (!satisfactionChart) {
        satisfactionChart = echarts.init(satisfactionChartRef.value)
      }
      
      const colors = ['#67C23A', '#409EFF', '#E6A23C', '#F56C6C', '#909399']
      const chartData = (res.data.labels || []).map((label: string, index: number) => ({
        value: res.data.data[index],
        name: label,
        itemStyle: { color: colors[index % colors.length] }
      }))
      
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c}人 ({d}%)'
        },
        series: [{
          type: 'pie',
          radius: '80%',
          center: ['50%', '50%'],
          data: chartData,
          roseType: 'radius',
          label: { color: '#606266' },
          labelLine: {
            lineStyle: { color: '#C0C4CC' },
            smooth: 0.2,
            length: 10,
            length2: 20
          },
          itemStyle: {
            borderRadius: 8,
            borderColor: '#FFFFFF',
            borderWidth: 2
          },
          animationType: 'scale',
          animationEasing: 'elasticOut'
        }]
      }
      
      satisfactionChart.setOption(option)
      console.log('满意度图表加载成功')
    }
  } catch (error) {
    console.error('初始化满意度图表失败', error)
  }
}
</script>

<style scoped>
.data-analysis-container {
  padding-bottom: 2rem;
}
</style> 