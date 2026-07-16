<template>
  <view class="page">
    <!-- 顶部操作 -->
    <view class="top-bar">
      <text class="top-title">我的行程</text>
      <view class="top-actions">
        <view class="top-btn top-btn-ghost" @tap="openCreate">
          <text class="top-btn-t-g">新建</text>
        </view>
        <view class="top-btn" @tap="onAiPlan">
          <text class="top-btn-t">AI 规划</text>
        </view>
      </view>
    </view>

    <view class="timeline">
      <view class="day-group" v-for="group in groupedList" :key="group.key">
        <view class="day-label">
          <text class="day-t">{{ group.date }}</text>
          <text
            v-if="group.itineraryId"
            class="day-del"
            @tap.stop="onDeleteItinerary(group.itineraryId, group.date)"
          >删除</text>
        </view>
        <view class="node" v-for="(item, i) in group.items" :key="i">
          <view class="node-left">
            <text class="node-time">{{ item.time }}</text>
          </view>
          <view class="node-line">
            <view class="dot" />
            <view class="line" v-if="i < group.items.length - 1" />
          </view>
          <view class="node-right">
            <view class="card" @tap="onItem(item)">
              <text class="card-title">{{ item.title }}</text>
              <text class="card-desc">{{ item.desc }}</text>
              <image v-if="item.img" class="card-img" :src="item.img" mode="aspectFill" />
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="empty" v-if="list.length === 0">
      <text class="empty-t">还没有行程</text>
      <view class="empty-btn" @tap="openCreate">
        <text class="empty-btn-t">创建第一个行程</text>
      </view>
      <view class="empty-btn empty-btn-ghost" @tap="onAiPlan">
        <text class="empty-btn-t-g">让 AI 帮你规划</text>
      </view>
    </view>

    <!-- AI 规划弹窗 -->
    <view v-if="showAiPlan" class="modal-mask" @tap="showAiPlan = false">
      <view class="modal" @tap.stop>
        <text class="modal-title">AI 智能规划</text>
        <view class="modal-row">
          <text class="modal-label">行程天数</text>
          <view class="modal-days">
            <view v-for="n in 7" :key="n" class="day-chip"
              :class="{ 'day-chip-active': aiPlanDays === n }" @tap="aiPlanDays = n">
              <text class="day-chip-t">{{ n }} 天</text>
            </view>
          </view>
        </view>
        <view class="modal-row">
          <text class="modal-label">主题偏好</text>
          <view class="modal-days">
            <view v-for="t in aiPlanThemes" :key="t" class="day-chip"
              :class="{ 'day-chip-active': aiPlanTheme === t }" @tap="aiPlanTheme = t">
              <text class="day-chip-t">{{ t }}</text>
            </view>
          </view>
        </view>
        <view class="modal-ft">
          <view class="modal-btn modal-btn-sub" @tap="showAiPlan = false">取消</view>
          <view class="modal-btn modal-btn-main" :class="{ 'disabled': aiPlanning }" @tap="doAiPlan">
            <text>{{ aiPlanning ? 'AI 规划中…' : '开始规划' }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 新建行程弹窗 -->
    <view v-if="showCreate" class="modal-mask" @tap="closeCreate">
      <view class="modal" @tap.stop>
        <text class="modal-title">新建行程</text>
        <view class="modal-row">
          <text class="modal-label">行程标题</text>
          <input class="modal-input" v-model="form.title" placeholder="例如：六盘水 3 日探秘" maxlength="30" />
        </view>
        <view class="modal-row">
          <text class="modal-label">天数</text>
          <view class="modal-days">
            <view
              v-for="n in 7"
              :key="n"
              class="day-chip"
              :class="{ 'day-chip-active': form.days === n }"
              @tap="form.days = n"
            >
              <text class="day-chip-t">{{ n }} 天</text>
            </view>
          </view>
        </view>
        <view class="modal-row">
          <text class="modal-label">首项（可选）</text>
          <input class="modal-input" v-model="form.firstTitle" placeholder="第一个行程点名称" maxlength="20" />
          <input class="modal-input modal-input-sm" v-model="form.firstTime" placeholder="如 09:00" maxlength="8" />
        </view>
        <view class="modal-ft">
          <view class="modal-btn modal-btn-sub" @tap="closeCreate">取消</view>
          <view class="modal-btn modal-btn-main" :class="{ 'disabled': submitting }" @tap="submitCreate">
            <text>{{ submitting ? '保存中…' : '保存' }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { fetchItineraries, createItinerary, deleteItinerary } from '@/api/itinerary'
import { chatWithAI, generateConversationId } from '@/api/digitalHuman'
import { resolveAssetUrl } from '@/utils/url'

const list = ref([])
const showCreate = ref(false)
const submitting = ref(false)
const form = reactive({
  title: '',
  days: 3,
  firstTitle: '',
  firstTime: ''
})

const groupedList = computed(() => {
  // 以 itineraryId 优先作为分组 key，保证不同行程标题相同也不会被错误合并；
  // 回落到 date 仅用于少见的旧数据或前端模拟场景。
  const groups = {}
  list.value.forEach((item) => {
    const key = item.itineraryId != null ? `id:${item.itineraryId}` : `date:${item.date || '行程'}`
    if (!groups[key]) {
      groups[key] = {
        key,
        date: item.date || '行程',
        itineraryId: item.itineraryId,
        items: []
      }
    }
    groups[key].items.push(item)
  })
  return Object.values(groups)
    .map((group) => ({
      ...group,
      items: [...group.items].sort((a, b) => (a.time || '99:99').localeCompare(b.time || '99:99'))
    }))
    .sort((a, b) => {
      const aId = Number(a.itineraryId || 0)
      const bId = Number(b.itineraryId || 0)
      if (aId && bId && aId !== bId) return bId - aId
      return (a.date || '').localeCompare(b.date || '')
    })
})

const getCurrentUserId = () => {
  const u = uni.getStorageSync('userInfo')
  return u && u.userId ? u.userId : null
}

const loadData = async () => {
  const userId = getCurrentUserId()
  if (!userId) return
  try {
    const res = await fetchItineraries(userId)
    if (!Array.isArray(res)) { list.value = []; return }
    // 后端返回 [{itinerary, items}]，展平为时间线
    const flat = []
    res.forEach(group => {
      const it = group.itinerary || {}
      const items = Array.isArray(group.items) ? group.items : []
      if (items.length === 0) {
        flat.push({
          itineraryId: it.id,
          date: it.title || '行程',
          time: '',
          title: it.title || '未命名行程',
          desc: `${it.days || 1}天行程`,
          img: ''
        })
      } else {
        items.forEach(item => {
          flat.push({
            itineraryId: it.id,
            spotId: item.spotId,
            date: it.title || `第${item.dayNum || 1}天`,
            time: item.timeSlot || '',
            title: item.title || '',
            desc: item.description || '',
            img: item.imageUrl ? resolveAssetUrl(item.imageUrl) : ''
          })
        })
      }
    })
    list.value = flat
  } catch (e) { list.value = [] }
}

const showAiPlan = ref(false)
const aiPlanDays = ref(3)
const aiPlanTheme = ref('')
const aiPlanning = ref(false)
const aiPlanThemes = ['自由行', '亲子游', '红色研学', '美食之旅', '摄影采风']

const onItem = (item) => {
  if (item.spotId) {
    uni.navigateTo({ url: `/pages/spot/detail?id=${encodeURIComponent(item.spotId)}` })
  } else if (item.title) {
    uni.navigateTo({ url: `/pages/search/index` })
  }
}

const onAiPlan = () => {
  const userId = getCurrentUserId()
  if (!userId) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  aiPlanDays.value = 3
  aiPlanTheme.value = '自由行'
  showAiPlan.value = true
}

const doAiPlan = async () => {
  if (aiPlanning.value) return
  aiPlanning.value = true

  const prompt = `请为我规划一个六盘水${aiPlanDays.value}天${aiPlanTheme.value}行程。` +
    `请严格按以下JSON格式回复，不要添加其他文字：` +
    `[{"day":1,"items":[{"time":"09:00","title":"景点名","desc":"简短描述"}]}]`

  try {
    const convId = generateConversationId()
    const res = await chatWithAI(prompt, convId)
    const reply = res.reply || ''

    const jsonMatch = reply.match(/\[[\s\S]*\]/)
    if (!jsonMatch) throw new Error('AI 返回格式异常')

    const plan = JSON.parse(jsonMatch[0])
    const userId = getCurrentUserId()
    const title = `AI ${aiPlanTheme.value} ${aiPlanDays.value}日`
    const allItems = []

    for (const day of plan) {
      if (Array.isArray(day.items)) {
        for (const it of day.items) {
          allItems.push({
            dayNum: day.day || 1,
            title: it.title || '',
            description: it.desc || '',
            timeSlot: it.time || ''
          })
        }
      }
    }

    await createItinerary(userId, title, aiPlanDays.value, allItems)
    showAiPlan.value = false
    uni.showToast({ title: 'AI 行程已生成', icon: 'success' })
    await loadData()
  } catch (err) {
    uni.showToast({ title: err.message || 'AI 规划失败，请重试', icon: 'none' })
  } finally {
    aiPlanning.value = false
  }
}

const openCreate = () => {
  const userId = getCurrentUserId()
  if (!userId) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  form.title = ''
  form.days = 3
  form.firstTitle = ''
  form.firstTime = ''
  showCreate.value = true
}

const closeCreate = () => {
  if (submitting.value) return
  showCreate.value = false
}

const submitCreate = async () => {
  if (submitting.value) return
  const title = (form.title || '').trim()
  if (!title) {
    uni.showToast({ title: '请填写行程标题', icon: 'none' })
    return
  }
  const userId = getCurrentUserId()
  if (!userId) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }

  const items = []
  if (form.firstTitle) {
    items.push({
      dayNum: 1,
      title: form.firstTitle.trim(),
      description: '手动创建',
      timeSlot: (form.firstTime || '').trim()
    })
  }

  submitting.value = true
  try {
    await createItinerary(userId, title, form.days, items)
    uni.showToast({ title: '行程已保存', icon: 'success' })
    showCreate.value = false
    await loadData()
  } catch (err) {
    uni.showToast({ title: (err && err.message) || '保存失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

const onDeleteItinerary = (id, title) => {
  if (!id) return
  uni.showModal({
    title: '删除行程',
    content: `确定要删除《${title || '未命名行程'}》吗？该行程内的所有安排都会被清空。`,
    confirmText: '删除',
    confirmColor: '#e74c3c',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deleteItinerary(id)
        uni.showToast({ title: '已删除', icon: 'success' })
        await loadData()
      } catch (err) {
        uni.showToast({ title: (err && err.message) || '删除失败', icon: 'none' })
      }
    }
  })
}

onMounted(() => { loadData() })
</script>

<style lang="scss">
.page { min-height: 100vh; background: #F2F5F8; padding: 0 16px 40px; }

.top-bar { display: flex; align-items: center; justify-content: space-between; padding: 16px 0; }
.top-title { font-size: 18px; font-weight: 800; color: #1A1A2E; }
.top-actions { display: flex; gap: 8px; }
.top-btn { padding: 6px 14px; background: #2A9D8F; border-radius: 20px; }
.top-btn-t { font-size: 12px; color: #fff; font-weight: 600; }
.top-btn-ghost { padding: 6px 14px; border-radius: 20px; border: 1px solid #2A9D8F; background: #fff; }
.top-btn-t-g { font-size: 12px; color: #2A9D8F; font-weight: 600; }

.day-group { margin-bottom: 8px; }
.day-label { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.day-t { font-size: 15px; font-weight: 700; color: #2A9D8F; background: #E0F2F1; padding: 4px 12px; border-radius: 8px; }
.day-del { font-size: 12px; color: #e74c3c; padding: 2px 6px; }

.timeline { padding-left: 10px; }
.node { display: flex; margin-bottom: 20px; }
.node-left { width: 46px; text-align: right; padding-right: 12px; }
.node-time { font-size: 14px; font-weight: 700; color: #333; }

.node-line { width: 20px; position: relative; display: flex; justify-content: center; }
.dot { width: 10px; height: 10px; border-radius: 50%; background: #2A9D8F; border: 2px solid #fff; box-shadow: 0 0 0 2px rgba(42,157,143,0.3); z-index: 2; }
.line { position: absolute; top: 10px; bottom: -30px; width: 2px; background: #e0e0e0; }

.node-right { flex: 1; padding-left: 12px; }
.card { background: #fff; border-radius: 12px; padding: 14px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
.card-title { font-size: 15px; font-weight: 700; color: #333; display: block; margin-bottom: 4px; }
.card-desc { font-size: 13px; color: #666; display: block; margin-bottom: 8px; }
.card-img { width: 100%; height: 120px; border-radius: 8px; }

.empty { text-align: center; margin-top: 60px; }
.empty-t { display: block; font-size: 14px; color: #999; margin-bottom: 16px; }
.empty-btn { display: inline-block; padding: 10px 24px; background: #2A9D8F; border-radius: 20px; margin: 0 6px; }
.empty-btn-t { font-size: 14px; color: #fff; font-weight: 600; }
.empty-btn-ghost { background: transparent; border: 1px solid #2A9D8F; }
.empty-btn-t-g { font-size: 14px; color: #2A9D8F; font-weight: 600; }

.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: flex-end; justify-content: center; z-index: 999; }
.modal { width: 100%; max-width: 520px; background: #fff; border-top-left-radius: 16px; border-top-right-radius: 16px; padding: 18px 20px 24px; }
.modal-title { display: block; font-size: 16px; font-weight: 700; color: #1A1A2E; text-align: center; margin-bottom: 12px; }
.modal-row { display: flex; flex-direction: column; margin-bottom: 12px; }
.modal-label { font-size: 13px; color: #666; margin-bottom: 6px; }
.modal-input { background: #F6F8FA; border-radius: 10px; padding: 10px 12px; font-size: 14px; color: #333; }
.modal-input-sm { margin-top: 8px; }
.modal-days { display: flex; flex-wrap: wrap; gap: 8px; }
.day-chip { padding: 6px 12px; border-radius: 14px; background: #F2F5F8; }
.day-chip-t { font-size: 13px; color: #666; }
.day-chip-active { background: #2A9D8F; }
.day-chip-active .day-chip-t { color: #fff; }
.modal-ft { display: flex; gap: 12px; margin-top: 12px; }
.modal-btn { flex: 1; text-align: center; padding: 10px 0; border-radius: 10px; font-size: 14px; font-weight: 600; }
.modal-btn-sub { background: #F2F5F8; color: #666; }
.modal-btn-main { background: #2A9D8F; color: #fff; }
.modal-btn-main.disabled { background: #95c4bf; }
</style>
