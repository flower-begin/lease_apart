import request from '@/utils/request'
import type { ApiResult, ProvinceInfo, CityInfo, DistrictInfo } from '@/types/api'

export function listProvince() {
  return request.get<ApiResult<ProvinceInfo[]>>('/region/province/list')
}

export function listCityByProvinceId(id: number) {
  return request.get<ApiResult<CityInfo[]>>('/region/city/listByProvinceId', { params: { id } })
}

export function listDistrictByCityId(id: number) {
  return request.get<ApiResult<DistrictInfo[]>>('/region/district/listByCityId', { params: { id } })
}
