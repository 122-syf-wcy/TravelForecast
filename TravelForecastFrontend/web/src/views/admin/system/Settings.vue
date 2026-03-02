<template>
  <div class="settings-container p-6">
    <div class="page-header mb-6">
      <h2 class="text-2xl font-bold text-gray-800">系统配置</h2>
      <p class="text-gray-500 mt-1">系统参数与全局配置管理</p>
    </div>

    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="基本设置" name="basic">
        <HolographicCard>
          <el-form :model="basicForm" label-width="120px">
            <el-form-item label="系统名称">
              <el-input v-model="basicForm.systemName" placeholder="输入系统名称" />
            </el-form-item>
            <el-form-item label="系统Logo">
              <div class="flex items-center gap-4">
                <el-upload
                  :action="uploadAction"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  :before-upload="beforeLogoUpload"
                  :on-success="handleLogoSuccess"
                >
                  <el-button size="small">点击上传</el-button>
                </el-upload>
                <el-image
                  v-if="basicForm.logo"
                  :src="basicForm.logo"
                  style="width: 100px; height: 50px"
                  fit="contain"
                />
              </div>
              <div class="text-xs text-gray-500 mt-1">支持JPG、PNG格式，大小不超过2MB</div>
            </el-form-item>
            <el-form-item label="备案号">
              <el-input v-model="basicForm.icp" placeholder="输入备案号" />
            </el-form-item>
            <el-form-item label="联系邮箱">
              <el-input v-model="basicForm.email" placeholder="输入联系邮箱" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSave">保存配置</el-button>
            </el-form-item>
          </el-form>
        </HolographicCard>
      </el-tab-pane>

      <el-tab-pane label="业务配置" name="business">
        <HolographicCard>
          <el-form :model="businessForm" label-width="150px">
            <el-form-item label="客流预测周期（天）">
              <el-input-number v-model="businessForm.predictDays" :min="1" :max="30" />
            </el-form-item>
            <el-form-item label="数据刷新间隔（分）">
              <el-input-number v-model="businessForm.refreshInterval" :min="1" :max="60" />
            </el-form-item>
            <el-form-item label="商户审核自动通过">
              <el-switch v-model="businessForm.autoApprove" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSave">保存配置</el-button>
            </el-form-item>
          </el-form>
        </HolographicCard>
      </el-tab-pane>

      <el-tab-pane label="安全设置" name="security">
        <HolographicCard>
          <el-form :model="securityForm" label-width="150px">
            <el-form-item label="密码最小长度">
              <el-input-number v-model="securityForm.minPasswordLength" :min="6" :max="20" />
            </el-form-item>
            <el-form-item label="登录失败锁定次数">
              <el-input-number v-model="securityForm.lockThreshold" :min="3" :max="10" />
            </el-form-item>
            <el-form-item label="会话超时时间（分）">
              <el-input-number v-model="securityForm.sessionTimeout" :min="10" :max="480" />
            </el-form-item>
            <el-form-item label="启用验证码">
              <el-switch v-model="securityForm.enableCaptcha" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSave">保存配置</el-button>
            </el-form-item>
          </el-form>
        </HolographicCard>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import HolographicCard from '@/components/HolographicCard.vue'
import { systemConfigApi } from '@/api/adminSystem'
import { useSystemConfig } from '@/composables/useSystemConfig'

const activeTab = ref('basic')

// 配置映射关系
const CONFIG_KEYS = {
  basic: {
    systemName: 'system_name',
    icp: 'system_icp',
    email: 'contact_email',
    logo: 'system_logo'
  },
  business: {
    predictDays: 'predict_days',
    refreshInterval: 'refresh_interval',
    autoApprove: 'auto_approve_merchant'
  },
  security: {
    minPasswordLength: 'min_password_length',
    lockThreshold: 'login_lock_threshold',
    sessionTimeout: 'session_timeout',
    enableCaptcha: 'enable_captcha'
  }
}

const basicForm = ref({
  systemName: '六盘水旅游管理系统',
  icp: '黔ICP备XXXXXXXX号',
  email: 'admin@liupanshui.gov.cn',
  logo: ''
})

const businessForm = ref({
  predictDays: 7,
  refreshInterval: 5,
  autoApprove: false
})

const securityForm = ref({
  minPasswordLength: 8,
  lockThreshold: 5,
  sessionTimeout: 30,
  enableCaptcha: true
})

// 加载配置
const loadConfig = async () => {
  try {
    const res: any = await systemConfigApi.getConfig()
    if (res.data) {
      const config = res.data
      
      // 基本设置
      if (config.system_name) basicForm.value.systemName = config.system_name
      if (config.system_icp) basicForm.value.icp = config.system_icp
      if (config.contact_email) basicForm.value.email = config.contact_email
      if (config.system_logo) basicForm.value.logo = config.system_logo
      
      // 业务配置
      if (config.predict_days) businessForm.value.predictDays = parseInt(config.predict_days)
      if (config.refresh_interval) businessForm.value.refreshInterval = parseInt(config.refresh_interval)
      if (config.auto_approve_merchant) businessForm.value.autoApprove = config.auto_approve_merchant === 'true'
      
      // 安全设置
      if (config.min_password_length) securityForm.value.minPasswordLength = parseInt(config.min_password_length)
      if (config.login_lock_threshold) securityForm.value.lockThreshold = parseInt(config.login_lock_threshold)
      if (config.session_timeout) securityForm.value.sessionTimeout = parseInt(config.session_timeout)
      if (config.enable_captcha) securityForm.value.enableCaptcha = config.enable_captcha === 'true'
    }
  } catch (error: any) {
    console.error('加载配置失败:', error)
  }
}

// 保存配置
const handleSave = async () => {
  try {
    let configToSave: Record<string, string> = {}
    
    // 根据当前标签页保存对应的配置
    if (activeTab.value === 'basic') {
      configToSave = {
        system_name: basicForm.value.systemName,
        system_icp: basicForm.value.icp,
        contact_email: basicForm.value.email
      }
      if (basicForm.value.logo) {
        configToSave.system_logo = basicForm.value.logo
      }
    } else if (activeTab.value === 'business') {
      configToSave = {
        predict_days: String(businessForm.value.predictDays),
        refresh_interval: String(businessForm.value.refreshInterval),
        auto_approve_merchant: String(businessForm.value.autoApprove)
      }
    } else if (activeTab.value === 'security') {
      configToSave = {
        min_password_length: String(securityForm.value.minPasswordLength),
        login_lock_threshold: String(securityForm.value.lockThreshold),
        session_timeout: String(securityForm.value.sessionTimeout),
        enable_captcha: String(securityForm.value.enableCaptcha)
      }
    }
    
    await systemConfigApi.saveConfig(configToSave)
    
    // 如果保存了基本设置，更新全局配置
    if (activeTab.value === 'basic') {
      const { updateFavicon, updateTitle } = useSystemConfig()
      if (basicForm.value.logo) {
        updateFavicon(basicForm.value.logo)
      }
      if (basicForm.value.systemName) {
        updateTitle(basicForm.value.systemName)
      }
    }
    
    ElMessage.success('配置保存成功')
  } catch (error: any) {
    ElMessage.error('保存失败: ' + (error?.message || '未知错误'))
  }
}

// Logo上传
const uploadAction = computed(() => {
  return `${import.meta.env.VITE_API_BASE_URL || ''}/api/upload/file`
})

const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return {
    'Authorization': `Bearer ${token}`
  }
})

const handleLogoSuccess = (response: any) => {
  if (response.code === 200 && response.data) {
    basicForm.value.logo = response.data.url
    
    // 立即更新favicon预览
    const { updateFavicon } = useSystemConfig()
    updateFavicon(response.data.url)
    
    ElMessage.success('Logo上传成功')
  } else {
    ElMessage.error('上传失败: ' + (response.message || '未知错误'))
  }
}

const beforeLogoUpload = (file: File) => {
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

// 页面加载时获取配置
onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.settings-container {
  min-height: 100vh;
  background: transparent;
}

.page-header {
  margin-bottom: 24px;
}

:deep(.el-tabs--border-card) {
  background: transparent;
  border-color: rgba(42, 157, 143, 0.2);
}

:deep(.el-tabs__header) {
  background: rgba(245, 247, 250, 0.5);
}

:deep(.el-tabs__item) {
  color: #888;
  border-color: rgba(42, 157, 143, 0.1);
}

:deep(.el-tabs__item.is-active) {
  color: #2A9D8F;
  border-bottom-color: transparent;
}

:deep(.el-tabs__content) {
  padding: 20px;
}

:deep(.el-form) {
  max-width: 800px;
}

:deep(.el-form-item__label) {
  color: #606266;
}

:deep(.el-input__wrapper) {
  background: rgba(42, 157, 143, 0.03);
  border: 1px solid rgba(42, 157, 143, 0.2);
}

:deep(.el-input-number) {
  background: rgba(42, 157, 143, 0.03);
}

:deep(.el-switch) {
  --el-switch-on-color: #2A9D8F;
}
</style>













