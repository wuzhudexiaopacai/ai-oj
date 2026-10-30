<template>
  <span class="difficulty" :class="`difficulty--${difficulty.key.toLowerCase()}`">
    {{ difficulty.desc }}
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { difficultyOf } from '@/utils/oj'

const props = defineProps({
  difficulty: { type: Number, required: true }
})

const difficulty = computed(() => difficultyOf(props.difficulty))
</script>

<style scoped>
.difficulty {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
  border-radius: 999px;
  border: 1px solid transparent;
  white-space: nowrap;
}

.difficulty--easy {
  color: var(--oj-success);
  background: var(--oj-success-soft);
  border-color: rgba(38, 166, 75, 0.22);
}

.difficulty--medium {
  color: var(--oj-warning);
  background: rgba(217, 164, 6, 0.1);
  border-color: rgba(217, 164, 6, 0.24);
}

.difficulty--hard {
  color: var(--oj-danger);
  background: var(--oj-danger-soft);
  border-color: rgba(227, 77, 89, 0.22);
}

.difficulty--unknown {
  color: var(--oj-info);
  background: var(--oj-info-soft);
  border-color: rgba(38, 118, 217, 0.22);
}
</style>
