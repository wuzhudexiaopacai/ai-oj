<template>
  <div class="solution-panel">
    <div class="panel-head">
      <span class="oj-muted">共 {{ total }} 篇题解</span>
      <el-button class="btn-submit" type="primary" size="small" :icon="EditPen" @click="openCreate">
        写题解
      </el-button>
    </div>

    <el-empty v-if="!loading && list.length === 0" description="还没有题解，来写第一篇吧" />
    <div v-loading="loading" class="solution-list">
      <div v-for="item in list" :key="item.id" class="solution-item" @click="openDetail(item)">
        <div class="item-main">
          <div class="item-title">{{ item.title }}</div>
          <div class="item-meta oj-muted">
            <span>{{ item.authorName || '用户' + item.userId }}</span>
            <el-divider direction="vertical" />
            <span>{{ formatTime(item.createTime) }}</span>
          </div>
        </div>
        <el-button
          text
          size="small"
          :type="item.liked ? 'primary' : 'default'"
          @click.stop="onLike(item)"
        >
          <el-icon>
            <StarFilled v-if="item.liked" />
            <Star v-else />
          </el-icon>
          <span class="like-count">{{ item.likeCount }}</span>
        </el-button>
      </div>
    </div>

    <el-pagination
      v-if="total > pageSize"
      class="panel-pager"
      layout="prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="pageNum"
      background
      small
      @current-change="onPageChange"
    />

    <!-- 题解详情 -->
    <el-drawer v-model="detailVisible" size="62%" :title="current?.title || '题解详情'">
      <div v-if="current" class="detail-body">
        <div class="item-meta oj-muted">
          <span>{{ current.authorName || '用户' + current.userId }}</span>
          <el-divider direction="vertical" />
          <span>{{ formatTime(current.createTime) }}</span>
        </div>
        <MarkdownView :content="current.content || ''" />
      </div>
    </el-drawer>

    <!-- 写题解 -->
    <el-dialog v-model="createVisible" title="写题解" width="760px" append-to-body>
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="128" show-word-limit placeholder="一句话概括解题思路" />
        </el-form-item>
        <el-form-item label="正文（支持 Markdown / 数学公式 / Mermaid）">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="14"
            placeholder="## 思路&#10;&#10;时间复杂度 ..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen, Star, StarFilled } from '@element-plus/icons-vue'
import MarkdownView from '@/components/MarkdownView.vue'
import {
  createSolution,
  getSolutionDetail,
  pageSolutions,
  toggleSolutionLike
} from '@/api/modules/problem'

const props = defineProps({
  problemId: { type: [Number, String], required: true }
})

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const detailVisible = ref(false)
const current = ref(null)

const createVisible = ref(false)
const creating = ref(false)
const form = ref({ title: '', content: '' })

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

async function load() {
  if (!props.problemId) return
  loading.value = true
  try {
    const data = await pageSolutions(props.problemId, {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function onPageChange(page) {
  pageNum.value = page
  load()
}

async function openDetail(item) {
  const data = await getSolutionDetail(item.id)
  current.value = data
  detailVisible.value = true
}

async function onLike(item) {
  const data = await toggleSolutionLike(item.id)
  item.liked = data.liked
  item.likeCount = data.likeCount
}

function openCreate() {
  form.value = { title: '', content: '' }
  createVisible.value = true
}

async function submitCreate() {
  if (!form.value.title.trim() || !form.value.content.trim()) {
    ElMessage.warning('标题和正文不能为空')
    return
  }
  creating.value = true
  try {
    await createSolution({
      problemId: props.problemId,
      title: form.value.title.trim(),
      content: form.value.content
    })
    ElMessage.success('题解发布成功')
    createVisible.value = false
    pageNum.value = 1
    await load()
  } finally {
    creating.value = false
  }
}

watch(
  () => props.problemId,
  () => {
    pageNum.value = 1
    list.value = []
    total.value = 0
  }
)

onMounted(load)
</script>

<style scoped>
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.solution-list {
  min-height: 80px;
}

.solution-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--oj-border);
  border-radius: var(--oj-radius);
  margin-bottom: 8px;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}

.solution-item:hover {
  border-color: var(--oj-primary);
  background: var(--oj-bg-fill);
}

.item-main {
  min-width: 0;
  flex: 1;
}

.item-title {
  font-weight: 500;
  color: var(--oj-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-meta {
  margin-top: 4px;
  font-size: 12px;
  display: flex;
  align-items: center;
}

.like-count {
  margin-left: 2px;
}

.panel-pager {
  margin-top: 12px;
  justify-content: flex-end;
}

.detail-body {
  padding: 0 4px 24px;
}
</style>
