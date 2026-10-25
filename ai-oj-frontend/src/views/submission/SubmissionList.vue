<template>
  <div class="oj-page">
    <div class="oj-container">
      <div class="oj-page-header">
        <h2 class="oj-page-title">我的提交记录</h2>
      </div>

      <div class="oj-card table-card">
        <SubmissionFilter @search="onFilterSearch" />
        <el-table
          v-loading="loading"
          :data="list"
          size="small"
          row-class-name="clickable-row"
          @row-click="openDetail"
        >
          <!-- 提交 ID 是 36 位 UUID，列宽留足并禁止换行，避免折成两行把行高撑高 -->
          <el-table-column label="提交 ID" prop="submitId" width="290">
            <template #default="{ row }">
              <span class="oj-muted mono">{{ row.submitId || row.id }}</span>
            </template>
          </el-table-column>
          <el-table-column label="题目" min-width="200">
            <template #default="{ row }">
              <router-link class="link" :to="`/problem/${row.problemId}`">
                {{ row.problemKey ? row.problemKey + ' ' : '' }}{{ row.problemTitle || '题目 #' + row.problemId }}
              </router-link>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <JudgeStatusTag :status="row.status" />
            </template>
          </el-table-column>
          <el-table-column label="得分" prop="score" width="70" align="center" />
          <el-table-column label="语言" width="100">
            <template #default="{ row }">{{ languageOf(row.language).label }}</template>
          </el-table-column>
          <el-table-column label="耗时 / 内存" width="150">
            <template #default="{ row }">
              <span class="oj-muted">
                {{ row.timeUsed != null ? row.timeUsed + ' ms' : '-' }} /
                {{ formatMemory(row.memoryUsed) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="170">
            <template #default="{ row }">
              <span class="oj-muted">{{ formatTime(row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无提交记录" />
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

    <!-- 提交详情抽屉：展示判题结果、编译错误、用例明细与源代码 -->
    <el-drawer v-model="detailVisible" title="提交详情" size="640px">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="提交 ID">
              <span class="mono">{{ detail.submitId }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <JudgeStatusTag :status="detail.status" />
            </el-descriptions-item>
            <el-descriptions-item label="语言">{{ languageOf(detail.language).label }}</el-descriptions-item>
            <el-descriptions-item label="得分">{{ detail.score ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="耗时">
              {{ detail.timeUsed != null ? detail.timeUsed + ' ms' : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="内存">{{ formatMemory(detail.memoryUsed) }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ formatTime(detail.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="判题时间">{{ formatTime(detail.judgeTime) }}</el-descriptions-item>
          </el-descriptions>

          <pre v-if="detail.errorMessage" class="compile-msg">{{ detail.errorMessage }}</pre>

          <template v-if="detail.cases?.length">
            <h4 class="section-title">用例结果</h4>
            <el-table :data="detail.cases" size="small">
              <el-table-column label="#" prop="seq" width="50" align="center" />
              <el-table-column label="结果" width="110">
                <template #default="{ row }">
                  <JudgeStatusTag :status="row.status" />
                </template>
              </el-table-column>
              <el-table-column label="耗时" width="90" align="center">
                <template #default="{ row }">
                  {{ row.timeUsed != null ? row.timeUsed + ' ms' : '-' }}
                </template>
              </el-table-column>
              <el-table-column label="内存" width="100" align="center">
                <template #default="{ row }">{{ formatMemory(row.memoryUsed) }}</template>
              </el-table-column>
            </el-table>
          </template>

          <h4 class="section-title">提交代码</h4>
          <pre class="code-block">{{ detail.code }}</pre>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { pageSubmissions, getSubmission } from '@/api/modules/judge'
import { languageOf } from '@/utils/oj'
import { formatTime, formatMemory } from '@/utils/format'
import JudgeStatusTag from '@/components/JudgeStatusTag.vue'
import SubmissionFilter from '@/components/SubmissionFilter.vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const query = reactive({
  pageNum: 1,
  pageSize: 10
})

/** 筛选条件（状态 / 语言 / 时间范围 / 关键字），由 SubmissionFilter 组件回传 */
const filters = ref({})

/** 打开提交详情（按 submitId 查询，含用例明细与编译错误） */
async function openDetail(row) {
  if (!row?.submitId) {
    return
  }
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getSubmission(row.submitId)
  } finally {
    detailLoading.value = false
  }
}

async function load(resetPage) {
  if (resetPage) query.pageNum = 1
  loading.value = true
  try {
    // 后端 /submission/page 固定查询当前登录用户的提交，无需 mine 参数
    const data = await pageSubmissions({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      ...filters.value
    })
    list.value = data.records || []
    // 后端 Long 会序列化成字符串，el-pagination 的 total 必须是 number
    total.value = Number(data.total) || 0
  } finally {
    loading.value = false
  }
}

/** 筛选条件变化：回到第一页重新查询 */
function onFilterSearch(params) {
  filters.value = params
  load(1)
}

onMounted(() => load())
</script>

<style scoped>
.table-card {
  padding: 8px 12px 16px;
}

.link {
  color: var(--oj-text-primary);
  font-weight: 500;
}

.link:hover {
  color: var(--oj-primary);
}

.mono {
  font-family: Consolas, Menlo, monospace;
  font-size: 12px;
  white-space: nowrap;
}

/* 行内容紧凑一些，避免表格显得空旷 */
.table-card :deep(.el-table) {
  font-size: 13px;
}

.table-card :deep(.el-table .cell) {
  line-height: 1.4;
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

.clickable-row {
  cursor: pointer;
}

.detail-body {
  min-height: 200px;
}

.section-title {
  margin: 20px 0 10px;
  font-size: 15px;
}

.compile-msg {
  margin-top: 16px;
  padding: 12px 14px;
  border-radius: 6px;
  background: var(--oj-bg-page);
  border-left: 3px solid var(--oj-danger, #f56c6c);
  color: var(--oj-danger, #f56c6c);
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.code-block {
  margin-top: 8px;
  padding: 12px 14px;
  border-radius: 6px;
  background: var(--oj-bg-page);
  border: 1px solid var(--oj-border);
  font-family: Consolas, Menlo, monospace;
  font-size: 12px;
  line-height: 1.6;
  max-height: 420px;
  overflow: auto;
  white-space: pre;
}
</style>
