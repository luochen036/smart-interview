import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// TS 版写法与 JS 完全一致（defineConfig 会自动做类型提示）
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 前端发 /api/xx → 自动转发到网关 9000（跨域就此消失）
      '/api': {
        target: 'http://localhost:9000',
        changeOrigin: true,
      },
    },
  },
})