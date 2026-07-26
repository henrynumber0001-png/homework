import { useMutation } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { loginByOAuth } from '@/features/auth/api'
import {
  consumeOAuthContext,
  getOAuthIdentityProvider,
} from '@/features/auth/oauth'
import { authToken } from '@/features/auth/token'
import type { OAuthProvider } from '@/features/auth/types'
import { getErrorMessage } from '@/shared/api/errors'

const providerNames = new Set<OAuthProvider>([
  'google',
  'apple',
  'wechat',
  'qq',
])

export function OAuthCallbackPage() {
  const navigate = useNavigate()
  const { provider: rawProvider } = useParams()
  const [searchParams] = useSearchParams()
  const [invalidRequest, setInvalidRequest] = useState(false)
  const mutation = useMutation({
    mutationFn: loginByOAuth,
    onSuccess: (token) => {
      authToken.set(token)
      navigate('/home', { replace: true })
    },
  })

  useEffect(() => {
    const provider = rawProvider as OAuthProvider
    const code = searchParams.get('code')
    const state = searchParams.get('state')
    const context = consumeOAuthContext(state)

    window.history.replaceState({}, '', `/oauth/callback/${rawProvider}`)

    if (
      !providerNames.has(provider) ||
      !code ||
      !context.valid ||
      Boolean(searchParams.get('error'))
    ) {
      setInvalidRequest(true)
      return
    }

    mutation.mutate({
      identityProvider: getOAuthIdentityProvider(provider),
      authCode: code,
      turnstileToken: context.turnstileToken,
    })
    // Mutation is intentionally started exactly once for this callback URL.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const provider = rawProvider as OAuthProvider
  const unsupportedProvider = !providerNames.has(provider)

  return (
    <main className="flex min-h-screen items-center justify-center px-5">
      <section className="surface-card w-full max-w-md p-8 text-center">
        {mutation.isPending ? (
          <>
            <div className="mx-auto size-9 animate-spin rounded-full border-2 border-line border-t-brand" />
            <h1 className="mt-5 text-lg font-bold">正在完成登录</h1>
            <p className="mt-2 text-sm text-muted">请稍候，不要关闭页面。</p>
          </>
        ) : mutation.isError || invalidRequest || unsupportedProvider ? (
          <>
            <h1 className="text-lg font-bold">第三方登录未完成</h1>
            <p className="mt-2 text-sm text-danger">
              {mutation.isError
                ? getErrorMessage(mutation.error)
                : '授权信息无效或已过期，请重新登录。'}
            </p>
            <Link
              className="mt-6 inline-block font-semibold text-brand"
              to="/login"
            >
              返回登录
            </Link>
          </>
        ) : (
          <p className="text-sm text-muted">正在验证授权信息…</p>
        )}
      </section>
    </main>
  )
}
