<template>
  <div class="not-found">
    <div class="not-found-card oj-reveal">
      <div class="code">404</div>
      <p class="title">访问的页面不存在</p>
      <p class="desc oj-muted">页面可能已被移除，或链接有误</p>
      <el-button type="primary" @click="$router.replace('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--oj-bg-page);
}

/* 收敛处理：去掉大号亮色数字，改为中性层级排版 */
.not-found-card {
  text-align: center;
}

.code {
  font-size: 56px;
  font-weight: 700;
  color: var(--oj-text-placeholder);
  line-height: 1;
  letter-spacing: 2px;
}

.title {
  margin: 18px 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: var(--oj-text-primary);
}

.desc {
  margin: 0 0 22px;
  font-size: 13px;
}
</style>
