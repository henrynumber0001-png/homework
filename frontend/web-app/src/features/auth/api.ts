import { apiRequest } from '@/shared/api/client'
import type {
  EmailCodeSendInput,
  EmailCodeVerifyInput,
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

export function sendEmailCode(data: EmailCodeSendInput) {
  return apiRequest<void>({
    url: '/app/auth/register/email/code/send',
    method: 'POST',
    data,
  })
}

export function verifyEmailCode(data: EmailCodeVerifyInput) {
  return apiRequest<string>({
    url: '/app/auth/register/email/code/send/verify',
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
