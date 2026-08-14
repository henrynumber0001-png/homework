export const QUESTION_BANK_GROUP_ID = {
  INTERVIEW: 1,
  CERTIFICATION: 2,
} as const

export const GroupType = {
  INTERVIEW: 1,
  CERTIFICATION: 2,
} as const

export const SortType = {
  HOT: 1,
  LATEST: 2,
} as const

export const QuestionType = {
  SINGLE_CHOICE: 1,
  MULTIPLE: 2,
  ESSAY: 3,
} as const

export const MembershipType = {
  PREMIUM: 1,
  PREMIUM_PLUS: 2,
} as const

export const MembershipStatus = {
  FREE: 0,
  PREMIUM: 1,
  PREMIUM_PLUS: 2,
} as const

export const HitActionType = {
  LIKE: 1,
  FAVORITE: 2,
  REPOST: 3,
} as const

export const ActionStatus = {
  ACTIVATE: 1,
  DEACTIVATE: 2,
} as const

export const ExamSessionStatus = {
  IN_PROGRESS: 1,
  SUBMITTED: 2,
  EXPIRED: 3,
} as const

export const MembershipOrderStatus = {
  PENDING: 1,
  PAID: 2,
  EXPIRED: 4,
  PAY_FAILED: 6,
} as const

export const Gender = {
  MALE: 1,
  FEMALE: 2,
} as const

export type GroupTypeValue = (typeof GroupType)[keyof typeof GroupType]
export type SortTypeValue = (typeof SortType)[keyof typeof SortType]
export type QuestionTypeValue = (typeof QuestionType)[keyof typeof QuestionType]
export type MembershipTypeValue =
  (typeof MembershipType)[keyof typeof MembershipType]
export type MembershipStatusValue =
  (typeof MembershipStatus)[keyof typeof MembershipStatus]
export type ActionStatusValue = (typeof ActionStatus)[keyof typeof ActionStatus]
export type GenderValue = (typeof Gender)[keyof typeof Gender]
