<template>
  <el-tag :type="status.type" size="small" effect="light" round>{{ status.desc }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'
import { judgeStatusOf } from '@/utils/oj'

const props = defineProps({
  status: { type: Number, required: true }
})

const status = computed(() => judgeStatusOf(props.status))
</script>
