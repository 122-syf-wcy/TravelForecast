<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="custom-nav" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-inner">
        <view class="nav-back" @tap="goBack">
          <text class="nav-back-icon">‹</text>
        </view>
        <text class="nav-title">数字导游</text>
        <view class="nav-placeholder" />
      </view>
    </view>

    <view class="dh-container">
      <!-- 状态栏 -->
      <view class="dh-status-bar">
        <view class="status-dot" :class="{ 'dot-busy': isLoading }" />
        <text class="status-txt">{{ statusText }}</text>
      </view>

      <!-- 聊天区域（全屏） -->
      <view class="dh-chat">
        <scroll-view scroll-y class="chat-list" :scroll-into-view="lastId" :scroll-with-animation="true">
          <view class="msg" v-for="(m, i) in msgs" :key="i" :id="'msg-' + i" :class="{ 'msg-me': m.isMe }">
            <view class="avatar" :class="{ 'avatar-speaking': m.playing }" v-if="!m.isMe">
              <image class="avatar-img" src="/static/dh-avatar-poster.jpg" mode="aspectFill" />
            </view>
            <view class="bubble" :class="{ 'bubble-loading': m.loading }">
              <view class="typing-dots" v-if="m.loading">
                <view class="td" v-for="j in 3" :key="j" />
              </view>
              <text class="bubble-t" v-else>{{ m.text }}</text>
            </view>
            <!-- 语音播放按钮 -->
            <view class="voice-btn" v-if="!m.isMe && !m.loading && m.text" @tap="playVoice(m)">
              <view class="voice-icon" :class="{ 'voice-playing': m.playing }">
                <view class="vi-bar" v-for="j in 3" :key="j" />
              </view>
            </view>
          </view>
          <view style="height: 20px;" />
        </scroll-view>

        <!-- 快捷问题 -->
        <scroll-view scroll-x class="quick-bar" :show-scrollbar="false" v-if="msgs.length <= 3">
          <view class="quick-item" v-for="(q, i) in quickQuestions" :key="i" @tap="onQuick(q)">
            <text class="quick-t">{{ q }}</text>
          </view>
        </scroll-view>

        <!-- 输入区域 -->
        <view class="input-area">
          <view class="voice-record-btn" @tap="toggleVoiceMode">
            <text class="vr-icon">{{ voiceMode ? '⌨' : '🎤' }}</text>
          </view>
          <view class="input-box" v-if="!voiceMode">
            <input class="chat-input" v-model="inputVal" placeholder="问问六盘水有什么好玩的..."
              @confirm="sendMsg" :disabled="isLoading" />
          </view>
          <view class="hold-talk-btn" v-else
            @touchstart="startRecord" @touchend="stopRecord" @touchcancel="cancelRecord">
            <text class="hold-t">{{ recording ? '松开发送' : '按住说话' }}</text>
          </view>
          <view class="send-btn" @tap="sendMsg" v-if="!voiceMode">
            <text class="send-t">发送</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { chatWithAI, textToSpeech, clearChatHistory, generateConversationId, uploadVoice } from '@/api/digitalHuman'

const generateSessionId = () => generateConversationId()
const clearSession = (id) => clearChatHistory(id)

const statusBarHeight = ref(20)
try {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
} catch (e) {}

const goBack = () => {
  uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/index/index' }) })
}

const inputVal = ref('')
const lastId = ref('')
const isLoading = ref(false)
const isSpeaking = ref(false)
const voiceMode = ref(false)
const recording = ref(false)
const statusText = ref('黔小游在线中')
const sessionId = ref('')
const innerAudioCtx = ref(null)

const msgs = ref([
  { text: '嗨！我是你的AI研学导师黔小游，欢迎来到中国凉都六盘水！', isMe: false },
  { text: '你可以问我关于景点、美食、路线、三线建设历史的任何问题，也可以点击下方快捷问题开始~', isMe: false }
])

const quickQuestions = [
  '六盘水有哪些必去景点？',
  '三线建设是什么？',
  '推荐一条研学路线',
  '六盘水有什么好吃的？',
  '梅花山明天人多吗？'
]

const stopAllPlaying = () => {
  isSpeaking.value = false
  msgs.value.forEach(m => { m.playing = false })
}

onMounted(() => {
  sessionId.value = generateSessionId()
  innerAudioCtx.value = uni.createInnerAudioContext()
  innerAudioCtx.value.onEnded(() => stopAllPlaying())
  innerAudioCtx.value.onError(() => stopAllPlaying())

  // 处理从导览页传入的AI讲解请求
  const pages = getCurrentPages()
  const curPage = pages[pages.length - 1]
  const options = curPage?.$page?.options || curPage?.options || {}
  if (options.initMsg) {
    const initText = decodeURIComponent(options.initMsg)
    setTimeout(() => {
      inputVal.value = initText
      sendMsg()
    }, 500)
  }
})

onUnmounted(() => {
  if (innerAudioCtx.value) {
    innerAudioCtx.value.destroy()
  }
  // 清除后端会话
  if (sessionId.value) {
    clearSession(sessionId.value).catch(() => {})
  }
})

const scrollToBottom = () => {
  lastId.value = ''
  setTimeout(() => {
    lastId.value = 'msg-' + (msgs.value.length - 1)
  }, 50)
}

const sendMsg = async () => {
  const text = inputVal.value.trim()
  if (!text || isLoading.value) return

  // 添加用户消息
  msgs.value.push({ text, isMe: true })
  inputVal.value = ''
  scrollToBottom()

  // 添加加载占位
  msgs.value.push({ text: '', isMe: false, loading: true })
  scrollToBottom()
  isLoading.value = true
  statusText.value = '正在思考...'

  try {
    console.log('[DH] 发送消息:', { message: text, conversationId: sessionId.value })
    const res = await chatWithAI(text, sessionId.value)
    console.log('[DH] AI响应:', JSON.stringify(res))

    const replyText = res.reply
    let audioData = null

    // 先合成语音，再同时展示文字+播放声音
    if (replyText && replyText.length <= 500) {
      try {
        statusText.value = '正在合成语音...'
        const ttsRes = await textToSpeech(replyText)
        if (ttsRes && ttsRes.audio) {
          audioData = ttsRes.audio
        }
      } catch (e) {
        console.warn('[DH] TTS合成失败，仅显示文字:', e)
      }
    }

    // 同时展示文字和播放语音
    msgs.value = msgs.value.filter(m => !m.loading)
    msgs.value.push({ text: replyText, isMe: false, playing: !!audioData })
    scrollToBottom()

    if (audioData) {
      playBase64Audio(audioData)
    }
  } catch (err) {
    msgs.value = msgs.value.filter(m => !m.loading)
    msgs.value.push({ text: '抱歉，网络出了点问题，请稍后再试~', isMe: false })
    scrollToBottom()
  } finally {
    isLoading.value = false
    statusText.value = '黔小游在线中'
  }
}

const onQuick = (q) => {
  inputVal.value = q
  sendMsg()
}

const autoPlayVoice = async (text) => {
  if (!text || text.length > 500) return
  try {
    statusText.value = '正在合成语音...'
    const res = await textToSpeech(text)
    if (res.audio) {
      playBase64Audio(res.audio)
      // 标记最后一条AI消息为播放中
      const lastAiMsg = [...msgs.value].reverse().find(m => !m.isMe && !m.loading)
      if (lastAiMsg) lastAiMsg.playing = true
    }
  } catch (e) {
    // 语音合成失败不影响文字显示
  } finally {
    statusText.value = '黔小游在线中'
  }
}

const playVoice = async (msg) => {
  if (msg.playing) {
    // 正在播放则停止
    if (innerAudioCtx.value) innerAudioCtx.value.stop()
    stopAllPlaying()
    return
  }

  try {
    statusText.value = '正在合成语音...'
    msgs.value.forEach(m => { m.playing = false })
    const res = await textToSpeech(msg.text)
    if (res.audio) {
      msg.playing = true
      playBase64Audio(res.audio)
    }
  } catch (e) {
    uni.showToast({ title: '语音播放失败', icon: 'none' })
  } finally {
    statusText.value = '黔小游在线中'
  }
}

const playBase64Audio = (base64Data) => {
  try {
    // #ifdef MP-WEIXIN
    // 小程序中将base64音频写入临时文件再播放
    const fs = uni.getFileSystemManager()
    const filePath = `${wx.env.USER_DATA_PATH}/tts_${Date.now()}.mp3`
    fs.writeFileSync(filePath, base64Data, 'base64')
    isSpeaking.value = true
    innerAudioCtx.value.src = filePath
    innerAudioCtx.value.play()
    // #endif

    // #ifdef H5
    // H5模式：将base64转为Blob URL播放
    const binary = atob(base64Data)
    const bytes = new Uint8Array(binary.length)
    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
    const blob = new Blob([bytes], { type: 'audio/mp3' })
    const blobUrl = URL.createObjectURL(blob)
    isSpeaking.value = true
    innerAudioCtx.value.src = blobUrl
    innerAudioCtx.value.play()
    // #endif
  } catch (e) {
    console.error('音频播放失败:', e)
    isSpeaking.value = false
  }
}

const toggleVoiceMode = () => {
  voiceMode.value = !voiceMode.value
}

const recorderManager = uni.getRecorderManager && uni.getRecorderManager()

const startRecord = () => {
  if (!recorderManager) {
    uni.showToast({ title: '当前环境不支持录音', icon: 'none' })
    return
  }
  recording.value = true
  statusText.value = '正在录音...'
  recorderManager.start({
    format: 'mp3',
    sampleRate: 16000,
    numberOfChannels: 1
  })
}

const stopRecord = () => {
  if (!recording.value) return
  recording.value = false
  statusText.value = '黔小游在线中'
  if (recorderManager) recorderManager.stop()
}

const cancelRecord = () => {
  recording.value = false
  statusText.value = '黔小游在线中'
  if (recorderManager) recorderManager.stop()
}

// 录音完成回调
if (recorderManager) {
  recorderManager.onStop(async (res) => {
    if (!res.tempFilePath) return
    
    uni.showLoading({ title: '正在识别语音', mask: true })
    try {
      // 上传音频到后端STT接口进行识别
      const text = await uploadVoice(res.tempFilePath)
      uni.hideLoading()
      
      if (text) {
        // 关闭语音模式，把识别结果填入输入框并发送
        voiceMode.value = false
        inputVal.value = text
        sendMsg()
      } else {
        uni.showToast({ title: '没听清，请更靠近麦克风', icon: 'none' })
      }
    } catch (e) {
      uni.hideLoading()
      uni.showToast({ title: e.message || '语音识别失败，请重试', icon: 'none' })
      // 如果 STT 用不了，暂时切回键盘
      setTimeout(() => {
        voiceMode.value = false
      }, 1500)
    }
  })
}
</script>

<style lang="scss">
.page { height: 100vh; background: #0a0a1a; display: flex; flex-direction: column; }

/* 自定义导航栏 */
.custom-nav {
  background: linear-gradient(180deg, #0d1b2a, #0d1b2a);
  position: relative; z-index: 100;
}
.nav-inner {
  height: 44px; display: flex; align-items: center;
  justify-content: space-between; padding: 0 12px;
}
.nav-back {
  width: 36px; height: 36px; display: flex;
  align-items: center; justify-content: center;
}
.nav-back-icon { color: #fff; font-size: 28px; font-weight: 300; }
.nav-title { color: #fff; font-size: 17px; font-weight: 600; }
.nav-placeholder { width: 36px; }

.dh-container { flex: 1; display: flex; flex-direction: column; position: relative; overflow: hidden; }

/* 状态栏 */
.dh-status-bar {
  display: flex; align-items: center; padding: 6px 16px;
  background: rgba(13, 27, 42, 0.8);
}
.status-dot {
  width: 6px; height: 6px; background: #00b894; border-radius: 50%;
  margin-right: 6px; box-shadow: 0 0 5px #00b894;
}
.status-dot.dot-busy { background: #ffc048; box-shadow: 0 0 5px #ffc048; animation: pulse 1s infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
.status-txt { color: rgba(255,255,255,0.8); font-size: 12px; }

/* 聊天区域（全屏） */
.dh-chat {
  flex: 1; background: #f5f7fa;
  display: flex; flex-direction: column; overflow: hidden;
}
.chat-list { flex: 1; padding: 16px 16px 0; box-sizing: border-box; }
.msg { display: flex; margin-bottom: 16px; align-items: flex-start; }
.msg-me { flex-direction: row-reverse; }
.avatar {
  width: 40px; height: 40px;
  border-radius: 50%; overflow: hidden;
  margin-right: 10px; flex-shrink: 0;
}
.avatar-img { width: 40px; height: 40px; border-radius: 50%; }
.avatar-video { width: 40px; height: 40px; border-radius: 50%; }
.msg-me .avatar { margin-right: 0; margin-left: 10px; }
.bubble { max-width: 70%; padding: 10px 14px; background: #f0f0f0; border-radius: 12px; }
.msg-me .bubble { background: #2A9D8F; }
.msg-me .bubble .bubble-t { color: #fff; }
.bubble-t { font-size: 14px; line-height: 1.6; color: #333; }
.bubble-loading { padding: 14px 20px; }

/* 打字动画 */
.typing-dots { display: flex; gap: 5px; align-items: center; }
.td {
  width: 8px; height: 8px; border-radius: 50%; background: #2A9D8F;
  animation: typing 1.2s infinite;
}
.td:nth-child(2) { animation-delay: 0.2s; }
.td:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing {
  0%, 60%, 100% { opacity: 0.3; transform: scale(0.8); }
  30% { opacity: 1; transform: scale(1); }
}

/* 语音播放按钮 */
.voice-btn {
  width: 28px; height: 28px; border-radius: 50%;
  background: #E0F2F1; display: flex; align-items: center; justify-content: center;
  margin-left: 6px; margin-top: 6px; flex-shrink: 0;
}
.voice-icon { display: flex; align-items: flex-end; gap: 2px; height: 14px; }
.vi-bar {
  width: 3px; background: #2A9D8F; border-radius: 1px;
}
.vi-bar:nth-child(1) { height: 6px; }
.vi-bar:nth-child(2) { height: 10px; }
.vi-bar:nth-child(3) { height: 14px; }
.voice-playing .vi-bar { animation: voicePlay 0.6s ease-in-out infinite alternate; }
.voice-playing .vi-bar:nth-child(2) { animation-delay: 0.15s; }
.voice-playing .vi-bar:nth-child(3) { animation-delay: 0.3s; }
@keyframes voicePlay {
  0% { height: 4px; }
  100% { height: 14px; }
}

/* 快捷问题 */
.quick-bar { padding: 0 16px 8px; white-space: nowrap; }
.quick-item {
  display: inline-block; padding: 6px 14px;
  background: #E0F2F1; border-radius: 16px; margin-right: 8px;
  border: 1px solid rgba(42,157,143,0.15);
}
.quick-t { font-size: 12px; color: #2A9D8F; font-weight: 500; }

/* 输入区域 */
.input-area {
  padding: 10px 16px 30px; border-top: 1px solid #eee;
  display: flex; align-items: center;
}
.voice-record-btn {
  width: 40px; height: 40px; border-radius: 50%;
  background: #f5f5f5; display: flex; align-items: center; justify-content: center;
  margin-right: 8px; flex-shrink: 0;
}
.vr-icon { font-size: 18px; }
.input-box { flex: 1; }
.chat-input {
  height: 40px; background: #f5f5f5; border-radius: 20px;
  padding: 0 16px; font-size: 14px;
}
.hold-talk-btn {
  flex: 1; height: 40px; background: #f5f5f5; border-radius: 20px;
  display: flex; align-items: center; justify-content: center;
}
.hold-t { font-size: 14px; color: #666; font-weight: 500; }
.send-btn {
  width: 60px; height: 40px; background: #2A9D8F; border-radius: 20px;
  display: flex; align-items: center; justify-content: center; margin-left: 8px;
}
.send-t { color: #fff; font-size: 14px; font-weight: 600; }
</style>
