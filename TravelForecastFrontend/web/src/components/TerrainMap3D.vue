<template>
  <div class="terrain-map-container">
    <div ref="mapContainer" class="map-container"></div>
    <div class="map-legend">
      <div class="legend-title">六盘水市景区分布</div>
      <div class="legend-items">
        <div class="legend-item">
          <div class="legend-icon" style="background-color: #00FEFC;"></div>
          <span>自然风光</span>
        </div>
        <div class="legend-item">
          <div class="legend-icon" style="background-color: #8A2BE2;"></div>
          <span>人文历史</span>
        </div>
        <div class="legend-item">
          <div class="legend-icon" style="background-color: #FF6B35;"></div>
          <span>休闲娱乐</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, defineEmits } from 'vue'
import maplibregl from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'

// MapLibre 不需要 Access Token，完全免费开源

const emit = defineEmits(['markerClick'])

const mapContainer = ref<HTMLElement | null>(null)
let map: maplibregl.Map | null = null

// 六盘水市景区数据
const scenicSpots = [
  {
    id: 1,
    name: '梅花山风景区',
    coordinates: [104.8304, 26.5927], // 示例坐标（需要使用实际坐标）
    category: '自然风光',
    color: '#00FEFC',
    description: '国家4A级景区，山水风光秀丽'
  },
  {
    id: 2,
    name: '玉舍国家森林公园',
    coordinates: [104.712, 26.432],
    category: '自然风光',
    color: '#00FEFC',
    description: '森林覆盖率95%以上，生态环境优美'
  },
  {
    id: 3,
    name: '乌蒙大草原',
    coordinates: [104.923, 26.651],
    category: '自然风光',
    color: '#00FEFC',
    description: '贵州最大的高原草场之一'
  },
  {
    id: 4,
    name: '水城古镇',
    coordinates: [104.958, 26.547],
    category: '人文历史',
    color: '#8A2BE2',
    description: '百年古镇，历史文化底蕴深厚'
  },
  {
    id: 5,
    name: '明湖国家湿地公园',
    coordinates: [104.8504, 26.6027],
    category: '休闲娱乐',
    color: '#FF6B35',
    description: '湖光山色，水上活动丰富'
  }
]

onMounted(() => {
  if (!mapContainer.value) return

  // 初始化地图（使用免费的OSM地图）
  map = new maplibregl.Map({
    container: mapContainer.value,
    style: {
      version: 8,
      sources: {
        'osm': {
          type: 'raster',
          tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
          tileSize: 256,
          attribution: '© OpenStreetMap contributors'
        }
      },
      layers: [
        {
          id: 'osm-layer',
          type: 'raster',
          source: 'osm',
          minzoom: 0,
          maxzoom: 19
        }
      ],
      glyphs: 'https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf'
    },
    center: [104.8304, 26.5927], // 六盘水市中心坐标
    zoom: 10,
    pitch: 45, // 3D视角倾斜角度
    bearing: 0,
    antialias: true
  })

  // 添加导航控制器
  map.addControl(new maplibregl.NavigationControl(), 'top-right')
  
  // 添加全屏控制器
  map.addControl(new maplibregl.FullscreenControl(), 'top-right')

  // 地图加载完成后添加标记
  map.on('load', () => {

    // 添加景区标记
    scenicSpots.forEach((spot) => {
      // 创建自定义标记元素
      const markerEl = document.createElement('div')
      markerEl.className = 'custom-marker'
      markerEl.style.backgroundImage = 'radial-gradient(circle, ' + spot.color + ', transparent)'
      markerEl.style.width = '30px'
      markerEl.style.height = '30px'
      markerEl.style.borderRadius = '50%'
      markerEl.style.cursor = 'pointer'
      markerEl.style.boxShadow = '0 0 20px ' + spot.color
      markerEl.style.animation = 'pulse 2s infinite'

      // 创建弹出窗口
      const popup = new maplibregl.Popup({ offset: 25 }).setHTML(`
        <div style="padding: 10px; min-width: 200px;">
          <h3 style="color: ${spot.color}; margin: 0 0 8px 0; font-size: 16px;">${spot.name}</h3>
          <p style="color: #ccc; margin: 0; font-size: 12px;">${spot.description}</p>
          <div style="margin-top: 8px; padding-top: 8px; border-top: 1px solid #444;">
            <span style="color: #999; font-size: 11px;">${spot.category}</span>
          </div>
        </div>
      `)

      // 添加标记到地图
      const marker = new maplibregl.Marker({ element: markerEl })
        .setLngLat(spot.coordinates as [number, number])
        .setPopup(popup)
        .addTo(map!)

      // 点击标记触发事件
      markerEl.addEventListener('click', () => {
        emit('markerClick', spot)
      })
    })

    // 添加脉冲动画样式
    const style = document.createElement('style')
    style.textContent = `
      @keyframes pulse {
        0%, 100% { transform: scale(1); opacity: 1; }
        50% { transform: scale(1.2); opacity: 0.8; }
      }
      .maplibregl-popup-content, .mapboxgl-popup-content {
        background: rgba(10, 17, 39, 0.95) !important;
        border: 1px solid rgba(0, 254, 252, 0.3) !important;
        border-radius: 8px !important;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5) !important;
      }
      .maplibregl-popup-tip, .mapboxgl-popup-tip {
        border-top-color: rgba(10, 17, 39, 0.95) !important;
      }
    `
    document.head.appendChild(style)
  })

  // 添加旋转动画（可选）
  let rotating = false
  const rotateCamera = (timestamp: number) => {
    if (rotating && map) {
      map.rotateTo((timestamp / 100) % 360, { duration: 0 })
      requestAnimationFrame(rotateCamera)
    }
  }

  // 鼠标进入地图时暂停旋转，离开时恢复
  map.on('mouseenter', () => {
    rotating = false
  })

  map.on('mouseleave', () => {
    // rotating = true // 取消注释以启用自动旋转
    // requestAnimationFrame(rotateCamera)
  })
})

onBeforeUnmount(() => {
  if (map) {
    map.remove()
    map = null
  }
})
</script>

<style scoped>
.terrain-map-container {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(0, 254, 252, 0.3);
  box-shadow: 0 0 30px rgba(0, 254, 252, 0.2);
}

.map-container {
  width: 100%;
  height: 100%;
  min-height: 500px;
}

.map-legend {
  position: absolute;
  top: 20px;
  left: 20px;
  background: rgba(10, 17, 39, 0.9);
  backdrop-filter: blur(10px);
  padding: 15px;
  border-radius: 8px;
  border: 1px solid rgba(0, 254, 252, 0.3);
  z-index: 10;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
}

.legend-title {
  color: #00FEFC;
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 10px;
  font-family: 'Orbitron', sans-serif;
}

.legend-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #ccc;
  font-size: 12px;
}

.legend-icon {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  box-shadow: 0 0 10px currentColor;
}

/* 适配暗色主题 */
:deep(.maplibregl-ctrl-group),
:deep(.mapboxgl-ctrl-group) {
  background: rgba(10, 17, 39, 0.9) !important;
  border: 1px solid rgba(0, 254, 252, 0.3) !important;
}

:deep(.maplibregl-ctrl-group button),
:deep(.mapboxgl-ctrl-group button) {
  background-color: transparent !important;
  color: #00FEFC !important;
}

:deep(.maplibregl-ctrl-group button:hover),
:deep(.mapboxgl-ctrl-group button:hover) {
  background-color: rgba(0, 254, 252, 0.1) !important;
}
</style>


