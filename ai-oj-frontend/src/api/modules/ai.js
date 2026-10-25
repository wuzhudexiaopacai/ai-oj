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
 * 流式对话
 *
 * @param {object} params
 * @param {number} params.sessionId 会话 ID
 * @param {string} params.question  用户问题
 * @param {(token: string) => void} params.onToken 逐 token 回调
 * @param {AbortSignal} [params.signal] 取消信号
 */
export async function streamChat({ sessionId, question, onToken, signal }) {
  const token = localStorage.getItem('ai-oj-token')
  const url = `/api/ai/chat/stream?sessionId=${encodeURIComponent(sessionId)}`
      + `&question=${encodeURIComponent(question)}`
  const resp = await fetch(url, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
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
    // SSE 帧以空行分隔，逐帧取出 data: 内容
    const frames = buffer.split('\n\n')
    buffer = frames.pop()
    for (const frame of frames) {
      const line = frame.split('\n').find((item) => item.startsWith('data:'))
      if (line) {
        onToken(line.slice(5))
      }
    }
  }
}
