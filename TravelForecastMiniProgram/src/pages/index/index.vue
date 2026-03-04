<template>
  <view class="page">
    <!-- 沉浸头部 -->
    <view class="hero" :style="{ height: heroH + 'px' }">
      <!-- 轮播图 -->
      <swiper class="hero-swiper" :autoplay="true" :interval="4000" :duration="600"
        circular :indicator-dots="false" @change="onSwiperChange">
        <swiper-item v-for="(b, i) in bannerList" :key="i" @tap="onBannerTap(b)">
          <image class="hero-bg" :src="b.image" mode="aspectFill" />
        </swiper-item>
      </swiper>
      <view class="hero-mask" />

      <!-- 轮播指示器 -->
      <view class="swiper-dots">
        <view class="swiper-dot" v-for="(b, i) in bannerList" :key="i"
          :class="{ 'dot-active': swiperCur === i }" />
      </view>

      <!-- 导航栏 -->
      <view class="navbar" :style="{ paddingTop: stBar + 'px' }">
        <view class="nav-loc">
          <view class="loc-dot" />
          <text class="loc-city">六盘水</text>
          <text class="loc-temp">{{ temperature }}</text>
        </view>
        <view class="nav-search" @tap="onSearch">
          <view class="s-icon">
            <view class="s-circle" />
            <view class="s-handle" />
          </view>
          <text class="s-text">搜索景区 / 美食</text>
        </view>
      </view>

      <!-- Slogan -->
      <view class="slogan">
        <text class="slogan-l1">走遍大地神州</text>
        <text class="slogan-l2">最美多彩贵州</text>
        <view class="slogan-stamp">贵州</view>
        <text class="slogan-en">Cool City · Liupanshui</text>
      </view>
    </view>

    <!-- 金刚区 -->
    <view class="menu-bar">
      <view class="mi" v-for="(m, i) in menuList" :key="i" @tap="onMenu(m)">
        <view class="mi-icon" :style="{ background: m.bg }">
          <text class="mi-char">{{ m.char }}</text>
        </view>
        <text class="mi-name">{{ m.name }}</text>
      </view>
    </view>

    <!-- 公告 -->
    <view class="notice">
      <view class="notice-tag">公告</view>
      <text class="notice-txt">{{ noticeText }}</text>
    </view>

    <!-- 功能卡片 -->
    <view class="cards">
      <view class="card-big" @tap="onAiGuide">
        <view class="cb-badge">AI</view>
        <text class="cb-title">AI 数字伴游</text>
        <text class="cb-sub">让黔小游带你玩转凉都</text>
        <view class="cb-btn">立即体验</view>
        <view class="cb-avatar">
          <text class="cb-av-text">游</text>
        </view>
        <view class="cb-deco1" />
        <view class="cb-deco2" />
      </view>
      <view class="card-col">
        <view class="card-sm card-warm" @tap="onSeason">
          <view class="cs-body">
            <text class="cs-tag">PLAY</text>
            <text class="cs-title">当季必玩</text>
            <text class="cs-desc">冬日滑雪正当时</text>
          </view>
          <view class="cs-icon snow-icon">
            <text class="cs-char">雪</text>
          </view>
        </view>
        <view class="card-sm card-red" @tap="onRedStudy">
          <view class="cs-body">
            <text class="cs-tag red">MEMORY</text>
            <text class="cs-title">红色研学</text>
            <text class="cs-desc">探访三线建设历史</text>
          </view>
          <view class="cs-icon red-icon">
            <text class="cs-char">红</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 分类 -->
    <view class="cats">
      <view class="cat" v-for="(c, i) in catList" :key="i" @tap="onCat(c)">
        <view class="cat-icon" :style="{ background: c.bg }">
          <text class="cat-char">{{ c.char }}</text>
        </view>
        <text class="cat-name">{{ c.name }}</text>
      </view>
    </view>

    <!-- 热门景区 -->
    <view class="rank-sec">
      <view class="rank-hd">
        <view class="rank-hd-l">
          <text class="rank-title">热门景区</text>
          <text class="rank-sub">这些景点大家都在打卡</text>
        </view>
        <view class="rank-more" @tap="onAllSpots">
          <text class="rank-more-t">更多</text>
        </view>
      </view>
      <scroll-view scroll-x class="rank-list" :show-scrollbar="false">
        <view class="rk" v-for="(r, i) in rankList" :key="i" @tap="onSpot(r)">
          <image class="rk-img" :src="r.img" mode="aspectFill" />
          <view class="rk-mask" />
          <view class="rk-no" :class="'rk-c' + (i+1)">
            <text class="rk-no-t">No.{{ i+1 }}</text>
          </view>
          <view class="rk-info">
            <text class="rk-name">{{ r.title }}</text>
            <text class="rk-loc">{{ r.loc }}</text>
          </view>
          <view class="rk-tag" v-if="r.tag">
            <text class="rk-tag-t">{{ r.tag }}</text>
          </view>
        </view>
        <view style="width:16px;flex-shrink:0;" />
      </scroll-view>
    </view>

    <!-- AI 悬浮球 -->
    <view class="ai-fab" @tap="onAiTap">
      <view class="ai-fab-bubble" v-if="showBubble">
        <text>{{ bubbleMsg }}</text>
      </view>
      <view class="ai-fab-btn">
        <image class="ai-fab-avatar" src="/static/dh-avatar.png" mode="aspectFill" />
      </view>
    </view>

    <view style="height:100px;" />
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchHomeBanners, fetchHotNews, fetchScenicSpots } from '@/api/home'
import { resolveAssetUrl } from '@/utils/url'

const stBar = ref(20)
const heroH = ref(360)
const showBubble = ref(true)
const swiperCur = ref(0)
const bubbleMsg = ref('嗨~我是黔小游')
const temperature = ref('')

const getSeasonTemp = () => {
  const m = new Date().getMonth() + 1
  const temps = { 1: '4°C', 2: '6°C', 3: '11°C', 4: '16°C', 5: '19°C', 6: '22°C', 7: '24°C', 8: '23°C', 9: '20°C', 10: '15°C', 11: '10°C', 12: '5°C' }
  return temps[m] || '19°C'
}
const noticeText = ref('六盘水梅花山滑雪场已开放，春季赏花活动火热进行中！')

const bannerList = ref([
  { image: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&q=80', title: '凉都六盘水', link: '' },
  { image: 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&q=80', title: '梅花山景区', link: '' },
  { image: 'https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=800&q=80', title: '乌蒙大草原', link: '' }
])

const menuList = [
  { name: '数字导游', char: '导', bg: 'linear-gradient(135deg, #f8a5c2, #f78fb3)' },
  { name: '红色研学', char: '红', bg: 'linear-gradient(135deg, #ff6b6b, #ee5a24)' },
  { name: '景区预约', char: '票', bg: 'linear-gradient(135deg, #ffd32a, #f6b93b)' },
  { name: 'AI 伴游',  char: 'AI', bg: 'linear-gradient(135deg, #74b9ff, #0984e3)' },
  { name: '文创商城', char: '创', bg: 'linear-gradient(135deg, #55efc4, #00b894)' }
]

const catList = [
  { name: '交通出行', char: '行', bg: 'linear-gradient(135deg, #dfe6e9, #b2bec3)' },
  { name: '酒店',     char: '宿', bg: 'linear-gradient(135deg, #81ecec, #00cec9)' },
  { name: '美食',     char: '食', bg: 'linear-gradient(135deg, #ffeaa7, #fdcb6e)' },
  { name: '非遗体验', char: '遗', bg: 'linear-gradient(135deg, #a29bfe, #6c5ce7)' },
  { name: '民宿',     char: '居', bg: 'linear-gradient(135deg, #fd79a8, #e84393)' },
  { name: '门票',     char: '门', bg: 'linear-gradient(135deg, #55efc4, #00b894)' },
  { name: '攻略',     char: '略', bg: 'linear-gradient(135deg, #74b9ff, #0984e3)' },
  { name: '特色活动', char: '动', bg: 'linear-gradient(135deg, #fab1a0, #e17055)' },
  { name: '优惠福利', char: '惠', bg: 'linear-gradient(135deg, #ffeaa7, #f39c12)' },
  { name: '更多服务', char: '+',  bg: 'linear-gradient(135deg, #dfe6e9, #b2bec3)' }
]

const rankList = ref([
  { id: '1', title: '乌蒙大草原', loc: '盘州市', tag: '热门',
    img: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&q=80' },
  { id: '2', title: '梅花山景区', loc: '钟山区', tag: '必玩',
    img: 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=400&q=80' },
  { id: '3', title: '三线建设博物馆', loc: '钟山区', tag: '研学',
    img: 'https://images.unsplash.com/photo-1517299321609-52687d1bc55a?w=400&q=80' },
  { id: '4', title: '北盘江', loc: '水城区', tag: null,
    img: 'https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=400&q=80' }
])

const loadHomeData = async () => {
  try {
    const [banners, hotNews, spots] = await Promise.all([
      fetchHomeBanners().catch(() => []),
      fetchHotNews(1).catch(() => null),
      fetchScenicSpots().catch(() => [])
    ])

    if (Array.isArray(banners) && banners.length > 0) {
      bannerList.value = banners.map(b => ({
        image: resolveAssetUrl(b.image) || b.image,
        title: b.title || '',
        link: b.link || ''
      }))
    }

    const hotList = hotNews && Array.isArray(hotNews.records) ? hotNews.records : []
    if (hotList.length > 0 && hotList[0].title) {
      noticeText.value = hotList[0].title
    }

    if (Array.isArray(spots) && spots.length > 0) {
      rankList.value = spots.slice(0, 4).map((spot) => {
        const img = resolveAssetUrl(spot.imageUrl)
        return {
          id: spot.id || spot.spotCode || spot.name,
          title: spot.name || '热门景区',
          loc: spot.city || spot.address || '六盘水',
          tag: (spot.tags && spot.tags[0]) || spot.level || null,
          img: img || heroBg.value
        }
      })
    }
  } catch (err) {
    // 保留默认展示，避免影响首屏
  }
}

onMounted(() => {
  const info = uni.getWindowInfo()
  stBar.value = info.statusBarHeight || 20
  heroH.value = Math.floor(info.windowHeight * 0.44)
  temperature.value = getSeasonTemp()
  setTimeout(() => { showBubble.value = false }, 4000)
  loadHomeData()
})

const onSwiperChange = (e) => { swiperCur.value = e.detail.current }
const onBannerTap = (b) => {
  if (b.link) {
    if (b.link.startsWith('/pages/')) {
      uni.navigateTo({ url: b.link })
    } else if (b.link.startsWith('http')) {
      // webview or copy link
      uni.setClipboardData({ data: b.link })
    }
  }
}

const onSearch = () => uni.navigateTo({ url: '/pages/search/index' })
const onMenu = (m) => {
  if (m.name === '数字导游' || m.name === 'AI 伴游') {
    uni.navigateTo({ url: '/pages/digital-human/index' })
  } else if (m.name === '红色研学') {
    uni.navigateTo({ url: '/pages/red-study/index' })
  } else if (m.name === '文创商城') {
    uni.switchTab({ url: '/pages/shop/index' })
  } else if (m.name === '景区预约') {
    uni.navigateTo({ url: '/pages/life-service/index?category=' + encodeURIComponent('门票') })
  } else {
    uni.navigateTo({ url: `/pages/life-service/index?category=${encodeURIComponent(m.name)}` })
  }
}
const onCat = (c) => {
  if (!c || !c.name) return
  uni.navigateTo({ url: `/pages/life-service/index?category=${encodeURIComponent(c.name)}` })
}
const onAiGuide = () => uni.navigateTo({ url: '/pages/digital-human/index' })
const onSeason = () => uni.navigateTo({ url: '/pages/search/index' })
const onRedStudy = () => uni.navigateTo({ url: '/pages/red-study/index' })
const onAllSpots = () => uni.navigateTo({ url: '/pages/search/index' })
const onSpot = (r) => {
  if (r && r.id) {
    uni.navigateTo({ url: `/pages/spot/detail?id=${encodeURIComponent(r.id)}` })
    return
  }
  uni.showToast({ title: r.title || '景区详情', icon: 'none' })
}
const onAiTap = () => {
  uni.navigateTo({ url: '/pages/digital-human/index' })
}
</script>

<style lang="scss">
@import "@/uni.scss";

.page {
  min-height: 100vh;
  background: #F2F5F8;
}

/* ===== hero ===== */
.hero {
  position: relative;
  width: 100%;
  overflow: hidden;
  border-radius: 0 0 24px 24px;
}
.hero-swiper {
  width: 100%; height: 100%;
  position: absolute;
  top: 0; left: 0;
}
.hero-bg {
  width: 100%; height: 100%;
}
/* 轮播指示器 */
.swiper-dots {
  position: absolute;
  bottom: 38px; right: 20px;
  z-index: 15;
  display: flex;
  gap: 6px;
}
.swiper-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: rgba(255,255,255,0.45);
  transition: all 0.3s;
}
.swiper-dot.dot-active {
  width: 18px;
  border-radius: 3px;
  background: #fff;
}
.hero-mask {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  background: linear-gradient(180deg,
    rgba(0,20,50,0.5) 0%,
    rgba(0,0,0,0.05) 50%,
    rgba(255,255,255,0.08) 100%);
}

/* navbar */
.navbar {
  position: absolute;
  top: 0; left: 0; right: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  padding: 8px 16px;
  box-sizing: border-box;
}
.nav-loc {
  display: flex;
  align-items: center;
  margin-right: 10px;
  flex-shrink: 0;
}
.loc-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #55efc4;
  margin-right: 5px;
  box-shadow: 0 0 6px rgba(85,239,196,0.6);
}
.loc-city {
  font-size: 15px;
  color: #fff;
  font-weight: 700;
  margin-right: 6px;
}
.loc-temp {
  font-size: 12px;
  color: rgba(255,255,255,0.85);
  background: rgba(255,255,255,0.18);
  padding: 2px 8px;
  border-radius: 20px;
}
.nav-search {
  flex: 1;
  height: 32px;
  background: rgba(255,255,255,0.18);
  border: 1px solid rgba(255,255,255,0.22);
  border-radius: 20px;
  display: flex;
  align-items: center;
  padding: 0 12px;
}
/* CSS search icon */
.s-icon {
  width: 14px; height: 14px;
  position: relative;
  margin-right: 6px;
}
.s-circle {
  width: 10px; height: 10px;
  border: 2px solid rgba(255,255,255,0.8);
  border-radius: 50%;
  position: absolute;
  top: 0; left: 0;
}
.s-handle {
  width: 5px; height: 2px;
  background: rgba(255,255,255,0.8);
  position: absolute;
  bottom: 0; right: 0;
  transform: rotate(45deg);
  border-radius: 1px;
}
.s-text {
  font-size: 13px;
  color: rgba(255,255,255,0.7);
}

/* Slogan */
.slogan {
  position: absolute;
  left: 24px;
  bottom: 64px;
  z-index: 10;
}
.slogan-l1, .slogan-l2 {
  display: block;
  font-size: 30px;
  font-weight: 900;
  color: #fff;
  text-shadow: 0 3px 12px rgba(0,0,0,0.35);
  letter-spacing: 3px;
  line-height: 1.35;
}
.slogan-stamp {
  position: absolute;
  top: 2px; right: -48px;
  width: 38px; height: 38px;
  background: rgba(230,60,60,0.85);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transform: rotate(12deg);
  border: 2px solid rgba(255,255,255,0.5);
}
.slogan-en {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: rgba(255,255,255,0.8);
  letter-spacing: 1.5px;
}

/* ===== menu ===== */
.menu-bar {
  margin: -36px 16px 0;
  padding: 16px 6px 12px;
  background: rgba(255,255,255,0.88);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
  display: flex;
  justify-content: space-around;
  position: relative;
  z-index: 20;
}
.mi {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}
.mi-icon {
  width: 46px; height: 46px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.1);
  position: relative;
  overflow: hidden;
}
.mi-icon::after {
  content: '';
  position: absolute;
  top: -30%; left: -20%;
  width: 55%; height: 55%;
  background: rgba(255,255,255,0.3);
  border-radius: 50%;
}
.mi-char {
  font-size: 18px;
  font-weight: 800;
  color: #fff;
  position: relative;
  z-index: 2;
}
.mi-name {
  font-size: 11px;
  color: #333;
  font-weight: 600;
}

/* ===== notice ===== */
.notice {
  margin: 12px 16px 0;
  padding: 10px 14px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  display: flex;
  align-items: center;
}
.notice-tag {
  font-size: 10px;
  color: #fff;
  background: #2A9D8F;
  padding: 2px 8px;
  border-radius: 20px;
  margin-right: 10px;
  font-weight: 600;
  flex-shrink: 0;
}
.notice-txt {
  flex: 1;
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== cards ===== */
.cards {
  margin: 12px 16px 0;
  display: flex;
  gap: 10px;
}
.card-big {
  flex: 1;
  min-height: 192px;
  border-radius: 18px;
  background: linear-gradient(150deg, #1a6b5a 0%, #2A9D8F 50%, #3CB8A9 100%);
  padding: 16px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 6px 18px rgba(42,157,143,0.25);
}
.cb-badge {
  display: inline-block;
  font-size: 10px;
  font-weight: 800;
  color: #2A9D8F;
  background: rgba(255,255,255,0.92);
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 8px;
}
.cb-title {
  display: block;
  font-size: 17px;
  font-weight: 800;
  color: #fff;
  margin-bottom: 3px;
}
.cb-sub {
  display: block;
  font-size: 12px;
  color: rgba(255,255,255,0.8);
  margin-bottom: 14px;
}
.cb-btn {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  color: #2A9D8F;
  background: #fff;
  padding: 5px 14px;
  border-radius: 20px;
  position: relative;
  z-index: 2;
}
.cb-avatar {
  position: absolute;
  right: 10px; bottom: 10px;
  width: 60px; height: 60px;
  border-radius: 50%;
  background: rgba(255,255,255,0.18);
  display: flex;
  align-items: center;
  justify-content: center;
}
.cb-av-text {
  font-size: 28px;
  font-weight: 900;
  color: rgba(255,255,255,0.7);
}
.cb-deco1 {
  position: absolute;
  top: -18px; right: -18px;
  width: 70px; height: 70px;
  border-radius: 50%;
  background: rgba(255,255,255,0.07);
}
.cb-deco2 {
  position: absolute;
  bottom: -25px; left: -12px;
  width: 90px; height: 90px;
  border-radius: 50%;
  background: rgba(255,255,255,0.05);
}

.card-col {
  width: 45%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.card-sm {
  flex: 1;
  border-radius: 18px;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  overflow: hidden;
  box-shadow: 0 3px 12px rgba(0,0,0,0.05);
}
.card-warm {
  background: linear-gradient(135deg, #fffaf0 0%, #fff3d4 100%);
  border: 1px solid rgba(253,203,110,0.15);
}
.card-red {
  background: linear-gradient(135deg, #fff8f8 0%, #ffe8e8 100%);
  border: 1px solid rgba(255,107,107,0.12);
}
.cs-body {
  display: flex;
  flex-direction: column;
  flex: 1;
}
.cs-tag {
  font-size: 9px;
  font-weight: 800;
  color: #F39C12;
  letter-spacing: 1px;
  margin-bottom: 3px;
}
.cs-tag.red { color: #e74c3c; }
.cs-title {
  font-size: 14px;
  font-weight: 700;
  color: #1A1A2E;
  margin-bottom: 2px;
}
.cs-desc {
  font-size: 10px;
  color: #999;
}
.cs-icon {
  width: 38px; height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.snow-icon { background: linear-gradient(135deg, #dfe6e9, #74b9ff); }
.red-icon { background: linear-gradient(135deg, #fab1a0, #e74c3c); }
.cs-char {
  font-size: 16px;
  font-weight: 800;
  color: #fff;
}

/* ===== cats ===== */
.cats {
  margin: 12px 16px 0;
  padding: 14px 6px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 3px 12px rgba(0,0,0,0.04);
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  row-gap: 14px;
}
.cat {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.cat-icon {
  width: 40px; height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 5px;
  box-shadow: 0 3px 8px rgba(0,0,0,0.08);
  position: relative;
  overflow: hidden;
}
.cat-icon::after {
  content: '';
  position: absolute;
  top: -25%; left: -15%;
  width: 48%; height: 48%;
  background: rgba(255,255,255,0.35);
  border-radius: 50%;
}
.cat-char {
  font-size: 16px;
  font-weight: 800;
  color: #fff;
  position: relative;
  z-index: 2;
}
.cat-name {
  font-size: 11px;
  color: #444;
  font-weight: 500;
}

/* ===== rank ===== */
.rank-sec {
  margin-top: 18px;
}
.rank-hd {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
  margin-bottom: 12px;
}
.rank-hd-l {
  display: flex;
  flex-direction: column;
}
.rank-title {
  font-size: 19px;
  font-weight: 800;
  color: #1A1A2E;
}
.rank-sub {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}
.rank-more {
  padding: 4px 12px;
  background: #E0F2F1;
  border-radius: 20px;
}
.rank-more-t {
  font-size: 12px;
  color: #2A9D8F;
  font-weight: 500;
}
.rank-list {
  white-space: nowrap;
  padding-left: 16px;
}
.rk {
  display: inline-block;
  width: 150px;
  height: 200px;
  margin-right: 12px;
  border-radius: 18px;
  overflow: hidden;
  position: relative;
  background: #e0e0e0;
  box-shadow: 0 4px 14px rgba(0,0,0,0.1);
  vertical-align: top;
}
.rk-img {
  width: 100%; height: 100%;
  position: absolute;
  top: 0; left: 0;
}
.rk-mask {
  position: absolute;
  bottom: 0; left: 0;
  width: 100%; height: 55%;
  background: linear-gradient(to top, rgba(0,0,0,0.65) 0%, transparent 100%);
}
.rk-no {
  position: absolute;
  top: 8px; left: 8px;
  padding: 3px 8px;
  border-radius: 6px;
}
.rk-no-t {
  font-size: 10px;
  font-weight: 700;
  color: #fff;
}
.rk-c1 { background: linear-gradient(135deg, #FFD700, #FFA502); }
.rk-c2 { background: linear-gradient(135deg, #b2bec3, #636e72); }
.rk-c3 { background: linear-gradient(135deg, #CD7F32, #e17055); }
.rk-c4 { background: rgba(0,0,0,0.35); }
.rk-info {
  position: absolute;
  bottom: 10px; left: 10px; right: 10px;
  z-index: 2;
}
.rk-name {
  display: block;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  white-space: normal;
  line-height: 1.25;
  margin-bottom: 2px;
}
.rk-loc {
  font-size: 11px;
  color: rgba(255,255,255,0.75);
}
.rk-tag {
  position: absolute;
  top: 8px; right: 8px;
  background: #FF9F43;
  padding: 2px 7px;
  border-radius: 20px;
}
.rk-tag-t {
  font-size: 10px;
  color: #fff;
  font-weight: 600;
}
</style>
