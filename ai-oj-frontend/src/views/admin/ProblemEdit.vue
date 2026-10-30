<template>
  <div class="problem-edit">
    <!-- 吸顶操作栏 -->
    <div class="edit-header oj-reveal">
      <div class="header-left">
        <el-button :icon="ArrowLeft" text @click="goBack">返回</el-button>
        <span class="header-title">{{ isEdit ? `编辑题目 ${form.problemKey || ''}` : '新建题目' }}</span>
        <el-tag v-if="isEdit" size="small" effect="plain">ID: {{ route.params.id }}</el-tag>
      </div>
      <div class="header-right">
        <el-button type="warning" plain :icon="MagicStick" @click="aiDrawer = true">AI 出题</el-button>
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">
          {{ isEdit ? '保存修改' : '创建题目' }}
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="edit-body oj-reveal" style="--reveal-delay: 0.06s">
      <!-- 基本信息 -->
      <el-card shadow="never" class="section-card">
        <template #header>
          <div class="section-title">
            <el-icon><InfoFilled /></el-icon>
            <span>基本信息</span>
          </div>
        </template>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" label-position="right" class="edit-form">
          <el-row :gutter="24">
            <el-col :span="8">
              <el-form-item label="展示题号" prop="problemKey">
                <div class="key-input-row">
                  <el-input v-model="form.problemKey" placeholder="如 A1001" />
                  <el-tooltip content="生成下一个可用题号" placement="top">
                    <el-button :icon="Refresh" :loading="keyLoading" circle plain @click="fillNextKey" />
                  </el-tooltip>
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="16">
              <el-form-item label="题目标题" prop="title">
                <el-input v-model="form.title" placeholder="请输入题目标题" maxlength="80" show-word-limit />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="24">
            <el-col :span="8">
              <el-form-item label="难度" prop="difficulty">
                <el-radio-group v-model="form.difficulty">
                  <el-radio-button v-for="(d, code) in DIFFICULTY" :key="code" :value="Number(code)">
                    {{ d.desc }}
                  </el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="计分模式">
                <el-radio-group v-model="form.ioScore">
                  <el-radio-button :value="0">ACM</el-radio-button>
                  <el-radio-button :value="1">OI</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="来源">
                <el-input v-model="form.source" placeholder="选填，如 2024 校赛" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="24">
            <el-col :span="8">
              <el-form-item label="时间限制" prop="timeLimit">
                <el-input-number
                  v-model="form.timeLimit"
                  :min="100"
                  :max="30000"
                  :step="100"
                  size="small"
                  controls-position="right"
                  class="limit-stepper"
                />
                <span class="unit-hint">毫秒</span>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="内存限制" prop="memoryLimit">
                <el-input-number
                  v-model="form.memoryLimit"
                  :min="16"
                  :max="2048"
                  :step="16"
                  size="small"
                  controls-position="right"
                  class="limit-stepper"
                />
                <span class="unit-hint">MB</span>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="栈限制">
                <el-input-number
                  v-model="form.stackLimit"
                  :min="16"
                  :max="1024"
                  :step="16"
                  size="small"
                  controls-position="right"
                  class="limit-stepper"
                />
                <span class="unit-hint">MB</span>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="24">
            <el-col :span="8">
              <el-form-item label="题目状态">
                <span class="switch-label" :class="{ 'is-on': form.status === 1 }">{{ form.status === 1 ? '上架' : '下架' }}</span>
                <el-switch v-model="form.status" :active-value="1" :inactive-value="0" class="form-switch" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="可见性">
                <span class="switch-label" :class="{ 'is-on': form.auth === 1 }">{{ form.auth === 1 ? '公开' : '私有' }}</span>
                <el-switch v-model="form.auth" :active-value="1" :inactive-value="2" class="form-switch" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="题目标签">
            <el-select v-model="form.tagIds" multiple filterable placeholder="选择标签（可多选）" style="width: 100%">
              <el-option-group v-for="cat in tagTree" :key="cat.categoryId" :label="cat.categoryName">
                <el-option v-for="tag in cat.tags" :key="tag.id" :label="tag.name" :value="tag.id">
                  <span class="tag-option">
                    <i class="tag-dot" :style="{ background: tag.color || 'var(--oj-info)' }"></i>
                    {{ tag.name }}
                  </span>
                </el-option>
              </el-option-group>
            </el-select>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 题面描述（Markdown 编辑/预览） -->
      <el-card shadow="never" class="section-card">
        <template #header>
          <div class="section-title">
            <el-icon><Document /></el-icon>
            <span>题面描述</span>
            <span class="section-tip">支持 Markdown 语法</span>
          </div>
        </template>
        <div v-for="field in mdFields" :key="field.key" class="md-field">
          <div class="md-field-head">
            <span class="md-field-label">
              {{ field.label }}
              <span v-if="field.required" class="md-required">必填</span>
            </span>
            <div class="md-tabs" role="tablist">
              <button
                v-for="m in MD_MODES"
                :key="m.value"
                type="button"
                role="tab"
                class="md-tab"
                :class="{ 'is-active': mdMode[field.key] === m.value }"
                :aria-selected="mdMode[field.key] === m.value"
                @click="mdMode[field.key] = m.value"
              >
                {{ m.label }}
              </button>
            </div>
          </div>
          <el-input
            v-show="mdMode[field.key] === 'edit'"
            v-model="form[field.key]"
            type="textarea"
            :rows="field.rows"
            :placeholder="field.placeholder"
            class="md-textarea"
          />
          <div v-show="mdMode[field.key] === 'preview'" class="md-preview">
            <MarkdownView :content="form[field.key] || '暂无内容'" />
          </div>
        </div>
      </el-card>

      <!-- 评测用例 -->
      <el-card shadow="never" class="section-card">
        <template #header>
          <div class="section-title">
            <el-icon><Files /></el-icon>
            <span>评测用例</span>
            <span class="section-tip">小数据直接填内容；大数据请先通过文件服务上传后填 objectKey</span>
          </div>
          <el-button type="primary" plain size="small" :icon="Plus" @click="addCase">添加用例</el-button>
        </template>
        <el-empty v-if="!form.testCases.length" description="暂无评测用例，至少添加一组" :image-size="60" />
        <div v-for="(tc, idx) in form.testCases" :key="idx" class="case-item">
          <div class="case-head">
            <span class="case-seq">#{{ idx + 1 }}</span>
            <el-input-number
              v-if="form.ioScore === 1"
              v-model="tc.score"
              :min="0"
              :max="100"
              size="small"
              placeholder="分值"
              style="width: 110px"
            />
            <span v-if="form.ioScore === 1" class="unit-hint">分</span>
            <el-button
              type="danger"
              text
              size="small"
              :icon="Delete"
              :disabled="form.testCases.length <= 1"
              @click="form.testCases.splice(idx, 1)"
            >
              删除
            </el-button>
          </div>
          <div class="case-grid">
            <div class="case-cell">
              <div class="case-label">输入</div>
              <el-input v-model="tc.inputData" type="textarea" :rows="4" placeholder="标准输入内容" class="mono" />
            </div>
            <div class="case-cell">
              <div class="case-label">期望输出</div>
              <el-input v-model="tc.outputData" type="textarea" :rows="4" placeholder="期望输出内容" class="mono" />
            </div>
          </div>
        </div>
      </el-card>

      <!-- 特殊判题 -->
      <el-card shadow="never" class="section-card">
        <template #header>
          <div class="section-title">
            <el-icon><MagicStick /></el-icon>
            <span>特殊判题（SPJ）</span>
          </div>
          <el-switch v-model="spjEnabled" active-text="启用" />
        </template>
        <template v-if="spjEnabled">
          <div class="spj-lang">
            <span class="case-label">SPJ 语言</span>
            <el-select v-model="form.spjLanguage" style="width: 200px">
              <el-option v-for="lang in LANGUAGES" :key="lang.value" :label="lang.label" :value="lang.value" />
            </el-select>
          </div>
          <CodeEditor v-model="form.spjCode" :language="spjMonacoLang" :height="280" />
        </template>
        <el-alert v-else type="info" :closable="false" title="未启用特殊判题，将按标准答案比对（忽略行尾空白与末尾空行）" />
      </el-card>

      <!-- 语言模板 -->
      <el-card shadow="never" class="section-card">
        <template #header>
          <div class="section-title">
            <el-icon><EditPen /></el-icon>
            <span>语言代码模板</span>
            <span class="section-tip">用户打开题目时编辑器预填的初始代码</span>
          </div>
          <el-button size="small" plain @click="fillDefaultTemplates">一键填充默认模板</el-button>
        </template>
        <el-tabs v-model="activeLangTab">
          <el-tab-pane v-for="tpl in form.languageTemplates" :key="tpl.language" :label="langLabel(tpl.language)" :name="tpl.language">
            <CodeEditor v-model="tpl.codeTemplate" :language="monacoLang(tpl.language)" :height="260" />
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>

    <!-- AI 出题抽屉 -->
    <el-drawer v-model="aiDrawer" title="AI 智能出题" size="520px" append-to-body :close-on-click-modal="false">
      <el-form label-position="top" :model="aiForm">
        <el-form-item label="知识点 / 主题" required>
          <el-input v-model="aiForm.topic" placeholder="如：动态规划、图论最短路、字符串匹配" maxlength="60" />
        </el-form-item>
        <el-form-item label="难度">
          <el-radio-group v-model="aiForm.difficulty" class="ai-drawer-radio">
            <el-radio-button v-for="(d, code) in DIFFICULTY" :key="code" :value="Number(code)">
              {{ d.desc }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="测试用例组数">
          <el-input-number v-model="aiForm.caseCount" :min="1" :max="20" :step="1" />
          <span class="unit-hint">组（默认 10）</span>
        </el-form-item>
        <el-form-item label="额外要求（可选）">
          <el-input
            v-model="aiForm.requirement"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="如：结合生活场景、数据范围不超 1e5、需要多组输入……"
          />
        </el-form-item>
      </el-form>

      <div class="ai-gen-actions">
        <el-button type="warning" :icon="MagicStick" :loading="aiGenerating" @click="doGenerate">
          {{ aiGenerating ? '生成中…' : '开始生成' }}
        </el-button>
        <el-button
          :icon="Refresh"
          :loading="aiValidating"
          :disabled="!aiDraft || !aiDraft.referenceCode"
          @click="validateDraftCases"
        >
          校验用例
        </el-button>
        <el-button
          type="primary"
          :icon="Select"
          :disabled="!aiDraft"
          @click="applyDraft"
        >
          填入当前表单
        </el-button>
      </div>

      <el-alert
        v-if="aiGenerating"
        class="ai-gen-tip"
        type="info"
        :closable="false"
        title="正在调用大模型生成题目（含参考代码与题解），预计需要十几秒，请稍候…"
      />
      <el-alert
        v-else-if="aiValidating"
        class="ai-gen-tip"
        type="warning"
        :closable="false"
        title="正在用参考代码逐组实跑校验用例，请稍候…"
      />
      <el-alert
        v-else-if="aiValidateSummary"
        class="ai-gen-tip"
        :type="aiValidateAllPassed ? 'success' : 'warning'"
        :closable="false"
        :title="aiValidateSummary"
      />

      <div v-if="aiDraft" class="ai-preview">
        <el-divider content-position="left">生成结果预览</el-divider>
        <div class="ai-preview-title">{{ aiDraft.title || '（无标题）' }}</div>
        <div class="ai-preview-meta">
          难度：{{ difficultyOf(aiDraft.difficulty).desc }}
          · 时间：{{ aiDraft.timeLimit }}ms
          · 内存：{{ aiDraft.memoryLimit }}MB
          · 用例：{{ aiDraft.testCases?.length || 0 }} 组
        </div>
        <div class="ai-preview-desc"><MarkdownView :content="aiDraft.description" /></div>
      </div>

      <div v-if="aiDraft && aiDraft.testCases && aiDraft.testCases.length" class="ai-cases">
        <el-divider content-position="left">测试用例</el-divider>
        <div v-for="(c, i) in aiDraft.testCases" :key="i" class="ai-case">
          <div class="ai-case-head">
            <span class="ai-case-seq">#{{ i + 1 }}</span>
            <el-tag v-if="c._validated && c._passed" type="success" size="small" effect="light">通过</el-tag>
            <el-tag v-else-if="c._validated && c._fixed" type="warning" size="small" effect="light">已修正</el-tag>
            <el-tag v-else-if="c._validated && c._error" type="danger" size="small" effect="light">{{ c._status }}</el-tag>
          </div>
          <div class="ai-case-io">
            <div class="ai-case-cell">
              <span class="ai-case-tag">输入</span>
              <pre class="ai-case-pre">{{ c.inputData }}</pre>
            </div>
            <div class="ai-case-cell">
              <span class="ai-case-tag">输出</span>
              <pre class="ai-case-pre">{{ c.outputData }}</pre>
            </div>
          </div>
        </div>
      </div>

      <div v-if="aiDraft && aiDraft.referenceCode" class="ai-extra">
        <el-divider content-position="left">参考代码（C++）</el-divider>
        <pre class="ai-code-pre">{{ aiDraft.referenceCode }}</pre>
      </div>

      <div v-if="aiDraft && aiDraft.solution" class="ai-extra">
        <el-divider content-position="left">题解</el-divider>
        <div class="ai-solution"><MarkdownView :content="aiDraft.solution" /></div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, InfoFilled, Document, Files, Plus, Delete, MagicStick, EditPen, Select, Refresh
} from '@element-plus/icons-vue'
import { createProblem, getAdminProblemDetail, nextProblemKey, updateProblem } from '@/api/modules/admin'
import { listTags, createSolution } from '@/api/modules/problem'
import { generateProblem } from '@/api/modules/ai'
import { validateCases } from '@/api/modules/judge'
import { CODE_TEMPLATES, DIFFICULTY, LANGUAGES, difficultyOf, languageOf } from '@/utils/oj'
import CodeEditor from '@/components/CodeEditor.vue'
import MarkdownView from '@/components/MarkdownView.vue'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const saving = ref(false)
const formRef = ref(null)
const tagTree = ref([])

const mdFields = [
  { key: 'description', label: '题目描述', required: true, rows: 10, placeholder: '题目背景、要求……' },
  { key: 'inputDesc', label: '输入描述', rows: 4, placeholder: '输入格式说明……' },
  { key: 'outputDesc', label: '输出描述', rows: 4, placeholder: '输出格式说明……' },
  { key: 'hint', label: '提示', rows: 3, placeholder: '数据范围、提示……' }
]

const MD_MODES = [
  { value: 'edit', label: '编写' },
  { value: 'preview', label: '预览' }
]

const mdMode = reactive({ description: 'preview', inputDesc: 'preview', outputDesc: 'preview', hint: 'preview' })

const defaultTemplates = () =>
  LANGUAGES.map((l) => ({ language: l.value, codeTemplate: CODE_TEMPLATES[l.value] || '' }))

const form = reactive({
  problemKey: '',
  title: '',
  description: '',
  inputDesc: '',
  outputDesc: '',
  hint: '',
  source: '',
  difficulty: 0,
  timeLimit: 1000,
  memoryLimit: 256,
  stackLimit: 128,
  ioScore: 0,
  status: 1,
  auth: 1,
  isSpj: 0,
  spjLanguage: 'cpp',
  spjCode: '',
  tagIds: [],
  testCases: [{ seq: 1, inputData: '', outputData: '', score: 0 }],
  languageTemplates: defaultTemplates()
})

const rules = {
  problemKey: [{ required: true, message: '请输入展示题号', trigger: 'blur' }],
  title: [{ required: true, message: '请输入题目标题', trigger: 'blur' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  timeLimit: [{ required: true, message: '请设置时间限制', trigger: 'blur' }],
  memoryLimit: [{ required: true, message: '请设置内存限制', trigger: 'blur' }]
}

const spjEnabled = computed({
  get: () => form.isSpj === 1,
  set: (v) => { form.isSpj = v ? 1 : 0 }
})

const activeLangTab = ref(form.languageTemplates[0]?.language || 'cpp')

const spjMonacoLang = computed(() => languageOf(form.spjLanguage).monaco)

function langLabel(value) {
  return languageOf(value).label
}

function monacoLang(value) {
  return languageOf(value).monaco
}

function addCase() {
  form.testCases.push({ seq: form.testCases.length + 1, inputData: '', outputData: '', score: 0 })
}

/* ---------------- AI 出题 ---------------- */
const aiDrawer = ref(false)
const aiGenerating = ref(false)
const aiValidating = ref(false)
const aiValidateSummary = ref('')
const aiValidateAllPassed = ref(false)
const aiDraft = ref(null)
const aiForm = reactive({ topic: '', difficulty: 1, caseCount: 10, requirement: '' })
const pendingAiSolution = reactive({ solution: '', title: '', referenceCode: '' })

async function doGenerate() {
  if (!aiForm.topic.trim()) {
    ElMessage.warning('请先填写知识点 / 主题')
    return
  }
  aiGenerating.value = true
  aiDraft.value = null
  try {
    aiDraft.value = await generateProblem({
      topic: aiForm.topic.trim(),
      difficulty: aiForm.difficulty,
      caseCount: aiForm.caseCount,
      requirement: aiForm.requirement.trim() || undefined
    })
    ElMessage.success('生成完成，可预览后填入表单')
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    aiGenerating.value = false
  }
}

function applyDraft() {
  const d = aiDraft.value
  if (!d) {
    return
  }
  if (d.title) form.title = d.title
  if (d.description) form.description = d.description
  if (d.inputDesc) form.inputDesc = d.inputDesc
  if (d.outputDesc) form.outputDesc = d.outputDesc
  if (d.hint) form.hint = d.hint
  if (d.difficulty != null) form.difficulty = d.difficulty
  if (d.timeLimit) form.timeLimit = d.timeLimit
  if (d.memoryLimit) form.memoryLimit = d.memoryLimit
  if (Array.isArray(d.testCases) && d.testCases.length) {
    form.testCases = d.testCases.map((c, i) => ({
      seq: i + 1,
      inputData: c.inputData || '',
      // 校验后的 outputData 已是参考代码实测值（未校验则沿用 AI 生成值）
      outputData: c.outputData || '',
      score: 0
    }))
  }
  // 暂存题解与参考代码，待保存题目确认后发布到题解表
  pendingAiSolution.solution = d.solution || ''
  pendingAiSolution.referenceCode = d.referenceCode || ''
  pendingAiSolution.title = d.title || form.title || '参考题解'
  aiDrawer.value = false
  ElMessage.success('已填入表单，请核对后保存')
}

async function validateDraftCases() {
  const d = aiDraft.value
  if (!d) return
  if (!d.referenceCode) {
    ElMessage.warning('本题未生成参考代码，无法校验')
    return
  }
  if (!Array.isArray(d.testCases) || !d.testCases.length) {
    ElMessage.warning('无测试用例可校验')
    return
  }
  aiValidating.value = true
  aiValidateSummary.value = ''
  aiValidateAllPassed.value = false
  try {
    const res = await validateCases({
      language: 'cpp',
      code: d.referenceCode,
      timeLimitMs: d.timeLimit || 1000,
      cases: d.testCases.map((c, i) => ({
        seq: i + 1,
        input: c.inputData || '',
        expectedOutput: c.outputData || ''
      }))
    })
    const list = (res && res.cases) || []
    let fixed = 0
    let abnormal = 0
    d.testCases.forEach((c, i) => {
      const r = list.find((x) => x.seq === i + 1) || list[i]
      c._validated = true
      c._fixed = false
      c._error = false
      if (!r) {
        c._passed = false
        c._error = true
        c._status = 'NO_RESULT'
        abnormal++
        return
      }
      c._status = r.status
      const ran = r.status === 'ACCEPTED'
      if (ran && r.actualOutput != null) {
        // 以参考代码实测输出为准回填
        c.outputData = r.actualOutput
        if (r.passed) {
          c._passed = true
        } else {
          c._fixed = true
          fixed++
        }
      } else {
        c._passed = false
        c._error = true
        abnormal++
      }
    })
    aiValidateAllPassed.value = !!(res && res.overallPassed)
    if (res && !res.compileOk) {
      aiValidateSummary.value = `参考代码编译失败，无法校验：${String(res.compileLog || '编译错误').slice(0, 200)}`
    } else if (res && res.overallPassed) {
      aiValidateSummary.value = `全部 ${res.totalCount} 组用例校验通过，期望输出可信`
    } else if (res) {
      aiValidateSummary.value = `校验完成：通过 ${res.passedCount}/${res.totalCount}，已按参考代码修正 ${fixed} 组${abnormal ? `，异常 ${abnormal} 组` : ''}`
    }
    ElMessage.success('校验完成，期望输出已按参考代码回填')
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    aiValidating.value = false
  }
}

function buildSolutionContent() {
  const s = (pendingAiSolution.solution || '').trim()
  const code = (pendingAiSolution.referenceCode || '').trim()
  if (!s) {
    return code ? `## 参考代码（C++）\n\n\`\`\`cpp\n${code}\n\`\`\`` : ''
  }
  if (code && !s.includes('```')) {
    return `${s}\n\n## 参考代码（C++）\n\n\`\`\`cpp\n${code}\n\`\`\``
  }
  return s
}

async function maybePublishSolution(problemId) {
  const content = buildSolutionContent()
  if (!content || !problemId) return
  try {
    await ElMessageBox.confirm('题目已保存，是否同时发布 AI 生成的题解？', '发布题解', {
      confirmButtonText: '发布题解',
      cancelButtonText: '暂不发布',
      type: 'success'
    })
  } catch {
    return
  }
  try {
    await createSolution({
      problemId,
      title: pendingAiSolution.title || '参考题解',
      content
    })
    ElMessage.success('题解已发布')
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

function fillDefaultTemplates() {
  const map = new Map(form.languageTemplates.map((t) => [t.language, t.codeTemplate]))
  form.languageTemplates = LANGUAGES.map((l) => ({
    language: l.value,
    codeTemplate: map.get(l.value) || CODE_TEMPLATES[l.value] || ''
  }))
}

function goBack() {
  router.push('/admin/problem')
}

/* ---------------- 题号自动生成 ---------------- */
const keyLoading = ref(false)

/** 按当前前缀生成下一个可用题号并回填（数字部分递增，仍可手动修改） */
async function fillNextKey() {
  const prefix = (/^[A-Za-z]{1,4}/.exec(form.problemKey || '') || ['A'])[0].toUpperCase()
  keyLoading.value = true
  try {
    form.problemKey = await nextProblemKey(prefix)
  } catch {
    // 接口不可用时保持手动填写，不阻断表单
  } finally {
    keyLoading.value = false
  }
}

async function loadTags() {
  try {
    tagTree.value = await listTags()
  } catch {
    tagTree.value = []
  }
}

async function loadDetail() {
  loading.value = true
  try {
    const data = await getAdminProblemDetail(route.params.id)
    Object.assign(form, {
      problemKey: data.problemKey,
      title: data.title,
      description: data.description || '',
      inputDesc: data.inputDesc || '',
      outputDesc: data.outputDesc || '',
      hint: data.hint || '',
      source: data.source || '',
      difficulty: data.difficulty ?? 0,
      timeLimit: data.timeLimit ?? 1000,
      memoryLimit: data.memoryLimit ?? 256,
      stackLimit: data.stackLimit ?? 128,
      ioScore: data.ioScore ?? 0,
      status: data.status ?? 1,
      auth: data.auth ?? 1,
      isSpj: data.isSpj ?? 0,
      spjLanguage: data.spjLanguage || 'cpp',
      spjCode: data.spjCode || '',
      tagIds: data.tagIds || [],
      testCases: (data.testCases && data.testCases.length)
        ? data.testCases
        : [{ seq: 1, inputData: '', outputData: '', score: 0 }],
      languageTemplates: (data.languageTemplates && data.languageTemplates.length)
        ? data.languageTemplates.map((t) => ({ language: t.language, codeTemplate: t.codeTemplate || '' }))
        : defaultTemplates()
    })
    activeLangTab.value = form.languageTemplates[0]?.language || 'cpp'
  } finally {
    loading.value = false
  }
}

async function save() {
  await formRef.value.validate().catch(() => {
    ElMessage.warning('请完善必填信息')
    return Promise.reject()
  })
  if (!form.description.trim()) {
    ElMessage.warning('题目描述不能为空')
    return
  }
  if (!form.testCases.length || form.testCases.every((c) => !c.inputData && !c.inputFile)) {
    ElMessage.warning('至少需要一组评测用例')
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form,
      testCases: form.testCases.map((c, i) => ({ ...c, seq: i + 1 }))
    }
    let problemId
    if (isEdit.value) {
      await updateProblem(route.params.id, payload)
      problemId = route.params.id
      ElMessage.success('保存成功')
    } else {
      problemId = await createProblem(payload)
      ElMessage.success('创建成功')
    }
    await maybePublishSolution(problemId)
    router.push('/admin/problem')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadTags()
  if (isEdit.value) {
    loadDetail()
  } else {
    // 新建时自动填充建议题号，避免手填重复撞唯一索引
    fillNextKey()
  }
})
</script>

<style scoped>
.problem-edit {
  min-height: 100%;
}

.edit-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  background: var(--oj-bg-card);
  border-bottom: 1px solid var(--oj-border);
  backdrop-filter: blur(8px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--oj-text-primary);
}

.edit-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-card {
  border: 1px solid var(--oj-border);
}

.section-card :deep(.el-card__header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--oj-text-primary);
}

.section-tip {
  font-size: 12px;
  font-weight: 400;
  color: var(--oj-text-secondary);
}

/* ---------- 表单间距与必填星号节奏统一 ---------- */
.edit-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.edit-form :deep(.el-form-item__label) {
  font-size: 13px;
  color: var(--oj-text-regular);
}

/* 必填星号紧贴标签 */
.edit-form :deep(.el-form-item.is-required:not(.is-no-asterisk) > .el-form-item__label-wrap > .el-form-item__label::before),
.edit-form :deep(.el-form-item.is-required:not(.is-no-asterisk) > .el-form-item__label::before) {
  margin-right: 2px;
}

/* ---------- 难度 / 计分模式单选按钮组：弱底 + hover 平滑过渡 + 选中橙色 ---------- */
.edit-form :deep(.el-radio-button__inner),
.ai-drawer-radio :deep(.el-radio-button__inner) {
  background: var(--oj-bg-fill);
  border-color: transparent;
  border-radius: var(--oj-radius);
  box-shadow: none;
  color: var(--oj-text-regular);
  font-weight: 500;
  transition:
    background-color 0.22s var(--oj-ease-out),
    color 0.22s var(--oj-ease-out),
    box-shadow 0.22s var(--oj-ease-out);
}

.edit-form :deep(.el-radio-button:not(.is-checked) .el-radio-button__inner:not(.is-active):hover),
.ai-drawer-radio :deep(.el-radio-button:not(.is-checked) .el-radio-button__inner:not(.is-active):hover) {
  background: var(--oj-bg-fill-dark);
  color: var(--oj-text-primary);
}

.edit-form :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner),
.ai-drawer-radio :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: var(--oj-primary);
  border-color: var(--oj-primary);
  color: #fff;
  box-shadow: none;
}

/* ---------- 限制值步进器：紧凑轻量化 ---------- */
.limit-stepper {
  width: 132px;
}

.limit-stepper :deep(.el-input-number__decrease),
.limit-stepper :deep(.el-input-number__increase) {
  background: transparent;
  border-color: var(--oj-border);
  color: var(--oj-text-secondary);
  transition: color 0.2s var(--oj-ease-out);
}

.limit-stepper :deep(.el-input-number__decrease:hover),
.limit-stepper :deep(.el-input-number__increase:hover) {
  color: var(--oj-primary);
}

/* ---------- 上架 / 公开开关 ---------- */
.switch-label {
  width: 30px;
  margin-right: 10px;
  font-size: 13px;
  color: var(--oj-text-secondary);
  transition: color 0.22s var(--oj-ease-out);
}

.switch-label.is-on {
  color: var(--oj-text-primary);
  font-weight: 500;
}

.form-switch :deep(.el-switch__core) {
  min-width: 36px;
  border-radius: 8px;
}

.form-switch :deep(.el-switch__core.is-checked) {
  background-color: var(--oj-primary);
  border-color: var(--oj-primary);
}

.unit-hint {
  margin-left: 8px;
  color: var(--oj-text-secondary);
  font-size: 12px;
}

/* 题号输入 + 生成按钮 */
.key-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.tag-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tag-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.md-field {
  margin-bottom: 20px;
}

.md-field:last-child {
  margin-bottom: 0;
}

.md-field-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.md-field-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  color: var(--oj-text-primary);
}

/* 必填记号：与表单星号同一语言，紧贴标签 */
.md-required {
  color: var(--oj-danger);
  font-size: 12px;
  font-weight: 400;
}

/* ---------- Markdown 编写 / 预览：分段 Tab 切换 ---------- */
.md-tabs {
  display: inline-flex;
  gap: 2px;
  padding: 2px;
  border-radius: var(--oj-radius);
  background: var(--oj-bg-fill);
}

.md-tab {
  border: none;
  background: transparent;
  padding: 4px 14px;
  border-radius: var(--oj-radius-sm);
  font-size: 12px;
  font-weight: 500;
  color: var(--oj-text-secondary);
  cursor: pointer;
  transition:
    background-color 0.22s var(--oj-ease-out),
    color 0.22s var(--oj-ease-out),
    box-shadow 0.22s var(--oj-ease-out);
}

.md-tab:hover {
  color: var(--oj-text-primary);
}

.md-tab.is-active {
  background: var(--oj-bg-raised);
  color: var(--oj-primary);
  box-shadow: var(--oj-shadow);
}

.md-textarea :deep(textarea) {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 13px;
}

.md-preview {
  min-height: 120px;
  padding: 12px 16px;
  border: 1px dashed var(--oj-border);
  border-radius: 6px;
  background: var(--oj-bg-page);
}

.case-item {
  border: 1px solid var(--oj-border);
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
}

.case-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.case-seq {
  font-weight: 600;
  color: var(--oj-text-primary);
  margin-right: auto;
}

.case-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 900px) {
  .case-grid {
    grid-template-columns: 1fr;
  }
}

.case-label {
  font-size: 12px;
  color: var(--oj-text-secondary);
  margin-bottom: 6px;
}

.mono :deep(textarea) {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 13px;
}

.spj-lang {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.ai-gen-actions {
  display: flex;
  gap: 12px;
  margin: 4px 0 12px;
}

/* 抽屉内难度单选组与表单风格统一 */
.ai-drawer-radio {
  width: 100%;
}

.ai-gen-tip {
  margin-bottom: 8px;
}

.ai-preview {
  margin-top: 8px;
}

.ai-preview-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--oj-text-primary);
  margin-bottom: 6px;
}

.ai-preview-meta {
  font-size: 12px;
  color: var(--oj-text-secondary);
  margin-bottom: 10px;
}

.ai-preview-desc {
  font-size: 13px;
  line-height: 1.7;
  color: var(--oj-text-regular);
  max-height: 260px;
  overflow-y: auto;
  padding: 12px;
  border: 1px solid var(--oj-border);
  border-radius: 8px;
  background: var(--oj-bg-page);
}

.ai-cases {
  margin-top: 8px;
}

.ai-case {
  border: 1px solid var(--oj-border);
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 10px;
}

.ai-case-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.ai-case-seq {
  font-weight: 600;
  color: var(--oj-text-primary);
  margin-right: auto;
}

.ai-case-io {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

@media (max-width: 640px) {
  .ai-case-io {
    grid-template-columns: 1fr;
  }
}

.ai-case-cell {
  min-width: 0;
}

.ai-case-tag {
  font-size: 12px;
  color: var(--oj-text-secondary);
}

.ai-case-pre {
  margin: 4px 0 0;
  padding: 8px 10px;
  background: var(--oj-code-bg);
  border: 1px solid var(--oj-border);
  border-radius: var(--oj-radius);
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12.5px;
  line-height: 1.5;
  color: var(--oj-text-regular);
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 160px;
  overflow-y: auto;
}

.ai-extra {
  margin-top: 8px;
}

.ai-code-pre {
  margin: 0;
  padding: 12px;
  background: var(--oj-code-bg);
  border: 1px solid var(--oj-border);
  border-radius: 8px;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--oj-text-regular);
  white-space: pre;
  overflow: auto;
  max-height: 320px;
}

.ai-solution {
  padding: 4px 2px;
  color: var(--oj-text-regular);
  font-size: 13px;
}
</style>
