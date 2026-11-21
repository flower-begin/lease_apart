import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  // 通过 hostc / ngrok 等隧道分享给别人看时，HMR 的 WebSocket 会被反复断开
  //（日志里一堆 Connection lost. Reconnecting...），页面会卡在加载态。
  // 在 .env.development.local 里加一行 VITE_DISABLE_HMR=1 即可关掉热更新。
  const disableHmr = env.VITE_DISABLE_HMR === '1'

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
      // 用 hostc / ngrok / cloudflare tunnel 之类把本地服务暴露到公网时，
      // Vite 会因为 Host 不在白名单里直接返回 "Blocked request"。
      // allowedHosts: true 表示不校验来源域名（只建议开发环境这么开）。
      allowedHosts: true,
      // 关闭热更新：隧道场景下 ws 会反复断线，导致页面卡住
      hmr: disableHmr ? false : undefined,
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
