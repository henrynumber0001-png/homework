export class ApiError extends Error {
  readonly code?: number
  readonly status?: number

  constructor(message: string, code?: number, status?: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
  }

  get isAuthenticationError() {
    return this.code === 501 || this.code === 601 || this.code === 602
  }
}

export function getErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) return error.message
  return '请求失败，请稍后重试'
}
