<template>
  <div class="landing-config-container">
    <holographic-card class="mb-6">
      <template #header>
        <div class="flex justify-between items-center">
          <h2 class="text-xl font-bold text-[#2A9D8F]">首页展示配置</h2>
          <div class="space-x-2">
            <neon-button @click="handleSave" :loading="saving">保存配置</neon-button>
            <neon-button type="info" @click="handlePreview">预览效果</neon-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- KPI 统计 -->
        <el-tab-pane label="KPI 统计" name="stats">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div v-for="(stat, index) in config.stats" :key="index" class="stat-edit-card">
              <el-form label-width="80px">
                <el-form-item label="标签">
                  <el-input v-model="stat.label" placeholder="如：预测准确率" />
                </el-form-item>
                <el-form-item label="数值">
                  <el-input-number v-model="stat.target" :min="0" />
                </el-form-item>
                <el-form-item label="后缀">
                  <el-input v-model="stat.suffix" placeholder="如：%" maxlength="5" />
                </el-form-item>
              </el-form>
            </div>
          </div>
        </el-tab-pane>

        <!-- 核心功能 -->
        <el-tab-pane label="核心功能" name="features">
          <div class="space-y-4">
            <div v-for="(feat, index) in config.features" :key="index" class="feat-edit-card">
              <el-form label-width="80px">
                <el-form-item label="图标">
                  <el-input v-model="feat.icon" placeholder="Emoji 或图标" maxlength="5" />
                </el-form-item>
                <el-form-item label="标题">
                  <el-input v-model="feat.title" placeholder="功能名称" />
                </el-form-item>
                <el-form-item label="描述">
                  <el-input v-model="feat.description" type="textarea" :rows="2" />
                </el-form-item>
              </el-form>
            </div>
          </div>
        </el-tab-pane>

        <!-- 平台亮点 -->
        <el-tab-pane label="平台亮点" name="highlights">
          <div class="space-y-6">
            <div v-for="(hl, index) in config.highlights" :key="index" class="hl-edit-card">
              <el-form label-width="80px">
                <el-form-item label="徽标">
                  <el-input v-model="hl.badge" placeholder="如：地域适配" />
                </el-form-item>
                <el-form-item label="标题">
                  <el-input v-model="hl.title" placeholder="如：六盘水本地化" />
                </el-form-item>
                <el-form-item label="描述">
                  <el-input v-model="hl.desc" type="textarea" :rows="2" />
                </el-form-item>
                <el-form-item label="要点清单">
                  <div v-for="(pt, i) in hl.points" :key="i" class="flex items-center mb-2">
                    <el-input v-model="hl.points[i]" placeholder="要点" />
                    <el-button type="danger" text @click="hl.points.splice(i, 1)" class="ml-2">删除</el-button>
                  </div>
                  <el-button text @click="hl.points.push('')">+ 新增要点</el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>
        </el-tab-pane>

        <!-- 典型场景 -->
        <el-tab-pane label="典型场景" name="scenarios">
          <div class="space-y-4">
            <div v-for="(sc, index) in config.scenarios" :key="index" class="sc-edit-card">
              <el-form label-width="80px">
                <el-form-item label="图标">
                  <el-input v-model="sc.icon" placeholder="Emoji" maxlength="5" />
                </el-form-item>
                <el-form-item label="标题">
                  <el-input v-model="sc.title" />
                </el-form-item>
                <el-form-item label="描述">
                  <el-input v-model="sc.desc" type="textarea" :rows="2" />
                </el-form-item>
              </el-form>
            </div>
          </div>
        </el-tab-pane>

        <!-- 用户评价 -->
        <el-tab-pane label="用户评价" name="testimonials">
          <div class="space-y-4">
            <div v-for="(t, index) in config.testimonials" :key="index" class="test-edit-card">
              <el-form label-width="80px">
                <el-form-item label="评价内容">
                  <el-input v-model="t.content" type="textarea" :rows="2" />
                </el-form-item>
                <el-form-item label="作者">
                  <el-input v-model="t.author" />
                </el-form-item>
                <el-form-item label="角色">
                  <el-input v-model="t.role" placeholder="如：运营经理" />
                </el-form-item>
              </el-form>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </holographic-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import HolographicCard from '@/components/HolographicCard.vue'
import NeonButton from '@/components/NeonButton.vue'

const router = useRouter()
const activeTab = ref('stats')
const saving = ref(false)

const config = reactive({
  stats: [
    { id: 1, label: '预测准确率', target: 96, suffix: '%' },
    { id: 2, label: '覆盖景区', target: 128, suffix: '+' },
    { id: 3, label: '平均响应', target: 180, suffix: 'ms' }
  ],
  features: [
    { id: 1, icon: '🔮', title: '智能客流预测', description: '基于LSTM-ARIMA混合模型，精准预测景区未来客流趋势' },
    { id: 2, icon: '🗺️', title: '行程规划', description: 'AI算法优化游览路线，避开高峰节省时间' },
    { id: 3, icon: '📊', title: '实时数据分析', description: '3D可视化大屏，实时监控景区运营状态' }
  ],
  highlights: [
    {
      id: 1,
      badge: '地域适配',
      title: '六盘水本地化',
      desc: '结合地形气候/节假日/民族节庆等在地特征进行模型调优',
      points: ['✓ 气候与季节特征工程', '✓ 景区承载与出入口流向', '✓ 本地交通/节庆事件注入']
    },
    {
      id: 2,
      badge: 'AI 预测',
      title: '混合时序算法',
      desc: 'LSTM × ARIMA 叠加假日增强，短中期客流趋势更稳',
      points: ['✓ 96%+ 验证集准确率目标', '✓ 小样本场景鲁棒性优化', '✓ 预测区间与置信度输出']
    },
    {
      id: 3,
      badge: '3D 可视化',
      title: '沉浸式数据大屏',
      desc: '三维地图与实时指标联动，热点/拥挤区一目了然',
      points: ['✓ 热点热力/人流轨迹联动', '✓ 阈值预警与消息推送', '✓ 多终端自适应展示']
    }
  ],
  scenarios: [
    { id: 1, icon: '🚦', title: '高峰错峰与导流', desc: '节假日提前预警并优化入园时段，减少拥堵' },
    { id: 2, icon: '🚌', title: '交通接驳编排', desc: '预测客流峰谷，动态调整公交与摆渡车频次' },
    { id: 3, icon: '🛡️', title: '承载与安全保障', desc: '超阈值实时告警，联动安保与广播提示' }
  ],
  testimonials: [
    { content: '预测结果基本贴合实际，我们的游客投诉率明显下降。', author: '水城古镇', role: '运营经理' },
    { content: '高峰时的接驳车辆调度效率提升了不少。', author: '市文旅局', role: '数据科' },
    { content: '路线规划对避开排队很有帮助，游客体验更好了。', author: '玉舍森林公园', role: '游客服务' }
  ]
})

const handleSave = async () => {
  saving.value = true
  try {
    await request({ url: '/content/landing', method: 'post', data: config })
    ElMessage.success('配置已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handlePreview = () => {
  window.open('/#/landing', '_blank')
}

onMounted(async () => {
  try {
    const res: any = await request({ url: '/content/landing', method: 'get' })
    const data: any = res?.data || res
    if (data && Object.keys(data).length) {
      Object.assign(config, data)
    }
  } catch (e) {
    // ignore
  }
})
</script>

<style scoped>
.landing-config-container {
  @apply p-6 min-h-screen bg-[#F5F7FA];
}

.stat-edit-card, .feat-edit-card, .hl-edit-card, .sc-edit-card, .test-edit-card {
  @apply bg-white border border-gray-200 rounded-lg p-5 transition-all duration-300 shadow-sm;
  position: relative;
  overflow: hidden;
}

.stat-edit-card:hover, .feat-edit-card:hover, .hl-edit-card:hover, .sc-edit-card:hover, .test-edit-card:hover {
  @apply border-[#2A9D8F] shadow-md;
  transform: translateY(-2px);
}

:deep(.el-tabs__item) {
  @apply text-gray-500 transition-colors;
}

:deep(.el-tabs__item.is-active) {
  @apply text-[#2A9D8F] font-bold;
}

:deep(.el-tabs__nav-wrap::after) {
  @apply bg-gray-200 h-[1px];
}

:deep(.el-tabs__active-bar) {
  @apply bg-[#2A9D8F] h-[2px];
}

:deep(.el-form-item__label) {
  @apply text-gray-700 font-medium !important;
  color: #374151 !important;
}

:deep(.el-input__wrapper), :deep(.el-textarea__inner) {
  @apply bg-white border-gray-300 shadow-none transition-all duration-300 !important;
}

:deep(.el-input__wrapper.is-focus), :deep(.el-textarea__inner:focus) {
  @apply border-[#2A9D8F] !important;
}

:deep(.el-input__inner) {
  @apply text-gray-800 !important;
}

:deep(.el-input-number__decrease), :deep(.el-input-number__increase) {
  @apply bg-gray-50 border-gray-300 text-[#2A9D8F] !important;
}

:deep(.el-input-number__decrease:hover), :deep(.el-input-number__increase:hover) {
  @apply text-[#2A9D8F] bg-gray-100 !important;
}
</style>













