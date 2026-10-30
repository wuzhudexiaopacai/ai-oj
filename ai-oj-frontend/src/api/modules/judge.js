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

/**
 * 当前用户的题目做题状态（题库列表标记「已通过 / 尝试过」）
 * 返回 { solvedIds: string[], attemptedIds: string[] }
 */
export function getUserProblemStatus() {
  return request.get('/submission/user-problem-status')
}

/**
 * 用例校验（AI 出题用参考代码逐组实跑，得到真实输出回填期望）
 * 10 组顺序跑，耗时较长，超时放宽至 90s
 *
 * @param {object} data { language, code, timeLimitMs?, cases:[{seq,input,expectedOutput}] }
 * @returns {Promise<{compileOk,compileLog,overallPassed,totalCount,passedCount,cases:[{seq,passed,actualOutput,expectedOutput,status,errorMessage,timeMs,memoryKb}]}>
 */
export function validateCases(data) {
  return request.post('/judge/validate-cases', data, { timeout: 90000 })
}
