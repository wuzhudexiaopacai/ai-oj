<template>
  <div class="oj-page">
    <div class="oj-container">
      <div class="oj-page-header">
        <h2 class="oj-page-title">比赛</h2>
      </div>

      <div v-loading="loading">
        <div v-if="list.length" class="contest-grid">
          <div
            v-for="c in list"
            :key="c.id"
            class="oj-card contest-card"
            @click="$router.push(`/contest/${c.id}`)"
          >
            <div class="contest-head">
              <h3 class="contest-title">{{ c.title }}</h3>
              <el-tag :type="statusTag(c).type" size="small" round>{{ statusTag(c).label }}</el-tag>
            </div>
            <p class="oj-muted contest-desc">{{ c.description || '暂无简介' }}</p>
            <div class="contest-meta oj-muted">
              <div><el-icon><Clock /></el-icon> {{ formatTime(c.startTime) }} 开始</div>
              <div><el-icon><Timer /></el-icon> 时长 {{ durationOf(c) }}</div>
              <div><el-icon><User /></el-icon> {{ c.registerCount || 0 }} 人已报名</div>
            </div>
          </div>
        </div>
        <el-empty v-else-if="!loading" description="暂无比赛" />

        <div class="pager">
          <el-pagination
            v-model:current-page="query.pageNum"
            :total="total"
            :page-size="query.pageSize"
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
import { Clock, Timer, User } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { pageContests } from '@/api/modules/contest'
import { formatTime } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 12 })

async function load() {
  loading.value = true
  try {
    const data = await pageContests({ pageNum: query.pageNum, pageSize: query.pageSize })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function statusTag(c) {
  const now = dayjs()
  if (now.isBefore(dayjs(c.startTime))) return { label: '未开始', type: 'info' }
  if (now.isAfter(dayjs(c.endTime))) return { label: '已结束', type: '' }
  return { label: '进行中', type: 'success' }
}

function durationOf(c) {
  const mins = dayjs(c.endTime).diff(dayjs(c.startTime), 'minute')
  if (mins >= 1440) return `${(mins / 1440).toFixed(1)} 天`
  if (mins >= 60) return `${(mins / 60).toFixed(1)} 小时`
  return `${mins} 分钟`
}

onMounted(load)
</script>

<style scoped>
.contest-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.contest-card {
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.contest-card:hover {
  transform: translateY(-2px);
}

.contest-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.contest-title {
  margin: 0;
  font-size: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contest-desc {
  font-size: 13px;
  margin: 10px 0;
  height: 38px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.contest-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
}

.contest-meta div {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
