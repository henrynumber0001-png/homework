import { apiRequest } from '@/shared/api/client'
import type {
  ChatboxPage,
  NotificationPage,
  PrivateChatbox,
  PrivateMessage,
} from '@/features/messages/types'

export function openNotificationTab(tab: string, pageNum = 1, pageSize = 20) {
  return apiRequest<NotificationPage>({
    url: '/app/messages/notifications/open-tab',
    method: 'PUT',
    params: { tab, pageNum, pageSize },
  })
}

export function getNotificationHistory(
  tab: string,
  pageNum = 1,
  pageSize = 20,
) {
  return apiRequest<NotificationPage>({
    url: '/app/messages/notifications/history',
    params: { tab, pageNum, pageSize },
  })
}

export function getChatboxes(pageNum = 1, pageSize = 30) {
  return apiRequest<ChatboxPage>({
    url: '/app/messages/chatboxes',
    params: { pageNum, pageSize },
  })
}

export function getChatboxWith(userId: number) {
  return apiRequest<PrivateChatbox | null>({
    url: `/app/messages/chatboxes/with/${userId}`,
  })
}

export function getPrivateMessages(
  chatboxId: number,
  params?: { beforeId?: number; afterId?: number; limit?: number },
) {
  return apiRequest<PrivateMessage[]>({
    url: `/app/messages/chatboxes/${chatboxId}/messages`,
    params,
  })
}

export function sendPrivateMessage(data: {
  receiverUserId: number
  content: string
}) {
  return apiRequest<PrivateMessage>({
    url: '/app/messages/private',
    method: 'POST',
    data,
  })
}

export function markPrivateMessageRead(messageId: number) {
  return apiRequest<void>({
    url: `/app/messages/private/${messageId}/read`,
    method: 'PUT',
  })
}
