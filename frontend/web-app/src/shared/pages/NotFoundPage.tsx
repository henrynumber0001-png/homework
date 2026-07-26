import { Link } from 'react-router-dom'
import { Button } from '@/shared/ui/Button'

export function NotFoundPage() {
  return (
    <main className="flex min-h-screen items-center justify-center px-5">
      <section className="surface-card max-w-md p-8 text-center">
        <p className="text-sm font-bold text-brand">404</p>
        <h1 className="mt-2 text-2xl font-extrabold">页面不存在</h1>
        <p className="mt-3 text-sm text-muted">
          这个地址可能已经失效，或者页面尚未开放。
        </p>
        <Button asChild className="mt-6">
          <Link to="/home">返回首页</Link>
        </Button>
      </section>
    </main>
  )
}
