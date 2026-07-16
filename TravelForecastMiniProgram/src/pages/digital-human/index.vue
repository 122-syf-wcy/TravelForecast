<template>
  <view class="page">
    <view class="dh-container">
      <!-- 聊天区域 -->
      <view class="dh-chat">
        <scroll-view scroll-y class="chat-list" :scroll-into-view="lastId" :scroll-with-animation="true">
          <!-- 顶部清空聊天入口：有用户消息时才显示 -->
          <view class="clear-tip" v-if="hasUserMessage" @tap="clearLocalHistory">
            <image class="clear-tip-icon" :src="commonIcons.trash" mode="aspectFit" />
            <text class="clear-tip-t">清空聊天记录</text>
          </view>
          <view class="msg" v-for="(m, i) in msgs" :key="i" :id="'msg-' + i" :class="{ 'msg-me': m.isMe }">
            <view class="avatar" v-if="!m.isMe">
              <image class="avatar-img" src="/static/dh-avatar.png" mode="aspectFill" />
            </view>
            <view class="bubble" :class="{ 'bubble-loading': m.loading }">
              <view class="typing-dots" v-if="m.loading">
                <view class="td" v-for="j in 3" :key="j" />
              </view>
              <text class="bubble-t" v-else>{{ m.text }}</text>
            </view>
            <view class="voice-btn" v-if="!m.isMe && !m.loading && m.text" @tap="playVoice(m)">
              <image class="voice-icon" :class="{ 'voice-playing': m.playing }" :src="commonIcons.audioWave" mode="aspectFit" />
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
            <image class="vr-icon" v-if="voiceMode" :src="commonIcons.keyboard" mode="aspectFit" />
            <image class="vr-icon" v-else :src="commonIcons.microphone" mode="aspectFit" />
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { chatWithAI, textToSpeech, clearChatHistory, getChatHistory, generateConversationId, uploadVoice } from '@/api/digitalHuman'
import { COMMON_ICONS } from '@/utils/icon-catalog'
import { ensureMiniProgramPermission } from '@/utils/permissions'

const commonIcons = COMMON_ICONS
const HISTORY_STORAGE_KEY = 'digitalHumanHistory'

const generateSessionId = () => generateConversationId()
const clearSession = (id) => clearChatHistory(id)

const inputVal = ref('')
const lastId = ref('')
const isLoading = ref(false)
const isSpeaking = ref(false)
const voiceMode = ref(false)
const recording = ref(false)
const sessionId = ref('')
const innerAudioCtx = ref(null)
const ctxScenicId = ref(null)
const ctxScenicName = ref('')
const isHistoryMode = ref(false)

const createDefaultMessages = () => [
  { text: '嗨！我是你的AI研学导师黔小游，欢迎来到中国凉都六盘水！', isMe: false },
  { text: '你可以问我关于景点、美食、路线、三线建设历史的任何问题，也可以点击下方快捷问题开始~', isMe: false }
]

const msgs = ref(createDefaultMessages())

const hasUserMessage = computed(() => msgs.value.some(m => m.isMe))

const quickQuestions = [
  '六盘水有哪些必去景点？',
  '三线建设是什么？',
  '推荐一条研学路线',
  '六盘水有什么好吃的？',
  '梅花山明天人多吗？'
]

const normalizeHistory = (history) => {
  if (!Array.isArray(history)) return []
  return history
    .filter((item) => item && item.text && !item.loading)
    .map((item) => ({
      text: String(item.text),
      isMe: !!item.isMe,
      time: item.time || ''
    }))
    .slice(-80)
}

const loadLocalHistory = () => normalizeHistory(uni.getStorageSync(HISTORY_STORAGE_KEY))

const persistLocalHistory = () => {
  const history = normalizeHistory(msgs.value)
  const hasUserQuestion = history.some((item) => item.isMe)
  if (hasUserQuestion) {
    uni.setStorageSync(HISTORY_STORAGE_KEY, history)
  }
}

const loadServerHistory = async () => {
  try {
    const history = await getChatHistory(sessionId.value)
    if (!Array.isArray(history) || history.length === 0) return null
    return history.map(item => ({
      text: item.content || item.message || item.text || '',
      isMe: item.role === 'user' || item.isMe === true,
      time: item.timestamp || item.time || ''
    })).filter(m => m.text)
  } catch (e) {
    return null
  }
}

const clearLocalHistory = () => {
  const history = loadLocalHistory()
  if (history.length === 0 && !msgs.value.some((item) => item.isMe)) {
    uni.showToast({ title: '暂无历史可清空', icon: 'none' })
    return
  }
  uni.showModal({
    title: '清空对话',
    content: '确定清空本机保存的 AI 对话历史吗？',
    success: (res) => {
      if (!res.confirm) return
      uni.removeStorageSync(HISTORY_STORAGE_KEY)
      msgs.value = createDefaultMessages()
      isHistoryMode.value = false
      if (sessionId.value) clearSession(sessionId.value).catch(() => {})
      sessionId.value = generateSessionId()
      scrollToBottom()
      uni.showToast({ title: '已清空', icon: 'none' })
    }
  })
}

const startVideoPlay = () => { isSpeaking.value = true }
const stopVideoPlay = () => { isSpeaking.value = false }

const stopAllPlaying = () => {
  stopVideoPlay()
  msgs.value.forEach(m => { m.playing = false })
}

onMounted(() => {
  sessionId.value = generateSessionId()
  innerAudioCtx.value = uni.createInnerAudioContext()
  innerAudioCtx.value.onEnded(() => stopAllPlaying())
  innerAudioCtx.value.onError(() => stopAllPlaying())

  // 处理从导览/景区详情页传入的 AI 讲解请求
  const pages = getCurrentPages()
  const curPage = pages[pages.length - 1]
  const options = curPage?.$page?.options || curPage?.options || {}

  if (options.history === '1') {
    isHistoryMode.value = true
    loadServerHistory().then((serverMsgs) => {
      if (serverMsgs && serverMsgs.length > 0) {
        msgs.value = serverMsgs
      } else {
        const history = loadLocalHistory()
        if (history.length > 0) {
          msgs.value = history
        }
      }
      scrollToBottom()
    })
  }

  // 景区上下文用于向 AI 传递 scenicId，便于 AI 侧注入预测/知识库
  if (options.scenicId) {
    const parsedId = Number(options.scenicId)
    ctxScenicId.value = Number.isFinite(parsedId) ? parsedId : options.scenicId
  }
  if (options.scenicName) {
    try {
      ctxScenicName.value = decodeURIComponent(options.scenicName)
    } catch (_) {
      ctxScenicName.value = options.scenicName
    }
    msgs.value.push({
      text: `已为你切到【${ctxScenicName.value}】的景区语境，可以直接问这里的客流、交通、玩法。`,
      isMe: false
    })
    persistLocalHistory()
  }

  if (options.initMsg) {
    let initText = options.initMsg
    try { initText = decodeURIComponent(options.initMsg) } catch (_) {}
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

  msgs.value.push({ text, isMe: true })
  inputVal.value = ''
  scrollToBottom()

  msgs.value.push({ text: '', isMe: false, loading: true })
  scrollToBottom()
  isLoading.value = true

  try {
    const res = await chatWithAI(text, sessionId.value, ctxScenicId.value)

    const replyText = res.reply
    let audioData = null

    // 先合成语音，再同时展示文字+播放声音
    if (replyText && replyText.length <= 500) {
      try {
        const ttsRes = await textToSpeech(replyText)
        if (ttsRes && ttsRes.audio) {
          audioData = ttsRes.audio
        }
      } catch (e) {
        console.warn('[DH] TTS合成失败，仅显示文字:', e)
      }
    }

    msgs.value = msgs.value.filter(m => !m.loading)
    msgs.value.push({ text: replyText || '抱歉，我暂时无法回答这个问题~', isMe: false, playing: !!audioData })
    persistLocalHistory()
    scrollToBottom()

    if (audioData) {
      startVideoPlay()
      playBase64Audio(audioData)
    }
  } catch (err) {
    msgs.value = msgs.value.filter(m => !m.loading)
    msgs.value.push({ text: '抱歉，网络出了点问题，请稍后再试~', isMe: false })
    persistLocalHistory()
    scrollToBottom()
  } finally {
    isLoading.value = false
  }
}

const onQuick = (q) => {
  inputVal.value = q
  sendMsg()
}

const playVoice = async (msg) => {
  if (msg.playing) {
    if (innerAudioCtx.value) innerAudioCtx.value.stop()
    stopAllPlaying()
    return
  }

  try {
    msgs.value.forEach(m => { m.playing = false })
    const res = await textToSpeech(msg.text)
    if (res.audio) {
      msg.playing = true
      startVideoPlay()
      playBase64Audio(res.audio)
    }
  } catch (e) {
    uni.showToast({ title: '语音播放失败', icon: 'none' })
    stopVideoPlay()
  }
}

const playBase64Audio = (base64Data) => {
  try {
    // #ifdef MP-WEIXIN
    const fs = uni.getFileSystemManager()
    const filePath = `${wx.env.USER_DATA_PATH}/tts_${Date.now()}.mp3`
    fs.writeFileSync(filePath, base64Data, 'base64')
    isSpeaking.value = true
    innerAudioCtx.value.src = filePath
    innerAudioCtx.value.play()
    // #endif

    // #ifdef H5
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
    stopVideoPlay()
  }
}

const toggleVoiceMode = () => {
  voiceMode.value = !voiceMode.value
}

const recorderManager = uni.getRecorderManager && uni.getRecorderManager()

const startRecord = async () => {
  if (!recorderManager) {
    uni.showToast({ title: '当前环境不支持录音', icon: 'none' })
    return
  }
  try {
    await ensureMiniProgramPermission('scope.record', '开启麦克风权限后，才能通过语音向黔小游提问。')
  } catch (err) {
    uni.showToast({ title: err.message || '未授予录音权限', icon: 'none' })
    return
  }
  recording.value = true
  recorderManager.start({
    format: 'mp3',
    sampleRate: 16000,
    numberOfChannels: 1
  })
}

const stopRecord = () => {
  if (!recording.value) return
  recording.value = false
  if (recorderManager) recorderManager.stop()
}

const cancelRecord = () => {
  recording.value = false
  if (recorderManager) recorderManager.stop()
}

if (recorderManager) {
  recorderManager.onStop(async (res) => {
    if (!res.tempFilePath) return

    uni.showLoading({ title: '正在识别语音', mask: true })
    try {
      const text = await uploadVoice(res.tempFilePath)
      uni.hideLoading()

      if (text) {
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
.page {
  height: 100vh;
  background: #ededed; /* 贴近微信聊天背景 */
  display: flex;
  flex-direction: column;
}

.dh-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

/* 聊天区域（全屏） */
.dh-chat {
  flex: 1;
  background: #ededed;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.chat-list { flex: 1; padding: 12px 16px 0; box-sizing: border-box; }

/* 顶部"清空聊天记录"小入口 */
.clear-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px 10px;
  margin: 0 auto 12px;
  background: rgba(0,0,0,0.04);
  border-radius: 14px;
  align-self: center;
  width: fit-content;
}
.clear-tip-icon {
  width: 12px; height: 12px; margin-right: 4px; opacity: 0.55;
}
.clear-tip-t {
  font-size: 11px; color: #999; line-height: 1;
}

.msg { display: flex; margin-bottom: 16px; align-items: flex-start; }
.msg-me { flex-direction: row-reverse; }
.avatar {
  width: 40px; height: 40px;
  border-radius: 4px; overflow: hidden;
  margin-right: 10px; flex-shrink: 0;
}
.avatar-img { width: 40px; height: 40px; border-radius: 4px; }
.msg-me .avatar { margin-right: 0; margin-left: 10px; }
.bubble {
  max-width: 70%;
  padding: 10px 14px;
  background: #ffffff;
  border-radius: 6px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}
.msg-me .bubble { background: #95ec69; }
.msg-me .bubble .bubble-t { color: #000; }
.bubble-t { font-size: 15px; line-height: 1.5; color: #1a1a1a; word-break: break-word; }
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
.voice-icon { width: 14px; height: 14px; display: block; }
.voice-playing { animation: voicePulse 0.75s ease-in-out infinite alternate; }
@keyframes voicePulse {
  0% { transform: scale(0.92); opacity: 0.72; }
  100% { transform: scale(1.08); opacity: 1; }
}

/* 快捷问题 */
.quick-bar { padding: 0 16px 8px; white-space: nowrap; }
.quick-item {
  display: inline-block; padding: 6px 14px;
  background: #fff; border-radius: 16px; margin-right: 8px;
  border: 1px solid rgba(0,0,0,0.06);
}
.quick-t { font-size: 12px; color: #2A9D8F; font-weight: 500; }

/* 输入区域 */
.input-area {
  padding: 10px 16px 30px;
  border-top: 1px solid #d8d8d8;
  background: #f7f7f7;
  display: flex;
  align-items: center;
}
.voice-record-btn {
  width: 40px; height: 40px; border-radius: 50%;
  background: #ffffff; display: flex; align-items: center; justify-content: center;
  margin-right: 8px; flex-shrink: 0;
  border: 1px solid rgba(0,0,0,0.06);
}
.vr-icon { width: 18px; height: 18px; display: block; }
.input-box { flex: 1; }
.chat-input {
  height: 40px; background: #ffffff; border-radius: 6px;
  padding: 0 12px; font-size: 15px;
  border: 1px solid rgba(0,0,0,0.06);
}
.hold-talk-btn {
  flex: 1; height: 40px; background: #ffffff; border-radius: 6px;
  display: flex; align-items: center; justify-content: center;
  border: 1px solid rgba(0,0,0,0.06);
}
.hold-t { font-size: 14px; color: #666; font-weight: 500; }
.send-btn {
  width: 60px; height: 40px; background: #07C160; border-radius: 6px;
  display: flex; align-items: center; justify-content: center; margin-left: 8px;
}
.send-t { color: #fff; font-size: 14px; font-weight: 600; }
</style>
