import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import {
  Bell,
  Heart,
  History,
  MessageCircle,
  Send,
  UserRound,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  getChatboxes,
  getNotificationHistory,
  getPrivateMessages,
  markPrivateMessageRead,
  openNotificationTab,
  sendPrivateMessage,
} from '@/features/messages/api'
import type { Notification, PrivateMessage } from '@/features/messages/types'
import { cn } from '@/shared/lib/cn'
import { formatRelativeTime } from '@/shared/lib/format'
import { useUnreadSummary } from '@/shared/queries/session'
import { Avatar } from '@/shared/ui/Avatar'
import { Badge } from '@/shared/ui/Badge'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { Textarea } from '@/shared/ui/Input'
import { ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

const notificationTabs = new Set(['comments', 'interactions', 'system'])
const tabs = [
  { value: 'comments', label: '评论和@', icon: MessageCircle },
  { value: 'interactions', label: '赞、收藏和转发', icon: Heart },
  { value: 'system', label: '系统消息', icon: Bell },
  { value: 'private', label: '私信', icon: Send },
] as const

export function MessagesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedTab = searchParams.get('tab')
  const activeTab = tabs.some((tab) => tab.value === requestedTab)
    ? requestedTab!
    : 'comments'
  const unreadQuery = useUnreadSummary()

  const setTab = (tab: string) => {
    setSearchParams({ tab })
  }

  return (
    <div className="app-container py-8">
      <header>
        <p className="text-sm font-semibold text-brand">通知与会话</p>
        <h1 className="mt-1 text-3xl font-extrabold">我的消息</h1>
      </header>

      <div className="mt-6 flex overflow-x-auto border-b border-line">
        {tabs.map((tab) => {
          const Icon = tab.icon
          const unread = unreadForTab(unreadQuery.data, tab.value)
          return (
            <button
              key={tab.value}
              type="button"
              className={cn(
                'relative flex shrink-0 items-center gap-2 px-4 py-3 text-sm font-semibold text-muted',
                activeTab === tab.value &&
                  'text-brand after:absolute after:inset-x-3 after:bottom-0 after:h-0.5 after:bg-brand',
              )}
              onClick={() => setTab(tab.value)}
            >
              <Icon className="size-4" />
              {tab.label}
              {unread ? (
                <Badge className="border-0 bg-danger px-1.5 py-0 text-[10px] text-white">
                  {unread > 99 ? '99+' : unread}
                </Badge>
              ) : null}
            </button>
          )
        })}
      </div>

      <div className="mt-5">
        {activeTab === 'private' ? (
          <PrivateMessages />
        ) : (
          <NotificationList tab={activeTab} />
        )}
      </div>
    </div>
  )
}

function NotificationList({ tab }: { tab: string }) {
  const queryClient = useQueryClient()
  const [historyMode, setHistoryMode] = useState(false)
  const openedQuery = useQuery({
    queryKey: ['messages', tab, 'opened'],
    queryFn: () => openNotificationTab(tab),
    enabled: notificationTabs.has(tab),
    staleTime: 0,
  })
  const historyQuery = useInfiniteQuery({
    queryKey: ['messages', tab, 'history'],
    queryFn: ({ pageParam }) => getNotificationHistory(tab, pageParam),
    initialPageParam: 1,
    getNextPageParam: (lastPage) =>
      lastPage.pageNum * lastPage.pageSize < lastPage.total
        ? lastPage.pageNum + 1
        : undefined,
    enabled: historyMode,
  })

  useEffect(() => {
    if (openedQuery.isSuccess) {
      void queryClient.invalidateQueries({
        queryKey: ['messages', 'unread-summary'],
      })
    }
  }, [openedQuery.isSuccess, queryClient])

  if (openedQuery.isLoading) return <PageSkeleton />
  if (openedQuery.isError) {
    return <ErrorState onRetry={() => void openedQuery.refetch()} />
  }

  const records = historyMode
    ? historyQuery.data?.pages.flatMap((page) => page.records) || []
    : openedQuery.data?.records || []

  return (
    <div>
      <div className="mb-3 flex justify-end">
        <Button
          variant="ghost"
          size="sm"
          onClick={() => setHistoryMode((current) => !current)}
        >
          <History className="size-4" />
          {historyMode ? '返回最新消息' : '查看历史信息'}
        </Button>
      </div>
      {records.length ? (
        <Card className="divide-y divide-line/70 overflow-hidden">
          {records.map((notification) => (
            <NotificationRow
              key={notification.id}
              notification={notification}
            />
          ))}
        </Card>
      ) : (
        <MessageEmptyHint>这里暂时没有消息</MessageEmptyHint>
      )}
      {historyMode && historyQuery.hasNextPage ? (
        <Button
          className="mt-4 w-full"
          variant="secondary"
          onClick={() => void historyQuery.fetchNextPage()}
        >
          加载更多
        </Button>
      ) : null}
    </div>
  )
}

function NotificationRow({ notification }: { notification: Notification }) {
  return (
    <div className="flex gap-3 px-4 py-4 sm:px-5">
      {notification.actionUserId ? (
        <Link to={`/users/${notification.actionUserId}`}>
          <Avatar
            src={notification.actionAvatar}
            name={notification.actionDisplayName}
          />
        </Link>
      ) : (
        <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-[#eee7e1] text-brand">
          <Bell className="size-4" />
        </span>
      )}
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-baseline justify-between gap-2">
          <h2 className="text-sm font-bold">{notification.title}</h2>
          <time className="text-xs text-muted">
            {formatRelativeTime(notification.createdTime)}
          </time>
        </div>
        <p className="mt-1 whitespace-pre-wrap text-sm leading-6 text-muted">
          {notification.content}
        </p>
        {!notification.postAvailable && notification.postId ? (
          <p className="mt-2 text-xs text-[#9a8f88]">关联的 Hit 已不可访问</p>
        ) : null}
      </div>
    </div>
  )
}

function PrivateMessages() {
  const queryClient = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()
  const chatboxId = Number(searchParams.get('chatboxId')) || null
  const newUserId = Number(searchParams.get('userId')) || null
  const [content, setContent] = useState('')

  const chatboxesQuery = useQuery({
    queryKey: ['chatboxes'],
    queryFn: () => getChatboxes(),
  })
  const chatboxes = chatboxesQuery.data?.records || []
  const activeChatbox = chatboxes.find(
    (chatbox) => chatbox.chatboxId === chatboxId,
  )
  const messagesQuery = useQuery({
    queryKey: ['private-messages', chatboxId],
    queryFn: () => getPrivateMessages(chatboxId!, { limit: 50 }),
    enabled: Boolean(chatboxId),
    refetchInterval: 15_000,
  })
  const sendMutation = useMutation({
    mutationFn: sendPrivateMessage,
    onSuccess: (message) => {
      setContent('')
      setSearchParams({
        tab: 'private',
        chatboxId: String(message.chatboxId),
      })
      void queryClient.invalidateQueries({ queryKey: ['chatboxes'] })
      void queryClient.invalidateQueries({
        queryKey: ['private-messages', message.chatboxId],
      })
    },
  })
  const readMutation = useMutation({
    mutationFn: markPrivateMessageRead,
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['private-messages', chatboxId],
      })
      void queryClient.invalidateQueries({
        queryKey: ['messages', 'unread-summary'],
      })
      void queryClient.invalidateQueries({ queryKey: ['chatboxes'] })
    },
  })

  const receiverId = activeChatbox?.otherUserId ?? newUserId
  const canSend = activeChatbox
    ? activeChatbox.canCurrentUserSend
    : Boolean(newUserId)
  const messages = messagesQuery.data || []

  if (chatboxesQuery.isLoading) return <PageSkeleton />
  if (chatboxesQuery.isError) {
    return <ErrorState onRetry={() => void chatboxesQuery.refetch()} />
  }

  if (!chatboxes.length && !newUserId) {
    return <MessageEmptyHint>暂无私信会话</MessageEmptyHint>
  }

  return (
    <div className="grid min-h-[560px] overflow-hidden rounded-2xl border border-line bg-surface lg:grid-cols-[300px_minmax(0,1fr)]">
      <aside className="border-b border-line lg:border-b-0 lg:border-r">
        <div className="border-b border-line px-4 py-3 text-sm font-bold">
          会话
        </div>
        {chatboxes.length ? (
          <div className="max-h-[500px] overflow-y-auto">
            {chatboxes.map((chatbox) => (
              <button
                key={chatbox.chatboxId}
                type="button"
                className={cn(
                  'flex w-full gap-3 border-b border-line/60 px-4 py-4 text-left hover:bg-[#f7f3ef]',
                  chatbox.chatboxId === chatboxId && 'bg-[#eee7e1]',
                )}
                onClick={() =>
                  setSearchParams({
                    tab: 'private',
                    chatboxId: String(chatbox.chatboxId),
                  })
                }
              >
                <Avatar
                  src={chatbox.otherAvatar}
                  name={chatbox.otherDisplayName}
                />
                <span className="min-w-0 flex-1">
                  <span className="flex items-center justify-between gap-2">
                    <strong className="truncate text-sm">
                      {chatbox.otherDisplayName}
                    </strong>
                    {chatbox.unreadCount ? (
                      <Badge className="border-0 bg-danger px-1.5 py-0 text-[10px] text-white">
                        {chatbox.unreadCount}
                      </Badge>
                    ) : null}
                  </span>
                  <span className="mt-1 block truncate text-xs text-muted">
                    {chatbox.lastMessage || '暂无消息'}
                  </span>
                </span>
              </button>
            ))}
          </div>
        ) : (
          <p className="px-4 py-12 text-center text-sm text-muted">
            暂无私信会话
          </p>
        )}
      </aside>

      <section className="flex min-h-[420px] flex-col">
        {receiverId ? (
          <>
            <div className="border-b border-line px-5 py-3">
              <h2 className="font-bold">
                {activeChatbox?.otherDisplayName || '新私信'}
              </h2>
            </div>
            <div className="flex flex-1 flex-col justify-end gap-3 overflow-y-auto bg-[#faf7f4] p-5">
              {messages.length ? (
                messages.map((message) => {
                  const mine = message.senderUserId !== receiverId
                  return (
                    <MessageBubble
                      key={message.id}
                      message={message}
                      mine={mine}
                      onRead={() => {
                        if (!mine && message.messageStatus === 1) {
                          readMutation.mutate(message.id)
                        }
                      }}
                    />
                  )
                })
              ) : (
                <p className="m-auto text-center text-sm text-muted">
                  发送第一条纯文本消息开始交流。
                </p>
              )}
            </div>
            <div className="border-t border-line p-4">
              {!canSend ? (
                <p className="mb-2 text-xs text-warning">
                  需要等待对方回复后，才能继续发送消息。
                </p>
              ) : null}
              <div className="flex items-end gap-2">
                <Textarea
                  rows={2}
                  maxLength={1000}
                  placeholder="输入私信内容…"
                  disabled={!canSend}
                  value={content}
                  onChange={(event) => setContent(event.target.value)}
                />
                <Button
                  size="icon"
                  aria-label="发送私信"
                  disabled={
                    !canSend || !content.trim() || sendMutation.isPending
                  }
                  onClick={() =>
                    sendMutation.mutate({
                      receiverUserId: receiverId,
                      content: content.trim(),
                    })
                  }
                >
                  <Send className="size-4" />
                </Button>
              </div>
            </div>
          </>
        ) : (
          <div className="m-auto text-center">
            <UserRound className="mx-auto size-8 text-[#a69a92]" />
            <p className="mt-3 text-sm text-muted">选择一个会话查看私信</p>
          </div>
        )}
      </section>
    </div>
  )
}

function MessageEmptyHint({ children }: { children: React.ReactNode }) {
  return (
    <p className="py-16 text-center text-sm leading-6 text-muted">{children}</p>
  )
}

function MessageBubble({
  message,
  mine,
  onRead,
}: {
  message: PrivateMessage
  mine: boolean
  onRead: () => void
}) {
  return (
    <button
      type="button"
      className={
        mine ? 'ml-auto max-w-[78%] text-left' : 'mr-auto max-w-[78%] text-left'
      }
      onClick={onRead}
    >
      <span
        className={
          mine
            ? 'block rounded-2xl rounded-br-sm bg-brand px-4 py-2.5 text-sm leading-6 text-white'
            : 'block rounded-2xl rounded-bl-sm bg-white px-4 py-2.5 text-sm leading-6 text-ink shadow-sm'
        }
      >
        {message.content}
      </span>
      <time
        className={
          mine
            ? 'mt-1 block text-right text-[10px] text-muted'
            : 'mt-1 block text-[10px] text-muted'
        }
      >
        {formatRelativeTime(message.createdTime)}
      </time>
    </button>
  )
}

function unreadForTab(
  summary:
    | {
        commentsAndMentions: number
        interactions: number
        system: number
        privateMessages: number
      }
    | undefined,
  tab: string,
) {
  if (!summary) return 0
  if (tab === 'comments') return summary.commentsAndMentions
  if (tab === 'interactions') return summary.interactions
  if (tab === 'system') return summary.system
  return summary.privateMessages
}
