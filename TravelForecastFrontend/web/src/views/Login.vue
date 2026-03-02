<template>
  <div class="login-container w-full h-screen flex items-center justify-center relative overflow-hidden">
    <ParticleBackground />
    <DataFlowMatrix />
    
    <div class="login-panel z-10 w-full max-w-md px-8 py-10">
      <div class="text-center mb-10">
        <h1 class="text-4xl mb-2 text-[#2A9D8F] font-bold">游韵华章</h1>
        <h2 class="text-xl text-[#2C3E50]">六盘水智慧旅游平台</h2>
      </div>
      
      <HolographicCard 
        title="用户登录" 
        icon="user-3-line" 
        class="mb-6"
      >
        <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef">
          <el-form-item prop="username">
            <div class="relative flex items-center custom-input-container">
              <span class="absolute left-3 text-[#2A9D8F]">
                <i class="ri-user-3-line"></i>
              </span>
              <input v-model="loginForm.username" type="text" placeholder="用户名" class="custom-input" />
            </div>
          </el-form-item>
          
        <el-form-item prop="password">
            <div class="relative flex items-center custom-input-container">
              <span class="absolute left-3 text-[#2A9D8F]">
                <i class="ri-lock-line"></i>
              </span>
            <input v-model="loginForm.password" :type="showPassword ? 'text' : 'password'" placeholder="密码" class="custom-input" />
            <span 
              class="absolute right-3 text-[#2A9D8F] cursor-pointer"
              @click="showPassword = !showPassword"
              :title="showPassword ? '隐藏密码' : '显示密码'"
            >
              <i :class="showPassword ? 'ri-eye-off-line' : 'ri-eye-line'"></i>
            </span>
            </div>
          </el-form-item>
        
        <el-form-item v-if="captchaEnabled" prop="captcha">
          <div class="flex items-center space-x-3">
            <input v-model="loginForm.captcha" placeholder="请输入验证码" style="width: 160px" class="custom-input captcha-input" />
            <img :src="captchaImage" alt="验证码" class="captcha-image cursor-pointer" @click="refreshCaptcha" />
          </div>
        </el-form-item>
          
          <div class="role-selection my-6">
            <p class="mb-2 text-sm text-gray-600">选择角色:</p>
            <div class="flex space-x-4 justify-around">
              <div 
                @click="selectRole('user')" 
                class="role-option"
                :class="{'selected': loginForm.role === 'user'}"
              >
                <i class="ri-user-3-line text-xl"></i>
                <span>用户</span>
              </div>
              <div 
                @click="selectRole('business')" 
                class="role-option"
                :class="{'selected': loginForm.role === 'business'}"
              >
                <i class="ri-store-2-line text-xl"></i>
                <span>商户</span>
              </div>
              <div 
                @click="selectRole('admin')" 
                class="role-option"
                :class="{'selected': loginForm.role === 'admin'}"
              >
                <i class="ri-admin-line text-xl"></i>
                <span>管理员</span>
              </div>
            </div>
          </div>
          
          <div class="flex justify-between items-center mt-8">
            <el-checkbox v-model="loginForm.remember" class="tech-checkbox">记住我</el-checkbox>
            <router-link to="/forgot-password" class="text-sm text-[#2A9D8F] hover:text-[#238B7E] transition">忘记密码?</router-link>
          </div>
          
          <div class="mt-6">
            <NeonButton 
              @click="handleLogin" 
              :disabled="loading" 
              size="lg" 
              class="w-full"
            >
              {{ loading ? '登录中...' : '登录' }}
            </NeonButton>
          </div>
        </el-form>
      </HolographicCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { authApi } from '../api/auth'
import ParticleBackground from '@/components/ParticleBackground.vue'
import DataFlowMatrix from '@/components/DataFlowMatrix.vue'
import HolographicCard from '@/components/HolographicCard.vue'
import NeonButton from '@/components/NeonButton.vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const loginFormRef = ref<FormInstance>()
const isDev = typeof window !== 'undefined' && /localhost|127\.0\.0\.1/.test(window.location.hostname)
const showPassword = ref(false)
const captchaEnabled = ref(true)

const loginForm = reactive({
  username: '',
  password: '',
  captcha: '',
  role: 'user',
  remember: false
})

const loginRules = computed<FormRules>(() => {
  const rules: FormRules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
  }
  if (captchaEnabled.value) {
    rules.captcha = [{ required: true, message: '请输入验证码', trigger: 'blur' }]
  }
  return rules
})

const selectRole = (role: 'user' | 'business' | 'admin') => {
  console.log('选择角色:', role)
  loginForm.role = role
}

const captchaImage = ref('')
const captchaId = ref('')

const refreshCaptcha = async () => {
  try {
    const res = await authApi.getCaptcha()
    // 后端返回的字段是 imageBase64 和 captchaId
    const img = res.data.imageBase64 || res.data.image
    captchaImage.value = img?.startsWith('data:') ? img : `data:image/png;base64,${img}`
    captchaId.value = res.data.captchaId
  } catch (e) {
    ElMessage.error('获取验证码失败')
  }
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    try {
      loading.value = true
      // 调用后端登录接口
      const loginData_req: any = {
        username: loginForm.username,
        password: loginForm.password
      }
      if (captchaEnabled.value) {
        loginData_req.captchaCode = loginForm.captcha
        loginData_req.captchaId = captchaId.value
      }
      const res = await authApi.login(loginData_req)
      
      // 后端返回的数据结构: { token, refreshToken, userId, username, nickname, role, email, phone, avatar }
      const loginData = res.data
      const userInfo = {
        id: loginData.userId,
        username: loginData.username,
        nickname: loginData.nickname,
        avatar: loginData.avatar || '',
        role: loginData.role,
        email: loginData.email,
        phone: loginData.phone
      }
      
      userStore.login(userInfo, loginData.token, loginData.refreshToken)
      
      // 根据角色跳转到对应页面
      const redirectMap: Record<string, string> = {
        user: '/user/dashboard',
        merchant: '/business/dashboard',
        business: '/business/dashboard',
        admin: '/admin/dashboard'
      }
      
      const redirectPath = redirectMap[loginData.role as keyof typeof redirectMap]
      console.log('当前角色:', loginData.role, '跳转路径:', redirectPath)
      
      ElMessage.success('登录成功')
      
      // 使用路由导航替代直接修改location
      router.push(redirectPath)
    } catch (error: any) {
      console.error('登录失败:', error)
      const errorMsg = error?.response?.data?.message || error?.message || '登录失败，请检查用户名/密码/验证码'
      ElMessage.error(errorMsg)
      if (captchaEnabled.value) refreshCaptcha()
    } finally {
      loading.value = false
    }
  })
}

onMounted(async () => {
  try {
    const configRes: any = await authApi.getLoginConfig()
    if (configRes?.data) {
      captchaEnabled.value = configRes.data.enableCaptcha !== false
    }
  } catch (e) {
    captchaEnabled.value = true
  }
  if (captchaEnabled.value) {
    refreshCaptcha()
  }
})
</script>

<style scoped>
.login-container {
  background: #F5F7FA;
}

.login-panel {
  backdrop-filter: blur(16px);
  animation: fadeIn 1s ease-out;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.tech-input :deep(.el-input__wrapper) {
  @apply bg-white border-gray-300 rounded-lg;
  box-shadow: none !important;
}

.tech-input :deep(.el-input__inner) {
  @apply text-gray-800;
  color: #2C3E50 !important;
}

.tech-input :deep(.el-input__prefix) {
  @apply text-[#2A9D8F];
}

.role-option {
  @apply flex flex-col items-center p-3 rounded-lg cursor-pointer transition-all duration-300;
  @apply bg-gray-50 border border-gray-200 text-gray-600;
  width: 80px;
}

.role-option:hover {
  @apply border-[#2A9D8F] text-gray-800;
}

.role-option.selected {
  @apply border-[#2A9D8F] text-[#2A9D8F] bg-[#2A9D8F]/5;
  box-shadow: 0 0 10px rgba(42, 157, 143, 0.15);
}

.tech-checkbox :deep(.el-checkbox__inner) {
  @apply bg-white border-gray-300;
}

.tech-checkbox :deep(.el-checkbox__label) {
  @apply text-gray-600;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 添加额外的输入框样式 */
.el-input__wrapper {
  background-color: #FFFFFF !important;
  border: 1px solid #DCDFE6 !important;
}

.el-input__inner {
  color: #2C3E50 !important;
}

/* 统一验证码输入框与上方输入框风格 */
.custom-input-el :deep(.el-input__wrapper) {
  background-color: #FFFFFF !important;
  border: 1px solid #DCDFE6 !important;
  box-shadow: none !important;
}
.custom-input-el :deep(.el-input__inner) {
  color: #303133 !important;
}

/* 修复密码框眼睛图标 */
.tech-input :deep(.el-input__suffix) {
  color: #2A9D8F;
}

/* 自定义输入框样式 */
.custom-input-container {
  position: relative;
  width: 100%;
}

.custom-input {
  width: 100%;
  height: 40px;
  background-color: #FFFFFF !important;
  border: 1px solid #DCDFE6 !important;
  border-radius: 0.5rem;
  color: #2C3E50 !important;
  padding: 0.5rem 2.5rem 0.5rem 2.5rem;
  transition: all 0.3s ease;
}

.captcha-input {
  padding-right: 1rem;
}

.custom-input:focus {
  outline: none;
  border-color: #2A9D8F;
  box-shadow: 0 0 5px rgba(42, 157, 143, 0.2);
}

.custom-input::placeholder {
  color: #A8ABB2;
}
</style> 