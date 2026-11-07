import request from '../request'

/** 题目服务（ai-oj-problem） */
export function pageProblems(params) {
  return request.get('/problem/page', { params })
}

export function getProblemDetail(id) {
  return request.get(`/problem/${id}`)
}

export function listTags() {
  return request.get('/problem/tags')
}

export function getProblemLanguages(id) {
  return request.get(`/problem/${id}/language`)
}

/**
 * 发表题解（写入题解表 Solution，作者为当前登录用户，状态直接正常）
 *
 * @param {object} data { problemId, title, content }
 * @returns {Promise<number>} 新建题解 ID
 */
export function createSolution(data) {
  return request.post('/solution', data)
}

/** 题解分页（列表不返回正文 content） */
export function pageSolutions(problemId, params = {}) {
  return request.get('/solution/page', { params: { problemId, pageNum: 1, pageSize: 10, ...params } })
}

/** 题解详情（含 Markdown 正文与作者昵称） */
export function getSolutionDetail(id) {
  return request.get(`/solution/${id}`)
}

/** 点赞/取消点赞（切换），返回 { liked, likeCount } */
export function toggleSolutionLike(id) {
  return request.post(`/solution/${id}/like`)
}
