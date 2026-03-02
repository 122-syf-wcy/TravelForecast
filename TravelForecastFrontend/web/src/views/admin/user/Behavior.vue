<template>
  <div class="behavior-container text-gray-800">
    <div class="page-header mb-6">
      <h2 class="text-2xl font-bold text-gray-800">用户行为分析</h2>
      <p class="text-gray-500 mt-1">用户访问与操作行为数据分析</p>
    </div>

    <!-- 关键指标 -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
      <holographic-card glow-color="neon-cyan" class="metric-card">
        <div class="text-center p-4">
          <p class="text-gray-500 text-sm mb-1">日均访问量</p>
          <p class="text-3xl font-bold text-[#2A9D8F] font-mono">{{ metrics[0].value }}</p>
          <p class="text-gray-500 text-xs mt-2">{{ metrics[0].desc }}</p>
        </div>
      </holographic-card>
      
      <holographic-card glow-color="energy-orange" class="metric-card">
        <div class="text-center p-4">
          <p class="text-gray-500 text-sm mb-1">平均停留时长</p>
          <p class="text-3xl font-bold text-orange-400 font-mono">{{ metrics[1].value }}</p>
          <p class="text-gray-500 text-xs mt-2">{{ metrics[1].desc }}</p>
        </div>
      </holographic-card>
      
      <holographic-card glow-color="tech-purple" class="metric-card">
        <div class="text-center p-4">
          <p class="text-gray-500 text-sm mb-1">总访问量</p>
          <p class="text-3xl font-bold text-[#457B9D] font-mono">{{ metrics[2].value }}</p>
          <p class="text-gray-500 text-xs mt-2">{{ metrics[2].desc }}</p>
        </div>
      </holographic-card>
      
      <holographic-card glow-color="neon-cyan" class="metric-card">
        <div class="text-center p-4">
          <p class="text-gray-500 text-sm mb-1">独立访客</p>
          <p class="text-3xl font-bold text-green-400 font-mono">{{ metrics[3].value }}</p>
          <p class="text-gray-500 text-xs mt-2">{{ metrics[3].desc }}</p>
        </div>
      </holographic-card>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
      <!-- 用户访问时段分布 -->
      <holographic-card glow-color="neon-cyan">
        <template #header>
          <div class="flex items-center gap-2">
            <el-icon class="text-[#2A9D8F]"><Clock /></el-icon>
            <h3 class="text-lg font-bold text-[#2A9D8F]">用户访问时段分布</h3>
          </div>
        </template>
        <div ref="timeChart" class="chart-container"></div>
      </holographic-card>

      <!-- 功能使用排行 -->
      <holographic-card glow-color="tech-purple">
        <template #header>
          <div class="flex items-center gap-2">
            <el-icon class="text-[#457B9D]"><TrendCharts /></el-icon>
            <h3 class="text-lg font-bold text-[#457B9D]">功能使用排行</h3>
          </div>
        </template>
        <div ref="featureChart" class="chart-container"></div>
      </holographic-card>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 用户停留时长分布 -->
      <holographic-card glow-color="energy-orange">
        <template #header>
          <div class="flex items-center gap-2">
            <el-icon class="text-orange-400"><Timer /></el-icon>
            <h3 class="text-lg font-bold text-orange-400">用户停留时长分布</h3>
          </div>
        </template>
        <div ref="durationChart" class="chart-container"></div>
      </holographic-card>

      <!-- 热门页面排行 -->
      <holographic-card glow-color="neon-cyan">
        <template #header>
          <div class="flex items-center gap-2">
            <el-icon class="text-red-400"><HotWater /></el-icon>
            <h3 class="text-lg font-bold text-red-400">热门页面 TOP10</h3>
          </div>
        </template>
        <el-table 
          :data="hotPages" 
          style="width: 100%" 
          :show-header="false" 
          class="bg-transparent"
        >
          <el-table-column type="index" width="60">
            <template #default="{ $index }">
              <span class="font-bold font-mono" :class="$index < 3 ? 'text-red-400' : 'text-gray-500'">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="page" label="页面">
            <template #default="{ row }">
              <span class="text-gray-500">{{ pageNameMap[row.page] || row.page }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="visits" label="访问量" width="100" align="right">
            <template #default="{ row }">
              <span class="text-[#2A9D8F] font-mono font-bold">{{ row.visits.toLocaleString() }}</span>
            </template>
          </el-table-column>
        </el-table>
      </holographic-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Clock, Timer, TrendCharts, HotWater } from '@element-plus/icons-vue'
import HolographicCard from '@/components/HolographicCard.vue'
import * as echarts from 'echarts'
import { adminApi } from '@/api/admin'

const metrics = ref([
  { label: '日均访问量', value: '0', desc: '平均每日UV' },
  { label: '平均停留时长', value: '0分', desc: '用户平均停留' },
  { label: '总访问量', value: '0', desc: '最近30天' },
  { label: '独立访客', value: '0', desc: '最近30天UV' }
])

const hotPages = ref<any[]>([])

const pageNameMap: Record<string, string> = {
  'dashboard': '首页仪表盘',
  'scenic': '景区探索',
  'prediction': '客流预测',
  'planning': '行程规划',
  'ai': '智能服务',
  'profile': '个人中心',
  'landing': '平台首页',
  'login': '登录页',
  'service': '实时服务',
  'register': '注册页',
  'news': '旅游资讯',
  'popular-spots': '热门景点'
}

const timeChart = ref()
const featureChart = ref()
const durationChart = ref()

// 加载统计数据
const loadStatistics = async () => {
  try {
    const res: any = await adminApi.getBehaviorStatistics()
    if (res.code === 200) {
      const data = res.data
      metrics.value = [
        { label: '日均访问量', value: data.dailyAvgVisits?.toLocaleString() || '0', desc: '平均每日UV' },
        { label: '平均停留时长', value: `${data.avgDuration || 0}分`, desc: '用户平均停留' },
        { label: '总访问量', value: data.totalVisits?.toLocaleString() || '0', desc: '最近30天' },
        { label: '独立访客', value: data.uniqueVisitors?.toLocaleString() || '0', desc: '最近30天UV' }
      ]
    }
  } catch (error: any) {
    console.error('加载统计数据失败:', error)
  }
}

// 加载热门页面
const loadHotPages = async () => {
  try {
    const res: any = await adminApi.getHotPages()
    if (res.code === 200) {
      hotPages.value = res.data || []
    }
  } catch (error: any) {
    console.error('加载热门页面失败:', error)
  }
}

// 初始化访问时段图表
const initTimeChart = async () => {
  if (!timeChart.value) return
  try {
    const res: any = await adminApi.getTimeDistribution()
    if (res.code === 200) {
      const distribution = res.data.distribution || []
      const hourlyData = new Array(24).fill(0)
      distribution.forEach((item: any) => {
        const hour = parseInt(item.hour)
        hourlyData[hour] = parseInt(item.count) || 0
      })
      
      const timeLabels = []
      const timeData = []
      for (let i = 0; i < 24; i += 2) {
        timeLabels.push(`${i}:00`)
        timeData.push(hourlyData[i] + hourlyData[i + 1])
      }
      
      const chart1 = echarts.init(timeChart.value)
      chart1.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis', backgroundColor: 'rgba(255, 255, 255, 0.98)', borderColor: '#2A9D8F', textStyle: { color: '#303133' } },
        grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
        xAxis: {
          type: 'category',
          data: timeLabels,
          axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.08)' } },
          axisLabel: { color: '#909399' }
        },
        yAxis: {
          type: 'value',
          axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.08)' } },
          axisLabel: { color: '#909399' },
          splitLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.04)' } }
        },
        series: [{
          data: timeData,
          type: 'line',
          smooth: true,
          symbol: 'none',
          lineStyle: { color: '#2A9D8F', width: 3 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(42, 157, 143, 0.3)' },
              { offset: 1, color: 'rgba(42, 157, 143, 0)' }
            ])
          }
        }]
      })
      window.addEventListener('resize', () => chart1.resize())
    }
  } catch (error: any) {
    console.error('加载访问时段数据失败:', error)
  }
}

// 初始化功能使用排行图表
const initFeatureChart = async () => {
  if (!featureChart.value) return
  try {
    const res: any = await adminApi.getModuleRanking()
    if (res.code === 200) {
      const ranking = res.data || []
      const names = ranking.map((item: any) => pageNameMap[item.name] || item.name || '未知')
      const values = ranking.map((item: any) => parseInt(item.count) || 0)
      
      const chart2 = echarts.init(featureChart.value)
      chart2.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis', backgroundColor: 'rgba(255, 255, 255, 0.98)', borderColor: '#457B9D', textStyle: { color: '#303133' } },
        grid: { left: '3%', right: '4%', bottom: '3%', top: '5%', containLabel: true },
        xAxis: {
          type: 'value',
          axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.08)' } },
          axisLabel: { color: '#909399' },
          splitLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.04)' } }
        },
        yAxis: {
          type: 'category',
          data: names.reverse(),
          axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.08)' } },
          axisLabel: { color: '#909399' }
        },
        series: [{
          data: values.reverse(),
          type: 'bar',
          itemStyle: {
             color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
              { offset: 0, color: '#457B9D' },
              { offset: 1, color: '#345d75' }
            ]),
            borderRadius: [0, 4, 4, 0]
          },
          barWidth: '50%'
        }]
      })
      window.addEventListener('resize', () => chart2.resize())
    }
  } catch (error: any) {
    console.error('加载功能使用排行失败:', error)
  }
}

// 初始化停留时长分布图表
const initDurationChart = async () => {
  if (!durationChart.value) return
  try {
    const res: any = await adminApi.getDurationDistribution()
    if (res.code === 200) {
      const distribution = res.data || []
      
      const colorMap: Record<string, string> = {
        '<1分钟': '#64748B',
        '1-3分钟': '#3B82F6',
        '3-5分钟': '#2A9D8F',
        '5-10分钟': '#F59E0B',
        '>10分钟': '#10B981'
      }
      
      const pieData = distribution.map((item: any) => ({
        value: parseInt(item.count) || 0,
        name: item.range_name || '未知',
        itemStyle: { color: colorMap[item.range_name] || '#64748B' }
      }))
      
      const chart3 = echarts.init(durationChart.value)
      chart3.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'item', backgroundColor: 'rgba(255, 255, 255, 0.98)', borderColor: '#2A9D8F', textStyle: { color: '#303133' } },
        legend: {
          orient: 'vertical',
          right: '5%',
          top: 'center',
          textStyle: { color: '#303133' }
        },
        series: [{
          name: '停留时长',
          type: 'pie',
          radius: ['50%', '70%'],
          center: ['35%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 5,
            borderColor: '#FFFFFF',
            borderWidth: 2
          },
          label: {
            show: false
          },
          data: pieData
        }]
      })
      window.addEventListener('resize', () => chart3.resize())
    }
  } catch (error: any) {
    console.error('加载停留时长分布失败:', error)
  }
}

const initCharts = async () => {
  await Promise.all([
    initTimeChart(),
    initFeatureChart(),
    initDurationChart()
  ])
}

onMounted(() => {
  loadStatistics()
  loadHotPages()
  nextTick(() => {
    initCharts()
  })
})

onBeforeUnmount(() => {
  [timeChart, featureChart, durationChart].forEach(r => {
    if (r.value) {
      const inst = echarts.getInstanceByDom(r.value)
      if (inst) inst.dispose()
    }
  })
})
</script>

<style scoped>
.behavior-container {
  padding: 20px;
}

.metric-card {
  transition: transform 0.3s;
}

.metric-card:hover {
  transform: translateY(-5px);
}

.chart-container {
  width: 100%;
  height: 300px;
}

/* Deep overrides for Element Plus Table */
:deep(.el-table) {
  background-color: transparent !important;
  --el-table-header-bg-color: #F5F7FA;
  --el-table-tr-bg-color: transparent;
  --el-table-border-color: rgba(0, 0, 0, 0.04);
  color: #2C3E50;
}

:deep(.el-table tr), :deep(.el-table th) {
  background-color: transparent !important;
}

:deep(.el-table td), :deep(.el-table th) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.04) !important;
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) {
  background-color: rgba(42, 157, 143, 0.1) !important;
}
</style>
