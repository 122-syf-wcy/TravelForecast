<template>
  <view class="page">
    <view class="list" v-if="list.length > 0">
      <view class="addr" v-for="(a, i) in list" :key="i">
        <view class="addr-body" @tap="selectAddr(a)">
          <view class="addr-top">
            <text class="addr-name">{{ a.name }}</text>
            <text class="addr-phone">{{ a.phone }}</text>
            <view class="addr-default" v-if="a.isDefault">
              <text class="addr-default-t">默认</text>
            </view>
          </view>
          <text class="addr-detail">{{ a.province }}{{ a.city }}{{ a.district }}{{ a.detail }}</text>
        </view>
        <view class="addr-ft">
          <view class="addr-action" @tap="setDefault(i)">
            <view class="radio" :class="{ 'radio-on': a.isDefault }" />
            <text class="addr-action-t">默认地址</text>
          </view>
          <view class="addr-actions">
            <view class="addr-btn" @tap="editAddr(i)">
              <text class="addr-btn-t">编辑</text>
            </view>
            <view class="addr-btn" @tap="delAddr(i)">
              <text class="addr-btn-t" style="color:#e74c3c;">删除</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="empty" v-if="list.length === 0">
      <text class="empty-icon">📍</text>
      <text class="empty-t">暂无收货地址</text>
      <text class="empty-sub">添加地址后可快速下单</text>
    </view>

    <view class="add-bar">
      <view class="add-btn" @tap="showForm = true; resetForm()">
        <text class="add-btn-t">+ 新增收货地址</text>
      </view>
    </view>

    <!-- 编辑/新增弹窗 -->
    <view class="mask" v-if="showForm" @tap="showForm = false">
      <view class="form-popup" @tap.stop>
        <text class="form-title">{{ editIndex >= 0 ? '编辑地址' : '新增地址' }}</text>
        <view class="field">
          <text class="field-label">收货人</text>
          <input class="field-input" v-model="form.name" placeholder="请输入姓名" />
        </view>
        <view class="field">
          <text class="field-label">手机号</text>
          <input class="field-input" v-model="form.phone" placeholder="请输入手机号" type="number" maxlength="11" />
        </view>
        <view class="field">
          <text class="field-label">所在地区</text>
          <picker mode="region" @change="onRegion">
            <view class="field-input field-picker">
              <text :class="form.province ? '' : 'placeholder'">{{ form.province ? form.province + ' ' + form.city + ' ' + form.district : '请选择省市区' }}</text>
            </view>
          </picker>
        </view>
        <view class="field">
          <text class="field-label">详细地址</text>
          <input class="field-input" v-model="form.detail" placeholder="街道/楼栋/门牌号" />
        </view>
        <view class="form-btn" @tap="saveAddr">
          <text class="form-btn-t">保存</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const STORAGE_KEY = 'addressList'
const list = ref([])
const showForm = ref(false)
const editIndex = ref(-1)
const form = ref({ name: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false })

onMounted(() => {
  list.value = uni.getStorageSync(STORAGE_KEY) || []
})

const saveToStorage = () => {
  uni.setStorageSync(STORAGE_KEY, list.value)
}

const resetForm = () => {
  editIndex.value = -1
  form.value = { name: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false }
}

const onRegion = (e) => {
  const [province, city, district] = e.detail.value
  form.value.province = province
  form.value.city = city
  form.value.district = district
}

const saveAddr = () => {
  if (!form.value.name.trim()) { uni.showToast({ title: '请输入收货人', icon: 'none' }); return }
  if (!form.value.phone.trim() || form.value.phone.length < 11) { uni.showToast({ title: '请输入正确手机号', icon: 'none' }); return }
  if (!form.value.province) { uni.showToast({ title: '请选择地区', icon: 'none' }); return }
  if (!form.value.detail.trim()) { uni.showToast({ title: '请输入详细地址', icon: 'none' }); return }

  if (editIndex.value >= 0) {
    list.value[editIndex.value] = { ...form.value }
  } else {
    if (list.value.length === 0) form.value.isDefault = true
    list.value.push({ ...form.value })
  }
  saveToStorage()
  showForm.value = false
  uni.showToast({ title: '保存成功', icon: 'success' })
}

const editAddr = (i) => {
  editIndex.value = i
  form.value = { ...list.value[i] }
  showForm.value = true
}

const delAddr = (i) => {
  uni.showModal({
    title: '提示',
    content: '确认删除该地址？',
    success: (res) => {
      if (res.confirm) {
        list.value.splice(i, 1)
        saveToStorage()
        uni.showToast({ title: '已删除', icon: 'none' })
      }
    }
  })
}

const setDefault = (i) => {
  list.value.forEach((a, idx) => { a.isDefault = idx === i })
  saveToStorage()
}

const selectAddr = (a) => {
  // 可被订单页调用选择地址
  const pages = getCurrentPages()
  if (pages.length > 1) {
    const prev = pages[pages.length - 2]
    if (prev && prev.$vm && prev.$vm.onAddressSelected) {
      prev.$vm.onAddressSelected(a)
      uni.navigateBack()
      return
    }
  }
}
</script>

<style lang="scss">
.page { min-height: 100vh; background: #f2f5f8; padding: 12px 16px 100px; }

.addr {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  margin-bottom: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.04);
}
.addr-top { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.addr-name { font-size: 16px; font-weight: 700; color: #1a1a2e; }
.addr-phone { font-size: 14px; color: #666; }
.addr-default { padding: 1px 6px; background: rgba(42,157,143,0.1); border-radius: 4px; }
.addr-default-t { font-size: 10px; color: #2A9D8F; font-weight: 600; }
.addr-detail { font-size: 13px; color: #888; line-height: 1.5; }

.addr-ft { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; padding-top: 12px; border-top: 1px solid #f0f0f0; }
.addr-action { display: flex; align-items: center; gap: 6px; }
.radio { width: 16px; height: 16px; border-radius: 50%; border: 2px solid #ddd; }
.radio-on { border-color: #2A9D8F; background: #2A9D8F; }
.addr-action-t { font-size: 12px; color: #999; }
.addr-actions { display: flex; gap: 16px; }
.addr-btn-t { font-size: 13px; color: #666; }

.empty { text-align: center; margin-top: 80px; }
.empty-icon { display: block; font-size: 40px; margin-bottom: 10px; }
.empty-t { display: block; font-size: 15px; color: #999; margin-bottom: 6px; }
.empty-sub { display: block; font-size: 12px; color: #ccc; }

.add-bar { position: fixed; bottom: 0; left: 0; right: 0; padding: 12px 16px; padding-bottom: calc(12px + env(safe-area-inset-bottom)); background: #fff; box-shadow: 0 -2px 10px rgba(0,0,0,0.06); }
.add-btn { padding: 14px; background: #2A9D8F; border-radius: 22px; text-align: center; }
.add-btn-t { font-size: 15px; color: #fff; font-weight: 600; }

.mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); z-index: 999; display: flex; align-items: flex-end; }
.form-popup { width: 100%; background: #fff; border-radius: 20px 20px 0 0; padding: 24px 20px; padding-bottom: calc(24px + env(safe-area-inset-bottom)); }
.form-title { display: block; font-size: 18px; font-weight: 700; color: #1a1a2e; text-align: center; margin-bottom: 20px; }

.field { margin-bottom: 16px; }
.field-label { display: block; font-size: 13px; color: #999; margin-bottom: 6px; }
.field-input { height: 44px; border: 1px solid #e8e8e8; border-radius: 10px; padding: 0 12px; font-size: 15px; color: #333; background: #fafafa; }
.field-picker { display: flex; align-items: center; line-height: 44px; }
.placeholder { color: #ccc; }

.form-btn { margin-top: 8px; padding: 14px; background: #2A9D8F; border-radius: 22px; text-align: center; }
.form-btn-t { font-size: 15px; color: #fff; font-weight: 600; }
</style>
