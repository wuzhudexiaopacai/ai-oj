import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      // 本地开发代理到网关
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true
      },
      // WebSocket（判题进度 / 比赛时间实时推送）
      '/ws': {
        target: 'ws://localhost:8088',
        ws: true,
        changeOrigin: true
      },
      // 公开静态资源（头像）：开发期直连 MinIO 公共桶
      '/files': {
        target: 'http://localhost:9000/ai-oj-public',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/files/, '')
      }
    }
  },
  build: {
    chunkSizeWarningLimit: 4096,
    rollupOptions: {
      output: {
        // 函数式分包：node_modules 统一进 vendor（monaco 单独拆出）
        // 避免 vue / vue-demi / element-plus 被拆到不同 chunk 形成跨块循环引用
        // （症状：Cannot access 'xx' before initialization）
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('monaco-editor')) return 'monaco'
            return 'vendor'
          }
          // Vite 的动态 import 预加载辅助函数必须留在 vendor：
          // 若被 Rollup 并入 monaco chunk，入口会静态 import monaco，
          // 导致 index.html 全站 modulepreload 3.9MB 编辑器产物体积
          if (id.includes('vite/preload-helper')) return 'vendor'
        }
      }
    }
  }
})
