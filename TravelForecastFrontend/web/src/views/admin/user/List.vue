<template>
  <div class="user-list text-gray-800">
    <div class="page-header mb-6">
      <h2 class="text-2xl font-bold text-gray-800">用户管理</h2>
      <p class="text-gray-500 mt-1">智教黔行用户管理系统</p>
    </div>
    
    <!-- 数据概览 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
      <holographic-card glow-color="neon-cyan">
        <template #header>
          <h3 class="text-lg text-[#2A9D8F] font-bold">用户增长趋势</h3>
        </template>
        <div ref="userGrowthChartRef" class="h-64"></div>
      </holographic-card>
      
      <holographic-card glow-color="tech-purple">
        <template #header>
          <h3 class="text-lg text-[#457B9D] font-bold">用户分布</h3>
        </template>
        <div ref="userDistributionChartRef" class="h-64"></div>
      </holographic-card>
      
      <holographic-card glow-color="energy-orange">
        <template #header>
          <h3 class="text-lg text-orange-400 font-bold">核心指标</h3>
        </template>
        <div class="grid grid-cols-2 gap-4 h-64 content-center">
          <div class="p-4 border-r border-gray-200">
            <div class="text-center">
              <div class="text-sm text-gray-500 mb-2">总用户数</div>
              <div class="text-3xl font-bold text-gray-800 font-mono">{{ userStats.totalUsers.toLocaleString() }}</div>
              <div class="text-xs text-green-400 mt-1 flex justify-center items-center">
                <el-icon><Top /></el-icon> 13.2%
              </div>
            </div>
          </div>
          <div class="p-4">
            <div class="text-center">
              <div class="text-sm text-gray-500 mb-2">活跃用户</div>
              <div class="text-3xl font-bold text-gray-800 font-mono">{{ userStats.activeUsers.toLocaleString() }}</div>
              <div class="text-xs text-green-400 mt-1 flex justify-center items-center">
                <el-icon><Top /></el-icon> 8.7%
              </div>
            </div>
          </div>
          <div class="p-4 border-r border-t border-gray-200">
            <div class="text-center">
              <div class="text-sm text-gray-500 mb-2">本月新增</div>
              <div class="text-3xl font-bold text-gray-800 font-mono">{{ userStats.monthlyNewUsers.toLocaleString() }}</div>
              <div class="text-xs text-green-400 mt-1 flex justify-center items-center">
                <el-icon><Top /></el-icon> 5.3%
              </div>
            </div>
          </div>
          <div class="p-4 border-t border-gray-200">
            <div class="text-center">
              <div class="text-sm text-gray-500 mb-2">付费用户</div>
              <div class="text-3xl font-bold text-gray-800 font-mono">{{ userStats.paidUsers.toLocaleString() }}</div>
              <div class="text-xs text-green-400 mt-1 flex justify-center items-center">
                <el-icon><Top /></el-icon> 3.9%
              </div>
            </div>
          </div>
        </div>
      </holographic-card>
    </div>
    
    <!-- 筛选条件 -->
    <holographic-card class="mb-6">
      <template #header>
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg text-[#2A9D8F] font-bold">用户筛选</h3>
          <div class="flex gap-3">
            <el-button type="primary" size="small" @click="openAddUser" class="glow-button">
              <el-icon class="mr-1"><Plus /></el-icon> 添加用户
            </el-button>
            <el-button size="small" @click="openExport" class="glass-button">
              <el-icon class="mr-1"><Download /></el-icon> 批量导出
            </el-button>
          </div>
        </div>
      </template>
      <el-form :model="filterForm" inline class="text-gray-800">
        <el-form-item label="用户名">
          <el-input v-model="filterForm.username" placeholder="输入用户名" class="glass-input" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="filterForm.phone" placeholder="输入手机号" class="glass-input" />
        </el-form-item>
        <el-form-item label="用户等级">
          <el-select v-model="filterForm.level" placeholder="选择等级" class="glass-select" style="width: 150px">
            <el-option v-for="item in levelOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="注册时间">
          <el-date-picker
            v-model="filterForm.registerDate"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 250px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="选择状态" class="glass-select" style="width: 150px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" class="glow-button">查询</el-button>
          <el-button @click="resetForm" class="glass-button">重置</el-button>
        </el-form-item>
      </el-form>
    </holographic-card>
    
    <!-- 用户列表 -->
    <holographic-card>
      <template #header>
        <div class="card-header flex items-center justify-between">
          <h3 class="text-lg text-[#2A9D8F] font-bold">用户列表</h3>
          <div class="flex items-center space-x-2">
            <el-input 
              placeholder="搜索用户..." 
              v-model="searchQuery" 
              size="small" 
              class="w-48 glass-input"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button :icon="Search" />
              </template>
            </el-input>
          </div>
        </div>
      </template>
      <el-table 
        :data="tableData" 
        style="width: 100%" 
        class="bg-transparent" 
        v-loading="loading"
        element-loading-background="rgba(0, 0, 0, 0.04)"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="userId" label="ID" width="70" align="center">
          <template #default="{ row }">
            <span class="text-gray-500 font-mono">{{ row.userId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户信息" min-width="200">
          <template #default="{ row }">
            <div class="flex items-center">
              <el-avatar :size="40" :src="row.avatar || 'https://via.placeholder.com/40'" class="mr-3 border border-gray-200" />
              <div>
                <div class="font-bold text-gray-800">{{ row.nickname || row.username }}</div>
                <div class="text-xs text-gray-500">{{ row.email || '未设置邮箱' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">
            <span class="font-mono text-[#2A9D8F]">{{ row.phone }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180">
          <template #default="{ row }">
            <span class="text-xs text-gray-500">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="loginAt" label="最后登录" width="180">
          <template #default="{ row }">
            <span class="text-xs text-gray-500">{{ row.loginAt ? formatDateTime(row.loginAt) : '从未登录' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small" effect="dark">{{ formatStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="flex space-x-2">
              <el-button type="primary" size="small" @click="handleEdit(row)" link>编辑</el-button>
              <el-button 
                :type="row.status === 'inactive' ? 'success' : 'danger'" 
                size="small" 
                @click="handleStatusToggle(row)" 
                link
              >
                {{ row.status === 'inactive' ? '启用' : '禁用' }}
              </el-button>
              <el-popconfirm
                title="确认删除该用户吗？"
                @confirm="handleDelete(row)"
              >
                <template #reference>
                  <el-button type="danger" size="small" link>删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-end mt-4">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadUserList"
          @current-change="loadUserList"
        />
      </div>
    </holographic-card>

    <!-- 用户新增表单 -->
    <el-dialog 
      v-model="editVisible" 
      :title="editForm.userId ? '编辑用户' : '添加用户'" 
      width="600px" 
      :close-on-click-modal="false"
      custom-class="glass-dialog"
    >
      <el-form :model="editForm" label-width="100px" ref="editRef" :rules="rules" class="text-gray-800">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" class="glass-input" :disabled="!!editForm.userId" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" class="glass-input" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" class="glass-input" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" class="glass-input" />
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="editForm.level" class="w-full glass-select">
            <el-option v-for="l in levelOptions" :key="l.value" :label="l.label" :value="l.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" class="w-full glass-select">
            <el-option label="正常" value="active" />
            <el-option label="禁用" value="inactive" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editVisible=false" class="glass-button">取消</el-button>
          <el-button type="primary" @click="submitUser" class="glow-button">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导出表单 -->
    <el-dialog 
      v-model="exportVisible" 
      title="导出用户" 
      width="520px"
      custom-class="glass-dialog"
    >
      <el-form :model="exportForm" label-width="100px" class="text-gray-800">
        <el-form-item label="导出格式">
          <el-radio-group v-model="exportForm.format">
            <el-radio label="xlsx" class="text-gray-800">Excel (.xlsx)</el-radio>
            <el-radio label="csv" class="text-gray-800">CSV (.csv)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="exportForm.status" class="w-full glass-select">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="exportForm.level" class="w-full glass-select">
            <el-option v-for="l in levelOptions" :key="l.value" :label="l.label" :value="l.value" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { 
  Plus, User, Edit, Delete, Top, Search, Download
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import HolographicCard from '@/components/HolographicCard.vue'
import * as echarts from 'echarts'
import { adminApi } from '@/api/admin'

const searchQuery = ref('')
const userGrowthChartRef = ref<HTMLElement | null>(null)
const userDistributionChartRef = ref<HTMLElement | null>(null)

// 筛选表单
const filterForm = reactive({
  username: '',
  phone: '',
  level: '',
  registerDate: [],
  status: ''
})

// 用户等级选项
const levelOptions = [
  { value: '', label: '全部等级' },
  { value: '普通用户', label: '普通用户' },
  { value: '银卡会员', label: '银卡会员' },
  { value: '金卡会员', label: '金卡会员' },
  { value: '钻石会员', label: '钻石会员' }
]

// 状态选项
const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'active', label: '正常' },
  { value: 'inactive', label: '禁用' }
]

// 表格数据
const tableData = ref<any[]>([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 用户统计数据
const userStats = reactive({
  totalUsers: 0,
  activeUsers: 0,
  monthlyNewUsers: 0,
  paidUsers: 0
})

// 获取状态类型样式
const getStatusType = (status: string) => {
  const types: Record<string, string> = {
    'active': 'success',
    'inactive': 'danger',
    'pending': 'warning'
  }
  return types[(status || '').toLowerCase()] || 'info'
}

// 格式化状态
const formatStatus = (status: string) => {
  const statusMap: Record<string, string> = {
    'active': '正常',
    'inactive': '禁用',
    'pending': '待审核'
  }
  return statusMap[(status || '').toLowerCase()] || status
}

// 格式化日期时间
const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ').substring(0, 19)
}

// 加载用户列表
const loadUserList = async () => {
  loading.value = true
  try {
    const params: any = {
      page: pagination.page,
      size: pagination.size
    }
    
    if (filterForm.username) params.username = filterForm.username
    if (filterForm.phone) params.phone = filterForm.phone
    if (filterForm.status) params.status = filterForm.status
    if (filterForm.registerDate && filterForm.registerDate.length === 2) {
      params.startDate = filterForm.registerDate[0]
      params.endDate = filterForm.registerDate[1]
    }
    if (searchQuery.value) params.keyword = searchQuery.value // Support simple search box too
    
    const res = await adminApi.getUserList(params)
    if (res.code === 200) {
      tableData.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const res = await adminApi.getUserStatistics()
    if (res.code === 200) {
      Object.assign(userStats, res.data)
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 处理搜索
const handleSearch = () => {
  pagination.page = 1
  loadUserList()
}

// 重置表单
const resetForm = () => {
  filterForm.username = ''
  filterForm.phone = ''
  filterForm.level = ''
  filterForm.registerDate = []
  filterForm.status = ''
  searchQuery.value = ''
  loadUserList()
}

// 新增/编辑用户表单
const editVisible = ref(false)
const editRef = ref()
const editForm = ref<any>({ userId: null, username: '', phone: '', email: '', nickname: '', level: '普通用户', status: 'active' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

const openAddUser = () => { 
  editForm.value = { userId: null, username: '', phone: '', email: '', nickname: '', level: '普通用户', status: 'active' }
  editVisible.value = true 
}

const submitUser = async () => { 
  try {
    await editRef.value?.validate?.()
    
    if (editForm.value.userId) {
      // 更新用户
      const res = await adminApi.updateUser(editForm.value.userId, editForm.value)
      if (res.code === 200) {
        ElMessage.success('用户信息更新成功')
        editVisible.value = false
        loadUserList()
      } else {
        ElMessage.error(res.message || '更新失败')
      }
    } else {
      // 新增用户 - 调用注册API
      try {
        const authApi = await import('@/api/auth')
        const res = await authApi.default.registerUser({
          username: editForm.value.username,
          password: editForm.value.password || '123456',
          phone: editForm.value.phone,
          email: editForm.value.email,
          role: editForm.value.role || 'user'
        })
        if (res.code === 200) {
          ElMessage.success('用户添加成功')
          editVisible.value = false
          loadUserList()
        } else {
          ElMessage.error(res.message || '添加失败')
        }
      } catch (addError: any) {
        ElMessage.error(addError?.response?.data?.message || '添加用户失败')
      }
    }
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 编辑用户
const handleEdit = (row: any) => {
  editForm.value = { 
    userId: row.userId, 
    username: row.username, 
    phone: row.phone, 
    email: row.email, 
    nickname: row.nickname,
    level: row.level || '普通用户',
    status: row.status 
  }
  editVisible.value = true
}

// 切换状态
const handleStatusToggle = async (row: any) => {
  const newStatus = row.status === 'inactive' ? 'active' : 'inactive'
  const statusText = newStatus === 'active' ? '启用' : '禁用'
  
  try {
    // await ElMessageBox.confirm(...) // Optional confirmation
    const res = await adminApi.updateUserStatus(row.userId, newStatus)
    if (res.code === 200) {
      ElMessage.success(`已将用户状态更改为${formatStatus(newStatus)}`)
      loadUserList()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error: any) {
     ElMessage.error(error.message || '操作失败')
  }
}

// 删除用户
const handleDelete = async (row: any) => {
  try {
    const res = await adminApi.deleteUser(row.userId)
    if (res.code === 200) {
      ElMessage.success('用户删除成功')
      if (tableData.value.length === 1 && pagination.page > 1) {
        pagination.page--
      }
      loadUserList()
      loadStatistics()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error: any) {
     ElMessage.error(error.message || '删除失败')
  }
}

// 导出
const exportVisible = ref(false)
const exportForm = ref({ format: 'xlsx', status: '', level: '' })
const openExport = () => { exportVisible.value = true }
const submitExport = async () => {
  try {
    const { format, status, level } = exportForm.value as any
    // 获取所有符合条件的用户数据
    const params: any = { page: 1, size: 10000 }
    if (status) params.status = status
    const res = await adminApi.getUserList(params)
    const users = res.data?.records || []
    
    if (users.length === 0) {
      ElMessage.warning('没有可导出的用户数据')
      return
    }

    const statusMap: Record<string, string> = { 'active': '正常', 'inactive': '禁用', 'pending': '待审核' }
    const header = ['ID', '用户名', '昵称', '手机号', '邮箱', '状态', '注册时间', '最后登录']
    const rows = users.map((u: any) => [
      u.userId, u.username, u.nickname || '', u.phone || '', u.email || '',
      statusMap[(u.status || '').toLowerCase()] || u.status,
      u.createdAt ? u.createdAt.replace('T', ' ').substring(0, 19) : '',
      u.loginAt ? u.loginAt.replace('T', ' ').substring(0, 19) : ''
    ])

    const csvContent = '\uFEFF' + [header, ...rows].map(r => r.map((v: any) => `"${v}"`).join(',')).join('\n')
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `用户列表_${new Date().toISOString().slice(0, 10)}.${format === 'xlsx' ? 'csv' : 'csv'}`
    link.click()
    URL.revokeObjectURL(url)
    
    ElMessage.success(`成功导出 ${users.length} 条用户数据`)
    exportVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '导出失败')
  }
}

onMounted(async () => {
  await loadStatistics()
  await loadUserList()
  
  nextTick(async () => {
    await initUserGrowthChart()
    await initUserDistributionChart()
  })
})

onBeforeUnmount(() => {
  [userGrowthChartRef, userDistributionChartRef].forEach(r => {
    if (r.value) {
      const inst = echarts.getInstanceByDom(r.value)
      if (inst) inst.dispose()
    }
  })
})

const initUserGrowthChart = async () => {
  const chartDom = userGrowthChartRef.value
  if (!chartDom) return
  
  try {
    const res = await adminApi.getUserGrowth()
    const growthData = res.code === 200 ? res.data : { months: [], newUsers: [], activeUsers: [] }
    
    const myChart = echarts.init(chartDom)
    const option = {
      backgroundColor: 'transparent',
      tooltip: { trigger: 'axis', backgroundColor: 'rgba(255, 255, 255, 0.98)', borderColor: '#2A9D8F', textStyle: { color: '#303133' } },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '8%', containLabel: true },
      xAxis: {
        type: 'category',
        data: growthData.months || ['1月', '2月', '3月', '4月', '5月', '6月', '7月'],
        axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.08)' } },
        axisLabel: { color: '#303133' }
      },
      yAxis: {
        type: 'value',
        axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.08)' } },
        axisLabel: { color: '#303133' },
        splitLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.04)' } }
      },
      series: [
        {
          name: '新增用户',
          type: 'bar',
          data: growthData.newUsers || [],
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#2A9D8F' },
              { offset: 1, color: 'rgba(42, 157, 143, 0.1)' }
            ])
          },
          barWidth: '20%'
        },
        {
          name: '活跃用户',
          type: 'line',
          data: growthData.activeUsers || [],
          smooth: true,
          symbol: 'none',
          lineStyle: { width: 3, color: '#FF6B35' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(255, 107, 53, 0.5)' },
              { offset: 1, color: 'rgba(255, 107, 53, 0)' }
            ])
          }
        }
      ]
    }
    myChart.setOption(option)
    window.addEventListener('resize', () => myChart.resize())
  } catch (error) { console.error('加载用户增长数据失败:', error) }
}

const initUserDistributionChart = async () => {
  const chartDom = userDistributionChartRef.value
  if (!chartDom) return
  
  try {
    const res = await adminApi.getUserDistribution()
    const distributionData = res.code === 200 ? res.data : []
    const colors = ['#2A9D8F', '#457B9D', '#FF6B35', '#2A9D8F', '#457B9D']
    const chartData = distributionData.map((item: any, index: number) => ({
      value: item.value,
      name: item.name,
      itemStyle: { color: colors[index % colors.length] }
    }))
    
    const myChart = echarts.init(chartDom)
    const option = {
      backgroundColor: 'transparent',
      tooltip: { trigger: 'item', backgroundColor: 'rgba(255, 255, 255, 0.98)', borderColor: '#2A9D8F', textStyle: { color: '#303133' } },
      legend: { orient: 'vertical', right: '0%', top: 'center', textStyle: { color: '#303133' } },
      series: [
        {
          name: '用户分布',
          type: 'pie',
          radius: ['45%', '70%'],
          center: ['35%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 10, borderColor: '#FFFFFF', borderWidth: 2 },
          label: { show: false },
          labelLine: { show: false },
          data: chartData
        }
      ]
    }
    myChart.setOption(option)
    window.addEventListener('resize', () => myChart.resize())
  } catch (error) { console.error('加载用户分布数据失败:', error) }
}
</script>

<style scoped>
.user-list {
  padding: 20px;
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
  box-shadow: 0 2px 8px rgba(42, 157, 143, 0.3);
}

.glow-button:hover {
  background: #238B7E;
  border-color: #238B7E;
  color: #FFFFFF;
  box-shadow: 0 4px 12px rgba(42, 157, 143, 0.4);
}
</style>