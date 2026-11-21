/**
 * 后端枚举都是 @JsonValue 的 code，前端统一在这里做 code <-> 文案 的映射。
 */

export interface DictItem {
  label: string
  value: number
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
}

/** 发布状态 ReleaseStatus */
export const RELEASE_STATUS: DictItem[] = [
  { label: '已发布', value: 1, type: 'success' },
  { label: '未发布', value: 0, type: 'info' }
]

/** 账号状态 BaseStatus */
export const BASE_STATUS: DictItem[] = [
  { label: '正常', value: 1, type: 'success' },
  { label: '禁用', value: 0, type: 'danger' }
]

/** 物品类型 ItemType */
export const ITEM_TYPE: DictItem[] = [
  { label: '公寓', value: 1, type: 'primary' },
  { label: '房间', value: 2, type: 'warning' }
]

/** 预约状态 AppointmentStatus */
export const APPOINTMENT_STATUS: DictItem[] = [
  { label: '待看房', value: 1, type: 'warning' },
  { label: '已取消', value: 2, type: 'info' },
  { label: '已看房', value: 3, type: 'success' }
]

/** 租约状态 LeaseStatus */
export const LEASE_STATUS: DictItem[] = [
  { label: '签约待确认', value: 1, type: 'warning' },
  { label: '已签约', value: 2, type: 'success' },
  { label: '已取消', value: 3, type: 'info' },
  { label: '已到期', value: 4, type: 'info' },
  { label: '退租待确认', value: 5, type: 'warning' },
  { label: '已退租', value: 6, type: 'danger' },
  { label: '续约待确认', value: 7, type: 'primary' }
]

/** 租约来源 LeaseSourceType */
export const LEASE_SOURCE: DictItem[] = [
  { label: '新签', value: 1, type: 'primary' },
  { label: '续约', value: 2, type: 'warning' }
]

/** 员工类型 SystemUserType */
export const SYSTEM_USER_TYPE: DictItem[] = [
  { label: '管理员', value: 0, type: 'danger' },
  { label: '普通员工', value: 1, type: 'primary' }
]

function find(list: DictItem[], value?: number | string | null): DictItem | undefined {
  if (value === undefined || value === null || value === '') return undefined
  return list.find(i => i.value === Number(value))
}

export function dictLabel(list: DictItem[], value?: number | string | null): string {
  return find(list, value)?.label ?? '-'
}

export function dictType(list: DictItem[], value?: number | string | null): DictItem['type'] {
  return find(list, value)?.type ?? 'info'
}

/** el-select 用 */
export function dictOptions(list: DictItem[]) {
  return list.map(i => ({ label: i.label, value: i.value }))
}
