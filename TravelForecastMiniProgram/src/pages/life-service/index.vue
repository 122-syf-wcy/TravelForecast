<template>
  <view class="page">
    <view class="hero">
      <text class="hero-title">{{ currentCategory }}服务</text>
      <text class="hero-sub">一站查看 {{ categories.length }} 类本地服务资源</text>

      <view class="search-box">
        <input
          class="search-input"
          type="text"
          v-model="keyword"
          placeholder="搜索服务名称、标签"
          confirm-type="search"
          @confirm="onSearch"
        />
        <view class="search-btn" @tap="onSearch">
          <text class="search-btn-t">搜索</text>
        </view>
      </view>
    </view>

    <scroll-view scroll-x class="tabs" :show-scrollbar="false">
      <view
        class="tab"
        v-for="(c, i) in categories"
        :key="i"
        :class="{ 'tab-on': currentCategory === c.name }"
        @tap="switchCategory(c.name)"
      >
        <view class="tab-icon" :style="{ background: c.bg }">
          <text class="tab-char">{{ c.char }}</text>
        </view>
        <text class="tab-name">{{ c.name }}</text>
      </view>
    </scroll-view>

    <view class="loading" v-if="loading">
      <text class="loading-t">加载中...</text>
    </view>

    <view class="list" v-else>
      <view class="item" v-for="(item, i) in items" :key="i" @tap="openItem(item)">
        <view class="item-cover">
          <image v-if="item.imageUrl" class="item-img" :src="item.imageUrl" mode="aspectFill" />
          <view v-else class="item-img-ph">
            <text class="item-img-char">{{ shortTitle(item.title) }}</text>
          </view>
        </view>

        <view class="item-main">
          <view class="item-top">
            <text class="item-title">{{ item.title }}</text>
            <text class="item-badge" v-if="item.badge">{{ item.badge }}</text>
          </view>
          <text class="item-summary">{{ item.summary }}</text>

          <view class="item-meta">
            <text class="item-tag" v-if="item.tag">{{ item.tag }}</text>
            <text class="item-price" v-if="item.priceText">{{ item.priceText }}</text>
          </view>
        </view>

        <view class="item-action" @tap.stop="openItem(item)">
          <text class="item-action-t">{{ item.actionText || '查看' }}</text>
        </view>
      </view>

      <view class="empty" v-if="items.length === 0">
        <text class="empty-t">当前分类暂无数据</text>
        <text class="empty-sub">试试切换分类或修改关键词</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchServiceCategories, fetchServiceItems } from '@/api/lifeService'
import { resolveAssetUrl } from '@/utils/url'

const categories = ref([])
const currentCategory = ref('交通出行')
const keyword = ref('')
const loading = ref(false)
const items = ref([])

const tabPages = ['/pages/index/index', '/pages/guide/index', '/pages/shop/index', '/pages/profile/index']

const fallbackCategories = [
  { name: '交通出行', char: '行', bg: 'linear-gradient(135deg, #dfe6e9, #b2bec3)' },
  { name: '酒店', char: '宿', bg: 'linear-gradient(135deg, #81ecec, #00cec9)' },
  { name: '美食', char: '食', bg: 'linear-gradient(135deg, #ffeaa7, #fdcb6e)' },
  { name: '非遗体验', char: '遗', bg: 'linear-gradient(135deg, #a29bfe, #6c5ce7)' },
  { name: '民宿', char: '居', bg: 'linear-gradient(135deg, #fd79a8, #e84393)' },
  { name: '门票', char: '门', bg: 'linear-gradient(135deg, #55efc4, #00b894)' },
  { name: '攻略', char: '略', bg: 'linear-gradient(135deg, #74b9ff, #0984e3)' },
  { name: '特色活动', char: '动', bg: 'linear-gradient(135deg, #fab1a0, #e17055)' },
  { name: '优惠福利', char: '惠', bg: 'linear-gradient(135deg, #ffeaa7, #f39c12)' },
  { name: '更多服务', char: '+', bg: 'linear-gradient(135deg, #dfe6e9, #b2bec3)' }
]

const loadCategories = async () => {
  try {
    const res = await fetchServiceCategories()
    categories.value = Array.isArray(res) && res.length > 0 ? res : fallbackCategories
  } catch (e) {
    categories.value = fallbackCategories
  }
}

const normalizeItem = (item) => {
  const imageUrl = item && item.imageUrl ? (resolveAssetUrl(item.imageUrl) || item.imageUrl) : ''
  return {
    ...item,
    imageUrl
  }
}

const loadItems = async () => {
  loading.value = true
  try {
    const res = await fetchServiceItems(currentCategory.value, keyword.value.trim())
    items.value = Array.isArray(res) ? res.map(normalizeItem) : []
  } catch (e) {
    items.value = []
  } finally {
    loading.value = false
  }
}

const switchCategory = (name) => {
  if (!name || currentCategory.value === name) return
  currentCategory.value = name
  loadItems()
}

const onSearch = () => {
  loadItems()
}

const isTabPage = (url) => {
  if (!url) return false
  const pure = url.split('?')[0]
  return tabPages.includes(pure)
}

const openItem = (item) => {
  if (!item) return

  if (item.targetType === 'spot' && item.targetId) {
    uni.navigateTo({ url: `/pages/spot/detail?id=${encodeURIComponent(item.targetId)}` })
    return
  }

  if (item.targetType === 'shop' && item.targetId) {
    uni.navigateTo({ url: `/pages/shop/detail?id=${encodeURIComponent(item.targetId)}` })
    return
  }

  if (item.targetType === 'phone' && item.phone) {
    uni.makePhoneCall({ phoneNumber: String(item.phone) })
    return
  }

  if (item.targetType === 'switchTab' && item.targetUrl) {
    uni.switchTab({ url: item.targetUrl })
    return
  }

  if (item.targetType === 'navigate' && item.targetUrl) {
    if (isTabPage(item.targetUrl)) {
      uni.switchTab({ url: item.targetUrl.split('?')[0] })
    } else {
      uni.navigateTo({ url: item.targetUrl })
    }
    return
  }

  if (item.targetUrl) {
    if (isTabPage(item.targetUrl)) {
      uni.switchTab({ url: item.targetUrl.split('?')[0] })
    } else {
      uni.navigateTo({ url: item.targetUrl })
    }
    return
  }

  uni.showToast({ title: item.title || '服务详情', icon: 'none' })
}

const shortTitle = (title) => {
  if (!title) return '服'
  return title.substring(0, 2)
}

onLoad(async (options) => {
  const fromCategory = options && options.category ? decodeURIComponent(options.category) : ''

  await loadCategories()

  const names = categories.value.map(c => c.name)
  if (fromCategory && names.includes(fromCategory)) {
    currentCategory.value = fromCategory
  }

  loadItems()
})
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  background: #f2f5f8;
}

.hero {
  padding: 20px 16px 14px;
  background: linear-gradient(160deg, #2a9d8f 0%, #1f7a6e 100%);
  border-radius: 0 0 20px 20px;
}

.hero-title {
  display: block;
  font-size: 22px;
  font-weight: 800;
  color: #fff;
  line-height: 1.2;
}

.hero-sub {
  display: block;
  margin-top: 5px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.82);
}

.search-box {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  flex: 1;
  height: 36px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.25);
  color: #fff;
  font-size: 13px;
  padding: 0 12px;
}

.search-btn {
  height: 36px;
  padding: 0 14px;
  border-radius: 18px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-btn-t {
  font-size: 12px;
  font-weight: 700;
  color: #2a9d8f;
}

.tabs {
  margin-top: 10px;
  white-space: nowrap;
  padding: 0 12px;
}

.tab {
  width: 70px;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  margin-right: 8px;
  padding: 10px 6px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid transparent;
}

.tab-on {
  border-color: rgba(42, 157, 143, 0.45);
  box-shadow: 0 4px 12px rgba(42, 157, 143, 0.14);
}

.tab-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-char {
  font-size: 14px;
  font-weight: 800;
  color: #fff;
}

.tab-name {
  margin-top: 6px;
  font-size: 11px;
  color: #444;
}

.loading {
  padding: 28px 0;
  text-align: center;
}

.loading-t {
  font-size: 13px;
  color: #999;
}

.list {
  padding: 10px 12px 20px;
}

.item {
  background: #fff;
  border-radius: 14px;
  padding: 12px;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}

.item-cover {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  margin-right: 10px;
}

.item-img {
  width: 100%;
  height: 100%;
}

.item-img-ph {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #dfe6e9, #b2bec3);
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-img-char {
  font-size: 20px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.72);
}

.item-main {
  flex: 1;
  min-width: 0;
}

.item-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-title {
  flex: 1;
  min-width: 0;
  font-size: 15px;
  font-weight: 700;
  color: #1a1a2e;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-badge {
  font-size: 10px;
  color: #2a9d8f;
  background: rgba(42, 157, 143, 0.1);
  padding: 2px 6px;
  border-radius: 12px;
  flex-shrink: 0;
}

.item-summary {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #666;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-meta {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.item-tag {
  font-size: 10px;
  color: #666;
  background: #f4f4f4;
  padding: 2px 6px;
  border-radius: 10px;
}

.item-price {
  font-size: 11px;
  color: #f39c12;
  font-weight: 700;
}

.item-action {
  margin-left: 8px;
  padding: 7px 10px;
  background: #2a9d8f;
  border-radius: 14px;
  flex-shrink: 0;
}

.item-action-t {
  font-size: 11px;
  color: #fff;
  font-weight: 700;
}

.empty {
  margin-top: 60px;
  text-align: center;
}

.empty-t {
  display: block;
  font-size: 14px;
  color: #888;
}

.empty-sub {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #b6b6b6;
}
</style>
