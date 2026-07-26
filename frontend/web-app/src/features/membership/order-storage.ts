import type { MembershipOrderCreate } from '@/features/membership/types'

const PREFIX = 'homework_membership_order_'

export function savePendingOrder(order: MembershipOrderCreate) {
  sessionStorage.setItem(`${PREFIX}${order.orderNo}`, JSON.stringify(order))
}

export function getPendingOrder(orderNo: string) {
  const value = sessionStorage.getItem(`${PREFIX}${orderNo}`)
  if (!value) return null

  try {
    return JSON.parse(value) as MembershipOrderCreate
  } catch {
    sessionStorage.removeItem(`${PREFIX}${orderNo}`)
    return null
  }
}

export function clearPendingOrder(orderNo: string) {
  sessionStorage.removeItem(`${PREFIX}${orderNo}`)
}
