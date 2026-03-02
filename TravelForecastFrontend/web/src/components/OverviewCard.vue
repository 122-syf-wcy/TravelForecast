<template>
  <div class="overview-card holographic-card relative overflow-hidden group">
    <div class="p-5 relative z-10">
      <!-- 动态背景光晕 -->
      <div 
        class="absolute -top-10 -right-10 w-32 h-32 rounded-full bg-gradient-to-br opacity-10 group-hover:opacity-20 transition-opacity duration-500 blur-xl" 
        :class="color"
      ></div>
      
      <!-- 图标 -->
      <div class="flex justify-between mb-4 relative">
        <div class="rounded-xl p-3 bg-[#2A9D8F]/10 border border-[#2A9D8F]/20 shadow-sm group-hover:scale-110 transition-transform duration-300">
          <el-icon class="text-2xl text-[#2A9D8F]">
            <component :is="icon" />
          </el-icon>
        </div>
        
        <div class="flex items-center space-x-1 bg-gray-100 rounded-full px-2 py-1 h-fit">
          <el-icon :class="increase >= 0 ? 'text-[#2A9D8F]' : 'text-[#E76F51]'" class="text-xs">
            <ArrowUp v-if="increase >= 0" />
            <ArrowDown v-else />
          </el-icon>
          <span :class="increase >= 0 ? 'text-[#2A9D8F]' : 'text-[#E76F51]'" class="text-xs font-mono">
            {{ Math.abs(increase) }}%
          </span>
        </div>
      </div>
      
      <!-- 数据 -->
      <div class="relative">
        <div class="text-3xl font-bold text-gray-800 mb-1 tracking-wide group-hover:text-[#2A9D8F] transition-colors duration-300">{{ value }}</div>
        <div class="text-gray-500 text-sm flex items-center">
          <span class="w-1 h-4 bg-gradient-to-b from-transparent via-gray-300 to-transparent mr-2"></span>
          {{ title }}
        </div>
      </div>
    </div>
    
    <!-- 边框效果 -->
    <div class="absolute inset-0 border border-gray-200 rounded-lg group-hover:border-[#2A9D8F]/30 transition-colors duration-500 pointer-events-none"></div>
  </div>
</template>

<script setup lang="ts">
import { ArrowUp, ArrowDown } from '@element-plus/icons-vue'

defineProps({
  title: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    required: true
  },
  increase: {
    type: Number,
    default: 0
  },
  icon: {
    type: [String, Object, Function],
    required: true
  },
  color: {
    type: String,
    default: 'from-teal-500 to-cyan-400'
  }
})
</script>

<style scoped>
.holographic-card {
  @apply bg-white rounded-xl transition-all duration-300;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.holographic-card:hover {
  @apply transform -translate-y-1;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}
</style> 