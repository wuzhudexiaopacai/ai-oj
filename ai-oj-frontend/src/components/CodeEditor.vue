<template>
  <div ref="editorRef" class="code-editor" :style="{ height: typeof height === 'number' ? height + 'px' : height }"></div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import * as monaco from 'monaco-editor'
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'

// 仅引入需要的语言包，控制产物体积
import 'monaco-editor/esm/vs/basic-languages/cpp/cpp.contribution'
import 'monaco-editor/esm/vs/basic-languages/java/java.contribution'
import 'monaco-editor/esm/vs/basic-languages/python/python.contribution'
import 'monaco-editor/esm/vs/basic-languages/go/go.contribution'
import 'monaco-editor/esm/vs/basic-languages/javascript/javascript.contribution'

import { useThemeStore } from '@/stores/theme'

self.MonacoEnvironment = {
  getWorker: () => new editorWorker()
}

const props = defineProps({
  modelValue: { type: String, default: '' },
  language: { type: String, default: 'cpp' },
  height: { type: [Number, String], default: 420 },
  readonly: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

const themeStore = useThemeStore()
const editorRef = ref(null)
let editor = null

onMounted(() => {
  editor = monaco.editor.create(editorRef.value, {
    value: props.modelValue,
    language: props.language,
    theme: themeStore.isDark ? 'vs-dark' : 'vs',
    readOnly: props.readonly,
    automaticLayout: true,
    fontSize: 14,
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
    tabSize: 4,
    insertSpaces: true,
    renderLineHighlight: 'line',
    scrollbar: { verticalScrollbarSize: 8, horizontalScrollbarSize: 8 }
  })

  editor.onDidChangeModelContent(() => {
    emit('update:modelValue', editor.getValue())
  })
})

onBeforeUnmount(() => {
  editor?.dispose()
  editor = null
})

// 外部赋值（切换语言模板等）
watch(
  () => props.modelValue,
  (val) => {
    if (editor && editor.getValue() !== val) {
      editor.setValue(val ?? '')
    }
  }
)

watch(
  () => props.language,
  (lang) => {
    if (editor) {
      monaco.editor.setModelLanguage(editor.getModel(), lang)
    }
  }
)

watch(
  () => themeStore.isDark,
  (dark) => {
    monaco.editor.setTheme(dark ? 'vs-dark' : 'vs')
  }
)
</script>

<style scoped>
.code-editor {
  width: 100%;
  border: 1px solid var(--oj-border);
  border-radius: 8px;
  overflow: hidden;
}
</style>
