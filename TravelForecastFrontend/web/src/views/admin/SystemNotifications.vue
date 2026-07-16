<template>
  <div class="system-notifications-container p-6">
    <holographic-card>
      <template #header>
        <div class="flex justify-between items-center p-4">
          <h2 class="text-xl font-bold text-[#2A9D8F]">系统通知管理</h2>
          <el-button type="primary" @click="openAddDialog">
            <el-icon class="mr-1"><Plus /></el-icon>
            发布通知
          </el-button>
        </div>
      </template>

      <!-- 通知列表 -->
      <div class="p-4">
        <el-table 
          :data="notificationList" 
          style="width: 100%"
          :header-cell-style="{ background: 'transparent', color: '#909399' }"
        >
          <el-table-column prop="id" label="ID" width="60" align="center" />
          
          <el-table-column label="类型" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="getTypeTagType(row.type)" size="small">
                {{ getTypeLabel(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="title" label="标题" width="180" show-overflow-tooltip />
          
          <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
          
          <el-table-column label="角色" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small">{{ getRoleLabel(row.targetRole) }}</el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="priority" label="优先级" width="80" align="center" />
          
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
                {{ row.status === 'active' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column label="创建时间" width="160" align="center">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="140" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openEditDialog(row)">
                编辑
              </el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </holographic-card>

    <!-- 添加/编辑通知弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑通知' : '发布通知'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="通知标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入通知标题" />
        </el-form-item>

        <el-form-item label="通知内容" prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="4"
            placeholder="请输入通知内容"
          />
        </el-form-item>

        <el-form-item label="通知类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择通知类型">
            <el-option label="信息" value="info" />
            <el-option label="警告" value="warning" />
            <el-option label="成功" value="success" />
            <el-option label="错误" value="error" />
          </el-select>
        </el-form-item>

        <el-form-item label="目标角色" prop="targetRole">
          <el-select v-model="formData.targetRole" placeholder="请选择目标角色">
            <el-option label="所有人" value="all" />
            <el-option label="商家" value="merchant" />
            <el-option label="用户" value="user" />
          </el-select>
        </el-form-item>

        <el-form-item label="优先级" prop="priority">
          <el-input-number
            v-model="formData.priority"
            :min="0"
            :max="100"
            placeholder="数字越大越靠前"
          />
        </el-form-item>

        <el-form-item label="状态" prop="status" v-if="isEdit">
          <el-radio-group v-model="formData.status">
            <el-radio value="active">启用</el-radio>
            <el-radio value="inactive">停用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="过期时间">
          <el-date-picker
            v-model="formData.expiresAt"
            type="datetime"
            placeholder="选择过期时间（可选）"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import HolographicCard from '@/components/HolographicCard.vue'
import {
  getAdminNotifications,
  createNotification,
  updateNotification,
  deleteNotification
} from '@/api/notification'

// 通知列表
const notificationList = ref<any[]>([])

// 弹窗控制
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

// 表单数据
const formData = ref({
  title: '',
  content: '',
  type: 'info',
  targetRole: 'all',
  priority: 0,
  status: 'active',
  expiresAt: ''
})

// 表单验证规则
const formRules: FormRules = {
  title: [{ required: true, message: '请输入通知标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入通知内容', trigger: 'blur' }],
  type: [{ required: true, message: '请选择通知类型', trigger: 'change' }],
  targetRole: [{ required: true, message: '请选择目标角色', trigger: 'change' }]
}

// 编辑中的通知ID
const editingId = ref<number | null>(null)

// 加载通知列表
const loadNotifications = async () => {
  try {
    const res = await getAdminNotifications()
    if (res.code === 200) {
      notificationList.value = res.data
    }
  } catch (error) {
    console.error('加载通知列表失败:', error)
    ElMessage.error('加载通知列表失败')
  }
}

// 打开添加弹窗
const openAddDialog = () => {
  isEdit.value = false
  editingId.value = null
  formData.value = {
    title: '',
    content: '',
    type: 'info',
    targetRole: 'all',
    priority: 0,
    status: 'active',
    expiresAt: ''
  }
  dialogVisible.value = true
}

// 打开编辑弹窗
const openEditDialog = (row: any) => {
  isEdit.value = true
  editingId.value = row.id
  formData.value = {
    title: row.title,
    content: row.content,
    type: row.type,
    targetRole: row.targetRole,
    priority: row.priority,
    status: row.status,
    expiresAt: row.expiresAt || ''
  }
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = () => {
  formRef.value?.validate(async (valid) => {
    if (valid) {
      try {
        const data = { ...formData.value }
        // 如果没有设置过期时间，传null
        if (!data.expiresAt) {
          data.expiresAt = ''
        }

        if (isEdit.value && editingId.value) {
          const res = await updateNotification(editingId.value, data)
          if (res.code === 200) {
            ElMessage.success('更新成功')
            dialogVisible.value = false
            loadNotifications()
          }
        } else {
          const res = await createNotification(data)
          if (res.code === 200) {
            ElMessage.success('发布成功')
            dialogVisible.value = false
            loadNotifications()
          }
        }
      } catch (error) {
        console.error('操作失败:', error)
        ElMessage.error('操作失败')
      }
    }
  })
}

// 删除通知
const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确认删除通知"${row.title}"？`, '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteNotification(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadNotifications()
      }
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 格式化日期时间
const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 获取类型标签样式
const getTypeTagType = (type: string) => {
  const map: any = {
    info: '',
    warning: 'warning',
    success: 'success',
    error: 'danger'
  }
  return map[type] || ''
}

// 获取类型标签文字
const getTypeLabel = (type: string) => {
  const map: any = {
    info: '信息',
    warning: '警告',
    success: '成功',
    error: '错误'
  }
  return map[type] || type
}

// 获取角色标签文字
const getRoleLabel = (role: string) => {
  const map: any = {
    all: '所有人',
    merchant: '商家',
    admin: '管理员',
    user: '用户'
  }
  return map[role] || role
}

onMounted(() => {
  loadNotifications()
})
</script>

<style scoped>
.system-notifications-container {
  min-height: 100vh;
  background: #F5F7FA;
}

.system-notifications-container :deep(.el-table) {
  background: #FFFFFF;
  color: #2C3E50;
}

.system-notifications-container :deep(.el-table__row) {
  background: #FFFFFF;
}

.system-notifications-container :deep(.el-table__row:hover) {
  background: #F5F7FA;
}

.system-notifications-container :deep(.el-table__cell) {
  border-color: #EBEEF5;
}

.system-notifications-container :deep(.el-dialog) {
  background: #FFFFFF;
  border: 1px solid #EBEEF5;
}

.system-notifications-container :deep(.el-dialog__header) {
  border-bottom: 1px solid #EBEEF5;
  padding-bottom: 16px;
}

.system-notifications-container :deep(.el-dialog__title) {
  color: #2A9D8F;
}

.system-notifications-container :deep(.el-form-item__label) {
  color: #94a3b8;
}

.system-notifications-container :deep(.el-input__wrapper) {
  background: #F5F7FA;
  border-color: rgba(42, 157, 143, 0.3);
}

.system-notifications-container :deep(.el-input__wrapper:hover),
.system-notifications-container :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(42, 157, 143, 0.6);
}

.system-notifications-container :deep(.el-textarea__inner) {
  background: #F5F7FA;
  border-color: rgba(42, 157, 143, 0.3);
  color: #2C3E50;
}

.system-notifications-container :deep(.el-textarea__inner:hover),
.system-notifications-container :deep(.el-textarea__inner:focus) {
  border-color: rgba(42, 157, 143, 0.6);
}

.system-notifications-container :deep(.el-select .el-input__wrapper) {
  background: #F5F7FA;
}

.system-notifications-container :deep(.el-input-number) {
  background: #F5F7FA;
}

.system-notifications-container :deep(.el-input-number .el-input__wrapper) {
  background: transparent;
}
</style>

