<template>
  <button 
    class="neon-button" 
    :class="`neon-${color}`"
    :style="buttonStyle"
    @mousemove="handleMouseMove"
    @mouseleave="handleMouseLeave"
    @click="handleClick"
    :disabled="disabled"
    v-bind="$attrs"
  >
    <span class="text-content">
      <i v-if="icon" :class="`ri-${icon} mr-2`"></i>
      <slot></slot>
    </span>
    <span class="hover-glow" ref="hoverGlow"></span>
  </button>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface Props {
  color?: 'cyan' | 'purple' | 'orange'
  icon?: string
  disabled?: boolean
  size?: 'sm' | 'md' | 'lg'
}

const props = withDefaults(defineProps<Props>(), {
  color: 'cyan',
  disabled: false,
  size: 'md'
})

const emit = defineEmits<{
  (e: 'click', ev: MouseEvent): void
}>()

const hoverGlow = ref<HTMLElement | null>(null)

const buttonStyle = computed(() => {
  const colorMap = {
    'cyan': {
      '--neon-color': '#00FEFC',
      '--glow-strength': '10px',
    },
    'purple': {
      '--neon-color': '#8A2BE2',
      '--glow-strength': '10px',
    },
    'orange': {
      '--neon-color': '#FF6B35',
      '--glow-strength': '10px',
    }
  }
  
  const sizeMap = {
    'sm': {
      '--padding-x': '1rem',
      '--padding-y': '0.5rem',
      '--font-size': '0.875rem',
    },
    'md': {
      '--padding-x': '1.5rem',
      '--padding-y': '0.75rem',
      '--font-size': '1rem',
    },
    'lg': {
      '--padding-x': '2rem',
      '--padding-y': '1rem',
      '--font-size': '1.125rem',
    }
  }
  
  return {
    ...colorMap[props.color],
    ...sizeMap[props.size]
  }
})

const handleMouseMove = (event: MouseEvent) => {
  if (props.disabled || !hoverGlow.value) return
  
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  
  hoverGlow.value.style.left = `${x}px`
  hoverGlow.value.style.top = `${y}px`
  hoverGlow.value.style.opacity = '1'
}

const handleMouseLeave = () => {
  if (!hoverGlow.value) return
  hoverGlow.value.style.opacity = '0'
}

const handleClick = (e: MouseEvent) => {
  if (props.disabled) return
  emit('click', e)
}
</script>

<style scoped>
.neon-button {
  @apply relative overflow-hidden rounded-lg border-2 bg-white transition-all duration-300;
  padding: var(--padding-y) var(--padding-x);
  font-size: var(--font-size);
  border-color: var(--neon-color);
  color: var(--neon-color);
  z-index: 10;
}

.neon-button:hover:not(:disabled) {
  box-shadow: 0 4px 12px rgba(42, 157, 143, 0.2);
  background-color: var(--neon-color);
  color: white;
}

.neon-button:disabled {
  @apply opacity-50 cursor-not-allowed;
}

.neon-cyan {
  --neon-color: #2A9D8F;
}

.neon-purple {
  --neon-color: #457B9D;
}

.neon-orange {
  --neon-color: #E9C46A;
}

.hover-glow {
  @apply absolute w-0 h-0 rounded-full opacity-0 transition-opacity duration-300 -z-10 pointer-events-none;
  background: radial-gradient(circle, var(--neon-color) 0%, transparent 70%);
  width: 150px;
  height: 150px;
  transform: translate(-50%, -50%);
}
</style> 