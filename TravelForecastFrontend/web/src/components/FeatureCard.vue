<template>
  <div class="feature-card">
    <scenic-card class="h-full" :hover-effect="true">
      <div class="flex items-start space-x-4">
        <div class="feature-icon-wrapper">
          <span class="feature-icon">{{ feature.icon }}</span>
        </div>
        <div>
          <h3 class="feature-title">{{ feature.title }}</h3>
          <p class="feature-desc">{{ feature.description }}</p>
        </div>
      </div>
    </scenic-card>
  </div>
</template>

<script setup lang="ts">
import ScenicCard from '@/components/ScenicCard.vue'
defineProps<{ feature: { id: number; icon: string; title: string; description: string } }>()
</script>

<style scoped>
.feature-icon-wrapper {
  width: 48px;
  height: 48px;
  background: #E6FFFA;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature-icon {
  font-size: 24px;
  line-height: 1;
}

.feature-title {
  font-size: 18px;
  font-weight: 600;
  color: #1D2129;
  margin-bottom: 8px;
}

.feature-desc {
  font-size: 14px;
  color: #86909C;
  line-height: 1.6;
}
</style>
