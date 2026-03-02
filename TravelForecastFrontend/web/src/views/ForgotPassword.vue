<template>
  <div class="forgot-container w-full h-screen flex items-center justify-center relative overflow-hidden bg-gradient-to-br from-gray-50 via-white to-teal-50">
    <div class="z-10 w-full max-w-md px-8 py-10">
      <div class="text-center mb-8">
        <h1 class="text-3xl mb-2 text-[#2A9D8F] font-bold">忘记密码</h1>
        <p class="text-gray-500">输入您的用户名或邮箱，我们将发送重置链接</p>
      </div>

      <holographic-card icon="key-2-line">
        <el-form :model="form" class="mt-2">
          <el-form-item>
            <div class="relative flex items-center w-full">
              <span class="absolute left-3 text-[#2A9D8F]">
                <i class="ri-mail-line"></i>
              </span>
              <input
                v-model="form.usernameOrEmail"
                type="text"
                placeholder="请输入用户名或邮箱"
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
            发送重置链接
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
import { ElMessage } from 'element-plus'
import HolographicCard from '@/components/HolographicCard.vue'
import { authApi } from '@/api/auth'

const form = ref({ usernameOrEmail: '' })
const loading = ref(false)

const submit = async () => {
  if (!form.value.usernameOrEmail) return ElMessage.warning('请输入用户名或邮箱')
  loading.value = true
  try {
    await authApi.forgotPassword(form.value.usernameOrEmail)
    ElMessage.success('重置链接已发送，请检查邮箱')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
</style>


