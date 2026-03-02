<template>
  <div class="emergency-rescue-container">
    <holographic-card class="mb-6">
      <template #header>
        <div class="flex justify-between items-center">
          <h2 class="text-2xl font-bold text-[#2A9D8F]">紧急救援管理</h2>
          <el-button type="primary" @click="loadRescues">
            <el-icon class="mr-1"><Refresh /></el-icon>
            刷新数据
          </el-button>
        </div>
      </template>

      <!-- 统计卡片 -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div class="stat-card urgent">
          <div class="text-gray-500 text-sm mb-1">紧急待处理</div>
          <div class="text-2xl font-bold text-red-500">{{ urgentPendingCount }}</div>
          <div class="text-xs text-red-400 mt-1">需立即响应</div>
        </div>
        <div class="stat-card">
          <div class="text-gray-500 text-sm mb-1">待处理</div>
          <div class="text-2xl font-bold text-yellow-400">{{ statistics.pendingCount || 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="text-gray-500 text-sm mb-1">处理中</div>
          <div class="text-2xl font-bold text-blue-400">{{ statistics.processingCount || 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="text-gray-500 text-sm mb-1">已完成</div>
          <div class="text-2xl font-bold text-green-400">{{ statistics.completedCount || 0 }}</div>
        </div>
      </div>

      <!-- 筛选表单 -->
      <el-form :inline="true" :model="queryForm" class="mb-4">
        <el-form-item label="救援状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable class="w-40">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="救援类型">
          <el-select v-model="queryForm.rescueType" placeholder="全部类型" clearable class="w-40">
            <el-option label="医疗救助" value="MEDICAL" />
            <el-option label="走失寻人" value="LOST" />
            <el-option label="事故求助" value="ACCIDENT" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="紧急程度">
          <el-select v-model="queryForm.emergencyLevel" placeholder="全部" clearable class="w-40">
            <el-option label="紧急" value="URGENT" />
            <el-option label="一般" value="NORMAL" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter">筛选</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 救援列表 -->
      <el-table
        :data="filteredRescueList"
        v-loading="loading"
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#F5F7FA', color: '#2A9D8F' }"
        :row-class-name="tableRowClassName"
      >
        <el-table-column label="紧急程度" width="85" align="center">
          <template #default="{ row }">
            <el-tag :type="row.emergencyLevel === 'URGENT' ? 'danger' : 'warning'" size="small">
              {{ row.emergencyLevel === 'URGENT' ? '紧急' : '一般' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="救援类型" width="95">
          <template #default="{ row }">
            <el-tag :type="getRescueTypeColor(row.rescueType)" size="small">
              {{ getRescueTypeText(row.rescueType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contactName" label="联系人" width="90" />
        <el-table-column prop="contactPhone" label="联系电话" width="120" />
        <el-table-column prop="location" label="位置" min-width="120" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
        <el-table-column prop="scenicName" label="景区" width="110" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusColor(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="100">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; flex-direction: column; gap: 6px; padding: 6px 0;">
              <div style="display: flex; gap: 6px; justify-content: space-between;">
                <el-button
                  v-if="row.status === 'PENDING'"
                  type="primary"
                  size="small"
                  @click="handleRescue(row)"
                  style="width: 110px;"
                >
                  接受处理
                </el-button>
                <el-button
                  v-if="row.status === 'PROCESSING'"
                  type="success"
                  size="small"
                  @click="completeRescue(row)"
                  style="width: 110px;"
                >
                  完成
                </el-button>
              </div>
              <div style="display: flex; gap: 6px; justify-content: space-between;">
                <el-button
                  size="small"
                  @click="viewDetail(row)"
                  style="width: 110px;"
                >
                  详情
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  @click="deleteRescue(row)"
                  style="width: 110px;"
                >
                  删除
                </el-button>
              </div>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </holographic-card>

    <!-- 救援详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="救援详情"
      width="700px"
      destroy-on-close
    >
      <div v-if="currentRescue" class="rescue-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="紧急程度">
            <el-tag :type="currentRescue.emergencyLevel === 'URGENT' ? 'danger' : 'warning'">
              {{ currentRescue.emergencyLevel === 'URGENT' ? '紧急' : '一般' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="救援类型">
            <el-tag :type="getRescueTypeColor(currentRescue.rescueType)">
              {{ getRescueTypeText(currentRescue.rescueType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="联系人">{{ currentRescue.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentRescue.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="所在景区" :span="2">{{ currentRescue.scenicName }}</el-descriptions-item>
          <el-descriptions-item label="具体位置" :span="2">{{ currentRescue.location }}</el-descriptions-item>
          <el-descriptions-item label="详细描述" :span="2">{{ currentRescue.description }}</el-descriptions-item>
          <el-descriptions-item label="提交时间" :span="2">{{ formatDateTime(currentRescue.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getStatusColor(currentRescue.status)">
              {{ getStatusText(currentRescue.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="处理人" v-if="currentRescue.handlerName">
            {{ currentRescue.handlerName }}
          </el-descriptions-item>
          <el-descriptions-item label="处理时间" :span="2" v-if="currentRescue.handleTime">
            {{ formatDateTime(currentRescue.handleTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="完成时间" :span="2" v-if="currentRescue.completeTime">
            {{ formatDateTime(currentRescue.completeTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="处理备注" :span="2" v-if="currentRescue.handleNotes">
            {{ currentRescue.handleNotes }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 完成救援对话框 -->
    <el-dialog
      v-model="completeDialogVisible"
      title="完成救援"
      width="500px"
      destroy-on-close
    >
      <el-form :model="completeForm" label-width="100px">
        <el-form-item label="处理备注">
          <el-input
            v-model="completeForm.handleNotes"
            type="textarea"
            :rows="5"
            placeholder="请输入救援处理的详细情况和结果..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="success" @click="submitComplete" :loading="submitting">完成救援</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import HolographicCard from '@/components/HolographicCard.vue'
import { getMerchantRescues, handleRescue as handleRescueApi, completeRescue as completeRescueApi, deleteRescue as deleteRescueApi } from '@/api/emergencyRescue'

interface RescueRecord {
  id: number
  rescueType: string
  status: string
  contactName: string
  contactPhone: string
  location: string
  description: string
  emergencyLevel: string
  scenicName: string
  userName?: string
  userAvatar?: string
  handlerName?: string
  handleTime?: string
  completeTime?: string
  handleNotes?: string
  createdAt: string
  updatedAt?: string
}

const loading = ref(false)
const rescueList = ref<RescueRecord[]>([])
const statistics = ref({
  totalCount: 0,
  pendingCount: 0,
  processingCount: 0,
  completedCount: 0,
  urgentCount: 0
})

const queryForm = ref({
  status: '',
  rescueType: '',
  emergencyLevel: ''
})

const detailDialogVisible = ref(false)
const completeDialogVisible = ref(false)
const currentRescue = ref<RescueRecord | null>(null)
const completeForm = ref({
  handleNotes: ''
})
const submitting = ref(false)

// 计算紧急待处理数量
const urgentPendingCount = computed(() => {
  return rescueList.value.filter(r => r.status === 'PENDING' && r.emergencyLevel === 'URGENT').length
})

// 过滤后的救援列表
const filteredRescueList = computed(() => {
  let list = rescueList.value
  
  if (queryForm.value.status) {
    list = list.filter(r => r.status === queryForm.value.status)
  }
  
  if (queryForm.value.rescueType) {
    list = list.filter(r => r.rescueType === queryForm.value.rescueType)
  }
  
  if (queryForm.value.emergencyLevel) {
    list = list.filter(r => r.emergencyLevel === queryForm.value.emergencyLevel)
  }
  
  // 按紧急程度和时间排序：紧急的在前，时间新的在前
  return list.sort((a, b) => {
    if (a.emergencyLevel !== b.emergencyLevel) {
      return a.emergencyLevel === 'URGENT' ? -1 : 1
    }
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  })
})

// 加载救援列表
const loadRescues = async () => {
  loading.value = true
  try {
    const response = await getMerchantRescues()
    console.log('API响应:', response)
    console.log('救援数据:', response.data)
    
    rescueList.value = response.data || []
    
    // 打印第一条数据用于调试
    if (rescueList.value.length > 0) {
      console.log('第一条救援记录:', rescueList.value[0])
      console.log('字段检查:', {
        emergencyLevel: rescueList.value[0].emergencyLevel,
        rescueType: rescueList.value[0].rescueType,
        contactName: rescueList.value[0].contactName,
        contactPhone: rescueList.value[0].contactPhone,
        status: rescueList.value[0].status
      })
    }
    
    // 计算统计数据
    statistics.value = {
      totalCount: rescueList.value.length,
      pendingCount: rescueList.value.filter(r => r.status === 'PENDING').length,
      processingCount: rescueList.value.filter(r => r.status === 'PROCESSING').length,
      completedCount: rescueList.value.filter(r => r.status === 'COMPLETED').length,
      urgentCount: rescueList.value.filter(r => r.emergencyLevel === 'URGENT').length
    }
    
    ElMessage.success('数据加载成功')
  } catch (error: any) {
    console.error('加载救援列表失败:', error)
    ElMessage.error(error?.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 处理救援
const handleRescue = async (row: RescueRecord) => {
  try {
    await ElMessageBox.confirm(
      `确认接受处理此救援请求吗？`,
      '确认接受',
      {
        confirmButtonText: '确认接受',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    loading.value = true
    const handlerName = '景区工作人员' // 可以从用户信息中获取
    await handleRescueApi(row.id, handlerName)
    
    ElMessage.success('已接受处理，请尽快前往现场')
    await loadRescues()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('接受处理失败:', error)
      ElMessage.error(error?.response?.data?.message || '操作失败')
    }
  } finally {
    loading.value = false
  }
}

// 完成救援
const completeRescue = (row: RescueRecord) => {
  currentRescue.value = row
  completeForm.value.handleNotes = ''
  completeDialogVisible.value = true
}

// 提交完成
const submitComplete = async () => {
  if (!completeForm.value.handleNotes.trim()) {
    ElMessage.warning('请填写处理备注')
    return
  }
  
  submitting.value = true
  try {
    await completeRescueApi(currentRescue.value!.id, completeForm.value.handleNotes)
    ElMessage.success('救援已完成')
    completeDialogVisible.value = false
    await loadRescues()
  } catch (error: any) {
    console.error('完成救援失败:', error)
    ElMessage.error(error?.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

// 查看详情
const viewDetail = (row: RescueRecord) => {
  currentRescue.value = row
  detailDialogVisible.value = true
}

// 删除救援
const deleteRescue = async (row: RescueRecord) => {
  try {
    await ElMessageBox.confirm(
      '确认删除此救援记录吗？删除后无法恢复。',
      '确认删除',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    loading.value = true
    await deleteRescueApi(row.id)
    
    ElMessage.success('删除成功')
    await loadRescues()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(error?.response?.data?.message || '删除失败')
    }
  } finally {
    loading.value = false
  }
}

// 筛选
const handleFilter = () => {
  // 筛选逻辑已在 computed 中处理
}

// 重置
const handleReset = () => {
  queryForm.value = {
    status: '',
    rescueType: '',
    emergencyLevel: ''
  }
}

// 表格行类名
const tableRowClassName = ({ row }: { row: RescueRecord }) => {
  if (row.status === 'PENDING' && row.emergencyLevel === 'URGENT') {
    return 'urgent-row'
  }
  return ''
}

// 获取救援类型文本
const getRescueTypeText = (type: string) => {
  const map: Record<string, string> = {
    MEDICAL: '医疗救助',
    LOST: '走失寻人',
    ACCIDENT: '事故求助',
    OTHER: '其他'
  }
  return map[type] || type
}

// 获取救援类型颜色
const getRescueTypeColor = (type: string) => {
  const map: Record<string, string> = {
    MEDICAL: 'danger',
    LOST: 'warning',
    ACCIDENT: 'danger',
    OTHER: 'info'
  }
  return map[type] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '待处理',
    PROCESSING: '处理中',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

// 获取状态颜色
const getStatusColor = (status: string) => {
  const map: Record<string, string> = {
    PENDING: 'warning',
    PROCESSING: 'primary',
    COMPLETED: 'success',
    CANCELLED: 'info'
  }
  return map[status] || 'info'
}

// 格式化时间
const formatTime = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 格式化详细时间
const formatDateTime = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`
}

onMounted(() => {
  loadRescues()
})
</script>

<style scoped>
.emergency-rescue-container {
  padding: 20px;
}

.stat-card {
  background: #FFFFFF;
  border: 1px solid #EBEEF5;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.stat-card.urgent {
  border-color: rgba(255, 77, 79, 0.4);
  background: #FFF5F5;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(42, 157, 143, 0.15);
}

:deep(.urgent-row) {
  background-color: rgba(255, 77, 79, 0.06) !important;
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

.rescue-detail {
  padding: 10px;
}

:deep(.el-descriptions__label) {
  font-weight: 600;
  color: #2A9D8F !important;
}

/* 表格浅色主题样式 */
:deep(.el-table) {
  background-color: #FFFFFF !important;
  color: #606266 !important;
}

:deep(.el-table tr) {
  background-color: #FFFFFF !important;
}

:deep(.el-table th.el-table__cell) {
  background-color: #F5F7FA !important;
  color: #2A9D8F !important;
  border-color: #EBEEF5 !important;
}

:deep(.el-table td.el-table__cell) {
  background-color: transparent !important;
  border-color: #EBEEF5 !important;
  color: #606266 !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background-color: #FAFAFA !important;
}

:deep(.el-table__body tr:hover > td) {
  background-color: #F5F7FA !important;
}

:deep(.el-table__empty-block) {
  background-color: transparent !important;
}

:deep(.el-table__empty-text) {
  color: #909399 !important;
}
</style>

