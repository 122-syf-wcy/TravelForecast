<template>
  <div class="mp-product-page">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-xl font-semibold text-gray-800">文创商品管理</h2>
      <el-button type="primary" @click="openDialog()">
        <el-icon class="mr-1"><Plus /></el-icon>新增商品
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="flex gap-3 mb-4 flex-wrap">
      <el-select v-model="filterCategory" placeholder="全部分类" clearable style="width:140px" @change="loadList">
        <el-option label="非遗文创" value="非遗文创" />
        <el-option label="地道美食" value="地道美食" />
        <el-option label="茶饮伴手" value="茶饮伴手" />
        <el-option label="手工艺品" value="手工艺品" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width:120px" @change="loadList">
        <el-option label="上架中" value="ACTIVE" />
        <el-option label="已下架" value="INACTIVE" />
      </el-select>
      <el-input v-model="filterKeyword" placeholder="搜索商品名称" clearable style="width:200px" @clear="loadList">
        <template #append>
          <el-button @click="loadList"><el-icon><Search /></el-icon></el-button>
        </template>
      </el-input>
    </div>

    <!-- 商品列表 -->
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column label="ID" prop="id" width="60" align="center" />
      <el-table-column label="图片" width="100" align="center">
        <template #default="{ row }">
          <el-image v-if="row.imageUrl" :src="row.imageUrl" :preview-src-list="[row.imageUrl]"
            fit="cover" style="width:60px;height:60px;border-radius:6px;" />
          <span v-else class="text-gray-400 text-xs">暂无</span>
        </template>
      </el-table-column>
      <el-table-column label="商品名称" prop="name" min-width="160" show-overflow-tooltip />
      <el-table-column label="分类" prop="category" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="categoryTagType(row.category)">{{ row.category }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="价格" width="90" align="center">
        <template #default="{ row }">
          <span class="text-orange-500 font-semibold">¥{{ row.price }}</span>
        </template>
      </el-table-column>
      <el-table-column label="库存" prop="stock" width="70" align="center" />
      <el-table-column label="销量" prop="sales" width="70" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
            {{ row.status === 'ACTIVE' ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openDialog(row)">编辑</el-button>
          <el-button size="small" :type="row.status === 'ACTIVE' ? 'warning' : 'success'" link @click="handleToggle(row)">
            {{ row.status === 'ACTIVE' ? '下架' : '上架' }}
          </el-button>
          <el-popconfirm title="确认删除此商品？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger" link>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑商品' : '新增商品'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="商品名称" required>
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="商品描述" />
        </el-form-item>
        <el-form-item label="商品图片">
          <div class="flex flex-col gap-2 w-full">
            <el-upload :show-file-list="false" :before-upload="beforeUpload" :http-request="customUpload" accept="image/*">
              <el-button type="primary" size="small" :loading="uploading">
                {{ uploading ? '上传中...' : '选择图片' }}
              </el-button>
            </el-upload>
            <el-image v-if="form.imageUrl" :src="form.imageUrl" fit="cover"
              style="width:120px;height:120px;border-radius:8px;margin-top:4px;" />
            <el-input v-model="form.imageUrl" placeholder="或直接填写图片URL" class="mt-1" />
          </div>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="价格" required>
              <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原价">
              <el-input-number v-model="form.originalPrice" :min="0" :precision="2" :step="1" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类" required>
              <el-select v-model="form.category" placeholder="选择分类" style="width:100%">
                <el-option label="非遗文创" value="非遗文创" />
                <el-option label="地道美食" value="地道美食" />
                <el-option label="茶饮伴手" value="茶饮伴手" />
                <el-option label="手工艺品" value="手工艺品" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库存">
              <el-input-number v-model="form.stock" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="黔豆价格">
              <el-input-number v-model="form.qdPrice" :min="0" style="width:100%" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="逗号分隔，如：热销,特产" />
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
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { mpProductApi } from '@/api/mpAdmin'

const loading = ref(false)
const list = ref<any[]>([])
const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const saving = ref(false)
const uploading = ref(false)

const filterCategory = ref('')
const filterStatus = ref('')
const filterKeyword = ref('')

const defaultForm = () => ({
  name: '', description: '', imageUrl: '', price: 0, originalPrice: null as number | null,
  category: '', stock: 999, qdPrice: null as number | null, sortOrder: 0, tags: ''
})
const form = ref(defaultForm())

const categoryTagType = (cat: string) => {
  const map: Record<string, string> = { '非遗文创': '', '地道美食': 'warning', '茶饮伴手': 'success', '手工艺品': 'danger' }
  return map[cat] || 'info'
}

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await mpProductApi.list({
      category: filterCategory.value || undefined,
      status: filterStatus.value || undefined,
      keyword: filterKeyword.value || undefined
    })
    list.value = res.data || []
  } catch (e) { /* ignore */ }
  loading.value = false
}

const openDialog = (row?: any) => {
  if (row) {
    editId.value = row.id
    form.value = {
      name: row.name || '', description: row.description || '', imageUrl: row.imageUrl || '',
      price: row.price || 0, originalPrice: row.originalPrice, category: row.category || '',
      stock: row.stock ?? 999, qdPrice: row.qdPrice, sortOrder: row.sortOrder || 0, tags: row.tags || ''
    }
  } else {
    editId.value = null
    form.value = defaultForm()
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.value.name) { ElMessage.warning('请输入商品名称'); return }
  if (!form.value.category) { ElMessage.warning('请选择分类'); return }
  saving.value = true
  try {
    if (editId.value) {
      await mpProductApi.update(editId.value, form.value)
    } else {
      await mpProductApi.create(form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } catch (e) { /* handled by interceptor */ }
  saving.value = false
}

const handleDelete = async (id: number) => {
  try {
    await mpProductApi.remove(id)
    ElMessage.success('已删除')
    loadList()
  } catch (e) { /* ignore */ }
}

const handleToggle = async (row: any) => {
  try {
    await mpProductApi.toggle(row.id)
    row.status = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    ElMessage.success(row.status === 'ACTIVE' ? '已上架' : '已下架')
  } catch (e) { /* ignore */ }
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
    const res: any = await mpProductApi.upload(options.file)
    form.value.imageUrl = res.data || ''
    ElMessage.success('图片上传成功')
  } catch (e) { /* handled by interceptor */ }
  uploading.value = false
}

onMounted(loadList)
</script>
