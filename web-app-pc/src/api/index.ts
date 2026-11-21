import { http } from '@/utils/request'
import type {
  AgreementDetailVo,
  AgreementItemVo,
  ApartmentDetailVo,
  ApartmentItemVo,
  AppointmentDetailVo,
  AppointmentItemVo,
  HistoryItemVo,
  IPage,
  LeaseAgreement,
  LeaseTerm,
  PaymentType,
  RegionNode,
  RoomDetailVo,
  RoomItemVo,
  RoomQuery,
  UserInfoVo,
  ViewAppointment
} from '@/types/api'

/* ------------------------------- 登录 / 用户 ------------------------------- */
export const authApi = {
  /** 账号密码登录，返回 token */
  login: (data: { username: string; password: string }) => http.post<string>('/app/login', data),
  /** 注册（账号 + 密码 + 手机号），成功直接返回 token */
  register: (data: { username: string; password: string; phone: string; nickname?: string }) =>
    http.post<string>('/app/register', data),
  /** 当前登录用户信息 */
  info: () => http.get<UserInfoVo>('/app/info')
}

export const userApi = {
  update: (data: { nickname: string; avatarUrl: string }) => http.put<UserInfoVo>('/app/user/update', data),
  upload: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return http.upload<string>('/app/user/upload', form)
  }
}

/* -------------------------------- 地区 / 字典 -------------------------------- */
export const regionApi = {
  /** 省市区三级树，一次拿全 */
  tree: () => http.get<RegionNode[]>('/app/region/listAsTree')
}

export const paymentApi = {
  list: () => http.get<PaymentType[]>('/app/payment/list'),
  listByRoom: (id: number) => http.get<PaymentType[]>('/app/payment/listByRoomId', { id })
}

export const termApi = {
  listByRoom: (id: number) => http.get<LeaseTerm[]>('/app/term/listByRoomId', { id })
}

/* ---------------------------------- 公寓 ---------------------------------- */
export const apartmentApi = {
  list: () => http.get<ApartmentItemVo[]>('/app/apartment/list'),
  detail: (id: number) => http.get<ApartmentDetailVo>('/app/apartment/getDetailById', { id })
}

/* ---------------------------------- 房间 ---------------------------------- */
export const roomApi = {
  /** 找房列表（分页 + 筛选 + 排序） */
  page: (params: Record<string, unknown>) => http.get<IPage<RoomItemVo>>('/app/room/pageItem', params),
  detail: (id: number) => http.get<RoomDetailVo>('/app/room/getDetailById', { id }),
  /** 某个公寓下的房间（分页） */
  pageByApartment: (params: { current: number; size: number; id: number }) =>
    http.get<IPage<RoomItemVo>>('/app/room/pageItemByApartmentId', params)
}

/** 把筛选条件整理成后端要的查询参数（空值不下发） */
export function buildRoomQuery(query: RoomQuery): Record<string, unknown> {
  const params: Record<string, unknown> = {}
  const keys: (keyof RoomQuery)[] = [
    'provinceId',
    'cityId',
    'districtId',
    'minRent',
    'maxRent',
    'paymentTypeId',
    'orderType',
    'keyword'
  ]
  keys.forEach(key => {
    const value = query[key]
    if (value !== undefined && value !== null && value !== '') {
      params[key] = value
    }
  })
  return params
}

/* --------------------------------- 预约看房 --------------------------------- */
export const appointmentApi = {
  save: (data: ViewAppointment) => http.post('/app/appointment/saveOrUpdate', data),
  cancel: (id: number) => http.post('/app/appointment/cancel', undefined, { id }),
  list: () => http.get<AppointmentItemVo[]>('/app/appointment/listItem'),
  detail: (id: number) => http.get<AppointmentDetailVo>('/app/appointment/getDetailById', { id })
}

/* ---------------------------------- 租约 ---------------------------------- */
export const agreementApi = {
  list: () => http.get<AgreementItemVo[]>('/app/agreement/listItem'),
  detail: (id: number) => http.get<AgreementDetailVo>('/app/agreement/getDetailById', { id }),
  /** 改状态：3 取消签约 / 5 申请退租 / 6 撤销退租 */
  updateStatus: (id: number, leaseStatus: number) =>
    http.post('/app/agreement/updateStatusById', undefined, { id, leaseStatus }),
  /** 续约 */
  renew: (params: {
    id: number
    leaseStartDate?: string
    leaseEndDate: string
    leaseTermId?: number
    rent?: number
    paymentTypeId?: number
  }) => http.post('/app/agreement/renew', undefined, params),
  /** 签约（新增租约，状态为签约待确认） */
  sign: (data: LeaseAgreement) => http.post('/app/agreement/saveOrUpdate', data)
}

/* --------------------------------- 浏览历史 --------------------------------- */
export const historyApi = {
  page: (params: { current: number; size: number }) =>
    http.get<IPage<HistoryItemVo>>('/app/history/pageItem', params)
}
