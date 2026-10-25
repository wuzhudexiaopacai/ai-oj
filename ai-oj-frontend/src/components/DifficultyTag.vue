<template>
  <span class="difficulty" :style="{ color: difficulty.color }">{{ difficulty.desc }}</span>
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
  font-size: 13px;
  font-weight: 500;
}
</style>
