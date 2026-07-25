import request from '@/utils/request'
import type { ApiResult, UserInfoVo } from '@/types/api'

export function getCode(phone: string) {
  return request.get<ApiResult>('/login/getCode', { params: { phone } })
}

export function login(phone: string, code: string) {
  return request.post<ApiResult<string>>('/login', { phone, code })
}

export function getUserInfo(token: string) {
  return request.get<ApiResult<UserInfoVo>>('/info')
}
