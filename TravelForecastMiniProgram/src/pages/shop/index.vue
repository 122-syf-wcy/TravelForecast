<template>
  <view class="shop-page">
    <!-- 头部 -->
    <view class="sh-hd" :style="{ paddingTop: stBar + 'px' }">
      <view class="sh-top">
        <text class="sh-title">文创商城</text>
        <view class="sh-cart" @tap="goCart">
          <view class="cart-ico">
            <image class="cart-icon-img" :src="commonIcons.cart" mode="aspectFit" />
          </view>
          <view class="cart-dot" v-if="cartNum > 0">
            <text class="cart-dot-t">{{ cartNum }}</text>
          </view>
        </view>
      </view>
      <view class="sh-search" @tap="onSearch">
        <view class="sh-s-ico">
          <image class="sh-s-icon-img" :src="commonIcons.search" mode="aspectFit" />
        </view>
        <text class="sh-s-txt">搜索特产 / 文创好物</text>
      </view>
      <!-- 黔豆 -->
      <view class="qd-bar">
        <view class="qd-l">
          <view class="qd-coin" />
          <text class="qd-lb">我的黔豆</text>
          <text class="qd-num">{{ qdBalance }}</text>
        </view>
        <view class="qd-btn" @tap="goQd">
          <text class="qd-btn-t">去兑换</text>
        </view>
      </view>
    </view>

    <!-- 寄回家 -->
    <view class="ship" @tap="onShip">
      <view class="ship-inner">
        <view class="ship-txt">
          <text class="ship-t1">一键寄回家</text>
          <text class="ship-t2">景区扫码购买，特产直达家门口</text>
        </view>
        <view class="ship-icon">
          <image class="ship-icon-img" :src="commonIcons.truck" mode="aspectFit" />
        </view>
      </view>
    </view>

    <view class="ship-extra">
      <view class="ship-extra-item" @tap="onShip">
        <text class="ship-extra-label">收货地址</text>
        <text class="ship-extra-value" :class="{ 'ship-extra-empty': !selectedAddressText }">{{ selectedAddressText || '去选择寄送地址' }}</text>
      </view>
      <view class="ship-extra-item" @tap="goCoupon">
        <text class="ship-extra-label">优惠券</text>
        <text class="ship-extra-value" :class="{ 'ship-extra-empty': !selectedCouponText }">{{ selectedCouponText || '去选择优惠券' }}</text>
      </view>
    </view>

    <!-- 分类 -->
    <scroll-view scroll-x class="tabs" :show-scrollbar="false">
      <view class="tab" v-for="(t, i) in tabs" :key="i"
        :class="{ 'tab-on': curTab === i }" @tap="curTab = i">
        <text class="tab-t">{{ t }}</text>
      </view>
    </scroll-view>

    <!-- 商品瀑布流 -->
    <view class="wf">
      <view class="wf-col" v-for="col in 2" :key="col">
        <view class="gd" v-for="(g, i) in colGoods(col-1)" :key="i" @tap="onGoods(g)">
          <view class="gd-img-w">
            <image v-if="g.imageUrl" class="gd-img-real" :src="g.imageUrl" mode="aspectFill" @error="g.imageUrl = ''" />
            <view v-else class="gd-img-placeholder" :style="{ background: g.color }">
              <image class="gd-img-icon" :src="g.icon" mode="aspectFit" />
            </view>
            <view class="gd-label" v-if="g.tag">
              <text class="gd-label-t">{{ g.tag }}</text>
            </view>
          </view>
          <view class="gd-info">
            <text class="gd-name">{{ g.name }}</text>
            <text class="gd-desc">{{ g.desc }}</text>
            <view class="gd-ft">
              <view class="gd-price">
                <text class="gd-yen">¥</text>
                <text class="gd-num">{{ g.price }}</text>
                <text class="gd-qd" v-if="g.qd">/ {{ g.qd }}豆</text>
              </view>
              <view class="gd-add" @tap.stop="addCartHandler(g)">
                <image class="gd-add-icon" :src="commonIcons.cart" mode="aspectFit" />
              </view>
            </view>
            <text class="gd-sales">已售 {{ g.sales }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 加载更多 -->
    <view class="load-more" v-if="goods.length > 0">
      <view v-if="loadingMore" class="load-more-inner">
        <text class="load-more-t">加载中...</text>
      </view>
      <view v-else-if="hasMore" class="load-more-inner" @tap="loadMore">
        <text class="load-more-t">点击加载更多</text>
      </view>
      <view v-else class="load-more-inner">
        <text class="load-more-t load-more-end">— 已经到底了 —</text>
      </view>
    </view>

    <!-- AI 悬浮球 -->
    <view class="ai-fab" @tap="onAi">
      <view class="ai-fab-bubble" v-if="showBubble">
        <text>{{ bubbleMsg }}</text>
      </view>
      <view class="ai-fab-btn">
        <image class="ai-fab-avatar" src="/static/dh-avatar.png" mode="aspectFill" />
      </view>
    </view>

    <!-- 模拟微信支付 -->
    <mock-pay
      :visible="showPay"
      :orderId="payOrderId"
      :orderNo="payOrderNo"
      :amount="payAmount"
      :pointsBalance="qdBalance"
      @close="closePay"
      @success="onPaySuccess"
      @fail="closePay"
    />

    <view style="height:110px;" />
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onReachBottom, onShow } from '@dcloudio/uni-app'
import { fetchProducts, addToCart, fetchCart, removeFromCart } from '@/api/shop'
import { getCheckInStatus } from '@/api/user'
import { resolveAssetUrl } from '@/utils/url'
import { COMMON_ICONS, resolveShopCategoryVisual } from '@/utils/icon-catalog'

const stBar = ref(20)
const commonIcons = COMMON_ICONS
const cartNum = ref(0)
const qdBalance = ref(0)
const curTab = ref(0)
const showBubble = ref(true)
const bubbleMsg = ref('刺梨汁维C很高哦')
const showPay = ref(false)
const payOrderId = ref('')
const payOrderNo = ref('')
const payAmount = ref(0)
const payCartIds = ref([])

const SELECTED_ADDRESS_KEY = 'selectedAddress'
const SELECTED_COUPON_KEY = 'selectedCoupon'
const selectedAddressText = ref('')
const selectedCouponText = ref('')

const tabs = ['全部', '非遗文创', '地道美食', '茶饮伴手', '手工艺品']
const categoryMap = { 1: '非遗文创', 2: '地道美食', 3: '茶饮伴手', 4: '手工艺品' }

const goods = ref([])
const currentPage = ref(1)
const pageSize = 20
const hasMore = ref(true)
const loadingMore = ref(false)

const getUserId = () => {
  const u = uni.getStorageSync('userInfo')
  return u && u.userId ? u.userId : null
}

const mapProduct = (p) => ({
  id: p.id,
  name: p.name,
  desc: p.description || '',
  price: p.price,
  qd: p.qdPrice,
  sales: p.sales || 0,
  tag: (p.tags || '').split(',')[0] || null,
  category: p.category,
  imageUrl: p.imageUrl ? resolveAssetUrl(p.imageUrl) : '',
  color: getColorByCategory(p.category),
  icon: getIconByCategory(p.category)
})

const loadProducts = async (reset = true) => {
  if (reset) {
    currentPage.value = 1
    hasMore.value = true
  }
  if (!hasMore.value || loadingMore.value) return
  loadingMore.value = true
  try {
    const list = await fetchProducts({ page: currentPage.value, pageSize })
    if (Array.isArray(list) && list.length > 0) {
      const mapped = list.map(mapProduct)
      if (reset) {
        goods.value = mapped
      } else {
        goods.value = [...goods.value, ...mapped]
      }
      if (list.length < pageSize) hasMore.value = false
    } else {
      if (reset) goods.value = []
      hasMore.value = false
    }
  } catch (e) {
    if (reset) goods.value = []
  } finally {
    loadingMore.value = false
  }
}

const loadMore = () => {
  if (!hasMore.value || loadingMore.value) return
  currentPage.value++
  loadProducts(false)
}

const getColorByCategory = (cat) => resolveShopCategoryVisual(cat).bg
const getIconByCategory = (cat) => resolveShopCategoryVisual(cat).icon

const filtered = () => {
  if (curTab.value === 0) return goods.value
  const cat = categoryMap[curTab.value]
  return goods.value.filter(g => g.category === cat)
}
const colGoods = (c) => filtered().filter((_, i) => i % 2 === c)

const loadCartCount = async () => {
  const userId = getUserId()
  if (!userId) return
  try {
    const cart = await fetchCart(userId)
    cartNum.value = Array.isArray(cart) ? cart.reduce((sum, c) => sum + (c.quantity || 1), 0) : 0
  } catch (e) { cartNum.value = 0 }
}

const syncSelections = () => {
  const selectedAddress = uni.getStorageSync(SELECTED_ADDRESS_KEY)
  const selectedCoupon = uni.getStorageSync(SELECTED_COUPON_KEY)
  selectedAddressText.value = selectedAddress && selectedAddress.name
    ? `${selectedAddress.name} ${selectedAddress.province || ''}${selectedAddress.city || ''}${selectedAddress.district || ''}${selectedAddress.detail || ''}`
    : ''
  selectedCouponText.value = selectedCoupon && selectedCoupon.name
    ? `${selectedCoupon.name} · ${selectedCoupon.discount}${selectedCoupon.unit}`
    : ''
}

onMounted(() => {
  const info = uni.getWindowInfo()
  stBar.value = info.statusBarHeight || 20
  setTimeout(() => { showBubble.value = false }, 4000)
  syncSelections()
  loadProducts()
  loadCartCount()
  loadQdBalance()
})

onShow(() => {
  syncSelections()
  loadCartCount()
  loadQdBalance()
})

const loadQdBalance = async () => {
  const userId = getUserId()
  if (!userId) return
  try {
    const res = await getCheckInStatus(userId)
    if (res && res.totalPoints != null) qdBalance.value = res.totalPoints
  } catch (e) { /* keep default */ }
}

const goCart = async () => {
  const userId = getUserId()
  if (!userId) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
  try {
    const cart = await fetchCart(userId)
    if (!Array.isArray(cart) || cart.length === 0) {
      uni.showToast({ title: '购物车是空的', icon: 'none' }); return
    }
    // 生成订单并调起支付
    const { createOrder } = await import('@/api/shop')
    const items = cart
      .filter(c => c.product?.id)
      .map(c => ({
        cartId: c.cartId,
        productId: c.product.id,
        productName: c.product.name || '商品',
        productImage: c.product.imageUrl,
        spec: c.spec,
        price: c.product.price || 0,
        quantity: c.quantity || 1
      }))
    if (items.length === 0) { uni.showToast({ title: '购物车商品信息异常', icon: 'none' }); return }
    const order = await createOrder(userId, items)
    if (!order || !order.id) { uni.showToast({ title: '下单失败', icon: 'none' }); return }
    payOrderId.value = order.id
    payOrderNo.value = order.orderNo
    payAmount.value = order.totalAmount
    payCartIds.value = items.map(i => i.cartId).filter(Boolean)
    showPay.value = true
  } catch (e) { uni.showToast({ title: '加载购物车失败', icon: 'none' }) }
}

const onSearch = () => uni.navigateTo({ url: '/pages/search/index' })
const goQd = () => uni.navigateTo({ url: '/pages/red-study/index' })
const onShip = () => uni.navigateTo({ url: '/pages/profile/address?select=1' })
const goCoupon = () => uni.navigateTo({ url: '/pages/profile/coupon?select=1' })
const onGoods = (g) => {
  uni.navigateTo({ url: `/pages/shop/detail?id=${g.id}` })
}
const addCartHandler = async (g) => {
  const userId = getUserId()
  if (!userId) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
  try {
    await addToCart(userId, g.id, 1)
    cartNum.value++
    uni.showToast({ title: '已加入购物车', icon: 'success' })
  } catch (e) { uni.showToast({ title: '加入失败', icon: 'none' }) }
}
const onAi = () => uni.navigateTo({ url: '/pages/digital-human/index' })

onReachBottom(() => { loadMore() })

const closePay = () => {
  showPay.value = false
  payOrderId.value = ''
  payOrderNo.value = ''
  payAmount.value = 0
  payCartIds.value = []
}

const onPaySuccess = async (payload = {}) => {
  const cartIds = [...payCartIds.value]
  showPay.value = false
  payOrderId.value = ''
  payOrderNo.value = ''
  payAmount.value = 0
  payCartIds.value = []
  for (const cid of cartIds) {
    try { await removeFromCart(cid) } catch (e) { /* 静默 */ }
  }
  if (payload.method === 'points') await loadQdBalance()
  uni.showToast({ title: '支付成功', icon: 'success' })
  await loadCartCount()
  if (payload.action === 'view-order') return
  if (payload.orderId) {
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/order/list?tab=2' })
    }, 350)
  }
}
</script>

<style lang="scss">
  @import "@/uni.scss";

  .shop-page { min-height: 100vh; background: #F2F5F8; }

  .sh-hd { background: linear-gradient(180deg, #2A9D8F, #1F7A6E); padding-bottom: 14px; border-radius: 0 0 22px 22px; }
  .sh-top { display: flex; align-items: center; justify-content: space-between; padding: 8px 16px; height: 44px; }
  .sh-title { font-size: 17px; font-weight: 800; color: #fff; }

  .ship { margin: 12px 16px 0; }
  .ship-inner { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px; background: linear-gradient(135deg, #fffaf0, #fff3d4); border-radius: 14px; border: 1px solid rgba(253,203,110,0.2); box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
  .ship-txt { flex: 1; }
  .ship-t1 { display: block; font-size: 15px; font-weight: 700; color: #1A1A2E; margin-bottom: 2px; }
  .ship-t2 { font-size: 12px; color: #888; }
  .ship-icon { width: 36px; height: 36px; border-radius: 50%; background: linear-gradient(135deg, #FDCB6E, #F39C12); display: flex; align-items: center; justify-content: center; }
  .ship-icon-img { width: 20px; height: 20px; display: block; }
  .ship-extra { margin: 10px 16px 0; background: #fff; border-radius: 14px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
  .ship-extra-item { display: flex; align-items: center; justify-content: space-between; padding: 12px 14px; }
  .ship-extra-item + .ship-extra-item { border-top: 1px solid #f3f3f3; }
  .ship-extra-label { font-size: 13px; color: #666; flex-shrink: 0; margin-right: 12px; }
  .ship-extra-value { font-size: 13px; color: #1A1A2E; text-align: right; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .ship-extra-empty { color: #999; }

  .tabs { margin-top: 12px; padding: 0 16px; white-space: nowrap; }
  .tab { display: inline-block; padding: 8px 16px; margin-right: 8px; border-radius: 20px; background: #fff; box-shadow: 0 2px 6px rgba(0,0,0,0.04); }
  .tab-on { background: #2A9D8F; box-shadow: 0 3px 10px rgba(42,157,143,0.25); }
  .tab-t { font-size: 13px; color: #666; font-weight: 500; }
  .tab-on .tab-t { color: #fff; font-weight: 600; }

  .wf { display: flex; padding: 12px 10px 0; gap: 8px; }
  .wf-col { flex: 1; display: flex; flex-direction: column; gap: 8px; }
  .gd { background: #fff; border-radius: 14px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
  .gd-img-w { position: relative; width: 100%; }
  .gd-img-real { width: 100%; height: 160px; }
  .gd-img-placeholder { width: 100%; height: 160px; display: flex; align-items: center; justify-content: center; }
  .gd-img-icon { width: 42px; height: 42px; display: block; opacity: 0.92; }
  .gd-label { position: absolute; top: 8px; left: 8px; padding: 2px 8px; border-radius: 20px; background: #FF9F43; }
  .gd-label-t { font-size: 10px; color: #fff; font-weight: 700; }
  .gd-info { padding: 10px; }
  .gd-name { display: block; font-size: 13px; font-weight: 600; color: #1A1A2E; margin-bottom: 2px; line-height: 1.35; display: -webkit-box; line-clamp: 2; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
  .gd-desc { display: block; font-size: 11px; color: #999; margin-bottom: 8px; }
  .gd-ft { display: flex; align-items: center; justify-content: space-between; }
  .gd-price { display: flex; align-items: baseline; }
  .gd-yen { font-size: 12px; color: #FF9F43; font-weight: 700; }
  .gd-num { font-size: 17px; color: #FF9F43; font-weight: 800; }
  .gd-qd { font-size: 10px; color: #F39C12; margin-left: 3px; }
  .gd-add { width: 24px; height: 24px; border-radius: 50%; background: #2A9D8F; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 6px rgba(42,157,143,0.3); }
  .gd-add-icon { width: 14px; height: 14px; display: block; }
  .gd-sales { display: block; margin-top: 4px; font-size: 11px; color: #bbb; }

.load-more { padding: 16px; text-align: center; }
.load-more-inner { padding: 10px; }
.load-more-t { font-size: 13px; color: #2A9D8F; }
.load-more-end { color: #ccc; }
</style>
