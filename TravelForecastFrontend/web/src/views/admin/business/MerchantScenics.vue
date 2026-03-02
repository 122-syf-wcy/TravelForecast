<template>
  <div class="merchant-scenics-container text-gray-800">
    <div class="page-header mb-6">
      <h2 class="text-2xl font-bold text-gray-800">景区申请管理</h2>
      <p class="text-gray-500 mt-1">配置商家可入驻的景区资源</p>
    </div>

    <HolographicCard glow-color="energy-orange">
      <template #header>
        <div class="card-header flex items-center justify-between">
          <div>
            <h3 class="text-lg font-bold text-orange-400">可申请景区列表</h3>
            <p class="text-xs text-gray-500 mt-1">管理哪些景区可以让商家申请</p>
          </div>
          <div class="flex gap-3">
            <el-button type="primary" @click="showAddDialog = true" class="glow-button">
              <el-icon class="mr-1"><Plus /></el-icon>
              添加景区
            </el-button>
            <el-button @click="loadList" class="glass-button">
              <el-icon class="mr-1"><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 数据表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        style="width: 100%"
        class="bg-transparent"
        :row-key="(row: any) => row.id"
        element-loading-background="rgba(0, 0, 0, 0.04)"
      >
        <el-table-column prop="scenicName" label="景区名称" min-width="200">
          <template #default="{ row }">
            <span class="text-orange-600 font-bold">{{ row.scenicName }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="scenicCity" label="城市" width="100">
          <template #default="{ row }">
            <span class="text-gray-500">{{ row.scenicCity }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="scenicLevel" label="级别" width="80" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small" effect="dark">{{ row.scenicLevel }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isAvailable ? 'success' : 'info'" size="small" effect="dark">
              {{ row.isAvailable ? '开放' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="商家数量" width="150" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2">
              <span :class="row.currentMerchants >= row.maxMerchants ? 'text-red-400' : 'text-green-400'">
                {{ row.currentMerchants }} / {{ row.maxMerchants }}
              </span>
              <el-progress 
                :percentage="calculatePercentage(row.currentMerchants, row.maxMerchants)" 
                :status="row.currentMerchants >= row.maxMerchants ? 'exception' : 'success'"
                :show-text="false"
                :stroke-width="5"
                style="width: 50px"
              />
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="priority" label="优先级" width="80" align="center">
          <template #default="{ row }">
            <span class="font-mono text-gray-500">{{ row.priority }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="添加时间" width="170" align="center">
          <template #default="{ row }">
            <span class="text-xs text-gray-500">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              :type="row.isAvailable ? 'warning' : 'success'"
              link
              @click="toggleAvailable(row)"
            >
              {{ row.isAvailable ? '关闭' : '开放' }}
            </el-button>
            <el-button
              size="small"
              type="primary"
              link
              @click="showEditDialog(row)"
            >
              编辑
            </el-button>
            <el-popconfirm
              title="确定要删除这个景区吗？"
              @confirm="deleteItem(row.id)"
              width="200"
            >
              <template #reference>
                <el-button size="small" type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </HolographicCard>

    <!-- 添加景区对话框 -->
    <el-dialog
      v-model="showAddDialog"
      title="添加可申请景区"
      width="600px"
      custom-class="glass-dialog"
      :close-on-click-modal="false"
    >
      <el-form :model="addForm" label-width="120px" class="text-gray-800">
        <el-form-item label="选择景区" required>
          <el-select
            v-model="addForm.scenicId"
            placeholder="请选择景区"
            filterable
            style="width: 100%"
            class="glass-select"
            @focus="loadAvailableToAdd"
          >
            <el-option
              v-for="scenic in availableScenicList"
              :key="scenic.id"
              :label="`${scenic.name} (${scenic.city})`"
              :value="scenic.id"
            >
              <div class="flex items-center justify-between w-full">
                <span>{{ scenic.name }}</span>
                <span class="text-gray-500 text-xs">{{ scenic.city }} · {{ scenic.level }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="最大商家数量" required>
          <el-input-number
            v-model="addForm.maxMerchants"
            :min="1"
            :max="50"
            style="width: 100%"
            class="glass-input-number"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAddDialog = false" class="glass-button">取消</el-button>
          <el-button type="primary" @click="handleAdd" class="glow-button">确定添加</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑景区对话框 -->
    <el-dialog
      v-model="showEditDialog_"
      title="编辑景区配置"
      width="600px"
      custom-class="glass-dialog"
      :close-on-click-modal="false"
    >
      <el-form :model="editForm" label-width="120px" class="text-gray-800">
        <el-form-item label="景区名称">
          <el-input v-model="editForm.scenicName" disabled />
        </el-form-item>
        
        <el-form-item label="是否开放申请">
          <el-switch 
            v-model="editForm.isAvailable" 
            style="--el-switch-on-color: #2A9D8F; --el-switch-off-color: #ff4949"
          />
        </el-form-item>
        
        <el-form-item label="最大商家数量" required>
          <el-input-number
            v-model="editForm.maxMerchants"
            :min="1"
            :max="50"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="优先级">
          <el-input-number
            v-model="editForm.priority"
            :min="0"
            :max="100"
            style="width: 100%"
          />
          <div class="text-gray-500 text-xs mt-1">数值越大，排序越靠前</div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditDialog_ = false" class="glass-button">取消</el-button>
          <el-button type="primary" @click="handleUpdate" class="glow-button">确定更新</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import HolographicCard from '@/components/HolographicCard.vue'
import request from '@/utils/request'

// 数据列表
const loading = ref(false)
const tableData = ref<any[]>([])

// 添加对话框
const showAddDialog = ref(false)
const availableScenicList = ref<any[]>([])
const addForm = ref({
  scenicId: null as number | null,
  maxMerchants: 1
})

// 编辑对话框
const showEditDialog_ = ref(false)
const editForm = ref({
  id: null as number | null,
  scenicName: '',
  isAvailable: true,
  maxMerchants: 1,
  priority: 0
})

// 加载可申请景区列表
const loadList = async () => {
  loading.value = true
  try {
    const res: any = await request({
      url: '/admin/merchant-scenics',
      method: 'get'
    })
    tableData.value = res.data || []
    ElMessage.success(`加载成功，共 ${tableData.value.length} 个景区`)
  } catch (error: any) {
    ElMessage.error('加载失败: ' + (error?.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 加载可添加的景区列表
const loadAvailableToAdd = async () => {
  try {
    const res: any = await request({
      url: '/admin/merchant-scenics/available-to-add',
      method: 'get'
    })
    availableScenicList.value = res.data || []
  } catch (error: any) {
    ElMessage.error('加载景区列表失败: ' + (error?.message || '未知错误'))
  }
}

// 添加景区
const handleAdd = async () => {
  if (!addForm.value.scenicId) {
    ElMessage.warning('请选择景区')
    return
  }
  
  try {
    await request({
      url: '/admin/merchant-scenics',
      method: 'post',
      data: {
        scenicId: addForm.value.scenicId,
        maxMerchants: addForm.value.maxMerchants
      }
    })
    
    ElMessage.success('添加成功')
    showAddDialog.value = false
    addForm.value = { scenicId: null, maxMerchants: 1 }
    loadList()
  } catch (error: any) {
    ElMessage.error('添加失败: ' + (error?.message || '未知错误'))
  }
}

// 显示编辑对话框
const showEditDialog = (row: any) => {
  editForm.value = {
    id: row.id,
    scenicName: row.scenicName,
    isAvailable: row.isAvailable,
    maxMerchants: row.maxMerchants,
    priority: row.priority
  }
  showEditDialog_.value = true
}

// 更新景区配置
const handleUpdate = async () => {
  try {
    await request({
      url: `/admin/merchant-scenics/${editForm.value.id}`,
      method: 'put',
      data: {
        isAvailable: editForm.value.isAvailable,
        maxMerchants: editForm.value.maxMerchants,
        priority: editForm.value.priority
      }
    })
    
    ElMessage.success('更新成功')
    showEditDialog_.value = false
    loadList()
  } catch (error: any) {
    ElMessage.error('更新失败: ' + (error?.message || '未知错误'))
  }
}

// 切换开放状态
const toggleAvailable = async (row: any) => {
  try {
    await request({
      url: `/admin/merchant-scenics/${row.id}`,
      method: 'put',
      data: {
        isAvailable: !row.isAvailable
      }
    })
    
    ElMessage.success(row.isAvailable ? '已关闭' : '已开放')
    loadList()
  } catch (error: any) {
    ElMessage.error('操作失败: ' + (error?.message || '未知错误'))
  }
}

// 删除景区
const deleteItem = async (id: number) => {
  try {
    await request({
      url: `/admin/merchant-scenics/${id}`,
      method: 'delete'
    })
    
    ElMessage.success('删除成功')
    loadList()
  } catch (error: any) {
    ElMessage.error('删除失败: ' + (error?.message || '未知错误'))
  }
}

// 计算百分比
const calculatePercentage = (current: number, max: number) => {
  if (max === 0) return 0
  return Math.round((current / max) * 100)
}

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.merchant-scenics-container {
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
  background-color: rgba(255, 107, 53, 0.1) !important; /* Orange hover */
}

:deep(.el-input__inner), :deep(.el-textarea__inner) {
  background-color: #FFFFFF !important;
  border: 1px solid #EBEEF5 !important;
  color: #303133 !important;
  box-shadow: none !important;
}

:deep(.el-input__inner:focus), :deep(.el-textarea__inner:focus) {
  border-color: #F4A261 !important;
}

:deep(.el-select__wrapper) {
  background-color: #FFFFFF !important;
  border: 1px solid #EBEEF5 !important;
  box-shadow: none !important;
}

:deep(.el-dialog) {
  background: #FFFFFF;
  backdrop-filter: blur(10px);
  border: 1px solid #EBEEF5;
  border-radius: 12px;
}

:deep(.el-dialog__title) {
  color: #F4A261;
  }

:deep(.el-dialog__body) {
  color: #2C3E50;
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
  background: #E76F51;
  border: 1px solid #E76F51;
  color: #FFFFFF;
  box-shadow: 0 2px 8px rgba(231, 111, 81, 0.3);
}

.glow-button:hover {
  background: #D45D3F;
  border-color: #D45D3F;
  color: #FFFFFF;
  box-shadow: 0 4px 12px rgba(231, 111, 81, 0.4);
}
</style>
