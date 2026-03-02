<template>
  <div class="scenic-explore-container">
    <div class="mb-6">
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6 scenic-main-card">
        <div class="scenic-header mb-6">
          <div class="flex justify-between items-center">
            <h2 class="text-xl font-bold text-gray-900">景区探索</h2>
            <el-input
              v-model="searchKeyword"
              placeholder="搜索景区"
              class="max-w-xs ml-auto scenic-search-input"
              clearable
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
        </div>
        
        <el-tabs v-model="activeTab" class="scenic-tabs" @tab-change="handleTabChange">
          <el-tab-pane label="3D地形地图" name="map3d">
            <div class="mt-4" style="height: 600px;">
              <TerrainMap3D ref="terrainMapRef" @marker-click="handleMarkerClick" />
            </div>
          </el-tab-pane>
          <el-tab-pane label="客流热力图" name="heatmap3d">
            <div v-if="activeTab === 'heatmap3d'" class="mt-4">
              <!-- 景区选择器 -->
              <div class="mb-4 flex items-center gap-4">
                <span class="text-gray-600">选择景区：</span>
                <el-select
                  v-model="selectedScenicForHeatmap"
                  placeholder="请选择景区"
                  class="w-64 scenic-select"
                  @change="handleScenicChange"
                  effect="light"
                  popper-class="scenic-select-dropdown"
                >
                  <el-option
                    v-for="scenic in scenics"
                    :key="scenic.id"
                    :label="scenic.name"
                    :value="scenic.id"
                  >
                    <span class="flex items-center justify-between w-full">
                      <span class="text-gray-700 font-medium">{{ scenic.name }}</span>
                      <span 
                        class="text-xs px-2 py-0.5 rounded border ml-2"
                        :class="[
                          scenic.category === '自然风光' ? 'bg-teal-50 text-teal-600 border-teal-100' :
                          scenic.category === '人文历史' ? 'bg-amber-50 text-amber-600 border-amber-100' :
                          'bg-blue-50 text-blue-600 border-blue-100'
                        ]"
                      >
                        {{ scenic.category }}
                      </span>
                    </span>
                  </el-option>
                </el-select>
                <span v-if="selectedScenicForHeatmap" class="text-cyan-400 text-sm">
                  正在显示：{{ getScenicName(selectedScenicForHeatmap) }}
                </span>
              </div>
              
              <!-- 热力图 -->
              <div style="height: 600px;">
                <Heatmap3D ref="heatmapRef" :scenic-id="selectedScenicForHeatmap" :key="selectedScenicForHeatmap || 0" />
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="所有景区" name="all">
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mt-4">
              <div v-for="(scenic, index) in filteredScenics" :key="index" class="scenic-card">
                <div class="h-full group overflow-hidden bg-white rounded-xl border border-gray-100 shadow-sm hover:shadow-lg transition-all duration-300">
                  <div class="relative overflow-hidden h-48">
                    <el-image 
                      :src="normalizeImageUrl(scenic.image)" 
                      class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" 
                      fit="cover" 
                      @error="(e: any) => handleImageError(e)" 
                    />
                    <div class="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                  </div>
                  <div class="p-4">
                    <div class="flex items-start justify-between">
                      <div>
                        <h3 class="text-lg font-bold text-gray-900 group-hover:text-primary transition-colors">{{ scenic.name }}</h3>
                        <div class="flex items-center text-primary text-sm mt-1">
                          <el-rate v-model="scenic.rating" disabled text-color="#F7BA2A" score-template="{value}" />
                          <span class="ml-2 font-bold">{{ scenic.rating }}</span>
                        </div>
                      </div>
                      <el-tag size="small" effect="plain" :type="getTagType(scenic.category)" class="border-none bg-gray-50">
                        {{ scenic.category }}
                      </el-tag>
                    </div>
                    
                    <p class="text-gray-500 text-sm mt-3 line-clamp-2 h-10">{{ scenic.description }}</p>
                    
                    <div class="flex justify-between items-center mt-4 border-t border-gray-100 pt-3">
                      <div class="flex items-center text-gray-500 text-sm">
                        <el-icon class="text-primary"><Location /></el-icon>
                        <span class="ml-1">{{ scenic.distance }}</span>
                      </div>
                      <el-button type="primary" size="small" @click="viewScenicDetail(scenic.id)" icon="ArrowRight" plain round>查看详情</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="自然风光" name="nature">
            <div v-if="natureScenics.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mt-4">
              <div v-for="(scenic, index) in natureScenics" :key="index" class="scenic-card">
                <div class="h-full group overflow-hidden bg-white rounded-xl border border-gray-100 shadow-sm hover:shadow-lg transition-all duration-300">
                  <div class="relative overflow-hidden h-48">
                    <el-image 
                      :src="normalizeImageUrl(scenic.image)" 
                      class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" 
                      fit="cover" 
                      @error="(e: any) => handleImageError(e)" 
                    />
                    <div class="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                  </div>
                  <div class="p-4">
                    <div class="flex items-start justify-between">
                      <div>
                        <h3 class="text-lg font-bold text-gray-900 group-hover:text-primary transition-colors">{{ scenic.name }}</h3>
                        <div class="flex items-center text-primary text-sm mt-1">
                          <el-rate v-model="scenic.rating" disabled text-color="#F7BA2A" score-template="{value}" />
                          <span class="ml-2 font-bold">{{ scenic.rating }}</span>
                        </div>
                      </div>
                      <el-tag size="small" effect="plain" :type="getTagType(scenic.category)" class="border-none bg-gray-50">
                        {{ scenic.category }}
                      </el-tag>
                    </div>
                    <p class="text-gray-500 text-sm mt-3 line-clamp-2 h-10">{{ scenic.description }}</p>
                    <div class="flex justify-between items-center mt-4 border-t border-gray-100 pt-3">
                      <div class="flex items-center text-gray-500 text-sm">
                        <el-icon class="text-primary"><Location /></el-icon>
                        <span class="ml-1">{{ scenic.distance }}</span>
                      </div>
                      <el-button type="primary" size="small" @click="viewScenicDetail(scenic.id)" icon="ArrowRight" plain round>查看详情</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="text-center py-10 text-gray-500">
              <el-empty description="暂无该分类景区" />
            </div>
          </el-tab-pane>
          <el-tab-pane label="人文历史" name="history">
            <div v-if="historyScenics.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mt-4">
              <div v-for="(scenic, index) in historyScenics" :key="index" class="scenic-card">
                <div class="h-full group overflow-hidden bg-white rounded-xl border border-gray-100 shadow-sm hover:shadow-lg transition-all duration-300">
                  <div class="relative overflow-hidden h-48">
                    <el-image 
                      :src="normalizeImageUrl(scenic.image)" 
                      class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" 
                      fit="cover" 
                      @error="(e: any) => handleImageError(e)" 
                    />
                    <div class="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                  </div>
                  <div class="p-4">
                    <div class="flex items-start justify-between">
                      <div>
                        <h3 class="text-lg font-bold text-gray-900 group-hover:text-primary transition-colors">{{ scenic.name }}</h3>
                        <div class="flex items-center text-primary text-sm mt-1">
                          <el-rate v-model="scenic.rating" disabled text-color="#F7BA2A" score-template="{value}" />
                          <span class="ml-2 font-bold">{{ scenic.rating }}</span>
                        </div>
                      </div>
                      <el-tag size="small" effect="plain" :type="getTagType(scenic.category)" class="border-none bg-gray-50">
                        {{ scenic.category }}
                      </el-tag>
                    </div>
                    <p class="text-gray-500 text-sm mt-3 line-clamp-2 h-10">{{ scenic.description }}</p>
                    <div class="flex justify-between items-center mt-4 border-t border-gray-100 pt-3">
                      <div class="flex items-center text-gray-500 text-sm">
                        <el-icon class="text-primary"><Location /></el-icon>
                        <span class="ml-1">{{ scenic.distance }}</span>
                      </div>
                      <el-button type="primary" size="small" @click="viewScenicDetail(scenic.id)" icon="ArrowRight" plain round>查看详情</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="text-center py-10 text-gray-500">
              <el-empty description="暂无该分类景区" />
            </div>
          </el-tab-pane>
          <el-tab-pane label="休闲娱乐" name="leisure">
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mt-4">
              <div v-for="(scenic, index) in leisureScenics" :key="index" class="scenic-card">
                <div class="h-full group overflow-hidden bg-white rounded-xl border border-gray-100 shadow-sm hover:shadow-lg transition-all duration-300">
                  <div class="relative overflow-hidden h-48">
                    <el-image 
                      :src="normalizeImageUrl(scenic.image)" 
                      class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" 
                      fit="cover" 
                      @error="(e: any) => handleImageError(e)" 
                    />
                    <div class="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                  </div>
                  <div class="p-4">
                    <div class="flex items-start justify-between">
                      <div>
                        <h3 class="text-lg font-bold text-gray-900 group-hover:text-primary transition-colors">{{ scenic.name }}</h3>
                        <div class="flex items-center text-primary text-sm mt-1">
                          <el-rate v-model="scenic.rating" disabled text-color="#F7BA2A" score-template="{value}" />
                          <span class="ml-2 font-bold">{{ scenic.rating }}</span>
                        </div>
                      </div>
                      <el-tag size="small" effect="plain" :type="getTagType(scenic.category)" class="border-none bg-gray-50">
                        {{ scenic.category }}
                      </el-tag>
                    </div>
                    <p class="text-gray-500 text-sm mt-3 line-clamp-2 h-10">{{ scenic.description }}</p>
                    <div class="flex justify-between items-center mt-4 border-t border-gray-100 pt-3">
                      <div class="flex items-center text-gray-500 text-sm">
                        <el-icon class="text-primary"><Location /></el-icon>
                        <span class="ml-1">{{ scenic.distance }}</span>
                      </div>
                      <el-button type="primary" size="small" @click="viewScenicDetail(scenic.id)" icon="ArrowRight" plain round>查看详情</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>

      </div>
    </div>

    <!-- 景区详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title=""
      width="75%"
      destroy-on-close
      class="scenic-detail-dialog"
      :show-close="false"
    >
      <!-- 自定义头部 -->
      <template #header>
        <div class="detail-dialog-header">
          <div class="header-left">
            <div class="header-icon">
              <el-icon><Location /></el-icon>
            </div>
            <div class="header-text">
              <h2 class="dialog-title">景区详情</h2>
              <p class="dialog-subtitle">{{ currentScenic?.name || '探索精彩景点' }}</p>
            </div>
          </div>
          <button class="close-btn" @click="dialogVisible = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>
      </template>
      
      <div v-if="currentScenic" class="scenic-detail-content">
        <!-- 图片轮播区 -->
        <div class="carousel-section">
          <el-carousel height="320px" indicator-position="outside" :autoplay="true" :interval="4000">
            <el-carousel-item v-for="(img, index) in currentScenic.imageList" :key="index">
              <div class="carousel-image-wrapper">
                <el-image :src="normalizeImageUrl(img)" fit="cover" class="carousel-image" @error="(e: any) => handleImageError(e)" />
                <div class="carousel-overlay"></div>
              </div>
            </el-carousel-item>
          </el-carousel>
        </div>

        <!-- 景区基本信息 -->
        <div class="scenic-info-header">
          <div class="info-left">
            <h2 class="scenic-name">{{ currentScenic.name }}</h2>
            <div class="scenic-rating">
              <el-rate v-model="currentScenic.rating" disabled text-color="#fbbf24" />
              <span class="rating-text">{{ currentScenic.rating }}</span>
              <span class="review-count">· {{ reviews.length }}条评论</span>
            </div>
          </div>
          <div class="info-right">
            <span class="category-tag" :class="getTagType(currentScenic.category)">
              {{ currentScenic.category }}
            </span>
          </div>
        </div>

        <!-- 信息卡片网格 -->
        <div class="info-cards-grid">
          <!-- 景区介绍 -->
          <div class="info-card intro-card">
            <div class="card-header">
              <div class="card-icon intro-icon">
                <span>📖</span>
              </div>
              <h3 class="card-title">景区介绍</h3>
            </div>
            <p class="card-content">{{ currentScenic.fullDescription }}</p>
          </div>
          
          <!-- 位置与交通 -->
          <div class="info-card location-card">
            <div class="card-header">
              <div class="card-icon location-icon">
                <span>📍</span>
              </div>
              <h3 class="card-title">位置与交通</h3>
            </div>
            <p class="address-text">{{ currentScenic.address }}</p>
            <!-- 高德地图显示 -->
            <div class="map-container">
              <AMapLocation
                v-if="currentScenic.longitude && currentScenic.latitude"
                :address="currentScenic.address"
                :longitude="currentScenic.longitude"
                :latitude="currentScenic.latitude"
                :scenic-name="currentScenic.name"
                height="200px"
                :show-traffic="true"
                :show-nearby="false"
              />
              <div v-else class="map-placeholder">
                <el-image :src="normalizeImageUrl(currentScenic.mapImage)" fit="cover" class="map-image" />
                <div class="map-overlay"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 开放信息和客流状态 -->
        <div class="detail-stats-grid">
          <div class="stat-item">
            <div class="stat-icon time-icon">⏰</div>
            <div class="stat-info">
              <span class="stat-label">开放时间</span>
              <span class="stat-value">{{ currentScenic.openingHours }}</span>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-icon price-icon">🎫</div>
            <div class="stat-info">
              <span class="stat-label">门票价格</span>
              <span class="stat-value">{{ currentScenic.price }}</span>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-icon duration-icon">⏱️</div>
            <div class="stat-info">
              <span class="stat-label">建议游玩</span>
              <span class="stat-value">{{ currentScenic.suggestedTime }}</span>
            </div>
          </div>
          <div class="stat-item crowd-stat">
            <div class="stat-icon crowd-icon">👥</div>
            <div class="stat-info">
              <span class="stat-label">当前客流</span>
              <div class="crowd-progress">
                <div class="progress-bar">
                  <div 
                    class="progress-fill"
                    :style="{ width: currentScenic.crowdedness + '%' }"
                    :class="getCrowdClass(currentScenic.crowdedness)"
                  ></div>
                </div>
                <span class="crowd-text">{{ formatCrowd(currentScenic.crowdedness) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <button class="action-btn primary-btn" @click="navigateToPlanning">
            <span class="btn-icon">🗺️</span>
            <span>规划行程</span>
          </button>
          <button class="action-btn secondary-btn" @click="showReviewSection = !showReviewSection">
            <span class="btn-icon">💬</span>
            <span>{{ showReviewSection ? '隐藏评价' : '查看评价' }}</span>
          </button>
          <button class="action-btn danger-btn" @click="showRescueDialog = true">
            <span class="btn-icon">🆘</span>
            <span>紧急救援</span>
          </button>
        </div>

        <!-- 评价区域 -->
        <div v-if="showReviewSection" class="reviews-section">
          <div class="reviews-header">
            <h3 class="reviews-title">
              <span class="title-icon">⭐</span>
              游客评价
            </h3>
            <button class="write-review-btn" @click="showReviewDialog = true">
              <el-icon><Edit /></el-icon>
              写评价
            </button>
          </div>

          <!-- 评价列表 -->
          <div v-if="reviews.length > 0" class="reviews-list">
            <div v-for="review in reviews" :key="review.id" class="review-item">
              <div class="review-header">
                <div class="reviewer-info">
                  <el-avatar :size="44" :src="review.userAvatar" />
                  <div class="reviewer-details">
                    <span class="reviewer-name">{{ review.username }}</span>
                    <el-rate v-model="review.rating" disabled size="small" />
                  </div>
                </div>
                <button 
                  @click="toggleLike(review)"
                  class="like-btn"
                  :class="{ 'liked': review.hasLiked }"
                >
                  <span class="like-icon">{{ review.hasLiked ? '❤️' : '🤍' }}</span>
                  <span class="like-count">{{ review.likes }}</span>
                </button>
              </div>
              
              <p class="review-content">{{ review.content }}</p>
              
              <!-- 评价图片 -->
              <div v-if="review.images && review.images.length > 0" class="review-images">
                <el-image 
                  v-for="(img, idx) in review.images" 
                  :key="idx" 
                  :src="normalizeImageUrl(img)" 
                  fit="cover" 
                  class="review-image"
                  :preview-src-list="review.images.map((i: string) => normalizeImageUrl(i))"
                  :initial-index="idx"
                  @error="(e: any) => handleImageError(e)"
                />
              </div>
              
              <div class="review-footer">
                <span class="review-date">{{ formatDate(review.createdAt) }}</span>
                <div class="review-tags">
                  <span v-for="tag in review.tags" :key="tag" class="review-tag">{{ tag }}</span>
                </div>
              </div>
              
              <!-- 商家回复 -->
              <div v-if="review.merchantReply" class="merchant-reply">
                <div class="reply-header">
                  <el-avatar :size="24" :src="review.merchantReply.avatar" />
                  <span class="reply-label">商家回复</span>
                  <span class="reply-date">{{ formatDate(review.merchantReply.createdAt) }}</span>
                </div>
                <p class="reply-content">{{ review.merchantReply.content }}</p>
              </div>
            </div>
          </div>
          
          <div v-else class="empty-reviews">
            <span class="empty-icon">📝</span>
            <p>暂无评价，快来抢沙发吧！</p>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 紧急救援对话框 -->
    <el-dialog
      v-model="showRescueDialog"
      title=""
      width="680px"
      destroy-on-close
      class="rescue-dialog-scenic"
      :show-close="false"
    >
      <!-- 自定义头部 -->
      <template #header>
        <div class="rescue-dialog-header">
          <div class="header-left">
            <div class="header-icon-wrapper">
              <span class="pulse-ring"></span>
              <span class="header-icon">🆘</span>
            </div>
            <div class="header-text">
              <h2 class="dialog-title">紧急救援</h2>
              <p class="dialog-subtitle">景区工作人员将立即响应</p>
            </div>
          </div>
          <button class="close-btn" @click="showRescueDialog = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>
      </template>
      
      <div class="rescue-dialog-content">
        <!-- 警告提示 -->
        <div class="warning-banner">
          <div class="warning-icon">⚠️</div>
          <div class="warning-text">
            <span class="warning-title">请确保这是真正的紧急情况</span>
            <span class="warning-desc">非紧急情况请通过其他渠道联系景区</span>
          </div>
        </div>

        <!-- 救援类型选择 -->
        <div class="rescue-section">
          <h3 class="section-title">
            <span class="title-icon">📋</span>
            选择救援类型
          </h3>
          <div class="rescue-type-grid">
            <div 
              v-for="type in rescueTypes" 
              :key="type.value"
              class="rescue-type-card"
              :class="{ active: rescueForm.rescueType === type.value }"
              @click="rescueForm.rescueType = type.value"
            >
              <span class="type-icon">{{ type.icon }}</span>
              <span class="type-label">{{ type.label }}</span>
            </div>
          </div>
        </div>

        <!-- 紧急程度 -->
        <div class="rescue-section">
          <h3 class="section-title">
            <span class="title-icon">🚨</span>
            紧急程度
          </h3>
          <div class="emergency-level-group">
            <div 
              class="level-option urgent"
              :class="{ active: rescueForm.emergencyLevel === 'URGENT' }"
              @click="rescueForm.emergencyLevel = 'URGENT'"
            >
              <span class="level-icon">🔴</span>
              <span class="level-text">紧急</span>
              <span class="level-desc">需要立即处理</span>
            </div>
            <div 
              class="level-option normal"
              :class="{ active: rescueForm.emergencyLevel === 'NORMAL' }"
              @click="rescueForm.emergencyLevel = 'NORMAL'"
            >
              <span class="level-icon">🟡</span>
              <span class="level-text">一般</span>
              <span class="level-desc">可稍后处理</span>
            </div>
          </div>
        </div>

        <!-- 联系信息 -->
        <div class="rescue-section">
          <h3 class="section-title">
            <span class="title-icon">📞</span>
            联系信息
          </h3>
          <el-form :model="rescueForm" ref="rescueFormRef" :rules="rescueRules" label-position="top" class="rescue-form">
            <div class="form-row">
              <el-form-item label="联系人姓名" prop="contactName" class="form-item-half">
                <el-input v-model="rescueForm.contactName" placeholder="请输入姓名" class="custom-input">
                  <template #prefix>
                    <span class="input-icon">👤</span>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="联系电话" prop="contactPhone" class="form-item-half">
                <el-input v-model="rescueForm.contactPhone" placeholder="请输入手机号" class="custom-input">
                  <template #prefix>
                    <span class="input-icon">📱</span>
                  </template>
                </el-input>
              </el-form-item>
            </div>
            <el-form-item label="当前位置" prop="location">
              <el-input v-model="rescueForm.location" placeholder="请详细描述您的位置，如：景区入口、山顶观景台等" class="custom-input">
                <template #prefix>
                  <span class="input-icon">📍</span>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="情况描述" prop="description">
              <el-input 
                v-model="rescueForm.description" 
                type="textarea" 
                :rows="3"
                placeholder="请详细描述紧急情况，以便工作人员更好地提供帮助"
                class="custom-textarea"
              />
            </el-form-item>
          </el-form>
        </div>

        <!-- 紧急联系提示 -->
        <div class="emergency-tip">
          <span class="tip-icon">💡</span>
          <span class="tip-text">如遇生命危险，请同时拨打 <strong>120</strong> 或 <strong>110</strong></span>
        </div>
      </div>

      <template #footer>
        <div class="rescue-dialog-footer">
          <button class="cancel-btn" @click="showRescueDialog = false">取消</button>
          <button class="submit-btn" @click="submitRescue" :disabled="rescueSubmitting">
            <span v-if="rescueSubmitting" class="loading-spinner"></span>
            <span v-else class="btn-icon">🆘</span>
            <span>{{ rescueSubmitting ? '提交中...' : '立即求助' }}</span>
          </button>
        </div>
      </template>
    </el-dialog>

    <!-- 写评价对话框 -->
    <el-dialog
      v-model="showReviewDialog"
      title="写评价"
      width="500px"
      destroy-on-close
    >
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.rating" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="4"
            placeholder="分享你的游玩体验..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-checkbox-group v-model="reviewForm.tags">
            <el-checkbox label="风景优美" />
            <el-checkbox label="服务好" />
            <el-checkbox label="交通便利" />
            <el-checkbox label="性价比高" />
            <el-checkbox label="值得推荐" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="游览日期">
          <el-date-picker
            v-model="reviewForm.visitDate"
            type="date"
            placeholder="选择游览日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReviewDialog = false">取消</el-button>
        <neon-button type="primary" @click="submitReview">提交评价</neon-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Location, Close, Edit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import TerrainMap3D from '@/components/TerrainMap3DFree.vue'
import Heatmap3D from '@/components/Heatmap3D.vue'
import AMapLocation from '@/components/AMapLocation.vue'
import { normalizeImageUrl, handleImageError } from '@/utils/imageProxy'
import { getScenicSpots, getScenicDetail, type ScenicSpot } from '@/api/scenic'
import { getReviews, submitReview as submitReviewApi, likeReview, unlikeReview, type Review } from '@/api/review'
import { useDigitalHumanStore } from '@/store/digitalHuman'
import { createRescue } from '@/api/emergencyRescue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const digitalHumanStore = useDigitalHumanStore()
const activeTab = ref('map3d')
const searchKeyword = ref('')
const dialogVisible = ref(false)
const loading = ref(false)
const selectedScenicForHeatmap = ref<number | null>(1) // 默认选择第一个景区（梅花山风景区）
const terrainMapRef = ref()
const heatmapRef = ref()

// 评价相关状态
const showReviewSection = ref(false)
const showReviewDialog = ref(false)
const reviews = ref<Review[]>([])
const reviewForm = ref({
  rating: 5,
  content: '',
  tags: [] as string[],
  visitDate: new Date().toISOString().split('T')[0]
})

// 救援相关状态
const showRescueDialog = ref(false)
const rescueSubmitting = ref(false)
const rescueFormRef = ref()
const rescueForm = ref({
  rescueType: '',
  emergencyLevel: 'NORMAL',
  contactName: '',
  contactPhone: '',
  location: '',
  description: ''
})
const rescueRules = {
  rescueType: [{ required: true, message: '请选择救援类型', trigger: 'change' }],
  contactName: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  location: [{ required: true, message: '请描述您的位置', trigger: 'blur' }],
  description: [{ required: true, message: '请描述紧急情况', trigger: 'blur' }]
}

// 救援类型选项
const rescueTypes = [
  { value: 'MEDICAL', label: '医疗救助', icon: '🏥' },
  { value: 'LOST', label: '走失寻人', icon: '🔍' },
  { value: 'ACCIDENT', label: '事故求助', icon: '⚠️' },
  { value: 'OTHER', label: '其他情况', icon: '📞' }
]

// 是否使用开发模式（使用mock数据）- 设置为false以使用真实后端数据
const isDev = false

interface ScenicItem {
  id: number
  name: string
  category: string
  description: string
  fullDescription: string
  image: string
  imageList: string[]
  mapImage: string
  rating: number
  reviewCount: number
  distance: string
  address: string
  openingHours: string
  price: string
  suggestedTime: string
  crowdedness: number
  longitude?: number  // 经度
  latitude?: number   // 纬度
}
const currentScenic = ref<ScenicItem | null>(null)

// 景区数据
const scenics = ref<ScenicItem[]>([])

// 加载景区列表
const loadScenics = async () => {
  loading.value = true
  try {
    if (isDev) {
      // 开发模式：使用mock数据
      scenics.value = [
  {
    id: 1,
    name: '梅花山风景区',
    category: '自然风光',
    description: '国家4A级景区，山水风光秀丽，是理想的避暑胜地。',
    fullDescription: '梅花山风景区位于六盘水市西部，是国家4A级景区，总面积约68平方公里。景区内植被茂密，气候宜人，年平均气温15℃，是理想的避暑胜地。区内有奇峰异石、飞瀑流泉、古树名花等自然景观，还有梅园、杜鹃园等人工景点。',
    image: '/images/1/gallery/202510/1760618835892.jpg',
    imageList: [
      '/images/1/gallery/202510/1760618835892.jpg',
      '/images/1/gallery/202510/1760618835892.jpg',
      '/images/1/gallery/202510/1760618835892.jpg'
    ],
    mapImage: '/images/1/gallery/202510/1760618835892.jpg',
    rating: 4.7,
    reviewCount: 328,
    distance: '12.5公里',
    address: '贵州省六盘水市钟山区大湾镇',
    openingHours: '08:00-18:00',
    price: '¥60/人',
    suggestedTime: '3-4小时',
    crowdedness: 45,
    longitude: 104.830392,
    latitude: 26.592745
  },
  {
    id: 2,
    name: '玉舍国家森林公园',
    category: '自然风光',
    description: '拥有丰富的野生动植物资源，自然环境优美。',
    fullDescription: '玉舍国家森林公园位于六盘水市南部，总面积约100平方公里，森林覆盖率达95%以上。公园内有众多珍稀野生动植物，自然环境优美，空气清新，是避暑、休闲、科考的理想场所。',
    image: '/images/2/gallery/202510/1760618856858.jpg',
    imageList: [
      '/images/2/gallery/202510/1760618856858.jpg',
      '/images/2/gallery/202510/1760618856858.jpg',
      '/images/2/gallery/202510/1760618856858.jpg'
    ],
    mapImage: '/images/2/gallery/202510/1760618856858.jpg',
    rating: 4.5,
    reviewCount: 214,
    distance: '18公里',
    address: '贵州省六盘水市水城县玉舍镇',
    openingHours: '全天开放',
    price: '¥40/人',
    suggestedTime: '2-3小时',
    crowdedness: 30,
    longitude: 104.819500,
    latitude: 26.230800
  },
  {
    id: 3,
    name: '乌蒙大草原',
    category: '自然风光',
    description: '广袤草原，风景如画，是观赏草原风光和民族风情的绝佳去处。',
    fullDescription: '乌蒙大草原位于六盘水市西部，海拔2000多米，是贵州省最大的高原草场之一。这里草原辽阔，牛羊成群，夏季绿草如茵，秋季层林尽染。草原上分布着彝族、苗族等少数民族村寨，民族风情浓郁。每年7-8月是观赏草原最佳季节，也是举办草原那达慕等民族活动的时期。',
    image: '/images/3/gallery/202510/1760619222708.jpg',
    imageList: [
      '/images/3/gallery/202510/1760619222708.jpg',
      '/images/3/gallery/202510/1760619222708.jpg',
      '/images/3/gallery/202510/1760619222708.jpg'
    ],
    mapImage: '/images/3/gallery/202510/1760619222708.jpg',
    rating: 4.8,
    reviewCount: 452,
    distance: '45公里',
    address: '贵州省六盘水市钟山区',
    openingHours: '08:00-17:30',
    price: '¥65/人',
    suggestedTime: '4-5小时',
    crowdedness: 60,
    longitude: 104.910356,
    latitude: 26.650123
  },
  {
    id: 4,
    name: '水城古镇',
    category: '人文历史',
    description: '百年古镇，历史文化底蕴深厚，民俗风情浓郁。',
    fullDescription: '水城古镇历史悠久，是贵州西部重要的历史文化名镇。古镇保存了大量明清时期的古建筑，青石板街道，木质结构房屋，体现了当地独特的建筑风格。这里还有丰富的民俗文化和特色美食，是了解当地历史文化的重要窗口。',
    image: '/images/4/gallery/202510/1760619249739.jpg',
    imageList: [
      '/images/4/gallery/202510/1760619249739.jpg',
      '/images/4/gallery/202510/1760619249739.jpg',
      '/images/4/gallery/202510/1760619249739.jpg'
    ],
    mapImage: '/images/4/gallery/202510/1760619249739.jpg',
    rating: 4.4,
    reviewCount: 216,
    distance: '28公里',
    address: '贵州省六盘水市水城县',
    openingHours: '全天开放',
    price: '免费',
    suggestedTime: '3小时',
    crowdedness: 45,
    longitude: 104.950000,
    latitude: 26.550000
  },
  {
    id: 5,
    name: '明湖国家湿地公园',
    category: '休闲娱乐',
    description: '湖光山色，水上活动丰富。',
    fullDescription: '明湖国家湿地公园位于六盘水市中心城区，总面积约8平方公里，其中水域面积约3平方公里。公园内湖光山色相映成趣，环境优美，有水上游乐区、湿地观鸟区、休闲垂钓区等功能区域，是市民休闲娱乐的理想去处。',
    image: '/images/5/gallery/202510/1760619288590.jpg',
    imageList: [
      '/images/5/gallery/202510/1760619288590.jpg',
      '/images/5/gallery/202510/1760619288590.jpg',
      '/images/5/gallery/202510/1760619288590.jpg'
    ],
    mapImage: '/images/5/gallery/202510/1760619288590.jpg',
    rating: 4.6,
    reviewCount: 278,
    distance: '5公里',
    address: '贵州省六盘水市钟山区',
    openingHours: '08:00-21:00',
    price: '免费',
    suggestedTime: '2-3小时',
    crowdedness: 85,
    longitude: 105.171500,
    latitude: 26.203800
  }
      ]
    } else {
      // 生产模式：调用API
      const response = await getScenicSpots({ city: '六盘水' })
      const apiData =
        (response as any)?.data?.data ||
        (response as any)?.data ||
        (response as any)?.list ||
        response ||
        []

      // 转换API数据为前端所需格式
      scenics.value = (Array.isArray(apiData) ? apiData : []).map((item: any) => {
        const imageUrl = item.imageUrl || item.image_url || '/images/1/gallery/202510/1760618835892.jpg'
        const mapImage = item.mapImage || item.map_image || imageUrl
        const maxCapacity = item.maxCapacity || item.max_capacity || 1
        const currentFlow = item.currentFlow || item.current_flow || 0
        
        // 使用后端返回的图片列表，如果没有则使用主图
        let imageList = item.images || item.imageList || item.image_list || []
        if (!Array.isArray(imageList) || imageList.length === 0) {
          imageList = [imageUrl]
        }

        return {
          id: item.id,
          name: item.name,
          category: mapCategory(item.tags || []),
          description: item.description,
          fullDescription: item.description,
          image: imageUrl,
          imageList: imageList,
          mapImage,
          rating: item.rating,
          reviewCount: Math.floor(Math.random() * 500),
          distance: `${Math.floor(Math.random() * 50)}公里`,
          address: `贵州省六盘水市`,
          openingHours: `${item.openingHours?.open || '08:00'}-${item.openingHours?.close || '18:00'}`,
          price: `¥${item.ticketPrice || 0}/人`,
          suggestedTime: '2-4小时',
          crowdedness: Math.floor(Math.min(1, currentFlow / maxCapacity) * 100)
        }
      })
    }
  } catch (error) {
    console.error('加载景区列表失败:', error)
    ElMessage.error('加载景区列表失败')
  } finally {
    loading.value = false
  }
}

// 将API标签映射为分类
const mapCategory = (tags: string[] = []): string => {
  if (tags.includes('自然风光') || tags.includes('natural')) return '自然风光'
  if (tags.includes('人文历史') || tags.includes('history')) return '人文历史'
  if (tags.includes('休闲娱乐') || tags.includes('leisure')) return '休闲娱乐'
  return '自然风光'
}

// 根据搜索关键词过滤景区列表
const filteredScenics = computed(() => {
  if (!searchKeyword.value) return scenics.value
  return scenics.value.filter(scenic => 
    scenic.name.includes(searchKeyword.value) || 
    scenic.description.includes(searchKeyword.value) ||
    scenic.category.includes(searchKeyword.value)
  )
})

// 分类筛选后的列表
const natureScenics = computed(() => {
  return filteredScenics.value.filter(s => s.category === '自然风光')
})
const historyScenics = computed(() => {
  return filteredScenics.value.filter(s => s.category === '人文历史')
})
const leisureScenics = computed(() => {
  return filteredScenics.value.filter(s => s.category === '休闲娱乐')
})

// 获取标签类型
const getTagType = (category: string) => {
  const types: Record<string, string> = {
    '自然风光': 'success',
    '人文历史': 'warning',
    '休闲娱乐': 'info'
  }
  return types[category] || ''
}

// 查看景区详情
const viewScenicDetail = async (id: number) => {
  const scenic = scenics.value.find(s => s.id === id)
  if (scenic) {
    currentScenic.value = scenic
    dialogVisible.value = true
    showReviewSection.value = false // 重置评价区域显示状态
    
    // 触发数字人景区讲解
    digitalHumanStore.triggerScenicNarration(scenic.name)
    
    // 加载详细信息（包括图片列表）
    try {
      const detail = await getScenicDetail(id) as any
      const detailData = detail?.data || detail
      // 更新当前景区的详细信息
      if (currentScenic.value && currentScenic.value.id === id) {
        currentScenic.value.fullDescription = detailData?.fullDescription || detailData?.description || currentScenic.value.description
        // 更新图片列表（从后端获取真实的多张图片）
        const images = detailData?.images || detailData?.imageList || []
        if (Array.isArray(images) && images.length > 0) {
          currentScenic.value.imageList = images
        }
      }
    } catch (error) {
      console.error('加载景区详情失败:', error)
    }
    
    // 加载评价列表
    await loadReviews(id)
  }
}

// 加载评价列表
const loadReviews = async (scenicId: number) => {
  try {
    const response = await getReviews(scenicId, { page: 1, size: 10, sortBy: 'newest' })
    const data: any = response?.data || response
    // 确保每条评价都有完整的用户信息和必要字段
    reviews.value = (data?.reviews || []).map((review: any) => ({
      ...review,
      username: review.username || review.userNickname || '匿名用户',
      userAvatar: review.userAvatar || review.avatar || '',
      merchantReply: review.merchantReply,
      hasLiked: review.hasLiked || false,
      likes: review.likes || 0
    }))
  } catch (error) {
    // 加载评价失败
    reviews.value = []
  }
}

// 提交评价
const submitReview = async () => {
  if (!currentScenic.value) return
  
  // 验证
  if (!reviewForm.value.content.trim()) {
    ElMessage.warning('请填写评价内容')
    return
  }
  
  // 检查是否登录（优先检查localStorage中的token）
  const token = localStorage.getItem('token')
  if (!token && !userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  try {
    await submitReviewApi({
      scenicId: currentScenic.value.id,
      rating: reviewForm.value.rating,
      content: reviewForm.value.content,
      tags: reviewForm.value.tags,
      visitDate: reviewForm.value.visitDate
    })
    
    ElMessage.success('评价提交成功！')
    showReviewDialog.value = false
    
    // 重置表单
    reviewForm.value = {
      rating: 5,
      content: '',
      tags: [],
      visitDate: new Date().toISOString().split('T')[0]
    }
    
    // 重新加载评价列表
    await loadReviews(currentScenic.value.id)
  } catch (error: any) {
    console.error('提交评价失败:', error)
    ElMessage.error(error?.message || '提交评价失败')
  }
}

// 切换点赞
const toggleLike = async (review: Review) => {
  // 检查是否登录（优先检查localStorage中的token）
  const token = localStorage.getItem('token')
  if (!token && !userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  try {
    if (review.hasLiked) {
      await unlikeReview(review.id)
      review.hasLiked = false
      review.likes--
      ElMessage.success('已取消点赞')
    } else {
      await likeReview(review.id)
      review.hasLiked = true
      review.likes++
      ElMessage.success('点赞成功')
    }
  } catch (error: any) {
    console.error('点赞操作失败:', error)
    ElMessage.error(error?.message || '操作失败')
  }
}

// 格式化日期
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  if (days < 365) return `${Math.floor(days / 30)}个月前`
  return `${Math.floor(days / 365)}年前`
}

// 组件挂载时加载数据
onMounted(() => {
  loadScenics()
})


// 格式化拥挤度显示
const formatCrowd = (percentage: number) => {
  if (percentage < 30) return '空闲'
  if (percentage < 70) return '适中'
  if (percentage < 90) return '拥挤'
  return '非常拥挤'
}

// 获取拥挤度样式类
const getCrowdClass = (percentage: number) => {
  if (percentage < 30) return 'low'
  if (percentage < 70) return 'medium'
  return 'high'
}

// 导航到行程规划页面
const navigateToPlanning = () => {
  dialogVisible.value = false
  router.push('/user/planning')
}

// 提交救援请求
const submitRescue = async () => {
  // 检查是否登录
  const token = localStorage.getItem('token')
  if (!token && !userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    showRescueDialog.value = false
    router.push('/login')
    return
  }

  // 表单验证
  if (!rescueFormRef.value) return
  
  try {
    await rescueFormRef.value.validate()
  } catch (error) {
    ElMessage.error('请填写完整的救援信息')
    return
  }

  rescueSubmitting.value = true

  try {
    const rescueData = {
      scenicId: currentScenic.value!.id,
      rescueType: rescueForm.value.rescueType,
      emergencyLevel: rescueForm.value.emergencyLevel,
      contactName: rescueForm.value.contactName,
      contactPhone: rescueForm.value.contactPhone,
      location: rescueForm.value.location,
      description: rescueForm.value.description
    }

    await createRescue(rescueData)
    
    ElMessage.success({
      message: '救援请求已提交！景区工作人员将立即响应',
      duration: 3000,
      showClose: true
    })
    
    showRescueDialog.value = false
    
    // 重置表单
    rescueForm.value = {
      rescueType: '',
      emergencyLevel: 'NORMAL',
      contactName: '',
      contactPhone: '',
      location: '',
      description: ''
    }
  } catch (error: any) {
    console.error('提交救援请求失败:', error)
    ElMessage.error(error?.response?.data?.message || '提交失败，请稍后重试')
  } finally {
    rescueSubmitting.value = false
  }
}

// 处理3D地图标记点击 → 跳转到实时服务页，数字人自动讲解景区
const handleMarkerClick = (spot: any) => {
  ElMessage({
    message: `正在前往「${spot.name}」实时服务页面...`,
    type: 'success',
    duration: 2000,
    showClose: true
  })
  
  // 跳转到实时服务页，带上景区 ID 和名称（用于数字人讲解）
  router.push({
    path: '/user/service',
    query: { scenicId: String(spot.id), scenicName: spot.name }
  })
}

// 处理景区选择变化
const handleScenicChange = (scenicId: number) => {
  const scenic = scenics.value.find(s => s.id === scenicId)
  if (scenic) {
    ElMessage({
      message: `正在加载 ${scenic.name} 的客流热力图`,
      type: 'info',
      duration: 1500
    })
  }
}

// 根据景区ID获取景区名称
const getScenicName = (scenicId: number | null) => {
  if (!scenicId) return ''
  const scenic = scenics.value.find(s => s.id === scenicId)
  return scenic ? scenic.name : ''
}

// 处理标签页切换，修复热力图和地图的尺寸问题
const handleTabChange = (tabName: string) => {
  // 由于使用了v-if，组件会在标签页激活时重新渲染
  // 只需要一次resize调用作为保险
  if (tabName === 'heatmap3d') {
    setTimeout(() => {
      if (heatmapRef.value?.resize) {
        heatmapRef.value.resize()
      }
    }, 400)
  }
}
</script>\n\n
<style scoped>
.scenic-explore-container {
  padding: 24px;
  background-color: transparent;
  min-height: 100vh;
}

/* 标签页样式重写 */
/* 搜索框样式强制重置 */
:deep(.scenic-search-input .el-input__wrapper) {
  background-color: #ffffff !important; /* 强制白底 */
  box-shadow: 0 0 0 1px #dcdfe6 inset !important; /* 浅灰边框 */
  border-radius: 20px !important; /* 圆角 */
  padding-left: 12px;
}

:deep(.scenic-search-input .el-input__inner) {
  color: #303133 !important; /* 深灰文字 */
  height: 36px;
}

:deep(.scenic-search-input .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--primary) inset !important;
}

:deep(.scenic-search-input .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--primary) inset !important;
  box-shadow: 0 0 0 1px var(--primary) inset, 0 0 0 4px rgba(42, 157, 143, 0.1) !important;
}

/* 确保主色调变量定义 */
.scenic-explore-container {
  --primary: #2A9D8F;
  --primary-rgb: 42, 157, 143;
}

.text-primary {
  color: var(--primary);
}

/* 全局选中文字样式修正 */
:deep(::selection) {
  background-color: rgba(42, 157, 143, 0.2) !important; /* 浅青色背景 */
  color: #1a5c54 !important; /* 深青色文字 */
}

/* 标签页样式重写 */
:deep(.el-tabs__header) {
  margin-bottom: 24px;
}

:deep(.el-tabs__nav-wrap::after) {
  background-color: #E5E6EB;
}

:deep(.el-tabs__item) {
  font-size: 16px;
  color: #4E5969;
  font-weight: 500;
}

:deep(.el-tabs__item.is-active) {
  color: var(--primary);
  font-weight: 600;
}

:deep(.el-tabs__active-bar) {
  background-color: var(--primary);
  height: 3px;
  border-radius: 3px;
}

/* 分类标签 */
.category-tag {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.category-tag.nature { background-color: #E6FFFA; color: #059669; }
.category-tag.history { background-color: #FEF3C7; color: #D97706; }
.category-tag.leisure { background-color: #E0F2FE; color: #0284C7; }

/* 信息卡片网格 */
.info-cards-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  padding: 0 28px 24px;
}

.info-card {
  background: #F9FAFB;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #E5E6EB;
  transition: all 0.3s ease;
}

.info-card:hover {
  background: #FFFFFF;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.intro-icon { background: #E0F2FE; color: #0284C7; }
.location-icon { background: #E6FFFA; color: #059669; }

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1F2937;
  margin: 0;
}

.card-content {
  font-size: 14px;
  color: #4B5563;
  line-height: 1.7;
  margin: 0;
}

.address-text {
  font-size: 14px;
  color: #6B7280;
  margin: 0 0 14px 0;
}

.map-container {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #E5E6EB;
}

/* 详情统计网格 */
.detail-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding: 0 28px 24px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: #F9FAFB;
  border-radius: 12px;
  border: 1px solid #E5E6EB;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.time-icon { background: #FEF3C7; color: #D97706; }
.price-icon { background: #E0F2FE; color: #0284C7; }
.duration-icon { background: #F3E8FF; color: #7C3AED; }
.crowd-icon { background: #FFEDD5; color: #EA580C; }

.stat-info {
  flex: 1;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: #6B7280;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 15px;
  font-weight: 600;
  color: #1F2937;
}

.crowd-stat {
  grid-column: span 1;
}

.crowd-progress {
  display: flex;
  align-items: center;
  gap: 10px;
}

.crowd-progress .progress-bar {
  flex: 1;
  height: 8px;
  background: #E5E6EB;
  border-radius: 4px;
  overflow: hidden;
}

.crowd-progress .progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.crowd-progress .progress-fill.low { background: #10B981; }
.crowd-progress .progress-fill.medium { background: #F59E0B; }
.crowd-progress .progress-fill.high { background: #EF4444; }

.crowd-text {
  font-size: 12px;
  color: #6B7280;
  min-width: 50px;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 0 28px 28px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
}

.primary-btn {
  background: var(--primary);
  color: #fff;
  box-shadow: 0 4px 6px -1px rgba(var(--primary-rgb), 0.1), 0 2px 4px -1px rgba(var(--primary-rgb), 0.06);
}

.primary-btn:hover {
  background: #23897d; /* darker primary */
  transform: translateY(-1px);
}

.secondary-btn {
  background: #FFFFFF;
  border: 1px solid #E5E6EB;
  color: #4B5563;
}

.secondary-btn:hover {
  background: #F3F4F6;
  border-color: #D1D5DB;
}

.danger-btn {
  background: #FEF2F2;
  border: 1px solid #FECACA;
  color: #EF4444;
}

.danger-btn:hover {
  background: #FEE2E2;
}

.btn-icon {
  font-size: 18px;
}

/* 评价区域 */
.reviews-section {
  padding: 28px;
  border-top: 1px solid #E5E6EB;
  background: #F9FAFB;
}

.reviews-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.reviews-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #1F2937;
  margin: 0;
}

.write-review-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #FFFFFF;
  border: 1px solid #E5E6EB;
  border-radius: 6px;
  color: #4B5563;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.write-review-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: #E6FFFA;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  background: #FFFFFF;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #E5E6EB;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.reviewer-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.reviewer-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.reviewer-name {
  font-size: 15px;
  font-weight: 600;
  color: #1F2937;
}

.like-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 16px;
  background: #F3F4F6;
  border: none;
  cursor: pointer;
  transition: all 0.3s;
}

.like-btn:hover {
  background: #E5E6EB;
}

.like-btn.liked {
  background: #FCE7F3;
  color: #EC4899;
}

.review-content {
  font-size: 14px;
  color: #4B5563;
  line-height: 1.6;
  margin: 0 0 12px 0;
}

.review-images {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.review-image {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.3s;
}

.review-image:hover {
  transform: scale(1.05);
}

.review-footer {
  display: flex;
  align-items: center;
  gap: 12px;
}

.review-date {
  font-size: 12px;
  color: #9CA3AF;
}

.review-tags {
  display: flex;
  gap: 8px;
}

.review-tag {
  padding: 2px 8px;
  background: #F3F4F6;
  border-radius: 4px;
  font-size: 11px;
  color: #6B7280;
}

.merchant-reply {
  margin-top: 14px;
  padding: 14px;
  background: #F0FDF4;
  border-radius: 8px;
  border-left: 3px solid #10B981;
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.reply-label {
  font-size: 13px;
  font-weight: 600;
  color: #059669;
}

.reply-content {
  font-size: 13px;
  color: #374151;
  line-height: 1.5;
  margin: 0;
}

.empty-reviews {
  text-align: center;
  padding: 40px 0;
}

.empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 12px;
}

@media (max-width: 900px) {
  .info-cards-grid {
    grid-template-columns: 1fr;
  }
  
  .detail-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .action-buttons {
    flex-wrap: wrap;
  }
}
</style>

<style scoped>
/* 紧急救援弹窗样式 - 浅色版 */
:deep(.rescue-dialog-scenic .el-dialog) {
  background: #FFFFFF !important;
  border-radius: 16px !important;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.25) !important;
  overflow: hidden;
}

:deep(.rescue-dialog-scenic .el-dialog__header) {
  padding: 0 !important;
  margin: 0 !important;
}

:deep(.rescue-dialog-scenic .el-dialog__body) {
  padding: 0 !important;
}

:deep(.rescue-dialog-scenic .el-dialog__footer) {
  padding: 0 !important;
  border-top: 1px solid #E5E6EB;
}

.rescue-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: #FEF2F2;
  border-bottom: 1px solid #FECACA;
}

.rescue-dialog-header .header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.rescue-dialog-header .header-icon-wrapper {
  position: relative;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rescue-dialog-header .pulse-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: rgba(239, 68, 68, 0.2);
  animation: pulse-ring 1.5s ease-out infinite;
}

@keyframes pulse-ring {
  0% { transform: scale(0.8); opacity: 1; }
  100% { transform: scale(1.5); opacity: 0; }
}

.rescue-dialog-header .header-icon {
  font-size: 28px;
  position: relative;
  z-index: 1;
}

.rescue-dialog-header .header-text .dialog-title {
  font-size: 20px;
  font-weight: 700;
  color: #EF4444;
  margin: 0;
}

.rescue-dialog-header .header-text .dialog-subtitle {
  font-size: 13px;
  color: #7F1D1D;
  margin: 4px 0 0 0;
}

.rescue-dialog-header .close-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: transparent;
  color: #6B7280;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.rescue-dialog-header .close-btn:hover {
  background: #FEE2E2;
  color: #EF4444;
}

.rescue-dialog-content {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.warning-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: #FFFBEB;
  border: 1px solid #FCD34D;
  border-radius: 8px;
  margin-bottom: 24px;
}

.warning-banner .warning-icon {
  font-size: 24px;
}

.warning-banner .warning-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.warning-banner .warning-title {
  font-size: 14px;
  font-weight: 600;
  color: #D97706;
}

.warning-banner .warning-desc {
  font-size: 12px;
  color: #92400E;
}

.rescue-section {
  margin-bottom: 24px;
}

.rescue-section .section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 14px 0;
}

.rescue-type-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.rescue-type-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  background: #F9FAFB;
  border: 1px solid #E5E6EB;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.rescue-type-card:hover {
  background: #FEF2F2;
  border-color: #FECACA;
  transform: translateY(-2px);
}

.rescue-type-card.active {
  background: #FEF2F2;
  border-color: #EF4444;
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.1);
}

.rescue-type-card .type-label {
  font-size: 13px;
  color: #6B7280;
  font-weight: 500;
}

.rescue-type-card.active .type-label {
  color: #EF4444;
}

.emergency-level-group {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.level-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #F9FAFB;
  border: 1px solid #E5E6EB;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.level-option:hover {
  transform: translateY(-2px);
}

.level-option.urgent:hover,
.level-option.urgent.active {
  background: #FEF2F2;
  border-color: #EF4444;
}

.level-option.normal:hover,
.level-option.normal.active {
  background: #FFFBEB;
  border-color: #F59E0B;
}

.level-option .level-text {
  font-size: 15px;
  font-weight: 600;
  color: #374151;
}

.level-option .level-desc {
  font-size: 12px;
  color: #9CA3AF;
  margin-left: auto;
}

.rescue-form {
  background: #F9FAFB;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #E5E6EB;
}

.rescue-form .form-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.rescue-form :deep(.el-form-item__label) {
  color: #4B5563 !important;
}

.rescue-form .custom-input :deep(.el-input__wrapper),
.rescue-form .custom-textarea :deep(.el-textarea__inner) {
  background: #FFFFFF !important;
  border: 1px solid #E5E6EB !important;
  border-radius: 8px !important;
  box-shadow: none !important;
  color: #1F2937 !important;
}

.rescue-form .custom-input :deep(.el-input__wrapper:hover),
.rescue-form .custom-textarea :deep(.el-textarea__inner:hover) {
  border-color: #EF4444 !important;
}

.rescue-form .custom-input :deep(.el-input__wrapper.is-focus),
.rescue-form .custom-textarea :deep(.el-textarea__inner:focus) {
  border-color: #EF4444 !important;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1) !important;
}

.rescue-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
}

.cancel-btn {
  padding: 10px 20px;
  border-radius: 8px;
  border: 1px solid #E5E6EB;
  background: #FFFFFF;
  color: #4B5563;
  cursor: pointer;
}

.submit-btn {
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  background: #EF4444;
  color: #FFFFFF;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
}

.submit-btn:hover {
  background: #DC2626;
}

.submit-btn:disabled {
  background: #FCA5A5;
  cursor: not-allowed;
}

.emergency-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background-color: #FEF2F2;
  border-radius: 8px;
  margin-top: 16px;
  color: #B91C1C;
  font-size: 13px;
}

.loading-spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid #ffffff;
  border-radius: 50%;
  border-top-color: transparent;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>


<style>
/* 下拉菜单自定义样式 (非 scoped) */
.scenic-select-dropdown.el-select-dropdown {
  background-color: #ffffff !important;
  border: 1px solid #f3f4f6 !important;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04) !important; /* shadow-xl */
  border-radius: 0.5rem !important; /* rounded-lg */
  padding: 4px !important;
  
  /* 强制重写 Element Plus 变量 */
  --el-bg-color-overlay: #ffffff !important;
  --el-fill-color-light: #f9fafb !important;
  --el-border-color-light: #f3f4f6 !important;
  --el-text-color-regular: #374151 !important;
}

.scenic-select-dropdown .el-scrollbar {
  background-color: transparent !important;
}

.scenic-select-dropdown .el-select-dropdown__list {
  background-color: #ffffff !important;
}

.scenic-select-dropdown .el-select-dropdown__item {
  color: #374151 !important; /* text-gray-700 */
  border-bottom: 1px solid #f9fafb !important;
  margin-bottom: 2px !important;
  border-radius: 4px !important;
}

.scenic-select-dropdown .el-select-dropdown__item:last-child {
  border-bottom: none !important;
}

.scenic-select-dropdown .el-select-dropdown__item.hover,
.scenic-select-dropdown .el-select-dropdown__item:hover {
  background-color: #f9fafb !important; /* hover:bg-gray-50 */
}

.scenic-select-dropdown .el-select-dropdown__item.selected {
  background-color: #f0fdfa !important; /* bg-teal-50 */
  color: #0d9488 !important; /* text-teal-600 */
  font-weight: 600 !important;
}
</style>

<style scoped>
/* 搜索框和下拉选择框通用样式 */
:deep(.scenic-search-input .el-input__wrapper),
:deep(.scenic-select .el-select__wrapper) {
  background-color: #ffffff !important; /* 强制白底 */
  box-shadow: 0 0 0 1px #dcdfe6 inset !important; /* 浅灰边框 */
  border-radius: 20px !important; /* 圆角 */
}

/* 兼容 Element Plus 不同版本的 Select wrapper 类名 */
:deep(.scenic-select .el-input__wrapper) {
  background-color: #ffffff !important;
  box-shadow: 0 0 0 1px #dcdfe6 inset !important;
  border-radius: 20px !important;
}

:deep(.scenic-search-input .el-input__inner),
:deep(.scenic-select .el-input__inner),
:deep(.scenic-select .el-select__input) {
  color: #303133 !important; /* 深灰文字 */
}

:deep(.scenic-search-input .el-input__wrapper:hover),
:deep(.scenic-select .el-select__wrapper:hover),
:deep(.scenic-select .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--primary) inset !important;
}

:deep(.scenic-search-input .el-input__wrapper.is-focus),
:deep(.scenic-select .el-select__wrapper.is-focused),
:deep(.scenic-select .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--primary) inset !important;
  box-shadow: 0 0 0 1px var(--primary) inset, 0 0 0 4px rgba(42, 157, 143, 0.1) !important;
}
</style>

<style>
/* 景区详情弹窗样式 - 浅色版 */
.scenic-detail-dialog.el-dialog {
  background: #FFFFFF !important;
  border-radius: 16px !important;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.25) !important;
  overflow: hidden;
  
  /* 强制覆盖变量 */
  --el-dialog-bg-color: #FFFFFF !important;
  --el-text-color-primary: #1F2937 !important;
  --el-text-color-regular: #4B5563 !important;
}

.scenic-detail-dialog .el-dialog__header {
  padding: 0 !important;
  margin: 0 !important;
  background: #FFFFFF !important;
}

.scenic-detail-dialog .el-dialog__body {
  padding: 0 !important;
  background: #FFFFFF !important;
  color: #4B5563 !important; 
}

/* 详情弹窗头部 */
.detail-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: #FFFFFF;
  border-bottom: 1px solid #E5E6EB;
}

.detail-dialog-header .header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.detail-dialog-header .header-icon {
  width: 48px;
  height: 48px;
  background: #F0FDF4;
  color: #10B981;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  border: 1px solid #D1FAE5;
}

.detail-dialog-header .header-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-dialog-header .dialog-title {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.detail-dialog-header .dialog-subtitle {
  font-size: 14px;
  color: #6B7280;
  margin: 0;
}

.detail-dialog-header .close-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #F3F4F6;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  color: #4B5563;
}

.detail-dialog-header .close-btn:hover {
  background: #E5E6EB;
  color: #1F2937;
  transform: rotate(90deg);
}

/* 内部滚动区域 */
.scenic-detail-content {
  height: 70vh;
  overflow-y: auto;
  background: #FFFFFF;
}

/* 轮播图区域 */
.carousel-section {
  position: relative;
  background: #000;
}

/* 标题区域 */
.scenic-info-header {
  padding: 24px 28px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: #FFFFFF;
}

.scenic-name {
  font-size: 28px;
  font-weight: 800;
  color: #111827;
  margin: 0 0 12px 0;
  letter-spacing: -0.5px;
}

/* 信息卡片 */
.info-cards-grid {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 24px;
  padding: 0 28px 24px;
}

.info-card {
  background: #F9FAFB;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #E5E6EB;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: #1F2937;
  margin: 0;
}

.card-content {
  font-size: 15px;
  line-height: 1.7;
  color: #4B5563;
}

.address-text {
  color: #6B7280;
}
</style>
