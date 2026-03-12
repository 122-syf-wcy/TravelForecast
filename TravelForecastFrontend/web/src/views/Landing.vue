<template>
  <div class="landing-page">
    <nav class="nav-bar" :class="{ 'nav-scrolled': isScrolled }">
      <div class="nav-container">
        <div class="nav-logo"><span class="logo-text">智教黔行</span></div>
        <div class="nav-menu">
          <a href="javascript:void(0)" @click="scrollToSection('hero')" class="nav-item">首页</a>
          <a href="javascript:void(0)" @click="scrollToSection('features')" class="nav-item">功能</a>
          <a href="javascript:void(0)" @click="scrollToExperience" class="nav-item">体验功能</a>
          <a href="javascript:void(0)" @click="scrollToSection('highlights')" class="nav-item">亮点</a>
        </div>
        <div class="nav-actions">
          <button class="nav-btn nav-btn-text" @click="goToLogin">登录</button>
          <button class="nav-btn nav-btn-primary" @click="goToRegister">注册</button>
        </div>
      </div>
    </nav>
    <section class="hero-section" id="hero">
      <div class="hero-bg">
        <transition name="fade">
          <img v-if="heroImage" :key="currentBannerIndex" :src="heroImage" alt="六盘水风景" class="hero-bg-img" loading="eager" fetchpriority="high" />
        </transition>
        <div class="hero-overlay"></div>
      </div>
      <div class="hero-content">
        <h1 class="hero-title">智教黔行数据预测平台</h1>
        <p class="hero-subtitle">基于AI大数据的六盘水旅游智能决策系统</p>
        <div class="hero-cta">
          <button class="cta-btn cta-btn-primary" @click="scrollToExperience"><span>立即体验</span></button>
          <button class="cta-btn cta-btn-outline" @click="goToLogin"><span>了解更多</span></button>
        </div>
      </div>
      <!-- 轮播指示器 -->
      <div v-if="banners.length > 1" class="carousel-indicators">
        <span v-for="(b, i) in banners" :key="i" class="indicator" :class="{ active: i === currentBannerIndex }" @click="currentBannerIndex = i"></span>
      </div>
      <div class="stats-container">
        <div class="stats-grid">
          <div v-for="s in stats" :key="s.id" class="stat-card">
            <div class="stat-value">{{ Math.round(s.value) }}<span class="stat-suffix">{{ s.suffix }}</span></div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>
      </div>
    </section>
    <section v-if="showcases.length > 0" class="showcase-section" id="showcase">
      <div class="section-container">
        <h2 class="section-title reveal">实景预览</h2>
        <p class="section-desc reveal">探索六盘水的自然美景与人文风情</p>
        <div class="showcase-grid">
          <div v-for="item in showcases" :key="item.id" class="showcase-card reveal" @click="openShowcase(item)">
            <div class="showcase-img-wrapper">
              <img :src="item.type === 'video' ? (item.cover || item.url) : item.url" :alt="item.title || '实景预览'" class="showcase-img" loading="lazy" @error="handleShowcaseImageError($event, item)" />
              <div v-if="item.type === 'video'" class="video-play-btn"><i class="ri-play-circle-fill"></i></div>
              <div class="showcase-overlay-text">
                <span class="showcase-title">{{ item.title || '实景预览' }}</span>
                <span v-if="item.description" class="showcase-desc">{{ item.description }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
    
    <!-- 视频弹窗 -->
    <div v-if="showVideoModal" class="video-modal" @click.self="showVideoModal = false">
      <div class="video-modal-content">
        <button class="video-close-btn" @click="showVideoModal = false"><i class="ri-close-line"></i></button>
        <video v-if="currentVideo" :src="currentVideo.url" controls autoplay class="modal-video"></video>
      </div>
    </div>
    <section class="experience-section" id="experience">
      <div class="section-container">
        <h2 class="section-title reveal">免费体验</h2>
        <p class="section-desc reveal">无需注册登录，立即体验我们的核心功能</p>
        <div class="experience-grid">
          <div v-for="exp in experiences" :key="exp.id" class="experience-card reveal" @click="goToExperience(exp.path)">
            <div class="exp-icon">{{ exp.icon }}</div>
            <h3 class="exp-title">{{ exp.title }}</h3>
            <p class="exp-desc">{{ exp.desc }}</p>
            <div class="exp-action">立即体验</div>
          </div>
        </div>
      </div>
    </section>

    <section class="features-section" id="features">
      <div class="section-container">
        <h2 class="section-title reveal">核心功能</h2>
        <p class="section-desc reveal">智能化旅游数据分析与预测平台</p>
        <div class="features-grid">
          <div v-for="feature in features" :key="feature.id" class="feature-card reveal">
            <div class="feature-icon">{{ feature.icon }}</div>
            <h3 class="feature-title">{{ feature.title }}</h3>
            <p class="feature-desc">{{ feature.description }}</p>
          </div>
        </div>
      </div>
    </section>
    <section class="highlights-section" id="highlights">
      <div class="section-container">
        <h2 class="section-title reveal">平台亮点</h2>
        <p class="section-desc reveal">创新技术驱动的智慧旅游解决方案</p>
        <div class="highlights-grid">
          <div v-for="h in highlights" :key="h.id" class="highlight-card reveal">
            <div class="highlight-badge" :class="h.badgeClass">{{ h.badge }}</div>
            <h3 class="highlight-title">{{ h.title }}</h3>
            <p class="highlight-desc">{{ h.desc }}</p>
          </div>
        </div>
      </div>
    </section>
    <section class="scenarios-section" id="scenarios">
      <div class="section-container">
        <h2 class="section-title reveal">典型场景</h2>
        <p class="section-desc reveal">多场景应用全方位提升旅游体验</p>
        <div class="scenarios-grid">
          <div v-for="s in scenarios" :key="s.id" class="scenario-card reveal">
            <div class="scenario-icon">{{ s.icon }}</div>
            <div class="scenario-content">
              <h3 class="scenario-title">{{ s.title }}</h3>
              <p class="scenario-desc">{{ s.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>
    <section class="model-section" id="model-comparison">
      <div class="section-container">
        <h2 class="section-title reveal">模型效果对比</h2>
        <p class="section-desc reveal">ARIMA vs LSTM vs LSTM-ARIMA vs 双流融合模型</p>
        <div class="model-chart-wrapper reveal">
          <div ref="modelChartRef" class="model-chart"></div>
        </div>
      </div>
    </section>
    <section class="footer-cta">
      <div class="section-container text-center">
        <h2 class="cta-title reveal">开始您的智慧旅游之旅</h2>
        <p class="cta-desc reveal">立即注册体验AI驱动的旅游数据分析平台</p>
        <div class="cta-buttons reveal">
          <button class="cta-btn cta-btn-white" @click="goToRegister">免费注册</button>
          <button class="cta-btn cta-btn-outline-white" @click="scrollToExperience">先体验一下</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
// ECharts 按需导入（减少约 600KB 包体积）
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { contentApi } from '@/api/content'
import { getImageUrl } from '@/utils/imageUrl'

// 注册必要的组件
echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const router = useRouter()
const isScrolled = ref(false)
const heroImage = ref('')
const currentBannerIndex = ref(0)
let carouselTimer: number | null = null
// 节流函数
const throttle = <T extends (...args: any[]) => void>(fn: T, delay: number) => {
  let lastCall = 0
  return (...args: Parameters<T>) => {
    const now = Date.now()
    if (now - lastCall >= delay) {
      lastCall = now
      fn(...args)
    }
  }
}

const updateScrollState = () => { isScrolled.value = window.scrollY > 50 }
const handleScroll = throttle(updateScrollState, 100) // 100ms 节流
const isLoading = ref(true)

const stats = ref([
  { id: 1, label: '预测准确率', value: 0, target: 92, suffix: '%' },
  { id: 2, label: '覆盖景区', value: 0, target: 5, suffix: '+' },
  { id: 3, label: '平均响应', value: 0, target: 180, suffix: 'ms' }
])

const features = ref([
  { id: 1, icon: '📊', title: '智能预测', description: '基于双流融合模型的客流量预测准确率高达92%' },
  { id: 2, icon: '🗺️', title: '实时监控', description: '景区人流实时监控热力图可视化展示' },
  { id: 3, icon: '📈', title: '数据分析', description: '多维度数据分析洞察旅游趋势变化' },
  { id: 4, icon: '🔔', title: '预警推送', description: '智能预警系统及时推送拥挤预警信息' },
  { id: 5, icon: '📱', title: '多端适配', description: '支持PC平板手机多终端访问' },
  { id: 6, icon: '🤖', title: 'AI助手', description: '智能问答助手随时解答旅游相关问题' }
])

const experiences = ref([
  { id: 1, icon: '📊', title: '客流预测', desc: '体验AI驱动的景区客流量预测功能', path: '/user/prediction' },
  { id: 2, icon: '🗺️', title: '实时服务', desc: '查看景区实时人流热力分布', path: '/user/service' },
  { id: 3, icon: '🤖', title: 'AI问答', desc: '与智能助手对话获取旅游建议', path: '/user/ai' }
])

const highlights = ref([
  { id: 1, badge: '地域适配', badgeClass: '', title: '六盘水本地化', desc: '结合地形气候节假日民族节庆等在地特征进行模型调优' },
  { id: 2, badge: 'AI 预测', badgeClass: 'highlight-badge-ai', title: '双流融合算法', desc: '时序特征流与外部因子流深度融合客流预测更精准' },
  { id: 3, badge: '3D 可视化', badgeClass: 'highlight-badge-3d', title: '沉浸式数据大屏', desc: '三维地图与实时指标联动热点拥挤区一目了然' }
])

const scenarios = ref([
  { id: 1, icon: '🏛️', title: '景区管理', desc: '实时掌握客流动态优化资源配置提升游客体验' },
  { id: 2, icon: '🚗', title: '交通调度', desc: '预测高峰时段提前部署交通疏导方案' },
  { id: 3, icon: '🏨', title: '酒店运营', desc: '预测入住需求动态调整房价策略' },
  { id: 4, icon: '📢', title: '营销推广', desc: '分析游客画像精准投放营销内容' }
])

const banners = ref<any[]>([])
const showcases = ref<any[]>([])
const showVideoModal = ref(false)
const currentVideo = ref<any>(null)

// 打开实景预览（视频弹窗或图片查看）
const openShowcase = (item: any) => {
  if (item.type === 'video') {
    currentVideo.value = item
    showVideoModal.value = true
  }
}

// 轮播切换
const startCarousel = () => {
  if (carouselTimer) clearInterval(carouselTimer)
  carouselTimer = window.setInterval(() => {
    if (banners.value.length > 1) {
      currentBannerIndex.value = (currentBannerIndex.value + 1) % banners.value.length
    }
  }, 5000) // 5秒切换一次
}

// 监听 currentBannerIndex 变化，更新 heroImage
watch(currentBannerIndex, (newIndex) => {
  if (banners.value[newIndex]) {
    heroImage.value = banners.value[newIndex].image
  }
})

const goToLogin = () => router.push('/login')
const goToRegister = () => router.push('/register')
const goToExperience = (path?: string) => {
  if (path) router.push(path)
  else document.getElementById('experience')?.scrollIntoView({ behavior: 'smooth' })
}
const scrollToSection = (id: string) => { document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' }) }
const scrollToExperience = () => { document.getElementById('experience')?.scrollIntoView({ behavior: 'smooth' }) }

// 图片加载失败处理 - 避免无限重试
const handleImageError = (e: Event) => { 
  const img = e.target as HTMLImageElement
  // 如果已经是 placeholder 或者已经处理过，不再重试
  if (img.dataset.errorHandled === 'true') return
  img.dataset.errorHandled = 'true'
  // 使用 data URI 的灰色占位图，避免请求不存在的文件
  img.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300"%3E%3Crect fill="%23e0e0e0" width="400" height="300"/%3E%3Ctext fill="%23999" font-family="sans-serif" font-size="16" x="50%25" y="50%25" text-anchor="middle" dy=".3em"%3E图片加载失败%3C/text%3E%3C/svg%3E'
}

// 实景预览图片加载失败 - 显示占位图而不是移除
const handleShowcaseImageError = (e: Event, item: any) => {
  const img = e.target as HTMLImageElement
  if (img.dataset.errorHandled === 'true') return
  img.dataset.errorHandled = 'true'
  // 使用占位图
  img.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300"%3E%3Crect fill="%232A9D8F" width="400" height="300"/%3E%3Ctext fill="%23fff" font-family="sans-serif" font-size="16" x="50%25" y="50%25" text-anchor="middle" dy=".3em"%3E' + encodeURIComponent(item.title || '实景预览') + '%3C/text%3E%3C/svg%3E'
}

const modelChartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const renderModelChart = () => {
  if (!modelChartRef.value) return
  chartInstance = echarts.init(modelChartRef.value)
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['ARIMA', 'LSTM', 'LSTM-ARIMA', '双流融合'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月'] },
    yAxis: { type: 'value', name: 'MAE' },
    series: [
      { name: 'ARIMA', type: 'line', data: [195, 182, 178, 190, 210, 185, 180], smooth: true, itemStyle: { color: '#E76F51' } },
      { name: 'LSTM', type: 'line', data: [142, 128, 135, 130, 155, 126, 122], smooth: true, itemStyle: { color: '#F4A261' } },
      { name: 'LSTM-ARIMA', type: 'line', data: [105, 95, 100, 98, 118, 92, 90], smooth: true, itemStyle: { color: '#E9C46A' } },
      { name: '双流融合', type: 'line', data: [72, 65, 68, 66, 82, 64, 60], smooth: true, itemStyle: { color: '#2A9D8F' }, areaStyle: { color: 'rgba(42,157,143,0.1)' } }
    ]
  })
}

const fetchLandingData = async () => {
  try {
    // 获取 landing 数据（stats, features 等）
    const res = await contentApi.getLanding()
    console.log('Landing API response:', res)
    
    let data = res.data
    if (res.data?.code === 200 && res.data?.data) {
      data = res.data.data
    }
    console.log('Parsed landing data:', data)
    
    // 更新 stats 数据
    if (data && data.stats && data.stats.length > 0) {
      console.log('Stats from API:', JSON.stringify(data.stats))
      stats.value = data.stats.map((s: any, i: number) => {
        const item = {
          id: s.id || i + 1,
          label: s.label || s.name || '',
          value: 0,
          target: Number(s.target) || Number(s.value) || 0,
          suffix: s.suffix || s.unit || ''
        }
        console.log('Mapped stat:', item)
        return item
      })
    }
    
    // 更新 features 数据
    if (data && data.features && data.features.length > 0) {
      features.value = data.features.map((f: any, i: number) => ({
        id: f.id || i + 1,
        icon: f.icon || '📊',
        title: f.title || '',
        description: f.description || f.desc || ''
      }))
    }
    
    // 更新 highlights 数据
    if (data && data.highlights && data.highlights.length > 0) {
      highlights.value = data.highlights.map((h: any, i: number) => ({
        id: h.id || i + 1,
        badge: h.badge || h.label || '',
        badgeClass: h.badgeClass || '',
        title: h.title || '',
        desc: h.desc || h.description || ''
      }))
    }
    
    // 更新 scenarios 数据
    if (data && data.scenarios && data.scenarios.length > 0) {
      scenarios.value = data.scenarios.map((s: any, i: number) => ({
        id: s.id || i + 1,
        icon: s.icon || '🏛️',
        title: s.title || '',
        desc: s.desc || s.description || ''
      }))
    }
  } catch (e) { 
    console.log('Landing API failed', e)
  }
  
  // 总是获取轮播图数据
  try {
    const bannersRes = await contentApi.getBanners()
    console.log('Banners API response:', bannersRes.data)
    
    let bannersData = bannersRes.data
    if (bannersRes.data?.code === 200 && bannersRes.data?.data) {
      bannersData = bannersRes.data.data
    } else if (Array.isArray(bannersRes.data)) {
      bannersData = bannersRes.data
    }
    
    if (bannersData && bannersData.length > 0) {
      console.log('Raw banner data:', bannersData[0])
      banners.value = bannersData.map((b: any) => {
        // 兼容多种字段名：image, imageUrl, img
        const imgUrl = b.image || b.imageUrl || b.img || ''
        console.log('Banner image URL:', imgUrl, '-> processed:', getImageUrl(imgUrl))
        return { 
          ...b, 
          title: b.title || b.name || '六盘水风景',
          image: getImageUrl(imgUrl)
        }
      })
      const firstImg = bannersData[0].image || bannersData[0].imageUrl || bannersData[0].img || ''
      heroImage.value = getImageUrl(firstImg)
      console.log('Banners loaded:', banners.value.length, 'Hero image:', heroImage.value)
      // 启动轮播
      if (banners.value.length > 1) {
        startCarousel()
      }
    }
  } catch (e) {
    console.log('Banners API failed', e)
  }
  
  // 获取实景预览数据
  try {
    const showcaseRes = await contentApi.getShowcases()
    console.log('Showcases API response:', showcaseRes)
    
    // 解析多层嵌套的响应数据
    let showcaseData = showcaseRes
    // 如果是 axios 响应，取 data
    if (showcaseRes?.data !== undefined) {
      showcaseData = showcaseRes.data
    }
    // 如果是 Result 包装，取 data
    if (showcaseData?.code === 200 && showcaseData?.data !== undefined) {
      showcaseData = showcaseData.data
    }
    // 如果还有一层 data
    if (showcaseData?.data && Array.isArray(showcaseData.data)) {
      showcaseData = showcaseData.data
    }
    
    console.log('Parsed showcase data:', showcaseData)
    
    if (showcaseData && Array.isArray(showcaseData) && showcaseData.length > 0) {
      showcases.value = showcaseData
        .filter((s: any) => s.url && s.url.trim() !== '') // 过滤掉没有 URL 的项
        .map((s: any) => ({
          ...s,
          url: s.url, // OSS 签名 URL 已经是完整的，不需要再处理
          cover: s.cover || ''
        }))
      console.log('Showcases loaded:', showcases.value.length, showcases.value)
    } else {
      showcases.value = []
      console.log('No showcase data found')
    }
  } catch (e) {
    console.log('Showcases API failed', e)
    showcases.value = []
  }
}

const animateStats = () => {
  const duration = 2000, startTime = Date.now()
  const animate = () => {
    const progress = Math.min((Date.now() - startTime) / duration, 1)
    stats.value.forEach(s => { s.value = Math.round(s.target * (1 - Math.pow(1 - progress, 3))) })
    if (progress < 1) requestAnimationFrame(animate)
  }
  animate()
}

// 创建全局 IntersectionObserver
let revealObserver: IntersectionObserver | null = null

const setupRevealObserver = () => {
  if (revealObserver) return revealObserver
  revealObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => { 
      if (entry.isIntersecting) {
        entry.target.classList.add('revealed')
      }
    })
  }, { threshold: 0.1, rootMargin: '50px' })
  return revealObserver
}

// 观察新添加的 reveal 元素
const observeRevealElements = () => {
  nextTick(() => {
    const observer = setupRevealObserver()
    document.querySelectorAll('.reveal:not(.revealed)').forEach(el => {
      observer.observe(el)
    })
  })
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
  handleScroll()
  fetchLandingData().then(() => {
    // 数据加载完成后再执行动画
    setTimeout(animateStats, 300)
    // 重新观察新渲染的 reveal 元素
    observeRevealElements()
  })
  nextTick(() => renderModelChart())
  // 初始观察
  observeRevealElements()
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
  chartInstance?.dispose()
  if (carouselTimer) clearInterval(carouselTimer)
  if (revealObserver) {
    revealObserver.disconnect()
    revealObserver = null
  }
})
</script>

<style scoped>
.landing-page{min-height:100vh;background:#fff !important;overflow-x:hidden}
.nav-bar{position:fixed;top:0;left:0;right:0;z-index:1000;padding:1rem 2rem;transition:all .3s ease;background:transparent}
.nav-bar.nav-scrolled{background:#fff;box-shadow:0 2px 20px rgba(0,0,0,.1)}
.nav-container{max-width:1400px;margin:0 auto;display:flex;align-items:center;justify-content:space-between}
.nav-logo .logo-text{font-size:1.5rem;font-weight:700;color:#fff;transition:color .3s}
.nav-scrolled .logo-text{color:#2A9D8F}
.nav-menu{display:flex;gap:2rem}
.nav-item{color:rgba(255,255,255,.9);text-decoration:none;font-weight:500;transition:color .3s}
.nav-scrolled .nav-item{color:#333}
.nav-item:hover{color:#2A9D8F}
.nav-actions{display:flex;gap:1rem}
.nav-btn{padding:.5rem 1.25rem;border-radius:8px;font-weight:500;cursor:pointer;transition:all .3s;border:none}
.nav-btn-text{background:transparent;color:#fff}
.nav-scrolled .nav-btn-text{color:#333}
.nav-btn-primary{background:#2A9D8F;color:#fff}
.nav-btn-primary:hover{background:#238b7e}
.hero-section{position:relative;height:100vh;min-height:700px;display:flex;flex-direction:column;justify-content:center;align-items:center;padding:0 2rem 120px;background:linear-gradient(135deg,#2A9D8F 0%,#457B9D 100%)}
.hero-bg{position:absolute;inset:0;z-index:1;background:linear-gradient(135deg,#2A9D8F 0%,#457B9D 100%)}
.hero-bg-img{position:absolute;top:0;left:0;width:100%;height:100%;object-fit:cover;z-index:2}
.hero-overlay{position:absolute;inset:0;background:rgba(15,23,42,.4);z-index:3}
.carousel-indicators{position:absolute;bottom:80px;left:50%;transform:translateX(-50%);display:flex;gap:8px;z-index:20}
.indicator{width:10px;height:10px;border-radius:50%;background:rgba(255,255,255,.5);cursor:pointer;transition:all .3s}
.indicator.active{background:#fff;transform:scale(1.2)}
.fade-enter-active,.fade-leave-active{transition:opacity 1s ease}
.fade-enter-from,.fade-leave-to{opacity:0}
.fade-leave-active{position:absolute}
.hero-content{position:relative;z-index:50;text-align:center;max-width:800px}
.hero-title{font-size:3.5rem;font-weight:800;color:#fff;margin-bottom:1.5rem;text-shadow:0 2px 20px rgba(0,0,0,.3)}
.hero-subtitle{font-size:1.5rem;color:rgba(255,255,255,.9);margin-bottom:2.5rem}
.hero-cta{display:flex;gap:1rem;justify-content:center}
.cta-btn{padding:1rem 2.5rem;border-radius:12px;font-size:1.1rem;font-weight:600;cursor:pointer;transition:all .3s;border:2px solid transparent}
.cta-btn-primary{background:#2A9D8F;color:#fff;border-color:#2A9D8F}
.cta-btn-primary:hover{background:#238b7e;transform:translateY(-3px);box-shadow:0 10px 30px rgba(42,157,143,.4)}
.cta-btn-outline{background:transparent;color:#fff;border-color:rgba(255,255,255,.5)}
.cta-btn-outline:hover{background:rgba(255,255,255,.1);border-color:#fff}
.stats-container{position:absolute;bottom:-60px;left:50%;transform:translateX(-50%);width:100%;max-width:900px;padding:0 2rem;z-index:50}
.stats-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:1.5rem}
.stat-card{background:#fff;border-radius:16px;padding:2rem;text-align:center;box-shadow:0 20px 40px rgba(0,0,0,.15)}
.stat-value{font-size:2.5rem;font-weight:800;color:#2A9D8F}
.stat-suffix{font-size:1.5rem}
.stat-label{margin-top:.5rem;color:#666}
.section-container{max-width:1200px;margin:0 auto;padding:0 2rem}
.section-title{font-size:2.5rem;font-weight:700;color:#2C3E50;text-align:center;margin-bottom:1rem}
.section-desc{font-size:1.1rem;color:#666;text-align:center;margin-bottom:3rem}
.text-center{text-align:center}
.showcase-section{padding:120px 0 80px;background:#F5F7FA}
.showcase-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:1.5rem}
.showcase-card{border-radius:16px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,.1);transition:transform .3s}
.showcase-card:hover{transform:translateY(-10px)}
.showcase-img-wrapper{position:relative;aspect-ratio:4/3}
.showcase-img{width:100%;height:100%;object-fit:cover}
.showcase-overlay-text{position:absolute;bottom:0;left:0;right:0;padding:1.5rem;background:linear-gradient(transparent,rgba(0,0,0,.7));display:flex;flex-direction:column}
.showcase-title{color:#fff;font-size:1.25rem;font-weight:600}
.showcase-desc{color:rgba(255,255,255,.8);font-size:.9rem;margin-top:.25rem}
.video-play-btn{position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);font-size:4rem;color:#fff;opacity:.9;transition:all .3s;text-shadow:0 4px 20px rgba(0,0,0,.5)}
.showcase-card:hover .video-play-btn{opacity:1;transform:translate(-50%,-50%) scale(1.1)}
.video-modal{position:fixed;inset:0;z-index:2000;background:rgba(0,0,0,.9);display:flex;align-items:center;justify-content:center}
.video-modal-content{position:relative;max-width:90vw;max-height:90vh}
.modal-video{max-width:100%;max-height:85vh;border-radius:12px}
.video-close-btn{position:absolute;top:-40px;right:0;background:none;border:none;color:#fff;font-size:2rem;cursor:pointer;transition:transform .3s}
.video-close-btn:hover{transform:scale(1.2)}
.experience-section{padding:80px 0;background:#fff}
.experience-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:2rem}
.experience-card{background:#fff;border:2px solid #eee;border-radius:16px;padding:2rem;text-align:center;cursor:pointer;transition:all .3s}
.experience-card:hover{border-color:#2A9D8F;transform:translateY(-5px);box-shadow:0 15px 40px rgba(42,157,143,.15)}
.exp-icon{font-size:3rem;margin-bottom:1rem}
.exp-title{font-size:1.25rem;font-weight:600;color:#2C3E50;margin-bottom:.5rem}
.exp-desc{color:#666;font-size:.95rem;margin-bottom:1rem}
.exp-action{color:#2A9D8F;font-weight:600}
.features-section{padding:80px 0;background:#F5F7FA}
.features-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:2rem}
.feature-card{background:#fff;border-radius:16px;padding:2rem;text-align:center;box-shadow:0 5px 20px rgba(0,0,0,.05);transition:all .3s}
.feature-card:hover{transform:translateY(-5px);box-shadow:0 15px 40px rgba(0,0,0,.1)}
.feature-icon{font-size:2.5rem;margin-bottom:1rem}
.feature-title{font-size:1.25rem;font-weight:600;color:#2C3E50;margin-bottom:.5rem}
.feature-desc{color:#666;font-size:.95rem;line-height:1.6}
.highlights-section{padding:80px 0;background:#fff}
.highlights-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:2rem}
.highlight-card{background:#fff;border:1px solid #eee;border-radius:16px;padding:2rem;transition:all .3s}
.highlight-card:hover{box-shadow:0 15px 40px rgba(0,0,0,.1);transform:translateY(-5px)}
.highlight-badge{display:inline-block;padding:.25rem .75rem;background:rgba(42,157,143,.1);color:#2A9D8F;border-radius:20px;font-size:.85rem;font-weight:600;margin-bottom:1rem}
.highlight-badge-ai{background:rgba(233,196,106,.2);color:#D4A853}
.highlight-badge-3d{background:rgba(69,123,157,.1);color:#457B9D}
.highlight-title{font-size:1.25rem;font-weight:600;color:#2C3E50;margin-bottom:.5rem}
.highlight-desc{color:#666;font-size:.95rem;line-height:1.6}
.scenarios-section{padding:80px 0;background:#F5F7FA}
.scenarios-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:1.5rem}
.scenario-card{display:flex;align-items:center;gap:1.5rem;background:#fff;border-radius:16px;padding:1.5rem 2rem;box-shadow:0 5px 20px rgba(0,0,0,.05);transition:all .3s}
.scenario-card:hover{transform:translateX(10px);box-shadow:0 10px 30px rgba(0,0,0,.1)}
.scenario-icon{font-size:2.5rem}
.scenario-title{font-size:1.1rem;font-weight:600;color:#2C3E50;margin-bottom:.25rem}
.scenario-desc{color:#666;font-size:.9rem}
.model-section{padding:80px 0;background:#fff}
.model-chart-wrapper{background:#fff;border-radius:16px;padding:2rem;box-shadow:0 10px 30px rgba(0,0,0,.05)}
.model-chart{width:100%;height:400px}
.footer-cta{padding:100px 0;background:linear-gradient(135deg,#2A9D8F 0%,#457B9D 100%)}
.cta-title{font-size:2.5rem;font-weight:700;color:#fff;margin-bottom:1rem}
.cta-desc{font-size:1.1rem;color:rgba(255,255,255,.9);margin-bottom:2rem}
.cta-buttons{display:flex;gap:1rem;justify-content:center}
.cta-btn-white{background:#fff;color:#2A9D8F;border-color:#fff}
.cta-btn-white:hover{background:#f0f0f0}
.cta-btn-outline-white{background:transparent;color:#fff;border-color:rgba(255,255,255,.5)}
.cta-btn-outline-white:hover{background:rgba(255,255,255,.1);border-color:#fff}
.reveal{opacity:0;transform:translateY(30px);transition:opacity .4s ease, transform .4s ease;will-change:opacity,transform}
.reveal.revealed{opacity:1;transform:translateY(0)}
@media(max-width:1024px){.features-grid,.highlights-grid,.experience-grid,.showcase-grid{grid-template-columns:repeat(2,1fr)}}
@media(max-width:768px){.nav-menu{display:none}.hero-title{font-size:2.5rem}.stats-grid{grid-template-columns:1fr}.stats-container{bottom:-180px}.showcase-section{padding-top:220px}.features-grid,.highlights-grid,.experience-grid,.showcase-grid,.scenarios-grid{grid-template-columns:1fr}.hero-cta,.cta-buttons{flex-direction:column}}
</style>
