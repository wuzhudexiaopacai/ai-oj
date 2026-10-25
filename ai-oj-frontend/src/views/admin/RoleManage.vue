<template>
  <div class="role-manage">
    <el-tabs v-model="activeTab" class="page-tabs">
      <!-- ==================== 角色列表 ==================== -->
      <el-tab-pane label="角色管理" name="role">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input
              v-model="query.keyword"
              placeholder="搜索角色编码 / 名称"
              clearable
              style="width: 220px"
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
          <el-button type="primary" :icon="Plus" @click="openEdit()">新建角色</el-button>
        </div>

        <el-card shadow="never" class="table-card">
          <el-table v-loading="loading" :data="list">
            <el-table-column label="ID" prop="id" width="70" />
            <el-table-column label="角色编码" prop="code" width="180">
              <template #default="{ row }">
                <el-tag effect="plain" size="small">{{ row.code }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="角色名称" prop="name" min-width="140" />
            <el-table-column label="描述" prop="description" min-width="220" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'" effect="light">
                  {{ row.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="160">
              <template #default="{ row }">{{ formatTime(row.createTime, 'YYYY-MM-DD HH:mm') }}</template>
            </el-table-column>
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" text size="small" @click="openPermission(row)">权限配置</el-button>
                <el-button type="primary" text size="small" @click="openEdit(row)">编辑</el-button>
                <el-popconfirm title="确定删除该角色？" width="200" @confirm="remove(row)">
                  <template #reference>
                    <el-button type="danger" text size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无角色" />
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
      </el-tab-pane>

      <!-- ==================== 权限节点管理 ==================== -->
      <el-tab-pane label="权限节点" name="permission">
        <div class="toolbar">
          <span class="perm-tip">权限节点用于后端接口鉴权（@PreAuthorize）与前端按钮级控制</span>
          <el-button type="primary" :icon="Plus" @click="openPermEdit()">新增根节点</el-button>
        </div>
        <el-card shadow="never" class="table-card">
          <el-table
            v-loading="permLoading"
            :data="permTree"
            row-key="id"
            default-expand-all
            :tree-props="{ children: 'children' }"
          >
            <el-table-column label="权限名称" prop="name" min-width="200" />
            <el-table-column label="权限标识" prop="code" min-width="200">
              <template #default="{ row }">
                <el-tag effect="plain" size="small">{{ row.code }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="permTypeOf(row.type).type" effect="light">
                  {{ permTypeOf(row.type).desc }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="路径 / 方法" min-width="200">
              <template #default="{ row }">
                <span v-if="row.path" class="oj-muted mono-text">
                  {{ row.method ? `[${row.method}] ` : '' }}{{ row.path }}
                </span>
                <span v-else class="oj-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column label="排序" prop="sort" width="70" align="center" />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'" effect="light">
                  {{ row.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" text size="small" @click="openPermEdit(null, row)">添加子节点</el-button>
                <el-button type="primary" text size="small" @click="openPermEdit(row)">编辑</el-button>
                <el-popconfirm title="删除将同时移除其子节点，确定？" width="220" @confirm="removePerm(row)">
                  <template #reference>
                    <el-button type="danger" text size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 角色编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editForm.id ? '编辑角色' : '新建角色'" width="480px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="90px">
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="editForm.code" :disabled="!!editForm.id" placeholder="如 ROLE_TEACHER" />
        </el-form-item>
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="editForm.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" maxlength="255" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 权限配置抽屉 -->
    <el-drawer v-model="permVisible" :title="`权限配置 — ${currentRole?.name || ''}`" size="420px">
      <div class="perm-panel">
        <el-alert
          type="info"
          :closable="false"
          title="勾选后点击保存生效；父节点半选时其下已选子节点也会一并保存"
          class="perm-alert"
        />
        <el-tree
          ref="permTreeRef"
          :data="permTree"
          show-checkbox
          node-key="id"
          :props="{ label: 'name', children: 'children' }"
          default-expand-all
        >
          <template #default="{ data }">
            <span class="tree-node">
              <span>{{ data.name }}</span>
              <span class="tree-code">{{ data.code }}</span>
            </span>
          </template>
        </el-tree>
      </div>
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPerm" @click="savePermission">保存</el-button>
      </template>
    </el-drawer>

    <!-- 权限节点编辑弹窗 -->
    <el-dialog v-model="permEditVisible" :title="permForm.id ? '编辑权限节点' : '新增权限节点'" width="520px" destroy-on-close>
      <el-form ref="permFormRef" :model="permForm" :rules="permRules" label-width="90px">
        <el-form-item label="父节点">
          <el-tree-select
            v-model="permForm.parentId"
            :data="permTreeSelectData"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            :render-after-expand="false"
            default-expand-all
            style="width: 100%"
            placeholder="不选则为根节点"
            clearable
          />
        </el-form-item>
        <el-form-item label="权限标识" prop="code">
          <el-input v-model="permForm.code" placeholder="如 problem:create" />
        </el-form-item>
        <el-form-item label="权限名称" prop="name">
          <el-input v-model="permForm.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="permForm.type">
            <el-radio-button :value="1">菜单</el-radio-button>
            <el-radio-button :value="2">按钮</el-radio-button>
            <el-radio-button :value="3">API</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="路由路径">
          <el-input v-model="permForm.path" placeholder="选填，如 /admin/problem" />
        </el-form-item>
        <el-form-item v-if="permForm.type === 3" label="HTTP 方法">
          <el-select v-model="permForm.method" clearable style="width: 160px">
            <el-option v-for="m in ['GET', 'POST', 'PUT', 'DELETE']" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="permForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="permForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="permEditVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPermNode" @click="savePermNode">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import {
  createPermission, createRole, deletePermission, deleteRole,
  getPermissionTree, getRoleDetail, pageRoles,
  updatePermission, updateRole
} from '@/api/modules/admin'
import { formatTime } from '@/utils/format'

const activeTab = ref('role')

// ==================== 角色列表 ====================

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: null })

async function load(resetPage) {
  if (resetPage) query.pageNum = 1
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== null && query.status !== undefined && query.status !== '') params.status = query.status
    const data = await pageRoles(params)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const editVisible = ref(false)
const saving = ref(false)
const editFormRef = ref(null)
const editForm = reactive({ id: null, code: '', name: '', description: '', status: 1 })

const editRules = {
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

function openEdit(row) {
  Object.assign(editForm, {
    id: row?.id ?? null,
    code: row?.code ?? '',
    name: row?.name ?? '',
    description: row?.description ?? '',
    status: row?.status ?? 1
  })
  editVisible.value = true
}

async function save() {
  await editFormRef.value.validate().catch(() => Promise.reject())
  saving.value = true
  try {
    const payload = { ...editForm, permissionIds: null }
    if (editForm.id) {
      await updateRole(payload)
      ElMessage.success('角色已更新')
    } else {
      await createRole(payload)
      ElMessage.success('角色已创建')
    }
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await deleteRole(row.id)
  ElMessage.success('已删除')
  if (list.value.length === 1 && query.pageNum > 1) query.pageNum -= 1
  load()
}

// ==================== 权限树 ====================

const permTree = ref([])
const permLoading = ref(false)

async function loadPermTree() {
  permLoading.value = true
  try {
    permTree.value = await getPermissionTree()
  } finally {
    permLoading.value = false
  }
}

const PERM_TYPES = {
  1: { desc: '菜单', type: 'primary' },
  2: { desc: '按钮', type: 'warning' },
  3: { desc: 'API', type: 'success' }
}

function permTypeOf(type) {
  return PERM_TYPES[type] || { desc: '未知', type: 'info' }
}

// 权限配置（角色 ↔ 权限绑定）
const permVisible = ref(false)
const savingPerm = ref(false)
const currentRole = ref(null)
const permTreeRef = ref(null)

async function openPermission(row) {
  currentRole.value = row
  permVisible.value = true
  if (!permTree.value.length) await loadPermTree()
  const detail = await getRoleDetail(row.id)
  // 仅回显叶子勾选，父节点由 el-tree 自动推导半选/全选
  const leafIds = collectLeafIds(permTree.value)
  const checked = (detail.permissionIds || []).filter((id) => leafIds.has(id))
  permTreeRef.value?.setCheckedKeys(checked)
}

function collectLeafIds(nodes, set = new Set()) {
  for (const node of nodes) {
    if (node.children && node.children.length) {
      collectLeafIds(node.children, set)
    } else {
      set.add(node.id)
    }
  }
  return set
}

async function savePermission() {
  savingPerm.value = true
  try {
    const checked = permTreeRef.value.getCheckedKeys()
    const halfChecked = permTreeRef.value.getHalfCheckedKeys()
    await updateRole({
      id: currentRole.value.id,
      code: currentRole.value.code,
      name: currentRole.value.name,
      description: currentRole.value.description,
      status: currentRole.value.status,
      permissionIds: [...checked, ...halfChecked]
    })
    ElMessage.success('权限已保存')
    permVisible.value = false
  } finally {
    savingPerm.value = false
  }
}

// ==================== 权限节点 CRUD ====================

const permEditVisible = ref(false)
const savingPermNode = ref(false)
const permFormRef = ref(null)
const permForm = reactive({
  id: null, parentId: null, code: '', name: '', type: 3, path: '', method: null, sort: 0, status: 1
})

const permRules = {
  code: [{ required: true, message: '请输入权限标识', trigger: 'blur' }],
  name: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

// 树选择器不允许选择自身或后代作为父节点（防环）
const permTreeSelectData = computed(() => {
  if (!permForm.id) return permTree.value
  const exclude = new Set([permForm.id])
  collectSubtreeIds(permTree.value, permForm.id, exclude)
  const prune = (nodes) =>
    nodes
      .filter((n) => !exclude.has(n.id))
      .map((n) => ({ ...n, children: n.children ? prune(n.children) : [] }))
  return prune(permTree.value)
})

function collectSubtreeIds(nodes, targetId, set) {
  for (const node of nodes) {
    if (node.id === targetId) {
      markAll(node, set)
    } else if (node.children) {
      collectSubtreeIds(node.children, targetId, set)
    }
  }
}

function markAll(node, set) {
  set.add(node.id)
  node.children?.forEach((c) => markAll(c, set))
}

function openPermEdit(row, parent) {
  Object.assign(permForm, {
    id: row?.id ?? null,
    parentId: parent?.id ?? row?.parentId ?? null,
    code: row?.code ?? '',
    name: row?.name ?? '',
    type: row?.type ?? 3,
    path: row?.path ?? '',
    method: row?.method ?? null,
    sort: row?.sort ?? 0,
    status: row?.status ?? 1
  })
  if (permForm.parentId === 0) permForm.parentId = null
  permEditVisible.value = true
}

async function savePermNode() {
  await permFormRef.value.validate().catch(() => Promise.reject())
  savingPermNode.value = true
  try {
    const payload = { ...permForm, parentId: permForm.parentId ?? 0 }
    if (permForm.id) {
      await updatePermission(payload)
      ElMessage.success('权限节点已更新')
    } else {
      await createPermission(payload)
      ElMessage.success('权限节点已创建')
    }
    permEditVisible.value = false
    loadPermTree()
  } finally {
    savingPermNode.value = false
  }
}

async function removePerm(row) {
  await deletePermission(row.id)
  ElMessage.success('已删除')
  loadPermTree()
}

onMounted(() => {
  load()
  loadPermTree()
})
</script>

<style scoped>
.page-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

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

.perm-tip {
  font-size: 13px;
  color: var(--oj-text-secondary);
}

.table-card {
  border: 1px solid var(--oj-border);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.perm-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.perm-alert {
  flex-shrink: 0;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tree-code {
  font-size: 12px;
  color: var(--oj-text-secondary);
  font-family: 'JetBrains Mono', Consolas, monospace;
}

.mono-text {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
}
</style>
