import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    base: env.VITE_APP_BASE_PATH || '/',
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      host: '0.0.0.0',
      port: Number(env.VITE_APP_PORT || 5175),
      proxy: {
        // 用户端接口
        '/app': {
          target: env.VITE_APP_BASE_URL || 'http://localhost:8081',
          changeOrigin: true
        },
        // 后台上传的图片返回的是相对路径 /upload/xxx.jpg，需要一起代理
        '/upload': {
          target: env.VITE_APP_BASE_URL || 'http://localhost:8081',
          changeOrigin: true
        }
      }
    }
  }
})
