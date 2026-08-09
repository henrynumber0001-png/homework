import * as AvatarPrimitive from '@radix-ui/react-avatar'
import { cn } from '@/shared/lib/cn'

interface AvatarProps {
  src?: string | null
  name?: string | null
  className?: string
}

export function Avatar({ src, name, className }: AvatarProps) {
  const fallback = name?.trim().charAt(0).toUpperCase() || 'H'

  return (
    <AvatarPrimitive.Root
      className={cn(
        'inline-flex size-10 shrink-0 items-center justify-center overflow-hidden rounded-full bg-brand-soft text-sm font-bold text-brand',
        className,
      )}
    >
      <AvatarPrimitive.Image
        className="size-full object-cover"
        src={src || undefined}
        alt={name ? `${name}的头像` : '用户头像'}
      />
      <AvatarPrimitive.Fallback delayMs={200}>
        {fallback}
      </AvatarPrimitive.Fallback>
    </AvatarPrimitive.Root>
  )
}
