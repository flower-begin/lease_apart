import request from '@/utils/request'
import type { ApiResult, ApartmentDetailVo } from '@/types/api'

export function getApartmentDetail(id: number) {
  return request.get<ApiResult<ApartmentDetailVo>>('/apartment/getDetailById', { params: { id } })
}
