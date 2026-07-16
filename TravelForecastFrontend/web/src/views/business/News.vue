<template>
  <div class="news-container">
    <el-card class="bg-white border border-gray-200 shadow-lg">
      <template #header>
        <div class="flex items-center justify-between">
          <div>
            <h3 class="text-lg text-[#2A9D8F] font-bold">新闻资讯管理</h3>
            <p class="text-gray-500 text-sm mt-1">发布和管理您的景区新闻资讯</p>
          </div>
          <el-button type="primary" @click="openAddDialog">
            <el-icon class="mr-1"><Plus /></el-icon>
            发布新闻
          </el-button>
        </div>
      </template>

      <!-- 搜索区域 -->
      <div class="mb-4 flex gap-4">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索新闻标题"
          clearable
          style="width: 300px"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="searchStatus" placeholder="状态" style="width: 120px" @change="handleSearch">
          <el-option label="全部" value="" />
          <el-option label="已发布" value="published" />
          <el-option label="草稿" value="draft" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon class="mr-1"><Search /></el-icon>
          查询
        </el-button>
      </div>

      <!-- 新闻列表 -->
      <el-table :data="newsList" style="width: 100%" v-loading="loading">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="title" label="标题" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-gray-800">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="views" label="浏览量" width="100" align="center">
          <template #default="{ row }">
            <span class="text-[#2A9D8F]">{{ row.views || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : 'info'" size="small">
              {{ row.status === 'published' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="180">
          <template #default="{ row }">
            <span class="text-gray-500">{{ formatDate(row.publishTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">
              <el-icon class="mr-1"><Edit /></el-icon>编辑
            </el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">
              <el-icon class="mr-1"><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSearch"
          @current-change="handleSearch"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑新闻' : '发布新闻'"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入新闻标题" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类">
            <el-option label="旅游攻略" value="旅游攻略" />
            <el-option label="景点推荐" value="景点推荐" />
            <el-option label="美食推荐" value="美食推荐" />
            <el-option label="活动通知" value="活动通知" />
            <el-option label="景区新闻" value="景区新闻" />
          </el-select>
        </el-form-item>
        <el-form-item label="摘要" prop="summary">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="3"
            placeholder="请输入新闻摘要"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="请输入新闻内容"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="published">立即发布</el-radio>
            <el-radio value="draft">保存草稿</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '更新' : '发布' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Search, Edit, Delete } from '@element-plus/icons-vue'
import { getMerchantNewsList, publishNews, updateNews, deleteNews } from '@/api/news'

const loading = ref(false)
const submitting = ref(false)
const searchKeyword = ref('')
const searchStatus = ref('')
const newsList = ref<any[]>([])
const pagination = ref({
  page: 1,
  pageSize: 10,
  total: 0
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const form = ref({
  id: undefined as number | undefined,
  title: '',
  category: '',
  summary: '',
  content: '',
  status: 'published'
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

// 加载新闻列表
const loadNewsList = async () => {
  loading.value = true
  try {
    const res: any = await getMerchantNewsList({
      page: pagination.value.page,
      size: pagination.value.pageSize,
      keyword: searchKeyword.value,
      status: searchStatus.value
    })
    
    if (res.data) {
      newsList.value = res.data.records || []
      pagination.value.total = res.data.total || 0
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.value.page = 1
  loadNewsList()
}

// 打开新增对话框
const openAddDialog = () => {
  isEdit.value = false
  form.value = {
    id: undefined,
    title: '',
    category: '',
    summary: '',
    content: '',
    status: 'published'
  }
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: any) => {
  isEdit.value = true
  form.value = {
    id: row.id,
    title: row.title,
    category: row.category,
    summary: row.summary || '',
    content: row.content || '',
    status: row.status
  }
  dialogVisible.value = true
}

// 提交
const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    submitting.value = true

    if (isEdit.value && form.value.id) {
      await updateNews(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await publishNews(form.value)
      ElMessage.success('发布成功')
    }

    dialogVisible.value = false
    loadNewsList()
  } catch (error: any) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  } finally {
    submitting.value = false
  }
}

// 删除
const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认删除新闻"${row.title}"？`, '确认删除', {
      type: 'warning'
    })

    await deleteNews(row.id)
    ElMessage.success('删除成功')
    loadNewsList()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadNewsList()
})
</script>

<style scoped>
.news-container {
  color: #2C3E50;
}

:deep(.el-table) {
  background: #FFFFFF !important;
  color: #606266 !important;
}

:deep(.el-table th),
:deep(.el-table tr),
:deep(.el-table td) {
  background: transparent !important;
  color: #606266 !important;
  border-color: #EBEEF5 !important;
}

:deep(.el-table__body tr:hover > td) {
  background: #F5F7FA !important;
}

:deep(.el-card) {
  background: #FFFFFF;
  border: 1px solid #EBEEF5;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

:deep(.el-card__header) {
  border-bottom: 1px solid #EBEEF5;
}

:deep(.el-input__wrapper),
:deep(.el-select .el-input__wrapper),
:deep(.el-textarea__inner) {
  background-color: #FFFFFF !important;
  border-color: #DCDFE6 !important;
  color: #2C3E50 !important;
}

:deep(.el-input__wrapper:hover),
:deep(.el-input__wrapper.is-focus) {
  background-color: #FFFFFF !important;
  border-color: #2A9D8F !important;
}

:deep(.el-pagination) {
  --el-color-primary: #2A9D8F;
}
</style>
