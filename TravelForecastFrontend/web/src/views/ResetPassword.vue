<template>
  <div class="reset-container w-full h-screen flex items-center justify-center relative overflow-hidden bg-gradient-to-br from-gray-50 via-white to-teal-50">
    <div class="z-10 w-full max-w-md px-8 py-10">
      <div class="text-center mb-8">
        <h1 class="text-3xl mb-2 text-[#2A9D8F] font-bold">重置密码</h1>
        <p class="text-gray-500">请输入您的新密码</p>
      </div>

      <holographic-card icon="lock-password-line">
        <el-form :model="form" :rules="rules" ref="formRef" class="mt-2">
          <el-form-item prop="password">
            <div class="relative flex items-center w-full">
              <span class="absolute left-3 text-[#2A9D8F]">
                <i class="ri-lock-line"></i>
              </span>
              <input
                v-model="form.password"
                type="password"
                placeholder="请输入新密码（至少6位）"
                class="w-full pl-10 pr-4 py-3 rounded-lg border border-gray-200 bg-white text-gray-800 placeholder-gray-400 focus:outline-none focus:border-[#2A9D8F] focus:ring-1 focus:ring-[#2A9D8F] transition"
              />
            </div>
          </el-form-item>
          <el-form-item prop="confirm">
            <div class="relative flex items-center w-full">
              <span class="absolute left-3 text-[#2A9D8F]">
                <i class="ri-lock-check-line"></i>
              </span>
              <input
                v-model="form.confirm"
                type="password"
                placeholder="再次输入新密码"
                class="w-full pl-10 pr-4 py-3 rounded-lg border border-gray-200 bg-white text-gray-800 placeholder-gray-400 focus:outline-none focus:border-[#2A9D8F] focus:ring-1 focus:ring-[#2A9D8F] transition"
              />
            </div>
          </el-form-item>

          <el-button
            type="primary"
            :loading="loading"
            @click="submit"
            class="w-full mt-2"
            style="background: #2A9D8F; border-color: #2A9D8F; height: 44px; font-size: 16px; border-radius: 8px;"
          >
            确认重置
          </el-button>

          <div class="text-center mt-6">
            <router-link to="/login" class="text-sm text-[#2A9D8F] hover:text-[#238B7E] transition">
              <i class="ri-arrow-left-line mr-1"></i>返回登录
            </router-link>
          </div>
        </el-form>
      </holographic-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import HolographicCard from '@/components/HolographicCard.vue'
import { authApi } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = ref({ password: '', confirm: '' })

const rules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '至少6位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_: any, val: string, cb: any) => {
        if (val !== form.value.password) cb(new Error('两次输入不一致'))
        else cb()
      },
      trigger: 'blur'
    }
  ]
}

const submit = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return
  const token = (route.query.token as string) || ''
  if (!token) return ElMessage.error('链接无效')
  loading.value = true
  try {
    await authApi.resetPassword(token, form.value.password)
    ElMessage.success('密码已重置，请使用新密码登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
</style>


