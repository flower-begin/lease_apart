import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types/api'

/** 本地存 token 的 key，与 stores/user 保持一致 */
export const TOKEN_KEY = 'lease_pc_token'

/** 登录失效的状态码（和移动端一致） */
const EXPIRE_CODES = [501, 601, 602]

const service = axios.create({
  // 开发环境走 vite 代理（同源，无跨域）；生产环境直接请求后端绝对地址
  baseURL: import.meta.env.PROD ? import.meta.env.VITE_APP_BASE_URL : '/',
  timeout: 20000
})

/* ------------------------------ 请求拦截 ------------------------------ */
service.interceptors.request.use(
  config => {
    // 注意：这里直接读 localStorage，避免和 pinia store 互相 import 形成循环依赖
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.ACCESS_TOKEN = token
    }
    return config
  },
  error => Promise.reject(error)
)

/* ------------------------------ 响应拦截 ------------------------------ */
service.interceptors.response.use(
  response => {
    const data = response.data as Result<unknown>

    // 未登录 / token 过期
    if (EXPIRE_CODES.includes(data.code)) {
      localStorage.removeItem(TOKEN_KEY)
      ElMessage.warning('登录已过期，请重新登录')
      const current = window.location.hash.slice(1) || '/'
      if (!current.startsWith('/login')) {
        window.location.hash = `#/login?redirect=${encodeURIComponent(current)}`
      }
      return Promise.reject(data)
    }

    if (data.code && data.code !== 200) {
      // 把完整的请求/响应打出来，方便复制给开发排查
      console.error('[接口业务错误]', {
        url: response.config.url,
        method: response.config.method,
        requestBody: response.config.data,
        response: data
      })
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(data)
    }

    return data as unknown as typeof response
  },
  error => {
    const status = error?.response?.status
    let message = error?.message || '网络异常'
    if (status === 404) message = '接口不存在（404）'
    if (status === 500) message = '服务器内部错误（500）'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

/** 把响应拦截器解包后的 Result 暴露出来，调用方拿到的就是 Result<T> */
export const http = {
  get<T = unknown>(url: string, params?: Record<string, unknown>) {
    return service.get(url, { params }) as unknown as Promise<Result<T>>
  },
  post<T = unknown>(url: string, data?: unknown, params?: Record<string, unknown>) {
    return service.post(url, data, { params }) as unknown as Promise<Result<T>>
  },
  put<T = unknown>(url: string, data?: unknown, params?: Record<string, unknown>) {
    return service.put(url, data, { params }) as unknown as Promise<Result<T>>
  },
  delete<T = unknown>(url: string, params?: Record<string, unknown>) {
    return service.delete(url, { params }) as unknown as Promise<Result<T>>
  },
  /** 文件上传（不要手动设置 Content-Type，交给浏览器带 boundary） */
  upload<T = unknown>(url: string, form: FormData) {
    return service.post(url, form) as unknown as Promise<Result<T>>
  }
}

export default service
