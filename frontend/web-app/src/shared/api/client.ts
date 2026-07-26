import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { queryClient } from '@/app/query-client'
import { authToken } from '@/features/auth/token'
import { ApiError } from '@/shared/api/errors'
import type { ApiResult } from '@/shared/api/result'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15_000,
})

apiClient.interceptors.request.use((config) => {
  const token = authToken.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function expireSession() {
  if (!authToken.get()) return
  authToken.clear()
  queryClient.clear()

  const currentUrl = `${window.location.pathname}${window.location.search}`
  const redirect = encodeURIComponent(currentUrl)
  window.location.replace(`/login?redirect=${redirect}`)
}

export async function apiRequest<T>(config: AxiosRequestConfig) {
  try {
    const response = await apiClient.request<ApiResult<T>>(config)
    const result = response.data

    if (result.code !== 200) {
      const error = new ApiError(result.message || '请求失败', result.code)
      if (error.isAuthenticationError) expireSession()
      throw error
    }

    return result.data
  } catch (error) {
    if (error instanceof ApiError) throw error

    if (error instanceof AxiosError) {
      const result = error.response?.data as ApiResult<unknown> | undefined
      if (result?.code) {
        const apiError = new ApiError(
          result.message || '请求失败',
          result.code,
          error.response?.status,
        )
        if (apiError.isAuthenticationError) expireSession()
        throw apiError
      }

      throw new ApiError(
        error.code === 'ECONNABORTED'
          ? '请求超时，请稍后重试'
          : '网络连接失败，请检查后端服务',
        undefined,
        error.response?.status,
      )
    }

    throw new ApiError('请求失败，请稍后重试')
  }
}
