import {
  AdminAccountAction,
  AdminStatus,
  BillingType,
  CommunityRestrictionScope,
  GroupType,
  HitPostStatus,
  MembershipOrderStatus,
  MembershipChangeType,
  MembershipPurchaseType,
  MembershipStatus,
  QuestionBankStatus,
  QuestionImportStatus,
  QuestionType,
  UserAuthIdentityProvider,
  UserAuthIdentityStatus,
  UserInfoStatus,
} from '@/types/admin'

export const questionTypeLabels: Record<number, string> = {
  [QuestionType.ESSAY]: '简答题',
  [QuestionType.SINGLE_CHOICE]: '单选题',
  [QuestionType.MULTIPLE]: '多选题',
}

export const bankStatusLabels: Record<number, string> = {
  [QuestionBankStatus.DRAFT]: '草稿',
  [QuestionBankStatus.PUBLISHED]: '已发布',
  [QuestionBankStatus.OFFLINE]: '已下架',
  [QuestionBankStatus.DELETED]: '已删除',
}

export const bankStatusNames: Record<number, string> = {
  [QuestionBankStatus.DRAFT]: 'DRAFT',
  [QuestionBankStatus.PUBLISHED]: 'PUBLISHED',
  [QuestionBankStatus.OFFLINE]: 'OFFLINE',
  [QuestionBankStatus.DELETED]: 'DELETED',
}

export const groupTypeLabels: Record<number, string> = {
  [GroupType.INTERVIEW]: '面试题库',
  [GroupType.CERTIFICATION]: '认证题库',
}

export const membershipTypeLabels: Record<number, string> = {
  [MembershipStatus.FREE]: '普通用户',
  [MembershipStatus.PREMIUM]: '会员',
  [MembershipStatus.PREMIUM_PLUS]: '高级会员',
}

export const adminStatusNames: Record<number, string> = {
  [AdminStatus.INVITED]: 'INVITED',
  [AdminStatus.ACTIVE]: 'ACTIVE',
  [AdminStatus.DISABLED]: 'DISABLED',
  [AdminStatus.ARCHIVED]: 'ARCHIVED',
}

export const userStatusNames: Record<number, string> = {
  [UserInfoStatus.ACTIVE]: 'ACTIVE',
  [UserInfoStatus.DISABLED]: 'DISABLED',
  [UserInfoStatus.BANNED]: 'BANNED',
}

export const userIdentityProviderLabels: Record<number, string> = {
  [UserAuthIdentityProvider.EMAIL_PASSWORD]: '邮箱密码',
  [UserAuthIdentityProvider.PHONE_OTP]: '手机验证码',
  [UserAuthIdentityProvider.GOOGLE]: 'Google',
  [UserAuthIdentityProvider.APPLE]: 'Apple',
  [UserAuthIdentityProvider.WECHAT]: '微信',
  [UserAuthIdentityProvider.QQ]: 'QQ',
}

export const userIdentityStatusLabels: Record<number, string> = {
  [UserAuthIdentityStatus.PENDING]: '待验证',
  [UserAuthIdentityStatus.VERIFIED]: '已验证',
  [UserAuthIdentityStatus.DISABLED]: '已禁用',
  [UserAuthIdentityStatus.UNLINKED]: '已解绑',
}

export const communityRestrictionScopeLabels: Record<number, string> = {
  [CommunityRestrictionScope.POST]: '发帖',
  [CommunityRestrictionScope.COMMENT]: '评论',
  [CommunityRestrictionScope.BOTH]: '发帖和评论',
}

export const adminAccountActionLabels: Record<number, string> = {
  [AdminAccountAction.DISABLE]: '禁用',
  [AdminAccountAction.ACTIVATE]: '激活',
  [AdminAccountAction.ARCHIVE]: '归档',
}

export const communityStatusNames: Record<number, string> = {
  [HitPostStatus.PUBLISHED]: 'PUBLISHED',
  [HitPostStatus.HIDDEN]: 'HIDDEN',
  [HitPostStatus.DELETED]: 'DELETED',
}

export const importStatusNames: Record<number, string> = {
  [QuestionImportStatus.VALIDATING]: 'VALIDATING',
  [QuestionImportStatus.READY]: 'READY',
  [QuestionImportStatus.INVALID]: 'VALIDATION_FAILED',
  [QuestionImportStatus.IMPORTING]: 'IMPORTING',
  [QuestionImportStatus.SUCCEEDED]: 'COMPLETED',
  [QuestionImportStatus.FAILED]: 'FAILED',
  [QuestionImportStatus.EXPIRED]: 'EXPIRED',
}

export const membershipOrderStatusNames: Record<number, string> = {
  [MembershipOrderStatus.PENDING]: 'PENDING',
  [MembershipOrderStatus.PAID]: 'PAID',
  [MembershipOrderStatus.EXPIRED]: 'EXPIRED',
  [MembershipOrderStatus.PAY_FAILED]: 'FAILED',
}

export const membershipChangeTypeLabels: Record<number, string> = {
  [MembershipChangeType.ADMIN_GRANT]: '管理员发放',
  [MembershipChangeType.ADMIN_SUSPEND]: '管理员暂停',
  [MembershipChangeType.ADMIN_RESUME]: '管理员恢复',
  [MembershipChangeType.ADMIN_REVOKE]: '管理员收回',
}

export const membershipPurchaseTypeLabels: Record<number, string> = {
  [MembershipPurchaseType.FULL]: '完整购买',
  [MembershipPurchaseType.DIFF]: '补差升级',
}

export const billingTypeLabels: Record<number, string> = {
  [BillingType.MONTHLY]: '按月',
  [BillingType.QUARTERLY]: '按季',
  [BillingType.YEARLY]: '按年',
}

export const permissionOptions = [
  ['dashboard:view', '查看概览'],
  ['bank:view', '查看题库'],
  ['bank:create', '创建题库'],
  ['bank:update', '编辑题库'],
  ['bank:publish', '发布/下架题库'],
  ['bank:delete', '删除题库'],
  ['question:view', '查看题目'],
  ['question:create', '创建题目'],
  ['question:update', '编辑题目'],
  ['question:publish', '发布/下架题目'],
  ['question:delete', '删除题目'],
  ['question:sort', '题目排序'],
  ['question:import', 'Excel 导入'],
  ['user:view', '查看用户'],
  ['user:manage', '用户治理'],
  ['community:moderate', '社区治理'],
  ['membership:view', '查看会员'],
  ['membership:manage', '管理会员'],
  ['audit:view', '查看操作日志'],
] as const
