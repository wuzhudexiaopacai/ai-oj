import { defineStore } from 'pinia'
import { reactive, ref } from 'vue'

/**
 * AI 助手全局状态：
 * - visible：助手是否被唤起（默认隐藏，点击题目页“AI 助手”按钮后才出现）
 * - open：面板展开/收起（收起后回到悬浮球）
 * - streaming：是否有流式回答进行中（悬浮球呼吸提示）
 * - context：当前题目上下文（题目详情/编辑器代码/语言），由题目详情页注入，
 *   其他页面 context.problem 为空，助手退化为通用问答
 */
export const useAssistantStore = defineStore('assistant', () => {
  const visible = ref(false)
  const open = ref(false)
  const streaming = ref(false)
  const context = reactive({
    problem: null,
    code: '',
    language: ''
  })

  function setContext(patch) {
    Object.assign(context, patch)
  }

  function clearContext() {
    context.problem = null
    context.code = ''
    context.language = ''
  }

  function show() {
    visible.value = true
    open.value = true
  }

  /** 整体隐藏：悬浮球与面板都不显示，直到再次点击“AI 助手”按钮 */
  function hide() {
    visible.value = false
    open.value = false
  }

  return { visible, open, streaming, context, setContext, clearContext, show, hide }
})
