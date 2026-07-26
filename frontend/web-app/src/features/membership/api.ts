import { apiRequest } from '@/shared/api/client'
import type {
  MembershipDetailPage,
  MembershipOrderCreate,
  MembershipOrderHistory,
} from '@/features/membership/types'

export function getMembershipPlans() {
  return apiRequest<MembershipDetailPage>({
    url: '/app/membership',
  })
}

export function createMembershipOrder(planId: number, idempotencyKey: string) {
  return apiRequest<MembershipOrderCreate>({
    url: '/app/membership/orders',
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    data: { planId },
  })
}

export function getMembershipOrderStatus(orderNo: string) {
  return apiRequest<number>({
    url: `/app/membership/orders/${orderNo}`,
  })
}

export function getMembershipOrderHistory() {
  return apiRequest<MembershipOrderHistory[]>({
    url: '/app/membership/orders',
  })
}
