import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 统一 axios 实例：
 * - baseURL '/api'，经 vite proxy / nginx 转发至网关 ai-oj-gateway:8080
 * - 请求头自动携带 Authorization: Bearer <token>
 * - 响应统一解包 Result{code,message,data}，业务失败抛错并提示
 *
 * 注意：本模块处于依赖链底层（stores/user → api/modules → request），
 * 严禁顶层 import router 或 store，否则形成循环依赖导致打包后
 * "Cannot access before initialization"。token 直接读 localStorage，
 * router 在使用处动态 import。
 */
const TOKEN_KEY = 'ai-oj-token'

const request = axios.create({
  baseURL: '/api',
  timeout: 20000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    // 非统一返回体（如文件流）直接透传
    if (body == null || typeof body.code === 'undefined') {
      return body
    }
    if (body.code === 200) {
      return body.data
    }
    if (body.code === 401) {
      handleUnauthorized()
      return Promise.reject(new Error(body.message || '未登录或登录已过期'))
    }
    if (!response.config?.silent) {
      ElMessage.error(body.message || '请求失败')
    }
    return Promise.reject(new Error(body.message || '请求失败'))
  },
  (error) => {
    if (error.response?.status === 401) {
      handleUnauthorized()
    } else if (!error.config?.silent) {
      // silent：探测类请求（服务健康检查等）不弹全局错误提示
      ElMessage.error(error.response?.data?.message || error.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

let redirecting = false
async function handleUnauthorized() {
  // 直接清理本地会话，避免引入 user store 造成循环依赖
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('ai-oj-user')
  // router 动态引入：本模块被 stores/user.js 间接依赖，顶层引入会成环
  const { default: router } = await import('@/router')
  if (!redirecting && router.currentRoute.value.name !== 'login') {
    redirecting = true
    ElMessage.warning('请先登录')
    router
      .replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
      .finally(() => {
        redirecting = false
      })
  }
}

export default request
