import type { HTMLAttributes } from 'react'
import { cn } from '@/shared/lib/cn'

export function Badge({
  className,
  ...props
}: HTMLAttributes<HTMLSpanElement>) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full border border-line bg-surface-muted px-2.5 py-1 text-xs font-medium text-muted',
        className,
      )}
      {...props}
    />
  )
}
