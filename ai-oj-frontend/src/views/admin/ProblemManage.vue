<template>
  <div>
    <div class="oj-card toolbar-card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索题号 / 标题"
          clearable
          :prefix-icon="Search"
          style="width: 240px"
          @keyup.enter="load(1)"
          @clear="load(1)"
        />
        <el-select v-model="query.difficulty" placeholder="难度" clearable style="width: 120px" @change="load(1)">
          <el-option v-for="(d, k) in DIFFICULTY" :key="k" :label="d.desc" :value="Number(k)" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="load(1)">查询</el-button>

        <div class="toolbar-right">
          <el-upload
            :show-file-list="false"
            accept=".xlsx,.xls"
            :auto-upload="false"
            :on-change="onImportFile"
          >
            <el-button :icon="Upload" :loading="importing">导入</el-button>
          </el-upload>
          <el-button :icon="Download" :loading="exporting" @click="onExport">导出</el-button>
          <el-button type="primary" :icon="Plus" @click="$router.push('/admin/problem/create')">
            新建题目
          </el-button>
        </div>
      </div>
    </div>

    <div class="oj-card table-card">
      <el-table v-loading="loading" :data="list" style="width: 100%">
        <el-table-column prop="problemKey" label="题号" width="110" />
        <el-table-column label="标题" min-width="220">
          <template #default="{ row }">
            <el-link class="problem-title" :underline="false" @click="goEdit(row)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="90" align="center">
          <template #default="{ row }">
            <DifficultyTag :difficulty="row.difficulty" />
          </template>
        </el-table-column>
        <el-table-column label="通过/提交" width="120" align="center">
          <template #default="{ row }">
            <span>{{ row.acceptCount ?? 0 }}/{{ row.submitCount ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="通过率" width="90" align="center">
          <template #default="{ row }">{{ formatRate(row.acceptCount, row.submitCount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间/内存" width="120" align="center">
          <template #default="{ row }">{{ row.timeLimit }}ms / {{ row.memoryLimit }}MB</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="goEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该题目？用例将一并删除" width="220" @confirm="onDelete(row)">
              <template #reference>
                <el-button text type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无题目，点击右上角「新建题目」创建" />
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
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Plus, Upload, Download } from '@element-plus/icons-vue'
import { adminPageProblems, deleteProblem, importProblems, exportProblems } from '@/api/modules/admin'
import { DIFFICULTY } from '@/utils/oj'
import { formatRate } from '@/utils/format'
import DifficultyTag from '@/components/DifficultyTag.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const importing = ref(false)
const exporting = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', difficulty: null })

async function load(resetPage) {
  if (resetPage) query.pageNum = 1
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.keyword) params.keyword = query.keyword
    if (query.difficulty !== null && query.difficulty !== '' && query.difficulty !== undefined) {
      params.difficulty = query.difficulty
    }
    const data = await adminPageProblems(params)
    list.value = data.records || []
    total.value = Number(data.total) || 0
  } finally {
    loading.value = false
  }
}

function goEdit(row) {
  router.push(`/admin/problem/${row.id}/edit`)
}

async function onDelete(row) {
  await deleteProblem(row.id)
  ElMessage.success(`已删除题目 ${row.problemKey}`)
  // 删除后若当前页只剩这一条，回退一页
  if (list.value.length === 1 && query.pageNum > 1) query.pageNum -= 1
  load()
}

async function onImportFile(file) {
  importing.value = true
  try {
    const result = await importProblems(file.raw)
    ElMessage.success(`导入完成：成功 ${result.successCount ?? 0}，失败 ${result.failCount ?? 0}`)
    if (result.errors?.length) {
      ElMessage.warning(result.errors.slice(0, 3).join('；'))
    }
    load(1)
  } finally {
    importing.value = false
  }
}

async function onExport() {
  exporting.value = true
  try {
    const blob = await exportProblems()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `problems-${Date.now()}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  if (route.query.import) ElMessage.info('点击「导入」选择 Excel 文件批量导入题目')
  load()
})
</script>

<style scoped>
.toolbar-card {
  padding: 14px 16px;
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar-right {
  margin-left: auto;
  display: flex;
  gap: 10px;
  align-items: center;
}

.toolbar-right .el-upload {
  display: inline-flex;
}

.table-card {
  padding: 8px 12px 16px;
}

.problem-title {
  font-weight: 500;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
