<template>
  <div class="auth-page">
    <!-- 极细点阵纹理，与首页主视觉同一套质感语言 -->
    <div class="auth-texture" aria-hidden="true"></div>
    <div class="oj-card auth-card oj-reveal">
      <div class="auth-logo">
        <img src="/favicon.svg" alt="AI-OJ" class="logo-icon" />
        <span class="logo-text">AI-OJ</span>
      </div>
      <h2 class="auth-title">登录在线评测平台</h2>
      <p class="auth-subtitle">使用已有账号访问题库、提交代码与参与赛事</p>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="密码"
            :prefix-icon="Lock"
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input
              v-model="form.captchaCode"
              placeholder="验证码"
              :prefix-icon="Key"
              @keyup.enter="onSubmit"
            />
            <img
              v-if="captchaImage"
              :src="captchaImage"
              class="captcha-img"
              alt="验证码"
              title="点击刷新"
              @click="refreshCaptcha"
            />
          </div>
        </el-form-item>
        <el-button type="primary" class="submit-btn" :loading="loading" @click="onSubmit">
          登录
        </el-button>
      </el-form>

      <div class="auth-footer oj-muted">
        还没有账号？
        <router-link class="link" to="/register">注册新账号</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getCaptcha } from '@/api/modules/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '', captchaKey: '', captchaCode: '' })
const captchaImage = ref('')

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

/** 加载图形验证码（登录/注册共用 captcha 接口） */
async function refreshCaptcha() {
  const data = await getCaptcha()
  form.captchaKey = data.captchaKey
  form.captchaCode = ''
  captchaImage.value = data.img
}

onMounted(refreshCaptcha)

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.replace(route.query.redirect || '/')
  } catch {
    // 登录失败（验证码错误/密码错误）后自动刷新验证码
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  /* 纯色底：去掉廉价浅渐变，纹理由独立伪层承载 */
  background: var(--oj-bg-page);
}

.auth-texture {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(var(--oj-border) 1px, transparent 1px);
  background-size: 26px 26px;
  opacity: 0.3;
  -webkit-mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.5), transparent 70%);
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.5), transparent 70%);
  pointer-events: none;
}

.auth-card {
  position: relative;
  width: 100%;
  max-width: 400px;
  padding: 40px 36px 32px;
  border-radius: var(--oj-radius-xl);
  box-shadow: var(--oj-shadow-pop);
  border-color: var(--oj-border);
}

.auth-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.logo-icon {
  width: 32px;
  height: 32px;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: var(--oj-text-primary);
  letter-spacing: 0.3px;
}

.auth-title {
  text-align: center;
  margin: 16px 0 6px;
  font-size: 18px;
  font-weight: 600;
  color: var(--oj-text-primary);
}

/* 页头副题：补充场景说明，拉开标题/说明层级 */
.auth-subtitle {
  text-align: center;
  margin: 0 0 26px;
  font-size: 13px;
  color: var(--oj-text-secondary);
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  height: 42px;
  font-size: 15px;
}

.captcha-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.captcha-row .el-input {
  flex: 1;
}

.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 4px;
  cursor: pointer;
  flex-shrink: 0;
  border: 1px solid var(--oj-border);
}

.auth-footer {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid var(--oj-divider);
  text-align: center;
  font-size: 13px;
}

.link {
  color: var(--oj-primary);
  font-weight: 500;
}
</style>
