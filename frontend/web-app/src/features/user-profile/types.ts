import type { HitComment, HitPost } from '@/features/hit/types'
import type { MembershipInfo } from '@/shared/types/session'

export interface PublicUserProfile {
  userId: number
  membershipInfoVO: MembershipInfo
  followerCount: number
  followingCount: number
  postCount: number
  answeredQuestionCount: number
  learnedBankCount: number
  studyHours: number
  receivedTotalActionCount: number
  self: boolean
  followedByCurrentUser: boolean | null
  mutualFollow: boolean
  canFollow: boolean
  canSendPrivateMessage: boolean
  chatboxId: number | null
}

export interface PublicUserActivity {
  activityType:
    'POST' | 'REPOST' | 'COMMENT' | 'LIKED_POST' | 'LIKED_COMMENT' | 'FAVORITE'
  activityTime: string
  post: HitPost
  comment: HitComment | null
}

export interface FollowState {
  active: boolean
  followerCount: number
  mutualFollow: boolean
}
