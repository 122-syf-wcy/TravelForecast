<template>
  <view class="detail-page">
    <!-- 顶部图片区域 -->
    <view class="hero-section">
      <view class="hero-bg" :style="{ background: coverColor }">
        <image v-if="coverImg" class="hero-img" :src="coverImg" mode="aspectFill" />
        <view v-else class="hero-placeholder">
          <view class="hero-emoji">{{ categoryEmoji }}</view>
          <text class="hero-char">{{ goods.name ? goods.name.substring(0, 4) : '文创' }}</text>
        </view>
      </view>
      <!-- 返回按钮 -->
      <view class="hero-back" @tap="goBack">
        <text class="hero-back-icon">&lt;</text>
      </view>
      <!-- 分享按钮 -->
      <view class="hero-share" @tap="onShare">
        <text class="hero-share-icon">···</text>
      </view>
      <!-- 图片指示器 -->
      <view class="hero-indicator">
        <view class="hero-dot hero-dot-active" />
        <view class="hero-dot" />
        <view class="hero-dot" />
      </view>
    </view>

    <!-- 价格卡片 -->
    <view class="price-card">
      <view class="price-row">
        <view class="price-main">
          <text class="price-sym">¥</text>
          <text class="price-val">{{ goods.price || '0.00' }}</text>
          <text class="price-orig" v-if="goods.originalPrice">¥{{ goods.originalPrice }}</text>
        </view>
        <view class="price-discount" v-if="goods.originalPrice">
          <text class="price-discount-t">{{ discountPercent }}折</text>
        </view>
      </view>
      <view class="price-qd" v-if="goods.qdPrice">
        <view class="qd-coin" />
        <text class="qd-text">可用 {{ goods.qdPrice }} 黔豆抵扣</text>
      </view>
    </view>

    <!-- 商品信息 -->
    <view class="info-card">
      <text class="info-name">{{ goods.name || '商品详情' }}</text>
      <text class="info-desc">{{ goods.description || '' }}</text>
      <view class="info-badges">
        <view class="badge" v-for="(tag, i) in tagList" :key="i" :style="{ background: badgeColors[i % badgeColors.length].bg }">
          <text class="badge-t" :style="{ color: badgeColors[i % badgeColors.length].fg }">{{ tag }}</text>
        </view>
        <view class="badge badge-sales" v-if="goods.sales">
          <text class="badge-t" style="color:#999;">已售 {{ formatSales(goods.sales) }}</text>
        </view>
      </view>
    </view>

    <!-- 服务保障 -->
    <view class="service-card">
      <view class="service-item">
        <view class="service-check" />
        <text class="service-t">正品保障</text>
      </view>
      <view class="service-item">
        <view class="service-check" />
        <text class="service-t">7天无理由</text>
      </view>
      <view class="service-item">
        <view class="service-check" />
        <text class="service-t">全国包邮</text>
      </view>
      <view class="service-item">
        <view class="service-check" />
        <text class="service-t">非遗认证</text>
      </view>
    </view>

    <!-- 商品规格 -->
    <view class="spec-card">
      <view class="spec-title-row">
        <text class="spec-title">规格选择</text>
      </view>
      <view class="spec-options">
        <view class="spec-opt" :class="{ 'spec-opt-on': specIdx === i }" v-for="(s, i) in specs" :key="i" @tap="specIdx = i">
          <text class="spec-opt-t">{{ s }}</text>
        </view>
      </view>
      <view class="qty-row">
        <text class="qty-label">购买数量</text>
        <view class="qty-ctrl">
          <view class="qty-btn" :class="{ 'qty-btn-disabled': qty <= 1 }" @tap="qty > 1 && qty--">
            <text class="qty-btn-t">−</text>
          </view>
          <text class="qty-num">{{ qty }}</text>
          <view class="qty-btn" @tap="qty++">
            <text class="qty-btn-t">+</text>
          </view>
        </view>
      </view>
      <view class="stock-row">
        <text class="stock-t">库存 {{ goods.stock || 999 }} 件</text>
      </view>
    </view>

    <!-- 商品详情描述 -->
    <view class="detail-card">
      <view class="detail-hd">
        <view class="detail-line" />
        <text class="detail-hd-t">商品详情</text>
        <view class="detail-line" />
      </view>
      <view class="detail-body">
        <view class="detail-feature" v-for="(f, i) in features" :key="i">
          <view class="feature-icon" :style="{ background: f.color }">
            <text class="feature-emoji">{{ f.emoji }}</text>
          </view>
          <view class="feature-text">
            <text class="feature-title">{{ f.title }}</text>
            <text class="feature-desc">{{ f.desc }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 用户评价 -->
    <view class="review-card">
      <view class="review-hd">
        <text class="review-title">用户评价</text>
        <text class="review-more">好评率 98% ></text>
      </view>
      <view class="review-item" v-for="(r, i) in reviews" :key="i">
        <view class="reviewer">
          <view class="reviewer-avatar" :style="{ background: r.color }">
            <text class="reviewer-initial">{{ r.name[0] }}</text>
          </view>
          <view class="reviewer-info">
            <text class="reviewer-name">{{ r.name }}</text>
            <view class="reviewer-stars">
              <text class="star" v-for="s in 5" :key="s">{{ s <= r.stars ? '★' : '☆' }}</text>
            </view>
          </view>
          <text class="reviewer-date">{{ r.date }}</text>
        </view>
        <text class="review-text">{{ r.text }}</text>
      </view>
    </view>

    <!-- 底部留白给 action bar -->
    <view style="height: 120px;" />

    <!-- 底部操作栏 -->
    <view class="action-bar">
      <view class="action-left">
        <view class="action-icon-wrap" @tap="goHome">
          <view class="action-home-icon" />
          <text class="action-icon-label">首页</text>
        </view>
        <view class="action-icon-wrap" @tap="addCartHandler">
          <view class="action-cart-icon" />
          <text class="action-icon-label">购物车</text>
        </view>
      </view>
      <view class="action-right">
        <view class="action-btn action-btn-cart" @tap="addCartHandler">
          <text class="action-btn-t">加入购物车</text>
        </view>
        <view class="action-btn action-btn-buy" @tap="buyNow">
          <text class="action-btn-t" style="color:#fff;">立即购买</text>
        </view>
      </view>
    </view>

    <!-- 模拟微信支付 -->
    <mock-pay
      :visible="showPay"
      :orderId="payOrderId"
      :orderNo="payOrderNo"
      :amount="payAmount"
      :pointsBalance="pointsBalance"
      @close="showPay = false"
      @success="onPaySuccess"
      @fail="showPay = false"
    />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchProductDetail, addToCart, createOrder } from '@/api/shop'
import { resolveAssetUrl } from '@/utils/url'

const goods = ref({})
const qty = ref(1)
const specIdx = ref(0)
const coverImg = ref('')
const showPay = ref(false)
const payOrderId = ref('')
const payOrderNo = ref('')
const payAmount = ref(0)
const pointsBalance = ref(0)

const badgeColors = [
  { bg: '#FFF0E6', fg: '#FF9F43' },
  { bg: '#E8F8F5', fg: '#2A9D8F' },
  { bg: '#F0E6FF', fg: '#6C5CE7' },
  { bg: '#FFE6E9', fg: '#E84393' }
]

const categoryColorMap = {
  '非遗文创': 'linear-gradient(135deg, #a29bfe 0%, #6c5ce7 100%)',
  '地道美食': 'linear-gradient(135deg, #fd79a8 0%, #e17055 100%)',
  '茶饮伴手': 'linear-gradient(135deg, #55efc4 0%, #00b894 100%)',
  '手工艺品': 'linear-gradient(135deg, #fdcb6e 0%, #e17055 100%)'
}

const emojiMap = {
  '非遗文创': '🎨', '地道美食': '🍜', '茶饮伴手': '🍵', '手工艺品': '✂️'
}

const coverColor = computed(() => categoryColorMap[goods.value.category] || 'linear-gradient(135deg, #ffeaa7 0%, #fdcb6e 100%)')
const categoryEmoji = computed(() => emojiMap[goods.value.category] || '🎁')
const tagList = computed(() => (goods.value.tags || '').split(',').filter(t => t.trim()))

const discountPercent = computed(() => {
  if (!goods.value.originalPrice || !goods.value.price) return ''
  return Math.round((goods.value.price / goods.value.originalPrice) * 10)
})

const specs = computed(() => {
  const cat = goods.value.category
  if (cat === '地道美食') return ['标准装', '家庭装', '礼盒装']
  if (cat === '茶饮伴手') return ['100g', '250g', '500g礼盒']
  if (cat === '非遗文创') return ['标准款', '精装款', '收藏款']
  return ['标准款', '精装款']
})

const features = computed(() => {
  const cat = goods.value.category
  if (cat === '地道美食') return [
    { emoji: '🏔️', title: '高原直供', desc: '来自六盘水海拔1800m+的天然产区', color: '#E8F8F5' },
    { emoji: '📦', title: '真空锁鲜', desc: '采用先进真空包装，保留原始风味', color: '#FFF0E6' },
    { emoji: '🚚', title: '产地直发', desc: '48小时内从产区直发到您手中', color: '#F0E6FF' }
  ]
  if (cat === '非遗文创') return [
    { emoji: '🎨', title: '非遗传承', desc: '由省级非遗传承人手工制作', color: '#F0E6FF' },
    { emoji: '📜', title: '文化证书', desc: '附带非遗文化认证证书', color: '#E8F8F5' },
    { emoji: '🎁', title: '精美包装', desc: '文化主题礼盒，送礼体面', color: '#FFF0E6' }
  ]
  if (cat === '茶饮伴手') return [
    { emoji: '🍃', title: '有机种植', desc: '高海拔有机茶园，无农药残留', color: '#E8F8F5' },
    { emoji: '🏆', title: '获奖品质', desc: '多次获得贵州省名优茶评比金奖', color: '#FFF0E6' },
    { emoji: '🎁', title: '送礼佳品', desc: '精美礼盒包装，传递六盘水温度', color: '#F0E6FF' }
  ]
  return [
    { emoji: '✨', title: '品质保障', desc: '严选六盘水地方特色好物', color: '#E8F8F5' },
    { emoji: '🎁', title: '精美包装', desc: '旅途好物，精心包装送达', color: '#FFF0E6' },
    { emoji: '🚚', title: '快速发货', desc: '下单后48小时内发出', color: '#F0E6FF' }
  ]
})

const reviews = [
  { name: '小红', color: '#FF9F43', stars: 5, date: '01-15', text: '非常正宗！包装精美，送朋友很有面子，六盘水的特色值得推荐～' },
  { name: '旅行者', color: '#2A9D8F', stars: 5, date: '01-08', text: '在景区看到的，回来就下单了，品质和在景区买的一模一样，性价比更高！' },
  { name: '美食家', color: '#6C5CE7', stars: 4, date: '12-20', text: '味道很好，就是量可以再多一些。总体满意，会回购的。' }
]

const formatSales = (n) => n > 9999 ? (n / 10000).toFixed(1) + '万' : n > 999 ? (n / 1000).toFixed(1) + 'k' : n

const getUserId = () => {
  const u = uni.getStorageSync('userInfo')
  return u && u.userId ? u.userId : null
}

const goBack = () => uni.navigateBack()
const goHome = () => uni.switchTab({ url: '/pages/index/index' })
const onShare = () => uni.showToast({ title: '分享功能开发中', icon: 'none' })

const addCartHandler = async () => {
  const userId = getUserId()
  if (!userId) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
  try {
    await addToCart(userId, goods.value.id, qty.value)
    uni.showToast({ title: '已加入购物车', icon: 'success' })
  } catch (e) { uni.showToast({ title: '加入失败', icon: 'none' }) }
}

const buyNow = async () => {
  const userId = getUserId()
  if (!userId) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
  try {
    const items = [{
      productId: goods.value.id,
      productName: goods.value.name,
      productImage: goods.value.imageUrl,
      price: goods.value.price,
      quantity: qty.value
    }]
    const order = await createOrder(userId, items)
    payOrderId.value = order.id
    payOrderNo.value = order.orderNo
    payAmount.value = order.totalAmount
    showPay.value = true
  } catch (e) { uni.showToast({ title: '下单失败', icon: 'none' }) }
}

const onPaySuccess = () => {
  showPay.value = false
  uni.showToast({ title: '支付成功', icon: 'success' })
}

onLoad(async (options) => {
  if (options && options.id) {
    try {
      const data = await fetchProductDetail(options.id)
      if (data) {
        goods.value = data
        if (data.imageUrl) coverImg.value = resolveAssetUrl(data.imageUrl)
      }
    } catch (e) { /* fallback */ }
  }
})
</script>

<style lang="scss">
@import "@/uni.scss";

.detail-page { min-height: 100vh; background: #F5F6FA; }

/* ======== 顶部图片 ======== */
.hero-section { position: relative; }
.hero-bg { width: 100%; height: 360px; overflow: hidden; position: relative; }
.hero-img { width: 100%; height: 100%; }
.hero-placeholder {
  width: 100%; height: 100%;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.hero-emoji { font-size: 64px; margin-bottom: 8px; }
.hero-char { font-size: 32px; font-weight: 900; color: rgba(255,255,255,0.7); letter-spacing: 4px; }
.hero-back {
  position: absolute; top: 48px; left: 16px;
  width: 36px; height: 36px; border-radius: 50%;
  background: rgba(0,0,0,0.25); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
}
.hero-back-icon { font-size: 18px; color: #fff; font-weight: 600; }
.hero-share {
  position: absolute; top: 48px; right: 16px;
  width: 36px; height: 36px; border-radius: 50%;
  background: rgba(0,0,0,0.25); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
}
.hero-share-icon { font-size: 14px; color: #fff; font-weight: 800; letter-spacing: -1px; }
.hero-indicator {
  position: absolute; bottom: 16px; left: 50%;
  transform: translateX(-50%);
  display: flex; gap: 6px;
}
.hero-dot { width: 6px; height: 6px; border-radius: 50%; background: rgba(255,255,255,0.4); }
.hero-dot-active { width: 18px; border-radius: 3px; background: #fff; }

/* ======== 价格卡片 ======== */
.price-card {
  margin: -24px 16px 0; position: relative; z-index: 2;
  background: linear-gradient(135deg, #FF6B6B 0%, #FF9F43 100%);
  border-radius: 16px; padding: 16px 18px;
  box-shadow: 0 6px 20px rgba(255,107,107,0.3);
}
.price-row { display: flex; align-items: center; justify-content: space-between; }
.price-main { display: flex; align-items: baseline; }
.price-sym { font-size: 16px; color: #fff; font-weight: 700; }
.price-val { font-size: 32px; color: #fff; font-weight: 900; margin-left: 2px; }
.price-orig { font-size: 14px; color: rgba(255,255,255,0.6); text-decoration: line-through; margin-left: 10px; }
.price-discount {
  padding: 3px 10px; background: rgba(255,255,255,0.2);
  border-radius: 20px; border: 1px solid rgba(255,255,255,0.3);
}
.price-discount-t { font-size: 12px; color: #fff; font-weight: 600; }
.price-qd {
  margin-top: 10px; display: flex; align-items: center;
  padding: 6px 12px; background: rgba(255,255,255,0.15);
  border-radius: 8px;
}
.qd-coin {
  width: 14px; height: 14px; border-radius: 50%;
  background: linear-gradient(135deg, #FFD93D, #F39C12);
  margin-right: 6px; border: 1px solid rgba(255,255,255,0.4);
}
.qd-text { font-size: 12px; color: rgba(255,255,255,0.9); }

/* ======== 商品信息 ======== */
.info-card {
  margin: 12px 16px 0; background: #fff; border-radius: 16px;
  padding: 18px; box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}
.info-name { font-size: 18px; font-weight: 800; color: #1A1A2E; line-height: 1.4; }
.info-desc { display: block; font-size: 13px; color: #888; margin-top: 6px; line-height: 1.5; }
.info-badges { margin-top: 12px; display: flex; flex-wrap: wrap; gap: 8px; }
.badge { padding: 4px 12px; border-radius: 20px; }
.badge-t { font-size: 11px; font-weight: 600; }
.badge-sales { background: #F5F6FA; }

/* ======== 服务保障 ======== */
.service-card {
  margin: 10px 16px 0; background: #fff; border-radius: 14px;
  padding: 14px 18px; display: flex; justify-content: space-between;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}
.service-item { display: flex; align-items: center; }
.service-check {
  width: 14px; height: 14px; border-radius: 50%;
  background: linear-gradient(135deg, #2A9D8F, #20B2AA);
  margin-right: 4px; position: relative;
}
.service-check::after {
  content: ''; position: absolute;
  left: 4px; top: 3px; width: 4px; height: 7px;
  border: solid #fff; border-width: 0 1.5px 1.5px 0;
  transform: rotate(45deg);
}
.service-t { font-size: 11px; color: #666; }

/* ======== 规格选择 ======== */
.spec-card {
  margin: 10px 16px 0; background: #fff; border-radius: 16px;
  padding: 18px; box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}
.spec-title-row { margin-bottom: 12px; }
.spec-title { font-size: 15px; font-weight: 700; color: #1A1A2E; }
.spec-options { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; }
.spec-opt {
  padding: 8px 20px; border-radius: 10px;
  background: #F5F6FA; border: 1.5px solid transparent;
  transition: all 0.2s;
}
.spec-opt-on { background: #E8F8F5; border-color: #2A9D8F; }
.spec-opt-t { font-size: 13px; color: #666; }
.spec-opt-on .spec-opt-t { color: #2A9D8F; font-weight: 600; }
.qty-row {
  display: flex; align-items: center; justify-content: space-between;
  padding-top: 14px; border-top: 1px solid #F5F6FA;
}
.qty-label { font-size: 14px; color: #333; font-weight: 500; }
.qty-ctrl { display: flex; align-items: center; gap: 0; }
.qty-btn {
  width: 32px; height: 32px; border-radius: 8px;
  background: #F5F6FA; display: flex; align-items: center; justify-content: center;
}
.qty-btn-disabled { opacity: 0.4; }
.qty-btn-t { font-size: 18px; color: #333; font-weight: 600; }
.qty-num {
  font-size: 16px; font-weight: 700; color: #1A1A2E;
  min-width: 44px; text-align: center;
}
.stock-row { margin-top: 8px; }
.stock-t { font-size: 12px; color: #bbb; }

/* ======== 商品详情 ======== */
.detail-card {
  margin: 10px 16px 0; background: #fff; border-radius: 16px;
  padding: 18px; box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}
.detail-hd { display: flex; align-items: center; justify-content: center; margin-bottom: 16px; }
.detail-line { flex: 1; height: 1px; background: linear-gradient(90deg, transparent, #ddd, transparent); }
.detail-hd-t { font-size: 14px; font-weight: 700; color: #1A1A2E; padding: 0 14px; }
.detail-body {}
.detail-feature {
  display: flex; align-items: flex-start; padding: 12px 0;
  border-bottom: 1px solid #F5F6FA;
}
.detail-feature:last-child { border-bottom: none; padding-bottom: 0; }
.feature-icon {
  width: 42px; height: 42px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  margin-right: 12px; flex-shrink: 0;
}
.feature-emoji { font-size: 22px; }
.feature-text { flex: 1; }
.feature-title { display: block; font-size: 14px; font-weight: 700; color: #1A1A2E; margin-bottom: 2px; }
.feature-desc { font-size: 12px; color: #999; line-height: 1.5; }

/* ======== 用户评价 ======== */
.review-card {
  margin: 10px 16px 0; background: #fff; border-radius: 16px;
  padding: 18px; box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}
.review-hd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.review-title { font-size: 15px; font-weight: 700; color: #1A1A2E; }
.review-more { font-size: 12px; color: #2A9D8F; }
.review-item { padding: 12px 0; border-top: 1px solid #F5F6FA; }
.review-item:first-of-type { border-top: none; }
.reviewer { display: flex; align-items: center; margin-bottom: 8px; }
.reviewer-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  margin-right: 8px;
}
.reviewer-initial { font-size: 14px; color: #fff; font-weight: 700; }
.reviewer-info { flex: 1; }
.reviewer-name { display: block; font-size: 13px; font-weight: 600; color: #333; }
.reviewer-stars { display: flex; gap: 1px; }
.star { font-size: 12px; color: #FFD93D; }
.reviewer-date { font-size: 11px; color: #ccc; }
.review-text { font-size: 13px; color: #666; line-height: 1.6; }

/* ======== 底部操作栏 ======== */
.action-bar {
  position: fixed; bottom: 0; left: 0; right: 0;
  background: #fff; padding: 10px 16px;
  padding-bottom: calc(10px + env(safe-area-inset-bottom));
  display: flex; align-items: center;
  box-shadow: 0 -4px 20px rgba(0,0,0,0.08);
  z-index: 100;
}
.action-left { display: flex; gap: 16px; margin-right: 14px; }
.action-icon-wrap { display: flex; flex-direction: column; align-items: center; }
.action-home-icon {
  width: 22px; height: 20px; position: relative;
  border-bottom: 2px solid #666; margin-bottom: 2px;
}
.action-home-icon::before {
  content: ''; position: absolute; top: -2px; left: 50%;
  transform: translateX(-50%);
  border-left: 12px solid transparent; border-right: 12px solid transparent;
  border-bottom: 10px solid #666;
}
.action-cart-icon {
  width: 20px; height: 18px; position: relative;
  border: 2px solid #666; border-radius: 0 0 4px 4px; margin-bottom: 2px;
}
.action-cart-icon::before {
  content: ''; position: absolute; top: -7px; left: 3px;
  width: 10px; height: 5px;
  border: 2px solid #666; border-bottom: none; border-radius: 5px 5px 0 0;
}
.action-icon-label { font-size: 10px; color: #999; }
.action-right { flex: 1; display: flex; gap: 0; }
.action-btn {
  flex: 1; height: 44px; display: flex; align-items: center; justify-content: center;
}
.action-btn-cart {
  background: linear-gradient(135deg, #FFD93D, #FF9F43);
  border-radius: 22px 0 0 22px;
}
.action-btn-buy {
  background: linear-gradient(135deg, #FF6B6B, #E55039);
  border-radius: 0 22px 22px 0;
}
.action-btn-t { font-size: 14px; font-weight: 700; color: #fff; }
</style>
