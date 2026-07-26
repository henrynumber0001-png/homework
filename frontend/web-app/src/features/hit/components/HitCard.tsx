import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Bookmark, Heart, MessageCircle, Repeat2 } from 'lucide-react'
import type { CSSProperties } from 'react'
import { Link } from 'react-router-dom'
import { updateHitAction } from '@/features/hit/api'
import type { HitPost } from '@/features/hit/types'
import { ActionStatus, HitActionType } from '@/shared/constants/domain'
import { cn } from '@/shared/lib/cn'
import { formatCount, formatRelativeTime } from '@/shared/lib/format'
import { Avatar } from '@/shared/ui/Avatar'
import { Badge } from '@/shared/ui/Badge'
import { Card } from '@/shared/ui/Card'

interface HitCardProps {
  post: HitPost
  compact?: boolean
  className?: string
  style?: CSSProperties
  onToggleComments?: () => void
}

export function HitCard({
  post,
  compact = false,
  className,
  style,
  onToggleComments,
}: HitCardProps) {
  const queryClient = useQueryClient()
  const mutation = useMutation({
    mutationFn: ({
      actionType,
      active,
    }: {
      actionType: number
      active: boolean
    }) =>
      updateHitAction(
        post.postId,
        actionType,
        active ? ActionStatus.ACTIVATE : ActionStatus.DEACTIVATE,
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['hits'] })
      void queryClient.invalidateQueries({ queryKey: ['home'] })
    },
  })

  return (
    <Card className={cn(compact ? 'p-4' : 'p-5', className)} style={style}>
      <div className="flex gap-3">
        <Link
          to={`/users/${post.userId}`}
          aria-label={`查看${post.displayName}的主页`}
        >
          <Avatar src={post.avatar} name={post.displayName} />
        </Link>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
            <Link
              to={`/users/${post.userId}`}
              className="truncate text-sm font-bold text-ink hover:text-brand"
            >
              {post.displayName}
            </Link>
            <time
              className="text-xs text-[#91867f]"
              dateTime={post.createdTime}
              title={post.createdTime}
            >
              {formatRelativeTime(post.createdTime)}
            </time>
          </div>
          <p className="mt-2 whitespace-pre-wrap break-words text-[15px] leading-7 text-ink">
            {post.content}
          </p>
          {post.tags.length ? (
            <div className="mt-3 flex flex-wrap gap-2">
              {post.tags.map((tag) => (
                <Badge key={tag} className="border-0 bg-[#edf3f1] text-accent">
                  #{tag}
                </Badge>
              ))}
            </div>
          ) : null}

          {!compact ? (
            <div className="mt-4 flex items-center justify-between border-t border-line/70 pt-3 text-xs text-muted">
              <ActionButton
                label="评论"
                count={post.commentCount}
                active={false}
                disabled={false}
                icon={MessageCircle}
                onClick={onToggleComments}
              />
              <ActionButton
                label="点赞"
                count={post.likeCount}
                active={post.liked}
                disabled={mutation.isPending}
                icon={Heart}
                onClick={() =>
                  mutation.mutate({
                    actionType: HitActionType.LIKE,
                    active: !post.liked,
                  })
                }
              />
              <ActionButton
                label="收藏"
                count={post.favoriteCount}
                active={post.favorited}
                disabled={mutation.isPending}
                icon={Bookmark}
                onClick={() =>
                  mutation.mutate({
                    actionType: HitActionType.FAVORITE,
                    active: !post.favorited,
                  })
                }
              />
              <ActionButton
                label="转发"
                count={post.repostCount}
                active={post.reposted}
                disabled={mutation.isPending}
                icon={Repeat2}
                onClick={() =>
                  mutation.mutate({
                    actionType: HitActionType.REPOST,
                    active: !post.reposted,
                  })
                }
              />
            </div>
          ) : null}
        </div>
      </div>
    </Card>
  )
}

interface ActionButtonProps {
  label: string
  count: number
  active: boolean
  disabled: boolean
  icon: typeof Heart
  onClick?: () => void
}

function ActionButton({
  label,
  count,
  active,
  disabled,
  icon: Icon,
  onClick,
}: ActionButtonProps) {
  return (
    <button
      type="button"
      className={
        active
          ? 'flex items-center gap-1.5 text-brand'
          : 'flex items-center gap-1.5 hover:text-ink'
      }
      aria-label={`${label}，${count}`}
      disabled={disabled}
      onClick={onClick}
    >
      <Icon className={active ? 'size-4 fill-current' : 'size-4'} />
      <span>{formatCount(count)}</span>
    </button>
  )
}
