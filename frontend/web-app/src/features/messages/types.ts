import type { PageResult } from '@/shared/api/result'

export interface Notification {
  id: number
  actionUserId: number | null
  actionDisplayName: string | null
  actionAvatar: string | null
  notificationType: number
  postId: number | null
  commentId: number | null
  commentDeleted: boolean
  postAvailable: boolean
  title: string
  content: string
  readStatus: number
  createdTime: string
}

export interface PrivateChatbox {
  chatboxId: number
  otherUserId: number
  otherDisplayName: string
  otherAvatar: string | null
  chatAccess: number
  canCurrentUserSend: boolean
  lastMessage: string | null
  lastMessageTime: string | null
  unreadCount: number
}

export interface PrivateMessage {
  id: number
  chatboxId: number
  senderUserId: number
  senderDisplayName: string
  senderAvatar: string | null
  receiverUserId: number
  content: string
  messageStatus: number
  createdTime: string
}

export type NotificationPage = PageResult<Notification>
export type ChatboxPage = PageResult<PrivateChatbox>
