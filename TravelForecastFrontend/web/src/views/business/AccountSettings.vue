<template>
  <div class="account-settings">
    <el-card class="bg-white border border-gray-200 shadow-lg">
      <template #header>
        <div class="flex items-center justify-between">
          <h3 class="text-lg text-[#2A9D8F] font-bold">账号设置</h3>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </div>
      </template>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- 左侧：头像上传 -->
        <div class="flex flex-col items-center space-y-4">
          <el-avatar :size="150" :src="form.avatar" icon="User" class="border-4 border-[#2A9D8F]" />
          
          <el-upload
            class="avatar-uploader"
            :action="uploadAction"
            :headers="{ Authorization: `Bearer ${token}` }"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :on-error="handleAvatarError"
            :before-upload="beforeAvatarUpload"
            accept="image/*"
          >
            <el-button type="primary" :icon="Upload">上传头像</el-button>
          </el-upload>
          
          <div class="text-gray-500 text-sm text-center space-y-1">
            <p>支持 JPG、PNG 格式<br/>文件大小不超过 2MB</p>
            <p class="mt-3">注册时间：{{ formatDate(form.createdAt) }}</p>
            <p>最后登录：{{ formatDate(form.loginAt) }}</p>
          </div>
        </div>

        <!-- 右侧：表单 -->
        <div class="lg:col-span-2">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
            <el-form-item label="用户名">
              <el-input v-model="form.username" disabled />
            </el-form-item>

            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入昵称" />
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>

            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>

            <el-divider />

            <h4 class="text-gray-800 text-base mb-4">修改密码</h4>
            
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input 
                v-model="form.oldPassword" 
                type="password" 
                placeholder="留空则不修改密码" 
                show-password 
              />
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword">
              <el-input 
                v-model="form.newPassword" 
                type="password" 
                placeholder="留空则不修改密码" 
                show-password 
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input 
                v-model="form.confirmPassword" 
                type="password" 
                placeholder="请再次输入新密码" 
                show-password 
              />
            </el-form-item>
          </el-form>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const saving = ref(false)
const token = computed(() => localStorage.getItem('token') || '')
const uploadAction = '/api/upload/avatar'

const form = ref<any>({
  userId: userStore.userInfo?.id,
  username: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
  createdAt: '',
  loginAt: '',
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validatePassword = (rule: any, value: any, callback: any) => {
  if (value && value.length < 6) {
    callback(new Error('密码长度不能少于6位'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (value && value !== form.value.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  nickname: [{ required: false, message: '请输入昵称', trigger: 'blur' }],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  newPassword: [{ validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

// 加载用户信息
const loadUserInfo = async () => {
  try {
    const res: any = await request({
      url: `/users/${userStore.userInfo?.id}`,
      method: 'get'
    })
    
    if (res?.data) {
      form.value = {
        ...form.value,
        username: res.data.username,
        nickname: res.data.nickname || '',
        email: res.data.email || '',
        phone: res.data.phone || '',
        avatar: res.data.avatar || '',
        createdAt: res.data.createdAt || '',
        loginAt: res.data.loginAt || ''
      }
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
  }
}

// 头像上传前的校验
const beforeAvatarUpload = (file: any) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }
  return true
}

// 头像上传成功
const handleAvatarSuccess = (response: any) => {
  if (response.code === 200 || response.code === 1) {
    form.value.avatar = response.data.url || response.data
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 头像上传失败
const handleAvatarError = (error: any) => {
  console.error('上传失败:', error)
  ElMessage.error('上传失败，请重试')
}

// 保存设置
const handleSave = async () => {
  if (!formRef.value) return
  
  try {
    // 检查是否要修改密码（任何一个密码字段有值）
    const hasPasswordInput = form.value.oldPassword || form.value.newPassword || form.value.confirmPassword
    
    if (hasPasswordInput) {
      // 如果要修改密码，必须三个字段都填写
      if (!form.value.oldPassword || !form.value.newPassword || !form.value.confirmPassword) {
        ElMessage.warning('修改密码需要填写：旧密码、新密码、确认密码')
        return
      }
      
      // 验证新密码
      if (form.value.newPassword !== form.value.confirmPassword) {
        ElMessage.warning('两次输入的新密码不一致')
        return
      }
      
      if (form.value.newPassword.length < 6) {
        ElMessage.warning('新密码长度不能少于6位')
        return
      }
    }
    
    saving.value = true
    
    // 保存基本信息（即使只修改头像也可以保存）
    await request({
      url: `/users/${form.value.userId}`,
      method: 'put',
      data: {
        nickname: form.value.nickname,
        email: form.value.email,
        phone: form.value.phone,
        avatar: form.value.avatar
      }
    })
    
    // 如果需要修改密码
    if (hasPasswordInput && form.value.oldPassword && form.value.newPassword) {
      await request({
        url: `/users/${form.value.userId}/password`,
        method: 'put',
        data: {
          oldPassword: form.value.oldPassword,
          newPassword: form.value.newPassword
        }
      })
      
      // 清空密码字段
      form.value.oldPassword = ''
      form.value.newPassword = ''
      form.value.confirmPassword = ''
      
      ElMessage.success('密码修改成功，个人信息已保存')
    } else {
      ElMessage.success('保存成功')
    }
    
    // 更新Store中的用户信息（使用响应式更新）
    if (userStore.userInfo) {
      const updatedUserInfo = {
        ...userStore.userInfo,
        nickname: form.value.nickname,
        avatar: form.value.avatar,
        email: form.value.email,
        phone: form.value.phone
      }
      userStore.userInfo = updatedUserInfo
      localStorage.setItem('userInfo', JSON.stringify(updatedUserInfo))
    }
  } catch (error: any) {
    console.error('保存失败:', error)
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return '未知'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.account-settings {
  color: #2C3E50;
}

:deep(.el-card) {
  background: #FFFFFF;
  border: 1px solid #EBEEF5;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

:deep(.el-card__header) {
  border-bottom: 1px solid #EBEEF5;
  padding: 12px 20px;
}

:deep(.el-form-item__label) {
  color: #606266 !important;
}

:deep(.el-input__wrapper) {
  background-color: #FFFFFF !important;
  border: 1px solid #DCDFE6 !important;
  box-shadow: none !important;
}

:deep(.el-input__wrapper:hover),
:deep(.el-input__wrapper.is-focus) {
  background-color: #FFFFFF !important;
  border-color: #2A9D8F !important;
}

:deep(.el-input__inner) {
  background-color: transparent !important;
  color: #2C3E50 !important;
}

:deep(.el-input__inner::placeholder) {
  color: #A8ABB2 !important;
}

:deep(.el-input.is-disabled .el-input__wrapper) {
  background-color: #F5F7FA !important;
  border-color: #E4E7ED !important;
}

:deep(.el-divider) {
  border-color: #EBEEF5;
}

.avatar-uploader {
  display: flex;
  justify-content: center;
}
</style>
