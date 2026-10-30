/**
 * 剪贴板工具
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */

/**
 * 复制文本到剪贴板。
 * <p>
 * 剪贴板 API 仅在安全上下文（https 或 localhost）可用，部署到 http 内网地址时会缺失，
 * 此处降级为临时 textarea + execCommand，保证任何部署环境下都能复制。
 *
 * @param {string} text 待复制文本
 * @returns {Promise<void>} 复制失败时 reject
 */
export async function copyText(text) {
  if (navigator.clipboard?.writeText && window.isSecureContext) {
    await navigator.clipboard.writeText(text)
    return
  }
  const ta = document.createElement('textarea')
  ta.value = text
  ta.style.position = 'fixed'
  ta.style.opacity = '0'
  document.body.appendChild(ta)
  ta.select()
  const ok = document.execCommand('copy')
  document.body.removeChild(ta)
  if (!ok) {
    throw new Error('execCommand copy 被拒绝')
  }
}
