import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// 后端地址：默认 http://localhost:8080，可通过环境变量 VITE_PROXY_TARGET 覆盖
const target = process.env.VITE_PROXY_TARGET || 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true,
    // 开发环境代理 /api 到后端，避免跨域与 cookie 同源问题
    proxy: {
      '/api': {
        target,
        changeOrigin: true,
      },
    },
  },
})
