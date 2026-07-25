import request from '@/utils/request'
import type { ApiResult, ChatResponseVo, ApartmentItemVo } from '@/types/api'

export function aiChat(message: string) {
  return request.post<ApiResult<ChatResponseVo>>('/ai/chat', { message })
}

export function aiRecommend(preference: string, cityId?: number, maxRent?: number) {
  return request.post<ApiResult<ApartmentItemVo[]>>('/ai/recommend', { preference, cityId, maxRent })
}
