<template>
  <div class="solve-page">
    <!-- 顶部工具条：返回比赛 + 比赛信息 + 实时倒计时 -->
    <div class="solve-topbar oj-card oj-reveal">
      <div class="topbar-left">
        <el-button text :icon="ArrowLeft" @click="backToContest">返回比赛题目</el-button>
        <el-divider direction="vertical" />
        <span class="contest-name">{{ contest?.title || '比赛' }}</span>
        <el-tag v-if="displayId" size="small" effect="plain" class="display-id">{{ displayId }}</el-tag>
      </div>
      <div class="countdown" :class="countdownClass">
        <span class="cd-label">{{ countdownLabel }}</span>
        <span class="cd-value">{{ countdownText }}</span>
      </div>
    </div>

    <!-- 提交可用性提示（未登录 / 未报名 / 未开赛 / 已结束） -->
    <el-alert
      v-if="noticeText"
      class="solve-notice"
      :type="noticeType"
      :title="noticeText"
      :closable="false"
      show-icon
    >
      <template v-if="showRegisterAction" #default>
        <el-button link type="primary" size="small" @click="backToContest">去报名</el-button>
      </template>
    </el-alert>

    <div class="split">
      <!-- 左侧：题目描述 -->
      <div class="pane pane-left oj-card oj-reveal" style="--reveal-delay: 0.06s">
        <div v-loading="loading" class="desc">
          <template v-if="problem">
            <div class="title-row">
              <h2 class="title">{{ problem.problemKey }}. {{ problem.title }}</h2>
              <DifficultyTag :difficulty="problem.difficulty" />
            </div>
            <div class="limits oj-muted">
              <span>时间限制：{{ problem.timeLimit }} ms</span>
              <el-divider direction="vertical" />
              <span>内存限制：{{ problem.memoryLimit }} MB</span>
            </div>

            <el-tabs v-model="leftTab">
              <el-tab-pane label="题目描述" name="desc">
                <MarkdownView :content="problem.description" />
                <template v-if="problem.samples?.length">
                  <h3 class="sample-title">示例</h3>
                  <div v-for="(s, i) in problem.samples" :key="i" class="sample">
                    <div class="sample-block">
                      <div class="sample-label oj-muted oj-copy-slot">
                        <span>输入</span>
                        <CopyButton :text="s.input" />
                      </div>
                      <pre>{{ s.input }}</pre>
                    </div>
                    <div class="sample-block">
                      <div class="sample-label oj-muted oj-copy-slot">
                        <span>输出</span>
                        <CopyButton :text="s.output" />
                      </div>
                      <pre>{{ s.output }}</pre>
                    </div>
                  </div>
                </template>
                <template v-if="problem.hint">
                  <h3 class="sample-title">提示</h3>
                  <MarkdownView :content="problem.hint" />
                </template>
              </el-tab-pane>
              <el-tab-pane label="我的提交" name="submissions">
                <MySubmissions ref="submissionsRef" :problem-id="problem.id" />
              </el-tab-pane>
            </el-tabs>
          </template>
          <el-empty v-else-if="!loading" description="题目不存在" />
        </div>
      </div>

      <!-- 右侧：代码编辑 + 提交（比赛页不提供 AI 助手） -->
      <div class="pane pane-right">
        <div class="oj-card editor-card">
          <div class="editor-toolbar">
            <el-select v-model="language" style="width: 150px" @change="onLanguageChange">
              <el-option v-for="l in LANGUAGES" :key="l.value" :label="l.label" :value="l.value" />
            </el-select>
            <el-button text size="small" @click="resetCode">重置代码</el-button>
          </div>
          <CodeEditor v-model="code" :language="monacoLanguage" :height="editorHeight" />
          <div class="editor-actions">
            <el-button type="primary" :loading="submitting" :disabled="!problem" @click="onSubmit">
              {{ submitting ? '提交评测中…' : '提交' }}
            </el-button>
          </div>
        </div>

        <!-- 评测结果 -->
        <div v-if="result" class="oj-card result-card">
          <div class="result-header">
            <span class="result-label">评测结果</span>
            <JudgeStatusTag :status="result.status" />
          </div>
          <div class="result-meta oj-muted">
            <span v-if="result.timeUsed != null">耗时 {{ result.timeUsed }} ms</span>
            <span v-if="result.memoryUsed != null">内存 {{ formatMemory(result.memoryUsed) }}</span>
            <span v-if="result.score != null">得分 {{ result.score }}</span>
          </div>
          <div v-if="result.errorMessage" class="oj-copyable">
            <pre class="compile-msg">{{ result.errorMessage }}</pre>
            <CopyButton :text="result.errorMessage" title="复制错误信息" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getProblemDetail } from '@/api/modules/problem'
import { getContestDetail } from '@/api/modules/contest'
import { submitCode, getSubmission } from '@/api/modules/judge'
import { LANGUAGES, CODE_TEMPLATES, languageOf, NON_TERMINAL_STATUS } from '@/utils/oj'
import { formatMemory } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import CodeEditor from '@/components/CodeEditor.vue'
import CopyButton from '@/components/CopyButton.vue'
import MarkdownView from '@/components/MarkdownView.vue'
import DifficultyTag from '@/components/DifficultyTag.vue'
import JudgeStatusTag from '@/components/JudgeStatusTag.vue'
import MySubmissions from '../problem/MySubmissions.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const contestId = computed(() => String(route.params.contestId || ''))
const problemId = computed(() => String(route.params.problemId || ''))

const loading = ref(false)
const problem = ref(null)
const contest = ref(null)
/** 当前用户是否已报名（未登录恒为 false） */
const registered = ref(false)
const leftTab = ref('desc')

/** 比赛时间（epoch 毫秒），以服务端推送为准 */
const startMs = ref(null)
const endMs = ref(null)
const contestStatus = ref('')
/** 服务端与本地时钟偏差（serverTime - clientTime），用于倒计时校正 */
const serverOffset = ref(0)
const nowTick = ref(Date.now())

const language = ref(localStorage.getItem('ai-oj-lang') || 'cpp')
const code = ref(CODE_TEMPLATES[language.value] || '')
const editorHeight = Math.max(320, Math.floor(window.innerHeight * 0.5))

const submitting = ref(false)
const result = ref(null)
const submissionsRef = ref(null)

let pollTimer = null
let clockTimer = null
let socket = null
let reconnectTimer = null
let reconnectAttempt = 0
/** 页面卸载后不再自动重连 */
let disposed = false

const monacoLanguage = computed(() => languageOf(language.value).monaco)

/** 赛中题号，如 A */
const displayId = computed(() => {
  const matched = (contest.value?.problems || []).find((p) => String(p.problemId) === problemId.value)
  return matched?.displayId || ''
})

/** 比赛阶段：由时间推算，避免依赖可能滞后的库内状态 */
const phase = computed(() => {
  if (contestStatus.value === 'CANCELED') {
    return 'canceled'
  }
  const now = nowTick.value + serverOffset.value
  if (startMs.value == null) {
    return 'unknown'
  }
  if (now < startMs.value) {
    return 'before'
  }
  if (endMs.value != null && now >= endMs.value) {
    return 'ended'
  }
  return 'running'
})

/** 当前阶段剩余毫秒（开赛前为距离开赛，进行中为距离结束） */
const remainMs = computed(() => {
  const now = nowTick.value + serverOffset.value
  if (phase.value === 'before') {
    return Math.max(0, startMs.value - now)
  }
  if (phase.value === 'running') {
    return endMs.value == null ? 0 : Math.max(0, endMs.value - now)
  }
  return 0
})

const countdownLabel = computed(
  () =>
    ({
      before: '距离开赛',
      running: '剩余时间',
      ended: '比赛已结束',
      canceled: '比赛已取消',
      unknown: '时间同步中'
    })[phase.value]
)

const countdownText = computed(() => {
  if (phase.value === 'ended' || phase.value === 'canceled' || phase.value === 'unknown') {
    return '--:--:--'
  }
  return formatDuration(remainMs.value)
})

/** 剩余不足 10 分钟标红提醒 */
const countdownClass = computed(() => {
  if (phase.value === 'running') {
    return remainMs.value <= 10 * 60 * 1000 ? 'is-urgent' : 'is-running'
  }
  return `is-${phase.value}`
})

const noticeText = computed(() => {
  if (!userStore.isLogin) {
    return '登录后即可提交代码'
  }
  if (contestStatus.value === 'CANCELED') {
    return '比赛已取消'
  }
  if (phase.value === 'before') {
    return '比赛尚未开始，开始后方可提交'
  }
  if (phase.value === 'ended') {
    return '比赛已结束，无法再提交'
  }
  if (phase.value === 'running' && !registered.value) {
    return '你尚未报名本场比赛，报名后方可提交'
  }
  return ''
})

const noticeType = computed(() =>
  contestStatus.value === 'CANCELED' || phase.value === 'ended' ? 'info' : 'warning'
)

const showRegisterAction = computed(
  () => userStore.isLogin && phase.value === 'running' && !registered.value
)

function pad2(value) {
  return String(value).padStart(2, '0')
}

/** 毫秒 → hh:mm:ss（超过 24 小时展示天数） */
function formatDuration(ms) {
  const total = Math.max(0, Math.floor(ms / 1000))
  const days = Math.floor(total / 86400)
  const hours = Math.floor((total % 86400) / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const seconds = total % 60
  const hms = `${pad2(hours)}:${pad2(minutes)}:${pad2(seconds)}`
  return days > 0 ? `${days} 天 ${hms}` : hms
}

async function loadContest() {
  try {
    const data = await getContestDetail(contestId.value)
    contest.value = data
    registered.value = !!data.registered
    startMs.value = data.startTime ? dayjs(data.startTime).valueOf() : null
    endMs.value = data.endTime ? dayjs(data.endTime).valueOf() : null
    contestStatus.value = data.status || ''
  } catch {
    // 请求失败已由全局拦截器提示
  }
}

async function loadProblem() {
  loading.value = true
  try {
    problem.value = await getProblemDetail(problemId.value)
  } catch {
    problem.value = null
  } finally {
    loading.value = false
  }
}

function backToContest() {
  router.push({ path: `/contest/${contestId.value}`, query: { tab: 'problems' } })
}

/**
 * 连接比赛时间推送 WebSocket。
 * 连接建立时服务端会立即下发一次当前时间快照，之后仅在管理员调整比赛时间/
 * 开赛结束状态迁移时推送，前端据此实时校正倒计时。
 */
function connectTimeSocket() {
  disposeSocket()
  if (disposed || !contestId.value) {
    return
  }
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const current = new WebSocket(`${protocol}://${window.location.host}/ws/contest?contestId=${contestId.value}`)
  socket = current
  current.onopen = () => {
    reconnectAttempt = 0
  }
  current.onmessage = (event) => {
    try {
      applyTimeSnapshot(JSON.parse(event.data))
    } catch {
      // 忽略非 JSON 消息
    }
  }
  current.onclose = () => {
    if (socket === current) {
      socket = null
    }
    if (!disposed) {
      scheduleReconnect()
    }
  }
  current.onerror = () => {
    // 由 onclose 统一触发重连
  }
}

/** 指数退避重连，最长间隔 30s */
function scheduleReconnect() {
  clearTimeout(reconnectTimer)
  const delay = Math.min(30000, 1000 * 2 ** reconnectAttempt)
  reconnectAttempt += 1
  reconnectTimer = setTimeout(connectTimeSocket, delay)
}

function disposeSocket() {
  clearTimeout(reconnectTimer)
  reconnectTimer = null
  if (socket) {
    socket.onopen = null
    socket.onmessage = null
    socket.onerror = null
    socket.onclose = null
    try {
      socket.close()
    } catch {
      // 关闭失败忽略
    }
    socket = null
  }
}

/** 应用服务端下发的时间快照（同时校正本地时钟偏差） */
function applyTimeSnapshot(data) {
  const server = Number(data.serverTimeMillis)
  if (Number.isFinite(server) && server > 0) {
    serverOffset.value = server - Date.now()
  }
  const start = Number(data.startTimeMillis)
  if (Number.isFinite(start) && start > 0) {
    startMs.value = start
  }
  const end = Number(data.endTimeMillis)
  if (Number.isFinite(end) && end > 0) {
    endMs.value = end
  }
  if (data.status) {
    contestStatus.value = data.status
  }
  nowTick.value = Date.now()
}

function onLanguageChange(lang) {
  localStorage.setItem('ai-oj-lang', lang)
  const values = Object.values(CODE_TEMPLATES)
  if (!code.value.trim() || values.includes(code.value)) {
    code.value = CODE_TEMPLATES[lang] || ''
  }
}

function resetCode() {
  code.value = CODE_TEMPLATES[language.value] || ''
}

async function onSubmit() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录后再提交')
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!registered.value) {
    ElMessage.warning('请先报名本场比赛')
    return
  }
  if (phase.value === 'before') {
    ElMessage.warning('比赛尚未开始，暂不能提交')
    return
  }
  if (phase.value !== 'running') {
    ElMessage.warning('比赛已结束，无法再提交')
    return
  }
  if (!code.value.trim()) {
    ElMessage.warning('请先编写代码')
    return
  }
  submitting.value = true
  result.value = null
  // 后端以提交标识做幂等控制，重复提交同一 submitId 会直接命中已有记录
  const submitId = crypto.randomUUID()
  try {
    await submitCode({
      submitId,
      problemId: problem.value.id,
      // contestId 为雪花 ID 字符串，不做 Number 转换（会丢精度），后端 Jackson 支持字符串转 Long
      contestId: contestId.value,
      language: language.value,
      code: code.value
    })
    ElMessage.success('提交成功，评测中…')
    submissionsRef.value?.refresh()
    pollResult(submitId)
  } catch {
    submitting.value = false
  }
}

/** 轮询评测结果直到终态（PENDING/COMPILING/JUDGING 为非终态） */
function pollResult(submitId) {
  clearTimeout(pollTimer)
  pollTimer = setTimeout(async () => {
    try {
      const data = await getSubmission(submitId)
      result.value = data
      if (NON_TERMINAL_STATUS.includes(data.status)) {
        pollResult(submitId)
      } else {
        submitting.value = false
        submissionsRef.value?.refresh()
      }
    } catch {
      submitting.value = false
    }
  }, 1500)
}

onMounted(async () => {
  clockTimer = setInterval(() => {
    nowTick.value = Date.now()
  }, 1000)
  await Promise.all([loadContest(), loadProblem()])
  connectTimeSocket()
})

onBeforeUnmount(() => {
  disposed = true
  clearInterval(clockTimer)
  clearTimeout(pollTimer)
  disposeSocket()
})

watch(problemId, (id) => {
  if (id) {
    result.value = null
    loadProblem()
  }
})

watch(contestId, () => {
  result.value = null
  loadContest()
  connectTimeSocket()
})
</script>

<style scoped>
.solve-page {
  display: flex;
  flex-direction: column;
  /* 固定为视口内高度：工具条常驻顶部，左右两栏各自内部滚动，内容不会滑入工具条下方被遮挡 */
  height: calc(100vh - 104px);
  padding: 12px 16px 16px;
}

/* 工具条：普通流式布局（不吸顶），避免滚动时覆盖编辑器工具栏 */
.solve-topbar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 16px;
  margin-bottom: 12px;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.contest-name {
  font-weight: 600;
  max-width: 420px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.countdown {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 8px;
  background: var(--oj-bg-fill);
  flex-shrink: 0;
}

.cd-label {
  font-size: 12px;
  color: var(--oj-text-secondary);
}

.cd-value {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: var(--oj-primary);
  font-variant-numeric: tabular-nums;
}

.countdown.is-running .cd-value {
  color: var(--oj-success);
}

.countdown.is-urgent .cd-value {
  color: var(--oj-danger);
}

.countdown.is-before .cd-value {
  color: var(--oj-info);
}

.countdown.is-ended .cd-value,
.countdown.is-canceled .cd-value,
.countdown.is-unknown .cd-value {
  color: var(--oj-text-secondary);
}

.solve-notice {
  flex-shrink: 0;
  margin-bottom: 12px;
}

.split {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 16px;
  align-items: stretch;
}

.pane-left {
  flex: 0 0 46%;
  height: 100%;
  overflow: auto;
}

.pane-right {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title {
  margin: 0;
  font-size: 20px;
}

.limits {
  margin: 8px 0 4px;
  font-size: 13px;
}

.sample-title {
  margin: 16px 0 8px;
  font-size: 15px;
}

.sample {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.sample-block {
  flex: 1;
  min-width: 0;
}

.sample-label {
  font-size: 12px;
  margin-bottom: 4px;
}

.sample-block pre {
  margin: 0;
  padding: 10px 12px;
  background: var(--oj-code-bg);
  border: 1px solid var(--oj-border);
  border-radius: 6px;
  white-space: pre-wrap;
  word-break: break-all;
}

.editor-card {
  padding: 12px;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.result-card {
  padding: 14px 16px;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.result-label {
  font-weight: 600;
}

.result-meta {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  font-size: 13px;
}

.compile-msg {
  margin: 10px 0 0;
  padding: 10px 12px;
  background: var(--oj-code-bg);
  border: 1px solid var(--oj-border);
  border-radius: 6px;
  font-size: 12px;
  white-space: pre-wrap;
  max-height: 200px;
  overflow: auto;
}

@media (max-width: 960px) {
  .solve-page {
    height: auto;
  }

  .split {
    flex-direction: column;
  }

  .pane-left {
    flex: none;
    width: 100%;
    height: auto;
    max-height: none;
  }

  .pane-right {
    height: auto;
    overflow: visible;
  }

  .contest-name {
    max-width: 180px;
  }
}
</style>
