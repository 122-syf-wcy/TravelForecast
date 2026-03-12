<template>
  <div class="trip-planning-container">
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
      <!-- 左侧面板 -->
      <div class="lg:col-span-4">
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
          <div class="flex items-center gap-2 mb-6 border-b border-gray-100 pb-4">
            <el-icon class="text-primary text-xl"><Setting /></el-icon>
            <h2 class="text-xl font-bold text-gray-900">行程规划</h2>
          </div>
          
          <el-form label-position="top">
            <el-form-item label="旅行时间">
              <el-date-picker
                v-model="tripDates"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                class="w-full"
                :disabled="isLoading"
                popper-class="light-theme-popper"
              />
            </el-form-item>
            
            <el-form-item label="旅行人数">
              <el-input-number v-model="peopleCount" :min="1" :max="20" class="w-full" :disabled="isLoading" />
            </el-form-item>
            
            <el-form-item label="出行方式">
              <el-radio-group v-model="transportation" :disabled="isLoading">
                <el-radio label="car" border>自驾</el-radio>
                <el-radio label="public" border>公共交通</el-radio>
                <el-radio label="tour" border>旅行团</el-radio>
              </el-radio-group>
            </el-form-item>
            
            <el-form-item label="预算范围">
              <el-slider
                v-model="budget"
                range
                :min="0"
                :max="10000"
                :step="500"
                :disabled="isLoading"
              />
              <div class="text-gray-500 flex justify-between text-sm mt-2">
                <span>¥{{ budget[0] }}</span>
                <span>¥{{ budget[1] }}</span>
              </div>
            </el-form-item>
            
            <el-form-item label="景点偏好">
              <el-select
                v-model="preferences"
                multiple
                placeholder="请选择景点偏好"
                class="w-full"
                :disabled="isLoading"
              >
                <el-option label="自然风景" value="nature" />
                <el-option label="历史人文" value="history" />
                <el-option label="特色美食" value="food" />
                <el-option label="休闲娱乐" value="leisure" />
                <el-option label="民俗体验" value="culture" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="景点选择">
              <el-checkbox-group v-model="selectedSpots" :disabled="isLoading || scenicLoading">
                <div class="grid grid-cols-1 gap-3">
                  <el-checkbox 
                    v-for="scenic in scenicSpots" 
                    :key="scenic.id" 
                    :label="scenic.code" 
                    class="text-gray-600"
                  >
                    {{ scenic.name }}
                  </el-checkbox>
                </div>
              </el-checkbox-group>
              <div v-if="scenicLoading" class="text-gray-500 text-sm mt-2">
                <el-icon class="is-loading"><Loading /></el-icon> 加载景区列表中...
              </div>
            </el-form-item>
            
            <el-form-item label="生成方式">
              <el-radio-group v-model="planningMode" :disabled="isLoading" class="flex flex-col space-y-2 w-full">
                <el-radio label="ai" border class="!mr-0 w-full">
                  <span class="flex items-center gap-2 justify-between w-full">
                    <span>🤖 AI智能规划（推荐）</span>
                    <el-tag type="warning" size="small" effect="plain" round>新功能</el-tag>
                  </span>
                </el-radio>
                <el-radio label="normal" border class="!mr-0 w-full">
                  <span class="flex items-center gap-2">📋 常规规划</span>
                </el-radio>
              </el-radio-group>
              <div v-if="planningMode === 'ai'" class="text-xs text-gray-500 mt-2 bg-blue-50 text-blue-600 p-2 rounded border border-blue-100">
                <el-icon class="mr-1"><MagicStick /></el-icon>
                使用通义千问AI智能优化行程，路线更合理，推荐更精准
              </div>
            </el-form-item>
            
            <div class="mt-8 pt-4 border-t border-gray-100">
              <el-button type="primary" class="w-full !h-12 !text-lg !rounded-xl shadow-lg shadow-blue-500/30 transition-all hover:scale-[1.02]" @click="generatePlan" :loading="isLoading" :disabled="isLoading">
                <span v-if="isLoading">{{ planningMode === 'ai' ? 'AI生成中...' : '生成中...' }}</span>
                <span v-else>{{ planningMode === 'ai' ? '🤖 AI智能生成' : '生成行程' }}</span>
              </el-button>
            </div>
          </el-form>
        </div>
        
        <!-- 客流预测卡片 -->
        <div v-if="tripPlan && flowPredictionData && Object.keys(flowPredictionData).length > 0" class="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mt-6">
          <div class="flex items-center justify-between mb-4 pb-3 border-b border-gray-100">
            <div class="flex items-center gap-2">
              <el-icon class="text-primary text-xl"><TrendCharts /></el-icon>
              <span class="text-lg font-bold text-gray-900">客流预测</span>
            </div>
            <el-tag type="success" size="small" effect="light" round>双流融合模型</el-tag>
          </div>
          
          <div class="flow-prediction-container">
            <div v-for="(predictions, spotName) in flowPredictionData" :key="spotName" class="spot-prediction mb-4 bg-gray-50 rounded-lg p-3 border border-gray-100">
              <div class="spot-name text-gray-800 font-bold mb-3 flex items-center gap-2">
                <el-icon class="text-primary"><Location /></el-icon>
                {{ spotName }}
              </div>
              <div class="prediction-bars">
                <div v-for="pred in predictions" :key="pred.date" class="prediction-item">
                  <div class="date-label text-xs text-gray-500">{{ formatPredictionDate(pred.date) }}</div>
                  <div class="bar-container">
                    <div 
                      class="bar" 
                      :class="getCrowdLevelClass(pred.crowdLevel)"
                      :style="{ width: getFlowBarWidth(pred.flow) + '%' }"
                    >
                      <span class="flow-value">{{ pred.flow }}人</span>
                    </div>
                  </div>
                  <div class="crowd-tag">
                    <el-tag 
                      :type="getCrowdTagType(pred.crowdLevel)" 
                      size="small" 
                      effect="dark"
                      class="crowd-level-tag"
                    >
                      {{ getCrowdLevelText(pred.crowdLevel) }}
                    </el-tag>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="legend mt-4 flex items-center justify-center gap-4 text-xs">
              <div class="flex items-center gap-1">
                <span class="legend-dot bg-green-500 rounded-full w-2.5 h-2.5"></span>
                <span class="text-gray-500">舒适(&lt;1500人)</span>
              </div>
              <div class="flex items-center gap-1">
                <span class="legend-dot bg-yellow-500 rounded-full w-2.5 h-2.5"></span>
                <span class="text-gray-500">适中(1500-3000人)</span>
              </div>
              <div class="flex items-center gap-1">
                <span class="legend-dot bg-red-500 rounded-full w-2.5 h-2.5"></span>
                <span class="text-gray-500">拥挤(&gt;3000人)</span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 行程预算卡片 -->
        <div v-if="tripPlan" class="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mt-6 budget-card">
          <div class="flex items-center gap-2 mb-4 pb-3 border-b border-gray-100">
            <el-icon class="text-primary text-xl"><Money /></el-icon>
            <h2 class="text-lg font-bold text-gray-900">行程预算</h2>
          </div>
          
          <div class="budget-content">
            <!-- 总预算展示 -->
            <div class="total-budget-section mb-6">
              <div class="total-budget-display">
                <div class="budget-amount">
                  <span class="currency">¥</span>
                  <span class="amount">{{ tripPlan.totalBudget }}</span>
                </div>
                <div class="budget-label text-gray-500 text-sm">预计总花费</div>
              </div>
              <div class="budget-progress mt-3">
                <el-progress 
                  :percentage="getBudgetPercentage()" 
                  :color="budgetColor"
                  :stroke-width="10"
                  :show-text="false"
                />
                <div class="progress-labels flex justify-between text-xs text-gray-500 mt-1">
                  <span>¥0</span>
                  <span>预算上限 ¥{{ budget[1] }}</span>
                </div>
              </div>
            </div>
            
            <!-- 费用明细 -->
            <div class="budget-breakdown">
              <div class="breakdown-item bg-gray-50 rounded-lg p-3 hover:bg-gray-100 transition-colors" v-for="(item, index) in budgetItems" :key="index">
                <div class="item-icon flex items-center justify-center w-8 h-8 rounded-lg text-lg" :style="{ backgroundColor: item.color + '20', color: item.color }">
                  <span>{{ item.icon }}</span>
                </div>
                <div class="item-info flex-1 ml-3">
                  <div class="flex justify-between items-center mb-1">
                    <div class="item-name text-gray-700 text-sm font-medium">{{ item.name }}</div>
                    <div class="item-value text-gray-900 font-bold">¥{{ item.value }}</div>
                  </div>
                  <div class="item-bar h-1.5 w-full bg-gray-200 rounded-full overflow-hidden">
                    <div class="bar-fill h-full rounded-full transition-all duration-500" :style="{ width: item.percentage + '%', backgroundColor: item.color }"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 右侧行程展示 -->
      <div class="lg:col-span-8">
        <!-- 加载状态 -->
        <div v-if="isLoading" class="h-96 flex items-center justify-center bg-white rounded-xl shadow-sm border border-gray-100">
          <div class="text-center">
            <el-icon class="text-5xl text-primary mb-4 is-loading"><Loading /></el-icon>
            <p class="text-gray-500">AI正在为您规划专属行程，请稍候...</p>
          </div>
        </div>
        
        <!-- 错误状态 -->
        <div v-else-if="hasError" class="h-96 flex items-center justify-center">
          <div class="text-center">
            <el-icon class="text-6xl text-red-500 mb-4"><Warning /></el-icon>
            <p class="text-red-400">{{ errorMessage }}</p>
            <neon-button class="mt-4" @click="generatePlan">重试</neon-button>
          </div>
        </div>
        
        <!-- 空状态 -->
        <div v-else-if="!tripPlan" class="h-96 flex items-center justify-center bg-white rounded-xl shadow-sm border border-gray-100">
          <div class="text-center">
            <el-icon class="text-6xl text-gray-500 mb-4"><Calendar /></el-icon>
            <p class="text-gray-500">请在左侧设置行程偏好，点击"生成行程"开始规划</p>
          </div>
        </div>
        
        <!-- 行程内容 -->
        <div v-else class="space-y-6">
          <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <div class="flex justify-between items-center mb-6 pb-4 border-b border-gray-100">
              <div class="flex items-center gap-3">
                <el-icon class="text-primary text-2xl"><MapLocation /></el-icon>
                <h2 class="text-xl font-bold text-gray-900">行程概览</h2>
              </div>
              <div class="flex gap-2">
                <el-button size="small" type="success" plain round @click="triggerPlanNarration">
                  <span class="mr-1">🎓</span>数字人讲解
                </el-button>
                <el-button size="small" type="primary" plain round @click="downloadPlan" icon="Download">下载行程</el-button>
              </div>
            </div>
            
            <div class="mb-6">
              <div class="flex items-center justify-between mb-4">
                <div>
                  <h3 class="text-gray-900 text-lg font-bold">六盘水精彩之旅</h3>
                  <p class="text-gray-500 mt-1 flex items-center gap-2">
                    <el-icon><Calendar /></el-icon>
                    {{ formatDate(tripDates[0]) }} - {{ formatDate(tripDates[1]) }}
                    <span class="mx-1">|</span>
                    共 {{ getDaysDiff() }} 天
                  </p>
                </div>
                <el-tag type="warning" effect="light" round>AI 智能推荐</el-tag>
              </div>
              
              <p class="text-gray-600 bg-gray-50 p-4 rounded-lg border border-gray-100 leading-relaxed text-sm">
                这是一份为您量身定制的六盘水旅行计划，涵盖了您选择的所有景点，并根据您的偏好安排了合理的游览顺序和时间。
              </p>
            </div>
            
            <div style="height: 480px;" class="mb-6 rounded-xl overflow-hidden shadow-sm border border-gray-200">
              <div v-if="!tripPlan" class="h-full flex items-center justify-center bg-gray-50">
                <div class="text-center">
                  <el-icon class="text-6xl text-gray-500 mb-4"><Location /></el-icon>
                  <span class="text-gray-500">请先生成行程计划</span>
                </div>
              </div>
              <TripRouteMap v-else :spots="selectedSpots" :transportation="transportation as 'car' | 'public' | 'tour'" :scenic-data="scenicSpots" />
            </div>
            
            <div class="flex space-x-2 mb-2 overflow-x-auto pb-2 scrollbar-hide">
              <el-tag 
                v-for="(day, index) in tripPlan.itinerary" 
                :key="index"
                :type="currentDay === index ? 'primary' : 'info'"
                :effect="currentDay === index ? 'dark' : 'plain'"
                class="cursor-pointer !px-4 !h-8"
                round
                @click="currentDay = index"
              >
                第 {{ index + 1 }} 天
              </el-tag>
            </div>
          </div>
          
          <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6 itinerary-card">
            <div class="flex justify-between items-center mb-6 pb-4 border-b border-gray-100">
              <div class="flex items-center gap-2">
                <div class="w-10 h-10 rounded-full bg-blue-50 flex items-center justify-center text-primary font-bold shadow-sm border border-blue-100">
                   {{ currentDay + 1 }}
                </div>
                <div>
                  <h3 class="text-lg font-bold text-gray-900">第 {{ currentDay + 1 }} 天行程</h3>
                  <div class="text-xs text-gray-500 mt-0.5">{{ formatDate(addDays(tripDates[0], currentDay)) }}</div>
                </div>
              </div>
            </div>
            
            <div class="itinerary-timeline">
              <div 
                v-for="(activity, index) in tripPlan.itinerary[currentDay].activities"
                :key="index"
                class="activity-item"
                :class="getActivityClass(activity.type)"
              >
                <div class="activity-time">
                  <span class="time-text">{{ activity.time }}</span>
                  <div class="time-line"></div>
                </div>
                <div class="activity-content">
                  <div class="activity-header">
                    <div class="activity-icon" :class="'icon-' + activity.type">
                      {{ getActivityIcon(activity.type) }}
                    </div>
                    <div class="activity-title-section">
                      <h4 class="activity-title">{{ activity.title }}</h4>
                      <div class="activity-meta">
                        <span v-if="activity.duration" class="meta-item">
                          <el-icon><Clock /></el-icon>
                          {{ activity.duration }}
                        </span>
                        <span v-if="activity.cost" class="meta-item cost">
                          <el-icon><Coin /></el-icon>
                          ¥{{ activity.cost }}
                        </span>
                        <span v-if="activity.location" class="meta-item">
                          <el-icon><Location /></el-icon>
                          {{ activity.location }}
                        </span>
                      </div>
                    </div>
                  </div>
                  <p class="activity-description">{{ activity.description }}</p>
                  
                  <!-- 景点额外信息 -->
                  <div v-if="activity.type === 'scenic' && activity.details" class="activity-extra scenic-extra">
                    <span v-if="activity.details.ticket" class="extra-tag">
                      🎫 {{ activity.details.ticket }}
                    </span>
                    <span v-if="activity.details.suggestedTime" class="extra-tag">
                      ⏱️ 建议{{ activity.details.suggestedTime }}
                    </span>
                    <span v-if="activity.details.crowdLevel" class="extra-tag" :class="'crowd-' + activity.details.crowdLevel">
                      👥 {{ activity.details.crowdLevel }}
                    </span>
                  </div>
                  
                  <!-- 餐饮额外信息 -->
                  <div v-if="activity.type === 'food' && activity.details" class="activity-extra food-extra">
                    <span v-if="activity.details.price" class="extra-tag">
                      💰 人均{{ activity.details.price }}
                    </span>
                    <span v-if="activity.details.rating" class="extra-tag">
                      ⭐ {{ activity.details.rating }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 当天费用小结 -->
            <div class="day-summary bg-green-50 rounded-lg p-3 mt-4 border border-green-100 flex justify-between items-center">
              <div class="flex items-center gap-2 text-green-700">
                <el-icon><Money /></el-icon>
                <span class="font-medium text-sm">当日预计支出</span>
              </div>
              <div class="text-green-600 font-bold text-lg">¥{{ getDayCost(currentDay) }}</div>
            </div>
          </div>
          
          <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <div class="flex items-center gap-2 mb-4 pb-3 border-b border-gray-100">
              <el-icon class="text-orange-500 text-xl"><Opportunity /></el-icon>
              <h3 class="text-lg font-bold text-gray-900">AI 旅行小贴士</h3>
            </div>
            
            <ul class="tips-list grid grid-cols-1 md:grid-cols-2 gap-4">
              <li v-for="(tip, index) in (tripPlan.tips || defaultTips)" :key="index" class="tip-item flex items-start gap-3 p-3 bg-orange-50 rounded-lg text-sm text-gray-700 leading-relaxed border border-orange-100">
                <span class="tip-icon mt-0.5 text-orange-500">💡</span>
                <span class="tip-text">{{ tip }}</span>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { Calendar, Ticket, Clock, User, Location, Star, Coin, Loading, Warning, Setting, TrendCharts, Money, MapLocation, MagicStick, Opportunity } from '@element-plus/icons-vue'
import TripRouteMap from '@/components/TripRouteMap.vue'
import * as echarts from 'echarts'
import { createPlan, getPlan, updatePlan, getUserPlans, type Attraction } from '@/api/planning'
import { generateAiPlan, type AiPlanningRequest, type AiPlanningResponse } from '@/api/aiPlanning'
import { getScenicSpots } from '@/api/scenic'
import { getScenicTrend } from '@/api/prediction'
import { useDigitalHumanStore } from '@/store/digitalHuman'

// 是否使用开发模式 - 设置为false以使用真实后端数据
const isDev = false

// 数字人 store
const digitalHumanStore = useDigitalHumanStore()

// 客流预测数据类型
interface FlowPrediction {
  date: string
  flow: number
  crowdLevel: string  // 'low' | 'medium' | 'high'
}

// 定义类型
interface TripActivity {
  time: string;
  title: string;
  description: string;
  type: 'scenic' | 'food' | 'hotel' | 'transit' | 'other';
  duration?: string;
  cost?: number;
  location?: string;
  details?: {
    ticket?: string;
    suggestedTime?: string;
    crowdLevel?: string;
    price?: string;
    rating?: string;
    location?: string;
  };
}

interface DayItinerary {
  date: Date;
  activities: TripActivity[];
}

interface TripPlanData {
  totalBudget: number;
  ticketCost: number;
  foodCost: number;
  transportationCost: number;
  accommodationCost: number;
  otherCost: number;
  itinerary: DayItinerary[];
  tips?: string[];
}

// 表单数据
const tripDates = ref<Date[]>([new Date(), new Date(Date.now() + 3 * 24 * 60 * 60 * 1000)])
const peopleCount = ref<number>(2)
const transportation = ref<string>('car')
const budget = ref<number[]>([1000, 5000])
const preferences = ref<string[]>(['nature', 'food'])
const selectedSpots = ref<string[]>([])
const planningMode = ref<string>('ai')  // 'ai' 或 'normal'
const selectedFactors = ref<string[]>([])  // 行程影响因素

// 景区列表（从后端动态加载）
const scenicSpots = ref<Array<{ id: number; code: string; name: string; longitude?: number; latitude?: number }>>([])
const scenicLoading = ref(false)

// 行程计划
const tripPlan = ref<TripPlanData | null>(null)
const currentDay = ref<number>(0)
const budgetColor = ref<string>('#36DBFF')

// 客流预测数据（用于展示）
const flowPredictionData = ref<Record<string, FlowPrediction[]>>({})

// 地图相关
const routeMapRef = ref<HTMLElement | null>(null)
let routeMap: any = null

// 景点坐标数据（支持新旧两种code格式，使用数据库中的真实坐标）
const spotCoordinates: Record<string, [number, number]> = {
  // 旧格式
  meihuashan: [104.8336, 26.5945],   // 梅花山风景区
  zhijindong: [104.4532, 26.0708],   // 乌蒙大草原
  yushe: [104.7608, 26.6203],        // 玉舍国家森林公园
  minghu: [104.8442, 26.5848],       // 明湖国家湿地公园
  panzhou: [104.9537, 26.5402],      // 水城古镇
  shuicheng: [104.9537, 26.5402],    // 水城古镇
  // 新格式（数据库中的spot_code）
  MHS001: [104.8336, 26.5945],       // 梅花山风景区
  YSH002: [104.7608, 26.6203],       // 玉舍国家森林公园
  WMC003: [104.4532, 26.0708],       // 乌蒙大草原
  SCGZ004: [104.9537, 26.5402],      // 水城古镇
  MHSD005: [104.8442, 26.5848]       // 明湖国家湿地公园
}

// 状态管理
const isLoading = ref<boolean>(false)
const hasError = ref<boolean>(false)
const errorMessage = ref<string>('')

// 景区名称到ID的映射（支持新旧两种code格式）
const scenicNameToId: Record<string, number> = {
  // 旧格式
  'meihuashan': 1,
  'yushe': 2,
  'zhijindong': 3,
  'shuicheng': 4,
  'minghu': 5,
  'panzhou': 4,
  // 新格式（数据库中的spot_code）
  'MHS001': 1,
  'YSH002': 2,
  'WMC003': 3,
  'SCGZ004': 4,
  'MHSD005': 5
}

// 景区ID到中文名称的映射（用于AI API，支持新旧两种code格式）
const scenicIdToName: Record<string, string> = {
  // 旧格式
  'meihuashan': '梅花山风景区',
  'yushe': '玉舍国家森林公园',
  'zhijindong': '乌蒙大草原',
  'shuicheng': '水城古镇',
  'minghu': '明湖国家湿地公园',
  'panzhou': '水城古镇',
  // 新格式
  'MHS001': '梅花山风景区',
  'YSH002': '玉舍国家森林公园',
  'WMC003': '乌蒙大草原',
  'SCGZ004': '水城古镇',
  'MHSD005': '明湖国家湿地公园'
}

// 生成行程
const generatePlan = async () => {
  if (!tripDates.value || !tripDates.value[0] || !tripDates.value[1]) {
    ElMessage.warning('请选择旅行时间')
    return
  }
  
  if (selectedSpots.value.length === 0) {
    ElMessage.warning('请至少选择一个景点')
    return
  }
  
  // 设置加载状态
  isLoading.value = true
  hasError.value = false
  errorMessage.value = ''
  
  // 显示加载状态
  ElMessage.success('正在为您生成行程，请稍候...')
  
  try {
    let newPlan: TripPlanData
    
    if (planningMode.value === 'ai') {
      // AI智能规划模式
      ElNotification({
        title: '🤖 AI正在规划您的行程',
        message: '正在获取客流预测数据并生成个性化行程...',
        type: 'info',
        duration: 3000
      })
      
      // 1. 先获取各景区的客流预测数据
      const flowPredictions = await fetchFlowPredictions()
      
      // 保存预测数据用于展示
      flowPredictionData.value = flowPredictions
      
      // 2. 构建AI请求（包含预测数据）
      const aiRequest: AiPlanningRequest = {
        startDate: formatDateForAPI(tripDates.value[0]),
        endDate: formatDateForAPI(tripDates.value[1]),
        attractions: selectedSpots.value.map(spot => scenicIdToName[spot]),
        transportation: transportation.value,
        peopleCount: peopleCount.value,
        budget: budget.value[1],  // 使用预算上限
        preferences: preferences.value,
        flowPredictions: flowPredictions  // 添加客流预测数据
      }
      
      // 调用AI API
      const aiResponse = await generateAiPlan(aiRequest)
      
      // 转换AI响应为前端数据格式
      newPlan = convertAiPlanToLocal(aiResponse)
      
      // 显示AI优化提示
      if (aiResponse.optimized) {
        ElNotification({
          title: '✅ AI规划完成',
          message: `已结合客流预测数据和${aiResponse.optimizedBy}验证优化路线，为您生成最佳行程！`,
          type: 'success',
          duration: 4000
        })
      }
    } else if (isDev) {
      // 常规模式 - 开发环境：使用mock数据
      newPlan = generateMockTripPlan()
    } else {
      // 常规模式 - 生产模式：调用API
      const attractionIds = selectedSpots.value.map(spot => scenicNameToId[spot] || 1)
      
      const response = await createPlan({
        date: formatDateForAPI(tripDates.value[0]),
        startTime: '09:00',
        endTime: '18:00',
        attractions: attractionIds,
        preferences: {
          avoidCrowded: selectedFactors.value.includes('crowd'),
          preferIndoor: preferences.value.includes('indoor'),
          preferOutdoor: preferences.value.includes('outdoor'),
          withChildren: preferences.value.includes('children'),
          withElderly: preferences.value.includes('elderly')
        }
      })
      
      // 转换API响应为前端数据格式
      newPlan = convertAPIPlanToLocal(response)
    }
    
    // 更新状态
    tripPlan.value = newPlan
    currentDay.value = 0
    
    // 确保DOM更新后再显示成功消息
    await nextTick()
    
    // 延迟一段时间确保DOM已渲染
    setTimeout(() => {
      ElMessage.success('行程生成成功！')
      // 完成加载
      isLoading.value = false

      // 自动触发数字人讲解生成的行程
      triggerPlanNarration()
    }, 300)
  } catch (error) {
    console.error('生成行程出错:', error)
    hasError.value = true
    errorMessage.value = '生成行程失败，请重试'
    ElMessage.error(errorMessage.value)
    isLoading.value = false
  }
}

/**
 * 获取各景区的客流预测数据
 * 使用双流融合预测模型获取选定日期范围内的客流预测
 */
const fetchFlowPredictions = async (): Promise<Record<string, FlowPrediction[]>> => {
  const predictions: Record<string, FlowPrediction[]> = {}
  const days = getDaysDiff()
  
  try {
    // 为每个选中的景区获取预测数据
    for (const spotCode of selectedSpots.value) {
      const scenicId = scenicNameToId[spotCode]
      if (!scenicId) continue
      
      try {
        // 调用预测API，使用双流融合模型
        const response: any = await getScenicTrend(
          scenicId,
          'dual_stream',  // 使用双流融合预测模型
          days,
          ['weather', 'holiday', 'historical']  // 预测因子
        )
        
        const data = response?.data || response
        
        if (data && data.dates && data.predictions) {
          const spotName = scenicIdToName[spotCode] || spotCode
          predictions[spotName] = data.dates.map((date: string, index: number) => {
            const flow = data.predictions[index] || 0
            // 根据客流量判断拥挤程度
            let crowdLevel = 'low'
            if (flow > 3000) crowdLevel = 'high'
            else if (flow > 1500) crowdLevel = 'medium'
            
            return {
              date,
              flow,
              crowdLevel
            }
          })
        }
      } catch (err) {
        console.warn(`获取景区 ${spotCode} 预测数据失败:`, err)
      }
    }
    
    console.log('📊 客流预测数据获取完成:', predictions)
    return predictions
  } catch (error) {
    console.error('获取客流预测数据失败:', error)
    return {}
  }
}

// 格式化日期为API格式
const formatDateForAPI = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 转换API响应为本地数据格式
const convertAPIPlanToLocal = (apiResponse: any): TripPlanData => {
  const days = getDaysDiff()
  const itinerary: DayItinerary[] = []
  
  // 将API返回的schedule按日期分组
  for (let i = 0; i < days; i++) {
    const dayDate = addDays(tripDates.value[0], i)
    const activities: TripActivity[] = apiResponse.schedule.map((item: any) => ({
      time: item.startTime,
      title: item.attraction.name,
      description: item.attraction.description,
      type: 'scenic' as const,
      duration: `${item.suggestedDuration}分钟`,
      details: {
        ticket: `¥${item.attraction.ticketPrice || 0}`,
        suggestedTime: `${item.suggestedDuration}分钟`,
        crowdLevel: item.congestionLevel
      }
    }))
    
    itinerary.push({
      date: dayDate,
      activities
    })
  }
  
  return {
    totalBudget: apiResponse.estimatedCost || budget.value[1],
    ticketCost: Math.floor(apiResponse.estimatedCost * 0.4),
    foodCost: Math.floor(apiResponse.estimatedCost * 0.25),
    transportationCost: Math.floor(apiResponse.estimatedCost * 0.2),
    accommodationCost: Math.floor(apiResponse.estimatedCost * 0.1),
    otherCost: Math.floor(apiResponse.estimatedCost * 0.05),
    itinerary
  }
}

// 转换AI响应为本地数据格式
const convertAiPlanToLocal = (aiResponse: AiPlanningResponse): TripPlanData => {
  const itinerary: DayItinerary[] = []
  
  // 转换每天的行程
  for (const day of aiResponse.itinerary) {
    const dayDate = new Date(day.date)
    const activities: TripActivity[] = day.activities.map((activity) => ({
      time: activity.time,
      title: activity.title,
      description: activity.description,
      type: activity.type,
      duration: activity.duration,
      cost: activity.cost || 0,  // 添加费用字段
      location: activity.location,
      details: {
        ticket: activity.cost ? `¥${activity.cost}` : undefined,
        location: activity.location,
        price: activity.cost ? `¥${activity.cost}` : undefined
      }
    }))
    
    itinerary.push({
      date: dayDate,
      activities
    })
  }
  
  return {
    totalBudget: aiResponse.totalBudget,
    ticketCost: aiResponse.budgetBreakdown.ticket,
    foodCost: aiResponse.budgetBreakdown.food,
    transportationCost: aiResponse.budgetBreakdown.transportation,
    accommodationCost: aiResponse.budgetBreakdown.accommodation,
    otherCost: aiResponse.budgetBreakdown.other,
    itinerary,
    tips: aiResponse.tips  // 保存AI返回的小贴士
  }
}

// 获取预算百分比
const getBudgetPercentage = () => {
  if (!tripPlan.value) return 0
  const max = budget.value[1]
  return Math.min(Math.round((tripPlan.value.totalBudget / max) * 100), 100)
}

// 预算明细项（计算属性）
const budgetItems = computed(() => {
  if (!tripPlan.value) return []
  const total = tripPlan.value.totalBudget || 1
  return [
    { name: '景点门票', value: tripPlan.value.ticketCost, icon: '🎫', color: '#36DBFF', percentage: Math.round((tripPlan.value.ticketCost / total) * 100) },
    { name: '交通费用', value: tripPlan.value.transportationCost, icon: '🚗', color: '#9F87FF', percentage: Math.round((tripPlan.value.transportationCost / total) * 100) },
    { name: '餐饮费用', value: tripPlan.value.foodCost, icon: '🍜', color: '#FF7ECB', percentage: Math.round((tripPlan.value.foodCost / total) * 100) },
    { name: '住宿费用', value: tripPlan.value.accommodationCost, icon: '🏨', color: '#FFC861', percentage: Math.round((tripPlan.value.accommodationCost / total) * 100) },
    { name: '其他费用', value: tripPlan.value.otherCost, icon: '📦', color: '#4FFBDF', percentage: Math.round((tripPlan.value.otherCost / total) * 100) }
  ]
})

// 客流预测相关辅助函数
const formatPredictionDate = (dateStr: string | number) => {
  // 如果是数字（天数索引），则基于开始日期计算
  if (typeof dateStr === 'number' || !isNaN(Number(dateStr))) {
    const dayIndex = typeof dateStr === 'number' ? dateStr : Number(dateStr)
    if (tripDates.value && tripDates.value[0]) {
      const startDate = new Date(tripDates.value[0])
      startDate.setDate(startDate.getDate() + dayIndex)
      return `${startDate.getMonth() + 1}/${startDate.getDate()}`
    }
    return `第${dayIndex + 1}天`
  }
  
  // 如果是日期字符串
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) {
    return dateStr // 无法解析则直接返回原值
  }
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const getCrowdLevelClass = (level: string) => {
  switch (level) {
    case 'high': return 'bar-high'
    case 'medium': return 'bar-medium'
    default: return 'bar-low'
  }
}

const getCrowdTagType = (level: string) => {
  switch (level) {
    case 'high': return 'danger'
    case 'medium': return 'warning'
    default: return 'success'
  }
}

const getCrowdLevelText = (level: string) => {
  switch (level) {
    case 'high': return '拥挤'
    case 'medium': return '适中'
    default: return '舒适'
  }
}

const getFlowBarWidth = (flow: number) => {
  // 最大客流量设为5000，计算百分比
  return Math.min((flow / 5000) * 100, 100)
}

// 获取时间轴图标类型
const getTimelineItemType = (type: string) => {
  const types: Record<string, string> = {
    'scenic': 'primary',
    'food': 'success',
    'hotel': 'warning',
    'transit': 'info'
  }
  return types[type] || 'info'
}

// 获取活动图标
const getActivityIcon = (type: string) => {
  const icons: Record<string, string> = {
    'scenic': '🏞️',
    'food': '🍜',
    'hotel': '🏨',
    'transit': '🚗',
    'other': '📍'
  }
  return icons[type] || '📍'
}

// 获取活动样式类
const getActivityClass = (type: string) => {
  return `activity-${type}`
}

// 计算当天费用
const getDayCost = (dayIndex: number) => {
  if (!tripPlan.value || !tripPlan.value.itinerary[dayIndex]) return 0
  const activities = tripPlan.value.itinerary[dayIndex].activities
  return activities.reduce((sum: number, activity: any) => {
    return sum + (activity.cost || 0)
  }, 0)
}

// 默认旅行小贴士
const defaultTips = [
  '六盘水海拔较高，早晚温差大，请准备足够的保暖衣物。',
  '梅花山风景区内海拔变化较大，建议穿着舒适的徒步鞋。',
  '当地特色美食包括酸汤鱼、羊肉粉、烙锅等，值得品尝。',
  '旅游旺季期间建议提前预约景点门票，避免排队等待。',
  '乌蒙大草原天气多变，建议携带防晒和防雨装备。'
]

// 方案四：数字人行程伴讲
const triggerPlanNarration = () => {
  if (!tripPlan.value) {
    ElMessage.warning('请先生成行程计划')
    return
  }
  // 将行程数据转为文字描述，发给数字人
  const plan = tripPlan.value
  let text = `共${plan.itinerary.length}天行程，预算约${plan.totalBudget}元。\n`
  plan.itinerary.forEach((day, i) => {
    text += `\n第${i + 1}天：\n`
    day.activities.forEach(act => {
      text += `  ${act.time} ${act.title}${act.location ? '（' + act.location + '）' : ''}${act.duration ? ' - 约' + act.duration : ''}\n`
    })
  })
  if (plan.tips && plan.tips.length > 0) {
    text += `\n贴士：${plan.tips.join('；')}`
  }
  digitalHumanStore.triggerItineraryNarration(text)
  ElMessage.success('已通知黔小游为你讲解行程！')
}

// 下载行程
const downloadPlan = () => {
  if (!tripPlan.value) {
    ElMessage.warning('请先生成行程计划')
    return
  }

  try {
    const plan = tripPlan.value
    const startDate = tripDates.value?.[0] ? formatDate(tripDates.value[0]) : '未设置'
    const endDate = tripDates.value?.[1] ? formatDate(tripDates.value[1]) : '未设置'
    const days = getDaysDiff()
    const travelModeText = transportation.value === 'car' ? '自驾' : 
                           transportation.value === 'public' ? '公共交通' : '旅行团'
  
  // 生成每日行程HTML
  const itineraryHTML = plan.itinerary.map((day, dayIndex) => {
    const dayDate = day.date ? formatDate(day.date) : '第' + (dayIndex + 1) + '天'
    const activitiesHTML = day.activities.map((activity) => {
      return '<div class="activity-item">' +
        '<div class="activity-time">' + (activity.time || '待定') + '</div>' +
        '<div class="activity-content">' +
        '<div class="activity-name">' + (activity.title || '活动') + '</div>' +
        '<div class="activity-location">📍 ' + (activity.location || '未知地点') + '</div>' +
        (activity.description ? '<div class="activity-desc">' + activity.description + '</div>' : '') +
        '<div class="activity-meta">' +
        '<span class="activity-duration">⏱️ ' + (activity.duration || '1小时') + '</span>' +
        '<span class="activity-cost">💰 ¥' + (activity.cost || 0) + '</span>' +
        '</div>' +
        '</div>' +
        '</div>'
    }).join('')
    
    const dayCost = day.activities.reduce((sum, act) => sum + (act.cost || 0), 0)
    
    return '<div class="day-section">' +
      '<div class="day-header">' +
      '<span class="day-title">📅 第' + (dayIndex + 1) + '天 - ' + dayDate + '</span>' +
      '<span class="day-cost">当日预算: ¥' + dayCost + '</span>' +
      '</div>' +
      '<div class="activities-list">' + activitiesHTML + '</div>' +
      '</div>'
  }).join('')

  // 生成预算明细HTML
  const budgetHTML = '<div class="budget-grid">' +
    '<div class="budget-item"><span class="budget-icon">🎫</span><span class="budget-name">景点门票</span><span class="budget-value">¥' + plan.ticketCost + '</span></div>' +
    '<div class="budget-item"><span class="budget-icon">🚗</span><span class="budget-name">交通费用</span><span class="budget-value">¥' + plan.transportationCost + '</span></div>' +
    '<div class="budget-item"><span class="budget-icon">🍜</span><span class="budget-name">餐饮费用</span><span class="budget-value">¥' + plan.foodCost + '</span></div>' +
    '<div class="budget-item"><span class="budget-icon">🏨</span><span class="budget-name">住宿费用</span><span class="budget-value">¥' + plan.accommodationCost + '</span></div>' +
    '<div class="budget-item"><span class="budget-icon">📦</span><span class="budget-name">其他费用</span><span class="budget-value">¥' + plan.otherCost + '</span></div>' +
    '</div>'

  // 生成小贴士HTML
  const tips = plan.tips || defaultTips
  const tipsHTML = tips.map(tip => '<li class="tip-item">💡 ' + tip + '</li>').join('')

  // 生成选中景点HTML
  const selectedSpotsHTML = selectedSpots.value.map(spotCode => {
    const spotInfo = scenicSpots.value.find(s => s.code === spotCode)
    const spotName = spotInfo ? spotInfo.name : (scenicIdToName[spotCode] || spotCode)
    return '<span class="spot-tag">' + spotName + '</span>'
  }).join('')

  // 生成完整报告HTML
  const reportHTML = '<!DOCTYPE html>' +
    '<html lang="zh-CN">' +
    '<head>' +
    '<meta charset="UTF-8">' +
    '<meta name="viewport" content="width=device-width, initial-scale=1.0">' +
    '<title>六盘水旅行计划</title>' +
    '<style>' +
    '* { margin: 0; padding: 0; box-sizing: border-box; }' +
    'body { font-family: "Microsoft YaHei", "PingFang SC", sans-serif; background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); color: #e2e8f0; min-height: 100vh; padding: 40px; }' +
    '.report-container { max-width: 900px; margin: 0 auto; background: rgba(30, 41, 59, 0.95); border-radius: 20px; padding: 40px; box-shadow: 0 25px 50px rgba(0, 0, 0, 0.5); border: 1px solid rgba(54, 219, 255, 0.2); }' +
    '.report-header { text-align: center; margin-bottom: 40px; padding-bottom: 30px; border-bottom: 2px solid rgba(54, 219, 255, 0.3); }' +
    '.report-title { font-size: 28px; font-weight: 700; color: #36DBFF; margin-bottom: 10px; }' +
    '.report-subtitle { color: #94a3b8; font-size: 14px; }' +
    '.section { margin-bottom: 30px; }' +
    '.section-title { font-size: 18px; font-weight: 600; color: #36DBFF; margin-bottom: 16px; padding-left: 14px; border-left: 4px solid #36DBFF; }' +
    '.info-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }' +
    '.info-item { background: rgba(15, 23, 42, 0.6); padding: 16px; border-radius: 12px; border: 1px solid rgba(71, 85, 105, 0.3); }' +
    '.info-label { font-size: 12px; color: #94a3b8; margin-bottom: 6px; }' +
    '.info-value { font-size: 18px; font-weight: 700; color: #e2e8f0; }' +
    '.info-value.highlight { color: #36DBFF; }' +
    '.spots-container { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }' +
    '.spot-tag { background: linear-gradient(135deg, rgba(54, 219, 255, 0.2), rgba(159, 135, 255, 0.1)); padding: 6px 14px; border-radius: 20px; font-size: 13px; border: 1px solid rgba(54, 219, 255, 0.3); }' +
    '.budget-total { text-align: center; padding: 20px; background: linear-gradient(135deg, rgba(54, 219, 255, 0.1), rgba(159, 135, 255, 0.05)); border-radius: 12px; margin-bottom: 20px; }' +
    '.budget-total-value { font-size: 36px; font-weight: 700; color: #36DBFF; }' +
    '.budget-total-label { font-size: 14px; color: #94a3b8; }' +
    '.budget-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; }' +
    '.budget-item { background: rgba(15, 23, 42, 0.6); padding: 16px; border-radius: 12px; text-align: center; border: 1px solid rgba(71, 85, 105, 0.3); }' +
    '.budget-icon { font-size: 24px; display: block; margin-bottom: 8px; }' +
    '.budget-name { font-size: 12px; color: #94a3b8; display: block; margin-bottom: 4px; }' +
    '.budget-value { font-size: 16px; font-weight: 600; color: #e2e8f0; }' +
    '.day-section { margin-bottom: 24px; background: rgba(15, 23, 42, 0.4); border-radius: 12px; overflow: hidden; }' +
    '.day-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; background: linear-gradient(135deg, rgba(54, 219, 255, 0.15), rgba(159, 135, 255, 0.1)); border-bottom: 1px solid rgba(54, 219, 255, 0.2); }' +
    '.day-title { font-size: 16px; font-weight: 600; color: #36DBFF; }' +
    '.day-cost { font-size: 14px; color: #4ade80; }' +
    '.activities-list { padding: 16px 20px; }' +
    '.activity-item { display: flex; gap: 16px; padding: 16px 0; border-bottom: 1px solid rgba(71, 85, 105, 0.2); }' +
    '.activity-item:last-child { border-bottom: none; }' +
    '.activity-time { min-width: 70px; font-size: 14px; font-weight: 600; color: #9F87FF; }' +
    '.activity-content { flex: 1; }' +
    '.activity-name { font-size: 16px; font-weight: 600; color: #e2e8f0; margin-bottom: 6px; }' +
    '.activity-location { font-size: 13px; color: #94a3b8; margin-bottom: 6px; }' +
    '.activity-desc { font-size: 13px; color: #64748b; margin-bottom: 8px; line-height: 1.5; }' +
    '.activity-meta { display: flex; gap: 16px; font-size: 12px; color: #94a3b8; }' +
    '.tips-list { list-style: none; }' +
    '.tip-item { padding: 12px 16px; background: rgba(15, 23, 42, 0.4); border-radius: 8px; margin-bottom: 10px; font-size: 14px; line-height: 1.6; border-left: 3px solid #fbbf24; }' +
    '.footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid rgba(71, 85, 105, 0.3); text-align: center; color: #64748b; font-size: 12px; }' +
    '@media print { body { background: white; color: #1e293b; padding: 20px; } .report-container { box-shadow: none; border: 1px solid #e2e8f0; } .report-title, .section-title, .day-title { color: #0891b2; } }' +
    '@media (max-width: 768px) { .info-grid, .budget-grid { grid-template-columns: 1fr; } }' +
    '</style>' +
    '</head>' +
    '<body>' +
    '<div class="report-container">' +
    '<div class="report-header">' +
    '<h1 class="report-title">🗺️ 六盘水精彩之旅</h1>' +
    '<p class="report-subtitle">智教黔行 · 专属行程计划 · ' + new Date().toLocaleDateString('zh-CN') + '</p>' +
    '</div>' +
    '<div class="section">' +
    '<h2 class="section-title">行程信息</h2>' +
    '<div class="info-grid">' +
    '<div class="info-item"><div class="info-label">出行日期</div><div class="info-value">' + startDate + ' - ' + endDate + '</div></div>' +
    '<div class="info-item"><div class="info-label">行程天数</div><div class="info-value highlight">' + days + ' 天</div></div>' +
    '<div class="info-item"><div class="info-label">出行人数</div><div class="info-value">' + peopleCount.value + ' 人</div></div>' +
    '<div class="info-item"><div class="info-label">出行方式</div><div class="info-value">' + travelModeText + '</div></div>' +
    '</div>' +
    '<div class="info-item" style="margin-top: 16px;"><div class="info-label">选择景点</div><div class="spots-container">' + selectedSpotsHTML + '</div></div>' +
    '</div>' +
    '<div class="section">' +
    '<h2 class="section-title">预算概览</h2>' +
    '<div class="budget-total"><div class="budget-total-value">¥' + plan.totalBudget + '</div><div class="budget-total-label">预计总花费</div></div>' +
    budgetHTML +
    '</div>' +
    '<div class="section">' +
    '<h2 class="section-title">详细行程</h2>' +
    itineraryHTML +
    '</div>' +
    '<div class="section">' +
    '<h2 class="section-title">温馨提示</h2>' +
    '<ul class="tips-list">' + tipsHTML + '</ul>' +
    '</div>' +
    '<div class="footer">' +
    '<p>🗺️ 本行程由智教黔行系统生成</p>' +
    '<p>生成时间：' + new Date().toLocaleString('zh-CN') + '</p>' +
    '<p>© 2024-2026 智教黔行 版权所有</p>' +
    '</div></div></body></html>'

  // 创建 Blob 并下载
  const blob = new Blob([reportHTML], { type: 'text/html;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = '六盘水旅行计划_' + new Date().toISOString().split('T')[0] + '.html'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  
  ElMessage.success('行程下载成功！')
  } catch (error) {
    console.error('下载行程出错:', error)
    ElMessage.error('下载行程失败，请重试')
  }
}

// 格式化日期
const formatDate = (date: Date) => {
  if (!date) return ''
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

// 获取日期差值
const getDaysDiff = () => {
  if (!tripDates.value || !tripDates.value[0] || !tripDates.value[1]) return 0
  const diffTime = tripDates.value[1].getTime() - tripDates.value[0].getTime()
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1
}

// 添加天数
const addDays = (date: Date, days: number) => {
  if (!date) return new Date()
  const result = new Date(date)
  result.setDate(result.getDate() + days)
  return result
}

// 初始化路线地图
const initRouteMap = () => {
  if (!routeMapRef.value || !tripPlan.value) {
    return
  }
  
  try {
    // 如果已经存在地图实例，先销毁它
    if (routeMap) {
      routeMap.dispose()
      routeMap = null
    }
    
    // 确保容器有尺寸
    routeMapRef.value.style.height = '300px'
    routeMapRef.value.style.width = '100%'
    
    // 创建ECharts实例
    routeMap = echarts.init(routeMapRef.value)
    
    // 获取选中的景点
    const spots = selectedSpots.value
    
    // 简单散点图和线图配置
    const option = {
      backgroundColor: '#f8fafc',
      title: {
        text: '行程路线示意图',
        left: 'center',
        textStyle: {
          color: '#334155',
          fontSize: 16,
          fontWeight: 'bold'
        }
      },
      xAxis: {
        show: false,
        type: 'value',
        min: 104.6,
        max: 105.0
      },
      yAxis: {
        show: false,
        type: 'value',
        min: 26.3,
        max: 26.7
      },
      tooltip: {
        trigger: 'item',
        backgroundColor: '#ffffff',
        borderColor: '#e2e8f0',
        textStyle: {
          color: '#334155'
        },
        formatter: function(params: any) {
          return params.name
        }
      },
      series: [
        {
          type: 'scatter',
          symbolSize: 15,
          itemStyle: {
            color: '#2A9D8F',
            shadowBlur: 10,
            shadowColor: 'rgba(42, 157, 143, 0.4)'
          },
          label: {
            show: true,
            position: 'right',
            formatter: '{b}',
            color: '#334155',
            fontWeight: 'bold',
            fontSize: 12,
            backgroundColor: 'rgba(255, 255, 255, 0.8)',
            padding: [4, 8],
            borderRadius: 4
          },
          data: spots.map(spot => ({
            name: getSpotName(spot),
            value: spotCoordinates[spot as keyof typeof spotCoordinates]
          }))
        },
        {
          type: 'line',
          smooth: true,
          symbol: 'none',
          lineStyle: {
            color: '#457B9D',
            width: 3,
            type: 'dashed'
          },
          data: spots.map(spot => ({
            name: getSpotName(spot),
            value: spotCoordinates[spot as keyof typeof spotCoordinates]
          }))
        }
      ]
    }
    
    routeMap.setOption(option)
    
    // 强制重绘
    setTimeout(() => {
      if (routeMap) {
        routeMap.resize()
      }
    }, 200)
    
    // 处理窗口大小变化
    const resizeHandler = () => {
      if (routeMap) {
        routeMap.resize()
      }
    }
    
    window.removeEventListener('resize', resizeHandler)
    window.addEventListener('resize', resizeHandler)
  } catch (error) {
    console.error('初始化地图失败:', error)
    // 显示错误信息
    if (routeMapRef.value) {
      routeMapRef.value.innerHTML = '<div style="padding: 20px; color: red;">地图加载失败</div>'
    }
  }
}

// 生成模拟行程数据
const generateMockTripPlan = (): TripPlanData => {
  // 根据选择的时间范围生成天数
  const days = getDaysDiff()
  
  // 获取选择的景点
  const spots = selectedSpots.value
  
  // 生成每天的行程安排
  const itinerary: DayItinerary[] = []
  
  // 定义所有景点数据
  type SpotDataType = {
    [key: string]: {
      title: string;
      description: string;
      ticket: string;
      suggestedTime: string;
      crowdLevel: string;
      }
  };
  
  const spotData: SpotDataType = {
    meihuashan: {
        title: '梅花山风景区',
        description: '游览国家4A级景区，欣赏山水风光，体验高山避暑胜地。',
          ticket: '¥60/人',
          suggestedTime: '3-4小时',
          crowdLevel: '中等'
    },
    zhijindong: {
        title: '乌蒙大草原',
        description: '探索广袤草原风光，体验少数民族风情和草原文化。',
          ticket: '¥65/人',
          suggestedTime: '4-5小时',
          crowdLevel: '中等'
    },
    yushe: {
      title: '玉舍国家森林公园',
      description: '体验原始森林风光，感受自然生态环境的神奇魅力。',
      ticket: '¥40/人',
      suggestedTime: '4-5小时',
      crowdLevel: '低'
    },
    minghu: {
      title: '明湖国家湿地公园',
      description: '漫步湿地公园，欣赏湖光山色，体验城市中的自然风光。',
      ticket: '免费',
      suggestedTime: '1.5-2小时',
      crowdLevel: '较高'
    },
    panzhou: {
      title: '水城古镇',
      description: '游览百年古镇，体验浓厚的历史文化氛围和民俗风情。',
      ticket: '免费',
      suggestedTime: '2-3小时',
      crowdLevel: '中等'
    }
  }
  
  // 根据不同的出行方式，安排不同的路线顺序
  let orderedSpots: string[] = [];
  
  if (transportation.value === 'car') {
    // 自驾路线：优先考虑景点之间的最短路径
    // 梅花山 -> 水城古镇 -> 玉舍森林公园 -> 明湖湿地公园 -> 乌蒙大草原
    const carRoute = ['meihuashan', 'panzhou', 'yushe', 'minghu', 'zhijindong'];
    orderedSpots = spots.filter(spot => carRoute.includes(spot))
                        .sort((a, b) => carRoute.indexOf(a) - carRoute.indexOf(b));
  } 
  else if (transportation.value === 'public') {
    // 公共交通路线：优先考虑交通便利的景点
    // 明湖湿地公园 -> 水城古镇 -> 梅花山 -> 乌蒙大草原 -> 玉舍森林公园
    const publicRoute = ['minghu', 'panzhou', 'meihuashan', 'zhijindong', 'yushe'];
    orderedSpots = spots.filter(spot => publicRoute.includes(spot))
                        .sort((a, b) => publicRoute.indexOf(a) - publicRoute.indexOf(b));
  }
  else if (transportation.value === 'tour') {
    // 旅行团路线：按照旅行社常规路线安排
    // 乌蒙大草原 -> 梅花山 -> 明湖湿地公园 -> 水城古镇 -> 玉舍森林公园
    const tourRoute = ['zhijindong', 'meihuashan', 'minghu', 'panzhou', 'yushe'];
    orderedSpots = spots.filter(spot => tourRoute.includes(spot))
                        .sort((a, b) => tourRoute.indexOf(a) - tourRoute.indexOf(b));
  }
  
  // 如果没有匹配到任何景点，使用原始顺序
  if (orderedSpots.length === 0) {
    orderedSpots = [...spots];
  }
  
  // 根据天数和选择的景点，规划每天的行程
  let spotIndex = 0;
  
  for (let i = 0; i < days; i++) {
    const dayActivities = [];
    
    // 早晨活动
      dayActivities.push({
      time: '08:00',
      title: '早餐',
      description: '在酒店享用丰盛早餐，为一天的行程补充能量。',
        type: 'food' as const,
        duration: '1小时',
        details: {
        price: '¥40/人',
        rating: '4.5分'
        }
    });
    
    // 上午景点活动 - 每天安排一到两个景点
    if (spotIndex < orderedSpots.length) {
      const spotKey = orderedSpots[spotIndex];
      const spot = spotData[spotKey];
      
      // 根据不同交通方式，添加不同的交通描述
      let transitDescription = '';
      if (transportation.value === 'car') {
        transitDescription = '自驾前往景区，沿途欣赏六盘水美丽风光。';
      } else if (transportation.value === 'public') {
        transitDescription = '乘坐公共交通前往景区，体验当地人的日常出行。';
      } else if (transportation.value === 'tour') {
        transitDescription = '乘坐旅游大巴前往景区，导游沿途讲解六盘水历史文化。';
      }
      
      // 添加交通时间（第一天第一个景点除外）
      if (i > 0 || spotIndex > 0) {
      dayActivities.push({
          time: '09:00',
          title: '前往' + spot.title,
          description: transitDescription,
        type: 'transit' as const,
          duration: transportation.value === 'car' ? '40分钟' : 
                   transportation.value === 'public' ? '1小时' : '50分钟'
        });
      }
      
      dayActivities.push({
        time: transportation.value === 'car' ? '09:40' : 
              transportation.value === 'public' ? '10:00' : '09:50',
        title: spot.title,
        description: spot.description,
        type: 'scenic' as const,
        duration: spot.suggestedTime.split('-')[1],
        details: {
          ticket: spot.ticket,
          suggestedTime: spot.suggestedTime,
          crowdLevel: spot.crowdLevel
        }
      });
      
      spotIndex++;
    }
    
    // 午餐
      dayActivities.push({
      time: '13:30',
        title: '午餐',
      description: i === 0 ? '在景区附近的农家乐品尝当地特色菜。' : 
                  (i === 1 ? '在景区餐厅享用特色民族美食。' : '在当地人气餐厅品尝六盘水特色菜。'),
        type: 'food' as const,
      duration: '1.5小时',
        details: {
        price: '¥50/人',
        rating: '4.2分'
        }
    });
    
    // 下午景点活动 - 如果景点足够多
    if (spotIndex < orderedSpots.length) {
      const spotKey = orderedSpots[spotIndex];
      const spot = spotData[spotKey];
      
      // 根据不同交通方式，添加不同的交通描述
      let transitDescription = '';
      if (transportation.value === 'car') {
        transitDescription = '驾车前往下一个景点，途经山间公路，风景宜人。';
      } else if (transportation.value === 'public') {
        transitDescription = '乘坐公交车前往下一个景点，沿途体验当地风土人情。';
      } else if (transportation.value === 'tour') {
        transitDescription = '乘坐旅游大巴前往下一个景点，导游讲解当地民俗文化。';
      }
      
      // 添加交通时间
      dayActivities.push({
        time: '15:00',
        title: '前往' + spot.title,
        description: transitDescription,
        type: 'transit' as const,
        duration: transportation.value === 'car' ? '40分钟' : 
                 transportation.value === 'public' ? '1小时10分钟' : '50分钟'
      });
      
      // 添加下午景点
      dayActivities.push({
        time: transportation.value === 'car' ? '15:40' : 
              transportation.value === 'public' ? '16:10' : '15:50',
        title: spot.title,
        description: spot.description,
        type: 'scenic' as const,
        duration: '2小时',
        details: {
          ticket: spot.ticket,
          suggestedTime: spot.suggestedTime,
          crowdLevel: spot.crowdLevel
        }
      });
      
      spotIndex++;
    } else if (i === days - 1 && spotIndex >= orderedSpots.length) {
      // 最后一天且已经安排完所有景点，添加购物活动
      dayActivities.push({
        time: '15:00',
        title: '六盘水特产购物',
        description: '在市区特产店选购当地特色产品，如六盘水腊肉、火腿等。',
        type: 'other' as const,
        duration: '2小时'
      });
    }
    
    // 晚餐
    dayActivities.push({
      time: '18:30',
      title: '晚餐',
      description: i === 0 ? '品尝六盘水著名的酸汤鱼' : (i === 1 ? '尝试当地的农家特色菜' : '享用贵州风味美食'),
      type: 'food' as const,
      duration: '1.5小时',
      details: {
        price: '¥60-80/人',
        rating: '4.4分'
      }
    });
    
    // 晚上活动 - 根据出行方式不同安排不同活动
    if (transportation.value === 'car') {
    dayActivities.push({
      time: '20:30',
        title: '自由活动',
        description: '可以自由安排晚间活动，如夜游市区或在酒店休息。',
        type: 'other' as const,
        duration: '1小时'
      });
    } else if (transportation.value === 'public') {
      dayActivities.push({
        time: '20:30',
        title: '夜市体验',
        description: '前往当地夜市，体验六盘水夜生活，品尝小吃。',
        type: 'other' as const,
        duration: '1.5小时'
      });
    } else if (transportation.value === 'tour') {
      dayActivities.push({
        time: '20:30',
        title: '民族歌舞表演',
        description: '欣赏少数民族特色歌舞表演，了解当地民俗文化。',
        type: 'other' as const,
        duration: '1.5小时'
      });
    }
    
    // 入住酒店
    dayActivities.push({
      time: transportation.value === 'car' ? '21:30' : 
            transportation.value === 'public' ? '22:00' : '22:00',
      title: '入住酒店',
      description: '返回酒店休息。',
      type: 'hotel' as const,
      details: {
        location: '六盘水市中心',
        rating: '4星级',
        price: '¥350/晚'
      }
    });
    
    itinerary.push({
      date: addDays(tripDates.value[0], i),
      activities: dayActivities
    });
  }
  
  // 计算预算 - 根据选择的景点计算门票费用
  let ticketCost = 0;
  orderedSpots.forEach(spotKey => {
    const spot = spotData[spotKey];
    if (spot.ticket !== '免费') {
      const match = spot.ticket.match(/\d+/);
      if (match && match[0]) {
        ticketCost += parseInt(match[0]) * peopleCount.value;
      }
    }
  });
  
  // 根据不同交通方式调整交通费用
  let transportationCost = 0;
  if (transportation.value === 'car') {
    transportationCost = 200 * days; // 汽油费+停车费
  } else if (transportation.value === 'public') {
    transportationCost = 80 * days * peopleCount.value; // 公交费用
  } else if (transportation.value === 'tour') {
    transportationCost = 150 * days * peopleCount.value; // 旅游大巴费用
  }
  
  const foodCost = 195 * days * peopleCount.value;  // 每人每天餐饮费
  const accommodationCost = 350 * (days - 1);  // 每晚住宿费
  const otherCost = 200 * peopleCount.value;  // 杂费
  
  return {
    totalBudget: ticketCost + foodCost + transportationCost + accommodationCost + otherCost,
    ticketCost,
    foodCost,
    transportationCost,
    accommodationCost,
    otherCost,
    itinerary
  };
}

// 获取景点在地图上的位置 - 简化版
const getSpotPositionSimple = (spot: string, index: number) => {
  // 预定义的位置映射
  const positionMap: Record<string, { top: string, left: string }> = {
    meihuashan: { top: '55%', left: '25%' },
    zhijindong: { top: '25%', left: '50%' },
    yushe: { top: '65%', left: '75%' },
    minghu: { top: '35%', left: '75%' },
    panzhou: { top: '70%', left: '40%' }
  }
  
  // 如果存在预定义位置，使用预定义位置
  if (positionMap[spot]) {
    return positionMap[spot]
  }
  
  // 默认均匀分布
  const totalSpots = selectedSpots.value.length
  const angle = (index / totalSpots) * 2 * Math.PI
  const radius = 35 // 分布半径 (%)
  const centerX = 50
  const centerY = 50
  
  const left = centerX + radius * Math.cos(angle)
  const top = centerY + radius * Math.sin(angle)
  
  return {
    left: `${left}%`,
    top: `${top}%`
  }
}

// 获取景点名称
const getSpotName = (spot: string) => {
  // 优先从动态加载的景区列表中获取名称
  const scenic = scenicSpots.value.find(s => s.code === spot || String(s.id) === spot)
  if (scenic) {
    return scenic.name
  }
  
  // 降级使用静态映射（支持新旧两种code格式）
  const nameMap: Record<string, string> = {
    // 旧格式
    meihuashan: '梅花山风景区',
    zhijindong: '乌蒙大草原',
    yushe: '玉舍国家森林公园',
    minghu: '明湖国家湿地公园',
    panzhou: '水城古镇',
    shuicheng: '水城古镇',
    // 新格式
    MHS001: '梅花山风景区',
    YSH002: '玉舍国家森林公园',
    WMC003: '乌蒙大草原',
    SCGZ004: '水城古镇',
    MHSD005: '明湖国家湿地公园'
  }
  
  return nameMap[spot] || spot
}

// 加载景区列表
const loadScenicSpots = async () => {
  scenicLoading.value = true
  try {
    const response: any = await getScenicSpots({ city: '六盘水' })
    const data = response?.data || response
    const list = Array.isArray(data) ? data : (data?.list || [])
    
    if (list.length > 0) {
      // 将后端数据转换为前端需要的格式
      // 后端字段是 spotCode，前端使用 code
      scenicSpots.value = list.map((item: any) => ({
        id: item.id,
        code: item.spotCode || item.code || `scenic_${item.id}`,
        name: item.name,
        longitude: item.longitude,
        latitude: item.latitude
      }))
      
      // 更新景区名称到ID的映射
      list.forEach((item: any) => {
        const code = item.spotCode || item.code
        if (code) {
          scenicNameToId[code] = item.id
        }
        scenicNameToId[`scenic_${item.id}`] = item.id
      })
      
      // 默认选择前3个景区
      if (selectedSpots.value.length === 0 && scenicSpots.value.length > 0) {
        selectedSpots.value = scenicSpots.value.slice(0, 3).map(s => s.code)
      }
    }
  } catch (error) {
    // 降级使用默认数据（使用与数据库一致的spot_code）
    scenicSpots.value = [
      { id: 1, code: 'MHS001', name: '梅花山风景区', longitude: 104.8336, latitude: 26.5945 },
      { id: 2, code: 'YSH002', name: '玉舍国家森林公园', longitude: 104.7608, latitude: 26.6203 },
      { id: 3, code: 'WMC003', name: '乌蒙大草原', longitude: 104.4532, latitude: 26.0708 },
      { id: 4, code: 'SCGZ004', name: '水城古镇', longitude: 104.9537, latitude: 26.5402 },
      { id: 5, code: 'MHSD005', name: '明湖国家湿地公园', longitude: 104.8442, latitude: 26.5848 }
    ]
    selectedSpots.value = ['MHS001', 'WMC003', 'YSH002']
  } finally {
    scenicLoading.value = false
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadScenicSpots()
})
</script>

<style scoped>
.trip-planning-container {
  @apply py-6;
}

/* 强制表单标签颜色为深色 */
:deep(.el-form-item__label) {
  color: #303133 !important;
  font-weight: 500;
}

/* 强制覆盖所有输入框为亮色主题 - 解决看不清字的问题 */
:deep(.el-input__wrapper),
:deep(.el-textarea__inner),
:deep(.el-select__wrapper),
:deep(.el-input-number__decrease),
:deep(.el-input-number__increase) {
  background-color: #ffffff !important;
  box-shadow: 0 0 0 1px #dcdfe6 inset !important;
  color: #303133 !important;
}

:deep(.el-input__inner), 
:deep(.el-textarea__inner) {
  color: #303133 !important;
  -webkit-text-fill-color: #303133 !important; /* 确保webkit浏览器文字颜色 */
}

:deep(.el-date-editor .el-range-input) {
  color: #303133 !important;
  background-color: transparent !important;
}

:deep(.el-range-separator) {
  color: #606266 !important;
}

:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset !important;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px var(--el-color-primary) inset !important;
}

/* 修复数字输入框按钮背景 */
:deep(.el-input-number__decrease),
:deep(.el-input-number__increase) {
  background: #f5f7fa !important;
  color: #606266 !important;
  border-color: #dcdfe6 !important;
}

:deep(.el-input-number__decrease:hover),
:deep(.el-input-number__increase:hover) {
  color: var(--el-color-primary) !important;
}

/* 覆盖 Element Plus 默认样式以匹配蓝色主题 */
:deep(.el-radio.is-bordered.is-checked) {
  border-color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}

:deep(.el-radio__input.is-checked .el-radio__inner) {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary);
}

:deep(.el-radio__label) {
  color: #606266;
}

:deep(.el-radio.is-checked .el-radio__label) {
  color: var(--el-color-primary);
}

:deep(.el-checkbox.is-bordered.is-checked) {
  border-color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: var(--el-color-primary);
  border-color: var(--el-color-primary);
}

:deep(.el-checkbox__label) {
  color: #606266;
}

:deep(.el-slider__bar) {
  background-color: var(--el-color-primary);
}

:deep(.el-slider__button) {
  border-color: var(--el-color-primary);
}

.budget-card {
  min-height: auto;
}

.chart-container {
  min-height: 200px;
  height: 200px;
}

/* 路线图样式 */
.route-map-container {
  position: relative;
  width: 100%;
  height: 230px;
  background-color: #f8fafc;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.route-map-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(rgba(255,255,255,0.1), rgba(255,255,255,0.2));
  z-index: 1;
}

.route-point {
  position: absolute;
  z-index: 10;
  transform: translate(-50%, -50%);
  pointer-events: auto;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.route-point:hover {
  transform: translate(-50%, -50%) scale(1.1);
  z-index: 20;
}

.route-marker {
  width: 32px;
  height: 32px;
  background-color: #3b82f6;
  border-radius: 50%;
  border: 2px solid #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: bold;
  font-size: 14px;
  box-shadow: 0 4px 6px -1px rgba(59, 130, 246, 0.5);
  transition: all 0.3s ease;
}

.route-name {
  position: absolute;
  white-space: nowrap;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.9);
  padding: 4px 8px;
  border-radius: 4px;
  color: #1e293b;
  margin-top: 5px;
  font-size: 12px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  z-index: 10;
  transition: opacity 0.3s ease;
  font-weight: 500;
}

/* 静态连接线 */
.route-lines-static {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 5;
  pointer-events: none;
}

/* 网格背景 */
.route-map-container::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(#f1f5f9 1px, transparent 1px),
    linear-gradient(90deg, #f1f5f9 1px, transparent 1px);
  background-size: 40px 40px;
  z-index: 0;
}

/* 客流预测样式 - 紧凑版 */
.flow-prediction-container {
  padding: 0;
}

.spot-prediction {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #f1f5f9;
}

.prediction-bars {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.prediction-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  background: #fff;
  padding: 6px;
  border-radius: 6px;
  border: 1px solid #f1f5f9;
}

.date-label {
  text-align: center;
  font-size: 11px;
  color: #64748b;
}

.bar-container {
  width: 100%;
  height: 4px;
  background: #f1f5f9;
  border-radius: 2px;
  overflow: hidden;
}

.bar {
  height: 100%;
  border-radius: 2px;
}

.bar-low { background: #10b981; }
.bar-medium { background: #f59e0b; }
.bar-high { background: #ef4444; }

.flow-value {
  display: none; /* 紧凑模式下隐藏具体数值 */
}

.crowd-tag {
  display: flex;
  justify-content: center;
}

.crowd-level-tag {
  font-size: 10px !important;
  padding: 0 4px !important;
  height: 18px !important;
  line-height: 16px !important;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

/* 预算卡片美化 */
.budget-content {
  padding: 4px 0;
}

.total-budget-section {
  text-align: center;
  padding: 12px;
  background: linear-gradient(to bottom right, #eff6ff, #f0f9ff);
  border-radius: 12px;
  border: 1px solid #dbeafe;
  margin-bottom: 16px;
}

.total-budget-display {
  margin-bottom: 4px;
}

.budget-amount {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
}

.budget-amount .currency {
  font-size: 20px;
  color: #3b82f6;
  font-weight: 500;
}

.budget-amount .amount {
  font-size: 32px;
  font-weight: 700;
  color: #3b82f6;
  line-height: 1;
}

.budget-breakdown {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.breakdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  transition: transform 0.2s ease;
}

.breakdown-item:hover {
  background: #f8fafc;
}

.item-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 12px;
  margin-bottom: 2px;
  color: #475569;
}

.item-bar {
  height: 4px;
  background: #f1f5f9;
  border-radius: 2px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.5s ease;
}

.item-value {
  font-size: 13px;
  min-width: 50px;
  text-align: right;
  color: #1e293b;
  font-weight: 600;
}

/* 行程卡片样式 */
.itinerary-card {
  min-height: 500px;
}

.itinerary-timeline {
  padding: 16px 0;
}

.activity-item {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  position: relative;
}

.activity-item:last-child {
  margin-bottom: 0;
}

/* 时间轴线 */
.activity-time {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 60px;
  flex-shrink: 0;
}

.time-text {
  font-weight: 700;
  color: #0f172a;
  font-family: 'Inter', system-ui, sans-serif;
  margin-bottom: 8px;
  font-size: 15px;
}

.time-line {
  flex: 1;
  width: 2px;
  background: #e2e8f0;
  position: relative;
}

.time-line::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 8px;
  height: 8px;
  background: #cbd5e1;
  border-radius: 50%;
  border: 2px solid #fff;
}

.activity-content {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  border: 1px solid #e2e8f0;
  transition: all 0.3s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.activity-content:hover {
  box-shadow: 0 4px 12px rgba(148, 163, 184, 0.1);
  border-color: #cbd5e1;
  transform: translateY(-1px);
}

.activity-header {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}

.activity-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.icon-scenic { background: linear-gradient(135deg, #dbeafe, #eff6ff); color: #2563eb; }
.icon-food { background: linear-gradient(135deg, #ffedd5, #fff7ed); color: #ea580c; }
.icon-hotel { background: linear-gradient(135deg, #f3e8ff, #faf5ff); color: #7c3aed; }
.icon-transit { background: linear-gradient(135deg, #d1fae5, #ecfdf5); color: #059669; }
.icon-other { background: linear-gradient(135deg, #f1f5f9, #f8fafc); color: #475569; }

.activity-title-section {
  flex: 1;
}

.activity-title {
  font-weight: 600;
  color: #1e293b;
  font-size: 16px;
  margin-bottom: 4px;
}

.activity-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #64748b;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.activity-description {
  color: #475569;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 12px;
}

.activity-extra {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.extra-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #475569;
}

.crowd-low { color: #10b981; background: rgba(16, 185, 129, 0.1); }
.crowd-medium { color: #f59e0b; background: rgba(245, 158, 11, 0.1); }
.crowd-high { color: #ef4444; background: rgba(239, 68, 68, 0.1); }

/* 隐藏滚动条但保留功能 */
.scrollbar-hide::-webkit-scrollbar {
    display: none;
}
.scrollbar-hide {
    -ms-overflow-style: none; /* IE and Edge */
    scrollbar-width: none; /* Firefox */
}

/* 覆盖TripRouteMap组件内部的深色背景 */
:deep(.trip-route-map) {
  background: transparent !important;
}
</style>

<!-- 全局样式覆盖，用于弹出层等挂载在body上的元素 -->
<style>
.light-theme-popper {
  /* 强制覆盖弹出层背景为白色 */
  --el-bg-color-overlay: #ffffff !important;
  --el-text-color-primary: #1a1a1a !important; /* 加深主文本颜色 */
  --el-text-color-regular: #333333 !important; /* 加深常规文本颜色 */
  --el-text-color-secondary: #666666 !important; /* 加深次要文本颜色 */
  --el-border-color-light: #e4e7ed !important;
  --el-disabled-text-color: #a8abb2 !important; /* 禁用状态也需保持一定可见度 */
  background-color: #ffffff !important;
  border: 1px solid #dcdfe6 !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
}

.light-theme-popper .el-picker-panel__body {
  background-color: #ffffff !important;
}

.light-theme-popper .el-date-table td.in-range .el-date-table-cell {
  background-color: var(--el-color-primary-light-9) !important;
}

.light-theme-popper .el-date-table td.current:not(.disabled) .el-date-table-cell__text {
  background-color: var(--el-color-primary) !important;
  color: #ffffff !important;
}

/* 强制普通日期文字颜色 */
.light-theme-popper .el-date-table td .el-date-table-cell__text {
  color: #1a1a1a !important;
}

/* 强制不可用/非本月日期颜色 */
.light-theme-popper .el-date-table td.disabled .el-date-table-cell__text,
.light-theme-popper .el-date-table td.prev-month .el-date-table-cell__text,
.light-theme-popper .el-date-table td.next-month .el-date-table-cell__text {
  color: #a8abb2 !important;
}

.light-theme-popper .el-picker-panel__icon-btn {
  color: #333333 !important;
  font-weight: bold !important;
}

.light-theme-popper .el-date-range-picker__header {
  color: #1a1a1a !important;
}

.light-theme-popper .el-date-range-picker__content .el-date-range-picker__header div {
  color: #1a1a1a !important;
  font-weight: 600 !important;
}

.light-theme-popper .el-date-table th {
  color: #333333 !important;
  border-bottom: 1px solid #ebeef5 !important;
  font-weight: 600 !important;
}
</style>
```