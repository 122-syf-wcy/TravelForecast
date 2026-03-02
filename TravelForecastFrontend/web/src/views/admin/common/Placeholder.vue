<template>
  <div class="placeholder-container">
    <holographic-card class="p-8">
      <div class="text-center">
        <div class="text-6xl mb-4">{{ icon }}</div>
        <h2 class="text-2xl font-bold text-[#2A9D8F] mb-2">{{ title }}</h2>
        <p class="text-gray-500">{{ description }}</p>
        <div class="mt-6 text-sm text-gray-500">
          <p>此功能页面开发中...</p>
          <p class="mt-2">路由路径: {{ route.path }}</p>
        </div>
      </div>
    </holographic-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import HolographicCard from '@/components/HolographicCard.vue'

const route = useRoute()

const props = defineProps<{
  title?: string
  description?: string
  icon?: string
}>()

const title = computed(() => props.title || route.meta.title || '功能页面')
const description = computed(() => props.description || '该功能模块正在开发中，敬请期待')
const icon = computed(() => props.icon || '🚧')
</script>

<style scoped>
.placeholder-container {
  @apply min-h-screen flex items-center justify-center;
}
</style>













