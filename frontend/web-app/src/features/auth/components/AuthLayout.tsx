import type { PropsWithChildren, ReactNode } from 'react'
import { Link } from 'react-router-dom'

interface AuthLayoutProps extends PropsWithChildren {
  title: string
  description: string
  footer?: ReactNode
}

export function AuthLayout({
  title,
  description,
  footer,
  children,
}: AuthLayoutProps) {
  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden px-5 py-10">
      <div className="absolute -left-24 top-10 size-72 rounded-full bg-[#c9dcf2]/65 blur-3xl" />
      <div className="absolute -right-20 bottom-0 size-80 rounded-full bg-[#dce8f7]/80 blur-3xl" />
      <section className="surface-card subtle-shadow relative w-full max-w-md p-6 sm:p-8">
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
    </main>
  )
}
