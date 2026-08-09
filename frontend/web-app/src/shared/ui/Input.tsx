import type { InputHTMLAttributes, TextareaHTMLAttributes } from 'react'
import { cn } from '@/shared/lib/cn'

export function Input({
  className,
  ...props
}: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={cn(
        'min-h-11 w-full rounded-xl border border-line bg-white px-3.5 text-sm text-ink placeholder:text-placeholder focus:border-accent focus:outline-none',
        className,
      )}
      {...props}
    />
  )
}

export function Textarea({
  className,
  ...props
}: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <textarea
      className={cn(
        'w-full resize-y rounded-xl border border-line bg-white px-3.5 py-3 text-sm leading-6 text-ink placeholder:text-placeholder focus:border-accent focus:outline-none',
        className,
      )}
      {...props}
    />
  )
}
