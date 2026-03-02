<template>
  <div class="review-container">
    <h1 class="text-2xl font-bold text-[#2C3E50] mb-6">评价系统</h1>
    
    <!-- 评价统计卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
      <stat-card 
        title="总评价数" 
        :value="reviewStats.total" 
        suffix="条" 
        :change="reviewStats.totalChange" 
        icon="ChatLineRound" 
        color="bg-gradient-to-r from-blue-500 to-cyan-400"
      />
      
      <stat-card 
        title="平均评分" 
        :value="reviewStats.avgRating" 
        suffix="分" 
        :change="reviewStats.ratingChange" 
        icon="Star" 
        color="bg-gradient-to-r from-yellow-500 to-orange-400"
      />
      
      <stat-card 
        title="好评率" 
        :value="reviewStats.positiveRate" 
        suffix="%" 
        :change="reviewStats.positiveChange" 
        icon="GoodFilled" 
        color="bg-gradient-to-r from-green-500 to-teal-400"
      />
      
      <stat-card 
        title="待回复" 
        :value="reviewStats.pendingReply" 
        suffix="条" 
        :change="0" 
        icon="ChatDotRound" 
        color="bg-gradient-to-r from-purple-500 to-indigo-400"
      />
    </div>
    
    <!-- 评价关键词词云 -->
    <div class="mb-6">
      <holographic-card>
        <template #header>
          <div class="flex justify-between items-center p-4">
            <h2 class="text-xl font-bold text-[#2A9D8F]">评价关键词</h2>
            <div class="flex items-center">
              <el-radio-group v-model="wordCloudType" size="small" class="mr-4">
                <el-radio-button label="all">全部</el-radio-button>
                <el-radio-button label="positive">好评</el-radio-button>
                <el-radio-button label="negative">差评</el-radio-button>
              </el-radio-group>
              <el-select v-model="trendTimeRange" size="small" style="width: 120px">
                <el-option label="最近7天" value="7days" />
                <el-option label="最近30天" value="30days" />
                <el-option label="最近90天" value="90days" />
              </el-select>
            </div>
          </div>
        </template>
        <div class="p-4 pt-0">
          <div ref="reviewTrendChartRef" style="width: 100%; height: 500px;"></div>
        </div>
      </holographic-card>
    </div>
    
    <!-- 评价管理 -->
    <holographic-card>
      <template #header>
        <div class="flex justify-between items-center p-4">
          <h2 class="text-xl font-bold text-[#2A9D8F]">评价管理</h2>
          <div class="flex items-center">
            <el-input
              v-model="reviewSearch"
              placeholder="搜索评价内容或用户"
              class="w-64 mr-4"
              clearable
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            
            <el-select v-model="reviewFilter" placeholder="筛选" class="w-32 mr-2">
              <el-option label="全部评价" value="all" />
              <el-option label="好评" value="positive" />
              <el-option label="中评" value="neutral" />
              <el-option label="差评" value="negative" />
              <el-option label="待回复" value="pending" />
            </el-select>
            
            <el-select v-model="reviewSort" placeholder="排序" class="w-32">
              <el-option label="最新优先" value="newest" />
              <el-option label="最早优先" value="oldest" />
              <el-option label="评分高优先" value="highest" />
              <el-option label="评分低优先" value="lowest" />
            </el-select>
          </div>
        </div>
      </template>
      
      <div class="p-4">
        <div class="space-y-6">
          <div v-for="review in pagedReviews" :key="review.orderId" class="border-b border-gray-200 pb-6 last:border-0">
            <div class="flex justify-between items-start mb-2">
              <div class="flex items-center">
                <el-avatar :size="40" :src="review.avatar"></el-avatar>
                <div class="ml-3">
                  <div class="flex items-center">
                    <span class="text-gray-800 font-medium mr-2">{{ review.username }}</span>
                    <el-tag size="small" :type="getVisitTypeTag(review.visitType)" effect="dark">
                      {{ review.visitType }}
                    </el-tag>
                    <el-tag v-if="review.featured" size="small" type="success" class="ml-2" effect="dark">
                      精选
                    </el-tag>
                  </div>
                  <div class="flex items-center mt-1">
                    <el-rate v-model="review.rating" disabled text-color="#F9F9F9" />
                    <span class="ml-2 text-xs text-gray-500">{{ review.time }}</span>
                  </div>
                </div>
              </div>
              <div class="flex space-x-2">
                <el-tag size="small" :type="getReviewTypeTag(review.rating)" effect="dark">
                  {{ getReviewTypeLabel(review.rating) }}
                </el-tag>
                <el-tag v-if="!review.replied" size="small" type="danger" effect="dark">
                  未回复
                </el-tag>
              </div>
            </div>
            
            <div class="mb-3">
              <div class="text-gray-800 mb-2">{{ review.content }}</div>
              <div class="flex flex-wrap gap-2">
                <el-image 
                  v-for="(img, imgIndex) in review.images" 
                  :key="imgIndex"
                  :src="img"
                  class="w-20 h-20 rounded object-cover"
                  :preview-src-list="review.images"
                />
              </div>
            </div>
            
            <div v-if="review.replied" class="bg-gray-50 p-3 rounded-lg mb-3 border border-gray-100">
              <div class="flex items-center mb-1">
                <el-avatar :size="24" :src="review.replyAvatar" class="mr-2"></el-avatar>
                <span class="text-sm text-gray-800">商家回复</span>
                <span class="ml-2 text-xs text-gray-500">{{ review.replyTime }}</span>
              </div>
              <div class="text-sm text-gray-600">{{ review.replyContent }}</div>
            </div>
            
            <div class="flex justify-between items-center">
              <div class="flex items-center text-sm text-gray-500">
                <span>{{ review.scenicSpot }}</span>
                <span class="mx-2">|</span>
                <span>订单号: {{ review.orderId }}</span>
              </div>
              <div class="flex space-x-3">
                <el-button size="small" type="primary" text v-if="!review.replied" @click="handleReply(review)">
                  回复
                </el-button>
                <el-button size="small" type="warning" text v-else @click="handleEditReply(review)">
                  编辑回复
                </el-button>
                <el-dropdown trigger="click" @command="handleMoreAction">
                  <el-button size="small" text>
                    更多<el-icon class="ml-1"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item :command="`feature_${review.orderId}`">标记为精选</el-dropdown-item>
                      <el-dropdown-item :command="`report_${review.orderId}`">举报评价</el-dropdown-item>
                      <el-dropdown-item :command="`hide_${review.orderId}`">删除评论</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </div>
        </div>
        
        <div class="flex justify-center mt-6">
          <el-pagination
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="filteredReviews.length"
            :current-page="currentPage"
            :page-size="pageSize"
            :page-sizes="pageSizes"
            @current-change="onPageChange"
            @size-change="onSizeChange"
          />
        </div>
      </div>
    </holographic-card>
    
    <!-- 评价分布 -->
    <div class="mt-6">
      <holographic-card>
        <template #header>
          <div class="flex justify-between items-center p-4">
            <h2 class="text-xl font-bold text-[#2A9D8F]">评价分布</h2>
          </div>
        </template>
        <div class="p-4 pt-0">
          <div ref="reviewDistributionRef" style="width: 100%; height: 400px;"></div>
        </div>
      </holographic-card>
    </div>

    <!-- 回复对话框 -->
    <el-dialog 
      v-model="replyDialogVisible" 
      :title="isEditReply ? '编辑回复' : '回复评价'"
      width="600px"
    >
      <div class="mb-4">
        <div class="flex items-center mb-2">
          <el-avatar :size="32" :src="currentReview?.avatar" class="mr-3"></el-avatar>
          <div>
            <div class="text-gray-800 font-medium">{{ currentReview?.username }}</div>
            <div class="flex items-center">
              <el-rate :model-value="currentReview?.rating" disabled size="small" />
              <span class="ml-2 text-xs text-gray-500">{{ currentReview?.time }}</span>
            </div>
          </div>
        </div>
        <div class="bg-gray-50 p-3 rounded-lg border border-gray-100">
          <div class="text-gray-800 text-sm">{{ currentReview?.content }}</div>
        </div>
      </div>
      
      <el-form :model="replyForm" :rules="replyRules" ref="replyFormRef" label-width="80px">
        <el-form-item label="回复内容" prop="content">
          <el-input 
            v-model="replyForm.content" 
            type="textarea" 
            :rows="4"
            placeholder="请输入回复内容..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="replyDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitReply">{{ isEditReply ? '更新回复' : '发送回复' }}</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch, onBeforeUnmount } from 'vue'
import { Search, ArrowDown } from '@element-plus/icons-vue'
import HolographicCard from '@/components/HolographicCard.vue'
import StatCard from '@/components/StatCard.vue'
import { getUserAvatar, getBusinessAvatar } from '@/utils/avatarUtils'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import 'echarts-wordcloud'
import { useUserStore } from '@/store/user'
import { getMerchantReviews, replyReview, deleteReview, getReviewStats } from '@/api/merchantReview'

const userStore = useUserStore()

// 评价统计数据
const reviewStats = ref({
  total: 0,
  totalChange: 0,
  avgRating: 0,
  ratingChange: 0,
  positiveRate: 0,
  positiveChange: 0,
  pendingReply: 0
})

// 评价趋势时间范围
const trendTimeRange = ref('30days')

// 词云类型筛选
const wordCloudType = ref('all')

// 评价筛选
const reviewSearch = ref('')
const reviewFilter = ref('all')
const reviewSort = ref('newest')

// 回复对话框相关
const replyDialogVisible = ref(false)
const isEditReply = ref(false)
const currentReview = ref<any>(null)
const replyFormRef = ref()
const replyForm = ref({
  content: ''
})
const replyRules = {
  content: [
    { required: true, message: '请输入回复内容', trigger: 'blur' },
    { min: 5, message: '回复内容至少5个字符', trigger: 'blur' }
  ]
}

// 商家头像（使用用户头像）
const businessAvatar = computed(() => userStore.userInfo?.avatar || getBusinessAvatar())

// 评价数据（初始为空，从API加载）
const reviewData = ref<any[]>([])

// 图表引用
const reviewTrendChartRef = ref(null)
const reviewDistributionRef = ref(null)

let reviewTrendChart: any = null
let reviewDistributionChart: any = null

// 窗口大小调整处理函数
const handleResize = () => {
  if (reviewTrendChart) {
    reviewTrendChart.resize()
  }
  if (reviewDistributionChart) {
    reviewDistributionChart.resize()
  }
}

// 当筛选条件变化时重置到第1页
watch([reviewSearch, reviewFilter, reviewSort], () => {
  currentPage.value = 1
})

// 监听时间范围和词云类型变化
watch([trendTimeRange, wordCloudType], () => {
  nextTick(() => {
    initReviewTrendChart()
  })
})

// 组件卸载时清理事件监听和图表实例
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  
  if (reviewTrendChart) {
    reviewTrendChart.dispose()
    reviewTrendChart = null
  }
  
  if (reviewDistributionChart) {
    reviewDistributionChart.dispose()
    reviewDistributionChart = null
  }
})

// 列表筛选与排序
const filteredReviews = computed(() => {
  let list = reviewData.value.slice()
  // 搜索
  if (reviewSearch.value.trim()) {
    const q = reviewSearch.value.trim().toLowerCase()
    list = list.filter(r =>
      r.username.toLowerCase().includes(q) ||
      r.content.toLowerCase().includes(q) ||
      r.scenicSpot.toLowerCase().includes(q) ||
      r.orderId.toLowerCase().includes(q)
    )
  }
  // 过滤
  if (reviewFilter.value === 'positive') {
    list = list.filter(r => r.rating >= 4)
  } else if (reviewFilter.value === 'neutral') {
    list = list.filter(r => r.rating === 3)
  } else if (reviewFilter.value === 'negative') {
    list = list.filter(r => r.rating <= 2)
  } else if (reviewFilter.value === 'pending') {
    list = list.filter(r => !r.replied)
  }
  // 排序
  if (reviewSort.value === 'newest') {
    list.sort((a, b) => new Date(b.time).getTime() - new Date(a.time).getTime())
  } else if (reviewSort.value === 'oldest') {
    list.sort((a, b) => new Date(a.time).getTime() - new Date(b.time).getTime())
  } else if (reviewSort.value === 'highest') {
    list.sort((a, b) => b.rating - a.rating)
  } else if (reviewSort.value === 'lowest') {
    list.sort((a, b) => a.rating - b.rating)
  }
  return list
})

// 前端分页控制
const currentPage = ref(1)
const pageSize = ref(5)
const pageSizes = [5, 10, 20, 50]

const pagedReviews = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredReviews.value.slice(start, start + pageSize.value)
})

const onPageChange = (page: number) => {
  currentPage.value = page
  // 滚动到列表顶部以便用户查看新页
  try {
    const el = document.querySelector('.review-container')
    el && el.scrollTo({ top: 0, behavior: 'smooth' })
  } catch {}
}

const onSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
}

// 从评论数据中提取关键词
const fetchKeywords = async () => {
  try {
    console.log('开始提取关键词...')
    console.log('当前评论数据数量:', reviewData.value.length)
    
    // 从已加载的评论数据中提取标签
    const keywordMap = new Map<string, { name: string; value: number; type: string }>()
    
    reviewData.value.forEach((review, index) => {
      let tags = review.tags || []
      
      // 如果tags是字符串，尝试解析为数组
      if (typeof tags === 'string') {
        try {
          tags = JSON.parse(tags)
        } catch (e) {
          console.warn(`评论${index}的tags解析失败:`, tags)
          tags = []
        }
      }
      
      // 确保tags是数组
      if (!Array.isArray(tags)) {
        tags = []
      }
      
      const type = review.rating >= 4 ? 'positive' : 'negative'
      
      console.log(`评论${index}: 评分=${review.rating}, 类型=${type}, 标签=`, tags)
      
      tags.forEach((tag: string) => {
        if (tag && typeof tag === 'string') {
          if (keywordMap.has(tag)) {
            const keyword = keywordMap.get(tag)!
            keyword.value++
          } else {
            keywordMap.set(tag, {
              name: tag,
              value: 1,
              type: type
            })
          }
        }
      })
    })
    
    // 转换为数组并根据类型过滤
    let keywords = Array.from(keywordMap.values())
    
    console.log('提取到的所有关键词:', keywords)
    console.log('当前筛选类型:', wordCloudType.value)
    
    if (wordCloudType.value === 'positive') {
      keywords = keywords.filter(k => k.type === 'positive')
    } else if (wordCloudType.value === 'negative') {
      keywords = keywords.filter(k => k.type === 'negative')
    }
    
    // 按出现次数排序
    keywords.sort((a, b) => b.value - a.value)
    
    console.log('最终关键词数据:', keywords)
    
    return keywords
  } catch (error) {
    console.error('提取关键词失败:', error)
    return []
  }
}

// 初始化评价关键词词云
const initReviewTrendChart = async () => {
  if (!reviewTrendChartRef.value) {
    console.warn('评价关键词词云容器未找到')
    return
  }
  
  try {
    // 如果已存在图表实例，先销毁
    if (reviewTrendChart) {
      reviewTrendChart.dispose()
    }
    reviewTrendChart = echarts.init(reviewTrendChartRef.value)
    
    // 从后端API获取关键词
    const allKeywords = await fetchKeywords()
    
    // 如果没有评论数据，显示提示
    if (allKeywords.length === 0) {
      const option = {
        title: {
          text: '暂无评价数据',
          left: 'center',
          top: 'center',
          textStyle: {
            color: '#94A3B8',
            fontSize: 18
          }
        }
      }
      reviewTrendChart.setOption(option)
      return
    }
    
    // 数据已经在后端按类型过滤，直接使用
    const data = allKeywords
    
    const option = {
      tooltip: {
        show: true,
        formatter: function(params: any) {
          const typeText = params.data.type === 'positive' ? '好评关键词' : '差评关键词'
          return `${params.name}<br/>${typeText}: ${params.value}次提及`
        },
        backgroundColor: '#FFFFFF',
        borderColor: '#EBEEF5',
        borderWidth: 1,
        textStyle: {
          color: '#303133'
        }
      },
      series: [{
        type: 'wordCloud',
        shape: 'circle',
        left: 'center',
        top: 'center',
        width: '95%',
        height: '95%',
        right: null,
        bottom: null,
        sizeRange: [16, 80],
        rotationRange: [-45, 45],
        rotationStep: 15,
        gridSize: 10,
        drawOutOfBound: false,
        layoutAnimation: true,
        textStyle: {
          fontFamily: 'Microsoft YaHei, sans-serif',
          fontWeight: 'bold',
          color: function(params: any) {
            const item = data.find((d: any) => d.name === params.name)
            if (item) {
              if (item.type === 'positive') {
                // 好评关键词使用青色到绿色的渐变
                const colors = ['#2A9D8F', '#34B3A5', '#67C23A', '#85CE61', '#457B9D']
                return colors[Math.floor(Math.random() * colors.length)]
              }
              if (item.type === 'negative') {
                // 差评关键词使用红色到橙色的渐变
                const colors = ['#F56C6C', '#FF6B6B', '#FF8787', '#E6A23C', '#F78989']
                return colors[Math.floor(Math.random() * colors.length)]
              }
            }
            return '#409EFF'
          }
        },
        emphasis: {
          focus: 'self',
          textStyle: {
            shadowBlur: 20,
            shadowColor: function(params: any) {
              const item = data.find((d: any) => d.name === params.name)
              return item?.type === 'positive' ? '#2A9D8F' : '#F56C6C'
            },
            fontSize: undefined
          }
        },
        data: data
      }]
    }
    
    reviewTrendChart.setOption(option)
  } catch (error) {
    console.error('初始化评价关键词词云失败:', error)
  }
}


// 初始化评价分布图表
const initReviewDistribution = () => {
  if (!reviewDistributionRef.value) {
    console.warn('评价分布图表容器未找到')
    return
  }
  
  try {
    // 如果已存在图表实例，先销毁
    if (reviewDistributionChart) {
      reviewDistributionChart.dispose()
    }
    reviewDistributionChart = echarts.init(reviewDistributionRef.value)
    
    const option = {
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c}条 ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        textStyle: {
          color: '#606266'
        }
      },
      series: [
        {
          name: '评分分布',
          type: 'pie',
          radius: '70%',
          center: ['60%', '50%'],
          data: [
            { value: 625, name: '5分', itemStyle: { color: '#67C23A' } },
            { value: 350, name: '4分', itemStyle: { color: '#85CE61' } },
            { value: 180, name: '3分', itemStyle: { color: '#E6A23C' } },
            { value: 65, name: '2分', itemStyle: { color: '#F56C6C' } },
            { value: 36, name: '1分', itemStyle: { color: '#ff0000' } }
          ],
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          },
          label: {
            color: '#606266',
            formatter: '{b}: {c}条 ({d}%)'
          }
        }
      ]
    }
    
    reviewDistributionChart.setOption(option)
  } catch (error) {
    console.error('初始化评价分布图表失败:', error)
  }
}

// 回复评价
const handleReply = (review: any) => {
  currentReview.value = review
  isEditReply.value = false
  replyForm.value.content = ''
  replyDialogVisible.value = true
}

// 编辑回复
const handleEditReply = (review: any) => {
  currentReview.value = review
  isEditReply.value = true
  replyForm.value.content = review.replyContent
  replyDialogVisible.value = true
}

// 提交回复
const submitReply = async () => {
  try {
    const valid = await replyFormRef.value.validate()
    if (!valid) return
    
    const review = reviewData.value.find(item => item.orderId === currentReview.value.orderId)
    if (!review) return
    
    // 调用后端API回复评论
    await replyReview(review.reviewId, replyForm.value.content)
    
    // 更新本地数据
    review.replied = true
    review.replyContent = replyForm.value.content
    review.replyAvatar = businessAvatar.value
    review.replyTime = new Date().toISOString()
    
    ElMessage.success(isEditReply.value ? '回复更新成功' : '回复发送成功')
    replyDialogVisible.value = false
    
    // 刷新评价列表
    await loadReviews()
  } catch (error: any) {
    console.error('回复失败:', error)
    ElMessage.error(error?.response?.data?.message || error?.message || '回复失败')
  }
}

// 处理更多操作
const handleMoreAction = (command: string) => {
  const [action, orderId] = command.split('_')
  const review = reviewData.value.find(item => item.orderId === orderId)
  
  if (!review) return
  
  switch (action) {
    case 'feature':
      ElMessageBox.confirm(
        `确定要将"${review.username}"的评价标记为精选吗？`,
        '标记精选',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'info'
        }
      ).then(async () => {
        try {
          const { featureReview } = await import('@/api/merchantReview')
          await featureReview(review.id)
          review.featured = true
          ElMessage.success('已标记为精选评价')
        } catch (error) {
          console.error('标记精选失败:', error)
          ElMessage.error('标记精选失败')
        }
      }).catch(() => {
        // 取消操作
      })
      break
      
    case 'report':
      ElMessageBox.confirm(
        `确定要举报"${review.username}"的评价吗？`,
        '举报评价',
        {
          confirmButtonText: '确定举报',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        try {
          const { reportReview } = await import('@/api/merchantReview')
          await reportReview(review.id)
          ElMessage.success('举报已提交，我们会尽快处理')
        } catch (error) {
          console.error('举报提交失败:', error)
          ElMessage.error('举报提交失败')
        }
      }).catch(() => {
        // 取消操作
      })
      break
      
    case 'hide':
      ElMessageBox.confirm(
        `确定要删除"${review.username}"的评价吗？删除后该评价将不再显示。`,
        '删除评价',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        try {
          // 调用后端API删除评论
          await deleteReview(review.reviewId)
          
          // 从列表中移除该评价
          const index = reviewData.value.findIndex(item => item.orderId === orderId)
          if (index > -1) {
            reviewData.value.splice(index, 1)
          }
          ElMessage.success('评价已删除')
          
          // 刷新评价列表
          await loadReviews()
        } catch (error: any) {
          console.error('删除评价失败:', error)
          ElMessage.error(error?.response?.data?.message || error?.message || '删除失败')
        }
      }).catch(() => {
        // 取消操作
      })
      break
  }
}

// 获取访问类型对应的样式
const getVisitTypeTag = (type: string) => {
  const typeMap: Record<string, string> = {
    '家庭出游': 'success',
    '朋友聚会': 'warning',
    '情侣出游': 'danger',
    '亲子游': 'info',
    '独自旅行': 'primary'
  }
  return typeMap[type] || ''
}

// 获取评价类型对应的样式
const getReviewTypeTag = (rating: number) => {
  if (rating >= 4) return 'success'
  if (rating >= 3) return 'warning'
  return 'danger'
}

// 获取评价类型对应的标签文本
const getReviewTypeLabel = (rating: number) => {
  if (rating >= 4) return '好评'
  if (rating >= 3) return '中评'
  return '差评'
}

// 加载商家景区ID（新API不再需要，但保留用于兼容）
const loadMerchantScenic = async () => {
  try {
    if (!userStore.userInfo?.id) {
      ElMessage.warning('请先登录')
      return
    }
    
    // 直接加载评论，新API会自动根据商家ID获取
    await loadReviews()
  } catch (error) {
    console.error('加载商家信息失败:', error)
  }
}

// 加载评价数据
const loadReviews = async () => {
  try {
    // 使用商家端专用的评论API
    const res: any = await getMerchantReviews({
      page: 1,
      size: 100,
      status: reviewFilter.value === 'all' ? undefined : reviewFilter.value,
      sortBy: reviewSort.value as any
    })
    
    const data = res?.data || res
    const reviews = data?.reviews || []
    
    // 转换数据格式以适配现有的UI
    reviewData.value = reviews.map((r: any) => {
      // 优先使用 username，其次使用 userNickname，最后显示匿名用户
      const displayName = r.username || r.userNickname || '匿名用户'
      // 优先使用 userAvatar，其次使用 avatar，最后使用默认头像
      const displayAvatar = r.userAvatar || r.avatar || getUserAvatar(displayName)
      
      return {
        username: displayName,
        avatar: displayAvatar,
        rating: r.rating,
        time: r.createdAt,
        content: r.content,
        images: r.images || [],
        tags: r.tags || [],  // 保留完整的tags数组用于词云
        visitType: r.tags?.[0] || '普通游客',
        scenicSpot: r.scenicName || '景区',
        orderId: `REV${r.id}`,
        replied: r.hasReply || false,
        replyAvatar: r.hasReply ? businessAvatar.value : '',
        replyTime: r.reply?.createdAt || '',
        replyContent: r.reply?.content || '',
        featured: false,
        reviewId: r.id
      }
    })
    
    console.log('商家端评论数据已加载:', reviewData.value)
    
    // 加载统计数据
    const statsRes: any = await getReviewStats()
    const stats = statsRes?.data || statsRes
    
    reviewStats.value.total = stats.totalReviews || 0
    reviewStats.value.avgRating = stats.averageRating || 0
    reviewStats.value.positiveRate = stats.positiveRate || 0
    reviewStats.value.pendingReply = stats.unrepliedCount || 0
    
    // 重新初始化词云图表（使用新的评论数据）
    nextTick(() => {
      initReviewTrendChart()
    })
    
  } catch (error: any) {
    console.error('加载评价失败:', error)
    console.error('错误详情:', {
      message: error?.message,
      response: error?.response?.data,
      status: error?.response?.status
    })
    
    const errorMsg = error?.response?.data?.message || error?.message || '加载评价失败'
    ElMessage.error(errorMsg)
  }
}

// 在筛选条件变化时重新加载评论
watch([reviewSort, reviewFilter], () => {
  loadReviews()
})

// 组件挂载时初始化
onMounted(async () => {
  // 添加窗口大小调整事件监听
  window.addEventListener('resize', handleResize)
  
  await loadMerchantScenic()
  // loadMerchantScenic 会调用 loadReviews，loadReviews 会初始化词云
  // 这里只需要初始化评分分布图
  nextTick(() => {
    initReviewDistribution()
  })
})
</script>

<style scoped>
.review-container {
  padding-bottom: 2rem;
}
</style> 