<template>
  <div class="particle-container" ref="particleContainer">
    <canvas ref="particleCanvas" class="particle-canvas"></canvas>
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'

interface Particle {
  x: number
  y: number
  size: number
  speed: number
  color: string
  alpha: number
  direction: number
}

interface Props {
  particleCount?: number
  particleColor?: string
  connectParticles?: boolean
  maxDistance?: number
  responsive?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  particleCount: 100,
  particleColor: '#00FEFC',
  connectParticles: true,
  maxDistance: 120,
  responsive: true
})

const particleContainer = ref<HTMLDivElement | null>(null)
const particleCanvas = ref<HTMLCanvasElement | null>(null)

let particles: Particle[] = []
let animationFrame: number | null = null
let ctx: CanvasRenderingContext2D | null = null
let canvasWidth = 0
let canvasHeight = 0
let mouseX = 0
let mouseY = 0
let mouseRadius = 150

const initCanvas = () => {
  if (!particleCanvas.value || !particleContainer.value) return
  
  canvasWidth = particleContainer.value.clientWidth
  canvasHeight = particleContainer.value.clientHeight
  
  particleCanvas.value.width = canvasWidth
  particleCanvas.value.height = canvasHeight
  
  ctx = particleCanvas.value.getContext('2d')
  
  if (!ctx) return
  
  createParticles()
  animateParticles()
  
  window.addEventListener('resize', handleResize)
  window.addEventListener('mousemove', handleMouseMove)
}

const createParticles = () => {
  particles = []
  
  for (let i = 0; i < props.particleCount; i++) {
    const particle: Particle = {
      x: Math.random() * canvasWidth,
      y: Math.random() * canvasHeight,
      size: Math.random() * 2 + 1,
      speed: Math.random() * 0.5 + 0.2,
      color: props.particleColor,
      alpha: Math.random() * 0.6 + 0.2,
      direction: Math.random() * 360
    }
    
    particles.push(particle)
  }
}

const animateParticles = () => {
  if (!ctx) return
  
  ctx.clearRect(0, 0, canvasWidth, canvasHeight)
  
  for (const particle of particles) {
    // 更新粒子位置
    const moveX = Math.cos(particle.direction * Math.PI / 180) * particle.speed
    const moveY = Math.sin(particle.direction * Math.PI / 180) * particle.speed
    
    particle.x += moveX
    particle.y += moveY
    
    // 边界检查
    if (particle.x < 0) particle.x = canvasWidth
    if (particle.x > canvasWidth) particle.x = 0
    if (particle.y < 0) particle.y = canvasHeight
    if (particle.y > canvasHeight) particle.y = 0
    
    // 绘制粒子
    ctx.beginPath()
    ctx.arc(particle.x, particle.y, particle.size, 0, Math.PI * 2)
    ctx.fillStyle = particle.color
    ctx.globalAlpha = particle.alpha
    ctx.fill()
    
    // 鼠标交互
    const dx = mouseX - particle.x
    const dy = mouseY - particle.y
    const distance = Math.sqrt(dx * dx + dy * dy)
    
    if (distance < mouseRadius) {
      const forceFactor = (mouseRadius - distance) / mouseRadius
      particle.x -= dx * forceFactor * 0.02
      particle.y -= dy * forceFactor * 0.02
      particle.alpha = Math.min(particle.alpha + 0.2, 1)
    }
    
    // 连接粒子
    if (props.connectParticles) {
      connectNearbyParticles(particle)
    }
  }
  
  animationFrame = requestAnimationFrame(animateParticles)
}

const connectNearbyParticles = (particle: Particle) => {
  if (!ctx) return
  
  for (const otherParticle of particles) {
    if (particle === otherParticle) continue
    
    const dx = particle.x - otherParticle.x
    const dy = particle.y - otherParticle.y
    const distance = Math.sqrt(dx * dx + dy * dy)
    
    if (distance < props.maxDistance) {
      const opacity = 1 - distance / props.maxDistance
      
      ctx.beginPath()
      ctx.moveTo(particle.x, particle.y)
      ctx.lineTo(otherParticle.x, otherParticle.y)
      ctx.strokeStyle = props.particleColor
      ctx.globalAlpha = opacity * 0.6
      ctx.lineWidth = 1
      ctx.stroke()
    }
  }
}

const handleResize = () => {
  if (!particleContainer.value || !particleCanvas.value || !props.responsive) return
  
  canvasWidth = particleContainer.value.clientWidth
  canvasHeight = particleContainer.value.clientHeight
  
  particleCanvas.value.width = canvasWidth
  particleCanvas.value.height = canvasHeight
  
  createParticles()
}

const handleMouseMove = (event: MouseEvent) => {
  if (!particleCanvas.value) return
  
  const rect = particleCanvas.value.getBoundingClientRect()
  mouseX = event.clientX - rect.left
  mouseY = event.clientY - rect.top
}

onMounted(() => {
  initCanvas()
})

onBeforeUnmount(() => {
  if (animationFrame !== null) {
    cancelAnimationFrame(animationFrame)
  }
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('mousemove', handleMouseMove)
})

watch(() => props.particleCount, () => {
  createParticles()
})
</script>

<style scoped>
.particle-container {
  @apply absolute inset-0 w-full h-full overflow-hidden;
  z-index: 0;
}

.particle-canvas {
  @apply block w-full h-full;
}
</style> 