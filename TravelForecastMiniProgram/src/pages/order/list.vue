<template>
  <view class="page">
    <view class="tabs">
      <view class="tab" v-for="(t, i) in tabs" :key="i" :class="{ active: curTab === i }" @tap="switchTab(i)">
        <text class="tab-txt">{{ t }}</text>
        <view class="tab-line" v-if="curTab === i" />
      </view>
    </view>

    <view class="list">
      <view class="empty" v-if="list.length === 0 && loaded">
        <image class="empty-icon" :src="commonIcons.package" mode="aspectFit" />
        <text class="empty-t">暂无订单</text>
        <view class="empty-btn" @tap="goShop"><text class="empty-btn-t">去逛逛</text></view>
      </view>
      <view class="order" v-for="(o, i) in list" :key="i">
        <view class="order-hd">
          <text class="order-shop">订单 {{ o.order.orderNo }}</text>
          <text class="order-st">{{ statusLabel(o.order.status) }}</text>
        </view>
        <view class="order-bd" v-for="(item, j) in o.items" :key="j">
          <view class="order-img-box">
            <image
              v-if="resolveItemImage(item)"
              class="order-img"
              :src="resolveItemImage(item)"
              mode="aspectFill"
            />
            <image v-else class="order-img-icon" :src="commonIcons.package" mode="aspectFit" />
          </view>
          <view class="order-info">
            <text class="order-name">{{ item.productName }}</text>
            <text class="order-spec">{{ item.spec ? item.spec + ' · ' : '' }}x{{ item.quantity || 1 }}</text>
            <view class="order-price">
              <text class="p-sym">¥</text><text class="p-num">{{ item.price }}</text>
            </view>
          </view>
        </view>
        <view class="order-total">
          <text class="ot-t">合计：</text>
          <text class="ot-price">¥{{ o.order.totalAmount }}</text>
        </view>
        <view class="order-ft">
          <view class="btn btn-sub" v-if="o.order.status === 'pending'" @tap="cancelOrder(o.order)">取消订单</view>
          <view class="btn btn-main" v-if="o.order.status === 'pending'" @tap="payOrder(o.order)">去支付</view>
          <view class="btn btn-main" v-if="o.order.status === 'paid'" @tap="useOrder(o)">去使用</view>
          <view class="btn btn-sub" v-if="o.order.status === 'completed'" @tap="delOrder(o.order)">删除</view>
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
      @close="closePay"
      @success="onPaySuccess"
      @fail="closePay"
    />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { fetchOrders, updateOrderStatus, deleteOrder } from '@/api/shop'
import { getCheckInStatus, getPayStatus } from '@/api/user'
import { resolveAssetUrl } from '@/utils/url'
import { COMMON_ICONS } from '@/utils/icon-catalog'

const commonIcons = COMMON_ICONS

const resolveItemImage = (item) => {
  if (!item) return ''
  const raw = item.productImage || item.image || item.coverImage || item.thumb
  return raw ? resolveAssetUrl(raw) : ''
}

const curTab = ref(0)
const tabs = ['全部', '待付款', '待使用', '已完成', '已取消']
const statusMap = { 0: null, 1: 'pending', 2: 'paid', 3: 'completed', 4: 'cancelled' }
const list = ref([])
const loaded = ref(false)
const showPay = ref(false)
const payOrderId = ref('')
const payOrderNo = ref('')
const payAmount = ref(0)
const pointsBalance = ref(0)

const getUserId = () => {
  const u = uni.getStorageSync('userInfo')
  return u && u.userId ? u.userId : null
}

const statusLabel = (s) => {
  const map = { pending: '待付款', paid: '待使用', shipped: '已发货', completed: '已完成', cancelled: '已取消' }
  return map[s] || s
}

const loadOrders = async () => {
  const userId = getUserId()
  if (!userId) { loaded.value = true; return }
  try {
    const status = statusMap[curTab.value]
    list.value = await fetchOrders(userId, status)
  } catch (e) { list.value = [] }
  loaded.value = true
}

const switchTab = (i) => { curTab.value = i; loaded.value = false; loadOrders() }

const cancelOrder = (order) => {
  uni.showModal({
    title: '取消订单',
    content: `确认取消订单 ${order.orderNo || ''} 吗？`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await updateOrderStatus(order.id, 'cancelled')
        uni.showToast({ title: '已取消', icon: 'none' })
        await loadOrders()
      } catch (e) {
        uni.showToast({ title: '操作失败', icon: 'none' })
      }
    }
  })
}

const payOrder = async (order) => {
  try {
    const payStatus = await getPayStatus(order.id)
    if (payStatus && payStatus.status === 'paid') {
      uni.showToast({ title: '该订单已支付', icon: 'none' })
      curTab.value = 2
      loaded.value = false
      loadOrders()
      return
    }
  } catch (e) { /* 继续支付流程 */ }
  if (order.status !== 'pending') {
    uni.showToast({ title: '订单状态已变更，请刷新', icon: 'none' })
    loadOrders()
    return
  }
  payOrderId.value = order.id
  payOrderNo.value = order.orderNo
  payAmount.value = order.totalAmount
  showPay.value = true
}

const resetPayState = () => {
  showPay.value = false
  payOrderId.value = ''
  payOrderNo.value = ''
  payAmount.value = 0
}

const closePay = () => {
  resetPayState()
}

const onPaySuccess = async (payload = {}) => {
  resetPayState()
  if (payload.method === 'points') await loadPointsBalance()
  uni.showToast({ title: '支付成功', icon: 'success' })
  curTab.value = 2
  loaded.value = false
  await loadOrders()
}

const delOrder = (order) => {
  uni.showModal({
    title: '删除订单',
    content: `确认删除订单 ${order.orderNo || ''} 吗？删除后不可恢复。`,
    confirmColor: '#e74c3c',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deleteOrder(order.id)
        uni.showToast({ title: '已删除', icon: 'none' })
        await loadOrders()
      } catch (e) {
        uni.showToast({ title: '删除失败', icon: 'none' })
      }
    }
  })
}

const useOrder = (o) => {
  const order = o.order
  const itemsStr = encodeURIComponent(JSON.stringify(
    (o.items || []).map(it => ({ productName: it.productName, quantity: it.quantity || 1 }))
  ))
  uni.navigateTo({
    url: `/pages/order/use?orderId=${order.id}&orderNo=${order.orderNo}&amount=${order.totalAmount}&items=${itemsStr}`
  })
}

const goShop = () => uni.switchTab({ url: '/pages/shop/index' })

const loadPointsBalance = async () => {
  const userId = getUserId()
  if (!userId) return
  try {
    const res = await getCheckInStatus(userId)
    if (res && res.totalPoints != null) pointsBalance.value = res.totalPoints
  } catch (e) { /* keep default */ }
}

onLoad((options) => { if (options && options.tab) curTab.value = parseInt(options.tab) || 0 })
onShow(() => {
  loaded.value = false
  loadOrders()
  loadPointsBalance()
})
</script>

<style lang="scss">
.page { min-height: 100vh; background: #F2F5F8; }
.tabs { display: flex; background: #fff; padding: 0 16px; position: sticky; top: 0; z-index: 10; }
.tab { flex: 1; height: 44px; display: flex; align-items: center; justify-content: center; position: relative; }
.tab-txt { font-size: 13px; color: #666; }
.active .tab-txt { color: #2A9D8F; font-weight: 600; }
.tab-line { position: absolute; bottom: 0; width: 20px; height: 2px; background: #2A9D8F; border-radius: 2px; }
.list { padding: 16px; }
.empty { text-align: center; margin-top: 60px; }
.empty-icon { display: block; width: 42px; height: 42px; margin: 0 auto 10px; }
.empty-t { display: block; font-size: 14px; color: #999; margin-bottom: 16px; }
.empty-btn { display: inline-block; padding: 8px 24px; background: #2A9D8F; border-radius: 20px; }
.empty-btn-t { font-size: 13px; color: #fff; }
.order { background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.order-hd { display: flex; justify-content: space-between; margin-bottom: 12px; }
.order-shop { font-size: 12px; color: #999; }
.order-st { font-size: 13px; color: #2A9D8F; font-weight: 500; }
.order-bd { display: flex; margin-bottom: 10px; }
.order-img-box { width: 70px; height: 70px; border-radius: 10px; margin-right: 12px; background: linear-gradient(135deg, #ffeaa7, #fdcb6e); display: flex; align-items: center; justify-content: center; flex-shrink: 0; overflow: hidden; }
.order-img { width: 100%; height: 100%; border-radius: 10px; }
.order-img-icon { width: 24px; height: 24px; display: block; }
.order-info { flex: 1; display: flex; flex-direction: column; justify-content: space-between; }
.order-name { font-size: 14px; color: #333; font-weight: 500; }
.order-spec { font-size: 12px; color: #999; }
.order-price { color: #e74c3c; font-weight: 600; }
.p-sym { font-size: 12px; }
.p-num { font-size: 16px; }
.order-total { display: flex; justify-content: flex-end; align-items: center; padding-top: 10px; border-top: 1px solid #f0f0f0; margin-bottom: 10px; }
.ot-t { font-size: 13px; color: #666; }
.ot-price { font-size: 16px; font-weight: 700; color: #e74c3c; margin-left: 4px; }
.order-ft { display: flex; justify-content: flex-end; gap: 10px; }
.btn { padding: 6px 16px; border-radius: 16px; font-size: 12px; }
.btn-sub { border: 1px solid #ddd; color: #666; }
.btn-main { background: #2A9D8F; color: #fff; }
</style>
