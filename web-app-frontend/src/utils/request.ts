import axios from 'axios'
import type { ApiResult } from '@/types/api'
import { showToast } from 'vant'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// Request interceptor - add access_token header
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['access_token'] = token
  }
  return config
})

// Response interceptor
request.interceptors.response.use(
  response => {
    const result: ApiResult = response.data
    if (result.code !== 200) {
      showToast(result.message || '请求失败')
      return Promise.reject(new Error(result.message))
    }
    return response
  },
  error => {
    if (error.response?.status === 305 || error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      window.location.href = '/login'
    }
    showToast('网络错误，请稍后重试')
    return Promise.reject(error)
  }
)

export default request
