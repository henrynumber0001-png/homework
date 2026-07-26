import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    permission?: string
    title?: string
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/invitation/:token',
    name: 'invitation',
    component: () => import('@/views/auth/InvitationView.vue'),
    meta: { public: true, title: '接受邀请' },
  },
  {
    path: '/admin/invitation',
    name: 'invitation-query',
    component: () => import('@/views/auth/InvitationView.vue'),
    meta: { public: true, title: '接受邀请' },
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    children: [
      { path: '', redirect: '/question-banks' },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { permission: 'dashboard:view', title: '数据概览' },
      },
      {
        path: 'question-banks',
        name: 'question-banks',
        component: () => import('@/views/banks/BankListView.vue'),
        meta: { permission: 'bank:view', title: '题库管理' },
      },
      {
        path: 'question-banks/:bankId',
        name: 'bank-workspace',
        component: () => import('@/views/banks/BankWorkspaceView.vue'),
        meta: { permission: 'question:view', title: '题库工作台' },
      },
      {
        path: 'question-banks/:bankId/questions/new',
        name: 'question-create',
        component: () => import('@/views/questions/QuestionFormView.vue'),
        meta: { permission: 'question:create', title: '创建题目' },
      },
      {
        path: 'question-banks/:bankId/questions/:questionId/edit',
        name: 'question-edit',
        component: () => import('@/views/questions/QuestionFormView.vue'),
        meta: { permission: 'question:update', title: '编辑题目' },
      },
      {
        path: 'question-banks/:bankId/import',
        name: 'question-import',
        component: () => import('@/views/questions/QuestionImportView.vue'),
        meta: { permission: 'question:import', title: 'Excel 导入题目' },
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('@/views/UsersView.vue'),
        meta: { permission: 'user:view', title: '用户管理' },
      },
      {
        path: 'community',
        name: 'community',
        component: () => import('@/views/CommunityView.vue'),
        meta: { permission: 'community:moderate', title: '社区治理' },
      },
      {
        path: 'memberships',
        name: 'memberships',
        component: () => import('@/views/MembershipsView.vue'),
        meta: { permission: 'membership:view', title: '会员与订单' },
      },
      {
        path: 'admins',
        name: 'admins',
        component: () => import('@/views/AdminsView.vue'),
        meta: { permission: 'admin:manage', title: '管理员' },
      },
      {
        path: 'audit-logs',
        name: 'audit-logs',
        component: () => import('@/views/AuditLogsView.vue'),
        meta: { permission: 'audit:view', title: '操作日志' },
      },
      {
        path: 'account',
        name: 'account',
        component: () => import('@/views/AccountView.vue'),
        meta: { title: '账号设置' },
      },
      {
        path: '403',
        name: 'forbidden',
        component: () => import('@/views/ForbiddenView.vue'),
        meta: { title: '无权访问' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { public: true, title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} · Homework 管理后台` : 'Homework 管理后台'
  if (to.meta.public) return true

  const auth = useAuthStore()
  if (!auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (!auth.current) {
    try {
      await auth.loadCurrent()
    } catch {
      auth.clearSession()
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }

  if (!auth.hasPermission(to.meta.permission)) {
    return { name: 'forbidden' }
  }
  return true
})

window.addEventListener('admin:unauthorized', () => {
  const auth = useAuthStore()
  auth.clearSession()
  if (router.currentRoute.value.name !== 'login') {
    void router.replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
  }
})

export default router
