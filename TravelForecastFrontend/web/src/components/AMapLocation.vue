<template>
  <div class="amap-container">
    <div id="amap-container" :style="{ width: width, height: height }"></div>
    <div v-if="loading" class="loading-overlay">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>正在加载地图...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

interface Props {
  // 景区地址
  address?: string
  // 景区经纬度
  longitude?: number
  latitude?: number
  // 景区名称
  scenicName?: string
  // 地图宽度
  width?: string
  // 地图高度
  height?: string
  // 是否显示交通路线
  showTraffic?: boolean
  // 是否显示周边
  showNearby?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  address: '',
  longitude: 0,
  latitude: 0,
  scenicName: '',
  width: '100%',
  height: '300px',
  showTraffic: true,
  showNearby: false
})

const loading = ref(true)
let map: any = null
let marker: any = null
let AMap: any = null
let geocoder: any = null

// 加载高德地图API
const loadAMapScript = (): Promise<void> => {
  return new Promise((resolve, reject) => {
    if (window.AMap) {
      AMap = window.AMap
      resolve()
      return
    }

    const script = document.createElement('script')
    script.type = 'text/javascript'
    script.src = `https://webapi.amap.com/maps?v=2.0&key=339146bdb9038c3caf85a7aca9c9bb7f&plugin=AMap.Geocoder,AMap.Driving,AMap.Transfer,AMap.Walking`
    script.onload = () => {
      AMap = window.AMap
      resolve()
    }
    script.onerror = () => {
      reject(new Error('高德地图加载失败'))
    }
    document.head.appendChild(script)
  })
}

// 初始化地图
const initMap = async () => {
  try {
    loading.value = true
    
    // 加载高德地图
    await loadAMapScript()
    
    // 创建地图实例
    map = new AMap.Map('amap-container', {
      zoom: 15,
      center: [104.830392, 26.592745], // 默认中心点（六盘水）
      mapStyle: 'amap://styles/dark', // 使用深色主题
      viewMode: '3D',
      pitch: 50
    })

    // 如果提供了经纬度，直接使用
    if (props.longitude && props.latitude) {
      const position = [props.longitude, props.latitude]
      map.setCenter(position)
      addMarker(position)
      
      // 如果需要显示交通
      if (props.showTraffic) {
        addTrafficLayer()
      }
      
      // 如果需要显示周边
      if (props.showNearby) {
        searchNearby(position)
      }
    } 
    // 如果只提供了地址，使用地理编码
    else if (props.address) {
      geocodeAddress(props.address)
    }
    
    loading.value = false
  } catch (error) {
    console.error('地图初始化失败:', error)
    ElMessage.error('地图加载失败，请刷新重试')
    loading.value = false
  }
}

// 添加标记
const addMarker = (position: number[]) => {
  if (marker) {
    marker.setMap(null)
  }
  
  marker = new AMap.Marker({
    position: position,
    title: props.scenicName || '景区位置',
    icon: new AMap.Icon({
      size: new AMap.Size(40, 50),
      image: '//a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-red.png',
      imageSize: new AMap.Size(40, 50)
    }),
    offset: new AMap.Pixel(-20, -50)
  })
  
  marker.setMap(map)
  
  // 添加信息窗口
  const infoWindow = new AMap.InfoWindow({
    content: `
      <div style="padding: 10px; color: #333;">
        <h3 style="margin: 0 0 8px 0; font-size: 16px; font-weight: bold;">${props.scenicName || '景区'}</h3>
        <p style="margin: 0; font-size: 14px; color: #666;">${props.address || '景区位置'}</p>
      </div>
    `,
    offset: new AMap.Pixel(0, -30)
  })
  
  marker.on('click', () => {
    infoWindow.open(map, marker.getPosition())
  })
}

// 地理编码（地址转坐标）
const geocodeAddress = (address: string) => {
  geocoder = new AMap.Geocoder({
    city: '六盘水市'
  })
  
  geocoder.getLocation(address, (status: string, result: any) => {
    if (status === 'complete' && result.geocodes.length) {
      const location = result.geocodes[0].location
      const position = [location.lng, location.lat]
      map.setCenter(position)
      addMarker(position)
      
      if (props.showTraffic) {
        addTrafficLayer()
      }
      
      if (props.showNearby) {
        searchNearby(position)
      }
    } else {
      ElMessage.warning('地址解析失败，使用默认位置')
    }
  })
}

// 添加实时交通图层
const addTrafficLayer = () => {
  const trafficLayer = new AMap.TileLayer.Traffic({
    zIndex: 10,
    opacity: 0.8
  })
  trafficLayer.setMap(map)
}

// 搜索周边（POI搜索）
const searchNearby = (position: number[]) => {
  AMap.plugin('AMap.PlaceSearch', () => {
    const placeSearch = new AMap.PlaceSearch({
      type: '公交站|地铁站|停车场',
      pageSize: 10,
      pageIndex: 1,
      city: '六盘水市',
      autoFitView: false
    })
    
    placeSearch.searchNearBy('', position, 1000, (status: string, result: any) => {
      if (status === 'complete' && result.poiList.pois.length) {
        result.poiList.pois.forEach((poi: any) => {
          const nearbyMarker = new AMap.Marker({
            position: [poi.location.lng, poi.location.lat],
            title: poi.name,
            icon: new AMap.Icon({
              size: new AMap.Size(25, 34),
              image: '//a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-default.png',
              imageSize: new AMap.Size(25, 34)
            }),
            offset: new AMap.Pixel(-13, -30)
          })
          
          nearbyMarker.setMap(map)
          
          const infoWindow = new AMap.InfoWindow({
            content: `
              <div style="padding: 8px; color: #333;">
                <h4 style="margin: 0 0 5px 0; font-size: 14px;">${poi.name}</h4>
                <p style="margin: 0; font-size: 12px; color: #999;">${poi.type}</p>
              </div>
            `,
            offset: new AMap.Pixel(0, -30)
          })
          
          nearbyMarker.on('click', () => {
            infoWindow.open(map, nearbyMarker.getPosition())
          })
        })
      }
    })
  })
}

// 监听属性变化
watch(() => [props.longitude, props.latitude, props.address], () => {
  if (map) {
    if (props.longitude && props.latitude) {
      const position = [props.longitude, props.latitude]
      map.setCenter(position)
      addMarker(position)
    } else if (props.address) {
      geocodeAddress(props.address)
    }
  }
})

onMounted(() => {
  initMap()
})

onBeforeUnmount(() => {
  if (map) {
    map.destroy()
  }
})

// 暴露方法供父组件调用
defineExpose({
  // 获取到景区的路线规划
  planRoute: (startPoint: [number, number], endPoint: [number, number], type: 'driving' | 'walking' | 'transfer' = 'driving') => {
    if (!map || !AMap) return
    
    let routePlanner: any
    
    if (type === 'driving') {
      routePlanner = new AMap.Driving({
        map: map,
        panel: 'route-panel'
      })
      routePlanner.search(startPoint, endPoint)
    } else if (type === 'walking') {
      routePlanner = new AMap.Walking({
        map: map
      })
      routePlanner.search(startPoint, endPoint)
    } else if (type === 'transfer') {
      routePlanner = new AMap.Transfer({
        map: map,
        city: '六盘水市'
      })
      routePlanner.search(startPoint, endPoint)
    }
  }
})
</script>

<style scoped>
.amap-container {
  position: relative;
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 254, 252, 0.1);
}

#amap-container {
  border-radius: 8px;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #00FEFC;
  font-size: 14px;
  gap: 10px;
  z-index: 1000;
}

.loading-overlay .el-icon {
  font-size: 32px;
}

/* 覆盖高德地图默认样式 */
:deep(.amap-logo) {
  display: none !important;
}

:deep(.amap-copyright) {
  opacity: 0.3;
}

:deep(.amap-info-content) {
  border-radius: 8px;
}
</style>

