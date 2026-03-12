<template>
  <div class="account-settings">
    <div class="page-header">
      <h1 class="text-2xl text-gray-800 mb-4">系统设置</h1>
      <p class="text-gray-500">智教黔行系统配置</p>
    </div>
    
    <el-tabs type="border-card" class="bg-white border border-gray-200 shadow-lg mb-6">
      <el-tab-pane label="账户管理">
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
          <div class="lg:col-span-1">
            <holographic-card title="管理员账户" icon="admin-line" glowColor="tech-purple">
              <div class="text-center mb-4">
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
                  <el-avatar :size="80" :src="accountForm.avatar || 'https://via.placeholder.com/150'" class="mb-2 cursor-pointer hover:opacity-80" />
                </el-upload>
                <h3 class="text-lg text-gray-800">{{ accountForm.username || '系统管理员' }}</h3>
                <p class="text-gray-500">{{ accountForm.email || 'admin@liupanshui.gov.cn' }}</p>
                <div class="mt-3 text-sm text-gray-500">
                  <p>注册时间：{{ formatDate(accountForm.createdAt) }}</p>
                  <p>最后登录：{{ formatDate(accountForm.loginAt) }}</p>
                </div>
              </div>
              <div class="flex justify-center">
                <el-upload
                  :action="uploadAction"
                  :headers="{ Authorization: `Bearer ${token}` }"
                  :show-file-list="false"
                  :on-success="handleAvatarSuccess"
                  :on-error="handleAvatarError"
                  :before-upload="beforeAvatarUpload"
                  accept="image/*"
                >
                  <el-button type="primary" size="small">
                    <el-icon class="mr-1"><Upload /></el-icon> 上传头像
                  </el-button>
                </el-upload>
              </div>
            </holographic-card>
          </div>
          
          <div class="lg:col-span-2">
            <holographic-card title="账户信息" icon="user-settings-line" glowColor="tech-purple">
              <el-form label-position="top" class="text-gray-800">
                <el-form-item label="用户名">
                  <el-input v-model="accountForm.username" placeholder="管理员用户名" />
                </el-form-item>
                <el-form-item label="邮箱">
                  <el-input v-model="accountForm.email" placeholder="管理员邮箱" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="accountForm.phone" placeholder="联系电话" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="saveAccountInfo">保存信息</el-button>
                  <el-button class="ml-2" type="danger" plain @click="logoutAllDevices">退出全部设备</el-button>
                </el-form-item>
              </el-form>
            </holographic-card>
          </div>
        </div>
        
        <holographic-card title="密码设置" icon="lock-password-line" glowColor="tech-purple">
          <el-form label-position="top" class="text-gray-800">
            <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
              <el-form-item label="当前密码">
                <el-input v-model="passwordForm.currentPassword" type="password" show-password placeholder="输入当前密码" />
              </el-form-item>
              <el-form-item label="新密码">
                <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="输入新密码" />
              </el-form-item>
              <el-form-item label="确认新密码">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
              </el-form-item>
            </div>
            <el-form-item>
              <el-button type="primary" @click="changePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </holographic-card>
      </el-tab-pane>
      
      <el-tab-pane label="系统配置">
        <holographic-card title="基础设置" icon="settings-line" glowColor="energy-orange">
          <el-form label-position="top" class="text-gray-800">
            <el-form-item label="系统名称">
              <el-input v-model="systemForm.name" placeholder="系统名称" />
            </el-form-item>
            <el-form-item label="系统描述">
              <el-input v-model="systemForm.description" type="textarea" :rows="3" placeholder="系统描述" />
            </el-form-item>
            <el-form-item label="联系邮箱">
              <el-input v-model="systemForm.contactEmail" placeholder="联系邮箱" />
            </el-form-item>
            <el-form-item label="技术支持电话">
              <el-input v-model="systemForm.supportPhone" placeholder="技术支持电话" />
            </el-form-item>
            <el-form-item label="版权信息">
              <el-input v-model="systemForm.copyright" placeholder="版权信息" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveSystemSettings">保存设置</el-button>
            </el-form-item>
          </el-form>
        </holographic-card>
        
        <holographic-card title="数据备份" icon="database-2-line" glowColor="energy-orange" class="mt-6">
          <div class="flex justify-between items-center mb-4">
            <div>
              <h3 class="text-gray-800 mb-1">自动备份设置</h3>
              <p class="text-gray-500 text-sm">配置系统数据自动备份策略</p>
            </div>
            <el-switch v-model="backupForm.autoBackup" />
          </div>
          
          <el-form label-position="top" class="text-gray-800" v-if="backupForm.autoBackup">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <el-form-item label="备份频率">
                <el-select v-model="backupForm.frequency" placeholder="选择备份频率" class="w-full">
                  <el-option label="每天" value="daily" />
                  <el-option label="每周" value="weekly" />
                  <el-option label="每月" value="monthly" />
                </el-select>
              </el-form-item>
              <el-form-item label="备份时间">
                <el-time-picker v-model="backupForm.time" format="HH:mm" placeholder="选择时间" class="w-full" />
              </el-form-item>
            </div>
            <el-form-item label="保留备份数量">
              <el-input-number v-model="backupForm.keepCount" :min="1" :max="30" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveBackupSettings">保存备份设置</el-button>
            </el-form-item>
          </el-form>
          
          <div class="mt-4">
            <div class="flex justify-between items-center mb-2">
              <h3 class="text-gray-800">手动备份</h3>
              <el-button type="success" size="small" @click="createManualBackup">
                <el-icon class="mr-1"><Plus /></el-icon> 创建备份
              </el-button>
            </div>
            <el-table :data="backupHistory" style="width: 100%" class="bg-transparent">
              <el-table-column prop="date" label="备份日期" width="180" />
              <el-table-column prop="size" label="大小" width="120" />
              <el-table-column prop="type" label="类型" width="120">
                <template #default="scope">
                  <el-tag :type="scope.row.type === '自动' ? 'info' : 'success'" size="small">
                    {{ scope.row.type }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200">
                <template #default="scope">
                  <el-button type="primary" link size="small" @click="downloadBackup(scope.row)">
                    下载
                  </el-button>
                  <el-button type="danger" link size="small" @click="deleteBackup(scope.row)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </holographic-card>
      </el-tab-pane>
      
      <el-tab-pane label="日志管理">
        <holographic-card title="系统日志" icon="file-list-line" glowColor="neon-cyan">
          <div class="flex justify-between items-center mb-4">
              <div class="flex items-center gap-2">
              <el-select v-model="logFilter.level" placeholder="日志级别" size="small" class="mr-2" @change="loadSystemLogs">
                <el-option label="全部" value="" />
                <el-option label="信息" value="info" />
                <el-option label="警告" value="warning" />
                <el-option label="错误" value="error" />
              </el-select>
              <el-date-picker
                v-model="logFilter.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                size="small"
                @change="loadSystemLogs"
              />
              <el-button type="primary" size="small" @click="loadSystemLogs">刷新</el-button>
            </div>
            <el-button type="primary" size="small" @click="exportLogs">
              <el-icon class="mr-1"><Download /></el-icon> 导出日志
            </el-button>
          </div>
          
          <el-table :data="systemLogs" style="width: 100%" class="bg-transparent">
            <el-table-column prop="logTime" label="时间" width="180" />
            <el-table-column prop="logLevel" label="级别" width="100">
              <template #default="scope">
                <el-tag
                  :type="scope.row.logLevel === 'error' ? 'danger' : scope.row.logLevel === 'warning' ? 'warning' : 'info'"
                  size="small"
                >
                  {{ scope.row.logLevel === 'error' ? '错误' : scope.row.logLevel === 'warning' ? '警告' : '信息' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="module" label="模块" width="120" />
            <el-table-column prop="message" label="内容" />
            <el-table-column prop="username" label="用户" width="120" />
            <el-table-column prop="ipAddress" label="IP地址" width="150" />
          </el-table>
          
          <div class="flex justify-end mt-4">
            <el-pagination
              v-model:current-page="logPagination.page"
              v-model:page-size="logPagination.size"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="logPagination.total"
              @size-change="loadSystemLogs"
              @current-change="loadSystemLogs"
            />
          </div>
        </holographic-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Plus, Download, Upload } from '@element-plus/icons-vue'
import HolographicCard from '@/components/HolographicCard.vue'
import { authApi } from '@/api/auth'
import { adminAccountApi, systemConfigApi, systemLogApi } from '@/api/adminSystem'
import { backupApi } from '@/api/backup'
import { useUserStore } from '@/store/user'

// 用户Store
const userStore = useUserStore()

// 上传配置
const token = computed(() => localStorage.getItem('token') || '')
const uploadAction = '/api/upload/file'

// 账户表单
const accountForm = reactive({
  userId: null as number | null,
  username: '',
  email: '',
  phone: '',
  avatar: '',
  createdAt: '',
  loginAt: ''
})

// 加载账户信息
const loadAccountInfo = async () => {
  try {
    const res: any = await adminAccountApi.getInfo()
    if (res.data) {
      Object.assign(accountForm, res.data)
    }
  } catch (error: any) {
    console.error('加载账户信息失败:', error)
  }
}

// 密码表单
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 系统设置表单
const systemForm = reactive({
  name: '智教黔行',
  description: '基于大数据的六盘水旅游景区客流量预测与智能管理平台',
  contactEmail: 'support@liupanshui.gov.cn',
  supportPhone: '0858-12345678',
  copyright: '© 2023 六盘水市旅游局'
})

// 备份设置
const backupForm = reactive({
  autoBackup: true,
  frequency: 'daily',
  time: new Date(2023, 0, 1, 2, 0),
  keepCount: 7
})

// 备份历史
const backupHistory = [
  { date: '2023-07-11 02:00:00', size: '156MB', type: '自动' },
  { date: '2023-07-10 02:00:00', size: '152MB', type: '自动' },
  { date: '2023-07-09 15:30:22', size: '150MB', type: '手动' },
  { date: '2023-07-09 02:00:00', size: '149MB', type: '自动' },
  { date: '2023-07-08 02:00:00', size: '148MB', type: '自动' }
]

// 日志筛选
const logFilter = reactive({
  level: '',
  dateRange: []
})

// 系统日志
const systemLogs = ref<any[]>([])
const logPagination = ref({
  page: 1,
  size: 10,
  total: 0
})

// 加载系统日志
const loadSystemLogs = async () => {
  try {
    const params: any = {
      page: logPagination.value.page,
      size: logPagination.value.size
    }

    if (logFilter.level) {
      params.level = logFilter.level
    }

    if (logFilter.dateRange && logFilter.dateRange.length === 2) {
      params.startTime = logFilter.dateRange[0]
      params.endTime = logFilter.dateRange[1]
    }

    const res: any = await systemLogApi.getLogs(params)
    if (res.data) {
      systemLogs.value = res.data.records || []
      logPagination.value.total = res.data.total || 0
    }
  } catch (error: any) {
    console.error('加载系统日志失败:', error)
  }
}

// 头像上传前校验
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
const handleAvatarSuccess = async (response: any) => {
  if (response.code === 200 || response.code === 1) {
    const avatarUrl = response.data.url || response.data
    accountForm.avatar = avatarUrl
    
    // 保存头像到数据库
    try {
      await adminAccountApi.updateInfo({
        avatar: avatarUrl
      })
      
      // 更新userStore中的头像，同步到右上角
      userStore.updateAvatar(avatarUrl)
      
      ElMessage.success('头像上传成功')
      loadAccountInfo()
    } catch (error: any) {
      ElMessage.error('保存失败: ' + (error?.message || '未知错误'))
    }
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 头像上传失败
const handleAvatarError = (error: any) => {
  console.error('上传失败:', error)
  ElMessage.error('上传失败，请重试')
}

// 保存账户信息
const saveAccountInfo = async () => {
  try {
    await adminAccountApi.updateInfo({
      username: accountForm.username,
      email: accountForm.email,
      phone: accountForm.phone,
      avatar: accountForm.avatar
    })
    ElMessage.success('账户信息保存成功')
    loadAccountInfo()
  } catch (error: any) {
    ElMessage.error('保存失败: ' + (error?.message || '未知错误'))
  }
}

// 退出全部设备
const logoutAllDevices = async () => {
  try {
    await authApi.logoutAll()
    ElMessage.success('已退出全部设备')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// 修改密码
const changePassword = async () => {
  if (!passwordForm.currentPassword) {
    ElMessage.warning('请输入当前密码')
    return
  }
  
  if (!passwordForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }

  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于6位')
    return
  }
  
  try {
    await adminAccountApi.changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    })
    
    ElMessage.success('密码修改成功')
    
    // 清空表单
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (error: any) {
    ElMessage.error('修改失败: ' + (error?.message || '未知错误'))
  }
}

// 加载系统配置
const loadSystemConfig = async () => {
  try {
    const res: any = await systemConfigApi.getConfig()
    if (res.data) {
      Object.assign(systemForm, {
        name: res.data.system_name || '智教黔行',
        description: res.data.system_description || '基于大数据的六盘水旅游景区客流量预测与智能管理平台',
        contactEmail: res.data.contact_email || 'support@liupanshui.gov.cn',
        supportPhone: res.data.support_phone || '0858-12345678',
        copyright: res.data.copyright || '© 2025 六盘水市旅游局'
      })
    }
  } catch (error: any) {
    console.error('加载系统配置失败:', error)
  }
}

// 保存系统设置
const saveSystemSettings = async () => {
  try {
    await systemConfigApi.saveConfig({
      system_name: systemForm.name,
      system_description: systemForm.description,
      contact_email: systemForm.contactEmail,
      support_phone: systemForm.supportPhone,
      copyright: systemForm.copyright
    })
    ElMessage.success('系统设置保存成功')
  } catch (error: any) {
    ElMessage.error('保存失败: ' + (error?.message || '未知错误'))
  }
}

// 保存备份设置
const saveBackupSettings = () => {
  ElMessage({
    type: 'success',
    message: '备份设置保存成功'
  })
}

// 创建手动备份
const createManualBackup = async () => {
  try {
    ElMessage.info('正在创建备份，请稍候...')
    
    await backupApi.create()
    
    // 重新加载备份列表
    await loadBackupList()
    
    ElMessage.success('备份创建成功')
  } catch (error: any) {
    ElMessage.error('备份创建失败: ' + (error?.message || '未知错误'))
  }
}

// 加载备份列表
const loadBackupList = async () => {
  try {
    const res = await backupApi.getList()
    if (res.data && Array.isArray(res.data)) {
      backupHistory.length = 0 // 清空数组
      res.data.forEach((item: any) => {
        backupHistory.push({
          filename: item.filename,
          date: item.date,
          size: item.size,
          type: item.type
        })
      })
    }
  } catch (error: any) {
    console.error('加载备份列表失败:', error)
    // 使用模拟数据，保持原有的备份历史
  }
}

// 下载备份
const downloadBackup = (backup: any) => {
  try {
    const downloadUrl = backupApi.download(backup.filename)
    
    // 创建隐藏的a标签来触发下载
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = backup.filename
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    ElMessage.success(`开始下载 ${backup.filename}`)
  } catch (error: any) {
    ElMessage.error('下载失败: ' + (error?.message || '未知错误'))
  }
}

// 删除备份
const deleteBackup = async (backup: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该备份吗？此操作不可恢复！', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await backupApi.delete(backup.filename || backup.date)
    
    // 从列表中移除
    const index = backupHistory.findIndex(item => item.date === backup.date)
    if (index !== -1) {
      backupHistory.splice(index, 1)
    }
    
    ElMessage.success('删除成功')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + (error?.message || '未知错误'))
    }
  }
}

// 导出日志（CSV，本地生成）
const exportLogs = async () => {
  try {
    const params: any = {}

    if (logFilter.level) {
      params.level = logFilter.level
    }

    if (logFilter.dateRange && logFilter.dateRange.length === 2) {
      params.startTime = logFilter.dateRange[0]
      params.endTime = logFilter.dateRange[1]
    }

    const res: any = await systemLogApi.exportLogs(params)
    if (res.data && res.data.logs) {
      const logs = res.data.logs
      const headers = ['time','level','module','message','user','ip']
      const csv = [
        headers.join(','),
        ...logs.map((r: any) => [
          r.logTime || '',
          r.logLevel || '',
          r.module || '',
          r.message || '',
          r.username || '',
          r.ipAddress || ''
        ].join(','))
      ].join('\n')
      
      const blob = new Blob(["\ufeff"+csv], { type: 'text/csv;charset=utf-8;' })
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = `系统日志_${new Date().toISOString().slice(0,10)}.csv`
      link.click()
      URL.revokeObjectURL(link.href)
      ElMessage.success('日志已导出')
    }
  } catch (error: any) {
    ElMessage.error('导出失败: ' + (error?.message || '未知错误'))
  }
}

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return '未知'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

// 页面初始化
onMounted(() => {
  loadAccountInfo()
  loadSystemConfig()
  loadSystemLogs()
  loadBackupList()
})
</script>

<style scoped>
.account-settings {
  color: #2C3E50;
}

:deep(.el-tabs) {
  background: transparent !important;
}

:deep(.el-tabs__header) {
  background: #F5F7FA !important;
  border-bottom: 1px solid rgba(42, 157, 143, 0.2) !important;
}

:deep(.el-tabs__item) {
  color: #606266 !important;
}

:deep(.el-tabs__item.is-active) {
  color: #2A9D8F !important;
}

:deep(.el-tabs__active-bar) {
  background-color: #2A9D8F !important;
}

:deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(0, 0, 0, 0.04) !important;
}

:deep(.el-tabs__content) {
  background: #F5F7FA !important;
}

:deep(.el-form-item__label) {
  color: #606266 !important;
}

:deep(.el-input__inner) {
  background-color: #FFFFFF !important;
  border: 1px solid #DCDFE6 !important;
  color: #303133 !important;
}

:deep(.el-textarea__inner) {
  background-color: #FFFFFF !important;
  border: 1px solid #DCDFE6 !important;
  color: #303133 !important;
}

:deep(.el-select__wrapper) {
  background-color: #FFFFFF !important;
  border: 1px solid #DCDFE6 !important;
}

:deep(.el-table) {
  background-color: transparent !important;
}

:deep(.el-table tr), :deep(.el-table th) {
  background-color: transparent !important;
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) {
  background-color: rgba(42, 157, 143, 0.1) !important;
}

:deep(.el-table td), :deep(.el-table th) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

:deep(.el-table .cell) {
  color: #303133;
}

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: #606266;
  --el-pagination-button-color: #606266;
  --el-pagination-button-bg-color: transparent;
  --el-pagination-button-disabled-color: #909399;
  --el-pagination-button-disabled-bg-color: transparent;
  --el-pagination-hover-color: var(--el-color-primary);
}

/* 头像上传样式 */
.avatar-uploader {
  display: inline-block;
}

.avatar-uploader :deep(.el-upload) {
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.3s;
}

.avatar-uploader :deep(.el-upload:hover) {
  opacity: 0.8;
}

.cursor-pointer {
  cursor: pointer;
}

.hover\:opacity-80:hover {
  opacity: 0.8;
}
</style> 
