<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="16">
      <el-col v-for="card in statCards" :key="card.label" :xs="12" :sm="12" :md="6">
        <div class="oj-card stat-card" @click="$router.push(card.to)">
          <div class="stat-icon" :style="{ background: card.bg }">
            <el-icon :size="24" :color="card.color"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label oj-muted">{{ card.label }}</div>
          </div>
          <el-icon class="stat-arrow"><ArrowRight /></el-icon>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <div class="oj-card section-card">
      <div class="section-title">快捷操作</div>
      <div class="quick-actions">
        <el-button type="primary" :icon="Plus" @click="$router.push('/admin/problem/create')">
          新建题目
        </el-button>
        <el-button :icon="Trophy" @click="createContestVisible = true">创建比赛</el-button>
        <el-button :icon="Upload" @click="$router.push('/admin/problem?import=1')">导入题目</el-button>
        <el-button :icon="User" @click="$router.push('/admin/user')">用户管理</el-button>
      </div>
    </div>

    <!-- 系统状态 -->
    <div class="oj-card section-card">
      <div class="section-head">
        <span class="section-title">服务状态</span>
        <el-button text size="small" :icon="Refresh" :loading="healthLoading" @click="loadHealth">
          刷新
        </el-button>
      </div>
      <div class="health-grid">
        <div v-for="svc in healthList" :key="svc.name" class="health-item">
          <span class="health-dot" :class="svc.ok ? 'ok' : 'down'"></span>
          <span class="health-name">{{ svc.name }}</span>
          <span class="health-status oj-muted">{{ svc.ok ? '正常' : '异常' }}</span>
        </div>
      </div>
    </div>

    <!-- 创建比赛快捷弹窗（简化版，完整管理在比赛管理页） -->
    <el-dialog v-model="createContestVisible" title="快速创建比赛" width="520px">
      <el-form :model="contestForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="contestForm.title" maxlength="128" placeholder="比赛标题" />
        </el-form-item>
        <el-form-item label="赛制">
          <el-radio-group v-model="contestForm.type">
            <el-radio :value="0">ACM</el-radio>
            <el-radio :value="1">OI</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="时间" required>
          <el-date-picker
            v-model="contestRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createContestVisible = false">取消</el-button>
        <el-button type="primary" :loading="contestSaving" @click="quickCreateContest">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  User, Collection, Document, Trophy, ArrowRight, Plus, Upload, Refresh
} from '@element-plus/icons-vue'
import { fetchDashboardStats, createContest } from '@/api/modules/admin'
import request from '@/api/request'

const router = useRouter()

const stats = ref({ userTotal: 0, problemTotal: 0, submissionTotal: 0, contestTotal: 0 })

const statCards = computed(() => [
  {
    label: '注册用户', value: stats.value.userTotal, icon: User, to: '/admin/user',
    color: '#409eff', bg: 'rgba(64, 158, 255, 0.12)'
  },
  {
    label: '题目数量', value: stats.value.problemTotal, icon: Collection, to: '/admin/problem',
    color: '#67c23a', bg: 'rgba(103, 194, 58, 0.12)'
  },
  {
    label: '提交总数', value: stats.value.submissionTotal, icon: Document, to: '/submission',
    color: '#e6a23c', bg: 'rgba(230, 162, 60, 0.12)'
  },
  {
    label: '比赛场次', value: stats.value.contestTotal, icon: Trophy, to: '/admin/contest',
    color: '#f56c6c', bg: 'rgba(245, 108, 108, 0.12)'
  }
])

// ==================== 服务健康检查 ====================

const healthLoading = ref(false)
const healthList = ref([])

const SERVICES = [
  // 注意：request 实例 baseURL 已是 /api，此处必须写相对路径，否则会变成 /api/api/... 404
  { name: '网关', url: '/auth/captcha' },
  { name: '题库服务', url: '/problem/page?pageNum=1&pageSize=1' },
  { name: '判题服务', url: '/submission/page?pageNum=1&pageSize=1' },
  { name: '比赛服务', url: '/contest/page?pageNum=1&pageSize=1' },
  { name: '文件服务', url: '/file/page?pageNum=1&pageSize=1' },
  { name: 'AI 服务', url: '/ai/session/page?pageNum=1&pageSize=1' }
]

async function loadHealth() {
  healthLoading.value = true
  try {
    healthList.value = await Promise.all(
      SERVICES.map(async (s) => {
        try {
          // silent：探测失败不弹全局错误提示
          await request.get(s.url, { timeout: 5000, silent: true })
          return { name: s.name, ok: true }
        } catch (e) {
          const status = e.response?.status
          // 401/403 说明服务在线（仅鉴权未通过）
          if (status === 401 || status === 403) return { name: s.name, ok: true }
          // 无响应（超时/服务未注册到网关）或 404/5xx 均视为异常
          return { name: s.name, ok: false }
        }
      })
    )
  } finally {
    healthLoading.value = false
  }
}

// ==================== 快速创建比赛 ====================

const createContestVisible = ref(false)
const contestSaving = ref(false)
const contestRange = ref([])
const contestForm = reactive({ title: '', type: 0 })

async function quickCreateContest() {
  if (!contestForm.title || contestRange.value?.length !== 2) {
    ElMessage.warning('请填写标题并选择起止时间')
    return
  }
  contestSaving.value = true
  try {
    await createContest({
      title: contestForm.title,
      type: contestForm.type,
      startTime: contestRange.value[0],
      endTime: contestRange.value[1],
      auth: 1
    })
    ElMessage.success('比赛创建成功')
    createContestVisible.value = false
    contestForm.title = ''
    contestRange.value = []
    router.push('/admin/contest')
  } finally {
    contestSaving.value = false
  }
}

onMounted(async () => {
  try {
    stats.value = await fetchDashboardStats()
  } catch { /* 提示由拦截器统一处理 */ }
  loadHealth()
})
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
  margin-bottom: 16px;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  margin-top: 2px;
}

.stat-arrow {
  color: var(--oj-text-placeholder, #c0c4cc);
}

.section-card {
  padding: 18px 20px;
  margin-bottom: 16px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.section-title,
.section-head .section-title {
  font-size: 15px;
  font-weight: 600;
}

.section-title {
  display: block;
  margin-bottom: 14px;
}

.quick-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.health-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 10px;
}

.health-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid var(--oj-border);
  border-radius: 8px;
  font-size: 13px;
}

.health-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.health-dot.ok {
  background: var(--oj-success, #67c23a);
  box-shadow: 0 0 6px rgba(103, 194, 58, 0.6);
}

.health-dot.down {
  background: var(--oj-danger, #f56c6c);
}

.health-name {
  flex: 1;
}

.health-status {
  font-size: 12px;
}
</style>
