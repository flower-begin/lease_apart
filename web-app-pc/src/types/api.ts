/* =============================================================================
 *  接口数据类型 —— 严格对应后端 web/web-app 的 VO
 * ========================================================================== */

export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface IPage<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface GraphVo {
  name?: string
  url: string
}

export interface LabelInfo {
  id: number
  name: string
  type?: number
}

export interface FacilityInfo {
  id: number
  name: string
  type?: number
}

export interface RegionNode {
  id: number
  name: string
  children?: RegionNode[]
}

export interface PaymentType {
  id: number
  name: string
  payMonthCount: number
  additionalInfo?: string
}

export interface LeaseTerm {
  id: number
  monthCount: number
  unit: string
}

/* ---------------- 公寓 ---------------- */
export interface ApartmentInfo {
  id: number
  name: string
  introduction?: string
  districtId?: number
  districtName?: string
  cityId?: number
  cityName?: string
  provinceId?: number
  provinceName?: string
  addressDetail?: string
  latitude?: string
  longitude?: string
  phone?: string
  isRelease?: number
}

export interface ApartmentItemVo extends ApartmentInfo {
  labelInfoList: LabelInfo[]
  graphVoList: GraphVo[]
  minRent: number
}

export interface ApartmentDetailVo extends ApartmentInfo {
  graphVoList: GraphVo[]
  labelInfoList: LabelInfo[]
  facilityInfoList: FacilityInfo[]
  minRent: number
}

/* ---------------- 房间 ---------------- */
export interface RoomInfo {
  id: number
  roomNumber: string
  rent: number
  apartmentId: number
  isRelease?: number
}

export interface RoomItemVo {
  id: number
  roomNumber: string
  rent: number
  graphVoList: GraphVo[]
  labelInfoList: LabelInfo[]
  apartmentInfo: ApartmentInfo
  isCheckIn: boolean
}

export interface AttrValueVo {
  id: number
  attrKeyId: number
  name: string
  attrKeyName: string
}

export interface FeeValueVo {
  id: number
  feeKeyId: number
  name: string
  amount: number
  unit: string
  feeKeyName: string
}

export interface RoomDetailVo extends RoomInfo {
  apartmentItemVo: ApartmentItemVo
  graphVoList: GraphVo[]
  attrValueVoList: AttrValueVo[]
  facilityInfoList: FacilityInfo[]
  labelInfoList: LabelInfo[]
  paymentTypeList: PaymentType[]
  feeValueVoList: FeeValueVo[]
  leaseTermList: LeaseTerm[]
  isCheckIn: boolean
}

export interface RoomQuery {
  provinceId?: number | string
  cityId?: number | string
  districtId?: number | string
  minRent?: number | string
  maxRent?: number | string
  paymentTypeId?: number | string
  orderType?: '' | 'asc' | 'desc'
  keyword?: string
}

/* ---------------- 预约 ---------------- */
export interface ViewAppointment {
  id?: number
  userId?: number
  name: string
  phone: string
  apartmentId: number
  appointmentTime: string
  additionalInfo?: string
  appointmentStatus?: number
}

export interface AppointmentItemVo {
  id: number
  apartmentName: string
  graphVoList: GraphVo[]
  appointmentTime: string
  appointmentStatus: number
}

export interface AppointmentDetailVo extends ViewAppointment {
  apartmentName: string
  apartmentAddress: string
  graphVoList: GraphVo[]
  apartmentItemVo: ApartmentItemVo
}

/* ---------------- 租约 ---------------- */
export interface LeaseAgreement {
  id?: number
  phone?: string
  name: string
  identificationNumber?: string
  apartmentId: number
  roomId: number
  leaseStartDate: string
  leaseEndDate: string
  leaseTermId?: number
  rent?: number
  deposit?: number
  paymentTypeId?: number
  status?: number
  sourceType?: number
  additionalInfo?: string
}

export interface AgreementItemVo {
  id: number
  roomGraphVoList: GraphVo[]
  apartmentName: string
  roomNumber: string
  leaseStatus: number
  leaseStartDate: string
  leaseEndDate: string
  sourceType: number
  rent: number
  isRenewable: boolean
}

export interface AgreementDetailVo extends LeaseAgreement {
  apartmentName: string
  apartmentGraphVoList: GraphVo[]
  roomNumber: string
  roomGraphVoList: GraphVo[]
  paymentTypeName: string
  leaseTermMonthCount: number
  leaseTermUnit: string
}

/* ---------------- 浏览历史 ---------------- */
export interface HistoryItemVo {
  id: number
  roomId: number
  roomNumber: string
  rent: number
  roomGraphVoList: GraphVo[]
  apartmentName: string
  provinceName: string
  cityName: string
  districtName: string
}

/* ---------------- 用户 ---------------- */
export interface UserInfoVo {
  nickname: string
  avatarUrl: string
  phone: string
}
