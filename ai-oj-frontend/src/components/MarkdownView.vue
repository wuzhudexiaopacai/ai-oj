<template>
  <!-- eslint-disable-next-line vue/no-v-html -->
  <div class="markdown-body" v-html="html" @click="onClick"></div>
</template>

<script setup>
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import katex from 'katex'
import texmath from 'markdown-it-texmath'
import 'katex/dist/katex.min.css'
import hljs from 'highlight.js/lib/core'
// 只注册 OJ 常见语言，避免全量包体积
import cpp from 'highlight.js/lib/languages/cpp'
import c from 'highlight.js/lib/languages/c'
import java from 'highlight.js/lib/languages/java'
import python from 'highlight.js/lib/languages/python'
import go from 'highlight.js/lib/languages/go'
import rust from 'highlight.js/lib/languages/rust'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import csharp from 'highlight.js/lib/languages/csharp'
import sql from 'highlight.js/lib/languages/sql'
import bash from 'highlight.js/lib/languages/bash'
import json from 'highlight.js/lib/languages/json'
import { ElMessage } from 'element-plus'
import { copyText } from '@/utils/clipboard'

const props = defineProps({
  content: { type: String, default: '' }
})

hljs.registerLanguage('cpp', cpp)
hljs.registerLanguage('c', c)
hljs.registerLanguage('java', java)
hljs.registerLanguage('python', python)
hljs.registerLanguage('go', go)
hljs.registerLanguage('golang', go)
hljs.registerLanguage('rust', rust)
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('csharp', csharp)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('json', json)

/** 语言标签美化：cpp → C++ 等，其余首字母大写 */
const LANG_LABELS = {
  cpp: 'C++',
  c: 'C',
  java: 'Java',
  python: 'Python',
  py: 'Python',
  go: 'Go',
  golang: 'Go',
  rust: 'Rust',
  javascript: 'JavaScript',
  js: 'JavaScript',
  typescript: 'TypeScript',
  ts: 'TypeScript',
  csharp: 'C#',
  sql: 'SQL',
  bash: 'Shell',
  sh: 'Shell',
  json: 'JSON'
}

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })

// LaTeX 数学公式：$...$ 行内、$$...$$ 块级（AI 题面常用），公式出错不阻断整篇渲染
md.use(texmath, {
  engine: katex,
  delimiters: 'dollars',
  katexOptions: { throwOnError: false, output: 'html' }
})

/**
 * 代码块渲染为独立卡片：头部（语言标签 + 复制按钮）+ 高亮正文。
 * 语言无法识别时退化为纯转义文本，保证内容完整可见。
 */
md.renderer.rules.fence = (tokens, idx) => {
  const token = tokens[idx]
  const lang = (token.info || '').trim().split(/\s+/)[0].toLowerCase()
  const label = LANG_LABELS[lang] || (lang ? lang.charAt(0).toUpperCase() + lang.slice(1) : '代码')
  let body
  if (lang && hljs.getLanguage(lang)) {
    body = hljs.highlight(token.content, { language: lang, ignoreIllegals: true }).value
  } else {
    body = md.utils.escapeHtml(token.content)
  }
  return (
    `<div class="code-block"><div class="code-block-head">` +
    `<span class="code-lang">${md.utils.escapeHtml(label)}</span>` +
    `<button type="button" class="code-copy" title="复制代码">复制</button>` +
    `</div><pre><code class="hljs">${body}</code></pre></div>`
  )
}

const html = computed(() => md.render(normalizeMath(props.content || '')))

/**
 * 数学公式规范化：AI 常把多行块级结构（cases / matrix / aligned 等）塞进行内 $...$，
 * 而 KaTeX 行内定界符不跨行，导致这类公式原样显示。此处把“跨行或含 \begin/\end 环境”
 * 的 $...$ 升级为独立的 $$...$$ 块级公式，交由 KaTeX displayMode 正确渲染。
 */
function normalizeMath(src) {
  if (!src) return src
  // 仅匹配单个 $ 包裹（排除已是 $$ 的块级）；[\S\s]*? 非贪婪、可跨行
  return src.replace(/(?<!\$)\$(?!\$)([\s\S]*?)(?<!\$)\$(?!\$)/g, (m, body) => {
    if (/\S/.test(body) && (body.includes('\n') || /\\(?:begin|end)\{/.test(body))) {
      return `\n\n$$\n${body}\n$$\n\n`
    }
    return m
  })
}

/** 事件委托处理“复制代码”按钮（v-html 内容无法直接绑定 Vue 事件） */
async function onClick(e) {
  const btn = e.target.closest('.code-copy')
  if (!btn) return
  const code = btn.closest('.code-block')?.querySelector('code')
  if (!code) return
  try {
    await copyText(code.textContent)
  } catch {
    ElMessage.warning('复制失败，请手动选择内容复制')
    return
  }
  btn.textContent = '已复制'
  btn.classList.add('is-done')
  setTimeout(() => {
    btn.textContent = '复制'
    btn.classList.remove('is-done')
  }, 1500)
}
</script>
