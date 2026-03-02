<template>
  <div class="data-export">
    <div class="page-header">
      <h1 class="text-2xl text-gray-800 mb-4">数据导出</h1>
      <p class="text-gray-500">导出六盘水旅游数据进行深入分析</p>
    </div>
    
    <holographic-card class="mb-6">
      <template #header>
        <div class="card-header">
          <h3 class="text-lg text-[#2A9D8F] font-bold">数据导出配置</h3>
        </div>
      </template>
      <el-form :model="exportForm" label-position="top" class="text-gray-800">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <el-form-item label="数据类型" class="text-gray-800">
              <el-select v-model="exportForm.dataType" placeholder="选择数据类型" class="w-full">
                <el-option v-for="item in dataTypeOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
              </el-select>
            </el-form-item>
          </div>
          
          <div>
            <el-form-item label="景区选择" class="text-gray-800">
              <el-select v-model="exportForm.scenicAreas" multiple placeholder="选择景区" class="w-full">
                <el-option v-for="item in scenicOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
              </el-select>
            </el-form-item>
          </div>
          
          <div>
            <el-form-item label="导出格式" class="text-gray-800">
              <el-select v-model="exportForm.format" placeholder="选择格式" class="w-full">
                <el-option v-for="item in formatOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
              </el-select>
            </el-form-item>
          </div>
          
          <div class="col-span-1 md:col-span-2">
            <el-form-item label="时间范围" class="text-gray-800">
              <el-date-picker
                v-model="exportForm.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                class="w-full"
              />
            </el-form-item>
          </div>
          
          <div>
            <el-form-item label="数据粒度" class="text-gray-800">
              <el-select v-model="exportForm.granularity" placeholder="选择数据粒度" class="w-full">
                <el-option v-for="item in granularityOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
              </el-select>
            </el-form-item>
          </div>
        </div>
        
        <el-divider class="border-gray-200" />
        
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-4">
          <div>
            <h4 class="text-gray-800 font-medium mb-2">高级选项</h4>
            <div class="flex flex-col space-y-2">
              <el-checkbox v-model="exportForm.includeRawData" label="包含原始数据" class="text-gray-800" />
              <el-checkbox v-model="exportForm.includeAnalytics" label="包含分析结果" class="text-gray-800" />
              <el-checkbox v-model="exportForm.includePredictions" label="包含预测数据" class="text-gray-800" />
              <el-checkbox v-model="exportForm.includeCharts" label="包含图表" class="text-gray-800" />
            </div>
          </div>
          
          <div>
            <h4 class="text-gray-800 font-medium mb-2">导出配置</h4>
            <div class="space-y-4">
              <el-select v-model="exportForm.template" placeholder="选择模板" class="w-full">
                <el-option v-for="item in templateOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
              </el-select>
              <div class="flex items-center justify-between">
                <span class="text-gray-800">预览导出</span>
                <el-switch v-model="exportForm.preview" active-color="#2A9D8F" />
              </div>
            </div>
          </div>
        </div>
        
        <div class="flex justify-end space-x-3">
          <el-button>保存模板</el-button>
          <el-button type="primary" @click="handleExport">生成导出</el-button>
        </div>
      </el-form>
    </holographic-card>
    
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
      <holographic-card>
        <div class="text-center mb-4">
          <el-icon class="text-4xl text-[#2A9D8F]"><Document /></el-icon>
          <h3 class="text-xl text-[#2A9D8F] mt-2">导出任务</h3>
        </div>
        <div class="text-center mb-2">
          <div class="text-4xl font-bold text-gray-800">{{ exportStats.total || 0 }}</div>
          <div class="text-sm text-gray-500">总计任务</div>
        </div>
        <div class="grid grid-cols-3 gap-2">
          <div class="text-center">
            <div class="text-xl font-bold text-green-400">{{ exportStats.completed || 0 }}</div>
            <div class="text-xs text-gray-500">已完成</div>
          </div>
          <div class="text-center">
            <div class="text-xl font-bold text-orange-400">{{ exportStats.processing || 0 }}</div>
            <div class="text-xs text-gray-500">处理中</div>
          </div>
          <div class="text-center">
            <div class="text-xl font-bold text-red-400">{{ exportStats.failed || 0 }}</div>
            <div class="text-xs text-gray-500">失败</div>
          </div>
        </div>
      </holographic-card>
      
      <holographic-card>
        <div class="text-center mb-4">
          <el-icon class="text-4xl text-[#457B9D]"><Download /></el-icon>
          <h3 class="text-xl text-[#457B9D] mt-2">今日下载量</h3>
        </div>
        <div class="text-center mb-2">
          <div class="text-4xl font-bold text-gray-800">{{ downloadStatsData.total || 0 }}</div>
          <div class="text-sm text-gray-500">文件下载次数</div>
        </div>
        <div class="grid grid-cols-3 gap-2">
          <div class="text-center">
            <div class="text-xl font-bold text-[#2A9D8F]">{{ downloadStatsData.excel || 0 }}</div>
            <div class="text-xs text-gray-500">Excel</div>
          </div>
          <div class="text-center">
            <div class="text-xl font-bold text-[#2A9D8F]">{{ downloadStatsData.csv || 0 }}</div>
            <div class="text-xs text-gray-500">CSV</div>
          </div>
          <div class="text-center">
            <div class="text-xl font-bold text-[#2A9D8F]">{{ downloadStatsData.pdf || 0 }}</div>
            <div class="text-xs text-gray-500">PDF</div>
          </div>
        </div>
      </holographic-card>
      
      <holographic-card>
        <div class="text-center mb-4">
          <el-icon class="text-4xl text-green-400"><DataBoard /></el-icon>
          <h3 class="text-xl text-green-400 mt-2">存储用量</h3>
        </div>
        <div class="text-center mb-2">
          <div class="text-4xl font-bold text-gray-800">{{ storageStatsData.percentage || 0 }}<span class="text-xl">%</span></div>
          <div class="text-sm text-gray-500">已使用容量</div>
        </div>
        <div class="mt-2">
          <el-progress :percentage="storageStatsData.percentage || 0" color="#2A9D8F" />
          <div class="flex justify-between text-xs text-gray-500 mt-1">
            <span>已用：{{ storageStatsData.used || '0B' }}</span>
            <span>总计：{{ storageStatsData.total || '5GB' }}</span>
          </div>
        </div>
      </holographic-card>
    </div>
    
    <holographic-card>
      <template #header>
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg text-orange-400 font-bold">导出历史</h3>
          <div class="flex items-center space-x-2">
            <el-input placeholder="搜索..." v-model="searchQuery" size="small" class="w-48" @keyup.enter="handleSearch" />
            <el-button type="info" size="small" @click="handleSearch">搜索</el-button>
            <el-button type="primary" size="small" icon="Delete" @click="clearExpired">清理过期</el-button>
          </div>
        </div>
      </template>
      <el-table :data="exportHistory" style="width: 100%" class="bg-transparent">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="文件名" min-width="180" />
        <el-table-column prop="type" label="数据类型" width="120" />
        <el-table-column prop="format" label="格式" width="80">
          <template #default="scope">
            <el-tag :type="getFormatType(scope.row.format)" size="small">{{ scope.row.format }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="size" label="大小" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <div class="flex space-x-2">
              <el-button type="primary" size="small" :disabled="scope.row.status !== '已完成'" link @click="download(scope.row)">下载</el-button>
              <el-button type="danger" size="small" link @click="remove(scope.row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-center mt-4">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="totalRecords"
          :page-size="pageSize"
          :current-page="currentPage"
          @current-change="handlePageChange"
        />
      </div>
    </holographic-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Document, Download, DataBoard } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import HolographicCard from '@/components/HolographicCard.vue'
import {
  exportData,
  getExportStats,
  getExportHistory,
  downloadExportFile,
  deleteExportRecord,
  clearExpiredRecords,
  getDownloadStats,
  getStorageStats
} from '@/api/export'
import { getScenicSpots } from '@/api/scenic'

const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const totalRecords = ref(0)

// 导出表单
const exportForm = reactive({
  dataType: 'visitor',
  scenicAreas: [null as number | null],
  format: 'csv',
  dateRange: [
    new Date(new Date().setMonth(new Date().getMonth() - 1)),
    new Date()
  ],
  granularity: 'day',
  includeRawData: true,
  includeAnalytics: true,
  includePredictions: false,
  includeCharts: false,
  template: 'default',
  preview: false
})

// 导出统计
const exportStats = reactive({
  total: 0,
  completed: 0,
  processing: 0,
  failed: 0
})

// 下载统计
const downloadStatsData = reactive({
  total: 0,
  excel: 0,
  csv: 0,
  pdf: 0
})

// 存储统计
const storageStatsData = reactive({
  used: '0B',
  total: '5GB',
  percentage: 0
})

// 数据类型选项
const dataTypeOptions = [
  { value: 'visitor', label: '游客流量数据' },
  { value: 'income', label: '收入数据' },
  { value: 'satisfaction', label: '满意度数据' },
  { value: 'source', label: '游客来源数据' },
  { value: 'prediction', label: '预测数据' }
]

// 景区选项（动态加载）
const scenicOptions = ref<any[]>([
  { value: null, label: '所有景区' }
])

// 导出格式选项
const formatOptions = [
  { value: 'excel', label: 'Excel (xlsx)' },
  { value: 'csv', label: 'CSV' },
  { value: 'pdf', label: 'PDF' },
  { value: 'json', label: 'JSON' }
]

// 数据粒度选项
const granularityOptions = [
  { value: 'hour', label: '小时' },
  { value: 'day', label: '天' },
  { value: 'week', label: '周' },
  { value: 'month', label: '月' }
]

// 模板选项
const templateOptions = [
  { value: 'default', label: '默认模板' },
  { value: 'report', label: '报表模板' },
  { value: 'analysis', label: '分析模板' },
  { value: 'presentation', label: '演示模板' }
]

// 导出历史
const exportHistory = ref<any[]>([])

// 加载景区列表
const loadScenicList = async () => {
  try {
    const res: any = await getScenicSpots({ city: '六盘水' })
    console.log('景区列表响应:', res)

    if (res.code === 200 && res.data) {
      const scenicList = Array.isArray(res.data)
        ? res.data.map((scenic: any) => ({
            value: scenic.id,
            label: scenic.name
          }))
        : res.data.list?.map((scenic: any) => ({
            value: scenic.id,
            label: scenic.name
          })) || []

      scenicOptions.value = [
        { value: null, label: '所有景区' },
        ...scenicList
      ]
      console.log('景区选项已加载:', scenicOptions.value)
    }
  } catch (error) {
    console.error('加载景区列表失败', error)
  }
}

// 加载导出统计
const loadExportStats = async () => {
  try {
    const res: any = await getExportStats()
    if (res.code === 200 && res.data) {
      Object.assign(exportStats, res.data)
    }
  } catch (error) {
    console.error('加载导出统计失败', error)
  }
}

// 加载导出历史
const loadExportHistory = async () => {
  try {
    const res: any = await getExportHistory(currentPage.value, pageSize.value, searchQuery.value)
    console.log('导出历史响应:', res)
    if (res.code === 200 && res.data) {
      exportHistory.value = res.data.data || []
      totalRecords.value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载导出历史失败', error)
  }
}

// 加载下载统计
const loadDownloadStats = async () => {
  try {
    const res: any = await getDownloadStats()
    if (res.code === 200 && res.data) {
      Object.assign(downloadStatsData, res.data)
    }
  } catch (error) {
    console.error('加载下载统计失败', error)
  }
}

// 加载存储统计
const loadStorageStats = async () => {
  try {
    const res: any = await getStorageStats()
    if (res.code === 200 && res.data) {
      Object.assign(storageStatsData, res.data)
    }
  } catch (error) {
    console.error('加载存储统计失败', error)
  }
}

// 初始化加载所有数据
const loadAllData = async () => {
  await Promise.all([
    loadScenicList(),
    loadExportStats(),
    loadExportHistory(),
    loadDownloadStats(),
    loadStorageStats()
  ])
}

// 处理导出
const handleExport = async () => {
  try {
    // 验证表单
    if (!exportForm.dateRange || exportForm.dateRange.length !== 2) {
      ElMessage.warning('请选择时间范围')
      return
    }

    // 格式化日期
    const startDate = new Date(exportForm.dateRange[0]).toISOString().split('T')[0]
    const endDate = new Date(exportForm.dateRange[1]).toISOString().split('T')[0]

    // 转换景区ID
    const scenicIds = exportForm.scenicAreas.filter(id => id !== null)

    const params = {
      dataType: exportForm.dataType,
      scenicIds: scenicIds.length > 0 ? scenicIds : [null],
      startDate,
      endDate,
      granularity: exportForm.granularity,
      format: exportForm.format,
      includeRawData: exportForm.includeRawData,
      includeAnalytics: exportForm.includeAnalytics,
      includePredictions: exportForm.includePredictions
    }

    console.log('导出参数:', params)

    const res: any = await exportData(params)
    console.log('导出响应:', res)

    if (res.code === 200) {
      ElMessage.success('数据导出任务已创建，正在处理...')
      // 刷新导出历史和统计
      await loadAllData()
    } else {
      ElMessage.error(res.message || '导出失败')
    }
  } catch (error: any) {
    console.error('导出失败', error)
    ElMessage.error('导出失败: ' + (error.message || '未知错误'))
  }
}

// 清理过期/失败
const clearExpired = async () => {
  try {
    await ElMessageBox.confirm('确定要清理所有未完成和失败的导出记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res: any = await clearExpiredRecords()
    if (res.code === 200) {
      ElMessage.success(res.message || '清理成功')
      await loadAllData()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('清理失败', error)
      ElMessage.error('清理失败')
    }
  }
}

// 下载文件
const download = async (row: any) => {
  try {
    console.log('下载文件:', row.id)
    const res: any = await downloadExportFile(row.id)
    
    // 创建Blob并下载
    const blob = new Blob([res], { type: 'application/octet-stream' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = row.name
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
    
    ElMessage.success('文件下载成功')
  } catch (error) {
    console.error('下载失败', error)
    ElMessage.error('下载失败')
  }
}

// 删除记录
const remove = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除导出记录 ${row.name} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res: any = await deleteExportRecord(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      await loadExportHistory()
      await loadExportStats()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败', error)
      ElMessage.error('删除失败')
    }
  }
}

// 分页处理
const handlePageChange = (page: number) => {
  currentPage.value = page
  loadExportHistory()
}

// 搜索处理
const handleSearch = () => {
  currentPage.value = 1
  loadExportHistory()
}

// 组件挂载时加载数据
onMounted(() => {
  loadAllData()
})

// 获取格式类型样式
const getFormatType = (format: string) => {
  const types: Record<string, string> = {
    'excel': 'success',
    'csv': 'info',
    'pdf': 'danger',
    'json': 'warning'
  }
  return types[format] || 'info'
}

// 获取状态类型样式
const getStatusType = (status: string) => {
  const types: Record<string, string> = {
    '已完成': 'success',
    '处理中': 'warning',
    '失败': 'danger'
  }
  return types[status] || 'info'
}
</script>

<style scoped>
.data-export {
  color: #2C3E50;
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

:deep(.el-form-item__label) {
  color: #606266 !important;
}

:deep(.el-input__inner), :deep(.el-select__wrapper) {
  background-color: #FFFFFF !important;
  border: 1px solid #DCDFE6 !important;
  color: #303133 !important;
}

:deep(.el-checkbox__label) {
  color: #606266 !important;
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
</style>