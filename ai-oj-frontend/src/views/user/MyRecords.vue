<template>
  <div class="oj-page">
    <div class="oj-container">
      <div class="oj-page-header">
        <h2 class="oj-page-title">我的记录</h2>
        <el-button text @click="router.push('/user/profile')">
          <el-icon><ArrowLeft /></el-icon>返回个人中心
        </el-button>
      </div>

      <div v-loading="loading" class="oj-card records-card">
        <el-tabs v-model="activeTab">
          <el-tab-pane name="contest">
            <template #label>我的比赛（{{ myContests.length }}）</template>
            <div class="toolbar">
              <el-input
                v-model="contestKeyword"
                class="search-input"
                clearable
                placeholder="搜索比赛名称 / 简介"
                :prefix-icon="Search"
              />
              <span class="toolbar-tip">共 {{ filteredContests.length }} 场</span>
            </div>
            <el-table :data="pagedContests" size="small">
              <el-table-column label="比赛" min-width="220">
                <template #default="{ row }">
                  <div class="contest-cell">
                    <div class="contest-main">
                      <router-link class="list-link" :to="`/contest/${row.id}`">{{ row.title }}</router-link>
                      <span class="contest-desc">{{ row.description || '暂无简介' }}</span>
                    </div>
                    <el-tag size="small" effect="plain" round>{{ row.type === 1 ? 'OI' : 'ACM' }}</el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="contestStatusType(row.status)" effect="plain" round>
                    {{ contestStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="报名人数" width="90" align="center">
                <template #default="{ row }">{{ row.registerCount ?? 0 }}</template>
              </el-table-column>
              <el-table-column label="开始时间" width="150" align="center">
                <template #default="{ row }">
                  <span class="oj-muted">{{ formatTime(row.startTime, 'YYYY-MM-DD HH:mm') }}</span>
                </template>
              </el-table-column>
              <el-table-column label="结束时间" width="150" align="center">
                <template #default="{ row }">
                  <span class="oj-muted">{{ formatTime(row.endTime, 'YYYY-MM-DD HH:mm') }}</span>
                </template>
              </el-table-column>
              <template #empty>
                <el-empty :description="contestKeyword ? '没有匹配的比赛' : '还没有报名过比赛'" :image-size="60" />
              </template>
            </el-table>
            <div v-if="filteredContests.length" class="pager">
              <el-pagination
                v-model:current-page="contestPage"
                :page-size="CONTEST_PAGE_SIZE"
                :total="filteredContests.length"
                layout="prev, pager, next"
                background
              />
            </div>
          </el-tab-pane>

          <el-tab-pane name="solved">
            <template #label>我的通过（{{ solvedCount }}）</template>
            <div class="toolbar">
              <el-input
                v-model="solvedKeyword"
                class="search-input"
                clearable
                placeholder="搜索题号 / 题目名称"
                :prefix-icon="Search"
              />
              <span class="toolbar-tip">共 {{ filteredSolved.length }} 题</span>
            </div>
            <div v-if="filteredSolved.length" class="solved-grid">
              <router-link
                v-for="item in pagedSolved"
                :key="item.problemId"
                class="solved-item"
                :to="problemLink(item)"
              >
                <el-tag size="small" effect="plain" class="solved-key">
                  {{ item.problemKey || '#' + item.problemId }}
                </el-tag>
                <span class="solved-title">{{ item.problemTitle || '题目 #' + item.problemId }}</span>
                <el-tag v-if="isContestProblem(item)" size="small" type="warning" effect="plain" round>
                  比赛
                </el-tag>
              </router-link>
            </div>
            <el-empty
              v-else
              :description="solvedKeyword ? '没有匹配的题目' : '还没有通过的题目'"
              :image-size="60"
            />
            <div v-if="filteredSolved.length" class="pager">
              <el-pagination
                v-model:current-page="solvedPage"
                :page-size="SOLVED_PAGE_SIZE"
                :total="filteredSolved.length"
                layout="prev, pager, next"
                background
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Search } from '@element-plus/icons-vue'
import { pageSubmissions } from '@/api/modules/judge'
import { getMyContests } from '@/api/modules/contest'
import { formatTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()

/** 提交记录分页拉取：后端单页上限 500，最多拉 10 页 */
const SUBMISSION_PAGE_SIZE = 500
const SUBMISSION_MAX_PAGES = 10
/** 列表页大小：比赛 10 条/页，通过题目 24 条/页（前端本地分页） */
const CONTEST_PAGE_SIZE = 10
const SOLVED_PAGE_SIZE = 24

const loading = ref(false)
const myContests = ref([])
const solvedProblems = ref([])
const solvedCount = computed(() => solvedProblems.value.length)

/** 支持通过 ?tab=solved 直接打开「我的通过」 */
const activeTab = ref(route.query.tab === 'solved' ? 'solved' : 'contest')

/* ---------- 搜索 + 本地分页 ---------- */
const contestKeyword = ref('')
const contestPage = ref(1)
const solvedKeyword = ref('')
const solvedPage = ref(1)

const filteredContests = computed(() => {
  const keyword = contestKeyword.value.trim().toLowerCase()
  if (!keyword) return myContests.value
  return myContests.value.filter((item) =>
    `${item.title || ''} ${item.description || ''}`.toLowerCase().includes(keyword)
  )
})

const pagedContests = computed(() => {
  const start = (contestPage.value - 1) * CONTEST_PAGE_SIZE
  return filteredContests.value.slice(start, start + CONTEST_PAGE_SIZE)
})

const filteredSolved = computed(() => {
  const keyword = solvedKeyword.value.trim().toLowerCase()
  if (!keyword) return solvedProblems.value
  return solvedProblems.value.filter((item) =>
    `${item.problemKey || ''} ${item.problemTitle || ''}`.toLowerCase().includes(keyword)
  )
})

const pagedSolved = computed(() => {
  const start = (solvedPage.value - 1) * SOLVED_PAGE_SIZE
  return filteredSolved.value.slice(start, start + SOLVED_PAGE_SIZE)
})

// 搜索条件变化回到第一页，避免落在不存在的页码上
watch(contestKeyword, () => {
  contestPage.value = 1
})
watch(solvedKeyword, () => {
  solvedPage.value = 1
})

const CONTEST_STATUS_TEXT = {
  SCHEDULED: '未开始',
  RUNNING: '进行中',
  ENDED: '已结束',
  CANCELED: '已取消'
}

function contestStatusText(status) {
  return CONTEST_STATUS_TEXT[status] || status || '-'
}

function contestStatusType(status) {
  if (status === 'RUNNING') return 'success'
  if (status === 'SCHEDULED') return 'warning'
  return 'info'
}

/** 是否为比赛题目（contestId 为空或 0 视为普通题目） */
function isContestProblem(item) {
  return item.contestId != null && String(item.contestId) !== '0'
}

/** 题目跳转：比赛题目回比赛做题页，普通题目回题库详情 */
function problemLink(item) {
  return isContestProblem(item)
    ? `/contest/${item.contestId}/problem/${item.problemId}`
    : `/problem/${item.problemId}`
}

/** 分页拉取全部通过记录（后端单页上限 500） */
async function fetchAcceptedSubmissions() {
  const list = []
  for (let pageNum = 1; pageNum <= SUBMISSION_MAX_PAGES; pageNum += 1) {
    const data = await pageSubmissions({ status: 3, pageNum, pageSize: SUBMISSION_PAGE_SIZE })
    const records = data?.records || []
    list.push(...records)
    const total = Number(data?.total || 0)
    if (!records.length || list.length >= total) break
  }
  return list
}

/** 通过记录去重：同一题多次 AC 只保留一条，优先保留普通题目记录（链接更通用） */
function pickSolved(list) {
  const map = new Map()
  for (const item of list) {
    const key = String(item.problemId)
    const exist = map.get(key)
    if (!exist || (isContestProblem(exist) && !isContestProblem(item))) {
      map.set(key, item)
    }
  }
  return [...map.values()]
}

async function load() {
  loading.value = true
  try {
    const [contestResult, acceptedResult] = await Promise.allSettled([
      getMyContests(),
      fetchAcceptedSubmissions()
    ])
    if (contestResult.status === 'fulfilled') {
      myContests.value = contestResult.value || []
    }
    if (acceptedResult.status === 'fulfilled') {
      solvedProblems.value = pickSolved(acceptedResult.value)
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.records-card {
  min-height: 320px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.search-input {
  width: 280px;
}

.toolbar-tip {
  font-size: 12px;
  color: var(--oj-text-secondary);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 720px) {
  .toolbar {
    flex-wrap: wrap;
  }

  .search-input {
    width: 100%;
  }
}

.contest-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.contest-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.list-link {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-weight: 500;
  color: var(--oj-text-primary);
}

.list-link:hover {
  color: var(--oj-primary);
}

.contest-desc {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 12px;
  color: var(--oj-text-secondary);
}

.solved-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 8px;
}

.solved-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border-radius: 6px;
  border: 1px solid var(--oj-border);
  color: var(--oj-text-primary);
  min-width: 0;
}

.solved-item:hover {
  background: var(--el-fill-color-light, #f5f7fa);
}

.solved-key {
  flex-shrink: 0;
}

.solved-title {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>
