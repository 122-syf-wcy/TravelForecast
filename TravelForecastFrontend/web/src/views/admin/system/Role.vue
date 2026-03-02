<template>
  <div class="role-management">
    <div class="header-section">
      <h2>角色权限管理</h2>
      <p class="subtitle">系统角色与权限配置</p>
    </div>

    <!-- 搜索和操作区 -->
    <div class="filter-section">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item>
          <el-input
            v-model="filterForm.roleName"
            placeholder="搜索角色名称/标识"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
            style="width: 240px"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item>
          <el-select
            v-model="filterForm.status"
            placeholder="状态筛选"
            clearable
            @change="handleSearch"
            style="width: 140px"
          >
            <el-option label="启用" value="active" />
            <el-option label="禁用" value="inactive" />
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </el-form-item>
      </el-form>

      <div class="action-buttons">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          添加角色
        </el-button>
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
          </el-button>
        </div>
      </div>

    <!-- 角色列表 -->
    <div class="table-section">
      <el-table 
        :data="list" 
        style="width: 100%" 
        stripe 
        border
        v-loading="loading"
      >
        <el-table-column prop="id" label="序号" width="80" align="center" />
        
        <el-table-column label="角色名称" min-width="140">
          <template #default="scope">
            <span>{{ scope.row.roleName }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="角色标识" min-width="150">
          <template #default="scope">
            <el-tag :type="getRoleTagType(scope.row.roleCode)" size="small">
              {{ scope.row.roleCode }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="描述" min-width="200" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ getDescription(scope.row.description) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="用户数" width="100" align="center">
          <template #default="scope">
            <span class="user-count">{{ getUserCount(scope.row.userCount) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-switch
              v-model="scope.row.status"
              active-value="active"
              inactive-value="inactive"
              @change="handleStatusChange(scope.row)"
              :disabled="isSystemRole(scope.row.roleCode)"
            />
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              link
              @click="handleView(scope.row)"
            >
              <el-icon><View /></el-icon>
              权限
            </el-button>
            <el-button
              type="primary"
              size="small"
              link
              @click="handleEdit(scope.row)"
            >
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button
              type="danger"
              size="small"
              link
              @click="handleDelete(scope.row)"
              :disabled="isSystemRole(scope.row.roleCode)"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        class="pagination"
      />
  </div>

    <!-- 添加/编辑角色对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="角色名称" prop="roleName">
          <el-input
            v-model="form.roleName"
            placeholder="请输入角色名称"
            clearable
          />
      </el-form-item>

        <el-form-item label="角色标识" prop="roleCode">
          <el-input
            v-model="form.roleCode"
            placeholder="请输入角色标识（如：ADMIN）"
            clearable
            :disabled="isEdit && isSystemRole(form.roleCode)"
          />
      </el-form-item>

        <el-form-item label="角色描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入角色描述"
            clearable
          />
      </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="active">启用</el-radio>
            <el-radio value="inactive">禁用</el-radio>
          </el-radio-group>
      </el-form-item>
    </el-form>

    <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
    </template>
  </el-dialog>

    <!-- 权限配置对话框 -->
    <el-dialog
      v-model="permissionDialogVisible"
      title="权限配置"
      width="700px"
      :close-on-click-modal="false"
    >
      <div v-if="currentRole" class="permission-header">
        <div class="role-info">
          <span class="label">角色：</span>
          <el-tag :type="getRoleTagType(currentRole.roleCode)">
            {{ currentRole.roleName }}
          </el-tag>
        </div>
        <div class="role-desc">{{ currentRole.description }}</div>
      </div>

      <el-alert
        title="提示：勾选权限后，该角色的用户将拥有对应的操作权限"
        type="info"
        :closable="false"
        style="margin: 15px 0"
      />

      <el-tree
        ref="permissionTreeRef"
        :data="permissionTree"
        :props="treeProps"
        show-checkbox
        node-key="id"
        :default-checked-keys="selectedPermissions"
        v-loading="permissionLoading"
      >
        <template #default="{ node, data }">
          <span class="tree-node-label">
            <el-icon style="margin-right: 5px">
              <component :is="getPermissionIcon(data.resourceType)" />
            </el-icon>
            {{ node.label }}
          </span>
        </template>
      </el-tree>

    <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePermissions" :loading="savingPermissions">
          保存权限
        </el-button>
    </template>
  </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  Search,
  Plus,
  Download,
  View,
  Edit,
  Delete,
  Menu,
  Operation,
  Document
} from '@element-plus/icons-vue'
import * as roleApi from '@/api/role'

const loading = ref(false)
const list = ref<any[]>([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const filterForm = reactive({
  roleName: '',
  status: ''
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)

const dialogTitle = computed(() => {
  return isEdit.value ? '编辑角色' : '添加角色'
})

const formRef = ref<FormInstance>()
const form = reactive({
  id: null as number | null,
  roleName: '',
  roleCode: '',
  description: '',
  status: 'active'
})

const validateRoleCode = (rule: any, value: any, callback: any) => {
  if (!value) {
    callback(new Error('请输入角色标识'))
  } else {
    const regex = /^[A-Z_]+$/
    if (!regex.test(value)) {
      callback(new Error('只能包含大写字母和下划线'))
    } else {
      callback()
    }
  }
}

const rules = {
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  roleCode: [
    { required: true, trigger: 'blur', validator: validateRoleCode }
  ]
}

const permissionDialogVisible = ref(false)
const currentRole = ref<any>(null)
const permissionLoading = ref(false)
const savingPermissions = ref(false)
const permissionTreeRef = ref()
const selectedPermissions = ref<number[]>([])

const treeProps = {
  children: 'children',
  label: 'permissionName'
}

const permissionTree = ref([
  {
    id: 1,
    permissionName: '控制台',
    resourceType: 'menu',
    children: [
      { id: 101, permissionName: '数据概览', resourceType: 'page' },
      { id: 102, permissionName: '统计报表', resourceType: 'page' }
    ]
  },
  {
    id: 2,
    permissionName: '数据中心',
    resourceType: 'menu',
    children: [
      { id: 201, permissionName: '景区管理', resourceType: 'page' },
      { id: 202, permissionName: '活动管理', resourceType: 'page' },
      { id: 203, permissionName: '设施管理', resourceType: 'page' }
    ]
  },
  {
    id: 3,
    permissionName: '商户管理',
    resourceType: 'menu',
    children: [
      { id: 301, permissionName: '商户列表', resourceType: 'page' },
      { id: 302, permissionName: '申请审核', resourceType: 'page' },
      { id: 303, permissionName: '审核记录', resourceType: 'page' },
      { id: 304, permissionName: '合同管理', resourceType: 'page' },
      { id: 305, permissionName: '景区申请', resourceType: 'page' }
    ]
  },
  {
    id: 4,
    permissionName: '用户管理',
    resourceType: 'menu',
    children: [
      { id: 401, permissionName: '用户列表', resourceType: 'page' },
      { id: 402, permissionName: '用户分析', resourceType: 'page' }
    ]
  },
  {
    id: 5,
    permissionName: '内容管理',
    resourceType: 'menu',
    children: [
      { id: 501, permissionName: '评论管理', resourceType: 'page' },
      { id: 502, permissionName: '轮播广告', resourceType: 'page' }
    ]
  },
  {
    id: 6,
    permissionName: '系统设置',
    resourceType: 'menu',
    children: [
      { id: 601, permissionName: '角色权限', resourceType: 'page' },
      { id: 602, permissionName: '系统配置', resourceType: 'page' },
      { id: 603, permissionName: '日志管理', resourceType: 'page' }
    ]
  }
])

const getDescription = (desc: string) => {
  return desc ? desc : '-'
}

const getUserCount = (count: number) => {
  return count ? count : 0
}

const loadData = async () => {
  loading.value = true
  try {
    const params: any = {
      page: pagination.page,
      size: pagination.size
    }
    
    if (filterForm.roleName) {
      params.roleName = filterForm.roleName
    }
    
    if (filterForm.status) {
      params.status = filterForm.status
    }
    
    const res = await roleApi.getRoleList(params)
    
    if (res.data) {
      list.value = res.data.records ? res.data.records : []
      pagination.total = res.data.total ? res.data.total : 0
    }
  } catch (error: any) {
    const msg = error && error.message ? error.message : '加载失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handlePageChange = (page: number) => {
  pagination.page = page
  loadData()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  loadData()
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  form.id = row.id
  form.roleName = row.roleName
  form.roleCode = row.roleCode
  form.description = row.description
  form.status = row.status
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  if (isSystemRole(row.roleCode)) {
    ElMessage.warning('系统内置角色不能删除')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除角色 "${row.roleName}" 吗？此操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await roleApi.deleteRole(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      const msg = error && error.message ? error.message : '删除失败'
      ElMessage.error(msg)
    }
  }
}

const handleStatusChange = async (row: any) => {
  try {
    await roleApi.updateRoleStatus(row.id, row.status)
    ElMessage.success('状态更新成功')
  } catch (error: any) {
    const msg = error && error.message ? error.message : '状态更新失败'
    ElMessage.error(msg)
    row.status = row.status === 'active' ? 'inactive' : 'active'
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      const data = {
        roleName: form.roleName,
        roleCode: form.roleCode,
        description: form.description,
        status: form.status
      }

      if (isEdit.value && form.id) {
        await roleApi.updateRole(form.id, data)
        ElMessage.success('更新成功')
      } else {
        await roleApi.createRole(data)
        ElMessage.success('创建成功')
      }

      dialogVisible.value = false
      loadData()
    } catch (error: any) {
      const msg = error && error.message ? error.message : '操作失败'
      ElMessage.error(msg)
    } finally {
      submitting.value = false
    }
  })
}

const handleView = async (row: any) => {
  currentRole.value = row
  permissionDialogVisible.value = true
  
  permissionLoading.value = true
  try {
    const res = await roleApi.getRolePermissions(row.id)
    selectedPermissions.value = res.data ? res.data : []
  } catch (error: any) {
    const msg = error && error.message ? error.message : '加载权限失败'
    ElMessage.error(msg)
  } finally {
    permissionLoading.value = false
  }
}

const handleSavePermissions = async () => {
  if (!currentRole.value) return
  if (!permissionTreeRef.value) return

  savingPermissions.value = true
  try {
    const checkedKeys = permissionTreeRef.value.getCheckedKeys()
    const halfCheckedKeys = permissionTreeRef.value.getHalfCheckedKeys()
    const allPermissionIds = [...checkedKeys, ...halfCheckedKeys]

    await roleApi.saveRolePermissions(currentRole.value.id, allPermissionIds)
    ElMessage.success('权限保存成功')
    permissionDialogVisible.value = false
  } catch (error: any) {
    const msg = error && error.message ? error.message : '保存失败'
    ElMessage.error(msg)
  } finally {
    savingPermissions.value = false
  }
}

const handleExport = () => {
  // 导出角色列表为CSV
  const headers = ['角色名称', '角色编码', '描述', '状态', '用户数']
  const rows = list.value.map((r: any) => [
    r.roleName, r.roleCode, r.description || '', r.status === 'active' ? '启用' : '禁用', r.userCount || 0
  ])
  const csv = [headers.join(','), ...rows.map((r: any) => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `角色列表_${new Date().toISOString().split('T')[0]}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
  ElMessage.success('导出成功')
}

const resetForm = () => {
  form.id = null
  form.roleName = ''
  form.roleCode = ''
  form.description = ''
  form.status = 'active'
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

const isSystemRole = (roleCode: string) => {
  const systemRoles = ['ADMIN', 'BUSINESS', 'USER']
  return systemRoles.includes(roleCode)
}

const getRoleTagType = (roleCode: string) => {
  if (roleCode === 'ADMIN') return 'danger'
  if (roleCode === 'BUSINESS') return 'success'
  if (roleCode === 'USER') return 'info'
  return 'primary'
}

const getPermissionIcon = (resourceType: string) => {
  if (resourceType === 'menu') return Menu
  if (resourceType === 'page') return Document
  if (resourceType === 'operation') return Operation
  return Document
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.role-management {
  padding: 20px;
  background: #F5F7FA;
  min-height: calc(100vh - 120px);
  color: #2C3E50;
}

.header-section {
  margin-bottom: 24px;
}

.header-section h2 {
  font-size: 24px;
  font-weight: 600;
  color: #2A9D8F;
  margin-bottom: 8px;
}

.header-section .subtitle {
  font-size: 14px;
  color: #94A3B8;
}

.filter-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding: 20px;
  background: rgba(42, 157, 143, 0.03);
  border: 1px solid #EBEEF5;
  border-radius: 8px;
}

.filter-section .filter-form {
  flex: 1;
}

.filter-section .action-buttons {
  display: flex;
  gap: 10px;
}

.table-section {
  background: rgba(42, 157, 143, 0.03);
  border: 1px solid #EBEEF5;
  border-radius: 8px;
  padding: 20px;
}

.user-count {
  color: #2A9D8F;
  font-weight: 600;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.permission-header {
  margin-bottom: 20px;
  padding: 15px;
  background: rgba(42, 157, 143, 0.03);
  border-radius: 8px;
}

.permission-header .role-info {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.permission-header .role-info .label {
  font-weight: 600;
  margin-right: 10px;
}

.permission-header .role-desc {
  font-size: 14px;
  color: #94A3B8;
}

.tree-node-label {
  display: flex;
  align-items: center;
  font-size: 14px;
}

:deep(.el-table) {
  background-color: transparent !important;
  color: #303133 !important;
}

:deep(.el-table th),
:deep(.el-table tr),
:deep(.el-table td) {
  background-color: transparent !important;
  color: #303133 !important;
  border-color: #DCDFE6 !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background-color: rgba(42, 157, 143, 0.03) !important;
}

:deep(.el-table__body tr:hover > td) {
  background-color: rgba(42, 157, 143, 0.1) !important;
}

:deep(.el-pagination .el-pagination__total),
:deep(.el-pagination .el-pagination__jump) {
  color: #2C3E50;
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next),
:deep(.el-pagination .el-pager li) {
  background-color: rgba(42, 157, 143, 0.1);
  color: #2C3E50;
  border: 1px solid #EBEEF5;
}

:deep(.el-pagination .btn-prev:hover),
:deep(.el-pagination .btn-next:hover),
:deep(.el-pagination .el-pager li:hover) {
  color: #2A9D8F;
}

:deep(.el-pagination .el-pager li.is-active) {
  background-color: #2A9D8F;
  color: #FFFFFF;
}

:deep(.el-input__wrapper) {
  background-color: #F5F7FA !important;
  border: 1px solid #DCDFE6 !important;
  color: #303133 !important;
}

:deep(.el-input__inner) {
  color: #303133 !important;
}

:deep(.el-select .el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 1px rgba(42, 157, 143, 0.4) !important;
}

:deep(.el-dialog) {
  background: #F5F7FA;
  border: 1px solid #EBEEF5;
}

:deep(.el-dialog__title) {
  color: #2A9D8F;
}

:deep(.el-form-item__label) {
  color: #303133 !important;
}

:deep(.el-textarea__inner) {
  background-color: #F5F7FA !important;
  border: 1px solid #DCDFE6 !important;
  color: #303133 !important;
}

:deep(.el-tree) {
  background-color: transparent !important;
  color: #303133 !important;
}

:deep(.el-tree .el-tree-node__content) {
  color: #303133 !important;
}

:deep(.el-tree .el-tree-node__content:hover) {
  background-color: rgba(42, 157, 143, 0.1) !important;
}
</style>
