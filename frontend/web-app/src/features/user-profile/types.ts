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
  canSendPrivateMessage: boolean
  chatboxId: number | null
}

export interface FollowState {
  active: boolean
  followerCount: number
  mutualFollow: boolean
}
