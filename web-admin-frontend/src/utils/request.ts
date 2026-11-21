import axios from 'axios'
import type { ApiResult } from '@/types/api'
import { ElMessage } from 'element-plus'

const AUTH_CODES = [305, 601, 602]

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers['access_token'] = token
  }
  return config
})

request.interceptors.response.use(
  response => {
    const result: ApiResult = response.data
    if (result.code !== 200) {
      if (AUTH_CODES.includes(result.code)) {
        localStorage.removeItem('admin_token')
        localStorage.removeItem('admin_user')
        ElMessage.error('登录已失效，请重新登录')
        window.location.hash = '/login'
        return Promise.reject(new Error(result.message || '未登录'))
      }
      ElMessage.error(result.message || '请求失败')
      return Promise.reject(new Error(result.message || '请求失败'))
    }
    return response
  },
  error => {
    const status = error.response?.status
    if (status === 401 || status === 403) {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_user')
      window.location.hash = '/login'
      ElMessage.error('登录已失效，请重新登录')
      return Promise.reject(error)
    }
    ElMessage.error(error.response?.data?.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default request
