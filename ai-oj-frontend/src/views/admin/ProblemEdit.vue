<template>
  <div class="problem-edit">
    <!-- 吸顶操作栏 -->
    <div class="edit-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" text @click="goBack">返回</el-button>
        <span class="header-title">{{ isEdit ? `编辑题目 ${form.problemKey || ''}` : '新建题目' }}</span>
        <el-tag v-if="isEdit" size="small" effect="plain">ID: {{ route.params.id }}</el-tag>
      </div>
      <div class="header-right">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">
          {{ isEdit ? '保存修改' : '创建题目' }}
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="edit-body">
      <!-- 基本信息 -->
      <el-card shadow="never" class="section-card">
        <template #header>
          <div class="section-title">
            <el-icon><InfoFilled /></el-icon>
            <span>基本信息</span>
          </div>
        </template>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" label-position="right">
          <el-row :gutter="24">
            <el-col :span="8">
              <el-form-item label="展示题号" prop="problemKey">
                <el-input v-model="form.problemKey" placeholder="如 A1001" :disabled="isEdit" />
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
                <el-input-number v-model="form.timeLimit" :min="100" :max="30000" :step="100" style="width: 100%" />
                <span class="unit-hint">毫秒</span>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="内存限制" prop="memoryLimit">
                <el-input-number v-model="form.memoryLimit" :min="16" :max="2048" :step="16" style="width: 100%" />
                <span class="unit-hint">MB</span>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="栈限制">
                <el-input-number v-model="form.stackLimit" :min="16" :max="1024" :step="16" style="width: 100%" />
                <span class="unit-hint">MB</span>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="24">
            <el-col :span="8">
              <el-form-item label="状态">
                <el-radio-group v-model="form.status">
                  <el-radio :value="1">上架</el-radio>
                  <el-radio :value="0">下架</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="可见性">
                <el-radio-group v-model="form.auth">
                  <el-radio :value="1">公开</el-radio>
                  <el-radio :value="2">私有</el-radio>
                </el-radio-group>
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
              <el-tag v-if="field.required" type="danger" size="small" effect="plain">必填</el-tag>
            </span>
            <el-radio-group v-model="mdMode[field.key]" size="small">
              <el-radio-button value="edit">编写</el-radio-button>
              <el-radio-button value="preview">预览</el-radio-button>
            </el-radio-group>
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, InfoFilled, Document, Files, Plus, Delete, MagicStick, EditPen
} from '@element-plus/icons-vue'
import { createProblem, getAdminProblemDetail, updateProblem } from '@/api/modules/admin'
import { listTags } from '@/api/modules/problem'
import { CODE_TEMPLATES, DIFFICULTY, LANGUAGES, languageOf } from '@/utils/oj'
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

const mdMode = reactive({ description: 'edit', inputDesc: 'edit', outputDesc: 'edit', hint: 'edit' })

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
    if (isEdit.value) {
      await updateProblem(route.params.id, payload)
      ElMessage.success('保存成功')
    } else {
      await createProblem(payload)
      ElMessage.success('创建成功')
    }
    router.push('/admin/problem')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadTags()
  if (isEdit.value) loadDetail()
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

.unit-hint {
  margin-left: 8px;
  color: var(--oj-text-secondary);
  font-size: 12px;
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

.md-field-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.md-field-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: var(--oj-text-primary);
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
</style>
