<template>
  <view class="page">
    <view class="tabs">
      <view class="tab" v-for="(t, i) in tabs" :key="i"
        :class="{ active: curTab === i }" @tap="curTab = i">
        <text class="tab-txt">{{ t }}</text>
        <view class="tab-line" v-if="curTab === i" />
      </view>
    </view>

    <view class="empty" v-if="filtered.length === 0 && loaded">
      <text class="empty-t">暂无优惠券</text>
      <text class="empty-sub">完成签到和研学任务可获得优惠券</text>
    </view>

    <view class="coupon-list">
      <view class="coupon" v-for="(c, i) in filtered" :key="i"
        :class="{ 'coupon-used': c.status !== 'valid' }">
        <view class="coupon-left" :style="{ background: c.status === 'valid' ? '#2A9D8F' : '#ccc' }">
          <text class="coupon-val">{{ c.discount }}</text>
          <text class="coupon-unit">{{ c.unit }}</text>
          <text class="coupon-cond">{{ c.condition }}</text>
        </view>
        <view class="coupon-right">
          <text class="coupon-name">{{ c.name }}</text>
          <text class="coupon-expire">{{ c.expireText }}</text>
          <view class="coupon-btn" v-if="c.status === 'valid'" @tap="useCoupon(c)">
            <text class="coupon-btn-t">去使用</text>
          </view>
          <text class="coupon-status" v-else>{{ c.status === 'used' ? '已使用' : '已过期' }}</text>
        </view>
        <view class="coupon-circle coupon-circle-l" />
        <view class="coupon-circle coupon-circle-r" />
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getCoupons } from '@/api/user'

const tabs = ['可使用', '已使用', '已过期']
const curTab = ref(0)
const list = ref([])
const loaded = ref(false)
const selectMode = ref(false)
const SELECTED_COUPON_KEY = 'selectedCoupon'

const getUserId = () => {
  const u = uni.getStorageSync('userInfo')
  return u && u.userId ? u.userId : null
}

onLoad((options) => {
  selectMode.value = !!(options && options.select === '1')
})

const loadData = async () => {
  const userId = getUserId()
  if (!userId) { loaded.value = true; return }
  try {
    const res = await getCoupons(userId)
    if (Array.isArray(res)) {
      list.value = res.map(c => {
        const isPercent = c.type === 'percent' || c.discountType === 'percent'
        return {
          id: c.id,
          name: c.name || c.title || '优惠券',
          discount: isPercent ? (c.discount || c.value || '9') : (c.discount || c.value || '5'),
          unit: isPercent ? '折' : '元',
          condition: c.condition || (c.minAmount ? `满${c.minAmount}可用` : '无门槛'),
          expireText: c.expireTime ? `有效期至 ${c.expireTime.split('T')[0]}` : '长期有效',
          status: c.status || 'valid'
        }
      })
    }
  } catch (e) {
    list.value = []
  }
  loaded.value = true
}

const filtered = computed(() => {
  const status = ['valid', 'used', 'expired'][curTab.value]
  return list.value.filter(c => c.status === status)
})

const useCoupon = (coupon) => {
  uni.setStorageSync(SELECTED_COUPON_KEY, coupon)
  uni.showToast({ title: '已选择优惠券', icon: 'success' })
  if (selectMode.value) {
    setTimeout(() => {
      uni.navigateBack()
    }, 250)
    return
  }
  setTimeout(() => {
    uni.switchTab({ url: '/pages/shop/index' })
  }, 250)
}

onMounted(() => { loadData() })
</script>

<style lang="scss">
.page { min-height: 100vh; background: #F2F5F8; }

.tabs { display: flex; background: #fff; padding: 0 16px; position: sticky; top: 0; z-index: 10; }
.tab { flex: 1; height: 44px; display: flex; align-items: center; justify-content: center; position: relative; }
.tab-txt { font-size: 13px; color: #666; }
.active .tab-txt { color: #2A9D8F; font-weight: 600; }
.tab-line { position: absolute; bottom: 0; width: 20px; height: 2px; background: #2A9D8F; border-radius: 2px; }

.empty { text-align: center; margin-top: 80px; }
.empty-t { display: block; font-size: 15px; color: #999; margin-bottom: 6px; }
.empty-sub { display: block; font-size: 12px; color: #ccc; }

.coupon-list { padding: 12px 16px; }

.coupon {
  display: flex; margin-bottom: 12px;
  background: #fff; border-radius: 12px;
  overflow: hidden; position: relative;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}
.coupon-used { opacity: 0.6; }

.coupon-left {
  width: 100px; padding: 16px 10px;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  flex-shrink: 0;
}
.coupon-val { font-size: 28px; font-weight: 800; color: #fff; line-height: 1; }
.coupon-unit { font-size: 12px; color: rgba(255,255,255,0.8); margin-top: 2px; }
.coupon-cond { font-size: 10px; color: rgba(255,255,255,0.7); margin-top: 4px; }

.coupon-right {
  flex: 1; padding: 14px;
  display: flex; flex-direction: column;
  justify-content: center;
}
.coupon-name { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 4px; }
.coupon-expire { font-size: 11px; color: #999; margin-bottom: 8px; }
.coupon-btn { align-self: flex-start; padding: 4px 14px; background: #2A9D8F; border-radius: 14px; }
.coupon-btn-t { font-size: 12px; color: #fff; font-weight: 600; }
.coupon-status { font-size: 12px; color: #ccc; }

.coupon-circle {
  position: absolute; width: 16px; height: 16px;
  background: #F2F5F8; border-radius: 50%;
}
.coupon-circle-l { left: 92px; top: -8px; }
.coupon-circle-r { left: 92px; bottom: -8px; }
</style>
