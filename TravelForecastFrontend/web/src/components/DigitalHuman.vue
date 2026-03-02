<template>
  <div class="digital-human-container">
    <!-- 聊天面板 -->
    <div 
      class="chat-panel" 
      :class="{ 'minimized': isMinimized }"
      :style="panelStyle"
      ref="chatPanel"
    >
      <!-- 最小化时的浮窗 -->
      <div v-if="isMinimized" class="minimized-trigger" @click="toggleMinimize">
        <div class="avatar-circle">
          <img src="/videos/avatar-poster.jpg" alt="Avatar" class="avatar-img" @error="handleImageError">
        </div>
        <div class="status-indicator" :class="currentStatus"></div>
      </div>

      <!-- 展开后的面板 -->
      <template v-else>
        <!-- 顶部拖拽区域 + 头像 + 控制按钮 -->
        <div class="panel-header" @mousedown="startDrag">
          <div class="header-left">
            <div class="mini-avatar" :class="{ 'speaking': isSpeaking }">
              <video 
                ref="avatarVideo" 
                src="/videos/avatar.mp4" 
                poster="/videos/avatar-poster.jpg"
                class="mini-avatar-video" 
                loop 
                muted 
                playsinline
                preload="auto"
              ></video>
              <div class="mini-status" :class="currentStatus"></div>
            </div>
            <div class="header-info">
              <span class="header-name">黔小游</span>
              <span class="header-status">研学导师 · 在线</span>
            </div>
          </div>
          <div class="header-actions">
            <button class="action-btn" @click.stop="toggleMinimize" title="最小化">
              <svg viewBox="0 0 24 24" class="action-icon"><path d="M19 13H5v-2h14v2z"/></svg>
            </button>
          </div>
        </div>

        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer">
          <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
            <div class="message-bubble" v-html="renderMarkdown(msg.content)"></div>
            <div class="message-time">{{ msg.time }}</div>
          </div>
          
          <div class="typing-indicator" :class="{ 'show': isTyping }">
            <div class="typing-dot"></div>
            <div class="typing-dot"></div>
            <div class="typing-dot"></div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-section">
          <button 
            class="mic-btn" 
            :class="{ 'recording': isRecording }"
            @mousedown.stop="startRecording" 
            @mouseup="stopRecording" 
            @mouseleave="stopRecording"
            @touchstart.prevent="startRecording"
            @touchend="stopRecording"
            title="按住说话"
          >
            <svg class="icon" viewBox="0 0 24 24">
              <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm-1 1.93c-3.94-.49-7-3.85-7-7.93h2c0 3.31 2.69 6 6 6s6-2.69 6-6h2c0 4.08-3.06 7.44-7 7.93V19h4v2H8v-2h4v-3.07z" />
            </svg>
          </button>
          
          <div class="input-wrapper">
            <input 
              type="text" 
              class="text-input" 
              v-model="inputText" 
              @keyup.enter="sendMessage"
              @mousedown.stop
              placeholder="输入问题..."
            >
            <button class="send-btn" @click.stop="sendMessage" :disabled="!inputText.trim()">
              <svg class="icon" viewBox="0 0 24 24">
                <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
              </svg>
            </button>
          </div>
        </div>

        <!-- 调整大小手柄 -->
        <div class="resize-handle resize-n" @mousedown.stop="startResize('n', $event)"></div>
        <div class="resize-handle resize-s" @mousedown.stop="startResize('s', $event)"></div>
        <div class="resize-handle resize-e" @mousedown.stop="startResize('e', $event)"></div>
        <div class="resize-handle resize-w" @mousedown.stop="startResize('w', $event)"></div>
        <div class="resize-handle resize-ne" @mousedown.stop="startResize('ne', $event)"></div>
        <div class="resize-handle resize-nw" @mousedown.stop="startResize('nw', $event)"></div>
        <div class="resize-handle resize-se" @mousedown.stop="startResize('se', $event)"></div>
        <div class="resize-handle resize-sw" @mousedown.stop="startResize('sw', $event)"></div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed, reactive, watch } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import { useDigitalHumanStore } from '@/store/digitalHuman'
import aiRequest from '@/utils/aiRequest'

// 使用共享的 store
const digitalHumanStore = useDigitalHumanStore()
const messages = computed(() => digitalHumanStore.messages)
const route = useRoute()

// 面板位置和大小
const panelPosition = reactive({
  top: 80,
  right: 20,
  width: 360,
  height: 520
})

const panelStyle = computed(() => ({
  top: `${panelPosition.top}px`,
  right: `${panelPosition.right}px`,
  width: `${panelPosition.width}px`,
  height: `${panelPosition.height}px`
}))

// 拖拽状态
const isDragging = ref(false)
const isResizing = ref(false)
const resizeDirection = ref('')
const dragStart = reactive({ x: 0, y: 0, top: 0, right: 0, width: 0, height: 0 })

// 本地状态
const inputText = ref('')
const isRecording = ref(false)
const isSpeaking = ref(false)
const isTyping = ref(false)
const currentStatus = ref('idle')
const isMinimized = ref(false)

// DOM 引用
const messagesContainer = ref<HTMLElement | null>(null)
const avatarVideo = ref<HTMLVideoElement | null>(null)
const chatPanel = ref<HTMLElement | null>(null)

// WebSocket - 通过AI服务代理，降级时直连旧数字人后端
const WS_URL_AI_PROXY = `ws://${window.location.hostname}:8081/ai-api/ws/digital-human`
const WS_URL_DIRECT = 'ws://localhost:8083/ws/avatar'
let ws: WebSocket | null = null
let mediaRecorder: MediaRecorder | null = null
let audioChunks: Blob[] = []
let currentAudio: HTMLAudioElement | null = null
let pendingText: string | null = null
let audioTimeout: ReturnType<typeof setTimeout> | null = null  // 音频超时计时器
let reconnectAttempts = 0
const MAX_RECONNECT_ATTEMPTS = 3
let heartbeatInterval: ReturnType<typeof setInterval> | null = null
let useHttpFallback = false  // WebSocket全部失败时降级为HTTP

// ========== AudioContext 解决浏览器自动播放限制 ==========
let audioCtx: AudioContext | null = null
let currentSource: AudioBufferSourceNode | null = null

const ensureAudioContext = (): AudioContext => {
  if (!audioCtx) {
    audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)()
  }
  return audioCtx
}

// 用户在页面上任意一次点击/触摸/按键即永久解锁音频
const unlockAudioContext = () => {
  const ctx = ensureAudioContext()
  if (ctx.state === 'suspended') {
    ctx.resume().then(() => console.log('[Audio] 音频上下文已解锁'))
  }
}

// 拖拽移动
const startDrag = (e: MouseEvent) => {
  if ((e.target as HTMLElement).closest('.action-btn')) return
  isDragging.value = true
  dragStart.x = e.clientX
  dragStart.y = e.clientY
  dragStart.top = panelPosition.top
  dragStart.right = panelPosition.right
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (e: MouseEvent) => {
  if (!isDragging.value) return
  const dx = e.clientX - dragStart.x
  const dy = e.clientY - dragStart.y
  panelPosition.top = Math.max(0, dragStart.top + dy)
  panelPosition.right = Math.max(0, dragStart.right - dx)
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

// 调整大小
const startResize = (direction: string, e: MouseEvent) => {
  isResizing.value = true
  resizeDirection.value = direction
  dragStart.x = e.clientX
  dragStart.y = e.clientY
  dragStart.width = panelPosition.width
  dragStart.height = panelPosition.height
  dragStart.top = panelPosition.top
  dragStart.right = panelPosition.right
  document.addEventListener('mousemove', onResize)
  document.addEventListener('mouseup', stopResize)
}

const onResize = (e: MouseEvent) => {
  if (!isResizing.value) return
  const dx = e.clientX - dragStart.x
  const dy = e.clientY - dragStart.y
  const dir = resizeDirection.value
  const minW = 280, minH = 400, maxW = 600, maxH = 800

  if (dir.includes('e')) {
    panelPosition.width = Math.min(maxW, Math.max(minW, dragStart.width + dx))
    panelPosition.right = Math.max(0, dragStart.right - dx)
  }
  if (dir.includes('w')) {
    panelPosition.width = Math.min(maxW, Math.max(minW, dragStart.width - dx))
  }
  if (dir.includes('s')) {
    panelPosition.height = Math.min(maxH, Math.max(minH, dragStart.height + dy))
  }
  if (dir.includes('n')) {
    const newH = Math.min(maxH, Math.max(minH, dragStart.height - dy))
    panelPosition.height = newH
    panelPosition.top = dragStart.top + (dragStart.height - newH)
  }
}

const stopResize = () => {
  isResizing.value = false
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
}

onMounted(() => {
  connectWebSocket()
  digitalHumanStore.initWelcomeMessage()
  heartbeatInterval = setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'PING' }))
    }
  }, 30000)

  // 注册音频上下文解锁（用户点击页面任意位置即生效）
  document.addEventListener('click', unlockAudioContext)
  document.addEventListener('touchstart', unlockAudioContext)
  // 立即尝试初始化（如果用户之前已有交互）
  ensureAudioContext()

  // 首次挂载时也触发当前页面的导览（延迟等待 WS 连接）
  setTimeout(() => {
    digitalHumanStore.triggerPageNarration(route.path)
  }, 3000)
})

// ========== 智能导览：页面感知自动讲解 ==========
// 监听路由变化 → 触发对应页面的导览
watch(() => route.path, (newPath) => {
  setTimeout(() => {
    digitalHumanStore.triggerPageNarration(newPath)
  }, 1500)
})

// 监听 store 中的 pendingNarration → 自动发送给数字人
watch(() => digitalHumanStore.pendingNarration, (prompt) => {
  if (!prompt) return
  // 取出并清空
  const narrationPrompt = digitalHumanStore.consumeNarration()
  if (!narrationPrompt) return

  console.log('[导览] 触发自动讲解:', narrationPrompt.substring(0, 40) + '...')

  // 自动展开浮窗
  if (isMinimized.value) {
    isMinimized.value = false
  }

  // 显示思考状态
  isTyping.value = true
  currentStatus.value = 'thinking'

  // 发送（不作为用户消息显示，直接发给后端）
  if (!useHttpFallback && ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'TEXT_INPUT', data: { text: narrationPrompt } }))
  } else {
    // HTTP 降级
    aiRequest.post('/chat/message', {
      message: narrationPrompt,
      conversationId: null
    }).then((res: any) => {
      const reply = res?.data?.reply || res?.reply || ''
      if (reply) addMessage('assistant', reply)
      isTyping.value = false
      currentStatus.value = 'idle'
    }).catch(() => {
      isTyping.value = false
      currentStatus.value = 'idle'
    })
  }
})

onUnmounted(() => {
  if (heartbeatInterval) clearInterval(heartbeatInterval)
  if (audioTimeout) clearTimeout(audioTimeout)
  if (ws) ws.close()
  if (currentAudio) currentAudio.pause()
  if (currentSource) { try { currentSource.stop() } catch {} }
  if (audioCtx) { audioCtx.close().catch(() => {}); audioCtx = null }
  document.removeEventListener('click', unlockAudioContext)
  document.removeEventListener('touchstart', unlockAudioContext)
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
})

const connectWebSocket = () => {
  if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
    // WebSocket全部失败，启用HTTP降级模式
    console.log('⚡ WebSocket连接失败，切换为HTTP聊天模式（通过AI服务）')
    useHttpFallback = true
    return
  }
  // 第一次尝试AI代理，失败后尝试直连
  const url = reconnectAttempts < 2 ? WS_URL_AI_PROXY : WS_URL_DIRECT
  console.log(`🔗 尝试连接 WebSocket (第${reconnectAttempts + 1}次):`, url)
  try {
    ws = new WebSocket(url)
    ws.onopen = () => { 
      console.log('✅ 数字人 WebSocket 已连接:', url)
      reconnectAttempts = 0 
      useHttpFallback = false
    }
    ws.onmessage = async (event) => {
      if (event.data instanceof Blob) {
        if (audioTimeout) {
          clearTimeout(audioTimeout)
          audioTimeout = null
        }
        await playAudioWithText(event.data)
      } else {
        try {
          handleMessage(JSON.parse(event.data))
        } catch (e) {
          console.error('消息解析失败:', e)
        }
      }
    }
    ws.onclose = () => {
      console.log('❌ 数字人 WebSocket 断开')
      reconnectAttempts++
      if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
        setTimeout(connectWebSocket, reconnectAttempts * 2000)
      } else {
        useHttpFallback = true
        console.log('⚡ 切换为HTTP聊天模式')
      }
    }
    ws.onerror = (e) => {
      console.error('WebSocket 错误:', e)
    }
  } catch (e) {
    console.error('WebSocket 创建失败:', e)
    reconnectAttempts++
    if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
      setTimeout(connectWebSocket, reconnectAttempts * 2000)
    } else {
      useHttpFallback = true
    }
  }
}

const handleMessage = (data: any) => {
  console.log('收到消息:', data.type, data.data)
  
  if (data.type === 'text_output' && !data.data.isPartial) {
    // 收到完整文字回复，先存起来等音频一起显示
    const text = data.data.text
    if (text) {
      pendingText = text
      console.log('文字已缓存，等待音频同步显示')
      
      // 超时保护：3秒内没收到音频，直接显示文字
      if (audioTimeout) clearTimeout(audioTimeout)
      audioTimeout = setTimeout(() => {
        if (pendingText) {
          console.log('音频超时，直接显示文字')
          addMessage('assistant', pendingText)
          pendingText = null
          isTyping.value = false
          currentStatus.value = 'idle'
        }
      }, 3000)
    }
  }
  
  if (data.type === 'status') {
    const status = data.data.status
    console.log('状态更新:', status)
    if (status === 'thinking') {
      currentStatus.value = 'thinking'
    } else if (status === 'speaking') {
      currentStatus.value = 'speaking'
    }
  }
  
  if (data.type === 'viseme') {
    // Viseme口型数据已接收，当前使用视频头像，暂不处理
    console.log('[Viseme] 收到口型数据:', data.data?.visemes?.length, '帧')
  }
}

const addMessage = (role: string, content: string) => {
  digitalHumanStore.addMessage(role as 'user' | 'assistant', content)
  scrollToBottom()
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const renderMarkdown = (content: string) => marked.parse(content)

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text) return

  addMessage('user', text)
  inputText.value = ''
  isTyping.value = true
  currentStatus.value = 'thinking'
  scrollToBottom()

  // 判断走WebSocket还是HTTP降级
  if (!useHttpFallback && ws && ws.readyState === WebSocket.OPEN) {
    // WebSocket模式
    ws.send(JSON.stringify({ type: 'TEXT_INPUT', data: { text } }))
  } else {
    // HTTP降级模式 - 通过AI服务聊天接口
    try {
      const result: any = await aiRequest.post('/chat/message', {
        message: text,
        conversationId: null
      })
      const reply = result?.data?.reply || result?.reply || '抱歉，暂时无法回答。'
      addMessage('assistant', reply)
      isTyping.value = false
      currentStatus.value = 'idle'
    } catch (e: any) {
      console.error('HTTP聊天失败:', e)
      addMessage('assistant', '网络异常，请稍后重试。')
      isTyping.value = false
      currentStatus.value = 'idle'
    }
  }
}

const startRecording = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: true } })
    mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm;codecs=opus' })
    audioChunks = []
    mediaRecorder.ondataavailable = (e) => { if (e.data.size > 0) audioChunks.push(e.data) }
    mediaRecorder.onstop = () => {
      sendAudio(new Blob(audioChunks, { type: 'audio/webm' }))
      stream.getTracks().forEach(t => t.stop())
    }
    mediaRecorder.start()
    isRecording.value = true
  } catch (e) {
    console.error('录音失败:', e)
  }
}

const stopRecording = () => {
  if (mediaRecorder && isRecording.value) {
    mediaRecorder.stop()
    isRecording.value = false
  }
}

const sendAudio = (blob: Blob) => {
  if (useHttpFallback || !ws || ws.readyState !== WebSocket.OPEN) {
    addMessage('assistant', '语音功能需要WebSocket连接，当前为HTTP模式。请使用文字输入。')
    return
  }
  addMessage('user', '🎤 语音消息')
  isTyping.value = true
  currentStatus.value = 'thinking'
  blob.arrayBuffer().then(buffer => ws!.send(buffer))
}

const playAudioWithText = async (blob: Blob) => {
  console.log('收到音频，准备播放, size:', blob.size)
  
  // 清除超时计时器
  if (audioTimeout) {
    clearTimeout(audioTimeout)
    audioTimeout = null
  }
  
  // 先显示文字
  if (pendingText) {
    addMessage('assistant', pendingText)
    pendingText = null
  }
  isTyping.value = false

  try {
    // 优先使用 AudioContext 播放（不受浏览器自动播放限制）
    const ctx = ensureAudioContext()
    if (ctx.state === 'suspended') {
      await ctx.resume().catch(() => {})
    }

    const arrayBuffer = await blob.arrayBuffer()
    const audioBuffer = await ctx.decodeAudioData(arrayBuffer)

    // 停止上一个音频源
    if (currentSource) {
      try { currentSource.stop() } catch {}
    }
    if (currentAudio) {
      currentAudio.pause()
      currentAudio = null
    }

    currentSource = ctx.createBufferSource()
    currentSource.buffer = audioBuffer
    currentSource.connect(ctx.destination)
    currentSource.start(0)
    console.log('AudioContext 音频开始播放')
    startSpeaking()

    currentSource.onended = () => {
      console.log('音频播放结束')
      stopSpeaking()
    }
  } catch (e) {
    console.warn('AudioContext播放失败，降级到Audio元素:', e)
    // 降级：使用传统 Audio 元素
    const url = URL.createObjectURL(blob)
    if (currentAudio) { currentAudio.pause(); currentAudio = null }
    currentAudio = new Audio(url)
    try {
      await currentAudio.play()
      console.log('Audio元素播放成功')
      startSpeaking()
      currentAudio.onended = () => {
        stopSpeaking()
        URL.revokeObjectURL(url)
      }
    } catch (e2) {
      console.error('音频播放完全失败:', e2)
      stopSpeaking()
      URL.revokeObjectURL(url)
    }
  }
}

const startSpeaking = () => {
  isSpeaking.value = true
  currentStatus.value = 'speaking'
  avatarVideo.value?.play().catch(() => {})
}

const stopSpeaking = () => {
  isSpeaking.value = false
  currentStatus.value = 'idle'
  avatarVideo.value?.pause()
}

const toggleMinimize = () => { isMinimized.value = !isMinimized.value }
const handleImageError = (e: Event) => { (e.target as HTMLImageElement).style.display = 'none' }
</script>

<style scoped>
.digital-human-container {
  font-family: 'Noto Sans SC', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  --primary: #2A9D8F;
  --primary-gradient: linear-gradient(135deg, #2A9D8F 0%, #4DB6AC 100%);
  --accent: #E9C46A;
  --bg-panel: rgba(255, 255, 255, 0.95);
  --bg-header: #FFFFFF;
  --bg-msg-assistant: #F2F3F5;
  --text-primary: #1D2129;
  --text-secondary: #86909C;
  --border-color: #E5E6EB;
  --shadow-panel: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.chat-panel {
  position: fixed;
  background: var(--bg-panel);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-panel);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 9999;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.chat-panel.minimized {
  width: 56px !important;
  height: 56px !important;
  top: auto !important;
  bottom: 24px;
  right: 24px !important;
  border-radius: 50%;
  background: transparent;
  box-shadow: none;
  border: none;
  backdrop-filter: none;
}

.minimized-trigger {
  width: 56px;
  height: 56px;
  background: var(--primary-gradient);
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  box-shadow: 0 4px 16px rgba(42, 157, 143, 0.4);
  transition: transform 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #fff;
}

.minimized-trigger:hover { transform: scale(1.05); }

.avatar-circle {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-indicator {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 14px;
  height: 14px;
  background: #10B981;
  border-radius: 50%;
  border: 2px solid #fff;
}

.status-indicator.speaking { background: #E9C46A; }
.status-indicator.thinking { background: #F4A261; }

/* 顶部栏 */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: var(--bg-header);
  border-bottom: 1px solid var(--border-color);
  cursor: move;
  user-select: none;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mini-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  position: relative;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.mini-avatar.speaking {
  animation: avatar-glow 1.5s ease-in-out infinite;
  border-color: var(--primary);
}

@keyframes avatar-glow {
  0%, 100% { box-shadow: 0 0 0 0 rgba(42, 157, 143, 0.4); }
  50% { box-shadow: 0 0 0 6px rgba(42, 157, 143, 0); }
}

.mini-avatar-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mini-status {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  background: #10B981;
  border-radius: 50%;
  border: 1.5px solid #fff;
}

.mini-status.thinking { background: #F4A261; }
.mini-status.speaking { background: #E9C46A; }

.header-info {
  display: flex;
  flex-direction: column;
}

.header-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}

.header-status {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: #F2F3F5;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover { background: #E5E6EB; color: var(--primary); }

.action-icon {
  width: 18px;
  height: 18px;
  fill: var(--text-secondary);
}
.action-btn:hover .action-icon { fill: var(--primary); }

/* 消息区域 */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #FAFAFA;
}

.messages-container::-webkit-scrollbar { width: 6px; }
.messages-container::-webkit-scrollbar-thumb { background: #E5E6EB; border-radius: 3px; }
.messages-container::-webkit-scrollbar-thumb:hover { background: #C9CDD4; }

.message {
  display: flex;
  flex-direction: column;
  max-width: 85%;
  animation: msgIn 0.3s ease;
}

@keyframes msgIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.message.assistant { align-self: flex-start; }
.message.user { align-self: flex-end; align-items: flex-end; }

.message-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
  box-shadow: 0 2px 6px rgba(0,0,0,0.04);
}

.message.assistant .message-bubble {
  background: #FFFFFF;
  color: var(--text-primary);
  border-top-left-radius: 4px;
  border: 1px solid #E5E6EB;
}

.message.user .message-bubble {
  background: var(--primary-gradient);
  color: #fff;
  border-top-right-radius: 4px;
  box-shadow: 0 4px 12px rgba(42, 157, 143, 0.2);
}

.message-time {
  font-size: 11px;
  color: #C9CDD4;
  margin-top: 6px;
  padding: 0 4px;
}

:deep(.message-bubble p) { margin: 4px 0; }
:deep(.message-bubble strong) { font-weight: 600; color: inherit; }

/* 输入区域 */
.input-section {
  padding: 16px 20px;
  background: #FFFFFF;
  display: flex;
  gap: 12px;
  align-items: center;
  border-top: 1px solid var(--border-color);
}

.mic-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid var(--border-color);
  background: #F7F8FA;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.mic-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: #E6FFFA;
}

.mic-btn.recording {
  background: #F56C6C;
  color: #fff;
  border-color: #F56C6C;
  animation: pulse-red 1.5s infinite;
}

@keyframes pulse-red {
  0% { box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.4); }
  70% { box-shadow: 0 0 0 6px rgba(245, 108, 108, 0); }
  100% { box-shadow: 0 0 0 0 rgba(245, 108, 108, 0); }
}

.input-wrapper {
  flex: 1;
  display: flex;
  background: #F2F3F5;
  border-radius: 20px;
  padding: 6px 14px;
  align-items: center;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.input-wrapper:focus-within {
  border-color: var(--primary);
  background: #FFFFFF;
  box-shadow: 0 0 0 2px rgba(42, 157, 143, 0.1);
}

.text-input {
  flex: 1;
  border: none;
  font-size: 14px;
  outline: none;
  background: transparent;
  padding: 8px 4px;
  color: var(--text-primary);
}

.text-input::placeholder { color: #86909C; }

.send-btn {
  border: none;
  background: none;
  color: var(--primary);
  opacity: 0.8;
  cursor: pointer;
  transition: all 0.2s;
  padding: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-btn:not(:disabled):hover { opacity: 1; transform: scale(1.1); background: #E6FFFA; border-radius: 50%; }
.send-btn:disabled { cursor: not-allowed; opacity: 0.4; background: none; }

.icon {
  width: 20px;
  height: 20px;
  fill: currentColor;
}

/* Typing */
.typing-indicator {
  display: none;
  padding: 12px 16px;
  background: #FFFFFF;
  border-radius: 16px;
  border-top-left-radius: 4px;
  width: fit-content;
  gap: 6px;
  border: 1px solid #E5E6EB;
  margin-left: 0;
  box-shadow: 0 2px 6px rgba(0,0,0,0.04);
}

.typing-indicator.show { display: flex; }

.typing-dot {
  width: 6px;
  height: 6px;
  background: var(--text-secondary);
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.typing-dot:nth-child(1) { animation-delay: -0.32s; }
.typing-dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* Resize Handles */
.resize-handle {
  position: absolute;
  background: transparent;
  z-index: 100;
}

.resize-n { top: 0; left: 0; width: 100%; height: 6px; cursor: ns-resize; }
.resize-e { top: 0; right: 0; width: 6px; height: 100%; cursor: ew-resize; }
.resize-s { bottom: 0; left: 0; width: 100%; height: 6px; cursor: ns-resize; }
.resize-w { top: 0; left: 0; width: 6px; height: 100%; cursor: ew-resize; }
.resize-ne { top: 0; right: 0; width: 12px; height: 12px; cursor: ne-resize; }
.resize-nw { top: 0; left: 0; width: 12px; height: 12px; cursor: nw-resize; }
.resize-se { bottom: 0; right: 0; width: 12px; height: 12px; cursor: se-resize; }
.resize-sw { bottom: 0; left: 0; width: 12px; height: 12px; cursor: sw-resize; }
</style>
