import request from '../request'

/**
 * 管理端 API（需管理员权限）
 * 覆盖：总览统计 / 题目 / 比赛 / 用户 / 角色 / 权限
 */

// ==================== 总览 ====================

/** Dashboard 统计：聚合各服务分页接口的 total（避免新增后端接口） */
export async function fetchDashboardStats() {
  // allSettled + silent：单个服务不可用不应导致整页统计失败或弹出错误提示
  const [userPage, problemPage, submissionPage, contestPage] = await Promise.allSettled([
    request.get('/admin/user/page', { params: { pageNum: 1, pageSize: 1 }, silent: true }),
    request.get('/problem/page', { params: { pageNum: 1, pageSize: 1 }, silent: true }),
    request.get('/submission/page', { params: { pageNum: 1, pageSize: 1 }, silent: true }),
    request.get('/contest/page', { params: { pageNum: 1, pageSize: 1 }, silent: true })
  ])
  const total = (r) => (r.status === 'fulfilled' ? r.value?.total || 0 : 0)
  return {
    userTotal: total(userPage),
    problemTotal: total(problemPage),
    submissionTotal: total(submissionPage),
    contestTotal: total(contestPage)
  }
}

// ==================== 题目管理 ====================

export function adminPageProblems(params) {
  return request.get('/admin/problem/page', { params })
}

/** 管理端题目详情（编辑回显用：含标签/用例/语言模板） */
export function getAdminProblemDetail(id) {
  return request.get(`/admin/problem/${id}/detail`)
}

/** 生成下一个可用题号（前缀 + 数字递增，供新建表单自动填充） */
export function nextProblemKey(prefix = 'A') {
  return request.get('/admin/problem/next-key', { params: { prefix }, silent: true })
}

export function createProblem(data) {
  return request.post('/admin/problem', data)
}

export function updateProblem(id, data) {
  return request.put(`/admin/problem/${id}`, data)
}

export function deleteProblem(id) {
  return request.delete(`/admin/problem/${id}`)
}

export function listTestCases(problemId) {
  return request.get(`/admin/problem/${problemId}/testcase`)
}

export function createTestCase(problemId, data) {
  return request.post(`/admin/problem/${problemId}/testcase`, data)
}

export function updateTestCase(problemId, caseId, data) {
  return request.put(`/admin/problem/${problemId}/testcase/${caseId}`, data)
}

export function deleteTestCase(problemId, caseId) {
  return request.delete(`/admin/problem/${problemId}/testcase/${caseId}`)
}

/** 导入题目 Excel（multipart） */
export function importProblems(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/admin/problem/import', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

/** 导出题目 Excel（blob 下载） */
export function exportProblems() {
  return request.get('/admin/problem/export', { responseType: 'blob', timeout: 60000 })
}

// ==================== 比赛管理 ====================

export function pageContests(params) {
  return request.get('/contest/page', { params })
}

/** 比赛详情（含题目列表，管理端回显/题目管理用） */
export function getContestDetail(id) {
  return request.get(`/contest/${id}`)
}

export function createContest(data) {
  return request.post('/contest', data)
}

export function updateContest(id, data) {
  return request.put(`/contest/${id}`, data)
}

export function deleteContest(id) {
  return request.delete(`/contest/${id}`)
}

export function bindContestProblem(id, data) {
  return request.post(`/contest/${id}/problem`, data)
}

// ==================== 用户管理 ====================

export function pageUsers(params) {
  return request.get('/admin/user/page', { params })
}

export function getUserDetail(id) {
  return request.get(`/admin/user/${id}`)
}

export function createUser(data) {
  return request.post('/admin/user', data)
}

export function updateUser(data) {
  return request.put('/admin/user', data)
}

export function deleteUser(id) {
  return request.delete(`/admin/user/${id}`)
}

export function assignUserRoles(data) {
  return request.put('/admin/user/roles', data)
}

// ==================== 角色管理 ====================

export function pageRoles(params) {
  return request.get('/admin/role/page', { params })
}

export function listAllRoles() {
  return request.get('/admin/role/all')
}

export function getRoleDetail(id) {
  return request.get(`/admin/role/${id}`)
}

export function createRole(data) {
  return request.post('/admin/role', data)
}

export function updateRole(data) {
  return request.put('/admin/role', data)
}

export function deleteRole(id) {
  return request.delete(`/admin/role/${id}`)
}

// ==================== 权限管理 ====================

export function getPermissionTree() {
  return request.get('/admin/permission/tree')
}

export function createPermission(data) {
  return request.post('/admin/permission', data)
}

export function updatePermission(data) {
  return request.put('/admin/permission', data)
}

export function deletePermission(id) {
  return request.delete(`/admin/permission/${id}`)
}
