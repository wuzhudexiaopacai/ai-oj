import request from '../request'

/** 比赛服务（ai-oj-contest） */
export function pageContests(params) {
  return request.get('/contest/page', { params })
}

export function getContestDetail(id) {
  return request.get(`/contest/${id}`)
}

export function registerContest(id) {
  return request.post(`/contest/${id}/register`)
}

export function getContestRank(id, params) {
  return request.get(`/contest/${id}/rank`, { params })
}
