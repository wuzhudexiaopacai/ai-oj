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
