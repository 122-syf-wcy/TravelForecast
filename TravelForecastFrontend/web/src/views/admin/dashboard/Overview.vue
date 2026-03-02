<template>
  <div class="overview-container">
    <!-- 顶部卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
      <overview-card
        title="平台总用户"
        :value="statisticsData.totalUsers"
        :increase="statisticsData.userIncrease"
        :icon="User"
        color="from-blue-500 to-cyan-400"
      />
      
      <overview-card
        title="平台总收入"
        :value="`¥${statisticsData.totalRevenue}`"
        :increase="statisticsData.revenueIncrease"
        :icon="Money"
        color="from-green-500 to-teal-400"
      />
      
      <overview-card
        title="总商户数"
        :value="statisticsData.totalBusiness"
        :increase="statisticsData.businessIncrease"
        :icon="Shop"
        color="from-purple-500 to-indigo-400"
      />
      
      <overview-card
        title="平台景区"
        :value="statisticsData.totalScenic"
        :increase="statisticsData.scenicIncrease"
        :icon="PictureFilled"
        color="from-yellow-500 to-orange-400"
      />
    </div>
    
    <!-- 两列布局 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
      <!-- 左侧数据趋势图 -->
      <div class="lg:col-span-2">
        <holographic-card class="h-[450px]">
          <template #header>
            <div class="flex justify-between items-center p-4">
              <h2 class="text-xl font-bold text-[#2A9D8F]">平台运营趋势</h2>
              <el-select v-model="trendTimeRange" placeholder="时间范围" size="small">
                <el-option label="最近7天" value="week" />
                <el-option label="最近30天" value="month" />
                <el-option label="最近90天" value="quarter" />
              </el-select>
            </div>
          </template>
          <div class="p-4 h-full">
            <div ref="trendChartRef" class="w-full h-full"></div>
          </div>
        </holographic-card>
      </div>
      
      <!-- 右侧数据分布 -->
      <div>
        <holographic-card class="h-[450px]">
          <template #header>
            <div class="flex justify-between items-center p-4">
              <h2 class="text-xl font-bold text-[#2A9D8F]">用户分布</h2>
              <el-dropdown trigger="click">
                <el-button size="small" plain type="primary">
                  {{ distributionMode }}
                  <el-icon class="ml-1"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="distributionMode = '地域分布'">地域分布</el-dropdown-item>
                    <el-dropdown-item @click="distributionMode = '年龄分布'">年龄分布</el-dropdown-item>
                    <el-dropdown-item @click="distributionMode = '性别分布'">性别分布</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
          <div class="p-4 h-full">
            <div ref="distributionChartRef" class="w-full h-full"></div>
          </div>
        </holographic-card>
      </div>
    </div>
    
    <!-- 两列数据展示 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
      <!-- 热门景区 -->
      <holographic-card>
        <template #header>
          <div class="flex justify-between items-center p-4">
            <h2 class="font-bold text-[#2A9D8F]">热门景区排行</h2>
            <el-button size="small" text type="primary" @click="goToScenicList">查看更多</el-button>
          </div>
        </template>
        <div class="p-4">
          <div class="space-y-4">
            <div v-for="(item, index) in hotScenicData" :key="index" class="flex items-center">
              <div class="rank-number mr-3" :class="getRankClass(index)">{{ index + 1 }}</div>
              <el-image 
                v-if="item.image" 
                :src="item.image" 
                class="w-12 h-12 rounded-lg object-cover mr-3"
                fit="cover"
              >
                <template #error>
                  <div class="w-12 h-12 rounded-lg bg-gray-200 flex items-center justify-center mr-3">
                    <span class="text-gray-500 text-xs">暂无图片</span>
                  </div>
                </template>
              </el-image>
              <div v-else class="w-12 h-12 rounded-lg bg-gray-200 flex items-center justify-center mr-3">
                <span class="text-gray-500 text-xs">无图</span>
              </div>
              <div class="flex-1 min-w-0">
                <div class="text-gray-800 text-sm mb-1 truncate">{{ item.name }}</div>
                <div class="flex justify-between text-xs">
                  <span class="text-gray-500 truncate mr-2">{{ item.location }}</span>
                  <span class="text-[#2A9D8F] whitespace-nowrap">{{ item.favoritesCount }}收藏</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </holographic-card>
      
      <!-- 待办事项 -->
      <holographic-card>
        <template #header>
          <div class="flex justify-between items-center p-4">
            <h2 class="font-bold text-[#2A9D8F]">待办事项</h2>
            <el-badge :value="pendingData.filter(item => !item.processed).length" type="danger">
              <el-button size="small" text type="primary" @click="goToPendingList">查看全部</el-button>
            </el-badge>
          </div>
        </template>
        <div class="p-4">
          <div class="space-y-3 max-h-[250px] overflow-y-auto">
            <div v-for="(item, index) in pendingData" :key="index" class="p-3 rounded-lg border border-gray-200">
              <div class="flex items-center justify-between mb-1">
                <span class="text-gray-800 text-sm">{{ item.title }}</span>
                <el-tag size="small" :type="item.processed ? 'success' : 'warning'" effect="dark">
                  {{ item.processed ? '已处理' : '待处理' }}
                </el-tag>
              </div>
              <p class="text-gray-500 text-sm mb-2">{{ item.description }}</p>
              <div class="flex justify-between text-xs">
                <span class="text-gray-500">提交人: {{ item.submitter }}</span>
                <span class="text-gray-500">{{ item.time }}</span>
              </div>
            </div>
          </div>
        </div>
      </holographic-card>
    </div>
    
    <!-- 底部数据表格 -->
    <holographic-card>
      <template #header>
        <div class="flex justify-between items-center p-4">
          <h2 class="text-xl font-bold text-[#2A9D8F]">平台最新动态</h2>
          <div>
            <el-input
              v-model="searchKeyword"
              placeholder="搜索动态"
              prefix-icon="Search"
              class="w-60"
              clearable
            />
          </div>
        </div>
      </template>
      <div class="p-4">
        <el-table
          :data="filterActivities"
          style="width: 100%"
          :header-cell-style="{ background: 'transparent', color: '#909399' }"
        >
          <el-table-column prop="time" label="时间" width="180" />
          <el-table-column prop="type" label="类型" width="120">
            <template #default="{ row }">
              <el-tag :type="getActivityType(row.type)" size="small">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="内容" />
          <el-table-column prop="operator" label="操作人" width="120" />
          <el-table-column prop="ip" label="IP地址" width="150" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="viewActivityDetail(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="flex justify-end mt-4">
          <el-pagination
            :page-size="10"
            layout="prev, pager, next"
            :total="activityData.length"
          />
        </div>
      </div>
    </holographic-card>

    <!-- 待办事项弹窗 -->
    <el-dialog 
      v-model="pendingDialogVisible" 
      width="920px" 
      :close-on-click-modal="false"
      class="pending-dialog"
    >
      <template #header>
        <div class="flex items-center">
          <el-icon class="text-2xl text-[#2A9D8F] mr-2"><Notebook /></el-icon>
          <span class="text-xl font-bold">待办事项管理</span>
        </div>
      </template>
      
      <div class="flex items-center justify-between mb-4 p-4 rounded-lg bg-gradient-to-r from-gray-50 to-gray-100 border border-gray-200">
        <el-radio-group v-model="pendingFilter" size="default" class="custom-radio-group">
          <el-radio-button label="all">
            <el-icon class="mr-1"><List /></el-icon>全部
          </el-radio-button>
          <el-radio-button label="pending">
            <el-icon class="mr-1"><Clock /></el-icon>待处理
          </el-radio-button>
          <el-radio-button label="processed">
            <el-icon class="mr-1"><CircleCheck /></el-icon>已处理
          </el-radio-button>
        </el-radio-group>
        <div class="flex items-center space-x-4">
          <el-button type="primary" size="small" @click="openAddTaskDialog">
            <el-icon class="mr-1"><Plus /></el-icon>添加待办
          </el-button>
          <div class="text-sm">
            <el-icon class="text-orange-400 mr-1"><Warning /></el-icon>
            <span class="text-gray-500">待处理：</span>
            <span class="text-orange-400 font-bold">{{ pendingData.filter(i => !i.processed).length }}</span>
          </div>
          <div class="text-sm">
            <el-icon class="text-[#2A9D8F] mr-1"><Document /></el-icon>
            <span class="text-gray-500">总数：</span>
            <span class="text-[#2A9D8F] font-bold">{{ pendingData.length }}</span>
          </div>
        </div>
      </div>
      
      <el-table 
        :data="filteredPendingData" 
        style="width: 100%" 
        max-height="500"
        :row-class-name="tableRowClassName"
      >
        <el-table-column prop="title" label="事项标题" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="flex items-center space-x-2">
              <el-icon class="text-base flex-shrink-0" :class="row.processed ? 'text-green-400' : 'text-orange-400'">
                <FolderOpened v-if="!row.processed" />
                <FolderChecked v-else />
              </el-icon>
              <span class="font-medium">{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="详细说明" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-gray-500 text-sm">{{ row.description || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitter" label="提交人" width="100" align="center">
          <template #default="{ row }">
            <span>{{ row.submitter }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="提交时间" width="150" align="center">
          <template #default="{ row }">
            <span class="text-sm text-gray-500">{{ row.time }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2">
              <el-tag 
                size="small" 
                :type="row.processed ? 'success' : 'warning'"
                effect="plain"
              >
                {{ row.processed ? '已处理' : '待处理' }}
              </el-tag>
              <el-button 
                v-if="!row.processed" 
                type="primary" 
                size="small"
                link
                @click="handleProcessItem(row)"
              >
                处理
              </el-button>
              <el-button 
                type="danger" 
                size="small"
                link
                @click="handleDeleteTask(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 添加待办事项对话框 -->
    <el-dialog
      v-model="addTaskDialogVisible"
      title="添加待办事项"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="taskForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="taskForm.title" placeholder="请输入事项标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="详细说明">
          <el-input
            v-model="taskForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入详细说明"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addTaskDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleAddTask">确认添加</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed, watch, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  ArrowDown, Notebook, List, Clock, CircleCheck, Warning, Document,
  FolderOpened, FolderChecked, User, Timer, Select, SuccessFilled, Plus, Delete,
  Money, Shop, PictureFilled
} from '@element-plus/icons-vue'
import HolographicCard from '@/components/HolographicCard.vue'
import OverviewCard from '@/components/OverviewCard.vue'
import * as echarts from 'echarts'
import {
  getOverviewStatistics,
  getTrendData,
  getDistributionData,
  getHotScenicRankings,
  getPendingTasks,
  getActivityLogs,
  markTaskAsProcessed,
  createTask,
  deleteTask
} from '@/api/admin-dashboard'

const router = useRouter()

// 图表引用
const trendChartRef = ref<HTMLElement | null>(null)
const distributionChartRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null
let distributionChart: echarts.ECharts | null = null

// 筛选和搜索
const trendTimeRange = ref('week')
const distributionMode = ref('地域分布')
const searchKeyword = ref('')
const pendingDialogVisible = ref(false)
const pendingFilter = ref('all')
const addTaskDialogVisible = ref(false)
const taskForm = ref({
  title: '',
  description: ''
})

// 统计数据
const statisticsData = ref({
  totalUsers: 0,
  userIncrease: 0,
  totalRevenue: '0',
  revenueIncrease: 0,
  totalBusiness: 0,
  businessIncrease: 0,
  totalScenic: 0,
  scenicIncrease: 0
})

// 热门景区数据
const hotScenicData = ref<any[]>([])

// 待处理事项
const pendingData = ref<any[]>([])

// 平台活动数据
const activityData = ref<any[]>([])

// 过滤后的活动数据
const filterActivities = computed(() => {
  if (!searchKeyword.value) return activityData.value
  
  const keyword = searchKeyword.value.toLowerCase()
  return activityData.value.filter(item => {
    return item.content.toLowerCase().includes(keyword) ||
           item.type.toLowerCase().includes(keyword) ||
           item.operator.toLowerCase().includes(keyword)
  })
})

// 获取排行样式
const getRankClass = (index: number) => {
  const classes = [
    'bg-yellow-500 text-gray-800',
    'bg-gray-400 text-gray-800',
    'bg-amber-700 text-gray-800',
    'bg-gray-200 text-gray-800',
    'bg-gray-200 text-gray-800'
  ]
  
  return classes[index] || 'bg-gray-200 text-gray-800'
}

// 获取活动类型
const getActivityType = (type: string) => {
  const types: Record<string, string> = {
    '登录': 'info',
    '操作': 'success',
    '审核': 'primary',
    '配置': 'warning'
  }
  return types[type] || 'info'
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const res: any = await getOverviewStatistics()
    if (res.code === 200) {
      statisticsData.value = res.data
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
  }
}

// 加载热门景区
const loadHotScenicRankings = async () => {
  try {
    const res: any = await getHotScenicRankings()
    if (res.code === 200) {
      hotScenicData.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载景区排行失败:', error)
  }
}

// 加载待办事项
const loadTasks = async () => {
  try {
    const res: any = await getPendingTasks()
    if (res.code === 200) {
      pendingData.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载待办事项失败:', error)
  }
}

// 加载活动日志
const loadActivityLogs = async () => {
  try {
    const res: any = await getActivityLogs()
    if (res.code === 200) {
      activityData.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载活动日志失败:', error)
  }
}

// 加载趋势数据并更新图表
const loadTrendData = async () => {
  try {
    const res: any = await getTrendData(trendTimeRange.value)
    if (res.code === 200 && trendChart) {
      updateTrendChart(res.data)
    }
  } catch (error) {
    console.error('加载趋势数据失败:', error)
  }
}

// 加载分布数据并更新图表
const loadDistributionData = async () => {
  try {
    const typeMap: Record<string, string> = {
      '地域分布': 'region',
      '年龄分布': 'age',
      '性别分布': 'gender'
    }
    const type = typeMap[distributionMode.value] || 'region'
    const res: any = await getDistributionData(type)
    if (res.code === 200 && distributionChart) {
      updateDistributionChart(res.data)
    }
  } catch (error) {
    console.error('加载分布数据失败:', error)
  }
}

// 监听时间范围变化
watch(trendTimeRange, () => {
  loadTrendData()
})

// 监听分布模式变化
watch(distributionMode, () => {
  loadDistributionData()
})

// 组件挂载时初始化图表
onMounted(() => {
  console.log('Overview组件已挂载')
  
  // 加载所有数据
  loadStatistics()
  loadHotScenicRankings()
  loadTasks()
  loadActivityLogs()
  
  // 初始化图表
  initCharts()
  
  // 添加额外的初始化尝试，以应对可能的DOM渲染延迟或首次渲染失败
  const retryTimes = [1000, 2000, 3000] // 在1秒、2秒和3秒后重试
  retryTimes.forEach(delay => {
    setTimeout(() => {
      if (!trendChart || !distributionChart) {
        console.log(`在${delay}ms后重试初始化图表`)
        initCharts()
      }
    }, delay)
  })
})

// 初始化所有图表
const initCharts = () => {
  // 确保DOM已完全渲染
  setTimeout(() => {
    try {
      console.log('开始初始化图表', trendChartRef.value, distributionChartRef.value)
      
      if (!trendChartRef.value || !distributionChartRef.value) {
        console.error('图表DOM引用未找到，将在1秒后重试')
        setTimeout(() => {
          initTrendChart()
          initDistributionChart()
        }, 1000)
        return
      }
      
      initTrendChart()
      initDistributionChart()
      
      // 图表初始化后立即加载真实数据
      setTimeout(() => {
        loadTrendData()
        loadDistributionData()
      }, 500) // 缩短等待时间
    } catch (error) {
      console.error('初始化图表失败:', error)
    }
  }, 800) // 增加等待时间
}

// 窗口大小调整处理函数
const handleResize = () => {
  if (trendChart) {
    trendChart.resize()
  }
  if (distributionChart) {
    distributionChart.resize()
  }
}

// 组件卸载时清理事件监听和图表实例
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
  
  if (distributionChart) {
    distributionChart.dispose()
    distributionChart = null
  }
})

// 初始化趋势图表
const initTrendChart = () => {
  if (!trendChartRef.value) {
    console.error('趋势图表DOM引用不存在')
    return
  }
  
  // 确保容器尺寸正常
  const containerWidth = trendChartRef.value.clientWidth
  const containerHeight = trendChartRef.value.clientHeight
  
  if (containerWidth <= 0 || containerHeight <= 0) {
    console.error(`趋势图表容器尺寸异常: ${containerWidth}x${containerHeight}，将在1秒后重试`)
    setTimeout(() => initTrendChart(), 1000)
    return
  }
  
  console.log(`趋势图表容器尺寸: ${containerWidth}x${containerHeight}`)
  
  // 清理旧实例
  if (trendChart) {
    trendChart.dispose()
  }
  
  // 创建新实例
  try {
    console.log('初始化趋势图表')
    trendChart = echarts.init(trendChartRef.value)
    
    // 根据选择的时间范围生成数据
    let xAxisData = []
    if (trendTimeRange.value === 'week') {
      xAxisData = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    } else if (trendTimeRange.value === 'month') {
      xAxisData = Array.from({ length: 30 }, (_, i) => `${i + 1}日`)
    } else {
      xAxisData = Array.from({ length: 12 }, (_, i) => `${i + 1}月`)
    }
    
    // 模拟数据 - 更新为六盘水旅游季节性特点的数据
    // 夏季(6-8月)和冬季(12-2月)是旅游高峰期
    const generateSeasonalData = (base: number, peak: number, offPeak: number): number[] => {
      if (trendTimeRange.value === 'week') {
        // 周末游客量上升
        return [
          base + Math.floor(Math.random() * 200),          // 周一
          base + Math.floor(Math.random() * 200),          // 周二
          base + Math.floor(Math.random() * 300),          // 周三
          base + Math.floor(Math.random() * 400),          // 周四
          base + Math.floor(Math.random() * 500) + 200,    // 周五
          base + Math.floor(Math.random() * 600) + 500,    // 周六
          base + Math.floor(Math.random() * 600) + 400     // 周日
        ]
      } else if (trendTimeRange.value === 'month') {
        // 月中和月末游客量上升
        return Array.from({ length: 30 }, (_, i) => {
          // 周末效应
          const isWeekend = (i + 1) % 7 === 0 || (i + 2) % 7 === 0
          // 月中和月末效应
          const isMiddleMonth = i >= 13 && i <= 17
          const isEndMonth = i >= 27
          
          let modifier = 0
          if (isWeekend) modifier += 300
          if (isMiddleMonth) modifier += 200
          if (isEndMonth) modifier += 400
          
          return base + Math.floor(Math.random() * 300) + modifier
        })
      } else {
        // 季节性效应 - 夏季和冬季是旅游高峰
        return [
          peak + Math.floor(Math.random() * 500),     // 1月 - 冬季高峰
          peak + Math.floor(Math.random() * 500),     // 2月 - 冬季高峰+春节
          offPeak + Math.floor(Math.random() * 300),  // 3月
          offPeak + Math.floor(Math.random() * 300),  // 4月
          base + Math.floor(Math.random() * 400),     // 5月 - 五一小高峰
          peak + Math.floor(Math.random() * 500),     // 6月 - 夏季高峰开始
          peak + Math.floor(Math.random() * 600),     // 7月 - 夏季高峰
          peak + Math.floor(Math.random() * 600),     // 8月 - 夏季高峰
          base + Math.floor(Math.random() * 400),     // 9月
          base + Math.floor(Math.random() * 500),     // 10月 - 国庆小高峰
          offPeak + Math.floor(Math.random() * 300),  // 11月
          peak + Math.floor(Math.random() * 500)      // 12月 - 冬季高峰
        ]
      }
    }
    
    // 生成季节性数据
    const visitorData = generateSeasonalData(800, 1500, 600)
    const orderData = generateSeasonalData(400, 800, 300)
    const revenueData = visitorData.map(v => v * 120 + Math.floor(Math.random() * 5000))
    
    const option = {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross',
          crossStyle: {
            color: '#909399'
          }
        }
      },
      legend: {
        data: ['游客量', '订单量', '营收额'],
        textStyle: {
          color: '#606266'
        },
        top: 0
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        top: '40',
        containLabel: true
      },
      xAxis: [
        {
          type: 'category',
          data: xAxisData,
          axisPointer: {
            type: 'shadow'
          },
          axisLine: {
            lineStyle: {
              color: 'rgba(0, 0, 0, 0.06)'
            }
          },
          axisTick: {
            show: false
          },
          axisLabel: {
            color: '#909399',
            fontFamily: 'Orbitron',
            rotate: trendTimeRange.value === 'month' ? 45 : 0
          }
        }
      ],
      yAxis: [
        {
          type: 'value',
          name: '人数/单数',
          min: 0,
          nameTextStyle: {
            color: '#909399'
          },
          axisLine: {
            show: true,
            lineStyle: {
              color: 'rgba(0, 0, 0, 0.04)'
            }
          },
          axisLabel: {
            color: '#909399',
            formatter: '{value}'
          },
          splitLine: {
            lineStyle: {
              color: 'rgba(42, 157, 143, 0.03)',
              type: 'dashed'
            }
          }
        },
        {
          type: 'value',
          name: '金额',
          min: 0,
          nameTextStyle: {
            color: '#909399'
          },
          axisLine: {
            show: true,
            lineStyle: {
              color: 'rgba(0, 0, 0, 0.04)'
            }
          },
          axisLabel: {
            color: '#909399',
            formatter: '{value}'
          },
          splitLine: {
            show: false
          }
        }
      ],
      series: [
        {
          name: '游客量',
          type: 'bar',
          barWidth: 12,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#2A9D8F' },
              { offset: 0.5, color: 'rgba(42, 157, 143, 0.5)' },
              { offset: 1, color: 'rgba(42, 157, 143, 0.1)' }
            ]),
            borderRadius: [6, 6, 0, 0]
          },
          data: visitorData
        },
        {
          name: '订单量',
          type: 'line', // 改为曲线更美观
          smooth: true,
          symbol: 'none',
          lineStyle: {
            width: 3,
            color: '#FF6B35',
            shadowColor: 'rgba(255, 107, 53, 0.5)',
            shadowBlur: 10
          },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(255, 107, 53, 0.3)' },
              { offset: 1, color: 'rgba(255, 107, 53, 0)' }
            ])
          },
          data: orderData
        },
        {
          name: '营收额',
          type: 'line',
          yAxisIndex: 1,
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          itemStyle: {
            color: '#457B9D',
            bordercolor: '#303133',
            borderWidth: 2
          },
          lineStyle: {
            width: 3,
            color: '#457B9D',
            shadowColor: 'rgba(69, 123, 157, 0.5)',
            shadowBlur: 10
          },
          data: revenueData
        }
      ]
    }
    
    // 设置图表选项
    trendChart.setOption(option)
    
    // 添加窗口大小变化监听
    window.addEventListener('resize', handleResize)
    
    console.log('趋势图表初始化完成')
  } catch (error) {
    console.error('初始化趋势图表失败:', error)
  }
}

// 初始化分布图表
const initDistributionChart = async () => {
  if (!distributionChartRef.value) {
    console.error('分布图表DOM引用不存在')
    return
  }
  
  // 确保容器尺寸正常
  const containerWidth = distributionChartRef.value.clientWidth
  const containerHeight = distributionChartRef.value.clientHeight
  
  if (containerWidth <= 0 || containerHeight <= 0) {
    console.error(`分布图表容器尺寸异常: ${containerWidth}x${containerHeight}，将在1秒后重试`)
    setTimeout(() => initDistributionChart(), 1000)
    return
  }
  
  console.log(`分布图表容器尺寸: ${containerWidth}x${containerHeight}`)
  
  // 清理旧实例
  if (distributionChart) {
    distributionChart.dispose()
  }
  
  // 创建新实例
  try {
    console.log('初始化分布图表')
    distributionChart = echarts.init(distributionChartRef.value)
    

    
    try {
      const res: any = await getDistributionData(distributionMode.value)
      if (res.code === 200) {
        const data = res.data || []
        let option = {}
        
        if (distributionMode.value === '地域分布') {
          option = {
            backgroundColor: 'transparent',
            tooltip: {
              trigger: 'item',
              formatter: '{a} <br/>{b}: {c} ({d}%)',
              backgroundColor: 'rgba(255, 255, 255, 0.98)',
              textStyle: { color: '#303133' },
              borderColor: '#2A9D8F'
            },
            legend: {
              orient: 'horizontal',
              bottom: '2%',
              left: 'center',
              textStyle: { color: '#909399', fontSize: 11 },
              itemWidth: 12,
              itemHeight: 12,
              itemGap: 20
            },
            series: [
              {
                name: '用户来源',
                type: 'pie',
                radius: ['45%', '70%'],
                center: ['50%', '42%'],
                avoidLabelOverlap: true,
                data: data,
                itemStyle: {
                  borderRadius: 8,
                  borderColor: '#FFFFFF',
                  borderWidth: 2
                },
                label: {
                  show: true,
                  position: 'outside',
                  formatter: '{b}: {d}%',
                  color: '#303133'
                },
                labelLine: { show: true, length: 15, length2: 10, lineStyle: { color: 'rgba(0, 0, 0, 0.15)' } },
                emphasis: {
                  scaleSize: 10,
                  itemStyle: {
                    shadowBlur: 20,
                    shadowOffsetX: 0,
                    shadowColor: 'rgba(42, 157, 143, 0.6)'
                  },
                  label: {
                    show: true,
                    fontSize: 15,
                    fontWeight: 'bold',
                    color: '#303133'
                  }
                }
              }
            ],
            color: ['#2A9D8F', '#457B9D', '#FF6B35', '#FFD700', '#E76F51', '#909399']
          }
        } else if (distributionMode.value === '年龄分布') {
          option = {
            backgroundColor: 'transparent',
            tooltip: {
              trigger: 'axis',
              axisPointer: { type: 'shadow' },
              backgroundColor: 'rgba(255, 255, 255, 0.98)',
              textStyle: { color: '#303133' },
              borderColor: '#2A9D8F'
            },
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              top: '40',
              containLabel: true
            },
            xAxis: [{
              type: 'category',
              data: data.map((item: any) => item.name),
              axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.04)' } },
              axisLabel: { color: '#909399' }
            }],
            yAxis: [{
              type: 'value',
              axisLine: { show: true, lineStyle: { color: 'rgba(0, 0, 0, 0.04)' } },
              axisLabel: { color: '#909399' },
              splitLine: { lineStyle: { color: 'rgba(42, 157, 143, 0.03)', type: 'dashed' } }
            }],
            series: [{
              name: '用户数量',
              type: 'bar',
              barWidth: '50%',
              data: data.map((item: any) => item.value),
              itemStyle: {
                color: function(params: any) {
                  const colorList = [
                    new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#FFD700' }, { offset: 1, color: '#FFAA00' }]),
                    new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#E76F51' }, { offset: 1, color: '#E76F51' }]),
                    new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#457B9D' }, { offset: 1, color: '#345d75' }]),
                    new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#2A9D8F' }, { offset: 1, color: '#2A9D8F' }]),
                    new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#2A9D8F' }, { offset: 1, color: '#238b7e' }]),
                    new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#909399' }, { offset: 1, color: '#718096' }]),
                    new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#E2E8F0' }, { offset: 1, color: '#CBD5E0' }])
                  ];
                  return colorList[params.dataIndex % colorList.length];
                },
                borderRadius: [4, 4, 0, 0]
              },
              label: { show: true, position: 'top', color: '#303133', formatter: '{c}人' }
            }]
          }
        } else {
          // 性别分布
          option = {
            backgroundColor: 'transparent',
            tooltip: {
              trigger: 'item',
              formatter: '{a} <br/>{b}: {c}人 ({d}%)',
              backgroundColor: 'rgba(255, 255, 255, 0.98)',
              textStyle: { color: '#303133' },
              borderColor: '#2A9D8F'
            },
            legend: {
              orient: 'horizontal',
              bottom: '2%',
              left: 'center',
              textStyle: { color: '#909399', fontSize: 11 },
              itemWidth: 12,
              itemHeight: 12,
              itemGap: 20
            },
            series: [{
              name: '性别比例',
              type: 'pie',
              radius: ['45%', '70%'],
              center: ['50%', '42%'],
              avoidLabelOverlap: true,
              itemStyle: {
                borderRadius: 10,
                borderColor: '#FFFFFF',
                borderWidth: 2
              },
              label: {
                show: true,
                position: 'outside',
                formatter: '{b}: {d}%',
                color: '#303133'
              },
              labelLine: { show: true },
              emphasis: {
                scaleSize: 10,
                itemStyle: {
                  shadowBlur: 20,
                  shadowOffsetX: 0,
                  shadowColor: 'rgba(42, 157, 143, 0.6)'
                },
                label: { show: true, fontSize: 15, fontWeight: 'bold', color: '#303133' }
              },
              data: data.map((item: any) => ({
                value: item.value,
                name: item.name,
                itemStyle: { color: item.name === '女性' || item.name === 'Female' ? '#E76F51' : '#2A9D8F' }
              }))
            }]
          }
        }
        
        distributionChart.setOption(option)
      }
    } catch (error) {
      console.error('初始化分布图表失败:', error)
    }

  } catch (error) {
    console.error('初始化分布图表(外层)失败:', error)
  }
}


// 注释掉重复的watch，避免用模拟数据覆盖真实数据
// watch(trendTimeRange, () => {
//   nextTick(() => {
//     initTrendChart()
//   })
// })

// watch(distributionMode, () => {
//   nextTick(() => {
//     initDistributionChart()
//   })
// })

// 热门景区：查看更多
const goToScenicList = () => {
  router.push('/admin/content/scenic')
}

// 待办事项：查看全部
const goToPendingList = () => {
  pendingDialogVisible.value = true
}

// 待办事项：过滤数据
const filteredPendingData = computed(() => {
  if (pendingFilter.value === 'pending') {
    return pendingData.value.filter(item => !item.processed)
  } else if (pendingFilter.value === 'processed') {
    return pendingData.value.filter(item => item.processed)
  }
  return pendingData.value
})

// 更新趋势图表数据
const updateTrendChart = (data: any) => {
  if (!trendChart || !data) {
    console.log('无法更新趋势图表', { trendChart: !!trendChart, data: !!data })
    return
  }
  
  try {
    console.log('更新趋势图表数据:', data)
    const option = {
      xAxis: [{
        data: data.dates || []
      }],
      series: [
        {
          data: data.visitors || []
        },
        {
          data: data.orders || []
        },
        {
          data: data.revenues || []
        }
      ]
    }
    trendChart.setOption(option) // 使用默认的合并模式
    console.log('趋势图表数据更新成功')
  } catch (error) {
    console.error('更新趋势图表失败:', error)
  }
}

// 更新分布图表数据
const updateDistributionChart = (data: any) => {
  if (!distributionChart || !data) return
  
  try {
    const chartData = data.data || []
    const option = {
      series: [{
        data: chartData
      }]
    }
    distributionChart.setOption(option)
  } catch (error) {
    console.error('更新分布图表失败:', error)
  }
}

// 待办事项：标记处理
const handleProcessItem = async (row: any) => {
  ElMessageBox.confirm(`确认将"${row.title}"标记为已处理？`, '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res: any = await markTaskAsProcessed(row.id)
      if (res.code === 200) {
        row.processed = true
        ElMessage.success('已标记为已处理')
        loadTasks() // 重新加载待办事项
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    } catch (error) {
      console.error('标记待办事项失败:', error)
      ElMessage.error('操作失败')
    }
  }).catch(() => {})
}

// 表格行样式
const tableRowClassName = ({ row }: { row: any }) => {
  return row.processed ? 'processed-row' : 'pending-row'
}

// 动态表格：查看详情
const viewActivityDetail = (row: any) => {
  ElMessageBox.alert(
    `<div style="color:#ccc">
      <p><strong>时间：</strong>${row.time}</p>
      <p><strong>类型：</strong>${row.type}</p>
      <p><strong>内容：</strong>${row.content}</p>
      <p><strong>操作人：</strong>${row.operator}</p>
      <p><strong>IP：</strong>${row.ip}</p>
    </div>`,
    '动态详情',
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '关闭'
    }
  )
}

// 打开添加待办事项对话框
const openAddTaskDialog = () => {
  taskForm.value = {
    title: '',
    description: ''
  }
  addTaskDialogVisible.value = true
}

// 添加待办事项
const handleAddTask = async () => {
  if (!taskForm.value.title.trim()) {
    ElMessage.warning('请输入事项标题')
    return
  }
  
  try {
    const res: any = await createTask(taskForm.value)
    if (res.code === 200) {
      ElMessage.success('添加成功')
      addTaskDialogVisible.value = false
      loadTasks() // 重新加载待办事项
    } else {
      ElMessage.error(res.message || '添加失败')
    }
  } catch (error) {
    console.error('添加待办事项失败:', error)
    ElMessage.error('添加失败')
  }
}

// 删除待办事项
const handleDeleteTask = (row: any) => {
  ElMessageBox.confirm(`确认删除待办事项"${row.title}"？`, '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res: any = await deleteTask(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadTasks() // 重新加载待办事项
      } else {
        ElMessage.error(res.message || '删除失败')
      }
    } catch (error) {
      console.error('删除待办事项失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}
</script>

<style scoped>
.rank-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: bold;
}

/* 确保图表容器有明确的尺寸 */
:deep(.holographic-card) .w-full.h-full {
  min-height: 300px;
  width: 100% !important;
  height: 100% !important;
}

:deep(.el-table) {
  --el-table-border-color: #EBEEF5;
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: transparent;
  --el-table-row-hover-bg-color: rgba(42, 157, 143, 0.05);
  --el-table-text-color: #2C3E50;
}

/* 待办事项弹窗美化 */
.pending-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid rgba(42, 157, 143, 0.2);
  padding-bottom: 16px;
}

.custom-radio-group :deep(.el-radio-button__inner) {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  transition: all 0.3s;
}

.custom-radio-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #2A9D8F 0%, #0A7FFF 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

/* 表格行样式 */
:deep(.pending-row) {
  background: rgba(251, 191, 36, 0.05) !important;
}

:deep(.processed-row) {
  background: rgba(34, 197, 94, 0.05) !important;
  opacity: 0.8;
}

:deep(.pending-row:hover),
:deep(.processed-row:hover) {
  background: rgba(42, 157, 143, 0.1) !important;
}
</style> 