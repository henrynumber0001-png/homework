import type {
  MembershipStatusValue,
  MembershipTypeValue,
} from '@/shared/constants/domain'

export interface MembershipSku {
  planId: number
  purchaseType: number
  billingType: number
  durationMonths: number
  price: number
  currency: string
}

export interface MembershipPlanCard {
  membershipType: MembershipTypeValue
  fullPurchaseOptions: MembershipSku[]
}

export interface MembershipDetailPage {
  memberStatus: MembershipStatusValue
  currentMembershipType: MembershipTypeValue | null
  currentExpireTime: string | null
  baseFreezeExpireTime: string | null
  fullPurchaseCards: MembershipPlanCard[]
  diffUpgradeAvailable: boolean
  maxDiffUpgradeMonths: number
  diffUpgradeOptions: MembershipSku[]
}

export interface MembershipOrderCreate {
  orderNo: string
  orderStatus: number
  amountDue: number
  currency: string
  paymentExpiredTime: string
  codeUrl: string
}

export interface MembershipOrderHistory {
  orderNo: string
  action: number
  membershipType: MembershipTypeValue
  billingType: number
  durationMonths: number
  payAmount: number
  currency: string
  orderStatus: number
  periodEnd: string | null
  payTime: string | null
}
