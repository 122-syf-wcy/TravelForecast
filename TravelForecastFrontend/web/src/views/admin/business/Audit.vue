<template>
  <div class="audit-container">
    <div class="page-header mb-6">
      <h2 class="text-2xl font-bold text-gray-800">商户审核管理</h2>
      <p class="text-gray-500 mt-1">商户入驻申请审核与管理</p>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
      <holographic-card class="stat-card" glow-color="energy-orange">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-gray-500 text-sm mb-1">待审核</p>
            <p class="text-3xl font-bold text-orange-400">{{ stats.pending }}</p>
          </div>
          <i class="ri-time-line text-4xl text-orange-400 opacity-30"></i>
        </div>
      </holographic-card>
      
      <holographic-card class="stat-card" glow-color="neon-cyan">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-gray-500 text-sm mb-1">已通过</p>
            <p class="text-3xl font-bold text-green-400">{{ stats.approved }}</p>
          </div>
          <i class="ri-checkbox-circle-line text-4xl text-green-400 opacity-30"></i>
        </div>
      </holographic-card>
      
      <holographic-card class="stat-card" glow-color="tech-purple">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-gray-500 text-sm mb-1">已拒绝</p>
            <p class="text-3xl font-bold text-red-400">{{ stats.rejected }}</p>
          </div>
          <i class="ri-close-circle-line text-4xl text-red-400 opacity-30"></i>
        </div>
      </holographic-card>
      
      <holographic-card class="stat-card" glow-color="neon-cyan">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-gray-500 text-sm mb-1">今日新增</p>
            <p class="text-3xl font-bold text-[#2A9D8F]">{{ stats.today }}</p>
          </div>
          <i class="ri-add-circle-line text-4xl text-[#2A9D8F] opacity-30"></i>
        </div>
      </holographic-card>
    </div>

    <!-- 筛选区域 -->
    <holographic-card class="mb-6">
      <template #header>
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg text-[#2A9D8F] font-bold">审核筛选</h3>
        </div>
      </template>
      <el-form :inline="true" class="text-gray-800">
        <el-form-item label="审核状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" style="width: 150px">
            <el-option label="全部状态" value="" />
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已拒绝" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item label="商户类型">
          <el-select v-model="searchForm.type" placeholder="全部类型" style="width: 150px">
            <el-option label="全部类型" value="" />
            <el-option label="景区" value="scenic" />
            <el-option label="酒店" value="hotel" />
            <el-option label="餐饮" value="restaurant" />
          </el-select>
        </el-form-item>
        <el-form-item label="商户名称">
          <el-input v-model="searchForm.name" placeholder="输入商户名称" style="width: 200px" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <i class="ri-search-line mr-1"></i>查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </holographic-card>

    <!-- 审核列表 -->
    <holographic-card>
      <template #header>
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg text-[#2A9D8F] font-bold">审核列表</h3>
        </div>
      </template>
      <el-table :data="auditList" style="width: 100%" class="bg-transparent">
        <el-table-column type="index" label="序号" width="60" />
        
        <el-table-column prop="businessName" label="商户名称" width="200">
          <template #default="{ row }">
            <div class="flex items-center">
              <el-avatar :size="40" :src="row.logo" class="mr-2" />
              <span class="text-gray-800 font-bold">{{ row.businessName }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeColor(row.type)" size="small" effect="dark">
              {{ getTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="contact" label="联系人" width="120">
          <template #default="{ row }">
            <span class="text-gray-500">{{ row.contact }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="phone" label="联系电话" width="130">
          <template #default="{ row }">
            <span class="text-gray-500 font-mono">{{ row.phone }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="applyTime" label="申请时间" width="180">
          <template #default="{ row }">
            <span class="text-gray-500 text-xs">{{ row.applyTime }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag 
              :type="row.status === 'pending' ? 'warning' : row.status === 'approved' ? 'success' : 'danger'"
              size="small"
              effect="dark"
            >
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="flex space-x-2">
              <el-button type="primary" size="small" link @click="viewDetail(row)">
                查看
              </el-button>
              <el-button 
                v-if="row.status === 'pending'" 
                type="success" 
                size="small" 
                link 
                @click="handleApprove(row)"
              >
                通过
              </el-button>
              <el-button 
                v-if="row.status === 'pending'" 
                type="danger" 
                size="small" 
                link 
                @click="handleReject(row)"
              >
                拒绝
              </el-button>
              <el-button type="info" size="small" link @click="viewLogs(row)">
                日志
              </el-button>
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
          @size-change="handleSearch"
          @current-change="handleSearch"
        />
      </div>
    </holographic-card>

    <!-- 详情对话框 -->
    <el-dialog 
      v-model="detailVisible" 
      title="商户审核详情" 
      width="800px"
      :close-on-click-modal="false"
      custom-class="glass-dialog"
    >
      <div v-if="currentAudit" class="detail-content">
        <div class="grid grid-cols-2 gap-6">
          <div>
            <h3 class="text-lg font-bold text-[#2A9D8F] mb-4 border-b border-gray-200 pb-2">基本信息</h3>
            <div class="space-y-3">
              <div class="info-item">
                <span class="label">商户名称：</span>
                <span class="value">{{ currentAudit.businessName }}</span>
              </div>
              <div class="info-item">
                <span class="label">商户类型：</span>
                <span class="value">{{ getTypeText(currentAudit.type) }}</span>
              </div>
              <div class="info-item">
                <span class="label">联系人：</span>
                <span class="value">{{ currentAudit.contact }}</span>
              </div>
              <div class="info-item">
                <span class="label">联系电话：</span>
                <span class="value">{{ currentAudit.phone }}</span>
              </div>
              <div class="info-item">
                <span class="label">申请时间：</span>
                <span class="value">{{ currentAudit.applyTime }}</span>
              </div>
            </div>
          </div>
          
          <div>
            <h3 class="text-lg font-bold text-[#2A9D8F] mb-4 border-b border-gray-200 pb-2">资质文件</h3>
            <div class="space-y-2">
              <div class="file-item flex items-center justify-between p-3 rounded border border-gray-200 bg-gray-50">
                <span class="text-gray-500">营业执照</span>
                <el-button type="primary" size="small" link>
                  <i class="ri-download-line mr-1"></i>下载
                </el-button>
              </div>
              <div class="file-item flex items-center justify-between p-3 rounded border border-gray-200 bg-gray-50">
                <span class="text-gray-500">法人身份证</span>
                <el-button type="primary" size="small" link>
                  <i class="ri-download-line mr-1"></i>下载
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-8" v-if="currentAudit.status === 'pending'">
          <h3 class="text-lg font-bold text-[#2A9D8F] mb-4 border-b border-gray-200 pb-2">审核操作</h3>
          <el-form label-width="80px" class="text-gray-800">
            <el-form-item label="审核结果">
              <el-radio-group v-model="auditForm.result">
                <el-radio label="approved" class="text-gray-800">通过</el-radio>
                <el-radio label="rejected" class="text-gray-800">拒绝</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="审核意见">
              <el-input 
                v-model="auditForm.comment" 
                type="textarea" 
                :rows="4"
                placeholder="请输入审核意见（拒绝时必填）"
                class="glass-input"
              />
            </el-form-item>
          </el-form>
        </div>

        <!-- 内嵌审核日志 -->
        <div class="mt-8">
          <h3 class="text-lg font-bold text-[#2A9D8F] mb-4 border-b border-gray-200 pb-2">该商家审核日志</h3>
          <el-table :data="auditLogs" style="width: 100%" class="bg-transparent" size="small">
            <el-table-column prop="action" label="动作" width="100">
              <template #default="{ row }">
                <el-tag :type="row.action==='approve' ? 'success' : 'danger'" size="small" effect="dark">{{ row.action }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="时间" width="160" />
            <el-table-column prop="adminUsername" label="操作人" width="120" />
          </el-table>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false" class="glass-button">取消</el-button>
          <el-button 
            v-if="currentAudit?.status === 'pending'"
            type="primary" 
            @click="submitAudit"
            class="glow-button"
          >
            提交审核
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import HolographicCard from '@/components/HolographicCard.vue'
import { getPendingMerchants, approveMerchant, rejectMerchant, pageAuditLogs } from '@/api/adminMerchant'
import { getMerchantStats } from '@/api/adminMerchant'

const stats = ref({ pending: 0, approved: 0, rejected: 0, today: 0 })

const fetchStats = async () => {
  try {
    const res = await getMerchantStats() as any
    const d: any = res.data || {}
    stats.value = {
      pending: d.pending || 0,
      approved: d.approved || 0,
      rejected: d.rejected || 0,
      today: d.today || 0
    }
  } catch(e) { console.error(e) }
}

const searchForm = ref({
  status: '',
  type: '',
  name: ''
})

const pagination = ref({ page: 1, pageSize: 10, total: 0 })

const auditList = ref<any[]>([])

const detailVisible = ref(false)
const currentAudit = ref<any>(null)
const auditForm = ref({
  result: 'approved',
  comment: ''
})

// 审核日志（内嵌）
const auditLogs = ref<any[]>([])
const logsPagination = ref({ page: 1, pageSize: 10, total: 0 })
const fetchAuditLogs = async () => {
  if (!currentAudit.value?.id) return
  try {
    const res: any = await pageAuditLogs({ page: logsPagination.value.page, size: logsPagination.value.pageSize, userId: currentAudit.value.id })
    const d: any = res.data || {}
    auditLogs.value = d.records || []
    logsPagination.value.total = d.total || 0
  } catch(e) { console.error(e) }
}

const getTypeColor = (type: string) => {
  const colors: any = {
    scenic: 'success',
    hotel: 'primary',
    restaurant: 'warning'
  }
  return colors[type] || 'info'
}

const getTypeText = (type: string) => {
  const texts: any = {
    scenic: '景区',
    hotel: '酒店',
    restaurant: '餐饮'
  }
  return texts[type] || type
}

const getStatusText = (status: string) => {
  const texts: any = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已拒绝'
  }
  return texts[status] || status
}

const fetchList = async () => {
  try {
    const res = await getPendingMerchants({ pageNum: pagination.value.page, pageSize: pagination.value.pageSize })
    const pageData: any = res.data || {}
    const records: any[] = pageData.records || []
    // 将后端用户结构映射到页面展示结构
    auditList.value = records.map((u: any) => ({
      id: u.userId,
      businessName: u.nickname || u.username,
      logo: u.avatar || '',
      type: u.category || 'merchant',
      contact: u.nickname || u.username,
      phone: u.phone || '',
      applyTime: u.createdAt || '',
      status: u.status === 'pending' ? 'pending' : (u.status === 'active' ? 'approved' : 'rejected')
    }))
    pagination.value.total = pageData.total || 0
    stats.value.pending = pageData.total || 0 // Update pending count from list? Or fetchStats does it better. Keep independent.
  } catch(e) { console.error(e) }
}

const handleSearch = () => { fetchList() }

const handleReset = () => {
  searchForm.value = { status: '', type: '', name: '' }
  handleSearch()
}

const viewDetail = (row: any) => {
  currentAudit.value = row
  auditForm.value = { result: 'approved', comment: '' }
  detailVisible.value = true
  logsPagination.value.page = 1
  fetchAuditLogs()
}

const viewLogs = (row: any) => {
  currentAudit.value = row
  detailVisible.value = true
  logsPagination.value.page = 1
  fetchAuditLogs()
}

const handleApprove = (row: any) => {
  ElMessageBox.confirm(`确认通过商户"${row.businessName}"的入驻申请？`, '确认操作', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'success'
  }).then(() => {
    approveMerchant(row.id, auditForm.value.comment || undefined).then(() => {
      ElMessage.success('审核通过！')
      fetchList()
      fetchStats()
    })
  })
}

const handleReject = (row: any) => {
  ElMessageBox.prompt('请输入拒绝原因', `拒绝商户"${row.businessName}"`, {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputPlaceholder: '请输入拒绝原因',
    inputValidator: (value) => {
      if (!value || value.trim() === '') {
        return '拒绝原因不能为空'
      }
      return true
    }
  }).then(({ value }) => {
    rejectMerchant(row.id, value).then(() => {
      ElMessage.success('已拒绝该申请')
      fetchList()
      fetchStats()
    })
  })
}

const submitAudit = () => {
  if (auditForm.value.result === 'rejected' && !auditForm.value.comment) {
    ElMessage.warning('拒绝时必须填写审核意见')
    return
  }
  
  if (!currentAudit.value) return
  const action = auditForm.value.result === 'approved' ? approveMerchant : rejectMerchant
  const reason = auditForm.value.comment || undefined
  action(currentAudit.value.id, reason as any).then(() => {
    ElMessage.success('已提交审核')
    detailVisible.value = false
    fetchList(); fetchStats()
  })
}

const openProfile = (row: any) => {
  window.open(`#/admin/business/profile?userId=${row.id}`, '_blank')
}

onMounted(() => { fetchList(); fetchStats() })
</script>

<style scoped>
.audit-container {
  @apply p-6;
  color: #2C3E50;
}

.stat-card {
  @apply transition-transform hover:scale-105 cursor-pointer;
}

/* Deep overrides for Element Plus */
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

.info-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid rgba(42, 157, 143, 0.03);
}

.info-item .label {
  color: #909399;
  min-width: 100px;
  font-weight: bold;
}

.info-item .value {
  color: #2A9D8F;
  flex: 1;
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
