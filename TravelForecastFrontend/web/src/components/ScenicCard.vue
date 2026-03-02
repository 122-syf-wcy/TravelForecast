<template>
  <div class="scenic-card" :class="{ 'has-hover': hoverEffect }">
    <div v-if="title || $slots.header" class="scenic-card-header">
      <div v-if="title" class="title-container">
        <div v-if="icon" class="icon-wrapper">
          <el-icon><component :is="icon" /></el-icon>
        </div>
        <h3 class="title-text">{{ title }}</h3>
      </div>
      <slot name="header"></slot>
    </div>
    <div class="scenic-card-content">
      <slot></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineProps } from 'vue'

interface Props {
  title?: string
  icon?: string // Element Plus icon name
  hoverEffect?: boolean
}

withDefaults(defineProps<Props>(), {
  title: '',
  hoverEffect: true
})
</script>

<style scoped>
.scenic-card {
  background: #FFFFFF;
  border-radius: 16px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.04);
  border: 1px solid #F2F3F5;
  height: 100%;
  display: flex;
  flex-direction: column;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.scenic-card.has-hover:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(42, 157, 143, 0.08); /* 淡淡的青绿色阴影 */
  border-color: rgba(42, 157, 143, 0.2);
}

.scenic-card-header {
  padding: 16px 24px;
  border-bottom: 1px solid #F7F8FA;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-color-primary);
  font-size: 20px;
}

.title-text {
  font-size: 18px;
  font-weight: 600;
  color: #1D2129;
  margin: 0;
  line-height: 1.4;
}

.scenic-card-content {
  flex: 1;
  padding: 24px;
}
</style>
