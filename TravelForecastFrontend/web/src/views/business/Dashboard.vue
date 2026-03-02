<template>
  <div class="dashboard-container">
    <!-- 数据概览卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
      <stat-card 
        title="今日游客" 
        :value="statisticsData.todayVisitors" 
        suffix="人次" 
        :change="statisticsData.visitorChange" 
        icon="User" 
        color="bg-gradient-to-r from-blue-500 to-cyan-400"
      />
      
      <stat-card 
        title="今日收入" 
        :value="statisticsData.todayRevenue" 
        suffix="元" 
        :change="statisticsData.revenueChange" 
        icon="Money" 
        color="bg-gradient-to-r from-green-500 to-teal-400"
      />
      
      <stat-card 
        title="在线售票" 
        :value="statisticsData.onlineTickets" 
        suffix="张" 
        :change="statisticsData.ticketChange" 
        icon="Ticket" 
        color="bg-gradient-to-r from-purple-500 to-indigo-400"
      />
      
      <stat-card 
        title="好评率" 
        :value="statisticsData.satisfactionRate" 
        suffix="%" 
        :change="statisticsData.satisfactionChange" 
        icon="Star" 
        color="bg-gradient-to-r from-yellow-500 to-orange-400"
      />
    </div>
    
    <!-- 主要图表区域 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
      <div class="lg:col-span-2">
        <holographic-card>
          <template #header>
            <div class="flex justify-between items-center p-4">
              <h2 class="text-xl font-bold text-[#2A9D8F]">游客流量趋势</h2>
              <div>
                <el-radio-group v-model="visitorTimeRange" size="small">
                  <el-radio-button label="day">今日</el-radio-button>
                  <el-radio-button label="week">本周</el-radio-button>
                  <el-radio-button label="month">本月</el-radio-button>
                </el-radio-group>
              </div>
            </div>
          </template>
          <div style="height: 350px; width: 100%;">
            <div id="visitor-chart" ref="visitorChartRef" style="height: 100%; width: 100%;"></div>
          </div>
        </holographic-card>
      </div>
      
      <div>
        <holographic-card>
          <template #header>
            <div class="flex justify-between items-center p-4">
              <h2 class="text-xl font-bold text-[#2A9D8F]">收入构成</h2>
              <el-select v-model="revenueTimeRange" size="small">
                <el-option label="今日" value="day" />
                <el-option label="本周" value="week" />
                <el-option label="本月" value="month" />
              </el-select>
            </div>
          </template>
          <div style="height: 350px; width: 100%;">
            <div id="revenue-chart" ref="revenueChartRef" style="height: 100%; width: 100%;"></div>
          </div>
        </holographic-card>
      </div>
    </div>
    
    <!-- 二级图表区域 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
      <holographic-card>
        <template #header>
          <div class="flex justify-between items-center p-4">
            <h2 class="text-xl font-bold text-[#2A9D8F]">景点热度排行</h2>
          </div>
        </template>
        <div class="p-4 pt-0">
          <el-table :data="scenicRankData" style="width: 100%" height="350" :header-cell-style="{ background: 'transparent', color: '#606266' }">
            <el-table-column prop="rank" label="排名" width="70">
              <template #default="{ row }">
                <div class="flex items-center justify-center">
                  <div :class="`w-6 h-6 rounded-full flex items-center justify-center ${getRankClass(row.rank)}`">
                    {{ row.rank }}
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="景点名称">
              <template #default="{ row }">
                <div class="flex items-center">
                  <div v-if="row.image" class="w-8 h-8 rounded mr-2 bg-gray-200 flex items-center justify-center">
                    <el-image :src="row.image" class="w-8 h-8 rounded" fit="cover" />
                  </div>
                  <div v-else class="w-8 h-8 rounded mr-2 bg-gray-200 flex items-center justify-center text-gray-600">
                    {{ row.name.substring(0, 1) }}
                  </div>
                  <span>{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="visitors" label="收藏数" sortable />
            <el-table-column prop="rate" label="增长率">
              <template #default="{ row }">
                <div class="flex items-center">
                  <el-icon :class="row.rate >= 0 ? 'text-green-500' : 'text-red-500'" class="mr-1">
                    <ArrowUp v-if="row.rate >= 0" />
                    <ArrowDown v-else />
                  </el-icon>
                  <span :class="row.rate >= 0 ? 'text-green-500' : 'text-red-500'">
                    {{ Math.abs(row.rate) }}%
                  </span>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </holographic-card>
      
      <holographic-card>
        <template #header>
          <div class="flex justify-between items-center p-4">
            <h2 class="text-xl font-bold text-[#2A9D8F]">实时公告</h2>
            <el-button type="primary" plain size="small" @click="openAnnouncementDialog">管理公告</el-button>
          </div>
        </template>
        <div class="p-4 pt-0">
          <div v-if="announcementData.length > 0" class="space-y-4 max-h-[350px] overflow-y-auto px-1">
            <div 
              v-for="announcement in announcementData.slice(0, 3)" 
              :key="announcement.id" 
              class="border-b border-gray-200 pb-4 last:border-0"
            >
              <div class="flex justify-between items-start mb-2">
                <div class="flex items-center">
                  <el-icon 
                    class="text-xl mr-2"
                    :class="{
                      'text-red-500': announcement.type === 'urgent',
                      'text-yellow-500': announcement.type === 'important',
                      'text-[#2A9D8F]': announcement.type === 'normal'
                    }"
                  >
                    <component :is="getAnnouncementIcon(announcement.type)" />
                  </el-icon>
                  <div>
                    <div class="text-gray-800 text-sm font-medium">{{ announcement.title }}</div>
                    <el-tag 
                      v-if="announcement.type !== 'normal'"
                      :type="announcement.type === 'urgent' ? 'danger' : 'warning'"
                      size="small"
                      effect="dark"
                      class="mt-1"
                    >
                      {{ announcement.type === 'urgent' ? '紧急' : '重要' }}
                    </el-tag>
                  </div>
                </div>
                <el-tag 
                  :type="announcement.status === 'active' ? 'success' : 'info'"
                  size="small"
                  effect="dark"
                >
                  {{ announcement.status === 'active' ? '启用中' : '已停用' }}
                </el-tag>
              </div>
              <p class="text-gray-600 text-sm mb-2">{{ announcement.content }}</p>
              <div class="flex justify-between items-center">
                <div class="text-gray-500 text-xs">
                  {{ formatDate(announcement.createdAt) }}
                </div>
                <div class="flex space-x-3">
                  <el-button size="small" text @click="handleEditAnnouncement(announcement)">编辑</el-button>
                  <el-button size="small" text type="danger" @click="handleDeleteAnnouncement(announcement.id)">删除</el-button>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="text-center py-8">
            <el-icon class="text-4xl text-gray-600 mb-2"><Bell /></el-icon>
            <p class="text-gray-500 text-sm">暂无公告，点击右上角添加</p>
          </div>
        </div>
      </holographic-card>
    
    <!-- 实时公告管理弹窗 -->
    <el-dialog 
      v-model="announcementDialogVisible" 
      width="1100px" 
      :close-on-click-modal="false"
      class="announcement-dialog"
    >
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center">
            <el-icon class="text-2xl text-[#2A9D8F] mr-2"><Bell /></el-icon>
            <span class="text-xl font-bold">实时公告管理</span>
          </div>
          <el-button type="primary" @click="openAddAnnouncementDialog">
            <el-icon class="mr-1"><Plus /></el-icon>发布公告
          </el-button>
        </div>
      </template>
      
      <el-table 
        :data="announcementData" 
        style="width: 100%" 
        max-height="500"
      >
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="250" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag 
              size="small"
              :type="row.type === 'urgent' ? 'danger' : (row.type === 'important' ? 'warning' : 'info')"
              effect="dark"
            >
              {{ row.type === 'urgent' ? '紧急' : (row.type === 'important' ? '重要' : '普通') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag 
              size="small" 
              :type="row.status === 'active' ? 'success' : 'info'"
              effect="dark"
            >
              {{ row.status === 'active' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            <div class="text-sm text-gray-500">
              {{ formatDate(row.createdAt) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2">
              <el-button size="small" text @click="handleEditAnnouncement(row)">编辑</el-button>
              <el-button size="small" text type="danger" @click="handleDeleteAnnouncement(row.id)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
    
    <!-- 添加/编辑公告弹窗 -->
    <el-dialog 
      v-model="announcementFormDialogVisible" 
      :title="isEditingAnnouncement ? '编辑公告' : '发布公告'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="announcementForm" :rules="announcementRules" ref="announcementFormRef" label-width="100px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="announcementForm.title" placeholder="请输入公告标题" />
        </el-form-item>
        
        <el-form-item label="公告内容" prop="content">
          <el-input
            v-model="announcementForm.content"
            type="textarea"
            :rows="5"
            placeholder="请输入公告内容"
          />
        </el-form-item>
        
        <el-form-item label="公告类型" prop="type">
          <el-radio-group v-model="announcementForm.type">
            <el-radio value="normal">普通</el-radio>
            <el-radio value="important">重要</el-radio>
            <el-radio value="urgent">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="announcementForm.priority" :min="0" :max="100" />
          <span class="text-xs text-gray-500 ml-2">数字越大越靠前</span>
        </el-form-item>
        
        <el-form-item label="显示时间">
          <el-col :span="11">
            <el-date-picker
              v-model="announcementForm.startTime"
              type="datetime"
              placeholder="开始时间"
              style="width: 100%"
            />
          </el-col>
          <el-col :span="2" class="text-center">
            <span class="text-gray-500">至</span>
          </el-col>
          <el-col :span="11">
            <el-date-picker
              v-model="announcementForm.endTime"
              type="datetime"
              placeholder="结束时间"
              style="width: 100%"
            />
          </el-col>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="announcementFormDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAnnouncementForm">{{ isEditingAnnouncement ? '保存' : '发布' }}</el-button>
      </template>
    </el-dialog>
    
    </div>
    
    <!-- 待办事项和公告 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <holographic-card class="lg:col-span-2">
        <template #header>
          <div class="flex justify-between items-center p-4">
            <h2 class="text-xl font-bold text-[#2A9D8F]">待办事项</h2>
            <div class="flex items-center space-x-2">
              <el-button type="primary" plain size="small" @click="openTodoDialog">查看全部</el-button>
              <el-button type="primary" plain size="small" @click="openAddTodoDialog">添加任务</el-button>
            </div>
          </div>
        </template>
        <div class="p-4 pt-0">
          <el-table :data="todoData.slice(0, 5)" style="width: 100%" height="250" :header-cell-style="{ background: 'transparent', color: '#606266' }">
            <el-table-column width="50">
              <template #default="{ row }">
                <el-checkbox v-model="row.completed" @change="handleToggleTodo(row)" />
              </template>
            </el-table-column>
            <el-table-column prop="priority" label="优先级" width="100">
              <template #default="{ row }">
                <el-tag :type="getPriorityType(row.priority)" size="small" effect="dark">
                  {{ getPriorityLabel(row.priority) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="任务">
              <template #default="{ row }">
                <span :class="{ 'line-through text-gray-500': row.completed }">{{ row.title }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="deadline" label="截止时间" width="150" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <div class="flex space-x-2">
                  <el-button size="small" text @click="handleEditTodo(row)">编辑</el-button>
                  <el-button size="small" text type="danger" @click="handleDeleteTodo(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </holographic-card>
    
    <!-- 待办事项查看全部弹窗 -->
    <el-dialog 
      v-model="todoDialogVisible" 
      width="1000px" 
      :close-on-click-modal="false"
      class="todo-dialog"
    >
      <template #header>
        <div class="flex items-center">
          <el-icon class="text-2xl text-[#2A9D8F] mr-2"><List /></el-icon>
          <span class="text-xl font-bold">待办事项管理</span>
        </div>
      </template>
      
      <div class="flex items-center justify-between mb-4 p-4 rounded-lg bg-gradient-to-r from-gray-50 to-gray-100 border border-gray-200">
        <el-radio-group v-model="todoFilter" size="default" class="custom-radio-group">
          <el-radio-button label="all">
            <el-icon class="mr-1"><List /></el-icon>全部
          </el-radio-button>
          <el-radio-button label="pending">
            <el-icon class="mr-1"><Clock /></el-icon>未完成
          </el-radio-button>
          <el-radio-button label="completed">
            <el-icon class="mr-1"><CircleCheck /></el-icon>已完成
          </el-radio-button>
        </el-radio-group>
        <div class="flex items-center space-x-4">
          <div class="text-sm">
            <el-icon class="text-orange-400 mr-1"><Warning /></el-icon>
            <span class="text-gray-600">未完成：</span>
            <span class="text-orange-500 font-bold">{{ todoData.filter(i => !i.completed).length }}</span>
          </div>
          <div class="text-sm">
            <el-icon class="text-cyan-400 mr-1"><Document /></el-icon>
            <span class="text-gray-600">总数：</span>
            <span class="text-[#2A9D8F] font-bold">{{ todoData.length }}</span>
          </div>
          <el-button type="primary" size="small" @click="openAddTodoDialog">
            <el-icon class="mr-1"><Plus /></el-icon>添加任务
          </el-button>
        </div>
      </div>
      
      <el-table 
        :data="filteredTodoData" 
        style="width: 100%" 
        max-height="500"
        :row-class-name="todoRowClassName"
      >
        <el-table-column width="60" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.completed" @change="handleToggleTodo(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="任务标题" min-width="250">
          <template #default="{ row }">
            <div class="flex items-center space-x-2">
              <el-icon class="text-base flex-shrink-0" :class="row.completed ? 'text-green-400' : 'text-orange-400'">
                <FolderOpened v-if="!row.completed" />
                <FolderChecked v-else />
              </el-icon>
              <span :class="{ 'line-through text-gray-500': row.completed }" class="flex-1">{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)" size="small" effect="dark">
              {{ getPriorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deadline" label="截止时间" width="180">
          <template #default="{ row }">
            <div class="flex items-center text-sm text-gray-500 space-x-1">
              <el-icon class="text-sm flex-shrink-0"><Timer /></el-icon>
              <span>{{ row.deadline }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="completed" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag 
              size="small" 
              :type="row.completed ? 'success' : 'warning'"
              effect="dark"
            >
              <el-icon class="mr-1">
                <CircleCheck v-if="row.completed" />
                <Clock v-else />
              </el-icon>
              {{ row.completed ? '已完成' : '未完成' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center space-x-1">
              <el-button size="small" text @click="handleEditTodo(row)">编辑</el-button>
              <el-button size="small" text type="danger" @click="handleDeleteTodo(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
    
    <!-- 添加/编辑任务弹窗 -->
    <el-dialog 
      v-model="addTodoVisible" 
      :title="editingTodo ? '编辑任务' : '添加任务'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="todoForm" :rules="todoRules" ref="todoFormRef" label-width="90px">
        <el-form-item label="任务标题" prop="title">
          <el-input v-model="todoForm.title" placeholder="请输入任务标题" />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="todoForm.priority" placeholder="请选择优先级" style="width: 100%">
            <el-option label="紧急" value="urgent" />
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间" prop="deadline">
          <el-date-picker
            v-model="todoForm.deadline"
            type="date"
            placeholder="请选择截止时间"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addTodoVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTodo">确定</el-button>
      </template>
    </el-dialog>
      
      <holographic-card>
        <template #header>
          <div class="flex justify-between items-center p-4">
            <h2 class="text-xl font-bold text-[#2A9D8F]">系统通知</h2>
          </div>
        </template>
        <div class="p-4 pt-0">
          <div class="space-y-4 max-h-[250px] overflow-y-auto px-1">
            <div v-for="(notice, index) in noticeData" :key="index" class="border-l-2 pl-4 pb-4" :class="getNoticeBorderClass(notice.type)">
              <div class="flex items-center justify-between">
                <div class="flex items-center">
                  <el-icon :class="getNoticeIconClass(notice.type)" class="mr-2">
                    <component :is="getNoticeIcon(notice.type)" />
                  </el-icon>
                  <span class="text-gray-800">{{ notice.title }}</span>
                </div>
                <span class="text-gray-500 text-xs">{{ notice.time }}</span>
              </div>
              <p class="text-gray-500 text-sm mt-1">{{ notice.content }}</p>
            </div>
          </div>
        </div>
      </holographic-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch, onBeforeUnmount, computed } from 'vue'
import { ArrowUp, ArrowDown, Warning, Bell, InfoFilled, List, Clock, CircleCheck, Document, Plus, FolderOpened, FolderChecked, Timer } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import HolographicCard from '@/components/HolographicCard.vue'
import StatCard from '@/components/StatCard.vue'
import * as echarts from 'echarts'
import { getAnalysisOverview, getVisitorTrend, getRevenueAnalysis, getSpotRanking } from '@/api/merchant-analysis'
import { getScenicTrend, getHourlyDistribution } from '@/api/prediction'
import { getMerchantAnnouncements, createAnnouncement, updateAnnouncement, deleteAnnouncement } from '@/api/announcement'
import { getTodos, createTodo, updateTodo, toggleTodoComplete, deleteTodo } from '@/api/todo'
import { getMerchantNotifications, markNotificationAsRead } from '@/api/notification'

// 数据过滤选择器
const visitorTimeRange = ref('week')  // 默认显示本周
const revenueTimeRange = ref('day')

// 图表引用
const visitorChartRef = ref(null)
const revenueChartRef = ref(null)
let visitorChart: any = null
let revenueChart: any = null

// 商家景区ID（从localStorage获取或使用默认值）
const merchantScenicId = computed(() => {
  // 尝试从localStorage获取用户信息
  try {
    const userInfo = localStorage.getItem('userInfo')
    if (userInfo) {
      const user = JSON.parse(userInfo)
      // 如果有scenicId字段，使用它
      if (user.scenicId) {
        return user.scenicId
      }
    }
  } catch (error) {
    console.warn('获取用户信息失败:', error)
  }
  
  // 默认使用梅花山景区（ID=1）
  // 实际项目中，每个商家登录后应该保存自己的景区ID
  return 1
})

// 窗口大小调整处理函数
const handleResize = () => {
  if (visitorChart) {
    visitorChart.resize()
  }
  if (revenueChart) {
    revenueChart.resize()
  }
}

// 统计数据
const statisticsData = ref({
  todayVisitors: 0,
  visitorChange: 0,
  todayRevenue: 0,
  revenueChange: 0,
  onlineTickets: 0,
  ticketChange: 0,
  satisfactionRate: 0,
  satisfactionChange: 0
})

// 加载统计概览数据
const loadOverviewData = async () => {
  try {
    // 默认获取过去30天的数据
    const end = new Date()
    const start = new Date()
    start.setDate(end.getDate() - 30)
    
    const startDate = start.toISOString().split('T')[0]
    const endDate = end.toISOString().split('T')[0]
    
    const res: any = await getAnalysisOverview(merchantScenicId.value, startDate, endDate)
    const data = res?.data || res
    
    if (data) {
      statisticsData.value = {
        todayVisitors: data.totalVisitors || data.todayVisitors || 0,
        visitorChange: data.visitorChange || 0,
        todayRevenue: data.totalRevenue || data.todayRevenue || 0,
        revenueChange: data.revenueChange || 0,
        onlineTickets: data.onlineTickets || 0,
        ticketChange: data.ticketChange || 0,
        satisfactionRate: data.satisfactionRate || data.returnRate || 0,
        satisfactionChange: data.satisfactionChange || data.returnRateChange || 0
      }
    }
  } catch (error) {
    console.error('加载统计概览失败:', error)
  }
}

// 景点排行数据
const scenicRankData = ref<any[]>([])

// 加载景点热度排行 (使用商家专属接口)
const loadScenicRank = async () => {
  try {
    // 默认本月数据
    const end = new Date()
    const start = new Date()
    start.setDate(1) // 本月1号
    
    const startDate = start.toISOString().split('T')[0]
    const endDate = end.toISOString().split('T')[0]

    const res: any = await getSpotRanking(merchantScenicId.value, startDate, endDate) 
    const spots = res?.data?.spots || res?.spots || res?.data || []
    
    if (Array.isArray(spots)) {
      // 转换为表格格式
      scenicRankData.value = spots.map((spot: any, index: number) => {
        return {
          rank: index + 1,
          name: spot.name,
          image: spot.image,
          visitors: spot.visitors || spot.visitorCount || 0, 
          rate: spot.trend || spot.growthRate || 0 
        }
      }).slice(0, 5) // 只显示前5名
    }
  } catch (error) {
    console.error('加载景点热度排行失败', error)
  }
}

// 公告数据
const announcementData = ref<any[]>([])

// 公告弹窗
const announcementDialogVisible = ref(false)
const announcementFormDialogVisible = ref(false)
const announcementFormRef = ref<FormInstance>()
const isEditingAnnouncement = ref(false)

// 公告表单
const announcementForm = ref({
  id: null as number | null,
  title: '',
  content: '',
  type: 'normal',
  priority: 0,
  startTime: null as Date | null,
  endTime: null as Date | null
})

// 公告表单验证规则
const announcementRules: FormRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  type: [{ required: true, message: '请选择公告类型', trigger: 'change' }]
}

// 时间格式化
const formatDate = (value: any) => {
  if (!value) return ''
  const d = new Date(value)
  if (isNaN(d.getTime())) return String(value)
  const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`)
  const Y = d.getFullYear()
  const M = pad(d.getMonth() + 1)
  const D = pad(d.getDate())
  const h = pad(d.getHours())
  const m = pad(d.getMinutes())
  return `${Y}-${M}-${D} ${h}:${m}`
}

// 加载公告数据
const loadAnnouncements = async () => {
  try {
    const res: any = await getMerchantAnnouncements({ page: 1, size: 100 })
    const data = res?.data || res
    announcementData.value = data.announcements || []
    console.log('加载公告数据:', announcementData.value)
  } catch (error) {
    console.error('加载公告失败:', error)
    ElMessage.error('加载公告失败')
  }
}

// 打开公告管理弹窗
const openAnnouncementDialog = () => {
  announcementDialogVisible.value = true
  loadAnnouncements()
}

// 打开添加公告弹窗
const openAddAnnouncementDialog = () => {
  isEditingAnnouncement.value = false
  announcementForm.value = {
    id: null,
    title: '',
    content: '',
    type: 'normal',
    priority: 0,
    startTime: null,
    endTime: null
  }
  announcementFormDialogVisible.value = true
}

// 编辑公告
const handleEditAnnouncement = (announcement: any) => {
  isEditingAnnouncement.value = true
  announcementForm.value = {
    id: announcement.id,
    title: announcement.title,
    content: announcement.content,
    type: announcement.type,
    priority: announcement.priority,
    startTime: announcement.startTime ? new Date(announcement.startTime) : null,
    endTime: announcement.endTime ? new Date(announcement.endTime) : null
  }
  announcementFormDialogVisible.value = true
}

// 删除公告
const handleDeleteAnnouncement = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条公告吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteAnnouncement(id)
    ElMessage.success('删除成功')
    await loadAnnouncements()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除公告失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 提交公告表单
const submitAnnouncementForm = async () => {
  await announcementFormRef.value?.validate(async (valid) => {
    if (!valid) return
    
    try {
      const formData = {
        ...announcementForm.value,
        startTime: announcementForm.value.startTime ? announcementForm.value.startTime.toISOString() : undefined,
        endTime: announcementForm.value.endTime ? announcementForm.value.endTime.toISOString() : undefined
      }
      
      if (isEditingAnnouncement.value && announcementForm.value.id) {
        await updateAnnouncement(announcementForm.value.id, formData)
        ElMessage.success('更新成功')
      } else {
        await createAnnouncement(formData)
        ElMessage.success('发布成功')
      }
      
      announcementFormDialogVisible.value = false
      await loadAnnouncements()
    } catch (error) {
      console.error('保存公告失败:', error)
      ElMessage.error('保存失败')
    }
  })
}

// 获取公告图标
const getAnnouncementIcon = (type: string) => {
  const icons: Record<string, any> = {
    urgent: Warning,
    important: InfoFilled,
    normal: Bell
  }
  return icons[type] || Bell
}

// 已移除客户反馈相关代码

// 待办事项
const todoData = ref<any[]>([])

// 待办事项弹窗
const todoDialogVisible = ref(false)
const todoFilter = ref('all')

// 添加/编辑任务弹窗
const addTodoVisible = ref(false)
const todoFormRef = ref<FormInstance>()
const editingTodo = ref<any>(null)

const todoForm = ref({
  title: '',
  priority: 'medium',
  deadline: ''
})

const todoRules: FormRules = {
  title: [{ required: true, message: '请输入任务标题', trigger: 'blur' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  deadline: [{ required: true, message: '请选择截止时间', trigger: 'change' }]
}

// 过滤后的待办数据
const filteredTodoData = computed(() => {
  if (todoFilter.value === 'pending') {
    return todoData.value.filter(item => !item.completed)
  } else if (todoFilter.value === 'completed') {
    return todoData.value.filter(item => item.completed)
  }
  return todoData.value
})

// 加载待办事项
const loadTodos = async () => {
  try {
    const res: any = await getTodos()
    if (res.code === 200) {
      todoData.value = res.data
    }
  } catch (error) {
    console.error('加载待办事项失败:', error)
  }
}

// 打开待办事项弹窗
const openTodoDialog = () => {
  todoDialogVisible.value = true
}

// 打开添加任务弹窗
const openAddTodoDialog = () => {
  editingTodo.value = null
  todoForm.value = {
    title: '',
    priority: 'medium',
    deadline: ''
  }
  addTodoVisible.value = true
}

// 编辑任务
const handleEditTodo = (row: any) => {
  editingTodo.value = row
  todoForm.value = {
    title: row.title,
    priority: row.priority,
    deadline: row.deadline
  }
  addTodoVisible.value = true
}

// 删除任务
const handleDeleteTodo = async (row: any) => {
  ElMessageBox.confirm(`确认删除任务"${row.title}"？`, '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res: any = await deleteTodo(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadTodos()
      }
    } catch (error) {
      console.error('删除任务失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 切换任务状态
const handleToggleTodo = async (row: any) => {
  try {
    const res: any = await toggleTodoComplete(row.id)
    if (res.code === 200) {
      row.completed = !row.completed
      ElMessage.success(row.completed ? '已标记为完成' : '已标记为未完成')
    }
  } catch (error) {
    console.error('切换任务状态失败:', error)
    ElMessage.error('操作失败')
  }
}

// 提交任务
const submitTodo = () => {
  todoFormRef.value?.validate(async (valid) => {
    if (valid) {
      try {
        if (editingTodo.value) {
          // 编辑模式
          const res: any = await updateTodo(editingTodo.value.id, todoForm.value)
          if (res.code === 200) {
            ElMessage.success('修改成功')
            loadTodos()
          }
        } else {
          // 添加模式
          const res: any = await createTodo(todoForm.value)
          if (res.code === 200) {
            ElMessage.success('添加成功')
            loadTodos()
          }
        }
        addTodoVisible.value = false
      } catch (error) {
        console.error('提交任务失败:', error)
        ElMessage.error('操作失败')
      }
    }
  })
}

// 表格行类名
const todoRowClassName = ({ row }: { row: any }) => {
  return row.completed ? 'completed-row' : 'pending-row'
}

// 系统通知
const noticeData = ref<any[]>([])

// 加载系统通知
const loadNotifications = async () => {
  try {
    const res: any = await getMerchantNotifications()
    if (res.code === 200) {
      noticeData.value = res.data.map((n: any) => ({
        id: n.id,
        type: n.type,
        title: n.title,
        content: n.content,
        time: n.timeAgo,
        isRead: n.isRead
      }))
    }
  } catch (error) {
    console.error('加载系统通知失败:', error)
  }
}

// 标记通知为已读（暂未使用，预留功能）
const handleNotificationRead = async (notification: any) => {
  if (notification.isRead) return
  
  try {
    const res: any = await markNotificationAsRead(notification.id)
    if (res.code === 200) {
      notification.isRead = true
    }
  } catch (error) {
    console.error('标记通知已读失败:', error)
  }
}

// 注意：watch 已在文件末尾定义，这里不再重复定义

onMounted(() => {
  // 加载统计概览
  loadOverviewData()
  // 加载景点热度排行数据
  loadScenicRank()
  // 加载游客流量趋势
  loadVisitorTrend()
  // 加载收入分析
  loadRevenueAnalysis()
  // 加载公告数据
  loadAnnouncements()
  // 加载待办事项
  loadTodos()
  // 加载系统通知
  loadNotifications()
  
  nextTick(() => {
    // 确保 DOM 也就绪后再初始化空图表，如果有数据会通过 watcher 或后续逻辑更新
    setTimeout(() => {
      initVisitorChart()
      initRevenueChart()
    }, 500)
  })
  
  window.addEventListener('resize', handleResize)
})

// 组件卸载时清理事件监听和图表实例
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  
  if (visitorChart) {
    visitorChart.dispose()
    visitorChart = null
  }
  
  if (revenueChart) {
    revenueChart.dispose()
    revenueChart = null
  }
})

// 获取排名样式
const getRankClass = (rank: number) => {
  if (rank === 1) return 'bg-yellow-500 text-white'
  if (rank === 2) return 'bg-gray-400 text-white'
  if (rank === 3) return 'bg-amber-700 text-white'
  return 'bg-gray-200 text-gray-500'
}

// 获取优先级类型
const getPriorityType = (priority: string) => {
  const types: Record<string, string> = {
    'urgent': 'danger',
    'high': 'warning',
    'medium': 'info',
    'low': 'success'
  }
  return types[priority] || 'info'
}

// 获取优先级标签
const getPriorityLabel = (priority: string) => {
  const labels: Record<string, string> = {
    'urgent': '紧急',
    'high': '高',
    'medium': '中',
    'low': '低'
  }
  return labels[priority] || '未知'
}

// 获取通知边框类
const getNoticeBorderClass = (type: string) => {
  const classes: Record<string, string> = {
    'warning': 'border-yellow-500',
    'info': 'border-blue-500',
    'success': 'border-green-500',
    'error': 'border-red-500'
  }
  return classes[type] || 'border-gray-500'
}

// 获取通知图标类
const getNoticeIconClass = (type: string) => {
  const classes: Record<string, string> = {
    'warning': 'text-yellow-500',
    'info': 'text-blue-500',
    'success': 'text-green-500',
    'error': 'text-red-500'
  }
  return classes[type] || 'text-gray-500'
}

// 获取通知图标
const getNoticeIcon = (type: string) => {
  const icons: Record<string, any> = {
    'warning': Warning,
    'info': InfoFilled,
    'success': 'SuccessFilled',
    'error': 'CircleCloseFilled'
  }
  return icons[type] || InfoFilled
}

// 游客流量数据
const visitorTrendData = ref<any[]>([])
const predictionTrendData = ref<any[]>([])

// 加载游客流量趋势
const loadVisitorTrend = async () => {
  try {
    const end = new Date()
    const start = new Date()
    let days = 7
    
    // 根据选择的时间范围计算开始时间
    if (visitorTimeRange.value === 'week') {
      start.setDate(end.getDate() - 7)
      days = 7
    } else if (visitorTimeRange.value === 'month') {
      start.setDate(end.getDate() - 30)
      days = 30
    } else {
      // 默认为当天
      start.setDate(end.getDate())
      days = 1 
    }
    
    const startDate = start.toISOString().split('T')[0]
    const endDate = end.toISOString().split('T')[0]

    // 并行获取历史数据和预测数据
    const [historyRes, predictionRes] = await Promise.all([
      getVisitorTrend(merchantScenicId.value, startDate, endDate, visitorTimeRange.value),
      visitorTimeRange.value === 'day' 
        ? getHourlyDistribution(endDate, merchantScenicId.value)
        : getScenicTrend(merchantScenicId.value, undefined, days) 
    ])

    // 处理历史数据
    const histRaw = (historyRes as any)?.data || historyRes || {}
    visitorTrendData.value = []
    
    if (histRaw.labels && histRaw.data) {
       visitorTrendData.value = histRaw.labels.map((label: string, idx: number) => ({
         date: label,
         visitors: histRaw.data[idx]
       }))
    } else if (Array.isArray(histRaw)) {
       visitorTrendData.value = histRaw
    }
    
    // 处理预测数据
    const predRaw = (predictionRes as any)?.data || predictionRes || {}
    predictionTrendData.value = []
    
    if (visitorTimeRange.value === 'day') {
      if (predRaw.hourlyData) {
        predictionTrendData.value = predRaw.hourlyData.map((h: any) => ({
          date: h.hour,
          visitors: h.expectedFlow
        }))
      }
    } else {
      // 兼容两种格式：对象数组或平行数组
      if (Array.isArray(predRaw.predictions) && predRaw.predictions.length > 0 && typeof predRaw.predictions[0] === 'object') {
         predictionTrendData.value = predRaw.predictions.map((p: any) => ({
           date: p.date, 
           visitors: p.expectedFlow
         }))
      } else if (predRaw.dates && predRaw.predictions) {
        predictionTrendData.value = predRaw.dates.map((date: string, idx: number) => ({
          date: date,
          visitors: predRaw.predictions[idx]
        }))
      }
    }
    
    // 如果图表已初始化，更新数据
    if (visitorChart) {
      initVisitorChart()
    }
  } catch (error) {
    console.error('加载游客流量趋势失败:', error)
  }
}

// 收入分析数据
const revenueAnalysisData = ref<any[]>([])

// 加载收入分析
const loadRevenueAnalysis = async () => {
  try {
    const end = new Date()
    const start = new Date()
    
    // 根据选择的时间范围计算开始时间
    if (revenueTimeRange.value === 'week') {
      start.setDate(end.getDate() - 7)
    } else if (revenueTimeRange.value === 'month') {
      start.setDate(end.getDate() - 30)
    } else {
      // 默认为当天
      start.setDate(end.getDate())
    }
    
    const startDate = start.toISOString().split('T')[0]
    const endDate = end.toISOString().split('T')[0]

    const res: any = await getRevenueAnalysis(merchantScenicId.value, startDate, endDate, 'all')
    const rawData = res?.data || res || {}
    revenueAnalysisData.value = []
    
    if (rawData.labels && rawData.data) {
       revenueAnalysisData.value = rawData.labels.map((label: string, idx: number) => ({
         date: label,
         amount: rawData.data[idx]
       }))
    }
    
    if (revenueChart) {
      initRevenueChart()
    }
  } catch (error) {
    console.error('加载收入分析失败:', error)
  }
}

// 初始化游客流量趋势图
const initVisitorChart = () => {
  const chartDom = document.getElementById('visitor-chart')
  if (!chartDom) return
  
  if (visitorChart) visitorChart.dispose()
  visitorChart = echarts.init(chartDom)
  
  // 辅助函数：格式化日期为 MM/DD (字符串处理版，避免时区问题)
  const normalizeDate = (dateStr: string) => {
    if (!dateStr) return ''
    // 如果包含 "月" 或 "周"，尝试提取数字
    if (dateStr.includes('月') && dateStr.includes('日')) {
       return dateStr.replace('月', '/').replace('日', '')
    }
    if (dateStr.includes('月') || dateStr.includes('周')) return dateStr
    
    // 处理 YYYY-MM-DD
    if (dateStr.includes('-')) {
      const parts = dateStr.split('-')
      if (parts.length === 3) {
        return `${Number(parts[1])}/${Number(parts[2])}` // 去除前导0
      }
    }
    
    // 处理 YYYY/MM/DD
    if (dateStr.includes('/')) {
       const parts = dateStr.split('/')
       if (parts.length === 3) {
         return `${Number(parts[1])}/${Number(parts[2])}`
       }
       if (parts.length === 2) return dateStr // 已经是 M/D
    }
    
    return dateStr
  }
  
  // 处理历史数据
  const histData = visitorTrendData.value || []
  const histMap = new Map()
  histData.forEach((item: any) => {
    const rawDate = item.date || item.time || item.hour
    const key = normalizeDate(rawDate)
    histMap.set(key, item.visitors || item.count || 0)
  })
  
  // 处理预测数据
  const predData = predictionTrendData.value || []
  const predMap = new Map()
  predData.forEach((item: any) => {
    const rawDate = item.date
    const key = normalizeDate(rawDate)
    predMap.set(key, item.visitors)
  })
  
  // 合并所有日期并去重排序
  const allDates = [...new Set([...histMap.keys(), ...predMap.keys()])].sort((a, b) => {
    try {
      if (a.includes('月')) return a.localeCompare(b)
      const [m1, d1] = a.split('/').map(Number)
      const [m2, d2] = b.split('/').map(Number)
      if (m1 !== m2) return m1 - m2
      return d1 - d2
    } catch (e) {
      return a.localeCompare(b)
    }
  })
  
  // 调试信息：仅输出到控制台，避免打断正常渲染
  console.log('Final Dates:', allDates, {
    historyCount: histData.length,
    predictionCount: predData.length,
    firstHistoryDate: histData[0]?.date ?? 'None',
    firstPredictionDate: predData[0]?.date ?? 'None'
  })
  
  // 映射数据到X轴
  const finalHistData = allDates.map(date => {
    return histMap.has(date) ? histMap.get(date) : null
  })
  
  const finalPredData = allDates.map(date => {
    return predMap.has(date) ? predMap.get(date) : null
  })

  // 补全连接点：如果历史数据的最后一个点也是预测数据的第一个点（日期相同），则无缝连接。
  // 如果日期不同（如昨天是历史，今天是预测），ECharts 的 connectNulls: true 会自动连接
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#FFFFFF',
      borderColor: '#EBEEF5',
      textStyle: { color: '#303133' }
    },
    legend: {
      data: ['历史客流', '预测趋势'],
      textStyle: { color: '#606266' },
      bottom: 0
    },
    grid: {
      top: '15%',
      left: '3%',
      right: '4%',
      bottom: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: allDates,
      axisLine: { lineStyle: { color: '#E4E7ED' } },
      axisLabel: { color: '#909399' }
    },
    yAxis: {
      type: 'value',
      name: '游客数量 (人)',
      nameTextStyle: { color: '#909399' },
      splitLine: { lineStyle: { color: '#F0F2F5', type: 'dashed' } },
      axisLabel: { color: '#909399' }
    },
    series: [
      {
        name: '历史客流',
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 3, color: '#22d3ee' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(34, 211, 238, 0.4)' },
            { offset: 1, color: 'rgba(34, 211, 238, 0.05)' }
          ])
        },
        data: finalHistData,
        connectNulls: true
      },
      {
        name: '预测趋势',
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 2, type: 'dashed', color: '#a855f7' },
        data: finalPredData,
        connectNulls: true
      }
    ]
  }
  
  visitorChart.setOption(option)
}

// 初始化收入构成图
const initRevenueChart = () => {
  const chartDom = document.getElementById('revenue-chart')
  if (!chartDom) return
  
  if (revenueChart) revenueChart.dispose()
  revenueChart = echarts.init(chartDom)
  
  const data = revenueAnalysisData.value || []
  const dates = data.map((item: any) => item.date)
  const amounts = data.map((item: any) => item.amount)

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#FFFFFF',
      borderColor: '#EBEEF5',
      textStyle: { color: '#303133' }
    },
    grid: {
      top: '15%',
      left: '3%',
      right: '4%',
      bottom: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#E4E7ED' } },
      axisLabel: { color: '#909399' }
    },
    yAxis: {
      type: 'value',
      name: '收入 (元)',
      nameTextStyle: { color: '#909399' },
      splitLine: { lineStyle: { color: '#F0F2F5', type: 'dashed' } },
      axisLabel: { color: '#909399' }
    },
    series: [
      {
        name: '收入',
        type: 'bar',
        barWidth: '40%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#34d399' },
            { offset: 1, color: '#059669' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        data: amounts
      }
    ]
  }
  
  revenueChart.setOption(option)
}

// 监听时间范围变化
watch(visitorTimeRange, () => {
  console.log('游客时间范围变化:', visitorTimeRange.value)
  nextTick(() => {
    if (visitorChart) {
      visitorChart.dispose()
      visitorChart = null
      nextTick(() => {
        initVisitorChart()
      })
    } else {
      initVisitorChart()
    }
  })
})

watch(revenueTimeRange, (newVal) => {
  console.log('收入时间范围变化:', newVal);
  
  // 强制清除旧图表
  if (revenueChart) {
    revenueChart.dispose();
    revenueChart = null;
  }
  
  // 确保DOM已更新
  nextTick(() => {
    console.log('开始重新初始化收入图表');
    initRevenueChart();
  });
})
</script>

<style scoped>
.dashboard-container {
  @apply space-y-6;
}

:deep(.el-table) {
  --el-table-border-color: #EBEEF5;
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: transparent;
  --el-table-row-hover-bg-color: rgba(42, 157, 143, 0.05);
  --el-table-text-color: #606266;
}

/* 待办事项弹窗样式 */
.todo-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #EBEEF5;
  padding-bottom: 16px;
}

.custom-radio-group :deep(.el-radio-button__inner) {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  transition: all 0.3s;
}

.custom-radio-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #2A9D8F 0%, #457B9D 100%);
  box-shadow: 0 0 10px rgba(42, 157, 143, 0.3);
}

/* 表格行样式 */
:deep(.pending-row) {
  background: rgba(251, 191, 36, 0.05) !important;
}

:deep(.completed-row) {
  background: rgba(34, 197, 94, 0.05) !important;
  opacity: 0.8;
}

:deep(.pending-row:hover),
:deep(.completed-row:hover) {
  background: rgba(42, 157, 143, 0.08) !important;
}

/* 客户反馈样式已删除 */
</style> 