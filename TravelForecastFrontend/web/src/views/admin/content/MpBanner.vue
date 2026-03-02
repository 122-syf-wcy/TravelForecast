<template>
  <div class="mp-banner-page">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-xl font-semibold text-gray-800">小程序轮播图管理</h2>
      <el-button type="primary" @click="openDialog()">
        <el-icon class="mr-1"><Plus /></el-icon>新增轮播图
      </el-button>
    </div>

    <!-- 轮播图列表 -->
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column label="ID" prop="id" width="60" align="center" />
      <el-table-column label="预览" width="160" align="center">
        <template #default="{ row }">
          <el-image v-if="row.image" :src="row.image" :preview-src-list="[row.image]"
            fit="cover" style="width:120px;height:60px;border-radius:6px;" />
          <span v-else class="text-gray-500">暂无图片</span>
        </template>
      </el-table-column>
      <el-table-column label="标题" prop="title" min-width="140" />
      <el-table-column label="跳转链接" prop="link" min-width="180" show-overflow-tooltip />
      <el-table-column label="排序" prop="sort" width="80" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="handleToggle(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确认删除此轮播图？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger" link>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑轮播图' : '新增轮播图'" width="520px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="轮播图标题" />
        </el-form-item>
        <el-form-item label="图片">
          <div class="flex flex-col gap-2 w-full">
            <el-upload
              :show-file-list="false"
              :before-upload="beforeUpload"
              :http-request="customUpload"
              accept="image/*"
            >
              <el-button type="primary" size="small" :loading="uploading">
                {{ uploading ? '上传中...' : '选择图片' }}
              </el-button>
            </el-upload>
            <el-image v-if="form.image" :src="form.image" fit="cover"
              style="width:100%;max-height:140px;border-radius:8px;margin-top:8px;" />
            <el-input v-model="form.image" placeholder="或直接填写图片URL" class="mt-1" />
          </div>
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="form.link" placeholder="点击跳转链接（可选）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { mpBannerApi } from '@/api/mpAdmin'

const loading = ref(false)
const list = ref<any[]>([])
const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const saving = ref(false)
const uploading = ref(false)

const form = ref({
  title: '',
  image: '',
  link: '',
  sort: 0,
  enabled: true
})

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await mpBannerApi.list()
    list.value = res.data || []
  } catch (e) { /* ignore */ }
  loading.value = false
}

const openDialog = (row?: any) => {
  if (row) {
    editId.value = row.id
    form.value = { title: row.title || '', image: row.image || '', link: row.link || '', sort: row.sort || 0, enabled: !!row.enabled }
  } else {
    editId.value = null
    form.value = { title: '', image: '', link: '', sort: 0, enabled: true }
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.value.image) { ElMessage.warning('请上传或填写图片地址'); return }
  saving.value = true
  try {
    if (editId.value) {
      await mpBannerApi.update(editId.value, form.value)
    } else {
      await mpBannerApi.create(form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } catch (e) { /* handled by interceptor */ }
  saving.value = false
}

const handleDelete = async (id: number) => {
  try {
    await mpBannerApi.remove(id)
    ElMessage.success('已删除')
    loadList()
  } catch (e) { /* ignore */ }
}

const handleToggle = async (row: any) => {
  try {
    await mpBannerApi.toggle(row.id)
    ElMessage.success(row.enabled ? '已启用' : '已禁用')
  } catch (e) {
    row.enabled = !row.enabled
  }
}

const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) { ElMessage.error('只能上传图片文件'); return false }
  if (!isLt5M) { ElMessage.error('图片大小不能超过 5MB'); return false }
  return true
}

const customUpload = async (options: any) => {
  uploading.value = true
  try {
    const res: any = await mpBannerApi.upload(options.file)
    form.value.image = res.data || ''
    ElMessage.success('图片上传成功')
  } catch (e) { /* handled by interceptor */ }
  uploading.value = false
}

onMounted(loadList)
</script>
