<template>
  <div class="profile-page">
    <scenic-card class="max-w-4xl mx-auto">
      <template #header>
        <div class="flex items-center justify-between">
          <h3 class="text-2xl font-bold text-[#2A9D8F]">用户信息管理</h3>
          <div class="flex gap-3">
            <el-button @click="handleSave" :loading="saving">
              <el-icon><Check /></el-icon>
              <span>保存修改</span>
            </el-button>
            <el-button variant="secondary" @click="handleReset">
              <el-icon><RefreshLeft /></el-icon>
              <span>重置</span>
            </el-button>
          </div>
        </div>
      </template>

      <div class="profile-content">
        <!-- 头像区域 -->
        <div class="avatar-section mb-8 flex items-center gap-6 p-6 bg-white rounded-lg">
          <div class="relative">
            <el-avatar :size="100" :src="form.avatar" class="border-4 border-[#2A9D8F] shadow-lg">
              <el-icon :size="50"><User /></el-icon>
            </el-avatar>
            <el-upload
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
              :http-request="handleAvatarUpload"
              accept="image/*"
            >
              <div class="absolute bottom-0 right-0 bg-[#2A9D8F] rounded-full p-2 cursor-pointer hover:bg-[#23897d] transition-colors">
                <el-icon :size="16"><Camera /></el-icon>
              </div>
            </el-upload>
          </div>
          <div class="flex-1">
            <h4 class="text-xl text-gray-800 mb-2">{{ form.username }}</h4>
            <p class="text-gray-500 text-sm">注册时间：{{ formatDate(form.createdAt) }}</p>
            <p class="text-gray-500 text-sm">最后登录：{{ formatDate(form.loginAt) }}</p>
            <p class="text-gray-600 text-sm mt-2">支持 JPG、PNG 格式，大小不超过 2MB</p>
          </div>
        </div>

        <!-- 基本信息表单 -->
        <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="profile-form">
          <div class="form-section">
            <h4 class="section-title">
              <el-icon><User /></el-icon>
              <span>基本信息</span>
            </h4>
            
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" disabled placeholder="用户名不可修改">
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入昵称">
                <template #prefix>
                  <el-icon><Stamp /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱">
                <template #prefix>
                  <el-icon><Message /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号">
                <template #prefix>
                  <el-icon><Phone /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </div>

          <div class="form-section mt-6">
            <h4 class="section-title">
              <el-icon><Lock /></el-icon>
              <span>安全设置</span>
            </h4>

            <el-form-item label="当前密码" prop="currentPassword">
              <el-input 
                v-model="form.currentPassword" 
                type="password" 
                placeholder="修改密码时需要输入当前密码"
                show-password
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword">
              <el-input 
                v-model="form.newPassword" 
                type="password" 
                placeholder="留空则不修改密码"
                show-password
              >
                <template #prefix>
                  <el-icon><Key /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input 
                v-model="form.confirmPassword" 
                type="password" 
                placeholder="请再次输入新密码"
                show-password
              >
                <template #prefix>
                  <el-icon><Key /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </div>

          <div class="form-section mt-6">
            <h4 class="section-title">
              <el-icon><InfoFilled /></el-icon>
              <span>账号信息</span>
            </h4>

            <el-form-item label="账号状态">
              <el-tag :type="form.status === 'active' ? 'success' : 'danger'" effect="dark" size="large">
                {{ form.status === 'active' ? '正常' : '已禁用' }}
              </el-tag>
            </el-form-item>

            <el-form-item label="用户角色">
              <el-tag type="info" effect="dark" size="large">
                {{ getRoleText(form.role) }}
              </el-tag>
            </el-form-item>
          </div>
        </el-form>

        <!-- 偏好设置 -->
        <div class="form-section mt-6">
          <h4 class="section-title">
            <el-icon><Setting /></el-icon>
            <span>偏好设置</span>
          </h4>
          
          <div class="preferences-grid">
            <el-form label-width="120px">
              <el-form-item label="主题模式">
                <el-radio-group v-model="preferences.theme" @change="savePreferences">
                  <el-radio value="dark">深色模式</el-radio>
                  <el-radio value="light">浅色模式</el-radio>
                  <el-radio value="auto">跟随系统</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="语言设置">
                <el-select v-model="preferences.language" @change="savePreferences" class="w-64">
                  <el-option label="简体中文" value="zh-CN" />
                  <el-option label="English" value="en-US" />
                  <el-option label="繁體中文" value="zh-TW" />
                </el-select>
              </el-form-item>

              <el-form-item label="时区设置">
                <el-select v-model="preferences.timezone" @change="savePreferences" class="w-64">
                  <el-option label="北京时间 (UTC+8)" value="Asia/Shanghai" />
                  <el-option label="东京时间 (UTC+9)" value="Asia/Tokyo" />
                  <el-option label="纽约时间 (UTC-5)" value="America/New_York" />
                  <el-option label="伦敦时间 (UTC+0)" value="Europe/London" />
                </el-select>
              </el-form-item>

              <el-form-item label="邮件通知">
                <el-switch 
                  v-model="preferences.emailNotifications" 
                  @change="savePreferences"
                  active-text="开启"
                  inactive-text="关闭"
                />
              </el-form-item>

              <el-form-item label="系统通知">
                <el-switch 
                  v-model="preferences.systemNotifications" 
                  @change="savePreferences"
                  active-text="开启"
                  inactive-text="关闭"
                />
              </el-form-item>

              <el-form-item label="营销推送">
                <el-switch 
                  v-model="preferences.marketingEmails" 
                  @change="savePreferences"
                  active-text="开启"
                  inactive-text="关闭"
                />
              </el-form-item>

              <el-form-item label="数据自动保存">
                <el-switch 
                  v-model="preferences.autoSave" 
                  @change="savePreferences"
                  active-text="开启"
                  inactive-text="关闭"
                />
              </el-form-item>

              <el-form-item label="显示个人资料">
                <el-switch 
                  v-model="privacy.showProfile" 
                  @change="savePrivacy"
                  active-text="公开"
                  inactive-text="隐私"
                />
              </el-form-item>

              <el-form-item label="允许搜索">
                <el-switch 
                  v-model="privacy.allowSearch" 
                  @change="savePrivacy"
                  active-text="允许"
                  inactive-text="禁止"
                />
              </el-form-item>
            </el-form>
          </div>
        </div>

        <!-- 会话管理 -->
        <div class="form-section mt-6">
          <h4 class="section-title">
            <el-icon><Monitor /></el-icon>
            <span>会话管理</span>
          </h4>
          
          <div class="sessions-list">
            <div v-if="sessionsLoading" class="text-center py-8">
              <el-icon class="is-loading text-4xl text-[#2A9D8F]"><Loading /></el-icon>
              <p class="text-gray-500 mt-2">加载中...</p>
            </div>
            
            <div v-else-if="sessions.length === 0" class="text-center py-8">
              <el-empty description="暂无会话记录" />
            </div>
            
            <div v-else class="space-y-3">
              <div 
                v-for="session in sessions" 
                :key="session.sessionId"
                class="session-item p-4 bg-white rounded-lg border border-gray-200 hover:border-[#2A9D8F] transition-colors"
              >
                <div class="flex items-start justify-between">
                  <div class="flex-1">
                    <div class="flex items-center gap-2 mb-2">
                      <el-icon :size="20" class="text-[#2A9D8F]"><Monitor /></el-icon>
                      <span class="text-gray-800 font-medium">{{ getDeviceInfo(session.userAgent) }}</span>
                      <el-tag v-if="session.isCurrent" type="success" size="small">当前会话</el-tag>
                    </div>
                    <div class="text-sm text-gray-500 space-y-1">
                      <p><el-icon><Location /></el-icon> IP: {{ session.ipAddress || '未知' }}</p>
                      <p><el-icon><Clock /></el-icon> 登录时间: {{ formatDate(session.loginAt) }}</p>
                      <p><el-icon><Calendar /></el-icon> 过期时间: {{ formatDate(session.expireAt) }}</p>
                    </div>
                  </div>
                  <div>
                    <el-button 
                      v-if="!session.isCurrent"
                      type="danger" 
                      size="small" 
                      plain
                      @click="revokeSession(session.sessionId)"
                      :loading="revokingSessionId === session.sessionId"
                    >
                      撤销
                    </el-button>
                    <el-tag v-else type="info" size="small">无法撤销</el-tag>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </scenic-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElLoading, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { 
  User, Camera, Message, Phone, Lock, Key, 
  Check, RefreshLeft, Stamp, InfoFilled,
  Monitor, Loading, Location, Clock, Calendar, Setting
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import ScenicCard from '@/components/ScenicCard.vue'


const userStore = useUserStore()
const formRef = ref<FormInstance>()
const saving = ref(false)

// 会话管理相关
const sessions = ref<any[]>([])
const sessionsLoading = ref(false)
const revokingSessionId = ref<number | null>(null)

// 偏好设置
const preferences = ref({
  theme: 'light',
  language: 'zh-CN',
  timezone: 'Asia/Shanghai',
  emailNotifications: true,
  systemNotifications: true,
  marketingEmails: false,
  autoSave: true
})

// 隐私设置
const privacy = ref({
  showProfile: true,
  allowSearch: true
})

interface UserForm {
  userId: number | undefined
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  status: string
  role: string
  createdAt: string
  loginAt: string
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

const form = ref<UserForm>({
  userId: undefined,
  username: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
  status: 'active',
  role: 'user',
  createdAt: '',
  loginAt: '',
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validatePassword = (rule: any, value: any, callback: any) => {
  if (form.value.newPassword && !form.value.currentPassword) {
    callback(new Error('请输入当前密码'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (value !== form.value.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  nickname: [
    { max: 20, message: '昵称长度不能超过20个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  currentPassword: [
    { validator: validatePassword, trigger: 'blur' }
  ],
  newPassword: [
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const loadProfile = async () => {
  // 获取userId（优先使用userId，否则使用id）
  const userId = userStore.userInfo?.userId || userStore.userInfo?.id
  
  if (!userId) {
    ElMessage.error('用户信息不完整，请重新登录')
    return
  }
  
  try {
    const res: any = await request({
      url: `/users/${userId}`,
      method: 'get'
    })
    if (res?.data) {
      // 确保使用正确的 userId（后端返回的可能是 user_id 或 userId）
      const actualUserId = res.data.userId || res.data.user_id || userId
      
      form.value = {
        ...form.value,
        userId: actualUserId,  // 显式设置userId
        username: res.data.username || form.value.username,
        // 确保null值显示为空字符串
        nickname: res.data.nickname || '',
        email: res.data.email || '',
        phone: res.data.phone || '',
        avatar: res.data.avatar || '',
        status: res.data.status || 'active',
        role: res.data.role || 'user',
        createdAt: res.data.createdAt || '',
        loginAt: res.data.loginAt || '',
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      }
    }
  } catch (error: any) {
    console.error('加载用户信息失败:', error)
    // 使用store中的数据作为备用
    if (userStore.userInfo) {
      form.value = {
        ...form.value,
        userId: userId,
        username: userStore.userInfo.username,
        nickname: userStore.userInfo.nickname || '',
        email: userStore.userInfo.email || '',
        phone: userStore.userInfo.phone || '',
        avatar: userStore.userInfo.avatar || '',
        status: userStore.userInfo.status || 'active',
        role: userStore.userInfo.role || 'user',
        createdAt: userStore.userInfo.createdAt || '',
        loginAt: userStore.userInfo.loginAt || ''
      }
    }
  }
}

const handleSave = async () => {
  if (!formRef.value) return
  
  // 确保有userId
  const userId = form.value.userId || userStore.userInfo?.userId || userStore.userInfo?.id
  
  if (!userId) {
    ElMessage.error('用户信息不完整，请刷新页面重试')
    return
  }
  
  try {
    const valid = await formRef.value.validate()
    if (!valid) return
  } catch (error) {
    return
  }

  saving.value = true
  try {
    const payload: any = {
      nickname: form.value.nickname,
      email: form.value.email,
      phone: form.value.phone
    }

    // 如果要修改密码
    if (form.value.newPassword) {
      payload.currentPassword = form.value.currentPassword
      payload.newPassword = form.value.newPassword
    }

    await request({
      url: `/users/${userId}`,
      method: 'put',
      data: payload
    })

    ElMessage.success('保存成功')
    
    // 清空密码字段
    form.value.currentPassword = ''
    form.value.newPassword = ''
    form.value.confirmPassword = ''
    
    // 更新store中的用户信息
    if (userStore.userInfo) {
      userStore.userInfo.nickname = form.value.nickname
      userStore.userInfo.email = form.value.email
      userStore.userInfo.phone = form.value.phone
    }
  } catch (error: any) {
    console.error('保存失败:', error)
    let errorMsg = error?.message || '保存失败'
    
    // 友好的错误提示
    if (errorMsg.includes('Duplicate') && errorMsg.includes('email')) {
      errorMsg = '该邮箱已被其他用户使用，请更换'
    } else if (errorMsg.includes('Duplicate') && errorMsg.includes('phone')) {
      errorMsg = '该手机号已被其他用户使用，请更换'
    } else if (errorMsg.includes('旧密码错误') || errorMsg.includes('当前密码')) {
      errorMsg = '当前密码输入错误'
    }
    
    ElMessage.error(errorMsg)
  } finally {
    saving.value = false
  }
}

const handleReset = () => {
  form.value.currentPassword = ''
  form.value.newPassword = ''
  form.value.confirmPassword = ''
  loadProfile()
}

// 头像上传前验证
const beforeAvatarUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

// 上传头像到阿里云OSS
const handleAvatarUpload = async (options: any) => {
  const { file } = options
  
  const loading = ElLoading.service({
    lock: true,
    text: '上传中...',
    background: 'rgba(0, 0, 0, 0.7)'
  })
  
  try {
    // 创建FormData
    const formData = new FormData()
    formData.append('file', file)
    
    // 调用后端上传接口
    const res: any = await request({
      url: '/upload/avatar',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    if (res?.data?.url) {
      form.value.avatar = res.data.url
      ElMessage.success('头像上传成功')
      
      // 自动保存
      const userId = form.value.userId || userStore.userInfo?.userId || userStore.userInfo?.id
      if (userId) {
        await request({
          url: `/users/${userId}`,
          method: 'put',
          data: { avatar: res.data.url }
        })
        
        // 更新store中的头像
        userStore.updateAvatar(res.data.url)
      }
    }
  } catch (error: any) {
    console.error('上传失败:', error)
    ElMessage.error(error?.message || '上传失败')
  } finally {
    loading.close()
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '未知'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const getRoleText = (role: string) => {
  const roleMap: Record<string, string> = {
    user: '普通用户',
    merchant: '商家用户',
    admin: '管理员'
  }
  return roleMap[role] || role
}

// 加载会话列表
const loadSessions = async () => {
  const userId = form.value.userId || userStore.userInfo?.userId || userStore.userInfo?.id
  if (!userId) return

  sessionsLoading.value = true
  try {
    const res: any = await request({
      url: `/users/${userId}/sessions`,
      method: 'get'
    })

    if (res?.data) {
      // 后端已经标记了isCurrent字段
      sessions.value = res.data
    }
  } catch (error) {
    console.error('加载会话失败:', error)
  } finally {
    sessionsLoading.value = false
  }
}

// 撤销会话
const revokeSession = async (sessionId: number) => {
  const userId = form.value.userId || userStore.userInfo?.userId || userStore.userInfo?.id
  if (!userId) return

  try {
    await ElMessageBox.confirm(
      '撤销后该设备将被强制退出登录，是否继续？',
      '确认撤销',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    revokingSessionId.value = sessionId
    
    await request({
      url: `/users/${userId}/sessions/${sessionId}`,
      method: 'delete'
    })

    ElMessage.success('会话已撤销')
    
    // 重新加载会话列表
    await loadSessions()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('撤销会话失败:', error)
      ElMessage.error(error?.message || '撤销会话失败')
    }
  } finally {
    revokingSessionId.value = null
  }
}

// 解析设备信息
const getDeviceInfo = (userAgent: string) => {
  if (!userAgent) return '未知设备'

  // 检测浏览器
  let browser = '未知浏览器'
  if (userAgent.includes('Edg')) browser = 'Edge'
  else if (userAgent.includes('Chrome')) browser = 'Chrome'
  else if (userAgent.includes('Firefox')) browser = 'Firefox'
  else if (userAgent.includes('Safari')) browser = 'Safari'

  // 检测操作系统
  let os = '未知系统'
  if (userAgent.includes('Windows')) os = 'Windows'
  else if (userAgent.includes('Mac')) os = 'Mac'
  else if (userAgent.includes('Linux')) os = 'Linux'
  else if (userAgent.includes('Android')) os = 'Android'
  else if (userAgent.includes('iPhone') || userAgent.includes('iPad')) os = 'iOS'

  return `${os} - ${browser}`
}

// 加载偏好设置
const loadPreferences = async () => {
  const userId = form.value.userId || userStore.userInfo?.userId || userStore.userInfo?.id
  if (!userId) return

  try {
    const res: any = await request({
      url: `/users/${userId}/preferences`,
      method: 'get'
    })

    if (res?.data && Object.keys(res.data).length > 0) {
      preferences.value = { ...preferences.value, ...res.data }
    }
  } catch (error) {
    console.error('加载偏好设置失败:', error)
  }
}

// 保存偏好设置
const savePreferences = async () => {
  const userId = form.value.userId || userStore.userInfo?.userId || userStore.userInfo?.id
  if (!userId) return

  try {
    await request({
      url: `/users/${userId}/preferences`,
      method: 'put',
      data: preferences.value
    })

    ElMessage.success('偏好设置已保存')
  } catch (error: any) {
    console.error('保存偏好设置失败:', error)
    ElMessage.error(error?.message || '保存偏好设置失败')
  }
}

// 加载隐私设置
const loadPrivacy = async () => {
  const userId = form.value.userId || userStore.userInfo?.userId || userStore.userInfo?.id
  if (!userId) return

  try {
    const res: any = await request({
      url: `/users/${userId}/privacy`,
      method: 'get'
    })

    if (res?.data && Object.keys(res.data).length > 0) {
      privacy.value = { ...privacy.value, ...res.data }
    }
  } catch (error) {
    console.error('加载隐私设置失败:', error)
  }
}

// 保存隐私设置
const savePrivacy = async () => {
  const userId = form.value.userId || userStore.userInfo?.userId || userStore.userInfo?.id
  if (!userId) return

  try {
    await request({
      url: `/users/${userId}/privacy`,
      method: 'put',
      data: privacy.value
    })

    ElMessage.success('隐私设置已保存')
  } catch (error: any) {
    console.error('保存隐私设置失败:', error)
    ElMessage.error(error?.message || '保存隐私设置失败')
  }
}

onMounted(() => {
  loadProfile()
  loadSessions()
  loadPreferences()
  loadPrivacy()
})
</script>

<style scoped>
.profile-page {
  padding: 24px;
  min-height: 100vh;
  background-color: #F0F2F5;
}

.profile-content {
  padding: 24px;
}

.avatar-section {
  border: 1px solid #EBEEF5;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  background: white;
  border-radius: 12px;
}

.form-section {
  padding: 24px;
  background: #FFFFFF;
  border-radius: 12px;
  border: 1px solid #EBEEF5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #2A9D8F;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid #F2F3F5;
}

/* 覆盖 Element Plus 样式 */
.profile-form :deep(.el-form-item__label) {
  color: #606266;
  font-weight: 500;
}

.profile-form :deep(.el-input__wrapper) {
  background: #FFFFFF;
  border: 1px solid #DCDFE6;
  box-shadow: none;
}

.profile-form :deep(.el-input__wrapper:hover) {
  border-color: #2A9D8F;
}

.profile-form :deep(.el-input__wrapper.is-focus) {
  border-color: #2A9D8F;
  box-shadow: 0 0 0 1px #2A9D8F;
}

.profile-form :deep(.el-input__inner) {
  color: #303133;
}

.profile-form :deep(.el-input.is-disabled .el-input__wrapper) {
  background: #F5F7FA;
  border-color: #E4E7ED;
}

.profile-form :deep(.el-input.is-disabled .el-input__inner) {
  color: #909399;
}

.sessions-list {
  max-height: 500px;
  overflow-y: auto;
}

.session-item {
  background: #ffffff;
  border-color: #EBEEF5;
}

.session-item:hover {
  background: #F7F8FA;
  border-color: #2A9D8F;
}

.preferences-grid :deep(.el-form-item) {
  margin-bottom: 20px;
}

.preferences-grid :deep(.el-radio-group) {
  display: flex;
  gap: 16px;
}

.preferences-grid :deep(.el-radio) {
  color: #606266;
}

.preferences-grid :deep(.el-radio.is-checked .el-radio__label) {
  color: #2A9D8F;
}

.preferences-grid :deep(.el-radio__input.is-checked .el-radio__inner) {
  border-color: #2A9D8F;
  background: #2A9D8F;
}

.preferences-grid :deep(.el-select .el-input__wrapper) {
  background: #FFFFFF;
  border: 1px solid #DCDFE6;
}

.preferences-grid :deep(.el-switch.is-checked .el-switch__core) {
  background-color: #2A9D8F;
  border-color: #2A9D8F;
}

.preferences-grid :deep(.el-switch__action) {
  background-color: #fff;
}
</style>

