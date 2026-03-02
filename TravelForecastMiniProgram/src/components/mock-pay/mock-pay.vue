<template>
  <!-- 模拟微信支付弹窗 -->
  <view class="pay-mask" v-if="visible" @tap.stop>
    <!-- 阶段1：支付确认面板 -->
    <view class="pay-panel" v-if="stage === 'confirm'" @tap.stop>
      <view class="pay-panel-hd">
        <view class="pay-close" @tap="onClose"><text class="pay-x">×</text></view>
        <text class="pay-hd-title">收银台</text>
        <view style="width:28px;" />
      </view>
      <!-- 金额 -->
      <view class="pay-amount-sec">
        <text class="pay-amount-label">支付金额</text>
        <view class="pay-amount-row">
          <text class="pay-yen">¥</text>
          <text class="pay-amount">{{ amountStr }}</text>
        </view>
        <text class="pay-merchant">{{ merchantName }}</text>
      </view>
      <!-- 支付方式 -->
      <view class="pay-methods">
        <view class="pay-method" :class="{ 'pm-active': payMethod === 'wechat' }" @tap="payMethod = 'wechat'">
          <view class="pm-icon pm-wx">
            <view class="pm-wx-inner" />
          </view>
          <view class="pm-info">
            <text class="pm-name">微信支付</text>
            <text class="pm-desc">推荐使用</text>
          </view>
          <view class="pm-check" :class="{ 'pm-checked': payMethod === 'wechat' }">
            <text class="pm-check-t" v-if="payMethod === 'wechat'">✓</text>
          </view>
        </view>
        <view class="pay-method" :class="{ 'pm-active': payMethod === 'points' }" @tap="payMethod = 'points'" v-if="showPointsPay">
          <view class="pm-icon pm-qd">
            <text class="pm-qd-t">豆</text>
          </view>
          <view class="pm-info">
            <text class="pm-name">黔豆支付</text>
            <text class="pm-desc">需 {{ pointsNeeded }} 黔豆 (余额 {{ pointsBalance }})</text>
          </view>
          <view class="pm-check" :class="{ 'pm-checked': payMethod === 'points' }">
            <text class="pm-check-t" v-if="payMethod === 'points'">✓</text>
          </view>
        </view>
      </view>
      <!-- 确认按钮 -->
      <view class="pay-confirm-btn" @tap="onConfirm">
        <text class="pay-confirm-t">确认支付</text>
      </view>
      <text class="pay-safe-tip">安全支付由游韵华章提供保障</text>
    </view>

    <!-- 阶段2：密码输入 -->
    <view class="pay-panel pay-pwd-panel" v-if="stage === 'password'" @tap.stop>
      <view class="pay-panel-hd">
        <view class="pay-close" @tap="onBackToConfirm"><text class="pay-x">←</text></view>
        <text class="pay-hd-title">请输入支付密码</text>
        <view style="width:28px;" />
      </view>
      <view class="pwd-amount-row">
        <text class="pwd-yen">¥</text>
        <text class="pwd-amount">{{ amountStr }}</text>
      </view>
      <text class="pwd-merchant">付给 {{ merchantName }}</text>
      <!-- 密码格子 -->
      <view class="pwd-dots">
        <view class="pwd-dot" v-for="i in 6" :key="i">
          <view class="pwd-dot-fill" v-if="password.length >= i" />
        </view>
      </view>
      <!-- 数字键盘 -->
      <view class="num-keyboard">
        <view class="kb-row" v-for="(row, ri) in keyboard" :key="ri">
          <view class="kb-key" v-for="(k, ki) in row" :key="ki"
            :class="{ 'kb-empty': k === '', 'kb-del': k === 'del', 'kb-zero': k === '0' && ri === 3 }"
            @tap="onKey(k)">
            <text class="kb-t" v-if="k !== 'del' && k !== ''">{{ k }}</text>
            <view class="kb-del-icon" v-if="k === 'del'">
              <view class="del-body" />
              <view class="del-x"><text class="del-x-t">×</text></view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 阶段3：支付中 -->
    <view class="pay-loading-overlay" v-if="stage === 'paying'" @tap.stop>
      <view class="pay-loading-box">
        <view class="pay-spinner">
          <view class="spinner-dot" v-for="i in 8" :key="i"
            :style="{ transform: 'rotate(' + (i * 45) + 'deg) translateY(-12px)', animationDelay: (i * 0.1) + 's' }" />
        </view>
        <text class="pay-loading-text">支付中...</text>
        <text class="pay-loading-sub">请勿关闭页面</text>
      </view>
    </view>

    <!-- 阶段4：支付结果 -->
    <view class="pay-result-overlay" v-if="stage === 'result'" @tap.stop>
      <view class="pay-result-box">
        <view class="result-icon" :class="paySuccess ? 'result-ok' : 'result-fail'">
          <text class="result-icon-t">{{ paySuccess ? '✓' : '✗' }}</text>
        </view>
        <text class="result-title">{{ paySuccess ? '支付成功' : '支付失败' }}</text>
        <text class="result-amount" v-if="paySuccess">¥{{ amountStr }}</text>
        <text class="result-msg">{{ resultMsg }}</text>
        <view class="result-order" v-if="paySuccess && orderNo">
          <text class="result-order-label">订单号</text>
          <text class="result-order-no">{{ orderNo }}</text>
        </view>
        <view class="result-btn" @tap="onResultDone">
          <text class="result-btn-t">{{ paySuccess ? '完成' : '重试' }}</text>
        </view>
        <view class="result-btn result-btn-sub" @tap="onViewOrder" v-if="paySuccess">
          <text class="result-btn-t result-btn-sub-t">查看订单</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { createPayment, payWithPoints } from '@/api/user'

const props = defineProps({
  visible: { type: Boolean, default: false },
  orderId: { type: [Number, String], default: '' },
  orderNo: { type: String, default: '' },
  amount: { type: [Number, String], default: 0 },
  merchantName: { type: String, default: '游韵华章·文创商城' },
  showPointsPay: { type: Boolean, default: true },
  pointsBalance: { type: Number, default: 0 }
})

const emit = defineEmits(['close', 'success', 'fail'])

const stage = ref('confirm')
const payMethod = ref('wechat')
const password = ref('')
const paySuccess = ref(false)
const resultMsg = ref('')

const amountStr = computed(() => {
  const n = Number(props.amount)
  return isNaN(n) ? '0.00' : n.toFixed(2)
})

const pointsNeeded = computed(() => {
  const n = Number(props.amount)
  return isNaN(n) ? 0 : Math.ceil(n * 10)
})

const keyboard = [
  ['1', '2', '3'],
  ['4', '5', '6'],
  ['7', '8', '9'],
  ['', '0', 'del']
]

watch(() => props.visible, (val) => {
  if (val) {
    stage.value = 'confirm'
    payMethod.value = 'wechat'
    password.value = ''
    paySuccess.value = false
    resultMsg.value = ''
  }
})

const onClose = () => { emit('close') }

const onBackToConfirm = () => {
  password.value = ''
  stage.value = 'confirm'
}

const onConfirm = () => {
  password.value = ''
  stage.value = 'password'
}

const onKey = (k) => {
  if (k === '') return
  if (k === 'del') {
    password.value = password.value.slice(0, -1)
    return
  }
  if (password.value.length >= 6) return
  password.value += k
  if (password.value.length === 6) {
    doPayment()
  }
}

const getUserId = () => {
  const u = uni.getStorageSync('userInfo')
  return u && u.userId ? u.userId : null
}

const doPayment = async () => {
  stage.value = 'paying'
  const userId = getUserId()

  // 模拟支付延迟（1.5~2.5秒，让体验更真实）
  const delay = 1500 + Math.floor(Math.random() * 1000)

  try {
    if (payMethod.value === 'points') {
      // 黔豆支付
      await new Promise(r => setTimeout(r, delay))
      if (userId && props.orderId) {
        const res = await payWithPoints(props.orderId, userId)
        if (res && res.success) {
          paySuccess.value = true
          resultMsg.value = `已扣除 ${res.usedPoints} 黔豆，剩余 ${res.remainingPoints} 黔豆`
        } else {
          paySuccess.value = false
          resultMsg.value = res?.message || '黔豆不足'
        }
      } else {
        paySuccess.value = false
        resultMsg.value = '支付参数错误'
      }
    } else {
      // 微信支付（模拟）
      if (userId && props.orderId) {
        const res = await createPayment(props.orderId, userId)
        // 后端dev模式会直接标记为paid
        await new Promise(r => setTimeout(r, delay))
        paySuccess.value = true
        resultMsg.value = res?.message || '微信支付成功'
      } else {
        // 无后端时纯前端模拟
        await new Promise(r => setTimeout(r, delay))
        paySuccess.value = true
        resultMsg.value = '模拟支付成功'
      }
    }
  } catch (err) {
    await new Promise(r => setTimeout(r, Math.max(0, delay - 500)))
    paySuccess.value = false
    resultMsg.value = err.message || '支付失败，请重试'
  }

  stage.value = 'result'
}

const onResultDone = () => {
  if (paySuccess.value) {
    emit('success')
  } else {
    // 重试 → 回到确认页
    password.value = ''
    stage.value = 'confirm'
  }
}

const onViewOrder = () => {
  emit('success')
  uni.navigateTo({ url: '/pages/order/list' })
}
</script>

<style lang="scss">
/* ===== 遮罩层 ===== */
.pay-mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  z-index: 9999; background: rgba(0,0,0,0.6);
  display: flex; align-items: flex-end; justify-content: center;
}

/* ===== 支付面板 ===== */
.pay-panel {
  width: 100%; background: #fff;
  border-radius: 16px 16px 0 0;
  animation: paySlideUp 0.3s ease;
  max-height: 85vh; overflow-y: auto;
}
@keyframes paySlideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.pay-panel-hd {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid #f0f0f0;
}
.pay-close { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; }
.pay-x { font-size: 22px; color: #999; font-weight: 300; }
.pay-hd-title { font-size: 16px; font-weight: 600; color: #333; }

/* 金额区域 */
.pay-amount-sec { text-align: center; padding: 28px 20px 20px; }
.pay-amount-label { display: block; font-size: 13px; color: #999; margin-bottom: 8px; }
.pay-amount-row { display: flex; align-items: baseline; justify-content: center; }
.pay-yen { font-size: 20px; color: #333; font-weight: 600; margin-right: 2px; }
.pay-amount { font-size: 40px; color: #333; font-weight: 700; letter-spacing: -1px; }
.pay-merchant { display: block; margin-top: 8px; font-size: 12px; color: #999; }

/* 支付方式 */
.pay-methods { padding: 0 20px 16px; }
.pay-method {
  display: flex; align-items: center; padding: 14px 16px;
  border: 2px solid #f0f0f0; border-radius: 12px; margin-bottom: 10px;
  transition: border-color 0.2s;
}
.pay-method.pm-active { border-color: #07C160; }
.pm-icon {
  width: 36px; height: 36px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  margin-right: 12px; flex-shrink: 0;
}
.pm-wx { background: #07C160; }
.pm-wx-inner {
  width: 18px; height: 14px; position: relative;
}
.pm-wx-inner::before {
  content: ''; position: absolute; top: 0; left: 2px;
  width: 14px; height: 12px; background: #fff; border-radius: 3px;
}
.pm-qd { background: linear-gradient(135deg, #FFD93D, #F39C12); }
.pm-qd-t { font-size: 14px; font-weight: 800; color: #fff; }
.pm-info { flex: 1; }
.pm-name { display: block; font-size: 14px; font-weight: 600; color: #333; }
.pm-desc { display: block; font-size: 11px; color: #999; margin-top: 2px; }
.pm-check {
  width: 22px; height: 22px; border-radius: 50%; border: 2px solid #ddd;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; transition: all 0.2s;
}
.pm-checked { border-color: #07C160; background: #07C160; }
.pm-check-t { font-size: 12px; color: #fff; font-weight: 700; }

/* 确认按钮 */
.pay-confirm-btn {
  margin: 8px 20px; padding: 14px; background: #07C160;
  border-radius: 10px; text-align: center;
  box-shadow: 0 4px 12px rgba(7,193,96,0.3);
}
.pay-confirm-t { font-size: 16px; font-weight: 600; color: #fff; }
.pay-safe-tip {
  display: block; text-align: center; font-size: 11px;
  color: #ccc; padding: 12px 0 24px;
}

/* ===== 密码输入面板 ===== */
.pay-pwd-panel { background: #f5f5f5; }
.pay-pwd-panel .pay-panel-hd { background: #fff; }
.pwd-amount-row {
  display: flex; align-items: baseline; justify-content: center;
  padding-top: 24px; background: #fff;
}
.pwd-yen { font-size: 18px; color: #333; font-weight: 600; margin-right: 2px; }
.pwd-amount { font-size: 36px; color: #333; font-weight: 700; }
.pwd-merchant {
  display: block; text-align: center; font-size: 12px;
  color: #999; padding: 6px 0 20px; background: #fff;
}

/* 密码圆点 */
.pwd-dots {
  display: flex; justify-content: center; gap: 0;
  padding: 0 40px 24px; background: #fff;
}
.pwd-dot {
  width: 42px; height: 42px; border: 1px solid #ddd;
  display: flex; align-items: center; justify-content: center;
  background: #fff;
}
.pwd-dot:first-child { border-radius: 4px 0 0 4px; }
.pwd-dot:last-child { border-radius: 0 4px 4px 0; }
.pwd-dot + .pwd-dot { border-left: none; }
.pwd-dot-fill { width: 12px; height: 12px; border-radius: 50%; background: #333; }

/* 数字键盘 */
.num-keyboard { background: #d1d5db; padding: 1px 0 0; }
.kb-row { display: flex; gap: 1px; margin-bottom: 1px; }
.kb-key {
  flex: 1; height: 54px; background: #fff;
  display: flex; align-items: center; justify-content: center;
  transition: background 0.1s;
}
.kb-key:active { background: #e0e0e0; }
.kb-t { font-size: 22px; color: #333; font-weight: 500; }
.kb-empty { background: #d1d5db; }
.kb-empty:active { background: #d1d5db; }
.kb-del { background: #e8e8e8; }

/* 删除键图标 */
.kb-del-icon { display: flex; align-items: center; }
.del-body {
  width: 20px; height: 16px; background: #666; border-radius: 2px;
  position: relative;
}
.del-body::before {
  content: ''; position: absolute; left: -8px; top: 0;
  width: 0; height: 0;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
  border-right: 8px solid #666;
}
.del-x { position: absolute; right: 4px; top: 50%; transform: translateY(-50%); }
.del-x-t { font-size: 10px; color: #fff; font-weight: 700; }

/* ===== 支付中 ===== */
.pay-loading-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  z-index: 10000; background: rgba(0,0,0,0.7);
  display: flex; align-items: center; justify-content: center;
}
.pay-loading-box { text-align: center; }
.pay-spinner {
  width: 32px; height: 32px; position: relative;
  margin: 0 auto 14px;
}
.spinner-dot {
  position: absolute; top: 50%; left: 50%;
  width: 4px; height: 4px; margin: -2px; border-radius: 50%;
  background: #fff; transform-origin: center center;
  animation: spinFade 0.8s infinite;
}
@keyframes spinFade {
  0% { opacity: 1; }
  100% { opacity: 0.15; }
}
.pay-loading-text { display: block; font-size: 15px; color: #fff; font-weight: 500; }
.pay-loading-sub { display: block; font-size: 12px; color: rgba(255,255,255,0.6); margin-top: 4px; }

/* ===== 支付结果 ===== */
.pay-result-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  z-index: 10000; background: #fff;
  display: flex; align-items: center; justify-content: center;
}
.pay-result-box { text-align: center; padding: 0 40px; width: 100%; box-sizing: border-box; }
.result-icon {
  width: 64px; height: 64px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 16px;
}
.result-ok { background: #07C160; }
.result-fail { background: #FA5151; }
.result-icon-t { font-size: 30px; color: #fff; font-weight: 700; }
.result-title { display: block; font-size: 20px; font-weight: 700; color: #333; margin-bottom: 6px; }
.result-amount { display: block; font-size: 28px; font-weight: 700; color: #333; margin-bottom: 8px; }
.result-msg { display: block; font-size: 13px; color: #999; margin-bottom: 20px; }

.result-order {
  display: flex; justify-content: space-between;
  padding: 12px 16px; background: #f5f5f5; border-radius: 8px;
  margin-bottom: 24px;
}
.result-order-label { font-size: 13px; color: #999; }
.result-order-no { font-size: 13px; color: #333; font-weight: 500; }

.result-btn {
  padding: 14px; background: #07C160; border-radius: 10px;
  text-align: center; margin-bottom: 12px;
  box-shadow: 0 4px 12px rgba(7,193,96,0.25);
}
.result-btn-t { font-size: 16px; font-weight: 600; color: #fff; }
.result-btn-sub { background: #f5f5f5; box-shadow: none; }
.result-btn-sub-t { color: #666 !important; }
</style>
