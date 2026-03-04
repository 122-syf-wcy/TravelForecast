<template>
  <view class="page">
    <view class="card">
      <text class="card-title">反馈类型</text>
      <view class="type-grid">
        <view class="type-item" v-for="(t, i) in types" :key="i"
          :class="{ 'type-on': curType === i }" @tap="curType = i">
          <text class="type-char">{{ t.char }}</text>
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

    <view class="submit-bar">
      <view class="submit-btn" :class="{ 'submit-disabled': !canSubmit }" @tap="submit">
        <text class="submit-t">提交反馈</text>
      </view>
    </view>

    <!-- 提交成功 -->
    <view class="mask" v-if="showSuccess" @tap="goBack">
      <view class="success-box" @tap.stop>
        <text class="success-icon">✓</text>
        <text class="success-title">提交成功</text>
        <text class="success-desc">感谢您的反馈，我们会尽快处理</text>
        <view class="success-btn" @tap="goBack">
          <text class="success-btn-t">返回</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { submitFeedback } from '@/api/feedback'

const types = [
  { name: '功能异常', char: '🐛' },
  { name: '体验问题', char: '💡' },
  { name: '内容纠错', char: '📝' },
  { name: '功能建议', char: '🚀' }
]

const curType = ref(0)
const content = ref('')
const contact = ref('')
const showSuccess = ref(false)

const canSubmit = computed(() => content.value.trim().length >= 10)

const submit = async () => {
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

  try {
    await submitFeedback(feedback)
  } catch (e) {
    // 后端不可用时保存到本地
    const history = uni.getStorageSync('feedbackHistory') || []
    history.unshift({ ...feedback, time: new Date().toISOString() })
    uni.setStorageSync('feedbackHistory', history.slice(0, 50))
  }

  showSuccess.value = true
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
.type-char { display: block; font-size: 22px; margin-bottom: 4px; }
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
  display: block;
  width: 56px;
  height: 56px;
  line-height: 56px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: #2A9D8F;
  font-size: 28px;
  color: #fff;
  font-weight: 700;
}
.success-title { display: block; font-size: 18px; font-weight: 700; color: #1a1a2e; margin-bottom: 8px; }
.success-desc { display: block; font-size: 13px; color: #999; margin-bottom: 20px; }
.success-btn { padding: 12px; background: #2A9D8F; border-radius: 22px; }
.success-btn-t { font-size: 14px; color: #fff; font-weight: 600; }
</style>
