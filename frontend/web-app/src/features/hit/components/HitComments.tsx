import {
  useInfiniteQuery,
  useMutation,
  useQueryClient,
} from '@tanstack/react-query'
import { Heart, Reply, Send } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import {
  createHitComment,
  listHitComments,
  updateHitCommentLike,
} from '@/features/hit/api'
import type { HitComment } from '@/features/hit/types'
import { ActionStatus } from '@/shared/constants/domain'
import { cn } from '@/shared/lib/cn'
import { formatRelativeTime } from '@/shared/lib/format'
import { Avatar } from '@/shared/ui/Avatar'
import { Button } from '@/shared/ui/Button'
import { Textarea } from '@/shared/ui/Input'

const PAGE_SIZE = 20

export function HitComments({ postId }: { postId: number }) {
  const queryClient = useQueryClient()
  const [content, setContent] = useState('')
  const [replyTo, setReplyTo] = useState<HitComment | null>(null)
  const commentsQuery = useInfiniteQuery({
    queryKey: ['hit-comments', postId],
    queryFn: ({ pageParam }) => listHitComments(postId, pageParam, PAGE_SIZE),
    initialPageParam: 1,
    getNextPageParam: (lastPage, pages) =>
      lastPage.length < PAGE_SIZE ? undefined : pages.length + 1,
  })
  const comments = commentsQuery.data?.pages.flat() || []

  const commentMutation = useMutation({
    mutationFn: (data: {
      parentCommentId: number | null
      comment: string
      mentionedUserIds: number[]
    }) => createHitComment(postId, data),
    onSuccess: () => {
      setContent('')
      setReplyTo(null)
      toast.success('评论已发布')
      void queryClient.invalidateQueries({
        queryKey: ['hit-comments', postId],
      })
      void queryClient.invalidateQueries({ queryKey: ['hits'] })
    },
  })
  const likeMutation = useMutation({
    mutationFn: ({
      commentId,
      active,
    }: {
      commentId: number
      active: boolean
    }) =>
      updateHitCommentLike(
        postId,
        commentId,
        active ? ActionStatus.ACTIVATE : ActionStatus.DEACTIVATE,
      ),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ['hit-comments', postId] }),
  })

  return (
    <div className="mt-4 border-t border-line/70 pt-4">
      {replyTo ? (
        <div className="mb-2 flex items-center justify-between rounded-lg bg-brand-soft px-3 py-2 text-xs text-muted">
          <span>回复 {replyTo.displayName}</span>
          <button type="button" onClick={() => setReplyTo(null)}>
            取消
          </button>
        </div>
      ) : null}
      <div className="flex items-end gap-2">
        <Textarea
          rows={2}
          maxLength={300}
          placeholder={replyTo ? `回复 ${replyTo.displayName}…` : '写下评论…'}
          value={content}
          onChange={(event) => setContent(event.target.value)}
        />
        <Button
          size="icon"
          aria-label="发布评论"
          disabled={!content.trim() || commentMutation.isPending}
          onClick={() =>
            commentMutation.mutate({
              parentCommentId: replyTo?.commentId ?? null,
              comment: content.trim(),
              mentionedUserIds: replyTo ? [replyTo.commentUserId] : [],
            })
          }
        >
          <Send className="size-4" />
        </Button>
      </div>

      <div className="mt-5 space-y-4">
        {comments.map((comment) => (
          <div
            key={comment.commentId}
            className={cn(
              'flex gap-3',
              comment.parentCommentId && 'ml-7 border-l border-line pl-4',
            )}
          >
            <Avatar
              src={comment.avatar}
              name={comment.displayName}
              className="size-8"
            />
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-x-2">
                <span className="text-xs font-bold">{comment.displayName}</span>
                <time className="text-[11px] text-muted">
                  {formatRelativeTime(comment.createdTime)}
                </time>
              </div>
              <p className="mt-1 whitespace-pre-wrap text-sm leading-6">
                {comment.comment}
              </p>
              <div className="mt-1.5 flex gap-4 text-xs text-muted">
                <button
                  type="button"
                  className="inline-flex items-center gap-1 hover:text-brand"
                  onClick={() => setReplyTo(comment)}
                >
                  <Reply className="size-3.5" />
                  回复
                </button>
                <button
                  type="button"
                  disabled={likeMutation.isPending}
                  className={cn(
                    'inline-flex items-center gap-1 hover:text-brand',
                    comment.liked && 'text-brand',
                  )}
                  onClick={() =>
                    likeMutation.mutate({
                      commentId: comment.commentId,
                      active: !comment.liked,
                    })
                  }
                >
                  <Heart
                    className={
                      comment.liked ? 'size-3.5 fill-current' : 'size-3.5'
                    }
                  />
                  {comment.likeCount}
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {commentsQuery.hasNextPage ? (
        <Button
          className="mt-4 w-full"
          variant="ghost"
          size="sm"
          disabled={commentsQuery.isFetchingNextPage}
          onClick={() => void commentsQuery.fetchNextPage()}
        >
          {commentsQuery.isFetchingNextPage ? '加载中…' : '加载更多评论'}
        </Button>
      ) : null}
    </div>
  )
}
