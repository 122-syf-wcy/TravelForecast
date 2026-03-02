<template>
  <div class="embedded-digital-human">
    <!-- 数字人头像区域 -->
    <div class="avatar-section">
      <div class="avatar-wrapper" :class="{ 'speaking': isSpeaking }">
        <div class="voice-ripple"></div>
        <div class="voice-ripple"></div>
        <div class="voice-ripple"></div>
        <video 
          ref="avatarVideo" 
          src="/videos/avatar.mp4" 
          class="avatar-video" 
          loop 
          muted 
          playsinline
        ></video>
        <div class="status-indicator" :class="currentStatus"></div>
      </div>
      <div class="avatar-info">
        <div class="avatar-name">黔小游</div>
        <div class="avatar-title">研学导师 · {{ isConnected ? '在线' : '离线' }}</div>
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
        @mousedown="startRecording" 
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
          placeholder="输入问题或按麦克风说话..."
        >
        <button class="send-btn" @click="sendMessage" :disabled="!inputText.trim()">
          <svg class="icon" viewBox="0 0 24 24">
            <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed, watch } from 'vue'
import { marked } from 'marked'
import { useDigitalHumanStore } from '@/store/digitalHuman'
import aiRequest from '@/utils/aiRequest'

// 使用共享的 store
const digitalHumanStore = useDigitalHumanStore()

// 从 store 获取消息
const messages = computed(() => digitalHumanStore.messages)

// 本地状态
const inputText = ref('')
const isRecording = ref(false)
const isSpeaking = ref(false)
const isTyping = ref(false)
const currentStatus = ref('idle')
const isConnected = ref(false)

// DOM 引用
const messagesContainer = ref<HTMLElement | null>(null)
const avatarVideo = ref<HTMLVideoElement | null>(null)

// WebSocket - 通过AI服务代理，降级时直连旧数字人后端
const WS_URL_AI_PROXY = `ws://${window.location.hostname}:8081/ai-api/ws/digital-human`
const WS_URL_DIRECT = 'ws://localhost:8083/ws/avatar'
let ws: WebSocket | null = null
let mediaRecorder: MediaRecorder | null = null
let audioChunks: Blob[] = []
let currentAudio: HTMLAudioElement | null = null
let pendingText: string | null = null
let reconnectAttempts = 0
const MAX_RECONNECT_ATTEMPTS = 3
let heartbeatInterval: ReturnType<typeof setInterval> | null = null
let useHttpFallback = false

// ========== AudioContext 解决浏览器自动播放限制 ==========
let audioCtx: AudioContext | null = null
let currentSource: AudioBufferSourceNode | null = null

const ensureAudioContext = (): AudioContext => {
  if (!audioCtx) {
    audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)()
  }
  return audioCtx
}

const unlockAudioContext = () => {
  const ctx = ensureAudioContext()
  if (ctx.state === 'suspended') {
    ctx.resume().then(() => console.log('[Audio] 嵌入式音频上下文已解锁'))
  }
}

// 监听消息变化，自动滚动
watch(messages, () => {
  scrollToBottom()
}, { deep: true })

onMounted(() => {
  // 设置为嵌入模式
  digitalHumanStore.setEmbeddedMode(true)
  
  connectWebSocket()
  
  // 初始化欢迎消息
  digitalHumanStore.initWelcomeMessage()
  
  // 心跳
  heartbeatInterval = setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'PING' }))
    }
  }, 30000)
  
  // 注册音频上下文解锁
  document.addEventListener('click', unlockAudioContext)
  document.addEventListener('touchstart', unlockAudioContext)
  ensureAudioContext()
  
  scrollToBottom()
})

// ========== 智能导览：监听 pendingNarration 自动发送 ==========
watch(() => digitalHumanStore.pendingNarration, (prompt) => {
  if (!prompt) return
  const narrationPrompt = digitalHumanStore.consumeNarration()
  if (!narrationPrompt) return

  console.log('[嵌入式导览] 触发自动讲解:', narrationPrompt.substring(0, 40) + '...')

  // 显示思考状态
  isTyping.value = true
  currentStatus.value = 'thinking'

  // 通过 WebSocket 或 HTTP 降级发送
  if (!useHttpFallback && ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'TEXT_INPUT', data: { text: narrationPrompt } }))
  } else {
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
  // 退出嵌入模式
  digitalHumanStore.setEmbeddedMode(false)
  
  if (heartbeatInterval) {
    clearInterval(heartbeatInterval)
  }
  if (ws) {
    ws.close()
  }
  if (currentAudio) {
    currentAudio.pause()
  }
  if (currentSource) { try { currentSource.stop() } catch {} }
  if (audioCtx) { audioCtx.close().catch(() => {}); audioCtx = null }
  document.removeEventListener('click', unlockAudioContext)
  document.removeEventListener('touchstart', unlockAudioContext)
})

const connectWebSocket = () => {
  if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
    console.log('⚡ 嵌入式数字人切换为HTTP聊天模式')
    useHttpFallback = true
    return
  }

  const url = reconnectAttempts < 2 ? WS_URL_AI_PROXY : WS_URL_DIRECT
  console.log(`🔗 嵌入式数字人尝试连接 (第${reconnectAttempts + 1}次):`, url)
  
  try {
    ws = new WebSocket(url)
    
    ws.onopen = () => {
      console.log('✅ 嵌入式数字人 WebSocket 已连接:', url)
      isConnected.value = true
      reconnectAttempts = 0
      useHttpFallback = false
    }
    
    ws.onmessage = async (event) => {
      if (event.data instanceof Blob) {
        await playAudioWithText(event.data)
      } else {
        try {
          const data = JSON.parse(event.data)
          handleMessage(data)
        } catch (e) {
          console.error('消息解析失败:', e)
        }
      }
    }
    
    ws.onclose = () => {
      isConnected.value = false
      reconnectAttempts++
      if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
        setTimeout(connectWebSocket, reconnectAttempts * 2000)
      } else {
        useHttpFallback = true
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

let textFallbackTimer: ReturnType<typeof setTimeout> | null = null

const handleMessage = (data: any) => {
  console.log('嵌入式数字人收到消息:', data.type, data.data)
  
  if (data.type === 'text_output' && !data.data.isPartial) {
    const text = data.data.text
    if (text) {
      pendingText = text
      console.log('嵌入式数字人文字已缓存，等待音频同步显示')
      
      // 超时保护：3秒内没收到音频，直接显示文字
      if (textFallbackTimer) clearTimeout(textFallbackTimer)
      textFallbackTimer = setTimeout(() => {
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
    console.log('嵌入式数字人状态更新:', status)
    if (status === 'thinking') {
      currentStatus.value = 'thinking'
    } else if (status === 'speaking') {
      currentStatus.value = 'speaking'
    }
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

const renderMarkdown = (content: string) => {
  return marked.parse(content)
}

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text) return
  
  addMessage('user', text)
  inputText.value = ''
  isTyping.value = true
  currentStatus.value = 'thinking'
  scrollToBottom()

  if (!useHttpFallback && ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'TEXT_INPUT', data: { text } }))
  } else {
    // HTTP降级 - 通过AI服务
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
    const stream = await navigator.mediaDevices.getUserMedia({ 
      audio: { echoCancellation: true, noiseSuppression: true } 
    })
    
    mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm;codecs=opus' })
    audioChunks = []
    
    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) audioChunks.push(e.data)
    }
    
    mediaRecorder.onstop = () => {
      const blob = new Blob(audioChunks, { type: 'audio/webm' })
      sendAudio(blob)
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
  
  blob.arrayBuffer().then(buffer => {
    ws!.send(buffer)
  })
}

const playAudioWithText = async (blob: Blob) => {
  console.log('嵌入式数字人收到音频, size:', blob.size)
  
  // 清除文字超时计时器，防止重复显示
  if (textFallbackTimer) {
    clearTimeout(textFallbackTimer)
    textFallbackTimer = null
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

    if (currentSource) { try { currentSource.stop() } catch {} }
    if (currentAudio) { currentAudio.pause(); currentAudio = null }

    currentSource = ctx.createBufferSource()
    currentSource.buffer = audioBuffer
    currentSource.connect(ctx.destination)
    currentSource.start(0)
    console.log('嵌入式 AudioContext 音频开始播放')
    startSpeaking()

    currentSource.onended = () => {
      console.log('嵌入式音频播放结束')
      stopSpeaking()
    }
  } catch (e) {
    console.warn('AudioContext播放失败，降级到Audio元素:', e)
    const url = URL.createObjectURL(blob)
    if (currentAudio) { currentAudio.pause(); currentAudio = null }
    currentAudio = new Audio(url)
    try {
      await currentAudio.play()
      startSpeaking()
      currentAudio.onended = () => {
        stopSpeaking()
        URL.revokeObjectURL(url)
      }
    } catch (e2) {
      console.error('嵌入式音频播放完全失败:', e2)
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
</script>

<style scoped>
.embedded-digital-human {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 400px;
  font-family: 'Noto Sans SC', sans-serif;
  --primary: #6366F1;
  --primary-gradient: linear-gradient(135deg, #6366F1 0%, #8B5CF6 100%);
  --accent: #06B6D4;
  --bg-card: rgba(30, 41, 59, 0.6);
  --text-primary: #f1f5f9;
  --text-secondary: #94a3b8;
  --border-color: rgba(148, 163, 184, 0.2);
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-card);
  border-radius: 12px;
  margin-bottom: 12px;
  border: 1px solid var(--border-color);
}

.avatar-wrapper {
  position: relative;
  width: 96px;
  height: 96px;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid var(--primary);
}

.avatar-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-wrapper.speaking {
  animation: avatar-glow 1.5s ease-in-out infinite;
}

@keyframes avatar-glow {
  0%, 100% { box-shadow: 0 0 0 0 rgba(99, 102, 241, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(99, 102, 241, 0); }
}

.voice-ripple {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 1px solid var(--primary);
  opacity: 0;
  pointer-events: none;
}

.speaking .voice-ripple {
  animation: ripple 2s cubic-bezier(0, 0.2, 0.8, 1) infinite;
}

.speaking .voice-ripple:nth-child(2) { animation-delay: 0.5s; }
.speaking .voice-ripple:nth-child(3) { animation-delay: 1s; }

@keyframes ripple {
  0% { width: 100%; height: 100%; opacity: 0.4; }
  100% { width: 180%; height: 180%; opacity: 0; }
}

.avatar-info {
  flex: 1;
}

.avatar-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.avatar-title {
  font-size: 12px;
  color: var(--text-secondary);
}

.status-indicator {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  background: #10B981;
  border-radius: 50%;
  border: 2px solid rgba(30, 41, 59, 1);
}

.status-indicator.thinking { background: #F59E0B; }
.status-indicator.speaking { background: var(--accent); }

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 380px;
  min-height: 280px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  margin-bottom: 12px;
  border: 1px solid var(--border-color);
}

.messages-container::-webkit-scrollbar { width: 4px; }
.messages-container::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, 0.2); border-radius: 4px; }

.message {
  display: flex;
  flex-direction: column;
  max-width: 85%;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.message.assistant { align-self: flex-start; }
.message.user { align-self: flex-end; align-items: flex-end; }

.message-bubble {
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.5;
  word-wrap: break-word;
}

.message.assistant .message-bubble {
  background: var(--bg-card);
  color: var(--text-primary);
  border-top-left-radius: 4px;
}

.message.user .message-bubble {
  background: var(--primary-gradient);
  color: #fff;
  border-top-right-radius: 4px;
}

.message-time {
  font-size: 10px;
  color: var(--text-secondary);
  margin-top: 4px;
  padding: 0 4px;
}

:deep(.message-bubble p) { margin: 4px 0; }
:deep(.message-bubble strong) { font-weight: 600; color: inherit; }

.input-section {
  display: flex;
  gap: 8px;
  align-items: center;
  padding-top: 8px;
  border-top: 1px solid var(--border-color);
}

.mic-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid var(--border-color);
  background: rgba(255, 255, 255, 0.05);
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
  background: rgba(99, 102, 241, 0.1);
}

.mic-btn.recording {
  background: #EF4444;
  color: #fff;
  border-color: #EF4444;
  animation: pulse-red 1.5s infinite;
}

@keyframes pulse-red {
  0% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4); }
  70% { box-shadow: 0 0 0 6px rgba(239, 68, 68, 0); }
  100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0); }
}

.input-wrapper {
  flex: 1;
  display: flex;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  padding: 4px 12px;
  align-items: center;
  border: 1px solid var(--border-color);
  transition: border-color 0.2s;
}

.input-wrapper:focus-within {
  border-color: rgba(99, 102, 241, 0.5);
}

.text-input {
  flex: 1;
  border: none;
  font-size: 13px;
  outline: none;
  background: transparent;
  padding: 8px 4px;
  color: var(--text-primary);
}

.text-input::placeholder { color: var(--text-secondary); }

.send-btn {
  border: none;
  background: none;
  color: var(--primary);
  opacity: 0.6;
  cursor: pointer;
  transition: all 0.2s;
  padding: 4px;
}

.send-btn:not(:disabled):hover { opacity: 1; transform: scale(1.1); }
.send-btn:disabled { cursor: not-allowed; opacity: 0.3; }

.icon {
  width: 18px;
  height: 18px;
  fill: currentColor;
}

.typing-indicator {
  display: none;
  padding: 8px 12px;
  background: var(--bg-card);
  border-radius: 12px;
  width: fit-content;
  gap: 4px;
}

.typing-indicator.show { display: flex; }

.typing-dot {
  width: 5px;
  height: 5px;
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
</style>
