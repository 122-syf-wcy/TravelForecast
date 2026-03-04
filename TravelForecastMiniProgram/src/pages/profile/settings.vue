<template>
  <view class="page">
    <view class="section">
      <view class="item" @tap="editProfile">
        <text class="item-l">个人资料</text>
        <text class="item-r">></text>
      </view>
      <view class="item">
        <text class="item-l">消息通知</text>
        <switch class="item-sw" :checked="notifyOn" @change="notifyOn = $event.detail.value" color="#2A9D8F" />
      </view>
    </view>

    <view class="section">
      <view class="item" @tap="clearCache">
        <text class="item-l">清除缓存</text>
        <text class="item-val">{{ cacheSize }}</text>
      </view>
      <view class="item">
        <text class="item-l">当前版本</text>
        <text class="item-val">v1.0.0</text>
      </view>
      <view class="item" @tap="goAbout">
        <text class="item-l">关于我们</text>
        <text class="item-r">></text>
      </view>
      <view class="item" @tap="goPrivacy">
        <text class="item-l">隐私政策</text>
        <text class="item-r">></text>
      </view>
    </view>

    <view class="section" v-if="isLogin">
      <view class="logout-btn" @tap="logout">
        <text class="logout-t">退出登录</text>
      </view>
    </view>

    <!-- 关于我们弹窗 -->
    <view class="mask" v-if="showAbout" @tap="showAbout = false">
      <view class="popup" @tap.stop>
        <text class="popup-title">关于凉都智游</text>
        <text class="popup-text">凉都智游是六盘水市智慧旅游小程序，为游客提供景区导览、AI伴游、文创商城、红色研学等一站式旅游服务。</text>
        <text class="popup-text">由六盘水市文化广电旅游局指导开发。</text>
        <text class="popup-text popup-ver">版本 v1.0.0</text>
        <view class="popup-btn" @tap="showAbout = false">
          <text class="popup-btn-t">我知道了</text>
        </view>
      </view>
    </view>

    <!-- 隐私政策弹窗 -->
    <view class="mask" v-if="showPrivacy" @tap="showPrivacy = false">
      <view class="popup popup-lg" @tap.stop>
        <text class="popup-title">隐私政策</text>
        <scroll-view scroll-y class="privacy-scroll">
          <text class="popup-text">本应用尊重并保护所有用户的个人隐私权。我们将按照本隐私政策的规定收集、使用您的个人信息。</text>
          <text class="popup-text">1. 我们收集的信息包括：微信昵称、头像、位置信息（用于导航）。</text>
          <text class="popup-text">2. 您的个人信息仅用于提供旅游服务，不会向第三方分享。</text>
          <text class="popup-text">3. 我们采用行业标准安全措施保护您的数据。</text>
          <text class="popup-text">4. 您可以随时在设置中管理您的个人信息。</text>
          <text class="popup-text">如有疑问请联系客服：0858-8888888</text>
        </scroll-view>
        <view class="popup-btn" @tap="showPrivacy = false">
          <text class="popup-btn-t">我知道了</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const isLogin = ref(false)
const notifyOn = ref(true)
const cacheSize = ref('0 KB')
const showAbout = ref(false)
const showPrivacy = ref(false)

onMounted(() => {
  const token = uni.getStorageSync('token')
  isLogin.value = !!token
  calcCache()
})

const calcCache = () => {
  try {
    const info = uni.getStorageInfoSync()
    const kb = info.currentSize || 0
    cacheSize.value = kb > 1024 ? (kb / 1024).toFixed(1) + ' MB' : kb + ' KB'
  } catch (e) {
    cacheSize.value = '0 KB'
  }
}

const clearCache = () => {
  uni.showModal({
    title: '提示',
    content: '确认清除缓存？搜索历史等本地数据将被清除',
    success: (res) => {
      if (res.confirm) {
        const token = uni.getStorageSync('token')
        const userInfo = uni.getStorageSync('userInfo')
        uni.clearStorageSync()
        if (token) uni.setStorageSync('token', token)
        if (userInfo) uni.setStorageSync('userInfo', userInfo)
        calcCache()
        uni.showToast({ title: '缓存已清除', icon: 'success' })
      }
    }
  })
}

const editProfile = () => {
  uni.navigateBack()
  setTimeout(() => {
    uni.showToast({ title: '请点击头像编辑资料', icon: 'none' })
  }, 300)
}

const goAbout = () => { showAbout.value = true }
const goPrivacy = () => { showPrivacy.value = true }

const logout = () => {
  uni.showModal({
    title: '提示',
    content: '确认退出登录？',
    success: (res) => {
      if (res.confirm) {
        uni.removeStorageSync('token')
        uni.removeStorageSync('userInfo')
        uni.showToast({ title: '已退出', icon: 'none' })
        setTimeout(() => {
          uni.switchTab({ url: '/pages/index/index' })
        }, 500)
      }
    }
  })
}
</script>

<style lang="scss">
.page { min-height: 100vh; background: #f2f5f8; padding-top: 12px; }

.section {
  margin: 0 16px 12px;
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0,0,0,0.04);
}

.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  position: relative;
}
.item:not(:last-child)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 16px;
  right: 16px;
  height: 1px;
  background: #f0f0f0;
}
.item-l { font-size: 15px; color: #333; }
.item-r { font-size: 14px; color: #ccc; }
.item-val { font-size: 13px; color: #999; }
.item-sw { transform: scale(0.8); }

.logout-btn {
  padding: 16px;
  text-align: center;
}
.logout-t { font-size: 15px; color: #e74c3c; font-weight: 600; }

.mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
}
.popup {
  width: 80%;
  max-width: 320px;
  background: #fff;
  border-radius: 18px;
  padding: 24px 20px;
}
.popup-lg { max-height: 70vh; }
.popup-title {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
  text-align: center;
  margin-bottom: 16px;
}
.popup-text {
  display: block;
  font-size: 13px;
  color: #666;
  line-height: 1.8;
  margin-bottom: 8px;
}
.popup-ver { color: #999; margin-top: 8px; text-align: center; }
.privacy-scroll { max-height: 40vh; }
.popup-btn {
  margin-top: 16px;
  padding: 12px;
  background: #2A9D8F;
  border-radius: 22px;
  text-align: center;
}
.popup-btn-t { font-size: 14px; color: #fff; font-weight: 600; }
</style>
