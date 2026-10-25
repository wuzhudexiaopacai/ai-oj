import request from '../request'

/** 判题服务（ai-oj-judge） */
export function submitCode(data) {
  return request.post('/judge/submit', data)
}

/**
 * 按 submitId 查询提交详情（含用例明细、编译错误信息）
 * 注意：判题服务 /submission/{id} 只接受数据库主键 ID，前端拿不到，
 * 因此统一使用 /submission/submit/{submitId}
 */
export function getSubmission(submitId) {
  return request.get(`/submission/submit/${submitId}`)
}

/** 我的提交分页（后端固定按当前登录用户过滤） */
export function pageSubmissions(params) {
  return request.get('/submission/page', { params })
}
