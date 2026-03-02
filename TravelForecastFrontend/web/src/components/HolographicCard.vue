<template>
  <div 
    class="holographic-card" 
    :style="cardStyle" 
    :class="[`border-${borderColor}`]"
  >
    <div v-if="title" class="card-header flex items-center mb-4">
      <div 
        v-if="icon" 
        class="neon-icon mr-3 text-xl" 
        :class="`text-${glowColor}`"
      >
        <i :class="`ri-${icon}`"></i>
      </div>
      <h3 
        class="text-lg font-medium tracking-wide m-0"
        :class="`text-${glowColor}`"
      >{{ title }}</h3>
    </div>
    <slot name="header"></slot>
    <div class="card-content relative z-10 pointer-events-auto">
      <slot></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  title?: string
  icon?: string
  glowColor?: 'neon-cyan' | 'tech-purple' | 'energy-orange'
  borderColor?: 'neon-cyan' | 'tech-purple' | 'energy-orange'
  glow?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  glowColor: 'neon-cyan',
  borderColor: 'neon-cyan',
  glow: false
})

const cardStyle = computed(() => {
  const colorMap = {
    'neon-cyan': '#2A9D8F',
    'tech-purple': '#457B9D',
    'energy-orange': '#E9C46A'
  }
  
  return {
    '--glow-color': colorMap[props.glowColor],
    '--border-color': colorMap[props.borderColor]
  }
})
</script>

<style scoped>
.holographic-card {
  @apply bg-white border border-gray-200 rounded-xl p-5 relative overflow-hidden transition-all duration-300;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.holographic-card:hover {
  @apply transform -translate-y-1;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  border-color: var(--border-color);
}

.text-neon-cyan {
  color: #2A9D8F;
}

.text-tech-purple {
  color: #457B9D;
}

.text-energy-orange {
  color: #E9C46A;
}

.border-neon-cyan {
  border-color: rgba(42, 157, 143, 0.3);
}

.border-tech-purple {
  border-color: rgba(69, 123, 157, 0.3);
}

.border-energy-orange {
  border-color: rgba(233, 196, 106, 0.3);
}
</style> 