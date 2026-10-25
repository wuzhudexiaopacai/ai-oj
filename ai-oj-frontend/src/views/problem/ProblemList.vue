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
          <el-table-column label="状态" width="70" align="center">
            <template #default="{ row }">
              <el-icon v-if="row.solved" color="var(--oj-success)"><CircleCheckFilled /></el-icon>
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
          <el-pagination
            v-model:current-page="query.pageNum"
            v-model:page-size="query.pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="load()"
            @size-change="load(1)"
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
import { DIFFICULTY } from '@/utils/oj'
import { formatRate } from '@/utils/format'
import DifficultyTag from '@/components/DifficultyTag.vue'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 20,
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
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function goDetail(row) {
  router.push(`/problem/${row.id}`)
}

onMounted(() => load())
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
  margin-top: 16px;
}
</style>
