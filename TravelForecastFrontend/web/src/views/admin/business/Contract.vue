<template>
  <div class="contract-management text-gray-800">
    <div class="page-header mb-6">
      <h2 class="text-2xl font-bold text-gray-800">合同管理</h2>
      <p class="text-gray-500 mt-1">商户合同签署与状态管理</p>
    </div>

    <holographic-card glow-color="neon-cyan">
      <template #header>
        <div class="flex items-center justify-between card-header">
          <h3 class="text-lg font-bold text-[#2A9D8F]">合同列表</h3>
          <div class="flex gap-2">
            <el-input
              v-model="keyword"
              placeholder="搜索合同编号或名称"
              clearable
              style="width: 250px"
              class="glass-input"
              @clear="loadContracts"
              @keyup.enter="loadContracts"
            >
              <template #append>
                <el-button :icon="Search" @click="loadContracts" class="glass-button-append" />
              </template>
            </el-input>
            <el-select
              v-model="statusFilter"
              placeholder="筛选状态"
              clearable
              style="width: 150px"
              class="glass-select"
              @change="loadContracts"
            >
              <el-option label="有效" value="active" />
              <el-option label="已过期" value="expired" />
              <el-option label="已取消" value="cancelled" />
            </el-select>
          </div>
        </div>
      </template>

      <el-table :data="contracts" v-loading="loading" class="bg-transparent" element-loading-background="rgba(0, 0, 0, 0.04)">
        <el-table-column prop="contractNo" label="合同编号" min-width="120">
          <template #default="{ row }">
            <span class="text-[#2A9D8F] font-mono">{{ row.contractNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contractName" label="合同名称" min-width="150" />
        <el-table-column prop="userId" label="商户ID" width="100">
          <template #default="{ row }">
            <span class="text-gray-500 font-mono">{{ row.userId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="110">
          <template #default="{ row }">
            <span class="text-gray-500">{{ row.startDate }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="endDate" label="结束日期" width="110">
          <template #default="{ row }">
            <span class="text-gray-500">{{ row.endDate }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="dark" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            <span class="text-xs text-gray-500">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="viewContract(row)">
              查看
            </el-button>
            <el-button size="small" type="warning" link @click="editContract(row)">
              编辑
            </el-button>
            <el-dropdown @command="(cmd: string) => handleStatusChange(row, cmd)">
              <el-button size="small" type="info" link>
                状态 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu class="glass-dropdown">
                  <el-dropdown-item command="active">设为有效</el-dropdown-item>
                  <el-dropdown-item command="expired">设为已过期</el-dropdown-item>
                  <el-dropdown-item command="cancelled">设为已取消</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button size="small" type="danger" link @click="deleteContract(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadContracts"
          @current-change="loadContracts"
        />
      </div>
    </holographic-card>

    <!-- 合同详情/编辑对话框 -->
    <el-dialog
      v-model="showDialog"
      :title="isViewing ? '查看合同' : '编辑合同'"
      width="700px"
      custom-class="glass-dialog"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="currentContract"
        :rules="rules"
        label-width="100px"
        :disabled="isViewing"
        class="text-gray-800"
      >
        <el-form-item label="合同编号" prop="contractNo">
          <el-input v-model="currentContract.contractNo" />
        </el-form-item>
        <el-form-item label="合同名称" prop="contractName">
          <el-input v-model="currentContract.contractName" />
        </el-form-item>
        <el-form-item label="商户ID">
          <el-input v-model="currentContract.userId" disabled />
        </el-form-item>
        <el-form-item label="合同文件" prop="contractUrl">
          <div class="contract-file-section w-full">
            <el-link
              v-if="currentContract.contractUrl"
              :href="currentContract.contractUrl"
              target="_blank"
              type="primary"
              :icon="Document"
              class="text-[#2A9D8F] hover:text-[#2A9D8F]"
            >
              点击查看/下载合同文件
            </el-link>
            <span v-else class="text-gray-500">暂无文件</span>
            <el-input
              v-if="!isViewing"
              v-model="currentContract.contractUrl"
              placeholder="合同文件URL"
              class="mt-2"
            />
          </div>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="currentContract.startDate"
            type="date"
            placeholder="选择开始日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker
            v-model="currentContract.endDate"
            type="date"
            placeholder="选择结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="currentContract.status" placeholder="请选择状态" class="w-full">
            <el-option label="有效" value="active" />
            <el-option label="已过期" value="expired" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="currentContract.remarks"
            type="textarea"
            :rows="4"
            placeholder="请输入备注"
          />
        </el-form-item>
        <div class="grid grid-cols-2 gap-4">
          <el-form-item label="创建时间">
            <el-input v-model="currentContract.createdAt" disabled />
          </el-form-item>
          <el-form-item label="更新时间">
            <el-input v-model="currentContract.updatedAt" disabled />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showDialog = false" class="glass-button">取消</el-button>
          <el-button v-if="!isViewing" type="primary" :loading="saving" @click="saveContract" class="glow-button">
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, ArrowDown, Document } from '@element-plus/icons-vue'
import request from '@/utils/request'
import HolographicCard from '@/components/HolographicCard.vue'

const loading = ref(false)
const contracts = ref<any[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref('')

const showDialog = ref(false)
const isViewing = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const currentContract = ref<any>({
  contractNo: '',
  contractName: '',
  userId: null,
  contractUrl: '',
  startDate: '',
  endDate: '',
  status: 'active',
  remarks: ''
})

const rules: FormRules = {
  contractNo: [{ required: true, message: '请输入合同编号', trigger: 'blur' }],
  contractName: [{ required: true, message: '请输入合同名称', trigger: 'blur' }],
  contractUrl: [{ required: true, message: '请输入合同文件URL', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 加载合同列表
const loadContracts = async () => {
  loading.value = true
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value) params.status = statusFilter.value

    const res: any = await request({
      url: '/admin/merchant/contracts',
      method: 'get',
      params
    })

    const data = res?.data || res
    contracts.value = data.records || []
    total.value = data.total || 0
  } catch (error: any) {
    ElMessage.error('加载失败: ' + (error?.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 查看合同
const viewContract = async (contract: any) => {
  isViewing.value = true
  try {
    const res: any = await request({
      url: `/admin/merchant/contracts/${contract.id}`,
      method: 'get'
    })
    currentContract.value = res?.data || contract
    showDialog.value = true
  } catch (error: any) {
    ElMessage.error('加载失败: ' + (error?.message || '未知错误'))
  }
}

// 编辑合同
const editContract = async (contract: any) => {
  isViewing.value = false
  try {
    const res: any = await request({
      url: `/admin/merchant/contracts/${contract.id}`,
      method: 'get'
    })
    currentContract.value = res?.data || contract
    showDialog.value = true
  } catch (error: any) {
    ElMessage.error('加载失败: ' + (error?.message || '未知错误'))
  }
}

// 保存合同
const saveContract = async () => {
  if (!formRef.value) return

  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    saving.value = true

    await request({
      url: `/admin/merchant/contracts/${currentContract.value.id}`,
      method: 'put',
      data: currentContract.value
    })

    ElMessage.success('更新成功')
    showDialog.value = false
    loadContracts()
  } catch (error: any) {
    ElMessage.error('保存失败: ' + (error?.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

// 修改合同状态
const handleStatusChange = async (contract: any, status: string) => {
  try {
    await request({
      url: `/admin/merchant/contracts/${contract.id}/status`,
      method: 'put',
      data: { status }
    })

    ElMessage.success('状态更新成功')
    loadContracts()
  } catch (error: any) {
    ElMessage.error('状态更新失败: ' + (error?.message || '未知错误'))
  }
}

// 删除合同
const deleteContract = async (contract: any) => {
  try {
    await ElMessageBox.confirm(
      `确认删除合同"${contract.contractName}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await request({
      url: `/admin/merchant/contracts/${contract.id}`,
      method: 'delete'
    })

    ElMessage.success('删除成功')
    loadContracts()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + (error?.message || '未知错误'))
    }
  }
}

// 获取状态标签类型
const getStatusType = (status: string) => {
  const map: Record<string, any> = {
    active: 'success',
    expired: 'info',
    cancelled: 'danger'
  }
  return map[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    active: '有效',
    expired: '已过期',
    cancelled: '已取消'
  }
  return map[status] || status
}

onMounted(() => {
  loadContracts()
})
</script>

<style scoped>
.contract-management {
  padding: 20px;
}

.contract-file-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* Deep overrides for consistent styling */
:deep(.el-table) {
  background-color: #FFFFFF !important;
  --el-table-header-bg-color: #F5F7FA;
  --el-table-tr-bg-color: #FFFFFF;
  --el-table-border-color: #EBEEF5;
  color: #2C3E50;
}

:deep(.el-table tr), :deep(.el-table th) {
  background-color: transparent !important;
}

:deep(.el-table td), :deep(.el-table th) {
  border-bottom: 1px solid #EBEEF5 !important;
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) {
  background-color: rgba(42, 157, 143, 0.1) !important;
}

:deep(.el-form-item__label) {
  color: #606266 !important;
}

:deep(.el-input__inner), :deep(.el-textarea__inner) {
  background-color: #FFFFFF !important;
  border: 1px solid #DCDFE6 !important;
  color: #303133 !important;
  box-shadow: none !important;
}

:deep(.el-input__inner:focus), :deep(.el-textarea__inner:focus) {
  border-color: #2A9D8F !important;
}

:deep(.el-select__wrapper) {
  background-color: #FFFFFF !important;
  border: 1px solid #DCDFE6 !important;
  box-shadow: none !important;
}

:deep(.el-select__wrapper .el-select__selected-item) {
  color: #303133 !important;
}

:deep(.el-input-group__append) {
  background-color: rgba(42, 157, 143, 0.2) !important;
  border: 1px solid #DCDFE6 !important;
  color: #303133 !important;
  border-left: 0 !important;
}

:deep(.el-dialog) {
  background: #FFFFFF;
  backdrop-filter: blur(10px);
  border: 1px solid #EBEEF5;
  border-radius: 12px;
}

:deep(.el-dialog__title) {
  color: #2A9D8F;
  }

:deep(.el-dialog__body) {
  color: #2C3E50;
}

:deep(.el-pagination) {
  --el-pagination-bg-color: #FFFFFF;
  --el-pagination-text-color: #606266;
  --el-pagination-button-color: #606266;
  --el-pagination-button-bg-color: #F5F7FA;
  --el-pagination-button-disabled-color: #C0C4CC;
  --el-pagination-button-disabled-bg-color: #FFFFFF;
  --el-pagination-hover-color: #2A9D8F;
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
  background: #2A9D8F;
  border: 1px solid #2A9D8F;
  color: #FFFFFF;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.glow-button:hover {
  background: #238B7E;
  border-color: #238B7E;
  color: #FFFFFF;
  box-shadow: 0 4px 12px rgba(42, 157, 143, 0.4);
}
</style>
