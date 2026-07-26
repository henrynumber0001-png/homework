import { apiRequest } from '@/shared/api/client'
import type {
  HitActionResult,
  HitComment,
  HitCommentLikeResult,
  HitPost,
  MentionUser,
} from '@/features/hit/types'
import type { ActionStatusValue } from '@/shared/constants/domain'

export function listHits(pageNum = 1, pageSize = 20) {
  return apiRequest<HitPost[]>({
    url: '/app/hits',
    params: { pageNum, pageSize },
  })
}

export function updateHitCommentLike(
  postId: number,
  commentId: number,
  actionStatus: ActionStatusValue,
) {
  return apiRequest<HitCommentLikeResult>({
    url: `/app/hits/${postId}/comments/${commentId}/like`,
    method: 'PUT',
    data: { actionStatus },
  })
}

export function searchMentionUsers(keyword: string, limit = 8) {
  return apiRequest<MentionUser[]>({
    url: '/app/users/search',
    params: { keyword, limit },
  })
}

export function publishHit(data: {
  content: string
  tags: string[]
  mentionedUserIds: number[]
}) {
  return apiRequest<number>({
    url: '/app/hits',
    method: 'POST',
    data,
  })
}

export function listHitComments(postId: number, pageNum = 1, pageSize = 20) {
  return apiRequest<HitComment[]>({
    url: `/app/hits/${postId}/comments`,
    params: { pageNum, pageSize },
  })
}

export function createHitComment(
  postId: number,
  data: {
    parentCommentId: number | null
    comment: string
    mentionedUserIds: number[]
  },
) {
  return apiRequest<number>({
    url: `/app/hits/${postId}/comments`,
    method: 'POST',
    data,
  })
}

export function updateHitAction(
  postId: number,
  actionType: number,
  actionStatus: ActionStatusValue,
) {
  return apiRequest<HitActionResult>({
    url: `/app/hits/${postId}/actions`,
    method: 'POST',
    data: { actionType, actionStatus },
  })
}
