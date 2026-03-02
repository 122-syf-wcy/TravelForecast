<template>
  <view class="page">
    <!-- 顶部操作 -->
    <view class="top-bar">
      <text class="top-title">我的行程</text>
      <view class="top-btn" @tap="onAiPlan">
        <text class="top-btn-t">AI 规划</text>
      </view>
    </view>

    <view class="timeline">
      <view class="day-group" v-for="(group, gi) in groupedList" :key="gi">
        <view class="day-label">
          <text class="day-t">{{ group.date }}</text>
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
      <view class="empty-btn" @tap="onAiPlan">
        <text class="empty-btn-t">让 AI 帮你规划</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchItineraries } from '@/api/itinerary'

const list = ref([])

const groupedList = computed(() => {
  const groups = {}
  list.value.forEach(item => {
    const key = item.date || '行程'
    if (!groups[key]) groups[key] = { date: key, items: [] }
    groups[key].items.push(item)
  })
  return Object.values(groups)
})

const loadData = async () => {
  list.value = await fetchItineraries()
}

const onItem = (item) => {
  if (item.title) {
    uni.showToast({ title: item.title, icon: 'none' })
  }
}

const onAiPlan = () => {
  uni.navigateTo({ url: '/pages/digital-human/index' })
}

onMounted(() => { loadData() })
</script>

<style lang="scss">
.page { min-height: 100vh; background: #F2F5F8; padding: 0 16px 40px; }

.top-bar { display: flex; align-items: center; justify-content: space-between; padding: 16px 0; }
.top-title { font-size: 18px; font-weight: 800; color: #1A1A2E; }
.top-btn { padding: 6px 14px; background: #2A9D8F; border-radius: 20px; }
.top-btn-t { font-size: 12px; color: #fff; font-weight: 600; }

.day-group { margin-bottom: 8px; }
.day-label { margin-bottom: 12px; }
.day-t { font-size: 15px; font-weight: 700; color: #2A9D8F; background: #E0F2F1; padding: 4px 12px; border-radius: 8px; }

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
.empty-btn { display: inline-block; padding: 10px 24px; background: #2A9D8F; border-radius: 20px; }
.empty-btn-t { font-size: 14px; color: #fff; font-weight: 600; }
</style>
