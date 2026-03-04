<template>
  <view class="guide-page">
    <!-- 顶部栏 -->
    <view class="g-header" :style="{ paddingTop: stBar + 'px' }">
      <view class="g-header-inner">
        <text class="g-title">智游六盘水</text>
        <view class="g-mode" @tap="toggleStudy">
          <view class="mode-dot" :class="{ 'mode-on': studyMode }" />
          <text class="mode-txt">{{ studyMode ? '研学模式' : '普通模式' }}</text>
        </view>
      </view>
    </view>

    <!-- 地图 -->
    <map
      id="guideMap"
      class="g-map"
      :latitude="center.lat"
      :longitude="center.lng"
      :markers="markers"
      :scale="13"
      show-location
      :enable-3D="true"
      :show-compass="true"
      @markertap="onMarkerTap"
    >
      <!-- 地图控件（用 view 替代 cover-view） -->
      <view class="map-ctrls">
        <view class="m-btn" @tap="moveToMe">
          <view class="m-btn-dot" />
          <text class="m-btn-t">定位</text>
        </view>
        <view class="m-btn m-btn-ai" @tap="startGuide">
          <text class="m-btn-ai-t">AI</text>
          <text class="m-btn-t" style="color:#fff;">讲解</text>
        </view>
        <view class="m-btn m-btn-quiz" v-if="studyMode" @tap="openQuiz">
          <text class="m-btn-q-t">?</text>
          <text class="m-btn-t" style="color:#fff;">答题</text>
        </view>
      </view>
    </map>

    <!-- 到达提醒 -->
    <view class="arrive" v-if="showArrive" @tap="onArriveTap">
      <view class="arrive-inner">
        <view class="arrive-av">
          <text class="arrive-av-t">游</text>
        </view>
        <view class="arrive-body">
          <text class="arrive-title">黔小游检测到你的位置</text>
          <text class="arrive-desc">{{ arriveMsg }}</text>
        </view>
        <view class="arrive-btn">
          <text class="arrive-btn-t">开启</text>
        </view>
      </view>
    </view>

    <!-- 研学答题弹窗 -->
    <view class="quiz-mask" v-if="showQuiz" @tap.stop>
      <view class="quiz-box">
        <view class="quiz-hd">
          <text class="quiz-hd-t">研学问答</text>
          <view class="quiz-close" @tap="closeQuiz">
            <text class="quiz-x">X</text>
          </view>
        </view>
        <view class="quiz-body">
          <text class="quiz-q">{{ quiz.question }}</text>
          <view class="quiz-opts">
            <view
              class="quiz-opt"
              v-for="(opt, idx) in quiz.options"
              :key="idx"
              :class="{
                'opt-ok': answered && idx === quiz.answer,
                'opt-err': answered && picked === idx && idx !== quiz.answer
              }"
              @tap="pickOpt(idx)"
            >
              <view class="opt-letter">
                <text class="opt-letter-t">{{ ['A','B','C','D'][idx] }}</text>
              </view>
              <text class="opt-text">{{ opt }}</text>
            </view>
          </view>
        </view>
        <view class="quiz-ft" v-if="answered">
          <view class="quiz-result" :class="isCorrect ? 'res-ok' : 'res-err'">
            <text class="res-txt">{{ isCorrect ? '回答正确！+10 黔豆' : '回答错误，再接再厉' }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- POI 抽屉 -->
    <view class="poi-drawer" v-if="curPoi">
      <view class="poi-handle" />
      <view class="poi-body">
        <view class="poi-hd">
          <view class="poi-hd-info">
            <text class="poi-name">{{ curPoi.title }}</text>
            <view class="poi-tags">
              <view class="poi-tag" v-if="curPoi.isStudy">
                <text class="poi-tag-t">研学点</text>
              </view>
              <view class="poi-tag poi-tag-s">
                <text class="poi-tag-t">{{ curPoi.category }}</text>
              </view>
            </view>
          </view>
          <view class="poi-close" @tap="curPoi = null">
            <text class="poi-x">X</text>
          </view>
        </view>
        <text class="poi-desc">{{ curPoi.desc }}</text>
        <view class="poi-acts">
          <view class="poi-act poi-act-p" @tap="navTo">
            <text class="poi-act-t" style="color:#fff;">开始导航</text>
          </view>
          <view class="poi-act poi-act-o" @tap="startGuide">
            <text class="poi-act-t" style="color:#2A9D8F;">AI 讲解</text>
          </view>
          <view class="poi-act poi-act-d" @tap="goDetail">
            <text class="poi-act-t" style="color:#fff;">详情</text>
          </view>
        </view>
      </view>
    </view>

    <!-- AI 悬浮球 -->
    <view class="ai-fab" @tap="onAi" v-if="!curPoi">
      <view class="ai-fab-bubble" v-if="showBubble">
        <text>{{ bubbleMsg }}</text>
      </view>
      <view class="ai-fab-btn">
        <image class="ai-fab-avatar" src="/static/dh-avatar.png" mode="aspectFill" />
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { fetchScenicSpots } from '@/api/home'
import { fetchQuizList, submitQuizAnswer } from '@/api/study'

const stBar = ref(20)
const studyMode = ref(false)
const showArrive = ref(false)
const arriveMsg = ref('你已到达三线建设博物馆，是否开启 AI 讲解？')
const showQuiz = ref(false)
const answered = ref(false)
const isCorrect = ref(false)
const picked = ref(-1)
const curPoi = ref(null)
const showBubble = ref(true)
const bubbleMsg = ref('试试研学模式')
const center = ref({ lat: 26.5946, lng: 104.8307 })

const poiList = ref([
  { id: 1, latitude: 26.5946, longitude: 104.8307, title: '钟山公园',
    desc: '六盘水市区中心城市公园，绿树成荫，登上钟山可俯瞰凉都城区。',
    category: '公园', isStudy: false,
    iconPath: '/static/tabbar/guide-active.png', width: 30, height: 30 },
  { id: 2, latitude: 26.6050, longitude: 104.8500, title: '贵州三线建设博物馆',
    desc: '全国首家"三线建设"主题博物馆，展示20世纪60-70年代三线建设伟大历程。',
    category: '博物馆', isStudy: true,
    iconPath: '/static/tabbar/guide-active.png', width: 30, height: 30 },
  { id: 3, latitude: 26.5800, longitude: 104.7900, title: '梅花山旅游景区',
    desc: '海拔2680米，世界最高城市滑雪场，冬可滑雪、夏可避暑。',
    category: '景区', isStudy: false,
    iconPath: '/static/tabbar/guide-active.png', width: 30, height: 30 },
  { id: 4, latitude: 26.5400, longitude: 104.8600, title: '水城古镇',
    desc: '600多年历史，保存大量明清古建筑，了解六盘水历史文化的绝佳之地。',
    category: '古镇', isStudy: true,
    iconPath: '/static/tabbar/guide-active.png', width: 30, height: 30 }
])

const toNumber = (val) => {
  if (val === null || val === undefined || val === '') return null
  const num = Number(val)
  return Number.isNaN(num) ? null : num
}

const loadSpots = async () => {
  try {
    const spots = await fetchScenicSpots()
    if (!Array.isArray(spots) || spots.length === 0) return

    const mapped = spots
      .map((spot) => {
        const lat = toNumber(spot.latitude)
        const lng = toNumber(spot.longitude)
        if (lat === null || lng === null) return null
        const tag = Array.isArray(spot.tags) ? spot.tags.join(' ') : ''
        return {
          id: spot.id,
          latitude: lat,
          longitude: lng,
          title: spot.name || '景区',
          desc: spot.description || spot.address || '六盘水景区',
          category: spot.category || '景区',
          isStudy: (spot.category && spot.category.includes('研学')) || (tag && tag.includes('研学')),
          iconPath: '/static/tabbar/guide-active.png',
          width: 30,
          height: 30
        }
      })
      .filter(Boolean)

    if (mapped.length > 0) {
      poiList.value = mapped
      center.value = { lat: mapped[0].latitude, lng: mapped[0].longitude }
    }
  } catch (err) {
    // 保留默认POI
  }
}

const markers = computed(() => poiList.value.map(p => ({
  id: p.id, latitude: p.latitude, longitude: p.longitude, title: p.title,
  iconPath: p.iconPath, width: p.width, height: p.height,
  callout: { content: p.title, color: '#2C3E50', fontSize: 12,
    borderRadius: 8, padding: 5, bgColor: '#fff', display: 'ALWAYS' }
})))

const quizBank = ref([])
const quizIdx = ref(0)
const quiz = computed(() => {
  if (quizBank.value.length > 0 && quizIdx.value < quizBank.value.length) {
    const q = quizBank.value[quizIdx.value]
    return {
      id: q.id,
      question: q.question,
      options: [q.optionA, q.optionB, q.optionC, q.optionD].filter(Boolean),
      answer: typeof q.answer === 'number' ? q.answer : 0
    }
  }
  return {
    id: null,
    question: '"三线建设"是从哪一年开始的？',
    options: ['1958年', '1964年', '1970年', '1978年'],
    answer: 1
  }
})

const loadQuiz = async () => {
  try {
    const poi = curPoi.value
    const scenicName = poi ? poi.title : ''
    const res = await fetchQuizList(scenicName)
    const arr = Array.isArray(res) ? res : (res && Array.isArray(res.records) ? res.records : [])
    if (arr.length > 0) {
      quizBank.value = arr
      quizIdx.value = Math.floor(Math.random() * arr.length)
    }
  } catch (e) {
    // 保留默认题目
  }
}

onMounted(() => {
  const info = uni.getWindowInfo()
  stBar.value = info.statusBarHeight || 20
  setTimeout(() => { showBubble.value = false }, 4000)
  loadSpots()
})

const toggleStudy = () => {
  studyMode.value = !studyMode.value
  uni.showToast({ title: studyMode.value ? '研学模式已开启' : '普通模式', icon: 'none' })
  if (studyMode.value) {
    setTimeout(() => {
      showArrive.value = true
      setTimeout(() => { showArrive.value = false }, 6000)
    }, 2000)
  }
}

const moveToMe = () => {
  uni.createMapContext('guideMap').moveToLocation()
}

const startGuide = () => {
  const poi = curPoi.value
  if (poi) {
    const query = encodeURIComponent(`请为我讲解${poi.title}的历史文化和游览亮点`)
    uni.navigateTo({ url: `/pages/digital-human/index?scenicName=${encodeURIComponent(poi.title)}&initMsg=${query}` })
  } else {
    uni.navigateTo({ url: '/pages/digital-human/index' })
  }
}

const openQuiz = () => {
  showQuiz.value = true
  answered.value = false
  picked.value = -1
  loadQuiz()
}

const closeQuiz = () => { showQuiz.value = false }

const pickOpt = async (idx) => {
  if (answered.value) return
  picked.value = idx
  answered.value = true
  isCorrect.value = idx === quiz.value.answer

  // 提交答案到后端（后端期望 int 类型）
  const u = uni.getStorageSync('userInfo')
  if (u && u.userId && quiz.value.id) {
    try {
      await submitQuizAnswer(u.userId, quiz.value.id, idx)
    } catch (e) { /* 静默 */ }
  }
}

const onMarkerTap = (e) => {
  const poi = poiList.value.find(p => p.id === e.markerId)
  if (poi) curPoi.value = poi
}

const navTo = () => {
  if (!curPoi.value) return
  uni.openLocation({
    latitude: curPoi.value.latitude, longitude: curPoi.value.longitude,
    name: curPoi.value.title, scale: 16
  })
}

const goDetail = () => {
  if (!curPoi.value) return
  const id = curPoi.value.id || curPoi.value.title
  uni.navigateTo({ url: `/pages/spot/detail?id=${encodeURIComponent(id)}` })
}

const onArriveTap = () => {
  showArrive.value = false
  startGuide()
}

const onAi = () => {
  uni.navigateTo({ url: '/pages/digital-human/index' })
}
</script>

<style lang="scss">
@import "@/uni.scss";

.guide-page { height: 100vh; display: flex; flex-direction: column; position: relative; }

.g-header {
  position: absolute; top: 0; left: 0; right: 0; z-index: 100;
  background: rgba(255,255,255,0.88);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0,0,0,0.05);
}
.g-header-inner { display: flex; align-items: center; justify-content: space-between; height: 44px; padding: 0 14px; }
.g-title { font-size: 16px; font-weight: 700; color: #1A1A2E; }
.g-mode { display: flex; align-items: center; padding: 4px 10px; background: #E0F2F1; border-radius: 20px; }
.mode-dot { width: 8px; height: 8px; border-radius: 50%; background: #ccc; margin-right: 5px; transition: background 0.2s; }
.mode-dot.mode-on { background: #2A9D8F; box-shadow: 0 0 6px rgba(42,157,143,0.5); }
.mode-txt { font-size: 11px; color: #2A9D8F; font-weight: 600; }

.g-map { width: 100%; height: 100vh; }

.map-ctrls { position: absolute; right: 12px; bottom: 140px; display: flex; flex-direction: column; }
.m-btn {
  background: #fff; padding: 10px; border-radius: 12px; margin-top: 10px;
  text-align: center; box-shadow: 0 2px 10px rgba(0,0,0,0.1); min-width: 50px;
  display: flex; flex-direction: column; align-items: center;
}
.m-btn-dot { width: 10px; height: 10px; border-radius: 50%; border: 2px solid #2A9D8F; margin-bottom: 2px; }
.m-btn-t { font-size: 10px; color: #666; }
.m-btn-ai { background: #2A9D8F; }
.m-btn-ai-t { font-size: 14px; font-weight: 800; color: #fff; }
.m-btn-quiz { background: #FF9F43; }
.m-btn-q-t { font-size: 16px; font-weight: 800; color: #fff; }

/* 到达提醒 */
.arrive { position: fixed; top: 100px; left: 16px; right: 16px; z-index: 200; animation: fadeInUp 0.4s ease; }
.arrive-inner {
  display: flex; align-items: center; padding: 14px;
  background: rgba(255,255,255,0.95); backdrop-filter: blur(16px);
  border-radius: 14px; box-shadow: 0 8px 24px rgba(0,0,0,0.12);
}
.arrive-av { width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, #2A9D8F, #1A6B5A); display: flex; align-items: center; justify-content: center; margin-right: 12px; flex-shrink: 0; }
.arrive-av-t { font-size: 18px; font-weight: 800; color: #fff; }
.arrive-body { flex: 1; }
.arrive-title { display: block; font-size: 13px; font-weight: 700; color: #1A1A2E; margin-bottom: 2px; }
.arrive-desc { font-size: 11px; color: #666; }
.arrive-btn { padding: 6px 14px; background: #2A9D8F; border-radius: 20px; flex-shrink: 0; }
.arrive-btn-t { font-size: 12px; color: #fff; font-weight: 600; }

/* 研学答题 */
.quiz-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.45); z-index: 500; display: flex; align-items: center; justify-content: center; }
.quiz-box { width: 84%; background: #fff; border-radius: 20px; overflow: hidden; box-shadow: 0 12px 32px rgba(0,0,0,0.15); animation: slideUp 0.35s ease; }
.quiz-hd { display: flex; align-items: center; justify-content: space-between; padding: 16px 18px; background: linear-gradient(135deg, #2A9D8F, #1A6B5A); }
.quiz-hd-t { font-size: 16px; font-weight: 700; color: #fff; }
.quiz-close { padding: 4px 8px; }
.quiz-x { color: rgba(255,255,255,0.7); font-size: 16px; font-weight: 700; }
.quiz-body { padding: 18px; }
.quiz-q { display: block; font-size: 15px; font-weight: 600; color: #1A1A2E; margin-bottom: 16px; line-height: 1.5; }
.quiz-opt {
  display: flex; align-items: center; padding: 12px 14px;
  border: 2px solid #eee; border-radius: 10px; margin-bottom: 10px;
}
.opt-letter { width: 26px; height: 26px; border-radius: 50%; background: #f0f0f0; display: flex; align-items: center; justify-content: center; margin-right: 10px; flex-shrink: 0; }
.opt-letter-t { font-size: 12px; font-weight: 700; color: #666; }
.opt-text { font-size: 14px; color: #333; }
.opt-ok { border-color: #07c160 !important; background: rgba(7,193,96,0.06); }
.opt-ok .opt-letter { background: #07c160; }
.opt-ok .opt-letter-t { color: #fff; }
.opt-err { border-color: #ee0a24 !important; background: rgba(238,10,36,0.04); }
.opt-err .opt-letter { background: #ee0a24; }
.opt-err .opt-letter-t { color: #fff; }
.quiz-ft { padding: 0 18px 18px; }
.quiz-result { padding: 12px; border-radius: 10px; text-align: center; }
.res-ok { background: rgba(7,193,96,0.08); }
.res-err { background: rgba(238,10,36,0.06); }
.res-txt { font-size: 14px; font-weight: 600; }
.res-ok .res-txt { color: #07c160; }
.res-err .res-txt { color: #ee0a24; }

/* POI 抽屉 */
.poi-drawer { position: fixed; bottom: 0; left: 0; right: 0; z-index: 150; animation: slideUp 0.3s ease; }
.poi-handle { width: 36px; height: 4px; background: #ddd; border-radius: 2px; margin: 10px auto; }
.poi-body { background: #fff; border-radius: 20px 20px 0 0; padding: 8px 18px 28px; box-shadow: 0 -4px 20px rgba(0,0,0,0.1); }
.poi-hd { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; }
.poi-hd-info { flex: 1; }
.poi-name { display: block; font-size: 19px; font-weight: 800; color: #1A1A2E; margin-bottom: 6px; }
.poi-tags { display: flex; gap: 6px; }
.poi-tag { padding: 2px 8px; background: #E0F2F1; border-radius: 20px; }
.poi-tag-s { background: rgba(255,159,67,0.1); }
.poi-tag-s .poi-tag-t { color: #FF9F43; }
.poi-tag-t { font-size: 11px; color: #2A9D8F; font-weight: 500; }
.poi-close { padding: 4px 8px; }
.poi-x { font-size: 16px; color: #ccc; font-weight: 700; }
.poi-desc { display: block; font-size: 13px; color: #666; line-height: 1.6; margin-bottom: 14px; }
.poi-acts { display: flex; gap: 10px; }
.poi-act { flex: 1; padding: 12px; border-radius: 10px; text-align: center; }
.poi-act-p { background: #2A9D8F; }
.poi-act-o { background: #E0F2F1; border: 1px solid rgba(42,157,143,0.15); }
.poi-act-d { background: #FF9F43; }
.poi-act-t { font-size: 14px; font-weight: 600; }
</style>
