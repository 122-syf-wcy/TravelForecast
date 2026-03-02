<template>
  <div class="p-6">
    <h2 class="text-2xl text-gray-800 mb-4">智能服务</h2>
    <scenic-card title="智能服务" icon="robot-2-line">
      <div class="conversation" ref="scrollRef">
        <div v-for="(m, i) in messages" :key="i" class="mb-3">
          <div :class="m.role === 'user' ? 'text-right' : 'text-left'">
            <span class="inline-block px-3 py-2 rounded-lg"
                  :class="m.role === 'user' ? 'bg-[#2A9D8F] text-white' : 'bg-gray-100 text-gray-800'">{{ m.content }}</span>
          </div>
        </div>
      </div>
      <div class="mt-4">
        <div class="flex flex-wrap gap-2 mb-3">
          <el-button size="small" @click="quick('梅花山风景区有哪些必去景点?')">梅花山风景区有哪些必去景点?</el-button>
          <el-button size="small" @click="quick('六盘水周边有什么美食推荐?')">六盘水周边有什么美食推荐?</el-button>
          <el-button size="small" @click="quick('明天天气怎么样? 适合出行吗?')">明天天气怎么样? 适合出行吗?</el-button>
          <el-button size="small" @click="quick('乌蒙大草原门票多少钱?')">乌蒙大草原门票多少钱?</el-button>
        </div>
        <div class="flex items-center gap-2">
          <el-input v-model="input" placeholder="询问景区、交通或其他问题..." @keyup.enter="send" />
          <el-button type="primary" :loading="loading" @click="send">发送</el-button>
        </div>
      </div>
    </scenic-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import ScenicCard from '@/components/ScenicCard.vue'
import { sendChatMessage, getChatHistory } from '@/api/chat'

interface Msg { role: 'user' | 'assistant'; content: string }

const messages = ref<Msg[]>([{ role: 'assistant', content: '你好！我是游韵华章旅游助手。请问有什么可以帮助你的吗?' }])
const input = ref('')
const loading = ref(false)
const conversationId = ref<string>('')
const scrollRef = ref<HTMLElement | null>(null)

const scrollBottom = async () => {
  await nextTick()
  if (scrollRef.value) scrollRef.value.scrollTop = scrollRef.value.scrollHeight
}

const quick = (q: string) => { input.value = q; send() }

const send = async () => {
  if (!input.value.trim()) return
  const content = input.value
  input.value = ''
  messages.value.push({ role: 'user', content })
  loading.value = true
  try {
    const res: any = await sendChatMessage({ message: content, conversationId: conversationId.value || undefined })
    const d = res.data || res
    conversationId.value = d.conversationId
    messages.value.push({ role: 'assistant', content: d.reply })
    await scrollBottom()
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '抱歉，服务暂时不可用。' })
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await scrollBottom()
  if (conversationId.value) {
    const res: any = await getChatHistory(conversationId.value)
    const list = res.data || []
    messages.value = list.map((x: any) => ({ role: x.role, content: x.content }))
  }
})
</script>

<style scoped>
.conversation { max-height: 420px; overflow: auto; padding: 12px; background: #F9FAFB; border: 1px solid #E5E7EB; border-radius: 8px; }
</style>
