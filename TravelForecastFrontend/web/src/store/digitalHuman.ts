import { defineStore } from 'pinia'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  time: string
}

// ==================== 页面感知导览配置 ====================
// 路由路径 → 数字人自动讲解 prompt（发给 LLM）
// 系统品牌：智教黔行 — 智教黔行·六盘水智慧研学旅游平台
const PAGE_NARRATIONS: Record<string, string> = {
  '/user/dashboard': '【页面引导】用户刚进入"智教黔行"平台的"数据总览"页面。请用2-3句话欢迎用户，说明"智教黔行"是六盘水智慧研学旅游平台，这里汇聚了全市旅游数据概况、热门景点排行和客流趋势。语气亲切，像研学导师迎接学生。',
  '/user/scenic': '【页面引导】用户刚进入"智教黔行"平台的"景区探索"页面。请用2-3句话介绍：这里有六盘水的3D全景地形图，可以点击地图上的景区标记直接跳转到实时服务查看详情。引导用户点击地图上的景区图标体验。',
  '/user/prediction': '【页面引导】用户刚进入"智教黔行"平台的"客流预测"页面。请用2-3句话介绍：可以选择景区和日期范围，平台会用AI预测模型分析未来客流趋势，帮助规划最佳出行时间。',
  '/user/planning': '【页面引导】用户刚进入"智教黔行"平台的"行程规划"页面。请用2-3句话引导用户：设置出行天数、人数和兴趣偏好，AI会自动生成个性化的六盘水研学路线，还可以让数字人讲解行程。',
  '/user/service': '【页面引导】用户刚进入"智教黔行"平台的"实时服务"页面。请用2-3句话介绍：可以查看当前景区的实时客流、天气、设施信息，还有紧急救援入口，是研学出行的实时保障中心。',
}

// ==================== 景区讲解配置（方案二用） ====================
// 景区名称 → 数字人讲解 prompt
export function buildScenicNarrationPrompt(scenicName: string): string {
  return `【景区讲解】你是"智教黔行"平台的AI研学导师"黔小游"。请用生动活泼的语言为研学学生讲解"${scenicName}"：包括地理位置特征、自然或文化亮点、研学价值。控制在150字以内，结尾抛一个趣味小问题引发思考。`
}

// ==================== 行程伴讲配置（方案四用） ====================
export function buildItineraryNarrationPrompt(itineraryText: string): string {
  return `【行程伴讲】你是"智教黔行"平台的AI研学导师"黔小游"。请按以下行程逐站点讲解，每个景点讲2-3句重点研学知识。语气像带学生出发前的行程说明会，轻松有趣。行程内容：\n${itineraryText}`
}

// ==================== 客流预测数据分析配置 ====================
export function buildPredictionNarrationPrompt(predictionText: string): string {
  return `【数据分析】你是"智教黔行"平台的AI研学导师"黔小游"。请根据以下客流预测数据，为研学团队提供专业分析和出行建议：哪天客流最少适合参观？哪天需要避开高峰？天气对出行有什么影响？用通俗易懂的语言，控制在200字以内。\n预测数据：\n${predictionText}`
}


export const useDigitalHumanStore = defineStore('digitalHuman', {
  state: () => ({
    messages: [] as ChatMessage[],
    isConnected: false,
    isSpeaking: false,
    isTyping: false,
    currentStatus: 'idle' as 'idle' | 'thinking' | 'speaking',
    // 用于在实时服务页面时隐藏悬浮窗
    isEmbeddedMode: false,

    // ========== 智能导览系统 ==========
    // 待发送的讲解 prompt（DigitalHuman.vue 监听并自动发送）
    pendingNarration: null as string | null,
    // 本次会话已讲解过的页面（不重复讲解）
    narratedPages: {} as Record<string, boolean>,
  }),

  getters: {
    getMessages: (state) => state.messages,
    hasMessages: (state) => state.messages.length > 0
  },

  actions: {
    addMessage(role: 'user' | 'assistant', content: string) {
      this.messages.push({
        role,
        content,
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      })
    },

    clearMessages() {
      this.messages = []
    },

    setConnected(connected: boolean) {
      this.isConnected = connected
    },

    setSpeaking(speaking: boolean) {
      this.isSpeaking = speaking
      this.currentStatus = speaking ? 'speaking' : 'idle'
    },

    setTyping(typing: boolean) {
      this.isTyping = typing
      if (typing) this.currentStatus = 'thinking'
    },

    setStatus(status: 'idle' | 'thinking' | 'speaking') {
      this.currentStatus = status
    },

    setEmbeddedMode(embedded: boolean) {
      this.isEmbeddedMode = embedded
    },

    // 初始化欢迎消息
    initWelcomeMessage() {
      if (this.messages.length === 0) {
        this.addMessage('assistant', '你好呀！欢迎来到**智教黔行**！我是**黔小游**，你的六盘水研学导师~\n\n想了解景区、规划行程、还是查看客流预测？尽管问我吧！')
      }
    },

    // ========== 智能导览 Actions ==========

    /**
     * 页面感知自动导览（方案一）
     * 用户进入某个页面时调用，首次进入自动触发数字人讲解
     */
    triggerPageNarration(path: string) {
      const prompt = PAGE_NARRATIONS[path]
      if (!prompt) return
      // 同一页面同一会话只讲一次
      if (this.narratedPages[path]) return
      this.narratedPages[path] = true
      this.pendingNarration = prompt
    },

    /**
     * 景区点击讲解（方案二）
     * 用户在地图上点击景区标记时调用
     */
    triggerScenicNarration(scenicName: string) {
      const key = `scenic:${scenicName}`
      if (this.narratedPages[key]) return
      this.narratedPages[key] = true
      this.pendingNarration = buildScenicNarrationPrompt(scenicName)
    },

    /**
     * AI行程伴讲（方案四）
     * 用户生成行程后自动 / 手动触发
     */
    triggerItineraryNarration(itineraryText: string) {
      this.pendingNarration = buildItineraryNarrationPrompt(itineraryText)
    },

    /**
     * 客流预测数据分析
     * 用户生成预测后自动触发，基于真实预测数据分析讲解
     */
    triggerPredictionNarration(predictionText: string) {
      this.pendingNarration = buildPredictionNarrationPrompt(predictionText)
    },

    /**
     * 消费 pendingNarration（DigitalHuman.vue 调用）
     * 取出待发送的 prompt 并清空
     */
    consumeNarration(): string | null {
      const prompt = this.pendingNarration
      this.pendingNarration = null
      return prompt
    },

    /** 重置导览记录（刷新/重新登录时） */
    resetNarrated() {
      this.narratedPages = {}
    }
  }
})
