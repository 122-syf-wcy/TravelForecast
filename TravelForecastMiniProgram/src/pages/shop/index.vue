<template>
  <view class="shop-page">
    <!-- 头部 -->
    <view class="sh-hd" :style="{ paddingTop: stBar + 'px' }">
      <view class="sh-top">
        <text class="sh-title">文创商城</text>
        <view class="sh-cart" @tap="goCart">
          <view class="cart-ico">
            <view class="cart-bag" />
            <view class="cart-handle" />
          </view>
          <view class="cart-dot" v-if="cartNum > 0">
            <text class="cart-dot-t">{{ cartNum }}</text>
          </view>
        </view>
      </view>
      <view class="sh-search" @tap="onSearch">
        <view class="sh-s-ico">
          <view class="sh-s-c" />
          <view class="sh-s-h" />
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
          <text class="ship-arrow">></text>
        </view>
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
            <image v-if="g.imageUrl" class="gd-img-real" :src="g.imageUrl" mode="aspectFill" />
            <view v-else class="gd-img-placeholder" :style="{ background: g.color }">
              <text class="gd-img-char">{{ g.name.substring(0,2) }}</text>
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
                <text class="gd-plus">+</text>
              </view>
            </view>
            <text class="gd-sales">已售 {{ g.sales }}</text>
          </view>
        </view>
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
      @close="showPay = false"
      @success="onPaySuccess"
      @fail="showPay = false"
    />

    <view style="height:110px;" />
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchProducts, addToCart, fetchCart } from '@/api/shop'
import { getCheckInStatus } from '@/api/user'
import { resolveAssetUrl } from '@/utils/url'

const stBar = ref(20)
const cartNum = ref(0)
const qdBalance = ref(0)
const curTab = ref(0)
const showBubble = ref(true)
const bubbleMsg = ref('刺梨汁维C很高哦')
const showPay = ref(false)
const payOrderId = ref('')
const payOrderNo = ref('')
const payAmount = ref(0)

const tabs = ['全部', '非遗文创', '地道美食', '茶饮伴手', '手工艺品']
const categoryMap = { 1: '非遗文创', 2: '地道美食', 3: '茶饮伴手', 4: '手工艺品' }

const goods = ref([])

const getUserId = () => {
  const u = uni.getStorageSync('userInfo')
  return u && u.userId ? u.userId : null
}

const loadProducts = async () => {
  try {
    const list = await fetchProducts()
    if (Array.isArray(list) && list.length > 0) {
      goods.value = list.map(p => ({
        id: p.id,
        name: p.name,
        desc: p.description || '',
        price: p.price,
        qd: p.qdPrice,
        sales: p.sales || 0,
        tag: (p.tags || '').split(',')[0] || null,
        category: p.category,
        imageUrl: p.imageUrl ? resolveAssetUrl(p.imageUrl) : '',
        color: getColorByCategory(p.category)
      }))
    }
  } catch (e) { /* 保留空列表 */ }
}

const getColorByCategory = (cat) => {
  const map = {
    '非遗文创': 'linear-gradient(135deg, #a29bfe, #6c5ce7)',
    '地道美食': 'linear-gradient(135deg, #fab1a0, #e17055)',
    '茶饮伴手': 'linear-gradient(135deg, #55efc4, #00b894)',
    '手工艺品': 'linear-gradient(135deg, #fd79a8, #e84393)'
  }
  return map[cat] || 'linear-gradient(135deg, #ffeaa7, #fdcb6e)'
}

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

onMounted(() => {
  const info = uni.getWindowInfo()
  stBar.value = info.statusBarHeight || 20
  setTimeout(() => { showBubble.value = false }, 4000)
  loadProducts()
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
    const items = cart.map(c => ({
      productId: c.product?.id,
      productName: c.product?.name || '商品',
      productImage: c.product?.imageUrl,
      price: c.product?.price || 0,
      quantity: c.quantity || 1
    }))
    const order = await createOrder(userId, items)
    payOrderId.value = order.id
    payOrderNo.value = order.orderNo
    payAmount.value = order.totalAmount
    showPay.value = true
  } catch (e) { uni.showToast({ title: '加载购物车失败', icon: 'none' }) }
}
const onSearch = () => uni.navigateTo({ url: '/pages/search/index' })
const goQd = () => uni.navigateTo({ url: '/pages/red-study/index' })
const onShip = () => uni.navigateTo({ url: '/pages/profile/address' })
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

const onPaySuccess = () => {
  showPay.value = false
  cartNum.value = 0
  uni.showToast({ title: '支付成功', icon: 'success' })
}
</script>

<style lang="scss">
@import "@/uni.scss";

.shop-page { min-height: 100vh; background: #F2F5F8; }

.sh-hd { background: linear-gradient(180deg, #2A9D8F, #1F7A6E); padding-bottom: 14px; border-radius: 0 0 22px 22px; }
.sh-top { display: flex; align-items: center; justify-content: space-between; padding: 8px 16px; height: 44px; }
.sh-title { font-size: 17px; font-weight: 800; color: #fff; }
.sh-cart { position: relative; }
/* CSS 购物车图标 */
.cart-ico { width: 22px; height: 20px; position: relative; }
.cart-bag { width: 18px; height: 14px; border: 2px solid #fff; border-radius: 0 0 4px 4px; position: absolute; bottom: 0; left: 2px; }
.cart-handle { width: 10px; height: 6px; border: 2px solid #fff; border-bottom: none; border-radius: 6px 6px 0 0; position: absolute; top: 0; left: 6px; }
.cart-dot { position: absolute; top: -4px; right: -6px; min-width: 14px; height: 14px; background: #FF9F43; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.cart-dot-t { font-size: 9px; color: #fff; font-weight: 700; }

.sh-search { margin: 0 16px; height: 34px; background: rgba(255,255,255,0.18); border: 1px solid rgba(255,255,255,0.2); border-radius: 20px; display: flex; align-items: center; padding: 0 12px; }
.sh-s-ico { width: 14px; height: 14px; position: relative; margin-right: 6px; }
.sh-s-c { width: 10px; height: 10px; border: 2px solid rgba(255,255,255,0.75); border-radius: 50%; position: absolute; top: 0; left: 0; }
.sh-s-h { width: 5px; height: 2px; background: rgba(255,255,255,0.75); position: absolute; bottom: 0; right: 0; transform: rotate(45deg); border-radius: 1px; }
.sh-s-txt { font-size: 13px; color: rgba(255,255,255,0.7); }

.qd-bar { margin: 10px 16px 0; padding: 10px 14px; background: rgba(255,255,255,0.14); border-radius: 10px; display: flex; align-items: center; justify-content: space-between; }
.qd-l { display: flex; align-items: center; }
.qd-coin { width: 16px; height: 16px; border-radius: 50%; background: linear-gradient(135deg, #FFD93D, #F39C12); margin-right: 6px; border: 1px solid rgba(255,255,255,0.3); }
.qd-lb { font-size: 13px; color: rgba(255,255,255,0.85); margin-right: 8px; }
.qd-num { font-size: 18px; font-weight: 800; color: #FFD93D; }
.qd-btn { padding: 4px 14px; background: rgba(255,255,255,0.9); border-radius: 20px; }
.qd-btn-t { font-size: 12px; color: #2A9D8F; font-weight: 600; }

.ship { margin: 12px 16px 0; }
.ship-inner { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px; background: linear-gradient(135deg, #fffaf0, #fff3d4); border-radius: 14px; border: 1px solid rgba(253,203,110,0.2); box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.ship-txt {}
.ship-t1 { display: block; font-size: 15px; font-weight: 700; color: #1A1A2E; margin-bottom: 2px; }
.ship-t2 { font-size: 12px; color: #888; }
.ship-icon { width: 36px; height: 36px; border-radius: 50%; background: linear-gradient(135deg, #FDCB6E, #F39C12); display: flex; align-items: center; justify-content: center; }
.ship-arrow { font-size: 16px; color: #fff; font-weight: 700; }

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
.gd-img-char { font-size: 28px; font-weight: 800; color: rgba(255,255,255,0.6); }
.gd-label { position: absolute; top: 8px; left: 8px; padding: 2px 8px; border-radius: 20px; background: #FF9F43; }
.gd-label-t { font-size: 10px; color: #fff; font-weight: 700; }
.gd-info { padding: 10px; }
.gd-name { display: block; font-size: 13px; font-weight: 600; color: #1A1A2E; margin-bottom: 2px; line-height: 1.35; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.gd-desc { display: block; font-size: 11px; color: #999; margin-bottom: 8px; }
.gd-ft { display: flex; align-items: center; justify-content: space-between; }
.gd-price { display: flex; align-items: baseline; }
.gd-yen { font-size: 12px; color: #FF9F43; font-weight: 700; }
.gd-num { font-size: 17px; color: #FF9F43; font-weight: 800; }
.gd-qd { font-size: 10px; color: #F39C12; margin-left: 3px; }
.gd-add { width: 24px; height: 24px; border-radius: 50%; background: #2A9D8F; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 6px rgba(42,157,143,0.3); }
.gd-plus { font-size: 16px; color: #fff; font-weight: 700; line-height: 1; }
.gd-sales { display: block; margin-top: 4px; font-size: 11px; color: #bbb; }
</style>
