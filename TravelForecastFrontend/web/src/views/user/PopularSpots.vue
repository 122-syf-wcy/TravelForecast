<template>
  <div class="popular-spots-page">
    <!-- 页面标题 -->
    <div class="page-header mb-6 flex items-center justify-between">
      <div>
        <h2 class="text-3xl text-[#2A9D8F] mb-2">热门景点推荐</h2>
        <p class="text-gray-500">按热度（收藏数）排序，发现最受欢迎的景区</p>
      </div>
      <el-button @click="goBack" size="large">
        <el-icon class="mr-1"><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>

    <!-- 搜索和排序区域 -->
    <scenic-card class="mb-6">
      <div class="flex gap-4 items-center">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索景点名称"
          clearable
          style="width: 400px"
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
        <div class="ml-auto text-gray-500 text-sm">
          共 {{ pagination.total }} 个热门景点
        </div>
      </div>
    </scenic-card>

    <!-- 景点列表 -->
    <div class="spots-list" v-loading="loading">
      <div v-if="spotsList.length === 0" class="text-center py-20">
        <el-empty description="暂无景点数据" />
      </div>
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <scenic-card
          v-for="spot in spotsList"
          :key="spot.id"
          class="spot-card cursor-pointer hover:shadow-lg-hover transition-shadow duration-300"
        >
          <div class="flex flex-col h-full">
            <el-image
              v-if="spot.image"
              :src="normalizeImageUrl(spot.image)"
              fit="cover"
              class="w-full h-48 object-cover rounded-md mb-4"
            />
            <div class="flex-grow">
              <div class="flex items-start justify-between mb-2">
                <h3 class="text-xl font-bold text-gray-800 flex-grow">{{ spot.name }}</h3>
                <el-tag size="small" type="info">{{ spot.category || '景区' }}</el-tag>
              </div>
              
              <p class="text-gray-600 text-sm mb-3 line-clamp-2">{{ spot.description }}</p>
              
              <div class="flex items-center justify-between mb-3">
                <div class="flex items-center">
                  <el-rate v-model="spot.rating" disabled :show-score="false" size="small" class="custom-rate" />
                  <span class="ml-2 text-xs text-gray-500">({{ spot.favoritesCount || 0 }} 人收藏)</span>
                </div>
              </div>
            </div>
            
            <div class="flex items-center gap-2 mt-auto pt-3 border-t border-gray-700">
              <el-button 
                :type="spot.isFavorited ? 'danger' : 'primary'" 
                size="small"
                style="flex: 1"
                @click="toggleFavorite(spot)"
                :loading="spot.favoriteLoading"
              >
                <el-icon class="mr-1">
                  <component :is="spot.isFavorited ? 'StarFilled' : 'Star'" />
                </el-icon>
                {{ spot.isFavorited ? '已收藏' : '收藏' }}
              </el-button>
              <el-button 
                type="primary" 
                size="small" 
                plain
                @click="viewDetail(spot)"
              >
                查看详情
              </el-button>
            </div>
          </div>
        </scenic-card>
      </div>
    </div>

    <!-- 分页 -->
    <div class="mt-8 flex justify-center">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[6, 12, 24]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSearch"
        @current-change="handleSearch"
        background
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { Search, ArrowLeft, Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ScenicCard from '@/components/ScenicCard.vue'
import { getPopularSpots } from '@/api/dashboard'
import { addFavorite, removeFavorite, checkFavoriteBatch } from '@/api/favorite'
import { normalizeImageUrl } from '@/utils/imageProxy'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const searchKeyword = ref('')
const spotsList = ref<any[]>([])
const pagination = ref({
  page: 1,
  pageSize: 6,
  total: 0
})

// 加载热门景点列表
const loadSpotsList = async () => {
  loading.value = true
  try {
    // 获取所有热门景点（不分页，前端处理）
    const res: any = await getPopularSpots(100)  // 获取前100个
    
    // 处理API响应，数据在res.data中
    const data = res?.data || res
    
    if (data && Array.isArray(data)) {
      let allSpots = data.map((spot: any) => {
        // 获取原始图片URL，直接使用OSS签名URL
        const imgUrl = spot.imageUrl || spot.image_url || spot.image || ''
        
        return {
          ...spot,
          image: imgUrl,
          isFavorited: false,
          favoriteLoading: false
        }
      })
      
      // 如果有搜索关键词，进行过滤
      if (searchKeyword.value) {
        allSpots = allSpots.filter((spot: any) => 
          spot.name.toLowerCase().includes(searchKeyword.value.toLowerCase())
        )
      }
      
      // 更新总数
      pagination.value.total = allSpots.length
      
      // 计算分页
      const start = (pagination.value.page - 1) * pagination.value.pageSize
      const end = start + pagination.value.pageSize
      spotsList.value = allSpots.slice(start, end)
      
      // 如果用户已登录，批量检查收藏状态（一次请求）
      if (userStore.isLoggedIn && spotsList.value.length > 0) {
        try {
          const scenicIds = spotsList.value.map(spot => spot.id)
          const checkRes: any = await checkFavoriteBatch(scenicIds)
          const favoriteMap = checkRes?.data || checkRes || {}
          
          // 更新收藏状态
          spotsList.value.forEach(spot => {
            spot.isFavorited = favoriteMap[spot.id] === true
          })
        } catch (error) {
          console.error('批量检查收藏状态失败:', error)
        }
      }
    } else {
      spotsList.value = []
      pagination.value.total = 0
    }
  } catch (error: any) {
    console.error('加载热门景点失败:', error)
    ElMessage.error(error?.message || '加载失败')
    spotsList.value = []
    pagination.value.total = 0
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.value.page = 1
  loadSpotsList()
}

// 切换收藏状态
const toggleFavorite = async (spot: any) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  spot.favoriteLoading = true
  try {
    if (spot.isFavorited) {
      // 取消收藏
      await removeFavorite(spot.id)
      spot.isFavorited = false
      spot.favoritesCount = Math.max(0, (spot.favoritesCount || 0) - 1)
      ElMessage.success('已取消收藏')
    } else {
      // 添加收藏
      await addFavorite(spot.id)
      spot.isFavorited = true
      spot.favoritesCount = (spot.favoritesCount || 0) + 1
      ElMessage.success('收藏成功')
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '操作失败')
  } finally {
    spot.favoriteLoading = false
  }
}

// 查看详情
const viewDetail = (spot: any) => {
  // 跳转到实时服务页面，并传递景区ID
  router.push({
    path: '/user/service',
    query: { scenicId: spot.id }
  })
}

// 返回上一页
const goBack = () => {
  router.back()
}

onMounted(() => {
  loadSpotsList()
})
</script>

<style scoped>
.popular-spots-page {
  @apply py-4;
}

/* 自定义评分组件样式 */
:deep(.custom-rate .el-rate__icon) {
  font-size: 14px !important;
}

:deep(.custom-rate .el-rate__icon.is-active) {
  color: #36DBFF !important;
}

:deep(.custom-rate .el-rate__icon:not(.is-active)) {
  color: #4a5568 !important;
}

.spot-card {
  transition: all 0.3s ease;
}

.spot-card:hover {
  transform: translateY(-4px);
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>

