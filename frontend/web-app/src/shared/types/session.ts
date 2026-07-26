import type {
  MembershipStatusValue,
  MembershipTypeValue,
} from '@/shared/constants/domain'

export interface UserInfo {
  accountNo: string
  displayName: string
  avatar: string | null
}

export interface MembershipInfo {
  displayName: string
  avatarUrl: string | null
  membershipType: MembershipTypeValue | null
  expiredTime: string | null
  memberStatus: MembershipStatusValue
  baseFreezeExpireTime: string | null
}

export interface MessageUnreadSummary {
  commentsAndMentions: number
  interactions: number
  system: number
  privateMessages: number
  total: number
}
