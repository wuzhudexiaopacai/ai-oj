<template>
  <span class="status-badge" :class="`is-${status.type}`">
    <i class="dot" />
    <span class="label">{{ status.desc }}</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { judgeStatusOf } from '@/utils/oj'

const props = defineProps({
  status: { type: Number, required: true }
})

const status = computed(() => judgeStatusOf(props.status))
</script>

<style scoped>
/* 圆角徽章：答案正确=绿，答案错误/运行/编译错误=红，超时/内存等=橙，进行中含脉冲点 */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.6;
  border: 1px solid transparent;
  white-space: nowrap;
  transition: background-color 0.2s var(--oj-ease), border-color 0.2s var(--oj-ease);
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  flex-shrink: 0;
}

.is-success {
  color: var(--oj-success);
  background: var(--oj-success-soft);
  border-color: color-mix(in srgb, var(--oj-success) 24%, transparent);
}

.is-danger {
  color: var(--oj-danger);
  background: var(--oj-danger-soft);
  border-color: color-mix(in srgb, var(--oj-danger) 24%, transparent);
}

.is-warning {
  color: var(--oj-warning);
  background: var(--oj-primary-soft);
  border-color: color-mix(in srgb, var(--oj-warning) 24%, transparent);
}

.is-info {
  color: var(--oj-text-secondary);
  background: var(--oj-bg-fill);
  border-color: var(--oj-border);
}

/* 排队 / 编译 / 评测中：状态点做轻微呼吸，提示"仍在进行" */
.is-info .dot {
  animation: badge-pulse 1.4s var(--oj-ease) infinite;
}

@keyframes badge-pulse {
  0%,
  100% {
    opacity: 0.35;
  }
  50% {
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .is-info .dot {
    animation: none;
    opacity: 1;
  }
}
</style>
