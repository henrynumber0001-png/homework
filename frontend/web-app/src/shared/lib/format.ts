import { format, formatDistanceToNow, isValid, parseISO } from 'date-fns'
import { zhCN } from 'date-fns/locale'

export function formatCount(value: number | null | undefined) {
  const count = value ?? 0
  if (count >= 10_000) {
    return `${Number((count / 10_000).toFixed(1))}万`
  }
  return String(count)
}

export function formatRate(value: number | null | undefined) {
  if (value == null) return '--'
  const percent = value <= 1 ? value * 100 : value
  return `${Number(percent.toFixed(1))}%`
}

export function formatRelativeTime(value: string | null | undefined) {
  if (!value) return ''
  const date = parseISO(value)
  if (!isValid(date)) return ''
  return formatDistanceToNow(date, { addSuffix: true, locale: zhCN })
}

export function formatDateTime(value: string | null | undefined) {
  if (!value) return '--'
  const date = parseISO(value)
  return isValid(date) ? format(date, 'yyyy-MM-dd HH:mm') : '--'
}

export function formatMoney(value: number, currency = 'CNY') {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(value)
}
