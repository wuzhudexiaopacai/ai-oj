<template>
  <el-container class="layout">
    <el-header class="layout-header" :class="{ 'is-scrolled': scrolled }" height="56px">
      <div class="header-inner">
        <router-link to="/" class="logo">
          <img src="/favicon.svg" alt="AI-OJ" class="logo-icon" />
          <span class="logo-text">AI-OJ</span>
        </router-link>

        <el-menu
          :default-active="activeMenu"
          mode="horizontal"
          :ellipsis="false"
          router
          class="nav-menu"
        >
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/problem">题库</el-menu-item>
          <el-menu-item index="/submission">提交记录</el-menu-item>
          <el-menu-item index="/contest">比赛</el-menu-item>
        </el-menu>

        <div class="header-right">
          <el-tooltip :content="isDark ? '切换到明亮模式' : '切换到暗黑模式'" placement="bottom">
            <el-button circle text class="theme-btn" @click="themeStore.toggle()">
              <el-icon :size="18">
                <Sunny v-if="isDark" />
                <Moon v-else />
              </el-icon>
            </el-button>
          </el-tooltip>

          <template v-if="userStore.isLogin">
            <el-dropdown trigger="click" @command="onUserCommand">
              <span class="user-entry">
                <el-avatar :size="30" :src="userStore.userInfo?.avatar">
                  {{ userStore.nickname.charAt(0).toUpperCase() }}
                </el-avatar>
                <span class="user-name">{{ userStore.nickname }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>个人中心
                  </el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" command="admin">
                    <el-icon><Setting /></el-icon>管理后台
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text class="login-btn" @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>

    <el-main class="layout-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>

    <el-footer class="layout-footer" height="56px">
      <div class="oj-container footer-inner">
        <span class="footer-brand">
          <img src="/favicon.svg" alt="" class="footer-logo" />
          AI-OJ 智能在线评测平台
        </span>
        <span class="footer-meta oj-muted">面向编程评测与竞赛场景的在线判题服务</span>
      </div>
    </el-footer>

    <!-- 全局 AI 助手：精灵球悬浮球 + 可停靠面板 -->
    <AiAssistant />
  </el-container>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Sunny, Moon, ArrowDown, User, SwitchButton, Setting } from '@element-plus/icons-vue'
import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import AiAssistant from '@/components/AiAssistant.vue'

const route = useRoute()
const router = useRouter()
const themeStore = useThemeStore()
const userStore = useUserStore()

const isDark = computed(() => themeStore.isDark)

// 吸顶导航滚动状态：页面下滚超过阈值后，顶栏切换为半透明 + 模糊 + 阴影，
// 过渡由 CSS 完成，不监听滚动距离做连续插值，避免重绘开销
const scrolled = ref(false)

function onWindowScroll() {
  scrolled.value = window.scrollY > 4
}

onMounted(() => {
  window.addEventListener('scroll', onWindowScroll, { passive: true })
  onWindowScroll()
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onWindowScroll)
})

// 刷新后本地只剩 token，用户信息（昵称/头像/ID）缺失时补拉一次，
// 否则排行榜「我」标记等依赖 userId 的展示会失效
onMounted(() => {
  if (userStore.isLogin && !userStore.userInfo?.id) {
    userStore.fetchUserInfo().catch(() => {})
  }
})

// 高亮当前一级菜单
const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/problem')) return '/problem'
  if (path.startsWith('/submission')) return '/submission'
  if (path.startsWith('/contest')) return '/contest'
  return '/'
})

function onUserCommand(command) {
  if (command === 'profile') {
    router.push('/user/profile')
  } else if (command === 'admin') {
    router.push('/admin')
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' }).then(() => {
      userStore.logout()
      router.push('/login')
    }).catch(() => {})
  }
}
</script>

<style scoped>
.layout {
  min-height: 100%;
}

.layout-header {
  background: var(--oj-header-bg);
  border-bottom: 1px solid var(--oj-border);
  padding: 0;
  position: sticky;
  top: 0;
  z-index: 100;
  /* 背景/阴影/边框平滑过渡，滚动后“浮现”不突变 */
  transition:
    background-color var(--oj-duration) var(--oj-ease-out),
    box-shadow var(--oj-duration) var(--oj-ease-out),
    border-color var(--oj-duration) var(--oj-ease);
}

/* 滚动后：半透明毛玻璃 + 柔和投影，分隔线隐入阴影 */
.layout-header.is-scrolled {
  background: var(--oj-header-bg-scrolled);
  -webkit-backdrop-filter: blur(10px);
  backdrop-filter: blur(10px);
  box-shadow: var(--oj-shadow);
  border-bottom-color: transparent;
}

.header-inner {
  max-width: 1240px;
  height: 100%;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.logo-icon {
  width: 26px;
  height: 26px;
}

.logo-text {
  font-size: 17px;
  font-weight: 700;
  color: var(--oj-text-primary);
  letter-spacing: 0.3px;
}

.nav-menu {
  flex: 1;
  background: transparent;
  border-bottom: none;
  --el-menu-active-color: var(--oj-primary);
  --el-menu-text-color: var(--oj-text-regular);
  --el-menu-hover-text-color: var(--oj-text-primary);
}

.nav-menu :deep(.el-menu-item) {
  background: transparent;
  font-size: 14px;
  font-weight: 500;
}

.login-btn {
  color: var(--oj-text-regular);
  font-weight: 500;
}

.layout-main {
  padding: 0;
  min-height: calc(100vh - 116px);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.theme-btn {
  color: var(--oj-text-regular);
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--oj-text-primary);
  outline: none;
}

.user-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.layout-footer {
  display: flex;
  align-items: center;
  border-top: 1px solid var(--oj-border);
  background: var(--oj-bg-raised);
  padding: 0;
}

.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--oj-text-regular);
}

.footer-logo {
  width: 18px;
  height: 18px;
}

.footer-meta {
  font-size: 12px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
