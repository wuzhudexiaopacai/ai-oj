import { defineStore } from 'pinia'

const STORAGE_KEY = 'ai-oj-theme'

/**
 * 力扣风格双主题：light / dark
 * 同时维护 html[data-theme]（自定义变量）与 html.dark（Element Plus 暗黑）
 */
export const useThemeStore = defineStore('theme', {
  state: () => ({
    theme: localStorage.getItem(STORAGE_KEY) || 'light'
  }),
  getters: {
    isDark: (state) => state.theme === 'dark'
  },
  actions: {
    apply() {
      const el = document.documentElement
      el.setAttribute('data-theme', this.theme)
      el.classList.toggle('dark', this.isDark)
      localStorage.setItem(STORAGE_KEY, this.theme)
    },
    toggle() {
      this.theme = this.isDark ? 'light' : 'dark'
      this.apply()
    }
  }
})
