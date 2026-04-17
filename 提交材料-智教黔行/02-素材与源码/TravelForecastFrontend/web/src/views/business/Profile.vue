<template>
  <div class="profile-page">
    <el-card class="bg-white border border-gray-200 shadow-lg">
      <template #header>
        <div class="flex items-center justify-between">
          <h3 class="text-lg text-[#2A9D8F] font-bold">商家资料</h3>
          <div>
            <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
            <el-button @click="handleReset">重置</el-button>
          </div>
        </div>
      </template>
      <!-- 账户信息区域 -->
      <div class="mb-6 p-4 bg-gray-50 rounded-lg border border-gray-100">
        <div class="flex items-center justify-between">
          <div>
            <h4 class="text-gray-800 text-lg mb-2">账户信息</h4>
            <p class="text-gray-500 text-sm">注册时间：{{ formatDate(accountInfo.createdAt) }}</p>
            <p class="text-gray-500 text-sm">最后登录：{{ formatDate(accountInfo.loginAt) }}</p>
          </div>
          <el-tag :type="accountInfo.status === 'active' ? 'success' : 'warning'" size="large">
            {{ accountInfo.status === 'active' ? '账户正常' : '待审核' }}
          </el-tag>
        </div>
      </div>

      <!-- 景区选择提示 -->
      <div class="mb-4">
        <el-alert type="warning" show-icon>
          <span>请先在下方管理景区处选择您管理的景区，若看不到该字段请按 Ctrl+F5 强制刷新</span>
        </el-alert>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="商户名称" prop="businessName">
          <el-input v-model="form.businessName" placeholder="请输入商户名称" />
        </el-form-item>
        <el-form-item label="商户类型" prop="category">
          <el-select v-model="form.category" placeholder="请选择类型">
            <el-option label="酒店" value="hotel" />
            <el-option label="餐饮" value="restaurant" />
            <el-option label="景区" value="scenic" />
            <el-option label="购物" value="shop" />
          </el-select>
        </el-form-item>
        <el-form-item label="管理景区" prop="scenicId">
          <div class="flex gap-2">
            <el-select v-model="form.scenicId" placeholder="请选择您管理的景区" filterable class="flex-1">
              <el-option 
                v-for="scenic in scenicList" 
                :key="scenic.id" 
                :label="scenic.name" 
                :value="scenic.id"
                :disabled="scenic.isFull"
              >
                <div class="flex items-center justify-between">
                  <span>{{ scenic.name }}</span>
                  <span style="color: #8492a6; font-size: 13px">
                    {{ scenic.city }}
                    <span v-if="scenic.isFull" class="text-red-400">（已满）</span>
                    <span v-else class="text-green-400">（{{ scenic.currentMerchants }}/{{ scenic.maxMerchants }}个）</span>
                  </span>
                </div>
              </el-option>
            </el-select>
            <el-button @click="loadScenicList" type="primary">刷新列表</el-button>
          </div>
          <div class="text-gray-500 text-xs mt-1">
            <span v-if="scenicList.length > 0">已加载 {{ scenicList.length }} 个可申请景区</span>
            <span v-else class="text-red-400">暂无可申请景区，请联系管理员</span>
          </div>
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="form.contact" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="营业执照号" prop="licenseNo">
          <el-input v-model="form.licenseNo" placeholder="请输入营业执照号" />
        </el-form-item>
        <el-form-item label="营业执照文件">
          <el-upload
            class="license-uploader"
            :action="uploadAction"
            :headers="{ Authorization: `Bearer ${token}` }"
            :show-file-list="false"
            :on-success="handleLicenseSuccess"
            :on-error="handleLicenseError"
            :before-upload="beforeLicenseUpload"
            accept="image/*,.pdf"
          >
            <el-image 
              v-if="form.licenseUrl" 
              :src="form.licenseUrl" 
              fit="contain"
              class="license-preview"
              :preview-src-list="[form.licenseUrl]"
            />
            <div v-else class="upload-placeholder">
              <el-icon class="upload-icon"><Upload /></el-icon>
              <div class="upload-text">点击上传营业执照</div>
              <div class="upload-tip">支持图片或PDF文件，大小不超过10MB</div>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入详细地址" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 合同管理区域 -->
    <el-card class="bg-white border border-gray-200 shadow-lg mt-4">
      <template #header>
        <div class="flex items-center justify-between">
          <h3 class="text-lg text-[#2A9D8F] font-bold">合同管理</h3>
          <el-button type="primary" @click="showContractDialog = true">
            <el-icon class="mr-1"><Plus /></el-icon>
            添加合同
          </el-button>
        </div>
      </template>
      
      <el-table :data="contracts" style="width: 100%" :row-style="{ background: 'transparent' }">
        <el-table-column prop="contractNo" label="合同编号" min-width="120" show-overflow-tooltip />
        <el-table-column prop="contractName" label="合同名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="startDate" label="开始日期" width="110" />
        <el-table-column prop="endDate" label="结束日期" width="110" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="viewContract(row)">查看</el-button>
            <el-button size="small" type="warning" link @click="editContract(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="deleteContract(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 合同编辑对话框 -->
    <el-dialog 
      v-model="showContractDialog" 
      :title="currentContract?.id ? '编辑合同' : '添加合同'"
      width="600px"
    >
      <el-form ref="contractFormRef" :model="contractForm" :rules="contractRules" label-width="100px">
        <el-form-item label="合同编号" prop="contractNo">
          <el-input v-model="contractForm.contractNo" placeholder="请输入合同编号" />
        </el-form-item>
        <el-form-item label="合同名称" prop="contractName">
          <el-input v-model="contractForm.contractName" placeholder="请输入合同名称" />
        </el-form-item>
        <el-form-item label="合同文件" prop="contractUrl">
          <el-upload
            class="contract-uploader"
            :action="uploadAction"
            :headers="{ Authorization: `Bearer ${token}` }"
            :show-file-list="false"
            :on-success="handleContractFileSuccess"
            :on-error="handleContractFileError"
            :before-upload="beforeContractUpload"
            accept=".pdf,.doc,.docx"
          >
            <div v-if="contractForm.contractUrl" class="contract-file-info">
              <el-icon class="file-icon"><Document /></el-icon>
              <span class="file-name">{{ contractForm.contractName || '合同文件' }}</span>
              <el-button type="primary" size="small">重新上传</el-button>
            </div>
            <div v-else class="upload-placeholder-small">
              <el-icon class="upload-icon-small"><Upload /></el-icon>
              <div class="upload-text-small">点击上传合同文件</div>
              <div class="upload-tip-small">支持PDF、Word文件，大小不超过20MB</div>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="contractForm.startDate"
            type="date"
            placeholder="选择开始日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker
            v-model="contractForm.endDate"
            type="date"
            placeholder="选择结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="contractForm.remarks"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showContractDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingContract" @click="saveContract">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Plus, Document } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const saving = ref(false)
const scenicList = ref<any[]>([])
const token = computed(() => localStorage.getItem('token') || '')
// 使用相对路径，Vite 会自动通过代理转发到后端
const uploadAction = '/api/upload/file'

// 账户信息
const accountInfo = ref({
  createdAt: '',
  loginAt: '',
  status: 'active'
})

const form = ref<any>({
  id: undefined,
  userId: userStore.userInfo?.id,
  businessName: '',
  category: '',
  scenicId: null,
  contact: '',
  phone: '',
  licenseNo: '',
  licenseUrl: '',
  address: ''
})

const rules: FormRules = {
  businessName: [{ required: true, message: '请输入商户名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择商户类型', trigger: 'change' }],
  scenicId: [{ required: true, message: '请选择管理的景区', trigger: 'change' }],
  contact: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  licenseNo: [{ required: true, message: '请输入营业执照号', trigger: 'blur' }]
}

// 加载景区列表（从商家可申请列表获取）
const loadScenicList = async () => {
  try {
    console.log('开始加载可申请景区列表...')
    const res: any = await request({ url: '/merchant/scenics/available', method: 'get' })
    console.log('可申请景区API响应:', res)
    const data = res?.data || res
    scenicList.value = Array.isArray(data) ? data : (data?.list || data?.records || [])
    console.log('加载到的景区列表:', scenicList.value)
    
    if (scenicList.value.length > 0) {
      ElMessage.success(`成功加载 ${scenicList.value.length} 个可申请景区`)
    } else {
      ElMessage.warning('暂无可申请景区，请联系管理员开放景区')
    }
  } catch (error: any) {
    console.error('加载景区列表失败:', error)
    ElMessage.error('加载景区列表失败: ' + (error?.message || '未知错误'))
  }
}

const loadProfile = async () => {
  if (!userStore.userInfo?.id) return
  
  // 加载商家资料
  const res: any = await request({ url: `/merchant/profiles/by-user/${userStore.userInfo.id}`, method: 'get' })
  if (res?.data) {
    form.value = { ...form.value, ...res.data }
  }
  
  // 加载账户信息（包括注册时间和最后登录时间）
  try {
    const userRes: any = await request({ url: `/users/${userStore.userInfo.id}`, method: 'get' })
    if (userRes?.data) {
      accountInfo.value = {
        createdAt: userRes.data.createdAt || '',
        loginAt: userRes.data.loginAt || '',
        status: userRes.data.status || 'active'
      }
    }
  } catch (error) {
    console.error('加载账户信息失败:', error)
  }
}

// 营业执照上传前的校验
const beforeLicenseUpload = (file: any) => {
  const isImage = file.type.startsWith('image/')
  const isPDF = file.type === 'application/pdf'
  const isValidType = isImage || isPDF
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isValidType) {
    ElMessage.error('只能上传图片或PDF文件')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }
  return true
}

// 营业执照上传成功
const handleLicenseSuccess = (response: any) => {
  if (response.code === 200 || response.code === 1) {
    form.value.licenseUrl = response.data.url || response.data
    ElMessage.success('营业执照上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 营业执照上传失败
const handleLicenseError = (error: any) => {
  console.error('上传失败:', error)
  ElMessage.error('上传失败，请重试')
}

const handleSave = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form.value, userId: userStore.userInfo?.id }
    await request({ url: '/merchant/profiles', method: 'post', data: payload })
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

const handleReset = () => loadProfile()

// 合同管理相关
const contracts = ref<any[]>([])
const showContractDialog = ref(false)
const contractFormRef = ref<FormInstance>()
const savingContract = ref(false)
const currentContract = ref<any>(null)

const contractForm = ref({
  contractNo: '',
  contractName: '',
  contractUrl: '',
  startDate: '',
  endDate: '',
  remarks: ''
})

const contractRules: FormRules = {
  contractNo: [{ required: true, message: '请输入合同编号', trigger: 'blur' }],
  contractName: [{ required: true, message: '请输入合同名称', trigger: 'blur' }],
  contractUrl: [{ required: true, message: '请上传合同文件', trigger: 'change' }]
}

// 加载合同列表
const loadContracts = async () => {
  try {
    const res: any = await request({ url: '/merchant/contracts', method: 'get' })
    contracts.value = res?.data || []
  } catch (error: any) {
    console.error('加载合同列表失败:', error)
  }
}

// 合同文件上传前校验
const beforeContractUpload = (file: any) => {
  const isPDF = file.type === 'application/pdf'
  const isDoc = file.type === 'application/msword' || file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  const isValidType = isPDF || isDoc
  const isLt20M = file.size / 1024 / 1024 < 20

  if (!isValidType) {
    ElMessage.error('只能上传PDF或Word文件')
    return false
  }
  if (!isLt20M) {
    ElMessage.error('文件大小不能超过 20MB')
    return false
  }
  return true
}

// 合同文件上传成功
const handleContractFileSuccess = (response: any) => {
  if (response.code === 200 || response.code === 1) {
    contractForm.value.contractUrl = response.data.url || response.data
    ElMessage.success('合同文件上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 合同文件上传失败
const handleContractFileError = (error: any) => {
  console.error('上传失败:', error)
  ElMessage.error('上传失败，请重试')
}

// 查看合同
const viewContract = (contract: any) => {
  if (contract.contractUrl) {
    window.open(contract.contractUrl, '_blank')
  } else {
    ElMessage.warning('暂无合同文件')
  }
}

// 编辑合同
const editContract = (contract: any) => {
  currentContract.value = contract
  contractForm.value = {
    contractNo: contract.contractNo,
    contractName: contract.contractName,
    contractUrl: contract.contractUrl,
    startDate: contract.startDate,
    endDate: contract.endDate,
    remarks: contract.remarks || ''
  }
  showContractDialog.value = true
}

// 删除合同
const deleteContract = async (contract: any) => {
  try {
    await ElMessageBox.confirm('确认删除该合同吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await request({ 
      url: `/merchant/contracts/${contract.id}`, 
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

// 保存合同
const saveContract = async () => {
  if (!contractFormRef.value) return
  
  try {
    const valid = await contractFormRef.value.validate()
    if (!valid) return
    
    savingContract.value = true
    
    if (currentContract.value?.id) {
      // 更新
      await request({
        url: `/merchant/contracts/${currentContract.value.id}`,
        method: 'put',
        data: contractForm.value
      })
      ElMessage.success('更新成功')
    } else {
      // 新增
      await request({
        url: '/merchant/contracts',
        method: 'post',
        data: contractForm.value
      })
      ElMessage.success('添加成功')
    }
    
    showContractDialog.value = false
    resetContractForm()
    loadContracts()
  } catch (error: any) {
    ElMessage.error('保存失败: ' + (error?.message || '未知错误'))
  } finally {
    savingContract.value = false
  }
}

// 重置合同表单
const resetContractForm = () => {
  currentContract.value = null
  contractForm.value = {
    contractNo: '',
    contractName: '',
    contractUrl: '',
    startDate: '',
    endDate: '',
    remarks: ''
  }
  contractFormRef.value?.resetFields()
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

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return '未知'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadScenicList()
  loadProfile()
  loadContracts()
})
</script>

<style scoped>
.profile-page { color: #2C3E50; }

/* 浅色主题：表单输入框 */
:deep(.el-select),
:deep(.el-select .el-input),
:deep(.el-select .el-input__wrapper),
:deep(.el-input__wrapper) {
  background-color: #FFFFFF !important;
  background: #FFFFFF !important;
  border-color: #DCDFE6 !important;
  box-shadow: none !important;
}

:deep(.el-select .el-input__wrapper:hover),
:deep(.el-select .el-input__wrapper.is-focus),
:deep(.el-input__wrapper:hover) {
  background-color: #FFFFFF !important;
  background: #FFFFFF !important;
  border-color: #2A9D8F !important;
}

:deep(.el-select .el-input__inner),
:deep(.el-input__inner),
:deep(input) {
  background-color: transparent !important;
  background: transparent !important;
  color: #2C3E50 !important;
}

:deep(.el-select .el-input__inner::placeholder),
:deep(.el-input__inner::placeholder),
:deep(input::placeholder) {
  color: #A8ABB2 !important;
}

:deep(.el-select-dropdown) {
  background-color: #FFFFFF !important;
  border: 1px solid #EBEEF5 !important;
}

:deep(.el-select-dropdown__item) {
  color: #606266 !important;
  background-color: transparent !important;
}

:deep(.el-select-dropdown__item > div) {
  background-color: transparent !important;
}

:deep(.el-select-dropdown__item span) {
  color: #606266 !important;
}

:deep(.el-select-dropdown__item.hover),
:deep(.el-select-dropdown__item:hover) {
  background: #F5F7FA !important;
}

:deep(.el-select-dropdown__item.selected) {
  background: rgba(42, 157, 143, 0.1) !important;
  color: #2A9D8F !important;
}

:deep(.el-select-dropdown__item.selected span) {
  color: #2A9D8F !important;
}

/* 下拉面板滚动条 */
:deep(.el-select-dropdown .el-scrollbar__bar.is-horizontal > div),
:deep(.el-select-dropdown .el-scrollbar__bar.is-vertical > div) {
  background-color: rgba(226, 232, 240, 0.4) !important;
}

/* 营业执照上传样式 */
.license-uploader {
  border: 2px dashed #DCDFE6;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.3s;
}

.license-uploader:hover {
  border-color: #2A9D8F;
}

.license-preview {
  width: 300px;
  height: 200px;
  display: block;
  object-fit: contain;
}

.upload-placeholder {
  width: 300px;
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #606266;
}

.upload-icon {
  font-size: 48px;
  color: #C0C4CC;
  margin-bottom: 12px;
}

.upload-text {
  font-size: 14px;
  margin-bottom: 4px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
}

/* 合同上传样式 */
.contract-uploader {
  width: 100%;
}

.contract-file-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #EBEEF5;
  border-radius: 4px;
  background: #FAFAFA;
}

.file-icon {
  font-size: 32px;
  color: #2A9D8F;
}

.file-name {
  flex: 1;
  color: #303133;
}

.upload-placeholder-small {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  border: 2px dashed #DCDFE6;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-placeholder-small:hover {
  border-color: #2A9D8F;
  background: rgba(42, 157, 143, 0.05);
}

.upload-icon-small {
  font-size: 32px;
  color: #C0C4CC;
  margin-bottom: 8px;
}

.upload-text-small {
  font-size: 14px;
  color: #606266;
  margin-bottom: 4px;
}

.upload-tip-small {
  font-size: 12px;
  color: #909399;
}

/* 表格浅色主题 */
:deep(.el-table) {
  background-color: #FFFFFF !important;
  color: #606266 !important;
}

:deep(.el-table th),
:deep(.el-table tr),
:deep(.el-table td) {
  background-color: transparent !important;
  color: #606266 !important;
  border-color: #EBEEF5 !important;
}

:deep(.el-table__body tr:hover > td) {
  background-color: #F5F7FA !important;
}

/* 日期选择器 */
:deep(.el-date-editor) {
  background-color: #FFFFFF !important;
  border-color: #DCDFE6 !important;
}

:deep(.el-date-editor .el-input__wrapper) {
  background-color: transparent !important;
  box-shadow: none !important;
}

:deep(.el-date-editor .el-input__inner) {
  color: #2C3E50 !important;
}

/* 表单标签 */
:deep(.el-form-item__label) {
  color: #606266 !important;
}
</style>
