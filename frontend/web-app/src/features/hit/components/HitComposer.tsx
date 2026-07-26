import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AtSign, Hash, Send } from 'lucide-react'
import { useMemo, useState } from 'react'
import { toast } from 'sonner'
import { publishHit, searchMentionUsers } from '@/features/hit/api'
import type { MentionUser } from '@/features/hit/types'
import { Avatar } from '@/shared/ui/Avatar'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { Textarea } from '@/shared/ui/Input'

const MAX_HIT_LENGTH = 140

function getMentionKeyword(content: string) {
  const match = content.match(/(?:^|\s)@([^\s@]*)$/u)
  return match?.[1] ?? null
}

function extractTags(content: string) {
  const tags = Array.from(
    content.matchAll(/#([\p{L}\p{N}_-]{1,30})/gu),
    (match) => match[1],
  )
  return Array.from(new Set(tags)).slice(0, 10)
}

export function HitComposer() {
  const queryClient = useQueryClient()
  const [content, setContent] = useState('')
  const [mentions, setMentions] = useState<MentionUser[]>([])
  const characterCount = Array.from(content).length
  const mentionKeyword = useMemo(() => getMentionKeyword(content), [content])
  const mentionQuery = useQuery({
    queryKey: ['mention-users', mentionKeyword],
    queryFn: () => searchMentionUsers(mentionKeyword || ''),
    enabled: mentionKeyword != null && mentionKeyword.trim().length > 0,
    staleTime: 30_000,
  })
  const publishMutation = useMutation({
    mutationFn: publishHit,
    onSuccess: () => {
      setContent('')
      setMentions([])
      toast.success('Hit 已发布')
      void queryClient.invalidateQueries({ queryKey: ['hits'] })
      void queryClient.invalidateQueries({ queryKey: ['home'] })
    },
  })

  const selectMention = (user: MentionUser) => {
    setContent((current) =>
      current.replace(/@([^\s@]*)$/u, `@${user.displayName} `),
    )
    setMentions((current) =>
      current.some((item) => item.userId === user.userId)
        ? current
        : [...current, user],
    )
  }

  return (
    <Card className="relative p-5">
      <div className="flex items-center gap-2">
        <span className="flex size-9 items-center justify-center rounded-xl bg-[#eee7e1] text-brand">
          <Send className="size-4" />
        </span>
        <div>
          <h2 className="text-sm font-bold">发布一条 Hit</h2>
          <p className="text-xs text-muted">分享一个发现、问题或学习进度</p>
        </div>
      </div>
      <Textarea
        className="mt-4 min-h-28 border-0 bg-[#f7f3ef] focus:ring-0"
        placeholder="最近学到了什么？使用 #标签 和 @用户 让内容更容易被发现。"
        value={content}
        onChange={(event) => setContent(event.target.value)}
      />

      {mentionQuery.data?.length && mentionKeyword ? (
        <div className="absolute left-5 right-5 top-44 z-20 max-h-56 overflow-y-auto rounded-xl border border-line bg-surface p-1.5 shadow-xl">
          {mentionQuery.data.map((user) => (
            <button
              key={user.userId}
              type="button"
              className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left hover:bg-[#eee7e1]"
              onClick={() => selectMention(user)}
            >
              <Avatar
                src={user.avatar}
                name={user.displayName}
                className="size-8"
              />
              <span className="min-w-0">
                <span className="block truncate text-sm font-semibold">
                  {user.displayName}
                </span>
                <span className="block text-xs text-muted">
                  {user.accountNo}
                </span>
              </span>
            </button>
          ))}
        </div>
      ) : null}

      <div className="mt-3 flex flex-wrap items-center gap-3">
        <span className="inline-flex items-center gap-1 text-xs text-muted">
          <Hash className="size-3.5" />
          标签直接写在正文
        </span>
        <span className="inline-flex items-center gap-1 text-xs text-muted">
          <AtSign className="size-3.5" />
          输入 @ 搜索用户
        </span>
        <span
          className={
            characterCount > MAX_HIT_LENGTH
              ? 'ml-auto text-xs font-bold text-danger'
              : 'ml-auto text-xs text-muted'
          }
        >
          {characterCount}/{MAX_HIT_LENGTH}
        </span>
        <Button
          size="sm"
          disabled={
            !content.trim() ||
            characterCount > MAX_HIT_LENGTH ||
            publishMutation.isPending
          }
          onClick={() =>
            publishMutation.mutate({
              content: content.trim(),
              tags: extractTags(content),
              mentionedUserIds: mentions.map((user) => user.userId),
            })
          }
        >
          发布
        </Button>
      </div>
      {publishMutation.isError ? (
        <p className="mt-2 text-xs text-danger">发布失败，请检查内容后重试。</p>
      ) : null}
    </Card>
  )
}
