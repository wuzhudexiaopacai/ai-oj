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
