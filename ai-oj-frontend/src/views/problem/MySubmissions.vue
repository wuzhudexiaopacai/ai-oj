<template>
  <div v-loading="loading">
    <el-table :data="list" size="small">
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <JudgeStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="语言" width="100">
        <template #default="{ row }">{{ languageOf(row.language).label }}</template>
      </el-table-column>
      <el-table-column label="耗时" width="90">
        <template #default="{ row }">
          <span class="oj-muted">{{ row.timeUsed != null ? row.timeUsed + ' ms' : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" min-width="150">
        <template #default="{ row }">
          <span class="oj-muted">{{ formatTime(row.createTime) }}</span>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无提交记录" :image-size="60" />
      </template>
    </el-table>
    <div v-if="total > pageSize" class="pager">
      <el-pagination
        v-model:current-page="pageNum"
        :total="total"
        :page-size="pageSize"
        layout="prev, pager, next"
        background
        small
        @current-change="load()"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { pageSubmissions } from '@/api/modules/judge'
import { languageOf } from '@/utils/oj'
import { formatTime } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import JudgeStatusTag from '@/components/JudgeStatusTag.vue'

const props = defineProps({
  problemId: { type: [Number, String], default: null }
})

const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10

async function load() {
  // 未登录不请求：该接口按登录用户过滤，匿名访问会触发 401 跳转登录
  if (!userStore.isLogin) {
    return
  }
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize, mine: true }
    if (props.problemId) params.problemId = props.problemId
    const data = await pageSubmissions(params)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
