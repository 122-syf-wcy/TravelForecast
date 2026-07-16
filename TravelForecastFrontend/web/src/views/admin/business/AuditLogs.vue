<template>
  <div class="audit-logs text-gray-800">
    <div class="page-header mb-6">
      <h2 class="text-2xl font-bold text-gray-800">审核日志</h2>
      <p class="text-gray-500 mt-1">系统操作与审核记录追踪</p>
    </div>

    <holographic-card glow-color="tech-purple">
      <template #header>
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg font-bold text-[#457B9D]">操作日志</h3>
          <div class="flex items-center gap-3">
            <el-input 
              v-model.number="query.userId" 
              placeholder="按商户ID筛选" 
              style="width: 220px" 
              clearable 
              class="glass-input"
              @keyup.enter="fetchData"
            >
              <template #prepend>ID</template>
            </el-input>
            <el-button type="primary" @click="fetchData" class="glow-button">查询</el-button>
            <el-button @click="reset" class="glass-button">重置</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" style="width: 100%" class="bg-transparent" border v-loading="loading" element-loading-background="rgba(0, 0, 0, 0.04)">
        <el-table-column prop="id" label="ID" width="80" align="center">
          <template #default="{ row }">
            <span class="text-gray-500 font-mono">{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="商户ID" width="100" align="center">
          <template #default="{ row }">
            <span class="font-mono text-[#2A9D8F]">{{ row.userId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="管理员" min-width="150">
          <template #default="{ row }">
            <div v-if="row.adminUserId" class="flex flex-col">
              <span class="text-[#457B9D]">{{ row.adminNickname || row.adminUsername || '管理员' }}</span>
              <span class="text-xs text-gray-500 font-mono">ID: {{ row.adminUserId }}</span>
            </div>
            <span v-else class="text-gray-400">系统管理员</span>
          </template>
        </el-table-column>
        <el-table-column label="动作" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getActionType(row.action)" size="small" effect="dark">
              {{ row.action }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态变更" width="200" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2">
              <el-tag type="info" size="small">{{ row.fromStatus || '无' }}</el-tag>
              <el-icon class="text-gray-500"><Right /></el-icon>
              <el-tag :type="getStatusType(row.toStatus)" size="small">{{ row.toStatus || '无' }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-gray-500">{{ row.remark || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center text-gray-500 text-sm">
              <el-icon class="mr-1"><Clock /></el-icon>
              <span>{{ row.createdAt || '-' }}</span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </holographic-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Clock, Right } from '@element-plus/icons-vue'
import HolographicCard from '@/components/HolographicCard.vue'
import { pageAuditLogs } from '@/api/adminMerchant'

const query = ref<{ userId?: number }>({})
const list = ref<any[]>([])
const pagination = ref({ page: 1, pageSize: 10, total: 0 })
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await pageAuditLogs({ page: pagination.value.page, size: pagination.value.pageSize, userId: query.value.userId })
    const d: any = res.data || {}
    list.value = d.records || []
    pagination.value.total = d.total || 0
  } catch (error) {
    console.error('加载审核日志失败:', error)
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.value = {}
  pagination.value.page = 1
  fetchData()
}

const getActionType = (action: string) => {
  if (action === 'approve') return 'success'
  if (action === 'reject') return 'danger'
  return 'info'
}

const getStatusType = (status: string) => {
  if (status === 'approved') return 'success'
  if (status === 'rejected') return 'danger'
  if (status === 'pending') return 'warning'
  return 'info'
}

onMounted(fetchData)
watch(() => query.value.userId, () => { pagination.value.page = 1 })
</script>

<style scoped>
.audit-logs {
  padding: 20px;
}

/* Deep overrides for Element Plus */
:deep(.el-table) {
  background-color: transparent !important;
  --el-table-header-bg-color: #F5F7FA;
  --el-table-tr-bg-color: transparent;
  --el-table-border-color: rgba(0, 0, 0, 0.04);
  color: #2C3E50;
}

:deep(.el-table tr), :deep(.el-table th) {
  background-color: transparent !important;
}

:deep(.el-table td), :deep(.el-table th) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.04) !important;
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) {
  background-color: rgba(69, 123, 157, 0.1) !important; /* Purple hover */
}

:deep(.el-input__inner) {
  background-color: #FFFFFF !important;
  border: 1px solid #EBEEF5 !important;
  color: #303133 !important;
  box-shadow: none !important;
}

:deep(.el-input__inner:focus) {
  border-color: #457B9D !important;
}

:deep(.el-input-group__prepend) {
  background-color: rgba(69, 123, 157, 0.2) !important;
  border: 1px solid #EBEEF5 !important;
  color: #909399 !important;
  border-right: 0 !important;
}

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: #606266;
  --el-pagination-button-color: #606266;
  --el-pagination-button-bg-color: rgba(0, 0, 0, 0.04);
  --el-pagination-button-disabled-color: rgba(0, 0, 0, 0.08);
  --el-pagination-button-disabled-bg-color: transparent;
  --el-pagination-hover-color: #457B9D;
}

.glass-button {
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.06);
  color: #2C3E50;
}

.glass-button:hover {
  background: rgba(0, 0, 0, 0.06);
  border-color: #2C3E50;
  color: #2C3E50;
}

.glow-button {
  background: #457B9D;
  border: 1px solid #457B9D;
  color: #FFFFFF;
  box-shadow: 0 2px 8px rgba(69, 123, 157, 0.3);
}

.glow-button:hover {
  background: #3A6A8A;
  border-color: #3A6A8A;
  color: #FFFFFF;
  box-shadow: 0 4px 12px rgba(69, 123, 157, 0.4);
}
</style>
