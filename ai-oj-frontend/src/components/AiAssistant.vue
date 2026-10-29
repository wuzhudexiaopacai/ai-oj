<template>
  <div class="ai-assistant">
    <!-- 精灵球悬浮球：仅在题目页且已唤起后显示；可拖动，松手吸附左右边缘，单击展开面板 -->
    <div
      v-show="visible && !store.open"
      class="ai-ball"
      :class="{ 'is-dragging': dragging.kind === 'ball', 'is-streaming': store.streaming }"
      :style="{ left: ball.x + 'px', top: ball.y + 'px' }"
      title="AI 智能助手（可拖动）"
      @pointerdown="onBallDown"
    >
      <span class="ball-top"></span>
      <span class="ball-bottom"></span>
      <span class="ball-band"></span>
      <span class="ball-core"></span>
    </div>

    <!-- 助手面板：浮动小窗 或 挂靠四边 -->
    <section
      v-if="mountedOnce"
      class="ai-panel"
      :class="[
        mode === 'float' ? 'is-float' : `is-dock dock-${dock.side}`,
        { 'is-collapsed': !store.open },
      ]"
      :style="panelStyle"
    >
      <header class="panel-head" @pointerdown="onHeadDown">
        <span class="mini-ball" aria-hidden="true">
          <span class="mini-top"></span>
          <span class="mini-band"></span>
          <span class="mini-core"></span>
        </span>
        <span class="panel-title">AI 智能助手</span>
        <span v-if="problemLabel" class="panel-chip" :title="problemLabel">{{ problemLabel }}</span>
        <span class="panel-space"></span>
        <div class="head-tools" @pointerdown.stop>
          <el-tooltip content="浮动小窗" placement="bottom">
            <button
              type="button"
              class="tool-btn"
              :class="{ 'is-active': mode === 'float' }"
              @click="toFloat"
            >
              <i class="shape shape-float"></i>
            </button>
          </el-tooltip>
          <el-tooltip v-for="s in DOCK_SIDES" :key="s" :content="`挂靠${DOCK_LABELS[s]}`" placement="bottom">
            <button
              type="button"
              class="tool-btn"
              :class="{ 'is-active': mode === 'dock' && dock.side === s }"
              @click="toDock(s)"
            >
              <i class="shape" :class="`shape-${s}`"></i>
            </button>
          </el-tooltip>
          <span class="tool-sep"></span>
          <el-tooltip content="收起为悬浮球" placement="bottom">
            <button type="button" class="tool-btn" @click="collapse">
              <el-icon :size="15"><Fold /></el-icon>
            </button>
          </el-tooltip>
        </div>
      </header>

      <AiChat
        :problem="store.context.problem"
        :code="store.context.code"
        :language="store.context.language"
      />

      <!-- 浮动模式：八向拉伸把手 -->
      <template v-if="mode === 'float'">
        <span
          v-for="d in RZ_DIRS"
          :key="d"
          class="rz"
          :class="`rz-${d}`"
          @pointerdown="onRzDown($event, d)"
        ></span>
      </template>
      <!-- 挂靠模式：面板占满该边，仅可沿垂直于边缘的方向拉伸厚度 -->
      <span
        v-else
        class="rz"
        :class="[`rz-${dockInnerEdge}`, 'rz-edge']"
        @pointerdown="onRzDown($event, dockInnerEdge)"
      ></span>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Fold } from '@element-plus/icons-vue'
import { useAssistantStore } from '@/stores/assistant'
import AiChat from './AiChat.vue'

const store = useAssistantStore()
const route = useRoute()

/* ---------------- 常量与几何 ---------------- */
const BALL_SIZE = 52
const BALL_MARGIN = 14
// 挂靠时沿边缘方向占满视口，交叉轴（厚度）有默认初始值，用户可拉伸
// 左/右默认较宽（约占视口宽 42%），上/下默认约占视口高 45%
const MIN_W = 340
const MIN_H = 280
const MIN_LEN = 280
const SNAPSHOT_EDGE = 20
const DOCK_SIDES = ['left', 'right', 'top', 'bottom']
const DOCK_LABELS = { left: '左侧', right: '右侧', top: '顶部', bottom: '底部' }
// 挂靠时唯一可用的拉伸把手：面板内侧自由边（右挂靠拉左边缘，以此类推）
const DOCK_INNER_EDGE = { left: 'e', right: 'w', top: 's', bottom: 'n' }
const RZ_DIRS = ['n', 's', 'e', 'w', 'ne', 'nw', 'se', 'sw']
const STORE_KEY = 'ai-oj-assistant-layout-v2'

const mode = ref('dock')
const dock = reactive({ side: 'right', offset: 0, length: 0 })
const float = reactive({ x: 0, y: 0, w: 420, h: 560 })
const ball = reactive({ x: 0, y: 0 })
const mountedOnce = ref(false)
const dragging = reactive({ kind: null, dir: null })

const dockAxis = computed(() => (dock.side === 'left' || dock.side === 'right' ? 'y' : 'x'))
const dockInnerEdge = computed(() => DOCK_INNER_EDGE[dock.side])

// 助手只在题目做题页（题目详情）出现；其他路由一律隐藏
const visible = computed(() => route.name === 'problem-detail' && store.visible)

watch(visible, (v) => {
  // 离开题目页时收起面板（悬浮球位置保留，回到题目页继续用）
  if (!v && store.open) store.open = false
})

const problemLabel = computed(() => {
  const p = store.context.problem
  if (!p) return ''
  return p.problemKey ? `${p.problemKey} · ${p.title}` : p.title
})

const panelStyle = computed(() => {
  if (store.open && mode.value === 'float') {
    return { left: `${float.x}px`, top: `${float.y}px`, width: `${float.w}px`, height: `${float.h}px` }
  }
  if (store.open) {
    // 挂靠：沿边占满视口，长度即厚度（左/右=宽，上/下=高），仅可向内拉伸
    const len = `${dock.length}px`
    if (dock.side === 'left') return { left: '0px', top: '0px', height: '100vh', width: len }
    if (dock.side === 'right') return { right: '0px', top: '0px', height: '100vh', width: len }
    if (dock.side === 'top') return { top: '0px', left: '0px', width: '100vw', height: len }
    return { bottom: '0px', left: '0px', width: '100vw', height: len }
  }
  // 收起态仅参与退场动画，位置保持上一次
  return closedStyleCache
})

let closedStyleCache = {}

/* ---------------- 布局持久化 ---------------- */
function currentRect() {
  if (mode.value === 'float') return { x: float.x, y: float.y, w: float.w, h: float.h }
  if (dockAxis.value === 'y') {
    return {
      x: dock.side === 'left' ? 0 : window.innerWidth - dock.length,
      y: 0,
      w: dock.length,
      h: window.innerHeight,
    }
  }
  return {
    x: 0,
    y: dock.side === 'top' ? 0 : window.innerHeight - dock.length,
    w: window.innerWidth,
    h: dock.length,
  }
}

function saveLayout() {
  localStorage.setItem(
    STORE_KEY,
    JSON.stringify({
      mode: mode.value,
      dock: { ...dock },
      float: { ...float },
      ball: { ...ball },
    })
  )
}

function loadLayout() {
  let raw = null
  try {
    raw = JSON.parse(localStorage.getItem(STORE_KEY))
  } catch {
    raw = null
  }
  const vw = window.innerWidth
  const vh = window.innerHeight
  if (raw) {
    mode.value = raw.mode === 'float' ? 'float' : 'dock'
    Object.assign(dock, { side: 'right', ...raw.dock })
    Object.assign(float, { x: 0, y: 0, w: 420, h: 560, ...raw.float })
    Object.assign(ball, { ...raw.ball })
  }
  // 默认几何（首次或数据缺失时）：右侧挂靠、悬浮球在右下
  if (!raw) {
    dock.side = 'right'
    dock.length = dockDefault('right')
    dock.offset = 0
    float.w = 420
    float.h = Math.min(560, vh - 80)
    float.x = vw - float.w - 24
    float.y = Math.round((vh - float.h) / 2)
    ball.x = vw - BALL_SIZE - BALL_MARGIN
    ball.y = vh - BALL_SIZE - 120
  }
  clampAll()
}

/** 视口变化/加载后把几何收敛回屏幕内，避免面板跑到视野外 */
function clampAll() {
  const vw = window.innerWidth
  const vh = window.innerHeight
  float.w = Math.min(float.w, vw - 16)
  float.h = Math.min(float.h, vh - 16)
  float.x = Math.min(Math.max(float.x, 0), vw - float.w)
  float.y = Math.min(Math.max(float.y, 0), vh - float.h)
  // 挂靠占满边，长度（厚度）只需限上下限
  const span = dockAxis.value === 'y' ? vh : vw
  dock.length = Math.min(Math.max(dock.length, MIN_LEN), span - 60)
  ball.x = Math.min(Math.max(ball.x, 4), vw - BALL_SIZE - 4)
  ball.y = Math.min(Math.max(ball.y, 64), vh - BALL_SIZE - 16)
}

/* ---------------- 拖拽体系（球 / 面板头 / 把手共用） ---------------- */
let dragBase = null
let dragStart = null
let ballMoved = false

function bindDrag() {
  window.addEventListener('pointermove', onDragMove)
  window.addEventListener('pointerup', onDragUp, { once: true })
  document.body.classList.add('is-assistant-dragging')
}

function unbindDrag() {
  window.removeEventListener('pointermove', onDragMove)
  document.body.classList.remove('is-assistant-dragging')
}

function onBallDown(e) {
  if (!e.isPrimary) return
  e.preventDefault()
  dragging.kind = 'ball'
  ballMoved = false
  dragStart = { px: e.clientX, py: e.clientY, bx: ball.x, by: ball.y }
  bindDrag()
}

function onHeadDown(e) {
  if (!e.isPrimary || !store.open) return
  e.preventDefault()
  dragging.kind = 'move'
  dragBase = { ...float, offset: dock.offset }
  dragStart = { px: e.clientX, py: e.clientY }
  bindDrag()
}

function onRzDown(e, dir) {
  if (!e.isPrimary) return
  e.preventDefault()
  e.stopPropagation()
  dragging.kind = 'resize'
  dragging.dir = dir
  dragBase = { ...float, offset: dock.offset, length: dock.length }
  dragStart = { px: e.clientX, py: e.clientY }
  bindDrag()
}

function onDragMove(e) {
  const dx = e.clientX - dragStart.px
  const dy = e.clientY - dragStart.py
  if (dragging.kind === 'ball') {
    if (Math.abs(dx) + Math.abs(dy) > 5) ballMoved = true
    ball.x = dragStart.bx + dx
    ball.y = dragStart.by + dy
    return
  }
  if (dragging.kind === 'move') {
    if (mode.value === 'float') {
      float.x = clampNum(dragBase.x + dx, 0, window.innerWidth - float.w)
      float.y = clampNum(dragBase.y + dy, 0, window.innerHeight - float.h)
    } else {
      const span = dockAxis.value === 'y' ? window.innerHeight : window.innerWidth
      dock.offset = clampNum(dragBase.offset + (dockAxis.value === 'y' ? dy : dx), 8, span - dock.length - 8)
    }
    return
  }
  if (dragging.kind === 'resize') {
    if (mode.value === 'float') {
      resizeFloat(dx, dy)
    } else {
      // 挂靠：拉伸内边缘改变厚度，占满边不随主轴偏移变化
      const span = dockAxis.value === 'y' ? window.innerHeight : window.innerWidth
      const delta = dockAxis.value === 'y' ? dx : dy
      const sign = dock.side === 'right' || dock.side === 'bottom' ? -1 : 1
      dock.length = clampNum(dragBase.length + sign * delta, MIN_LEN, span - 60)
    }
  }
}

function resizeFloat(dx, dy) {
  const d = dragging.dir
  const b = dragBase
  let { x, y, w, h } = b
  if (d.includes('e')) w = clampNum(b.w + dx, MIN_W, window.innerWidth - b.x)
  if (d.includes('s')) h = clampNum(b.h + dy, MIN_H, window.innerHeight - b.y)
  if (d.includes('w')) {
    w = clampNum(b.w - dx, MIN_W, b.x + b.w)
    x = b.x + b.w - w
  }
  if (d.includes('n')) {
    h = clampNum(b.h - dy, MIN_H, b.y + b.h)
    y = b.y + b.h - h
  }
  float.x = x
  float.y = y
  float.w = w
  float.h = h
}

function onDragUp() {
  unbindDrag()
  if (dragging.kind === 'ball') {
    if (!ballMoved) {
      store.open = true
    } else {
      // 松手吸附到最近的左/右边缘
      const vw = window.innerWidth
      const cx = ball.x + BALL_SIZE / 2
      ball.x = cx < vw / 2 ? BALL_MARGIN : vw - BALL_SIZE - BALL_MARGIN
      ball.y = clampNum(ball.y, 64, window.innerHeight - BALL_SIZE - 16)
      saveLayout()
    }
  } else if (dragging.kind === 'move') {
    if (mode.value === 'float') trySnapDock()
    saveLayout()
  } else if (dragging.kind === 'resize') {
    saveLayout()
  }
  dragging.kind = null
  dragging.dir = null
}

/** 浮动窗拖出边缘附近时自动转为挂靠 */
function trySnapDock() {
  const vw = window.innerWidth
  const vh = window.innerHeight
  if (float.x < SNAPSHOT_EDGE) toDock('left')
  else if (float.x + float.w > vw - SNAPSHOT_EDGE) toDock('right')
  else if (float.y < SNAPSHOT_EDGE) toDock('top')
  else if (float.y + float.h > vh - SNAPSHOT_EDGE) toDock('bottom')
}

function clampNum(v, min, max) {
  return Math.min(Math.max(v, min), max)
}

/** 指定停靠边的默认厚度（左/右=宽，上/下=高），根据视口尺寸自适应 */
function dockDefault(side) {
  if (side === 'left' || side === 'right') {
    return Math.round(clampNum(Math.round(window.innerWidth * 0.42), 460, 720))
  }
  return Math.round(clampNum(Math.round(window.innerHeight * 0.45), 320, 560))
}

/* ---------------- 模式切换 ---------------- */
function toDock(side) {
  const fromFloat = mode.value === 'float'
  const rect = currentRect()
  mode.value = 'dock'
  dock.side = side
  // 从浮动转挂靠：取原尺寸在交叉轴上的投影并限幅；
  // 在四条挂靠边之间切换：直接用该边的默认厚度
  const span = side === 'left' || side === 'right' ? window.innerWidth : window.innerHeight
  if (fromFloat) {
    const projected = side === 'left' || side === 'right' ? rect.w : rect.h
    dock.length = clampNum(projected, MIN_LEN, span - 60)
  } else {
    dock.length = clampNum(dockDefault(side), MIN_LEN, span - 60)
  }
  dock.offset = 0
  saveLayout()
}

function toFloat() {
  const rect = currentRect()
  mode.value = 'float'
  if (rect) {
    // 从挂靠转出：直接用当前占满视口的矩形作为小窗起点（适当限幅）
    float.w = clampNum(rect.w, MIN_W, window.innerWidth - 16)
    float.h = clampNum(rect.h, MIN_H, window.innerHeight - 16)
    float.x = clampNum(rect.x, 0, window.innerWidth - float.w)
    float.y = clampNum(rect.y, 0, window.innerHeight - float.h)
  } else {
    float.w = clampNum(float.w || 420, MIN_W, window.innerWidth - 16)
    float.h = clampNum(float.h || 560, MIN_H, window.innerHeight - 16)
    float.x = clampNum(float.x, 0, window.innerWidth - float.w)
    float.y = clampNum(float.y, 0, window.innerHeight - float.h)
  }
  saveLayout()
}

function collapse() {
  // 退场动画需要保留最后一次的定位样式
  closedStyleCache = { ...panelStyle.value }
  store.open = false
  saveLayout()
}

watch(
  () => store.open,
  (v) => {
    if (v) mountedOnce.value = true
  },
  { immediate: true }
)

function onWindowResize() {
  clampAll()
}

onMounted(() => {
  loadLayout()
  window.addEventListener('resize', onWindowResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onWindowResize)
  unbindDrag()
})
</script>

<style scoped>
/* ================= 悬浮球（精灵球造型：上橙下白 + 中缝 + 核心键） ================= */
.ai-ball {
  position: fixed;
  z-index: 2000;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  cursor: grab;
  touch-action: none;
  overflow: hidden;
  border: 1px solid var(--oj-border-strong);
  box-shadow:
    0 2px 6px rgba(29, 33, 41, 0.12),
    0 8px 24px rgba(29, 33, 41, 0.16);
  transition:
    transform var(--oj-duration) var(--oj-ease),
    box-shadow var(--oj-duration) var(--oj-ease);
}

.ai-ball:hover {
  transform: scale(1.07);
  box-shadow:
    0 4px 10px rgba(29, 33, 41, 0.14),
    0 12px 32px rgba(29, 33, 41, 0.2);
}

.ai-ball.is-dragging {
  cursor: grabbing;
  transform: scale(1.04);
}

.ball-top,
.ball-bottom {
  position: absolute;
  left: 0;
  right: 0;
  height: 50%;
}

.ball-top {
  top: 0;
  background: linear-gradient(180deg, var(--oj-primary-hover), var(--oj-primary));
}

.ball-bottom {
  bottom: 0;
  background: var(--oj-bg-raised);
}

.ball-band {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 5px;
  transform: translateY(-50%);
  background: #23262d;
}

.ball-core {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 17px;
  height: 17px;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  background: #fff;
  border: 3px solid #23262d;
  box-shadow: inset 0 0 0 2px #fff;
}

/* 流式回答进行中：核心键呼吸提示 */
.ai-ball.is-streaming .ball-core {
  animation: ball-breath 1.2s var(--oj-ease) infinite;
}

@keyframes ball-breath {
  0%,
  100% {
    box-shadow: inset 0 0 0 2px #fff, 0 0 0 0 var(--oj-primary-soft);
  }
  50% {
    box-shadow: inset 0 0 0 2px #fff, 0 0 0 8px transparent;
    background: var(--oj-primary);
  }
}

/* ================= 面板 ================= */
.ai-panel {
  position: fixed;
  z-index: 2000;
  display: flex;
  flex-direction: column;
  background: var(--oj-bg-raised);
  border: 1px solid var(--oj-border);
  box-shadow: var(--oj-shadow-pop);
  overflow: hidden;
  transition:
    opacity var(--oj-duration) var(--oj-ease),
    transform var(--oj-duration) var(--oj-ease),
    visibility 0s;
}

.ai-panel.is-collapsed {
  opacity: 0;
  transform: scale(0.94);
  visibility: hidden;
  pointer-events: none;
  transition:
    opacity var(--oj-duration) var(--oj-ease),
    transform var(--oj-duration) var(--oj-ease),
    visibility 0s var(--oj-duration);
}

.is-float {
  border-radius: var(--oj-radius-xl);
  transform-origin: 100% 100%;
}

.is-dock {
  border-radius: 0;
}

.dock-left {
  border-right-width: 1px;
  border-left: none;
  border-radius: 0 var(--oj-radius-lg) var(--oj-radius-lg) 0;
  transform-origin: 0 50%;
}

.dock-right {
  border-left-width: 1px;
  border-right: none;
  border-radius: var(--oj-radius-lg) 0 0 var(--oj-radius-lg);
  transform-origin: 100% 50%;
}

.dock-top {
  border-bottom-width: 1px;
  border-top: none;
  border-radius: 0 0 var(--oj-radius-lg) var(--oj-radius-lg);
  transform-origin: 50% 0;
}

.dock-bottom {
  border-top-width: 1px;
  border-bottom: none;
  border-radius: var(--oj-radius-lg) var(--oj-radius-lg) 0 0;
  transform-origin: 50% 100%;
}

/* ---------- 面板头（拖动手柄） ---------- */
.panel-head {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 44px;
  padding: 0 8px 0 14px;
  flex-shrink: 0;
  cursor: grab;
  touch-action: none;
  user-select: none;
  border-bottom: 1px solid var(--oj-divider);
  background: var(--oj-bg-raised);
}

.mini-ball {
  position: relative;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid var(--oj-border-strong);
}

.mini-top {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 50%;
  background: var(--oj-primary);
}

.mini-band {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 2px;
  transform: translateY(-50%);
  background: #23262d;
}

.mini-core {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  background: #fff;
  border: 1px solid #23262d;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--oj-text-primary);
  white-space: nowrap;
}

.panel-chip {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--oj-text-secondary);
  background: var(--oj-bg-fill);
  border-radius: var(--oj-radius-sm);
  padding: 2px 8px;
}

.panel-space {
  flex: 1;
}

.head-tools {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.tool-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: none;
  border-radius: var(--oj-radius);
  background: transparent;
  color: var(--oj-text-secondary);
  cursor: pointer;
  transition:
    background-color var(--oj-duration-fast) var(--oj-ease),
    color var(--oj-duration-fast) var(--oj-ease);
}

.tool-btn:hover {
  background: var(--oj-bg-fill);
  color: var(--oj-text-primary);
}

.tool-btn.is-active {
  background: var(--oj-primary-soft);
  color: var(--oj-primary-active);
}

html[data-theme='dark'] .tool-btn.is-active {
  color: var(--oj-primary);
}

.tool-sep {
  width: 1px;
  height: 16px;
  margin: 0 4px;
  background: var(--oj-divider);
}

/* 挂靠/浮动模式的小图形示意：12px 圆角框 + 高亮边（表示停靠侧） */
.shape {
  position: relative;
  width: 13px;
  height: 13px;
  border: 1px solid currentColor;
  border-radius: 3px;
  opacity: 0.9;
}

.shape::after {
  content: '';
  position: absolute;
  background: currentColor;
  border-radius: 1px;
}

.shape-left::after {
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  border-radius: 3px 0 0 3px;
}

.shape-right::after {
  right: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  border-radius: 0 3px 3px 0;
}

.shape-top::after {
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  border-radius: 3px 3px 0 0;
}

.shape-bottom::after {
  bottom: 0;
  left: 0;
  right: 0;
  height: 4px;
  border-radius: 0 0 3px 3px;
}

.shape-float::after {
  inset: 3px;
  border-radius: 2px;
}

/* ---------- 拉伸把手 ---------- */
.rz {
  position: absolute;
  z-index: 1;
}

.rz-n {
  top: -3px;
  left: 10px;
  right: 10px;
  height: 7px;
  cursor: ns-resize;
}

.rz-s {
  bottom: -3px;
  left: 10px;
  right: 10px;
  height: 7px;
  cursor: ns-resize;
}

.rz-e {
  right: -3px;
  top: 10px;
  bottom: 10px;
  width: 7px;
  cursor: ew-resize;
}

.rz-w {
  left: -3px;
  top: 10px;
  bottom: 10px;
  width: 7px;
  cursor: ew-resize;
}

.rz-ne {
  top: -3px;
  right: -3px;
  width: 13px;
  height: 13px;
  cursor: nesw-resize;
}

.rz-nw {
  top: -3px;
  left: -3px;
  width: 13px;
  height: 13px;
  cursor: nwse-resize;
}

.rz-se {
  bottom: -3px;
  right: -3px;
  width: 13px;
  height: 13px;
  cursor: nwse-resize;
}

.rz-sw {
  bottom: -3px;
  left: -3px;
  width: 13px;
  height: 13px;
  cursor: nesw-resize;
}

/* 把手中央的可见抓取条：默认低调，悬停面板时显现 */
.rz-edge::after {
  content: '';
  position: absolute;
  background: var(--oj-border-strong);
  border-radius: 2px;
  opacity: 0;
  transition: opacity var(--oj-duration-fast) var(--oj-ease);
}

.rz-s.rz-edge::after {
  left: 50%;
  bottom: 4px;
  width: 36px;
  height: 4px;
  transform: translateX(-50%);
}

.rz-e.rz-edge::after {
  top: 50%;
  right: 4px;
  width: 4px;
  height: 36px;
  transform: translateY(-50%);
}

.ai-panel:hover .rz-edge::after {
  opacity: 0.8;
}

@media (prefers-reduced-motion: reduce) {
  .ai-ball.is-streaming .ball-core {
    animation: none;
  }

  .ai-panel {
    transition: none;
  }
}
</style>
