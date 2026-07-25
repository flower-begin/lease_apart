import request from '@/utils/request'
import type { ApiResult, HistoryItemVo, PageResult } from '@/types/api'

export function getHistoryPage(current: number, size: number) {
  return request.get<ApiResult<PageResult<HistoryItemVo>>>('/history/pageItem', {
    params: { current, size }
  })
}
