<template>
  <view class="page">
    <view class="card">
      <text class="card-title">反馈类型</text>
      <view class="type-grid">
      <view class="type-item" v-for="(t, i) in types" :key="i"
          :class="{ 'type-on': curType === i }" @tap="curType = i">
          <view class="type-icon-wrap" :style="{ background: t.bg }">
            <image class="type-icon" :src="t.icon" mode="aspectFit" />
          </view>
          <text class="type-name">{{ t.name }}</text>
        </view>
      </view>
    </view>

    <view class="card">
      <text class="card-title">问题描述</text>
      <textarea
        class="textarea"
        v-model="content"
        placeholder="请详细描述您遇到的问题或建议（至少10个字）"
        :maxlength="500"
        :auto-height="false"
      />
      <text class="char-count">{{ content.length }}/500</text>
    </view>

    <view class="card">
      <text class="card-title">联系方式（选填）</text>
      <input class="input" v-model="contact" placeholder="手机号或微信号，方便我们联系您" />
    </view>

    <view class="pending-bar" v-if="pendingCount > 0">
      <text class="pending-t">有 {{ pendingCount }} 条暂存反馈正在等待提交</text>
      <view class="pending-retry" @tap="retryPendingFeedback">
        <text class="pending-retry-t">立即重试</text>
      </view>
    </view>

    <view class="submit-bar">
      <view class="submit-btn" :class="{ 'submit-disabled': !canSubmit }" @tap="submit">
        <text class="submit-t">提交反馈</text>
      </view>
    </view>

    <!-- 提交成功 -->
    <view class="mask" v-if="showSuccess" @tap="goBack">
      <view class="success-box" @tap.stop>
        <image class="success-icon" :src="commonIcons.checkCircle" mode="aspectFit" />
        <text class="success-title">{{ successTitle }}</text>
        <text class="success-desc">{{ successDesc }}</text>
        <view class="success-btn" @tap="goBack">
          <text class="success-btn-t">返回</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { submitFeedback } from '@/api/feedback'
import { COMMON_ICONS, decorateFeedbackType } from '@/utils/icon-catalog'

const commonIcons = COMMON_ICONS
const types = [
  { name: '功能异常' },
  { name: '体验问题' },
  { name: '内容纠错' },
  { name: '功能建议' }
].map(decorateFeedbackType)

const curType = ref(0)
const content = ref('')
const contact = ref('')
const showSuccess = ref(false)
const pendingCount = ref(0)
const submiting = ref(false)
const successMode = ref('submitted')

const canSubmit = computed(() => content.value.trim().length >= 10)
const successTitle = computed(() => successMode.value === 'saved' ? '已暂存反馈' : '提交成功')
const successDesc = computed(() => (
  successMode.value === 'saved'
    ? '当前网络不可用，反馈已暂存在本机，下次打开页面会自动重试提交'
    : '感谢您的反馈，我们会尽快处理'
))

const retryPendingFeedback = async () => {
  const history = uni.getStorageSync('feedbackHistory') || []
  if (history.length === 0) return
  pendingCount.value = history.length

  const remaining = []
  for (const item of history) {
    try {
      await submitFeedback({
        type: item.type,
        content: item.content,
        contact: item.contact,
        userId: item.userId
      })
    } catch (_) {
      remaining.push(item)
    }
  }
  uni.setStorageSync('feedbackHistory', remaining)
  pendingCount.value = remaining.length
  if (remaining.length < history.length && remaining.length === 0) {
    uni.showToast({ title: '暂存反馈已全部提交', icon: 'success' })
  }
}

onMounted(() => {
  const history = uni.getStorageSync('feedbackHistory') || []
  pendingCount.value = history.length
  if (history.length > 0) retryPendingFeedback()
})

const submit = async () => {
  if (submiting.value) return
  if (!canSubmit.value) {
    uni.showToast({ title: '请输入至少10个字的描述', icon: 'none' })
    return
  }

  const u = uni.getStorageSync('userInfo')
  const feedback = {
    type: types[curType.value].name,
    content: content.value.trim(),
    contact: contact.value.trim(),
    userId: u ? u.userId : null
  }

  submiting.value = true
  try {
    await submitFeedback(feedback)
    successMode.value = 'submitted'
    showSuccess.value = true
  } catch (e) {
    const history = uni.getStorageSync('feedbackHistory') || []
    history.unshift({ ...feedback, time: new Date().toISOString(), pending: true })
    const nextHistory = history.slice(0, 50)
    uni.setStorageSync('feedbackHistory', nextHistory)
    pendingCount.value = nextHistory.length
    successMode.value = 'saved'
    uni.showToast({ title: '已暂存，将在下次打开时自动提交', icon: 'none' })
    showSuccess.value = true
  } finally {
    submiting.value = false
  }
}

const goBack = () => {
  uni.navigateBack()
}
</script>

<style lang="scss">
.page { min-height: 100vh; background: #f2f5f8; padding: 12px 16px 100px; }

.card {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.04);
}
.card-title { display: block; font-size: 15px; font-weight: 700; color: #1a1a2e; margin-bottom: 12px; }

.type-grid { display: flex; gap: 10px; flex-wrap: wrap; }
.type-item {
  flex: 1;
  min-width: 70px;
  padding: 12px 8px;
  border-radius: 10px;
  background: #f5f6fa;
  text-align: center;
  border: 2px solid transparent;
}
.type-on { border-color: #2A9D8F; background: #e8f8f5; }
.type-icon-wrap {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 6px;
}
.type-icon { width: 18px; height: 18px; display: block; }
.type-name { font-size: 12px; color: #666; }
.type-on .type-name { color: #2A9D8F; font-weight: 600; }

.textarea {
  width: 100%;
  height: 140px;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 12px;
  font-size: 14px;
  color: #333;
  background: #fafafa;
  line-height: 1.6;
}
.char-count { display: block; text-align: right; font-size: 12px; color: #ccc; margin-top: 4px; }

.input {
  height: 44px;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 0 12px;
  font-size: 14px;
  color: #333;
  background: #fafafa;
}

.pending-bar {
  margin: 0 0 12px;
  padding: 10px 14px;
  background: #FFF8E1;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.pending-t { font-size: 12px; color: #F39C12; flex: 1; }
.pending-retry { padding: 4px 12px; background: #F39C12; border-radius: 14px; }
.pending-retry-t { font-size: 12px; color: #fff; font-weight: 600; }

.submit-bar {
  position: fixed;
  bottom: 0; left: 0; right: 0;
  padding: 12px 16px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.06);
}
.submit-btn { padding: 14px; background: #2A9D8F; border-radius: 22px; text-align: center; }
.submit-disabled { opacity: 0.5; }
.submit-t { font-size: 15px; color: #fff; font-weight: 600; }

.mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
}
.success-box {
  width: 72%;
  background: #fff;
  border-radius: 20px;
  padding: 32px 20px;
  text-align: center;
}
.success-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  display: block;
}
.success-title { display: block; font-size: 18px; font-weight: 700; color: #1a1a2e; margin-bottom: 8px; }
.success-desc { display: block; font-size: 13px; color: #999; margin-bottom: 20px; }
.success-btn { padding: 12px; background: #2A9D8F; border-radius: 22px; }
.success-btn-t { font-size: 14px; color: #fff; font-weight: 600; }
</style>
