<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside :width="collapsed ? '64px' : '220px'" class="admin-aside">
      <div class="aside-logo" @click="$router.push('/admin')">
        <img src="/favicon.svg" alt="AI-OJ" class="logo-icon" />
        <span v-show="!collapsed" class="logo-text">管理后台</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        class="aside-menu"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>总览</template>
        </el-menu-item>
        <el-menu-item index="/admin/problem">
          <el-icon><Collection /></el-icon>
          <template #title>题目管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/contest">
          <el-icon><Trophy /></el-icon>
          <template #title>比赛管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/user">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/role">
          <el-icon><Key /></el-icon>
          <template #title>角色权限</template>
        </el-menu-item>
      </el-menu>

      <div class="aside-footer" @click="collapsed = !collapsed">
        <el-icon>
          <Expand v-if="collapsed" />
          <Fold v-else />
        </el-icon>
      </div>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="admin-header" height="56px">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">管理后台</el-breadcrumb-item>
          <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="header-right">
          <el-tooltip :content="isDark ? '切换到明亮模式' : '切换到暗黑模式'" placement="bottom">
            <el-button circle text @click="themeStore.toggle()">
              <el-icon :size="18">
                <Sunny v-if="isDark" />
                <Moon v-else />
              </el-icon>
            </el-button>
          </el-tooltip>

          <el-button text @click="$router.push('/')">
            <el-icon><HomeFilled /></el-icon>
            <span class="back-text">返回前台</span>
          </el-button>

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
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="admin-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Odometer, Collection, Trophy, User, Key, Sunny, Moon,
  ArrowDown, SwitchButton, HomeFilled, Expand, Fold
} from '@element-plus/icons-vue'
import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const themeStore = useThemeStore()
const userStore = useUserStore()

const collapsed = ref(false)
const isDark = computed(() => themeStore.isDark)

// 高亮当前菜单（编辑页等深层路由归到所属列表菜单）
const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/admin/problem')) return '/admin/problem'
  if (path.startsWith('/admin/contest')) return '/admin/contest'
  if (path.startsWith('/admin/user')) return '/admin/user'
  if (path.startsWith('/admin/role')) return '/admin/role'
  return '/admin/dashboard'
})

function onUserCommand(command) {
  if (command === 'profile') {
    router.push('/user/profile')
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' }).then(() => {
      userStore.logout()
      router.push('/login')
    }).catch(() => {})
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.admin-aside {
  background: var(--oj-bg-raised);
  border-right: 1px solid var(--oj-border);
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease;
  position: sticky;
  top: 0;
  height: 100vh;
}

.aside-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-bottom: 1px solid var(--oj-border);
  cursor: pointer;
  flex-shrink: 0;
}

.logo-icon {
  width: 26px;
  height: 26px;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: var(--oj-primary);
  white-space: nowrap;
}

.aside-menu {
  flex: 1;
  border-right: none;
  background: transparent;
}

.aside-menu:not(.el-menu--collapse) {
  width: 100%;
}

.aside-menu :deep(.el-menu-item) {
  background: transparent;
}

.aside-footer {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid var(--oj-border);
  cursor: pointer;
  color: var(--oj-text-regular);
  flex-shrink: 0;
}

.aside-footer:hover {
  color: var(--oj-primary);
}

.admin-header {
  background: var(--oj-bg-raised);
  border-bottom: 1px solid var(--oj-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.back-text {
  margin-left: 4px;
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

.admin-main {
  padding: 20px;
  background: var(--oj-bg);
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
