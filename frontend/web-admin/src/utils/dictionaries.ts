export const questionTypeLabels: Record<string, string> = {
  ESSAY: '简答题',
  SINGLE_CHOICE: '单选题',
  MULTIPLE: '多选题',
}

export const bankStatusLabels: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  OFFLINE: '已下架',
}

export const groupTypeLabels: Record<string, string> = {
  INTERVIEW: '面试题库',
  CERTIFICATION: '认证题库',
}

export const membershipTypeLabels: Record<string, string> = {
  NONE: '普通用户',
  PREMIUM: '会员',
  PREMIUM_PLUS: '高级会员',
}

export const permissionOptions = [
  ['dashboard:view', '查看概览'],
  ['bank:view', '查看题库'],
  ['bank:create', '创建题库'],
  ['bank:update', '编辑题库'],
  ['bank:publish', '发布/下架题库'],
  ['bank:delete', '删除/恢复题库'],
  ['question:view', '查看题目'],
  ['question:create', '创建题目'],
  ['question:update', '编辑题目'],
  ['question:publish', '发布/下架题目'],
  ['question:delete', '删除/恢复题目'],
  ['question:sort', '题目排序'],
  ['question:import', 'Excel 导入'],
  ['user:view', '查看用户'],
  ['user:manage', '用户治理'],
  ['community:moderate', '社区治理'],
  ['membership:view', '查看会员'],
  ['membership:manage', '管理会员'],
  ['audit:view', '查看操作日志'],
] as const
