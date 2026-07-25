<template>
  <div class="ai-chat">
    <van-nav-bar title="AI助手" fixed />
    <div class="chat-container">
      <div class="messages" ref="msgContainer">
        <div v-for="(msg, i) in messages" :key="i" :class="['message', msg.role]">
          <div class="avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
          <div class="bubble" v-html="msg.content"></div>
        </div>
        <div v-if="typing" class="message assistant">
          <div class="avatar">AI</div>
          <div class="bubble typing">正在思考...</div>
        </div>
      </div>

      <!-- 快捷提问 -->
      <div class="quick-questions" v-if="messages.length <= 1">
        <van-tag
          v-for="q in quickQuestions"
          :key="q"
          plain
          type="primary"
          size="medium"
          class="quick-tag"
          @click="sendQuick(q)"
        >{{ q }}</van-tag>
      </div>

      <div class="input-bar">
        <van-field
          v-model="input"
          placeholder="输入您的问题..."
          :disabled="typing"
          @keypress.enter="sendMessage"
        />
        <van-button type="primary" size="small" :disabled="!input || typing" @click="sendMessage">发送</van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { showToast } from 'vant'
import { aiChat } from '@/api/ai'
import { marked } from 'marked'

interface Message {
  role: 'user' | 'assistant'
  content: string
}

const quickQuestions = [
  '附近有哪些公寓？',
  '推荐一个性价比高的房源',
  '两室一厅有哪些？',
  '月租2000以内有什么选择？'
]

const messages = ref<Message[]>([
  { role: 'assistant', content: '你好！我是您的租赁助手，可以帮您了解公寓信息、推荐合适的房源。请问有什么可以帮您的？' }
])
const input = ref('')
const typing = ref(false)
const msgContainer = ref<HTMLElement>()

function sendQuick(text: string) {
  input.value = text
  sendMessage()
}

async function sendMessage() {
  const text = input.value.trim()
  if (!text || typing.value) return
  messages.value.push({ role: 'user', content: text })
  input.value = ''
  typing.value = true
  await scrollToBottom()
  try {
    const res = await aiChat(text)
    const reply = res.data.data?.message || '抱歉，暂无回复'
    messages.value.push({ role: 'assistant', content: marked(reply) as string })
  } catch {
    showToast('发送失败')
  } finally {
    typing.value = false
    await scrollToBottom()
  }
}

async function scrollToBottom() {
  await nextTick()
  if (msgContainer.value) {
    msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  }
}
</script>

<style scoped>
.ai-chat { display: flex; flex-direction: column; height: 100vh; }
.chat-container { flex: 1; display: flex; flex-direction: column; padding-top: 46px; }
.messages { flex: 1; overflow-y: auto; padding: 10px 15px; }
.message { display: flex; margin-bottom: 15px; }
.message.user { flex-direction: row-reverse; }
.avatar { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; color: #fff; flex-shrink: 0; }
.message.user .avatar { background: #1989fa; }
.message.assistant .avatar { background: #07c160; }
.bubble { max-width: 75%; padding: 10px 14px; border-radius: 12px; font-size: 14px; line-height: 1.6; word-break: break-word; }
.message.user .bubble { background: #1989fa; color: #fff; border-top-right-radius: 4px; }
.message.assistant .bubble { background: #fff; border-top-left-radius: 4px; }
.typing { color: #999; }
.quick-questions { padding: 10px 15px; display: flex; flex-wrap: wrap; gap: 8px; }
.quick-tag { cursor: pointer; }
.input-bar { display: flex; align-items: center; padding: 8px 10px; background: #fff; border-top: 1px solid #eee; }
.input-bar .van-field { flex: 1; }
</style>
