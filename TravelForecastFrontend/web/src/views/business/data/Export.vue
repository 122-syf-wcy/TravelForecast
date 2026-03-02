<template>
  <div class="data-export">
    <div class="page-header mb-6">
      <h1 class="text-2xl text-[#2C3E50] mb-2">数据导出</h1>
      <p class="text-gray-600">导出{{ currentScenicName }}的运营数据进行分析</p>
    </div>

    <!-- 快捷导出 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      <div class="export-card" @click="quickExport('daily')">
        <el-icon class="text-4xl text-[#2A9D8F] mb-3"><Calendar /></el-icon>
        <h3 class="text-gray-800 font-bold mb-1">今日报表</h3>
        <p class="text-gray-500 text-sm">导出今日运营数据</p>
      </div>
      <div class="export-card" @click="quickExport('weekly')">
        <el-icon class="text-4xl text-purple-400 mb-3"><DataLine /></el-icon>
        <h3 class="text-gray-800 font-bold mb-1">本周报表</h3>
        <p class="text-gray-500 text-sm">导出近7天运营数据</p>
      </div>
      <div class="export-card" @click="quickExport('monthly')">
        <el-icon class="text-4xl text-green-400 mb-3"><TrendCharts /></el-icon>
        <h3 class="text-gray-800 font-bold mb-1">本月报表</h3>
        <p class="text-gray-500 text-sm">导出本月运营数据</p>
      </div>
      <div class="export-card" @click="quickExport('annual')">
        <el-icon class="text-4xl text-orange-400 mb-3"><PieChart /></el-icon>
        <h3 class="text-gray-800 font-bold mb-1">年度报表</h3>
        <p class="text-gray-500 text-sm">导出全年运营数据</p>
      </div>
    </div>

    <!-- 自定义导出 -->
    <el-card class="bg-white border border-gray-200 shadow-lg mb-6">
      <template #header>
        <h3 class="text-lg text-[#2A9D8F] font-bold">自定义导出</h3>
      </template>

      <el-form :model="exportForm" label-width="100px">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <el-form-item label="数据类型">
            <el-checkbox-group v-model="exportForm.dataTypes">
              <el-checkbox label="visitor">客流数据</el-checkbox>
              <el-checkbox label="revenue">营收数据</el-checkbox>
              <el-checkbox label="ticket">门票数据</el-checkbox>
              <el-checkbox label="review">评价数据</el-checkbox>
            </el-checkbox-group>
          </el-form-item>

          <el-form-item label="时间范围">
            <el-date-picker
              v-model="exportForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              class="w-full"
            />
          </el-form-item>

          <el-form-item label="导出格式">
            <el-radio-group v-model="exportForm.format">
              <el-radio label="csv">CSV</el-radio>
              <el-radio label="excel">Excel</el-radio>
              <el-radio label="json">JSON</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="数据粒度">
            <el-select v-model="exportForm.granularity" class="w-full">
              <el-option label="按小时" value="hour" />
              <el-option label="按天" value="day" />
              <el-option label="按周" value="week" />
              <el-option label="按月" value="month" />
            </el-select>
          </el-form-item>
        </div>

        <el-divider class="border-gray-200" />

        <div class="mb-4">
          <h4 class="text-gray-800 font-medium mb-2">高级选项</h4>
          <div class="flex flex-wrap gap-4">
            <el-checkbox v-model="exportForm.includeRawData">包含原始数据</el-checkbox>
            <el-checkbox v-model="exportForm.includeAnalytics">包含分析结果</el-checkbox>
            <el-checkbox v-model="exportForm.includePredictions">包含预测数据</el-checkbox>
          </div>
        </div>

        <el-form-item>
          <el-button type="primary" @click="customExport" :loading="exporting">
            <el-icon class="mr-1"><Download /></el-icon>
            生成导出
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 导出统计 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
      <el-card class="bg-white border border-gray-200 shadow-sm text-center">
        <el-icon class="text-3xl text-[#2A9D8F] mb-2"><Document /></el-icon>
        <div class="text-3xl font-bold text-gray-800">{{ exportStats.total || 0 }}</div>
        <div class="text-sm text-gray-500 mb-2">导出任务</div>
        <div class="grid grid-cols-3 gap-2 text-center">
          <div>
            <div class="text-lg font-bold text-green-500">{{ exportStats.completed || 0 }}</div>
            <div class="text-xs text-gray-500">已完成</div>
          </div>
          <div>
            <div class="text-lg font-bold text-orange-400">{{ exportStats.processing || 0 }}</div>
            <div class="text-xs text-gray-500">处理中</div>
          </div>
          <div>
            <div class="text-lg font-bold text-red-400">{{ exportStats.failed || 0 }}</div>
            <div class="text-xs text-gray-500">失败</div>
          </div>
        </div>
      </el-card>

      <el-card class="bg-white border border-gray-200 shadow-sm text-center">
        <el-icon class="text-3xl text-[#457B9D] mb-2"><Download /></el-icon>
        <div class="text-3xl font-bold text-gray-800">{{ downloadStatsData.total || 0 }}</div>
        <div class="text-sm text-gray-500 mb-2">今日下载</div>
        <div class="grid grid-cols-3 gap-2 text-center">
          <div>
            <div class="text-lg font-bold text-[#2A9D8F]">{{ downloadStatsData.excel || 0 }}</div>
            <div class="text-xs text-gray-500">Excel</div>
          </div>
          <div>
            <div class="text-lg font-bold text-[#2A9D8F]">{{ downloadStatsData.csv || 0 }}</div>
            <div class="text-xs text-gray-500">CSV</div>
          </div>
          <div>
            <div class="text-lg font-bold text-[#2A9D8F]">{{ downloadStatsData.pdf || 0 }}</div>
            <div class="text-xs text-gray-500">PDF</div>
          </div>
        </div>
      </el-card>

      <el-card class="bg-white border border-gray-200 shadow-sm text-center">
        <el-icon class="text-3xl text-green-400 mb-2"><DataBoard /></el-icon>
        <div class="text-3xl font-bold text-gray-800">{{ storageStatsData.percentage || 0 }}<span class="text-lg">%</span></div>
        <div class="text-sm text-gray-500 mb-2">存储用量</div>
        <el-progress :percentage="storageStatsData.percentage || 0" color="#2A9D8F" />
        <div class="flex justify-between text-xs text-gray-500 mt-1">
          <span>已用：{{ storageStatsData.used || '0B' }}</span>
          <span>总计：{{ storageStatsData.total || '5GB' }}</span>
        </div>
      </el-card>
    </div>

    <!-- 导出历史 -->
    <el-card class="bg-white border border-gray-200 shadow-lg">
      <template #header>
        <div class="flex justify-between items-center">
          <h3 class="text-lg text-purple-400 font-bold">导出历史</h3>
          <div class="flex items-center space-x-2">
            <el-input placeholder="搜索..." v-model="searchQuery" size="small" class="w-48" @keyup.enter="handleSearch" />
            <el-button size="small" @click="handleSearch">搜索</el-button>
            <el-button type="danger" size="small" text @click="clearExpired">清理过期</el-button>
          </div>
        </div>
      </template>

      <el-table :data="exportHistory" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="文件名" min-width="180" />
        <el-table-column prop="type" label="数据类型" width="120" />
        <el-table-column prop="format" label="格式" width="80">
          <template #default="{ row }">
            <el-tag :type="getFormatType(row.format)" size="small">{{ (row.format || '').toUpperCase() }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="size" label="文件大小" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" size="small" link :disabled="row.status !== '已完成'" @click="downloadFile(row)">下载</el-button>
            <el-button type="danger" size="small" link @click="removeRecord(row)">删除</el-button>
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
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Calendar, DataLine, TrendCharts, PieChart, Download, Document, DataBoard } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMerchantScenicInfo } from '@/api/merchantScenic'
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

// 景区信息
const currentScenicName = ref('加载中...')
const currentScenicId = ref<number | null>(null)

// 分页与搜索
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const totalRecords = ref(0)
const exporting = ref(false)

// 导出表单
const exportForm = reactive({
  dataTypes: ['visitor', 'revenue'] as string[],
  dateRange: [] as string[],
  format: 'csv',
  granularity: 'day',
  includeRawData: true,
  includeAnalytics: true,
  includePredictions: false
})

// 导出统计
const exportStats = reactive({ total: 0, completed: 0, processing: 0, failed: 0 })
const downloadStatsData = reactive({ total: 0, excel: 0, csv: 0, pdf: 0 })
const storageStatsData = reactive({ used: '0B', total: '5GB', percentage: 0 })

// 导出历史
const exportHistory = ref<any[]>([])

// 初始化景区信息
const initScenicInfo = async () => {
  try {
    const res: any = await getMerchantScenicInfo()
    const data = res?.data || res
    if (data && data.scenicName) {
      currentScenicName.value = data.scenicName
      currentScenicId.value = data.scenicId || null
    } else {
      currentScenicName.value = '我的景区'
    }
  } catch (error) {
    console.error('获取景区信息失败:', error)
    currentScenicName.value = '我的景区'
  }
}

// 加载导出统计
const loadExportStats = async () => {
  try {
    const res: any = await getExportStats()
    if (res.code === 200 && res.data) Object.assign(exportStats, res.data)
  } catch (error) {
    console.error('加载导出统计失败:', error)
  }
}

// 加载导出历史
const loadExportHistory = async () => {
  try {
    const res: any = await getExportHistory(currentPage.value, pageSize.value, searchQuery.value)
    if (res.code === 200 && res.data) {
      exportHistory.value = res.data.data || []
      totalRecords.value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载导出历史失败:', error)
  }
}

// 加载下载统计
const loadDownloadStats = async () => {
  try {
    const res: any = await getDownloadStats()
    if (res.code === 200 && res.data) Object.assign(downloadStatsData, res.data)
  } catch (error) {
    console.error('加载下载统计失败:', error)
  }
}

// 加载存储统计
const loadStorageStats = async () => {
  try {
    const res: any = await getStorageStats()
    if (res.code === 200 && res.data) Object.assign(storageStatsData, res.data)
  } catch (error) {
    console.error('加载存储统计失败:', error)
  }
}

// 加载所有数据
const loadAllData = async () => {
  await Promise.all([
    loadExportStats(),
    loadExportHistory(),
    loadDownloadStats(),
    loadStorageStats()
  ])
}

// 快捷导出
const quickExport = async (type: string) => {
  const now = new Date()
  let startDate = ''
  const endDate = now.toISOString().split('T')[0]

  switch (type) {
    case 'daily':
      startDate = endDate
      break
    case 'weekly': {
      const weekAgo = new Date(now)
      weekAgo.setDate(weekAgo.getDate() - 7)
      startDate = weekAgo.toISOString().split('T')[0]
      break
    }
    case 'monthly': {
      const monthAgo = new Date(now)
      monthAgo.setMonth(monthAgo.getMonth() - 1)
      startDate = monthAgo.toISOString().split('T')[0]
      break
    }
    case 'annual': {
      startDate = `${now.getFullYear()}-01-01`
      break
    }
  }

  try {
    const res: any = await exportData({
      dataType: 'visitor',
      scenicIds: currentScenicId.value ? [currentScenicId.value] : [null],
      startDate,
      endDate,
      granularity: type === 'daily' ? 'hour' : 'day',
      format: 'csv',
      includeRawData: true,
      includeAnalytics: true,
      includePredictions: false
    })

    if (res.code === 200) {
      ElMessage.success('导出任务已创建，正在处理...')
      await loadAllData()
    } else {
      ElMessage.error(res.message || '导出失败')
    }
  } catch (error: any) {
    console.error('快捷导出失败:', error)
    ElMessage.error('导出失败: ' + (error.message || '未知错误'))
  }
}

// 自定义导出
const customExport = async () => {
  if (exportForm.dataTypes.length === 0) {
    ElMessage.warning('请至少选择一种数据类型')
    return
  }
  if (!exportForm.dateRange || exportForm.dateRange.length !== 2) {
    ElMessage.warning('请选择时间范围')
    return
  }

  exporting.value = true
  try {
    const res: any = await exportData({
      dataType: exportForm.dataTypes.join(','),
      scenicIds: currentScenicId.value ? [currentScenicId.value] : [null],
      startDate: exportForm.dateRange[0],
      endDate: exportForm.dateRange[1],
      granularity: exportForm.granularity,
      format: exportForm.format,
      includeRawData: exportForm.includeRawData,
      includeAnalytics: exportForm.includeAnalytics,
      includePredictions: exportForm.includePredictions
    })

    if (res.code === 200) {
      ElMessage.success('导出任务已创建，正在处理...')
      await loadAllData()
    } else {
      ElMessage.error(res.message || '导出失败')
    }
  } catch (error: any) {
    console.error('自定义导出失败:', error)
    ElMessage.error('导出失败: ' + (error.message || '未知错误'))
  } finally {
    exporting.value = false
  }
}

// 下载文件
const downloadFile = async (row: any) => {
  try {
    const res: any = await downloadExportFile(row.id)
    const blob = new Blob([res], { type: 'application/octet-stream' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = row.name
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
    ElMessage.success('下载成功')
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败')
  }
}

// 删除记录
const removeRecord = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除导出记录「${row.name}」吗？`, '提示', {
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
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 清理过期
const clearExpired = async () => {
  try {
    await ElMessageBox.confirm('确定要清理所有过期和失败的导出记录吗？', '提示', {
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
      console.error('清理失败:', error)
      ElMessage.error('清理失败')
    }
  }
}

// 重置表单
const resetForm = () => {
  exportForm.dataTypes = ['visitor', 'revenue']
  exportForm.dateRange = []
  exportForm.format = 'csv'
  exportForm.granularity = 'day'
  exportForm.includeRawData = true
  exportForm.includeAnalytics = true
  exportForm.includePredictions = false
}

// 分页
const handlePageChange = (page: number) => {
  currentPage.value = page
  loadExportHistory()
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadExportHistory()
}

// 格式标签样式
const getFormatType = (format: string) => {
  const types: Record<string, string> = { excel: 'success', csv: 'info', pdf: 'danger', json: 'warning' }
  return types[format] || 'info'
}

// 状态标签样式
const getStatusType = (status: string) => {
  const types: Record<string, string> = { '已完成': 'success', '处理中': 'warning', '失败': 'danger' }
  return types[status] || 'info'
}

onMounted(async () => {
  await initScenicInfo()
  await loadAllData()
})
</script>

<style scoped>
.data-export {
  color: #2C3E50;
}

.export-card {
  @apply p-6 rounded-lg border border-gray-200 bg-white shadow-sm
         cursor-pointer transition-all text-center hover:border-[#2A9D8F] hover:shadow-md;
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

:deep(.el-table) {
  background-color: #FFFFFF !important;
}

:deep(.el-table tr), :deep(.el-table th) {
  background-color: transparent !important;
}

:deep(.el-table td), :deep(.el-table th) {
  border-bottom: 1px solid #EBEEF5;
}

:deep(.el-table .cell) {
  color: #606266;
}

:deep(.el-checkbox__label) {
  color: #606266;
}

:deep(.el-radio__label) {
  color: #606266;
}

:deep(.el-form-item__label) {
  color: #606266 !important;
}

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: #606266;
}
</style>
