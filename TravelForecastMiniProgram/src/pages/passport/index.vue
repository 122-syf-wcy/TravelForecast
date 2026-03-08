<template>
  <view class="pp-page">
    <!-- 护照封面头部 -->
    <view class="pp-cover">
      <view class="pp-cover-deco1" />
      <view class="pp-cover-deco2" />
      <view class="pp-cover-body">
        <view class="pp-user">
          <view class="pp-user-avatar">
            <text class="pp-user-emoji">📖</text>
          </view>
          <view class="pp-user-info">
            <text class="pp-user-name">{{ userName }}的研学护照</text>
            <text class="pp-user-id">编号：QXY-{{ userId }}</text>
          </view>
        </view>
        <view class="pp-stats">
          <view class="pp-stat">
            <text class="pp-stat-num">{{ passportData.checkedSpots }}</text>
            <text class="pp-stat-lb">打卡景区</text>
          </view>
          <view class="pp-stat-divider" />
          <view class="pp-stat">
            <text class="pp-stat-num">{{ unlockedCount }}</text>
            <text class="pp-stat-lb">徽章解锁</text>
          </view>
          <view class="pp-stat-divider" />
          <view class="pp-stat">
            <text class="pp-stat-num">{{ passportData.totalPoints }}</text>
            <text class="pp-stat-lb">黔豆积分</text>
          </view>
          <view class="pp-stat-divider" />
          <view class="pp-stat">
            <text class="pp-stat-num">{{ quizCorrect }}</text>
            <text class="pp-stat-lb">答对题目</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 总进度 -->
    <view class="pp-progress-sec">
      <view class="pp-progress-hd">
        <text class="pp-progress-title">研学进度</text>
        <text class="pp-progress-pct">{{ Math.round(unlockedCount / allBadges.length * 100) }}%</text>
      </view>
      <view class="pp-progress-bar">
        <view class="pp-progress-fill" :style="{ width: (unlockedCount / allBadges.length * 100) + '%' }" />
      </view>
      <text class="pp-progress-tip">完成所有徽章可获得「凉都研学大师」称号</text>
    </view>

    <!-- 成就徽章 -->
    <view class="pp-badge-sec">
      <text class="sec-title">成就徽章</text>
      <view class="badge-list">
        <view class="badge-card" v-for="(b, i) in allBadges" :key="i" :class="{ 'badge-locked': !b.unlocked }">
          <view class="bc-icon" :style="{ background: b.unlocked ? b.bg : '' }">
            <text class="bc-emoji">{{ b.emoji }}</text>
          </view>
          <view class="bc-info">
            <text class="bc-name">{{ b.name }}</text>
            <text class="bc-desc">{{ b.desc }}</text>
          </view>
          <view class="bc-status">
            <text class="bc-status-t" :class="b.unlocked ? 'bc-on' : 'bc-off'">{{ b.unlocked ? '✓ 已解锁' : '🔒 未解锁' }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 打卡足迹 -->
    <view class="pp-stamp-sec">
      <text class="sec-title">打卡足迹</text>
      <view class="stamp-timeline" v-if="stamps.length > 0">
        <view class="stamp-item" v-for="(s, i) in stamps" :key="i">
          <view class="stamp-dot" />
          <view class="stamp-line" v-if="i < stamps.length - 1" />
          <view class="stamp-card">
            <view class="sc-top">
              <text class="sc-name">{{ s.name }}</text>
              <text class="sc-time">{{ s.time }}</text>
            </view>
            <text class="sc-loc">{{ s.location }}</text>
            <view class="sc-stamp">
              <text class="sc-stamp-t">✓ 已打卡</text>
            </view>
          </view>
        </view>
      </view>
      <view class="stamp-empty" v-else>
        <text class="stamp-empty-emoji">📍</text>
        <text class="stamp-empty-t">还没有打卡记录</text>
        <text class="stamp-empty-sub">去景区扫码打卡，解锁更多徽章吧</text>
        <view class="stamp-empty-btn" @tap="goExplore">
          <text class="stamp-empty-btn-t">去探索</text>
        </view>
      </view>
    </view>

    <!-- 研学答题入口 -->
    <view class="pp-quiz-sec" @tap="goQuiz">
      <view class="quiz-entry">
        <view class="qe-left">
          <view class="qe-icon">
            <text class="qe-icon-t">📝</text>
          </view>
          <view class="qe-info">
            <text class="qe-title">研学答题挑战</text>
            <text class="qe-desc">答题赢黔豆，已答对 {{ quizCorrect }} 题</text>
          </view>
        </view>
        <text class="qe-arrow">></text>
      </view>
    </view>

    <view style="height:40px;" />
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchPassport } from '@/api/study'

const userName = ref('旅行者')
const userId = ref('000001')
const quizCorrect = ref(0)

const passportData = ref({ badges: [], totalPoints: 0, checkedSpots: 0 })

const allBadges = ref([
  { name: '初来乍到', emoji: '🎒', bg: 'linear-gradient(135deg, #74b9ff, #0984e3)', desc: '首次登录小程序', unlocked: true },
  { name: '三线学者', emoji: '🏭', bg: 'linear-gradient(135deg, #ff6b6b, #ee5a24)', desc: '参观三线建设博物馆', unlocked: false },
  { name: '美食猎人', emoji: '🍜', bg: 'linear-gradient(135deg, #ffd32a, #f6b93b)', desc: '打卡 3 个美食景点', unlocked: true },
  { name: '凉都达人', emoji: '⛰️', bg: 'linear-gradient(135deg, #55efc4, #00b894)', desc: '打卡 5 个景区', unlocked: false },
  { name: '文创大师', emoji: '🎨', bg: 'linear-gradient(135deg, #a29bfe, #6c5ce7)', desc: '购买 1 件文创商品', unlocked: false },
  { name: '答题学霸', emoji: '🎓', bg: 'linear-gradient(135deg, #fd79a8, #e84393)', desc: '答对 10 道研学题', unlocked: false },
  { name: '全景探索', emoji: '🗺️', bg: 'linear-gradient(135deg, #00cec9, #0984e3)', desc: '打卡全部景区', unlocked: false },
  { name: '凉都研学大师', emoji: '🏆', bg: 'linear-gradient(135deg, #FFD93D, #F39C12)', desc: '解锁全部徽章', unlocked: false }
])

const unlockedCount = computed(() => allBadges.value.filter(b => b.unlocked).length)

const stamps = ref([
  { name: '梅花山风景区', location: '六盘水市钟山区', time: '2025-12-20' },
  { name: '玉舍国家森林公园', location: '六盘水市水城区', time: '2025-12-18' }
])

onMounted(async () => {
  const u = uni.getStorageSync('userInfo')
  if (u) {
    userName.value = u.nickname || '旅行者'
    userId.value = String(u.userId || '000001').padStart(6, '0')
  }
  try {
    const uid = u?.userId
    if (uid) {
      const res = await fetchPassport(uid)
      if (res) {
        passportData.value = res
        if (Array.isArray(res.badges)) {
          allBadges.value = res.badges.map((b, i) => ({
            ...allBadges.value[i],
            ...b,
            emoji: b.emoji || allBadges.value[i]?.emoji || '🏅',
            unlocked: !!b.unlocked
          }))
        }
        if (Array.isArray(res.stamps)) {
          stamps.value = res.stamps
        }
        if (res.quizCorrect != null) {
          quizCorrect.value = res.quizCorrect
        }
      }
    }
  } catch (e) { /* 保留默认数据 */ }
})

const goExplore = () => uni.switchTab({ url: '/pages/guide/index' })
const goQuiz = () => uni.navigateTo({ url: '/pages/red-study/index' })
</script>

<style lang="scss">
.pp-page { min-height: 100vh; background: #F2F5F8; }

/* 护照封面 */
.pp-cover {
  background: linear-gradient(135deg, #2A9D8F 0%, #1A6B5A 60%, #145249 100%);
  padding: 0 0 24px;
  border-radius: 0 0 24px 24px;
  position: relative;
  overflow: hidden;
}
.pp-cover-deco1 { position: absolute; top: -30px; right: -20px; width: 120px; height: 120px; border-radius: 50%; background: rgba(255,255,255,0.06); }
.pp-cover-deco2 { position: absolute; bottom: -20px; left: -30px; width: 100px; height: 100px; border-radius: 50%; background: rgba(255,255,255,0.04); }
.pp-cover-body { position: relative; z-index: 2; padding-top: 60px; }

.pp-user { display: flex; align-items: center; padding: 0 20px; margin-bottom: 20px; }
.pp-user-avatar {
  width: 60px; height: 60px; border-radius: 16px;
  background: rgba(255,255,255,0.18);
  border: 2px solid rgba(255,255,255,0.35);
  display: flex; align-items: center; justify-content: center;
  margin-right: 14px;
}
.pp-user-emoji { font-size: 30px; }
.pp-user-info { flex: 1; }
.pp-user-name { display: block; font-size: 20px; font-weight: 800; color: #fff; margin-bottom: 4px; }
.pp-user-id { font-size: 12px; color: rgba(255,255,255,0.65); letter-spacing: 1px; }

.pp-stats {
  display: flex; justify-content: space-around; align-items: center;
  background: rgba(255,255,255,0.12); border-radius: 14px;
  margin: 0 16px; padding: 14px 0;
}
.pp-stat { display: flex; flex-direction: column; align-items: center; flex: 1; }
.pp-stat-num { font-size: 22px; font-weight: 800; color: #fff; line-height: 1.2; }
.pp-stat-lb { font-size: 10px; color: rgba(255,255,255,0.7); margin-top: 3px; }
.pp-stat-divider { width: 1px; height: 26px; background: rgba(255,255,255,0.2); }

/* 进度 */
.pp-progress-sec {
  margin: -12px 16px 0; background: #fff; border-radius: 16px;
  padding: 16px; box-shadow: 0 4px 16px rgba(0,0,0,0.06);
  position: relative; z-index: 10;
}
.pp-progress-hd { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.pp-progress-title { font-size: 14px; font-weight: 700; color: #1A1A2E; }
.pp-progress-pct { font-size: 14px; font-weight: 800; color: #2A9D8F; }
.pp-progress-bar { height: 8px; background: #f0f0f0; border-radius: 4px; overflow: hidden; }
.pp-progress-fill { height: 100%; background: linear-gradient(90deg, #2A9D8F, #55efc4); border-radius: 4px; transition: width 0.6s ease; min-width: 8px; }
.pp-progress-tip { font-size: 11px; color: #999; margin-top: 8px; display: block; }

/* 通用标题 */
.sec-title { display: block; font-size: 16px; font-weight: 700; color: #1A1A2E; margin-bottom: 14px; }

/* 徽章 */
.pp-badge-sec { margin: 14px 16px 0; background: #fff; border-radius: 16px; padding: 16px; box-shadow: 0 2px 10px rgba(0,0,0,0.04); }
.badge-list {}
.badge-card {
  display: flex; align-items: center; padding: 12px;
  background: #fafafa; border-radius: 12px; margin-bottom: 10px;
}
.badge-locked { opacity: 0.5; }
.badge-locked .bc-icon { background: #e0e0e0 !important; filter: grayscale(100%); }
.bc-icon {
  width: 46px; height: 46px; border-radius: 13px;
  display: flex; align-items: center; justify-content: center;
  margin-right: 12px; flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.bc-emoji { font-size: 22px; }
.bc-info { flex: 1; display: flex; flex-direction: column; }
.bc-name { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 3px; }
.bc-desc { font-size: 11px; color: #999; }
.bc-status { flex-shrink: 0; margin-left: 8px; }
.bc-status-t { font-size: 11px; font-weight: 500; }
.bc-on { color: #2A9D8F; }
.bc-off { color: #ccc; }

/* 打卡足迹 */
.pp-stamp-sec { margin: 14px 16px 0; background: #fff; border-radius: 16px; padding: 16px; box-shadow: 0 2px 10px rgba(0,0,0,0.04); }
.stamp-timeline { position: relative; padding-left: 20px; }
.stamp-item { position: relative; margin-bottom: 16px; }
.stamp-item:last-child { margin-bottom: 0; }
.stamp-dot {
  position: absolute; left: -20px; top: 8px;
  width: 10px; height: 10px; border-radius: 50%;
  background: #2A9D8F; border: 2px solid #fff;
  box-shadow: 0 0 0 2px #2A9D8F;
}
.stamp-line {
  position: absolute; left: -16px; top: 22px;
  width: 2px; bottom: -16px;
  background: #e0e0e0;
}
.stamp-card {
  background: #f9fafb; border-radius: 10px; padding: 12px;
  border-left: 3px solid #2A9D8F;
}
.sc-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.sc-name { font-size: 14px; font-weight: 600; color: #333; }
.sc-time { font-size: 11px; color: #999; }
.sc-loc { font-size: 12px; color: #888; margin-bottom: 6px; display: block; }
.sc-stamp { display: inline-flex; }
.sc-stamp-t { font-size: 10px; color: #2A9D8F; font-weight: 600; background: rgba(42,157,143,0.08); padding: 2px 8px; border-radius: 10px; }

.stamp-empty { display: flex; flex-direction: column; align-items: center; padding: 24px 0; }
.stamp-empty-emoji { font-size: 40px; margin-bottom: 10px; }
.stamp-empty-t { font-size: 15px; font-weight: 600; color: #666; margin-bottom: 4px; }
.stamp-empty-sub { font-size: 12px; color: #999; margin-bottom: 16px; }
.stamp-empty-btn { padding: 8px 28px; background: #2A9D8F; border-radius: 20px; }
.stamp-empty-btn-t { font-size: 13px; color: #fff; font-weight: 600; }

/* 答题入口 */
.pp-quiz-sec { margin: 14px 16px 0; }
.quiz-entry {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; border-radius: 16px; padding: 16px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.04);
}
.qe-left { display: flex; align-items: center; }
.qe-icon { width: 42px; height: 42px; border-radius: 12px; background: linear-gradient(135deg, #FFD93D, #F39C12); display: flex; align-items: center; justify-content: center; margin-right: 12px; }
.qe-icon-t { font-size: 20px; }
.qe-info { display: flex; flex-direction: column; }
.qe-title { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 2px; }
.qe-desc { font-size: 11px; color: #999; }
.qe-arrow { font-size: 16px; color: #ccc; }
</style>
