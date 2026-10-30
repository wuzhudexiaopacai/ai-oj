<template>
  <div class="submission-filter">
    <el-select
      v-model="form.status"
      placeholder="状态"
      clearable
      :size="size"
      class="item status"
      @change="emitSearch"
    >
      <el-option v-for="(s, code) in JUDGE_STATUS" :key="code" :label="s.desc" :value="Number(code)" />
    </el-select>
    <el-select
      v-model="form.language"
      placeholder="语言"
      clearable
      :size="size"
      class="item lang"
      @change="emitSearch"
    >
      <el-option v-for="l in LANGUAGES" :key="l.value" :label="l.label" :value="l.value" />
    </el-select>
    <el-date-picker
      v-model="form.range"
      type="datetimerange"
      :size="size"
      start-placeholder="开始时间"
      end-placeholder="结束时间"
      value-format="YYYY-MM-DD HH:mm:ss"
      class="item range"
      @change="emitSearch"
    />
    <el-input
      v-if="showKeyword"
      v-model="form.keyword"
      :size="size"
      placeholder="题目 / 提交ID"
      clearable
      class="keyword"
      @keyup.enter="emitSearch"
      @clear="emitSearch"
    />
    <el-button :size="size" :icon="Search" @click="emitSearch">搜索</el-button>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { JUDGE_STATUS, LANGUAGES } from '@/utils/oj'

defineProps({
  size: { type: String, default: 'default' },
  /** 是否显示「题目 / 提交ID」关键字框：题目详情页已按 problemId 过滤，可关闭以让筛选收进一行 */
  showKeyword: { type: Boolean, default: true }
})

const emit = defineEmits(['search'])

const form = reactive({
  status: null,
  language: '',
  range: null,
  keyword: ''
})

/** 归一化为后端查询参数：空值统一传 null，由 axios 省略该参数 */
function emitSearch() {
  emit('search', {
    status: form.status ?? null,
    language: form.language || null,
    keyword: form.keyword.trim() || null,
    startTime: form.range ? form.range[0] : null,
    endTime: form.range ? form.range[1] : null
  })
}
</script>

<style scoped>
.submission-filter {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

/* 下拉框 / 搜索框 / 日期 / 按钮统一圆角，靠 size 属性对齐高度 */
.submission-filter :deep(.el-select__wrapper),
.submission-filter :deep(.el-input__wrapper),
.submission-filter :deep(.el-date-editor.el-input__wrapper) {
  border-radius: var(--oj-radius);
}

.status {
  width: 120px;
}

.lang {
  width: 120px;
}

.range {
  flex: 1 1 320px;
  min-width: 240px;
}

.keyword {
  flex: 0 1 200px;
  min-width: 160px;
}
</style>
