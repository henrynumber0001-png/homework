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

export interface AdminSummary {
  id: number
  email: string
  displayName: string
  role: 'SUPER_ADMIN' | 'ADMIN'
  status: string
}

export interface CurrentAdmin {
  admin: AdminSummary
  permissions: string[]
  bankDataScope: 'ALL_BANKS' | 'ASSIGNED_BANKS'
  assignedBankIds: number[]
  sessionExpiresTime: string
}

export interface AdminLoginResult {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
  admin: AdminSummary
  permissions: string[]
  bankDataScope: 'ALL_BANKS' | 'ASSIGNED_BANKS'
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
  groupType: 'INTERVIEW' | 'CERTIFICATION'
  modules: CategoryModule[]
}

export interface NamedId {
  id: number
  name: string
}

export interface QuestionBank {
  id: number
  bankName: string
  groupType: 'INTERVIEW' | 'CERTIFICATION'
  group: NamedId
  module: NamedId
  subModule: NamedId
  status: 'DRAFT' | 'PUBLISHED' | 'OFFLINE'
  tags: string[]
  /** 变更：原 priority 改为题库人工曝光权重。 */
  sortOrder: number
  questionCount: number
  releasedQuestionCount: number
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

export type QuestionType = 'ESSAY' | 'SINGLE_CHOICE' | 'MULTIPLE'

export interface Question {
  id: number
  bankId: number
  questionType: QuestionType
  title: string
  imageUrl?: string
  released: boolean
  /** 变更：关系表已删除，题目手动顺序直接来自题目实体。 */
  sortOrder: number
  createdTime: string
  updatedTime: string
  version: number
}

export interface QuestionDetail extends Question {
  groupType: 'INTERVIEW' | 'CERTIFICATION'
  analysis?: string
  options: QuestionOption[]
  correctAnswers: string[]
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
  status: 'VALIDATING' | 'READY' | 'VALIDATION_FAILED' | 'IMPORTING' | 'COMPLETED' | 'FAILED' | 'EXPIRED'
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
  action: string
  status: string
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
  status: string
  membershipType: string
  registeredTime: string
  version: number
}

export interface UserDetail extends UserRow {
  identities: Array<{
    provider: string
    maskedIdentifier: string
    status: string
    lastUsedTime?: string
  }>
  communityRestriction?: {
    scope: string
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
  status: string
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
  status: string
  likeCount: number
  createdTime: string
  version: number
}

export interface MembershipRow {
  userId: number
  accountNo: string
  displayName: string
  currentType: string
  accessStatus: string
  premiumExpireTime?: string
  premiumPlusExpireTime?: string
  suspended: boolean
  ledgerVersion: number
}

export interface MembershipDetail extends MembershipRow {
  recentChanges: Array<{
    changeType: string
    membershipType?: string
    durationMonths?: number
    reason: string
    adminId: number
    createdTime: string
  }>
}

export interface MembershipOrder {
  orderNo: string
  userId: number
  membershipType: string
  durationMonths: number
  payAmount: number
  currency: string
  orderStatus: string
  payTime?: string
  refundable: false
}

export interface MembershipPlan {
  id: number
  membershipType: string
  purchaseType: string
  durationMonths: number
  billingType?: string
  price: number
  currency: string
  enabled: boolean
  version: number
}

export interface AdminRow {
  id: number
  email: string
  displayName: string
  role: string
  status: string
  permissions: string[]
  bankDataScope: 'ALL_BANKS' | 'ASSIGNED_BANKS'
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

export interface ResourceActionPayload {
  action: string
  reason: string
  version: number
}

export interface QuestionPayload {
  questionType: QuestionType
  title: string
  analysis?: string
  imageObjectKey?: string
  removeImage?: boolean
  options: QuestionOption[]
  correctAnswers: string[]
  reason?: string
  version?: number
}
