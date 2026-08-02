import { download, request } from './http'
import type {
  ActionResult,
  AdminAccountActionPayload,
  AdminLoginResult,
  AdminRow,
  AuditLog,
  BankDataScope,
  BillingType,
  CategoryGroup,
  CommunityComment,
  CommunityContentActionPayload,
  CommunityPost,
  CommunityRestrictionScope,
  CurrentAdmin,
  Dashboard,
  InvitationPreview,
  MembershipDetail,
  MembershipActionPayload,
  MembershipOrder,
  MembershipPlan,
  MembershipPurchaseType,
  MembershipRow,
  MembershipType,
  PageResult,
  Question,
  QuestionActionPayload,
  QuestionBank,
  QuestionBankActionPayload,
  QuestionDetail,
  QuestionImageUpload,
  QuestionImportError,
  QuestionImportTask,
  QuestionPayload,
  UserAccountActionPayload,
  UserDetail,
  UserRow,
} from '@/types/admin'

type QueryValue = string | number | boolean | undefined | null
type Query = Record<string, QueryValue>

/** 使用管理员邮箱和密码登录。 */
export function login(payload: { email: string; password: string; turnstileToken?: string }) {
  return request<AdminLoginResult>({ method: 'POST', url: '/auth/login', data: payload })
}

/** 注销当前管理员会话。 */
export function logout() {
  return request<void>({ method: 'POST', url: '/auth/logout' })
}

/** 查询当前管理员、权限和题库数据范围。 */
export function getCurrentAdmin() {
  return request<CurrentAdmin>({ method: 'GET', url: '/auth/me' })
}

/** 查询管理员邀请是否有效及脱敏信息。 */
export function getInvitation(token: string) {
  return request<InvitationPreview>({ method: 'GET', url: `/auth/invitations/${token}` })
}

/** 接受邀请并设置管理员密码。 */
export function acceptInvitation(token: string, payload: { password: string; confirmPassword: string }) {
  return request<void>({ method: 'POST', url: `/auth/invitations/${token}/accept`, data: payload })
}

/** 修改当前管理员密码。 */
export function changePassword(payload: {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}) {
  return request<void>({ method: 'PUT', url: '/auth/password', data: payload })
}

/** 执行高风险操作前进行二次密码认证。 */
export function reauthenticate(password: string, actionScope: string) {
  return request<{ reauthToken: string; expiresTime: string }>({
    method: 'POST',
    url: '/auth/reauth',
    data: { password, actionScope },
  })
}

/** 查询后台只读分类树。 */
export function getCategoryTree() {
  return request<CategoryGroup[]>({ method: 'GET', url: '/categories/tree' })
}

/** 查询业务概览指标。 */
export function getDashboard() {
  return request<Dashboard>({ method: 'GET', url: '/dashboard' })
}

/** 分页查询当前管理员可见的题库。 */
export function listQuestionBanks(params: Query) {
  return request<PageResult<QuestionBank>>({ method: 'GET', url: '/question-banks', params })
}

/** 查询单个题库详情。 */
export function getQuestionBank(bankId: number) {
  return request<QuestionBank>({ method: 'GET', url: `/question-banks/${bankId}` })
}

/** 手动创建一个草稿题库。 */
export function createQuestionBank(payload: {
  subModuleId: number
  bankName: string
  tags: string[]
  // 变更：原 priority 请求字段改为 sortOrder 人工曝光权重。
  sortOrder: number
}) {
  return request<QuestionBank>({ method: 'POST', url: '/question-banks', data: payload })
}

/** 编辑题库基础信息。 */
export function updateQuestionBank(
  bankId: number,
  payload: {
    subModuleId: number
    bankName: string
    tags: string[]
    // 变更：编辑题库时同步使用 sortOrder，不再发送 priority。
    sortOrder: number
    reason: string
    version: number
  },
) {
  return request<QuestionBank>({ method: 'PUT', url: `/question-banks/${bankId}`, data: payload })
}

/** 发布、下架或删除题库。 */
export function actOnQuestionBank(bankId: number, payload: QuestionBankActionPayload) {
  return request<ActionResult>({
    method: 'POST',
    url: `/question-banks/${bankId}/actions`,
    data: payload,
  })
}

/** 分页查询指定题库中的题目。 */
export function listQuestions(bankId: number, params: Query) {
  return request<PageResult<Question>>({
    method: 'GET',
    url: `/question-banks/${bankId}/questions`,
    params,
  })
}

/** 查询题目完整内容。 */
export function getQuestion(bankId: number, questionId: number) {
  return request<QuestionDetail>({
    method: 'GET',
    url: `/question-banks/${bankId}/questions/${questionId}`,
  })
}

/** 在指定题库中创建一条未发布题目。 */
export function createQuestion(bankId: number, payload: QuestionPayload) {
  return request<QuestionDetail>({
    method: 'POST',
    url: `/question-banks/${bankId}/questions`,
    data: payload,
  })
}

/** 编辑题目主体；不传图片字段时保留原图片。 */
export function updateQuestion(bankId: number, questionId: number, payload: QuestionPayload) {
  return request<QuestionDetail>({
    method: 'PUT',
    url: `/question-banks/${bankId}/questions/${questionId}`,
    data: payload,
  })
}

/** 发布、下架或删除单条题目。 */
export function actOnQuestion(bankId: number, questionId: number, payload: QuestionActionPayload) {
  return request<ActionResult>({
    method: 'POST',
    url: `/question-banks/${bankId}/questions/${questionId}/actions`,
    data: payload,
  })
}

/** 原子保存题库内全部有效题目的顺序。 */
export function updateQuestionOrder(
  bankId: number,
  payload: { questionIds: number[]; bankQuestionOrderVersion: number; reason: string },
) {
  return request<{ bankId: number; questionCount: number; bankQuestionOrderVersion: number }>({
    method: 'PUT',
    url: `/question-banks/${bankId}/questions/order`,
    data: payload,
  })
}

/** 上传题目图片并返回临时 COS objectKey。 */
export function uploadQuestionImage(file: File) {
  const data = new FormData()
  data.append('file', file)
  return request<QuestionImageUpload>({
    method: 'POST',
    url: '/uploads/question-images',
    data,
  })
}

/** 下载当前题库类型对应的 Excel 导入模板。 */
export function downloadQuestionImportTemplate(bankId: number) {
  return download({
    method: 'GET',
    url: `/question-banks/${bankId}/question-import-template`,
  })
}

/** 上传 Excel 并同步执行逐行预检。 */
export function createQuestionImportTask(bankId: number, file: File) {
  const data = new FormData()
  data.append('file', file)
  return request<QuestionImportTask>({
    method: 'POST',
    url: '/question-imports',
    params: { bankId },
    data,
    timeout: 60_000,
  })
}

/** 查询题目导入任务状态。 */
export function getQuestionImportTask(taskId: string) {
  return request<QuestionImportTask>({ method: 'GET', url: `/question-imports/${taskId}` })
}

/** 查询题目导入任务全部逐行错误。 */
export function listQuestionImportErrors(taskId: string) {
  return request<QuestionImportError[]>({
    method: 'GET',
    url: `/question-imports/${taskId}/errors`,
  })
}

/** 将 READY 任务中的全部题目写入题库。 */
export function commitQuestionImport(taskId: string, confirmTotalRows: number) {
  return request<QuestionImportTask>({
    method: 'POST',
    url: `/question-imports/${taskId}/commit`,
    data: { confirmTotalRows },
    timeout: 60_000,
  })
}

/** 分页查询 App 用户。 */
export function listUsers(params: Query) {
  return request<PageResult<UserRow>>({ method: 'GET', url: '/users', params })
}

/** 查询 App 用户详情。 */
export function getUser(userId: number) {
  return request<UserDetail>({ method: 'GET', url: `/users/${userId}` })
}

/** 执行 App 用户状态动作。 */
export function actOnUser(
  userId: number,
  payload: UserAccountActionPayload,
  reauthToken?: string,
) {
  return request<ActionResult>({
    method: 'POST',
    url: `/users/${userId}/actions`,
    headers: reauthToken ? { 'X-Admin-Reauth-Token': reauthToken } : undefined,
    data: payload,
  })
}

/** 更新 App 用户的社区访问限制。 */
export function updateUserCommunityAccess(
  userId: number,
  payload: {
    restricted: boolean
    scope?: CommunityRestrictionScope
    endTime?: string
    reason?: string
    version: number
  },
) {
  return request<UserDetail>({
    method: 'PUT',
    url: `/users/${userId}/community-access`,
    data: payload,
  })
}

/** 分页查询社区帖子。 */
export function listCommunityPosts(params: Query) {
  return request<PageResult<CommunityPost>>({ method: 'GET', url: '/community/posts', params })
}

/** 隐藏、恢复或删除社区帖子。 */
export function actOnCommunityPost(postId: number, payload: CommunityContentActionPayload) {
  return request<ActionResult>({
    method: 'POST',
    url: `/community/posts/${postId}/actions`,
    data: payload,
  })
}

/** 分页查询社区评论。 */
export function listCommunityComments(params: Query) {
  return request<PageResult<CommunityComment>>({ method: 'GET', url: '/community/comments', params })
}

/** 隐藏、恢复或删除社区评论。 */
export function actOnCommunityComment(commentId: number, payload: CommunityContentActionPayload) {
  return request<ActionResult>({
    method: 'POST',
    url: `/community/comments/${commentId}/actions`,
    data: payload,
  })
}

/** 分页查询用户会员状态。 */
export function listMemberships(params: Query) {
  return request<PageResult<MembershipRow>>({ method: 'GET', url: '/memberships', params })
}

/** 查询单个用户的会员状态和最近变更记录。 */
export function getMembership(userId: number) {
  return request<MembershipDetail>({ method: 'GET', url: `/memberships/users/${userId}` })
}

/** 执行发放、暂停、恢复或回收会员动作。 */
export function actOnMembership(
  userId: number,
  payload: MembershipActionPayload,
  reauthToken?: string,
) {
  return request<MembershipRow>({
    method: 'POST',
    url: `/memberships/users/${userId}/actions`,
    headers: reauthToken ? { 'X-Admin-Reauth-Token': reauthToken } : undefined,
    data: payload,
  })
}

/** 分页查询会员订单；V1 只读且不支持退款。 */
export function listMembershipOrders(params: Query) {
  return request<PageResult<MembershipOrder>>({ method: 'GET', url: '/membership-orders', params })
}

/** 查询全部会员套餐配置。 */
export function listMembershipPlans() {
  return request<MembershipPlan[]>({ method: 'GET', url: '/membership-plans' })
}

/** 创建会员套餐配置。 */
export function createMembershipPlan(
  payload: {
    membershipType: MembershipType
    purchaseType: MembershipPurchaseType
    durationMonths: number
    billingType?: BillingType
    price: number
    currency: string
    enabled: boolean
    reason: string
  },
  reauthToken: string,
) {
  return request<MembershipPlan>({
    method: 'POST',
    url: '/membership-plans',
    headers: { 'X-Admin-Reauth-Token': reauthToken },
    data: payload,
  })
}

/** 修改会员套餐的价格和启停状态。 */
export function updateMembershipPlan(
  planId: number,
  payload: { price: number; enabled: boolean; reason: string; version: number },
  reauthToken: string,
) {
  return request<MembershipPlan>({
    method: 'PUT',
    url: `/membership-plans/${planId}`,
    headers: { 'X-Admin-Reauth-Token': reauthToken },
    data: payload,
  })
}

/** 分页查询管理员账号。 */
export function listAdmins(params: Query) {
  return request<PageResult<AdminRow>>({ method: 'GET', url: '/admins', params })
}

/** 创建普通管理员邀请并返回可复制链接。 */
export function createAdminInvitation(
  payload: {
    email: string
    displayName: string
    permissions: string[]
    bankDataScope: BankDataScope
    assignedBankIds: number[]
    reason: string
  },
  reauthToken: string,
) {
  return request<{ email: string; invitationUrl: string; expiresTime: string }>({
    method: 'POST',
    url: '/admin-invitations',
    headers: { 'X-Admin-Reauth-Token': reauthToken },
    data: payload,
  })
}

/** 修改普通管理员的权限和题库数据范围。 */
export function updateAdminAccess(
  adminId: number,
  payload: {
    permissions: string[]
    bankDataScope: BankDataScope
    assignedBankIds: number[]
    reason: string
    version: number
  },
  reauthToken: string,
) {
  return request<AdminRow>({
    method: 'PUT',
    url: `/admins/${adminId}/access`,
    headers: { 'X-Admin-Reauth-Token': reauthToken },
    data: payload,
  })
}

/** 禁用、激活或归档普通管理员账号。 */
export function actOnAdmin(
  adminId: number,
  payload: AdminAccountActionPayload,
  reauthToken: string,
) {
  return request<ActionResult>({
    method: 'POST',
    url: `/admins/${adminId}/actions`,
    headers: { 'X-Admin-Reauth-Token': reauthToken },
    data: payload,
  })
}

/** 分页查询后台操作日志。 */
export function listAuditLogs(params: Query) {
  return request<PageResult<AuditLog>>({ method: 'GET', url: '/audit-logs', params })
}
