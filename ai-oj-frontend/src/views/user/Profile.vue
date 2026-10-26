<template>
  <div class="oj-page">
    <div class="oj-container">
      <div class="oj-page-header">
        <h2 class="oj-page-title">个人中心</h2>
      </div>

      <div class="profile-layout">
        <!-- 左侧：身份信息（窄栏，资料编辑移到独立页面） -->
        <aside class="oj-card side-card">
          <el-avatar :size="88" :src="userStore.userInfo?.avatar" class="side-avatar">
            {{ userStore.nickname.charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="side-name">{{ userStore.nickname }}</div>
          <div class="side-username">@{{ userStore.userInfo?.username || '-' }}</div>
          <p v-if="userStore.userInfo?.intro" class="side-intro">{{ userStore.userInfo.intro }}</p>
          <el-button class="side-edit" round @click="router.push('/user/settings')">编辑个人资料</el-button>

          <div class="side-stats">
            <div class="side-stat">
              <b>{{ solvedCount }}</b>
              <span>已通过</span>
            </div>
            <div class="side-stat">
              <b>{{ myContests.length }}</b>
              <span>参赛</span>
            </div>
            <div class="side-stat">
              <b>{{ heatTotal }}</b>
              <span>近一年提交</span>
            </div>
          </div>

          <ul class="side-meta">
            <li>
              <el-icon><Message /></el-icon>
              <span>{{ userStore.userInfo?.email || '未设置邮箱' }}</span>
            </li>
            <li v-if="userStore.userInfo?.createTime">
              <el-icon><Calendar /></el-icon>
              <span>加入于 {{ formatTime(userStore.userInfo.createTime, 'YYYY-MM-DD') }}</span>
            </li>
          </ul>
        </aside>

        <!-- 右侧：卡片主栏 -->
        <main class="profile-main">
          <!-- 做题概览 -->
          <div class="oj-card overview-card">
            <div class="donut" :style="donutStyle">
              <div class="donut-inner">
                <b>{{ solvedCount }}</b>
                <span>已通过</span>
              </div>
            </div>
            <div class="overview-info">
              <div class="overview-line">
                <span class="overview-label">尝试过的题目</span>
                <b>{{ attemptedCount }}</b>
              </div>
              <div class="overview-line">
                <span class="overview-label">近一年提交</span>
                <b>{{ heatTotal }}</b>
              </div>
              <div class="overview-line">
                <span class="overview-label">参加比赛</span>
                <b>{{ myContests.length }}</b>
              </div>
            </div>
          </div>

          <!-- 最近提交：只展示最近几条，更多进提交记录页 -->
          <div class="oj-card">
            <div class="card-head">
              <h3 class="section-title">最近提交</h3>
              <el-button size="small" text type="primary" @click="router.push('/submission')">
                查看全部<el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
            <div v-loading="statsLoading" class="recent-list">
              <router-link
                v-for="item in recentSubmissions"
                :key="item.id"
                class="recent-item"
                :to="problemLink(item)"
              >
                <JudgeStatusTag :status="item.status" />
                <span class="recent-title">
                  {{ item.problemKey ? item.problemKey + ' ' : '' }}{{ item.problemTitle || '题目 #' + item.problemId }}
                </span>
                <span class="recent-lang">{{ languageOf(item.language).label }}</span>
                <span class="recent-time">{{ formatFromNow(item.createTime) }}</span>
              </router-link>
              <el-empty
                v-if="!statsLoading && !recentSubmissions.length"
                description="还没有提交记录"
                :image-size="60"
              />
            </div>
          </div>

          <!-- 刷题热力图 -->
          <div class="oj-card heat-card">
            <div class="heat-head">
              <div class="heat-title">过去一年共提交 <b>{{ heatTotal }}</b> 次</div>
              <div class="heat-meta">
                <span>累计提交天数：{{ activeDays }}</span>
                <el-divider direction="vertical" />
                <span>连续提交：{{ streak }}</span>
              </div>
            </div>
            <div ref="heatmapEl" v-loading="statsLoading" class="heatmap-box"></div>
          </div>

          <div class="profile-lists">
            <div class="oj-card">
              <div class="list-head">
                <h3 class="section-title">我的比赛</h3>
                <div class="list-actions">
                  <span class="list-count">{{ myContests.length }} 场</span>
                  <el-button size="small" text type="primary" @click="router.push('/user/records?tab=contest')">
                    查看全部<el-icon><ArrowRight /></el-icon>
                  </el-button>
                </div>
              </div>
              <el-table v-loading="statsLoading" :data="myContests" size="small">
                <el-table-column label="比赛" min-width="160">
                  <template #default="{ row }">
                    <div class="contest-cell">
                      <div class="contest-main">
                        <router-link class="list-link" :to="`/contest/${row.id}`">{{ row.title }}</router-link>
                        <span class="contest-time">{{ formatTime(row.startTime, 'YYYY-MM-DD HH:mm') }}</span>
                      </div>
                      <el-tag size="small" effect="plain" round>{{ row.type === 1 ? 'OI' : 'ACM' }}</el-tag>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="84" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="contestStatusType(row.status)" effect="plain" round>
                      {{ contestStatusText(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <template #empty>
                  <el-empty description="还没有报名过比赛" :image-size="60" />
                </template>
              </el-table>
            </div>

            <div class="oj-card">
              <div class="list-head">
                <h3 class="section-title">我的通过</h3>
                <div class="list-actions">
                  <span class="list-count">已通过 {{ solvedCount }} 题</span>
                  <el-button size="small" text type="primary" @click="router.push('/user/records?tab=solved')">
                    查看全部<el-icon><ArrowRight /></el-icon>
                  </el-button>
                </div>
              </div>
              <div v-if="solvedProblems.length" class="passed-list">
                <router-link
                  v-for="item in solvedProblems"
                  :key="item.problemId"
                  class="passed-item"
                  :to="problemLink(item)"
                >
                  <el-tag size="small" effect="plain" class="passed-key">
                    {{ item.problemKey || '#' + item.problemId }}
                  </el-tag>
                  <span class="passed-title">{{ item.problemTitle || '题目 #' + item.problemId }}</span>
                  <el-tag v-if="isContestProblem(item)" size="small" type="warning" effect="plain" round>
                    比赛
                  </el-tag>
                </router-link>
              </div>
              <el-empty v-else-if="!statsLoading" description="还没有通过的题目" :image-size="60" />
            </div>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ArrowRight, Calendar, Message } from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import { CalendarComponent, TooltipComponent, VisualMapComponent } from 'echarts/components'
import { HeatmapChart } from 'echarts/charts'
import { CanvasRenderer } from 'echarts/renderers'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { pageSubmissions } from '@/api/modules/judge'
import { getMyContests } from '@/api/modules/contest'
import { languageOf } from '@/utils/oj'
import { formatTime, formatFromNow } from '@/utils/format'
import JudgeStatusTag from '@/components/JudgeStatusTag.vue'

echarts.use([CalendarComponent, HeatmapChart, TooltipComponent, VisualMapComponent, CanvasRenderer])

const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()

/** 最近提交展示条数 */
const RECENT_SIZE = 5
/** 提交记录分页拉取：后端单页上限 500，最多拉 10 页 */
const SUBMISSION_PAGE_SIZE = 500
const SUBMISSION_MAX_PAGES = 10

/* ---------- 做题统计 / 最近提交 / 热力图 ---------- */
const statsLoading = ref(false)
const myContests = ref([])
/** 已通过的题目（按 problemId 去重，包含比赛题目） */
const solvedProblems = ref([])
/** 尝试过的题目数（按 problemId 去重） */
const attemptedCount = ref(0)
/** 最近提交（只取前几条） */
const recentSubmissions = ref([])
/** 过去一年每天的提交次数：{ 'YYYY-MM-DD': count } */
const heatCounts = ref({})

const solvedCount = computed(() => solvedProblems.value.length)
const heatTotal = computed(() =>
  Object.values(heatCounts.value).reduce((sum, count) => sum + count, 0)
)
const activeDays = computed(() => Object.keys(heatCounts.value).length)

/** 环形进度：已通过题数占尝试题数比例 */
const solvedPercent = computed(() =>
  attemptedCount.value ? Math.round((solvedCount.value / attemptedCount.value) * 100) : 0
)
const donutStyle = computed(() => ({
  background: `conic-gradient(var(--oj-primary) 0 ${solvedPercent.value}%, var(--el-fill-color-light, #f0f2f5) ${solvedPercent.value}% 100%)`
}))

/** 连续提交天数：今天有提交从今天算起，否则从昨天算起 */
const streak = computed(() => {
  const counts = heatCounts.value
  let cursor = dayjs()
  if (!counts[cursor.format('YYYY-MM-DD')]) {
    cursor = cursor.subtract(1, 'day')
  }
  let days = 0
  while (counts[cursor.format('YYYY-MM-DD')]) {
    days += 1
    cursor = cursor.subtract(1, 'day')
  }
  return days
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

/** 分页拉取全部提交（后端单页上限 500） */
async function fetchAllSubmissions(params) {
  const list = []
  for (let pageNum = 1; pageNum <= SUBMISSION_MAX_PAGES; pageNum += 1) {
    const data = await pageSubmissions({ ...params, pageNum, pageSize: SUBMISSION_PAGE_SIZE })
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

/** 按天统计提交次数 */
function countByDay(list) {
  const counts = {}
  for (const item of list) {
    if (!item.createTime) continue
    const date = dayjs(item.createTime).format('YYYY-MM-DD')
    counts[date] = (counts[date] || 0) + 1
  }
  return counts
}

async function loadStats() {
  statsLoading.value = true
  try {
    const [contestResult, submissionResult, recentResult] = await Promise.allSettled([
      getMyContests(),
      fetchAllSubmissions({}),
      pageSubmissions({ pageNum: 1, pageSize: RECENT_SIZE })
    ])
    if (contestResult.status === 'fulfilled') {
      myContests.value = contestResult.value || []
    }
    if (submissionResult.status === 'fulfilled') {
      const list = submissionResult.value
      solvedProblems.value = pickSolved(list.filter((item) => item.status === 3))
      attemptedCount.value = new Set(list.map((item) => String(item.problemId))).size
      const yearStart = dayjs().subtract(1, 'year').startOf('day')
      heatCounts.value = countByDay(
        list.filter((item) => item.createTime && !dayjs(item.createTime).isBefore(yearStart))
      )
    }
    if (recentResult.status === 'fulfilled') {
      recentSubmissions.value = recentResult.value?.records || []
    }
  } finally {
    statsLoading.value = false
    renderHeatmap()
  }
}

/* ---------- 热力图（ECharts calendar） ---------- */
const heatmapEl = ref(null)
let heatChart = null
let heatObserver = null

/** 生成整年逐日数据（含 0，保证空格子也上色） */
function calendarData() {
  const counts = heatCounts.value
  const start = dayjs().subtract(1, 'year').startOf('day')
  const end = dayjs().startOf('day')
  const data = []
  let cursor = start
  while (!cursor.isAfter(end)) {
    const date = cursor.format('YYYY-MM-DD')
    data.push([date, counts[date] || 0])
    cursor = cursor.add(1, 'day')
  }
  return data
}

function buildHeatOption() {
  const dark = themeStore.isDark
  const cardBg = dark ? '#282828' : '#ffffff'
  const textColor = dark ? '#e5e7eb' : '#303133'
  const axisColor = '#8c8c8c'
  const levels = dark
    ? ['#2d333b', '#0e4429', '#006d32', '#26a641', '#39d353']
    : ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39']
  const maxCount = Math.max(1, ...Object.values(heatCounts.value))

  return {
    backgroundColor: 'transparent',
    tooltip: {
      backgroundColor: dark ? 'rgba(37, 41, 46, 0.96)' : 'rgba(255, 255, 255, 0.97)',
      borderColor: dark ? '#3d3d3d' : '#e5e6eb',
      textStyle: { color: textColor, fontSize: 12 },
      formatter(params) {
        const value = params.value || []
        return `${value[0]}：${value[1] || 0} 次提交`
      }
    },
    visualMap: {
      min: 0,
      max: maxCount,
      orient: 'horizontal',
      right: 4,
      bottom: 0,
      itemWidth: 10,
      itemHeight: 10,
      text: ['多', '少'],
      textStyle: { color: axisColor, fontSize: 10 },
      inRange: { color: levels },
      calculable: false
    },
    calendar: {
      top: 22,
      left: 28,
      right: 4,
      bottom: 24,
      range: [
        dayjs().subtract(1, 'year').format('YYYY-MM-DD'),
        dayjs().format('YYYY-MM-DD')
      ],
      orient: 'horizontal',
      cellSize: ['auto', 11],
      splitLine: { show: false },
      itemStyle: { color: 'transparent', borderWidth: 0 },
      dayLabel: {
        firstDay: 1,
        nameMap: ['日', '一', '二', '三', '四', '五', '六'],
        color: axisColor,
        fontSize: 10,
        margin: 4
      },
      monthLabel: {
        nameMap: 'ZH',
        color: axisColor,
        fontSize: 10,
        margin: 4
      },
      yearLabel: { show: false }
    },
    series: [
      {
        type: 'heatmap',
        coordinateSystem: 'calendar',
        data: calendarData(),
        itemStyle: { borderWidth: 2, borderColor: cardBg, borderRadius: 2 },
        emphasis: { itemStyle: { borderColor: '#f59e0b' } }
      }
    ]
  }
}

function renderHeatmap() {
  nextTick(() => {
    if (!heatmapEl.value) return
    if (!heatChart) {
      heatChart = echarts.init(heatmapEl.value)
      heatObserver = new ResizeObserver(() => heatChart && heatChart.resize())
      heatObserver.observe(heatmapEl.value)
    }
    heatChart.setOption(buildHeatOption(), true)
    heatChart.resize()
  })
}

watch(() => themeStore.isDark, renderHeatmap)

onMounted(() => {
  userStore.fetchUserInfo().catch(() => {})
  loadStats()
})

onBeforeUnmount(() => {
  if (heatObserver) {
    heatObserver.disconnect()
    heatObserver = null
  }
  if (heatChart) {
    heatChart.dispose()
    heatChart = null
  }
})
</script>

<style scoped>
.profile-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

@media (max-width: 900px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}

/* ---------- 左侧身份卡片 ---------- */
.side-card {
  text-align: center;
  position: sticky;
  top: 72px;
}

@media (max-width: 900px) {
  .side-card {
    position: static;
  }
}

.side-avatar {
  border: 1px solid var(--oj-border);
}

.side-name {
  margin-top: 10px;
  font-size: 18px;
  font-weight: 600;
}

.side-username {
  margin-top: 4px;
  font-size: 13px;
  color: var(--oj-text-secondary);
}

.side-intro {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--oj-text-regular);
  word-break: break-word;
}

.side-edit {
  margin-top: 14px;
  width: 100%;
}

.side-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-top: 16px;
  padding: 12px 0;
  border-top: 1px solid var(--oj-divider);
  border-bottom: 1px solid var(--oj-divider);
}

.side-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.side-stat b {
  font-size: 16px;
}

.side-stat span {
  font-size: 12px;
  color: var(--oj-text-secondary);
}

.side-meta {
  list-style: none;
  margin: 14px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-align: left;
  font-size: 12px;
  color: var(--oj-text-secondary);
}

.side-meta li {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.side-meta li span {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* ---------- 右侧卡片主栏 ---------- */
.profile-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.overview-card {
  display: flex;
  align-items: center;
  gap: 32px;
}

.donut {
  width: 112px;
  height: 112px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.donut-inner {
  width: 84px;
  height: 84px;
  border-radius: 50%;
  background: var(--oj-bg-raised);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.donut-inner b {
  font-size: 20px;
  line-height: 1.2;
}

.donut-inner span {
  font-size: 12px;
  color: var(--oj-text-secondary);
}

.overview-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.overview-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}

.overview-label {
  color: var(--oj-text-secondary);
}

.overview-line b {
  font-size: 15px;
}

/* ---------- 最近提交 ---------- */
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.card-head .section-title {
  margin: 0;
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-height: 60px;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 8px;
  border-radius: 6px;
  color: var(--oj-text-primary);
}

.recent-item:hover {
  background: var(--el-fill-color-light, #f5f7fa);
}

.recent-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-weight: 500;
}

.recent-lang {
  width: 84px;
  font-size: 12px;
  color: var(--oj-text-secondary);
}

.recent-time {
  width: 84px;
  text-align: right;
  font-size: 12px;
  color: var(--oj-text-secondary);
}

/* ---------- 提交热力图 ---------- */
.heat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.heat-title {
  font-size: 14px;
}

.heat-title b {
  font-size: 18px;
  margin: 0 2px;
}

.heat-meta {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: var(--oj-text-secondary, #8c8c8c);
}

.heatmap-box {
  width: 100%;
  height: 140px;
  margin-top: 4px;
}

/* ---------- 我的比赛 / 我的通过 ---------- */
.profile-lists {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 1100px) {
  .profile-lists {
    grid-template-columns: 1fr;
  }
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.list-head .section-title {
  margin: 0;
}

.list-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.list-actions :deep(.el-button) {
  padding: 4px;
}

.section-title {
  margin: 0;
  font-size: 16px;
}

.list-count {
  font-size: 12px;
  color: var(--oj-text-secondary, #8c8c8c);
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

.contest-time {
  font-size: 12px;
  color: var(--oj-text-secondary);
}

.passed-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 320px;
  overflow-y: auto;
}

.passed-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 8px;
  border-radius: 6px;
  color: var(--oj-text-primary);
}

.passed-item:hover {
  background: var(--el-fill-color-light, #f5f7fa);
}

.passed-title {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

@media (max-width: 720px) {
  .overview-card {
    gap: 20px;
  }

  .donut {
    width: 96px;
    height: 96px;
  }

  .donut-inner {
    width: 72px;
    height: 72px;
  }

  .recent-lang {
    display: none;
  }
}
</style>
