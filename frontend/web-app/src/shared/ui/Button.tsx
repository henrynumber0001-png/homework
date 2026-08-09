import { Slot } from '@radix-ui/react-slot'
import { cva, type VariantProps } from 'class-variance-authority'
import type { ButtonHTMLAttributes } from 'react'
import { cn } from '@/shared/lib/cn'

const buttonVariants = cva(
  'inline-flex min-h-10 items-center justify-center gap-2 rounded-xl px-4 text-sm font-semibold transition-[color,background-color,border-color,box-shadow,transform] duration-150 ease-[var(--ease-out-ui)] active:scale-[0.97] disabled:cursor-not-allowed disabled:opacity-55 disabled:active:scale-100 motion-reduce:transition-none',
  {
    variants: {
      variant: {
        primary:
          'bg-brand text-white shadow-sm shadow-brand/15 hover:bg-brand-dark hover:shadow-md',
        secondary:
          'border border-line bg-surface text-ink hover:border-brand/45 hover:bg-brand-soft',
        ghost: 'text-muted hover:bg-brand-soft hover:text-brand-dark',
        danger: 'bg-danger text-white hover:bg-[#963d4a]',
      },
      size: {
        sm: 'min-h-9 rounded-lg px-3 text-xs',
        md: 'min-h-10 px-4',
        lg: 'min-h-12 px-5 text-base',
        icon: 'size-10 p-0',
      },
    },
    defaultVariants: {
      variant: 'primary',
      size: 'md',
    },
  },
)

interface ButtonProps
  extends
    ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean
}

export function Button({
  asChild,
  className,
  variant,
  size,
  ...props
}: ButtonProps) {
  const Component = asChild ? Slot : 'button'
  return (
    <Component
      className={cn(buttonVariants({ variant, size }), className)}
      {...props}
    />
  )
}
