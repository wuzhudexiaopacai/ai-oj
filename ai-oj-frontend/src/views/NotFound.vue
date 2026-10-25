<template>
  <div class="not-found">
    <div class="code">404</div>
    <p class="oj-muted">页面不存在或已被移除</p>
    <el-button type="primary" round @click="$router.replace('/')">返回首页</el-button>
  </div>
</template>

<script setup>
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.code {
  font-size: 88px;
  font-weight: 800;
  color: var(--oj-primary);
  line-height: 1;
}
</style>
