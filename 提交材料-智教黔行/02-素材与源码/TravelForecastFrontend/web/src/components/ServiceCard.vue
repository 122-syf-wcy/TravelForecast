<template>
  <div 
    class="service-card"
    :class="[
      { 'active-service': active },
      `theme-${colorTheme}`
    ]"
    @click="$emit('click')"
  >
    <!-- 图标容器 -->
    <div class="icon-container">
      <div class="icon-bg"></div>
      <el-icon class="service-icon">
        <Ticket v-if="icon === 'Ticket'" />
        <List v-else-if="icon === 'List'" />
        <VideoPlay v-else-if="icon === 'VideoPlay'" />
        <Warning v-else-if="icon === 'Warning'" />
        <Comment v-else-if="icon === 'Comment'" />
        <component v-else :is="icon" />
      </el-icon>
    </div>
    
    <!-- 内容 -->
    <div class="card-content">
      <div class="service-title">{{ title }}</div>
      <div class="service-desc">{{ description }}</div>
    </div>
    
    <!-- 热门标签 -->
    <div v-if="active" class="hot-badge">
      <span class="badge-icon">🔥</span>
      <span class="badge-text">热门</span>
    </div>
    
    <!-- 悬浮箭头 -->
    <div class="hover-arrow">
      <el-icon><ArrowRight /></el-icon>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowRight, Ticket, List, VideoPlay, Warning, Comment } from '@element-plus/icons-vue'
import { computed } from 'vue'

const props = defineProps({
  icon: {
    type: String,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    required: true
  },
  active: {
    type: Boolean,
    default: false
  },
  theme: {
    type: String,
    default: 'teal'
  }
})

defineEmits(['click'])

// 根据图标自动选择颜色主题
const colorTheme = computed(() => {
  if (props.theme !== 'teal') return props.theme
  
  const iconThemeMap: Record<string, string> = {
    'Ticket': 'teal',
    'List': 'blue',
    'VideoPlay': 'indigo',
    'Warning': 'orange',
    'Comment': 'rose'
  }
  return iconThemeMap[props.icon] || 'teal'
})
</script>

<style scoped>
.service-card {
  --theme-color: #2A9D8F;
  --theme-color-rgb: 42, 157, 143;
  
  position: relative;
  padding: 20px 16px;
  border-radius: 16px;
  background: #FFFFFF;
  border: 1px solid #EBEEF5;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  min-height: 120px;
}

/* 颜色主题 */
.theme-teal {
  --theme-color: #2A9D8F;
  --theme-color-rgb: 42, 157, 143;
}

.theme-blue {
  --theme-color: #3B82F6;
  --theme-color-rgb: 59, 130, 246;
}

.theme-indigo {
  --theme-color: #6366F1;
  --theme-color-rgb: 99, 102, 241;
}

.theme-orange {
  --theme-color: #E76F51;
  --theme-color-rgb: 231, 111, 81;
}

.theme-rose {
  --theme-color: #F472B6;
  --theme-color-rgb: 244, 114, 182;
}

.theme-green {
  --theme-color: #10B981;
  --theme-color-rgb: 16, 185, 129;
}

.service-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--theme-color), rgba(var(--theme-color-rgb), 0.3));
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.4s ease;
}

.service-card:hover::before {
  transform: scaleX(1);
}

.service-card:hover {
  transform: translateY(-4px);
  border-color: rgba(var(--theme-color-rgb), 0.3);
  box-shadow: 0 12px 32px rgba(var(--theme-color-rgb), 0.1), 0 4px 12px rgba(0, 0, 0, 0.04);
}

/* 图标容器 */
.icon-container {
  position: relative;
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.icon-bg {
  position: absolute;
  inset: 0;
  background: rgba(var(--theme-color-rgb), 0.08);
  border-radius: 14px;
  border: 1px solid rgba(var(--theme-color-rgb), 0.15);
  transition: all 0.3s ease;
}

.service-card:hover .icon-bg {
  background: rgba(var(--theme-color-rgb), 0.12);
  border-color: rgba(var(--theme-color-rgb), 0.3);
  transform: scale(1.05);
}

.service-icon {
  font-size: 26px;
  color: var(--theme-color);
  position: relative;
  z-index: 1;
  transition: all 0.3s ease;
}

.service-card:hover .service-icon {
  transform: scale(1.1);
}

/* 内容样式 */
.card-content {
  position: relative;
  z-index: 1;
}

.service-title {
  font-size: 15px;
  font-weight: 600;
  color: #2C3E50;
  margin-bottom: 4px;
  transition: color 0.3s ease;
}

.service-card:hover .service-title {
  color: var(--theme-color);
}

.service-desc {
  font-size: 12px;
  color: #909399;
  transition: color 0.3s ease;
}

.service-card:hover .service-desc {
  color: #606266;
}

/* 热门标签 */
.hot-badge {
  position: absolute;
  top: -1px;
  right: -1px;
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 4px 10px 4px 8px;
  background: linear-gradient(135deg, #E76F51, #E85D04);
  border-radius: 0 16px 0 12px;
  box-shadow: 0 2px 8px rgba(231, 111, 81, 0.3);
}

.badge-icon {
  font-size: 10px;
}

.badge-text {
  font-size: 10px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 0.5px;
}

/* 悬浮箭头 */
.hover-arrow {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%) translateX(10px);
  opacity: 0;
  color: var(--theme-color);
  font-size: 16px;
  transition: all 0.3s ease;
}

.service-card:hover .hover-arrow {
  opacity: 1;
  transform: translateY(-50%) translateX(0);
}

/* 激活状态 */
.active-service {
  border-color: rgba(var(--theme-color-rgb), 0.3);
  background: rgba(var(--theme-color-rgb), 0.03);
}

.active-service .icon-bg {
  background: rgba(var(--theme-color-rgb), 0.12);
}

.active-service::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 16px;
  padding: 1px;
  background: linear-gradient(135deg, rgba(var(--theme-color-rgb), 0.3), rgba(var(--theme-color-rgb), 0.08));
  -webkit-mask: 
    linear-gradient(#fff 0 0) content-box, 
    linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}
</style>
