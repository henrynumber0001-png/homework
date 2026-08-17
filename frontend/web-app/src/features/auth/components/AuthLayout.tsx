import type { PropsWithChildren, ReactNode } from 'react'
import { Link } from 'react-router-dom'

interface AuthLayoutProps extends PropsWithChildren {
  title: string
  description: string
  footer?: ReactNode
}

const authPatternWords = [
  { text: 'HomeWork', lang: 'en', x: 18, y: 60 },
  { text: 'Deberes', lang: 'es', x: 320, y: 60 },
  { text: '宿題', lang: 'ja', x: 38, y: 180 },
  { text: '作业', lang: 'zh-CN', x: 230, y: 180 },
  { text: '숙제', lang: 'ko', x: 422, y: 180 },
]

function AuthBrandPattern() {
  return (
    <div
      aria-hidden="true"
      className="pointer-events-none absolute inset-0 select-none overflow-hidden"
    >
      <svg
        className="h-full w-full"
        xmlns="http://www.w3.org/2000/svg"
        preserveAspectRatio="xMidYMid slice"
      >
        <defs>
          <pattern
            id="auth-homework-pattern"
            width="560"
            height="270"
            patternUnits="userSpaceOnUse"
            patternTransform="rotate(-7)"
          >
            {authPatternWords.map((word) => (
              <text
                key={word.lang}
                className="auth-brand-pattern-word"
                lang={word.lang}
                x={word.x}
                y={word.y}
                fill="rgb(23 63 115 / 0.12)"
                fontSize="32"
              >
                {word.text}
              </text>
            ))}
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill="url(#auth-homework-pattern)" />
      </svg>
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,rgb(243_246_251/0.92)_0%,rgb(243_246_251/0.64)_30%,rgb(243_246_251/0.12)_68%,transparent_100%)]" />
    </div>
  )
}

export function AuthLayout({
  title,
  description,
  footer,
  children,
}: AuthLayoutProps) {
  return (
    <main className="relative isolate flex min-h-screen items-center justify-center overflow-hidden bg-[#edf2f8] px-5 py-10">
      <AuthBrandPattern />
      <div className="relative z-10 w-full max-w-md">
        <section className="surface-card subtle-shadow relative w-full bg-white/95 p-6 backdrop-blur-[2px] sm:p-8">
          <Link
            to="/login"
            className="mb-8 inline-flex items-center gap-2 text-ink"
          >
            <span className="flex size-9 items-center justify-center rounded-xl bg-brand text-lg font-black text-white">
              H
            </span>
            <span className="text-lg font-extrabold tracking-tight">
              HomeWork
            </span>
          </Link>
          <h1 className="text-2xl font-extrabold tracking-tight text-ink">
            {title}
          </h1>
          <p className="mt-2 text-sm leading-6 text-muted">{description}</p>
          <div className="mt-7">{children}</div>
          {footer ? (
            <div className="mt-6 border-t border-line pt-5 text-center text-sm text-muted">
              {footer}
            </div>
          ) : null}
        </section>
      </div>
    </main>
  )
}
