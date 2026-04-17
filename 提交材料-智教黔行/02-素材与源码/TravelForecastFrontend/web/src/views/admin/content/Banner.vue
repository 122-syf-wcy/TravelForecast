<template>
  <div class="banner-container p-6">
    <div class="page-header mb-6">
      <h2 class="text-2xl font-bold text-gray-800">轮播广告管理</h2>
      <p class="text-gray-500 mt-1">首页轮播图配置与管理</p>
    </div>

    <holographic-card class="mb-6">
      <div class="flex justify-between items-center">
        <div></div>
        <div class="space-x-2">
          <el-button type="primary" @click="openAdd()">
            <i class="ri-add-line mr-1"></i>添加轮播图
          </el-button>
          <el-button @click="openExport()">
            <i class="ri-download-2-line mr-1"></i>导出
          </el-button>
        </div>
      </div>
    </holographic-card>

    <holographic-card title="轮播图列表" icon="image-2-line">
      <el-table :data="bannerList" style="width: 100%">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="image" label="图片预览" width="220">
          <template #default="{ row }">
            <img :src="row.image" 
                 style="width: 150px; height: 80px; object-fit: cover; border-radius: 6px; background:#F5F7FA" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{ row }">
            <span class="text-gray-800">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="100">
          <template #default="{ row }">
            <span class="text-[#2A9D8F]">{{ row.sort }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="changeStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleEdit(row)">
              <i class="ri-edit-line mr-1"></i>编辑
            </el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)">
              <i class="ri-delete-bin-line mr-1"></i>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </holographic-card>

    <!-- 新增/编辑表单 -->
    <el-dialog v-model="editVisible" :title="editForm.id ? '编辑轮播图' : '添加轮播图'" width="600px" :close-on-click-modal="false">
    <el-form :model="editForm" label-width="90px" ref="editRef" :rules="rules">
      <el-form-item label="标题" prop="title">
        <el-input v-model="editForm.title" placeholder="请输入标题" />
      </el-form-item>
      <el-form-item label="图片" prop="image">
        <el-upload
          :http-request="uploadBanner"
          :show-file-list="false"
          list-type="picture-card">
          <template #default>
            <el-icon v-if="!editForm.image"><Plus /></el-icon>
            <img v-else :src="editPreview || editForm.image" style="width:100px;height:100px;object-fit:cover;border-radius:6px" />
          </template>
        </el-upload>
      </el-form-item>
      <el-form-item label="跳转链接">
        <el-input v-model="editForm.link" placeholder="可选，如 /#/landing" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="editForm.sort" :min="1" />
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="editForm.enabled" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editVisible=false">取消</el-button>
      <el-button type="primary" @click="submitEdit">保存</el-button>
    </template>
  </el-dialog>

  <!-- 导出参数表单 -->
  <el-dialog v-model="exportVisible" title="导出轮播图" width="520px">
    <el-form :model="exportForm" label-width="100px">
      <el-form-item label="导出格式">
        <el-radio-group v-model="exportForm.format">
          <el-radio label="xlsx">Excel (.xlsx)</el-radio>
          <el-radio label="csv">CSV (.csv)</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="仅导出启用">
        <el-switch v-model="exportForm.enabledOnly" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="exportVisible=false">取消</el-button>
      <el-button type="primary" @click="submitExport">导出</el-button>
    </template>
  </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import HolographicCard from '@/components/HolographicCard.vue'
import { contentApi, clearContentCache } from '@/api/content'
import { Plus } from '@element-plus/icons-vue'

const bannerList = ref<any[]>([])
// 仅表单内预览（签名URL），数据库只保存短公共URL
const editPreview = ref('')

// 表单
const editVisible = ref(false)
const editRef = ref()
const editForm = ref<any>({ id: 0, title: '', image: '', link: '', sort: 1, enabled: true })
const rules = { title: [{ required: true, message: '请输入标题', trigger: 'blur' }] }
const openAdd = () => { editForm.value = { id: 0, title: '', image: '', link: '', sort: 1, enabled: true }; editPreview.value=''; editVisible.value = true }
const handleEdit = (row: any) => { editForm.value = { ...row }; editPreview.value=''; editVisible.value = true }
const submitEdit = async () => { await editRef.value?.validate?.();
  console.log('💾 准备保存, editForm:', JSON.parse(JSON.stringify(editForm.value)))
  const payload = { id: editForm.value.id || undefined, title: editForm.value.title, image: editForm.value.image, link: editForm.value.link, sort: editForm.value.sort, enabled: !!editForm.value.enabled }
  console.log('💾 payload:', payload)
  await contentApi.saveBanner(payload)
  clearContentCache()
  ElMessage.success('已保存')
  editVisible.value = false
  await loadList()
}
const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确认删除"${row.title}"？`, '确认删除', { type: 'warning' })
    .then(async () => { await contentApi.deleteBanner(row.id); clearContentCache(); ElMessage.success('删除成功'); await loadList() })
}

// 自定义上传到 OSS
const uploadBanner = async (option: any) => {
  const { file } = option
  const form = new FormData()
  form.append('file', file)
  form.append('title', editForm.value.title || 'banner')
  form.append('sort', String(editForm.value.sort || 0))
  try {
    const res: any = await request({ url: '/oss/upload/banner', method: 'post', data: form, headers: { 'Content-Type': 'multipart/form-data' } })
    console.log('📤 OSS上传响应:', res)
    const data = res?.data || res
    console.log('📤 提取的data:', data)
    // 数据库仅存公共短链，表单内预览使用签名地址
    const publicUrl = data?.url || ''
    const previewUrl = data?.signedUrl || publicUrl
    console.log('📤 设置image URL(保存公共URL):', publicUrl)
    editForm.value.image = publicUrl
    editPreview.value = previewUrl
    console.log('📤 当前editForm:', JSON.parse(JSON.stringify(editForm.value)))
    ElMessage.success('已上传至OSS')
  } catch (e) {
    console.error('❌ OSS上传失败:', e)
    ElMessage.error('上传失败')
  }
}

// 导出
const exportVisible = ref(false)
const exportForm = ref({ format: 'xlsx', enabledOnly: true })
const openExport = () => { exportVisible.value = true }
const submitExport = async () => { exportVisible.value = false; ElMessage.success('请在后续版本中使用导出功能') }

const loadList = async () => {
  const res: any = await contentApi.pageBanners({ page: 1, size: 100 })
  const data = res?.data || res
  console.log('📥 后端返回的分页数据:', data)
  bannerList.value = data?.records || data?.list || []
  console.log('📥 渲染的列表数据:', bannerList.value)
}

onMounted(() => {
  loadList()
})

const changeStatus = async (row: any) => {
  await contentApi.saveBanner({ id: row.id, enabled: !!row.enabled })
  clearContentCache()
  ElMessage.success(row.enabled ? '已启用' : '已停用')
}
</script>

<style scoped>
:deep(.el-table) { 
  background: transparent !important; 
  --el-table-border-color: rgba(42, 157, 143, 0.2);
  --el-table-header-bg-color: #F5F7FA;
  --el-table-row-hover-bg-color: rgba(42, 157, 143, 0.05);
  --el-table-tr-bg-color: transparent;
  color: #303133;
}

:deep(.el-table th) { 
  background: #F5F7FA !important; 
  border-bottom: 2px solid rgba(42, 157, 143, 0.4) !important;
  color: #2A9D8F;
  font-weight: bold;
}

:deep(.el-table td) { 
  border-bottom: 1px solid rgba(42, 157, 143, 0.1) !important; 
  padding: 16px 0;
}

:deep(.el-table__inner-wrapper::before) {
  display: none;
}

:deep(.el-upload--picture-card) {
  background: #F5F7FA;
  border: 1px dashed #DCDFE6;
  border-radius: 8px;
}

:deep(.el-upload--picture-card:hover) {
  border-color: #2A9D8F;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

:deep(.el-dialog) {
  background: #FFFFFF;
  border: 1px solid #EBEEF5;
  box-shadow: 0 0 30px rgba(255, 255, 255, 0.98), inset 0 0 20px rgba(42, 157, 143, 0.1);
}

:deep(.el-dialog__title) {
  color: #2A9D8F;
  }
</style>

