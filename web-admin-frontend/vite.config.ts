import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 后台管理前端，后端是 web-admin（默认 8080），接口前缀 /admin
//
// 后端端口/地址可通过 .env.local 覆盖：
//   VITE_API_TARGET=http://localhost:8080
//   VITE_PORT=5174
export default defineConfig(({ mode }) => {
  const root = fileURLToPath(new URL('.', import.meta.url))
  const env = loadEnv(mode, root, '')
  const apiTarget = env.VITE_API_TARGET || 'http://localhost:8080'
  const port = Number(env.VITE_PORT || 5174)

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      port,
      host: true,
      proxy: {
        // /api/apartment/pageItem -> {apiTarget}/admin/apartment/pageItem
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          rewrite: path => path.replace(/^\/api/, '/admin')
        },
        // 上传的图片
        '/upload': {
          target: apiTarget,
          changeOrigin: true
        }
      }
    }
  }
})
