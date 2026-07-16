<template>
  <div class="register-container w-full min-h-screen flex items-center justify-center py-10">
    <holographic-card class="register-card w-full max-w-lg p-8">
      <template #header>
        <h2 class="text-xl font-bold text-[#2A9D8F]">用户注册</h2>
      </template>

      <el-form :model="registerForm" :rules="rules" ref="registerFormRef" label-width="90px">
        <el-form-item label="注册类型">
          <el-radio-group v-model="registerForm.userType">
            <el-radio label="USER">普通用户</el-radio>
            <el-radio label="MERCHANT">商家用户</el-radio>
          </el-radio-group>
          <div v-if="registerForm.userType === 'MERCHANT'" class="text-[#F4A261] text-xs mt-1">ⓘ 商家用户需要管理员审核后方可登录</div>
        </el-form-item>

        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="4-16位字母、数字、下划线" clearable />
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="请输入昵称" clearable />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="6-20位密码" show-password />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次输入密码" show-password />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱（可选）" clearable />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号（可选）" clearable maxlength="11" />
        </el-form-item>

        <el-form-item label="验证码" prop="captcha">
          <div class="flex items-center space-x-3">
            <el-input v-model="registerForm.captcha" placeholder="输入验证码" style="width: 120px" />
            <img :src="captchaImage" alt="验证码" class="captcha-image cursor-pointer" @click="refreshCaptcha" />
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="handleRegister">立即注册</el-button>
        </el-form-item>
      </el-form>

      <div class="text-center text-gray-600 text-sm">
        已有账号？<el-link type="primary" @click="goToLogin">立即登录</el-link>
      </div>
    </holographic-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import HolographicCard from '@/components/HolographicCard.vue'
import { authApi } from '@/api/auth'

const router = useRouter()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)
const captchaImage = ref('')
const captchaId = ref('')

const registerForm = reactive({
  userType: 'USER',
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  captcha: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 16, message: '用户名长度4-16位', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,16}$/, message: '用户名只能包含字母、数字、下划线', trigger: 'blur' }
  ],
  nickname: [
    { max: 50, message: '昵称长度不能超过50位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== registerForm.password) callback(new Error('两次输入密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '验证码长度为4位', trigger: 'blur' }
  ]
}

const refreshCaptcha = async () => {
  try {
    const res = await authApi.getCaptcha()
    // 后端返回的字段是 imageBase64 和 captchaId
    captchaImage.value = res.data.imageBase64
    captchaId.value = res.data.captchaId
  } catch (e) {
    ElMessage.error('获取验证码失败')
  }
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  const valid = await registerFormRef.value.validate()
  if (!valid) return
  loading.value = true
  try {
    const payload = {
      username: registerForm.username,
      password: registerForm.password,
      nickname: registerForm.nickname || null,
      email: registerForm.email || null,
      phone: registerForm.phone || null,
      captchaCode: registerForm.captcha,
      captchaId: captchaId.value
    }
    if (registerForm.userType === 'MERCHANT') {
      await authApi.registerMerchant(payload)
      ElMessage.success('商家注册成功，需管理员审核后方可登录')
    } else {
      await authApi.registerUser(payload)
      ElMessage.success('注册成功！即将跳转到登录页面')
    }
    setTimeout(() => router.push('/login'), 1200)
  } catch (err: any) {
    console.error('注册失败:', err)
    const errorMsg = err?.response?.data?.message || err?.message || '注册失败'
    ElMessage.error(errorMsg)
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

const goToLogin = () => router.push('/login')
onMounted(refreshCaptcha)
</script>

<style scoped>
.register-container { background: #F5F7FA; }
</style>






