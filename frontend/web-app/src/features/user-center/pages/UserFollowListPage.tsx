import {
  useInfiniteQuery,
  useMutation,
  useQueryClient,
} from '@tanstack/react-query'
import { ArrowLeft, UserCheck, UserPlus, Users } from 'lucide-react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { getFollowers, getFollowing } from '@/features/user-center/api'
import type { FollowListKind } from '@/features/user-center/types'
import { updateFollow } from '@/features/user-profile/api'
import { Avatar } from '@/shared/ui/Avatar'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { EmptyState, ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

const PAGE_SIZE = 20

interface FollowListRow {
  userId: number
  displayName: string
  avatarUrl: string | null
  mutualFollow: boolean
}

const pageCopy: Record<
  FollowListKind,
  { title: string; description: string; empty: string }
> = {
  followers: {
    title: '粉丝',
    description: '正在关注你的人',
    empty: '还没有粉丝',
  },
  following: {
    title: '关注',
    description: '你正在关注的人',
    empty: '还没有关注任何人',
  },
}

export function UserFollowListPage({ kind }: { kind: FollowListKind }) {
  const queryClient = useQueryClient()
  const copy = pageCopy[kind]
  const listQuery = useInfiniteQuery({
    queryKey: ['user-center', kind],
    queryFn: async ({ pageParam }) => {
      if (kind === 'followers') {
        return (await getFollowers(pageParam, PAGE_SIZE)).map((item) => ({
          userId: item.followerUserId,
          displayName: item.followerDisplayName,
          avatarUrl: item.followerAvatarUrl,
          mutualFollow: item.mutualFollow,
        }))
      }
      return (await getFollowing(pageParam, PAGE_SIZE)).map((item) => ({
        userId: item.followeeUserId,
        displayName: item.followeeDisplayName,
        avatarUrl: item.followeeAvatarUrl,
        mutualFollow: item.mutualFollow,
      }))
    },
    initialPageParam: 1,
    getNextPageParam: (lastPage, pages) =>
      lastPage.length < PAGE_SIZE ? undefined : pages.length + 1,
  })
  const followMutation = useMutation({
    mutationFn: ({ userId, active }: { userId: number; active: boolean }) =>
      updateFollow(userId, active),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['user-center'] }),
        queryClient.invalidateQueries({ queryKey: ['public-profile'] }),
      ])
    },
    onError: (error) => {
      toast.error('关注状态更新失败', {
        description: error instanceof Error ? error.message : '请稍后重试',
      })
    },
  })

  const rows: FollowListRow[] = listQuery.data?.pages.flat() ?? []

  return (
    <div className="reading-container py-8">
      <Button asChild variant="ghost" size="sm">
        <Link to="/me">
          <ArrowLeft className="size-4" />
          返回个人中心
        </Link>
      </Button>

      <header className="mt-4">
        <div className="flex items-center gap-3">
          <span className="flex size-11 items-center justify-center rounded-2xl bg-brand-soft text-brand">
            <Users className="size-5" />
          </span>
          <div>
            <h1 className="text-3xl font-extrabold">{copy.title}</h1>
            <p className="mt-1 text-sm text-muted">{copy.description}</p>
          </div>
        </div>
      </header>

      {listQuery.isLoading ? (
        <div className="mt-6">
          <PageSkeleton />
        </div>
      ) : listQuery.isError ? (
        <div className="mt-6">
          <ErrorState onRetry={() => void listQuery.refetch()} />
        </div>
      ) : rows.length ? (
        <Card className="mt-6 overflow-hidden">
          <div className="divide-y divide-line/70">
            {rows.map((row) => {
              const followedByCurrentUser =
                kind === 'following' || row.mutualFollow
              const pending =
                followMutation.isPending &&
                followMutation.variables?.userId === row.userId

              return (
                <div
                  key={row.userId}
                  className="flex items-center gap-3 px-4 py-3.5 sm:px-5"
                >
                  <Link
                    to={`/users/${row.userId}`}
                    className="flex min-w-0 flex-1 items-center gap-3 rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/35"
                  >
                    <Avatar
                      src={row.avatarUrl}
                      name={row.displayName}
                      className="size-12"
                    />
                    <span className="truncate font-bold text-ink">
                      {row.displayName}
                    </span>
                  </Link>
                  <Button
                    size="sm"
                    variant={followedByCurrentUser ? 'secondary' : 'primary'}
                    disabled={pending}
                    onClick={() =>
                      followMutation.mutate({
                        userId: row.userId,
                        active: !followedByCurrentUser,
                      })
                    }
                  >
                    {followedByCurrentUser ? (
                      <UserCheck className="size-4" />
                    ) : (
                      <UserPlus className="size-4" />
                    )}
                    {row.mutualFollow
                      ? 'Mutual'
                      : followedByCurrentUser
                        ? 'Following'
                        : 'Follow'}
                  </Button>
                </div>
              )
            })}
          </div>
          {listQuery.hasNextPage ? (
            <div className="border-t border-line p-3">
              <Button
                className="w-full"
                variant="secondary"
                disabled={listQuery.isFetchingNextPage}
                onClick={() => void listQuery.fetchNextPage()}
              >
                {listQuery.isFetchingNextPage ? '加载中…' : '加载更多'}
              </Button>
            </div>
          ) : null}
        </Card>
      ) : (
        <Card className="mt-6">
          <EmptyState title={copy.empty} />
        </Card>
      )}
    </div>
  )
}
