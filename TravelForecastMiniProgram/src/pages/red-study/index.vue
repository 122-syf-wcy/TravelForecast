<template>
  <view class="page">
    <!-- 顶部 Banner -->
    <view class="hero">
      <image class="hero-bg" src="https://images.unsplash.com/photo-1517299321609-52687d1bc55a?w=800&q=80" mode="aspectFill" />
      <view class="hero-mask" />
      <view class="hero-content">
        <text class="hero-title">红色研学之旅</text>
        <text class="hero-sub">追寻三线记忆，传承红色基因</text>
        <view class="hero-stats">
          <view class="hs-item">
            <text class="hs-num">{{ bases.length }}</text>
            <text class="hs-lb">研学基地</text>
          </view>
          <view class="hs-divider" />
          <view class="hs-item">
            <text class="hs-num">{{ quizTotal }}</text>
            <text class="hs-lb">研学题目</text>
          </view>
          <view class="hs-divider" />
          <view class="hs-item">
            <text class="hs-num">{{ correctCount }}</text>
            <text class="hs-lb">已答对</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Tab 切换 -->
    <view class="tabs">
      <view class="tab" :class="{ 'tab-on': curTab === 0 }" @tap="curTab = 0">
        <text class="tab-t">研学基地</text>
      </view>
      <view class="tab" :class="{ 'tab-on': curTab === 1 }" @tap="curTab = 1">
        <text class="tab-t">研学答题</text>
      </view>
      <view class="tab" :class="{ 'tab-on': curTab === 2 }" @tap="curTab = 2">
        <text class="tab-t">研学护照</text>
      </view>
    </view>

    <!-- 研学基地列表 -->
    <view class="list" v-if="curTab === 0">
      <view class="item" v-for="(item, i) in bases" :key="i" @tap="goDetail(item)">
        <image class="item-img" :src="item.img" mode="aspectFill" />
        <view class="item-info">
          <text class="item-title">{{ item.title }}</text>
          <text class="item-desc">{{ item.desc }}</text>
          <view class="item-tags">
            <text class="tag">研学基地</text>
            <text class="tag tag-loc">{{ item.loc }}</text>
          </view>
        </view>
      </view>
      <view class="empty" v-if="bases.length === 0">
        <text class="empty-t">暂无研学基地数据</text>
      </view>
    </view>

    <!-- 研学答题 -->
    <view class="quiz-sec" v-if="curTab === 1">
      <view class="quiz-progress">
        <text class="qp-t">第 {{ quizIndex + 1 }} / {{ quizList.length }} 题</text>
        <view class="qp-bar">
          <view class="qp-fill" :style="{ width: ((quizIndex + 1) / quizList.length * 100) + '%' }" />
        </view>
      </view>

      <view class="quiz-card" v-if="currentQuiz">
        <view class="qc-spot">
          <text class="qc-spot-t">{{ currentQuiz.spot }}</text>
          <text class="qc-diff">{{ currentQuiz.difficulty }}</text>
        </view>
        <text class="qc-question">{{ currentQuiz.question }}</text>
        <view class="qc-opts">
          <view class="qc-opt" v-for="(opt, idx) in currentQuiz.options" :key="idx"
            :class="{
              'opt-ok': quizAnswered && idx === currentQuiz.answer,
              'opt-err': quizAnswered && quizPicked === idx && idx !== currentQuiz.answer,
              'opt-disabled': quizAnswered
            }"
            @tap="pickAnswer(idx)">
            <view class="opt-letter">
              <text class="opt-letter-t">{{ ['A','B','C','D'][idx] }}</text>
            </view>
            <text class="opt-text">{{ opt }}</text>
          </view>
        </view>

        <!-- 答题结果 -->
        <view class="qc-result" v-if="quizAnswered">
          <view class="qr-badge" :class="quizCorrect ? 'qr-ok' : 'qr-err'">
            <text class="qr-t">{{ quizCorrect ? '✓ 回答正确！+10 黔豆' : '✗ 回答错误' }}</text>
          </view>
          <text class="qr-explain">{{ currentQuiz.explanation }}</text>
          <view class="qr-next" @tap="nextQuiz">
            <text class="qr-next-t">{{ quizIndex < quizList.length - 1 ? '下一题' : '完成答题' }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 研学护照 -->
    <view class="passport-sec" v-if="curTab === 2">
      <view class="pp-header">
        <view class="pp-avatar">
          <text class="pp-av-t">游</text>
        </view>
        <view class="pp-info">
          <text class="pp-name">我的研学护照</text>
          <text class="pp-points">黔豆：{{ passportData.totalPoints }} · 打卡：{{ passportData.checkedSpots }} 个景区</text>
        </view>
      </view>

      <view class="pp-badges">
        <text class="pp-sec-title">成就徽章</text>
        <view class="badge-grid">
          <view class="badge" v-for="(b, i) in passportData.badges" :key="i" :class="{ 'badge-off': !b.unlocked }">
            <view class="badge-icon" :style="{ background: b.unlocked ? b.bg : '#ccc' }">
              <text class="badge-char">{{ b.char }}</text>
            </view>
            <text class="badge-name">{{ b.name }}</text>
            <text class="badge-desc">{{ b.desc }}</text>
            <text class="badge-st">{{ b.unlocked ? '已解锁' : '未解锁' }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchQuizList, fetchPassport } from '@/api/study'
import { fetchScenicSpots } from '@/api/home'
import { resolveAssetUrl } from '@/utils/url'

const curTab = ref(0)
const correctCount = ref(0)

// ---- 研学基地 ----
const bases = ref([
  { id: 1, title: '贵州三线建设博物馆', desc: '全国首家三线建设主题博物馆，了解20世纪60-70年代三线建设伟大历程', loc: '钟山区', img: 'https://images.unsplash.com/photo-1517299321609-52687d1bc55a?w=400&q=80' },
  { id: 2, title: '水城古镇三线记忆', desc: '600多年历史古镇里的红色印记，历史与现代的交融', loc: '水城区', img: 'https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=400&q=80' },
  { id: 3, title: '六枝特区三线遗址', desc: '探访老厂房，重温那段激情燃烧的岁月', loc: '六枝特区', img: 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=400&q=80' }
])

const loadBases = async () => {
  try {
    const spots = await fetchScenicSpots()
    if (Array.isArray(spots) && spots.length > 0) {
      const studySpots = spots.filter(s => {
        const tags = Array.isArray(s.tags) ? s.tags.join(' ') : ''
        return (s.category && s.category.includes('研学')) || tags.includes('研学') || tags.includes('红色')
      })
      if (studySpots.length > 0) {
        bases.value = studySpots.map(s => ({
          id: s.id, title: s.name || '研学基地',
          desc: s.description || s.address || '',
          loc: s.city || s.address || '六盘水',
          img: resolveAssetUrl(s.imageUrl) || 'https://images.unsplash.com/photo-1517299321609-52687d1bc55a?w=400&q=80'
        }))
      }
    }
  } catch (e) { /* 保留默认数据 */ }
}

// ---- 研学答题 ----
const quizList = ref([])
const quizIndex = ref(0)
const quizAnswered = ref(false)
const quizPicked = ref(-1)
const quizCorrect = ref(false)
const quizTotal = computed(() => quizList.value.length)
const currentQuiz = computed(() => quizList.value[quizIndex.value] || null)

const loadQuiz = async () => {
  try {
    const data = await fetchQuizList()
    quizList.value = Array.isArray(data) ? data : []
  } catch (e) { /* 保留空 */ }
}

const pickAnswer = (idx) => {
  if (quizAnswered.value) return
  quizPicked.value = idx
  quizAnswered.value = true
  quizCorrect.value = idx === currentQuiz.value.answer
  if (quizCorrect.value) correctCount.value++
}

const nextQuiz = () => {
  if (quizIndex.value < quizList.value.length - 1) {
    quizIndex.value++
    quizAnswered.value = false
    quizPicked.value = -1
  } else {
    uni.showToast({ title: `答题完成！答对 ${correctCount.value} 题`, icon: 'none' })
  }
}

// ---- 研学护照 ----
const passportData = ref({ badges: [], totalPoints: 0, checkedSpots: 0 })

const loadPassport = async () => {
  try {
    passportData.value = await fetchPassport()
  } catch (e) { /* 保留默认 */ }
}

const goDetail = (item) => {
  uni.navigateTo({ url: `/pages/spot/detail?id=${item.id}&type=red` })
}

onMounted(() => {
  loadBases()
  loadQuiz()
  loadPassport()
})
</script>

<style lang="scss">
.page { min-height: 100vh; background: #F2F5F8; }

/* Hero */
.hero { height: 220px; position: relative; overflow: hidden; }
.hero-bg { width: 100%; height: 100%; }
.hero-mask { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: linear-gradient(to top, rgba(0,0,0,0.7), rgba(0,0,0,0.2)); }
.hero-content { position: absolute; bottom: 0; left: 0; width: 100%; padding: 16px 20px; box-sizing: border-box; }
.hero-title { display: block; font-size: 24px; font-weight: 800; color: #fff; margin-bottom: 4px; }
.hero-sub { display: block; font-size: 13px; color: rgba(255,255,255,0.8); margin-bottom: 12px; }
.hero-stats { display: flex; align-items: center; }
.hs-item { display: flex; flex-direction: column; align-items: center; }
.hs-num { font-size: 20px; font-weight: 800; color: #fff; }
.hs-lb { font-size: 10px; color: rgba(255,255,255,0.7); margin-top: 2px; }
.hs-divider { width: 1px; height: 24px; background: rgba(255,255,255,0.3); margin: 0 20px; }

/* Tabs */
.tabs { display: flex; background: #fff; margin: 0 16px; border-radius: 12px; padding: 4px; margin-top: -16px; position: relative; z-index: 10; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.tab { flex: 1; height: 36px; display: flex; align-items: center; justify-content: center; border-radius: 8px; }
.tab-on { background: #2A9D8F; }
.tab-t { font-size: 13px; color: #666; font-weight: 500; }
.tab-on .tab-t { color: #fff; font-weight: 600; }

/* 基地列表 */
.list { padding: 16px; }
.item { background: #fff; border-radius: 12px; overflow: hidden; margin-bottom: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
.item-img { width: 100%; height: 160px; }
.item-info { padding: 12px 16px; }
.item-title { font-size: 17px; font-weight: 700; color: #333; margin-bottom: 6px; display: block; }
.item-desc { font-size: 13px; color: #666; margin-bottom: 10px; display: block; line-height: 1.5; }
.item-tags { display: flex; gap: 8px; }
.tag { font-size: 10px; padding: 2px 8px; background: #ffe8e8; color: #e74c3c; border-radius: 4px; }
.tag-loc { background: #f0f0f0; color: #666; }
.empty { text-align: center; margin-top: 40px; }
.empty-t { font-size: 14px; color: #999; }

/* 答题 */
.quiz-sec { padding: 16px; }
.quiz-progress { margin-bottom: 16px; }
.qp-t { font-size: 13px; color: #666; margin-bottom: 8px; display: block; }
.qp-bar { height: 6px; background: #e0e0e0; border-radius: 3px; overflow: hidden; }
.qp-fill { height: 100%; background: linear-gradient(90deg, #2A9D8F, #1A6B5A); border-radius: 3px; transition: width 0.3s; }

.quiz-card { background: #fff; border-radius: 16px; padding: 20px; box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
.qc-spot { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; }
.qc-spot-t { font-size: 12px; color: #e74c3c; background: #ffe8e8; padding: 2px 8px; border-radius: 4px; }
.qc-diff { font-size: 11px; color: #999; }
.qc-question { display: block; font-size: 16px; font-weight: 700; color: #1A1A2E; margin-bottom: 18px; line-height: 1.5; }

.qc-opts {}
.qc-opt {
  display: flex; align-items: center; padding: 12px 14px;
  border: 2px solid #eee; border-radius: 10px; margin-bottom: 10px;
}
.opt-disabled { pointer-events: none; }
.opt-letter { width: 28px; height: 28px; border-radius: 50%; background: #f0f0f0; display: flex; align-items: center; justify-content: center; margin-right: 12px; flex-shrink: 0; }
.opt-letter-t { font-size: 13px; font-weight: 700; color: #666; }
.opt-text { font-size: 14px; color: #333; }
.opt-ok { border-color: #07c160 !important; background: rgba(7,193,96,0.06); }
.opt-ok .opt-letter { background: #07c160; }
.opt-ok .opt-letter-t { color: #fff; }
.opt-err { border-color: #ee0a24 !important; background: rgba(238,10,36,0.04); }
.opt-err .opt-letter { background: #ee0a24; }
.opt-err .opt-letter-t { color: #fff; }

.qc-result { margin-top: 16px; }
.qr-badge { padding: 10px; border-radius: 10px; text-align: center; margin-bottom: 10px; }
.qr-ok { background: rgba(7,193,96,0.08); }
.qr-err { background: rgba(238,10,36,0.06); }
.qr-t { font-size: 14px; font-weight: 600; }
.qr-ok .qr-t { color: #07c160; }
.qr-err .qr-t { color: #ee0a24; }
.qr-explain { display: block; font-size: 13px; color: #666; line-height: 1.6; margin-bottom: 14px; padding: 10px; background: #f9f9f9; border-radius: 8px; }
.qr-next { padding: 12px; background: #2A9D8F; border-radius: 10px; text-align: center; }
.qr-next-t { font-size: 14px; font-weight: 600; color: #fff; }

/* 研学护照 */
.passport-sec { padding: 16px; }
.pp-header { display: flex; align-items: center; background: linear-gradient(135deg, #2A9D8F, #1A6B5A); padding: 20px; border-radius: 16px; margin-bottom: 16px; }
.pp-avatar { width: 50px; height: 50px; border-radius: 50%; background: rgba(255,255,255,0.2); display: flex; align-items: center; justify-content: center; margin-right: 14px; }
.pp-av-t { font-size: 22px; font-weight: 800; color: #fff; }
.pp-info { flex: 1; }
.pp-name { display: block; font-size: 18px; font-weight: 700; color: #fff; margin-bottom: 4px; }
.pp-points { font-size: 12px; color: rgba(255,255,255,0.8); }

.pp-badges { background: #fff; border-radius: 16px; padding: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.pp-sec-title { display: block; font-size: 15px; font-weight: 700; color: #1A1A2E; margin-bottom: 14px; }
.badge-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.badge { display: flex; flex-direction: column; align-items: center; text-align: center; }
.badge-off { opacity: 0.4; }
.badge-icon { width: 50px; height: 50px; border-radius: 14px; display: flex; align-items: center; justify-content: center; margin-bottom: 6px; box-shadow: 0 3px 8px rgba(0,0,0,0.1); }
.badge-char { font-size: 20px; font-weight: 800; color: #fff; }
.badge-name { font-size: 12px; font-weight: 600; color: #333; margin-bottom: 2px; }
.badge-desc { font-size: 10px; color: #999; margin-bottom: 2px; }
.badge-st { font-size: 10px; color: #bbb; }
</style>
