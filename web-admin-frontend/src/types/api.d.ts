export interface ApiResult<T = any> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
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

export interface ApartmentInfo {
  id?: number
  name: string
  introduction?: string
  provinceId?: number
  provinceName?: string
  cityId?: number
  cityName?: string
  districtId?: number
  districtName?: string
  addressDetail?: string
  latitude?: string
  longitude?: string
  phone?: string
  isRelease?: number
}

export interface ApartmentItemVo extends ApartmentInfo {
  totalRoomCount?: number
  freeRoomCount?: number
  /** 封面图（后台列表用，第一张公寓图片的 URL） */
  cover?: string
}

export interface ApartmentDetailVo extends ApartmentInfo {
  graphVoList?: GraphVo[]
  labelInfoList?: LabelInfo[]
  facilityInfoList?: FacilityInfo[]
  feeValueVoList?: FeeValueVo[]
}

export interface ApartmentSubmitVo extends ApartmentInfo {
  facilityInfoIds?: number[]
  labelIds?: number[]
  feeValueIds?: number[]
  graphVoList?: GraphVo[]
}

export interface RoomInfo {
  id?: number
  roomNumber: string
  rent: number
  apartmentId?: number
  isRelease?: number
}

export interface RoomItemVo extends RoomInfo {
  leaseEndDate?: string
  isCheckIn?: boolean
  apartmentInfo?: ApartmentInfo
}

export interface RoomDetailVo extends RoomInfo {
  apartmentInfo?: ApartmentInfo
  graphVoList?: GraphVo[]
  attrValueVoList?: AttrValueVo[]
  facilityInfoList?: FacilityInfo[]
  labelInfoList?: LabelInfo[]
  paymentTypeList?: PaymentType[]
  leaseTermList?: LeaseTerm[]
}

export interface RoomSubmitVo extends RoomInfo {
  graphVoList?: GraphVo[]
  attrValueIds?: number[]
  facilityInfoIds?: number[]
  labelInfoIds?: number[]
  paymentTypeIds?: number[]
  leaseTermIds?: number[]
}

export interface LabelInfo {
  id?: number
  name: string
  type?: number
}

export interface FacilityInfo {
  id?: number
  name: string
  type?: number
  icon?: string
}

export interface AttrKey {
  id?: number
  name: string
  attrValueList?: AttrValue[]
}

export interface AttrValue {
  id?: number
  name: string
  attrKeyId?: number
  attrKeyName?: string
}

export interface AttrValueVo extends AttrValue {}

export interface FeeKey {
  id?: number
  name: string
  feeValueList?: FeeValue[]
}

export interface FeeValue {
  id?: number
  name: string
  unit?: string
  feeKeyId?: number
  feeKeyName?: string
}

export interface FeeValueVo extends FeeValue {}

export interface PaymentType {
  id?: number
  name: string
  payMonthCount?: number
  additionalInfo?: string
}

export interface LeaseTerm {
  id?: number
  monthCount: number
  unit: string
}

export interface ProvinceInfo {
  id: number
  name: string
}

export interface CityInfo {
  id: number
  name: string
  provinceId?: number
}

export interface DistrictInfo {
  id: number
  name: string
  cityId?: number
}

export interface AppointmentVo {
  id?: number
  userId?: number
  name?: string
  phone?: string
  apartmentId?: number
  appointmentTime?: string
  additionalInfo?: string
  appointmentStatus?: number
  apartmentInfo?: ApartmentInfo
}

export interface AgreementVo {
  id?: number
  phone?: string
  name?: string
  identificationNumber?: string
  apartmentId?: number
  roomId?: number
  leaseStartDate?: string
  leaseEndDate?: string
  leaseTermId?: number
  rent?: number
  deposit?: number
  paymentTypeId?: number
  status?: number
  sourceType?: number
  additionalInfo?: string
  apartmentInfo?: ApartmentInfo
  roomInfo?: RoomInfo
  paymentType?: PaymentType
  leaseTerm?: LeaseTerm
}

export interface UserInfo {
  id?: number
  phone?: string
  avatarUrl?: string
  nickname?: string
  status?: number
}

export interface SystemPost {
  id?: number
  /** 后端字段名叫 postCode，映射到数据库列 code */
  postCode?: string
  name: string
  description?: string
  status?: number
}

export interface SystemUser {
  id?: number
  username?: string
  password?: string
  name?: string
  type?: number
  phone?: string
  avatarUrl?: string
  additionalInfo?: string
  postId?: number
  status?: number
  postName?: string
}

export interface SystemUserInfoVo {
  name?: string
  avatarUrl?: string
}

export interface CaptchaVo {
  image: string
  key: string
}
