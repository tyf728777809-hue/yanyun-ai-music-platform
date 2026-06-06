import react from '@vitejs/plugin-react';
import {defineConfig} from 'vitest/config';

// 这是独立的前端原型，只读地调用本地后端 OpenAPI v0.1。
// 浏览器同源访问 `/api/v1`，由 dev proxy 转发到本地 8080，避免后端未配置 CORS 的问题。
// 需要指向其它后端时设置 VITE_API_PROXY_TARGET。
const apiTarget = process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5273,
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    environmentOptions: {
      jsdom: { url: 'http://localhost:5273/' },
    },
    setupFiles: './src/setupTests.ts',
    globals: true,
  },
});
