<template>
  <div class="contest-manage">
    <!-- 搜索/操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="load(1)">
          <el-option v-for="(s, key) in CONTEST_STATUS" :key="key" :label="s.desc" :value="key" />
        </el-select>
        <el-select v-model="query.type" placeholder="赛制" clearable style="width: 110px" @change="load(1)">
          <el-option label="ACM" :value="0" />
          <el-option label="OI" :value="1" />
        </el-select>
      </div>
      <el-button type="primary" :icon="Plus" @click="openEdit()">创建比赛</el-button>
    </div>

    <!-- 比赛列表 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="list">
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="标题" min-width="220">
          <template #default="{ row }">
            <span class="contest-title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="赛制" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 0 ? 'primary' : 'warning'" effect="light">
              {{ row.type === 0 ? 'ACM' : 'OI' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusOf(row.status).type" effect="dark">
              {{ statusOf(row.status).desc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="170">
          <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="170">
          <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="openProblems(row)">题目</el-button>
            <el-button type="primary" text size="small" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该比赛？" width="200" @confirm="remove(row)">
              <template #reference>
                <el-button type="danger" text size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无比赛，点击右上角创建" />
        </template>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load()"
          @size-change="load(1)"
        />
      </div>
    </el-card>

    <!-- 创建/编辑弹窗 -->
    <el-dialog
      v-model="editVisible"
      :title="editForm.id ? '编辑比赛' : '创建比赛'"
      width="640px"
      destroy-on-close
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="96px">
        <el-form-item label="比赛标题" prop="title">
          <el-input v-model="editForm.title" maxlength="128" show-word-limit placeholder="请输入比赛标题" />
        </el-form-item>
        <el-form-item label="赛制" prop="type">
          <el-radio-group v-model="editForm.type">
            <el-radio-button :value="0">ACM</el-radio-button>
            <el-radio-button :value="1">OI</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="起止时间" prop="timeRange">
          <el-date-picker
            v-model="editForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="封榜时间">
          <el-date-picker
            v-model="editForm.sealTime"
            type="datetime"
            placeholder="选填，封榜后排行榜停止刷新"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="可见性">
          <el-radio-group v-model="editForm.auth">
            <el-radio :value="1">公开</el-radio>
            <el-radio :value="2">私有</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="editForm.auth === 2" label="比赛密码" prop="password">
          <el-input
            v-model="editForm.password"
            maxlength="64"
            show-password
            :placeholder="editForm.hasPassword ? '已设置密码，留空表示不修改' : '私有赛需凭密码报名'"
          />
          <div v-if="editForm.hasPassword" class="oj-muted pwd-hint">
            已设置密码（不回显明文），留空保存则保持不变；输入新密码即覆盖
          </div>
        </el-form-item>
        <el-form-item label="比赛说明">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="6"
            placeholder="支持 Markdown：比赛规则、奖励、注意事项……"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveContest">保存</el-button>
      </template>
    </el-dialog>

    <!-- 题目绑定抽屉 -->
    <el-drawer v-model="problemVisible" :title="`比赛题目 — ${currentContest?.title || ''}`" size="560px">
      <div class="bind-panel">
        <div class="bind-form">
          <el-select
            v-model="bindForm.problem"
            filterable
            remote
            :remote-method="searchProblems"
            :loading="problemSearching"
            placeholder="输入题号 / 标题搜索题目"
            style="flex: 1"
            @change="onProblemPick"
          >
            <el-option
              v-for="p in problemOptions"
              :key="p.id"
              :label="`${p.problemKey} ${p.title}`"
              :value="p.id"
            />
          </el-select>
          <el-input v-model="bindForm.title" placeholder="赛中标题（选填）" style="width: 150px" />
          <el-input-number
            v-if="currentContest?.type === 1"
            v-model="bindForm.score"
            :min="0"
            :max="1000"
            placeholder="分值"
            style="width: 110px"
          />
          <el-button type="primary" :disabled="!bindForm.problem" :loading="binding" @click="bindProblem">
            添加
          </el-button>
        </div>
        <el-alert
          type="info"
          :closable="false"
          title="赛中题号按添加顺序自动生成 A / B / C…"
          class="bind-tip"
        />
        <el-table :data="pagedContestProblems" v-loading="problemsLoading">
          <el-table-column label="题号" prop="displayId" width="70" align="center">
            <template #default="{ row }">
              <el-tag effect="dark" size="small">{{ row.displayId }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="题目" min-width="220">
            <template #default="{ row }">{{ row.title || `题目 #${row.problemId}` }}</template>
          </el-table-column>
          <el-table-column v-if="currentContest?.type === 1" label="分值" prop="score" width="80" align="center" />
          <template #empty>
            <el-empty description="尚未绑定题目" :image-size="60" />
          </template>
        </el-table>
        <!-- 有题目就展示分页器，避免题量不足一页时看起来像没有分页 -->
        <div v-if="contestProblems.length > 0" class="problem-pager">
          <el-pagination
            v-model:current-page="contestProblemPage"
            :page-size="contestProblemPageSize"
            :total="contestProblems.length"
            layout="total, prev, pager, next"
            background
            small
          />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  bindContestProblem, createContest, deleteContest, getContestDetail,
  pageContests, updateContest, adminPageProblems
} from '@/api/modules/admin'
import { formatTime } from '@/utils/format'

const CONTEST_STATUS = {
  SCHEDULED: { desc: '未开始', type: 'info' },
  RUNNING: { desc: '进行中', type: 'success' },
  ENDED: { desc: '已结束', type: 'warning' },
  CANCELED: { desc: '已取消', type: 'danger' }
}

function statusOf(status) {
  return CONTEST_STATUS[status] || { desc: status || '未知', type: 'info' }
}

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, status: null, type: null })

async function load(resetPage) {
  if (resetPage) query.pageNum = 1
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.status) params.status = query.status
    if (query.type !== null && query.type !== undefined && query.type !== '') params.type = query.type
    const data = await pageContests(params)
    list.value = data.records || []
    total.value = Number(data.total) || 0
  } finally {
    loading.value = false
  }
}

// ==================== 创建 / 编辑 ====================

const editVisible = ref(false)
const saving = ref(false)
const editFormRef = ref(null)
const editForm = reactive({
  id: null,
  title: '',
  type: 0,
  timeRange: [],
  sealTime: null,
  auth: 1,
  password: '',
  hasPassword: false,
  description: ''
})

const editRules = {
  title: [{ required: true, message: '请输入比赛标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择赛制', trigger: 'change' }],
  timeRange: [{ required: true, type: 'array', len: 2, message: '请选择起止时间', trigger: 'change' }],
  // 私有赛只在「尚未设置过密码」时才要求填写；已设置过的留空表示沿用原密码
  password: [
    {
      validator: (rule, value, callback) => {
        if (editForm.auth === 2 && !editForm.hasPassword && !value) {
          callback(new Error('私有赛请设置密码'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function openEdit(row) {
  Object.assign(editForm, {
    id: null, title: '', type: 0, timeRange: [], sealTime: null, auth: 1, password: '', hasPassword: false, description: ''
  })
  if (row) {
    const detail = await getContestDetail(row.id)
    Object.assign(editForm, {
      id: detail.id,
      title: detail.title,
      type: detail.type,
      timeRange: [detail.startTime, detail.endTime],
      sealTime: detail.sealTime || null,
      auth: detail.auth ?? 1,
      password: '',
      hasPassword: !!detail.hasPassword,
      description: detail.description || ''
    })
  }
  editVisible.value = true
}

async function saveContest() {
  await editFormRef.value.validate().catch(() => Promise.reject())
  saving.value = true
  try {
    const payload = {
      title: editForm.title,
      description: editForm.description,
      type: editForm.type,
      startTime: editForm.timeRange[0],
      endTime: editForm.timeRange[1],
      sealTime: editForm.sealTime || null,
      auth: editForm.auth,
      password: editForm.auth === 2 ? editForm.password : null
    }
    if (editForm.id) {
      await updateContest(editForm.id, payload)
      ElMessage.success('比赛已更新')
    } else {
      await createContest(payload)
      ElMessage.success('比赛已创建')
    }
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await deleteContest(row.id)
  ElMessage.success('已删除')
  if (list.value.length === 1 && query.pageNum > 1) query.pageNum -= 1
  load()
}

// ==================== 题目绑定 ====================

const problemVisible = ref(false)
const currentContest = ref(null)
const contestProblems = ref([])
/** 已绑定题目前端分页（题目随比赛详情一次性返回） */
const contestProblemPage = ref(1)
const contestProblemPageSize = 10
const pagedContestProblems = computed(() => {
  const start = (contestProblemPage.value - 1) * contestProblemPageSize
  return contestProblems.value.slice(start, start + contestProblemPageSize)
})
const problemsLoading = ref(false)
const problemOptions = ref([])
const problemSearching = ref(false)
const binding = ref(false)
const bindForm = reactive({ problem: null, title: '', score: 100 })

async function openProblems(row) {
  currentContest.value = row
  problemVisible.value = true
  bindForm.problem = null
  bindForm.title = ''
  bindForm.score = 100
  contestProblemPage.value = 1
  await loadContestProblems()
  searchProblems('')
}

async function loadContestProblems() {
  problemsLoading.value = true
  try {
    const detail = await getContestDetail(currentContest.value.id)
    contestProblems.value = detail.problems || []
    currentContest.value = { ...currentContest.value, type: detail.type }
  } finally {
    problemsLoading.value = false
  }
}

async function searchProblems(keyword) {
  problemSearching.value = true
  try {
    const data = await adminPageProblems({ pageNum: 1, pageSize: 20, keyword: keyword || undefined })
    problemOptions.value = data.records || []
  } finally {
    problemSearching.value = false
  }
}

function onProblemPick(id) {
  const picked = problemOptions.value.find((p) => p.id === id)
  if (picked && !bindForm.title) bindForm.title = picked.title
}

async function bindProblem() {
  binding.value = true
  try {
    await bindContestProblem(currentContest.value.id, {
      problemId: bindForm.problem,
      title: bindForm.title || null,
      score: currentContest.value.type === 1 ? bindForm.score : null
    })
    ElMessage.success('题目已绑定')
    bindForm.problem = null
    bindForm.title = ''
    await loadContestProblems()
    // 新题目追加在末尾，翻到最后一页让管理员看到结果
    contestProblemPage.value = Math.max(1, Math.ceil(contestProblems.value.length / contestProblemPageSize))
  } finally {
    binding.value = false
  }
}

onMounted(() => load())
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.table-card {
  border: 1px solid var(--oj-border);
}

.contest-title {
  font-weight: 500;
  color: var(--oj-text-primary);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.bind-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bind-form {
  display: flex;
  gap: 10px;
}

.bind-tip {
  margin-bottom: 4px;
}

.problem-pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.pwd-hint {
  width: 100%;
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
}
</style>
