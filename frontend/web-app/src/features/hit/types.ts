import type { ActionStatusValue } from '@/shared/constants/domain'

export interface HitPost {
  postId: number
  userId: number
  displayName: string
  avatar: string | null
  content: string
  tags: string[]
  commentCount: number
  likeCount: number
  favoriteCount: number
  repostCount: number
  liked: boolean
  favorited: boolean
  reposted: boolean
  createdTime: string
}

export interface HitComment {
  commentId: number
  postId: number
  commentUserId: number
  displayName: string
  avatar: string | null
  parentCommentId: number | null
  comment: string
  likeCount: number
  liked: boolean
  createdTime: string
}

export interface HitActionResult {
  actionType: number
  actionStatus: ActionStatusValue
  likeCount: number
  favoriteCount: number
  repostCount: number
}

export interface HitCommentLikeResult {
  liked: boolean
  likeCount: number
}

export interface MentionUser {
  userId: number
  accountNo: string
  displayName: string
  avatar: string | null
}
