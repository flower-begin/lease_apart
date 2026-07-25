import request from '@/utils/request'
import type { ApiResult, RoomItemVo, RoomDetailVo, RoomQueryVo, PageResult } from '@/types/api'

export function getRoomPage(current: number, size: number, queryVo: RoomQueryVo) {
  return request.get<ApiResult<PageResult<RoomItemVo>>>('/room/pageItem', {
    params: { current, size, ...queryVo }
  })
}

export function getRoomDetail(id: number) {
  return request.get<ApiResult<RoomDetailVo>>('/room/getDetailById', { params: { id } })
}

export function getRoomPageByApartmentId(current: number, size: number, id: number) {
  return request.get<ApiResult<PageResult<RoomItemVo>>>('/room/pageItemByApartmentId', {
    params: { current, size, id }
  })
}
