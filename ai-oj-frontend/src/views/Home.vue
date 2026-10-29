<template>
  <div class="home">
    <!-- ============ 主视觉：左文案 + 右产品示意，纯色底 + 极细点阵纹理 ============ -->
    <section class="hero">
      <!-- 背景纹理：低对比点阵，遮罩向下渐隐，克制不抢内容 -->
      <div class="hero-texture" aria-hidden="true"></div>

      <div class="oj-container hero-inner">
        <div class="hero-copy">
          <div class="hero-badge oj-reveal" style="--reveal-delay: 0.05s">
            <el-icon :size="14"><Cpu /></el-icon>
            AI 驱动的在线评测与竞赛平台
          </div>

          <!-- 标题层级：主标题沉稳大号，关键词仅用主色点色，不做花哨处理 -->
          <h1 class="hero-title oj-reveal" style="--reveal-delay: 0.12s">
            专业级在线判题基础设施<br />
            服务于<span class="accent">评测</span>与<span class="accent">竞赛</span>场景
          </h1>

          <p class="hero-subtitle oj-reveal" style="--reveal-delay: 0.2s">
            提供多语言容器沙箱评测、实时结果反馈、赛事全流程运营与 AI
            辅助分析能力，支撑日常训练与高并发竞赛的稳定性要求。
          </p>

          <!-- 主次按钮：主按钮实心强调，次按钮描边弱化，尺寸一致但视觉重量拉开 -->
          <div class="hero-actions oj-reveal" style="--reveal-delay: 0.28s">
            <el-button type="primary" size="large" class="cta-primary" @click="$router.push('/problem')">
              开始刷题
            </el-button>
            <el-button size="large" class="cta-secondary" @click="$router.push('/contest')">
              查看当前赛事
              <el-icon class="cta-arrow"><ArrowRight /></el-icon>
            </el-button>
          </div>

          <!-- 能力指标：只列可核实的平台事实，不放虚标数字；各项带分隔与 hover 反馈 -->
          <div class="hero-metrics">
            <div
              v-for="(m, i) in metrics"
              :key="m.label"
              class="metric-item"
              data-rv
              :style="{ '--rv-delay': `${0.32 + i * 0.06}s` }"
            >
              <span class="metric-value">{{ m.value }}</span>
              <span class="metric-label">{{ m.label }}</span>
            </div>
          </div>
        </div>

        <!-- 产品示意区：两张 Demo 卡错错落排布，错序滑入后缓慢悬浮 -->
        <div class="hero-visual" aria-hidden="true">
          <div class="viz-slot viz-slot-demo">
            <div class="viz-card viz-demo">
              <div class="viz-head">
                <span class="viz-title">判题详情</span>
                <span class="viz-tag">Demo</span>
              </div>
              <div class="viz-row">
                <span class="viz-key">提交编号</span>
                <span class="viz-mono">#2048135</span>
              </div>
              <div class="viz-row">
                <span class="viz-key">语言 / 状态</span>
                <span class="viz-status">
                  <span class="viz-mono">C++ 17</span>
                  <span class="viz-pass">Accepted</span>
                </span>
              </div>
              <div class="viz-code">
                <span class="code-bar" style="width: 76%"></span>
                <span class="code-bar" style="width: 58%"></span>
                <span class="code-bar" style="width: 68%"></span>
                <span class="code-bar" style="width: 40%"></span>
              </div>
              <div class="viz-metrics">
                <span>用时 <b>23 ms</b></span>
                <span>内存 <b>3.8 MB</b></span>
                <span>用例 <b>24 / 24</b></span>
              </div>
            </div>
          </div>

          <div class="viz-slot viz-slot-rank">
            <div class="viz-card viz-rank">
              <div class="viz-head">
                <span class="viz-title">实时榜单</span>
                <span class="viz-tag">Live</span>
              </div>
              <div v-for="(r, i) in demoRank" :key="r.name" class="rank-row">
                <span class="rank-no" :class="`rank-no-${i + 1}`">{{ i + 1 }}</span>
                <span class="rank-name">{{ r.name }}</span>
                <span class="rank-score">{{ r.score }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ 平台核心能力：统一线性图标，四卡矩阵 ============ -->
    <section class="oj-container features">
      <div class="section-head" data-rv>
        <h2 class="section-title">平台核心能力</h2>
        <p class="section-desc">
          覆盖题目评测、提交反馈、赛事运营与智能辅助的完整业务链路
        </p>
      </div>

      <div class="feature-grid">
        <div
          v-for="(f, i) in features"
          :key="f.title"
          class="feature-card"
          data-rv
          :style="{ '--rv-delay': `${i * 0.08}s` }"
        >
          <div class="feature-icon">
            <el-icon :size="22"><component :is="f.icon" /></el-icon>
          </div>
          <h3 class="feature-title">{{ f.title }}</h3>
          <p class="feature-desc">{{ f.desc }}</p>
        </div>
      </div>
    </section>

    <!-- ============ 底部转化区：稳重收束，不用夸张色块 ============ -->
    <section class="cta-section">
      <div class="oj-container cta-inner" data-rv>
        <div class="cta-text">
          <h2 class="cta-title">即刻体验标准化的评测与竞赛服务</h2>
          <p class="cta-desc">注册账号即可访问题库与进行中的赛事，无需额外配置。</p>
        </div>
        <el-button type="primary" size="large" class="cta-primary" @click="$router.push(userStore.isLogin ? '/problem' : '/register')">
          {{ userStore.isLogin ? '进入题库' : '注册并开始' }}
        </el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
// 图标统一取 Element Plus 线性图标族，保证风格一致、无混搭
import { onBeforeUnmount, onMounted } from 'vue'
import { Cpu, Odometer, Trophy, ChatLineSquare, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 主视觉下方的能力事实条（均为平台真实能力，非营销虚数）
const metrics = [
  { value: '6', label: '语言编译器' },
  { value: '沙箱', label: '容器隔离运行' },
  { value: 'ACM / OI', label: '双赛制支持' },
  { value: '秒级', label: '判题结果反馈' },
]

// 平台核心能力矩阵：文案克制、描述功能边界而非情绪
const features = [
  {
    icon: Cpu,
    title: '多语言沙箱评测',
    desc: '基于容器隔离沙箱，支持 C / C++ / Java / Python / Go / JavaScript，对资源用量与执行时限做硬性约束，保证评测环境安全、结果可复现。',
  },
  {
    icon: Odometer,
    title: '实时评测反馈',
    desc: '评测流水线由消息队列驱动，提交状态、运行耗时、内存占用与失败用例逐条回传，帮助使用者快速定位问题。',
  },
  {
    icon: Trophy,
    title: '赛事运营与实时榜单',
    desc: '支持 ACM / OI 两种赛制，覆盖赛事发布、报名、榜单实时推送与结果归档的完整流程，适配周期性竞赛运营。',
  },
  {
    icon: ChatLineSquare,
    title: 'AI 智能辅助',
    desc: '结合题目上下文提供分级思路提示，提交未通过时自动分析错误原因，降低自主排查成本。',
  },
]

// 示意榜单（静态演示数据，仅用于产品展示区排版示意）
const demoRank = [
  { name: '选手 A', score: '6 题 / 00:42' },
  { name: '选手 B', score: '5 题 / 01:08' },
  { name: '选手 C', score: '4 题 / 01:31' },
]

/* 滚动入场：区块进入视口后淡入上浮一次（280ms ease-out），
   节奏由元素上的 --rv-delay 控制；比全局挂载入场更适合长页面下滚场景 */
let rvObserver = null

onMounted(() => {
  rvObserver = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          entry.target.classList.add('rv-in')
          rvObserver.unobserve(entry.target)
        }
      }
    },
    { threshold: 0.12, rootMargin: '0px 0px -6% 0px' }
  )
  document.querySelectorAll('.home [data-rv]').forEach((el) => rvObserver.observe(el))
})

onBeforeUnmount(() => rvObserver?.disconnect())
</script>

<style scoped>
/* ================= 滚动入场（IntersectionObserver 驱动） ================= */
/* 初始隐藏，进入视口后加 .rv-in 淡入上浮一次；节奏由元素上的 --rv-delay 控制 */
.home [data-rv] {
  opacity: 0;
  transform: translateY(18px);
  transition:
    opacity var(--oj-duration) var(--oj-ease-out) var(--rv-delay, 0s),
    transform var(--oj-duration) var(--oj-ease-out) var(--rv-delay, 0s);
  will-change: opacity, transform;
}

.home [data-rv].rv-in {
  opacity: 1;
  transform: none;
  will-change: auto;
}

/* ================= 主视觉 ================= */
.hero {
  position: relative;
  overflow: hidden;
  /* 纯色底，去掉旧版浅渐变；纹理由独立伪层承载 */
  background: var(--oj-bg-raised);
  border-bottom: 1px solid var(--oj-border);
}

/* 极细点阵纹理：低对比 + 向下渐隐遮罩，近似"低噪点"质感 */
.hero-texture {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(var(--oj-border) 1px, transparent 1px);
  background-size: 26px 26px;
  opacity: 0.35;
  -webkit-mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.6), transparent 78%);
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.6), transparent 78%);
  pointer-events: none;
}

.hero-inner {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: 56px;
  align-items: center;
  padding-top: 84px;
  padding-bottom: 84px;
}

/* 左：文案列 */
.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  border-radius: 999px;
  border: 1px solid var(--oj-border);
  background: var(--oj-bg-fill);
  color: var(--oj-text-regular);
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 22px;
}

.hero-badge .el-icon {
  color: var(--oj-primary);
}

.hero-title {
  font-size: 40px;
  font-weight: 700;
  letter-spacing: -0.6px;
  line-height: 1.32;
  margin: 0 0 20px;
  color: var(--oj-text-primary);
}

.accent {
  color: var(--oj-primary);
}

.hero-subtitle {
  font-size: 15px;
  color: var(--oj-text-regular);
  margin: 0 0 34px;
  max-width: 520px;
  line-height: 1.9;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

/* 主按钮：品牌实心橙 + 主色投影，视觉重量最高 */
.cta-primary {
  padding: 20px 34px;
  box-shadow: 0 4px 14px var(--oj-primary-soft);
}

/* 次按钮：透明底描边，hover 仅边框/文字变色，与主按钮主次分明 */
.cta-secondary {
  padding: 20px 28px;
  background: transparent;
  border-color: var(--oj-border-strong);
  color: var(--oj-text-regular);
}

.cta-secondary:hover {
  border-color: var(--oj-primary);
  color: var(--oj-primary-active);
  background: transparent;
}

.cta-arrow {
  margin-left: 4px;
  transition: transform var(--oj-duration-fast) var(--oj-ease);
}

.cta-secondary:hover .cta-arrow {
  transform: translateX(3px);
}

/* 能力事实条：等距分隔 + 逐项 hover 微弱反馈，提升可读性 */
.hero-metrics {
  display: flex;
  margin-top: 44px;
  padding-top: 26px;
  border-top: 1px solid var(--oj-divider);
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 30px;
  border-radius: var(--oj-radius);
  cursor: default;
  transition:
    background-color var(--oj-duration) var(--oj-ease-out),
    transform var(--oj-duration) var(--oj-ease-out);
}

.metric-item:first-child {
  padding-left: 10px;
}

/* 竖向细分隔线：用左边框实现，最后一项之后不再出现 */
.metric-item + .metric-item {
  border-left: 1px solid var(--oj-divider);
}

.metric-item:hover {
  background: var(--oj-bg-fill);
  transform: translateY(-1px);
}

.metric-value {
  font-size: 17px;
  font-weight: 600;
  color: var(--oj-text-primary);
  letter-spacing: 0.2px;
  transition: color var(--oj-duration) var(--oj-ease-out);
}

.metric-item:hover .metric-value {
  color: var(--oj-primary-active);
}

html[data-theme='dark'] .metric-item:hover .metric-value {
  color: var(--oj-primary);
}

.metric-label {
  font-size: 12px;
  color: var(--oj-text-secondary);
}

/* 右：产品示意区 —— slot 层负责入场滑入（错序），card 层负责缓慢悬浮，
   两层动画分离避免 transform 互相覆盖 */
.hero-visual {
  position: relative;
  min-height: 340px;
}

.viz-slot {
  will-change: transform, opacity;
}

/* 判题详情卡：处于常规流，向右留出让位，与榜单卡形成错落 */
.viz-slot-demo {
  margin-right: 28px;
  animation: viz-slide-in 0.3s var(--oj-ease-out) 0.3s both;
}

/* 实时榜单卡：绝对定位压在主卡右下角，晚一拍滑入 */
.viz-slot-rank {
  position: absolute;
  right: -8px;
  bottom: -34px;
  width: 260px;
  animation: viz-slide-in 0.3s var(--oj-ease-out) 0.45s both;
}

@keyframes viz-slide-in {
  from {
    opacity: 0;
    transform: translateX(28px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 缓慢微弱的上下悬浮：振幅 6px、7~9s 周期，两卡错相位不同步摆动 */
@keyframes viz-float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
  }
}

.viz-demo {
  width: 100%;
  max-width: 400px;
  animation: viz-float 7s ease-in-out 0.8s infinite;
}

.viz-rank {
  /* 层叠卡片：主卡投影更深一档，拉开层级 */
  box-shadow: var(--oj-shadow-pop);
  animation: viz-float 9s ease-in-out 2.6s infinite;
}

.viz-card {
  background: var(--oj-bg-raised);
  border: 1px solid var(--oj-border);
  border-radius: var(--oj-radius-lg);
  box-shadow: var(--oj-shadow-hover);
  padding: 20px 22px;
}

.viz-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.viz-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--oj-text-primary);
}

/* 演示标记：小尺寸中性徽标，声明"这是示意图" */
.viz-tag {
  font-size: 11px;
  font-weight: 500;
  color: var(--oj-text-secondary);
  background: var(--oj-bg-fill);
  border-radius: var(--oj-radius-sm);
  padding: 1px 8px;
  letter-spacing: 0.4px;
}

.viz-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  padding: 6px 0;
}

.viz-key {
  color: var(--oj-text-secondary);
}

.viz-mono {
  font-family: 'SFMono-Regular', Consolas, Menlo, monospace;
  font-size: 12.5px;
  color: var(--oj-text-regular);
}

.viz-status {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

/* Accepted 状态徽标：绿点 + 绿字，全页唯一状态色 */
.viz-pass {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--oj-success);
  background: var(--oj-success-soft);
  border-radius: var(--oj-radius-sm);
  padding: 2px 8px;
}

.viz-pass::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

/* 代码骨架条：纯装饰占位，用中性填充色，不模拟语法高亮 */
.viz-code {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: var(--oj-code-bg);
  border: 1px solid var(--oj-divider);
  border-radius: var(--oj-radius);
  padding: 14px 16px;
  margin: 12px 0;
}

.code-bar {
  height: 6px;
  border-radius: 3px;
  background: var(--oj-bg-fill-dark);
}

.viz-metrics {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--oj-text-secondary);
}

.viz-metrics b {
  font-weight: 600;
  color: var(--oj-text-regular);
}

.rank-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  padding: 7px 0;
  border-top: 1px solid var(--oj-divider);
}

.rank-no {
  width: 20px;
  height: 20px;
  border-radius: var(--oj-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  background: var(--oj-bg-fill);
  color: var(--oj-text-secondary);
}

/* 前三名用主色浅底微强调，克制不花哨 */
.rank-no-1 {
  background: var(--oj-primary-soft);
  color: var(--oj-primary-active);
}

.rank-name {
  flex: 1;
  color: var(--oj-text-regular);
}

.rank-score {
  font-family: 'SFMono-Regular', Consolas, Menlo, monospace;
  font-size: 12px;
  /* 得分属最弱信息层，用 placeholder 档灰拉开文字层次 */
  color: var(--oj-text-placeholder);
}

/* ================= 核心能力 ================= */
.features {
  padding-top: 72px;
  padding-bottom: 24px;
}

.section-head {
  text-align: center;
  margin-bottom: 40px;
}

.section-title {
  font-size: 26px;
  font-weight: 600;
  letter-spacing: -0.2px;
  margin: 0 0 10px;
  color: var(--oj-text-primary);
}

.section-desc {
  font-size: 14px;
  color: var(--oj-text-secondary);
  margin: 0;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
}

.feature-card {
  background: var(--oj-bg-raised);
  border: 1px solid var(--oj-border);
  border-radius: var(--oj-radius-lg);
  padding: 26px 24px;
  /* hover：上浮 3px + 阴影柔和放大 + 边框高亮，280ms ease-out，无弹跳 */
  transition:
    transform var(--oj-duration) var(--oj-ease-out),
    box-shadow var(--oj-duration) var(--oj-ease-out),
    border-color var(--oj-duration) var(--oj-ease-out);
}

.feature-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--oj-shadow-pop);
  border-color: var(--oj-primary-active);
}

html[data-theme='dark'] .feature-card:hover {
  border-color: var(--oj-primary);
}

/* 图标：中性底 + 线性图形，hover 时统一过渡为品牌色（单色体系，不搞五颜六色） */
.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--oj-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--oj-bg-fill);
  color: var(--oj-text-regular);
  transition:
    background-color var(--oj-duration) var(--oj-ease),
    color var(--oj-duration) var(--oj-ease);
}

.feature-card:hover .feature-icon {
  background: var(--oj-primary-soft);
  color: var(--oj-primary-active);
}

html[data-theme='dark'] .feature-card:hover .feature-icon {
  color: var(--oj-primary);
}

.feature-title {
  margin: 18px 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--oj-text-primary);
}

.feature-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--oj-text-secondary);
}

/* ================= 底部转化区 ================= */
.cta-section {
  background: var(--oj-bg-raised);
  border-top: 1px solid var(--oj-border);
  margin-top: 64px;
}

.cta-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding-top: 48px;
  padding-bottom: 48px;
}

.cta-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 6px;
  color: var(--oj-text-primary);
}

.cta-desc {
  font-size: 13px;
  color: var(--oj-text-secondary);
  margin: 0;
}

/* ================= 响应式 ================= */
@media (max-width: 992px) {
  /* 双栏退化为单栏，示意区移到文案下方并简化定位 */
  .hero-inner {
    grid-template-columns: 1fr;
    gap: 48px;
    padding-top: 56px;
    padding-bottom: 56px;
  }

  .hero-visual {
    min-height: 0;
    max-width: 420px;
  }

  .viz-slot-rank {
    position: static;
    width: 100%;
    margin-top: 16px;
  }

  .viz-slot-demo {
    margin-right: 0;
  }

  .hero-title {
    font-size: 32px;
  }

  .hero-metrics {
    flex-wrap: wrap;
  }

  /* 换行后竖分隔线会错位，撤掉边框改回间距 */
  .metric-item {
    padding: 6px 0;
  }

  .metric-item:first-child {
    padding-left: 0;
  }

  .metric-item + .metric-item {
    border-left: none;
    margin-left: 28px;
  }

  .cta-inner {
    flex-direction: column;
    align-items: flex-start;
  }
}

/* 减少动态偏好：关闭循环悬浮与入场位移，直接呈现内容 */
@media (prefers-reduced-motion: reduce) {
  .home [data-rv] {
    opacity: 1;
    transform: none;
    transition: none;
  }

  .viz-slot,
  .viz-demo,
  .viz-rank {
    animation: none;
  }
}
</style>
