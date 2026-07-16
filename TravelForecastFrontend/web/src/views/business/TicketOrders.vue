<template>
  <div class="ticket-orders-container">
    <holographic-card class="mb-6">
      <template #header>
        <div class="flex justify-between items-center">
          <h2 class="text-2xl font-bold text-[#2A9D8F]">门票订单管理</h2>
          <el-button type="primary" @click="loadOrders">
            <el-icon class="mr-1"><Refresh /></el-icon>
            刷新数据
          </el-button>
        </div>
      </template>

      <!-- 统计卡片 -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div class="stat-card">
          <div class="text-gray-500 text-sm mb-1">今日订单</div>
          <div class="text-2xl font-bold text-[#2A9D8F]">{{ statistics.todayOrders }}</div>
          <div class="text-xs text-gray-500 mt-1">¥{{ statistics.todayAmount.toFixed(2) }}</div>
        </div>
        <div class="stat-card">
          <div class="text-gray-500 text-sm mb-1">待支付</div>
          <div class="text-2xl font-bold text-yellow-400">{{ statistics.pendingOrders }}</div>
        </div>
        <div class="stat-card">
          <div class="text-gray-500 text-sm mb-1">已支付</div>
          <div class="text-2xl font-bold text-green-400">{{ statistics.paidOrders }}</div>
        </div>
        <div class="stat-card">
          <div class="text-gray-500 text-sm mb-1">已使用</div>
          <div class="text-2xl font-bold text-blue-400">{{ statistics.usedOrders }}</div>
        </div>
      </div>

      <!-- 查询表单 -->
      <el-form :inline="true" :model="queryForm" class="mb-4">
        <el-form-item label="订单状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable class="w-40">
            <el-option label="待支付" value="pending" />
            <el-option label="已支付" value="paid" />
            <el-option label="已使用" value="used" />
            <el-option label="已取消" value="cancelled" />
            <el-option label="已退款" value="refunded" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="queryForm.keyword"
            placeholder="订单号/游客姓名/手机号"
            clearable
            class="w-60"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 订单表格 -->
      <el-table
        :data="orderList"
        v-loading="loading"
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#F5F7FA', color: '#2A9D8F' }"
      >
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="visitorName" label="游客姓名" width="100" />
        <el-table-column prop="visitorPhone" label="联系电话" width="120" />
        <el-table-column label="票种" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getTicketTypeColor(row.ticketType)">
              {{ getTicketTypeText(row.ticketType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ticketCount" label="数量" width="80" align="center" />
        <el-table-column label="总金额" width="100">
          <template #default="{ row }">
            <span class="text-green-400 font-bold">¥{{ row.totalAmount.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="visitDate" label="参观日期" width="110" />
        <el-table-column label="订单状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getStatusColor(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <template v-if="row.status === 'pending'">
                <el-button type="success" size="small" @click="handleConfirm(row)">确认</el-button>
                <el-button type="danger" size="small" @click="handleCancel(row)">取消</el-button>
              </template>
              <template v-else-if="row.status === 'paid'">
                <el-button type="success" size="small" @click="handleVerify(row)">核销</el-button>
              </template>
              <el-button type="primary" size="small" @click="handleDetail(row)">详情</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="flex justify-end mt-4">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadOrders"
          @current-change="loadOrders"
        />
      </div>
    </holographic-card>

    <!-- 订单详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="订单详情"
      width="600px"
    >
      <div v-if="currentOrder" class="space-y-4">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">
            {{ currentOrder.orderNo }}
          </el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getStatusColor(currentOrder.status)">
              {{ getStatusText(currentOrder.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="景区名称">
            {{ currentOrder.scenicName }}
          </el-descriptions-item>
          <el-descriptions-item label="游客姓名">
            {{ currentOrder.visitorName }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            {{ currentOrder.visitorPhone }}
          </el-descriptions-item>
          <el-descriptions-item label="票种">
            <el-tag size="small" :type="getTicketTypeColor(currentOrder.ticketType)">
              {{ getTicketTypeText(currentOrder.ticketType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="购买数量">
            {{ currentOrder.ticketCount }} 张
          </el-descriptions-item>
          <el-descriptions-item label="单价">
            ¥{{ currentOrder.unitPrice.toFixed(2) }}
          </el-descriptions-item>
          <el-descriptions-item label="总金额">
            <span class="text-green-400 font-bold text-lg">
              ¥{{ currentOrder.totalAmount.toFixed(2) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="参观日期">
            {{ currentOrder.visitDate }}
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">
            {{ currentOrder.createdAt }}
          </el-descriptions-item>
          <el-descriptions-item label="支付时间" v-if="currentOrder.paymentTime">
            {{ currentOrder.paymentTime }}
          </el-descriptions-item>
          <el-descriptions-item label="使用时间" v-if="currentOrder.usedTime">
            {{ currentOrder.usedTime }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <div class="flex justify-end">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import HolographicCard from '@/components/HolographicCard.vue'
import {
  getMerchantTicketOrders,
  getTicketOrderStatistics,
  updateTicketOrderStatus,
  verifyTickets,
  type TicketOrder,
  type TicketOrderStatus,
  type TicketType
} from '@/api/ticket'

// 查询表单
const queryForm = reactive({
  status: '' as TicketOrderStatus | '',
  startDate: '',
  endDate: '',
  keyword: '',
  page: 1,
  size: 10
})

// 日期范围
const dateRange = ref<string[]>([])

// 订单列表
const orderList = ref<TicketOrder[]>([])
const total = ref(0)
const loading = ref(false)

// 统计数据
const statistics = ref({
  totalOrders: 0,
  totalAmount: 0,
  totalTickets: 0,
  pendingOrders: 0,
  paidOrders: 0,
  usedOrders: 0,
  cancelledOrders: 0,
  todayOrders: 0,
  todayAmount: 0
})

// 当前订单
const currentOrder = ref<TicketOrder | null>(null)
const detailDialogVisible = ref(false)

// 加载订单列表
const loadOrders = async () => {
  loading.value = true
  try {
    const res: any = await getMerchantTicketOrders({
      status: queryForm.status || undefined,
      startDate: queryForm.startDate || undefined,
      endDate: queryForm.endDate || undefined,
      keyword: queryForm.keyword || undefined,
      page: queryForm.page,
      size: queryForm.size
    })
    
    const data = res?.data || res
    orderList.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    console.error('加载订单列表失败:', error)
    ElMessage.error('加载订单列表失败')
  } finally {
    loading.value = false
  }
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const res: any = await getTicketOrderStatistics()
    const data = res?.data || res
    statistics.value = {
      totalOrders: data.totalOrders || 0,
      totalAmount: data.totalAmount || 0,
      totalTickets: data.totalTickets || 0,
      pendingOrders: data.pendingOrders || 0,
      paidOrders: data.paidOrders || 0,
      usedOrders: data.usedOrders || 0,
      cancelledOrders: data.cancelledOrders || 0,
      todayOrders: data.todayOrders || 0,
      todayAmount: data.todayAmount || 0
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 查询
const handleQuery = () => {
  if (dateRange.value && dateRange.value.length === 2) {
    queryForm.startDate = dateRange.value[0]
    queryForm.endDate = dateRange.value[1]
  } else {
    queryForm.startDate = ''
    queryForm.endDate = ''
  }
  queryForm.page = 1
  loadOrders()
}

// 重置
const handleReset = () => {
  queryForm.status = ''
  queryForm.startDate = ''
  queryForm.endDate = ''
  queryForm.keyword = ''
  queryForm.page = 1
  dateRange.value = []
  loadOrders()
}

// 核销门票
const handleVerify = async (order: TicketOrder) => {
  try {
    await ElMessageBox.confirm(
      `确认核销订单 ${order.orderNo}（${order.ticketCount}张票）？`,
      '核销确认',
      {
        confirmButtonText: '确认核销',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res: any = await verifyTickets([order.id])
    const data = res?.data || res
    
    if (data.success > 0) {
      ElMessage.success('核销成功')
      loadOrders()
      loadStatistics()
    } else {
      ElMessage.error(data.message || '核销失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('核销失败:', error)
      ElMessage.error(error?.message || '核销失败')
    }
  }
}

// 确认订单（待支付 -> 已支付）
const handleConfirm = async (order: TicketOrder) => {
  try {
    await ElMessageBox.confirm(
      `确认订单 ${order.orderNo}？确认后订单将变为已支付状态，游客可凭票入园。`,
      '确认订单',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    await updateTicketOrderStatus(order.id, 'paid')
    ElMessage.success('订单已确认，游客可凭票入园')
    loadOrders()
    loadStatistics()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('确认订单失败:', error)
      ElMessage.error(error?.message || '确认订单失败')
    }
  }
}

// 查看详情
const handleDetail = (order: TicketOrder) => {
  currentOrder.value = order
  detailDialogVisible.value = true
}

// 取消订单
const handleCancel = async (order: TicketOrder) => {
  try {
    await ElMessageBox.confirm(
      `确认取消订单 ${order.orderNo}？`,
      '取消确认',
      {
        confirmButtonText: '确认取消',
        cancelButtonText: '返回',
        type: 'warning'
      }
    )
    
    await updateTicketOrderStatus(order.id, 'cancelled')
    ElMessage.success('订单已取消')
    loadOrders()
    loadStatistics()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('取消订单失败:', error)
      ElMessage.error(error?.message || '取消订单失败')
    }
  }
}

// 获取票种文本
const getTicketTypeText = (type: TicketType) => {
  const typeMap: Record<TicketType, string> = {
    adult: '成人票',
    child: '儿童票',
    student: '学生票',
    elder: '老人票'
  }
  return typeMap[type] || type
}

// 获取票种颜色
const getTicketTypeColor = (type: TicketType) => {
  const colorMap: Record<TicketType, string> = {
    adult: '',
    child: 'success',
    student: 'warning',
    elder: 'info'
  }
  return colorMap[type] || ''
}

// 获取状态文本
const getStatusText = (status: TicketOrderStatus) => {
  const statusMap: Record<TicketOrderStatus, string> = {
    pending: '待支付',
    paid: '已支付',
    used: '已使用',
    cancelled: '已取消',
    refunded: '已退款'
  }
  return statusMap[status] || status
}

// 获取状态颜色
const getStatusColor = (status: TicketOrderStatus) => {
  const colorMap: Record<TicketOrderStatus, any> = {
    pending: 'warning',
    paid: 'success',
    used: 'info',
    cancelled: 'danger',
    refunded: 'danger'
  }
  return colorMap[status] || ''
}

onMounted(() => {
  loadOrders()
  loadStatistics()
})
</script>

<style scoped>
.ticket-orders-container {
  @apply p-6;
}

.stat-card {
  @apply p-4 rounded-lg border border-gray-200 bg-white shadow-sm transition-all hover:border-[#2A9D8F] hover:shadow-md;
}

/* 操作按钮样式 */
.action-buttons {
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  gap: 8px;
  align-items: center;
}

.action-buttons .el-button {
  margin: 0 !important;
}

/* 表格样式 - 浅色主题 */
:deep(.el-table) {
  background-color: #FFFFFF !important;
  color: #606266 !important;
}

:deep(.el-table__body-wrapper) {
  background-color: #FFFFFF !important;
}

:deep(.el-table tr) {
  background-color: #FFFFFF !important;
  color: #606266 !important;
}

:deep(.el-table__row) {
  background-color: #FFFFFF !important;
  color: #606266 !important;
}

:deep(.el-table__row:hover > td) {
  background-color: #F5F7FA !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background-color: #FAFAFA !important;
}

:deep(.el-table td),
:deep(.el-table th) {
  background-color: transparent !important;
  border-color: #EBEEF5 !important;
  color: #606266 !important;
}

:deep(.el-table__expanded-cell) {
  background-color: #F5F7FA !important;
  padding: 20px !important;
}

:deep(.el-table__expand-icon) {
  color: #2A9D8F !important;
}

/* 描述列表样式 */
:deep(.el-descriptions) {
  background-color: transparent !important;
}

:deep(.el-descriptions__label),
:deep(.el-descriptions__content) {
  background-color: #FFFFFF !important;
  color: #606266 !important;
  border-color: #EBEEF5 !important;
}

:deep(.el-descriptions__label) {
  color: #2A9D8F !important;
  font-weight: 600 !important;
}

/* 分页样式 */
:deep(.el-pagination) {
  --el-color-primary: #2A9D8F;
}
</style>

