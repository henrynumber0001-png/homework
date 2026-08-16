import type {
  GenderValue,
  MembershipStatusValue,
  MembershipTypeValue,
} from '@/shared/constants/domain'

export interface PublicUserProfile {
  userId: number
  userInfo: {
    accountNo?: string
    displayName: string
    avatarUrl: string | null
    bannerUrl: string | null
    companyOrSchool?: string | null
    subTechDirectionId?: number | null
    gender?: GenderValue | null
    introduction?: string | null
  }
  membershipStatus: MembershipStatusValue
  membershipType: MembershipTypeValue | null
  followerCount: number
  followingCount: number
  answeredQuestionCount?: number
  learnedBankCount?: number
  studyHours?: number
  self: boolean
  followedByCurrentUser: boolean | null
  mutualFollow: boolean
  blocked: boolean
  blockedByCurrentUser: boolean
  canSendPrivateMessage: boolean
  chatboxId: number | null
}

export interface FollowState {
  active: boolean
  followerCount: number
  mutualFollow: boolean
}

export const BlockStatus = {
  ACTIVATE: 1,
  DEACTIVATE: 2,
} as const

export type BlockStatusValue = (typeof BlockStatus)[keyof typeof BlockStatus]

export interface BlockResult {
  self: boolean
  blocked: boolean
  profileUserId: number
  blockStatus: BlockStatusValue
}
