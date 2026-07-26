<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Collection,
  DataAnalysis,
  Document,
  Fold,
  Lock,
  Postcard,
  Setting,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { showApiError } from '@/api/http'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activeMenu = computed(() => {
  if (route.path.startsWith('/question-banks')) return '/question-banks'
  return route.path
})

const menuItems = [
  { path: '/dashboard', label: '数据概览', icon: DataAnalysis, permission: 'dashboard:view' },
  { path: '/question-banks', label: '题库与题目', icon: Collection, permission: 'bank:view' },
  { path: '/users', label: '用户管理', icon: User, permission: 'user:view' },
  { path: '/community', label: '社区治理', icon: Postcard, permission: 'community:moderate' },
  { path: '/memberships', label: '会员与订单', icon: Document, permission: 'membership:view' },
  { path: '/admins', label: '管理员', icon: UserFilled, permission: 'admin:manage' },
  { path: '/audit-logs', label: '操作日志', icon: Lock, permission: 'audit:view' },
]

async function handleLogout(): Promise<void> {
  try {
    await auth.logout()
    await router.replace('/login')
  } catch (error) {
    showApiError(error)
  }
}
</script>

<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">H</div>
        <div>
          <strong>Homework</strong>
          <span>管理后台</span>
        </div>
      </div>

      <el-menu :default-active="activeMenu" router>
        <template v-for="item in menuItems" :key="item.path">
          <el-menu-item v-if="auth.hasPermission(item.permission)" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </template>
      </el-menu>

      <div class="sidebar-note">
        <el-icon><Fold /></el-icon>
        <span>桌面管理端 · V1</span>
      </div>
    </aside>

    <section class="main-shell">
      <header class="topbar">
        <div class="topbar-title">{{ route.meta.title }}</div>
        <el-dropdown trigger="click">
          <button class="account-trigger">
            <span class="avatar">{{ auth.admin?.displayName?.slice(0, 1).toUpperCase() }}</span>
            <span>
              <strong>{{ auth.admin?.displayName }}</strong>
              <small>{{ auth.admin?.role === 'SUPER_ADMIN' ? '超级管理员' : '管理员' }}</small>
            </span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/account')">
                <el-icon><Setting /></el-icon>账号设置
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>
      <main class="content">
        <router-view />
      </main>
    </section>
  </div>
</template>

<style scoped>
.admin-shell {
  display: flex;
  min-height: 100vh;
  background: #f6f7fb;
}

.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 10;
  display: flex;
  flex-direction: column;
  width: 232px;
  padding: 0 14px 18px;
  color: #dbe3f4;
  background: #17213a;
}

.brand {
  display: flex;
  gap: 12px;
  align-items: center;
  height: 76px;
  padding: 0 10px;
}

.brand-mark {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  color: #fff;
  font-size: 19px;
  font-weight: 800;
  background: #4d72ee;
  border-radius: 10px;
  box-shadow: 0 8px 18px rgb(49 87 213 / 32%);
}

.brand strong,
.brand span {
  display: block;
}

.brand strong {
  color: #fff;
  font-size: 16px;
}

.brand span {
  margin-top: 2px;
  color: #8492ae;
  font-size: 12px;
}

.el-menu {
  flex: 1;
  background: transparent;
  border-right: 0;
}

.el-menu-item {
  height: 46px;
  margin-bottom: 4px;
  color: #aeb9cd;
  border-radius: 9px;
}

.el-menu-item:hover {
  color: #fff;
  background: rgb(255 255 255 / 7%);
}

.el-menu-item.is-active {
  color: #fff;
  background: #3157d5;
}

.sidebar-note {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 12px;
  color: #71809e;
  font-size: 12px;
  border-top: 1px solid rgb(255 255 255 / 8%);
}

.main-shell {
  width: calc(100% - 232px);
  margin-left: 232px;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 30px;
  background: rgb(255 255 255 / 93%);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(10px);
}

.topbar-title {
  color: var(--text-secondary);
  font-size: 14px;
}

.account-trigger {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 5px 8px;
  text-align: left;
  background: transparent;
  border: 0;
  border-radius: 9px;
  cursor: pointer;
}

.account-trigger:hover {
  background: #f4f6fa;
}

.account-trigger strong,
.account-trigger small {
  display: block;
}

.account-trigger strong {
  color: var(--text-primary);
  font-size: 13px;
}

.account-trigger small {
  margin-top: 2px;
  color: var(--text-secondary);
  font-size: 11px;
}

.avatar {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  color: #3157d5;
  font-weight: 700;
  background: #e9edff;
  border-radius: 50%;
}

.content {
  max-width: 1600px;
  margin: 0 auto;
  padding: 28px 30px 48px;
}
</style>
