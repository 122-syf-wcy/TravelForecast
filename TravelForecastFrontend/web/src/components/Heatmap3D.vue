<template>
  <div class="heatmap-3d-container">
    <div ref="chartRef" class="chart-container"></div>
    <div class="chart-controls">
      <el-button-group>
        <el-button size="small" @click="rotateChart">
          <el-icon><Refresh /></el-icon>
          旋转视角
        </el-button>
        <el-button size="small" @click="resetView">
          <el-icon><Position /></el-icon>
          重置视图
        </el-button>
      </el-button-group>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'
import 'echarts-gl'
import { Refresh, Position } from '@element-plus/icons-vue'
import { getScenicHeatmapData } from '@/api/heatmap'
import { ElMessage } from 'element-plus'

interface HeatmapData {
  time: string[]
  areas: string[]
  data: number[][]
}

const props = defineProps<{
  scenicId?: number | null
  data?: HeatmapData
}>()

const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null
let autoRotate = true

// 热力图数据（从后端获取）
const heatmapData = ref<HeatmapData | null>(null)
const loading = ref(false)

// 默认数据：当API调用失败时使用
const defaultData: HeatmapData = {
  time: ['8:00', '9:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00'],
  areas: ['入口区', '核心景点A', '核心景点B', '观景台', '休息区', '餐饮区', '商业区', '出口区'],
  data: [
    [0, 0, 15], [0, 1, 35], [0, 2, 45], [0, 3, 38], [0, 4, 22], [0, 5, 18], [0, 6, 12], [0, 7, 8],
    [1, 0, 45], [1, 1, 68], [1, 2, 78], [1, 3, 65], [1, 4, 42], [1, 5, 35], [1, 6, 28], [1, 7, 15],
    [2, 0, 78], [2, 1, 95], [2, 2, 100], [2, 3, 88], [2, 4, 62], [2, 5, 55], [2, 6, 45], [2, 7, 25],
    [3, 0, 88], [3, 1, 98], [3, 2, 95], [3, 3, 92], [3, 4, 75], [3, 5, 68], [3, 6, 58], [3, 7, 32],
    [4, 0, 65], [4, 1, 72], [4, 2, 68], [4, 3, 70], [4, 4, 82], [4, 5, 78], [4, 6, 52], [4, 7, 28],
    [5, 0, 58], [5, 1, 68], [5, 2, 65], [5, 3, 68], [5, 4, 72], [5, 5, 85], [5, 6, 65], [5, 7, 35],
    [6, 0, 72], [6, 1, 82], [6, 2, 78], [6, 3, 75], [6, 4, 62], [6, 5, 70], [6, 6, 88], [6, 7, 45],
    [7, 0, 85], [7, 1, 92], [7, 2, 88], [7, 3, 82], [7, 4, 55], [7, 5, 62], [7, 6, 78], [7, 7, 58],
    [8, 0, 68], [8, 1, 75], [8, 2, 72], [8, 3, 68], [8, 4, 45], [8, 5, 52], [8, 6, 58], [8, 7, 68],
    [9, 0, 38], [9, 1, 48], [9, 2, 45], [9, 3, 42], [9, 4, 28], [9, 5, 32], [9, 6, 35], [9, 7, 78]
  ]
}

// 加载景区热力图数据
const loadHeatmapData = async (scenicId: number) => {
  if (!scenicId) {
    console.warn('景区ID为空，使用默认数据')
    heatmapData.value = defaultData
    return
  }
  
  loading.value = true
  console.log(`📊 正在加载景区ID ${scenicId} 的热力图数据...`)
  
  try {
    const res: any = await getScenicHeatmapData(scenicId)
    
    // 检查返回数据格式
    const data = res?.data || res
    
    if (data && data.time && data.areas && data.data) {
      heatmapData.value = {
        time: data.time || [],
        areas: data.areas || [],
        data: data.data || []
      }
      console.log(`✅ 景区ID ${scenicId} 热力图数据加载成功，数据点数: ${heatmapData.value.data.length}`)
      if (data.avgVisitorCount) {
        console.log(`   平均访客数: ${data.avgVisitorCount}`)
      }
    } else {
      console.warn('热力图数据格式不完整，使用默认数据')
      heatmapData.value = defaultData
    }
  } catch (error) {
    console.warn(`⚠️ 加载景区ID ${scenicId} 热力图数据失败，使用默认数据:`, error)
    // 不显示错误提示，静默使用默认数据
    heatmapData.value = defaultData
  } finally {
    loading.value = false
  }
}

const initChart = () => {
  if (!chartRef.value) return

  // 如果已存在实例，先销毁
  if (chart) {
    chart.dispose()
  }

  chart = echarts.init(chartRef.value)

  // 使用加载的热力图数据
  const data = props.data || heatmapData.value || defaultData
  
  // 3D 坐标轴通用配置（让文字和线条变深）
  const axisCommon = {
    axisLine: {
      lineStyle: { color: '#666' } // 轴线颜色：深灰
    },
    axisLabel: {
      textStyle: { color: '#333', fontSize: 12 } // 文字颜色：接近黑
    },
    splitLine: {
      show: true,
      lineStyle: { color: '#eee', opacity: 0.5 } // 网格线：浅灰（保留空间感）
    }
  }

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      formatter: (params: any) => {
        const value = params.value
        const density = value[2]
        const densityColor = density > 70 ? '#FF6B6B' : density > 40 ? '#FFD93D' : '#6BCB77'
        return `
      <div style="padding: 12px 16px; background: #FFFFFF; border: 1px solid rgba(14, 165, 233, 0.2); border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">
            <div style="color: #0284C7; font-weight: 600; font-size: 14px; margin-bottom: 8px; display: flex; align-items: center; gap: 6px;">
              <span style="width: 8px; height: 8px; background: ${densityColor}; border-radius: 50%; display: inline-block;"></span>
              客流密度分析
            </div>
            <div style="display: grid; gap: 6px;">
              <div style="color: #64748b; font-size: 13px;">⏰ 时间: <span style="color: #1e293b; font-weight: 500;">${data.time[value[0]]}</span></div>
              <div style="color: #64748b; font-size: 13px;">📍 区域: <span style="color: #1e293b; font-weight: 500;">${data.areas[value[1]]}</span></div>
              <div style="color: #64748b; font-size: 13px; margin-top: 4px;">
                👥 人流密度: <span style="color: ${densityColor}; font-size: 18px; font-weight: bold;">${value[2]}%</span>
              </div>
            </div>
          </div>
        `
      }
    },
    visualMap: {
      min: 0,
      max: 100,
      inRange: {
        color: ['#E0F7FA', '#4DB6AC', '#009688', '#00695C'] // 方案A (清凉山水): 从淡青到深蓝
      },
      textStyle: {
        color: '#333', // 让“高/低”文字变黑
        fontSize: 12
      },
      orient: 'vertical',
      left: 'right',
      top: 'center',
      itemWidth: 20,
      itemHeight: 140,
      text: ['高', '低'],
      calculable: true,
      realtime: false,
      borderColor: '#E5E7EB',
      borderWidth: 1,
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      padding: 12,
      borderRadius: 8
    },
    xAxis3D: {
      type: 'category',
      data: data.time,
      name: '时间',
      nameGap: 30,
      nameTextStyle: {
        color: '#1F2937',
        fontSize: 14,
        fontWeight: 'bold'
      },
      ...axisCommon.axisLine,
      ...axisCommon.splitLine,
      axisLabel: {
        ...axisCommon.axisLabel,
        margin: 12
      }
    },
    yAxis3D: {
      type: 'category',
      data: data.areas,
      name: '区域',
      nameGap: 30,
      nameTextStyle: {
        color: '#1F2937',
        fontSize: 14,
        fontWeight: 'bold'
      },
      ...axisCommon.axisLine,
      ...axisCommon.splitLine,
      axisLabel: {
        ...axisCommon.axisLabel,
        margin: 12
      }
    },
    zAxis3D: {
      type: 'value',
      name: '人流密度(%)',
      nameGap: 35,
      nameTextStyle: {
        color: '#1F2937',
        fontSize: 14,
        fontWeight: 'bold'
      },
      ...axisCommon.axisLine,
      ...axisCommon.splitLine,
      axisLabel: {
        ...axisCommon.axisLabel,
        margin: 8
      }
    },
    grid3D: {
      boxWidth: 200,
      boxDepth: 140,
      boxHeight: 80,
      left: -20,
      right: 100,
      top: 20,
      bottom: 20,
      viewControl: {
        projection: 'perspective',
        autoRotate: autoRotate,
        autoRotateSpeed: 4,
        distance: 280,
        alpha: 25,
        beta: 40,
        minAlpha: 5,
        maxAlpha: 90,
        rotateSensitivity: 1,
        zoomSensitivity: 1
      },
      light: {
        main: {
          intensity: 1.2,
          shadow: true,
          shadowQuality: 'medium',
          alpha: 35,
          beta: 50
        },
        ambient: {
          intensity: 0.4
        }
      },
      environment: 'none',
      postEffect: {
        enable: true,
        bloom: {
          enable: true,
          bloomIntensity: 0.1
        },
        SSAO: {
          enable: true,
          radius: 2,
          intensity: 1
        }
      }
    },
    series: [
      {
        type: 'bar3D',
        data: data.data,
        shading: 'realistic',
        realisticMaterial: {
          roughness: 0.3,
          metalness: 0.1
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            color: '#374151',
            fontWeight: 'bold',
            backgroundColor: 'rgba(255, 255, 255, 0.9)',
            borderColor: '#0284C7',
            borderWidth: 1,
            padding: [4, 8],
            borderRadius: 4
          },
          itemStyle: {
            opacity: 1
          }
        },
        itemStyle: {
          opacity: 0.92
        },
        barSize: 12,
        bevelSize: 0.3,
        bevelSmoothness: 2
      }
    ]
  }

  chart.setOption(option)

  // 窗口大小改变时重绘
  window.addEventListener('resize', handleResize)
}

const handleResize = () => {
  chart?.resize()
}

const rotateChart = () => {
  autoRotate = !autoRotate
  initChart()
}

const resetView = () => {
  if (!chart) return
  chart.setOption({
    grid3D: {
      viewControl: {
        alpha: 30,
        beta: 30,
        distance: 220
      }
    }
  })
}

// 调整图表大小（供父组件调用）
const resize = () => {
  console.log('热力图resize被调用')
  if (chart && chartRef.value) {
    console.log('当前容器尺寸:', chartRef.value.clientWidth, 'x', chartRef.value.clientHeight)
    chart.resize()
    // 延迟再次resize确保生效
    setTimeout(() => {
      chart?.resize()
    }, 100)
  } else {
    console.warn('图表实例或容器不存在，重新初始化')
    // 如果图表不存在，重新初始化
    if (chartRef.value) {
      initChart()
    }
  }
}

// 暴露方法给父组件
defineExpose({
  resize
})

onMounted(async () => {
  // 先加载数据
  if (props.scenicId) {
    await loadHeatmapData(props.scenicId)
  } else {
    heatmapData.value = defaultData
  }
  
  // 使用nextTick + setTimeout确保DOM完全渲染
  setTimeout(() => {
    if (chartRef.value) {
      const width = chartRef.value.clientWidth
      const height = chartRef.value.clientHeight
      console.log('热力图容器尺寸:', width, 'x', height)
      
      if (width > 0 && height > 0) {
        initChart()
        // 初始化后立即resize
        setTimeout(() => {
          if (chart) {
            chart.resize()
            console.log('热力图已resize')
          }
        }, 100)
      } else {
        console.warn('容器尺寸为0，延迟重试')
        // 容器尺寸为0，延迟重试
        setTimeout(() => {
          initChart()
          setTimeout(() => chart?.resize(), 100)
        }, 500)
      }
    }
  }, 300)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})

// 监听数据变化
watch(() => props.data, () => {
  initChart()
}, { deep: true })

// 监听景区ID变化
watch(() => props.scenicId, async (newId, oldId) => {
  console.log(`🔄 景区切换: ${oldId} -> ${newId}`)
  if (newId) {
    console.log(`📍 正在加载景区ID ${newId} 的客流热力图数据`)
    // 根据景区ID加载不同的热力图数据
    await loadHeatmapData(newId)
    // 重新初始化图表
    initChart()
  }
})
</script>

<style scoped>
.heatmap-3d-container {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 300px;
  max-height: 100%;
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid #E5E7EB;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  background: #FFFFFF;
  backdrop-filter: blur(10px);
}

.chart-container {
  width: 100%;
  height: 100%;
  min-height: 300px;
  max-height: 550px;
  position: relative;
}

.chart-container::before { display: none; }

.chart-container::after { display: none; }

.chart-controls {
  position: absolute;
  bottom: 20px;
  right: 20px;
  z-index: 10;
  display: flex;
  gap: 10px;
}

:deep(.el-button-group) {
  display: flex;
  gap: 8px;
}

:deep(.el-button-group .el-button) {
  border-radius: 12px !important;
}

:deep(.el-button) {
  background: #FFFFFF !important;
  backdrop-filter: blur(10px) !important;
  border: 1px solid #E5E7EB !important;
  color: #2A9D8F !important;
  border-radius: 12px !important;
  padding: 10px 18px !important;
  transition: all 0.3s ease !important;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  font-weight: 500 !important;
  font-size: 13px !important;
}

:deep(.el-button:hover) {
  background: #F0F9FF !important;
  border-color: #0EA5E9 !important;
  color: #0284C7 !important;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px) !important;
}

:deep(.el-icon) {
  color: #2A9D8F !important;
  margin-right: 4px;
}
</style>


