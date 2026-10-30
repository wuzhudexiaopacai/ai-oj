<template>
  <!-- 提交详情抽屉：展示判题结果、编译错误、用例明细与源代码 -->
  <el-drawer
    :model-value="modelValue"
    title="提交详情"
    size="640px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="detail-body">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="提交 ID">
            <span class="mono">{{ detail.submitId }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <JudgeStatusTag :status="detail.status" />
          </el-descriptions-item>
          <el-descriptions-item label="语言">{{ languageOf(detail.language).label }}</el-descriptions-item>
          <el-descriptions-item label="得分">{{ detail.score ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="耗时">
            {{ detail.timeUsed != null ? detail.timeUsed + ' ms' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="内存">{{ formatMemory(detail.memoryUsed) }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="判题时间">{{ formatTime(detail.judgeTime) }}</el-descriptions-item>
        </el-descriptions>

        <pre v-if="detail.errorMessage" class="compile-msg">{{ detail.errorMessage }}</pre>

        <template v-if="detail.cases?.length">
          <h4 class="section-title">用例结果</h4>
          <el-table :data="detail.cases" size="small">
            <el-table-column label="#" prop="seq" width="50" align="center" />
            <el-table-column label="结果" width="110">
              <template #default="{ row }">
                <JudgeStatusTag :status="row.status" />
              </template>
            </el-table-column>
            <el-table-column label="耗时" width="90" align="center">
              <template #default="{ row }">
                {{ row.timeUsed != null ? row.timeUsed + ' ms' : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="内存" width="100" align="center">
              <template #default="{ row }">{{ formatMemory(row.memoryUsed) }}</template>
            </el-table-column>
          </el-table>
        </template>

        <h4 class="section-title">提交代码</h4>
        <pre class="code-block">{{ detail.code }}</pre>
      </template>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getSubmission } from '@/api/modules/judge'
import { languageOf } from '@/utils/oj'
import { formatTime, formatMemory } from '@/utils/format'
import JudgeStatusTag from './JudgeStatusTag.vue'

const props = defineProps({
  /** 抽屉显隐（v-model） */
  modelValue: { type: Boolean, default: false },
  /** 要查看的提交 submitId，打开时按此 ID 拉取详情 */
  submitId: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const loading = ref(false)
const detail = ref(null)

/** 抽屉打开且 submitId 变化时拉取详情 */
async function load(submitId) {
  if (!submitId) {
    return
  }
  loading.value = true
  detail.value = null
  try {
    detail.value = await getSubmission(submitId)
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.modelValue, props.submitId],
  ([visible, id]) => {
    if (visible && id) {
      load(id)
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.detail-body {
  min-height: 200px;
}

.mono {
  font-family: Consolas, Menlo, monospace;
  font-size: 12px;
}

.section-title {
  margin: 20px 0 10px;
  font-size: 15px;
}

.compile-msg {
  margin-top: 16px;
  padding: 12px 14px;
  border-radius: 6px;
  background: var(--oj-bg-page);
  border-left: 3px solid var(--oj-danger);
  color: var(--oj-danger);
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.code-block {
  margin-top: 8px;
  padding: 12px 14px;
  border-radius: 6px;
  background: var(--oj-bg-page);
  border: 1px solid var(--oj-border);
  font-family: Consolas, Menlo, monospace;
  font-size: 12px;
  line-height: 1.6;
  max-height: 420px;
  overflow: auto;
  white-space: pre;
}
</style>
