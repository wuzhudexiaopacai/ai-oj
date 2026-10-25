import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi, getUserInfo } from '@/api/modules/auth'

const TOKEN_KEY = 'ai-oj-token'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: JSON.parse(localStorage.getItem('ai-oj-user') || 'null')
  }),
  getters: {
    isLogin: (state) => !!state.token,
    nickname: (state) => state.userInfo?.nickname || state.userInfo?.username || '',
    /** 是否管理员（角色含 ROLE_ADMIN 即视为管理员） */
    isAdmin: (state) => (state.userInfo?.roles || []).includes('ROLE_ADMIN'),
    /** 权限点集合 */
    permissions: (state) => new Set(state.userInfo?.permissions || [])
  },
  actions: {
    setSession(token, user) {
      this.token = token
      this.userInfo = user || null
      localStorage.setItem(TOKEN_KEY, token)
      if (user) {
        localStorage.setItem('ai-oj-user', JSON.stringify(user))
      }
    },
    async login(payload) {
      const data = await loginApi(payload)
      // 登录返回 { token, userInfo } 结构
      this.setSession(data.token, data.userInfo || data.user)
      return data
    },
    async register(payload) {
      return registerApi(payload)
    },
    async fetchUserInfo() {
      const data = await getUserInfo()
      this.userInfo = data
      localStorage.setItem('ai-oj-user', JSON.stringify(data))
      return data
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem('ai-oj-user')
    }
  }
})
