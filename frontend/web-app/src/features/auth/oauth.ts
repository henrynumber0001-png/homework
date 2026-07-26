import type { OAuthProvider } from '@/features/auth/types'

const OAUTH_STATE_KEY = 'homework_oauth_state'
const OAUTH_TURNSTILE_KEY = 'homework_oauth_turnstile'

const oauthConfig = {
  google: {
    clientId: import.meta.env.VITE_GOOGLE_CLIENT_ID,
    redirectUri: import.meta.env.VITE_GOOGLE_REDIRECT_URI,
    identityProvider: 3,
  },
  apple: {
    clientId: import.meta.env.VITE_APPLE_CLIENT_ID,
    redirectUri: import.meta.env.VITE_APPLE_REDIRECT_URI,
    identityProvider: 4,
  },
  wechat: {
    clientId: import.meta.env.VITE_WECHAT_APP_ID,
    redirectUri: import.meta.env.VITE_WECHAT_REDIRECT_URI,
    identityProvider: 5,
  },
  qq: {
    clientId: import.meta.env.VITE_QQ_APP_ID,
    redirectUri: import.meta.env.VITE_QQ_REDIRECT_URI,
    identityProvider: 6,
  },
} as const

export function getConfiguredOAuthProviders() {
  if (import.meta.env.VITE_OAUTH_ENABLED !== 'true') return []

  return (Object.keys(oauthConfig) as OAuthProvider[]).filter((provider) => {
    const config = oauthConfig[provider]
    return Boolean(config.clientId && config.redirectUri)
  })
}

export function startOAuth(provider: OAuthProvider, turnstileToken: string) {
  const config = oauthConfig[provider]
  const state = crypto.randomUUID()
  sessionStorage.setItem(OAUTH_STATE_KEY, state)
  sessionStorage.setItem(OAUTH_TURNSTILE_KEY, turnstileToken)

  const commonParams = {
    client_id: config.clientId,
    redirect_uri: config.redirectUri,
    response_type: 'code',
    state,
  }

  if (provider === 'google') {
    const params = new URLSearchParams({
      ...commonParams,
      scope: 'openid email profile',
      prompt: 'select_account',
    })
    window.location.assign(
      `https://accounts.google.com/o/oauth2/v2/auth?${params}`,
    )
    return
  }

  if (provider === 'apple') {
    const params = new URLSearchParams({
      ...commonParams,
      scope: 'name email',
      response_mode: 'query',
    })
    window.location.assign(`https://appleid.apple.com/auth/authorize?${params}`)
    return
  }

  if (provider === 'wechat') {
    const params = new URLSearchParams({
      appid: config.clientId,
      redirect_uri: config.redirectUri,
      response_type: 'code',
      scope: 'snsapi_login',
      state,
    })
    window.location.assign(
      `https://open.weixin.qq.com/connect/qrconnect?${params}#wechat_redirect`,
    )
    return
  }

  const params = new URLSearchParams({
    ...commonParams,
    scope: 'get_user_info',
  })
  window.location.assign(`https://graph.qq.com/oauth2.0/authorize?${params}`)
}

export function consumeOAuthContext(state: string | null) {
  const storedState = sessionStorage.getItem(OAUTH_STATE_KEY)
  const turnstileToken = sessionStorage.getItem(OAUTH_TURNSTILE_KEY) || ''

  sessionStorage.removeItem(OAUTH_STATE_KEY)
  sessionStorage.removeItem(OAUTH_TURNSTILE_KEY)

  return {
    valid: Boolean(state && storedState && state === storedState),
    turnstileToken,
  }
}

export function getOAuthIdentityProvider(provider: OAuthProvider) {
  return oauthConfig[provider].identityProvider
}
