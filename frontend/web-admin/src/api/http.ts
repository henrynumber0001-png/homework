import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types/admin'

export const TOKEN_STORAGE_KEY = 'homework-admin-token'

export class ApiError extends Error {
  code: number
  status?: number
  requestId?: string

  constructor(message: string, code = -1, status?: number, requestId?: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
    this.requestId = requestId
  }
}

const http = axios.create({
  baseURL: import.meta.env.VITE_ADMIN_API_BASE_URL || '/api/admin',
  timeout: 20_000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status
    const body = error.response?.data
    const requestId = error.response?.headers['x-request-id'] as string | undefined
    const message = body?.message || (status === 401 ? '登录状态已失效，请重新登录' : '请求失败，请稍后重试')

    if (status === 401) {
      localStorage.removeItem(TOKEN_STORAGE_KEY)
      window.dispatchEvent(new CustomEvent('admin:unauthorized'))
    }

    return Promise.reject(new ApiError(message, body?.code, status, requestId))
  },
)

/** 请求后端统一 Result 响应并返回其中的 data。 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiResult<T>>(config)
  const result = response.data
  if (result.code !== 200) {
    throw new ApiError(result.message, result.code, response.status)
  }
  return result.data
}

/** 下载不使用统一 Result 包装的二进制文件。 */
export async function download(config: AxiosRequestConfig): Promise<Blob> {
  const response = await http.request<Blob>({ ...config, responseType: 'blob' })
  return response.data
}

/** 将未被页面主动处理的接口异常转成统一提示。 */
export function showApiError(error: unknown): void {
  const message = error instanceof Error ? error.message : '操作失败，请稍后重试'
  ElMessage.error(message)
}
