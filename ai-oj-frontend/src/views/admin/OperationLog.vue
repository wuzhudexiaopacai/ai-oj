<template>
  <div class="admin-page operation-log">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" class="filter-form" @submit.prevent>
        <el-form-item label="模块">
          <el-input v-model="query.module" placeholder="如 题目管理" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="query.username" placeholder="用户名" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.success" placeholder="全部" clearable style="width: 110px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="路径或方法名" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 340px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load(1)">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table v-loading="loading" :data="rows" row-key="id" border stripe>
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="log-detail">
            <div class="detail-item">
              <div class="detail-title">请求入参</div>
              <pre class="detail-pre">{{ formatJson(row.requestParam) }}</pre>
            </div>
            <div class="detail-item">
              <div class="detail-title">响应出参</div>
              <pre class="detail-pre">{{ formatJson(row.responseResult) }}</pre>
            </div>
            <div v-if="row.errorMsg" class="detail-item">
              <div class="detail-title">失败原因</div>
              <pre class="detail-pre is-error">{{ row.errorMsg }}</pre>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="logTime" label="操作时间" width="170" />
      <el-table-column prop="module" label="模块" width="120" show-overflow-tooltip />
      <el-table-column prop="operation" label="操作" min-width="140" show-overflow-tooltip />
      <el-table-column prop="actionTypeLabel" label="类型" width="80" />
      <el-table-column prop="username" label="操作人" width="110" show-overflow-tooltip />
      <el-table-column prop="httpMethod" label="方法" width="80" />
      <el-table-column prop="uri" label="请求路径" min-width="200" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="130" />
      <el-table-column prop="costMs" label="耗时(ms)" width="100" align="right" />
      <el-table-column label="结果" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">
            {{ row.success === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="traceId" label="TraceId" width="150" show-overflow-tooltip />
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="load()"
        @size-change="load(1)"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { fetchOperationLogPage } from '@/api/modules/log'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const timeRange = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  module: '',
  username: '',
  success: null,
  keyword: '',
  startTime: null,
  endTime: null
})

async function load(page) {
  if (page) query.pageNum = page
  query.startTime = timeRange.value?.[0] || null
  query.endTime = timeRange.value?.[1] || null
  loading.value = true
  try {
    const data = await fetchOperationLogPage(query)
    rows.value = data?.records || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

function reset() {
  query.module = ''
  query.username = ''
  query.success = null
  query.keyword = ''
  timeRange.value = []
  load(1)
}

/** 入参/出参为 JSON 字符串时格式化展示，解析失败则原样输出 */
function formatJson(value) {
  if (!value) return '—'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

onMounted(() => load())
</script>

<style scoped>
.operation-log {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-card :deep(.el-card__body) {
  padding: 14px 16px 2px;
}

.log-detail {
  display: grid;
  gap: 12px;
  padding: 8px 16px 12px;
}

.detail-title {
  font-size: 12px;
  color: var(--oj-text-secondary);
  margin-bottom: 6px;
}

.detail-pre {
  margin: 0;
  padding: 10px 12px;
  max-height: 260px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.6;
  background: var(--oj-bg-fill);
  border: 1px solid var(--oj-border);
  border-radius: var(--oj-radius-sm);
  white-space: pre-wrap;
  word-break: break-all;
}

.detail-pre.is-error {
  color: var(--el-color-danger);
}

.pagination {
  display: flex;
  justify-content: flex-end;
}
</style>
