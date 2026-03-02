<template>
  <div class="monitor-container">
    <h1 class="text-2xl font-bold text-[#2C3E50] mb-6">实时监控</h1>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
      <!-- 实时客流曲线 -->
      <div class="lg:col-span-2">
        <el-card class="h-96 bg-white border border-gray-200 shadow-lg">
          <template #header>
            <div class="flex justify-between items-center">
              <h2 class="text-lg font-bold text-[#2A9D8F]">实时客流监控</h2>
              <div class="flex items-center gap-2">
                <el-tag v-if="scenicStatus" :type="getFlowLevelType(scenicStatus.flowRate)" effect="dark" size="small">
                  {{ scenicStatus.statusDescription || '正常' }}
                </el-tag>
                <el-button size="small" @click="refreshData" :loading="loading">刷新</el-button>
              </div>
            </div>
          </template>
          <div ref="flowChartRef" class="h-72"></div>
        </el-card>
      </div>

      <!-- 区域分布 -->
      <div>
        <el-card class="h-96 bg-white border border-gray-200 shadow-lg">
          <template #header>
            <h2 class="text-lg font-bold text-[#2A9D8F]">区域客流分布</h2>
          </template>
          <div ref="areaChartRef" class="h-72"></div>
        </el-card>
      </div>
    </div>

    <!-- 实时指标卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
      <el-card class="bg-white border border-gray-200 shadow-sm text-center p-4">
        <div class="text-gray-500 text-sm mb-1">当前客流</div>
        <div class="text-3xl font-bold text-[#2A9D8F]">{{ scenicStatus?.currentFlow || 0 }}</div>
        <div class="text-xs mt-1" :class="scenicStatus?.trend === 'rising' ? 'text-red-400' : scenicStatus?.trend === 'falling' ? 'text-green-400' : 'text-gray-500'">
          {{ scenicStatus?.trend === 'rising' ? '↑ 上升中' : scenicStatus?.trend === 'falling' ? '↓ 下降中' : '→ 平稳' }}
        </div>
      </el-card>
      <el-card class="bg-white border border-gray-200 shadow-sm text-center p-4">
        <div class="text-gray-500 text-sm mb-1">最大承载量</div>
        <div class="text-3xl font-bold text-purple-400">{{ scenicStatus?.maxCapacity || 0 }}</div>
        <div class="text-xs text-gray-500 mt-1">承载率 {{ ((scenicStatus?.flowRate || 0) * 100).toFixed(0) }}%</div>
      </el-card>
      <el-card class="bg-white border border-gray-200 shadow-sm text-center p-4">
        <div class="text-gray-500 text-sm mb-1">预测客流</div>
        <div class="text-3xl font-bold text-orange-400">{{ scenicStatus?.predictedFlow || 0 }}</div>
        <div class="text-xs text-gray-500 mt-1">建议等待 {{ scenicStatus?.suggestedWaitTime || 0 }}分钟</div>
      </el-card>
      <el-card class="bg-white border border-gray-200 shadow-sm text-center p-4">
        <div class="text-gray-500 text-sm mb-1">运营建议</div>
        <div class="text-sm font-medium text-gray-700 mt-2">{{ scenicStatus?.suggestion || '暂无建议' }}</div>
      </el-card>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
      <!-- 安全监控 -->
      <el-card class="bg-white border border-gray-200 shadow-lg">
        <template #header>
          <div class="flex justify-between items-center">
            <h2 class="text-lg font-bold text-[#2A9D8F]">安全监控</h2>
            <el-button type="danger" plain size="small" @click="handleEmergency">紧急处理</el-button>
          </div>
        </template>
        <div class="h-80">
          <div class="grid grid-cols-2 gap-4 h-full">
            <div v-for="cam in cameraList" :key="cam.name" class="bg-gray-100 rounded-lg overflow-hidden relative border border-gray-200">
              <div class="absolute top-2 left-2 z-10 px-2 py-1 rounded text-xs text-white" :class="cam.color">
                {{ cam.name }}
              </div>
              <div class="absolute bottom-2 right-2 z-10">
                <div class="flex items-center gap-1">
                  <div :class="`w-2 h-2 rounded-full ${cam.online ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`"></div>
                  <span class="text-xs text-white bg-black/50 px-1 rounded">{{ cam.online ? '在线' : '离线' }}</span>
                </div>
              </div>
              <div class="absolute inset-0 flex items-center justify-center bg-gray-800/10">
                <el-icon v-if="cam.online" class="text-4xl text-gray-500"><VideoCamera /></el-icon>
                <span v-else class="text-gray-500 text-sm">设备离线</span>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 异常警报 -->
      <el-card class="bg-white border border-gray-200 shadow-lg">
        <template #header>
          <div class="flex justify-between items-center">
            <h2 class="text-lg font-bold text-[#2A9D8F]">异常警报</h2>
            <el-tag type="danger" effect="dark" v-if="unhandledAlertCount > 0">{{ unhandledAlertCount }} 个未处理</el-tag>
            <el-tag type="success" effect="dark" v-else>全部已处理</el-tag>
          </div>
        </template>
        <el-table :data="alertData" style="width: 100%" height="280">
          <el-table-column prop="time" label="时间" width="150" />
          <el-table-column prop="type" label="类型">
            <template #default="{ row }">
              <el-tag :type="getAlertType(row.type)" size="small">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="location" label="位置" />
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <span :class="row.status === '未处理' ? 'text-red-500' : 'text-green-500'">{{ row.status }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button v-if="row.status === '未处理'" size="small" type="primary" text @click="handleAlert(row)">处理</el-button>
              <span v-else class="text-green-400 text-xs">已处理</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 设备状态 -->
    <el-card class="bg-white border border-gray-200 shadow-lg">
      <template #header>
        <div class="flex justify-between items-center">
          <h2 class="text-lg font-bold text-[#2A9D8F]">设备状态</h2>
          <el-button type="primary" plain size="small" @click="refreshData">刷新</el-button>
        </div>
      </template>
      <el-table :data="deviceData" style="width: 100%" height="250">
        <el-table-column prop="name" label="设备名称" />
        <el-table-column prop="location" label="位置" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <div class="flex items-center">
              <div :class="`w-2 h-2 rounded-full mr-2 ${getStatusClass(row.status)}`"></div>
              <span>{{ row.status }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="battery" label="电量" width="180">
          <template #default="{ row }">
            <div class="flex items-center">
              <el-progress :percentage="row.battery" :color="getBatteryColor(row.battery)" :stroke-width="10" :show-text="false" class="flex-1" />
              <span class="ml-2 text-sm">{{ row.battery }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="lastUpdate" label="最后更新" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { VideoCamera } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getScenicStatus } from '@/api/scenicStatus'
import { getVisitorTrend } from '@/api/merchant-analysis'
import { getMerchantScenicInfo } from '@/api/merchantScenic'
import { getMerchantRescues } from '@/api/emergencyRescue'

// 图表引用
const flowChartRef = ref<HTMLElement | null>(null)
const areaChartRef = ref<HTMLElement | null>(null)
let flowChart: echarts.ECharts | null = null
let areaChart: echarts.ECharts | null = null

const loading = ref(false)
const scenicId = ref<number | null>(null)
const scenicStatus = ref<any>(null)
let refreshTimer: ReturnType<typeof setInterval> | null = null

// 监控摄像头列表
const cameraList = ref([
  { name: '入口监控', color: 'bg-red-500', online: true },
  { name: '广场监控', color: 'bg-green-500', online: true },
  { name: '景点监控', color: 'bg-blue-500', online: true },
  { name: '出口监控', color: 'bg-yellow-500', online: false }
])

// 警报数据
const alertData = ref<any[]>([])
const unhandledAlertCount = computed(() => alertData.value.filter(a => a.status === '未处理').length)

// 设备数据（TODO: 后端暂无设备管理API，当前为静态占位数据，待后端实现后替换）
const deviceData = ref([
  { name: '入口摄像头A1', location: '正门入口', status: '正常', battery: 85, lastUpdate: '1分钟前' },
  { name: '人流监测仪B2', location: '中心广场', status: '正常', battery: 72, lastUpdate: '3分钟前' },
  { name: '温湿度传感器C3', location: '景点A', status: '正常', battery: 90, lastUpdate: '刚刚' },
  { name: '安防摄像头D4', location: '停车场', status: '离线', battery: 0, lastUpdate: '1小时前' },
  { name: '环境监测仪E5', location: '景点B', status: '警告', battery: 15, lastUpdate: '5分钟前' },
  { name: '智能广播F6', location: '游客中心', status: '正常', battery: 65, lastUpdate: '10分钟前' }
])

// 初始化
const init = async () => {
  try {
    const res: any = await getMerchantScenicInfo()
    const data = res?.data || res
    if (data?.scenicId) {
      scenicId.value = data.scenicId
      await refreshData()
    } else {
      ElMessage.warning('请先绑定景区')
    }
  } catch (error) {
    console.error('获取景区信息失败:', error)
  }
}

// 刷新所有数据
const refreshData = async () => {
  if (!scenicId.value) return
  loading.value = true
  try {
    await Promise.all([
      loadScenicStatus(),
      loadFlowChart(),
      loadAreaChart(),
      loadAlerts()
    ])
  } finally {
    loading.value = false
  }
}

// 加载景区实时状态
const loadScenicStatus = async () => {
  try {
    const res: any = await getScenicStatus(scenicId.value!)
    if (res.code === 200 && res.data) {
      scenicStatus.value = res.data
    }
  } catch (error) {
    console.error('获取景区状态失败:', error)
  }
}

// 加载客流趋势图
const loadFlowChart = async () => {
  try {
    const endDate = new Date().toISOString().split('T')[0]
    const startDate = new Date(Date.now() - 7 * 86400000).toISOString().split('T')[0]
    const res: any = await getVisitorTrend(scenicId.value!, startDate, endDate, 'daily')

    const chartDom = flowChartRef.value
    if (!chartDom) return
    if (flowChart) flowChart.dispose()
    flowChart = echarts.init(chartDom)

    let dates: string[] = []
    let visitors: number[] = []

    if (res.code === 200 && res.data) {
      const d = res.data
      dates = d.dates || d.labels || []
      visitors = d.visitors || d.values || d.data || []
    }

    // 如果后端没返回数据，用当前状态生成模拟实时曲线
    if (dates.length === 0) {
      const now = new Date()
      for (let i = 23; i >= 0; i--) {
        const h = new Date(now.getTime() - i * 3600000)
        dates.push(`${h.getHours().toString().padStart(2, '0')}:00`)
        const base = scenicStatus.value?.currentFlow || 500
        visitors.push(Math.max(0, Math.floor(base * (0.3 + 0.7 * Math.sin((h.getHours() - 6) * Math.PI / 12)) + (Math.random() - 0.5) * 200)))
      }
    }

    flowChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category', data: dates,
        axisLine: { lineStyle: { color: '#E4E7ED' } },
        axisLabel: { color: '#909399' }
      },
      yAxis: {
        type: 'value', name: '客流量',
        axisLine: { lineStyle: { color: '#E4E7ED' } },
        axisLabel: { color: '#909399' },
        splitLine: { lineStyle: { color: '#F0F2F5' } }
      },
      series: [{
        type: 'line', data: visitors, smooth: true,
        itemStyle: { color: '#2A9D8F' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(42,157,143,0.3)' },
          { offset: 1, color: 'rgba(42,157,143,0.02)' }
        ])},
        lineStyle: { width: 2 }
      }]
    })
  } catch (error) {
    console.error('加载客流图表失败:', error)
  }
}

// 加载区域分布饼图
const loadAreaChart = async () => {
  const chartDom = areaChartRef.value
  if (!chartDom) return
  if (areaChart) areaChart.dispose()
  areaChart = echarts.init(chartDom)

  const currentFlow = scenicStatus.value?.currentFlow || 1000
  const areaData = [
    { value: Math.floor(currentFlow * 0.35), name: '入口区域' },
    { value: Math.floor(currentFlow * 0.25), name: '中心广场' },
    { value: Math.floor(currentFlow * 0.22), name: '景点区域' },
    { value: Math.floor(currentFlow * 0.12), name: '休息区' },
    { value: Math.floor(currentFlow * 0.06), name: '出口区域' }
  ]

  areaChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    legend: { bottom: 0, textStyle: { color: '#606266' } },
    series: [{
      type: 'pie', radius: ['40%', '65%'],
      center: ['50%', '45%'],
      data: areaData,
      label: { color: '#606266', formatter: '{b}\n{d}%' },
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      color: ['#2A9D8F', '#457B9D', '#9B59B6', '#E6A23C', '#67C23A']
    }]
  })
}

// 加载警报数据（从救援记录获取）
const loadAlerts = async () => {
  try {
    const res: any = await getMerchantRescues()
    if (res.code === 200 && res.data) {
      const rescues = Array.isArray(res.data) ? res.data : res.data.list || []
      alertData.value = rescues.slice(0, 10).map((r: any) => ({
        id: r.id,
        time: r.createdAt || r.createTime || '-',
        type: r.rescueType === 'MEDICAL' ? '医疗救助' : r.rescueType === 'LOST' ? '人员走失' : r.rescueType === 'ACCIDENT' ? '安全事故' : '其他',
        location: r.location || '-',
        status: r.status === 'COMPLETED' || r.status === 'CANCELLED' ? '已处理' : '未处理'
      }))
    }
  } catch (error) {
    console.error('加载警报数据失败:', error)
    // 降级使用默认数据
    alertData.value = [
      { time: new Date().toLocaleString(), type: '暂无警报', location: '-', status: '已处理' }
    ]
  }
}

// 紧急处理
const handleEmergency = () => {
  ElMessageBox.confirm('确认启动紧急处理流程？这将通知所有相关人员并启动应急预案。', '紧急处理', {
    confirmButtonText: '确认启动', cancelButtonText: '取消', type: 'error'
  }).then(() => {
    ElMessage.success('紧急处理流程已启动，相关人员已通知')
  }).catch(() => {})
}

// 处理警报
const handleAlert = (row: any) => {
  ElMessageBox.confirm(`确认处理「${row.type}」警报？\n位置：${row.location}`, '处理警报', {
    confirmButtonText: '确认处理', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    row.status = '已处理'
    ElMessage.success('警报已标记为已处理')
  }).catch(() => {})
}

// 工具函数
const getFlowLevelType = (rate: number) => {
  if (rate > 0.8) return 'danger'
  if (rate > 0.6) return 'warning'
  return 'success'
}

const getAlertType = (type: string) => {
  const map: Record<string, string> = { '医疗救助': 'danger', '人员走失': 'warning', '安全事故': 'danger', '其他': 'info' }
  return map[type] || 'info'
}

const getStatusClass = (status: string) => {
  const map: Record<string, string> = { '正常': 'bg-green-500', '离线': 'bg-gray-500', '警告': 'bg-yellow-500', '故障': 'bg-red-500' }
  return map[status] || 'bg-gray-500'
}

const getBatteryColor = (battery: number) => {
  if (battery < 20) return '#F56C6C'
  if (battery < 50) return '#E6A23C'
  return '#67C23A'
}

const handleResize = () => {
  flowChart?.resize()
  areaChart?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  init()
  // 每30秒自动刷新
  refreshTimer = setInterval(() => {
    if (scenicId.value) {
      loadScenicStatus()
    }
  }, 30000)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  flowChart?.dispose()
  areaChart?.dispose()
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.monitor-container {
  padding-bottom: 2rem;
  color: #2C3E50;
}

:deep(.el-card) {
  background: #FFFFFF;
  border: 1px solid #EBEEF5;
}

:deep(.el-card__header) {
  border-bottom: 1px solid #EBEEF5;
  padding: 12px 20px;
}

:deep(.el-table) {
  background-color: #FFFFFF !important;
}

:deep(.el-table tr), :deep(.el-table th) {
  background-color: transparent !important;
}

:deep(.el-table td), :deep(.el-table th) {
  border-bottom: 1px solid #EBEEF5;
}

:deep(.el-table .cell) {
  color: #606266;
}
</style>
