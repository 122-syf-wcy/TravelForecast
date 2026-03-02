<template>
  <div class="terrain-map-container">
    <div ref="mapContainer" class="map-container"></div>
    <div class="map-legend">
      <div class="legend-title">六盘水市景区分布</div>
      <div class="legend-items">
        <div class="legend-item">
          <div class="legend-icon" style="background-color: #1F2937;"></div>
          <span>自然景观</span>
        </div>
        <div class="legend-item">
          <div class="legend-icon" style="background-color: #8A2BE2;"></div>
          <span>人文景观</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { getScenicSpots } from '@/api/scenic'

// 声明高德地图全局变量类型
declare global {
  interface Window {
    AMap: any
  }
}

const emit = defineEmits(['markerClick'])

const mapContainer = ref<HTMLElement | null>(null)
let map: any = null
let markers: any[] = []

// 景区数据接口
interface ScenicSpotData {
  id: number
  name: string
  coordinates: [number, number]
  category: string
  color: string
  description: string
}

// 景区数据 - 从API获取
const scenicSpots = ref<ScenicSpotData[]>([])

// 从API加载景区数据
const loadScenicData = async () => {
  try {
    console.log('📡 开始从API加载景区数据...')
    const res: any = await getScenicSpots({ city: '六盘水', size: 100 })
    const data = res?.data || res
    const list = data?.list || data?.records || (Array.isArray(data) ? data : [])

    if (list && list.length > 0) {
      // 创建景区名称到精确坐标的映射
      const preciseCoords: Record<string, [number, number]> = {
        '梅花山风景区': [104.7892, 26.6328],
        '玉舍国家森林公园': [104.6845, 26.6012],
        '乌蒙大草原': [104.4156, 25.9823],
        '水城古镇': [104.8523, 26.5478],
        '明湖国家湿地公园': [104.8312, 26.5756]
      }

      const baseSpots = list.map((spot: any) => {
        const precise = preciseCoords[spot.name]
        if (precise) {
          return {
            id: spot.id,
            name: spot.name,
            coordinates: precise,
            category: spot.category || '自然景观',
            color: (spot.category === '人文历史' || spot.category === '人文景观') ? '#8A2BE2' : '#00FEFC',
            description: spot.address || spot.description || ''
          }
        }

        const hasCoords = spot.longitude && spot.latitude
        return {
          id: spot.id,
          name: spot.name,
          coordinates: hasCoords
            ? ([parseFloat(spot.longitude), parseFloat(spot.latitude)] as [number, number])
            : ([0, 0] as [number, number]),
          category: spot.category || '自然景观',
          color: (spot.category === '人文历史' || spot.category === '人文景观') ? '#8A2BE2' : '#00FEFC',
          description: spot.address || spot.description || ''
        }
      })

      scenicSpots.value = baseSpots.filter((spot: any) => spot.coordinates[0] !== 0 && spot.coordinates[1] !== 0)
      console.log(`✅ 加载 ${scenicSpots.value.length} 个景区数据`)
    } else {
      console.warn('⚠️ API返回空数据，使用默认数据')
      useDefaultData()
    }
  } catch (error) {
    console.error('❌ 加载景区数据失败:', error)
    useDefaultData()
  }
}

// 使用默认数据
const useDefaultData = () => {
  scenicSpots.value = [
    {
      id: 1,
      name: '梅花山风景区',
      coordinates: [104.7892, 26.6328],
      category: '自然景观',
      color: '#00FEFC',
      description: '贵州省六盘水市钟山区梅花山国际度假区'
    },
    {
      id: 2,
      name: '玉舍国家森林公园',
      coordinates: [104.6845, 26.6012],
      category: '自然景观',
      color: '#00FEFC',
      description: '贵州省六盘水市水城区玉舍镇'
    },
    {
      id: 3,
      name: '乌蒙大草原',
      coordinates: [104.4156, 25.9823],
      category: '自然景观',
      color: '#00FEFC',
      description: '贵州省六盘水市盘州市乌蒙镇'
    },
    {
      id: 4,
      name: '水城古镇',
      coordinates: [104.8523, 26.5478],
      category: '人文景观',
      color: '#8A2BE2',
      description: '贵州省六盘水市钟山区'
    },
    {
      id: 5,
      name: '明湖国家湿地公园',
      coordinates: [104.8312, 26.5756],
      category: '自然景观',
      color: '#00FEFC',
      description: '贵州省六盘水市钟山区明湖路'
    }
  ]
}

onMounted(async () => {
  console.log('🎬 组件挂载，开始加载数据...')
  
  // 先加载景区数据
  await loadScenicData()
  
  if (!mapContainer.value) {
    console.error('❌ 地图容器不存在！')
    return
  }
  
  console.log('📦 地图容器已就绪')
  
  const rect = mapContainer.value.getBoundingClientRect()
  console.log('📏 容器初始尺寸:', { width: rect.width, height: rect.height })
  
  if (rect.width === 0 || rect.height === 0) {
    console.warn('⚠️ 容器初始尺寸为0，延迟500ms后再初始化...')
    setTimeout(() => {
      initMap()
    }, 500)
    return
  }
  
  initMap()
})

const initMap = () => {
  if (!mapContainer.value) return
  
  // 检查高德地图API是否加载
  if (!window.AMap) {
    console.error('❌ 高德地图API未加载，500ms后重试...')
    setTimeout(initMap, 500)
    return
  }
  
  console.log('🚀 开始初始化高德地图...')
  
  try {
    // 创建高德地图实例 - 3D视图
    map = new window.AMap.Map(mapContainer.value, {
      zoom: 9,
      center: [104.65, 26.35],
      pitch: 50, // 3D视角倾斜角度
      viewMode: '3D', // 开启3D视图
      mapStyle: 'amap://styles/normal', // 深色主题
      showLabel: true,
      features: ['bg', 'road', 'building', 'point']
    })
    
    console.log('✅ 高德地图创建成功')
    console.log('📍 地图中心:', map.getCenter())
    console.log('🔍 缩放级别:', map.getZoom())
    
    // 加载高德地图插件
    window.AMap.plugin(['AMap.Scale', 'AMap.ToolBar', 'AMap.ControlBar'], () => {
      // 添加比例尺
      const scale = new window.AMap.Scale()
      map.addControl(scale)
      
      // 添加工具条
      const toolbar = new window.AMap.ToolBar({
        position: { top: '10px', right: '10px' }
      })
      map.addControl(toolbar)
      
      // 添加3D控制器（旋转、俯仰）
      const controlBar = new window.AMap.ControlBar({
        position: { top: '60px', right: '10px' }
      })
      map.addControl(controlBar)
      
      console.log('🧭 地图控制器已添加')
    })
    
    // 地图加载完成后添加景区标记
    map.on('complete', () => {
      console.log('✅ 地图加载完成！')
      console.log(`📍 开始添加 ${scenicSpots.value.length} 个景区标记`)
      
      // 添加景区标记
      scenicSpots.value.forEach((spot, index) => {
        if (!spot.coordinates || isNaN(spot.coordinates[0]) || isNaN(spot.coordinates[1]) ||
            spot.coordinates[0] === 0 || spot.coordinates[1] === 0) {
          console.warn(`  ⚠️ 跳过无效坐标的景区: ${spot.name}`)
          return
        }
        
        console.log(`  添加标记 ${index + 1}/${scenicSpots.value.length}: ${spot.name} [${spot.coordinates[0]}, ${spot.coordinates[1]}]`)
        
        // 创建自定义标记HTML - 脉冲动画效果
        const markerContent = `
          <div class="scenic-marker-wrapper" style="position: relative;">
            <div class="scenic-marker-pulse" style="
              position: absolute;
              width: 60px;
              height: 60px;
              left: -10px;
              top: -10px;
              background-color: ${spot.color};
              border-radius: 50%;
              opacity: 0.3;
              animation: pulse-ring 2s ease-out infinite;
            "></div>
            <div class="scenic-marker" style="
              position: relative;
              width: 40px;
              height: 40px;
              background: linear-gradient(135deg, ${spot.color}, ${spot.color}88);
              border: 3px solid rgba(255, 255, 255, 0.95);
              border-radius: 50%;
              box-shadow: 0 4px 20px ${spot.color}80, 0 0 30px ${spot.color}40;
              cursor: pointer;
              display: flex;
              align-items: center;
              justify-content: center;
              font-size: 18px;
              z-index: 1;
            ">
              <span style="color: white; font-weight: bold; ">
                ${spot.category === '自然景观' ? '🏔️' : '🏛️'}
              </span>
            </div>
          </div>
          <style>
            @keyframes pulse-ring {
              0% { transform: scale(0.8); opacity: 0.5; }
              50% { transform: scale(1.2); opacity: 0.2; }
              100% { transform: scale(0.8); opacity: 0.5; }
            }
          </style>
        `
        
        // 创建高德地图标记
        const marker = new window.AMap.Marker({
          position: new window.AMap.LngLat(spot.coordinates[0], spot.coordinates[1]),
          content: markerContent,
          offset: new window.AMap.Pixel(-20, -20)
        })
        
        // 创建信息窗体 - 美化样式
        const infoWindow = new window.AMap.InfoWindow({
          content: `
            <div style="
              padding: 12px 16px;
              background: #FFFFFF;
              border-radius: 10px;
              border: 1px solid ${spot.color}60;
              box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
              min-width: 180px;
            ">
              <h4 style="
                margin: 0 0 8px 0;
                color: ${spot.color};
                font-size: 15px;
                font-weight: 600;
                
              ">
                ${spot.category === '自然景观' ? '🏔️' : '🏛️'} ${spot.name}
              </h4>
              <p style="
                margin: 0 0 6px 0;
                color: #4B5563;
                font-size: 12px;
                line-height: 1.5;
              ">
                ${spot.description}
              </p>
              <div style="
                display: flex;
                align-items: center;
                gap: 6px;
                margin-top: 8px;
                padding-top: 8px;
                border-top: 1px solid #E5E7EB;
              ">
                <span style="
                  display: inline-block;
                  padding: 2px 8px;
                  background: ${spot.color}30;
                  color: ${spot.color};
                  font-size: 11px;
                  border-radius: 10px;
                  border: 1px solid ${spot.color}50;
                ">${spot.category}</span>
              </div>
            </div>
          `,
          offset: new window.AMap.Pixel(0, -35),
          closeWhenClickMap: true
        })
        
        // 标记点击事件
        marker.on('click', () => {
          console.log(`🎯 点击景区标记: ${spot.name}`)
          const pos = marker.getPosition()
          if (pos && !isNaN(pos.getLng()) && !isNaN(pos.getLat())) {
            infoWindow.open(map, pos)
          }
          emit('markerClick', spot)
        })
        
        // 标记鼠标悬停事件
        marker.on('mouseover', () => {
          const pos = marker.getPosition()
          if (pos && !isNaN(pos.getLng()) && !isNaN(pos.getLat())) {
            infoWindow.open(map, pos)
          }
        })
        
        marker.on('mouseout', () => {
          infoWindow.close()
        })
        
        // 添加标记到地图
        marker.setMap(map)
        markers.push(marker)
        
        console.log(`  ✓ 标记 ${spot.name} 已添加到地图`)
      })
      
      console.log(`🎉 所有 ${scenicSpots.value.length} 个景区标记添加完成！`)
    })
    
  } catch (error) {
    console.error('❌ 地图初始化失败:', error)
  }
}

onBeforeUnmount(() => {
  console.log('🧹 组件卸载，清理地图资源...')
  
  // 清除所有标记
  markers.forEach(marker => {
    marker.setMap(null)
  })
  markers = []
  
  // 销毁地图实例
  if (map) {
    map.destroy()
    map = null
    console.log('✅ 地图资源已清理')
  }
})
</script>

<style scoped>
.terrain-map-container {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 600px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.3);
}

.map-container {
  width: 100%;
  height: 100%;
  min-height: 600px;
  background: #F0F2F5;
}

/* 图例样式 */
.map-legend {
  position: absolute;
  bottom: 30px;
  left: 20px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 15px 20px;
  border-radius: 12px;
  border: 1px solid #E5E7EB;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  z-index: 10;
}

.legend-title {
  font-size: 14px;
  font-weight: 600;
  color: #1F2937;
  margin-bottom: 12px;
  
}

.legend-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.legend-icon {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 0 10px currentColor;
}

.legend-item span {
  font-size: 13px;
  color: #4B5563;
}

/* 全局样式 - 高德地图控件美化 */
:deep(.amap-logo),
:deep(.amap-copyright) {
  opacity: 0.6 !important;
}

:deep(.amap-toolbar) {
  background: #FFFFFF !important;
  border: 1px solid #E5E7EB !important;
  border-radius: 8px !important;
}

:deep(.amap-scale-text) {
  background: #FFFFFF !important;
  color: #1F2937 !important;
  border: 1px solid #E5E7EB !important;
}
</style>
