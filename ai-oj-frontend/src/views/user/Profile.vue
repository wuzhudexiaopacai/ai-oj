<template>
  <div class="oj-page">
    <div class="oj-container">
      <div class="oj-page-header">
        <h2 class="oj-page-title">个人中心</h2>
      </div>

      <div class="profile-grid">
        <div class="oj-card info-card">
          <div class="avatar-row">
            <el-upload
              class="avatar-upload"
              :show-file-list="false"
              accept="image/jpeg,image/png,image/gif,image/webp"
              :before-upload="beforeAvatarUpload"
              :http-request="doUploadAvatar"
              :disabled="uploadingAvatar"
            >
              <el-avatar :size="72" :src="profileForm.avatar">
                {{ userStore.nickname.charAt(0).toUpperCase() }}
              </el-avatar>
              <div v-loading="uploadingAvatar" class="avatar-mask">更换头像</div>
            </el-upload>
            <div>
              <div class="nickname">{{ userStore.nickname }}</div>
              <div class="oj-muted">@{{ userStore.userInfo?.username }}</div>
              <div class="oj-muted avatar-tip">支持 JPG / PNG / GIF / WEBP，不超过 2MB</div>
            </div>
          </div>
          <el-descriptions :column="1" border class="info-desc">
            <el-descriptions-item label="邮箱">{{ userStore.userInfo?.email || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="userStore.userInfo?.createTime" label="注册时间">
              {{ formatTime(userStore.userInfo.createTime, 'YYYY-MM-DD') }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="oj-card">
          <h3 class="section-title">修改资料</h3>
          <el-form :model="profileForm" label-width="80px" style="max-width: 420px">
            <el-form-item label="昵称">
              <el-input v-model="profileForm.nickname" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" />
            </el-form-item>
            <el-form-item label="个人简介">
              <el-input v-model="profileForm.intro" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingProfile" @click="onSaveProfile">保存</el-button>
            </el-form-item>
          </el-form>

          <el-divider />

          <h3 class="section-title">修改密码</h3>
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px" style="max-width: 420px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingPwd" @click="onSavePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { updateProfile, updatePassword } from '@/api/modules/auth'
import { uploadAvatar } from '@/api/modules/file'
import { formatTime } from '@/utils/format'

const userStore = useUserStore()

/** 头像大小上限：2MB */
const AVATAR_MAX_SIZE = 2 * 1024 * 1024
/** 头像允许的图片类型 */
const AVATAR_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']

const profileForm = reactive({
  nickname: userStore.userInfo?.nickname || '',
  email: userStore.userInfo?.email || '',
  avatar: userStore.userInfo?.avatar || '',
  // 后端字段名为 intro（个人简介），不是 bio
  intro: userStore.userInfo?.intro || ''
})

const uploadingAvatar = ref(false)

const pwdFormRef = ref(null)
const pwdForm = reactive({ oldPassword: '', newPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

const savingProfile = ref(false)
const savingPwd = ref(false)

/** 上传前校验：只允许常见图片格式且不超过 2MB */
function beforeAvatarUpload(file) {
  if (!AVATAR_TYPES.includes(file.type)) {
    ElMessage.error('仅支持 JPG / PNG / GIF / WEBP 格式的图片')
    return false
  }
  if (file.size > AVATAR_MAX_SIZE) {
    ElMessage.error('头像大小不能超过 2MB')
    return false
  }
  return true
}

/** 上传头像：先存文件服务，再把返回地址写入个人资料 */
async function doUploadAvatar({ file }) {
  uploadingAvatar.value = true
  try {
    profileForm.avatar = await uploadAvatar(file)
    await updateProfile(profileForm)
    await userStore.fetchUserInfo().catch(() => {})
    ElMessage.success('头像已更新')
  } finally {
    uploadingAvatar.value = false
  }
}

async function onSaveProfile() {
  savingProfile.value = true
  try {
    await updateProfile(profileForm)
    await userStore.fetchUserInfo().catch(() => {})
    ElMessage.success('资料已更新')
  } finally {
    savingProfile.value = false
  }
}

async function onSavePassword() {
  await pwdFormRef.value.validate()
  savingPwd.value = true
  try {
    await updatePassword(pwdForm)
    ElMessage.success('密码已修改，请重新登录')
    userStore.logout()
    window.location.href = '/login'
  } finally {
    savingPwd.value = false
  }
}

onMounted(() => {
  userStore.fetchUserInfo().then((info) => {
    profileForm.nickname = info.nickname || ''
    profileForm.email = info.email || ''
    profileForm.avatar = info.avatar || ''
    profileForm.intro = info.intro || ''
  }).catch(() => {})
})
</script>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
}

@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

/* 头像上传：悬浮显示「更换头像」遮罩 */
.avatar-upload {
  position: relative;
  display: inline-flex;
  border-radius: 50%;
  cursor: pointer;
}

.avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-upload:hover .avatar-mask {
  opacity: 1;
}

.avatar-tip {
  margin-top: 4px;
  font-size: 12px;
}

.nickname {
  font-size: 18px;
  font-weight: 600;
}

.section-title {
  margin: 0 0 16px;
  font-size: 16px;
}
</style>
