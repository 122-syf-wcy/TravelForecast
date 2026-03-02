<template>
  <div class="scenic-list">
    <div class="page-header mb-6">
      <h1 class="text-2xl text-gray-800 mb-2">景区管理</h1>
      <p class="text-gray-500">六盘水旅游景区内容管理系统</p>
    </div>
    
    <!-- 筛选栏 -->
    <HolographicCard class="mb-6">
      <div class="flex items-center justify-between mb-4 pb-2 border-b border-gray-200">
        <h3 class="text-lg text-[#2A9D8F] font-bold">景区筛选</h3>
        <el-button type="primary" size="small" @click="handleAddScenic">
          <el-icon><Plus /></el-icon> 添加景区
        </el-button>
      </div>
      <el-form :model="filterForm" inline class="text-gray-800">
        <el-form-item label="景区名称">
          <el-input v-model="filterForm.name" placeholder="输入景区名称" />
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="filterForm.level" placeholder="等级" class="w-32">
            <el-option v-for="item in levelOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filterForm.type" placeholder="类型" class="w-32">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="状态" class="w-32">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </HolographicCard>
    
    <!-- KPI 看板 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
      <HolographicCard>
        <div class="text-center py-2">
          <el-icon class="text-4xl text-[#2A9D8F] mb-2"><MapLocation /></el-icon>
          <h3 class="text-xl text-[#2A9D8F]">景区总数</h3>
          <div class="text-4xl font-bold text-gray-800 mt-1">{{ scenicStats.total }}</div>
          <div class="text-sm text-gray-500">已上线景区</div>
        </div>
      </HolographicCard>
      
      <HolographicCard>
        <div class="text-center py-2">
          <el-icon class="text-4xl text-green-400 mb-2"><DataLine /></el-icon>
          <h3 class="text-xl text-green-400">总客流量</h3>
          <div class="text-4xl font-bold text-gray-800 mt-1">{{ scenicStats.totalVisitors.toLocaleString() }}</div>
          <div class="text-sm text-gray-500">年度累计</div>
        </div>
      </HolographicCard>
      
      <HolographicCard>
        <div class="text-center py-2">
          <el-icon class="text-4xl text-orange-400 mb-2"><Star /></el-icon>
          <h3 class="text-xl text-orange-400">平均评分</h3>
          <div class="text-4xl font-bold text-gray-800 mt-1">{{ scenicStats.avgRating }}</div>
          <div class="text-sm text-gray-500">用户满意度</div>
        </div>
      </HolographicCard>
    </div>
    
    <!-- 图表 -->
    <HolographicCard class="mb-6">
      <div class="flex items-center justify-between mb-4 pb-2 border-b border-gray-200">
        <h3 class="text-lg text-[#2A9D8F] font-bold">景区客流分析</h3>
        <el-radio-group v-model="chartTimeRange" size="small">
          <el-radio-button label="week">本周</el-radio-button>
          <el-radio-button label="month">本月</el-radio-button>
          <el-radio-button label="year">全年</el-radio-button>
        </el-radio-group>
      </div>
      <div ref="visitorChartRef" class="h-80"></div>
    </HolographicCard>
    
    <!-- 列表 -->
    <HolographicCard>
      <div class="flex items-center justify-between mb-4 pb-2 border-b border-gray-200">
        <h3 class="text-lg text-[#2A9D8F] font-bold">景区列表</h3>
        <div class="flex items-center space-x-2">
          <el-input placeholder="搜索景区..." v-model="searchQuery" size="small" class="w-48" />
        </div>
      </div>
      <el-table :data="tableData" style="width: 100%" class="bg-transparent" v-loading="loading">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="景区信息" min-width="220">
          <template #default="scope">
            <div class="flex items-center">
              <el-image 
                v-if="scope.row.image"
                :src="scope.row.image" 
                :preview-src-list="[scope.row.image]"
                fit="cover"
                class="w-16 h-12 rounded-md bg-gray-50"
              >
                <template #error>
                  <div class="w-16 h-12 rounded-md bg-gray-200 flex items-center justify-center text-xs text-gray-400">无图</div>
                </template>
              </el-image>
              <div v-else class="w-16 h-12 rounded-md bg-gray-200 flex items-center justify-center text-xs text-gray-400">无图</div>
              <div class="ml-3">
                <div class="font-bold text-gray-800">{{ scope.row.name }}</div>
                <div class="text-xs text-gray-500 truncate" style="max-width: 200px" :title="scope.row.address">{{ scope.row.address }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="90">
          <template #default="scope">
            <el-tag :type="getLevelStyle(scope.row.level)" size="small" effect="dark">{{ scope.row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="scope">
            <el-tag :type="getTypeStyle(scope.row.type)" size="small" effect="plain">{{ scope.row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="visitors" label="客流" width="90" align="center" />
        <el-table-column label="评分" width="120">
          <template #default="scope">
            <el-rate v-model="scope.row.rating" disabled text-color="#ff9900" size="small"/>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small" effect="dark">{{ formatStatus(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="280">
          <template #default="scope">
            <div class="flex items-center space-x-2">
              <el-button type="primary" size="small" @click="handleEdit(scope.row)" link>编辑</el-button>
              <el-button type="warning" size="small" @click="handleManageMedia(scope.row)" link>媒体</el-button>
              <el-button type="success" size="small" @click="handlePreview(scope.row)" link>预览</el-button>
              <el-button :type="isClosedStatus(scope.row.status) ? 'success' : 'danger'" size="small" @click="handleStatusToggle(scope.row)" link>
                {{ isClosedStatus(scope.row.status) ? '开放' : '关闭' }}
              </el-button>
              <el-button type="danger" size="small" @click="handleDelete(scope.row)" link>删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </HolographicCard>
  
    <!-- 添加/编辑景区弹窗 -->
    <el-dialog v-model="editVisible" width="600px" :title="editForm.id ? '编辑景区' : '新增景区'" append-to-body>
      <el-form :model="editForm" :rules="rules" ref="editRef" label-width="80px">
        <el-form-item label="景区名称" prop="name">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="editForm.address" />
        </el-form-item>
        <div class="grid grid-cols-2 gap-4">
          <el-form-item label="等级" prop="level">
            <el-select v-model="editForm.level" class="w-full">
              <el-option v-for="o in levelOptions.filter(i=>i.value)" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="类型" prop="type">
            <el-select v-model="editForm.type" class="w-full">
              <el-option v-for="o in typeOptions.filter(i=>i.value)" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="封面图">
          <el-input v-model="editForm.image" placeholder="输入图片URL" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="editForm.status" class="w-full">
            <el-option v-for="s in statusOptions.filter(i=>i.value)" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  
    <!-- 媒体管理弹窗 -->
    <el-dialog v-model="mediaVisible" width="800px" :title="currentScenic ? `${currentScenic.name} - 媒体管理` : '媒体管理'" append-to-body destroy-on-close>
      <el-tabs v-model="activeMediaTab" type="border-card" class="bg-gray-50 border-gray-200">
        <!-- 图片管理 -->
        <el-tab-pane label="图片管理" name="images">
          <div class="mb-4 flex items-center justify-between">
            <div class="flex items-center space-x-2">
               <span class="text-gray-500">类型:</span>
               <el-select v-model="uploadImageType" size="small" class="w-24">
                  <el-option label="封面" value="COVER" />
                  <el-option label="画廊" value="GALLERY" />
               </el-select>
            </div>
            <el-upload
              class="upload-demo"
              :http-request="customUploadImage"
              :show-file-list="false"
              accept="image/*"
            >
              <el-button type="primary" size="small"><el-icon class="mr-1"><Upload /></el-icon> 上传图片</el-button>
            </el-upload>
          </div>
          
          <div v-if="scenicImages.length > 0" class="grid grid-cols-4 gap-4 max-h-96 overflow-y-auto p-2">
            <div v-for="img in scenicImages" :key="img.id" class="relative group border border-gray-200 rounded-lg overflow-hidden">
              <el-image 
                :src="img.imageUrl" 
                :preview-src-list="scenicImages.map(i => i.imageUrl)"
                fit="cover"
                class="w-full h-32"
              />
              <div class="absolute top-1 left-1">
                 <el-tag size="small" :type="getImageTypeTag(img.imageType)" effect="dark">{{ getImageTypeName(img.imageType) }}</el-tag>
              </div>
              <div class="absolute inset-0 bg-black/60 hidden group-hover:flex items-center justify-center transition-opacity">
                <el-button type="danger" circle size="small" @click="handleDeleteImage(img.id)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无图片" :image-size="60" />
        </el-tab-pane>
  
        <!-- 视频管理 -->
        <el-tab-pane label="视频管理" name="videos">
          <div class="mb-4 flex items-center space-x-3">
            <el-input v-model="videoTitle" placeholder="视频标题" size="small" class="w-48" />
            <el-upload
              class="upload-demo"
              :http-request="customUploadVideo"
              :show-file-list="false"
              accept="video/*"
            >
              <el-button type="primary" size="small"><el-icon class="mr-1"><VideoPlay /></el-icon> 上传视频</el-button>
            </el-upload>
          </div>
          
          <div v-if="scenicVideos.length > 0" class="space-y-2 max-h-96 overflow-y-auto">
            <div v-for="video in scenicVideos" :key="video.id" class="flex items-center justify-between p-3 bg-gray-50 rounded border border-gray-200">
               <div class="flex items-center space-x-3">
                 <el-icon class="text-2xl text-[#2A9D8F]"><VideoPlay /></el-icon>
                 <div>
                   <div class="font-bold text-gray-700">{{ video.title }}</div>
                   <div class="text-xs text-gray-500">{{ formatDuration(video.duration) }}</div>
                 </div>
               </div>
               <div class="flex items-center space-x-3">
                 <a :href="video.videoUrl" target="_blank" class="text-[#2A9D8F] hover:text-[#238b7e] text-sm">播放</a>
                 <el-button type="danger" size="small" text @click="handleDeleteVideo(video.id)">删除</el-button>
               </div>
            </div>
          </div>
          <el-empty v-else description="暂无视频" :image-size="60" />
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { Plus, MapLocation, DataLine, Star, Upload, VideoPlay, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminScenicApi } from '@/api/scenic'
import HolographicCard from '@/components/HolographicCard.vue'
import * as echarts from 'echarts'

// ================= 状态定义 =================
const searchQuery = ref('')
const chartTimeRange = ref('month')
const visitorChartRef = ref<HTMLElement | null>(null)
const loading = ref(false)
const tableData = ref<any[]>([])
const scenicStats = reactive({ total: 0, totalVisitors: 0, avgRating: 0 })

// 筛选表单
const filterForm = reactive({ name: '', level: '', type: '', status: '' })

// 添加/编辑表单
const editVisible = ref(false)
const editRef = ref()
const editForm = reactive<any>({ id: '', name: '', address: '', level: '', type: '', image: '', description: '', status: '开放' })
const rules = {
  name: [{ required: true, message: '请输入景区名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
  level: [{ required: true, message: '请选择等级', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

// 媒体管理
const mediaVisible = ref(false)
const activeMediaTab = ref('images')
const currentScenic = ref<any>(null)
const uploadImageType = ref('GALLERY')
const scenicImages = ref<any[]>([])
const videoTitle = ref('')
const scenicVideos = ref<any[]>([])

// ================= 选项定义 =================
const levelOptions = [
  { value: '', label: '全部' }, { value: '5A', label: '5A级' }, { value: '4A', label: '4A级' }, 
  { value: '3A', label: '3A级' }, { value: '2A', label: '2A级' }, { value: '1A', label: '1A级' }, { value: '未评级', label: '未评级' }
]
const typeOptions = [
  { value: '', label: '全部' }, { value: '自然风光', label: '自然风光' }, { value: '人文景观', label: '人文景观' },
  { value: '历史古迹', label: '历史古迹' }, { value: '主题公园', label: '主题公园' }, { value: '乡村旅游', label: '乡村旅游' }
]
const statusOptions = [
  { value: '', label: '全部' }, { value: '开放', label: '开放' }, { value: '关闭', label: '关闭' }, { value: '维护中', label: '维护中' }
]

// ================= 生命周期 =================
onMounted(() => { initVisitorChart(); loadScenicList() })
onBeforeUnmount(() => { if (visitorChartRef.value) { const inst = echarts.getInstanceByDom(visitorChartRef.value); if (inst) inst.dispose() } })

// ================= 辅助函数 =================
const getLevelStyle = (l: string) => ({ '5A': 'danger', '4A': 'warning', '3A': 'success', '2A': 'info', '1A': 'info' }[l] || '')
const getTypeStyle = (t: string) => ({ '自然风光': 'success', '人文景观': 'warning', '历史古迹': 'info', '主题公园': 'danger', '乡村旅游': 'primary' }[t] || '')
const statusTypeMap: Record<string, string> = { '开放': 'success', '关闭': 'danger', '维护中': 'warning', 'active': 'success', 'inactive': 'danger', 'closed': 'danger', 'maintenance': 'warning' }
const getStatusType = (s: string) => statusTypeMap[(s || '').toLowerCase()] || statusTypeMap[s] || 'info'
const statusNameMap: Record<string, string> = { 'active': '开放', 'inactive': '关闭', 'closed': '关闭', 'maintenance': '维护中', 'ACTIVE': '开放', 'INACTIVE': '关闭', 'CLOSED': '关闭', 'MAINTENANCE': '维护中' }
const formatStatus = (s: string) => statusNameMap[s] || s
const isClosedStatus = (s: string) => ['closed', 'inactive', '关闭'].includes((s || '').toLowerCase())
const getImageTypeTag = (t: string) => (t === 'COVER' ? 'danger' : 'success')
const getImageTypeName = (t: string) => (t === 'COVER' ? '封面图' : '画廊图')
const formatDuration = (s: number) => (!s ? '未知' : `${Math.floor(s / 60)}分${s % 60}秒`)

// ================= 业务逻辑 =================
const loadScenicList = async () => {
  loading.value = true
  try {
    const params = { page: 1, size: 100, keyword: filterForm.name || undefined, status: filterForm.status || undefined }
    const res: any = await adminScenicApi.getList(params)
    let list: any[] = []
    const data = res?.data || res
    if (data && data.records) list = data.records
    else if (Array.isArray(data)) list = data
    else if (data?.list) list = data.list

    tableData.value = list.map((item: any) => ({
      id: item.id,
      name: item.name,
      image: item.imageUrl || item.image || '',
      address: item.address,
      level: item.level,
      type: item.type || item.category || '',
      visitors: item.visitors || Math.floor(Math.random() * 5000),
      rating: item.rating || 5.0,
      status: item.status
    }))
    // 对没有封面图的景区，从媒体图片中获取封面图
    for (const row of tableData.value) {
      if (!row.image && row.id) {
        try {
          const imgRes: any = await adminScenicApi.getImages(row.id)
          const imgs = Array.isArray(imgRes) ? imgRes : (imgRes.data || [])
          const cover = imgs.find((i: any) => i.imageType === 'COVER') || imgs[0]
          if (cover) {
            row.image = cover.imageUrl || cover.imageURL || ''
          }
        } catch (_e) { /* ignore */ }
      }
    }
    // 更新KPI统计
    scenicStats.total = tableData.value.length
    scenicStats.totalVisitors = tableData.value.reduce((sum: number, s: any) => sum + (s.visitors || 0), 0)
    const ratings = tableData.value.filter((s: any) => s.rating > 0)
    scenicStats.avgRating = ratings.length > 0 ? parseFloat((ratings.reduce((sum: number, s: any) => sum + s.rating, 0) / ratings.length).toFixed(1)) : 0
  } catch (e: any) { ElMessage.error('加载失败: ' + (e.message || '网络错误')) } 
  finally { loading.value = false }
}

const handleSearch = () => { loadScenicList(); ElMessage.success('搜索条件已应用') }
const resetForm = () => { Object.assign(filterForm, { name: '', level: '', type: '', status: '' }); loadScenicList() }

const handleAddScenic = () => {
  Object.assign(editForm, { id: '', name: '', address: '', level: '', type: '', image: '', description: '', status: '开放' })
  editVisible.value = true
}
const handleEdit = (row: any) => { Object.assign(editForm, row); editVisible.value = true }
const submitEdit = async () => {
  await editRef.value?.validate()
  try {
    const data = { ...editForm }
    if (!data.id) { await adminScenicApi.create(data); ElMessage.success('新增成功') }
    else { await adminScenicApi.update(data.id, data); ElMessage.success('更新成功') }
    editVisible.value = false
    loadScenicList()
  } catch (e: any) { ElMessage.error('保存失败') }
}
const handleDelete = async (row: any) => {
  try { await ElMessageBox.confirm('确认删除?', '警告', { type: 'warning' }); await adminScenicApi.delete(row.id); ElMessage.success('已删除'); loadScenicList() } 
  catch(e) {}
}
const handleStatusToggle = async (row: any) => {
  const newStatus = isClosedStatus(row.status) ? 'active' : 'inactive'
  try { await adminScenicApi.updateStatus(row.id, newStatus); row.status = newStatus; ElMessage.success('状态已更新') } 
  catch(e) { ElMessage.error('更新失败') }
}
const handlePreview = (row: any) => {
  const w = window.open('about:blank')
  if(w) {
    w.document.title = row.name
    w.document.body.style.background='#FFFFFF'
    w.document.body.innerHTML = `<div style="display:flex;justify-content:center;align-items:center;height:100vh"><img src="${row.image}" style="max-width:100%;max-height:90vh;border-radius:8px"></div>`
  }
}

const handleManageMedia = async (row: any) => {
  currentScenic.value = { ...row, numericId: row.id }
  mediaVisible.value = true
  activeMediaTab.value = 'images'
  await loadScenicMedia(row.id)
}
const loadScenicMedia = async (id: number) => {
  scenicImages.value = []; scenicVideos.value = []
  try {
    const res: any = await adminScenicApi.getImages(id)
    const list = Array.isArray(res) ? res : (res.data || [])
    scenicImages.value = list.map((i: any) => ({ ...i, imageUrl: i.imageUrl || i.imageURL }))
  } catch(e) {}
  try {
    const res: any = await adminScenicApi.getVideos(id)
    scenicVideos.value = Array.isArray(res) ? res : (res.data || [])
  } catch(e) {}
}
const customUploadImage = async (options: any) => {
  const { file } = options
  try {
    const res = await adminScenicApi.uploadImage(currentScenic.value.id, file, uploadImageType.value, 0)
    ElMessage.success('上传成功')
    const img = res.data || res
    if(img.imageURL) img.imageUrl = img.imageURL
    scenicImages.value.push(img)
  } catch(e) { ElMessage.error('上传失败') }
}
const customUploadVideo = async (options: any) => {
  const { file } = options
  if(!videoTitle.value) { ElMessage.warning('请输入标题'); return }
  try {
    const res = await adminScenicApi.uploadVideo(currentScenic.value.id, file, videoTitle.value, undefined, 0)
    ElMessage.success('上传成功')
    scenicVideos.value.push(res.data || res)
    videoTitle.value = ''
  } catch(e) { ElMessage.error('上传失败') }
}
const handleDeleteImage = async (id: number) => {
  try { await ElMessageBox.confirm('确认删除?', '警告', { type: 'warning' }); await adminScenicApi.deleteImage(id); scenicImages.value = scenicImages.value.filter(i => i.id !== id); ElMessage.success('已删除') } catch(e) {}
}
const handleDeleteVideo = async (id: number) => {
  try { await ElMessageBox.confirm('确认删除?', '警告', { type: 'warning' }); await adminScenicApi.deleteVideo(id); scenicVideos.value = scenicVideos.value.filter(v => v.id !== id); ElMessage.success('已删除') } catch(e) {}
}

const initVisitorChart = () => {
  const chartDom = visitorChartRef.value
  if (!chartDom) return
  nextTick(() => {
    const myChart = echarts.init(chartDom)
    const option = {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
      xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'], axisLabel: { color: '#303133' }, axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.15)' } } },
      yAxis: { type: 'value', axisLabel: { color: '#303133' }, axisLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.15)' } }, splitLine: { lineStyle: { color: 'rgba(0, 0, 0, 0.06)' } } },
      series: [
        { name: '客流', type: 'line', data: [1800, 2200, 1900, 2100, 2400, 3600, 4200], smooth: true, itemStyle: { color: '#2A9D8F' }, lineStyle: { width: 3, color: '#2A9D8F' } }
      ]
    }
    myChart.setOption(option)
    window.addEventListener('resize', () => myChart.resize())
  })
}
</script>

<style scoped>
.scenic-list { color: #2C3E50; }
:deep(.el-form-item__label) { color: #606266 !important; }
:deep(.el-input__inner) { background-color: #FFFFFF !important; border: 1px solid #DCDFE6 !important; color: #303133 !important; }
:deep(.el-select__wrapper) { background-color: #FFFFFF !important; border: 1px solid #DCDFE6 !important; }
:deep(.el-table) { background-color: transparent !important; --el-table-border-color: rgba(42, 157, 143, 0.1); --el-table-header-bg-color: rgba(42, 157, 143, 0.05); --el-table-row-hover-bg-color: rgba(42, 157, 143, 0.05); --el-table-text-color: #2C3E50; --el-table-header-text-color: #2A9D8F; }
:deep(.el-table tr), :deep(.el-table th) { background-color: transparent !important; }
:deep(.el-table td), :deep(.el-table th) { border-bottom: 1px solid rgba(0, 0, 0, 0.04) !important; }
:deep(.el-pagination) { --el-pagination-bg-color: transparent; --el-pagination-text-color: #606266; --el-pagination-button-color: #606266; --el-pagination-button-bg-color: transparent; }
:deep(.el-dialog) { background: #FFFFFF; border: 1px solid #EBEEF5; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12); }
:deep(.el-dialog__title) { color: #2A9D8F; }
:deep(.el-tabs--border-card) { background: transparent; border: 1px solid rgba(0, 0, 0, 0.04); }
:deep(.el-tabs--border-card>.el-tabs__header) { background: #F5F7FA; border-bottom: 1px solid #EBEEF5; }
:deep(.el-tabs--border-card>.el-tabs__header .el-tabs__item.is-active) { background: rgba(42, 157, 143, 0.1); color: #2A9D8F; border-right-color: rgba(0, 0, 0, 0.04); border-left-color: rgba(0, 0, 0, 0.04); }
:deep(.el-tabs--border-card>.el-tabs__content) { background: transparent; }
</style>