<template>
  <div class="user-manage">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="query.username"
          placeholder="搜索用户名"
          clearable
          style="width: 200px"
          @keyup.enter="load(1)"
          @clear="load(1)"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px" @change="load(1)">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" plain @click="load(1)">查询</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="openEdit()">新建用户</el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="list">
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="30" :src="row.avatar">{{ (row.nickname || row.username || '?').charAt(0).toUpperCase() }}</el-avatar>
              <div class="user-meta">
                <span class="user-name">{{ row.nickname || row.username }}</span>
                <span class="user-username">@{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="邮箱" prop="email" min-width="200" show-overflow-tooltip />
        <el-table-column label="通过 / 提交" width="110" align="center">
          <template #default="{ row }">
            <span class="oj-muted">{{ row.acceptCount ?? 0 }} / {{ row.submitCount ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Rating" prop="rating" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'" effect="light">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime, 'YYYY-MM-DD HH:mm') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该用户？" width="200" @confirm="remove(row)">
              <template #reference>
                <el-button type="danger" text size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无用户" />
        </template>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load()"
          @size-change="load(1)"
        />
      </div>
    </el-card>

    <!-- 新建/编辑用户 -->
    <el-dialog v-model="editVisible" :title="editForm.id ? '编辑用户' : '新建用户'" width="520px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" :disabled="!!editForm.id" placeholder="4-32 位登录名" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" maxlength="64" placeholder="选填" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="必填" />
        </el-form-item>
        <el-form-item :label="editForm.id ? '重置密码' : '初始密码'" prop="password">
          <el-input
            v-model="editForm.password"
            show-password
            :placeholder="editForm.id ? '留空表示不修改密码' : '6-32 位'"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.roleIds" multiple style="width: 100%" placeholder="分配角色">
            <el-option v-for="r in roleOptions" :key="r.id" :label="`${r.name}（${r.code}）`" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { createUser, deleteUser, listAllRoles, pageUsers, updateUser } from '@/api/modules/admin'
import { formatTime } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, username: '', status: null })

async function load(resetPage) {
  if (resetPage) query.pageNum = 1
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.username) params.username = query.username
    if (query.status !== null && query.status !== undefined && query.status !== '') params.status = query.status
    const data = await pageUsers(params)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const roleOptions = ref([])

async function loadRoles() {
  try {
    roleOptions.value = await listAllRoles()
  } catch {
    roleOptions.value = []
  }
}

// ==================== 新建 / 编辑 ====================

const editVisible = ref(false)
const saving = ref(false)
const editFormRef = ref(null)
const editForm = reactive({
  id: null, username: '', nickname: '', email: '', password: '', status: 1, roleIds: []
})

const editRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 32, message: '长度需为 4-32 位', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    {
      validator: (rule, value, cb) => {
        if (!editForm.id && !value) return cb(new Error('请设置初始密码'))
        if (value && (value.length < 6 || value.length > 32)) return cb(new Error('长度需为 6-32 位'))
        cb()
      },
      trigger: 'blur'
    }
  ]
}

function openEdit(row) {
  Object.assign(editForm, {
    id: row?.id ?? null,
    username: row?.username ?? '',
    nickname: row?.nickname ?? '',
    email: row?.email ?? '',
    password: '',
    status: row?.status ?? 1,
    roleIds: row?.roleIds ? [...row.roleIds] : []
  })
  editVisible.value = true
}

async function save() {
  await editFormRef.value.validate().catch(() => Promise.reject())
  saving.value = true
  try {
    const payload = {
      id: editForm.id,
      username: editForm.username,
      nickname: editForm.nickname || null,
      email: editForm.email,
      password: editForm.password || null,
      status: editForm.status,
      roleIds: editForm.roleIds
    }
    if (editForm.id) {
      await updateUser(payload)
      ElMessage.success('用户已更新')
    } else {
      await createUser(payload)
      ElMessage.success('用户已创建')
    }
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await deleteUser(row.id)
  ElMessage.success('已删除')
  if (list.value.length === 1 && query.pageNum > 1) query.pageNum -= 1
  load()
}

onMounted(() => {
  load()
  loadRoles()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.table-card {
  border: 1px solid var(--oj-border);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.user-name {
  font-weight: 500;
  color: var(--oj-text-primary);
}

.user-username {
  font-size: 12px;
  color: var(--oj-text-secondary);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
