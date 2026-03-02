<template>
  <view class="spot-page">
    <image class="spot-cover" :src="coverUrl" mode="aspectFill" />
    <view class="spot-card">
      <text class="spot-name">{{ spot.name || '景区详情' }}</text>
      <text class="spot-meta">{{ spot.address || '六盘水' }}</text>
      <view class="spot-tags" v-if="tagList.length > 0">
        <view class="spot-tag" v-for="(t, i) in tagList" :key="i">
          <text class="spot-tag-t">{{ t }}</text>
        </view>
      </view>
      <view class="spot-row">
        <text class="spot-row-l">评分</text>
        <text class="spot-row-r">{{ spot.rating ? spot.rating.toFixed(1) : '4.5' }}</text>
      </view>
      <view class="spot-row" v-if="spot.openingHours">
        <text class="spot-row-l">开放时间</text>
        <text class="spot-row-r">{{ spot.openingHours }}</text>
      </view>
      <view class="spot-row" v-if="spot.price">
        <text class="spot-row-l">门票</text>
        <text class="spot-row-r">{{ spot.price }}</text>
      </view>
      <view class="spot-row" v-if="spot.currentFlow !== null && spot.currentFlow !== undefined">
        <text class="spot-row-l">实时客流</text>
        <text class="spot-row-r">{{ spot.currentFlow }} 人</text>
      </view>
    </view>

    <view class="spot-desc" v-if="spot.fullDescription || spot.description">
      <text class="spot-desc-title">景区介绍</text>
      <text class="spot-desc-text">{{ spot.fullDescription || spot.description }}</text>
    </view>

    <!-- 景区内景点列表 -->
    <view class="spot-sub-section" v-if="subSpots.length > 0">
      <text class="spot-desc-title">景区内景点</text>
      <view class="sub-spot-list">
        <view class="sub-spot-card" v-for="(sub, i) in subSpots" :key="i" @tap="onSubSpotTap(sub)">
          <image 
            class="sub-spot-img" 
            :src="sub.imageUrl || 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&q=60'" 
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

    <view class="spot-gallery" v-if="images.length > 0">
      <text class="spot-desc-title">图片</text>
      <scroll-view scroll-x class="spot-gallery-row" :show-scrollbar="false">
        <image class="spot-gallery-img" v-for="(img, i) in images" :key="i" :src="img" mode="aspectFill" />
      </scroll-view>
    </view>

    <view class="spot-actions">
      <view class="spot-btn spot-btn-primary" @tap="onNav">
        <text class="spot-btn-t">开始导航</text>
      </view>
      <view class="spot-btn spot-btn-light" @tap="onGuide">
        <text class="spot-btn-t">AI 讲解</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchSpotDetail, fetchSubSpots } from '@/api/spots'
import { resolveAssetUrl } from '@/utils/url'

const spot = ref({})
const coverUrl = ref('https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&q=80')
const images = ref([])
const tagList = ref([])
const subSpots = ref([])

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

    // 加载子景点
    loadSubSpots(idOrCode)
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
  if (!spot.value.latitude || !spot.value.longitude) {
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

const onGuide = () => {
  const name = spot.value.name || ''
  uni.navigateTo({ url: `/pages/digital-human/index?spot=${encodeURIComponent(name)}` })
}

onLoad((options) => {
  const id = options && (options.id || options.code || options.name)
  if (id) {
    loadDetail(id)
  }
})
</script>

<style lang="scss">
@import "@/uni.scss";

.spot-page { min-height: 100vh; background: #F2F5F8; }
.spot-cover { width: 100%; height: 240px; }
.spot-card {
  margin: -30px 16px 0;
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
}
.spot-name { font-size: 18px; font-weight: 800; color: #1A1A2E; }
.spot-meta { display: block; font-size: 12px; color: #888; margin-top: 4px; }
.spot-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
.spot-tag { padding: 2px 8px; background: #E0F2F1; border-radius: 20px; }
.spot-tag-t { font-size: 10px; color: #2A9D8F; }
.spot-row { display: flex; justify-content: space-between; margin-top: 10px; }
.spot-row-l { font-size: 12px; color: #666; }
.spot-row-r { font-size: 12px; color: #1A1A2E; font-weight: 600; }
.spot-desc { margin: 12px 16px 0; background: #fff; border-radius: 16px; padding: 16px; }
.spot-desc-title { font-size: 14px; font-weight: 700; color: #1A1A2E; }
.spot-desc-text { display: block; margin-top: 8px; font-size: 12px; color: #666; line-height: 1.6; }
.spot-gallery { margin: 12px 16px 0; background: #fff; border-radius: 16px; padding: 16px; }
.spot-gallery-row { margin-top: 8px; white-space: nowrap; }
.spot-gallery-img { width: 140px; height: 90px; border-radius: 10px; margin-right: 8px; display: inline-block; }
.spot-actions { margin: 14px 16px 24px; display: flex; gap: 10px; }
.spot-btn { flex: 1; padding: 12px; border-radius: 12px; text-align: center; }
.spot-btn-primary { background: #2A9D8F; }
.spot-btn-light { background: #E0F2F1; border: 1px solid rgba(42,157,143,0.2); }
.spot-btn-t { font-size: 14px; font-weight: 600; color: #fff; }
.spot-btn-light .spot-btn-t { color: #2A9D8F; }

/* 子景点列表 */
.spot-sub-section { margin: 12px 16px 0; background: #fff; border-radius: 16px; padding: 16px; }
.sub-spot-list { margin-top: 10px; }
.sub-spot-card {
  display: flex;
  padding: 10px 0;
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
