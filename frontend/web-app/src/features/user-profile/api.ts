import { apiRequest } from '@/shared/api/client'
import type {
  FollowState,
  PublicUserActivity,
  PublicUserProfile,
} from '@/features/user-profile/types'

export function getPublicUserProfile(userId: number) {
  return apiRequest<PublicUserProfile>({
    url: `/app/users/${userId}/profile`,
  })
}

export function getPublicUserActivities(
  userId: number,
  tab: string,
  pageNum = 1,
  pageSize = 20,
) {
  return apiRequest<PublicUserActivity[]>({
    url: `/app/users/${userId}/profile/activities`,
    params: { tab, pageNum, pageSize },
  })
}

export function updateFollow(userId: number, active: boolean) {
  return apiRequest<FollowState>({
    url: `/app/users/${userId}/follow`,
    method: 'PUT',
    data: { active },
  })
}
