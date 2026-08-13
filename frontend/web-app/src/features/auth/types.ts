export interface EmailLoginInput {
  email: string
  password: string
  turnstileToken: string
}

export interface EmailRegisterInput {
  email: string
  password: string
  passwordConfirm: string
  displayName: string
  secureTicket: string
}

export interface EmailCodeSendInput {
  email: string
  turnstileToken: string
}

export interface EmailCodeVerifyInput {
  email: string
  code: string
}

export interface OAuthLoginInput {
  identityProvider: number
  authCode: string
  turnstileToken: string
}

export type OAuthProvider = 'google' | 'apple' | 'wechat' | 'qq'
