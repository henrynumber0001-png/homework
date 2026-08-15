import type {
  GenderValue,
  MembershipTypeValue,
  QuestionTypeValue,
} from '@/shared/constants/domain'
import type { HitComment, HitPost } from '@/features/hit/types'

export interface UserCenterCounts {
  followerCount: number
  followingCount: number
  answeredQuestionCount: number
  learnedBankCount: number
  studyHours: number
  wrongQuestionCount: number
  favoriteQuestionCount: number
  noteCount: number
}

export interface UserCenterData {
  userInfoVO: {
    accountNo: string
    displayName: string
    avatarUrl: string | null
    bannerUrl: string | null
    companyOrSchool: string | null
    subTechDirectionId: number | null
    gender: GenderValue | null
    introduction: string | null
  }
  membershipActive: boolean
  membershipType: MembershipTypeValue | null
  aiFeaturesEnabled: boolean
  countsVO: UserCenterCounts
}

export interface UserCenterActivity {
  activityType:
    'POST' | 'REPOST' | 'COMMENT' | 'LIKED_POST' | 'LIKED_COMMENT' | 'FAVORITE'
  activityTime: string
  post: HitPost
  comment: HitComment | null
}

export interface FollowerListItem {
  followerUserId: number
  followerDisplayName: string
  followerAvatarUrl: string | null
  mutualFollow: boolean
  blocked: boolean
}

export interface FollowingListItem {
  followeeUserId: number
  followeeDisplayName: string
  followeeAvatarUrl: string | null
  mutualFollow: boolean
  blocked: boolean
}

export type FollowListKind = 'followers' | 'following'

export type UserImageType = 'avatar' | 'banner'

export interface UserImageUpload {
  imageObjectKey: string
  previewUrl: string
}

export interface UserProfile {
  avatarUrl: string | null
  displayName: string
  companyOrSchool: string | null
  subTechDirectionId: number | null
  gender: GenderValue | null
  introduction: string | null
  version: number
}

export interface SubTechDirectionOption {
  subTechDirectionId: number
  subTechDirectionName: string
}

export interface TechDirectionOption {
  directionId: number
  directionName: string
  subTechDirectionTreeVOList: SubTechDirectionOption[]
}

export interface UserProfileOptions {
  techDirectionTreeVOList: TechDirectionOption[]
}

export interface EditProfileInput {
  displayName: string
  companyOrSchool: string | null
  subTechDirectionId: number | null
  gender: GenderValue | null
  introduction: string | null
  version: number
}

export type EditedProfile = EditProfileInput

export interface LearningCalendarItem {
  date: string
  studyMinutes: number
}

export interface UserQuestionBank {
  bankId: number
  bankName: string
  tagNames: string[]
  wrongQuestionCount?: number
  favoriteQuestionCount?: number
  noteCount?: number
}

export interface UserQuestionListItem {
  questionId: number
  title: string
  questionType: QuestionTypeValue
  isAvailable: boolean
}

export interface UserQuestionDetail {
  questionId: number
  title: string
  questionType: QuestionTypeValue
  options?: string[]
  imageUrl?: string | null
  chosenOptions?: string[]
  correctAnswer?: string[]
  analysis?: string
  content?: string
  noteContent?: string
  updatedTime?: string
  answeredTime?: string
}

export type LibraryKind = 'wrong' | 'favorite' | 'note'
