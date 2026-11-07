<template>
  <div class="problem-page">
    <div class="split">
      <!-- 左侧：题目描述 -->
      <div class="pane pane-left oj-card oj-reveal">
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
                      <div class="sample-label oj-copy-slot">
                        <span>输入</span>
                        <CopyButton :text="s.input" />
                      </div>
                      <pre class="code-block">{{ s.input }}</pre>
                    </div>
                    <div class="sample-block">
                      <div class="sample-label oj-copy-slot">
                        <span>输出</span>
                        <CopyButton :text="s.output" />
                      </div>
                      <pre class="code-block">{{ s.output }}</pre>
                    </div>
                  </div>
                </template>
                <template v-if="problem.hint">
                  <h3 class="sample-title">提示</h3>
                  <MarkdownView :content="problem.hint" />
                </template>
              </el-tab-pane>
              <el-tab-pane label="提交记录" name="submissions">
                <MySubmissions ref="submissionsRef" :problem-id="problem.id" />
              </el-tab-pane>
              <el-tab-pane label="题解" name="solutions" lazy>
                <SolutionPanel :problem-id="problem.id" />
              </el-tab-pane>
            </el-tabs>
          </template>
          <el-empty v-else-if="!loading" description="题目不存在" />
        </div>
      </div>

      <!-- 右侧：代码编辑 + 提交 -->
      <div class="pane pane-right oj-reveal" style="--reveal-delay: 0.08s">
        <div class="oj-card editor-card">
          <div class="editor-toolbar">
            <el-select v-model="language" style="width: 150px" @change="onLanguageChange">
              <el-option v-for="l in LANGUAGES" :key="l.value" :label="l.label" :value="l.value" />
            </el-select>
            <el-button class="btn-ghost" text size="small" @click="resetCode">重置代码</el-button>
          </div>
          <CodeEditor v-model="code" :language="monacoLanguage" :height="editorHeight" />
          <div class="editor-actions">
            <el-button class="btn-ai" text :icon="MagicStick" @click="assistant.show()">AI 助手</el-button>
            <el-button
              class="btn-submit"
              type="primary"
              :loading="submitting"
              :disabled="!problem"
              @click="onSubmit"
            >
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
          <!-- 后端 SubmissionDetailVO 的编译/运行错误字段是 errorMessage -->
          <div v-if="result.errorMessage" class="oj-copyable">
            <pre class="compile-msg">{{ result.errorMessage }}</pre>
            <CopyButton :text="result.errorMessage" title="复制错误信息" />
          </div>

          <!-- 失败用例回显（仅普通题目提交：比赛提交后端不返回用例数据） -->
          <template v-if="failedCases.length">
            <div class="wrong-case-head">
              <span class="wrong-case-title">错误用例（共 {{ failedCases.length }} 个）</span>
              <el-button link type="primary" size="small" @click="downloadWrongCases">下载错误用例</el-button>
            </div>
            <el-collapse class="wrong-case-list">
              <el-collapse-item v-for="item in failedCases" :key="item.seq" :name="item.seq">
                <template #title>
                  <span class="wrong-case-seq">用例 #{{ item.seq }}</span>
                  <JudgeStatusTag :status="item.status" />
                </template>
                <div class="case-block">
                  <div class="case-label oj-copy-slot">
                    <span>输入</span>
                    <CopyButton :text="item.inputPreview" />
                  </div>
                  <pre class="case-pre">{{ item.inputPreview || '（空）' }}</pre>
                </div>
                <div class="case-block">
                  <div class="case-label oj-copy-slot">
                    <span>期望输出</span>
                    <CopyButton :text="item.expectedPreview" />
                  </div>
                  <pre class="case-pre">{{ item.expectedPreview || '（空）' }}</pre>
                </div>
                <div class="case-block">
                  <div class="case-label oj-copy-slot">
                    <span>你的输出</span>
                    <CopyButton :text="item.outputPreview" />
                  </div>
                  <pre class="case-pre">{{ item.outputPreview || '（空）' }}</pre>
                </div>
              </el-collapse-item>
            </el-collapse>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import { getProblemDetail } from '@/api/modules/problem'
import { submitCode, getSubmission } from '@/api/modules/judge'
import { LANGUAGES, CODE_TEMPLATES, languageOf, NON_TERMINAL_STATUS } from '@/utils/oj'
import { formatMemory } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import CodeEditor from '@/components/CodeEditor.vue'
import MarkdownView from '@/components/MarkdownView.vue'
import SolutionPanel from '@/components/SolutionPanel.vue'
import DifficultyTag from '@/components/DifficultyTag.vue'
import JudgeStatusTag from '@/components/JudgeStatusTag.vue'
import CopyButton from '@/components/CopyButton.vue'
import MySubmissions from './MySubmissions.vue'
import { useAssistantStore } from '@/stores/assistant'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const assistant = useAssistantStore()

const loading = ref(false)
const problem = ref(null)
const leftTab = ref('desc')

const language = ref(localStorage.getItem('ai-oj-lang') || 'cpp')
const code = ref(CODE_TEMPLATES[language.value] || '')
const editorHeight = Math.max(320, Math.floor(window.innerHeight * 0.55))

const submitting = ref(false)
const result = ref(null)
const submissionsRef = ref(null)

let pollTimer = null

const monacoLanguage = computed(() => languageOf(language.value).monaco)

/**
 * 失败用例：后端只对普通题目（非比赛）提交回填输入/期望输出，
 * 因此用这两个字段是否存在来判断是否可回显
 */
const failedCases = computed(() =>
  (result.value?.cases || []).filter((item) => item.inputPreview != null || item.expectedPreview != null)
)

/** 下载错误用例（前端根据已获取的用例内容生成 txt，无需额外接口） */
function downloadWrongCases() {
  const text = failedCases.value
    .map((item) =>
      [
        `===== 用例 #${item.seq}（${item.statusDesc || ''}）=====`,
        '【输入】',
        item.inputPreview || '',
        '【期望输出】',
        item.expectedPreview || '',
        '【你的输出】',
        item.outputPreview || '',
        ''
      ].join('\n')
    )
    .join('\n')
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `错误用例-${result.value?.submitId || 'submission'}.txt`
  a.click()
  URL.revokeObjectURL(url)
}

async function load() {
  loading.value = true
  try {
    problem.value = await getProblemDetail(route.params.id)
  } finally {
    loading.value = false
  }
}

function onLanguageChange(lang) {
  localStorage.setItem('ai-oj-lang', lang)
  // 若编辑器内容为空或仍是旧模板，则切换为新语言模板
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
  if (!code.value.trim()) {
    ElMessage.warning('请先编写代码')
    return
  }
  submitting.value = true
  result.value = null
  // 后端要求客户端生成幂等提交标识（@NotBlank），重复提交同一 submitId 会直接命中已有记录
  const submitId = crypto.randomUUID()
  try {
    await submitCode({
      submitId,
      problemId: problem.value.id,
      // contestId 为雪花 ID 字符串，不能用 Number() 转换（会丢精度），后端 Jackson 支持字符串转 Long
      contestId: route.query.contestId || 0,
      language: language.value,
      code: code.value
    })
    ElMessage.success('提交成功，评测中…')
    // 提交成功后立即刷新提交记录列表，无需用户手动刷新页面
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
        // 出终态后同步列表里的状态/耗时，避免列表仍显示「评测中」
        submissionsRef.value?.refresh()
      }
    } catch {
      submitting.value = false
    }
  }, 1500)
}

onMounted(load)
onBeforeUnmount(() => {
  clearTimeout(pollTimer)
  assistant.clearContext()
  // 离开题目页后隐藏悬浮球与面板，再次点击“AI 助手”才会出现
  assistant.hide()
})

// 将题目与编辑器实时状态同步给全局 AI 助手悬浮球
watch(
  [problem, code, language],
  () => assistant.setContext({ problem: problem.value, code: code.value, language: language.value }),
  { immediate: true }
)

watch(
  () => route.params.id,
  (id) => {
    if (id) {
      result.value = null
      load()
    }
  }
)
</script>

<style scoped>
.problem-page {
  display: flex;
  flex-direction: column;
  /* 固定为视口内高度：左右两栏各自内部滚动，宽屏/窄屏下代码编辑器宽度都能自适应 */
  height: calc(100vh - 104px);
  padding: 16px;
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
  font-size: 12.5px;
  color: var(--oj-text-secondary);
}

/* Tab 选中态文字高亮 + hover 平滑背景过渡 */
.desc :deep(.el-tabs__item) {
  height: 34px;
  line-height: 34px;
  padding: 0 14px;
  border-radius: var(--oj-radius);
  transition: color var(--oj-duration-fast) var(--oj-ease-out),
    background-color var(--oj-duration-fast) var(--oj-ease-out);
}
.desc :deep(.el-tabs__item:hover) {
  color: var(--oj-primary);
  background: var(--oj-bg-fill);
}
.desc :deep(.el-tabs__item.is-active) {
  color: var(--oj-primary);
  font-weight: 600;
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
  font-weight: 500;
  color: var(--oj-text-secondary);
  margin-bottom: 5px;
}

/* 示例：只读代码块，等宽字体，仅用于展示 */
.code-block {
  margin: 0;
  padding: 10px 12px;
  background: var(--oj-code-bg);
  border: 1px solid var(--oj-border);
  border-radius: var(--oj-radius);
  font-family: 'JetBrains Mono', Consolas, 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: var(--oj-text-regular);
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
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}

/* 重置代码：视觉弱化 */
.btn-ghost {
  color: var(--oj-text-secondary);
  transition: color var(--oj-duration-fast) var(--oj-ease-out);
}
.btn-ghost:hover {
  color: var(--oj-text-regular);
}

/* AI 助手：权重低于提交（文字按钮 + 次级色） */
.btn-ai {
  color: var(--oj-text-regular);
  transition: color var(--oj-duration-fast) var(--oj-ease-out);
}
.btn-ai:hover {
  color: var(--oj-primary);
}

/* 提交：主按钮，hover 柔和过渡 */
.editor-actions :deep(.btn-submit) {
  transition: background-color var(--oj-duration-fast) var(--oj-ease-out),
    box-shadow var(--oj-duration-fast) var(--oj-ease-out),
    transform var(--oj-duration-fast) var(--oj-ease-out);
}
.editor-actions :deep(.btn-submit:hover) {
  box-shadow: var(--oj-shadow-hover);
}

.result-card {
  padding: 14px 16px;
  animation: result-in var(--oj-duration) var(--oj-ease-out) both;
}

/* 评测面板平滑向下展开入场 */
@keyframes result-in {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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

.wrong-case-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

.wrong-case-title {
  font-size: 13px;
  font-weight: 600;
}

.wrong-case-list {
  margin-top: 4px;
  border-top: none;
}

/* 错误用例条目 hover 背景反馈 */
.wrong-case-list :deep(.el-collapse-item__header) {
  padding: 0 10px;
  border-radius: var(--oj-radius);
  transition: background-color var(--oj-duration-fast) var(--oj-ease-out);
}
.wrong-case-list :deep(.el-collapse-item__header:hover) {
  background: var(--oj-bg-fill);
}

.wrong-case-seq {
  margin-right: 8px;
  font-size: 13px;
}

.case-block + .case-block {
  margin-top: 10px;
}

.case-label {
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--oj-text-secondary);
}

.case-pre {
  margin: 0;
  padding: 8px 10px;
  background: var(--oj-code-bg);
  border: 1px solid var(--oj-border);
  border-radius: 6px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow: auto;
}

@media (max-width: 960px) {
  .problem-page {
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
}
</style>
