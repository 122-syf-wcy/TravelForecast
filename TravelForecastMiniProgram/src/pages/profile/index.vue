<template>
  <view class="pf-page">
    <!-- 头部 -->
    <view class="pf-hd" :style="{ paddingTop: stBar + 'px' }">
      <view class="pf-top">
        <view style="width:32px;" />
        <text class="pf-top-title">个人中心</text>
        <view class="pf-set" @tap="goSet">
          <view class="set-gear">
            <view class="gear-circle" />
            <view class="gear-tooth" v-for="i in 4" :key="i" :style="{ transform: 'rotate(' + (i*45) + 'deg)' }" />
          </view>
        </view>
      </view>

      <!-- 用户 -->
      <view class="user-card">
        <view class="uav" @tap="goLogin">
          <view class="uav-circle">
            <image v-if="userAvatar" class="uav-img" :src="userAvatar" mode="aspectFill" />
            <text v-else class="uav-char">游</text>
          </view>
        </view>
        <view class="u-info">
          <text class="u-name">{{ isLogin ? userName : '点击登录' }}</text>
          <text class="u-desc">{{ isLogin ? '已打卡 ' + checkedSpots + ' 个景点' : '登录同步旅行数据' }}</text>
        </view>
        <view class="ck-btn" @tap="doCheckIn">
          <text class="ck-t">签到 +5</text>
        </view>
      </view>

      <!-- 数据行 -->
      <view class="d-row">
        <view class="d-item" v-for="(d, i) in dataRow" :key="i" @tap="onDataTap(d)">
          <text class="d-num">{{ d.val }}</text>
          <text class="d-lb">{{ d.label }}</text>
        </view>
      </view>
    </view>

    <!-- 研学护照 -->
    <view class="pp-sec">
      <view class="pp-hd">
        <text class="pp-title">研学护照</text>
        <view class="pp-more" @tap="goPassport">
          <text class="pp-more-t">查看全部 ></text>
        </view>
      </view>
      <view class="badge-grid">
        <view class="badge" v-for="(b, i) in badges" :key="i" :class="{ 'badge-off': !b.on }">
          <view class="badge-icon" :style="{ background: b.on ? b.bg : '#e0e0e0' }">
            <text class="badge-char">{{ b.char }}</text>
          </view>
          <text class="badge-name">{{ b.name }}</text>
          <text class="badge-st">{{ b.on ? '已解锁' : '未解锁' }}</text>
        </view>
      </view>
    </view>

    <!-- 订单 -->
    <view class="order-sec">
      <view class="order-hd">
        <text class="order-title">我的订单</text>
        <view class="order-all" @tap="goOrders">
          <text class="order-all-t">全部订单 ></text>
        </view>
      </view>
      <view class="order-grid">
        <view class="order-item" v-for="(o, i) in orderTypes" :key="i" @tap="goOrder(o)">
          <view class="oi-icon" :style="{ background: o.bg }">
            <text class="oi-char">{{ o.char }}</text>
            <view class="oi-dot" v-if="o.count > 0" />
          </view>
          <text class="oi-name">{{ o.name }}</text>
        </view>
      </view>
    </view>

    <!-- 菜单 -->
    <view class="menu-sec">
      <view class="menu-item" v-for="(m, i) in menuList" :key="i" @tap="onMenu(m)">
        <view class="menu-l">
          <view class="menu-ico" :style="{ background: m.bg }">
            <text class="menu-ico-c">{{ m.char }}</text>
          </view>
          <text class="menu-txt">{{ m.name }}</text>
        </view>
        <view class="menu-r">
          <text class="menu-tip" v-if="m.tip">{{ m.tip }}</text>
          <text class="menu-arrow">></text>
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

    <!-- 完善资料弹窗 -->
    <view class="profile-mask" v-if="showProfilePopup" @tap="closeProfilePopup">
      <view class="profile-popup" @tap.stop>
        <view class="popup-header">
          <text class="popup-title">完善个人资料</text>
          <text class="popup-close" @tap="closeProfilePopup">×</text>
        </view>
        <view class="popup-body">
          <!-- 头像选择 - 使用微信新版 chooseAvatar -->
          <view class="avatar-section">
            <text class="section-label">头像</text>
            <button class="avatar-choose-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
              <view class="avatar-preview">
                <image v-if="tempAvatar" class="avatar-preview-img" :src="tempAvatar" mode="aspectFill" />
                <view v-else class="avatar-placeholder">
                  <text class="avatar-placeholder-text">+</text>
                </view>
              </view>
              <text class="avatar-tip">点击选择头像</text>
            </button>
          </view>
          <!-- 昵称输入 - 使用微信新版 nickname 类型 -->
          <view class="nickname-section">
            <text class="section-label">昵称</text>
            <input
              class="nickname-input"
              type="nickname"
              v-model="tempNickname"
              placeholder="请输入昵称"
              @blur="onNicknameBlur"
            />
          </view>
        </view>
        <view class="popup-footer">
          <button class="save-btn" :loading="saving" @tap="saveProfile">保存</button>
        </view>
      </view>
    </view>

    <view style="height:110px;" />
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { wechatLogin, uploadAvatar } from '@/api/auth'
import { checkIn, getCheckInStatus, getFavorites } from '@/api/user'
import { fetchPassport } from '@/api/study'
import { fetchOrders } from '@/api/shop'

const stBar = ref(20)
const isLogin = ref(false)
const userName = ref('旅行者')
const userAvatar = ref('')
const showBubble = ref(true)
const bubbleMsg = ref('签到领黔豆~')
const checkedSpots = ref(0)

// ---- 完善资料弹窗 ----
const showProfilePopup = ref(false)
const tempAvatar = ref('')       // chooseAvatar 选择的临时文件路径
const tempNickname = ref('')     // nickname 输入框的值
const saving = ref(false)

const dataRow = ref([
  { label: '黔豆', val: 0, key: 'qd' },
  { label: '优惠券', val: 0, key: 'coupon' },
  { label: '收藏', val: 0, key: 'fav' },
  { label: '足迹', val: 0, key: 'foot' }
])

const badges = ref([
  { name: '初来乍到', char: '初', bg: 'linear-gradient(135deg, #74b9ff, #0984e3)', on: false },
  { name: '三线学者', char: '线', bg: 'linear-gradient(135deg, #ff6b6b, #ee5a24)', on: false },
  { name: '美食猎人', char: '味', bg: 'linear-gradient(135deg, #ffd32a, #f6b93b)', on: false },
  { name: '凉都达人', char: '凉', bg: 'linear-gradient(135deg, #55efc4, #00b894)', on: false },
  { name: '文创大师', char: '创', bg: 'linear-gradient(135deg, #a29bfe, #6c5ce7)', on: false },
  { name: '全景解锁', char: '全', bg: 'linear-gradient(135deg, #FFD93D, #F39C12)', on: false }
])

const orderTypes = ref([
  { name: '待付款', char: '付', bg: 'linear-gradient(135deg, #74b9ff, #0984e3)', count: 0 },
  { name: '待使用', char: '票', bg: 'linear-gradient(135deg, #55efc4, #00b894)', count: 0 },
  { name: '待评价', char: '评', bg: 'linear-gradient(135deg, #ffd32a, #f6b93b)', count: 0 },
  { name: '退款', char: '退', bg: 'linear-gradient(135deg, #dfe6e9, #b2bec3)', count: 0 }
])

const menuList = ref([
  { name: '我的行程', char: '程', bg: 'linear-gradient(135deg, #74b9ff, #0984e3)', tip: '' },
  { name: 'AI 对话历史', char: 'AI', bg: 'linear-gradient(135deg, #2A9D8F, #1A6B5A)', tip: '3条新' },
  { name: '地址管理', char: '址', bg: 'linear-gradient(135deg, #fab1a0, #e17055)', tip: '' },
  { name: '意见反馈', char: '馈', bg: 'linear-gradient(135deg, #a29bfe, #6c5ce7)', tip: '' },
  { name: '联系客服', char: '服', bg: 'linear-gradient(135deg, #55efc4, #00b894)', tip: '在线' }
])

// ---- 判断是否需要完善资料 ----
const needSetupProfile = (userInfo) => {
  if (!userInfo) return true
  const nick = userInfo.nickname || ''
  const avatar = userInfo.avatarUrl || userInfo.avatar || ''
  // 默认昵称或空昵称 → 需要完善
  if (!nick || nick === '微信用户') return true
  // 没有头像 → 需要完善
  if (!avatar) return true
  return false
}

onMounted(() => {
  const info = uni.getWindowInfo()
  stBar.value = info.statusBarHeight || 20
  setTimeout(() => { showBubble.value = false }, 4000)

  const token = uni.getStorageSync('token')
  const u = uni.getStorageSync('userInfo')
  if (token && u) {
    isLogin.value = true
    userName.value = u.nickname || '微信用户'
    userAvatar.value = u.avatarUrl || u.avatar || ''
    loadUserData(u.userId)
  }
})

const loadUserData = async (userId) => {
  if (!userId) return
  try {
    // 并行加载：签到状态、收藏、研学护照、订单
    const [checkInRes, favRes, passportRes, ordersRes] = await Promise.all([
      getCheckInStatus(userId).catch(() => null),
      getFavorites(userId).catch(() => []),
      fetchPassport(userId).catch(() => null),
      fetchOrders(userId, null).catch(() => [])
    ])

    // 更新黔豆
    if (checkInRes && checkInRes.totalPoints != null) {
      dataRow.value[0].val = checkInRes.totalPoints
    } else if (passportRes && passportRes.totalPoints != null) {
      dataRow.value[0].val = passportRes.totalPoints
    }

    // 更新收藏数
    if (Array.isArray(favRes)) {
      dataRow.value[2].val = favRes.length
    }

    // 更新打卡景点数
    if (passportRes && passportRes.checkedSpots != null) {
      checkedSpots.value = passportRes.checkedSpots
    }

    // 更新徽章
    if (passportRes && Array.isArray(passportRes.badges)) {
      badges.value = passportRes.badges.map(b => ({
        name: b.name || '',
        char: b.char || b.name?.substring(0, 1) || '',
        bg: b.bg || 'linear-gradient(135deg, #74b9ff, #0984e3)',
        on: !!b.unlocked
      }))
    }

    // 更新订单数
    if (Array.isArray(ordersRes)) {
      orderTypes.value[0].count = ordersRes.filter(o => o.order?.status === 'pending').length
      orderTypes.value[1].count = ordersRes.filter(o => o.order?.status === 'paid').length
      orderTypes.value[2].count = ordersRes.filter(o => o.order?.status === 'completed').length
      orderTypes.value[3].count = ordersRes.filter(o => o.order?.status === 'cancelled').length
    }
  } catch (e) {
    // 静默失败，保留默认值
  }
}

const goSet = () => uni.navigateTo({ url: '/pages/profile/settings' })

// ---- 登录（简化：只做 wx.login → 后端换 token，不再调废弃的 getUserProfile） ----
const goLogin = () => {
  // 未登录 → 走登录流程
  if (!isLogin.value) {
    uni.showLoading({ title: '登录中...' })
    uni.login({
      provider: 'weixin',
      success: async (loginRes) => {
        if (!loginRes.code) {
          uni.hideLoading()
          uni.showToast({ title: '获取登录凭证失败', icon: 'none' })
          return
        }
        try {
          const data = await wechatLogin({ code: loginRes.code })
          uni.setStorageSync('token', data.token)
          uni.setStorageSync('userInfo', data)
          isLogin.value = true
          userName.value = data.nickname || '微信用户'
          userAvatar.value = data.avatarUrl || data.avatar || ''
          uni.hideLoading()
          uni.showToast({ title: '登录成功', icon: 'success' })
          loadUserData(data.userId)

          // 登录成功后，如果资料不完善则自动弹出完善资料弹窗
          if (needSetupProfile(data)) {
            setTimeout(() => {
              tempAvatar.value = ''
              tempNickname.value = ''
              showProfilePopup.value = true
            }, 500)
          }
        } catch (err) {
          uni.hideLoading()
          uni.showToast({ title: err.message || '登录失败', icon: 'none' })
        }
      },
      fail: () => {
        uni.hideLoading()
        uni.showToast({ title: '微信登录失败', icon: 'none' })
      }
    })
    return
  }

  // 已登录 → 点击头像可以重新编辑资料
  tempAvatar.value = userAvatar.value || ''
  tempNickname.value = userName.value === '微信用户' ? '' : userName.value
  showProfilePopup.value = true
}

// ---- 微信新版 chooseAvatar 回调 ----
const onChooseAvatar = (e) => {
  if (e.detail && e.detail.avatarUrl) {
    tempAvatar.value = e.detail.avatarUrl
  }
}

// ---- nickname 输入框 blur 回调（微信会自动填充昵称） ----
const onNicknameBlur = (e) => {
  if (e.detail && e.detail.value) {
    tempNickname.value = e.detail.value
  }
}

// ---- 关闭弹窗 ----
const closeProfilePopup = () => {
  showProfilePopup.value = false
}

// ---- 保存资料 ----
const saveProfile = async () => {
  const u = uni.getStorageSync('userInfo')
  if (!u || !u.userId) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }

  const hasNewAvatar = tempAvatar.value && !tempAvatar.value.startsWith('http://localhost')
  const hasNewNickname = tempNickname.value && tempNickname.value.trim()

  if (!hasNewAvatar && !hasNewNickname) {
    uni.showToast({ title: '请选择头像或输入昵称', icon: 'none' })
    return
  }

  saving.value = true
  try {
    // 上传头像文件 + 昵称 到后端（后端会上传到 OSS 并更新数据库）
    const filePath = hasNewAvatar ? tempAvatar.value : ''
    const nickname = hasNewNickname ? tempNickname.value.trim() : ''

    let resp = null
    if (filePath) {
      // 有新头像 → 通过 uploadFile 上传
      resp = await uploadAvatar(filePath, u.userId, nickname)
    } else if (nickname) {
      // 只改昵称 → 通过 updateUserInfo
      const { updateUserInfo } = await import('@/api/auth')
      resp = await updateUserInfo({
        userId: u.userId,
        openid: u.openid,
        nickname: nickname
      })
    }

    if (resp) {
      // 更新本地存储和页面显示
      const newUser = {
        ...u,
        nickname: resp.nickname || nickname || u.nickname,
        avatarUrl: resp.avatarUrl || u.avatarUrl
      }
      uni.setStorageSync('userInfo', newUser)
      userName.value = newUser.nickname || '微信用户'
      userAvatar.value = newUser.avatarUrl || ''
    }

    showProfilePopup.value = false
    uni.showToast({ title: '资料更新成功', icon: 'success' })
  } catch (err) {
    console.error('保存资料失败:', err)
    uni.showToast({ title: err.message || '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

const doCheckIn = async () => {
  const u = uni.getStorageSync('userInfo')
  if (!u || !u.userId) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  try {
    const res = await checkIn(u.userId)
    dataRow.value[0].val = res.totalPoints || dataRow.value[0].val
    uni.showToast({ title: res.message || '签到成功', icon: res.points > 0 ? 'success' : 'none' })
  } catch (err) {
    uni.showToast({ title: err.message || '签到失败', icon: 'none' })
  }
}
const onDataTap = (d) => {
  if (d.key === 'fav') {
    uni.navigateTo({ url: '/pages/profile/favorites' })
  } else if (d.key === 'qd') {
    uni.navigateTo({ url: '/pages/red-study/index' })
  } else if (d.key === 'foot') {
    uni.navigateTo({ url: '/pages/search/index' })
  } else if (d.key === 'coupon') {
    uni.switchTab({ url: '/pages/shop/index' })
  }
}
const goPassport = () => uni.navigateTo({ url: '/pages/red-study/index' })
const goOrders = () => uni.navigateTo({ url: '/pages/order/list' })
const goOrder = (o) => {
  let t = 0
  if (o.name === '待付款') t = 1
  if (o.name === '待使用') t = 2
  if (o.name === '待评价') t = 3
  if (o.name === '退款') t = 4
  uni.navigateTo({ url: '/pages/order/list?tab=' + t })
}
const onMenu = (m) => {
  if (m.name === '我的行程') {
    uni.navigateTo({ url: '/pages/itinerary/list' })
  } else if (m.name === 'AI 对话历史') {
    uni.navigateTo({ url: '/pages/digital-human/index' })
  } else if (m.name === '地址管理') {
    uni.navigateTo({ url: '/pages/profile/address' })
  } else if (m.name === '意见反馈') {
    uni.navigateTo({ url: '/pages/profile/feedback' })
  } else if (m.name === '联系客服') {
    uni.makePhoneCall({ phoneNumber: '0858-8888888' }).catch(() => {})
  } else {
    uni.showToast({ title: m.name + '开发中', icon: 'none' })
  }
}
const onAi = () => uni.navigateTo({ url: '/pages/digital-human/index' })
</script>

<style lang="scss">
@import "@/uni.scss";

.pf-page { min-height: 100vh; background: #F2F5F8; }

.pf-hd {
  background: linear-gradient(135deg, #2A9D8F 0%, #1A6B5A 100%);
  padding-bottom: 22px;
  border-radius: 0 0 22px 22px;
  position: relative;
  overflow: hidden;
}
.pf-hd::before { content: ''; position: absolute; top: -35px; right: -25px; width: 120px; height: 120px; border-radius: 50%; background: rgba(255,255,255,0.06); }
.pf-hd::after { content: ''; position: absolute; bottom: -18px; left: -18px; width: 90px; height: 90px; border-radius: 50%; background: rgba(255,255,255,0.04); }

.pf-top { display: flex; align-items: center; justify-content: space-between; padding: 8px 16px; height: 44px; }
.pf-top-title { font-size: 16px; font-weight: 700; color: #fff; }
.pf-set { padding: 4px; }
/* CSS 齿轮图标 */
.set-gear { width: 20px; height: 20px; position: relative; }
.gear-circle { width: 10px; height: 10px; border: 2px solid #fff; border-radius: 50%; position: absolute; top: 5px; left: 5px; }
.gear-tooth { position: absolute; top: 3px; left: 9px; width: 2px; height: 4px; background: #fff; border-radius: 1px; transform-origin: center 7px; }

.user-card { display: flex; align-items: center; padding: 0 18px; position: relative; z-index: 2; }
.uav { margin-right: 14px; }
.uav-circle { width: 56px; height: 56px; border-radius: 50%; background: rgba(255,255,255,0.18); border: 2px solid rgba(255,255,255,0.4); display: flex; align-items: center; justify-content: center; }
.uav-img { width: 100%; height: 100%; border-radius: 50%; }
.uav-char { font-size: 24px; font-weight: 900; color: rgba(255,255,255,0.85); }
.u-info { flex: 1; display: flex; flex-direction: column; }
.u-name { font-size: 19px; font-weight: 800; color: #fff; margin-bottom: 2px; }
.u-desc { font-size: 12px; color: rgba(255,255,255,0.7); }
.ck-btn { padding: 7px 14px; background: rgba(255,255,255,0.18); border: 1px solid rgba(255,255,255,0.25); border-radius: 20px; }
.ck-t { font-size: 12px; color: #fff; font-weight: 600; }

.d-row { display: flex; justify-content: space-around; margin-top: 18px; padding: 0 16px; position: relative; z-index: 2; }
.d-item { display: flex; flex-direction: column; align-items: center; }
.d-num { font-size: 19px; font-weight: 800; color: #fff; margin-bottom: 2px; }
.d-lb { font-size: 12px; color: rgba(255,255,255,0.7); }

/* 研学护照 */
.pp-sec { margin: -10px 16px 0; background: #fff; border-radius: 18px; padding: 16px; box-shadow: 0 4px 16px rgba(0,0,0,0.07); position: relative; z-index: 10; }
.pp-hd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.pp-title { font-size: 16px; font-weight: 700; color: #1A1A2E; }
.pp-more {}
.pp-more-t { font-size: 12px; color: #999; }

.badge-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.badge { display: flex; flex-direction: column; align-items: center; }
.badge-off { opacity: 0.45; }
.badge-off .badge-icon { filter: grayscale(100%); }
.badge-icon {
  width: 48px; height: 48px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 5px; box-shadow: 0 3px 8px rgba(0,0,0,0.1);
  position: relative; overflow: hidden;
}
.badge-icon::after { content: ''; position: absolute; top: -22%; left: -12%; width: 46%; height: 46%; background: rgba(255,255,255,0.3); border-radius: 50%; }
.badge-char { font-size: 20px; font-weight: 800; color: #fff; position: relative; z-index: 2; }
.badge-name { font-size: 11px; font-weight: 600; color: #333; margin-bottom: 1px; }
.badge-st { font-size: 10px; color: #bbb; }

/* 订单 */
.order-sec { margin: 12px 16px 0; background: #fff; border-radius: 18px; padding: 16px; box-shadow: 0 2px 10px rgba(0,0,0,0.04); }
.order-hd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.order-title { font-size: 15px; font-weight: 700; color: #1A1A2E; }
.order-all {}
.order-all-t { font-size: 12px; color: #999; }
.order-grid { display: flex; justify-content: space-around; }
.order-item { display: flex; flex-direction: column; align-items: center; }
.oi-icon { width: 42px; height: 42px; border-radius: 12px; display: flex; align-items: center; justify-content: center; margin-bottom: 5px; position: relative; overflow: hidden; box-shadow: 0 2px 6px rgba(0,0,0,0.08); }
.oi-icon::after { content: ''; position: absolute; top: -22%; left: -12%; width: 46%; height: 46%; background: rgba(255,255,255,0.3); border-radius: 50%; }
.oi-char { font-size: 16px; font-weight: 800; color: #fff; position: relative; z-index: 2; }
.oi-dot { position: absolute; top: 2px; right: 2px; width: 7px; height: 7px; border-radius: 50%; background: #ee0a24; border: 1px solid #fff; z-index: 3; }
.oi-name { font-size: 12px; color: #666; }

/* 菜单 */
.menu-sec { margin: 12px 16px 0; background: #fff; border-radius: 18px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.04); }
.menu-item { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px; position: relative; }
.menu-item:not(:last-child)::after { content: ''; position: absolute; bottom: 0; left: 50px; right: 16px; height: 1px; background: #f0f0f0; }
.menu-l { display: flex; align-items: center; }
.menu-ico { width: 30px; height: 30px; border-radius: 8px; display: flex; align-items: center; justify-content: center; margin-right: 12px; }
.menu-ico-c { font-size: 13px; font-weight: 800; color: #fff; }
.menu-txt { font-size: 14px; color: #333; font-weight: 500; }
.menu-r { display: flex; align-items: center; }
.menu-tip { font-size: 11px; color: #FF9F43; margin-right: 4px; font-weight: 500; }
.menu-arrow { font-size: 14px; color: #ccc; }

/* 完善资料弹窗 */
.profile-mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); z-index: 999;
  display: flex; align-items: center; justify-content: center;
}
.profile-popup {
  width: 85%; max-width: 340px;
  background: #fff; border-radius: 20px;
  overflow: hidden; box-shadow: 0 12px 40px rgba(0,0,0,0.2);
}
.popup-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 20px 0;
}
.popup-title { font-size: 18px; font-weight: 700; color: #1A1A2E; }
.popup-close { font-size: 24px; color: #999; padding: 0 4px; line-height: 1; }
.popup-body { padding: 20px; }

.avatar-section { display: flex; flex-direction: column; align-items: center; margin-bottom: 24px; }
.section-label { font-size: 13px; color: #999; margin-bottom: 10px; }
.avatar-choose-btn {
  display: flex; flex-direction: column; align-items: center;
  background: none; border: none; padding: 0; margin: 0;
  line-height: normal; font-size: inherit;
}
/* 覆盖微信 button 默认样式 */
.avatar-choose-btn::after { display: none; }
.avatar-preview {
  width: 80px; height: 80px; border-radius: 50%;
  overflow: hidden; background: #f0f0f0;
  border: 3px dashed #ccc;
  display: flex; align-items: center; justify-content: center;
}
.avatar-preview-img { width: 100%; height: 100%; border-radius: 50%; }
.avatar-placeholder { display: flex; align-items: center; justify-content: center; width: 100%; height: 100%; }
.avatar-placeholder-text { font-size: 32px; color: #ccc; font-weight: 300; }
.avatar-tip { font-size: 12px; color: #2A9D8F; margin-top: 8px; }

.nickname-section { display: flex; flex-direction: column; }
.nickname-input {
  height: 44px; border: 1px solid #e0e0e0; border-radius: 12px;
  padding: 0 14px; font-size: 15px; color: #333;
  background: #f9f9f9; margin-top: 6px;
}
.nickname-input:focus { border-color: #2A9D8F; background: #fff; }

.popup-footer { padding: 0 20px 20px; }
.save-btn {
  width: 100%; height: 44px; line-height: 44px;
  background: linear-gradient(135deg, #2A9D8F, #1A6B5A);
  color: #fff; font-size: 16px; font-weight: 600;
  border: none; border-radius: 22px;
}
.save-btn::after { display: none; }
</style>
