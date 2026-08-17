import { useMemo } from 'react'
import { Button } from '@/shared/ui/Button'
import { getConfiguredOAuthProviders, startOAuth } from '@/features/auth/oauth'
import type { OAuthProvider } from '@/features/auth/types'
import { cn } from '@/shared/lib/cn'

const providerNames: Record<OAuthProvider, string> = {
  google: '使用 Google 继续',
  apple: '使用 Apple 继续',
  wechat: '使用微信继续',
  qq: '使用 QQ 继续',
}

interface OAuthButtonsProps {
  turnstileToken: string
  className?: string
}

export function OAuthButtons({ turnstileToken, className }: OAuthButtonsProps) {
  const providers = useMemo(() => getConfiguredOAuthProviders(), [])
  if (providers.length === 0) return null

  return (
    <div className={cn('mt-6 space-y-3', className)}>
      <div
        className="flex items-center gap-3 pb-3 text-xs font-medium text-placeholder"
        role="separator"
      >
        <span className="h-px flex-1 bg-line" />
        或使用第三方账号
        <span className="h-px flex-1 bg-line" />
      </div>
      <div className="grid grid-cols-1 gap-3">
        {providers.map((provider) => (
          <Button
            key={provider}
            className="relative w-full border-[#cbd5e1] bg-white text-ink shadow-sm hover:border-[#aebfd2] hover:bg-surface-muted hover:shadow-md"
            type="button"
            variant="secondary"
            size="lg"
            disabled={!turnstileToken}
            onClick={() => startOAuth(provider, turnstileToken)}
          >
            <ProviderIcon provider={provider} />
            {providerNames[provider]}
          </Button>
        ))}
      </div>
    </div>
  )
}

function ProviderIcon({ provider }: { provider: OAuthProvider }) {
  if (provider === 'google') {
    return (
      <svg aria-hidden="true" className="size-5 shrink-0" viewBox="0 0 24 24">
        <path
          fill="#4285F4"
          d="M21.6 12.23c0-.71-.06-1.4-.18-2.06H12v3.9h5.38a4.6 4.6 0 0 1-2 3.02v2.53h3.24c1.9-1.75 2.98-4.32 2.98-7.39Z"
        />
        <path
          fill="#34A853"
          d="M12 22c2.7 0 4.97-.9 6.62-2.38l-3.24-2.53c-.9.6-2.05.96-3.38.96-2.61 0-4.82-1.76-5.61-4.13H3.04v2.61A10 10 0 0 0 12 22Z"
        />
        <path
          fill="#FBBC05"
          d="M6.39 13.92A6.02 6.02 0 0 1 6.07 12c0-.67.11-1.32.32-1.92V7.47H3.04A10 10 0 0 0 2 12c0 1.61.38 3.14 1.04 4.53l3.35-2.61Z"
        />
        <path
          fill="#EA4335"
          d="M12 5.95c1.47 0 2.79.5 3.83 1.5l2.87-2.88A9.62 9.62 0 0 0 12 2a10 10 0 0 0-8.96 5.47l3.35 2.61C7.18 7.71 9.39 5.95 12 5.95Z"
        />
      </svg>
    )
  }

  if (provider === 'apple') {
    return (
      <svg
        aria-hidden="true"
        className="size-5 shrink-0 fill-current"
        viewBox="0 0 24 24"
      >
        <path d="M17.05 12.54c-.03-2.74 2.24-4.07 2.34-4.13a5.02 5.02 0 0 0-3.95-2.14c-1.66-.18-3.28 1-4.13 1-.87 0-2.18-.98-3.6-.95a5.26 5.26 0 0 0-4.42 2.7c-1.91 3.31-.49 8.18 1.35 10.86.92 1.31 1.98 2.78 3.4 2.73 1.38-.06 1.9-.88 3.56-.88 1.65 0 2.14.88 3.58.85 1.49-.03 2.43-1.32 3.31-2.64a10.8 10.8 0 0 0 1.52-3.1 4.72 4.72 0 0 1-2.96-4.3ZM14.36 4.51A4.8 4.8 0 0 0 15.46 1a4.9 4.9 0 0 0-3.2 1.67 4.58 4.58 0 0 0-1.13 3.38 4.08 4.08 0 0 0 3.23-1.54Z" />
      </svg>
    )
  }

  if (provider === 'wechat') {
    return (
      <svg aria-hidden="true" className="size-5 shrink-0" viewBox="0 0 24 24">
        <path
          fill="#07C160"
          d="M9.55 3.25c-4.14 0-7.5 2.78-7.5 6.2 0 1.93 1.08 3.65 2.77 4.79l-.7 2.08 2.43-1.22c.91.35 1.92.55 3 .55.25 0 .5-.01.74-.04a5.8 5.8 0 0 1-.2-1.49c0-3.46 3.1-6.26 6.93-6.26h.02C16.2 5.2 13.15 3.25 9.55 3.25Z"
        />
        <path
          fill="#07C160"
          d="M21.95 14.12c0-2.86-2.78-5.18-6.2-5.18s-6.2 2.32-6.2 5.18 2.78 5.18 6.2 5.18c.86 0 1.68-.15 2.42-.41l2.03 1.02-.58-1.75c1.43-.95 2.33-2.4 2.33-4.04Z"
        />
        <circle cx="7" cy="8.9" r=".75" fill="white" />
        <circle cx="12" cy="8.9" r=".75" fill="white" />
        <circle cx="13.7" cy="13.55" r=".65" fill="white" />
        <circle cx="17.9" cy="13.55" r=".65" fill="white" />
      </svg>
    )
  }

  return (
    <span
      aria-hidden="true"
      className="flex size-5 shrink-0 items-center justify-center rounded-full bg-[#1677ff] text-[10px] font-black text-white"
    >
      Q
    </span>
  )
}
