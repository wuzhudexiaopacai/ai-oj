import request from '../request'

/** 比赛服务（ai-oj-contest） */
export function pageContests(params) {
  return request.get('/contest/page', { params })
}

export function getContestDetail(id) {
  return request.get(`/contest/${id}`)
}

export function registerContest(id, password) {
  // 私有赛需携带比赛密码，公开赛不传
  return request.post(`/contest/${id}/register`, null, { params: password ? { password } : {} })
}

export function getContestRank(id, params) {
  return request.get(`/contest/${id}/rank`, { params })
}
