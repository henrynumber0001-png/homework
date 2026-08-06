export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface PageQuery {
  pageNum: number
  pageSize: number
}

export const AdminRole = { SUPER_ADMIN: 1, STANDARD_ADMIN: 2 } as const
export type AdminRole = (typeof AdminRole)[keyof typeof AdminRole]

export const AdminStatus = { INVITED: 1, ACTIVE: 2, DISABLED: 3, ARCHIVED: 4 } as const
export type AdminStatus = (typeof AdminStatus)[keyof typeof AdminStatus]

export const BankDataScope = { ALL_BANKS: 1, ASSIGNED_BANKS: 2 } as const
export type BankDataScope = (typeof BankDataScope)[keyof typeof BankDataScope]

/** 管理端题库和题目列表共用的排序模式。 */
export const AdminSortMode = {
  UPDATED_TIME_DESC: 1,
  SORT_ORDER_DESC: 2,
  QUESTION_NO_ASC: 3,
} as const
export type AdminSortMode = (typeof AdminSortMode)[keyof typeof AdminSortMode]

export const GroupType = { INTERVIEW: 1, CERTIFICATION: 2 } as const
export type GroupType = (typeof GroupType)[keyof typeof GroupType]

export const QuestionBankStatus = { DRAFT: 1, PUBLISHED: 2, OFFLINE: 3, DELETED: 4 } as const
export type QuestionBankStatus = (typeof QuestionBankStatus)[keyof typeof QuestionBankStatus]

export const QuestionInfoStatus = { DRAFT: 1, PUBLISHED: 2, OFFLINE: 3, DELETED: 4 } as const
export type QuestionInfoStatus = (typeof QuestionInfoStatus)[keyof typeof QuestionInfoStatus]

export const QuestionType = { SINGLE_CHOICE: 1, MULTIPLE: 2, ESSAY: 3 } as const
export type QuestionType = (typeof QuestionType)[keyof typeof QuestionType]

export const QuestionImportStatus = {
  VALIDATING: 1,
  READY: 2,
  INVALID: 3,
  IMPORTING: 4,
  SUCCEEDED: 5,
  FAILED: 6,
  EXPIRED: 7,
} as const
export type QuestionImportStatus =
  (typeof QuestionImportStatus)[keyof typeof QuestionImportStatus]

export const UserInfoStatus = { ACTIVE: 1, DISABLED: 2, BANNED: 3 } as const
export type UserInfoStatus = (typeof UserInfoStatus)[keyof typeof UserInfoStatus]

export const UserAuthIdentityProvider = {
  EMAIL_PASSWORD: 1,
  PHONE_OTP: 2,
  GOOGLE: 3,
  APPLE: 4,
  WECHAT: 5,
  QQ: 6,
} as const
export type UserAuthIdentityProvider =
  (typeof UserAuthIdentityProvider)[keyof typeof UserAuthIdentityProvider]

export const UserAuthIdentityStatus = { PENDING: 1, VERIFIED: 2, DISABLED: 3, UNLINKED: 4 } as const
export type UserAuthIdentityStatus =
  (typeof UserAuthIdentityStatus)[keyof typeof UserAuthIdentityStatus]

export const CommunityRestrictionScope = { POST: 1, COMMENT: 2, BOTH: 3 } as const
export type CommunityRestrictionScope =
  (typeof CommunityRestrictionScope)[keyof typeof CommunityRestrictionScope]

export const HitPostStatus = { PUBLISHED: 1, HIDDEN: 2, DELETED: 3 } as const
export type HitPostStatus = (typeof HitPostStatus)[keyof typeof HitPostStatus]

export const MembershipStatus = { FREE: 0, PREMIUM: 1, PREMIUM_PLUS: 2 } as const
export type MembershipStatus = (typeof MembershipStatus)[keyof typeof MembershipStatus]

export const MembershipType = { PREMIUM: 1, PREMIUM_PLUS: 2 } as const
export type MembershipType = (typeof MembershipType)[keyof typeof MembershipType]

export const MembershipChangeType = {
  ADMIN_GRANT: 1,
  ADMIN_SUSPEND: 2,
  ADMIN_RESUME: 3,
  ADMIN_REVOKE: 4,
} as const
export type MembershipChangeType =
  (typeof MembershipChangeType)[keyof typeof MembershipChangeType]

export const MembershipOrderStatus = { PENDING: 1, PAID: 2, EXPIRED: 4, PAY_FAILED: 6 } as const
export type MembershipOrderStatus =
  (typeof MembershipOrderStatus)[keyof typeof MembershipOrderStatus]

export const MembershipPurchaseType = { FULL: 1, DIFF: 2 } as const
export type MembershipPurchaseType =
  (typeof MembershipPurchaseType)[keyof typeof MembershipPurchaseType]

export const BillingType = { MONTHLY: 1, QUARTERLY: 2, YEARLY: 3 } as const
export type BillingType = (typeof BillingType)[keyof typeof BillingType]

export interface AdminSummary {
  id: number
  email: string
  displayName: string
  role: AdminRole
  status: AdminStatus
}

export interface CurrentAdmin {
  admin: AdminSummary
  permissions: string[]
  bankDataScope: BankDataScope
  assignedBankIds: number[]
  sessionExpiresTime: string
}

export interface AdminLoginResult {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
  admin: AdminSummary
  permissions: string[]
  bankDataScope: BankDataScope
}

export interface InvitationPreview {
  emailMasked: string
  displayName: string
  expiresTime: string
  valid: boolean
}

export interface CategorySubModule {
  id: number
  subModuleName: string
  sortOrder: number
}

export interface CategoryModule {
  id: number
  moduleName: string
  sortOrder: number
  subModules: CategorySubModule[]
}

export interface CategoryGroup {
  id: number
  groupName: string
  groupType: GroupType
  modules: CategoryModule[]
}

export interface NamedId {
  id: number
  name: string
}

export interface QuestionBank {
  id: number
  bankName: string
  groupType: GroupType
  group: NamedId
  module: NamedId
  subModule: NamedId
  status: QuestionBankStatus
  tags: string[]
  /** 变更：原 priority 改为题库人工曝光权重。 */
  sortOrder: number
  questionCount: number
  publishedQuestionCount: number
  viewCount: number
  completeCount: number
  publishedTime?: string
  updatedTime: string
  version: number
}

export interface QuestionOption {
  key: string
  content: string
}

export interface Question {
  id: number
  bankId: number
  questionType: QuestionType
  title: string
  imageUrl?: string
  status: QuestionInfoStatus
  /** 同一题库内从 1 开始的连续题目序号。 */
  questionNo: number
  createdTime: string
  updatedTime: string
  version: number
}

export interface QuestionDetail extends Question {
  groupType: GroupType
  analysis?: string
  options: QuestionOption[]
  correctAnswerKeys: string[]
}

export interface QuestionImageUpload {
  objectKey: string
  previewUrl: string
  previewUrlExpiresTime: string
  uploadExpiresTime: string
}

export interface QuestionImportTask {
  taskId: string
  bankId: number
  fileName: string
  status: QuestionImportStatus
  totalRows: number
  validRows: number
  errorRows: number
  importedRows: number
  failureReason?: string
  expiresTime: string
  finishedTime?: string
}

export interface QuestionImportError {
  rowNumber: number
  fieldName: string
  errorMessage: string
}

export interface ActionResult {
  targetId: number
  action: number
  status: number
  version: number
  updatedTime: string
}

export interface DashboardMetric {
  daily: number
  total: number
}

export interface Dashboard {
  statDate: string
  bankViews: DashboardMetric
  bankCompletedUsers: DashboardMetric
  loginUsers: DashboardMetric
  registeredUsers: DashboardMetric
  postingUsers: DashboardMetric
  paidUsers: {
    premiumDaily: number
    premiumTotal: number
    premiumPlusDaily: number
    premiumPlusTotal: number
  }
  updatedTime: string
}

export interface UserRow {
  id: number
  accountNo: string
  displayName: string
  avatar?: string
  status: UserInfoStatus
  membershipType: MembershipStatus
  registeredTime: string
  version: number
}

export interface UserDetail extends UserRow {
  identities: Array<{
    provider: UserAuthIdentityProvider
    maskedIdentifier: string
    status: UserAuthIdentityStatus
    lastUsedTime?: string
  }>
  communityRestriction?: {
    scope: CommunityRestrictionScope
    startTime: string
    endTime?: string
    reason: string
  }
  postCount: number
  commentCount: number
}

export interface CommunityPost {
  id: number
  userId: number
  displayName: string
  content: string
  tagsJson?: string
  status: HitPostStatus
  commentCount: number
  likeCount: number
  favoriteCount: number
  repostCount: number
  createdTime: string
  version: number
}

export interface CommunityComment {
  id: number
  postId: number
  userId: number
  displayName: string
  parentCommentId?: number
  content: string
  status: HitPostStatus
  likeCount: number
  createdTime: string
  version: number
}

export interface MembershipRow {
  userId: number
  accountNo: string
  displayName: string
  currentType: MembershipStatus
  accessStatus: string
  premiumExpireTime?: string
  premiumPlusExpireTime?: string
  suspended: boolean
  ledgerVersion: number
}

export interface MembershipDetail extends MembershipRow {
  recentChanges: Array<{
    changeType: MembershipChangeType
    membershipType?: MembershipType
    durationMonths?: number
    reason: string
    adminId: number
    createdTime: string
  }>
}

export interface MembershipOrder {
  orderNo: string
  userId: number
  membershipType: MembershipType
  durationMonths: number
  payAmount: number
  currency: string
  orderStatus: MembershipOrderStatus
  payTime?: string
  refundable: false
}

export interface MembershipPlan {
  id: number
  membershipType: MembershipType
  purchaseType: MembershipPurchaseType
  durationMonths: number
  billingType?: BillingType
  price: number
  currency: string
  enabled: boolean
  version: number
}

export interface AdminRow {
  id: number
  email: string
  displayName: string
  role: AdminRole
  status: AdminStatus
  permissions: string[]
  bankDataScope: BankDataScope
  assignedBankIds: number[]
  lastLoginTime?: string
  version: number
}

export interface AuditLog {
  requestId: string
  operatorAdminId: number
  operatorName: string
  module: string
  action: string
  targetType: string
  targetId: string
  reason?: string
  beforeSnapshot?: string
  afterSnapshot?: string
  success: boolean
  failureMessage?: string
  ip: string
  userAgent: string
  createdTime: string
}

/** 题库允许执行的状态动作。 */
export const QuestionBankAction = { PUBLISH: 1, OFFLINE: 2, DELETE: 3 } as const
export type QuestionBankAction = (typeof QuestionBankAction)[keyof typeof QuestionBankAction]

/** 题目允许执行的状态动作。 */
export const QuestionAction = { PUBLISH: 1, OFFLINE: 2, DELETE: 3 } as const
export type QuestionAction = (typeof QuestionAction)[keyof typeof QuestionAction]

/** 社区动态和评论允许执行的治理动作。 */
export const CommunityContentAction = { HIDE: 1, RESTORE: 2, DELETE: 3 } as const
export type CommunityContentAction =
  (typeof CommunityContentAction)[keyof typeof CommunityContentAction]

/** App 用户账号允许执行的状态动作。 */
export const UserAccountAction = { DISABLE: 1, ACTIVATE: 2, BAN: 3, UNBAN: 4 } as const
export type UserAccountAction = (typeof UserAccountAction)[keyof typeof UserAccountAction]

/** 普通管理员账号允许执行的状态动作。 */
export const AdminAccountAction = { DISABLE: 1, ACTIVATE: 2, ARCHIVE: 3 } as const
export type AdminAccountAction = (typeof AdminAccountAction)[keyof typeof AdminAccountAction]

/** 用户会员权益允许执行的管理动作。 */
export const MembershipAction = { GRANT: 1, SUSPEND: 2, RESUME: 3, REVOKE: 4 } as const
export type MembershipAction = (typeof MembershipAction)[keyof typeof MembershipAction]

export interface QuestionBankActionPayload {
  action: QuestionBankAction
  reason: string
  version: number
}

export interface QuestionActionPayload {
  action: QuestionAction
  reason: string
  version: number
}

export interface CommunityContentActionPayload {
  action: CommunityContentAction
  reason: string
  version: number
}

export interface UserAccountActionPayload {
  action: UserAccountAction
  reason: string
  version: number
}

export interface AdminAccountActionPayload {
  action: AdminAccountAction
  reason: string
  version: number
}

export interface MembershipActionPayload {
  action: MembershipAction
  membershipType?: MembershipType
  durationMonths?: number
  reason: string
  ledgerVersion: number
}

export interface QuestionPayload {
  questionType: QuestionType
  title: string
  analysis?: string
  imageObjectKey?: string
  removeImage?: boolean
  options: QuestionOption[]
  correctAnswerKeys: string[]
  reason?: string
  version?: number
}
