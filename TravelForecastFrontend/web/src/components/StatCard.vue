<template>
  <div class="stat-card rounded-xl overflow-hidden">
    <div class="p-5 relative">
      <!-- 背景装饰图标 -->
      <div class="absolute top-3 right-3 text-[#2A9D8F] opacity-10">
        <el-icon class="text-5xl">
          <component :is="icon" />
        </el-icon>
      </div>
      
      <!-- 左侧色条 -->
      <div class="color-accent" :style="accentStyle"></div>
      
      <!-- 内容 -->
      <div class="relative z-10">
        <div class="text-sm text-[#909399] mb-2 font-medium">{{ title }}</div>
        <div class="flex items-baseline">
          <span class="text-3xl font-bold text-[#2C3E50]">{{ formattedValue }}</span>
          <span class="ml-1 text-sm text-[#909399]">{{ suffix }}</span>
        </div>
        <div class="mt-3 flex items-center text-sm">
          <el-icon class="mr-1" :class="change >= 0 ? 'text-[#2A9D8F]' : 'text-[#E76F51]'">
            <ArrowUp v-if="change >= 0" />
            <ArrowDown v-else />
          </el-icon>
          <span :class="change >= 0 ? 'text-[#2A9D8F]' : 'text-[#E76F51]'" class="font-medium">
            {{ Math.abs(change) }}% {{ change >= 0 ? '增长' : '下降' }}
          </span>
          <span class="ml-2 text-[#C0C4CC]">相比昨日</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowUp, ArrowDown } from '@element-plus/icons-vue'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  value: {
    type: [Number, String],
    required: true
  },
  suffix: {
    type: String,
    default: ''
  },
  change: {
    type: Number,
    default: 0
  },
  icon: {
    type: String,
    required: true
  },
  color: {
    type: String,
    default: '#2A9D8F'
  }
})

const formattedValue = computed(() => {
  if (typeof props.value === 'number') {
    return props.value.toLocaleString()
  }
  return props.value
})

// 根据传入的颜色或默认使用主题色
const accentStyle = computed(() => {
  return {
    background: `linear-gradient(to bottom, ${props.color}, ${props.color}88)`
  }
})
</script>

<style scoped>
.stat-card {
  position: relative;
  background: #FFFFFF;
  border: 1px solid #EBEEF5;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(42, 157, 143, 0.1);
  border-color: rgba(42, 157, 143, 0.25);
}

/* 左侧色条装饰 */
.color-accent {
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  border-radius: 0 4px 4px 0;
}
</style>
