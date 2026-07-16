<template>
  <view class="page">
    <view class="search-bar">
      <view class="search-input-box">
        <icon type="search" size="14" color="#999" />
        <input class="search-input" type="text" placeholder="搜索景区 / 商品"
          v-model="keyword" confirm-type="search" @confirm="doSearch" :focus="true" />
        <icon v-if="keyword" type="clear" size="14" color="#ccc" @tap="keyword = ''" />
      </view>
      <text class="search-btn" @tap="doSearch">搜索</text>
    </view>

    <view class="section" v-if="!hasResults && history.length > 0">
      <view class="sec-hd">
        <text class="sec-title">历史搜索</text>
        <icon type="cancel" size="14" color="#ccc" @tap="clearHistory" />
      </view>
      <view class="tags">
        <view class="tag" v-for="(h, i) in history" :key="i" @tap="onTag(h)">{{ h }}</view>
      </view>
    </view>

    <view class="section" v-if="!hasResults">
      <view class="sec-hd">
        <text class="sec-title">热门搜索</text>
      </view>
      <view class="tags">
        <view class="tag tag-hot" v-for="(h, i) in hotKeys" :key="i" @tap="onTag(h)">{{ h }}</view>
      </view>
    </view>

    <view class="loading-box" v-if="searching">
      <text class="loading-t">搜索中...</text>
    </view>

    <view class="results" v-if="hasResults && !searching">
      <!-- 分类 Tab -->
      <view class="type-tabs">
        <view :class="['type-tab', activeTab === 'all' && 'type-tab-active']" @tap="activeTab = 'all'">
          全部<text class="tab-count" v-if="totalCount > 0">({{ totalCount }})</text>
        </view>
        <view :class="['type-tab', activeTab === 'spot' && 'type-tab-active']" @tap="activeTab = 'spot'">
          景区<text class="tab-count" v-if="spotResults.length > 0">({{ spotResults.length }})</text>
        </view>
        <view :class="['type-tab', activeTab === 'product' && 'type-tab-active']" @tap="activeTab = 'product'">
          商品<text class="tab-count" v-if="productResults.length > 0">({{ productResults.length }})</text>
        </view>
      </view>

      <view class="res-item" v-for="(r, i) in filteredResults" :key="r.type + '-' + i" @tap="goDetail(r)">
        <image class="res-img" :src="r.img" mode="aspectFill" />
        <view class="res-info">
          <text class="res-name">{{ r.name }}</text>
          <text class="res-desc">{{ r.desc }}</text>
          <view class="res-tags">
            <text :class="['res-tag', r.type === 'product' ? 'res-tag-product' : '']">{{ r.typeLabel }}</text>
            <text class="res-tag res-tag-loc" v-if="r.extra">{{ r.extra }}</text>
          </view>
          <text class="res-price" v-if="r.price">¥{{ r.price }}</text>
        </view>
      </view>

      <view class="empty" v-if="filteredResults.length === 0 && searched">
        <text class="empty-t">未找到"{{ keyword }}"相关内容</text>
        <text class="empty-sub">换个关键词试试吧</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { searchAll } from '@/api/search'
import { resolveAssetUrl } from '@/utils/url'

const keyword = ref('')
const searched = ref(false)
const searching = ref(false)
const activeTab = ref('all')
const history = ref(uni.getStorageSync('searchHistory') || [])
const hotKeys = ref(['乌蒙大草原', '三线建设博物馆', '梅花山', '水城古镇', '烙锅', '羊肉粉'])
const spotResults = ref([])
const productResults = ref([])

const hasResults = computed(() => searched.value && (spotResults.value.length > 0 || productResults.value.length > 0 || keyword.value.trim()))
const totalCount = computed(() => spotResults.value.length + productResults.value.length)

const filteredResults = computed(() => {
  if (activeTab.value === 'spot') return spotResults.value
  if (activeTab.value === 'product') return productResults.value
  return [...spotResults.value, ...productResults.value]
})

const doSearch = async () => {
  const kw = keyword.value.trim()
  if (!kw) return
  searched.value = true
  searching.value = true
  activeTab.value = 'all'

  const idx = history.value.indexOf(kw)
  if (idx > 0) history.value.splice(idx, 1)
  if (idx !== 0) {
    history.value.unshift(kw)
    if (history.value.length > 10) history.value = history.value.slice(0, 10)
    uni.setStorageSync('searchHistory', history.value)
  }

  try {
    const { spots, products } = await searchAll(kw)
    spotResults.value = spots.map(s => ({
      type: 'spot',
      typeLabel: s.category || '景区',
      id: s.id || s.spotCode,
      name: s.name || '景区',
      desc: s.description || s.address || '',
      extra: s.city || s.address || '',
      img: resolveAssetUrl(s.imageUrl) || '/static/default-spot.svg',
      price: null
    }))
    productResults.value = products.map(p => ({
      type: 'product',
      typeLabel: p.category || '商品',
      id: p.id,
      name: p.name || '商品',
      desc: p.description || '',
      extra: p.sales ? `已售${p.sales}` : '',
      img: resolveAssetUrl(p.imageUrl) || '/static/default-spot.svg',
      price: p.price || p.pointsPrice
    }))
  } catch (e) {
    spotResults.value = []
    productResults.value = []
    uni.showToast({ title: '搜索失败，请重试', icon: 'none' })
  } finally {
    searching.value = false
  }
}

const onTag = (k) => {
  keyword.value = k
  doSearch()
}

const clearHistory = () => {
  history.value = []
  uni.removeStorageSync('searchHistory')
}

const goDetail = (r) => {
  if (!r.id) {
    uni.showToast({ title: r.name || '暂无详情', icon: 'none' })
    return
  }
  if (r.type === 'product') {
    uni.navigateTo({ url: `/pages/shop/detail?id=${encodeURIComponent(r.id)}` })
  } else {
    uni.navigateTo({ url: `/pages/spot/detail?id=${encodeURIComponent(r.id)}` })
  }
}
</script>

<style lang="scss">
.page { min-height: 100vh; background: #fff; padding: 0 16px; }
.search-bar { display: flex; align-items: center; padding: 10px 0; }
.search-input-box {
  flex: 1; height: 36px; background: #f5f5f5; border-radius: 18px;
  display: flex; align-items: center; padding: 0 12px; margin-right: 12px;
}
.search-input { flex: 1; font-size: 14px; margin: 0 8px; }
.search-btn { font-size: 14px; color: #2A9D8F; font-weight: 600; }

.section { margin-top: 24px; }
.sec-hd { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.sec-title { font-size: 15px; font-weight: 700; color: #333; }
.tags { display: flex; flex-wrap: wrap; gap: 10px; }
.tag { padding: 6px 14px; background: #f5f5f5; border-radius: 16px; font-size: 12px; color: #666; }
.tag-hot { color: #2A9D8F; background: rgba(42,157,143,0.1); }

.loading-box { text-align: center; margin-top: 60px; }
.loading-t { font-size: 14px; color: #999; }

.type-tabs { display: flex; gap: 0; margin-bottom: 16px; border-bottom: 1px solid #f0f0f0; }
.type-tab {
  flex: 1; text-align: center; padding: 10px 0; font-size: 14px; color: #999;
  position: relative; transition: color 0.2s;
}
.type-tab-active { color: #2A9D8F; font-weight: 600; }
.type-tab-active::after {
  content: ''; position: absolute; bottom: 0; left: 50%; transform: translateX(-50%);
  width: 24px; height: 2px; background: #2A9D8F; border-radius: 1px;
}
.tab-count { font-size: 11px; margin-left: 2px; }

.results { margin-top: 12px; }
.res-item { display: flex; margin-bottom: 16px; }
.res-img { width: 90px; height: 70px; border-radius: 10px; margin-right: 12px; background: #eee; flex-shrink: 0; }
.res-info { flex: 1; display: flex; flex-direction: column; justify-content: space-between; }
.res-name { font-size: 15px; font-weight: 700; color: #333; }
.res-desc { font-size: 12px; color: #999; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.res-tags { display: flex; gap: 6px; }
.res-tag { font-size: 10px; color: #2A9D8F; background: rgba(42,157,143,0.1); padding: 2px 6px; border-radius: 4px; }
.res-tag-product { color: #e67e22; background: rgba(230,126,34,0.1); }
.res-tag-loc { color: #666; background: #f0f0f0; }
.res-price { font-size: 14px; color: #e74c3c; font-weight: 700; margin-top: 2px; }
.empty { text-align: center; margin-top: 40px; }
.empty-t { display: block; font-size: 14px; color: #999; }
.empty-sub { display: block; font-size: 12px; color: #ccc; margin-top: 6px; }
</style>
