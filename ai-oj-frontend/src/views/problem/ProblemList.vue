<template>
  <div class="oj-page">
    <div class="oj-container">
      <div class="oj-page-header">
        <h2 class="oj-page-title">题库</h2>
        <div class="filters">
          <el-select
            v-model="query.difficulty"
            placeholder="难度"
            clearable
            style="width: 110px"
            @change="load(1)"
          >
            <el-option v-for="(d, code) in DIFFICULTY" :key="code" :label="d.desc" :value="Number(code)" />
          </el-select>
          <el-input
            v-model="query.keyword"
            placeholder="搜索题号 / 标题"
            clearable
            style="width: 220px"
            @keyup.enter="load(1)"
            @clear="load(1)"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </div>

      <div class="oj-card table-card">
        <el-table v-loading="loading" :data="list" @row-click="goDetail">
          <el-table-column label="状态" width="92" align="center">
            <template #default="{ row }">
              <el-tooltip v-if="isSolved(row)" content="已通过" placement="top">
                <el-icon color="var(--oj-success)" :size="18"><CircleCheckFilled /></el-icon>
              </el-tooltip>
              <el-tag v-else-if="isAttempted(row)" size="small" type="warning" effect="plain" round>
                尝试过
              </el-tag>
              <span v-else class="oj-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="题号" prop="problemKey" width="110" />
          <el-table-column label="标题" min-width="260">
            <template #default="{ row }">
              <span class="problem-title">{{ row.title }}</span>
            </template>
          </el-table-column>
          <el-table-column label="难度" width="90" align="center">
            <template #default="{ row }">
              <DifficultyTag :difficulty="row.difficulty" />
            </template>
          </el-table-column>
          <el-table-column label="通过率" width="100" align="center">
            <template #default="{ row }">
              <span class="oj-muted">{{ formatRate(row.acceptCount, row.submitCount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="提交数" prop="submitCount" width="90" align="center" />
          <template #empty>
            <el-empty description="暂无题目" />
          </template>
        </el-table>

        <div class="pager">
          <!-- 每页条数单独用下拉控制，分页器只保留基础布局，避免带 sizes 布局在生产构建下不渲染 -->
          <el-select v-model="query.pageSize" class="page-size" @change="load(1)">
            <el-option :value="10" label="10 条/页" />
            <el-option :value="20" label="20 条/页" />
            <el-option :value="50" label="50 条/页" />
          </el-select>
          <el-pagination
            v-model:current-page="query.pageNum"
            :page-size="query.pageSize"
            :total="total"
            layout="total, prev, pager, next"
            background
            @current-change="load()"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, CircleCheckFilled } from '@element-plus/icons-vue'
import { pageProblems } from '@/api/modules/problem'
import { getUserProblemStatus } from '@/api/modules/judge'
import { useUserStore } from '@/stores/user'
import { DIFFICULTY } from '@/utils/oj'
import { formatRate } from '@/utils/format'
import DifficultyTag from '@/components/DifficultyTag.vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)

// 当前用户做题状态（ID 为字符串，雪花 ID 不能转 Number）
const solvedIds = ref(new Set())
const attemptedIds = ref(new Set())
const isSolved = (row) => solvedIds.value.has(String(row.id))
const isAttempted = (row) => attemptedIds.value.has(String(row.id))

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  difficulty: null
})

async function load(resetPage) {
  if (resetPage) query.pageNum = 1
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.keyword) params.keyword = query.keyword
    if (query.difficulty !== null && query.difficulty !== undefined && query.difficulty !== '') {
      params.difficulty = query.difficulty
    }
    const data = await pageProblems(params)
    list.value = data.records || []
    // 后端 Long 会序列化成字符串，el-pagination 的 total 必须是 number
    total.value = Number(data.total) || 0
  } finally {
    loading.value = false
  }
}

function goDetail(row) {
  router.push(`/problem/${row.id}`)
}

/**
 * 加载当前用户做题状态（未登录跳过；失败静默，不影响题库浏览）
 */
async function loadStatus() {
  if (!userStore.isLogin) {
    return
  }
  try {
    const data = await getUserProblemStatus()
    solvedIds.value = new Set((data.solvedIds || []).map(String))
    attemptedIds.value = new Set((data.attemptedIds || []).map(String))
  } catch (e) {
    // 状态标记属于增强信息，加载失败时保持默认「—」
  }
}

onMounted(() => {
  load()
  loadStatus()
})
</script>

<style scoped>
.filters {
  display: flex;
  gap: 12px;
}

.table-card {
  padding: 8px 12px 16px;
}

.problem-title {
  color: var(--oj-text-primary);
  font-weight: 500;
}

.el-table :deep(tbody tr) {
  cursor: pointer;
}

.pager {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.page-size {
  width: 110px;
}
</style>
