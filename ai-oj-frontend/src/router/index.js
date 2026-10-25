import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    component: () => import('@/layout/AppLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
      { path: 'problem', name: 'problem-list', component: () => import('@/views/problem/ProblemList.vue'), meta: { title: '题库' } },
      { path: 'problem/:id', name: 'problem-detail', component: () => import('@/views/problem/ProblemDetail.vue'), meta: { title: '题目详情' } },
      { path: 'submission', name: 'submission-list', component: () => import('@/views/submission/SubmissionList.vue'), meta: { title: '提交记录', requiresAuth: true } },
      { path: 'contest', name: 'contest-list', component: () => import('@/views/contest/ContestList.vue'), meta: { title: '比赛' } },
      { path: 'contest/:id', name: 'contest-detail', component: () => import('@/views/contest/ContestDetail.vue'), meta: { title: '比赛详情' } },
      // 比赛做题页：无 AI 助手，带赛内倒计时与返回比赛入口
      { path: 'contest/:contestId/problem/:problemId', name: 'contest-problem', component: () => import('@/views/contest/ContestProblem.vue'), meta: { title: '比赛做题' } },
      {
        path: 'user/profile',
        name: 'user-profile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  },
  { path: '/login', name: 'login', component: () => import('@/views/auth/Login.vue'), meta: { title: '登录' } },
  { path: '/register', name: 'register', component: () => import('@/views/auth/Register.vue'), meta: { title: '注册' } },
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { requiresAdmin: true },
    children: [
      { path: 'dashboard', name: 'admin-dashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '总览', requiresAdmin: true } },
      { path: 'problem', name: 'admin-problem', component: () => import('@/views/admin/ProblemManage.vue'), meta: { title: '题目管理', requiresAdmin: true } },
      { path: 'problem/create', name: 'admin-problem-create', component: () => import('@/views/admin/ProblemEdit.vue'), meta: { title: '新建题目', requiresAdmin: true } },
      { path: 'problem/:id/edit', name: 'admin-problem-edit', component: () => import('@/views/admin/ProblemEdit.vue'), meta: { title: '编辑题目', requiresAdmin: true } },
      { path: 'contest', name: 'admin-contest', component: () => import('@/views/admin/ContestManage.vue'), meta: { title: '比赛管理', requiresAdmin: true } },
      { path: 'user', name: 'admin-user', component: () => import('@/views/admin/UserManage.vue'), meta: { title: '用户管理', requiresAdmin: true } },
      { path: 'role', name: 'admin-role', component: () => import('@/views/admin/RoleManage.vue'), meta: { title: '角色权限', requiresAdmin: true } }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFound.vue'), meta: { title: '页面不存在' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - AI-OJ` : 'AI-OJ 智能在线评测平台'
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLogin) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  // 管理后台守卫：未登录跳登录页，非管理员回首页
  if (to.meta.requiresAdmin) {
    if (!userStore.isLogin) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    if (!userStore.isAdmin) {
      return { name: 'home' }
    }
  }
  return true
})

export default router
