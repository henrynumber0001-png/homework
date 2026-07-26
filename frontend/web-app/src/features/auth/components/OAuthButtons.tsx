import { useMemo } from 'react'
import { Button } from '@/shared/ui/Button'
import { getConfiguredOAuthProviders, startOAuth } from '@/features/auth/oauth'
import type { OAuthProvider } from '@/features/auth/types'

const providerNames: Record<OAuthProvider, string> = {
  google: 'Google',
  apple: 'Apple',
  wechat: '微信',
  qq: 'QQ',
}

interface OAuthButtonsProps {
  turnstileToken: string
}

export function OAuthButtons({ turnstileToken }: OAuthButtonsProps) {
  const providers = useMemo(() => getConfiguredOAuthProviders(), [])
  if (providers.length === 0) return null

  return (
    <>
      <div className="my-6 flex items-center gap-3 text-xs text-[#998e86]">
        <span className="h-px flex-1 bg-line" />
        或使用第三方账号
        <span className="h-px flex-1 bg-line" />
      </div>
      <div className="grid grid-cols-2 gap-3">
        {providers.map((provider) => (
          <Button
            key={provider}
            type="button"
            variant="secondary"
            onClick={() => startOAuth(provider, turnstileToken)}
          >
            {providerNames[provider]}
          </Button>
        ))}
      </div>
    </>
  )
}
