<template>
  <view class="page">
    <view class="list" v-if="list.length > 0">
      <view class="fav" v-for="(f, i) in list" :key="i" @tap="goSpot(f)">
        <view class="fav-img-box">
          <image v-if="f.scenicImage" class="fav-img" :src="f.scenicImage" mode="aspectFill" />
          <view v-else class="fav-img-ph">
            <image class="fav-img-icon" :src="commonIcons.location" mode="aspectFit" />
          </view>
        </view>
        <view class="fav-info">
          <text class="fav-name">{{ f.scenicName || '景区' }}</text>
          <text class="fav-cat" v-if="f.scenicCategory">{{ f.scenicCategory }}</text>
          <text class="fav-time">收藏于 {{ formatTime(f.createdAt) }}</text>
        </view>
        <view class="fav-del" @tap.stop="removeFav(f, i)">
          <text class="fav-del-t">取消</text>
        </view>
      </view>
    </view>

    <view class="empty" v-if="list.length === 0 && loaded">
      <image class="empty-icon" :src="commonIcons.heartOutline" mode="aspectFit" />
      <text class="empty-t">{{ notLoggedIn ? '请先登录' : '还没有收藏' }}</text>
      <text class="empty-sub">{{ notLoggedIn ? '登录后可查看收藏' : '浏览景区时点击收藏吧' }}</text>
      <view class="empty-btn" @tap="notLoggedIn ? goLogin() : goExplore()">
        <text class="empty-btn-t">{{ notLoggedIn ? '去登录' : '去探索' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFavorites, removeFavorite } from '@/api/user'
import { resolveAssetUrl } from '@/utils/url'
import { COMMON_ICONS } from '@/utils/icon-catalog'

const list = ref([])
const loaded = ref(false)
const notLoggedIn = ref(false)
const commonIcons = COMMON_ICONS

const getUserId = () => {
  const u = uni.getStorageSync('userInfo')
  return u && u.userId ? u.userId : null
}

const loadData = async () => {
  const userId = getUserId()
  if (!userId) { notLoggedIn.value = true; loaded.value = true; return }
  try {
    const res = await getFavorites(userId)
    if (Array.isArray(res)) {
      list.value = res.map(f => ({
        ...f,
        scenicImage: f.scenicImage ? resolveAssetUrl(f.scenicImage) : ''
      }))
    }
  } catch (e) {
    list.value = []
  }
  loaded.value = true
}

const removeFav = async (f, i) => {
  const userId = getUserId()
  if (!userId) return
  uni.showModal({
    title: '提示',
    content: `确认取消收藏「${f.scenicName || '景区'}」？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await removeFavorite(userId, f.scenicId)
          list.value.splice(i, 1)
          uni.showToast({ title: '已取消收藏', icon: 'none' })
        } catch (e) {
          uni.showToast({ title: '操作失败', icon: 'none' })
        }
      }
    }
  })
}

const goSpot = (f) => {
  if (f.scenicId) {
    uni.navigateTo({ url: `/pages/spot/detail?id=${encodeURIComponent(f.scenicId)}` })
  }
}

const goExplore = () => {
  uni.switchTab({ url: '/pages/guide/index' })
}

const goLogin = () => {
  uni.switchTab({ url: '/pages/profile/index' })
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return ''
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

onMounted(() => { loadData() })
</script>

<style lang="scss">
.page { min-height: 100vh; background: #f2f5f8; padding: 12px 16px; }

.fav {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 14px;
  padding: 12px;
  margin-bottom: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.04);
}

.fav-img-box {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  margin-right: 12px;
}
.fav-img { width: 100%; height: 100%; }
.fav-img-ph {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #2A9D8F, #1A6B5A);
  display: flex;
  align-items: center;
  justify-content: center;
}
.fav-img-icon { width: 24px; height: 24px; display: block; }

.fav-info { flex: 1; min-width: 0; }
.fav-name { display: block; font-size: 15px; font-weight: 700; color: #1a1a2e; margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.fav-cat { display: inline-block; font-size: 10px; color: #2A9D8F; background: rgba(42,157,143,0.1); padding: 1px 6px; border-radius: 4px; margin-bottom: 4px; }
.fav-time { display: block; font-size: 11px; color: #bbb; }

.fav-del { padding: 6px 12px; border: 1px solid #eee; border-radius: 16px; flex-shrink: 0; margin-left: 8px; }
.fav-del-t { font-size: 12px; color: #999; }

.empty { text-align: center; margin-top: 80px; }
.empty-icon { display: block; width: 40px; height: 40px; margin: 0 auto 10px; }
.empty-t { display: block; font-size: 15px; color: #999; margin-bottom: 6px; }
.empty-sub { display: block; font-size: 12px; color: #ccc; margin-bottom: 20px; }
.empty-btn { display: inline-block; padding: 10px 24px; background: #2A9D8F; border-radius: 20px; }
.empty-btn-t { font-size: 14px; color: #fff; font-weight: 600; }
</style>
