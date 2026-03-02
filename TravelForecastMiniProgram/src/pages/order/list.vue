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
        <text class="empty-icon">📦</text>
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
            <text class="order-img-t">{{ (item.productName || '').substring(0, 2) }}</text>
          </view>
          <view class="order-info">
            <text class="order-name">{{ item.productName }}</text>
            <text class="order-spec">x{{ item.quantity || 1 }}</text>
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
          <view class="btn btn-main" v-if="o.order.status === 'paid'">去使用</view>
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
      @close="showPay = false"
      @success="onPaySuccess"
      @fail="showPay = false"
    />
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchOrders, updateOrderStatus, deleteOrder } from '@/api/shop'

const curTab = ref(0)
const tabs = ['全部', '待付款', '待使用', '待评价', '退款/售后']
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

const cancelOrder = async (order) => {
  try {
    await updateOrderStatus(order.id, 'cancelled')
    uni.showToast({ title: '已取消', icon: 'none' })
    loadOrders()
  } catch (e) { uni.showToast({ title: '操作失败', icon: 'none' }) }
}

const payOrder = (order) => {
  payOrderId.value = order.id
  payOrderNo.value = order.orderNo
  payAmount.value = order.totalAmount
  showPay.value = true
}

const onPaySuccess = () => {
  showPay.value = false
  uni.showToast({ title: '支付成功', icon: 'success' })
  loadOrders()
}

const delOrder = async (order) => {
  try {
    await deleteOrder(order.id)
    uni.showToast({ title: '已删除', icon: 'none' })
    loadOrders()
  } catch (e) { uni.showToast({ title: '删除失败', icon: 'none' }) }
}

const goShop = () => uni.switchTab({ url: '/pages/shop/index' })

onLoad((options) => { if (options && options.tab) curTab.value = parseInt(options.tab) || 0 })
onMounted(() => { loadOrders() })
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
.empty-icon { display: block; font-size: 40px; margin-bottom: 10px; }
.empty-t { display: block; font-size: 14px; color: #999; margin-bottom: 16px; }
.empty-btn { display: inline-block; padding: 8px 24px; background: #2A9D8F; border-radius: 20px; }
.empty-btn-t { font-size: 13px; color: #fff; }
.order { background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.order-hd { display: flex; justify-content: space-between; margin-bottom: 12px; }
.order-shop { font-size: 12px; color: #999; }
.order-st { font-size: 13px; color: #2A9D8F; font-weight: 500; }
.order-bd { display: flex; margin-bottom: 10px; }
.order-img-box { width: 70px; height: 70px; border-radius: 10px; margin-right: 12px; background: linear-gradient(135deg, #ffeaa7, #fdcb6e); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.order-img-t { font-size: 20px; font-weight: 800; color: rgba(255,255,255,0.6); }
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
