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
              <div class="rank-toolbar">
                <el-radio-group v-model="rankView" size="small">
                  <el-radio-button value="bar">柱状图</el-radio-button>
                  <el-radio-button value="line">折线图</el-radio-button>
                  <el-radio-button value="combo">组合图</el-radio-button>
                  <el-radio-button value="table">排名</el-radio-button>
                </el-radio-group>
                <span class="oj-muted rank-metric">
                  {{ contest.type === 1 ? '按总分排序' : '按通过数、罚时排序' }}
                  <template v-if="hasPassed"> · 橙色为你</template>
                </span>
              </div>

              <!-- 图表视图：ECharts 绘制，仅展示前 N 名，避免整页下拉 -->
              <div v-if="rankView !== 'table'" v-loading="rankLoading" class="rank-chart">
                <el-empty v-if="!rank.length" description="暂无榜单数据" :image-size="60" />
                <div v-else ref="chartEl" class="rank-chart-box"></div>
              </div>

              <!-- 排名视图：表格 + 前端分页，自己所在行高亮 -->
              <template v-else>
                <el-table v-loading="rankLoading" :data="pagedRank" :row-class-name="rankRowClass">
                  <el-table-column label="排名" prop="rank" width="80" align="center" />
                  <el-table-column label="用户" min-width="220">
                    <template #default="{ row }">
                      <div class="rank-user">
                        <el-avatar :size="30" :src="row.avatar">
                          {{ (row.nickname || row.username || '?').charAt(0).toUpperCase() }}
                        </el-avatar>
                        <div class="rank-user-text">
                          <div class="rank-user-name">
                            {{ row.nickname || row.username || '用户 #' + row.userId }}
                            <el-tag v-if="isMe(row)" size="small" type="warning" effect="plain" round>我</el-tag>
                          </div>
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
                <div v-if="rank.length > rankPageSize" class="table-pager">
                  <el-pagination
                    v-model:current-page="rankPage"
                    :page-size="rankPageSize"
                    :total="rank.length"
                    layout="total, prev, pager, next"
                    background
                  />
                </div>
              </template>
            </el-tab-pane>
          </el-tabs>
        </div>
      </template>
      <el-empty v-else-if="!loading" description="比赛不存在" />
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getContestDetail, registerContest, getContestRank } from '@/api/modules/contest'
import { formatTime } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import MarkdownView from '@/components/MarkdownView.vue'

echarts.use([BarChart, LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const route = useRoute()
const userStore = useUserStore()
const themeStore = useThemeStore()
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

/** 排行榜视图：柱状图 / 折线图 / 组合图 / 排名表 */
const rankView = ref('combo')
/** 图表展示的前 N 名 */
const CHART_TOP_N = 10
/** 排名表前端分页 */
const rankPage = ref(1)
const rankPageSize = 10

/** 当前登录用户 ID（雪花 ID 以字符串返回，统一转字符串比较） */
const myUserId = computed(() => {
  const id = userStore.userInfo?.id
  return id == null ? '' : String(id)
})

/** 当前用户在榜单中的记录（未参赛则不存在） */
const myRankRow = computed(() =>
  myUserId.value ? rank.value.find((row) => String(row.userId) === myUserId.value) : null
)

/** 有通过记录才在榜单/图表中标记自己 */
const hasPassed = computed(() => !!myRankRow.value && Number(myRankRow.value.acceptCount) > 0)

function isMe(row) {
  return hasPassed.value && String(row.userId) === myUserId.value
}

/** 图表数据：前 N 名；自己有通过记录但不在前 N 时，追加自己的真实名次 */
const chartItems = computed(() => {
  const top = rank.value.slice(0, CHART_TOP_N)
  const mine = myRankRow.value
  if (!hasPassed.value || !mine || mine.rank <= CHART_TOP_N) {
    return top
  }
  return [...top, { ...mine, divider: true }]
})

/** 图表主指标：OI 看总分，ACM 看通过数 */
function metricOf(row) {
  return Number(contest.value?.type === 1 ? row.totalScore : row.acceptCount) || 0
}

/** 组合图副指标：OI 看通过题数，ACM 看罚时分钟（与主指标区分开才有意义） */
function secondaryOf(row) {
  if (contest.value?.type === 1) {
    return Number(row.acceptCount) || 0
  }
  return Math.round((Number(row.penaltySeconds) || 0) / 60)
}

/** 主/副指标轴名称 */
const primaryName = computed(() => (contest.value?.type === 1 ? '总分' : '通过数'))
const secondaryName = computed(() => (contest.value?.type === 1 ? '通过题数' : '罚时（分钟）'))

/** 排名表当前页数据 */
const pagedRank = computed(() => {
  const start = (rankPage.value - 1) * rankPageSize
  return rank.value.slice(start, start + rankPageSize)
})

function rankRowClass({ row }) {
  return isMe(row) ? 'rank-me-row' : ''
}

/* ---------- ECharts 图表 ---------- */
const chartEl = ref(null)
let chartInstance = null
let resizeObserver = null

/** 柱体渐变：自己用橙色，其他人用蓝色 */
function barGradient(row) {
  return {
    type: 'linear',
    x: 0,
    y: 0,
    x2: 0,
    y2: 1,
    colorStops: isMe(row)
      ? [
          { offset: 0, color: '#fbbf24' },
          { offset: 1, color: '#f59e0b' }
        ]
      : [
          { offset: 0, color: '#7fc4ff' },
          { offset: 1, color: '#4f8cff' }
        ]
  }
}

/** 按当前视图/主题生成 ECharts 配置 */
function buildChartOption() {
  const dark = themeStore.isDark
  const items = chartItems.value
  const axisColor = dark ? '#909399' : '#909399'
  const textColor = dark ? '#e5e7eb' : '#303133'
  const splitColor = dark ? 'rgba(255, 255, 255, 0.08)' : '#f0f2f5'
  const isCombo = rankView.value === 'combo'

  // 名次作类目轴，比用户名更好压缩，窄屏也不会挤成一团
  const labels = items.map((row) => String(row.rank))

  const axisLabel = { color: axisColor, fontSize: 11, hideOverlap: true }
  const valueAxis = {
    type: 'value',
    minInterval: 1,
    axisLabel,
    splitLine: { lineStyle: { color: splitColor } },
    axisLine: { show: false },
    axisTick: { show: false }
  }

  const barSeries = {
    name: primaryName.value,
    type: 'bar',
    barMaxWidth: 30,
    data: items.map((row) => ({ value: metricOf(row), itemStyle: { color: barGradient(row) } })),
    emphasis: { focus: 'series' }
  }

  const lineSeries = {
    name: isCombo ? secondaryName.value : primaryName.value,
    type: 'line',
    smooth: true,
    yAxisIndex: isCombo ? 1 : 0,
    symbol: 'circle',
    data: items.map((row) => ({
      value: isCombo ? secondaryOf(row) : metricOf(row),
      symbolSize: isMe(row) ? 10 : 6,
      itemStyle: { color: isMe(row) ? '#f59e0b' : '#10b981' }
    })),
    lineStyle: { width: 2.2, color: isCombo ? '#10b981' : '#10b981' },
    areaStyle: isCombo
      ? undefined
      : {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(16, 185, 129, 0.22)' },
              { offset: 1, color: 'rgba(16, 185, 129, 0.02)' }
            ]
          }
        },
    emphasis: { focus: 'series' }
  }

  const series =
    rankView.value === 'bar'
      ? [barSeries]
      : rankView.value === 'line'
        ? [lineSeries]
        : rankView.value === 'combo'
          ? [barSeries, lineSeries]
          : []

  const yAxis = isCombo
    ? [
        { ...valueAxis, name: primaryName.value, nameTextStyle: { color: axisColor, fontSize: 11 } },
        {
          ...valueAxis,
          name: secondaryName.value,
          nameTextStyle: { color: axisColor, fontSize: 11 },
          splitLine: { show: false }
        }
      ]
    : [{ ...valueAxis, name: primaryName.value, nameTextStyle: { color: axisColor, fontSize: 11 } }]

  return {
    backgroundColor: 'transparent',
    animationDuration: 400,
    grid: { left: 6, right: 6, top: isCombo ? 54 : 34, bottom: 6, containLabel: true },
    legend: isCombo
      ? {
          show: true,
          top: 2,
          left: 'center',
          itemWidth: 12,
          itemHeight: 8,
          itemGap: 14,
          textStyle: { color: axisColor, fontSize: 11 }
        }
      : { show: false },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: dark ? 'rgba(37, 41, 46, 0.96)' : 'rgba(255, 255, 255, 0.97)',
      borderColor: splitColor,
      textStyle: { color: textColor, fontSize: 12 },
      formatter(params) {
        const row = items[params[0]?.dataIndex]
        if (!row) return ''
        const name = row.nickname || row.username || '用户 #' + row.userId
        const lines = [
          `第 ${row.rank} 名 · ${name}${isMe(row) ? '（我）' : ''}`,
          `${primaryName.value}：${metricOf(row)}${contest.value?.type === 1 ? ' 分' : ' 题'}`
        ]
        if (isCombo) {
          lines.push(
            `${secondaryName.value}：${secondaryOf(row)}${contest.value?.type === 1 ? ' 题' : ' 分钟'}`
          )
        }
        if (contest.value?.type !== 1) {
          lines.push(`罚时：${formatPenalty(row.penaltySeconds)}`)
        }
        return lines.join('<br/>')
      }
    },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: splitColor } }
    },
    yAxis,
    series
  }
}

function disposeChart() {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}

/** 渲染/更新图表；容器宽高随窗口变化（ResizeObserver 驱动 resize） */
function renderChart() {
  if (rankView.value === 'table' || !rank.value.length) {
    disposeChart()
    return
  }
  nextTick(() => {
    if (!chartEl.value) return
    if (!chartInstance) {
      chartInstance = echarts.init(chartEl.value)
      resizeObserver = new ResizeObserver(() => chartInstance && chartInstance.resize())
      resizeObserver.observe(chartEl.value)
    }
    chartInstance.setOption(buildChartOption(), true)
    chartInstance.resize()
  })
}

watch(rankView, renderChart)
watch(rank, renderChart)
watch(() => themeStore.isDark, renderChart)
watch(() => contest.value?.type, renderChart)

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
    rankPage.value = 1
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

watch(tab, async (t) => {
  if (t === 'rank') {
    if (!rank.value.length) {
      await loadRank()
    }
    // 页签切换后容器才可见，重新挂载/适配尺寸
    renderChart()
  }
})

onMounted(load)
onBeforeUnmount(disposeChart)
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

.rank-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-bottom: 10px;
}

.rank-metric {
  font-size: 12px;
  margin-left: auto;
}

/* ---------- 图表视图 ---------- */
.rank-chart {
  min-height: 260px;
}

.rank-chart-box {
  width: 100%;
  height: 320px;
}

/* 排名表中「我」所在行高亮 */
:deep(.rank-me-row) {
  background: var(--el-color-warning-light-9, #fdf6ec);
}

@media (max-width: 720px) {
  .rank-toolbar {
    justify-content: flex-start;
  }

  .rank-metric {
    width: 100%;
    margin-left: 0;
    text-align: right;
  }

  .rank-chart {
    min-height: 220px;
  }

  .rank-chart-box {
    height: 260px;
  }
}

.table-pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
