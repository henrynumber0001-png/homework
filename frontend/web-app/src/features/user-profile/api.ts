import { apiRequest } from '@/shared/api/client'
import type {
  BlockResult,
  BlockStatusValue,
  FollowState,
  PublicUserProfile,
} from '@/features/user-profile/types'
import type { HitPost } from '@/features/hit/types'
import type { ActionStatusValue } from '@/shared/constants/domain'

export function getPublicUserProfile(userId: number) {
  return apiRequest<PublicUserProfile>({
    url: `/app/users/${userId}/profile`,
  })
}

export function getPublicUserPosts(userId: number, pageNum = 1, pageSize = 20) {
  return apiRequest<HitPost[]>({
    url: `/app/users/${userId}/profile/posts`,
    params: { pageNum, pageSize },
  })
}

export function updateFollow(userId: number, status: ActionStatusValue) {
  return apiRequest<FollowState>({
    url: `/app/users/${userId}/follow`,
    method: 'PUT',
    params: { status },
  })
}

export function updateBlock(userId: number, blockStatus: BlockStatusValue) {
  return apiRequest<BlockResult>({
    url: `/app/users/${userId}/block`,
    method: 'PUT',
    data: { blockStatus },
  })
}
