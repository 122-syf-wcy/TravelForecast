<template>
  <div class="flow-prediction-container p-6">
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
      <!-- 预测控制面板 -->
      <div class="lg:col-span-4">
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
          <div class="flex items-center gap-2 mb-6 border-b border-gray-100 pb-4">
            <el-icon class="text-primary text-xl"><Setting /></el-icon>
            <h2 class="text-xl font-bold text-gray-900">客流预测控制台</h2>
          </div>
          
          <div class="space-y-6">
            <div>
              <h3 class="text-gray-700 font-medium mb-3">选择景区</h3>
              <el-select v-model="selectedScenic" placeholder="请选择景区" class="w-full scenic-select" popper-class="scenic-select-dropdown">
                <el-option 
                  v-for="item in scenicOptions" 
                  :key="item.value" 
                  :label="item.label" 
                  :value="item.value" 
                />
              </el-select>
            </div>
            
            <div>
              <h3 class="text-gray-700 font-medium mb-3">选择日期</h3>
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                class="w-full"
              />
            </div>
            
            <div>
              <h3 class="text-gray-700 font-medium mb-3">预测因子</h3>
              <el-checkbox-group v-model="selectedFactors">
                <div class="grid grid-cols-2 gap-3">
                  <el-checkbox label="weather" class="!text-gray-600">天气因素</el-checkbox>
                  <el-checkbox label="holiday" class="!text-gray-600">节假日</el-checkbox>
                  <el-checkbox label="season" class="!text-gray-600">季节因素</el-checkbox>
                  <el-checkbox label="event" class="!text-gray-600">特殊活动</el-checkbox>
                  <el-checkbox label="social" class="!text-gray-600">社交媒体</el-checkbox>
                  <el-checkbox label="historical" class="!text-gray-600">历史数据</el-checkbox>
                </div>
              </el-checkbox-group>
            </div>
            
            <div>
              <h3 class="text-gray-700 font-medium mb-3">预测模型</h3>
              <el-radio-group v-model="selectedModel" class="flex flex-col space-y-3 w-full">
                <el-radio label="dual_stream" class="!mr-0 w-full model-radio-item" border>
                  <div class="flex items-center justify-between w-full">
                    <span class="font-bold text-gray-800">双流融合预测模型</span>
                    <el-tag size="small" type="danger" effect="plain" round>NEW</el-tag>
                  </div>
                </el-radio>
                <el-radio label="lstm" class="!mr-0 w-full model-radio-item" border>LSTM 深度学习模型</el-radio>
                <el-radio label="arima" class="!mr-0 w-full model-radio-item" border>ARIMA 经典统计学模型</el-radio>
              </el-radio-group>
            </div>
            
            <div class="pt-4">
              <el-button type="primary" size="large" @click="generatePrediction" class="w-full" :loading="loading" round>
                生成预测
              </el-button>
            </div>
          </div>
        </div>
        
        <div class="space-y-6 mt-6" v-if="showResult">
          <!-- 影响因素分析 (移动到左侧) -->
          <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <div class="flex items-center gap-2 mb-4 border-b border-gray-100 pb-3">
              <el-icon class="text-primary"><PieChart /></el-icon>
              <h2 class="text-lg font-bold text-gray-900">影响因素分析</h2>
            </div>
            <div class="h-64 w-full" ref="factorsChartRef"></div>
          </div>
          
          <!-- 时段分布预测 (移动到左侧) -->
          <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <div class="flex items-center gap-2 mb-4 border-b border-gray-100 pb-3">
              <el-icon class="text-primary"><Timer /></el-icon>
              <h2 class="text-lg font-bold text-gray-900">时段分布预测</h2>
            </div>
            <div class="h-64 w-full" ref="hourlyChartRef"></div>
          </div>
        </div>
      </div>
      
      <!-- 预测图表展示 -->
      <div class="lg:col-span-8 space-y-6">
        <!-- 预测分析结果 (置顶显示) -->
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6" v-if="showResult">
          <div class="flex justify-between items-center mb-6">
            <div class="flex items-center gap-2">
              <el-icon class="text-primary text-xl"><DataAnalysis /></el-icon>
              <h2 class="text-xl font-bold text-gray-900">预测结果分析</h2>
            </div>
            <div class="flex gap-3">
              <el-button 
                size="small" 
                type="primary"
                plain
                round
                :loading="aiAnalyzing"
                @click="generateAIAnalysis"
              >
                <template #icon><el-icon><Cpu /></el-icon></template>
                {{ aiAnalyzing ? '分析中...' : 'AI智能分析' }}
              </el-button>
              <el-button size="small" type="info" plain @click="downloadReport" icon="Download" round>下载报告</el-button>
            </div>
          </div>

          <div v-if="analysisData">
            <!-- 关键指标卡片 -->
            <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
              <div class="bg-gray-50 rounded-xl p-4 border border-gray-100">
                <h3 class="text-gray-500 text-sm mb-2">峰值客流预测</h3>
                <div class="flex items-baseline gap-2">
                  <span class="text-2xl font-bold text-gray-900">{{ analysisData.peakFlow?.toLocaleString() }}</span>
                  <span class="text-xs text-gray-500">人次</span>
                </div>
                <div class="text-sm text-gray-600 mt-1">
                  预计日期: <span class="font-medium text-primary">{{ analysisData.peakDate }}</span>
                </div>
              </div>
              
              <div class="bg-gray-50 rounded-xl p-4 border border-gray-100">
                <h3 class="text-gray-500 text-sm mb-2">平均日客流量</h3>
                <div class="flex items-baseline gap-2">
                  <span class="text-2xl font-bold text-gray-900">{{ analysisData.avgFlow?.toLocaleString() }}</span>
                  <span class="text-xs text-gray-500">人次/天</span>
                </div>
                <div class="text-sm text-gray-600 mt-1">
                  预测周期: {{ dateRange ? Math.round((new Date((dateRange[1] as Date)).getTime() - new Date((dateRange[0] as Date)).getTime()) / (1000 * 3600 * 24)) : 7 }} 天
                </div>
              </div>
              
              <div class="bg-gray-50 rounded-xl p-4 border border-gray-100">
                 <h3 class="text-gray-500 text-sm mb-2">模型置信度</h3>
                 <div class="flex items-center gap-2 mb-1">
                   <span class="text-2xl font-bold text-gray-900">{{ Math.round((analysisData.confidence || 0.85) * 100) }}%</span>
                   <el-tag size="small" type="success" effect="light" round>高可信</el-tag>
                 </div>
                 <el-progress 
                   :percentage="Math.round((analysisData.confidence || 0.85) * 100)" 
                   :color="predictConfidenceColor" 
                   :stroke-width="6"
                   :show-text="false"
                 />
              </div>
            </div>

            <!-- AI 智能建议 -->
            <div class="bg-[rgba(6,182,212,0.03)] rounded-xl p-5 border border-[rgba(6,182,212,0.1)]">
              <h3 class="text-gray-800 font-bold mb-3 flex items-center gap-2">
                <el-icon class="text-primary"><MagicStick /></el-icon>
                <span>AI 智能决策建议</span>
              </h3>
              <div v-if="aiSuggestions" class="markdown-content text-sm text-gray-600 leading-relaxed max-h-60 overflow-y-auto pr-2 custom-scrollbar">
                <div v-html="renderedAiSuggestions"></div>
              </div>
              <ul v-else class="grid grid-cols-1 md:grid-cols-2 gap-3 text-gray-600 text-sm">
                <li v-for="(suggestion, index) in analysisData.suggestions" :key="index" class="flex items-start gap-2">
                  <span class="text-primary mt-1">•</span>
                  <span>{{ suggestion }}</span>
                </li>
              </ul>
            </div>
          </div>
          
          <div v-else class="text-center text-gray-500 py-12">
            <el-icon class="text-4xl mb-3"><DataLine /></el-icon>
            <p>正在进行复杂模型运算...</p>
          </div>
        </div>
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
          <div class="flex justify-between items-center mb-6">
            <div class="flex items-center gap-2">
              <el-icon class="text-primary text-xl"><TrendCharts /></el-icon>
              <h2 class="text-xl font-bold text-gray-900">客流预测趋势图</h2>
            </div>
            <div v-if="showResult">
              <el-radio-group v-model="chartType" size="small">
                <el-radio-button label="line">折线图</el-radio-button>
                <el-radio-button label="bar">柱状图</el-radio-button>
                <el-radio-button label="heatmap">热力图</el-radio-button>
              </el-radio-group>
            </div>
          </div>
          
          <div class="h-96 flex items-center justify-center bg-gray-50 rounded-xl" v-if="!showResult">
            <div class="text-center">
              <el-icon class="text-6xl text-gray-500 mb-4"><DataAnalysis /></el-icon>
              <p class="text-gray-500">请在左侧设置预测参数，然后点击"生成预测"按钮</p>
            </div>
          </div>
          
          <div v-else>
            <div class="h-96 w-full" ref="predictionChartRef"></div>
          </div>
        </div>
        

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Setting, 
  TrendCharts,
  DataAnalysis,
  Cpu,
  MagicStick,
  DataLine,
  PieChart,
  Timer
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getFlowPrediction, getHourlyPrediction, getBestVisitTime } from '@/api/prediction'
import { getScenicSpots } from '@/api/scenic'
import request from '@/utils/request'
import aiRequest from '@/utils/aiRequest'
import { marked } from 'marked'
import { useDigitalHumanStore } from '@/store/digitalHuman'

// 是否使用开发模式 - 设置为false以使用真实后端数据
const isDev = false
const digitalHumanStore = useDigitalHumanStore()

// 表单数据
const selectedScenic = ref('')
const dateRange = ref([new Date(), new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)])
const selectedFactors = ref(['weather', 'holiday', 'historical'])
const selectedModel = ref('dual_stream')

// 结果相关
const showResult = ref(false)
const chartType = ref('line')
const predictConfidenceColor = ref('#2A9D8F')
const loading = ref(false)
const predictionData = ref<any>(null)
const analysisData = ref<any>(null)
const aiAnalyzing = ref(false)
const aiSuggestions = ref('')

// 图表引用
const predictionChartRef = ref<HTMLElement | null>(null)
const factorsChartRef = ref<HTMLElement | null>(null)
const hourlyChartRef = ref<HTMLElement | null>(null)

// 图表实例
let predictionChart: any = null
let factorsChart: any = null
let hourlyChart: any = null

// 景区选项（从后端动态加载）
const scenicOptions = ref<Array<{ value: number; label: string }>>([])
const scenicLoading = ref(false)

// 加载景区列表
const loadScenicOptions = async () => {
  scenicLoading.value = true
  try {
    const response: any = await getScenicSpots({ city: '六盘水' })
    const data = response?.data || response
    const list = Array.isArray(data) ? data : (data?.list || [])
    
    if (list.length > 0) {
      scenicOptions.value = list.map((item: any) => ({
        value: item.id,
        label: item.name
      }))
      // 默认选择第一个景区
      if (!selectedScenic.value && scenicOptions.value.length > 0) {
        selectedScenic.value = scenicOptions.value[0].value
      }
    }
  } catch (error) {
    // 降级使用默认数据
    // 降级使用默认数据
    scenicOptions.value = [
      { value: 1, label: '梅花山风景区' },
      { value: 2, label: '玉舍国家森林公园' },
      { value: 3, label: '乌蒙大草原' },
      { value: 4, label: '水城古镇' },
      { value: 5, label: '明湖国家湿地公园' }
    ]
    selectedScenic.value = 1
  } finally {
    scenicLoading.value = false
  }
}

// 生成预测
const generatePrediction = async () => {
  if (!selectedScenic.value) {
    ElMessage.warning('请选择景区')
    return
  }
  
  if (!dateRange.value || !dateRange.value[0] || !dateRange.value[1]) {
    ElMessage.warning('请选择日期范围')
    return
  }
  
  loading.value = true
  ElMessage.success('正在生成预测，请稍候...')
  
  try {
    // 计算预测天数
    let days = 7
    if (dateRange.value && dateRange.value[1]) {
      const end = new Date(dateRange.value[1])
      const today = new Date()
      // 计算从今天到结束日期的天数
      const diffTime = end.getTime() - today.getTime()
      days = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
      // 确保至少预测1天，且不超过30天（后端限制）
      days = Math.max(1, Math.min(days, 30))
    }

    // 模型映射
    let modelParam = selectedModel.value
    if (modelParam === 'hybrid') {
      modelParam = 'dual_stream' // 映射到最新的双流模型
    }

    // 调用API获取预测数据
    const scenicId = typeof selectedScenic.value === 'number' ? selectedScenic.value : parseInt(selectedScenic.value as string, 10) || 1
    predictionData.value = await getFlowPrediction(scenicId, modelParam, days, selectedFactors.value)
    
    showResult.value = true
    await nextTick()
    
    // 给DOM一点时间完全渲染，特别是网格布局需要计算
    setTimeout(() => {
      // 初始化图表
      initPredictionChart()
      initFactorsChart()
      initHourlyChart()
      
      // 生成预测分析
      generateAnalysis()

      // 触发数字人基于真实预测数据进行分析讲解
      triggerPredictionAnalysis()
    }, 300)
    
    ElMessage.success('预测生成成功')
  } catch (error) {
    console.error('生成预测失败:', error)
    ElMessage.error('生成预测失败，请重试')
  } finally {
    loading.value = false
  }
}

// 数字人基于真实预测数据分析讲解
const triggerPredictionAnalysis = () => {
  if (!predictionData.value || !predictionData.value.predictions) return

  const predictions = predictionData.value.predictions
  const scenicLabel = scenicOptions.value.find((o: any) => o.value === selectedScenic.value)?.label || '未知景区'
  const confidence = predictionData.value.confidence || 0.85

  let text = `景区：${scenicLabel}，预测置信度：${(confidence * 100).toFixed(0)}%\n`
  text += `预测周期：${predictions.length}天\n\n`

  predictions.forEach((p: any, i: number) => {
    text += `${p.date}（${p.weekday || ''}）：预计客流${p.expectedFlow}人`
    if (p.weatherCondition || p.weather) text += `，天气${p.weatherCondition || p.weather}`
    if (p.temperature) text += ` ${p.temperature}°C`
    if (p.congestionLevel) text += `，拥挤度${p.congestionLevel}`
    if (p.peakHours && p.peakHours.length) text += `，高峰${p.peakHours.join('/')}`
    text += '\n'
  })

  // 找出客流最高和最低的日期
  const sorted = [...predictions].sort((a: any, b: any) => a.expectedFlow - b.expectedFlow)
  if (sorted.length >= 2) {
    text += `\n客流最低：${sorted[0].date}（${sorted[0].expectedFlow}人），最高：${sorted[sorted.length - 1].date}（${sorted[sorted.length - 1].expectedFlow}人）`
  }

  digitalHumanStore.triggerPredictionNarration(text)
}

// 下载报告
const downloadReport = () => {
  if (!predictionData.value || !predictionData.value.predictions) {
    ElMessage.warning('请先生成预测数据')
    return
  }

  ElMessage.success('报告已开始下载')
  
  const predictions = predictionData.value.predictions || []
  const scenicName = scenicOptions.value.find(s => s.value === selectedScenic.value)?.label || '景区'
  const confidence = predictionData.value.confidence || 0.85
  const modelName = selectedModel.value === 'dual_stream' ? '双流神经网络' : 
                    selectedModel.value === 'lstm' ? 'LSTM模型' : 
                    selectedModel.value === 'transformer' ? 'Transformer模型' : '集成模型'
  
  // 计算统计数据
  const totalFlow = predictions.reduce((sum: number, p: any) => sum + (p.expectedFlow || 0), 0)
  const avgFlow = Math.round(totalFlow / predictions.length)
  const maxPrediction = predictions.reduce((max: any, p: any) => 
    (p.expectedFlow > (max?.expectedFlow || 0)) ? p : max, predictions[0])
  const minPrediction = predictions.reduce((min: any, p: any) => 
    (p.expectedFlow < (min?.expectedFlow || Infinity)) ? p : min, predictions[0])
  
  // 生成表格行
  const tableRows = predictions.map((p: any) => {
    const flowLevel = p.expectedFlow > avgFlow * 1.2 ? 'high' : 
                     p.expectedFlow < avgFlow * 0.8 ? 'low' : 'medium'
    const flowText = flowLevel === 'high' ? '拥挤' : flowLevel === 'low' ? '空闲' : '适中'
    const suggestion = flowLevel === 'high' ? '建议错峰出行' : 
                      flowLevel === 'low' ? '适合游览' : '正常游览'
    return '<tr>' +
      '<td>' + (p.date || '-') + '</td>' +
      '<td><strong>' + (p.expectedFlow?.toLocaleString() || '-') + '</strong> 人</td>' +
      '<td class="flow-' + flowLevel + '">' + flowText + '</td>' +
      '<td>' + (p.weather || '晴') + ' ' + (p.temperature || '22') + '°C</td>' +
      '<td>' + suggestion + '</td>' +
      '</tr>'
  }).join('')

  // 生成AI建议部分（使用marked转换Markdown）
  let aiSection = ''
  if (aiSuggestions.value) {
    try {
      const renderedContent = marked(aiSuggestions.value)
      aiSection = '<div class="section">' +
        '<h2 class="section-title">AI 智能分析建议</h2>' +
        '<div class="suggestions-box">' + renderedContent + '</div>' +
        '</div>'
    } catch (e) {
      // 如果marked转换失败，使用简单的换行处理
      const suggestionLines = aiSuggestions.value.split('\n')
        .filter((line: string) => line.trim())
        .map((line: string) => '<p>' + line + '</p>')
        .join('')
      aiSection = '<div class="section">' +
        '<h2 class="section-title">AI 智能分析建议</h2>' +
        '<div class="suggestions-box">' + suggestionLines + '</div>' +
        '</div>'
    }
  }

  // 生成报告HTML
  const reportHTML = '<!DOCTYPE html>' +
    '<html lang="zh-CN">' +
    '<head>' +
    '<meta charset="UTF-8">' +
    '<meta name="viewport" content="width=device-width, initial-scale=1.0">' +
    '<title>客流预测分析报告 - ' + scenicName + '</title>' +
    '<style>' +
    '* { margin: 0; padding: 0; box-sizing: border-box; }' +
    'body { font-family: "Microsoft YaHei", "PingFang SC", sans-serif; background: #f9fafb; color: #374151; min-height: 100vh; padding: 40px; }' +
    '.report-container { max-width: 900px; margin: 0 auto; background: #ffffff; border-radius: 20px; padding: 40px; box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05); border: 1px solid #e5e7eb; }' +
    '.report-header { text-align: center; margin-bottom: 40px; padding-bottom: 30px; border-bottom: 2px solid #f3f4f6; }' +
    '.report-title { font-size: 28px; font-weight: 700; color: #111827; margin-bottom: 10px; }' +
    '.report-subtitle { color: #6b7280; font-size: 14px; }' +
    '.section { margin-bottom: 30px; }' +
    '.section-title { font-size: 18px; font-weight: 600; color: #111827; margin-bottom: 16px; padding-left: 14px; border-left: 4px solid #06b6d4; }' +
    '.info-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }' +
    '.info-item { background: #f9fafb; padding: 16px; border-radius: 12px; border: 1px solid #e5e7eb; }' +
    '.info-label { font-size: 12px; color: #6b7280; margin-bottom: 6px; }' +
    '.info-value { font-size: 20px; font-weight: 700; color: #111827; }' +
    '.info-value.highlight { color: #06b6d4; }' +
    '.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }' +
    '.stat-card { background: #ffffff; padding: 20px; border-radius: 12px; text-align: center; border: 1px solid #e5e7eb; box-shadow: 0 2px 5px rgba(0,0,0,0.02); }' +
    '.stat-value { font-size: 28px; font-weight: 700; color: #06b6d4; margin-bottom: 6px; }' +
    '.stat-label { font-size: 12px; color: #6b7280; }' +
    '.prediction-table { width: 100%; border-collapse: collapse; margin-top: 16px; }' +
    '.prediction-table th, .prediction-table td { padding: 12px 16px; text-align: left; border-bottom: 1px solid #e5e7eb; }' +
    '.prediction-table th { background: #f9fafb; color: #374151; font-weight: 600; font-size: 13px; }' +
    '.prediction-table td { font-size: 14px; color: #4b5563; }' +
    '.flow-high { color: #ef4444; font-weight: 600; }' +
    '.flow-medium { color: #f59e0b; font-weight: 600; }' +
    '.flow-low { color: #10b981; font-weight: 600; }' +
    '.suggestions-box { background: #ecfeff; padding: 20px; border-radius: 12px; border-left: 4px solid #06b6d4; }' +
    '.suggestions-box p { margin-bottom: 12px; line-height: 1.8; font-size: 14px; color: #374151; }' +
    '.suggestions-box h1, .suggestions-box h2, .suggestions-box h3 { color: #111827; margin: 16px 0 10px 0; font-size: 16px; }' +
    '.suggestions-box strong { color: #06b6d4; font-weight: 600; }' +
    '.suggestions-box ul, .suggestions-box ol { margin: 10px 0; padding-left: 24px; }' +
    '.suggestions-box li { margin-bottom: 8px; line-height: 1.6; }' +
    '.suggestions-box em { color: #d97706; font-style: normal; }' +
    '.footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #e5e7eb; text-align: center; color: #9ca3af; font-size: 12px; }' +
    '.confidence-bar { height: 8px; background: #e5e7eb; border-radius: 4px; overflow: hidden; margin-top: 8px; }' +
    '.confidence-fill { height: 100%; background: linear-gradient(90deg, #06b6d4, #3b82f6); border-radius: 4px; }' +
    '@media print { body { background: white; padding: 0; } .report-container { box-shadow: none; border: none; padding: 0; } }' +
    '</style>' +
    '</head>' +
    '<body>' +
    '<div class="report-container">' +
    '<div class="report-header">' +
    '<h1 class="report-title">📊 客流预测分析报告</h1>' +
    '<p class="report-subtitle">智教黔行 · ' + scenicName + ' · ' + new Date().toLocaleDateString('zh-CN') + '</p>' +
    '</div>' +
    '<div class="section">' +
    '<h2 class="section-title">基本信息</h2>' +
    '<div class="info-grid">' +
    '<div class="info-item"><div class="info-label">预测景区</div><div class="info-value">' + scenicName + '</div></div>' +
    '<div class="info-item"><div class="info-label">预测模型</div><div class="info-value">' + modelName + '</div></div>' +
    '<div class="info-item"><div class="info-label">预测周期</div><div class="info-value">' + predictions.length + ' 天</div></div>' +
    '<div class="info-item"><div class="info-label">预测可信度</div><div class="info-value highlight">' + (confidence * 100).toFixed(1) + '%</div><div class="confidence-bar"><div class="confidence-fill" style="width: ' + (confidence * 100) + '%"></div></div></div>' +
    '</div></div>' +
    '<div class="section">' +
    '<h2 class="section-title">统计概览</h2>' +
    '<div class="stats-grid">' +
    '<div class="stat-card"><div class="stat-value">' + totalFlow.toLocaleString() + '</div><div class="stat-label">预测总客流</div></div>' +
    '<div class="stat-card"><div class="stat-value">' + avgFlow.toLocaleString() + '</div><div class="stat-label">日均客流</div></div>' +
    '<div class="stat-card"><div class="stat-value">' + (maxPrediction?.expectedFlow?.toLocaleString() || '-') + '</div><div class="stat-label">峰值客流</div></div>' +
    '<div class="stat-card"><div class="stat-value">' + (minPrediction?.expectedFlow?.toLocaleString() || '-') + '</div><div class="stat-label">谷值客流</div></div>' +
    '</div></div>' +
    '<div class="section">' +
    '<h2 class="section-title">每日预测详情</h2>' +
    '<table class="prediction-table">' +
    '<thead><tr><th>日期</th><th>预测客流</th><th>拥挤程度</th><th>天气</th><th>建议</th></tr></thead>' +
    '<tbody>' + tableRows + '</tbody>' +
    '</table></div>' +
    aiSection +
    '<div class="footer">' +
    '<p>📊 本报告由智教黔行系统自动生成</p>' +
    '<p>生成时间：' + new Date().toLocaleString('zh-CN') + ' | 数据来源：' + modelName + '预测模型</p>' +
    '<p>© 2024-2026 智教黔行 版权所有</p>' +
    '</div></div></body></html>'

  // 创建 Blob 并下载
  const blob = new Blob([reportHTML], { type: 'text/html;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = '客流预测报告_' + scenicName + '_' + new Date().toISOString().split('T')[0] + '.html'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

// 初始化预测趋势图表
const initPredictionChart = () => {
  if (predictionChartRef.value && predictionData.value) {
    predictionChart = echarts.init(predictionChartRef.value)
    
    const predictions = predictionData.value.predictions || []
    const dates = predictions.map((p: any) => p.date)
    const predictedData = predictions.map((p: any) => p.expectedFlow)
    // 生成模拟历史数据（实际项目中应从API获取）
    const historicalData = predictedData.map((v: number) => Math.floor(v * (0.85 + Math.random() * 0.2)))
    
    const option = {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross',
          crossStyle: {
            color: '#06b6d4'
          },
          lineStyle: {
            color: 'rgba(6, 182, 212, 0.4)',
            width: 1,
            type: 'dashed'
          }
        },
        backgroundColor: '#ffffff',
        borderColor: '#e5e7eb',
        borderWidth: 1,
        padding: [12, 16],
        textStyle: {
          color: '#374151',
          fontSize: 13
        },
        extraCssText: 'box-shadow: 0 4px 12px rgba(0,0,0,0.1);',
        formatter: (params: any) => {
          let result = `<div style="font-weight:600;color:#111827;margin-bottom:8px;font-size:14px">${params[0].axisValue}</div>`
          params.forEach((item: any) => {
            const marker = `<span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${item.color};margin-right:8px;"></span>`
            result += `<div style="margin:4px 0">${marker}${item.seriesName}: <span style="font-weight:600;color:${item.color}">${item.value?.toLocaleString()}</span> 人</div>`
          })
          return result
        }
      },
      legend: {
        data: ['预测客流', '历史客流'],
        textStyle: {
          color: '#6b7280',
          fontSize: 13
        },
        top: 10,
        icon: 'roundRect',
        itemWidth: 20,
        itemHeight: 10,
        itemGap: 25
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '8%',
        top: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: dates,
        axisLine: {
          lineStyle: {
            color: '#e5e7eb',
            width: 1
          }
        },
        axisLabel: {
          color: '#6b7280',
          fontSize: 12,
          margin: 12
        },
        axisTick: {
          show: false
        }
      },
      yAxis: {
        type: 'value',
        name: '游客数量',
        nameTextStyle: {
          color: '#9ca3af',
          fontSize: 12,
          padding: [0, 40, 0, 0]
        },
        axisLine: {
          show: false
        },
        axisLabel: {
          color: '#6b7280',
          fontSize: 12,
          formatter: (value: number) => value >= 1000 ? (value / 1000).toFixed(1) + 'k' : value
        },
        splitLine: {
          lineStyle: {
            color: '#f3f4f6',
            type: 'dashed'
          }
        }
      },
      series: [
        {
          name: '预测客流',
          type: 'line',
          smooth: 0.4,
          lineStyle: {
            width: 3,
            color: '#06b6d4',
            shadowColor: 'rgba(6, 182, 212, 0.3)',
            shadowBlur: 10,
            shadowOffsetY: 4
          },
          symbol: 'circle',
          symbolSize: 8,
          itemStyle: {
            color: '#ffffff',
            borderColor: '#06b6d4',
            borderWidth: 2,
            shadowColor: 'rgba(6, 182, 212, 0.4)',
            shadowBlur: 4
          },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(6, 182, 212, 0.2)' },
              { offset: 0.5, color: 'rgba(6, 182, 212, 0.05)' },
              { offset: 1, color: 'rgba(6, 182, 212, 0)' }
            ])
          },
          emphasis: {
            scale: true,
            itemStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(6, 182, 212, 0.8)'
            }
          },
          data: predictedData,
          animationDuration: 1500,
          animationEasing: 'cubicOut'
        },
        {
          name: '历史客流',
          type: 'line',
          smooth: 0.4,
          symbol: 'none',
          lineStyle: {
            width: 2,
            type: 'dashed',
            color: '#9CA3AF'
          },
          itemStyle: {
            color: '#9CA3AF'
          },
          data: historicalData,
          animationDuration: 1800,
          animationEasing: 'cubicOut',
          animationDelay: 300
        }
      ],
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    }
    
    predictionChart.setOption(option)
    
    window.addEventListener('resize', () => {
      predictionChart?.resize()
    })
  }
}

// 初始化影响因素图表
const initFactorsChart = () => {
  if (factorsChartRef.value) {
    // 检查容器尺寸
    const rect = factorsChartRef.value.getBoundingClientRect()
    
    if (rect.width === 0 || rect.height === 0) {
      return
    }
    
    factorsChart = echarts.init(factorsChartRef.value)
    
    const option = {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        backgroundColor: '#FFFFFF',
        borderColor: '#EBEEF5',
        borderWidth: 1,
        padding: [12, 16],
        textStyle: {
          color: '#303133',
          fontSize: 13
        },
        extraCssText: 'box-shadow: 0 4px 12px rgba(0,0,0,0.1);',
        formatter: (params: any) => {
          return `<div style="font-weight:600;color:${params.color};margin-bottom:6px">${params.name}</div>
                  <div style="color:#606266">影响权重: <span style="font-weight:600;color:#06b6d4">${params.value}%</span></div>
                  <div style="color:#606266">占比: <span style="font-weight:600;color:#3b82f6">${params.percent}%</span></div>`
        }
      },
      legend: {
        orient: 'horizontal',
        bottom: 0,
        textStyle: {
          color: '#606266',
          fontSize: 11
        },
        icon: 'circle',
        itemWidth: 10,
        itemHeight: 10,
        itemGap: 12
      },
      series: [
        {
          name: '影响权重',
          type: 'pie',
          radius: ['30%', '60%'],
          center: ['50%', '42%'],
          roseType: 'radius',
          avoidLabelOverlap: true,
          itemStyle: {
            borderRadius: 8,
            borderColor: '#FFFFFF',
            borderWidth: 3,
            shadowBlur: 20,
            shadowColor: 'rgba(0, 0, 0, 0.08)'
          },
          label: {
            show: true,
            position: 'outside',
            color: '#606266',
            fontSize: 11,
            formatter: '{b}\n{d}%',
            alignTo: 'labelLine',
            bleedMargin: 5,
            distanceToLabelLine: 5
          },
          labelLine: {
            show: true,
            length: 12,
            length2: 15,
            smooth: 0.3,
            lineStyle: {
              color: 'rgba(102, 102, 102, 0.6)',
              width: 1
            }
          },
          labelLayout: {
            hideOverlap: true,
            moveOverlap: 'shiftY'
          },
          emphasis: {
            scale: true,
            scaleSize: 10,
            label: {
              show: true,
              fontSize: 13,
              fontWeight: 'bold',
              color: '#303133'
            },
            itemStyle: {
              shadowBlur: 25,
              shadowOffsetX: 0,
              shadowColor: 'rgba(42, 157, 143, 0.6)'
            }
          },
          data: [
            { 
              value: 35, 
              name: '历史数据',
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
                  { offset: 0, color: '#0ea5e9' },
                  { offset: 1, color: '#3b82f6' }
                ])
              }
            },
            { 
              value: 25, 
              name: '天气因素',
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
                  { offset: 0, color: '#06b6d4' },
                  { offset: 1, color: '#0891b2' }
                ])
              }
            },
            { 
              value: 20, 
              name: '节假日',
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
                  { offset: 0, color: '#f59e0b' },
                  { offset: 1, color: '#d97706' }
                ])
              }
            },
            { 
              value: 10, 
              name: '社交热度',
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
                  { offset: 0, color: '#FFC861' },
                  { offset: 1, color: '#cc9e4d' }
                ])
              }
            },
            { 
              value: 10, 
              name: '特殊活动',
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
                  { offset: 0, color: '#4FFBDF' },
                  { offset: 1, color: '#3cc9b3' }
                ])
              }
            }
          ],
          animationType: 'scale',
          animationEasing: 'elasticOut',
          animationDuration: 1200,
          animationDelay: (idx: number) => idx * 100
        }
      ]
    }
    
    factorsChart.setOption(option)
    
    // 强制刷新尺寸
    setTimeout(() => {
      factorsChart?.resize()
    }, 50)
    
    window.addEventListener('resize', () => {
      factorsChart?.resize()
    })
  }
}

// 初始化小时分布图表
const initHourlyChart = () => {
  if (hourlyChartRef.value) {
    // 检查容器尺寸
    const rect = hourlyChartRef.value.getBoundingClientRect()
    
    if (rect.width === 0 || rect.height === 0) {
      return
    }
    
    hourlyChart = echarts.init(hourlyChartRef.value)
    
    const hours = Array.from({ length: 12 }, (_, i) => `${i + 8}:00`)
    const data = [120, 180, 350, 650, 830, 720, 680, 890, 1000, 780, 500, 220]
    
    // 根据数值计算颜色（高峰时段用暖色）
    const maxVal = Math.max(...data)
    const getBarColor = (value: number) => {
      const ratio = value / maxVal
      if (ratio > 0.8) return ['#f43f5e', '#e11d48'] // 高峰 - 红色
      if (ratio > 0.6) return ['#f59e0b', '#d97706'] // 较高 - 橙色
      if (ratio > 0.4) return ['#06b6d4', '#0891b2'] // 中等 - 青色
      return ['#3b82f6', '#2563eb'] // 较低 - 蓝色
    }
    
    const option = {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow',
          shadowStyle: {
            color: 'rgba(42, 157, 143, 0.08)'
          }
        },
        backgroundColor: '#FFFFFF',
        borderColor: '#EBEEF5',
        borderWidth: 1,
        padding: [12, 16],
        textStyle: {
          color: '#303133',
          fontSize: 13
        },
        extraCssText: 'box-shadow: 0 4px 12px rgba(0,0,0,0.1);',
        formatter: (params: any) => {
          const value = params[0].value
          const ratio = value / maxVal
          let level = '低峰'
          let levelColor = '#3b82f6'
          if (ratio > 0.8) { level = '高峰'; levelColor = '#f43f5e' }
          else if (ratio > 0.6) { level = '较高'; levelColor = '#f59e0b' }
          else if (ratio > 0.4) { level = '中等'; levelColor = '#06b6d4' }
          
          return `<div style="font-weight:600;color:#2A9D8F;margin-bottom:8px;font-size:14px">${params[0].axisValue}</div>
                  <div style="margin:4px 0;color:#606266">预测客流: <span style="font-weight:600;color:#06b6d4">${value.toLocaleString()}</span> 人</div>
                  <div style="margin:4px 0;color:#606266">拥挤程度: <span style="font-weight:600;color:${levelColor}">${level}</span></div>`
        }
      },
      grid: {
        left: '12%',
        right: '4%',
        bottom: '12%',
        top: '10%',
        containLabel: false
      },
      xAxis: {
        type: 'category',
        data: hours,
        axisLine: {
          lineStyle: {
            color: '#e5e7eb',
            width: 1
          }
        },
        axisLabel: {
          color: '#909399',
          fontSize: 10,
          rotate: 0,
          interval: 0,
          margin: 10
        },
        axisTick: {
          show: false
        }
      },
      yAxis: {
        type: 'value',
        name: '游客数',
        nameTextStyle: {
          color: '#909399',
          fontSize: 11,
          padding: [0, 0, 0, 0]
        },
        axisLine: {
          show: false
        },
        axisLabel: {
          color: '#909399',
          fontSize: 10,
          formatter: (value: number) => value >= 1000 ? (value / 1000).toFixed(0) + 'k' : value
        },
        splitLine: {
          show: true,
          lineStyle: {
            color: '#F0F2F5',
            type: 'dashed'
          }
        }
      },
      series: [
        {
          name: '游客数量',
          data: data.map((value, index) => {
            const colors = getBarColor(value)
            return {
              value,
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: colors[0] },
                  { offset: 1, color: colors[1] }
                ]),
                borderRadius: [6, 6, 0, 0],
                shadowColor: 'rgba(6, 182, 212, 0.3)',
                shadowBlur: 8,
                shadowOffsetY: 2
              }
            }
          }),
          type: 'bar',
          barWidth: '55%',
          showBackground: true,
          backgroundStyle: {
            color: 'rgba(42, 157, 143, 0.05)',
            borderRadius: [6, 6, 0, 0]
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 15,
              shadowColor: 'rgba(42, 157, 143, 0.6)'
            }
          },
          label: {
            show: false
          },
          animationDuration: 1000,
          animationEasing: 'elasticOut',
          animationDelay: (idx: number) => idx * 50
        }
      ]
    }
    
    hourlyChart.setOption(option)
    
    // 强制刷新尺寸
    setTimeout(() => {
      hourlyChart?.resize()
    }, 50)
    
    window.addEventListener('resize', () => {
      hourlyChart?.resize()
    })
  }
}

// 工具函数：生成日期范围
const generateDateRange = (start: Date, end: Date) => {
  const dates = []
  const current = new Date(start)
  
  while (current <= end) {
    dates.push(new Date(current))
    current.setDate(current.getDate() + 1)
  }
  
  return dates
}

// 工具函数：生成随机数据
const generateRandomData = (length: number, min: number, max: number) => {
  return Array.from({ length }, () => Math.floor(Math.random() * (max - min)) + min)
}

// 工具函数：格式化日期
const formatDate = (date: Date) => {
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 生成预测分析
const generateAnalysis = () => {
  if (!predictionData.value || !predictionData.value.predictions) {
    return
  }
  
  const predictions = predictionData.value.predictions
  
  // 找出峰值
  let peakFlow = 0
  let peakDate = ''
  predictions.forEach((p: any) => {
    if (p.expectedFlow > peakFlow) {
      peakFlow = p.expectedFlow
      peakDate = p.date
    }
  })
  
  // 计算平均值
  const totalFlow = predictions.reduce((sum: number, p: any) => sum + p.expectedFlow, 0)
  const avgFlow = Math.round(totalFlow / predictions.length)
  
  // 获取可信度
  const confidence = predictionData.value.confidence || 0.85
  
  // 生成建议
  const suggestions = []
  
  // 根据峰值日期判断是否周末
  const peakDay = new Date(peakDate).getDay()
  const isWeekend = peakDay === 0 || peakDay === 6
  
  if (isWeekend) {
    suggestions.push(`景区在${peakDate}（周${['日','一','二','三','四','五','六'][peakDay]}）可能出现拥堵，建议错峰出行`)
  } else {
    suggestions.push(`预测客流高峰在${peakDate}，建议提前规划行程`)
  }
  
  // 根据高峰时段给建议
  if (predictions[0]?.peakHours) {
    const peakHoursStr = predictions[0].peakHours.join('、')
    suggestions.push(`${peakHoursStr}为游览高峰期，建议避开此时段`)
  }
  
  suggestions.push('建议提前预约门票，避免排队等待')
  
  if (avgFlow > 5000) {
    suggestions.push('预测客流较大，请合理安排游览路线')
  }
  
  analysisData.value = {
    peakDate,
    peakFlow,
    avgFlow,
    confidence,
    suggestions
  }
}

// 生成AI智能分析
const generateAIAnalysis = async () => {
  if (!predictionData.value || !predictionData.value.predictions) {
    ElMessage.warning('请先生成预测数据')
    return
  }
  
  aiAnalyzing.value = true
  aiSuggestions.value = ''
  
  try {
    // 准备分析数据
    const predictions = predictionData.value.predictions
    const scenicName = scenicOptions.value.find(s => s.value === selectedScenic.value)?.label || '景区'
    
    // 构建提示词
    const prompt = `作为一个专业的旅游数据分析师，请基于以下客流预测数据，为游客提供专业的出行建议：

景区：${scenicName}
预测数据：
${predictions.map((p: any, i: number) => 
  `${i + 1}. ${p.date} (${p.weekday}): 预测客流${p.expectedFlow}人，天气${p.weatherCondition || '未知'}，拥挤度${p.congestionLevel || '适中'}`
).join('\n')}

平均客流：${analysisData.value?.avgFlow}人/天
峰值客流：${analysisData.value?.peakFlow}人 (${analysisData.value?.peakDate})
预测可信度：${Math.round((analysisData.value?.confidence || 0.85) * 100)}%

请提供：
1. 最佳游览时间建议（具体到日期和时段）
2. 需要注意的事项（基于天气、客流等）
3. 错峰出行建议
4. 其他实用建议

要求：语言简洁专业，条理清晰，直接给出建议，不要重复数据。`

    // 调用AI智能服务接口（使用非流式接口）
    const result = await aiRequest.post('/chat/message', {
      message: prompt,
      scenicId: selectedScenic.value,
      conversationId: null
    })

    if ((result.code === 200 || result.code === 1) && result.data?.reply) {
      aiSuggestions.value = result.data.reply
      ElMessage.success('AI分析完成')
    } else {
      // 降级：生成基础建议
      generateBasicSuggestions()
      ElMessage.info('使用基础分析模式')
    }
    
  } catch (error: any) {
    console.error('AI分析失败:', error)
    
    // 降级方案：生成基础建议
    generateBasicSuggestions()
    
    ElMessage.warning('AI服务暂不可用，已切换到基础分析模式')
  } finally {
    aiAnalyzing.value = false
  }
}

// 生成基础建议（AI降级方案）
const generateBasicSuggestions = () => {
  if (!analysisData.value) return
  
  const predictions = predictionData.value?.predictions || []
  const scenicName = scenicOptions.value.find(s => s.value === selectedScenic.value)?.label || '景区'
  const peakDate = analysisData.value.peakDate
  const peakFlow = analysisData.value.peakFlow
  const avgFlow = analysisData.value.avgFlow
  
  let suggestions = `📊 ${scenicName}客流预测分析\n\n`
  
  // 1. 最佳游览时间
  suggestions += `🎯 最佳游览时间建议：\n`
  const sortedPredictions = [...predictions].sort((a: any, b: any) => a.expectedFlow - b.expectedFlow)
  const bestDays = sortedPredictions.slice(0, 2)
  bestDays.forEach((day: any) => {
    suggestions += `• ${day.date}（${day.weekday}）- 预计客流${day.expectedFlow}人，${day.weatherCondition === 'sunny' ? '☀️晴天' : day.weatherCondition === 'rainy' ? '🌧️雨天' : '☁️多云'}，拥挤度${day.congestionLevel === 'low' ? '较低' : day.congestionLevel === 'medium' ? '适中' : '较高'}\n`
  })
  suggestions += `建议游览时段：早上8:00-10:00或下午16:00后，避开游览高峰\n\n`
  
  // 2. 高峰提醒
  suggestions += `⚠️ 高峰期提醒：\n`
  suggestions += `• 峰值预测：${peakDate} 预计客流达${peakFlow}人，请做好拥挤准备\n`
  const peakDay = predictions.find((p: any) => p.date === peakDate)
  if (peakDay) {
    const dayOfWeek = new Date(peakDate).getDay()
    const isWeekend = dayOfWeek === 0 || dayOfWeek === 6
    if (isWeekend) {
      suggestions += `• 该日为周末，建议考虑工作日前往以获得更好体验\n`
    }
    suggestions += `• 高峰时段：${peakDay.peakHours?.join('、') || '10:00-12:00、14:00-16:00'}，建议避开\n`
  }
  suggestions += `\n`
  
  // 3. 天气注意事项
  suggestions += `🌤️ 天气注意事项：\n`
  const rainyDays = predictions.filter((p: any) => p.weatherCondition === 'rainy')
  if (rainyDays.length > 0) {
    suggestions += `• 预测有${rainyDays.length}天可能降雨：${rainyDays.map((d: any) => d.date).join('、')}\n`
    suggestions += `• 雨天客流通常下降30-40%，但请携带雨具\n`
  } else {
    suggestions += `• 近期天气较好，适合出游\n`
  }
  suggestions += `\n`
  
  // 4. 实用建议
  suggestions += `💡 实用建议：\n`
  suggestions += `• 提前线上预约门票，节省排队时间\n`
  if (avgFlow > 3000) {
    suggestions += `• 平均客流${avgFlow}人/天，建议制定详细游览路线\n`
    suggestions += `• 考虑聘请导游或使用语音导览，提高游览效率\n`
  } else {
    suggestions += `• 客流适中，可以悠闲游览，享受景区风光\n`
  }
  suggestions += `• 关注景区官方通知，了解最新开放信息\n`
  suggestions += `• 携带充电宝、防晒用品等必备物品\n\n`
  
  suggestions += `📈 预测可信度：${Math.round((analysisData.value.confidence || 0.88) * 100)}%\n`
  suggestions += `⏰ 分析时间：${new Date().toLocaleString('zh-CN')}`
  
  aiSuggestions.value = suggestions
}

// 将AI建议的Markdown渲染为HTML
const renderedAiSuggestions = computed(() => {
  if (!aiSuggestions.value) return ''
  try {
    return marked(aiSuggestions.value)
  } catch (error) {
    console.error('Markdown渲染失败:', error)
    return aiSuggestions.value
  }
})

// 监听图表类型变化，更新图表显示
watch(chartType, (newType) => {
  if (!predictionChart || !predictionData.value) {
    return
  }
  
  updatePredictionChart(newType)
})

// 更新预测图表类型
const updatePredictionChart = (type: string) => {
  if (!predictionChart || !predictionData.value) return
  
  const predictions = predictionData.value.predictions || []
  const dates = predictions.map((p: any) => p.date)
  const predictedData = predictions.map((p: any) => p.expectedFlow)
  const historicalData = predictedData.map((v: number) => Math.floor(v * (0.85 + Math.random() * 0.2)))
  
  let option: any = {}
  
  if (type === 'line') {
    // 折线图配置 - 浅色主题
    option = {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross',
          crossStyle: { color: '#06b6d4' },
          lineStyle: { color: 'rgba(6, 182, 212, 0.4)', width: 1, type: 'dashed' }
        },
        backgroundColor: '#FFFFFF',
        borderColor: '#EBEEF5',
        borderWidth: 1,
        padding: [12, 16],
        textStyle: { color: '#303133', fontSize: 13 },
        extraCssText: 'box-shadow: 0 4px 12px rgba(0,0,0,0.1);',
        formatter: (params: any) => {
          let result = `<div style="font-weight:600;color:#2A9D8F;margin-bottom:8px;font-size:14px">${params[0].axisValue}</div>`
          params.forEach((item: any) => {
            const marker = `<span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${item.color};margin-right:8px;box-shadow:0 0 6px ${item.color}"></span>`
            result += `<div style="margin:4px 0;color:#606266">${marker}${item.seriesName}: <span style="font-weight:600;color:${item.color}">${item.value?.toLocaleString()}</span> 人</div>`
          })
          return result
        }
      },
      legend: {
        data: ['预测客流', '历史客流'],
        textStyle: { color: '#606266', fontSize: 13 },
        top: 10,
        icon: 'roundRect',
        itemWidth: 20,
        itemHeight: 10,
        itemGap: 25
      },
      grid: { left: '3%', right: '4%', bottom: '8%', top: '15%', containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: dates,
        axisLine: { lineStyle: { color: '#e5e7eb', width: 1 } },
        axisLabel: { color: '#909399', fontSize: 12, margin: 12 },
        axisTick: { show: false }
      },
      yAxis: {
        type: 'value',
        name: '游客数量',
        nameTextStyle: { color: '#909399', fontSize: 12, padding: [0, 40, 0, 0] },
        axisLine: { show: false },
        axisLabel: { color: '#909399', fontSize: 12, formatter: (value: number) => value >= 1000 ? (value / 1000).toFixed(1) + 'k' : value },
        splitLine: { lineStyle: { color: '#F0F2F5', type: 'dashed' } }
      },
      series: [
        {
          name: '预测客流',
          type: 'line',
          smooth: 0.4,
          lineStyle: {
            width: 3,
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#2A9D8F' },
              { offset: 0.5, color: '#457B9D' },
              { offset: 1, color: '#2A9D8F' }
            ]),
            shadowColor: 'rgba(42, 157, 143, 0.4)',
            shadowBlur: 12,
            shadowOffsetY: 4
          },
          symbol: 'circle',
          symbolSize: 10,
          itemStyle: {
            color: '#2A9D8F',
            borderColor: '#fff',
            borderWidth: 2,
            shadowColor: 'rgba(42, 157, 143, 0.6)',
            shadowBlur: 8
          },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(42, 157, 143, 0.2)' },
              { offset: 0.5, color: 'rgba(42, 157, 143, 0.08)' },
              { offset: 1, color: 'rgba(42, 157, 143, 0)' }
            ])
          },
          emphasis: { scale: true, itemStyle: { shadowBlur: 15, shadowColor: 'rgba(42, 157, 143, 0.8)' } },
          data: predictedData
        },
        {
          name: '历史客流',
          type: 'line',
          smooth: 0.4,
          symbol: 'diamond',
          symbolSize: 6,
          lineStyle: { width: 2, type: 'dashed', color: '#457B9D', shadowColor: 'rgba(69, 123, 157, 0.4)', shadowBlur: 6 },
          itemStyle: { color: '#457B9D', borderColor: '#fff', borderWidth: 1 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(69, 123, 157, 0.15)' },
              { offset: 1, color: 'rgba(69, 123, 157, 0.02)' }
            ])
          },
          data: historicalData
        }
      ]
    }
  } else if (type === 'bar') {
    // 柱状图配置 - 浅色主题
    option = {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(42, 157, 143, 0.08)' } },
        backgroundColor: '#FFFFFF',
        borderColor: '#EBEEF5',
        borderWidth: 1,
        padding: [12, 16],
        textStyle: { color: '#303133', fontSize: 13 },
        extraCssText: 'box-shadow: 0 4px 12px rgba(0,0,0,0.1);',
        formatter: (params: any) => {
          let result = `<div style="font-weight:600;color:#2A9D8F;margin-bottom:8px;font-size:14px">${params[0].axisValue}</div>`
          params.forEach((item: any) => {
            result += `<div style="margin:4px 0;color:#606266">${item.seriesName}: <span style="font-weight:600;color:${item.color}">${item.value?.toLocaleString()}</span> 人</div>`
          })
          return result
        }
      },
      legend: {
        data: ['预测客流', '历史客流'],
        textStyle: { color: '#606266', fontSize: 13 },
        top: 10,
        icon: 'roundRect',
        itemWidth: 20,
        itemHeight: 10,
        itemGap: 25
      },
      grid: { left: '3%', right: '4%', bottom: '8%', top: '15%', containLabel: true },
      xAxis: {
        type: 'category',
        data: dates,
        axisLine: { lineStyle: { color: '#e5e7eb', width: 1 } },
        axisLabel: { color: '#909399', fontSize: 12, rotate: 30, margin: 12 },
        axisTick: { show: false }
      },
      yAxis: {
        type: 'value',
        name: '游客数量',
        nameTextStyle: { color: '#909399', fontSize: 12 },
        axisLine: { show: false },
        axisLabel: { color: '#909399', fontSize: 12, formatter: (value: number) => value >= 1000 ? (value / 1000).toFixed(1) + 'k' : value },
        splitLine: { lineStyle: { color: '#F0F2F5', type: 'dashed' } }
      },
      series: [
        {
          name: '预测客流',
          type: 'bar',
          barWidth: '30%',
          barGap: '20%',
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#06b6d4' },
              { offset: 0.5, color: '#0ea5e9' },
              { offset: 1, color: '#3b82f6' }
            ]),
            shadowColor: 'rgba(6, 182, 212, 0.4)',
            shadowBlur: 10,
            shadowOffsetY: 2
          },
          emphasis: { itemStyle: { shadowBlur: 15, shadowColor: 'rgba(6, 182, 212, 0.7)' } },
          data: predictedData
        },
        {
          name: '历史客流',
          type: 'bar',
          barWidth: '30%',
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#7EC8E3' },
              { offset: 0.5, color: '#457B9D' },
              { offset: 1, color: '#2A6F97' }
            ]),
            shadowColor: 'rgba(69, 123, 157, 0.4)',
            shadowBlur: 10,
            shadowOffsetY: 2
          },
          emphasis: { itemStyle: { shadowBlur: 15, shadowColor: 'rgba(69, 123, 157, 0.7)' } },
          data: historicalData
        }
      ]
    }
  } else if (type === 'heatmap') {
    // 热力图配置 - 浅色主题
    const hours = ['8:00', '10:00', '12:00', '14:00', '16:00', '18:00']
    const heatmapData: any[] = []
    
    dates.forEach((date: string, dateIdx: number) => {
      const baseFlow = predictedData[dateIdx]
      hours.forEach((hour, hourIdx) => {
        // 模拟不同时段的客流分布
        let flowRatio = 1.0
        if (hourIdx === 0) flowRatio = 0.3  // 早上8点
        else if (hourIdx === 1) flowRatio = 0.7  // 上午10点
        else if (hourIdx === 2) flowRatio = 1.0  // 中午12点
        else if (hourIdx === 3) flowRatio = 0.9  // 下午2点
        else if (hourIdx === 4) flowRatio = 0.6  // 下午4点
        else if (hourIdx === 5) flowRatio = 0.4  // 下午6点
        
        heatmapData.push([dateIdx, hourIdx, Math.floor(baseFlow * flowRatio)])
      })
    })
    
    // 找出最大值和最小值用于颜色映射
    const values = heatmapData.map(item => item[2])
    const maxValue = Math.max(...values)
    const minValue = Math.min(...values)
    
    option = {
      backgroundColor: 'transparent',
      tooltip: {
        position: 'top',
        backgroundColor: '#FFFFFF',
        borderColor: '#EBEEF5',
        borderWidth: 1,
        padding: [12, 16],
        textStyle: { color: '#303133', fontSize: 13 },
        extraCssText: 'box-shadow: 0 4px 12px rgba(0,0,0,0.1);',
        formatter: (params: any) => {
          const date = dates[params.data[0]]
          const hour = hours[params.data[1]]
          const value = params.data[2]
          const ratio = value / maxValue
          let level = '低峰'
          let levelColor = '#3b82f6'
          if (ratio > 0.8) { level = '高峰'; levelColor = '#f43f5e' }
          else if (ratio > 0.6) { level = '较高'; levelColor = '#f59e0b' }
          else if (ratio > 0.4) { level = '中等'; levelColor = '#06b6d4' }
          
          return `<div style="font-weight:600;color:#2A9D8F;margin-bottom:8px;font-size:14px">${date} ${hour}</div>
                  <div style="margin:4px 0;color:#606266">预测客流: <span style="font-weight:600;color:#06b6d4">${value.toLocaleString()}</span> 人</div>
                  <div style="margin:4px 0;color:#606266">拥挤程度: <span style="font-weight:600;color:${levelColor}">${level}</span></div>`
        }
      },
      grid: {
        left: '3%',
        right: '15%',
        bottom: '8%',
        top: '5%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: dates,
        splitArea: { show: true, areaStyle: { color: ['rgba(42, 157, 143, 0.02)', 'rgba(42, 157, 143, 0.04)'] } },
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        axisLabel: { color: '#909399', rotate: 30, fontSize: 11 },
        axisTick: { show: false }
      },
      yAxis: {
        type: 'category',
        data: hours,
        splitArea: { show: true, areaStyle: { color: ['rgba(42, 157, 143, 0.02)', 'rgba(42, 157, 143, 0.04)'] } },
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        axisLabel: { color: '#909399', fontSize: 11 },
        axisTick: { show: false }
      },
      visualMap: {
        min: minValue,
        max: maxValue,
        calculable: true,
        orient: 'vertical',
        right: '2%',
        top: 'center',
        itemWidth: 15,
        itemHeight: 120,
        textStyle: { color: '#909399', fontSize: 11 },
        inRange: {
          color: ['#ecfeff', '#cffafe', '#a5f3fc', '#67e8f9', '#22d3ee', '#06b6d4', '#0891b2', '#0e7490']
        },
        handleStyle: {
          borderColor: '#2A9D8F'
        }
      },
      series: [
        {
          name: '客流热力',
          type: 'heatmap',
          data: heatmapData,
          label: { show: false },
          itemStyle: {
            borderColor: '#FFFFFF',
            borderWidth: 2,
            borderRadius: 4
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 15,
              shadowColor: 'rgba(42, 157, 143, 0.6)',
              borderColor: '#2A9D8F',
              borderWidth: 3
            }
          }
        }
      ]
    }
  }
  
  predictionChart.setOption(option, true)  // true表示不合并配置，完全替换
  setTimeout(() => {
    predictionChart?.resize()
  }, 50)
}

// 组件挂载时加载景区列表
onMounted(() => {
  loadScenicOptions()
})

onBeforeUnmount(() => {
  if (predictionChart) { predictionChart.dispose(); predictionChart = null }
  if (factorsChart) { factorsChart.dispose(); factorsChart = null }
  if (hourlyChart) { hourlyChart.dispose(); hourlyChart = null }
})
</script>

<style scoped>
.flow-prediction-container {
  @apply py-4;
}

/* 标题样式 - 现代简约 */
h3.text-gray-900 {
  position: relative;
  padding-left: 14px;
  font-weight: 600;
}

h3.text-gray-900::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 18px;
  background: #06b6d4;
  border-radius: 4px;
}

/* Element Plus 组件样式覆盖 - 浅色主题 */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-textarea__inner) {
  background-color: #f9fafb !important;
  box-shadow: 0 0 0 1px #e5e7eb inset !important;
  color: #111827 !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover),
:deep(.el-textarea__inner:hover) {
  background-color: #ffffff !important;
  box-shadow: 0 0 0 1px #cbd5e1 inset !important;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused),
:deep(.el-textarea__inner:focus) {
  background-color: #ffffff !important;
  box-shadow: 0 0 0 2px rgba(6, 182, 212, 0.2) inset, 0 0 0 1px #06b6d4 inset !important;
}

:deep(.el-input__inner) {
  color: #111827 !important;
  height: 40px;
}

:deep(.el-range-editor.el-input__wrapper) {
  padding: 0 12px;
}

:deep(.el-range-separator) {
  color: #6b7280 !important;
}

/* 复选框美化 */
:deep(.el-checkbox) {
  margin-right: 0 !important;
  height: auto;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.2s;
}

:deep(.el-checkbox:hover) {
  background-color: #f3f4f6;
}

:deep(.el-checkbox__inner) {
  border-color: #d1d5db;
  border-radius: 4px;
  transition: all 0.2s;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #06b6d4;
  border-color: #06b6d4;
}

:deep(.el-checkbox__label) {
  color: #4b5563 !important;
  padding-left: 8px;
}

:deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
  color: #0e7490 !important;
  font-weight: 500;
}

/* 单选框美化 */
:deep(.el-radio) {
  margin-right: 0 !important;
  height: auto;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
}

:deep(.el-radio:hover) {
  background-color: #f3f4f6;
}

:deep(.el-radio__inner) {
  border-color: #d1d5db;
}

:deep(.el-radio__input.is-checked .el-radio__inner) {
  background: #06b6d4;
  border-color: #06b6d4;
}

:deep(.el-radio__label) {
  color: #4b5563 !important;
  padding-left: 8px;
}

:deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #0e7490 !important;
  font-weight: 500;
}

/* AI 建议 Markdown 样式 - 浅色版 */
.markdown-content {
  color: #374151;
  line-height: 1.7;
  font-size: 14px;
}

.markdown-content h1,
.markdown-content h2,
.markdown-content h3 {
  color: #111827;
  font-weight: 600;
  margin-top: 1.5em;
  margin-bottom: 0.8em;
  line-height: 1.3;
}

.markdown-content p {
  margin-bottom: 1em;
}

.markdown-content strong {
  color: #06b6d4;
  font-weight: 600;
}

.markdown-content ul,
.markdown-content ol {
  padding-left: 1.5em;
  margin-bottom: 1em;
}

.markdown-content ul {
  list-style-type: disc;
}

.markdown-content li {
  margin-bottom: 0.4em;
}

.markdown-content code {
  background-color: #f3f4f6;
  color: #0e7490;
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.9em;
}

.markdown-content pre {
  background-color: #1f2937;
  color: #f3f4f6;
  padding: 1em;
  border-radius: 8px;
  overflow-x: auto;
  margin-bottom: 1em;
}

.markdown-content pre code {
  background-color: transparent;
  color: inherit;
  padding: 0;
}

/* 按钮动画 */
.el-button {
  transition: all 0.2s;
}

.el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.el-button:active {
  transform: translateY(0);
}
</style> 