<template>
  <div 
    class="data-matrix-container" 
    ref="matrixContainer"
    :style="{ 
      opacity: opacity, 
      '--matrix-color': color 
    }"
  >
    <canvas ref="matrixCanvas" class="matrix-canvas"></canvas>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'

interface Props {
  color?: string
  speed?: number
  density?: number
  fontSize?: number
  opacity?: number
}

const props = withDefaults(defineProps<Props>(), {
  color: '#00FEFC',
  speed: 1,
  density: 0.8,
  fontSize: 14,
  opacity: 0.15
})

const matrixContainer = ref<HTMLDivElement | null>(null)
const matrixCanvas = ref<HTMLCanvasElement | null>(null)

interface Column {
  x: number
  y: number
  speed: number
  chars: string[]
  currentChar: number
}

let columns: Column[] = []
let animationFrame: number | null = null
let ctx: CanvasRenderingContext2D | null = null
let canvasWidth = 0
let canvasHeight = 0

const possibleChars = '01'.split('')

const initMatrix = () => {
  if (!matrixCanvas.value || !matrixContainer.value) return
  
  canvasWidth = matrixContainer.value.clientWidth
  canvasHeight = matrixContainer.value.clientHeight
  
  matrixCanvas.value.width = canvasWidth
  matrixCanvas.value.height = canvasHeight
  
  ctx = matrixCanvas.value.getContext('2d')
  if (!ctx) return
  
  const columnCount = Math.floor(canvasWidth / props.fontSize)
  columns = []
  
  for (let i = 0; i < columnCount * props.density; i++) {
    const column: Column = {
      x: Math.floor(Math.random() * columnCount) * props.fontSize,
      y: Math.floor(Math.random() * canvasHeight),
      speed: Math.random() * 3 + 1 * props.speed,
      chars: Array(Math.floor(Math.random() * 15 + 5)).fill('').map(() => getRandomChar()),
      currentChar: 0
    }
    columns.push(column)
  }
  
  animateMatrix()
  window.addEventListener('resize', handleResize)
}

const animateMatrix = () => {
  if (!ctx) return
  
  ctx.fillStyle = 'rgba(10, 17, 39, 0.05)'
  ctx.fillRect(0, 0, canvasWidth, canvasHeight)
  
  ctx.font = `${props.fontSize}px monospace`
  
  for (const column of columns) {
    // 当前字符
    const char = column.chars[column.currentChar % column.chars.length]
    
    // 绘制字符
    ctx.fillStyle = props.color
    ctx.fillText(char, column.x, column.y)
    
    // 更新位置
    column.y += column.speed
    
    // 切换字符
    if (Math.random() > 0.95) {
      column.currentChar++
    }
    
    // 重置位置
    if (column.y > canvasHeight) {
      column.y = -props.fontSize
      column.x = Math.floor(Math.random() * (canvasWidth / props.fontSize)) * props.fontSize
      column.speed = Math.random() * 3 + 1 * props.speed
    }
  }
  
  animationFrame = requestAnimationFrame(animateMatrix)
}

const getRandomChar = () => {
  return possibleChars[Math.floor(Math.random() * possibleChars.length)]
}

const handleResize = () => {
  if (!matrixContainer.value || !matrixCanvas.value) return
  
  canvasWidth = matrixContainer.value.clientWidth
  canvasHeight = matrixContainer.value.clientHeight
  
  matrixCanvas.value.width = canvasWidth
  matrixCanvas.value.height = canvasHeight
  
  initMatrix()
}

onMounted(() => {
  initMatrix()
})

onBeforeUnmount(() => {
  if (animationFrame !== null) {
    cancelAnimationFrame(animationFrame)
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.data-matrix-container {
  @apply absolute inset-0 w-full h-full overflow-hidden;
  z-index: -1;
}

.matrix-canvas {
  @apply block w-full h-full;
}
</style> 