import request from '@/utils/request'
import type { ApiResult, AppointmentItemVo, AppointmentDetailVo } from '@/types/api'

export function listAppointments() {
  return request.get<ApiResult<AppointmentItemVo[]>>('/appointment/listItem')
}

export function getAppointmentDetail(id: number) {
  return request.get<ApiResult<AppointmentDetailVo>>('/appointment/getDetailById', { params: { id } })
}

export function saveOrUpdateAppointment(data: any) {
  return request.post<ApiResult>('/appointment/saveOrUpdate', data)
}
