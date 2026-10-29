<template>
  <div class="auth-page">
    <!-- 极细点阵纹理，与首页主视觉同一套质感语言 -->
    <div class="auth-texture" aria-hidden="true"></div>
    <div class="oj-card auth-card oj-reveal">
      <div class="auth-logo">
        <img src="/favicon.svg" alt="AI-OJ" class="logo-icon" />
        <span class="logo-text">AI-OJ</span>
      </div>
      <h2 class="auth-title">创建账号</h2>
      <p class="auth-subtitle">注册后即可使用题库评测、提交记录与赛事功能</p>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名（4-20 位字母数字下划线）" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（选填）" :prefix-icon="Postcard" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱（选填）" :prefix-icon="Message" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="密码（至少 6 位）" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="确认密码"
            :prefix-icon="Lock"
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
          注册
        </el-button>
      </el-form>

      <div class="auth-footer oj-muted">
        已有账号？
        <router-link class="link" to="/login">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, Postcard, Key } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getCaptcha } from '@/api/modules/auth'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const form = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: '',
  captchaKey: '',
  captchaCode: ''
})
const captchaImage = ref('')

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^\w{4,20}$/, message: '4-20 位字母、数字或下划线', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (r, v, cb) => (v === form.password ? cb() : cb(new Error('两次输入的密码不一致'))),
      trigger: 'blur'
    }
  ],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

/** 加载图形验证码 */
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
    const { confirmPassword, ...payload } = form
    await userStore.register(payload)
    ElMessage.success('注册成功，请登录')
    router.replace('/login')
  } catch {
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
  max-width: 420px;
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
