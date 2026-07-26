import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import {
  AtSign,
  BookOpenCheck,
  Clock3,
  Heart,
  MessageCircle,
  Send,
  UserCheck,
  UserPlus,
  Users,
} from 'lucide-react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { HitCard } from '@/features/hit/components/HitCard'
import {
  getPublicUserActivities,
  getPublicUserProfile,
  updateFollow,
} from '@/features/user-profile/api'
import { MembershipType } from '@/shared/constants/domain'
import { cn } from '@/shared/lib/cn'
import { formatCount, formatRelativeTime } from '@/shared/lib/format'
import { Avatar } from '@/shared/ui/Avatar'
import { Badge } from '@/shared/ui/Badge'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { EmptyState, ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

const PAGE_SIZE = 20
const activityTabs = [
  { value: 'posts', label: 'Posts' },
  { value: 'commented', label: 'Commented' },
  { value: 'liked', label: 'Liked' },
  { value: 'favorite', label: 'Favorite' },
] as const

export function PublicUserProfilePage() {
  const queryClient = useQueryClient()
  const { userId: rawUserId } = useParams()
  const userId = Number(rawUserId)
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedTab = searchParams.get('tab')
  const activeTab = activityTabs.some((item) => item.value === requestedTab)
    ? requestedTab!
    : 'posts'

  const profileQuery = useQuery({
    queryKey: ['public-profile', userId],
    queryFn: () => getPublicUserProfile(userId),
  })
  const activitiesQuery = useInfiniteQuery({
    queryKey: ['public-profile', userId, 'activities', activeTab],
    queryFn: ({ pageParam }) =>
      getPublicUserActivities(userId, activeTab, pageParam, PAGE_SIZE),
    initialPageParam: 1,
    getNextPageParam: (lastPage, pages) =>
      lastPage.length < PAGE_SIZE ? undefined : pages.length + 1,
  })
  const followMutation = useMutation({
    mutationFn: (active: boolean) => updateFollow(userId, active),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['public-profile', userId],
      })
    },
  })

  if (profileQuery.isLoading) {
    return (
      <div className="reading-container py-8">
        <PageSkeleton />
      </div>
    )
  }

  if (profileQuery.isError || !profileQuery.data) {
    return (
      <div className="reading-container py-8">
        <ErrorState onRetry={() => void profileQuery.refetch()} />
      </div>
    )
  }

  const profile = profileQuery.data
  const info = profile.membershipInfoVO
  const activities = activitiesQuery.data?.pages.flat() || []

  return (
    <div className="reading-container py-8">
      <Card className="overflow-hidden">
        <div className="h-24 bg-[#d9ccc2]">
          <div className="h-full bg-[radial-gradient(circle_at_20%_10%,rgba(255,255,255,.65),transparent_35%),radial-gradient(circle_at_80%_100%,rgba(98,125,121,.28),transparent_40%)]" />
        </div>
        <div className="px-5 pb-6 sm:px-7">
          <div className="-mt-10 flex flex-wrap items-end justify-between gap-4">
            <Avatar
              src={info.avatarUrl}
              name={info.displayName}
              className="size-20 border-4 border-surface"
            />
            <div className="flex gap-2">
              {profile.self ? (
                <Button asChild variant="secondary">
                  <Link to="/me">进入个人中心</Link>
                </Button>
              ) : (
                <>
                  {profile.canFollow ? (
                    <Button
                      variant={
                        profile.followedByCurrentUser ? 'secondary' : 'primary'
                      }
                      disabled={followMutation.isPending}
                      onClick={() =>
                        followMutation.mutate(!profile.followedByCurrentUser)
                      }
                    >
                      {profile.followedByCurrentUser ? (
                        <UserCheck className="size-4" />
                      ) : (
                        <UserPlus className="size-4" />
                      )}
                      {profile.followedByCurrentUser ? '已关注' : '关注'}
                    </Button>
                  ) : null}
                  {profile.canSendPrivateMessage ? (
                    <Button asChild variant="secondary">
                      <Link to={`/messages?tab=private&userId=${userId}`}>
                        <Send className="size-4" />
                        私信
                      </Link>
                    </Button>
                  ) : null}
                </>
              )}
            </div>
          </div>

          <div className="mt-4 flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-extrabold">{info.displayName}</h1>
            {info.memberStatus ? (
              <Badge className="border-[#dfc98f] bg-[#fff8e8] text-premium">
                {info.membershipType === MembershipType.PREMIUM_PLUS
                  ? 'Premium Plus'
                  : 'Premium'}
              </Badge>
            ) : null}
            {profile.mutualFollow ? (
              <Badge className="border-[#add0c3] bg-[#f0f7f4] text-success">
                互相关注
              </Badge>
            ) : null}
          </div>
          <div className="mt-4 flex flex-wrap gap-x-5 gap-y-2 text-sm text-muted">
            <span>
              <strong className="text-ink">
                {formatCount(profile.followerCount)}
              </strong>{' '}
              粉丝
            </span>
            <span>
              <strong className="text-ink">
                {formatCount(profile.followingCount)}
              </strong>{' '}
              关注
            </span>
            <span>
              <strong className="text-ink">
                {formatCount(profile.postCount)}
              </strong>{' '}
              Hit
            </span>
          </div>
        </div>
      </Card>

      <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <ProfileStat
          icon={AtSign}
          label="累计作答"
          value={`${formatCount(profile.answeredQuestionCount)} 题`}
        />
        <ProfileStat
          icon={BookOpenCheck}
          label="学习题库"
          value={`${formatCount(profile.learnedBankCount)} 个`}
        />
        <ProfileStat
          icon={Clock3}
          label="学习时长"
          value={`${formatCount(profile.studyHours)} 小时`}
        />
        <ProfileStat
          icon={Heart}
          label="收到互动"
          value={formatCount(profile.receivedTotalActionCount)}
        />
      </div>

      <section className="mt-7">
        <div className="flex overflow-x-auto border-b border-line">
          {activityTabs.map((tab) => (
            <button
              key={tab.value}
              type="button"
              className={cn(
                'relative shrink-0 px-4 py-3 text-sm font-semibold text-muted',
                activeTab === tab.value &&
                  'text-brand after:absolute after:inset-x-3 after:bottom-0 after:h-0.5 after:bg-brand',
              )}
              onClick={() => setSearchParams({ tab: tab.value })}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {activitiesQuery.isLoading ? (
          <div className="mt-4">
            <PageSkeleton />
          </div>
        ) : activities.length ? (
          <div className="mt-4 space-y-4">
            {activities.map((activity, index) => (
              <div
                key={`${activity.activityType}-${activity.activityTime}-${index}`}
              >
                <p className="mb-2 flex items-center gap-1.5 text-xs text-muted">
                  {activity.activityType === 'COMMENT' ? (
                    <MessageCircle className="size-3.5" />
                  ) : (
                    <Users className="size-3.5" />
                  )}
                  {activityLabel(activity.activityType)} ·{' '}
                  {formatRelativeTime(activity.activityTime)}
                </p>
                <HitCard post={activity.post} compact />
                {activity.comment ? (
                  <p className="-mt-3 rounded-b-xl border border-t-0 border-line bg-[#faf7f4] px-5 py-3 text-sm text-muted">
                    {activity.comment.comment}
                  </p>
                ) : null}
              </div>
            ))}
            {activitiesQuery.hasNextPage ? (
              <Button
                className="w-full"
                variant="secondary"
                onClick={() => void activitiesQuery.fetchNextPage()}
              >
                加载更多
              </Button>
            ) : null}
          </div>
        ) : (
          <Card className="mt-4">
            <EmptyState title="这个分类还没有内容" />
          </Card>
        )}
      </section>
    </div>
  )
}

function ProfileStat({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof Heart
  label: string
  value: string
}) {
  return (
    <Card className="p-4">
      <Icon className="size-4 text-accent" />
      <p className="mt-3 font-extrabold">{value}</p>
      <p className="mt-1 text-xs text-muted">{label}</p>
    </Card>
  )
}

function activityLabel(type: string) {
  const labels: Record<string, string> = {
    POST: '发布了 Hit',
    REPOST: '转发了 Hit',
    COMMENT: '评论了 Hit',
    LIKED_POST: '点赞了 Hit',
    LIKED_COMMENT: '点赞了评论',
    FAVORITE: '收藏了 Hit',
  }
  return labels[type] || '产生了新动态'
}
