import { useEffect, useId, useRef } from 'react'

declare global {
  interface Window {
    turnstile?: {
      render: (
        element: HTMLElement,
        options: {
          sitekey: string
          callback: (token: string) => void
          'expired-callback': () => void
          'error-callback': () => void
          theme: 'light'
        },
      ) => string
      remove: (widgetId: string) => void
    }
  }
}

interface TurnstileProps {
  onTokenChange: (token: string) => void
}

export function Turnstile({ onTokenChange }: TurnstileProps) {
  const siteKey = import.meta.env.VITE_TURNSTILE_SITE_KEY
  const containerRef = useRef<HTMLDivElement>(null)
  const reactId = useId()

  useEffect(() => {
    if (!siteKey || !containerRef.current) return

    let widgetId: string | undefined
    let cancelled = false

    const renderWidget = () => {
      if (cancelled || !containerRef.current || !window.turnstile) return
      widgetId = window.turnstile.render(containerRef.current, {
        sitekey: siteKey,
        callback: onTokenChange,
        'expired-callback': () => onTokenChange(''),
        'error-callback': () => onTokenChange(''),
        theme: 'light',
      })
    }

    const existingScript = document.querySelector<HTMLScriptElement>(
      'script[data-homework-turnstile]',
    )

    if (window.turnstile) {
      renderWidget()
    } else if (existingScript) {
      existingScript.addEventListener('load', renderWidget, { once: true })
    } else {
      const script = document.createElement('script')
      script.src =
        'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit'
      script.async = true
      script.defer = true
      script.dataset.homeworkTurnstile = 'true'
      script.addEventListener('load', renderWidget, { once: true })
      document.head.appendChild(script)
    }

    return () => {
      cancelled = true
      if (widgetId && window.turnstile) window.turnstile.remove(widgetId)
    }
  }, [onTokenChange, reactId, siteKey])

  if (!siteKey) {
    return (
      <p className="rounded-xl bg-brand-soft px-3 py-2 text-xs text-muted">
        当前环境尚未配置人机验证 Site Key。
      </p>
    )
  }

  return <div ref={containerRef} className="min-h-[65px]" />
}
