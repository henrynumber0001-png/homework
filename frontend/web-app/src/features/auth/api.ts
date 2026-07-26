import { apiRequest } from '@/shared/api/client'
import type {
  EmailLoginInput,
  EmailRegisterInput,
  OAuthLoginInput,
} from '@/features/auth/types'

export function loginByEmail(data: EmailLoginInput) {
  return apiRequest<string>({
    url: '/app/auth/login/email',
    method: 'POST',
    data,
  })
}

export function registerByEmail(data: EmailRegisterInput) {
  return apiRequest<string>({
    url: '/app/auth/register/email',
    method: 'POST',
    data,
  })
}

export function loginByOAuth(data: OAuthLoginInput) {
  return apiRequest<string>({
    url: '/app/auth/login/oauth',
    method: 'POST',
    data,
  })
}
