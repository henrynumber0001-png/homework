import { useQuery } from '@tanstack/react-query'
import { apiRequest } from '@/shared/api/client'
import type {
  MembershipInfo,
  MessageUnreadSummary,
  UserInfo,
} from '@/shared/types/session'

export function useCurrentUser() {
  return useQuery({
    queryKey: ['current-user'],
    queryFn: () =>
      apiRequest<UserInfo>({
        url: '/app/user/info',
      }),
  })
}

export function useMembershipCenter() {
  return useQuery({
    queryKey: ['membership', 'center'],
    queryFn: () =>
      apiRequest<MembershipInfo>({
        url: '/app/membership/center',
      }),
  })
}

export function useUnreadSummary() {
  return useQuery({
    queryKey: ['messages', 'unread-summary'],
    queryFn: () =>
      apiRequest<MessageUnreadSummary>({
        url: '/app/messages/unread-summary',
      }),
    refetchInterval: 60_000,
  })
}
