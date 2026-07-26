import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as adminApi from '@/api/admin'
import { TOKEN_STORAGE_KEY } from '@/api/http'
import type { CurrentAdmin } from '@/types/admin'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_STORAGE_KEY) || '')
  const current = ref<CurrentAdmin | null>(null)
  const loading = ref(false)

  const admin = computed(() => current.value?.admin ?? null)
  const permissions = computed(() => current.value?.permissions ?? [])
  const isSuperAdmin = computed(() => admin.value?.role === 'SUPER_ADMIN')

  function hasPermission(permission?: string): boolean {
    if (!permission) return true
    return isSuperAdmin.value || permissions.value.includes(permission)
  }

  async function login(email: string, password: string): Promise<void> {
    const result = await adminApi.login({ email, password })
    token.value = result.accessToken
    localStorage.setItem(TOKEN_STORAGE_KEY, result.accessToken)
    await loadCurrent()
  }

  async function loadCurrent(): Promise<void> {
    if (!token.value) return
    loading.value = true
    try {
      current.value = await adminApi.getCurrentAdmin()
    } finally {
      loading.value = false
    }
  }

  async function logout(): Promise<void> {
    try {
      await adminApi.logout()
    } finally {
      clearSession()
    }
  }

  function clearSession(): void {
    token.value = ''
    current.value = null
    localStorage.removeItem(TOKEN_STORAGE_KEY)
  }

  return {
    token,
    current,
    admin,
    permissions,
    loading,
    isSuperAdmin,
    hasPermission,
    login,
    loadCurrent,
    logout,
    clearSession,
  }
})
