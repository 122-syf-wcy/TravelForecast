import request from '@/utils/aiRequest'

/**
 * AI智能客服聊天接口
 */

// 聊天消息类型
export interface ChatMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp?: number
}

// 发送消息请求参数
export interface ChatRequest {
  message: string
  scenicId?: number  // 当前景区ID（可选，用于上下文）
  conversationId?: string  // 会话ID（可选，用于多轮对话）
}

// 聊天响应
export interface ChatResponse {
  reply: string
  conversationId: string
  suggestions?: string[]  // 建议的后续问题
  relatedLinks?: Array<{  // 相关链接
    text: string
    url: string
  }>
}

/**
 * 发送聊天消息
 * @param data 聊天请求数据
 */
export function sendChatMessage(data: ChatRequest) {
  return request<ChatResponse>({
    url: '/chat/message',
    method: 'post',
    data,
    // 模型生成可能耗时较长，延长超时时间
    timeout: 60000
  })
}

/**
 * 获取聊天历史（可选）
 * @param conversationId 会话ID
 */
export function getChatHistory(conversationId: string) {
  return request<ChatMessage[]>({
    url: `/chat/history/${conversationId}`,
    method: 'get'
  })
}

/**
 * 获取预设问题（可选）
 * @param scenicId 景区ID
 */
export function getPresetQuestions(scenicId?: number) {
  return request<string[]>({
    url: '/chat/preset-questions',
    method: 'get',
    params: { scenicId }
  })
}

/**
 * 清空会话历史（可选）
 * @param conversationId 会话ID
 */
export function clearChatHistory(conversationId: string) {
  return request({
    url: `/chat/history/${conversationId}`,
    method: 'delete'
  })
}

