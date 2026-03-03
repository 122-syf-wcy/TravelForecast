import { AI_API_BASE_URL } from '@/config'

/**
 * AI聊天（走公用AI后端 8081）
 * @param {string} message - 用户消息
 * @param {string} [conversationId] - 会话ID（可选，为空创建新会话）
 * @param {number} [scenicId] - 关联景区ID（可选）
 * @returns {Promise<{reply: string, conversationId: string}>}
 */
export const chatWithAI = (message, conversationId, scenicId) => {
  const token = uni.getStorageSync('token')
  return new Promise((resolve, reject) => {
    uni.request({
      url: AI_API_BASE_URL + '/chat/message',
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      data: { message, conversationId, scenicId },
      timeout: 30000,
      success: (res) => {
        const body = res.data || {}
        if (body.code === 200 && body.data) {
          resolve(body.data)
        } else {
          reject(new Error(body.message || 'AI对话失败'))
        }
      },
      fail: (err) => reject(err)
    })
  })
}

/**
 * 文本转语音（走公用AI后端 8081，返回二进制音频）
 * @param {string} text - 要合成的文本
 * @param {string} [voice] - 发音人
 * @returns {Promise<ArrayBuffer>} 音频二进制数据
 */
export const textToSpeech = (text, voice) => {
  return new Promise((resolve, reject) => {
    uni.request({
      url: AI_API_BASE_URL + '/speech/tts',
      method: 'POST',
      header: { 'Content-Type': 'application/json' },
      data: { text, voice, speed: 1.0 },
      responseType: 'arraybuffer',
      timeout: 30000,
      success: (res) => {
        if (res.statusCode === 200 && res.data) {
          // 将ArrayBuffer转为base64供playBase64Audio使用
          const base64 = uni.arrayBufferToBase64(res.data)
          resolve({ audio: base64 })
        } else {
          reject(new Error('语音合成失败'))
        }
      },
      fail: (err) => reject(err)
    })
  })
}

/**
 * 获取会话历史
 */
export const getChatHistory = (conversationId) => {
  const token = uni.getStorageSync('token')
  return new Promise((resolve, reject) => {
    uni.request({
      url: AI_API_BASE_URL + `/chat/history/${conversationId}`,
      header: token ? { Authorization: `Bearer ${token}` } : {},
      timeout: 10000,
      success: (res) => {
        const body = res.data || {}
        resolve(body.code === 200 ? body.data : [])
      },
      fail: () => resolve([])
    })
  })
}

/**
 * 清除会话历史
 */
export const clearChatHistory = (conversationId) => {
  return new Promise((resolve, reject) => {
    uni.request({
      url: AI_API_BASE_URL + `/chat/history/${conversationId}`,
      method: 'DELETE',
      timeout: 10000,
      success: () => resolve(),
      fail: () => resolve()
    })
  })
}

/**
 * 生成唯一会话ID
 */
export const generateConversationId = () => {
  return 'mp_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8)
}

/**
 * 语音识别 (STT) - 调用数字人Python后端(5000端口)进行极速本地语音转文本
 * @param {string} tempFilePath 小程序本地录音路径
 * @returns {Promise<string>} 识别出的文本
 */
export const uploadVoice = (tempFilePath) => {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: 'http://localhost:5000/api/stt', // 调用Python后端的STT接口
      filePath: tempFilePath,
      name: 'file',
      success: (res) => {
        try {
          const body = JSON.parse(res.data || '{}')
          if (body.code === 200 && body.data) {
            resolve(body.data)
          } else {
            reject(new Error(body.message || '由于说话声音小等原因，没有识别到文字'))
          }
        } catch (e) {
          reject(new Error('语音识别解析失败'))
        }
      },
      fail: (err) => reject(err)
    })
  })
}
