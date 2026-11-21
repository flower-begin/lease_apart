import request from '@/utils/request'
import type {
  AgreementVo,
  ApiResult,
  ApartmentDetailVo,
  ApartmentItemVo,
  ApartmentSubmitVo,
  AppointmentVo,
  AttrKey,
  AttrValue,
  CaptchaVo,
  CityInfo,
  DistrictInfo,
  FacilityInfo,
  FeeKey,
  FeeValue,
  LabelInfo,
  LeaseTerm,
  MenuVo,
  PageResult,
  PaymentType,
  ProvinceInfo,
  RoomDetailVo,
  RoomInfo,
  RoomItemVo,
  RoomSubmitVo,
  SystemPost,
  SystemUser,
  SystemUserInfoVo,
  SysRole,
  UserInfo
} from '@/types/api'

/* ------------------------------------------------------------------ 登录 */
export const authApi = {
  captcha: () => request.get<ApiResult<CaptchaVo>>('/login/captcha'),
  login: (data: { username: string; password: string; captchaKey: string; captchaCode: string }) =>
    request.post<ApiResult<string>>('/login', data),
  info: () => request.get<ApiResult<SystemUserInfoVo>>('/info')
}

/* -------------------------------------------------------------- 省市区 */
export const regionApi = {
  provinces: () => request.get<ApiResult<ProvinceInfo[]>>('/region/province/list'),
  cities: (provinceId: number) =>
    request.get<ApiResult<CityInfo[]>>('/region/city/listByProvinceId', { params: { id: provinceId } }),
  districts: (cityId: number) =>
    request.get<ApiResult<DistrictInfo[]>>('/region/district/listByCityId', { params: { id: cityId } })
}

/* ---------------------------------------------------------------- 公寓 */
export const apartmentApi = {
  page: (params: {
    current: number
    size: number
    provinceId?: number
    cityId?: number
    districtId?: number
    name?: string
  }) => request.get<ApiResult<PageResult<ApartmentItemVo>>>('/apartment/pageItem', { params }),
  detail: (id: number) => request.get<ApiResult<ApartmentDetailVo>>('/apartment/getDetailById', { params: { id } }),
  save: (data: ApartmentSubmitVo) => request.post<ApiResult>('/apartment/saveOrUpdate', data),
  remove: (id: number) => request.delete<ApiResult>('/apartment/removeById', { params: { id } }),
  updateRelease: (id: number, status: number) =>
    request.post<ApiResult>('/apartment/updateReleaseStatusById', null, { params: { id, status } }),
  listByDistrict: (districtId: number) =>
    request.get<ApiResult<ApartmentItemVo[]>>('/apartment/listInfoByDistrictId', { params: { id: districtId } })
}

/* ---------------------------------------------------------------- 房间 */
export const roomApi = {
  page: (params: {
    current: number
    size: number
    apartmentId?: number
    provinceId?: number
    cityId?: number
    districtId?: number
  }) => request.get<ApiResult<PageResult<RoomItemVo>>>('/room/pageItem', { params }),
  detail: (id: number) => request.get<ApiResult<RoomDetailVo>>('/room/getDetailById', { params: { id } }),
  save: (data: RoomSubmitVo) => request.post<ApiResult>('/room/saveOrUpdate', data),
  remove: (id: number) => request.delete<ApiResult>('/room/removeById', { params: { id } }),
  updateRelease: (id: number, status: number) =>
    request.post<ApiResult>('/room/updateReleaseStatusById', null, { params: { id, status } }),
  listBasicByApartment: (apartmentId: number) =>
    request.get<ApiResult<RoomInfo[]>>('/room/listBasicByApartmentId', { params: { id: apartmentId } })
}

/* ---------------------------------------------------------------- 预约 */
export const appointmentApi = {
  page: (params: { current: number; size: number; name?: string; phone?: string; apartmentId?: number }) =>
    request.get<ApiResult<PageResult<AppointmentVo>>>('/appointment/page', { params }),
  updateStatus: (id: number, status: number) =>
    request.post<ApiResult>('/appointment/updateStatusById', null, { params: { id, status } })
}

/* ---------------------------------------------------------------- 租约 */
export const agreementApi = {
  page: (params: { current: number; size: number; name?: string; phone?: string; apartmentId?: number }) =>
    request.get<ApiResult<PageResult<AgreementVo>>>('/agreement/page', { params }),
  detail: (id: number) => request.get<ApiResult<AgreementVo>>('/agreement/getById', { params: { id } }),
  save: (data: AgreementVo) => request.post<ApiResult>('/agreement/saveOrUpdate', data),
  remove: (id: number) => request.delete<ApiResult>('/agreement/removeById', { params: { id } }),
  updateStatus: (id: number, status: number) =>
    request.post<ApiResult>('/agreement/updateStatusById', null, { params: { id, status } })
}

/* -------------------------------------------------------------- App 用户 */
export const userApi = {
  page: (params: { current: number; size: number; phone?: string }) =>
    request.get<ApiResult<PageResult<UserInfo>>>('/user/page', { params }),
  updateStatus: (id: number, status: number) =>
    request.post<ApiResult>('/user/updateStatusById', null, { params: { id, status } })
}

/* ------------------------------------------------------------ 系统-员工 */
export const systemUserApi = {
  page: (params: { current: number; size: number; name?: string; phone?: string }) =>
    request.get<ApiResult<PageResult<SystemUser>>>('/system/user/page', { params }),
  detail: (id: number) => request.get<ApiResult<SystemUser>>('/system/user/getById', { params: { id } }),
  save: (data: SystemUser) => request.post<ApiResult>('/system/user/saveOrUpdate', data),
  remove: (id: number) => request.delete<ApiResult>('/system/user/deleteById', { params: { id } }),
  updateStatus: (id: number, status: number) =>
    request.post<ApiResult>('/system/user/updateStatusByUserId', null, { params: { id, status } }),
  checkUsername: (username: string) =>
    request.get<ApiResult<boolean>>('/system/user/isUserNameAvailable', { params: { username } })
}

/* ------------------------------------------------------------ 系统-岗位 */
export const systemPostApi = {
  page: (params: { current: number; size: number; name?: string }) =>
    request.get<ApiResult<PageResult<SystemPost>>>('/system/post/page', { params }),
  list: () => request.get<ApiResult<SystemPost[]>>('/system/post/list'),
  detail: (id: number) => request.get<ApiResult<SystemPost>>('/system/post/getById', { params: { id } }),
  save: (data: SystemPost) => request.post<ApiResult>('/system/post/saveOrUpdate', data),
  remove: (id: number) => request.delete<ApiResult>('/system/post/deleteById', { params: { id } }),
  updateStatus: (id: number, status: number) =>
    request.post<ApiResult>('/system/post/updateStatusByPostId', null, { params: { id, status } })
}

/* ------------------------------------------------------------ 系统-角色（RBAC） */
export const roleApi = {
  list: () => request.get<ApiResult<SysRole[]>>('/system/role/list'),
  save: (data: SysRole) => request.post<ApiResult>('/system/role/saveOrUpdate', data),
  remove: (id: number) => request.delete<ApiResult>('/system/role/deleteById', { params: { id } }),
  menuIds: (id: number) => request.get<ApiResult<number[]>>('/system/role/menuIds', { params: { id } }),
  assignMenu: (roleId: number, menuIds: number[]) =>
    request.put<ApiResult>('/system/role/assignMenu', { roleId, menuIds }),
  updateStatus: (id: number, status: number) =>
    request.post<ApiResult>('/system/role/updateStatus', null, { params: { id, status } })
}

/* ------------------------------------------------------------ 系统-菜单权限 */
export const menuApi = {
  tree: () => request.get<ApiResult<MenuVo[]>>('/system/menu/tree')
}

/* ------------------------------------------------------------ 系统-员工角色 */
export const userRoleApi = {
  roles: (id: number) => request.get<ApiResult<SysRole[]>>('/system/user/roles', { params: { id } }),
  roleIds: (id: number) => request.get<ApiResult<number[]>>('/system/user/roleIds', { params: { id } }),
  assign: (userId: number, roleIds: number[]) =>
    request.put<ApiResult>('/system/user/assignRole', { userId, roleIds })
}

/* -------------------------------------------------------------- 字典项 */
export const labelApi = {
  list: (type?: number) => request.get<ApiResult<LabelInfo[]>>('/label/list', { params: { type } }),
  save: (data: LabelInfo) => request.post<ApiResult>('/label/saveOrUpdate', data),
  remove: (id: number) => request.delete<ApiResult>('/label/deleteById', { params: { id } })
}

export const facilityApi = {
  list: (type?: number) => request.get<ApiResult<FacilityInfo[]>>('/facility/list', { params: { type } }),
  save: (data: FacilityInfo) => request.post<ApiResult>('/facility/saveOrUpdate', data),
  remove: (id: number) => request.delete<ApiResult>('/facility/deleteById', { params: { id } })
}

export const attrApi = {
  list: () => request.get<ApiResult<AttrKey[]>>('/attr/list'),
  saveKey: (data: AttrKey) => request.post<ApiResult>('/attr/key/saveOrUpdate', data),
  saveValue: (data: AttrValue) => request.post<ApiResult>('/attr/value/saveOrUpdate', data),
  removeKey: (attrKeyId: number) => request.delete<ApiResult>('/attr/key/deleteById', { params: { attrKeyId } }),
  removeValue: (id: number) => request.delete<ApiResult>('/attr/value/deleteById', { params: { id } })
}

export const feeApi = {
  list: () => request.get<ApiResult<FeeKey[]>>('/fee/list'),
  saveKey: (data: FeeKey) => request.post<ApiResult>('/fee/key/saveOrUpdate', data),
  saveValue: (data: FeeValue) => request.post<ApiResult>('/fee/value/saveOrUpdate', data),
  removeKey: (feeKeyId: number) => request.delete<ApiResult>('/fee/key/deleteById', { params: { feeKeyId } }),
  removeValue: (id: number) => request.delete<ApiResult>('/fee/value/deleteById', { params: { id } })
}

export const paymentApi = {
  list: () => request.get<ApiResult<PaymentType[]>>('/payment/list'),
  save: (data: PaymentType) => request.post<ApiResult>('/payment/saveOrUpdate', data),
  remove: (id: number) => request.delete<ApiResult>('/payment/deleteById', { params: { id } })
}

export const termApi = {
  list: () => request.get<ApiResult<LeaseTerm[]>>('/term/list'),
  save: (data: LeaseTerm) => request.post<ApiResult>('/term/saveOrUpdate', data),
  remove: (id: number) => request.delete<ApiResult>('/term/deleteById', { params: { id } })
}

/* ---------------------------------------------------------------- 文件 */
export const fileApi = {
  upload: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    // 不要手动设置 Content-Type，交给 axios 自动带 boundary
    return request.post<ApiResult<string>>('/file/upload', form)
  }
}
