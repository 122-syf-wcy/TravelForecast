<template>
  <view class="page">
    <view class="ticket">
      <view class="ticket-top">
        <text class="ticket-title">使用凭证</text>
        <text class="ticket-no">订单号：{{ orderNo }}</text>
      </view>
      <view class="qr-area">
        <canvas canvas-id="qrCanvas" class="qr-canvas" />
        <view class="verify-code-box">
          <text class="verify-code-label">模拟核销码</text>
          <text class="verify-code-value">{{ verifyCode }}</text>
        </view>
        <text class="qr-tip">当前为演示凭证，请向商家出示上方核销码或图形码</text>
      </view>
      <view class="ticket-info">
        <view class="info-row" v-for="(item, i) in items" :key="i">
          <text class="info-name">{{ item.productName }}</text>
          <text class="info-spec">x{{ item.quantity || 1 }}</text>
        </view>
      </view>
      <view class="ticket-ft">
        <text class="ticket-amount">¥{{ amount }}</text>
        <text class="ticket-status">已支付</text>
      </view>
      <view class="ticket-wave" />
    </view>
    <view class="bottom-tip">
      <text class="bt-t">凭证有效期至订单完成前</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const orderNo = ref('')
const orderId = ref('')
const amount = ref('0')
const items = ref([])
const verifyCode = ref('')

onLoad((options) => {
  if (options) {
    orderNo.value = options.orderNo || ''
    orderId.value = options.orderId || ''
    amount.value = options.amount || '0'
    if (options.items) {
      try { items.value = JSON.parse(decodeURIComponent(options.items)) } catch (_) {}
    }
  }
  verifyCode.value = buildVerifyCode()
})

onMounted(() => {
  setTimeout(() => drawQR(), 300)
})

const buildVerifyCode = () => {
  const source = `${orderId.value || ''}${orderNo.value || ''}`.replace(/[^0-9A-Za-z]/g, '') || `${Date.now()}`
  const code = source.slice(-8).toUpperCase().padStart(8, '0')
  return `${code.slice(0, 4)} ${code.slice(4)}`
}

const drawQR = () => {
  const ctx = uni.createCanvasContext('qrCanvas')
  const data = `VERIFY:${verifyCode.value.replace(/\s+/g, '')}|ORDER:${orderId.value}|${orderNo.value}`
  const size = 200
  const moduleCount = 21
  const cellSize = Math.floor(size / moduleCount)

  ctx.setFillStyle('#FFFFFF')
  ctx.fillRect(0, 0, size, size)
  ctx.setFillStyle('#1A1A2E')

  let seed = 0
  for (let i = 0; i < data.length; i++) seed = ((seed << 5) - seed) + data.charCodeAt(i)

  for (let row = 0; row < moduleCount; row++) {
    for (let col = 0; col < moduleCount; col++) {
      const isPosition = (row < 7 && col < 7) || (row < 7 && col >= moduleCount - 7) || (row >= moduleCount - 7 && col < 7)
      if (isPosition) {
        const outerR = row < 7 ? 0 : moduleCount - 7
        const outerC = col < 7 ? 0 : (col >= moduleCount - 7 ? moduleCount - 7 : 0)
        const lr = row - outerR
        const lc = col - outerC
        const isBorder = lr === 0 || lr === 6 || lc === 0 || lc === 6
        const isInner = lr >= 2 && lr <= 4 && lc >= 2 && lc <= 4
        if (isBorder || isInner) {
          ctx.fillRect(col * cellSize, row * cellSize, cellSize, cellSize)
        }
      } else {
        const hash = ((seed * (row * moduleCount + col + 1)) >>> 0) % 100
        if (hash < 45) {
          ctx.fillRect(col * cellSize, row * cellSize, cellSize, cellSize)
        }
      }
    }
  }
  ctx.draw()
}
</script>

<style lang="scss">
.page { min-height: 100vh; background: #F2F5F8; padding: 20px 16px; }

.ticket {
  background: #fff;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  position: relative;
}
.ticket-top {
  background: linear-gradient(135deg, #2A9D8F, #1A6B5A);
  padding: 20px;
}
.ticket-title { display: block; font-size: 20px; font-weight: 800; color: #fff; margin-bottom: 4px; }
.ticket-no { font-size: 12px; color: rgba(255,255,255,0.7); }

.qr-area { padding: 24px; text-align: center; }
.qr-canvas { width: 200px; height: 200px; margin: 0 auto; }
.verify-code-box {
  margin: 14px auto 0;
  width: fit-content;
  padding: 10px 18px;
  background: #F6FAF9;
  border-radius: 12px;
}
.verify-code-label { display: block; font-size: 12px; color: #8AA39F; margin-bottom: 4px; }
.verify-code-value { font-size: 20px; font-weight: 800; color: #1A6B5A; letter-spacing: 2px; }
.qr-tip { display: block; margin-top: 12px; font-size: 13px; color: #999; }

.ticket-info { padding: 0 20px; border-top: 1px dashed #e0e0e0; }
.info-row { display: flex; justify-content: space-between; padding: 12px 0; }
.info-row:not(:last-child) { border-bottom: 1px solid #f5f5f5; }
.info-name { font-size: 14px; color: #333; flex: 1; }
.info-spec { font-size: 13px; color: #999; }

.ticket-ft {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; border-top: 1px dashed #e0e0e0;
}
.ticket-amount { font-size: 20px; font-weight: 800; color: #e74c3c; }
.ticket-status { font-size: 13px; color: #2A9D8F; font-weight: 600; padding: 4px 12px; background: #E0F2F1; border-radius: 20px; }

.ticket-wave {
  height: 12px;
  background: radial-gradient(circle at 12px -4px, transparent 12px, #fff 12px);
  background-size: 24px 12px;
}

.bottom-tip { text-align: center; margin-top: 20px; }
.bt-t { font-size: 12px; color: #ccc; }
</style>
