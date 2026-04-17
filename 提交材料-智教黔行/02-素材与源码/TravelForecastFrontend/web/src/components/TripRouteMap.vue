<template>
  <div class="trip-route-map-container">
    <div ref="mapContainer" class="map-container"></div>
    <div v-if="loading" class="loading-overlay">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>正在规划路线...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import maplibregl from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage, ElNotification } from 'element-plus'

interface ScenicSpotData {
  code: string
  name: string
  longitude: number
  latitude: number
}

interface Props {
  spots: string[]  // 景区ID列表
  transportation?: 'car' | 'public' | 'tour'
  scenicData?: ScenicSpotData[]  // 景区坐标数据
}

const props = withDefaults(defineProps<Props>(), {
  transportation: 'car',
  scenicData: () => []
})

const mapContainer = ref<HTMLElement | null>(null)
const loading = ref(false)
let map: maplibregl.Map | null = null
const routeMarkers: maplibregl.Marker[] = []

// 景区坐标数据（默认值，当没有传入scenicData时使用）
// 支持两种code格式：旧格式(meihuashan)和新格式(MHS001)
const defaultCoordinates: Record<string, [number, number]> = {
  // 旧格式
  'meihuashan': [104.8336, 26.5945],      // 梅花山风景区
  'yushe': [104.7608, 26.6203],           // 玉舍国家森林公园
  'zhijindong': [104.4532, 26.0708],      // 乌蒙大草原
  'shuicheng': [104.9537, 26.5402],       // 水城古镇
  'minghu': [104.8442, 26.5848],          // 明湖国家湿地公园
  'panzhou': [104.9537, 26.5402],         // 盘州古镇（同水城古镇）
  // 新格式（数据库中的spot_code）
  'MHS001': [104.8336, 26.5945],          // 梅花山风景区
  'YSH002': [104.7608, 26.6203],          // 玉舍国家森林公园
  'WMC003': [104.4532, 26.0708],          // 乌蒙大草原
  'SCGZ004': [104.9537, 26.5402],         // 水城古镇
  'MHSD005': [104.8442, 26.5848]          // 明湖国家湿地公园
}

// 获取景区坐标（优先使用传入的scenicData）
const getCoordinates = (spotCode: string): [number, number] | null => {
  // 优先从传入的scenicData中查找
  if (props.scenicData && props.scenicData.length > 0) {
    const spot = props.scenicData.find(s => s.code === spotCode)
    if (spot && spot.longitude && spot.latitude) {
      return [spot.longitude, spot.latitude]
    }
  }
  // 降级使用默认坐标
  return defaultCoordinates[spotCode] || null
}

// 获取景区名称
const getScenicName = (spotCode: string): string => {
  // 优先从传入的scenicData中查找
  if (props.scenicData && props.scenicData.length > 0) {
    const spot = props.scenicData.find(s => s.code === spotCode)
    if (spot) {
      return spot.name
    }
  }
  // 降级使用默认名称
  return scenicNames[spotCode] || spotCode
}

// 景区名称（默认值）
// 支持两种code格式
const scenicNames: Record<string, string> = {
  // 旧格式
  'meihuashan': '梅花山风景区',
  'yushe': '玉舍国家森林公园',
  'zhijindong': '乌蒙大草原',
  'shuicheng': '水城古镇',
  'minghu': '明湖国家湿地公园',
  'panzhou': '水城古镇',
  // 新格式
  'MHS001': '梅花山风景区',
  'YSH002': '玉舍国家森林公园',
  'WMC003': '乌蒙大草原',
  'SCGZ004': '水城古镇',
  'MHSD005': '明湖国家湿地公园'
}

// 景区颜色
const scenicColors: Record<string, string> = {
  // 旧格式
  'meihuashan': '#00FEFC',
  'yushe': '#00FEFC',
  'zhijindong': '#00FEFC',
  'shuicheng': '#8A2BE2',
  'minghu': '#FF6B35',
  'panzhou': '#8A2BE2',
  // 新格式
  'MHS001': '#00FEFC',
  'YSH002': '#00FEFC',
  'WMC003': '#00FEFC',
  'SCGZ004': '#8A2BE2',
  'MHSD005': '#FF6B35'
}

// 初始化地图
const initMap = () => {
  if (!mapContainer.value) return
  
  console.log('🗺️ 初始化行程路线地图...')
  
  // 创建地图实例
  map = new maplibregl.Map({
    container: mapContainer.value,
    style: {
      version: 8,
      sources: {
        'amap-tiles': {
          type: 'raster',
          tiles: [
            'https://webrd01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
            'https://webrd02.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
            'https://webrd03.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}'
          ],
          tileSize: 256,
          attribution: '高德地图'
        }
      },
      layers: [
        {
          id: 'background',
          type: 'background',
          paint: {
            'background-color': '#0A1127'
          }
        },
        {
          id: 'amap',
          type: 'raster',
          source: 'amap-tiles',
          paint: {
            'raster-opacity': 0.85,
            'raster-brightness-min': 0.15,
            'raster-brightness-max': 0.75,
            'raster-saturation': -0.7,
            'raster-contrast': 0.3
          }
        }
      ]
    },
    center: [104.8600, 26.4500],
    zoom: 9.5,
    pitch: 0,
    bearing: 0,
    antialias: true
  })

  // 添加导航控制
  map.addControl(new maplibregl.NavigationControl(), 'top-right')
  
  // 地图加载完成后绘制路线
  map.on('load', () => {
    console.log('✅ 地图加载完成')
    drawRoute()
  })
}

// 绘制路线
const drawRoute = async () => {
  if (!map || !props.spots || props.spots.length === 0) return
  
  loading.value = true
  console.log(`📍 开始绘制路线，景区数量: ${props.spots.length}`)
  console.log(`📍 景区列表: ${props.spots.join(', ')}`)
  console.log(`📍 传入的scenicData: ${props.scenicData?.length || 0} 条`)
  
  // 清除旧标记
  routeMarkers.forEach(marker => marker.remove())
  routeMarkers.length = 0
  
  // 移除旧路线图层
  if (map.getLayer('route-line')) {
    map.removeLayer('route-line')
  }
  if (map.getLayer('route-line-glow')) {
    map.removeLayer('route-line-glow')
  }
  if (map.getSource('route')) {
    map.removeSource('route')
  }
  
  try {
    // 获取景区坐标
    const coordinates: [number, number][] = []
    props.spots.forEach((spotId, index) => {
      const coord = getCoordinates(spotId)
      if (coord) {
        coordinates.push(coord)
        const name = getScenicName(spotId)
        const color = scenicColors[spotId] || '#00FEFC'
        
        console.log(`📍 景区 ${index + 1}: ${name} [${coord[0]}, ${coord[1]}]`)
        
        // 添加标记
        addMarker(coord, index + 1, name, color)
      } else {
        console.warn(`⚠️ 未找到景区 ${spotId} 的坐标`)
      }
    })
    
    if (coordinates.length < 2) {
      loading.value = false
      return
    }
    
    // 调用高德地图路径规划API
    const routePath = await planRoute(coordinates)
    
    if (routePath && routePath.length > 0) {
      // 添加路线到地图
      map.addSource('route', {
        type: 'geojson',
        data: {
          type: 'Feature',
          properties: {},
          geometry: {
            type: 'LineString',
            coordinates: routePath
          }
        }
      })
      
      // 添加路线图层（实线，表示真实路径）
      map.addLayer({
        id: 'route-line',
        type: 'line',
        source: 'route',
        layout: {
          'line-join': 'round',
          'line-cap': 'round'
        },
        paint: {
          'line-color': '#00FEFC',
          'line-width': 5,
          'line-opacity': 0.9
        }
      })
      
      // 添加路线发光效果
      map.addLayer({
        id: 'route-line-glow',
        type: 'line',
        source: 'route',
        layout: {
          'line-join': 'round',
          'line-cap': 'round'
        },
        paint: {
          'line-color': '#00FEFC',
          'line-width': 8,
          'line-opacity': 0.3,
          'line-blur': 4
        }
      }, 'route-line')
      
      // 调整视野包含所有点
      const bounds = new maplibregl.LngLatBounds()
      coordinates.forEach(coord => bounds.extend(coord))
      map.fitBounds(bounds, { padding: 80, duration: 1000 })
      
      console.log('✅ 路线绘制完成')
      ElMessage.success('路线规划完成')
    } else {
      // 如果API失败，使用直线连接
      drawStraightLines(coordinates)
    }
  } catch (error) {
    console.error('❌ 路线规划失败:', error)
    // 降级：使用直线连接景区
    const coordinates: [number, number][] = props.spots
      .map(spotId => scenicCoordinates[spotId])
      .filter(Boolean)
    if (coordinates.length > 1) {
      drawStraightLines(coordinates)
    }
  } finally {
    loading.value = false
  }
}

// 调用高德地图路径规划API（通过后端代理）
const planRoute = async (coordinates: [number, number][]): Promise<[number, number][]> => {
  try {
    console.log('📡 调用高德地图路径规划API...')
    
    if (coordinates.length < 2) return coordinates
    
    const routePath: [number, number][] = []
    
    // 根据出行方式选择API端点
    const apiEndpoint = props.transportation === 'car' ? 'driving' : 
                       props.transportation === 'public' ? 'transit' : 'driving'
    
    // 逐段规划路线
    for (let i = 0; i < coordinates.length - 1; i++) {
      const origin = `${coordinates[i][0]},${coordinates[i][1]}`
      const destination = `${coordinates[i + 1][0]},${coordinates[i + 1][1]}`
      
      console.log(`🛣️ 规划路段 ${i + 1}: ${origin} -> ${destination}`)
      
      // 调用后端代理接口
      const response = await fetch(
        `/api/amap/${apiEndpoint}?origin=${origin}&destination=${destination}${apiEndpoint === 'transit' ? '&city=六盘水' : ''}`
      )
      
      if (!response.ok) {
        console.warn(`⚠️ 路段 ${i + 1} 规划失败，使用直线连接`)
        continue
      }
      
      const result = await response.json()
      
      // 检查返回结果
      if (result.code !== 200) {
        console.warn(`⚠️ API返回错误: ${result.message}`)
        continue
      }
      
      // 解析返回数据（高德API返回的是字符串，需要再解析一次）
      const data = typeof result.data === 'string' ? JSON.parse(result.data) : result.data
      
      // 检查是否有有效路径
      let hasValidPath = false
      
      // 处理驾车路径
      if (data.status === '1' && data.route?.paths?.length > 0) {
        const path = data.route.paths[0]
        const steps = path.steps
        
        if (steps && steps.length > 0) {
          hasValidPath = true
          console.log(`✅ 路段 ${i + 1} 规划成功，包含 ${steps.length} 个步骤`)
          
          // 解析polyline坐标
          steps.forEach((step: any) => {
            if (step.polyline) {
              const polyline = step.polyline
              const points = polyline.split(';')
              points.forEach((point: string) => {
                const coords = point.split(',')
                if (coords.length === 2) {
                  const lng = parseFloat(coords[0])
                  const lat = parseFloat(coords[1])
                  if (!isNaN(lng) && !isNaN(lat)) {
                    routePath.push([lng, lat])
                  }
                }
              })
            }
          })
        }
      }
      
      // 处理公交路径
      if (data.status === '1' && data.route?.transits?.length > 0) {
        const transit = data.route.transits[0]
        if (transit.segments && transit.segments.length > 0) {
          hasValidPath = true
          console.log(`✅ 路段 ${i + 1} 公交规划成功`)
          
          transit.segments.forEach((segment: any) => {
            // 处理步行路段
            if (segment.walking?.steps) {
              segment.walking.steps.forEach((step: any) => {
                if (step.polyline) {
                  const points = step.polyline.split(';')
                  points.forEach((point: string) => {
                    const coords = point.split(',')
                    if (coords.length === 2) {
                      const lng = parseFloat(coords[0])
                      const lat = parseFloat(coords[1])
                      if (!isNaN(lng) && !isNaN(lat)) {
                        routePath.push([lng, lat])
                      }
                    }
                  })
                }
              })
            }
            // 处理公交路段
            if (segment.bus?.buslines?.[0]?.polyline) {
              const points = segment.bus.buslines[0].polyline.split(';')
              points.forEach((point: string) => {
                const coords = point.split(',')
                if (coords.length === 2) {
                  const lng = parseFloat(coords[0])
                  const lat = parseFloat(coords[1])
                  if (!isNaN(lng) && !isNaN(lat)) {
                    routePath.push([lng, lat])
                  }
                }
              })
            }
          })
        }
      }
      
      // 如果公交规划失败，尝试使用驾车路线
      if (!hasValidPath && apiEndpoint === 'transit') {
        console.warn(`⚠️ 路段 ${i + 1} 无公交路线，尝试使用驾车路线`)
        
        // 显示提示（只显示一次）
        if (i === 0) {
          ElNotification({
            title: '公交路线不可用',
            message: '景区之间没有直达公交，已自动切换为驾车路线',
            type: 'warning',
            duration: 5000
          })
        }
        
        // 重新调用驾车API
        const drivingResponse = await fetch(
          `/api/amap/driving?origin=${origin}&destination=${destination}`
        )
        
        if (drivingResponse.ok) {
          const drivingResult = await drivingResponse.json()
          if (drivingResult.code === 200) {
            const drivingData = typeof drivingResult.data === 'string' 
              ? JSON.parse(drivingResult.data) 
              : drivingResult.data
            
            if (drivingData.status === '1' && drivingData.route?.paths?.length > 0) {
              const path = drivingData.route.paths[0]
              const steps = path.steps
              
              hasValidPath = true
              console.log(`✅ 路段 ${i + 1} 改用驾车路线，包含 ${steps.length} 个步骤`)
              
              steps.forEach((step: any) => {
                if (step.polyline) {
                  const points = step.polyline.split(';')
                  points.forEach((point: string) => {
                    const coords = point.split(',')
                    if (coords.length === 2) {
                      const lng = parseFloat(coords[0])
                      const lat = parseFloat(coords[1])
                      if (!isNaN(lng) && !isNaN(lat)) {
                        routePath.push([lng, lat])
                      }
                    }
                  })
                }
              })
            }
          }
        }
      }
      
      if (!hasValidPath) {
        console.warn(`⚠️ 路段 ${i + 1} 无有效路径数据`)
      }
    }
    
    // 如果成功获取路径数据，返回；否则降级到直线
    if (routePath.length > 0) {
      console.log(`✅ 路径规划完成，共 ${routePath.length} 个坐标点`)
      return routePath
    } else {
      console.warn('⚠️ 所有路段规划失败，降级使用直线连接')
      return coordinates
    }
  } catch (error) {
    console.error('❌ 路径规划API调用失败:', error)
    // 降级：返回直线坐标
    return coordinates
  }
}

// 使用直线连接景区（降级方案）
const drawStraightLines = (coordinates: [number, number][]) => {
  if (!map) return
  
  console.log('ℹ️ 使用直线连接景区')
  
  map.addSource('route', {
    type: 'geojson',
    data: {
      type: 'Feature',
      properties: {},
      geometry: {
        type: 'LineString',
        coordinates: coordinates
      }
    }
  })
  
  map.addLayer({
    id: 'route-line',
    type: 'line',
    source: 'route',
    layout: {
      'line-join': 'round',
      'line-cap': 'round'
    },
    paint: {
      'line-color': '#00FEFC',
      'line-width': 4,
      'line-opacity': 0.6,
      'line-dasharray': [3, 3]  // 虚线表示直线连接
    }
  })
  
  // 添加发光效果
  map.addLayer({
    id: 'route-line-glow',
    type: 'line',
    source: 'route',
    layout: {
      'line-join': 'round',
      'line-cap': 'round'
    },
    paint: {
      'line-color': '#00FEFC',
      'line-width': 8,
      'line-opacity': 0.2,
      'line-blur': 6
    }
  }, 'route-line')
  
  // 调整视野
  const bounds = new maplibregl.LngLatBounds()
  coordinates.forEach(coord => bounds.extend(coord))
  map.fitBounds(bounds, { padding: 80, duration: 1000 })
}

// 添加景区标记
const addMarker = (coordinates: [number, number], number: number, name: string, color: string) => {
  if (!map) return
  
  // 创建标记元素
  const el = document.createElement('div')
  el.className = 'trip-marker'
  el.innerHTML = `
    <div class="marker-pin" style="background: ${color}; border-color: ${color};">
      <span class="marker-number">${number}</span>
    </div>
    <div class="marker-shadow"></div>
  `
  
  // 创建弹窗
  const popup = new maplibregl.Popup({ 
    offset: 35,
    closeButton: false
  }).setHTML(`
    <div style="padding: 8px 12px; background: rgba(10, 17, 39, 0.95); border: 1px solid ${color}; border-radius: 8px;">
      <div style="color: ${color}; font-weight: bold; margin-bottom: 4px;">第${number}站</div>
      <div style="color: white; font-size: 14px;">${name}</div>
    </div>
  `)
  
  // 添加到地图
  const marker = new maplibregl.Marker({ element: el, anchor: 'bottom' })
    .setLngLat(coordinates)
    .setPopup(popup)
    .addTo(map)
  
  routeMarkers.push(marker)
}

// 监听景区变化
watch(() => props.spots, () => {
  if (map && map.loaded()) {
    drawRoute()
  }
}, { deep: true })

// 监听出行方式变化
watch(() => props.transportation, () => {
  if (map && map.loaded()) {
    drawRoute()
  }
})

onMounted(() => {
  initMap()
})

onBeforeUnmount(() => {
  if (map) {
    map.remove()
    map = null
  }
})
</script>

<style scoped>
.trip-route-map-container {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 400px;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid rgba(0, 254, 252, 0.5);
  box-shadow: 
    0 0 40px rgba(0, 254, 252, 0.3),
    inset 0 0 60px rgba(0, 254, 252, 0.1);
  background: #0A1127;
}

.map-container {
  width: 100%;
  height: 100%;
  min-height: 400px;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(10, 17, 39, 0.9);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: #00FEFC;
  font-size: 18px;
  z-index: 1000;
}

.loading-overlay .el-icon {
  font-size: 48px;
}

/* 行程标记样式 */
:deep(.trip-marker) {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
}

:deep(.marker-pin) {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 3px solid white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 
    0 4px 10px rgba(0, 0, 0, 0.4),
    0 0 30px currentColor;
  animation: markerBounce 2s ease-in-out infinite;
}

:deep(.marker-number) {
  color: white;
  font-weight: bold;
  font-size: 16px;
  text-shadow: 0 0 8px rgba(0, 0, 0, 0.8);
}

:deep(.marker-shadow) {
  width: 24px;
  height: 8px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 50%;
  margin-top: -4px;
  filter: blur(3px);
  animation: shadowPulse 2s ease-in-out infinite;
}

@keyframes markerBounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

@keyframes shadowPulse {
  0%, 100% {
    transform: scale(1);
    opacity: 0.3;
  }
  50% {
    transform: scale(0.7);
    opacity: 0.5;
  }
}

/* 弹窗样式 */
:deep(.maplibregl-popup-content) {
  background: transparent !important;
  border: none !important;
  padding: 0 !important;
  box-shadow: none !important;
}

:deep(.maplibregl-popup-tip) {
  display: none !important;
}
</style>

