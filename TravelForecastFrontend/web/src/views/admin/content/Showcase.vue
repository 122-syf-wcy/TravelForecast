<template>
  <div class="showcase-container p-6">
    <div class="page-header mb-6">
      <h2 class="text-2xl font-bold text-gray-800">实景预览管理</h2>
      <p class="text-gray-500 mt-1">首页实景预览配置（支持图片和视频）</p>
    </div>

    <holographic-card class="mb-6">
      <div class="flex justify-between items-center">
        <div></div>
        <el-button type="primary" @click="openAdd()">
          <i class="ri-add-line mr-1"></i>添加实景
        </el-button>
      </div>
    </holographic-card>

    <holographic-card title="实景列表" icon="film-line">
      <el-table :data="list" style="width: 100%">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="cover" label="封面/预览" width="180">
          <template #default="{ row }">
            <div class="media-preview">
              <img v-if="row.type === 'image'" :src="row.url" class="preview-img" />
              <div v-else class="video-thumb">
                <img :src="row.cover || row.url" class="preview-img" />
                <i class="ri-play-circle-fill play-icon"></i>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="150">
          <template #default="{ row }">
            <span class="text-gray-800">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 'video' ? 'warning' : 'success'" size="small">
              {{ row.type === 'video' ? '视频' : '图片' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80">
          <template #default="{ row }">
            <span class="text-[#2A9D8F]">{{ row.sort }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="80">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="changeStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </holographic-card>

    <!-- 新增/编辑表单 -->
    <el-dialog v-model="editVisible" :title="editForm.id ? '编辑实景' : '添加实景'" width="600px" :close-on-click-modal="false">
    <el-form :model="editForm" label-width="90px" ref="editRef" :rules="rules">
      <el-form-item label="标题" prop="title">
        <el-input v-model="editForm.title" placeholder="请输入标题" />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-radio-group v-model="editForm.type">
          <el-radio label="image">图片</el-radio>
          <el-radio label="video">视频</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="editForm.type === 'video' ? '视频' : '图片'" prop="url">
        <el-upload :http-request="uploadMedia" :show-file-list="false" :accept="editForm.type === 'video' ? 'video/*' : 'image/*'">
          <template #trigger>
            <el-button type="primary">上传{{ editForm.type === 'video' ? '视频' : '图片' }}</el-button>
          </template>
        </el-upload>
        <div v-if="editForm.url" class="mt-2">
          <img v-if="editForm.type === 'image'" :src="previewUrl || editForm.url" style="max-width:200px;border-radius:8px" />
          <video v-else :src="previewUrl || editForm.url" controls style="max-width:300px;border-radius:8px"></video>
        </div>
      </el-form-item>
      <el-form-item v-if="editForm.type === 'video'" label="封面图">
        <el-upload :http-request="uploadCover" :show-file-list="false" accept="image/*">
          <template #trigger>
            <el-button>上传封面</el-button>
          </template>
        </el-upload>
        <img v-if="editForm.cover" :src="coverPreview || editForm.cover" style="max-width:150px;margin-top:8px;border-radius:8px" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="editForm.description" type="textarea" :rows="2" placeholder="可选描述" />
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
      <el-button type="primary" @click="submitEdit" :loading="saving">保存</el-button>
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

const list = ref<any[]>([])
const editVisible = ref(false)
const editRef = ref()
const saving = ref(false)
const previewUrl = ref('')
const coverPreview = ref('')

const editForm = ref<any>({ id: 0, title: '', type: 'image', url: '', cover: '', description: '', sort: 1, enabled: true })
const rules = { 
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  url: [{ required: true, message: '请上传图片或视频', trigger: 'change' }]
}

const openAdd = () => { 
  editForm.value = { id: 0, title: '', type: 'image', url: '', cover: '', description: '', sort: 1, enabled: true }
  previewUrl.value = ''
  coverPreview.value = ''
  editVisible.value = true 
}

const handleEdit = (row: any) => { 
  editForm.value = { ...row }
  previewUrl.value = ''
  coverPreview.value = ''
  editVisible.value = true 
}

const submitEdit = async () => { 
  await editRef.value?.validate?.()
  saving.value = true
  try {
    await contentApi.saveShowcase(editForm.value)
    clearContentCache()
    ElMessage.success('已保存')
    editVisible.value = false
    await loadList()
  } finally {
    saving.value = false
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确认删除"${row.title}"？`, '确认删除', { type: 'warning' })
    .then(async () => { 
      await contentApi.deleteShowcase(row.id)
      clearContentCache()
      ElMessage.success('删除成功')
      await loadList() 
    })
}

const uploadMedia = async (option: any) => {
  const form = new FormData()
  form.append('file', option.file)
  form.append('type', editForm.value.type)
  try {
    const res: any = await request({ url: '/oss/upload/showcase', method: 'post', data: form, headers: { 'Content-Type': 'multipart/form-data' }, timeout: 120000 })
    const data = res?.data || res
    editForm.value.url = data?.url || ''
    previewUrl.value = data?.signedUrl || data?.url || ''
    ElMessage.success('上传成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '上传失败，请检查文件大小或网络')
  }
}

const uploadCover = async (option: any) => {
  const form = new FormData()
  form.append('file', option.file)
  try {
    const res: any = await request({ url: '/oss/upload/showcase', method: 'post', data: form, headers: { 'Content-Type': 'multipart/form-data' } })
    const data = res?.data || res
    editForm.value.cover = data?.url || ''
    coverPreview.value = data?.signedUrl || data?.url || ''
    ElMessage.success('封面上传成功')
  } catch (e) {
    ElMessage.error('封面上传失败')
  }
}

const loadList = async () => {
  try {
    const res: any = await contentApi.pageShowcases({ page: 1, size: 100 })
    const data = res?.data || res
    list.value = data?.records || data?.list || data || []
  } catch (e) {
    list.value = []
  }
}

const changeStatus = async (row: any) => {
  await contentApi.saveShowcase({ id: row.id, enabled: !!row.enabled })
  clearContentCache()
  ElMessage.success(row.enabled ? '已启用' : '已停用')
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.media-preview { position: relative; width: 120px; height: 80px; }
.preview-img { width: 100%; height: 100%; object-fit: cover; border-radius: 6px; background: #F5F7FA; }
.video-thumb { position: relative; width: 100%; height: 100%; }
.play-icon { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); font-size: 2rem; color: #303133; text-shadow: 0 2px 8px rgba(0,0,0,.5); }

:deep(.el-table) { background: transparent !important; --el-table-border-color: rgba(42, 157, 143, 0.2); --el-table-header-bg-color: #F5F7FA; --el-table-row-hover-bg-color: rgba(42, 157, 143, 0.05); --el-table-tr-bg-color: transparent; color: #303133; }
:deep(.el-table th) { background: #F5F7FA !important; border-bottom: 2px solid rgba(42, 157, 143, 0.4) !important; color: #2A9D8F; font-weight: bold; }
:deep(.el-table td) { border-bottom: 1px solid rgba(42, 157, 143, 0.1) !important; padding: 12px 0; }
:deep(.el-dialog) { background: #FFFFFF; border: 1px solid #EBEEF5; }
:deep(.el-dialog__title) { color: #2A9D8F; }
</style>
