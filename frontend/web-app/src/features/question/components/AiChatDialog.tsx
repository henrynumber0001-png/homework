import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Bot, ChevronRight, MessageCircleMore, Send } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { getAiChat, sendAiFollowUp } from '@/features/question/api'
import type { GroupTypeValue } from '@/shared/constants/domain'
import { cn } from '@/shared/lib/cn'
import { formatRelativeTime } from '@/shared/lib/format'
import { Avatar } from '@/shared/ui/Avatar'
import { Button } from '@/shared/ui/Button'
import { Textarea } from '@/shared/ui/Input'

interface AiChatDrawerProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  bankId: number
  questionId: number
  groupType: GroupTypeValue
}

/**
 * 非模态 AI 追问抽屉。页面仍可交互，由答题页负责在页面被点击时收起抽屉。
 */
export function AiChatDrawer({
  open,
  onOpenChange,
  bankId,
  questionId,
  groupType,
}: AiChatDrawerProps) {
  const queryClient = useQueryClient()
  const [message, setMessage] = useState('')
  const [hasOpened, setHasOpened] = useState(open)
  const inputRef = useRef<HTMLTextAreaElement>(null)
  const messageListRef = useRef<HTMLDivElement>(null)
  const chatQuery = useQuery({
    queryKey: ['ai-chat', bankId, groupType],
    queryFn: () => getAiChat(bankId, groupType),
    enabled: open,
  })
  const sendMutation = useMutation({
    mutationFn: sendAiFollowUp,
    onSuccess: (chat) => {
      queryClient.setQueryData(['ai-chat', bankId, groupType], chat)
      setMessage('')
    },
  })

  useEffect(() => {
    if (!open) return
    setHasOpened(true)
    inputRef.current?.focus()
  }, [open])

  useEffect(() => {
    if (!open) return
    if (messageListRef.current) {
      messageListRef.current.scrollTop = messageListRef.current.scrollHeight
    }
  }, [chatQuery.data?.messages.length, open])

  useEffect(() => {
    if (!open) return
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onOpenChange(false)
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [onOpenChange, open])

  return createPortal(
    <>
      <button
        type="button"
        aria-label="再次打开 AI 追问"
        aria-hidden={!hasOpened || open}
        tabIndex={hasOpened && !open ? 0 : -1}
        className={cn(
          'fixed right-0 top-1/2 z-50 flex -translate-y-1/2 items-center gap-1.5 rounded-l-lg border border-r-0 border-line bg-brand px-2 py-2.5 text-xs font-bold text-white shadow-[0_10px_26px_rgba(14,45,86,0.18)] transition-[opacity,transform] duration-200 ease-[var(--ease-out-ui)] motion-reduce:transition-opacity',
          hasOpened && !open
            ? 'translate-x-0 opacity-100'
            : 'pointer-events-none translate-x-full opacity-0',
        )}
        onClick={() => onOpenChange(true)}
      >
        <MessageCircleMore className="size-3.5" />
        <span className="[writing-mode:vertical-rl]">AI 追问</span>
      </button>

      <aside
        role="dialog"
        aria-modal="false"
        aria-labelledby="ai-chat-title"
        aria-hidden={!open}
        inert={!open ? true : undefined}
        data-ai-drawer
        className={cn(
          'fixed bottom-[4.5rem] right-0 top-16 z-50 flex w-[min(100vw,26rem)] flex-col border-l border-line bg-surface shadow-[-18px_0_48px_rgba(14,45,86,0.16)] transition-[opacity,transform] duration-[260ms] ease-[var(--ease-drawer)] md:bottom-0 motion-reduce:transition-opacity motion-reduce:duration-150',
          open
            ? 'translate-x-0 opacity-100'
            : 'pointer-events-none translate-x-full opacity-0 motion-reduce:translate-x-0',
        )}
      >
        <header className="flex items-start gap-3 border-b border-line px-5 py-4">
          <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg bg-accent-soft text-accent">
            <Bot className="size-3.5" />
          </span>
          <div className="min-w-0 flex-1">
            <h2 id="ai-chat-title" className="font-extrabold text-ink">
              追问 AI
            </h2>
            <p className="mt-0.5 text-xs leading-5 text-muted">
              围绕当前题目和解析继续提问
            </p>
          </div>
          <button
            type="button"
            aria-label="收起 AI 追问"
            className="flex size-9 shrink-0 items-center justify-center rounded-xl text-muted transition-colors hover:bg-brand-soft hover:text-brand"
            onClick={() => onOpenChange(false)}
          >
            <ChevronRight className="size-5" />
          </button>
        </header>

        <div
          ref={messageListRef}
          className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto bg-surface-muted px-4 py-5"
        >
          {chatQuery.isLoading ? (
            <div className="m-auto size-8 animate-spin rounded-full border-2 border-line border-t-brand" />
          ) : chatQuery.data?.messages.length ? (
            chatQuery.data.messages.map((item) => {
              const ai = item.senderType === 2
              return (
                <div
                  key={item.messageId}
                  className={
                    ai ? 'flex gap-2.5' : 'flex flex-row-reverse gap-2.5'
                  }
                >
                  {ai ? (
                    <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-accent text-white">
                      <Bot className="size-4" />
                    </span>
                  ) : (
                    <Avatar name="我" className="size-8" />
                  )}
                  <div
                    className={cn(
                      'max-w-[82%] rounded-2xl px-4 py-3 shadow-sm',
                      ai
                        ? 'rounded-tl-sm bg-white'
                        : 'rounded-tr-sm bg-brand text-white',
                    )}
                  >
                    <p className="whitespace-pre-wrap text-sm leading-6">
                      {item.messageContent}
                    </p>
                    <time
                      className={cn(
                        'mt-1 block text-[10px]',
                        ai ? 'text-muted' : 'text-white/65',
                      )}
                    >
                      {formatRelativeTime(item.createdTime)}
                    </time>
                  </div>
                </div>
              )
            })
          ) : (
            <p className="m-auto max-w-xs text-center text-sm leading-6 text-muted">
              还没有追问记录。你可以询问解题思路、概念差异或进一步的例子。
            </p>
          )}
        </div>

        <div className="border-t border-line bg-surface p-4">
          <div className="flex items-end gap-2">
            <Textarea
              ref={inputRef}
              rows={2}
              maxLength={1000}
              value={message}
              className="max-h-32 min-h-[4.5rem] resize-none"
              placeholder="输入你的问题…"
              onChange={(event) => setMessage(event.target.value)}
              onKeyDown={(event) => {
                if (
                  event.key === 'Enter' &&
                  !event.shiftKey &&
                  message.trim() &&
                  !sendMutation.isPending
                ) {
                  event.preventDefault()
                  sendMutation.mutate({
                    bankId,
                    questionId,
                    groupType,
                    message: message.trim(),
                  })
                }
              }}
            />
            <Button
              size="icon"
              aria-label="发送追问"
              disabled={!message.trim() || sendMutation.isPending}
              onClick={() =>
                sendMutation.mutate({
                  bankId,
                  questionId,
                  groupType,
                  message: message.trim(),
                })
              }
            >
              <Send className="size-4" />
            </Button>
          </div>
          {sendMutation.isError ? (
            <p className="mt-2 text-xs text-danger">发送失败，请稍后重试。</p>
          ) : null}
        </div>
      </aside>
    </>,
    document.body,
  )
}
