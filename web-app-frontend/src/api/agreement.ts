import request from '@/utils/request'
import type { ApiResult, AgreementItemVo, AgreementDetailVo } from '@/types/api'

export function listAgreements() {
  return request.get<ApiResult<AgreementItemVo[]>>('/agreement/listItem')
}

export function getAgreementDetail(id: number) {
  return request.get<ApiResult<AgreementDetailVo>>('/agreement/getDetailById', { params: { id } })
}

export function updateAgreementStatus(id: number, status: string) {
  return request.post<ApiResult>('/agreement/updateStatusById', null, { params: { id, leaseStatus: status } })
}

export function saveOrUpdateAgreement(data: any) {
  return request.post<ApiResult>('/agreement/saveOrUpdate', data)
}
