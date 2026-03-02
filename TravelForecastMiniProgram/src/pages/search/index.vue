<template>
  <view class="page">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <view class="search-input-box">
        <icon type="search" size="14" color="#999" />
        <input class="search-input" type="text" placeholder="搜索景区 / 美食 / 攻略"
          v-model="keyword" confirm-type="search" @confirm="doSearch" :focus="true" />
        <icon v-if="keyword" type="clear" size="14" color="#ccc" @tap="keyword = ''" />
      </view>
      <text class="search-btn" @tap="doSearch">搜索</text>
    </view>

    <!-- 搜索历史 -->
    <view class="section" v-if="!keyword && history.length > 0">
      <view class="sec-hd">
        <text class="sec-title">历史搜索</text>
        <icon type="cancel" size="14" color="#ccc" @tap="clearHistory" />
      </view>
      <view class="tags">
        <view class="tag" v-for="(h, i) in history" :key="i" @tap="onTag(h)">{{ h }}</view>
      </view>
    </view>

    <!-- 热门搜索 -->
    <view class="section" v-if="!keyword">
      <view class="sec-hd">
        <text class="sec-title">热门搜索</text>
      </view>
      <view class="tags">
        <view class="tag tag-hot" v-for="(h, i) in hotKeys" :key="i" @tap="onTag(h)">{{ h }}</view>
      </view>
    </view>

    <!-- 搜索中 -->
    <view class="loading-box" v-if="searching">
      <text class="loading-t">搜索中...</text>
    </view>

    <!-- 搜索结果 -->
    <view class="results" v-if="keyword && !searching">
      <view class="res-item" v-for="(r, i) in results" :key="i" @tap="goDetail(r)">
        <image class="res-img" :src="r.img" mode="aspectFill" />
        <view class="res-info">
          <text class="res-name">{{ r.name }}</text>
          <text class="res-desc">{{ r.desc }}</text>
          <view class="res-tags">
            <text class="res-tag" v-if="r.category">{{ r.category }}</text>
            <text class="res-tag res-tag-loc" v-if="r.loc">{{ r.loc }}</text>
          </view>
        </view>
      </view>
      <view class="empty" v-if="results.length === 0 && searched">
        <text class="empty-t">未找到"{{ keyword }}"相关内容</text>
        <text class="empty-sub">换个关键词试试吧</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { searchAll } from '@/api/search'
import { resolveAssetUrl } from '@/utils/url'

const keyword = ref('')
const searched = ref(false)
const searching = ref(false)
const history = ref(uni.getStorageSync('searchHistory') || [])
const hotKeys = ref(['乌蒙大草原', '三线建设博物馆', '梅花山', '水城古镇', '烙锅', '羊肉粉'])
const results = ref([])

const doSearch = async () => {
  const kw = keyword.value.trim()
  if (!kw) return
  searched.value = true
  searching.value = true

  // 保存搜索历史
  if (!history.value.includes(kw)) {
    history.value.unshift(kw)
    if (history.value.length > 10) history.value = history.value.slice(0, 10)
    uni.setStorageSync('searchHistory', history.value)
  }

  try {
    const spots = await searchAll(kw)
    if (Array.isArray(spots)) {
      results.value = spots.map(s => ({
        id: s.id || s.spotCode,
        name: s.name || '景区',
        desc: s.description || s.address || '',
        category: s.category || '景区',
        loc: s.city || s.address || '',
        img: resolveAssetUrl(s.imageUrl) || 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=200&q=80'
      }))
    } else {
      results.value = []
    }
  } catch (e) {
    results.value = []
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
  if (r.id) {
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

.results { margin-top: 20px; }
.res-item { display: flex; margin-bottom: 16px; }
.res-img { width: 90px; height: 70px; border-radius: 10px; margin-right: 12px; background: #eee; flex-shrink: 0; }
.res-info { flex: 1; display: flex; flex-direction: column; justify-content: space-between; }
.res-name { font-size: 15px; font-weight: 700; color: #333; }
.res-desc { font-size: 12px; color: #999; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.res-tags { display: flex; gap: 6px; }
.res-tag { font-size: 10px; color: #2A9D8F; background: rgba(42,157,143,0.1); padding: 2px 6px; border-radius: 4px; }
.res-tag-loc { color: #666; background: #f0f0f0; }
.empty { text-align: center; margin-top: 40px; }
.empty-t { display: block; font-size: 14px; color: #999; }
.empty-sub { display: block; font-size: 12px; color: #ccc; margin-top: 6px; }
</style>
