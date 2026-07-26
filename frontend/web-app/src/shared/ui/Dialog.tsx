import * as DialogPrimitive from '@radix-ui/react-dialog'
import { X } from 'lucide-react'
import type { PropsWithChildren, ReactNode } from 'react'
import { cn } from '@/shared/lib/cn'

interface DialogProps extends PropsWithChildren {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description?: string
  footer?: ReactNode
  className?: string
}

export function Dialog({
  open,
  onOpenChange,
  title,
  description,
  footer,
  children,
  className,
}: DialogProps) {
  return (
    <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-[#2f2925]/35 backdrop-blur-[2px]" />
        <DialogPrimitive.Content
          className={cn(
            'fixed left-1/2 top-1/2 z-50 w-[min(92vw,32rem)] -translate-x-1/2 -translate-y-1/2 rounded-2xl border border-line bg-surface p-6 shadow-2xl',
            className,
          )}
        >
          <div className="pr-8">
            <DialogPrimitive.Title className="text-lg font-bold text-ink">
              {title}
            </DialogPrimitive.Title>
            {description ? (
              <DialogPrimitive.Description className="mt-1 text-sm leading-6 text-muted">
                {description}
              </DialogPrimitive.Description>
            ) : null}
          </div>
          <div className="mt-5">{children}</div>
          {footer ? (
            <div className="mt-6 flex justify-end gap-3">{footer}</div>
          ) : null}
          <DialogPrimitive.Close
            className="absolute right-4 top-4 flex size-9 items-center justify-center rounded-full text-muted transition hover:bg-[#eee7e1] hover:text-ink"
            aria-label="关闭"
          >
            <X className="size-4" />
          </DialogPrimitive.Close>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  )
}
