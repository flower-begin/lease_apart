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

export interface LoginVo {
  phone: string
  code: string
}

export interface UserInfoVo {
  nickname: string
  avatarUrl: string
}

export interface ProvinceInfo {
  id: number
  name: string
}

export interface CityInfo {
  id: number
  name: string
  provinceId: number
}

export interface DistrictInfo {
  id: number
  name: string
  cityId: number
}

export interface GraphVo {
  name: string
  url: string
}

export interface LabelInfo {
  id: number
  name: string
  type: number
}

export interface FacilityInfo {
  id: number
  name: string
  type: number
  icon: string
}

export interface FeeValueVo {
  id: number
  name: string
  unit: string
  feeKeyName: string
}

export interface AttrValueVo {
  id: number
  name: string
  attrKeyName: string
}

export interface PaymentType {
  id: number
  name: string
  payMonthCount: number
  additionalInfo: string
}

export interface LeaseTerm {
  id: number
  monthCount: number
  unit: string
}

export interface ApartmentDetailVo {
  id: number
  name: string
  introduction: string
  provinceName: string
  cityName: string
  districtName: string
  addressDetail: string
  latitude: string
  longitude: string
  phone: string
  isRelease: number
  graphVoList: GraphVo[]
  labelInfoList: LabelInfo[]
  facilityInfoList: FacilityInfo[]
  minRent: number
}

export interface ApartmentItemVo extends ApartmentDetailVo {}

export interface RoomItemVo {
  id: number
  roomNumber: string
  rent: number
  graphVoList: GraphVo[]
  labelInfoList: LabelInfo[]
  apartmentInfo: {
    id: number
    name: string
    provinceName: string
    cityName: string
    districtName: string
  }
}

export interface RoomDetailVo {
  id: number
  roomNumber: string
  rent: number
  apartmentId: number
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

export interface RoomQueryVo {
  provinceId?: number
  cityId?: number
  districtId?: number
  minRent?: number
  maxRent?: number
  paymentTypeId?: number
  orderType?: string
}

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
  browseTime: string
}

export interface AppointmentItemVo {
  id: number
  apartmentName: string
  graphVoList: GraphVo[]
  appointmentTime: string
  appointmentStatus: number
}

export interface AppointmentDetailVo {
  id: number
  apartmentId: number
  apartmentName: string
  name: string
  phone: string
  appointmentTime: string
  additionalInfo: string
  appointmentStatus: number
}

export interface AgreementItemVo {
  id: number
  roomGraphVoList: GraphVo[]
  apartmentName: string
  roomNumber: string
  leaseStatus: string
  leaseStartDate: string
  leaseEndDate: string
  sourceType: string
  rent: number
  isRenewable: boolean
}

export interface AgreementDetailVo {
  id: number
  apartmentId: number
  roomId: number
  apartmentName: string
  apartmentGraphVoList: GraphVo[]
  roomNumber: string
  roomGraphVoList: GraphVo[]
  paymentTypeName: string
  leaseTermMonthCount: number
  leaseTermUnit: string
  name: string
  phone: string
  identificationNumber: string
  leaseStartDate: string
  leaseEndDate: string
  rent: number
  deposit: number
  status: string
  sourceType: string
  additionalInfo: string
}

export interface ChatResponseVo {
  message: string
  recommendedApartmentIds: number[]
}
