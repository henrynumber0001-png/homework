import { AlertCircle, Inbox } from 'lucide-react'
import { Button } from '@/shared/ui/Button'

export function PageSkeleton() {
  return (
    <div className="space-y-5" aria-label="正在加载">
      <div className="h-28 animate-pulse rounded-2xl bg-white/70" />
      <div className="grid gap-5 md:grid-cols-2">
        <div className="h-64 animate-pulse rounded-2xl bg-white/70" />
        <div className="h-64 animate-pulse rounded-2xl bg-white/70" />
      </div>
    </div>
  )
}

interface ErrorStateProps {
  message?: string
  onRetry?: () => void
}

export function ErrorState({
  message = '页面加载失败，请稍后重试',
  onRetry,
}: ErrorStateProps) {
  return (
    <div className="surface-card flex min-h-56 flex-col items-center justify-center gap-3 p-8 text-center">
      <AlertCircle className="size-7 text-danger" aria-hidden="true" />
      <p className="text-sm text-muted">{message}</p>
      {onRetry ? (
        <Button variant="secondary" onClick={onRetry}>
          重新加载
        </Button>
      ) : null}
    </div>
  )
}

interface EmptyStateProps {
  title: string
  description?: string
}

export function EmptyState({ title, description }: EmptyStateProps) {
  return (
    <div className="flex min-h-40 flex-col items-center justify-center gap-2 p-6 text-center">
      <Inbox className="size-7 text-[#a69a92]" aria-hidden="true" />
      <p className="font-semibold text-ink">{title}</p>
      {description ? <p className="text-sm text-muted">{description}</p> : null}
    </div>
  )
}
