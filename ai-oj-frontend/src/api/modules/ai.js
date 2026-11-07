import request from '../request'

/**
 * AI 助手服务（ai-oj-ai）
 * <p>
 * 后端契约：先 POST /ai/session 创建会话，再 GET /ai/chat/stream（SSE）流式对话。
 * EventSource 无法携带 Authorization 头，因此这里用 fetch + ReadableStream 读取流。
 */

/** 创建会话，scene 取值：chat/hint/review/generate */
export function createAiSession(data) {
  // data: { scene, problemId?, title? }
  return request.post('/ai/session', data)
}

/**
 * AI 出题（同步）：返回结构化题目草稿，供编辑页一键回填
 *
 * @param {object} data { topic, difficulty(0/1/2), requirement?, caseCount? }
 * @returns {Promise<{title,description,inputDesc,outputDesc,hint,difficulty,timeLimit,memoryLimit,testCases:[{inputData,outputData}]}>}
 */
export function generateProblem(data) {
  // 出题含参考代码+题解，大模型生成耗时长（可达分钟级），单独放宽超时
  return request.post('/ai/generate', data, { timeout: 300000 })
}

/**
 * 流式对话（POST + SSE）
 *
 * <p>用 POST 而不是 GET：当前代码可能有数千行，放 URL 里会超出长度限制。</p>
 *
 * @param {object} params
 * @param {number} params.sessionId 会话 ID
 * @param {string} params.question  用户问题
 * @param {string} [params.code]    用户当前编辑器代码（随每次提问上报，AI 才能看到最新代码）
 * @param {string} [params.language] 当前代码语言
 * @param {(token: string) => void} params.onToken 逐 token 回调
 * @param {AbortSignal} [params.signal] 取消信号
 */
export async function streamChat({ sessionId, question, code, language, onToken, signal }) {
  const token = localStorage.getItem('ai-oj-token')
  const resp = await fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify({ sessionId, question, code, language }),
    signal
  })
  if (!resp.ok || !resp.body) {
    throw new Error(`AI 服务不可用（HTTP ${resp.status}）`)
  }
  const reader = resp.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  for (;;) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    // SSE 帧以空行分隔；按规范一帧可含多条 data: 行（token 中的换行会被拆行），需用 \n 拼回
    const frames = buffer.split('\n\n')
    buffer = frames.pop()
    for (const frame of frames) {
      const lines = frame.split('\n').filter((item) => item.startsWith('data:'))
      if (lines.length) {
        onToken(lines.map((line) => line.slice(5)).join('\n'))
      }
    }
  }
}
