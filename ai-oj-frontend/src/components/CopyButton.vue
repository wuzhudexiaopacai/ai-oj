<template>
  <!-- 内容块复制角标：默认弱化，hover 提亮，成功后短暂显示对勾 -->
  <button
    type="button"
    class="oj-copy"
    :class="{ 'is-done': copied }"
    :title="copied ? '已复制' : title"
    :aria-label="copied ? '已复制' : title"
    :disabled="!hasContent"
    @click="onCopy"
  >
    <el-icon :size="14">
      <Select v-if="copied" />
      <CopyDocument v-else />
    </el-icon>
  </button>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Select } from '@element-plus/icons-vue'
import { copyText } from '@/utils/clipboard'

const props = defineProps({
  /** 待复制文本，为空时按钮自动禁用 */
  text: { type: String, default: '' },
  /** 悬停提示文案 */
  title: { type: String, default: '复制' }
})

const copied = ref(false)
const hasContent = computed(() => !!props.text)

let resetTimer = null

/** 复制成功后 1.2s 内显示对勾，不弹提示打断阅读；失败时给出可操作的反馈 */
async function onCopy() {
  if (!hasContent.value) {
    return
  }
  try {
    await copyText(props.text)
    copied.value = true
    clearTimeout(resetTimer)
    resetTimer = setTimeout(() => {
      copied.value = false
    }, 1200)
  } catch {
    ElMessage.warning('复制失败，请手动选择内容复制')
  }
}

onBeforeUnmount(() => clearTimeout(resetTimer))
</script>
