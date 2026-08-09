import type {
  MembershipTypeValue,
  QuestionTypeValue,
} from '@/shared/constants/domain'

export interface UserCenterCounts {
  followerCount: number
  followingCount: number
  postCount: number
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
  }
  membershipActive: boolean
  membershipType: MembershipTypeValue | null
  aiFeaturesEnabled: boolean
  countsVO: UserCenterCounts
}

export type UserImageType = 'avatar' | 'banner'

export interface UserImageUpload {
  imageObjectKey: string
  previewUrl: string
}

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
