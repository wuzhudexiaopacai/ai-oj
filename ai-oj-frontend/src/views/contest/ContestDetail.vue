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
              <!-- 题目列表随比赛详情一起返回，无需额外请求 -->
              <el-table :data="contest.problems || []">
                <el-table-column label="题号" width="80" align="center">
                  <template #default="{ row }">{{ row.displayId || row.sort + 1 }}</template>
                </el-table-column>
                <el-table-column label="题目" min-width="260">
                  <template #default="{ row }">
                    <router-link class="link" :to="`/problem/${row.problemId}?contestId=${contest.id}`">
                      {{ row.title || '题目 #' + row.problemId }}
                    </router-link>
                  </template>
                </el-table-column>
                <el-table-column v-if="contest.type === 1" label="分值" width="90" align="center">
                  <template #default="{ row }">{{ row.score ?? '-' }}</template>
                </el-table-column>
                <template #empty>
                  <el-empty description="暂无题目" :image-size="60" />
                </template>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="排行榜" name="rank">
              <el-table v-loading="rankLoading" :data="rank">
                <el-table-column label="排名" prop="rank" width="80" align="center" />
                <el-table-column label="用户" prop="username" min-width="160" />
                <el-table-column label="总分" prop="score" width="100" align="center" />
                <el-table-column label="通过数" prop="acceptCount" width="100" align="center" />
                <el-table-column label="罚时" width="120" align="center">
                  <template #default="{ row }">
                    <span class="oj-muted">{{ row.penalty != null ? row.penalty + ' 分钟' : '-' }}</span>
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
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getContestDetail, registerContest, getContestRank } from '@/api/modules/contest'
import { formatTime } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import MarkdownView from '@/components/MarkdownView.vue'

const route = useRoute()
const userStore = useUserStore()
const contestId = route.params.id

const loading = ref(false)
const contest = ref(null)
const tab = ref('desc')
const registering = ref(false)
// 报名状态：优先取后端返回的 registered，报名成功后本地置为 true
const registered = ref(false)

const rank = ref([])
const rankLoading = ref(false)

async function load() {
  loading.value = true
  try {
    contest.value = await getContestDetail(contestId)
    registered.value = !!contest.value.registered
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

async function onRegister() {
  registering.value = true
  try {
    await registerContest(contestId)
    ElMessage.success('报名成功')
    registered.value = true
    contest.value.registerCount = (contest.value.registerCount || 0) + 1
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
</style>
