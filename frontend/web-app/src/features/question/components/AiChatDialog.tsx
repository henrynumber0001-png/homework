import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Bot, Send } from 'lucide-react'
import { useState } from 'react'
import { getAiChat, sendAiFollowUp } from '@/features/question/api'
import type { GroupTypeValue } from '@/shared/constants/domain'
import { formatRelativeTime } from '@/shared/lib/format'
import { Avatar } from '@/shared/ui/Avatar'
import { Button } from '@/shared/ui/Button'
import { Dialog } from '@/shared/ui/Dialog'
import { Textarea } from '@/shared/ui/Input'

interface AiChatDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  bankId: number
  questionId: number
  groupType: GroupTypeValue
}

export function AiChatDialog({
  open,
  onOpenChange,
  bankId,
  questionId,
  groupType,
}: AiChatDialogProps) {
  const queryClient = useQueryClient()
  const [message, setMessage] = useState('')
  const chatQuery = useQuery({
    queryKey: ['ai-chat', bankId],
    queryFn: () => getAiChat(bankId, groupType),
    enabled: open,
  })
  const sendMutation = useMutation({
    mutationFn: sendAiFollowUp,
    onSuccess: (chat) => {
      queryClient.setQueryData(['ai-chat', bankId], chat)
      setMessage('')
    },
  })

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title="追问 AI"
      description="围绕当前题目和答案解析继续提问。"
      className="max-h-[88vh] w-[min(94vw,42rem)]"
    >
      <div className="flex max-h-[52vh] min-h-64 flex-col gap-4 overflow-y-auto rounded-xl bg-[#f7f3ef] p-4">
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
                  className={
                    ai
                      ? 'max-w-[82%] rounded-2xl rounded-tl-sm bg-white px-4 py-3'
                      : 'max-w-[82%] rounded-2xl rounded-tr-sm bg-brand px-4 py-3 text-white'
                  }
                >
                  <p className="whitespace-pre-wrap text-sm leading-6">
                    {item.messageContent}
                  </p>
                  <time
                    className={
                      ai
                        ? 'mt-1 block text-[10px] text-muted'
                        : 'mt-1 block text-[10px] text-white/65'
                    }
                  >
                    {formatRelativeTime(item.createdTime)}
                  </time>
                </div>
              </div>
            )
          })
        ) : (
          <p className="m-auto max-w-xs text-center text-sm leading-6 text-muted">
            还没有追问记录。你可以询问思路、概念差异或进一步的例子。
          </p>
        )}
      </div>
      <div className="mt-4 flex items-end gap-3">
        <Textarea
          rows={2}
          maxLength={1000}
          value={message}
          placeholder="输入你的问题…"
          onChange={(event) => setMessage(event.target.value)}
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
    </Dialog>
  )
}
