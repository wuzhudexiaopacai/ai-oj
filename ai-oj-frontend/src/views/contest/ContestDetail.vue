<template>
  <div class="oj-page">
    <div class="oj-container" v-loading="loading">
      <template v-if="contest">
        <div class="oj-card head-card">
          <div class="head-main">
            <h2 class="oj-page-title">{{ contest.title }}</h2>
            <div class="oj-muted head-meta">
              <span>{{ formatTime(contest.startTime) }} ~ {{ formatTime(contest.endTime) }}</span>
              <el-divider direction="vertical" />
              <span>{{ contest.registerCount ?? 0 }} 人已报名</span>
            </div>
          </div>
          <el-tag v-if="contest.auth === 2" type="warning" effect="plain" round>私有赛</el-tag>
          <el-tag v-if="registered" type="success" round>已报名</el-tag>
          <el-button
            v-else-if="userStore.isLogin"
            type="primary"
            round
            :loading="registering"
            @click="onRegister"
          >
            立即报名
          </el-button>
          <el-button v-else round @click="$router.push('/login')">登录后报名</el-button>
        </div>

        <div class="oj-card body-card">
          <el-tabs v-model="tab">
            <el-tab-pane label="比赛说明" name="desc">
              <MarkdownView :content="contest.description || '暂无说明'" />
            </el-tab-pane>

            <el-tab-pane label="题目列表" name="problems">
              <!-- 题目列表随比赛详情一起返回，此处前端分页展示 -->
              <el-table :data="pagedProblems">
                <el-table-column label="状态" width="92" align="center">
                  <template #default="{ row }">
                    <el-tooltip v-if="row.status === 'AC'" content="已通过" placement="top">
                      <el-icon color="var(--oj-success)" :size="18"><CircleCheckFilled /></el-icon>
                    </el-tooltip>
                    <el-tag v-else-if="row.status === 'ATTEMPTED'" size="small" type="warning" effect="plain" round>
                      尝试过
                    </el-tag>
                    <span v-else class="oj-muted">—</span>
                  </template>
                </el-table-column>
                <el-table-column label="题号" width="80" align="center">
                  <template #default="{ row }">{{ row.displayId || row.sort + 1 }}</template>
                </el-table-column>
                <el-table-column label="题目" min-width="260">
                  <template #default="{ row }">
                    <router-link class="link" :to="`/contest/${contest.id}/problem/${row.problemId}`">
                      {{ row.title || '题目 #' + row.problemId }}
                    </router-link>
                  </template>
                </el-table-column>
                <el-table-column v-if="contest.type === 1" label="分值" width="90" align="center">
                  <template #default="{ row }">{{ row.score ?? '-' }}</template>
                </el-table-column>
                <template #empty>
                  <el-empty :description="problemsHint" :image-size="60" />
                </template>
              </el-table>
              <!-- 有题目就展示分页器，避免题量不足一页时看起来像没有分页 -->
              <div v-if="problemTotal > 0" class="table-pager">
                <el-pagination
                  v-model:current-page="problemPage"
                  :page-size="problemPageSize"
                  :total="problemTotal"
                  layout="total, prev, pager, next"
                  background
                />
              </div>
            </el-tab-pane>

            <el-tab-pane label="排行榜" name="rank">
              <el-table v-loading="rankLoading" :data="rank">
                <el-table-column label="排名" prop="rank" width="80" align="center" />
                <el-table-column label="用户" min-width="220">
                  <template #default="{ row }">
                    <div class="rank-user">
                      <el-avatar :size="30" :src="row.avatar">
                        {{ (row.nickname || row.username || '?').charAt(0).toUpperCase() }}
                      </el-avatar>
                      <div class="rank-user-text">
                        <div class="rank-user-name">{{ row.nickname || row.username || '用户 #' + row.userId }}</div>
                        <!-- 简介过长时单行省略，hover 用 title 展示全文 -->
                        <div v-if="row.intro" class="rank-user-intro" :title="row.intro">{{ row.intro }}</div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <!-- OI 看总分；ACM 看通过数与罚时（后端 totalScore 在 ACM 下恒为 0，不展示避免误解） -->
                <el-table-column v-if="contest.type === 1" label="总分" prop="totalScore" width="100" align="center" />
                <el-table-column v-else label="通过数" prop="acceptCount" width="100" align="center" />
                <el-table-column v-if="contest.type !== 1" label="罚时" width="120" align="center">
                  <template #default="{ row }">
                    <span class="oj-muted">{{ formatPenalty(row.penaltySeconds) }}</span>
                  </template>
                </el-table-column>
                <template #empty>
                  <el-empty description="暂无榜单数据" :image-size="60" />
                </template>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>
      </template>
      <el-empty v-else-if="!loading" description="比赛不存在" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import { getContestDetail, registerContest, getContestRank } from '@/api/modules/contest'
import { formatTime } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import MarkdownView from '@/components/MarkdownView.vue'

const route = useRoute()
const userStore = useUserStore()
const contestId = route.params.id

const loading = ref(false)
const contest = ref(null)
// 支持从比赛做题页「返回比赛题目」直接落到题目列表页签
const tab = ref(route.query.tab === 'problems' ? 'problems' : 'desc')
const registering = ref(false)
// 报名状态：优先取后端返回的 registered，报名成功后本地置为 true
const registered = ref(false)

const rank = ref([])
const rankLoading = ref(false)

/** 题目列表前端分页：题目随比赛详情一次性返回 */
const problemPage = ref(1)
const problemPageSize = 10
const problemTotal = computed(() => (contest.value?.problems || []).length)
const pagedProblems = computed(() => {
  const all = contest.value?.problems || []
  const start = (problemPage.value - 1) * problemPageSize
  return all.slice(start, start + problemPageSize)
})

/** 题目列表为空时的提示：区分「未开赛」与「私有赛未报名」 */
const problemsHint = computed(() => {
  if (!contest.value) return '暂无题目'
  if (dayjs(contest.value.startTime).isAfter(dayjs())) return '比赛开始后可见题目'
  if (contest.value.auth === 2 && !registered.value) return '该比赛为私有赛，请输入密码报名后查看题目'
  return '暂无题目'
})

async function load() {
  loading.value = true
  try {
    contest.value = await getContestDetail(contestId)
    registered.value = !!contest.value.registered
    problemPage.value = 1
  } finally {
    loading.value = false
  }
}

async function loadRank() {
  rankLoading.value = true
  try {
    const data = await getContestRank(contestId, { pageNum: 1, pageSize: 100 })
    rank.value = data.records || data || []
  } finally {
    rankLoading.value = false
  }
}

/** 罚时（秒）→ 可读文本；未参赛/无 AC 显示 - */
function formatPenalty(seconds) {
  if (seconds == null || seconds <= 0) return seconds === 0 ? '0 分' : '-'
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return minutes > 0 ? `${minutes} 分 ${rest} 秒` : `${rest} 秒`
}

async function onRegister() {
  // 私有赛报名前需输入比赛密码
  let password
  if (contest.value.auth === 2) {
    try {
      const { value } = await ElMessageBox.prompt('该比赛为私有赛，请输入比赛密码后报名', '私有比赛', {
        inputType: 'password',
        inputPlaceholder: '请输入比赛密码',
        confirmButtonText: '报名',
        cancelButtonText: '取消',
        inputValidator: (v) => (v && v.trim() ? true : '请输入比赛密码')
      })
      password = value.trim()
    } catch (e) {
      // 用户取消输入密码，不发起报名
      return
    }
  }
  registering.value = true
  try {
    await registerContest(contestId, password)
    ElMessage.success('报名成功')
    registered.value = true
    contest.value.registerCount = (contest.value.registerCount || 0) + 1
    // 私有赛报名后后端才会返回题目列表，重新拉取详情
    if (contest.value.auth === 2) {
      await load()
    }
  } finally {
    registering.value = false
  }
}

watch(tab, (t) => {
  if (t === 'rank' && !rank.value.length) loadRank()
})

onMounted(load)
</script>

<style scoped>
.head-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.head-meta {
  font-size: 13px;
  margin-top: 6px;
}

.body-card {
  padding: 8px 20px 20px;
}

.link {
  color: var(--oj-text-primary);
  font-weight: 500;
}

.link:hover {
  color: var(--oj-primary);
}

.rank-user {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.rank-user-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.3;
}

.rank-user-name {
  font-weight: 500;
}

/* 简介超长时单行省略，避免撑高表格行 */
.rank-user-intro {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--oj-text-secondary, #909399);
}

.table-pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
