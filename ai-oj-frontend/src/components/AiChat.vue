<template>
  <div class="ai-chat">
    <!-- 能力快捷条：保留“生成思路提示”，并补充常见追问入口 -->
    <div class="chat-toolbar">
      <template v-if="problem">
        <button
          v-for="s in QUICK_ASKS"
          :key="s.text"
          type="button"
          class="quick-chip"
          :disabled="sending"
          @click="onQuick(s)"
        >
          <el-icon :size="13"><component :is="s.icon" /></el-icon>
          {{ s.text }}
        </button>
      </template>
      <span v-else class="toolbar-tip oj-muted">通用问答模式 · 进入题目页后可结合题目与代码作答</span>
      <span class="toolbar-space"></span>
      <button v-if="messages.length" type="button" class="quick-chip is-plain" @click="clearConversation">
        <el-icon :size="13"><Delete /></el-icon>
        清空
      </button>
    </div>

    <div ref="scrollRef" class="messages">
      <div v-if="!messages.length" class="empty">
        <div class="hello-card">
          <span class="hello-ball">
            <span class="hb-top"></span>
            <span class="hb-band"></span>
            <span class="hb-core"></span>
          </span>
          <div>
            <p class="hello-title">我是你的刷题助手</p>
            <p class="hello-desc">
              {{ problem ? `当前题目：${problem.title}` : '可以向我询问算法思路、语言特性与判题相关的问题' }}
            </p>
          </div>
        </div>
        <ul class="ability-list">
          <li><el-icon :size="14"><Compass /></el-icon>分析题目思路与解法</li>
          <li><el-icon :size="14"><FirstAidKit /></el-icon>诊断代码错误与边界情况</li>
          <li><el-icon :size="14"><DataAnalysis /></el-icon>解释评测失败原因与复杂度</li>
        </ul>
      </div>

      <div
        v-for="(m, i) in messages"
        :key="i"
        class="message"
        :class="m.role === 'user' ? 'from-user' : 'from-ai'"
      >
        <!-- AI 消息：头像 + 无底衬正文，代码块与解析段落自然分区 -->
        <template v-if="m.role === 'assistant'">
          <span class="ai-avatar" aria-hidden="true">
            <span class="av-top"></span>
            <span class="av-band"></span>
            <span class="av-core"></span>
          </span>
          <div class="ai-body">
            <div v-if="!m.content && sending && i === messages.length - 1" class="typing">
              <i></i><i></i><i></i>
            </div>
            <MarkdownView v-else :content="m.content" />
          </div>
        </template>
        <div v-else class="bubble">{{ m.content }}</div>
      </div>
    </div>

    <div class="composer" :class="{ 'is-focus': composerFocus }">
      <el-input
        v-model="input"
        type="textarea"
        :autosize="{ minRows: 2, maxRows: 6 }"
        resize="none"
        placeholder="输入你的问题，Enter 发送 / Shift + Enter 换行"
        @focus="composerFocus = true"
        @blur="composerFocus = false"
        @keydown.enter.exact.prevent="onSend"
      />
      <div class="composer-foot">
        <span class="composer-meta oj-muted">
          <template v-if="problem">已关联当前题目与编辑器代码</template>
          <template v-else>回答由 AI 生成，请结合题意甄别</template>
        </span>
        <div class="composer-actions">
          <el-tooltip v-if="sending" content="停止生成" placement="top">
            <button type="button" class="icon-btn is-stop" @click="stop">
              <el-icon :size="15"><VideoPause /></el-icon>
            </button>
          </el-tooltip>
          <button
            type="button"
            class="icon-btn is-send"
            :class="{ 'is-ready': input.trim() && !sending }"
            :disabled="!input.trim() || sending"
            @click="onSend"
          >
            <el-icon :size="15"><Promotion /></el-icon>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import {
  Compass,
  DataAnalysis,
  Delete,
  FirstAidKit,
  Lightning,
  Promotion,
  Select,
  VideoPause,
} from '@element-plus/icons-vue'
import { createAiSession, streamChat } from '@/api/modules/ai'
import { useAssistantStore } from '@/stores/assistant'
import MarkdownView from './MarkdownView.vue'

const props = defineProps({
  problem: { type: Object, default: null },
  code: { type: String, default: '' },
  language: { type: String, default: '' }
})

const store = useAssistantStore()

const messages = ref([])
const input = ref('')
const sending = ref(false)
const composerFocus = ref(false)
const scrollRef = ref(null)

// 会话按场景复用：chat 普通问答 / hint 题目提示
const sessionIds = { chat: null, hint: null }
let abortController = null

const QUICK_ASKS = [
  { text: '生成思路提示', icon: Lightning, scene: 'hint', question: '请给我一点解题提示' },
  { text: '诊断代码', icon: Select, scene: 'chat', question: '帮我检查当前代码中的错误与边界情况，并给出修改建议' },
  { text: '复杂度分析', icon: DataAnalysis, scene: 'chat', question: '分析这道题最优解法的时间与空间复杂度' },
]

function setSending(v) {
  sending.value = v
  store.streaming = v
}

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
  abortController = new AbortController()
  await streamChat({
    sessionId,
    question,
    // 实时上报当前代码：用户改完代码再追问，AI 看到的是最新版本
    code: props.code,
    language: props.language,
    signal: abortController.signal,
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

async function runAsk(scene, question) {
  if (sending.value) return
  messages.value.push({ role: 'user', content: question })
  scrollToBottom()
  setSending(true)
  try {
    await ask(scene, question)
  } catch (e) {
    if (e?.name !== 'AbortError') {
      messages.value.push({ role: 'assistant', content: `抱歉，AI 服务暂时不可用：${e.message || '请求失败'}` })
    }
  } finally {
    setSending(false)
    abortController = null
    scrollToBottom()
  }
}

async function onSend() {
  const question = input.value.trim()
  if (!question || sending.value) return
  input.value = ''
  await runAsk('chat', question)
}

function onQuick(s) {
  if (s.scene === 'chat' && !props.problem && s.text === '诊断代码') return
  runAsk(s.scene, s.question)
}

/** 中断当前流式生成，保留已输出的内容 */
function stop() {
  abortController?.abort()
}

function clearConversation() {
  messages.value = []
  sessionIds.chat = null
  sessionIds.hint = null
}
</script>

<style scoped>
.ai-chat {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  background: var(--oj-bg-raised);
}

/* ---------- 能力快捷条 ---------- */
.chat-toolbar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-bottom: 1px solid var(--oj-divider);
  flex-shrink: 0;
  overflow-x: auto;
  scrollbar-width: none;
}

.toolbar-space {
  flex: 1;
}

.toolbar-tip {
  font-size: 12px;
  white-space: nowrap;
}

.quick-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  height: 26px;
  padding: 0 10px;
  font-size: 12px;
  color: var(--oj-text-regular);
  background: var(--oj-bg-fill);
  border: 1px solid transparent;
  border-radius: 13px;
  cursor: pointer;
  transition:
    background-color var(--oj-duration-fast) var(--oj-ease),
    color var(--oj-duration-fast) var(--oj-ease),
    border-color var(--oj-duration-fast) var(--oj-ease);
}

.quick-chip:hover:not(:disabled) {
  background: var(--oj-primary-soft);
  border-color: var(--oj-primary);
  color: var(--oj-primary-active);
}

html[data-theme='dark'] .quick-chip:hover:not(:disabled) {
  color: var(--oj-primary);
}

.quick-chip:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.quick-chip.is-plain {
  background: transparent;
  color: var(--oj-text-secondary);
}

/* ---------- 消息区 ---------- */
.messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 14px 8px;
  overflow-x: hidden;
}

.empty {
  padding: 4px 2px;
}

.hello-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--oj-border);
  border-radius: var(--oj-radius-lg);
  background: var(--oj-primary-softer);
}

.hello-ball {
  position: relative;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid var(--oj-border-strong);
  background: var(--oj-bg-raised);
}

.hb-top {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 50%;
  background: var(--oj-primary);
}

.hb-band {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 3px;
  transform: translateY(-50%);
  background: #23262d;
}

.hb-core {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  background: #fff;
  border: 2px solid #23262d;
}

.hello-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--oj-text-primary);
}

.hello-desc {
  margin: 3px 0 0;
  font-size: 12px;
  color: var(--oj-text-secondary);
  line-height: 1.6;
}

.ability-list {
  margin: 14px 2px 0;
  padding: 0;
  list-style: none;
}

.ability-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 2px;
  font-size: 13px;
  color: var(--oj-text-regular);
}

.ability-list .el-icon {
  color: var(--oj-primary);
}

.message {
  display: flex;
  margin-bottom: 16px;
}

.from-user {
  justify-content: flex-end;
}

.from-ai {
  gap: 10px;
}

.ai-avatar {
  position: relative;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  margin-top: 2px;
  border: 1px solid var(--oj-border);
  background: var(--oj-bg-raised);
}

.av-top {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 50%;
  background: var(--oj-primary);
}

.av-band {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 2px;
  transform: translateY(-50%);
  background: #23262d;
}

.av-core {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  background: #fff;
  border: 1.5px solid #23262d;
}

.ai-body {
  min-width: 0;
  flex: 1;
  padding-top: 1px;
}

.bubble {
  max-width: 82%;
  padding: 9px 14px;
  border-radius: 12px 12px 3px 12px;
  font-size: 13px;
  line-height: 1.7;
  color: #fff;
  background: var(--oj-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

/* 生成中的跳点占位 */
.typing {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 24px;
}

.typing i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--oj-text-placeholder);
  animation: typing-bounce 1s var(--oj-ease) infinite;
}

.typing i:nth-child(2) {
  animation-delay: 0.15s;
}

.typing i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes typing-bounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-3px);
    opacity: 1;
  }
}

/* ---------- 输入区 ---------- */
.composer {
  flex-shrink: 0;
  margin: 4px 12px 12px;
  padding: 8px 10px 8px;
  border: 1px solid var(--oj-border);
  border-radius: var(--oj-radius-lg);
  background: var(--oj-bg-raised);
  transition:
    border-color var(--oj-duration-fast) var(--oj-ease),
    box-shadow var(--oj-duration-fast) var(--oj-ease);
}

.composer.is-focus {
  border-color: var(--oj-primary);
  box-shadow: 0 0 0 2px var(--oj-primary-soft);
}

.composer :deep(.el-textarea__inner) {
  box-shadow: none !important;
  background: transparent;
  padding: 2px 2px 2px 4px;
  font-size: 13px;
}

.composer-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}

.composer-meta {
  font-size: 11px;
}

.composer-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition:
    background-color var(--oj-duration-fast) var(--oj-ease),
    transform var(--oj-duration-fast) var(--oj-ease);
}

.is-send {
  background: var(--oj-bg-fill);
  color: var(--oj-text-placeholder);
}

.is-send.is-ready {
  background: var(--oj-primary);
  color: #fff;
}

.is-send.is-ready:hover {
  background: var(--oj-primary-hover);
  transform: scale(1.05);
}

.is-send:disabled {
  cursor: not-allowed;
}

.is-stop {
  background: var(--oj-danger-soft);
  color: var(--oj-danger);
}

.is-stop:hover {
  background: var(--oj-danger);
  color: #fff;
}

@media (prefers-reduced-motion: reduce) {
  .typing i {
    animation: none;
  }
}
</style>
