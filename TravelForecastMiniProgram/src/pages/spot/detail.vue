<template>
  <view class="spot-page">
    <!-- 沉浸式封面 -->
    <view class="cover-wrap">
      <image class="spot-cover" :src="coverUrl" mode="aspectFill" />
      <view class="cover-mask" />
      <view class="cover-back" @tap="goBack">
        <image class="cover-back-icon" src="/static/icons/back.svg" mode="aspectFit" />
      </view>
      <view class="cover-title-area">
        <text class="cover-name">{{ spot.name || '景区详情' }}</text>
        <text class="cover-addr">{{ spot.address || '六盘水' }}</text>
      </view>
    </view>

    <!-- 信息卡片 -->
    <view class="spot-card">
      <view class="spot-tags" v-if="tagList.length > 0">
        <view class="spot-tag" v-for="(t, i) in tagList" :key="i">
          <text class="spot-tag-t">{{ t }}</text>
        </view>
      </view>
      <view class="info-grid">
        <view class="info-item">
          <text class="info-val">{{ spot.rating ? spot.rating.toFixed(1) : '4.5' }}</text>
          <text class="info-lbl">评分</text>
        </view>
        <view class="info-divider" />
        <view class="info-item" v-if="spot.openingHours">
          <text class="info-val">{{ spot.openingHours }}</text>
          <text class="info-lbl">开放时间</text>
        </view>
        <view class="info-divider" v-if="spot.openingHours && spot.price" />
        <view class="info-item" v-if="spot.price">
          <text class="info-val">{{ spot.price }}</text>
          <text class="info-lbl">门票</text>
        </view>
      </view>
      <view class="spot-row" v-if="spot.currentFlow !== null && spot.currentFlow !== undefined">
        <text class="spot-row-l">实时客流</text>
        <text class="spot-row-r flow-tag">{{ spot.currentFlow }} 人</text>
      </view>
    </view>

    <!-- 景区介绍 -->
    <view class="spot-section" v-if="spot.fullDescription || spot.description">
      <view class="section-hd">
        <view class="section-bar" />
        <text class="section-title">景区介绍</text>
      </view>
      <text class="spot-desc-text">{{ spot.fullDescription || spot.description }}</text>
    </view>

    <!-- 景区内景点列表 -->
    <view class="spot-section" v-if="subSpots.length > 0">
      <view class="section-hd">
        <view class="section-bar" />
        <text class="section-title">景区内景点</text>
      </view>
      <view class="sub-spot-list">
        <view class="sub-spot-card" v-for="(sub, i) in subSpots" :key="i" @tap="onSubSpotTap(sub)">
          <image 
            class="sub-spot-img" 
            :src="sub.imageUrl || '/static/default-spot.svg'" 
            mode="aspectFill" 
          />
          <view class="sub-spot-info">
            <text class="sub-spot-name">{{ sub.name }}</text>
            <text class="sub-spot-desc" v-if="sub.description">{{ sub.description }}</text>
            <view class="sub-spot-tags" v-if="sub.tags && sub.tags.length > 0">
              <view class="spot-tag" v-for="(t, j) in sub.tags" :key="j">
                <text class="spot-tag-t">{{ t }}</text>
              </view>
            </view>
            <text class="sub-spot-hours" v-if="sub.openingHours">{{ sub.openingHours }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 图片画廊 -->
    <view class="spot-section" v-if="images.length > 0">
      <view class="section-hd">
        <view class="section-bar" />
        <text class="section-title">图片</text>
      </view>
      <scroll-view scroll-x class="spot-gallery-row" :show-scrollbar="false">
        <image class="spot-gallery-img" v-for="(img, i) in images" :key="i" :src="img" mode="aspectFill" />
      </scroll-view>
    </view>

    <!-- 底部留白（给固定操作栏让位） -->
    <view style="height: 80px;" />

    <!-- 固定底部操作栏 -->
    <view class="spot-actions">
      <view class="spot-btn spot-btn-fav" @tap="toggleFav">
        <image class="spot-btn-icon" :src="isFav ? '/static/icons/heart-filled.svg' : '/static/icons/heart-outline.svg'" mode="aspectFit" />
        <text class="spot-btn-t" :style="{ color: isFav ? '#e74c3c' : '#999' }">{{ isFav ? '已收藏' : '收藏' }}</text>
      </view>
      <view class="spot-btn spot-btn-primary" @tap="onNav">
        <image class="spot-btn-icon" src="/static/icons/location-pin.svg" mode="aspectFit" />
        <text class="spot-btn-t">开始导航</text>
      </view>
      <view class="spot-btn spot-btn-light" @tap="onGuide">
        <image class="spot-btn-icon" src="/static/icons/ai-orbit.svg" mode="aspectFit" />
        <text class="spot-btn-t">AI 讲解</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchSpotDetail, fetchSubSpots } from '@/api/spots'
import { checkFavorite, addFavorite, removeFavorite, addFootprint } from '@/api/user'
import { resolveAssetUrl } from '@/utils/url'

const spot = ref({})
const coverUrl = ref('/static/default-spot.svg')
const images = ref([])
const tagList = ref([])
const subSpots = ref([])
const isFav = ref(false)
const spotId = ref('')

const getUserId = () => {
  const u = uni.getStorageSync('userInfo')
  return u && u.userId ? u.userId : null
}

const loadFavStatus = async (id) => {
  const userId = getUserId()
  if (!userId || !id) return
  try {
    const res = await checkFavorite(userId, id)
    isFav.value = !!res
  } catch (e) { isFav.value = false }
}

const toggleFav = async () => {
  const userId = getUserId()
  if (!userId) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
  try {
    if (isFav.value) {
      await removeFavorite(userId, spotId.value)
      isFav.value = false
      uni.showToast({ title: '已取消收藏', icon: 'none' })
    } else {
      await addFavorite(userId, spotId.value)
      isFav.value = true
      uni.showToast({ title: '收藏成功', icon: 'success' })
    }
  } catch (e) {
    uni.showToast({ title: '操作失败', icon: 'none' })
  }
}

const loadDetail = async (idOrCode) => {
  try {
    const data = await fetchSpotDetail(idOrCode)
    spot.value = data || {}
    const cover = resolveAssetUrl(spot.value.imageUrl)
    if (cover) coverUrl.value = cover

    const imgs = Array.isArray(spot.value.images) ? spot.value.images : []
    images.value = imgs.map(resolveAssetUrl).filter(Boolean)

    const tags = Array.isArray(spot.value.tags) ? spot.value.tags : []
    if (spot.value.level) tags.push(spot.value.level)
    if (spot.value.category) tags.push(spot.value.category)
    tagList.value = [...new Set(tags)].filter(Boolean)

    // 加载子景点和收藏状态
    loadSubSpots(idOrCode)
    loadFavStatus(idOrCode)
  } catch (err) {
    uni.showToast({ title: '景区详情加载失败', icon: 'none' })
  }
}

const loadSubSpots = async (idOrCode) => {
  try {
    const data = await fetchSubSpots(idOrCode)
    subSpots.value = (Array.isArray(data) ? data : []).map(item => ({
      ...item,
      imageUrl: resolveAssetUrl(item.imageUrl),
      tags: Array.isArray(item.tags) ? item.tags : []
    }))
  } catch (err) {
    console.warn('子景点加载失败:', err)
  }
}

const onSubSpotTap = (sub) => {
  if (sub.latitude && sub.longitude) {
    uni.openLocation({
      latitude: Number(sub.latitude),
      longitude: Number(sub.longitude),
      name: sub.name || '景点',
      scale: 18
    })
  } else {
    uni.showToast({ title: sub.description || sub.name, icon: 'none', duration: 2000 })
  }
}

const onNav = () => {
  if (spot.value.latitude == null || spot.value.longitude == null) {
    uni.showToast({ title: '暂无定位信息', icon: 'none' })
    return
  }
  uni.openLocation({
    latitude: Number(spot.value.latitude),
    longitude: Number(spot.value.longitude),
    name: spot.value.name || '景区',
    scale: 16
  })
}

const goBack = () => {
  uni.navigateBack({ delta: 1 })
}

const onGuide = () => {
  const name = spot.value.name || ''
  const id = spot.value.id || spot.value.spotCode || ''
  const params = []
  if (id) params.push(`scenicId=${encodeURIComponent(id)}`)
  if (name) params.push(`scenicName=${encodeURIComponent(name)}`)
  const qs = params.length ? '?' + params.join('&') : ''
  uni.navigateTo({ url: `/pages/digital-human/index${qs}` })
}

const recordFootprint = (id, title) => {
  const userId = getUserId()
  if (!userId || !id) return
  addFootprint(userId, id, 'spot', title).catch(() => {
    const local = uni.getStorageSync('localFootprints') || []
    local.unshift({ id, title, type: 'spot', date: new Date().toISOString().split('T')[0], time: new Date().toTimeString().substring(0, 5) })
    uni.setStorageSync('localFootprints', local.slice(0, 200))
  })
}

onLoad((options) => {
  const id = options && (options.id || options.code || options.name)
  if (id) {
    spotId.value = id
    loadDetail(id).then(() => {
      recordFootprint(id, spot.value.name || '景区')
    })
  }
})
</script>

<style lang="scss">
@import "@/uni.scss";

.spot-page { min-height: 100vh; background: #F2F5F8; }

/* 沉浸式封面 */
.cover-wrap { position: relative; width: 100%; height: 300px; }
.spot-cover { width: 100%; height: 100%; }
.cover-mask {
  position: absolute; left: 0; right: 0; bottom: 0; height: 50%;
  background: linear-gradient(to top, rgba(0,0,0,0.55), transparent);
}
.cover-back {
  position: absolute; top: 48px; left: 16px;
  width: 36px; height: 36px; border-radius: 50%;
  background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center;
}
.cover-back-icon { width: 18px; height: 18px; display: block; }
.cover-title-area { position: absolute; left: 20px; bottom: 20px; }
.cover-name { font-size: 22px; font-weight: 800; color: #fff; text-shadow: 0 2px 8px rgba(0,0,0,0.3); }
.cover-addr { display: block; font-size: 12px; color: rgba(255,255,255,0.85); margin-top: 4px; }

/* 信息卡片 */
.spot-card {
  margin: -24px 16px 0; position: relative; z-index: 2;
  background: #fff; border-radius: 16px; padding: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
}
.spot-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.spot-tag { padding: 3px 10px; background: #E0F2F1; border-radius: 20px; }
.spot-tag-t { font-size: 11px; color: #2A9D8F; font-weight: 500; }
.info-grid {
  display: flex; align-items: center; justify-content: space-around;
  margin-top: 14px; padding: 12px 0;
  border-top: 1px solid #F0F0F0; border-bottom: 1px solid #F0F0F0;
}
.info-item { display: flex; flex-direction: column; align-items: center; flex: 1; }
.info-val { font-size: 14px; font-weight: 700; color: #1A1A2E; text-align: center; }
.info-lbl { font-size: 10px; color: #999; margin-top: 4px; }
.info-divider { width: 1px; height: 28px; background: #F0F0F0; }
.spot-row { display: flex; justify-content: space-between; margin-top: 12px; align-items: center; }
.spot-row-l { font-size: 13px; color: #666; }
.spot-row-r { font-size: 13px; color: #1A1A2E; font-weight: 600; }
.flow-tag { padding: 2px 10px; background: #FFF3E0; border-radius: 20px; color: #E65100; font-size: 12px; }

/* 通用 section */
.spot-section { margin: 12px 16px 0; background: #fff; border-radius: 16px; padding: 16px; }
.section-hd { display: flex; align-items: center; margin-bottom: 12px; }
.section-bar { width: 3px; height: 16px; background: #2A9D8F; border-radius: 2px; margin-right: 8px; }
.section-title { font-size: 15px; font-weight: 700; color: #1A1A2E; }
.spot-desc-text { display: block; font-size: 13px; color: #555; line-height: 1.8; white-space: pre-wrap; }

/* 图片画廊 */
.spot-gallery-row { margin-top: 4px; white-space: nowrap; }
.spot-gallery-img { width: 140px; height: 96px; border-radius: 10px; margin-right: 8px; display: inline-block; }

/* 底部操作栏 */
.spot-actions {
  position: fixed; left: 0; right: 0; bottom: 0;
  display: flex; gap: 10px; padding: 12px 16px;
  background: #fff; box-shadow: 0 -2px 12px rgba(0,0,0,0.06);
  z-index: 100;
}
.spot-btn { flex: 1; padding: 12px; border-radius: 12px; text-align: center; display: flex; align-items: center; justify-content: center; gap: 6px; }
.spot-btn-icon { width: 16px; height: 16px; display: block; flex-shrink: 0; }
.spot-btn-fav { background: #fff; border: 1px solid #eee; }
.spot-btn-primary { background: linear-gradient(135deg, #2A9D8F, #26A69A); }
.spot-btn-light { background: #E0F2F1; border: 1px solid rgba(42,157,143,0.2); }
.spot-btn-t { font-size: 14px; font-weight: 600; color: #fff; }
.spot-btn-light .spot-btn-t { color: #2A9D8F; }

/* 子景点列表 */
.sub-spot-list { margin-top: 4px; }
.sub-spot-card {
  display: flex; padding: 10px 0;
  border-bottom: 1px solid #F0F0F0;
}
.sub-spot-card:last-child { border-bottom: none; }
.sub-spot-img { width: 80px; height: 80px; border-radius: 10px; flex-shrink: 0; }
.sub-spot-info { flex: 1; margin-left: 10px; display: flex; flex-direction: column; justify-content: center; }
.sub-spot-name { font-size: 14px; font-weight: 700; color: #1A1A2E; }
.sub-spot-desc { font-size: 11px; color: #888; margin-top: 4px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.sub-spot-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 6px; }
.sub-spot-hours { font-size: 10px; color: #2A9D8F; margin-top: 4px; }
</style>
