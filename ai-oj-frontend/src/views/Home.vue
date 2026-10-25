<template>
  <div class="oj-page">
    <div class="oj-container">
      <section class="hero">
        <h1 class="hero-title">
          用 <span class="accent">AI</span> 点亮你的算法之路
        </h1>
        <p class="hero-subtitle oj-muted">
          在线评测 · 实时判题 · 周赛竞技 · AI 智能提示与代码诊断
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" round @click="$router.push('/problem')">
            开始刷题
          </el-button>
          <el-button size="large" round @click="$router.push('/contest')">参加比赛</el-button>
        </div>
      </section>

      <section class="features">
        <div v-for="f in features" :key="f.title" class="oj-card feature-card">
          <el-icon :size="28" class="feature-icon"><component :is="f.icon" /></el-icon>
          <h3>{{ f.title }}</h3>
          <p class="oj-muted">{{ f.desc }}</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { Cpu, Timer, Trophy, MagicStick } from '@element-plus/icons-vue'

const features = [
  { icon: Cpu, title: '多语言评测', desc: 'C / C++ / Java / Python / Go / JavaScript，沙箱隔离评测' },
  { icon: Timer, title: '实时反馈', desc: '判题状态秒级回显，耗时与内存一目了然' },
  { icon: Trophy, title: '比赛排行', desc: '周期性周赛，实时榜单，与高手同场竞技' },
  { icon: MagicStick, title: 'AI 助手', desc: '卡题时给出思路提示，提交后诊断错误原因' }
]
</script>

<style scoped>
.hero {
  text-align: center;
  padding: 72px 0 56px;
}

.hero-title {
  font-size: 40px;
  font-weight: 700;
  margin: 0 0 16px;
}

.accent {
  color: var(--oj-primary);
}

.hero-subtitle {
  font-size: 16px;
  margin: 0 0 32px;
}

.hero-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  padding-bottom: 24px;
}

.feature-card {
  text-align: center;
  padding: 28px 20px;
}

.feature-icon {
  color: var(--oj-primary);
}

.feature-card h3 {
  margin: 12px 0 8px;
}

.feature-card p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
}
</style>
