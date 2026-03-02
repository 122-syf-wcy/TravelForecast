<template>
  <div class="real-time-service-container">
    <div class="flex gap-6 items-start">
      <!-- 实时信息面板 -->
      <!-- 实时信息面板 -->
      <div class="md:col-span-8">
        <!-- 景区选择卡片 -->
        <scenic-card class="mb-0" style="height: auto;">
          <div class="p-5 selector-section">
          <div class="selector-header">
            <div class="header-left">
              <div class="header-icon">
                <el-icon><Location /></el-icon>
              </div>
              <div class="header-text">
                <h3 class="header-title text-gray-800">选择景区</h3>
                <p class="header-subtitle text-gray-500">点击切换查看不同景区实时状态</p>
              </div>
            </div>
            <el-button type="primary" size="small" plain @click="refreshScenicData">刷新数据</el-button>
          </div>
          <div class="scenic-grid">
            <div 
              v-for="option in scenicOptions" 
              :key="option.value"
              class="scenic-card"
              :class="{ active: selectedScenic === option.value }"
              @click="changeScenic(option.value)"
            >
              <div class="scenic-image-container">
                <el-image 
                  :src="getScenicImage(option.value)" 
                  fit="cover" 
                  class="scenic-img"
                />
                <div class="scenic-overlay"></div>
                <div class="scenic-glow" v-if="selectedScenic === option.value"></div>
              </div>
              <div class="scenic-content">
                <div class="scenic-name">{{ option.label }}</div>
                <div class="scenic-status-badge" v-if="getScenicStatusForCard(option.value)">
                  <span class="status-dot" :class="getStatusType(getScenicStatusForCard(option.value))"></span>
                  <span class="status-text">{{ getStatusText(getScenicStatusForCard(option.value)) }}</span>
                </div>
              </div>
              <div class="selected-indicator" v-if="selectedScenic === option.value">
                <el-icon><Check /></el-icon>
              </div>
            </div>
          </div> <!-- Close scenic-grid -->
          </div> <!-- Close selector-section -->
        </scenic-card>

        <!-- 实时景区状态 -->
        <scenic-card v-if="currentScenic" class="mt-6 status-card">
        <div class="p-6 status-section">
          <div class="status-header">
            <div class="status-title-wrapper">
                <div class="title-icon">
                  <el-icon><DataAnalysis /></el-icon>
                </div>
                <div class="title-content">
                  <h2 class="status-title text-gray-800">实时景区状态</h2>
                  <p class="status-subtitle text-gray-500">{{ currentScenic?.name || '请选择景区' }}</p>
                </div>
              </div>
              <el-select v-model="selectedScenic" placeholder="选择景区" size="small" class="scenic-select" @change="changeScenic">
                <el-option
                  v-for="item in scenicOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </div>
            <!-- 景区信息展示 -->
            <div class="scenic-info-section">
              <div class="scenic-main-image">
                <el-image 
                  :src="currentScenic.image" 
                  fit="cover" 
                  class="main-img"
                />
                <div class="image-overlay-gradient"></div>
                <div class="image-badges">
                  <span class="badge level-badge">🏔️ 4A景区</span>
                  <span class="badge hot-badge" v-if="currentScenic.recommended">🔥 推荐</span>
                </div>
              </div>
              <div class="scenic-details">
                <h3 class="scenic-title">{{ currentScenic.name }}</h3>
                <div class="scenic-meta">
                  <div class="status-tag" :class="getStatusType(currentScenic.status)">
                    <span class="status-indicator"></span>
                    <span>{{ getStatusText(currentScenic.status) }}</span>
                  </div>
                  <span class="update-time">
                    <el-icon><Clock /></el-icon>
                    更新于 {{ currentScenic.updateTime }}
                  </span>
                </div>
                <p class="scenic-description">{{ currentScenic.description }}</p>
                <div class="quick-stats">
                  <div class="quick-stat">
                    <span class="stat-icon">👥</span>
                    <span class="stat-value">{{ currentScenic.visitorCount }}</span>
                    <span class="stat-label">当前人数</span>
                  </div>
                  <div class="quick-stat">
                    <span class="stat-icon">⏱️</span>
                    <span class="stat-value">{{ currentScenic.waitTime }}</span>
                    <span class="stat-label">等待(分钟)</span>
                  </div>
                  <div class="quick-stat">
                    <span class="stat-icon">🌡️</span>
                    <span class="stat-value">{{ currentScenic.weather.temperature }}°</span>
                    <span class="stat-label">{{ currentScenic.weather.condition }}</span>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 智能建议 -->
            <div v-if="currentScenic.suggestion" class="suggestion-card"
              :class="{
                'suggestion-good': currentScenic.recommended,
                'suggestion-warning': !currentScenic.recommended && currentScenic.status !== 'limited',
                'suggestion-danger': currentScenic.status === 'limited'
              }"
            >
              <div class="suggestion-icon">
                <el-icon v-if="currentScenic.recommended"><CircleCheckFilled /></el-icon>
                <el-icon v-else-if="currentScenic.status !== 'limited'"><Warning /></el-icon>
                <el-icon v-else><WarningFilled /></el-icon>
              </div>
              <div class="suggestion-content">
                <div class="suggestion-header">
                  <span class="suggestion-title">
                    {{ currentScenic.recommended ? '✨ 适合游览' : '⚠️ 温馨提示' }}
                  </span>
                  <span 
                    v-if="currentScenic.trend"
                    class="trend-badge"
                    :class="{
                      'trend-up': currentScenic.trend === 'rising',
                      'trend-down': currentScenic.trend === 'falling',
                      'trend-stable': currentScenic.trend === 'stable'
                    }"
                  >
                    <span class="trend-icon">{{ currentScenic.trend === 'rising' ? '📈' : currentScenic.trend === 'falling' ? '📉' : '➡️' }}</span>
                    {{ currentScenic.trend === 'rising' ? '客流上升' : currentScenic.trend === 'falling' ? '客流下降' : '客流稳定' }}
                  </span>
                </div>
                <p class="suggestion-text">{{ currentScenic.suggestion }}</p>
                <div v-if="currentScenic.predictedFlow" class="prediction-info">
                  <el-icon><TrendCharts /></el-icon>
                  预测未来1小时客流：<strong>{{ currentScenic.predictedFlow }}人</strong>
                  （当前：{{ currentScenic.visitorCount }}人）
                </div>
              </div>
            </div>
            
            <el-divider />
            
            <!-- 数据统计卡片 -->
            <div class="stats-grid">
              <div class="stat-card-enhanced visitor-stat">
                <div class="stat-icon-wrapper">
                  <el-icon><User /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">当前客流量</div>
                  <div class="stat-value-row">
                    <span class="stat-number">{{ currentScenic.visitorCount }}</span>
                    <span class="stat-unit">人</span>
                  </div>
                  <div class="stat-progress">
                    <div class="progress-bar">
                      <div 
                        class="progress-fill"
                        :style="{ width: getPercentage(currentScenic.visitorCount, currentScenic.capacity) + '%' }"
                        :class="getProgressClass(getPercentage(currentScenic.visitorCount, currentScenic.capacity))"
                      ></div>
                    </div>
                    <span class="progress-text">{{ getPercentage(currentScenic.visitorCount, currentScenic.capacity) }}%</span>
                  </div>
                </div>
              </div>
              
              <div class="stat-card-enhanced wait-stat">
                <div class="stat-icon-wrapper">
                  <el-icon><Clock /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">等待时间</div>
                  <div class="stat-value-row">
                    <span class="stat-number">{{ currentScenic.waitTime }}</span>
                    <span class="stat-unit">分钟</span>
                  </div>
                  <div class="wait-indicator">
                    <span class="wait-dot" :class="currentScenic.waitTime < 10 ? 'fast' : currentScenic.waitTime < 30 ? 'normal' : 'slow'"></span>
                    <span class="wait-text">{{ currentScenic.waitTime < 10 ? '无需等待' : currentScenic.waitTime < 30 ? '稍需等待' : '较长等待' }}</span>
                  </div>
                </div>
              </div>
              
              <div class="stat-card-enhanced weather-stat">
                <div class="stat-icon-wrapper weather-icon">
                  <span class="weather-emoji">{{ getWeatherEmoji(currentScenic.weather.condition) }}</span>
                </div>
                <div class="stat-info">
                  <div class="stat-label">当前天气</div>
                  <div class="stat-value-row">
                    <span class="stat-number">{{ currentScenic.weather.temperature }}</span>
                    <span class="stat-unit">°C</span>
                  </div>
                  <div class="weather-condition">{{ currentScenic.weather.condition }}</div>
                </div>
              </div>
            </div>
            
            <el-divider />
            
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <h3 class="text-gray-700 font-medium mb-4 flex items-center gap-2">
                  <span class="inline-block w-1 h-5 rounded bg-gradient-to-b from-teal-500 to-blue-500"></span>
                  实时客流趋势
                </h3>
                <div class="visitor-chart-container">
                  <div style="height: 400px;" ref="visitorChartRef"></div>
                </div>
              </div>
              
              <div>
                <h3 class="text-gray-700 font-medium mb-4">区域人流热力图（3D）</h3>
                <div style="height: 400px; width: 100%; overflow: hidden;">
                  <Heatmap3D :scenic-id="scenicNameToId[selectedScenic]" :key="selectedScenic" style="width: 100%; height: 100%;" />
                </div>
              </div>
            </div>
            
            <el-divider />
            
            <div class="flex justify-between items-center mb-3">
              <h3 class="text-gray-700 font-medium">正在进行的活动</h3>
              <el-tag type="success" effect="dark" v-if="activities.length > 0">{{ activities.length }}项</el-tag>
            </div>
            
            <div v-if="activities.length > 0" class="space-y-3">
              <div 
                v-for="activity in activities" 
                :key="activity.id" 
                class="activity-item p-3 rounded-lg border border-gray-200 hover:border-[#2A9D8F] transition-all cursor-pointer"
              >
                <div class="flex items-start">
                  <!-- 活动封面图 -->
                  <div class="flex-shrink-0 mr-3">
                    <img 
                      v-if="activity.coverImage" 
                      :src="getImageUrl(activity.coverImage)" 
                      :alt="activity.name"
                      class="w-20 h-20 rounded object-cover"
                      @error="(e: any) => e.target.style.display = 'none'"
                    />
                    <div v-else class="w-20 h-20 rounded bg-white flex items-center justify-center">
                      <el-icon class="text-[#2A9D8F] text-2xl">
                        <Calendar />
                      </el-icon>
                    </div>
                  </div>
                  <div class="flex-1">
                    <div class="flex items-center justify-between mb-1">
                      <span class="text-gray-700 font-medium">{{ activity.name }}</span>
                      <el-tag 
                        size="small" 
                        :type="activity.status === 'active' ? 'success' : 'warning'"
                        effect="dark"
                      >
                        {{ activity.status === 'active' ? '进行中' : '即将开始' }}
                      </el-tag>
                    </div>
                    <p class="text-gray-500 text-sm mb-2">{{ activity.description || '暂无描述' }}</p>
                    <div class="flex flex-wrap gap-2 text-xs text-gray-500">
                      <span v-if="activity.startTime && activity.endTime">
                        <el-icon class="mr-1"><Clock /></el-icon>
                        {{ activity.startTime }} ~ {{ activity.endTime }}
                      </span>
                      <span v-if="activity.location">
                        <el-icon class="mr-1"><Location /></el-icon>{{ activity.location }}
                      </span>
                      <span v-if="activity.currentParticipants !== undefined">
                        <el-icon class="mr-1"><User /></el-icon>
                        {{ activity.currentParticipants }}人参与
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
            <div v-else class="text-center py-8">
              <el-icon class="text-4xl text-gray-500 mb-2"><Calendar /></el-icon>
              <p class="text-gray-500 text-sm">暂无进行中的活动</p>
            </div>
            
            <el-divider />
            
            <!-- 景区设施 -->
            <div>
              <div class="flex justify-between items-center mb-3">
                <h3 class="text-gray-700 font-medium">景区设施</h3>
                <el-select 
                  v-model="facilityCategory" 
                  placeholder="全部类型" 
                  size="small" 
                  clearable
                  @change="loadFacilities"
                  class="w-32"
                >
                  <el-option label="全部" value="" />
                  <el-option label="停车场" value="停车场" />
                  <el-option label="餐厅" value="餐厅" />
                  <el-option label="卫生间" value="卫生间" />
                  <el-option label="服务中心" value="服务中心" />
                  <el-option label="医疗点" value="医疗点" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </div>
              
              <div v-if="facilities.length > 0" class="space-y-3">
                <div 
                  v-for="facility in facilities" 
                  :key="facility.id" 
                  class="facility-item p-3 rounded-lg border border-gray-200 hover:border-[#2A9D8F] transition-all cursor-pointer"
                  @click="showFacilityDetail(facility)"
                >
                  <div class="flex items-start">
                    <!-- 设施缩略图 -->
                    <div class="flex-shrink-0 mr-3">
                      <img 
                        v-if="facility.thumbnail && !facility.thumbnail.includes('default')" 
                        :src="getImageUrl(facility.thumbnail)" 
                        :alt="facility.name"
                        class="w-16 h-16 rounded object-cover"
                        @error="(e: any) => { if (e.target) e.target.style.display = 'none' }"
                      />
                      <div v-else class="w-16 h-16 rounded bg-white flex items-center justify-center">
                        <el-icon class="text-[#2A9D8F] text-2xl">
                          <component :is="getFacilityIcon(facility.category)" />
                        </el-icon>
                      </div>
                    </div>
                    <div class="flex-1">
                      <div class="flex items-center justify-between mb-1">
                        <span class="text-gray-700 font-medium">{{ facility.name }}</span>
                        <el-tag 
                          size="small" 
                          :type="facility.status === 'normal' || facility.status === 'active' ? 'success' : (facility.status === 'maintenance' ? 'warning' : 'danger')"
                          effect="dark"
                        >
                          {{ (facility.status === 'normal' || facility.status === 'active') ? '正常' : (facility.status === 'maintenance' ? '维护中' : '关闭') }}
                        </el-tag>
                      </div>
                      <p class="text-gray-500 text-sm mb-2">{{ facility.description || '暂无描述' }}</p>
                      <div class="flex flex-wrap gap-2 text-xs text-gray-500">
                        <span v-if="facility.location">
                          <el-icon class="mr-1"><Location /></el-icon>{{ facility.location }}
                        </span>
                        <span v-if="facility.openingHours">
                          <el-icon class="mr-1"><Clock /></el-icon>{{ facility.openingHours }}
                        </span>
                        <span v-if="facility.isFree !== null">
                          <el-icon class="mr-1"><Coin /></el-icon>{{ facility.isFree ? '免费' : facility.priceInfo || '收费' }}
                        </span>
                        <span v-if="facility.capacity">
                          <el-icon class="mr-1"><User /></el-icon>容量: {{ facility.capacity }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
              <div v-else class="text-center py-8">
                <el-icon class="text-4xl text-gray-500 mb-2"><OfficeBuilding /></el-icon>
                <p class="text-gray-500 text-sm">暂无设施信息</p>
              </div>
              
              <!-- 加载更多 -->
              <div v-if="facilities.length > 0 && hasMoreFacilities" class="text-center mt-4">
                <el-button type="primary" text @click="loadMoreFacilities">加载更多</el-button>
              </div>
            </div>
          </div>
        </scenic-card>
          
        <scenic-card v-else class="h-96 flex items-center justify-center">
            <div class="text-center">
              <el-icon class="text-6xl text-gray-500 mb-4"><Loading /></el-icon>
              <p class="text-gray-500">请选择景区查看实时信息</p>
            </div>
          </scenic-card>
      </div>
      
      <!-- 服务面板 -->
      <div class="w-80 flex-shrink-0">
        <scenic-card class="p-3">
          <template #header>
            <div class="flex justify-between items-center">
              <h2 class="text-base font-bold text-gray-800">智能服务</h2>
              <el-button v-if="digitalHumanStore.hasMessages" size="small" type="danger" plain @click="clearDigitalHumanChat">清空</el-button>
            </div>
          </template>
          
          <!-- 嵌入式数字人AI助手 -->
          <div class="mb-3">
            <EmbeddedDigitalHuman />
          </div>
          
          <!-- 快捷服务 -->
          <div class="mt-3 quick-services-section">
            <h3 class="text-gray-500 font-medium text-sm mb-3">快捷服务</h3>
            <div class="grid grid-cols-2 gap-3">
              <!-- 在线购票 -->
              <div class="quick-service-card" @click="ticketDialogVisible = true">
                <div class="service-badge hot">热门</div>
                <div class="service-card-content">
                  <div class="service-card-title">在线购票</div>
                  <div class="service-card-desc">景区门票预订</div>
                </div>
              </div>
              <!-- 视频讲解 -->
              <div class="quick-service-card" @click="openVideoGuide">
                <div class="service-card-content">
                  <div class="service-card-title">视频讲解</div>
                  <div class="service-card-desc">景区视频介绍</div>
                </div>
              </div>
              <!-- 紧急救援 -->
              <div class="quick-service-card" @click="emergencyDialogVisible = true">
                <div class="service-card-content">
                  <div class="service-card-title">紧急救援</div>
                  <div class="service-card-desc">紧急情况求助</div>
                </div>
              </div>
              <!-- 游客评价 -->
              <div class="quick-service-card" @click="reviewDialogVisible = true">
                <div class="service-card-content">
                  <div class="service-card-title">游客评价</div>
                  <div class="service-card-desc">分享您的体验</div>
                </div>
              </div>
            </div>
          </div>
        </scenic-card>
        
        <!-- 系统公告 -->
        <scenic-card class="p-3 mt-4">
          <template #header>
            <h2 class="text-base font-bold text-gray-800">系统公告</h2>
          </template>
          
          <div v-if="systemNotifications.length > 0" class="space-y-2">
            <div 
              v-for="notification in systemNotifications" 
              :key="notification.id" 
              class="p-2 rounded border transition-all hover:border-[#2A9D8F] cursor-pointer"
              :class="{
                'border-red-500 bg-red-900 bg-opacity-20': notification.type === 'error',
                'border-yellow-500 bg-yellow-900 bg-opacity-20': notification.type === 'warning',
                'border-green-500 bg-green-900 bg-opacity-20': notification.type === 'success',
                'border-blue-500 bg-blue-900 bg-opacity-20': notification.type === 'info'
              }"
              @click="handleSystemNotificationClick(notification)"
            >
              <div class="flex items-start gap-2">
                <el-icon 
                  class="text-base flex-shrink-0 mt-0.5" 
                  :class="{
                    'text-red-500': notification.type === 'error',
                    'text-yellow-500': notification.type === 'warning',
                    'text-green-500': notification.type === 'success',
                    'text-blue-500': notification.type === 'info'
                  }"
                >
                  <WarningFilled v-if="notification.type === 'error'" />
                  <Warning v-else-if="notification.type === 'warning'" />
                  <SuccessFilled v-else-if="notification.type === 'success'" />
                  <InfoFilled v-else />
                </el-icon>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center justify-between gap-1">
                    <span class="text-gray-800 font-medium text-sm truncate">{{ notification.title }}</span>
                  </div>
                  <p class="text-gray-500 text-xs mt-0.5 line-clamp-2">{{ notification.content }}</p>
                  <div class="text-xs text-gray-500 mt-1">
                    {{ notification.timeAgo || notification.time }}
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div v-else class="text-center py-4">
            <el-icon class="text-2xl text-gray-500 mb-1"><Bell /></el-icon>
            <p class="text-gray-500 text-xs">暂无系统公告</p>
          </div>
        </scenic-card>
        
        <!-- 景区实时公告 -->
        <scenic-card class="p-3 mt-3">
          <template #header>
            <h2 class="text-base font-bold text-gray-800">景区公告</h2>
          </template>
          
          <div v-if="announcements.length > 0" class="space-y-2">
            <div 
              v-for="announcement in announcements" 
              :key="announcement.id" 
              class="p-2 rounded border"
              :class="{
                'border-red-500 bg-red-900 bg-opacity-20': announcement.type === 'urgent',
                'border-yellow-500 bg-yellow-900 bg-opacity-20': announcement.type === 'important',
                'border-gray-600': announcement.type === 'normal'
              }"
            >
              <div class="flex items-start gap-2">
                <el-icon 
                  class="text-sm flex-shrink-0 mt-0.5" 
                  :class="{
                    'text-red-500': announcement.type === 'urgent',
                    'text-yellow-500': announcement.type === 'important',
                    'text-gray-500': announcement.type === 'normal'
                  }"
                >
                  <Warning v-if="announcement.type === 'urgent'" />
                  <InfoFilled v-else-if="announcement.type === 'important'" />
                  <Notification v-else />
                </el-icon>
                <div class="flex-1 min-w-0">
                  <span class="text-gray-800 font-medium text-sm block truncate">{{ announcement.title }}</span>
                  <p class="text-gray-500 text-xs mt-0.5 line-clamp-2">{{ announcement.content }}</p>
                  <div class="text-xs text-gray-500 mt-1">
                    {{ formatDate(announcement.createdAt) }}
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div v-else class="text-center py-4">
            <el-icon class="text-2xl text-gray-500 mb-1"><Notification /></el-icon>
            <p class="text-gray-500 text-xs">暂无实时公告</p>
          </div>
        </scenic-card>
      </div>
    </div>
    
    <!-- 紧急救援对话框 -->
    <el-dialog v-model="emergencyDialogVisible" title="" width="450px" class="emergency-dialog" :show-close="false">
      <!-- 自定义头部 -->
      <template #header>
        <div class="emergency-dialog-header">
          <div class="emergency-icon-wrapper">
            <el-icon class="emergency-icon"><Warning /></el-icon>
          </div>
          <div class="header-text">
            <h3 class="emergency-title">🆘 紧急救援</h3>
            <p class="emergency-subtitle">工作人员将尽快与您联系</p>
          </div>
          <button class="close-btn" @click="emergencyDialogVisible = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>
      </template>
      
      <div class="emergency-dialog-content">
        <!-- 救援类型选择 -->
        <div class="rescue-type-section">
          <div class="section-label">救援类型</div>
          <div class="rescue-type-grid">
            <div 
              class="rescue-type-card"
              :class="{ active: emergencyType === 'medical' }"
              @click="emergencyType = 'medical'"
            >
              <span class="rescue-icon">🏥</span>
              <span class="rescue-name">医疗救援</span>
            </div>
            <div 
              class="rescue-type-card"
              :class="{ active: emergencyType === 'accident' }"
              @click="emergencyType = 'accident'"
            >
              <span class="rescue-icon">🛡️</span>
              <span class="rescue-name">安全救援</span>
            </div>
            <div 
              class="rescue-type-card"
              :class="{ active: emergencyType === 'lost' }"
              @click="emergencyType = 'lost'"
            >
              <span class="rescue-icon">🧭</span>
              <span class="rescue-name">迷路求助</span>
            </div>
            <div 
              class="rescue-type-card"
              :class="{ active: emergencyType === 'other' }"
              @click="emergencyType = 'other'"
            >
              <span class="rescue-icon">📞</span>
              <span class="rescue-name">其他情况</span>
            </div>
          </div>
        </div>
        
        <!-- 联系信息 -->
        <div class="emergency-form">
          <div class="form-item">
            <label class="form-label">📱 联系电话</label>
            <el-input v-model="emergencyContact" placeholder="请输入您的联系电话" maxlength="11" class="custom-input" />
          </div>
          
          <div class="form-item">
            <label class="form-label">📍 位置说明</label>
            <el-input v-model="emergencyLocation" placeholder="请尽可能详细描述您的位置" class="custom-input" />
          </div>
          
          <div class="form-item">
            <label class="form-label">📝 情况描述</label>
            <el-input 
              type="textarea" 
              v-model="emergencyDescription" 
              placeholder="请简要描述您的情况" 
              :rows="3" 
              class="custom-textarea"
            />
          </div>
        </div>
        
        <!-- 紧急电话提示 -->
        <div class="emergency-tip">
          <el-icon><Phone /></el-icon>
          <span>如遇紧急情况，请直接拨打 <strong>120</strong> 或 <strong>110</strong></span>
        </div>
      </div>
      
      <template #footer>
        <div class="emergency-dialog-footer">
          <button class="emergency-call-btn" @click="callEmergency">
            <el-icon><Phone /></el-icon>
            <span>紧急电话: 120</span>
          </button>
          <div class="action-btns">
            <button class="cancel-btn" @click="emergencyDialogVisible = false">取消</button>
            <button class="submit-btn" @click="submitEmergency">
              <el-icon><Position /></el-icon>
              <span>提交求助</span>
            </button>
          </div>
        </div>
      </template>
    </el-dialog>
    
    <!-- 购票对话框 -->
    <el-dialog v-model="ticketDialogVisible" title="" width="520px" class="ticket-dialog" :show-close="false">
      <!-- 自定义头部 -->
      <template #header>
        <div class="ticket-dialog-header">
          <div class="header-icon-wrapper">
            <el-icon class="header-icon"><Ticket /></el-icon>
          </div>
          <div class="header-text">
            <h3 class="header-title">🎫 在线购票</h3>
            <p class="header-subtitle">快速预订，便捷入园</p>
          </div>
          <button class="close-btn" @click="ticketDialogVisible = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>
      </template>
      
      <div class="ticket-dialog-content">
        <!-- 景区信息卡片 -->
        <div v-if="currentScenic" class="scenic-info-card">
          <div class="scenic-image-wrapper">
            <el-image :src="currentScenic.image" fit="cover" class="scenic-image" />
            <div class="scenic-image-overlay"></div>
          </div>
          <div class="scenic-info">
            <div class="scenic-name">{{ currentScenic.name }}</div>
            <div class="scenic-desc">{{ currentScenic.description }}</div>
            <div class="scenic-tags">
              <span class="tag">🏔️ 4A景区</span>
              <span class="tag">⭐ 热门推荐</span>
            </div>
          </div>
        </div>
        
        <!-- 票种选择 -->
        <div class="form-section">
          <div class="section-title">
            <el-icon><Tickets /></el-icon>
            <span>票种选择</span>
          </div>
          <div class="ticket-type-grid">
            <div 
              class="ticket-type-card"
              :class="{ active: selectedTicketType === 'adult' }"
              @click="selectedTicketType = 'adult'"
            >
              <div class="ticket-type-icon">👤</div>
              <div class="ticket-type-info">
                <div class="ticket-type-name">成人票</div>
                <div class="ticket-type-price">¥60</div>
              </div>
              <div class="ticket-type-check" v-if="selectedTicketType === 'adult'">
                <el-icon><Check /></el-icon>
              </div>
            </div>
            <div 
              class="ticket-type-card"
              :class="{ active: selectedTicketType === 'child' }"
              @click="selectedTicketType = 'child'"
            >
              <div class="ticket-type-icon">👶</div>
              <div class="ticket-type-info">
                <div class="ticket-type-name">儿童票</div>
                <div class="ticket-type-price">¥30 <span class="discount">半价</span></div>
              </div>
              <div class="ticket-type-check" v-if="selectedTicketType === 'child'">
                <el-icon><Check /></el-icon>
              </div>
            </div>
            
            <div class="ticket-qty-control">
              <span class="qty-label">购买数量</span>
              <div class="qty-selector">
                <button class="qty-btn" @click="ticketCount = Math.max(1, ticketCount - 1)">
                  <el-icon><Minus /></el-icon>
                </button>
                <span class="qty-value">{{ ticketCount }}</span>
                <button class="qty-btn" @click="ticketCount = Math.min(10, ticketCount + 1)">
                  <el-icon><Plus /></el-icon>
                </button>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 购票信息 -->
        <div class="form-section">
          <div class="section-title">
            <el-icon><Calendar /></el-icon>
            <span>购票信息</span>
          </div>
          <div class="form-grid">
            <div class="form-item">
              <label class="form-label">购票数量</label>
              <div class="quantity-selector">
                <button class="qty-btn" @click="ticketCount = Math.max(1, ticketCount - 1)">
                  <el-icon><Minus /></el-icon>
                </button>
                <span class="qty-value">{{ ticketCount }}</span>
                <button class="qty-btn" @click="ticketCount = Math.min(10, ticketCount + 1)">
                  <el-icon><Plus /></el-icon>
                </button>
              </div>
            </div>
            <div class="form-item">
              <label class="form-label">参观日期</label>
              <el-date-picker 
                v-model="ticketDate" 
                type="date" 
                placeholder="选择日期" 
                class="custom-date-picker"
                :disabled-date="(date: Date) => date < new Date()" 
              />
            </div>
          </div>
        </div>
        
        <!-- 游客信息 -->
        <div class="form-section">
          <div class="section-title">
            <el-icon><User /></el-icon>
            <span>游客信息</span>
          </div>
          <div class="form-grid">
            <div class="form-item">
              <label class="form-label">游客姓名</label>
              <el-input v-model="visitorName" placeholder="请输入游客姓名" class="custom-input" />
            </div>
            <div class="form-item">
              <label class="form-label">联系电话</label>
              <el-input v-model="visitorPhone" placeholder="请输入联系电话" class="custom-input" />
            </div>
          </div>
        </div>
        
        <!-- 价格汇总 -->
        <div class="price-summary">
          <div class="price-row">
            <span class="price-label">票价</span>
            <span class="price-value">¥{{ ticketPrice }} × {{ ticketCount }}</span>
          </div>
          <div class="price-row total">
            <span class="price-label">应付金额</span>
            <span class="price-total">¥{{ ticketPrice * ticketCount }}</span>
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="ticket-dialog-footer">
          <button class="cancel-btn" @click="ticketDialogVisible = false">取消</button>
          <button class="submit-btn" @click="submitTicket">
            <el-icon><ShoppingCart /></el-icon>
            <span>确认购票</span>
          </button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 购票成功对话框（显示条形码） -->
    <el-dialog v-model="ticketSuccessDialogVisible" title="🎉 购票成功" width="400px" :close-on-click-modal="false">
      <div class="ticket-success-content">
        <div class="success-icon">
          <el-icon class="text-5xl text-green-500"><SuccessFilled /></el-icon>
        </div>
        
        <div class="order-info">
          <div class="info-row">
            <span class="label">订单号：</span>
            <span class="value">{{ ticketOrderInfo?.orderNo }}</span>
          </div>
          <div class="info-row">
            <span class="label">景区：</span>
            <span class="value">{{ ticketOrderInfo?.scenicName }}</span>
          </div>
          <div class="info-row">
            <span class="label">参观日期：</span>
            <span class="value">{{ ticketOrderInfo?.visitDate }}</span>
          </div>
          <div class="info-row">
            <span class="label">票数：</span>
            <span class="value">{{ ticketOrderInfo?.ticketCount }} 张</span>
          </div>
          <div class="info-row">
            <span class="label">总金额：</span>
            <span class="value price">¥{{ ticketOrderInfo?.totalAmount }}</span>
          </div>
        </div>
        
        <div class="barcode-section">
          <div class="barcode-title">入园凭证（条形码）</div>
          <div class="barcode-container">
            <canvas ref="barcodeCanvas" class="barcode-canvas"></canvas>
          </div>
          <div class="barcode-tip">请在入园时出示此条形码扫码检票</div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-center gap-3">
          <el-button @click="ticketSuccessDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="saveTicketImage">保存票据</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 我的订单对话框 -->
    <el-dialog v-model="ordersDialogVisible" title="我的订单" width="600px">
      <div v-loading="ordersLoading" class="orders-container">
        <div v-if="myOrders.length === 0 && !ordersLoading" class="empty-orders">
          <el-empty description="暂无订单记录" />
        </div>
        <div v-else class="orders-list">
          <div 
            v-for="order in myOrders" 
            :key="order.id" 
            class="order-item"
            @click="viewOrderDetail(order)"
          >
            <div class="order-header">
              <span class="order-no">订单号：{{ order.orderNo }}</span>
              <el-tag :type="getOrderStatusType(order.status)" size="small">
                {{ getOrderStatusText(order.status) }}
              </el-tag>
            </div>
            <div class="order-body">
              <div class="order-info-row">
                <span class="label">景区：</span>
                <span class="value">{{ order.scenicName }}</span>
              </div>
              <div class="order-info-row">
                <span class="label">参观日期：</span>
                <span class="value">{{ order.visitDate }}</span>
              </div>
              <div class="order-info-row">
                <span class="label">票数：</span>
                <span class="value">{{ order.ticketCount }} 张</span>
              </div>
              <div class="order-info-row">
                <span class="label">金额：</span>
                <span class="value price">¥{{ order.totalAmount }}</span>
              </div>
            </div>
            <div class="order-footer">
              <span class="order-time">{{ order.createdAt }}</span>
              <el-button type="primary" size="small" link @click.stop="viewOrderDetail(order)">
                查看票据
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
    
    <!-- 订单详情对话框（显示条形码） -->
    <el-dialog v-model="orderDetailDialogVisible" title="票据详情" width="400px">
      <div v-if="selectedOrder" class="ticket-detail-content">
        <div class="order-info">
          <div class="info-row">
            <span class="label">订单号：</span>
            <span class="value">{{ selectedOrder.orderNo }}</span>
          </div>
          <div class="info-row">
            <span class="label">景区：</span>
            <span class="value">{{ selectedOrder.scenicName }}</span>
          </div>
          <div class="info-row">
            <span class="label">参观日期：</span>
            <span class="value">{{ selectedOrder.visitDate }}</span>
          </div>
          <div class="info-row">
            <span class="label">游客姓名：</span>
            <span class="value">{{ selectedOrder.visitorName }}</span>
          </div>
          <div class="info-row">
            <span class="label">票数：</span>
            <span class="value">{{ selectedOrder.ticketCount }} 张</span>
          </div>
          <div class="info-row">
            <span class="label">总金额：</span>
            <span class="value price">¥{{ selectedOrder.totalAmount }}</span>
          </div>
          <div class="info-row">
            <span class="label">状态：</span>
            <el-tag :type="getOrderStatusType(selectedOrder.status)" size="small">
              {{ getOrderStatusText(selectedOrder.status) }}
            </el-tag>
          </div>
        </div>
        
        <div class="barcode-section">
          <div class="barcode-title">入园凭证（条形码）</div>
          <div class="barcode-container">
            <canvas ref="orderBarcodeCanvas" class="barcode-canvas"></canvas>
          </div>
          <div class="barcode-tip">请在入园时出示此条形码扫码检票</div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-center">
          <el-button @click="orderDetailDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 视频讲解对话框 -->
    <el-dialog v-model="guideDialogVisible" title="视频讲解" width="800px" @close="closeGuideDialog">
      <div class="space-y-4">
        <div v-if="currentScenic" class="flex items-center mb-4 p-3 bg-white bg-opacity-50 rounded-lg">
          <el-image :src="currentScenic.image" fit="cover" class="w-16 h-16 rounded mr-3" />
          <div>
            <div class="text-gray-700 font-medium">{{ currentScenic.name }} - 景区视频讲解</div>
            <div class="text-gray-500 text-sm">观看视频了解景区亮点和特色</div>
          </div>
        </div>
        
        <div class="bg-white bg-opacity-50 p-4 rounded-lg">
          <h3 class="text-gray-700 font-medium mb-3">景区讲解视频</h3>
          <div class="w-full aspect-video bg-black rounded-lg overflow-hidden">
            <video 
              v-if="currentVideo" 
              class="w-full h-full" 
              controls 
              :src="currentVideo"
            >
              您的浏览器不支持视频播放，请升级或更换浏览器。
            </video>
            <div v-else class="w-full h-full flex items-center justify-center">
              <div class="text-center">
                <el-icon class="text-6xl text-gray-500 mb-4"><VideoPlay /></el-icon>
                <p class="text-gray-500">暂无视频资源</p>
              </div>
            </div>
          </div>
          
          <div class="mt-4 text-gray-500 text-sm">
            <div class="flex justify-between items-center">
              <p>当前景区: {{ currentScenic?.name || '未选择' }}</p>
              <el-button size="small" type="primary" @click="() => refreshVideo(selectedScenic)">刷新视频</el-button>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end">
          <el-button @click="closeGuideDialog">关闭</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 评价对话框 -->
    <el-dialog v-model="reviewDialogVisible" title="游客评价" width="500px">
      <div class="space-y-4">
        <div v-if="currentScenic" class="flex items-center mb-4 p-3 bg-white bg-opacity-50 rounded-lg">
          <el-image :src="currentScenic.image" fit="cover" class="w-16 h-16 rounded mr-3" />
          <div>
            <div class="text-gray-700 font-medium">{{ currentScenic.name }}</div>
            <div class="text-gray-500 text-sm">分享您的游览体验</div>
          </div>
        </div>
        
        <el-form>
          <el-form-item label="评分">
            <el-rate v-model="reviewScore" :colors="['#F56C6C', '#E6A23C', '#67C23A']" />
          </el-form-item>
          
          <el-form-item label="评价内容">
            <el-input type="textarea" v-model="reviewContent" placeholder="请分享您的游览体验和建议..." rows="4" />
          </el-form-item>
          
          <el-form-item label="选择标签（可选）">
            <el-checkbox-group v-model="reviewTags">
              <el-checkbox label="风景优美">风景优美</el-checkbox>
              <el-checkbox label="空气清新">空气清新</el-checkbox>
              <el-checkbox label="服务周到">服务周到</el-checkbox>
              <el-checkbox label="交通便利">交通便利</el-checkbox>
              <el-checkbox label="设施完善">设施完善</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          
          <el-form-item label="游览日期">
            <el-date-picker 
              v-model="reviewVisitDate" 
              type="date" 
              placeholder="选择游览日期" 
              class="w-full"
              :disabled-date="(date: Date) => date > new Date()"
            />
          </el-form-item>
          
          <el-form-item label="上传图片（可选）">
            <el-upload
              action="#"
              list-type="picture-card"
              :auto-upload="false"
              :limit="3"
              :file-list="reviewImageFiles"
              :on-change="handleReviewImageChange"
              :on-remove="handleReviewImageRemove"
              accept="image/*"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="text-gray-500 text-xs mt-1">最多上传3张图片，每张不超过2MB</div>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="flex justify-end">
          <el-button @click="reviewDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitReview">提交评价</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 设施详情弹窗 -->
    <el-dialog
      v-model="facilityDetailVisible"
      :title="''"
      width="600px"
      class="facility-detail-dialog"
      :show-close="false"
    >
      <template #header>
        <div class="facility-dialog-header">
          <div class="header-icon">
            <el-icon :size="24"><OfficeBuilding /></el-icon>
          </div>
          <div class="header-content">
            <h3 class="header-title">{{ selectedFacility?.name || '设施详情' }}</h3>
            <p class="header-subtitle">设施信息</p>
          </div>
          <button class="close-btn" @click="facilityDetailVisible = false">
            <el-icon :size="20"><Close /></el-icon>
          </button>
        </div>
      </template>
      
      <div v-if="selectedFacility" class="facility-detail-content">
        <!-- 设施图片 -->
        <div v-if="selectedFacility.thumbnail && !selectedFacility.thumbnail.includes('default')" class="facility-image-wrapper">
          <img 
            :src="getImageUrl(selectedFacility.thumbnail)" 
            :alt="selectedFacility.name"
            class="facility-image"
            @error="(e: any) => { if (e.target) e.target.style.display = 'none' }"
          />
          <div class="image-overlay"></div>
          <div class="image-badge" v-if="selectedFacility.category">
            <el-icon :size="14"><PriceTag /></el-icon>
            {{ selectedFacility.category }}
          </div>
        </div>
        
        <!-- 设施信息卡片 -->
        <div class="facility-info-grid">
          <!-- 设施名称 -->
          <div class="info-card full-width">
            <div class="info-icon name-icon">
              <el-icon :size="18"><OfficeBuilding /></el-icon>
            </div>
            <div class="info-content">
              <span class="info-label">设施名称</span>
              <span class="info-value highlight">{{ selectedFacility.name }}</span>
            </div>
          </div>
          
          <!-- 设施描述 -->
          <div v-if="selectedFacility.description" class="info-card full-width description-card">
            <div class="info-icon desc-icon">
              <el-icon :size="18"><Document /></el-icon>
            </div>
            <div class="info-content">
              <span class="info-label">设施描述</span>
              <p class="info-description">{{ selectedFacility.description }}</p>
            </div>
          </div>
          
          <!-- 开放时间 -->
          <div v-if="selectedFacility.openingHours" class="info-card">
            <div class="info-icon time-icon">
              <el-icon :size="18"><Clock /></el-icon>
            </div>
            <div class="info-content">
              <span class="info-label">开放时间</span>
              <span class="info-value">{{ selectedFacility.openingHours }}</span>
            </div>
          </div>
          
          <!-- 位置信息 -->
          <div v-if="selectedFacility.location" class="info-card">
            <div class="info-icon location-icon">
              <el-icon :size="18"><Location /></el-icon>
            </div>
            <div class="info-content">
              <span class="info-label">位置信息</span>
              <span class="info-value">{{ selectedFacility.location }}</span>
            </div>
          </div>
          
          <!-- 容纳量 -->
          <div v-if="selectedFacility.capacity" class="info-card">
            <div class="info-icon capacity-icon">
              <el-icon :size="18"><User /></el-icon>
            </div>
            <div class="info-content">
              <span class="info-label">容纳量</span>
              <span class="info-value">{{ selectedFacility.capacity }} 人</span>
            </div>
          </div>
          
          <!-- 收费信息 -->
          <div v-if="selectedFacility.isFree !== null && selectedFacility.isFree !== undefined" class="info-card">
            <div class="info-icon price-icon">
              <el-icon :size="18"><Coin /></el-icon>
            </div>
            <div class="info-content">
              <span class="info-label">收费信息</span>
              <span class="info-value" :class="{ 'free-tag': selectedFacility.isFree }">
                {{ selectedFacility.isFree ? '🆓 免费' : (selectedFacility.priceInfo || '💰 收费') }}
              </span>
            </div>
          </div>
          
          <!-- 当前状态 -->
          <div class="info-card status-card">
            <div class="info-icon status-icon" :class="getStatusClass(selectedFacility.status)">
              <el-icon :size="18"><CircleCheck /></el-icon>
            </div>
            <div class="info-content">
              <span class="info-label">当前状态</span>
              <span class="status-badge" :class="getStatusClass(selectedFacility.status)">
                {{ getFacilityStatusText(selectedFacility.status) }}
              </span>
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="facility-dialog-footer">
          <button class="close-button" @click="facilityDetailVisible = false">
            <el-icon :size="16"><Close /></el-icon>
            <span>关闭</span>
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed, onBeforeUnmount, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { User, Clock, Sunny, Warning, Notification, Location, Loading, OfficeBuilding, Coin, Calendar, InfoFilled, WarningFilled, SuccessFilled, Bell, Close, CircleCheck, Document, PriceTag, Ticket, Tickets, Check, Minus, Phone, Position, Refresh, DataAnalysis, CircleCheckFilled, TrendCharts } from '@element-plus/icons-vue'
import { Van, Bicycle, Ship, Guide, VideoPlay, Headset, Plus, HomeFilled, ShoppingCart, HelpFilled, FirstAidKit, List } from '@element-plus/icons-vue'
import ScenicCard from '@/components/ScenicCard.vue'

import CombinedScenicCard from '@/components/CombinedScenicCard.vue'
import EmbeddedDigitalHuman from '@/components/EmbeddedDigitalHuman.vue'
import Heatmap3D from '@/components/Heatmap3D.vue'
import * as echarts from 'echarts'
import { getScenicDetail, getScenicFlow, getScenicHeatmap, getScenicServices, getScenicVideos } from '@/api/scenic'
import { sendChatMessage, type ChatRequest } from '@/api/chat'
import { getWeatherInfo } from '@/api/dashboard'
import { submitReview as submitReviewAPI } from '@/api/review'
import { getScenicFacilities, getOngoingActivities } from '@/api/resource'
import { getScenicAnnouncements } from '@/api/announcement'
import { getUserNotifications, markUserNotificationAsRead } from '@/api/notification'
import { createTicketOrder, getMyTicketOrders, type TicketType, type TicketOrder } from '@/api/ticket'
import { createRescue, type EmergencyRescue as EmergencyRescueType } from '@/api/emergencyRescue'
import { useUserStore } from '@/store/user'
import { useDigitalHumanStore } from '@/store/digitalHuman'
import request from '@/utils/request'
import { marked } from 'marked'
import { getImageUrl } from '@/utils/imageUrl'

// 获取路由实例
const route = useRoute()

// 数字人 store
const digitalHumanStore = useDigitalHumanStore()

// 清空数字人对话
const clearDigitalHumanChat = () => {
  digitalHumanStore.clearMessages()
  digitalHumanStore.initWelcomeMessage()
}

// 是否使用开发模式（仅用于其它模块，不再影响AI聊天）
const isDev = Boolean((import.meta as any)?.env?.DEV)
// 强制AI优先调用后端，失败再本地兜底
const useRemoteAI = true

// 用户store
const userStore = useUserStore()

// 景区ID映射（直接使用数字ID）
const scenicNameToId: Record<string, number> = {
  '1': 1,
  '2': 2,
  '3': 3,
  '4': 4,
  '5': 5
}

// 景区选择（从后端动态加载）
const scenicOptions = ref<Array<{ value: string; label: string }>>([])

// 加载景区选项列表
const loadScenicOptions = async () => {
  try {
    const { getScenicSpots } = await import('@/api/scenic')
    const response: any = await getScenicSpots({ city: '六盘水' })
    const scenics =
      response?.data?.data ||
      response?.data?.list ||
      response?.data ||
      response?.list ||
      response
    
    if (Array.isArray(scenics) && scenics.length > 0) {
      scenicOptions.value = scenics.map((scenic: any) => ({
        value: String(scenic.id),
        label: scenic.name
      }))
    } else {
      // 降级使用默认选项
      scenicOptions.value = [
        { value: '1', label: '梅花山风景区' },
        { value: '2', label: '玉舍国家森林公园' },
        { value: '3', label: '乌蒙大草原' },
        { value: '4', label: '水城古镇' },
        { value: '5', label: '明湖国家湿地公园' },
      ]
    }
  } catch (error) {
    // 降级使用默认选项
    scenicOptions.value = [
      { value: '1', label: '梅花山风景区' },
      { value: '2', label: '玉舍国家森林公园' },
      { value: '3', label: '乌蒙大草原' },
      { value: '4', label: '水城古镇' },
      { value: '5', label: '明湖国家湿地公园' },
    ]
  }
}

// 定义类型
interface ScenicData {
  id: string;
  name: string;
  description: string;
  image: string;
  status: string;
  statusDescription?: string;
  visitorCount: number;
  capacity: number;
  waitTime: number;
  predictedFlow?: number;
  trend?: string;
  recommended?: boolean;
  suggestion?: string;
  weather: {
    temperature: number;
    condition: string;
    humidity?: number;
    windSpeed?: number;
  };
  updateTime: string;
  hasEmergency: boolean;
  notices: Array<{
    title: string;
    content: string;
    time: string;
    important: boolean;
  }>;
  mapImage: string;
}

interface ChatMessage {
  type: string;
  content: string;
  isLoading?: boolean;
}

const selectedScenic = ref('1')  // 默认选择梅花山风景区（ID=1）
const currentScenic = ref<ScenicData | null>(null)
const visitorChartRef = ref<HTMLElement | null>(null)
let visitorChart: any = null

// 状态文本映射
const getStatusText = (status: string): string => {
  const statusMap: Record<string, string> = {
    'open': '正常开放',
    'normal': '正常开放',
    'busy': '较为拥挤',
    'idle': '人流较少',
    'limited': '限流中',
    'closed': '已关闭',
    'unknown': '状态未知'
  }
  return statusMap[status] || '正常开放'
}

// 状态类型映射（用于 el-tag）
const getStatusType = (status: string): string => {
  const typeMap: Record<string, string> = {
    'open': 'success',
    'normal': 'success',
    'busy': 'warning',
    'idle': 'info',
    'limited': 'danger',
    'closed': 'danger',
    'unknown': 'info'
  }
  return typeMap[status] || 'success'
}

// 获取百分比
const getPercentage = (current: number, max: number): number => {
  if (!max || max <= 0) return 0
  return Math.min(100, Math.round((current / max) * 100))
}

// 获取进度条颜色
const getProgressColor = (percentage: number): string => {
  if (percentage < 50) return '#4ade80'
  if (percentage < 80) return '#fbbf24'
  return '#ef4444'
}

// 获取进度条样式类
const getProgressClass = (percentage: number): string => {
  if (percentage < 50) return 'progress-low'
  if (percentage < 80) return 'progress-medium'
  return 'progress-high'
}

// 获取天气emoji
const getWeatherEmoji = (condition: string): string => {
  if (!condition) return '☀️'
  const conditionLower = condition.toLowerCase()
  if (conditionLower.includes('晴')) return '☀️'
  if (conditionLower.includes('多云')) return '⛅'
  if (conditionLower.includes('阴')) return '☁️'
  if (conditionLower.includes('雨')) return '🌧️'
  if (conditionLower.includes('雪')) return '❄️'
  if (conditionLower.includes('雾')) return '🌫️'
  if (conditionLower.includes('风')) return '💨'
  return '☀️'
}

// 缓存景区数据（用于获取真实的图片URL）
const scenicDataCache = ref<Record<string, any>>({})

// 设施相关
const facilities = ref<any[]>([])
const facilityCategory = ref('')
const facilityPage = ref(1)
const facilitySize = ref(10)
const hasMoreFacilities = ref(false)

// 活动相关
const activities = ref<any[]>([])
const activityPage = ref(1)
const activitySize = ref(5)

// 设施详情弹窗
const facilityDetailVisible = ref(false)
const selectedFacility = ref<any>(null)

// 公告相关
const announcements = ref<any[]>([])
const systemNotifications = ref<any[]>([])

// 聊天相关
const userMessage = ref('')
const chatMessages = ref<ChatMessage[]>([
  { type: 'ai', content: '你好！我是游韵华章旅游助手，专门为您提供六盘水景区的咨询服务。请问有什么可以帮助你的吗？' }
])

// 常见问题建议（聚焦六盘水景区）
const commonQuestions = [
  '六盘水梅花山风景区有哪些必去景点？',
  '六盘水有什么特色美食推荐？',
  '明天六盘水天气怎么样？适合出行吗？',
  '乌蒙大草原门票多少钱？怎么去？'
]

// 聊天框引用
const chatContainerRef = ref<HTMLElement | null>(null)
// 多轮对话ID（由后端返回），保持上下文一致
const conversationId = ref<string>('')

// Markdown 渲染器
// 配置marked选项
marked.setOptions({
  breaks: true,  // 支持换行
  gfm: true,     // 启用GitHub风格的Markdown
})

const renderMarkdown = (text: string) => {
  if (!text) return ''
  try {
    // 使用marked解析markdown（marked.parse等同于marked函数）
    return marked(text, {
      breaks: true,
      gfm: true
    }) as string
  } catch (e) {
    console.error('Markdown渲染失败:', e)
    // 降级处理：转义HTML并保留换行
    return (text || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\n/g, '<br/>')
  }
}

// 紧急救援对话框
const emergencyDialogVisible = ref(false)
const emergencyType = ref('')
const emergencyContact = ref('')
const emergencyLocation = ref('')
const emergencyDescription = ref('')

// 购票对话框
const ticketDialogVisible = ref(false)
const selectedTicketType = ref<TicketType>('adult')
const ticketCount = ref(1)
const ticketDate = ref<string | Date>('')
const visitorName = ref('')
const visitorPhone = ref('')
const ticketPrice = computed(() => {
  const basePrice = currentScenic.value ? getPriceByScenic(currentScenic.value.id) : 0
  return selectedTicketType.value === 'child' ? Math.floor(basePrice * 0.5) : basePrice
})

// 购票成功对话框（显示条形码）
const ticketSuccessDialogVisible = ref(false)
const ticketOrderInfo = ref<{
  orderNo: string
  barcode: string
  scenicName: string
  visitDate: string
  ticketCount: number
  totalAmount: number
} | null>(null)
const barcodeCanvas = ref<HTMLCanvasElement | null>(null)

// 我的订单对话框
const ordersDialogVisible = ref(false)
const myOrders = ref<TicketOrder[]>([])
const ordersLoading = ref(false)
const selectedOrder = ref<TicketOrder | null>(null)
const orderDetailDialogVisible = ref(false)
const orderBarcodeCanvas = ref<HTMLCanvasElement | null>(null)

// 视频讲解对话框
const guideDialogVisible = ref(false)
const currentVideo = ref<string>('')

// 评价对话框
const reviewDialogVisible = ref(false)
const reviewScore = ref(5)
const reviewContent = ref('')
const reviewImages = ref<string[]>([])
const reviewTags = ref<string[]>([])
const reviewVisitDate = ref<string | Date>('')
const reviewImageFiles = ref<any[]>([])

// 快捷服务
const quickServices = [
  { 
    icon: 'Ticket', 
    title: '在线购票', 
    description: '景区门票预订',
    action: 'ticket'
  },
  { 
    icon: 'List', 
    title: '我的订单', 
    description: '查看购票记录',
    action: 'orders'
  },
  { 
    icon: 'VideoPlay', 
    title: '视频讲解', 
    description: '景区视频介绍',
    action: 'guide'
  },
  { 
    icon: 'Warning', 
    title: '紧急救援', 
    description: '紧急情况求助',
    action: 'emergency'
  },
  { 
    icon: 'Comment', 
    title: '游客评价', 
    description: '分享您的体验',
    action: 'review'
  }
]

// 交通路线
const transportRoutes = ref([
  {
    type: 'bus',
    name: '旅游专线1号',
    duration: '约30分钟',
    description: '从市区汽车站直达梅花山景区，每30分钟一班。',
    cost: '¥10/人',
    nextDeparture: '10分钟后'
  },
  {
    type: 'car',
    name: '自驾路线',
    duration: '约45分钟',
    description: '沿S203省道行驶，根据导航前往景区停车场。',
    cost: '停车费¥20/次',
    nextDeparture: '随时可行'
  },
  {
    type: 'tour',
    name: '一日游专车',
    duration: '全天',
    description: '含多个景点的旅游专线车，配备导游服务。',
    cost: '¥150/人',
    nextDeparture: '9:00'
  }
])

// 初始化时加载所有景区数据到缓存
const loadAllScenicData = async () => {
  try {
    const { getScenicSpots } = await import('@/api/scenic')
    const response: any = await getScenicSpots({ city: '六盘水' })
    const scenics =
      response?.data?.data ||
      response?.data?.list ||
      response?.data ||
      response?.list ||
      response
    
    if (Array.isArray(scenics) && scenics.length > 0) {
      const cache: Record<string, any> = {}
      const options: Array<{ value: string; label: string }> = []
      
      scenics.forEach((scenic: any) => {
        const imageUrl = scenic.imageUrl || scenic.image_url
        const id = String(scenic.id)
        cache[id] = {
          id: scenic.id,
          name: scenic.name,
          imageUrl,
          description: scenic.description,
          maxCapacity: scenic.maxCapacity || scenic.max_capacity || 1000
        }
        options.push({ value: id, label: scenic.name })
        // 同时更新ID映射
        scenicNameToId[id] = scenic.id
      })
      
      scenicDataCache.value = cache
      scenicOptions.value = options
    }
  } catch (error) {
    // 加载景区列表失败，使用默认数据
  }
}

onMounted(async () => {
  // 首先加载所有景区数据到缓存（包括图片URL和景区选项）
  // 使用 await 确保缓存加载完成后再进行后续操作
  await loadAllScenicData()
  
  // 检查路由参数中是否有景区ID
  const queryScenicId = route.query.scenicId
  const queryScenicName = route.query.scenicName
  
  // 如果有路由参数，使用路由参数中的景区ID
  if (queryScenicId) {
    selectedScenic.value = String(queryScenicId)
    changeScenic(selectedScenic.value)
    ElMessage.success('已自动加载选定景区信息')

    // 从景区探索 3D 地图跳转过来 → 触发数字人景区讲解
    if (queryScenicName) {
      setTimeout(() => {
        digitalHumanStore.triggerScenicNarration(String(queryScenicName))
      }, 2500) // 延迟等待 EmbeddedDigitalHuman 的 WS 连接就绪
    }
  } else if (scenicOptions.value.length > 0) {
    // 否则默认选中第一个景区
    selectedScenic.value = scenicOptions.value[0].value
    changeScenic(selectedScenic.value)
  }
  
  // 只加载系统通知（公告会在watch中随景区切换自动加载，避免重复请求）
  loadSystemNotifications().catch(err => console.warn('加载通知失败:', err))
  
  // 添加窗口大小改变事件监听
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  // 清理echarts实例
  if (visitorChart) {
    visitorChart.dispose()
  }
  
  // 移除窗口大小改变事件监听
  window.removeEventListener('resize', handleResize)
})

// 切换景区
const changeScenic = async (value: string) => {
  ElMessage.success('数据加载中...')
  selectedScenic.value = value
  
  // 更新视频链接
  refreshVideo(value)
  
  try {
    // 始终优先尝试从后端API加载真实数据
    const scenicId = scenicNameToId[value]
    
    // 并行获取景区数据和天气数据
    const [detailData, flowData, weatherData] = await Promise.all([
      getScenicDetail(scenicId),
      getScenicFlow(scenicId),
      getWeatherInfo('六盘水').catch(() => null)  // 天气API失败不影响其他数据
    ])
    
    // 解包统一返回
    const detail: any = (detailData as any)?.data || detailData
    const flow: any = (flowData as any)?.data || flowData
    const weather: any = (weatherData as any)?.data || weatherData

    // 获取智能状态评估
    let intelligentStatus: any = null
    try {
      const { getScenicStatus } = await import('@/api/scenicStatus')
      const statusRes: any = await getScenicStatus(scenicId)
      intelligentStatus = statusRes?.data || statusRes
    } catch (error) {
      // 获取智能状态失败，使用基础判断
    }
    
    // 转换API数据为前端格式
    currentScenic.value = {
      id: value,
      name: detail.name,
      description: detail.description,
      image: getImageUrl(detail.imageUrl || detail.image_url) || getImageUrl('/images/1/gallery/202510/1760618835892.jpg'),
      visitorCount: flow.currentFlow,
      capacity: flow.maxCapacity,
      waitTime: intelligentStatus?.suggestedWaitTime || Math.floor(Math.random() * 20),
      status: intelligentStatus?.status || (flow.flowRate > 0.8 ? 'busy' : flow.flowRate > 0.5 ? 'normal' : 'idle'),
      statusDescription: intelligentStatus?.statusDescription || '',
      predictedFlow: intelligentStatus?.predictedFlow || flow.currentFlow,
      trend: intelligentStatus?.trend || 'stable',
      recommended: intelligentStatus?.recommended !== undefined ? intelligentStatus.recommended : true,
      suggestion: intelligentStatus?.suggestion || '',
      weather: {
        temperature: weather?.temperature || 22,
        condition: weather?.condition || '晴朗',
        humidity: weather?.humidity || 65,
        windSpeed: weather?.windSpeed || 3
      },
      updateTime: intelligentStatus?.updateTime || weather?.updateTime || flow.updateTime || new Date().toLocaleTimeString(),
      hasEmergency: false,
      notices: [],
      mapImage: getScenicMapImage(value)
    }
    
    await nextTick()
    initVisitorChart()
    
    // 加载活动和设施数据
    loadActivities()
    loadFacilities()
    
    ElMessage.success('数据加载成功')
  } catch (error) {
    // 加载景区数据失败
    ElMessage.error('加载景区数据失败，请重试')
    
    // 降级：使用缓存的景区数据
    const cachedData = scenicDataCache.value[value]
    if (cachedData) {
      // 尝试获取真实天气数据
      let weatherInfo = { temperature: 22, condition: '晴朗', humidity: 65, windSpeed: 3 }
      try {
        const wData: any = await getWeatherInfo('六盘水')
        const w: any = (wData as any)?.data || wData
        if (w) {
          weatherInfo = {
            temperature: w.temperature || 22,
            condition: w.condition || '晴朗',
            humidity: w.humidity || 65,
            windSpeed: w.windSpeed || 3
          }
        }
      } catch (e) {
        console.warn('获取天气数据失败，使用默认天气数据', e)
      }
      
      currentScenic.value = {
        id: value,
        name: cachedData.name,
        description: cachedData.description || '暂无描述',
        image: getImageUrl(cachedData.imageUrl),
        visitorCount: 0,
        capacity: cachedData.maxCapacity || 1000,
        waitTime: 0,
        status: 'unknown',
        statusDescription: '数据加载失败',
        predictedFlow: 0,
        trend: 'stable',
        recommended: true,
        suggestion: '数据加载失败，请稍后重试',
        weather: weatherInfo,
        updateTime: new Date().toLocaleTimeString(),
        hasEmergency: false,
        notices: [],
        mapImage: getScenicMapImage(value)
      }
    } else {
      // 完全没有数据时显示错误状态
      currentScenic.value = null
    }
    
    nextTick(() => {
      initVisitorChart()
      // 即使降级，也尝试加载真实的活动和设施
      loadActivities()
      loadFacilities()
    })
  }
}

// 刷新视频
const refreshVideo = async (scenicId: string) => {
  try {
    // 从后端API获取视频
    const id = scenicNameToId[scenicId]
    if (!id) {
      currentVideo.value = ''
      return
    }
    
    const response: any = await getScenicVideos(id)
    const videos: any[] = (response as any)?.data || response || []
    
    if (videos && videos.length > 0) {
      // 使用第一个视频
      const videoUrl = videos[0].videoUrl
      if (videoUrl && videoUrl.trim()) {
        // 添加时间戳参数，避免缓存
        currentVideo.value = `${videoUrl}?t=${new Date().getTime()}`
      } else {
        currentVideo.value = ''
      }
    } else {
      currentVideo.value = ''
    }
  } catch (error) {
    currentVideo.value = ''
  }
}

// 发送消息
const sendMessage = async () => {
  if (!userMessage.value.trim()) return
  
  // 添加用户消息
  chatMessages.value.push({ type: 'user', content: userMessage.value })
  
  // 保存用户消息并清空输入框
  const userQuery = userMessage.value
  userMessage.value = ''
  
  // 滚动到底部
  scrollToBottom()
  
  // 添加"正在输入"状态
  const loadingMessage = { type: 'ai', content: '正在思考...', isLoading: true }
  chatMessages.value.push(loadingMessage)
  scrollToBottom()
  
  try {
    // 天气类问题直接命中天气接口，六盘水
    const lower = userQuery.toLowerCase()
    if (lower.includes('天气') || lower.includes('气温') || lower.includes('下雨')) {
      try {
        const wResp: any = await getWeatherInfo('六盘水')
        const w: any = (wResp && typeof wResp === 'object' && 'data' in wResp) ? wResp.data : wResp
        const askTomorrow = lower.includes('明天') || lower.includes(' tomorrow ')
        const askToday = !askTomorrow
        const todayLine = `- **今日**: ${w?.minTemp != null ? Math.round(w.minTemp) : '--'}°C ~ ${w?.maxTemp != null ? Math.round(w.maxTemp) : '--'}°C`
        const tomorrowLine = (w?.tomorrowMinTemp != null || w?.tomorrowMaxTemp != null)
          ? `- **明日**: ${w?.tomorrowMinTemp != null ? Math.round(w.tomorrowMinTemp) : '--'}°C ~ ${w?.tomorrowMaxTemp != null ? Math.round(w.tomorrowMaxTemp) : '--'}°C`
          : ''
        const currentLine = `- **当前**: ${w?.temperature != null ? Math.round(w.temperature) : '--'}°C，${w?.condition || '—'}，湿度 ${w?.humidity != null ? Math.round(w.humidity) : '--'}%`
        const commonTail = `${w?.windSpeed != null ? `\n- **风速**: ${w.windSpeed} m/s` : ''}\n- **更新时间**: ${w?.updateTime || '—'}`
        // 询问明天：不展示实时“当前”，只展示“明日范围”
        const md = askTomorrow
          ? `### 六盘水明日天气\n\n${tomorrowLine || todayLine}${commonTail}`
          : `### 六盘水实时天气\n\n${currentLine}\n${todayLine}${commonTail}`
        const loadingIndex = chatMessages.value.findIndex(msg => msg.isLoading)
        if (loadingIndex !== -1) {
          chatMessages.value.splice(loadingIndex, 1, { type: 'ai', content: md })
        } else {
          chatMessages.value.push({ type: 'ai', content: md })
        }
        // 补一条AI建议（通过后端大模型生成）
        const suggestionPrompt = askTomorrow
          ? `根据明日气温范围（${Math.round(w?.tomorrowMinTemp ?? w?.minTemp ?? 0)}-${Math.round(w?.tomorrowMaxTemp ?? w?.maxTemp ?? 0)}°C），给出适合的出行与穿衣建议，简洁分点回复。`
          : `根据今日气温范围（${Math.round(w?.minTemp ?? 0)}-${Math.round(w?.maxTemp ?? 0)}°C）以及当前体感“${w?.condition || '—'}”，给出适合的出行与穿衣建议，简洁分点回复。`
        const response: any = await sendChatMessage({
          message: suggestionPrompt,
          scenicId: selectedScenic.value ? scenicNameToId[selectedScenic.value] : undefined,
          conversationId: conversationId.value || undefined
        })
        const d = response?.data || response
        if (d?.conversationId) conversationId.value = d.conversationId
        if (d?.reply) chatMessages.value.push({ type: 'ai', content: d.reply })
        // 同步当前卡片简要天气（如存在）
        if (currentScenic.value && w) {
          if (typeof w.temperature === 'number') currentScenic.value.weather.temperature = Math.round(w.temperature)
          if (typeof w.condition === 'string') currentScenic.value.weather.condition = w.condition
          currentScenic.value.updateTime = w.updateTime || new Date().toLocaleTimeString()
        }
        scrollToBottom()
        return
      } catch (e) {
        // 忽略失败，继续走AI
      }
    }
    
    let reply: string
    // 优先调用后端AI
    if (useRemoteAI) {
      const response: any = await sendChatMessage({
        message: userQuery,
        scenicId: selectedScenic.value ? scenicNameToId[selectedScenic.value] : undefined,
        conversationId: conversationId.value || undefined
      })
      const d = response?.data || response
      reply = d?.reply
      if (d?.conversationId) conversationId.value = d.conversationId
      if (!reply) throw new Error('empty_reply')
    } else {
      throw new Error('force_local')
    }
    
    // 替换加载消息
    const loadingIndex = chatMessages.value.findIndex(msg => msg.isLoading)
    if (loadingIndex !== -1) {
      chatMessages.value.splice(loadingIndex, 1, { type: 'ai', content: reply })
    } else {
      chatMessages.value.push({ type: 'ai', content: reply })
    }
    
    // 滚动到底部
    scrollToBottom()
  } catch (error) {
    console.error('AI回复失败:', error)
    // 失败兜底：提示失败（不再返回本地固定话术，避免与后端不一致）
    const loadingIndex = chatMessages.value.findIndex(msg => msg.isLoading)
    const tip = '抱歉，服务暂时不可用，请稍后再试。'
    if (loadingIndex !== -1) {
      chatMessages.value.splice(loadingIndex, 1, { type: 'ai', content: tip })
    } else {
      chatMessages.value.push({ type: 'ai', content: tip })
    }
    scrollToBottom()
  }
}

// 直接提问预设问题
const askQuestion = (question: string) => {
  userMessage.value = question
  sendMessage()
}

// 滚动聊天框到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainerRef.value) {
      chatContainerRef.value.scrollTop = chatContainerRef.value.scrollHeight
    }
  })
}

// 获取AI回复
const getAIResponse = (query: string): string => {
  // 转换为小写并去除空格，用于匹配
  const normalizedQuery = query.toLowerCase().trim()
  
  // 景点相关问题
  if (normalizedQuery.includes('必去景点') || normalizedQuery.includes('值得去') || normalizedQuery.includes('推荐景点')) {
    if (normalizedQuery.includes('梅花山')) {
      return '梅花山风景区必去景点有：1. 梅花谷-欣赏梅花盛开；2. 望江亭-可俯瞰整个风景区；3. 飞瀑廊-壮观的瀑布群；4. 古松园-百年古松林。建议安排4-5小时游览。'
    } else if (normalizedQuery.includes('乌蒙') || normalizedQuery.includes('草原')) {
      return '乌蒙大草原必去景点有：1. 草原观景台-360度全景；2. 民族村寨-体验少数民族文化；3. 骑马场-可体验草原骑马；4. 篝火广场-晚间有民族表演。夏季和秋季是最佳游览时间。'
    } else {
      return '六盘水地区值得推荐的景点有：梅花山风景区、乌蒙大草原、玉舍国家森林公园、明湖国家湿地公园和水城古镇。您想了解哪个景点的详细信息呢？'
    }
  }
  
  // 美食相关问题
  else if (normalizedQuery.includes('美食') || normalizedQuery.includes('吃什么') || normalizedQuery.includes('特色菜')) {
    return '六盘水地区特色美食有：1. 酸汤鱼-贵州名菜，鲜酸可口；2. 豆腐圆子-当地特色小吃；3. 凉粉-清爽可口；4. 糯米饭-少数民族特色；5. 烙锅-农家风味。推荐您在市区的"贵州味道"或景区内的"农家乐"品尝。'
  }
  
  // 天气相关问题
  else if (normalizedQuery.includes('天气') || normalizedQuery.includes('气温') || normalizedQuery.includes('下雨')) {
    if (normalizedQuery.includes('明天')) {
      return '根据气象预报，明天六盘水天气晴转多云，气温17-26°C，适合户外活动，但建议带上外套，早晚温差较大。'
    } else if (normalizedQuery.includes('后天') || normalizedQuery.includes('未来')) {
      return '未来三天六盘水天气：明天晴转多云，17-26°C；后天多云，16-24°C；大后天小雨，15-22°C。如果您计划户外活动，建议选择明天或后天。'
    } else {
      return '今天六盘水天气晴朗，气温18-25°C，非常适合游玩。紫外线较强，建议做好防晒措施。'
    }
  }
  
  // 门票相关问题
  else if (normalizedQuery.includes('门票') || normalizedQuery.includes('价格') || normalizedQuery.includes('多少钱')) {
    if (normalizedQuery.includes('梅花山')) {
      return '梅花山风景区门票60元/人，学生、老人和军人凭证半价。景区内观光车另收费20元/人。'
    } else if (normalizedQuery.includes('乌蒙') || normalizedQuery.includes('草原')) {
      return '乌蒙大草原门票65元/人，包含草原主景区游览。骑马、射箭等活动需另付费。现在正值旅游旺季，可在线预约购票避免排队。'
    } else if (normalizedQuery.includes('玉舍') || normalizedQuery.includes('森林公园')) {
      return '玉舍国家森林公园门票40元/人，儿童身高1.2米以下免票。公园内有生态游览车，单程15元/人。'
    } else if (normalizedQuery.includes('明湖') || normalizedQuery.includes('湿地')) {
      return '明湖国家湿地公园门票免费，但部分游船项目需收费，如划船30元/小时，游船50元/人。'
    } else if (normalizedQuery.includes('水城') || normalizedQuery.includes('古镇')) {
      return '水城古镇免门票，但镇内部分博物馆和古宅需单独购票，价格在10-30元不等。'
    } else {
      return '六盘水主要景点门票价格：梅花山风景区60元/人，乌蒙大草原65元/人，玉舍国家森林公园40元/人，明湖国家湿地公园免费，水城古镇免费。您想了解哪个景点的详细信息？'
    }
  }
  
  // 交通相关问题
  else if (normalizedQuery.includes('交通') || normalizedQuery.includes('怎么去') || normalizedQuery.includes('路线')) {
    if (normalizedQuery.includes('梅花山')) {
      return '从六盘水市区到梅花山风景区有以下方式：1. 旅游专线1号巴士，从市区汽车站直达景区，每30分钟一班，票价10元/人；2. 出租车约50元；3. 自驾导航"梅花山风景区"即可，有停车场。'
    } else if (normalizedQuery.includes('乌蒙') || normalizedQuery.includes('草原')) {
      return '前往乌蒙大草原可以：1. 在市区汽车站乘坐2号旅游专线，票价15元/人；2. 参加一日游团队，含往返交通约150元/人；3. 自驾约1小时车程，导航"乌蒙大草原游客中心"。'
    } else {
      return '六盘水市内有完善的公共交通和旅游专线连接各大景点。您可以选择：1. 旅游专线巴士，票价10-20元不等；2. 出租车，起步价8元；3. 共享单车，适合市区短途；4. 参加旅行团一日游。您需要前往哪个具体景点呢？'
    }
  }
  
  // 住宿相关问题
  else if (normalizedQuery.includes('住宿') || normalizedQuery.includes('酒店') || normalizedQuery.includes('住哪')) {
    return '六盘水住宿推荐：1. 市中心-交通便利，如锦江之星(200-300元/晚)；2. 梅花山附近-环境优美，如梅花山度假村(400-600元/晚)；3. 水城古镇-体验古镇风情，如水城客栈(150-250元/晚)。旅游旺季建议提前预订。'
  }
  
  // 游玩时间相关问题
  else if (normalizedQuery.includes('几天') || normalizedQuery.includes('行程') || normalizedQuery.includes('游玩时间')) {
    return '游览六盘水建议安排2-3天：第1天游览梅花山风景区(全天)；第2天游览乌蒙大草原(上午)和玉舍国家森林公园(下午)；第3天游览明湖湿地公园(上午)和水城古镇(下午)。如时间有限，推荐优先游览梅花山和乌蒙大草原。'
  }
  
  // 拥挤程度相关问题
  else if (normalizedQuery.includes('拥挤') || normalizedQuery.includes('人多') || normalizedQuery.includes('排队')) {
    if (normalizedQuery.includes('梅花山')) {
      return '目前梅花山景区游客适中，不需要排队。建议上午9点-11点，下午4点后前往，游客较少。周末和节假日相对拥挤，建议提前1小时到达。'
    } else if (normalizedQuery.includes('乌蒙') || normalizedQuery.includes('草原')) {
      return '乌蒙大草原目前游客较多，入口处可能需要排队15-20分钟。建议早上8点前或下午3点后前往，人流较少。'
    } else {
      return '目前六盘水主要景区中，乌蒙大草原和明湖湿地公园游客较多，梅花山风景区、玉舍森林公园和水城古镇游客适中。建议错峰出行，避开11:00-14:00的高峰期。'
    }
  }
  
  // 默认回复
  else {
    return '感谢您的咨询。我是游韵华章旅游助手，可以为您提供景点推荐、美食指南、天气预报、交通路线等信息。如有其他问题，请随时向我咨询。'
  }
}

// 处理服务点击
const handleService = (action: string) => {
  switch (action) {
    case 'ticket':
      // 显示购票对话框
      ticketDialogVisible.value = true
      // 设置默认日期为明天
      const tomorrow = new Date()
      tomorrow.setDate(tomorrow.getDate() + 1)
      ticketDate.value = tomorrow.toISOString().split('T')[0]
      break
    case 'orders':
      // 显示我的订单对话框
      loadMyOrders()
      ordersDialogVisible.value = true
      break
    case 'guide':
      // 显示视频讲解对话框
      guideDialogVisible.value = true
      // 加载当前景区的视频
      refreshVideo(selectedScenic.value)
      break
    case 'emergency':
      emergencyDialogVisible.value = true
      break
    case 'review':
      // 显示评价对话框
      reviewDialogVisible.value = true
      // 设置默认游览日期为今天
      if (!reviewVisitDate.value) {
        reviewVisitDate.value = new Date().toISOString().split('T')[0]
      }
      break
    default:
      break
  }
}

// 打开视频讲解对话框
const openVideoGuide = () => {
  handleService('guide')
}

// 加载我的订单
const loadMyOrders = async () => {
  const token = localStorage.getItem('token') || userStore.token
  if (!token) {
    ElMessage.warning('请先登录后查看订单')
    return
  }
  
  ordersLoading.value = true
  try {
    const res: any = await getMyTicketOrders({ page: 1, size: 50 })
    const data = res?.data || res
    myOrders.value = data?.list || data?.records || (Array.isArray(data) ? data : [])
  } catch (error) {
    console.error('加载订单失败:', error)
    ElMessage.error('加载订单失败')
  } finally {
    ordersLoading.value = false
  }
}

// 查看订单详情（显示条形码）
const viewOrderDetail = (order: TicketOrder) => {
  selectedOrder.value = order
  orderDetailDialogVisible.value = true
  // 延迟绘制条形码
  setTimeout(() => {
    drawOrderBarcode(order.barcode || generateLocalBarcode())
  }, 100)
}

// 绘制订单条形码
const drawOrderBarcode = (code: string) => {
  const canvas = orderBarcodeCanvas.value
  if (!canvas || !code) return
  
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  
  // 设置画布尺寸
  canvas.width = 280
  canvas.height = 100
  
  // 清空画布
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  
  // EAN-13 编码表
  const L_CODE: Record<string, string> = {
    '0': '0001101', '1': '0011001', '2': '0010011', '3': '0111101', '4': '0100011',
    '5': '0110001', '6': '0101111', '7': '0111011', '8': '0110111', '9': '0001011'
  }
  const R_CODE: Record<string, string> = {
    '0': '1110010', '1': '1100110', '2': '1101100', '3': '1000010', '4': '1011100',
    '5': '1001110', '6': '1010000', '7': '1000100', '8': '1001000', '9': '1110100'
  }
  
  // 确保条形码是13位
  let barcode = code.padStart(13, '0').slice(0, 13)
  
  // 生成条形码二进制串
  let binary = '101' // 起始符
  for (let i = 1; i < 7; i++) {
    binary += L_CODE[barcode[i]] || '0000000'
  }
  binary += '01010' // 中间分隔符
  for (let i = 7; i < 13; i++) {
    binary += R_CODE[barcode[i]] || '0000000'
  }
  binary += '101' // 结束符
  
  // 绘制条形码
  const barWidth = 2
  const barHeight = 60
  const startX = (canvas.width - binary.length * barWidth) / 2
  const startY = 10
  
  ctx.fillStyle = '#000000'
  for (let i = 0; i < binary.length; i++) {
    if (binary[i] === '1') {
      ctx.fillRect(startX + i * barWidth, startY, barWidth, barHeight)
    }
  }
  
  // 绘制条形码数字
  ctx.fillStyle = '#000000'
  ctx.font = '14px monospace'
  ctx.textAlign = 'center'
  ctx.fillText(barcode, canvas.width / 2, startY + barHeight + 20)
}

// 获取订单状态文本
const getOrderStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'pending': '待支付',
    'paid': '已支付',
    'used': '已使用',
    'cancelled': '已取消',
    'refunded': '已退款'
  }
  return statusMap[status] || status
}

// 获取订单状态类型（用于 el-tag）
const getOrderStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    'pending': 'warning',
    'paid': 'success',
    'used': 'info',
    'cancelled': 'danger',
    'refunded': 'info'
  }
  return typeMap[status] || 'info'
}

// 获取景区购票链接 - 不再使用外部链接
const getTicketUrl = (scenicId: string): string => {
  // 一码游贵州基础URL
  const baseUrl = 'https://ymyguizhou.com/scenic'
  
  // 景区ID映射表（这里使用模拟数据，实际应使用真实的景区ID）
  const scenicMap: Record<string, string> = {
    'meihuashan': '10086', // 梅花山风景区ID
    'zhijindong': '10087', // 乌蒙大草原ID
    'yushe': '10088',      // 玉舍国家森林公园ID
    'minghu': '10089',     // 明湖国家湿地公园ID
    'panzhou': '10090'     // 水城古镇ID
  }
  
  // 获取对应的景区ID，如果没有找到则使用默认链接
  const realScenicId = scenicMap[scenicId as keyof typeof scenicMap] || ''
  
  // 返回完整的购票链接
  return `${baseUrl}?id=${realScenicId}&source=smarttourism`
}

// 获取语音导览链接
const getGuideUrl = (scenicId: string): string => {
  // 语音导览基础URL
  const baseUrl = 'https://guide.liupanshui.travel/audio'
  
  // 景区导览ID映射表
  const guideMap: Record<string, string> = {
    'meihuashan': 'meihuashan',
    'zhijindong': 'wumeng',
    'yushe': 'yushe',
    'minghu': 'minghu',
    'panzhou': 'shuicheng'
  }
  
  // 获取对应的导览ID
  const guideId = guideMap[scenicId as keyof typeof guideMap] || 'default'
  
  // 返回完整的导览链接
  return `${baseUrl}?scenic=${guideId}&lang=zh`
}

// 获取评价系统链接
const getReviewUrl = (scenicId: string): string => {
  // 评价系统基础URL
  const baseUrl = 'https://review.liupanshui.travel/submit'
  
  // 景区评价ID映射表
  const reviewMap: Record<string, string> = {
    'meihuashan': 'meihuashan',
    'zhijindong': 'wumeng',
    'yushe': 'yushe',
    'minghu': 'minghu',
    'panzhou': 'shuicheng'
  }
  
  // 获取对应的评价ID
  const reviewId = reviewMap[scenicId as keyof typeof reviewMap] || 'default'
  
  // 返回完整的评价链接
  return `${baseUrl}?scenic=${reviewId}&source=app`
}

// 获取交通图标
const getTransportIcon = (type: string) => {
  const icons: Record<string, { icon: string, color: string }> = {
    'bus': { icon: 'Van', color: 'text-green-400' },
    'car': { icon: 'Van', color: 'text-blue-400' },
    'bike': { icon: 'Bicycle', color: 'text-cyan-400' },
    'boat': { icon: 'Ship', color: 'text-indigo-400' },
    'tour': { icon: 'Guide', color: 'text-yellow-400' }
  }
  return icons[type as keyof typeof icons] || { icon: 'Van', color: 'text-gray-500' }
}

// 根据景区ID获取票价
const getPriceByScenic = (scenicId: string): number => {
  const priceMap: Record<string, number> = {
    '1': 60,  // 梅花山
    '3': 65,  // 乌蒙大草原
    '2': 40,  // 玉舍
    '5': 0,   // 明湖（免费）
    '4': 0    // 水城古镇（免费）
  }
  return priceMap[scenicId as keyof typeof priceMap] || 50
}

// 加载景区导览点
const loadGuideSpotsForScenic = (scenicId: string) => {
  // 此函数已不再需要，保留空实现
}

// 获取景区视频链接 - 已废弃，现在从API获取
// const getScenicVideo = (scenicId: string): string => {
//   // 旧的硬编码方式已废弃
//   return ''
// }

// 提交购票
const submitTicket = async () => {
  // 检查是否登录
  const token = localStorage.getItem('token') || userStore.token
  if (!token) {
    ElMessage.warning('请先登录后再购票')
    ticketDialogVisible.value = false
    return
  }
  
  if (!ticketDate.value) {
    ElMessage.warning('请选择参观日期')
    return
  }
  
  if (!visitorName.value) {
    ElMessage.warning('请输入游客姓名')
    return
  }
  
  if (!visitorPhone.value) {
    ElMessage.warning('请输入联系电话')
    return
  }
  
  // 验证手机号格式
  const phoneReg = /^1[3-9]\d{9}$/
  if (!phoneReg.test(visitorPhone.value)) {
    ElMessage.warning('请输入正确的手机号码')
    return
  }
  
  // 获取当前景区ID
  if (!selectedScenic.value) {
    ElMessage.warning('请先选择景区')
    return
  }
  
  const scenicId = scenicNameToId[selectedScenic.value]
  if (!scenicId) {
    ElMessage.error('景区ID获取失败')
    return
  }
  
  try {
    // 格式化参观日期
    const visitDate = ticketDate.value instanceof Date 
      ? ticketDate.value.toISOString().split('T')[0]
      : ticketDate.value
    
    // 调用后端API创建订单
    const res: any = await createTicketOrder({
      scenicId,
      ticketType: selectedTicketType.value,
      ticketCount: ticketCount.value,
      visitDate,
      visitorName: visitorName.value.trim(),
      visitorPhone: visitorPhone.value.trim()
    })
    
    const data = res?.data || res
    
    // 关闭购票对话框
    ticketDialogVisible.value = false
    
    // 保存订单信息并显示条形码对话框
    ticketOrderInfo.value = {
      orderNo: data.orderNo || '',
      barcode: data.barcode || generateLocalBarcode(),
      scenicName: currentScenic.value?.name || selectedScenic.value || '',
      visitDate: visitDate,
      ticketCount: ticketCount.value,
      totalAmount: data.totalAmount || (ticketPrice.value * ticketCount.value)
    }
    ticketSuccessDialogVisible.value = true
    
    // 延迟绘制条形码（等待DOM更新）
    setTimeout(() => {
      drawBarcode(ticketOrderInfo.value?.barcode || '')
    }, 100)
    
    // 重置表单
    selectedTicketType.value = 'adult'
    ticketCount.value = 1
    ticketDate.value = ''
    visitorName.value = ''
    visitorPhone.value = ''
  } catch (error: any) {
    console.error('购票失败:', error)
    ElMessage.error(error?.message || '购票失败，请重试')
  }
}

// 生成本地条形码（备用）
const generateLocalBarcode = (): string => {
  const timestamp = Date.now().toString().slice(-8)
  const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0')
  return `69${timestamp}${random}`
}

// 绘制条形码到Canvas
const drawBarcode = (code: string) => {
  const canvas = barcodeCanvas.value
  if (!canvas || !code) return
  
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  
  // 设置画布尺寸
  canvas.width = 280
  canvas.height = 100
  
  // 清空画布
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  
  // EAN-13 编码表
  const L_CODE: Record<string, string> = {
    '0': '0001101', '1': '0011001', '2': '0010011', '3': '0111101', '4': '0100011',
    '5': '0110001', '6': '0101111', '7': '0111011', '8': '0110111', '9': '0001011'
  }
  const R_CODE: Record<string, string> = {
    '0': '1110010', '1': '1100110', '2': '1101100', '3': '1000010', '4': '1011100',
    '5': '1001110', '6': '1010000', '7': '1000100', '8': '1001000', '9': '1110100'
  }
  
  // 确保条形码是13位
  let barcode = code.padStart(13, '0').slice(0, 13)
  
  // 生成条形码二进制串
  let binary = '101' // 起始符
  for (let i = 1; i < 7; i++) {
    binary += L_CODE[barcode[i]] || '0000000'
  }
  binary += '01010' // 中间分隔符
  for (let i = 7; i < 13; i++) {
    binary += R_CODE[barcode[i]] || '0000000'
  }
  binary += '101' // 结束符
  
  // 绘制条形码
  const barWidth = 2
  const barHeight = 60
  const startX = (canvas.width - binary.length * barWidth) / 2
  const startY = 10
  
  ctx.fillStyle = '#000000'
  for (let i = 0; i < binary.length; i++) {
    if (binary[i] === '1') {
      ctx.fillRect(startX + i * barWidth, startY, barWidth, barHeight)
    }
  }
  
  // 绘制条形码数字
  ctx.fillStyle = '#000000'
  ctx.font = '14px monospace'
  ctx.textAlign = 'center'
  ctx.fillText(barcode, canvas.width / 2, startY + barHeight + 20)
}

// 保存票据图片
const saveTicketImage = () => {
  const canvas = barcodeCanvas.value
  if (!canvas) {
    ElMessage.warning('条形码未生成')
    return
  }
  
  // 创建下载链接
  const link = document.createElement('a')
  link.download = `ticket_${ticketOrderInfo.value?.orderNo || 'barcode'}.png`
  link.href = canvas.toDataURL('image/png')
  link.click()
  
  ElMessage.success('票据已保存')
}

// 播放语音导览
const playGuideAudio = (spotId: string) => {
  // 此函数已不再使用，保留空实现
}

// 停止播放
const stopAudio = () => {
  // 此函数已不再使用，保留空实现
}

// 关闭导览对话框
const closeGuideDialog = () => {
  guideDialogVisible.value = false
}

// 处理评价图片变化
const handleReviewImageChange = (file: any, fileList: any[]) => {
  reviewImageFiles.value = fileList
}

// 处理评价图片移除
const handleReviewImageRemove = (file: any, fileList: any[]) => {
  reviewImageFiles.value = fileList
}

// 上传单个图片到OSS
const uploadImageToOSS = async (file: File): Promise<string> => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('folder', 'reviews')
  
  try {
    const res: any = await request({
      url: '/upload/file',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    const data = res?.data || res
    if (data?.url) {
      return data.url
    } else {
      throw new Error('上传失败，未返回URL')
    }
  } catch (error: any) {
    console.error('图片上传失败:', error)
    throw new Error(error?.message || '图片上传失败')
  }
}

// 提交评价
const submitReview = async () => {
  // 检查是否登录
  const token = localStorage.getItem('token') || userStore.token
  if (!token) {
    ElMessage.warning('请先登录后再提交评价')
    reviewDialogVisible.value = false
    return
  }
  
  // 验证必填项
  if (!reviewContent.value || reviewContent.value.trim() === '') {
    ElMessage.warning('请输入评价内容')
    return
  }
  
  if (!reviewVisitDate.value) {
    ElMessage.warning('请选择游览日期')
    return
  }
  
  // 获取当前景区ID
  if (!selectedScenic.value) {
    ElMessage.warning('请先选择景区')
    return
  }
  
  const scenicId = scenicNameToId[selectedScenic.value]
  if (!scenicId) {
    ElMessage.error('景区ID获取失败')
    return
  }
  
  try {
    // 1. 如果有图片，先上传图片
    const imageUrls: string[] = []
    if (reviewImageFiles.value.length > 0) {
      ElMessage.info('正在上传图片...')
      
      for (const fileItem of reviewImageFiles.value) {
        // 验证文件
        const file = fileItem.raw || fileItem
        if (!file) continue
        
        // 验证文件类型
        if (!file.type.startsWith('image/')) {
          ElMessage.warning(`${file.name} 不是图片文件，已跳过`)
          continue
        }
        
        // 验证文件大小（2MB）
        if (file.size > 2 * 1024 * 1024) {
          ElMessage.warning(`${file.name} 超过2MB，已跳过`)
          continue
        }
        
        try {
          const url = await uploadImageToOSS(file)
          imageUrls.push(url)
        } catch (error) {
          // 上传图片失败
          ElMessage.warning(`${file.name} 上传失败`)
        }
      }
      
      if (imageUrls.length > 0) {
        ElMessage.success(`成功上传 ${imageUrls.length} 张图片`)
      }
    }
    
    // 2. 格式化游览日期
    const visitDate = reviewVisitDate.value instanceof Date 
      ? reviewVisitDate.value.toISOString().split('T')[0]
      : reviewVisitDate.value
    
    // 3. 调用后端API提交评价
    await submitReviewAPI({
      scenicId,
      rating: reviewScore.value,
      content: reviewContent.value.trim(),
      tags: reviewTags.value,
      images: imageUrls,  // 使用上传后的图片URL
      visitDate
    })
    
    ElMessage.success('感谢您的评价！')
    reviewDialogVisible.value = false
    
    // 4. 重置表单
    reviewScore.value = 5
    reviewContent.value = ''
    reviewImages.value = []
    reviewTags.value = []
    reviewVisitDate.value = ''
    reviewImageFiles.value = []
  } catch (error: any) {
    ElMessage.error(error?.message || '提交评价失败，请重试')
  }
}

// 打开地图导航
const openMap = () => {
  ElMessage.success('已打开地图导航')
}

// 提交紧急救援请求
const submitEmergency = async () => {
  // 检查是否登录
  const token = localStorage.getItem('token') || userStore.token
  if (!token) {
    ElMessage.warning('请先登录后再提交救援请求')
    emergencyDialogVisible.value = false
    return
  }
  
  // 验证必填项
  if (!emergencyType.value) {
    ElMessage.warning('请选择救援类型')
    return
  }
  
  if (!emergencyLocation.value) {
    ElMessage.warning('请描述您的位置')
    return
  }
  
  if (!emergencyContact.value) {
    ElMessage.warning('请输入联系电话')
    return
  }
  
  // 验证手机号格式
  const phoneReg = /^1[3-9]\d{9}$/
  if (!phoneReg.test(emergencyContact.value)) {
    ElMessage.warning('请输入正确的手机号码')
    return
  }
  
  // 获取当前景区ID
  if (!selectedScenic.value) {
    ElMessage.warning('请先选择景区')
    return
  }
  
  const scenicId = scenicNameToId[selectedScenic.value]
  if (!scenicId) {
    ElMessage.error('景区ID获取失败')
    return
  }
  
  try {
    // 映射救援类型到后端枚举值
    const typeMap: Record<string, string> = {
      'medical': 'MEDICAL',
      'lost': 'LOST',
      'accident': 'ACCIDENT',
      'other': 'OTHER'
    }
    
    // 构建救援请求数据
    const rescueData: EmergencyRescueType = {
      scenicId: scenicId,
      rescueType: typeMap[emergencyType.value] || 'OTHER',
      contactName: userStore.userInfo?.nickname || userStore.userInfo?.username || '游客',
      contactPhone: emergencyContact.value.trim(),
      location: emergencyLocation.value.trim(),
      description: emergencyDescription.value?.trim() || '',
      emergencyLevel: emergencyType.value === 'medical' ? 'URGENT' : 'NORMAL'
    }
    
    // 调用后端API
    await createRescue(rescueData)
    
    ElMessage.success('您的救援请求已提交，工作人员将尽快与您联系')
    emergencyDialogVisible.value = false
    
    // 重置表单
    emergencyType.value = ''
    emergencyLocation.value = ''
    emergencyDescription.value = ''
    emergencyContact.value = ''
  } catch (error: any) {
    console.error('提交救援请求失败:', error)
    ElMessage.error(error?.message || '提交失败，请重试')
  }
}

// 初始化游客流量图表
const initVisitorChart = async () => {
  if (!visitorChartRef.value || !currentScenic.value) return
  
  visitorChart = echarts.init(visitorChartRef.value)
  
  // 默认时间和数据
  let hours = ['8:00', '9:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00']
  let flowData = [120, 200, 350, 500, 420, 390, 450, 520, 390, 200]
  
  // 尝试从后端获取真实的小时客流分布数据
  try {
    const scenicId = scenicNameToId[selectedScenic.value]
    if (scenicId) {
      const { getHourlyDistribution } = await import('@/api/prediction')
      const today = new Date().toISOString().split('T')[0]
      const response: any = await getHourlyDistribution(today, scenicId)
      const data = response?.data || response
      
      if (data && data.hours && data.predictions) {
        hours = data.hours
        flowData = data.predictions
      }
    }
  } catch (error) {
    // 获取失败时使用默认数据
  }
  
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#FFFFFF',
      borderColor: '#EBEEF5',
      borderWidth: 1,
      borderRadius: 12,
      padding: [12, 16],
      textStyle: {
        color: '#303133',
        fontSize: 13
      },
      axisPointer: {
        type: 'line',
        lineStyle: {
          color: 'rgba(42, 157, 143, 0.5)',
          width: 2,
          type: 'dashed'
        }
      },
      formatter: (params: any) => {
        const data = params[0]
        return `
          <div style="font-weight: 600; color: #2A9D8F; margin-bottom: 8px; font-size: 14px;">
            🕐 ${data.name}
          </div>
          <div style="display: flex; align-items: center; gap: 8px;">
            <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: linear-gradient(135deg, #2A9D8F, #457B9D);"></span>
            <span style="color: #606266;">游客数量</span>
            <span style="color: #303133; font-weight: 600; margin-left: auto;">${data.value} 人</span>
          </div>
        `
      }
    },
    grid: {
      top: '12%',
      left: '3%',
      right: '4%',
      bottom: '12%',
      containLabel: true
    },
    xAxis: [
      {
        type: 'category',
        data: hours,
        boundaryGap: false,
        axisLine: {
          lineStyle: {
            color: 'rgba(42, 157, 143, 0.3)',
            width: 2
          }
        },
        axisTick: {
          show: false
        },
        axisLabel: {
          color: '#606266',
          fontSize: 11,
          margin: 12
        }
      }
    ],
    yAxis: [
      {
        type: 'value',
        name: '人数',
        nameTextStyle: {
          color: '#64748b',
          fontSize: 12,
          padding: [0, 0, 8, 0]
        },
        axisLine: {
          show: false
        },
        axisTick: {
          show: false
        },
        axisLabel: {
          color: '#64748b',
          fontSize: 11
        },
        splitLine: {
          lineStyle: {
            color: '#F0F2F5',
            type: 'dashed'
          }
        }
      }
    ],
    series: [
      {
        name: '游客数量',
        type: 'line',
        smooth: 0.4,
        lineStyle: {
          width: 3,
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#2A9D8F' },
            { offset: 0.5, color: '#457B9D' },
            { offset: 1, color: '#E9C46A' }
          ]),
          shadowColor: 'rgba(42, 157, 143, 0.3)',
          shadowBlur: 12,
          shadowOffsetY: 4
        },
        symbol: 'circle',
        symbolSize: 10,
        showSymbol: false,
        emphasis: {
          focus: 'series',
          itemStyle: {
            color: '#fff',
            borderColor: '#2A9D8F',
            borderWidth: 3,
            shadowColor: 'rgba(42, 157, 143, 0.5)',
            shadowBlur: 12
          }
        },
        itemStyle: {
          color: '#2A9D8F',
          borderColor: '#fff',
          borderWidth: 2
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(42, 157, 143, 0.25)' },
            { offset: 0.5, color: 'rgba(69, 123, 157, 0.12)' },
            { offset: 1, color: 'rgba(233, 196, 106, 0.02)' }
          ])
        },
        data: flowData
      }
    ]
  }
  
  visitorChart.setOption(option)
  
  window.addEventListener('resize', () => {
    visitorChart?.resize()
  })
}

// 获取景区图片（优先使用从API获取的真实数据）
const getScenicImage = (scenicId: string): string => {
  // 先检查缓存中是否有真实的景区数据
  const cached = scenicDataCache.value[scenicId]
  if (cached && cached.imageUrl) {
    return getImageUrl(cached.imageUrl)
  }
  
  // 备用硬编码数据（加载期间使用）
  const imageMap: Record<string, string> = {
    '1': '/images/1/gallery/202510/1760618835892.jpg',  // 梅花山
    '3': '/images/3/gallery/202510/1760619222708.jpg',  // 乌蒙大草原
    '2': '/images/2/gallery/202510/1760618856858.jpg',  // 玉舍
    '5': '/images/5/gallery/202510/1760619288590.jpg',  // 明湖
    '4': '/images/4/gallery/202510/1760619249739.jpg'   // 水城古镇
  }
  return getImageUrl(imageMap[scenicId] || '/images/1/gallery/202510/1760618835892.jpg')
}

// 获取热力图背景图像（使用缓存的景区图片）
const getScenicMapImage = (scenicId: string): string => {
  // 优先使用缓存的景区图片
  const cachedData = scenicDataCache.value[scenicId]
  if (cachedData?.imageUrl) {
    return getImageUrl(cachedData.imageUrl)
  }
  // 降级使用默认图片
  return getImageUrl('/images/placeholder.jpg')
}

// 获取景区状态（从currentScenic获取，不再硬编码）
const getScenicStatusForCard = (scenicId: string): string => {
  // 如果是当前选中的景区，返回其状态
  if (currentScenic.value && currentScenic.value.id === scenicId) {
    return currentScenic.value.status || ''
  }
  return ''
}

// getMockScenicData 已移除，现在完全使用后端API数据

// 刷新景区数据
const refreshScenicData = async () => {
  if (!selectedScenic.value) {
    ElMessage.warning('请先选择景区')
    return
  }
  
  ElMessage.success('景区数据刷新中...')
  await changeScenic(selectedScenic.value)
}

// 加载景区设施
const loadFacilities = async () => {
  if (!selectedScenic.value) {
    facilities.value = []
    return
  }
  
  try {
    const scenicId = scenicNameToId[selectedScenic.value]
    const res: any = await getScenicFacilities({
      scenicId,
      category: facilityCategory.value || undefined,
      page: facilityPage.value,
      size: facilitySize.value
    })
    
    const data = res?.data || res
    facilities.value = data.facilities || []
    hasMoreFacilities.value = (data.total || 0) > (facilityPage.value * facilitySize.value)
  } catch (error) {
    facilities.value = []
  }
}

// 加载更多设施
const loadMoreFacilities = async () => {
  facilityPage.value++
  try {
    const scenicId = scenicNameToId[selectedScenic.value]
    const res: any = await getScenicFacilities({
      scenicId,
      category: facilityCategory.value || undefined,
      page: facilityPage.value,
      size: facilitySize.value
    })
    
    const data = res?.data || res
    facilities.value.push(...(data.facilities || []))
    hasMoreFacilities.value = (data.total || 0) > (facilityPage.value * facilitySize.value)
  } catch (error) {
    console.error('加载更多设施失败:', error)
  }
}

// 加载活动列表
const loadActivities = async () => {
  console.log('[活动] 开始加载活动, selectedScenic:', selectedScenic.value)
  
  if (!selectedScenic.value) {
    console.warn('[活动] selectedScenic为空，无法加载活动')
    activities.value = []
    return
  }
  
  try {
    const scenicId = scenicNameToId[selectedScenic.value]
    console.log('[活动] 景区ID映射:', selectedScenic.value, '->', scenicId)
    
    if (!scenicId) {
      console.error('[活动] 无法获取景区ID')
      activities.value = []
      return
    }
    
    console.log('[活动] 开始调用API: getOngoingActivities, scenicId=', scenicId, 'page=', activityPage.value, 'size=', activitySize.value)
    const res: any = await getOngoingActivities({
      scenicId,
      page: activityPage.value,
      size: activitySize.value
    })
    console.log('[活动] API返回原始数据:', res)
    
    const data = res?.data || res
    console.log('[活动] 解析后的数据:', data)
    
    activities.value = data.activities || []
    console.log('[活动] 解析后的活动数据:', activities.value)
    console.log('[活动] 活动数量:', activities.value.length)
  } catch (error) {
    console.error('[活动] 加载活动失败:', error)
    activities.value = []
  }
}

// 加载公告列表
const loadAnnouncements = async () => {
  console.log('[公告] 开始加载公告, selectedScenic:', selectedScenic.value)
  
  if (!selectedScenic.value) {
    console.warn('[公告] selectedScenic为空，无法加载公告')
    announcements.value = []
    return
  }
  
  try {
    const scenicId = scenicNameToId[selectedScenic.value]
    console.log('[公告] 景区ID映射:', selectedScenic.value, '->', scenicId)
    
    if (!scenicId) {
      console.error('[公告] 无法获取景区ID')
      announcements.value = []
      return
    }
    
    console.log('[公告] 开始调用API: getScenicAnnouncements, scenicId=', scenicId)
    const res: any = await getScenicAnnouncements({ scenicId })
    console.log('[公告] API返回数据:', res)
    
    const data = res?.data || res
    announcements.value = data.announcements || []
    
    console.log('[公告] 解析后的公告数据:', announcements.value)
    console.log('[公告] 公告数量:', announcements.value.length)
  } catch (error) {
    console.error('[公告] 加载公告失败:', error)
    announcements.value = []
  }
}

// 加载系统通知
const loadSystemNotifications = async () => {
  try {
    const res: any = await getUserNotifications()
    if (res.code === 200) {
      systemNotifications.value = res.data || []
      console.log('[系统通知] 加载成功:', systemNotifications.value.length, '条')
    }
  } catch (error) {
    console.error('[系统通知] 加载失败:', error)
    systemNotifications.value = []
  }
}

// 点击系统通知
const handleSystemNotificationClick = async (notification: any) => {
  // 如果已读，直接返回
  if (notification.isRead) {
    return
  }
  
  // 如果未登录，提示登录
  const userStore = useUserStore()
  if (!userStore.isLoggedIn) {
    ElMessage.info('请先登录后标记已读')
    return
  }
  
  try {
    const res: any = await markUserNotificationAsRead(notification.id)
    if (res.code === 200) {
      // 更新本地状态
      notification.isRead = true
      console.log('[系统通知] 标记已读成功:', notification.id)
    } else if (res.code === 401) {
      ElMessage.warning('请先登录')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('[系统通知] 标记已读失败:', error)
    ElMessage.error('操作失败，请重试')
  }
}

// 格式化日期时间
const formatDate = (dateStr: string | Date) => {
  if (!dateStr) return '未知时间'
  try {
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  } catch (e) {
    console.error('日期格式化失败:', e)
    return String(dateStr)
  }
}

// 显示设施详情
const showFacilityDetail = (facility: any) => {
  selectedFacility.value = facility
  facilityDetailVisible.value = true
}

// 获取状态样式类
const getStatusClass = (status: string) => {
  if (status === 'normal' || status === 'active') return 'status-normal'
  if (status === 'maintenance') return 'status-maintenance'
  return 'status-closed'
}

// 获取设施状态文本
const getFacilityStatusText = (status: string) => {
  if (status === 'normal' || status === 'active') return '✅ 正常运营'
  if (status === 'maintenance') return '🔧 维护中'
  return '⛔ 暂停服务'
}

// 获取设施图标
const getFacilityIcon = (category: string) => {
  const iconMap: Record<string, any> = {
    '停车场': Van,
    '餐厅': ShoppingCart,
    '卫生间': HelpFilled,
    '服务中心': HomeFilled,
    '医疗点': FirstAidKit,
    '其他': OfficeBuilding
  }
  return iconMap[category] || OfficeBuilding
}

// 注意：景区切换时的数据加载已在 changeScenic 函数中处理
// 此 watch 仅用于重置分页状态和加载公告（公告不在 changeScenic 中加载）
watch(selectedScenic, (newVal, oldVal) => {
  if (newVal && newVal !== oldVal) {
    facilityPage.value = 1
    // 只加载公告，设施和活动已在 changeScenic 中加载
    loadAnnouncements()
  }
})

// 清空对话
const clearChat = () => {
  chatMessages.value = [{ type: 'ai', content: '你好！我是游韵华章旅游助手。请问有什么可以帮助你的吗？' }]
  userMessage.value = ''
  scrollToBottom()
}

// 窗口大小改变处理函数
const handleResize = () => {
  visitorChart?.resize()
}

// 获取景区地图图片URL（使用缓存的景区图片）
const getMapImageUrl = () => {
  return getScenicMapImage(selectedScenic.value)
};

// 获取热力图图片URL
const getHeatmapImageUrl = () => {
  switch (selectedScenic.value) {
    case '1': // 梅花山
      return 'https://mdn.alipayobjects.com/huamei_qa8qxu/afts/img/A*2VvTSZmI4PMAAAAAAAAAAAAADmJ7AQ/original';
    case '3': // 乌蒙大草原
      return 'https://mdn.alipayobjects.com/huamei_qa8qxu/afts/img/A*_aqoS73Se3sAAAAAAAAAAAAADmJ7AQ/original';
    case '2': // 玉舍
      return 'https://mdn.alipayobjects.com/huamei_qa8qxu/afts/img/A*JBCXT7lIpqUAAAAAAAAAAAAADmJ7AQ/original';
    case '5': // 明湖
      return 'https://mdn.alipayobjects.com/huamei_qa8qxu/afts/img/A*8GF6TLiwRg4AAAAAAAAAAAAADmJ7AQ/original';
    case '4': // 水城古镇
      return 'https://mdn.alipayobjects.com/huamei_qa8qxu/afts/img/A*DKHxSaHipK8AAAAAAAAAAAAADmJ7AQ/original';
    default:
      return 'https://mdn.alipayobjects.com/huamei_qa8qxu/afts/img/A*2VvTSZmI4PMAAAAAAAAAAAAADmJ7AQ/original';
  }
};

// 获取音频URL
const getAudioUrl = (spotId: string, language: string): string => {
  // 在实际项目中，这里应该返回真实的音频文件URL
  // 由于我们没有实际的音频文件，使用一些公共的示例音频
  
  // 使用一些示例音频URL
  const audioSamples = {
    zh: {
      'mh1': 'https://interactive-examples.mdn.mozilla.net/media/cc0-audio/t-rex-roar.mp3',
      'mh2': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',
      'mh3': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',
      'mh4': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3',
      'wm1': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3',
      'wm2': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3',
      'wm3': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3',
      'wm4': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3',
      'ys1': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3',
      'ys2': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3',
      'ys3': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3',
      'sc1': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3',
      'sc2': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3',
      'sc3': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3',
      'sc4': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3'
    },
    en: {
      'mh1': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3',
      'mh2': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-16.mp3',
      'mh3': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',
      'mh4': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',
      'wm1': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3',
      'wm2': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3',
      'wm3': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3',
      'wm4': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3',
      'ys1': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3',
      'ys2': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3',
      'ys3': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3',
      'sc1': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3',
      'sc2': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3',
      'sc3': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3',
      'sc4': 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3'
    }
  };
  
  // 返回对应的音频URL，如果没有找到则返回默认音频
  try {
    return audioSamples[language as keyof typeof audioSamples][spotId as keyof (typeof audioSamples)['zh']] || 
           'https://interactive-examples.mdn.mozilla.net/media/cc0-audio/t-rex-roar.mp3';
  } catch (error) {
    return 'https://interactive-examples.mdn.mozilla.net/media/cc0-audio/t-rex-roar.mp3';
  }
}

// 上传视频
const uploadVideo = () => {
  // 打开视频上传指南
  window.open('/videos/README.html', '_blank')
  ElMessage.info('已打开视频上传指南，请按照指南上传视频文件')
}
// 紧急电话
const callEmergency = () => {
  window.open('tel:120')
}
</script>

<style scoped>
.real-time-service-container {
  @apply p-6 bg-[#F5F7FA] min-h-[calc(100vh-64px)];
}

.selector-section {
  border-bottom: 1px solid #f0f0f0;
}

.section-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(42, 157, 143, 0.2), transparent);
  width: 100%;
}

:deep(.scenic-selector) {
  position: relative;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 12px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #e5e7eb;
}

:deep(.scenic-selector:hover) {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  border-color: #2A9D8F;
}

:deep(.scenic-selector.active) {
  border: 2px solid #2A9D8F;
  background: #E0F2F1;
}

:deep(.scenic-selector.active::after) {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom right, rgba(42, 157, 143, 0.05), rgba(42, 157, 143, 0.1));
  pointer-events: none;
}

:deep(.scenic-selector:hover::before) {
  opacity: 1;
}

/* 统计卡片美化 */
.stat-card {
  padding: 20px;
  border-radius: 16px;
  border: 1px solid rgba(54, 219, 255, 0.15);
  background: linear-gradient(145deg, rgba(30, 41, 59, 0.6), rgba(15, 23, 42, 0.8));
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #2A9D8F, #457B9D, #E9C46A);
  transform: scaleX(0);
  transition: transform 0.3s;
}

.stat-card:hover {
  border-color: rgba(42, 157, 143, 0.3);
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(42, 157, 143, 0.1);
}

.stat-card:hover::before {
  transform: scaleX(1);
}

/* 活动卡片美化 */
.activity-item {
  background: #ffffff;
  border: 1px solid #e4e7ed !important;
  border-radius: 16px !important;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.activity-item::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #2A9D8F, #67C23A);
  transform: scaleX(0);
  transition: transform 0.3s;
}

.activity-item:hover {
  border-color: rgba(42, 157, 143, 0.3) !important;
  transform: translateX(8px);
  box-shadow: 0 4px 15px rgba(42, 157, 143, 0.08);
}

.activity-item:hover::after {
  transform: scaleX(1);
}

/* 设施卡片美化 */
.facility-item {
  background: #ffffff;
  border: 1px solid #e4e7ed !important;
  border-radius: 16px !important;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.facility-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, #2A9D8F, #457B9D);
  transform: scaleY(0);
  transition: transform 0.3s;
}

.facility-item:hover {
  border-color: rgba(42, 157, 143, 0.3) !important;
  transform: translateX(6px);
  box-shadow: 0 4px 15px rgba(42, 157, 143, 0.08);
}

.facility-item:hover::before {
  transform: scaleY(1);
}

/* 快捷服务卡片美化 */
.quick-service-card {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #e4e7ed;
}

.quick-service-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 8px 20px rgba(42, 157, 143, 0.12);
  border-color: rgba(42, 157, 143, 0.4);
}

.quick-service-card::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(to right, #2A9D8F, #457B9D, #E9C46A);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s ease;
}

.quick-service-card:hover::after {
  transform: scaleX(1);
}

/* 购票成功对话框样式 */
.ticket-success-content {
  @apply text-center;
}

.success-icon {
  @apply mb-4;
}

.order-info {
  background: #f8fafc;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;
  text-align: left;
  border: 1px solid #e4e7ed;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid rgba(54, 219, 255, 0.1);
}

.info-row:last-child {
  border-bottom: none;
}

.info-row .label {
  color: #909399;
}

.info-row .value {
  color: #303133;
  font-weight: 500;
}

.info-row .value.price {
  color: #2A9D8F;
  font-size: 1.25rem;
  font-weight: 600;
}

.barcode-section {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
}

.barcode-title {
  color: #1e293b;
  font-weight: 600;
  margin-bottom: 16px;
}

.barcode-container {
  display: flex;
  justify-content: center;
  margin-bottom: 12px;
}

.barcode-canvas {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.barcode-tip {
  color: #64748b;
  font-size: 13px;
}

/* 我的订单对话框样式 */
.orders-container {
  max-height: 500px;
  overflow-y: auto;
}

.empty-orders {
  padding: 40px 0;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-item {
  background: linear-gradient(145deg, rgba(30, 41, 59, 0.6), rgba(15, 23, 42, 0.8));
  border-radius: 16px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(54, 219, 255, 0.15);
}

.order-item:hover {
  border-color: rgba(54, 219, 255, 0.4);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(54, 219, 255, 0.15);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(54, 219, 255, 0.1);
}

.order-no {
  color: #64748b;
  font-size: 13px;
}

.order-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.order-info-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.order-info-row .label {
  color: #909399;
}

.order-info-row .value {
  color: #303133;
}

.order-info-row .value.price {
  color: #2A9D8F;
  font-weight: 600;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #e4e7ed;
}

.order-time {
  color: #909399;
  font-size: 12px;
}

/* 票据详情样式 */
.ticket-detail-content {
  text-align: center;
}

/* AI聊天容器滚动条 */
.ai-chat-container::-webkit-scrollbar {
  width: 6px;
}

.ai-chat-container::-webkit-scrollbar-track {
  background: rgba(30, 41, 59, 0.5);
  border-radius: 3px;
}

.ai-chat-container::-webkit-scrollbar-thumb {
  background: rgba(54, 219, 255, 0.3);
  border-radius: 3px;
}

.ai-chat-container::-webkit-scrollbar-thumb:hover {
  background: rgba(54, 219, 255, 0.5);
}

/* 打字动画 */
.typing-indicator {
  display: inline-flex;
  align-items: center;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  margin: 0 2px;
  background-color: #2A9D8F;
  border-radius: 50%;
  display: inline-block;
  animation: typing-dot 1.4s infinite ease-in-out both;
}

.typing-indicator span:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing-dot {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* 景区选择器 */
.scenic-selector {
  position: relative;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 16px;
  overflow: hidden;
}

.scenic-selector:hover {
  transform: translateY(-6px) scale(1.02);
  box-shadow: 0 8px 20px rgba(42, 157, 143, 0.15);
}

.scenic-selector.active {
  border: 2px solid #2A9D8F;
}

/* Markdown 渲染样式 */
.ai-md {
  line-height: 1.7;
}

.ai-md :deep(h1),
.ai-md :deep(h2),
.ai-md :deep(h3),
.ai-md :deep(h4),
.ai-md :deep(h5),
.ai-md :deep(h6) {
  margin-top: 0.5em;
  margin-bottom: 0.5em;
  font-weight: bold;
  color: #fff;
}

.ai-md :deep(h1) { font-size: 1.5em; }
.ai-md :deep(h2) { font-size: 1.3em; }
.ai-md :deep(h3) { font-size: 1.1em; }

.ai-md :deep(p) {
  margin: 0.5em 0;
}

.ai-md :deep(strong),
.ai-md :deep(b) {
  font-weight: bold;
  color: #fff;
}

.ai-md :deep(em),
.ai-md :deep(i) {
  font-style: italic;
}

.ai-md :deep(ul),
.ai-md :deep(ol) {
  margin: 0.5em 0;
  padding-left: 1.5em;
}

.ai-md :deep(li) {
  margin: 0.25em 0;
}

.ai-md :deep(code) {
  background-color: rgba(42, 157, 143, 0.1);
  padding: 0.2em 0.5em;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
  color: #2A9D8F;
}

.ai-md :deep(pre) {
  background-color: #f8fafc;
  padding: 1em;
  border-radius: 12px;
  overflow-x: auto;
  margin: 0.5em 0;
  border: 1px solid #e4e7ed;
}

.ai-md :deep(pre code) {
  background-color: transparent;
  padding: 0;
  color: #303133;
}

.ai-md :deep(blockquote) {
  border-left: 3px solid #2A9D8F;
  padding-left: 1em;
  margin: 0.5em 0;
  color: #606266;
  background: rgba(42, 157, 143, 0.05);
  padding: 0.5em 1em;
  border-radius: 0 8px 8px 0;
}

.ai-md :deep(a) {
  color: #2A9D8F;
  text-decoration: none;
  border-bottom: 1px dashed rgba(42, 157, 143, 0.5);
  transition: all 0.2s;
}

.ai-md :deep(a:hover) {
  color: #457B9D;
  border-bottom-color: #457B9D;
}

.ai-md :deep(hr) {
  border: none;
  border-top: 1px solid #e4e7ed;
  margin: 1em 0;
}

.ai-md :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 0.5em 0;
  border-radius: 8px;
  overflow: hidden;
}

.ai-md :deep(th),
.ai-md :deep(td) {
  border: 1px solid #e4e7ed;
  padding: 0.75em;
  text-align: left;
}

.ai-md :deep(th) {
  background-color: rgba(42, 157, 143, 0.08);
  font-weight: bold;
  color: #2A9D8F;
}

/* 全局对话框美化 */
:deep(.el-dialog) {
  background: #ffffff !important;
  border: 1px solid #E5E7EB;
  border-radius: 20px !important;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
}

:deep(.el-dialog__header) {
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  padding: 20px 24px !important;
}

:deep(.el-dialog__title) {
  color: #303133 !important;
  font-weight: 600;
  font-size: 18px;
}

:deep(.el-dialog__body) {
  padding: 24px !important;
  color: #606266;
}

:deep(.el-dialog__footer) {
  border-top: 1px solid #e4e7ed;
  padding: 16px 24px !important;
}

/* 表单美化 */
:deep(.el-form-item__label) {
  color: #606266 !important;
}

:deep(.el-input__wrapper) {
  background: #ffffff !important;
  border: 1px solid #DCDFE6 !important;
  border-radius: 10px !important;
  box-shadow: none !important;
}

:deep(.el-input__wrapper:hover) {
  border-color: rgba(42, 157, 143, 0.4) !important;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #2A9D8F !important;
  box-shadow: 0 0 0 2px rgba(42, 157, 143, 0.1) !important;
}

:deep(.el-input__inner) {
  color: #303133 !important;
}

:deep(.el-textarea__inner) {
  background: #ffffff !important;
  border: 1px solid #DCDFE6 !important;
  border-radius: 10px !important;
  color: #303133 !important;
}

:deep(.el-textarea__inner:focus) {
  border-color: #2A9D8F !important;
  box-shadow: 0 0 0 2px rgba(42, 157, 143, 0.1) !important;
}

:deep(.el-select .el-input__wrapper) {
  background: #ffffff !important;
}

/* 分割线美化 */
:deep(.el-divider) {
  border-color: #e4e7ed !important;
}

:deep(.el-divider__text) {
  background: #ffffff !important;
  color: #909399 !important;
}

/* 进度条美化 */
:deep(.el-progress-bar__outer) {
  background: #f0f2f5 !important;
  border-radius: 10px !important;
}

:deep(.el-progress-bar__inner) {
  border-radius: 10px !important;
}

/* 设施详情弹窗美化 */
.facility-detail-dialog :deep(.el-dialog) {
  background: #ffffff !important;
  backdrop-filter: blur(24px);
  border: 1px solid #E5E7EB;
  border-radius: 24px !important;
  box-shadow: 0 30px 100px rgba(0, 0, 0, 0.6), 0 0 60px rgba(54, 219, 255, 0.1);
  overflow: hidden;
}

.facility-detail-dialog :deep(.el-dialog__header) {
  padding: 0 !important;
  margin: 0;
}

.facility-detail-dialog :deep(.el-dialog__body) {
  padding: 0 !important;
}

.facility-detail-dialog :deep(.el-dialog__footer) {
  padding: 0 !important;
  border: none;
}

.facility-dialog-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: linear-gradient(180deg, rgba(54, 219, 255, 0.1), transparent);
  border-bottom: 1px solid rgba(54, 219, 255, 0.15);
}

.header-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: #E0F2F1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2A9D8F;
  border: 1px solid rgba(42, 157, 143, 0.2);
}

.header-content {
  flex: 1;
}

.header-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}

.header-subtitle {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.close-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: #ef4444;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.close-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.4);
  transform: rotate(90deg);
}

.facility-detail-content {
  padding: 24px;
}

.facility-image-wrapper {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 24px;
}

.facility-image {
  width: 100%;
  height: 220px;
  object-fit: cover;
  display: block;
}

.image-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 50%, rgba(15, 23, 42, 0.8) 100%);
  pointer-events: none;
}

.image-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  padding: 8px 14px;
  background: linear-gradient(135deg, rgba(54, 219, 255, 0.9), rgba(159, 135, 255, 0.9));
  border-radius: 20px;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  backdrop-filter: blur(8px);
  box-shadow: 0 4px 15px rgba(54, 219, 255, 0.3);
}

.facility-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  background: linear-gradient(145deg, rgba(30, 41, 59, 0.5), rgba(15, 23, 42, 0.7));
  border-radius: 14px;
  border: 1px solid rgba(54, 219, 255, 0.1);
  transition: all 0.3s;
}

.info-card:hover {
  border-color: rgba(54, 219, 255, 0.25);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(54, 219, 255, 0.1);
}

.info-card.full-width {
  grid-column: span 2;
}

.info-card.description-card {
  background: linear-gradient(145deg, rgba(54, 219, 255, 0.05), rgba(159, 135, 255, 0.05));
}

.info-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.info-icon.name-icon {
  background: rgba(42, 157, 143, 0.1);
  color: #2A9D8F;
}

.info-icon.desc-icon {
  background: rgba(69, 123, 157, 0.1);
  color: #457B9D;
}

.info-icon.time-icon {
  background: rgba(230, 162, 60, 0.1);
  color: #E6A23C;
}

.info-icon.location-icon {
  background: rgba(103, 194, 58, 0.1);
  color: #67C23A;
}

.info-icon.capacity-icon {
  background: rgba(64, 158, 255, 0.1);
  color: #409EFF;
}

.info-icon.price-icon {
  background: rgba(233, 196, 106, 0.1);
  color: #E9C46A;
}

.info-icon.status-icon {
  background: rgba(103, 194, 58, 0.1);
  color: #67C23A;
}

.info-icon.status-icon.status-maintenance {
  background: rgba(230, 162, 60, 0.1);
  color: #E6A23C;
}

.info-icon.status-icon.status-closed {
  background: rgba(245, 108, 108, 0.1);
  color: #F56C6C;
}

.info-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #909399;
}

.info-value {
  font-size: 15px;
  color: #303133;
  font-weight: 500;
}

.info-value.highlight {
  font-size: 18px;
  color: #303133;
  font-weight: 600;
}

.info-value.free-tag {
  color: #4ade80;
}

.info-description {
  font-size: 14px;
  color: #94a3b8;
  line-height: 1.6;
  margin: 0;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.status-badge.status-normal {
  background: linear-gradient(135deg, rgba(74, 222, 128, 0.2), rgba(74, 222, 128, 0.1));
  color: #4ade80;
  border: 1px solid rgba(74, 222, 128, 0.3);
}

.status-badge.status-maintenance {
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.2), rgba(251, 191, 36, 0.1));
  color: #fbbf24;
  border: 1px solid rgba(251, 191, 36, 0.3);
}

.status-badge.status-closed {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.2), rgba(239, 68, 68, 0.1));
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.facility-dialog-footer {
  padding: 20px 24px;
  display: flex;
  justify-content: center;
  border-top: 1px solid #e4e7ed;
}

.close-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 32px;
  background: rgba(42, 157, 143, 0.1);
  border: 1px solid rgba(42, 157, 143, 0.3);
  border-radius: 25px;
  color: #2A9D8F;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.close-button:hover {
  background: rgba(42, 157, 143, 0.15);
  border-color: rgba(42, 157, 143, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(42, 157, 143, 0.15);
}

/* 实时客流图表容器美化 */
.visitor-chart-container {
  position: relative;
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e4e7ed;
  padding: 4px;
}

.visitor-chart-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #2A9D8F, #457B9D, #E9C46A, transparent);
  border-radius: 16px 16px 0 0;
  opacity: 0.6;
}

/* ========== 购票弹窗美化 ========== */
.ticket-dialog :deep(.el-dialog) {
  background: #ffffff !important;
  border-radius: 24px !important;
  overflow: hidden;
}

.ticket-dialog :deep(.el-dialog__header) {
  padding: 0 !important;
  margin: 0;
}

.ticket-dialog :deep(.el-dialog__body) {
  padding: 0 !important;
}

.ticket-dialog :deep(.el-dialog__footer) {
  padding: 0 !important;
  border: none;
}

.ticket-dialog-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: linear-gradient(180deg, rgba(54, 219, 255, 0.1), transparent);
  border-bottom: 1px solid rgba(54, 219, 255, 0.15);
}

.header-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(54, 219, 255, 0.2), rgba(159, 135, 255, 0.2));
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(54, 219, 255, 0.3);
}

.header-icon {
  font-size: 24px;
  color: #2A9D8F;
}

.header-text {
  flex: 1;
}

.header-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}

.header-subtitle {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.ticket-dialog-content {
  padding: 24px;
}

/* 景区信息卡片 */
.scenic-info-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 16px;
  border: 1px solid #e4e7ed;
  margin-bottom: 24px;
}

.scenic-image-wrapper {
  position: relative;
  width: 100px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
}

.scenic-image {
  width: 100%;
  height: 100%;
}

.scenic-image-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 50%, rgba(0, 0, 0, 0.3) 100%);
}

.scenic-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.scenic-name {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 4px;
}

.scenic-desc {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.scenic-tags {
  display: flex;
  gap: 8px;
}

.scenic-tags .tag {
  font-size: 11px;
  padding: 2px 8px;
  background: rgba(42, 157, 143, 0.1);
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  color: #2A9D8F;
}

/* 表单区块 */
.form-section {
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 14px;
}

.section-title .el-icon {
  color: #2A9D8F;
}

/* 票种选择 */
.ticket-type-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.ticket-type-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #ffffff;
  border-radius: 14px;
  border: 2px solid #e4e7ed;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.ticket-type-card:hover {
  border-color: rgba(42, 157, 143, 0.3);
  transform: translateY(-2px);
}

.ticket-type-card.active {
  border-color: #2A9D8F;
  background: rgba(42, 157, 143, 0.05);
  box-shadow: 0 4px 15px rgba(42, 157, 143, 0.1);
}

.ticket-type-icon {
  font-size: 28px;
}

.ticket-type-info {
  flex: 1;
}

.ticket-type-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 2px;
}

.ticket-type-price {
  font-size: 18px;
  font-weight: 700;
  color: #2A9D8F;
}

.ticket-type-price .discount {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 6px;
  background: linear-gradient(135deg, #F56C6C, #E76F51);
  border-radius: 8px;
  color: #fff;
  margin-left: 6px;
}

.ticket-type-check {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #2A9D8F;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
}

/* 表单网格 */
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 13px;
  color: #909399;
}

/* 数量选择器 */
.quantity-selector {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 16px;
  background: #ffffff;
  border: 1px solid #DCDFE6;
  border-radius: 12px;
}

.qty-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(42, 157, 143, 0.1);
  border: 1px solid #e4e7ed;
  color: #2A9D8F;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.qty-btn:hover {
  background: rgba(42, 157, 143, 0.15);
  border-color: rgba(42, 157, 143, 0.4);
}

.qty-value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  min-width: 40px;
  text-align: center;
}

/* 价格汇总 */
.price-summary {
  background: rgba(42, 157, 143, 0.05);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(42, 157, 143, 0.15);
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.price-row.total {
  border-top: 1px dashed rgba(42, 157, 143, 0.2);
  margin-top: 8px;
  padding-top: 16px;
}

.price-label {
  font-size: 14px;
  color: #909399;
}

.price-value {
  font-size: 15px;
  color: #303133;
}

.price-total {
  font-size: 28px;
  font-weight: 700;
  color: #2A9D8F;
}

/* 弹窗底部 */
.ticket-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #e4e7ed;
}

.cancel-btn {
  padding: 12px 24px;
  background: #f0f2f5;
  border: 1px solid #DCDFE6;
  border-radius: 12px;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-btn:hover {
  background: #e4e7ed;
  color: #303133;
}

.submit-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  background: #2A9D8F;
  border: none;
  border-radius: 12px;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(42, 157, 143, 0.25);
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(42, 157, 143, 0.35);
}

/* ========== 紧急救援弹窗美化 ========== */
.emergency-dialog :deep(.el-dialog) {
  background: #ffffff !important;
  border-radius: 24px !important;
  overflow: hidden;
  border: 1px solid rgba(248, 113, 113, 0.2);
}

.emergency-dialog :deep(.el-dialog__header) {
  padding: 0 !important;
  margin: 0;
}

.emergency-dialog :deep(.el-dialog__body) {
  padding: 0 !important;
}

.emergency-dialog :deep(.el-dialog__footer) {
  padding: 0 !important;
  border: none;
}

.emergency-dialog-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: linear-gradient(180deg, rgba(248, 113, 113, 0.1), transparent);
  border-bottom: 1px solid rgba(248, 113, 113, 0.15);
}

.emergency-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(248, 113, 113, 0.2), rgba(239, 68, 68, 0.2));
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(248, 113, 113, 0.3);
  animation: emergency-pulse 2s ease-in-out infinite;
}

@keyframes emergency-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(248, 113, 113, 0.4); }
  50% { box-shadow: 0 0 0 10px rgba(248, 113, 113, 0); }
}

.emergency-icon {
  font-size: 24px;
  color: #F87171;
}

.emergency-title {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 4px 0;
}

.emergency-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.emergency-dialog-content {
  padding: 24px;
}

/* 救援类型选择 */
.rescue-type-section {
  margin-bottom: 24px;
}

.section-label {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 12px;
}

.rescue-type-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.rescue-type-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  background: linear-gradient(145deg, rgba(30, 41, 59, 0.5), rgba(15, 23, 42, 0.7));
  border-radius: 14px;
  border: 2px solid rgba(100, 116, 139, 0.2);
  cursor: pointer;
  transition: all 0.3s;
}

.rescue-type-card:hover {
  border-color: rgba(248, 113, 113, 0.3);
  transform: translateY(-2px);
}

.rescue-type-card.active {
  border-color: #F87171;
  background: linear-gradient(145deg, rgba(248, 113, 113, 0.1), rgba(15, 23, 42, 0.8));
  box-shadow: 0 0 20px rgba(248, 113, 113, 0.15);
}

.rescue-icon {
  font-size: 28px;
}

.rescue-name {
  font-size: 12px;
  color: #e2e8f0;
  text-align: center;
}

/* 紧急表单 */
.emergency-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 20px;
}

.custom-input :deep(.el-input__wrapper),
.custom-textarea :deep(.el-textarea__inner) {
  background: rgba(30, 41, 59, 0.6) !important;
  border: 1px solid rgba(248, 113, 113, 0.2) !important;
  border-radius: 12px !important;
}

.custom-input :deep(.el-input__wrapper:hover),
.custom-textarea :deep(.el-textarea__inner:hover) {
  border-color: rgba(248, 113, 113, 0.4) !important;
}

.custom-input :deep(.el-input__wrapper.is-focus),
.custom-textarea :deep(.el-textarea__inner:focus) {
  border-color: #F87171 !important;
}

/* 紧急提示 */
.emergency-tip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  background: linear-gradient(145deg, rgba(248, 113, 113, 0.1), rgba(239, 68, 68, 0.05));
  border-radius: 12px;
  border: 1px solid rgba(248, 113, 113, 0.2);
  font-size: 13px;
  color: #F87171;
}

.emergency-tip strong {
  font-weight: 700;
  color: #fff;
}

/* 紧急弹窗底部 */
.emergency-dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-top: 1px solid rgba(248, 113, 113, 0.1);
}

.emergency-call-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  border: none;
  border-radius: 12px;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(239, 68, 68, 0.3);
}

.emergency-call-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(239, 68, 68, 0.4);
}

.action-btns {
  display: flex;
  gap: 12px;
}

/* ========== 景区选择卡片美化 ========== */
.scenic-selector-card {
  position: relative;
  overflow: hidden;
}

.scenic-selector-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #2A9D8F, #457B9D, #E9C46A);
}

.selector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.selector-header .header-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #E0F2F1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2A9D8F;
  font-size: 22px;
  border: 1px solid rgba(42, 157, 143, 0.2);
}

.selector-header .header-text h3 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}

.selector-header .header-text p {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  background: rgba(42, 157, 143, 0.1);
  border: 1px solid rgba(42, 157, 143, 0.2);
  border-radius: 10px;
  color: #2A9D8F;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
}

.refresh-btn:hover {
  background: rgba(42, 157, 143, 0.15);
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(42, 157, 143, 0.15);
}

.scenic-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

@media (max-width: 1200px) {
  .scenic-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 900px) {
  .scenic-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 600px) {
  .scenic-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.scenic-card {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  background: #ffffff;
  border: 2px solid #e4e7ed;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.scenic-card:hover {
  transform: translateY(-8px) scale(1.02);
  border-color: rgba(42, 157, 143, 0.4);
  box-shadow: 0 12px 24px rgba(42, 157, 143, 0.12);
}

.scenic-card.active {
  border-color: #2A9D8F;
  box-shadow: 0 8px 20px rgba(42, 157, 143, 0.2);
}

.scenic-image-container {
  position: relative;
  height: 100px;
  overflow: hidden;
}

.scenic-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.scenic-card:hover .scenic-img {
  transform: scale(1.1);
}

.scenic-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 30%, rgba(0, 0, 0, 0.5) 100%);
  pointer-events: none;
}

.scenic-glow {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(42, 157, 143, 0.15), transparent);
  animation: glow-pulse 2s ease-in-out infinite;
}

@keyframes glow-pulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

.scenic-content {
  padding: 12px;
}

.scenic-card .scenic-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.scenic-status-badge {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  animation: dot-pulse 2s ease-in-out infinite;
}

.status-dot.success {
  background: #2A9D8F;
  box-shadow: 0 0 8px rgba(42, 157, 143, 0.5);
}

.status-dot.warning {
  background: #E6A23C;
  box-shadow: 0 0 8px rgba(230, 162, 60, 0.5);
}

.status-dot.danger {
  background: #F56C6C;
  box-shadow: 0 0 8px rgba(245, 108, 108, 0.5);
}

.status-dot.info {
  background: #409EFF;
  box-shadow: 0 0 8px rgba(64, 158, 255, 0.5);
}

@keyframes dot-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}

.status-text {
  font-size: 11px;
  color: #606266;
}

.selected-indicator {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #2A9D8F;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
}

/* ========== 实时景区状态卡片美化 ========== */
/* Dark Theme Restoration */
.real-time-service-container {
  @apply py-4;
  background-color: #F5F7FA;
}

/* Merged Status Styling */
.status-section {
  flex: 1;
  min-height: 400px;
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-title-wrapper {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0; /* Prevent shrinking */
  min-width: 200px;
}

.title-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #E0F2F1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2A9D8F;
  font-size: 24px;
}

.title-content .status-title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 4px 0;
  flex-shrink: 0;
}

.title-content .status-subtitle {
  font-size: 13px;
  color: #606266;
  margin: 0;
}

.scenic-select :deep(.el-input__wrapper) {
  background: #ffffff !important;
  border-radius: 10px !important;
  border: 1px solid #dcdfe6;
  box-shadow: none !important;
}

/* 景区信息展示区 */
.scenic-info-section {
  display: flex;
  gap: 24px;
  padding: 24px;
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.scenic-main-image {
  position: relative;
  width: 220px;
  height: 160px;
  border-radius: 16px;
  overflow: hidden;
  flex-shrink: 0;
}

.main-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-overlay-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 50%, rgba(0, 0, 0, 0.6) 100%);
}

.image-badges {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.image-badges .badge {
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 500;
  backdrop-filter: blur(8px);
}

.level-badge {
  background: rgba(255, 255, 255, 0.9);
  color: #2A9D8F;
}

.hot-badge {
  background: rgba(255, 255, 255, 0.9);
  color: #F4A261;
}

.scenic-details {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.scenic-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 12px 0;
}

.scenic-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.status-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.status-tag.success {
  background: rgba(42, 157, 143, 0.1);
  border: 1px solid rgba(42, 157, 143, 0.3);
  color: #2A9D8F;
}

.status-tag.warning {
  background: rgba(230, 162, 60, 0.1);
  border: 1px solid rgba(230, 162, 60, 0.3);
  color: #E6A23C;
}

.status-tag.danger {
  background: rgba(245, 108, 108, 0.1);
  border: 1px solid rgba(245, 108, 108, 0.3);
  color: #F56C6C;
}

.status-tag.info {
  background: rgba(64, 158, 255, 0.1);
  border: 1px solid rgba(64, 158, 255, 0.3);
  color: #409EFF;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  animation: indicator-pulse 2s ease-in-out infinite;
}

@keyframes indicator-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.2); }
}

.update-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
}

.scenic-description {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 16px 0;
}

.quick-stats {
  display: flex;
  gap: 20px;
  margin-top: auto;
}

.quick-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 20px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.quick-stat .stat-icon {
  font-size: 20px;
  margin-bottom: 4px;
  color: #2A9D8F;
}

.quick-stat .stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.quick-stat .stat-label {
  font-size: 11px;
  color: #909399;
}

/* 智能建议卡片 */
.suggestion-card {
  display: flex;
  gap: 16px;
  padding: 20px;
  border-radius: 16px;
  border: 1px solid;
  transition: all 0.3s ease;
}

.suggestion-good {
  background: linear-gradient(145deg, rgba(42, 157, 143, 0.08), rgba(42, 157, 143, 0.03));
  border-color: rgba(42, 157, 143, 0.25);
}

.suggestion-warning {
  background: linear-gradient(145deg, rgba(230, 162, 60, 0.08), rgba(230, 162, 60, 0.03));
  border-color: rgba(230, 162, 60, 0.25);
}

.suggestion-danger {
  background: linear-gradient(145deg, rgba(245, 108, 108, 0.08), rgba(245, 108, 108, 0.03));
  border-color: rgba(245, 108, 108, 0.25);
}

.suggestion-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.suggestion-good .suggestion-icon {
  background: rgba(42, 157, 143, 0.15);
  color: #2A9D8F;
}

.suggestion-warning .suggestion-icon {
  background: rgba(230, 162, 60, 0.15);
  color: #E6A23C;
}

.suggestion-danger .suggestion-icon {
  background: rgba(245, 108, 108, 0.15);
  color: #F56C6C;
}

.suggestion-content {
  flex: 1;
}

.suggestion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.suggestion-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.trend-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.trend-up {
  background: rgba(245, 108, 108, 0.1);
  color: #F56C6C;
}

.trend-down {
  background: rgba(42, 157, 143, 0.1);
  color: #2A9D8F;
}

.trend-stable {
  background: rgba(64, 158, 255, 0.1);
  color: #409EFF;
}

.trend-icon {
  font-size: 14px;
}

.suggestion-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 10px 0;
}

.prediction-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
}

.prediction-info strong {
  color: #2A9D8F;
}

/* 数据统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}

.stat-card-enhanced {
  display: flex;
  gap: 16px;
  padding: 20px;
  background: #ffffff;
  border-radius: 18px;
  border: 1px solid #e4e7ed;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.stat-card-enhanced::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--stat-color, #2A9D8F), transparent);
  opacity: 0;
  transition: opacity 0.3s;
}

.stat-card-enhanced:hover {
  transform: translateY(-4px);
  border-color: var(--stat-color, #2A9D8F);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}

.stat-card-enhanced:hover::before {
  opacity: 1;
}

.visitor-stat { --stat-color: #2A9D8F; }
.wait-stat { --stat-color: #457B9D; }
.weather-stat { --stat-color: #E9C46A; }

.stat-icon-wrapper {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.visitor-stat .stat-icon-wrapper {
  background: #E0F2F1;
  color: #2A9D8F;
}

.wait-stat .stat-icon-wrapper {
  background: #E3F2FD;
  color: #457B9D;
}

.weather-stat .stat-icon-wrapper {
  background: #FCE4EC;
}

.weather-emoji {
  font-size: 28px;
}

.stat-info {
  flex: 1;
}

.stat-info .stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.stat-value-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 10px;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.stat-unit {
  font-size: 14px;
  color: #909399;
}

/* 进度条 */
.stat-progress {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: #f0f2f5;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.progress-fill.progress-low {
  background: linear-gradient(90deg, #4ade80, #22c55e);
}

.progress-fill.progress-medium {
  background: linear-gradient(90deg, #fbbf24, #f59e0b);
}

.progress-fill.progress-high {
  background: linear-gradient(90deg, #ef4444, #dc2626);
}

.progress-text {
  font-size: 12px;
  color: #64748b;
  min-width: 36px;
}

/* 等待指示器 */
.wait-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.wait-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.wait-dot.fast {
  background: #4ade80;
  box-shadow: 0 0 10px rgba(74, 222, 128, 0.5);
}

.wait-dot.normal {
  background: #fbbf24;
  box-shadow: 0 0 10px rgba(251, 191, 36, 0.5);
}

.wait-dot.slow {
  background: #ef4444;
  box-shadow: 0 0 10px rgba(239, 68, 68, 0.5);
}

.wait-text {
  font-size: 12px;
  color: #94a3b8;
}

.weather-condition {
  font-size: 13px;
  color: #94a3b8;
}

/* Quick Service Section Styles */
.quick-services-section {
  background: white;
  border-radius: 12px;
}

.service-btn-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 4px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #F5F7FA;
  border: 1px solid transparent;
}

.service-btn-item:hover {
  background: #E0F2F1;
  transform: translateY(-2px);
}

.service-btn-item.warning:hover {
  background: #FEF2F2;
}

.service-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2A9D8F;
  font-size: 20px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
}

.service-btn-item:hover .service-icon-box {
  background: #ffffff;
  color: #2A9D8F;
  box-shadow: 0 4px 10px rgba(42, 157, 143, 0.2);
}

.service-btn-item.warning .service-icon-box {
  color: #F56C6C;
}

.service-btn-item.warning:hover .service-icon-box {
  box-shadow: 0 4px 10px rgba(245, 108, 108, 0.2);
}

.service-btn-item span {
  font-size: 12px;
  color: #606266;
  font-weight: 500;
}

/* 快捷服务卡片样式 - 柔和浅色系 */
.quick-service-card {
  position: relative;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  min-height: 80px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.quick-service-card:hover {
  border-color: #2A9D8F;
  box-shadow: 0 4px 12px rgba(42, 157, 143, 0.15);
  transform: translateY(-2px);
}

.quick-service-card:first-child {
  border-color: #2A9D8F;
  background: linear-gradient(135deg, #f0fdf9 0%, #ffffff 100%);
}

.service-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
}

.service-badge.hot {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
  color: white;
}

.service-card-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.service-card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.service-card-desc {
  font-size: 12px;
  color: #9ca3af;
}
</style>

<!-- 全局样式覆盖 Element Plus 下拉选择器 -->
<style>
/* 下拉选择器选中项样式 */
.el-select-dropdown__item.is-selected {
  background-color: #f0fdf9 !important;
  color: #2A9D8F !important;
  font-weight: 600;
}

.el-select-dropdown__item.is-selected::after {
  display: none;
}

.el-select-dropdown__item:hover {
  background-color: #f5f7fa !important;
}

/* 下拉框整体样式 */
.el-select-dropdown {
  border: 1px solid #e5e7eb !important;
  border-radius: 8px !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1) !important;
}

/* 输入框样式 */
.el-select .el-input__wrapper {
  background-color: white !important;
  border-radius: 8px !important;
}

.el-select .el-input__wrapper.is-focus {
  box-shadow: 0 0 0 2px rgba(42, 157, 143, 0.2) !important;
}

/* 日期选择器弹窗样式 */
.el-date-picker {
  background: white !important;
  border: 1px solid #e5e7eb !important;
  border-radius: 12px !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12) !important;
}

.el-date-picker__header {
  background: white !important;
}

.el-date-picker__header-label {
  color: #1f2937 !important;
  font-weight: 600 !important;
}

.el-picker-panel__icon-btn {
  color: #6b7280 !important;
}

.el-picker-panel__icon-btn:hover {
  color: #2A9D8F !important;
}

.el-date-table th {
  color: #9ca3af !important;
  background: #f9fafb !important;
}

.el-date-table td {
  color: #374151 !important;
}

.el-date-table td.today .el-date-table-cell__text {
  color: #2A9D8F !important;
  font-weight: 600;
}

.el-date-table td.current .el-date-table-cell__text {
  background: #2A9D8F !important;
  color: white !important;
}

.el-date-table td.available:hover .el-date-table-cell__text {
  background: #f0fdf9 !important;
  color: #2A9D8F !important;
}

/* 对话框样式 */
.el-dialog {
  background: white !important;
  border-radius: 16px !important;
}

.el-dialog__header {
  background: white !important;
  border-bottom: 1px solid #f3f4f6 !important;
}

.el-dialog__title {
  color: #1f2937 !important;
  font-weight: 600 !important;
}

.el-dialog__body {
  background: white !important;
  color: #374151 !important;
}

.el-dialog__footer {
  background: white !important;
  border-top: 1px solid #f3f4f6 !important;
}

/* 输入框样式 */
.el-input__wrapper {
  background: white !important;
  border-radius: 8px !important;
}

.el-textarea__inner {
  background: white !important;
  border-radius: 8px !important;
}

/* 按钮主色 */
.el-button--primary {
  background: #2A9D8F !important;
  border-color: #2A9D8F !important;
}

.el-button--primary:hover {
  background: #238b7e !important;
  border-color: #238b7e !important;
}
</style>