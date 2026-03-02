<template>
  <div class="tourism-news-page">
    <!-- 页面标题 -->
    <div class="page-header mb-6 flex items-center justify-between">
      <div>
        <h2 class="text-3xl text-[#2A9D8F] mb-2">旅游资讯</h2>
        <p class="text-gray-500">最新的六盘水旅游动态与资讯</p>
      </div>
      <el-button @click="goBack" size="large">
        <el-icon class="mr-1"><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>

    <!-- 搜索区域 -->
    <scenic-card class="mb-6">
      <div class="flex gap-4">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索新闻标题"
          clearable
          style="width: 550px"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleSearch">
          <el-icon class="mr-1"><Search /></el-icon>
          搜索
        </el-button>
      </div>
    </scenic-card>

    <!-- 新闻列表 -->
    <div class="news-list" v-loading="loading">
      <div v-if="newsList.length === 0" class="text-center py-20">
        <el-empty description="暂无新闻资讯" />
      </div>
      <div v-else class="grid grid-cols-1 gap-6">
        <scenic-card
          v-for="news in newsList"
          :key="news.id"
          class="news-item cursor-pointer hover:shadow-lg transition-all"
          @click="viewNewsDetail(news.id)"
        >
          <div class="flex gap-6">
            <!-- 封面图 -->
            <div v-if="news.covers && news.covers.length > 0" class="flex-shrink-0">
              <el-image
                :src="news.covers[0]"
                fit="cover"
                class="w-48 h-32 rounded-lg"
              >
                <template #error>
                  <div class="w-48 h-32 flex items-center justify-center bg-white">
                    <el-icon class="text-4xl text-gray-600"><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
            </div>

            <!-- 新闻信息 -->
            <div class="flex-1 flex flex-col justify-between">
              <div>
                <div class="flex items-center mb-2">
                  <el-tag size="small" class="mr-2">{{ news.category }}</el-tag>
                  <span class="text-gray-500 text-sm">{{ formatDate(news.publishTime) }}</span>
                </div>
                <h3 class="text-xl text-gray-800 font-bold mb-2">{{ news.title }}</h3>
                <p class="text-gray-500 line-clamp-2">{{ news.summary || news.content }}</p>
              </div>
              <div class="flex items-center justify-between mt-4">
                <div class="flex items-center text-gray-500 text-sm">
                  <el-icon class="mr-1"><View /></el-icon>
                  <span>{{ news.views || 0 }} 浏览</span>
                </div>
                <el-button type="primary" link>
                  查看详情 <el-icon class="ml-1"><ArrowRight /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </scenic-card>
      </div>

      <!-- 分页 -->
      <div class="mt-8 flex justify-center">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadNewsList"
          @current-change="loadNewsList"
        />
      </div>
    </div>

    <!-- 新闻详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      width="800px"
      :close-on-click-modal="false"
      :show-close="false"
      class="news-detail-dialog"
    >
      <template #header>
        <div class="dialog-header">
          <h3 class="dialog-title">{{ currentNews?.title }}</h3>
          <button class="close-btn" @click="detailVisible = false">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
              <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
            </svg>
          </button>
        </div>
      </template>
      
      <div v-if="currentNews" class="news-detail-content">
        <!-- 元信息 -->
        <div class="news-meta">
          <span class="news-category-tag">{{ currentNews.category }}</span>
          <span class="meta-item">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
              <path d="M19 4h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V9h14v11zM9 11H7v2h2v-2zm4 0h-2v2h2v-2zm4 0h-2v2h2v-2z"/>
            </svg>
            {{ formatDate(currentNews.publishTime) }}
          </span>
          <span class="meta-item">
            <el-icon><View /></el-icon>
            {{ currentNews.views || 0 }} 浏览
          </span>
        </div>
        
        <!-- 封面图 -->
        <div v-if="currentNews.covers && currentNews.covers.length > 0" class="news-cover">
          <el-image
            :src="currentNews.covers[0]"
            fit="cover"
            class="cover-image"
          />
          <div class="cover-overlay"></div>
        </div>

        <!-- 摘要 -->
        <div v-if="currentNews.summary" class="news-summary-box">
          <div class="summary-icon">📌</div>
          <p class="summary-text">{{ currentNews.summary }}</p>
        </div>

        <!-- 内容 -->
        <div class="news-body">
          {{ currentNews.content }}
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <button class="footer-btn" @click="detailVisible = false">
            关闭
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, View, ArrowRight, Picture, ArrowLeft } from '@element-plus/icons-vue'
import ScenicCard from '@/components/ScenicCard.vue'
import { getNewsList, getNewsDetail } from '@/api/news'

const router = useRouter()

const loading = ref(false)
const searchKeyword = ref('')
const newsList = ref<any[]>([])
const pagination = ref({
  page: 1,
  pageSize: 10,
  total: 0
})

const detailVisible = ref(false)
const currentNews = ref<any>(null)

// 加载新闻列表
const loadNewsList = async () => {
  loading.value = true
  try {
    const res: any = await getNewsList({
      page: pagination.value.page,
      size: pagination.value.pageSize,
      keyword: searchKeyword.value
    })
    
    if (res.data) {
      newsList.value = res.data.records || []
      pagination.value.total = res.data.total || 0
    } else {
      newsList.value = []
    }
  } catch (error: any) {
    console.error('加载新闻失败:', error)
    ElMessage.error(error?.message || '加载失败')
    newsList.value = []
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.value.page = 1
  loadNewsList()
}

// 查看新闻详情
const viewNewsDetail = async (id: number) => {
  try {
    const res: any = await getNewsDetail(id)
    if (res.data) {
      currentNews.value = res.data
      detailVisible.value = true
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载详情失败')
  }
}

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 返回上一页
const goBack = () => {
  router.back()
}

onMounted(() => {
  loadNewsList()
})
</script>


<style scoped>
.tourism-news-page {
  padding: 24px;
  min-height: 100vh;
  background-color: #F0F2F5;
}

.news-item {
  padding: 20px;
  background: white;
  border-radius: 12px;
  border: 1px solid #EBEEF5;
  transition: all 0.3s;
}

.news-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.05);
  border-color: #2A9D8F;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.content {
  font-size: 1rem;
  line-height: 1.8;
  color: #4B5563;
}

/* 覆盖 Element Plus 输入框样式 - 浅色山水主题 */
:deep(.el-input__wrapper),
:deep(.el-select .el-input__wrapper) {
  background-color: #ffffff !important;
  box-shadow: 0 0 0 1px #dcdfe6 inset !important;
  color: #303133 !important;
}

:deep(.el-input__inner) {
  color: #303133 !important;
}

:deep(.el-input__wrapper:hover),
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #2A9D8F inset !important;
}

/* 分页样式覆盖 */
:deep(.el-pagination) {
  --el-color-primary: #2A9D8F;
  --el-pagination-bg-color: #ffffff;
  --el-pagination-text-color: #606266;
}

:deep(.el-pagination button),
:deep(.el-pagination .el-pager li) {
  background-color: #ffffff;
  color: #606266;
  border: 1px solid #EBEEF5;
  margin: 0 4px;
  border-radius: 4px;
}

:deep(.el-pagination button:hover),
:deep(.el-pagination .el-pager li:hover) {
  color: #2A9D8F;
  border-color: #2A9D8F;
}

:deep(.el-pagination .el-pager li.is-active) {
  background-color: #2A9D8F;
  color: white;
  border-color: #2A9D8F;
}

/* 新闻详情弹窗样式 - 浅色 */
:deep(.news-detail-dialog .el-dialog) {
  background: #ffffff !important;
  border-radius: 16px;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

:deep(.news-detail-dialog .el-dialog__header) {
  padding: 0;
  margin: 0;
}

:deep(.news-detail-dialog .el-dialog__body) {
  padding: 0 32px 32px;
  max-height: 75vh;
  overflow-y: auto;
}

:deep(.news-detail-dialog .el-dialog__body::-webkit-scrollbar) {
  width: 6px;
}

:deep(.news-detail-dialog .el-dialog__body::-webkit-scrollbar-track) {
  background: #f1f1f1;
}

:deep(.news-detail-dialog .el-dialog__body::-webkit-scrollbar-thumb) {
  background: #c1c1c1;
  border-radius: 3px;
}

:deep(.news-detail-dialog .el-dialog__body::-webkit-scrollbar-thumb:hover) {
  background: #a8a8a8;
}

:deep(.news-detail-dialog .el-dialog__footer) {
  padding: 0;
  border-top: 1px solid #EBEEF5;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 24px 32px 20px;
  background: linear-gradient(to bottom, #F9FAFB, #FFFFFF);
  border-bottom: 1px solid #F2F3F5;
}

.dialog-title {
  font-size: 24px;
  font-weight: 700;
  color: #1D2129;
  line-height: 1.4;
  flex: 1;
  padding-right: 20px;
}

.close-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid transparent;
  background: #F2F3F5;
  color: #86909C;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.close-btn:hover {
  background: #E5E6EB;
  color: #1D2129;
  transform: rotate(90deg);
}

.news-detail-content {
  color: #333;
}

.news-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
  padding-top: 20px;
}

.news-category-tag {
  padding: 4px 12px;
  background: #E6FFFA;
  border: 1px solid #B7EB8F;
  border-radius: 4px;
  font-size: 12px;
  color: #2A9D8F;
  font-weight: 600;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #86909C;
}

.news-cover {
  position: relative;
  margin-bottom: 28px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.cover-image {
  width: 100%;
  max-height: 400px;
  object-fit: cover;
  display: block;
}

.news-summary-box {
  display: flex;
  gap: 14px;
  padding: 20px 24px;
  background: #F7F8FA;
  border-left: 4px solid #2A9D8F;
  border-radius: 4px;
  margin-bottom: 28px;
}

.summary-icon {
  font-size: 20px;
  flex-shrink: 0;
  margin-top: 2px;
}

.summary-text {
  font-size: 15px;
  line-height: 1.7;
  color: #4E5969;
  font-weight: 500;
  margin: 0;
}

.news-body {
  font-size: 16px;
  line-height: 1.8;
  color: #1D2129;
  white-space: pre-wrap;
  text-align: justify;
}

.dialog-footer {
  padding: 16px 32px;
  display: flex;
  justify-content: flex-end;
  background: #F9FAFB;
}

.footer-btn {
  padding: 10px 28px;
  background: #ffffff;
  border: 1px solid #DCDFE6;
  border-radius: 6px;
  color: #606266;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.footer-btn:hover {
  background: #F2F3F5;
  border-color: #C0C4CC;
  color: #303133;
}
</style>

