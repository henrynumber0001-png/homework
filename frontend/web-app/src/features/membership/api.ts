import { apiRequest } from '@/shared/api/client'
import type {
  AllMembershipOrder,
  MembershipDetailPage,
  MembershipOrderCreate,
  PendingMembershipOrder,
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

export function getPendingMembershipOrders() {
  return apiRequest<PendingMembershipOrder[]>({
    url: '/app/membership/pending-orders',
  })
}

export function getAllMembershipOrders() {
  return apiRequest<AllMembershipOrder[]>({
    url: '/app/membership/all-orders',
  })
}
