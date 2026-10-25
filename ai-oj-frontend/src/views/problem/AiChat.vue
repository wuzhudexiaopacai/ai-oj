<template>
  <div class="ai-chat">
    <div ref="scrollRef" class="messages">
      <div v-if="!messages.length" class="empty oj-muted">
        <p>我是 AI 刷题助手，可以帮你：</p>
        <ul>
          <li>分析题目思路与复杂度</li>
          <li>诊断你代码中的错误</li>
          <li>解释评测失败原因</li>
        </ul>
        <el-button size="small" type="primary" plain :loading="hinting" @click="onHint">
          给我一点提示
        </el-button>
      </div>

      <div
        v-for="(m, i) in messages"
        :key="i"
        class="message"
        :class="m.role === 'user' ? 'from-user' : 'from-ai'"
      >
        <div class="bubble">
          <MarkdownView :content="m.content" />
        </div>
      </div>
    </div>

    <div class="input-bar">
      <el-input
        v-model="input"
        type="textarea"
        :rows="2"
        resize="none"
        placeholder="输入你的问题，Enter 发送"
        @keydown.enter.exact.prevent="onSend"
      />
      <el-button type="primary" :loading="sending" :disabled="!input.trim()" @click="onSend">
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { createAiSession, streamChat } from '@/api/modules/ai'
import MarkdownView from '@/components/MarkdownView.vue'

const props = defineProps({
  problem: { type: Object, default: null },
  code: { type: String, default: '' },
  language: { type: String, default: '' }
})

const messages = ref([])
const input = ref('')
const sending = ref(false)
const hinting = ref(false)
const scrollRef = ref(null)

// 会话按场景复用：chat 普通问答 / hint 题目提示
const sessionIds = { chat: null, hint: null }

async function scrollToBottom() {
  await nextTick()
  scrollRef.value?.scrollTo({ top: scrollRef.value.scrollHeight, behavior: 'smooth' })
}

/** 懒创建会话（后端要求先有 sessionId 才能流式对话） */
async function ensureSession(scene) {
  if (!sessionIds[scene]) {
    sessionIds[scene] = await createAiSession({ scene, problemId: props.problem?.id })
  }
  return sessionIds[scene]
}

/** 发起一次流式问答，token 逐个追加到最后一条助手消息 */
async function ask(scene, question) {
  const sessionId = await ensureSession(scene)
  messages.value.push({ role: 'assistant', content: '' })
  const index = messages.value.length - 1
  await streamChat({
    sessionId,
    question,
    onToken: (token) => {
      messages.value[index].content += token
      scrollToBottom()
    }
  })
  if (!messages.value[index].content) {
    // 后端模型未配置 Key 时流会直接结束，这里给出可定位的提示
    messages.value[index].content = 'AI 服务未返回内容，请检查模型 API Key 是否已正确配置。'
  }
  scrollToBottom()
}

async function onSend() {
  const question = input.value.trim()
  if (!question) return
  input.value = ''
  messages.value.push({ role: 'user', content: question })
  scrollToBottom()

  sending.value = true
  try {
    await ask('chat', question)
  } catch (e) {
    messages.value.push({ role: 'assistant', content: `抱歉，AI 服务暂时不可用：${e.message || '请求失败'}` })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

async function onHint() {
  hinting.value = true
  try {
    await ask('hint', '请给我一点解题提示')
  } catch (e) {
    messages.value.push({ role: 'assistant', content: `抱歉，AI 服务暂时不可用：${e.message || '请求失败'}` })
  } finally {
    hinting.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
.ai-chat {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.messages {
  flex: 1;
  overflow: auto;
  padding-bottom: 12px;
}

.empty {
  font-size: 13px;
  line-height: 1.8;
}

.message {
  display: flex;
  margin-bottom: 12px;
}

.from-user {
  justify-content: flex-end;
}

.bubble {
  max-width: 88%;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
  background: var(--oj-bg-fill);
}

.from-user .bubble {
  background: var(--oj-primary);
  color: #fff;
}

.from-user .bubble :deep(.markdown-body p) {
  margin: 0;
}

.input-bar {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding-top: 8px;
  border-top: 1px solid var(--oj-divider);
}
</style>
