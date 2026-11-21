/* =============================================================================
 *  展示用的格式化工具 & 状态字典（与后端枚举 code 对齐）
 * ========================================================================== */

/** 预约状态：AppointmentStatus 1 待看房 / 2 已取消 / 3 已看房 */
export const APPOINTMENT_STATUS: Record<number, string> = {
  1: '待看房',
  2: '已取消',
  3: '已看房'
}

/** 租约状态：LeaseStatus 1~7 */
export const LEASE_STATUS: Record<number, string> = {
  1: '签约待确认',
  2: '已签约',
  3: '已取消',
  4: '已到期',
  5: '退租待确认',
  6: '已退租',
  7: '续约待确认'
}

/** 租约来源：LeaseSourceType 1 新签 / 2 续约 */
export const LEASE_SOURCE: Record<number, string> = {
  1: '新签',
  2: '续约'
}

type TagType = 'success' | 'warning' | 'info' | 'danger' | 'primary'

export function appointmentTag(status?: number): TagType {
  if (status === 1) return 'warning'
  if (status === 3) return 'success'
  return 'info'
}

export function leaseTag(status?: number): TagType {
  if (status === 2) return 'success'
  if (status === 1 || status === 5 || status === 7) return 'warning'
  if (status === 3) return 'danger'
  return 'info'
}

/** 金额：¥4800 */
export function money(value?: number | string | null): string {
  if (value === null || value === undefined || value === '') return '-'
  const n = Number(value)
  if (Number.isNaN(n)) return '-'
  return `¥${n.toFixed(0)}`
}

/** 日期：2024-05-01（后端可能是 "2024-05-01" 或带时间的 ISO） */
export function dateOnly(value?: string | null): string {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

/** 时间：2024-05-01 14:30 */
export function dateTime(value?: string | null): string {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

/**
 * 租期展示。
 * 后端 lease_term 是 { monthCount, unit }，unit 可能是 'MONTH'/'DAY'/'YEAR' 之类的枚举 code。
 */
export function termText(monthCount?: number | null, unit?: string | null): string {
  if (!monthCount) return '-'
  const u = (unit || '').toUpperCase()
  const suffix = u.includes('DAY') ? '天' : u.includes('YEAR') ? '年' : '个月'
  return `${monthCount}${suffix}`
}

/** 支付方式补充说明：押一付三 => "押一付三" */
export function paymentText(name?: string | null, payMonthCount?: number | null): string {
  if (!name) return '-'
  return payMonthCount ? `${name}（每次付 ${payMonthCount} 个月）` : name
}

/** 取第一张图 */
export function firstImage(list?: { url: string }[] | null, fallback = ''): string {
  return list && list.length ? list[0].url : fallback
}
