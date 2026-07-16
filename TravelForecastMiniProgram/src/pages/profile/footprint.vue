<template>
  <view class="page">
    <view class="empty" v-if="list.length === 0 && loaded">
      <text class="empty-t">暂无足迹</text>
      <text class="empty-sub">去逛逛景区留下你的足迹吧</text>
    </view>

    <view class="timeline">
      <view class="day-group" v-for="group in groupedList" :key="group.date">
        <view class="day-label">
          <text class="day-t">{{ group.date }}</text>
        </view>
        <view class="fp-item" v-for="(item, i) in group.items" :key="i" @tap="onItem(item)">
          <view class="fp-img-box">
            <image v-if="item.image" class="fp-img" :src="item.image" mode="aspectFill" />
            <view v-else class="fp-img-ph" />
          </view>
          <view class="fp-info">
            <text class="fp-title">{{ item.title }}</text>
            <text class="fp-time">{{ item.time }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getFootprints } from '@/api/user'
import { resolveAssetUrl } from '@/utils/url'

const list = ref([])
const loaded = ref(false)

const getUserId = () => {
  const u = uni.getStorageSync('userInfo')
  return u && u.userId ? u.userId : null
}

const loadData = async () => {
  const userId = getUserId()
  if (!userId) { loaded.value = true; return }
  try {
    const res = await getFootprints(userId)
    if (Array.isArray(res)) {
      list.value = res.map(f => ({
        id: f.targetId || f.id,
        title: f.title || f.name || '未知',
        type: f.targetType || 'spot',
        image: f.imageUrl ? resolveAssetUrl(f.imageUrl) : '',
        date: (f.createdAt || f.time || '').split('T')[0] || '近期',
        time: (f.createdAt || f.time || '').split('T')[1]?.substring(0, 5) || ''
      }))
    }
  } catch (e) {
    // 后端暂无此接口时从本地 storage 读取
    const local = uni.getStorageSync('localFootprints') || []
    list.value = local
  }
  loaded.value = true
}

const groupedList = computed(() => {
  const groups = {}
  list.value.forEach(item => {
    const date = item.date || '近期'
    if (!groups[date]) groups[date] = { date, items: [] }
    groups[date].items.push(item)
  })
  return Object.values(groups)
})

const onItem = (item) => {
  if (item.type === 'spot' && item.id) {
    uni.navigateTo({ url: `/pages/spot/detail?id=${item.id}` })
  } else if (item.type === 'product' && item.id) {
    uni.navigateTo({ url: `/pages/shop/detail?id=${item.id}` })
  }
}

onMounted(() => { loadData() })
</script>

<style lang="scss">
.page { min-height: 100vh; background: #F2F5F8; padding: 12px 16px; }

.empty { text-align: center; margin-top: 80px; }
.empty-t { display: block; font-size: 15px; color: #999; margin-bottom: 6px; }
.empty-sub { display: block; font-size: 12px; color: #ccc; }

.day-group { margin-bottom: 16px; }
.day-label { margin-bottom: 10px; }
.day-t { font-size: 13px; font-weight: 600; color: #2A9D8F; background: #E0F2F1; padding: 4px 10px; border-radius: 6px; }

.fp-item {
  display: flex; align-items: center;
  background: #fff; border-radius: 12px;
  padding: 12px; margin-bottom: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.fp-img-box { width: 56px; height: 56px; border-radius: 10px; overflow: hidden; flex-shrink: 0; margin-right: 12px; }
.fp-img { width: 100%; height: 100%; }
.fp-img-ph { width: 100%; height: 100%; background: linear-gradient(135deg, #e0e0e0, #f5f5f5); }
.fp-info { flex: 1; }
.fp-title { display: block; font-size: 14px; font-weight: 600; color: #333; margin-bottom: 4px; }
.fp-time { font-size: 12px; color: #999; }
</style>
