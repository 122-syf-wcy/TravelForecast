<template>
  <div class="business-list">
    <div class="page-header">
      <h1 class="text-2xl text-gray-800 mb-4">商户管理</h1>
      <p class="text-gray-500">智教黔行商户管理系统</p>
    </div>
    
    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
      <holographic-card glow-color="neon-cyan">
        <div class="flex items-center">
          <div class="p-3 rounded-full bg-blue-500 bg-opacity-20 mr-4">
            <el-icon class="text-2xl text-blue-400"><Shop /></el-icon>
          </div>
          <div>
            <div class="text-sm text-gray-500">总商户数</div>
            <div class="text-2xl font-bold text-gray-800">{{ stats.total }}</div>
          </div>
        </div>
      </holographic-card>
      
      <holographic-card glow-color="tech-purple">
        <div class="flex items-center">
          <div class="p-3 rounded-full bg-green-500 bg-opacity-20 mr-4">
            <el-icon class="text-2xl text-green-400"><Check /></el-icon>
          </div>
          <div>
            <div class="text-sm text-gray-500">已审核商户</div>
            <div class="text-2xl font-bold text-gray-800">{{ stats.approved }}</div>
          </div>
        </div>
      </holographic-card>
      
      <holographic-card glow-color="energy-orange">
        <div class="flex items-center">
          <div class="p-3 rounded-full bg-yellow-500 bg-opacity-20 mr-4">
            <el-icon class="text-2xl text-yellow-400"><Timer /></el-icon>
          </div>
          <div>
            <div class="text-sm text-gray-500">待审核商户</div>
            <div class="text-2xl font-bold text-gray-800">{{ stats.pending }}</div>
          </div>
        </div>
      </holographic-card>
      
      <holographic-card glow-color="neon-cyan">
        <div class="flex items-center">
          <div class="p-3 rounded-full bg-purple-500 bg-opacity-20 mr-4">
            <el-icon class="text-2xl text-[#457B9D]"><TrendCharts /></el-icon>
          </div>
          <div>
            <div class="text-sm text-gray-500">今日新增</div>
            <div class="text-2xl font-bold text-gray-800">{{ stats.today }}</div>
          </div>
        </div>
      </holographic-card>
    </div>
    
    <!-- 筛选条件 -->
    <holographic-card class="mb-6">
      <template #header>
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg text-[#2A9D8F] font-bold">商户筛选</h3>
          <div class="space-x-2">
            <el-button type="primary" size="small" @click="openAddBusiness()">
              <el-icon><Plus /></el-icon> 添加商户
            </el-button>
            <el-button size="small" @click="openExport()">
              批量导出
            </el-button>
          </div>
        </div>
      </template>
      <el-form :model="filterForm" inline class="text-gray-800">
        <el-form-item label="商户名称" class="text-gray-800">
          <el-input v-model="filterForm.name" placeholder="输入商户名称" />
        </el-form-item>
        <el-form-item label="商户类型" class="text-gray-800">
          <el-select v-model="filterForm.type" placeholder="选择类型" style="width: 150px">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="所属景区" class="text-gray-800">
          <el-select v-model="filterForm.scenic" placeholder="选择景区" style="width: 150px">
            <el-option v-for="item in scenicOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态" class="text-gray-800">
          <el-select v-model="filterForm.status" placeholder="选择状态" style="width: 150px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </holographic-card>
    
    <!-- 商户列表 -->
    <holographic-card>
      <template #header>
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg text-[#2A9D8F] font-bold">商户列表</h3>
          <div class="flex items-center space-x-2">
            <el-input placeholder="搜索商户..." v-model="searchQuery" size="small" class="w-48" @keyup.enter="handleSearch" />
            <el-button type="primary" size="small" @click="openExport()">批量导出</el-button>
          </div>
        </div>
      </template>
      <el-table :data="tableData" style="width: 100%" class="bg-transparent" v-loading="loading" element-loading-background="rgba(0, 0, 0, 0.04)">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="商户信息" min-width="200">
          <template #default="scope">
            <div class="flex items-center">
              <el-avatar 
                :size="48"
                :src="scope.row.avatar" 
                icon="User"
                class="mr-3"
              />
              <div>
                <div class="font-bold text-gray-800">{{ scope.row.name }}</div>
                <div class="text-xs text-gray-500">{{ scope.row.address || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="120">
          <template #default="scope">
            <el-tag :type="getTypeStyle(scope.row.type)" size="small" effect="dark">{{ scope.row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scenic" label="所属景区" width="150" />
        <el-table-column prop="contact" label="联系人" width="100" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="rating" label="评分" width="120">
          <template #default="scope">
            <div class="flex items-center">
              <el-rate v-model="scope.row.rating" disabled text-color="#ff9900" />
              <span class="ml-1 text-gray-500">{{ scope.row.rating }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small" effect="dark">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <div class="flex space-x-2">
              <el-button type="primary" size="small" @click="handleEdit(scope.row)" link>编辑</el-button>
              <el-button type="danger" size="small" @click="handleDelete(scope.row)" link>删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-center mt-4">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="pager.total"
          :page-size="pager.pageSize"
          :current-page="pager.pageNum"
          @current-change="(p)=>{pager.pageNum=p;fetchTable()}"
        />
      </div>
    </holographic-card>

    <!-- 查看/编辑商户详情对话框 -->
    <el-dialog v-model="editVisible" title="商户详情" width="800px" :close-on-click-modal="false" custom-class="glass-dialog">
      <div class="merchant-detail">
        <h3 class="text-lg font-bold text-gray-800 mb-4 border-b border-gray-200 pb-2">基本信息</h3>
        <div class="grid grid-cols-2 gap-4 mb-6">
          <div class="info-item">
            <span class="label">商户ID：</span>
            <span class="value">{{ merchantDetail.id }}</span>
          </div>
          <div class="info-item">
            <span class="label">商户名称：</span>
            <span class="value">{{ merchantDetail.businessName || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">商户类型：</span>
            <span class="value">{{ merchantDetail.category || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">联系人：</span>
            <span class="value">{{ merchantDetail.contact || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">联系电话：</span>
            <span class="value">{{ merchantDetail.phone || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">营业执照号：</span>
            <span class="value">{{ merchantDetail.licenseNo || '-' }}</span>
          </div>
          <div class="info-item col-span-2">
            <span class="label">地址：</span>
            <span class="value">{{ merchantDetail.address || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">状态：</span>
            <el-tag :type="getStatusType(merchantDetail.status)" size="small">
              {{ merchantDetail.status }}
            </el-tag>
          </div>
        </div>

        <h3 class="text-lg font-bold text-gray-800 mb-4 border-b border-gray-200 pb-2">营业执照</h3>
        <div class="license-section mb-6">
          <el-image 
            v-if="merchantDetail.licenseUrl" 
            :src="merchantDetail.licenseUrl" 
            fit="contain"
            class="license-image"
            :preview-src-list="[merchantDetail.licenseUrl]"
          />
          <div v-else class="no-license">
            <el-icon class="text-4xl text-gray-500"><DocumentRemove /></el-icon>
            <p class="text-gray-500 mt-2">暂未上传营业执照</p>
          </div>
        </div>

        <h3 class="text-lg font-bold text-gray-800 mb-4 border-b border-gray-200 pb-2">账号信息</h3>
        <div class="grid grid-cols-2 gap-4">
          <div class="info-item">
            <span class="label">用户名：</span>
            <span class="value">{{ merchantDetail.username || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">昵称：</span>
            <span class="value">{{ merchantDetail.nickname || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">邮箱：</span>
            <span class="value">{{ merchantDetail.email || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">注册时间：</span>
            <span class="value">{{ merchantDetail.createdAt || '-' }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editVisible=false" class="glass-button">关闭</el-button>
          <el-button type="primary" @click="openProfileEdit" class="glow-button">前往编辑</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导出参数表单 -->
    <el-dialog v-model="exportVisible" title="导出商户" width="520px" custom-class="glass-dialog">
      <el-form :model="exportForm" label-width="100px" class="text-gray-800">
        <el-form-item label="导出格式">
          <el-radio-group v-model="exportForm.format">
            <el-radio label="xlsx" class="text-gray-800">Excel (.xlsx)</el-radio>
            <el-radio label="csv" class="text-gray-800">CSV (.csv)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="exportForm.status" placeholder="选择状态" class="w-full">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="exportForm.type" placeholder="选择类型" class="w-full">
            <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属景区">
          <el-select v-model="exportForm.scenic" placeholder="选择景区" class="w-full">
            <el-option v-for="s in scenicOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="exportVisible=false" class="glass-button">取消</el-button>
          <el-button type="primary" @click="submitExport" class="glow-button">导出</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 添加商户弹窗 -->
    <el-dialog v-model="addMerchantVisible" title="添加商户" width="500px">
      <el-form :model="addMerchantForm" label-width="80px" :rules="addMerchantRules" ref="addMerchantFormRef">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="addMerchantForm.username" placeholder="请输入商户登录用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="addMerchantForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="商户名称" prop="businessName">
          <el-input v-model="addMerchantForm.businessName" placeholder="请输入商户名称" />
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="addMerchantForm.contact" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="addMerchantForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="addMerchantForm.email" placeholder="请输入邮箱（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addMerchantVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAddMerchant" :loading="addMerchantLoading">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { 
  Plus, Shop, Check, Timer, TrendCharts, DocumentRemove, User
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import HolographicCard from '@/components/HolographicCard.vue'
import { adminApi } from '@/api/admin'
import { pageMerchantList, getMerchantStats } from '@/api/adminMerchant'
import request from '@/utils/request'

const searchQuery = ref('')
const loading = ref(false)
const stats = ref({ total: 0, approved: 0, pending: 0, today: 0 })

// 筛选表单
const filterForm = reactive({
  name: '',
  type: '',
  scenic: '',
  status: ''
})

// 商户类型选项
const typeOptions = [
  { value: '', label: '全部类型' },
  { value: '餐饮', label: '餐饮' },
  { value: '住宿', label: '住宿' },
  { value: '购物', label: '购物' },
  { value: '娱乐', label: '娱乐' },
  { value: '交通', label: '交通' }
]

// 景区选项
const scenicOptions = [
  { value: '', label: '全部景区' },
  { value: '梅花山风景区', label: '梅花山风景区' },
  { value: '玉舍国家滑雪场', label: '玉舍国家滑雪场' },
  { value: '乌蒙大草原', label: '乌蒙大草原' },
  { value: '北盘江大桥景区', label: '北盘江大桥景区' },
  { value: '中营苗寨', label: '中营苗寨' }
]

// 状态选项
const statusOptions = [
  { value: '', label: '全部状态' },
  { value: '已审核', label: '已审核' },
  { value: '未审核', label: '未审核' },
  { value: '已禁用', label: '已禁用' }
]

// 表格数据 - 初始化为空，强制使用后端数据
const tableData = ref<any[]>([])
const pager = ref({ pageNum: 1, pageSize: 10, total: 0 })

const fetchTable = async () => {
  loading.value = true
  try {
    const res: any = await pageMerchantList({ page: pager.value.pageNum, size: pager.value.pageSize, keyword: searchQuery.value || filterForm.name, type: filterForm.type, status: filterForm.status })
    const page = res?.data
    pager.value.total = page?.total || 0
    tableData.value = (page?.records || []).map((m: any) => ({
      id: m.userId,
      name: m.businessName || m.nickname || m.username || '未命名商户',
      avatar: m.avatar || '',
      address: m.address || '-',
      type: m.category || '-',
      scenic: '-',
      contact: m.contact || m.nickname || '-',
      phone: m.phone || '-',
      rating: 0, // 评分数据目前接口未返回，暂为0
      status: m.status === 'active' ? '已审核' : m.status === 'pending' ? '未审核' : '已禁用'
    }))
  } catch (error) {
    console.error('获取商户列表失败', error)
    // ElMessage.error('获取商户列表失败') // 可选提示
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const res: any = await getMerchantStats()
    const d = res?.data || {}
    stats.value = {
      total: (d.pending || 0) + (d.approved || 0) + (d.rejected || 0),
      approved: d.approved || 0,
      pending: d.pending || 0,
      today: d.today || 0
    }
  } catch (_) {}
}

// 获取商户类型样式
const getTypeStyle = (type: string) => {
  const styles: Record<string, string> = {
    '餐饮': 'success',
    '住宿': 'primary',
    '购物': 'warning',
    '娱乐': 'danger',
    '交通': 'info'
  }
  return styles[type] || ''
}

// 获取状态类型样式
const getStatusType = (status: string) => {
  const types: Record<string, string> = {
    '已审核': 'success',
    '未审核': 'warning',
    '已禁用': 'danger'
  }
  return types[status] || 'info'
}

// 处理搜索
const handleSearch = () => { pager.value.pageNum = 1; fetchTable() }

// 重置表单
const resetForm = () => {
  filterForm.name = ''
  filterForm.type = ''
  filterForm.scenic = ''
  filterForm.status = ''
  searchQuery.value = ''
  pager.value.pageNum = 1
  fetchTable()
}

// 商户详情对话框
const editVisible = ref(false)
const merchantDetail = ref<any>({})

// 编辑商户（查看完整信息）
const handleEdit = async (row: any) => {
  try {
    // 加载用户基本信息
    const userRes: any = await request({ 
      url: `/admin/users/${row.id}`, 
      method: 'get' 
    })
    
    // 加载商家资料
    const profileRes: any = await request({ 
      url: `/admin/merchant/profile/${row.id}`, 
      method: 'get' 
    })
    
    merchantDetail.value = {
      id: row.id,
      username: userRes?.data?.username || '-',
      nickname: userRes?.data?.nickname || '-',
      email: userRes?.data?.email || '-',
      createdAt: userRes?.data?.createdAt || '-',
      businessName: profileRes?.data?.businessName || '-',
      category: profileRes?.data?.category || '-',
      contact: profileRes?.data?.contact || '-',
      phone: profileRes?.data?.phone || '-',
      licenseNo: profileRes?.data?.licenseNo || '-',
      licenseUrl: profileRes?.data?.licenseUrl || '',
      address: profileRes?.data?.address || '-',
      status: row.status
    }
    
    editVisible.value = true
  } catch (error) {
    console.error('加载商户信息失败:', error)
    ElMessage.error('加载商户信息失败')
  }
}

// 前往编辑页面
const openProfileEdit = () => {
  window.open(`#/admin/business/profile?userId=${merchantDetail.value.id}`, '_blank')
}

// 跳转添加商户
const addMerchantVisible = ref(false)
const addMerchantLoading = ref(false)
const addMerchantFormRef = ref()
const addMerchantForm = ref({
  username: '',
  password: '',
  businessName: '',
  contact: '',
  phone: '',
  email: ''
})
const addMerchantRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  businessName: [{ required: true, message: '请输入商户名称', trigger: 'blur' }],
  contact: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }]
}

const openAddBusiness = () => {
  addMerchantForm.value = { username: '', password: '', businessName: '', contact: '', phone: '', email: '' }
  addMerchantVisible.value = true
}

const submitAddMerchant = async () => {
  const formEl = addMerchantFormRef.value
  if (!formEl) return
  await formEl.validate(async (valid: boolean) => {
    if (!valid) return
    addMerchantLoading.value = true
    try {
      const { default: authApi } = await import('@/api/auth')
      await authApi.registerMerchant({
        username: addMerchantForm.value.username,
        password: addMerchantForm.value.password,
        businessName: addMerchantForm.value.businessName,
        contact: addMerchantForm.value.contact,
        phone: addMerchantForm.value.phone,
        email: addMerchantForm.value.email
      })
      ElMessage.success('商户添加成功')
      addMerchantVisible.value = false
      loadMerchantList()
      loadStats()
    } catch (error: any) {
      console.error('添加商户失败:', error)
      ElMessage.error(error?.response?.data?.message || '添加商户失败')
    } finally {
      addMerchantLoading.value = false
    }
  })
}

// 删除商户（调用后端API）
const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认删除商户"${row.name}"？此操作将删除该商户的账号和所有相关数据。`, '警告', { 
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
    
    // 调用后端API删除商户
    await request({ 
      url: `/admin/users/${row.id}`, 
      method: 'delete' 
    })
    
    ElMessage.success('删除成功')
    fetchTable()
    fetchStats()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 导出
const exportVisible = ref(false)
const exportForm = ref({ format: 'xlsx', status: '', type: '', scenic: '' })
const openExport = () => { exportVisible.value = true }
const submitExport = async () => {
  const { format, status, type, scenic } = exportForm.value as any
  const res = await adminApi.exportData({ resource: 'business', format: format as any, filters: { status, type, scenic } })
  ElMessage.success(`导出任务已创建：${res.data.taskId || 'dev'}`)
  exportVisible.value = false
}

onMounted(() => { fetchTable(); fetchStats() })
</script>

<style scoped>
.business-list {
  color: #2C3E50;
  min-height: 80vh;
}

/* Deep styles for Element Plus components override */
:deep(.el-table) {
  background-color: #FFFFFF !important;
  --el-table-border-color: #EBEEF5;
  --el-table-header-bg-color: #F5F7FA;
  --el-table-tr-bg-color: #FFFFFF;
  color: #2C3E50;
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
  line-height: 1.5;
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

:deep(.el-pagination) {
  --el-pagination-bg-color: #FFFFFF;
  --el-pagination-text-color: #606266;
  --el-pagination-button-color: #606266;
  --el-pagination-button-bg-color: #F5F7FA;
  --el-pagination-button-disabled-color: #C0C4CC;
  --el-pagination-button-disabled-bg-color: #FFFFFF;
  --el-pagination-hover-color: #2A9D8F;
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #2A9D8F; color: #FFFFFF;
}

/* 商户详情样式 */
.merchant-detail {
  color: #2C3E50;
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

.license-section {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
  border: 1px dashed rgba(42, 157, 143, 0.3);
  border-radius: 8px;
  background: #FAFAFA;
  padding: 10px;
}

.license-image {
  max-width: 100%;
  max-height: 400px;
}

.no-license {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

/* Glass Dialog Styles via :deep since custom-class might not filter down if scoped, but global CSS is usually handled in App.vue or index.css. 
   Here we try to apply local override if possible or assume global theme. */
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