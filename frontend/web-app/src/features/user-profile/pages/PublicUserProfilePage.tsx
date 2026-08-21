import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import {
  AtSign,
  Ban,
  BookOpenCheck,
  BriefcaseBusiness,
  Code2,
  Clock3,
  LoaderCircle,
  Mars,
  Send,
  UserCheck,
  UserPlus,
  Venus,
} from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { HitCard } from '@/features/hit/components/HitCard'
import { getUserProfileOptions } from '@/features/user-center/api'
import type { TechDirectionOption } from '@/features/user-center/types'
import {
  getPublicUserPosts,
  getPublicUserProfile,
  updateBlock,
  updateFollow,
} from '@/features/user-profile/api'
import {
  BlockStatus,
  type PublicUserProfile,
} from '@/features/user-profile/types'
import {
  ActionStatus,
  type ActionStatusValue,
  MembershipStatus,
  MembershipType,
} from '@/shared/constants/domain'
import { formatCount } from '@/shared/lib/format'
import { Avatar } from '@/shared/ui/Avatar'
import { Badge } from '@/shared/ui/Badge'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { EmptyState, ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

const PAGE_SIZE = 20
export function PublicUserProfilePage() {
  const queryClient = useQueryClient()
  const { userId: rawUserId } = useParams()
  const userId = Number(rawUserId)

  const profileQuery = useQuery({
    queryKey: ['public-profile', userId],
    queryFn: () => getPublicUserProfile(userId),
  })
  const profileOptionsQuery = useQuery({
    queryKey: ['user-profile-options'],
    queryFn: getUserProfileOptions,
    enabled: Boolean(profileQuery.data && !profileQuery.data.blocked),
    staleTime: 60 * 60 * 1000,
  })
  const postsQuery = useInfiniteQuery({
    queryKey: ['public-profile', userId, 'posts'],
    queryFn: ({ pageParam }) =>
      getPublicUserPosts(userId, pageParam, PAGE_SIZE),
    initialPageParam: 1,
    enabled: Boolean(profileQuery.data && !profileQuery.data.blocked),
    getNextPageParam: (lastPage, pages) =>
      lastPage.length < PAGE_SIZE ? undefined : pages.length + 1,
  })
  const followMutation = useMutation({
    mutationFn: (status: ActionStatusValue) => updateFollow(userId, status),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['public-profile', userId],
      })
    },
  })
  const blockMutation = useMutation({
    mutationFn: (blockedByCurrentUser: boolean) =>
      updateBlock(
        userId,
        blockedByCurrentUser ? BlockStatus.DEACTIVATE : BlockStatus.ACTIVATE,
      ),
    onSuccess: (result) => {
      queryClient.setQueryData<PublicUserProfile>(
        ['public-profile', userId],
        (current) =>
          current
            ? { ...current, blockedByCurrentUser: result.blocked }
            : current,
      )
      void queryClient.invalidateQueries({
        queryKey: ['public-profile', userId],
      })
      toast.success(result.blocked ? '已拉黑该用户' : '已解除拉黑')
    },
    onError: () => {
      toast.error('拉黑状态更新失败，请稍后重试')
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
  const info = profile.userInfo
  const posts = postsQuery.data?.pages.flat() || []
  const techDirectionName = findSubTechDirectionName(
    profileOptionsQuery.data?.techDirectionTreeVOList ?? [],
    info.subTechDirectionId ?? null,
  )

  return (
    <div className="reading-container py-8">
      <Card className="overflow-hidden">
        <div className="relative h-36 overflow-hidden bg-[#173f73] sm:h-48">
          {info.bannerUrl ? (
            <img
              src={info.bannerUrl}
              alt={`${info.displayName}的主页封面`}
              className="absolute inset-0 z-0 size-full object-cover"
            />
          ) : null}
          <div className="absolute inset-0 z-10 bg-[radial-gradient(circle_at_20%_10%,rgba(255,255,255,.24),transparent_35%),radial-gradient(circle_at_80%_100%,rgba(79,163,220,.34),transparent_40%)]" />
          <div className="absolute inset-x-0 bottom-0 z-10 h-20 bg-gradient-to-t from-black/25 to-transparent" />
          {!profile.self ? (
            <Button
              className="absolute bottom-3 right-3 z-20 min-h-8 rounded-full border border-white/40 bg-slate-950/30 px-3 text-[11px] text-white shadow-sm shadow-black/10 backdrop-blur-md hover:border-white/60 hover:bg-slate-950/45 sm:bottom-4 sm:right-4"
              size="sm"
              variant="ghost"
              aria-pressed={profile.blockedByCurrentUser}
              disabled={blockMutation.isPending}
              onClick={() => blockMutation.mutate(profile.blockedByCurrentUser)}
            >
              {blockMutation.isPending ? (
                <LoaderCircle className="size-3.5 animate-spin motion-reduce:animate-none" />
              ) : (
                <Ban className="size-3.5" />
              )}
              {blockMutation.isPending
                ? '处理中'
                : profile.blockedByCurrentUser
                  ? '解除拉黑'
                  : '拉黑'}
            </Button>
          ) : null}
        </div>
        <div className="relative flow-root px-5 pb-6 sm:px-7">
          <div className="-mt-11 flex sm:-mt-12">
            <div
              data-testid="profile-avatar-layer"
              className="relative z-20 size-[5.5rem] shrink-0 overflow-hidden rounded-full bg-surface p-1 shadow-lg sm:size-[6.5rem]"
            >
              <Avatar
                src={info.avatarUrl}
                name={info.displayName}
                className="size-full"
              />
            </div>
          </div>
          {!profile.self ? (
            <div className="absolute right-5 top-3 z-20 flex flex-col gap-2 sm:right-7">
              <Button
                variant={
                  profile.followedByCurrentUser ? 'secondary' : 'primary'
                }
                disabled={followMutation.isPending}
                onClick={() =>
                  followMutation.mutate(
                    profile.followedByCurrentUser
                      ? ActionStatus.DEACTIVATE
                      : ActionStatus.ACTIVATE,
                  )
                }
              >
                {profile.followedByCurrentUser ? (
                  <UserCheck className="size-4" />
                ) : (
                  <UserPlus className="size-4" />
                )}
                {profile.mutualFollow
                  ? 'Mutual'
                  : profile.followedByCurrentUser
                    ? 'Following'
                    : 'Follow'}
              </Button>
              {profile.canSendPrivateMessage ? (
                <Button asChild variant="secondary">
                  <Link
                    to={
                      profile.chatboxId
                        ? `/messages?tab=private&chatboxId=${profile.chatboxId}`
                        : `/messages?tab=private&userId=${userId}`
                    }
                  >
                    <Send className="size-4" />
                    私信
                  </Link>
                </Button>
              ) : null}
            </div>
          ) : null}

          <div className="mt-4 flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-extrabold">{info.displayName}</h1>
            {profile.membershipStatus !== MembershipStatus.FREE ? (
              <Badge className="border-[#dfc98f] bg-premium-soft text-premium">
                {profile.membershipType === MembershipType.PREMIUM_PLUS
                  ? 'Premium Plus'
                  : 'Premium'}
              </Badge>
            ) : null}
            {profile.mutualFollow && !profile.blocked ? (
              <Badge className="border-[#add0c3] bg-success-soft text-success">
                互相关注
              </Badge>
            ) : null}
          </div>
          {!profile.blocked && info.accountNo ? (
            <p className="mt-1 text-sm text-muted">@{info.accountNo}</p>
          ) : null}
          {!profile.blocked && info.introduction ? (
            <p className="mt-3 max-w-2xl text-sm leading-6 text-ink/80">
              {info.introduction}
            </p>
          ) : null}
          {!profile.blocked ? (
            <div className="mt-3 flex flex-wrap gap-x-4 gap-y-2 text-xs text-muted">
              {info.gender ? (
                <span className="inline-flex items-center gap-1.5">
                  {info.gender === 1 ? (
                    <Mars className="size-3.5 text-brand" />
                  ) : (
                    <Venus className="size-3.5 text-accent" />
                  )}
                  {info.gender === 1 ? '男' : '女'}
                </span>
              ) : null}
              {techDirectionName ? (
                <span className="inline-flex items-center gap-1.5">
                  <Code2 className="size-3.5 text-brand" />
                  {techDirectionName}
                </span>
              ) : null}
              {info.companyOrSchool ? (
                <span className="inline-flex items-center gap-1.5">
                  <BriefcaseBusiness className="size-3.5 text-brand" />
                  {info.companyOrSchool}
                </span>
              ) : null}
            </div>
          ) : null}
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
          </div>
        </div>
      </Card>

      {!profile.blocked ? (
        <>
          <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
            <ProfileStat
              icon={AtSign}
              label="累计作答"
              value={`${formatCount(profile.answeredQuestionCount ?? 0)} 题`}
            />
            <ProfileStat
              icon={BookOpenCheck}
              label="学习题库"
              value={`${formatCount(profile.learnedBankCount ?? 0)} 个`}
            />
            <ProfileStat
              icon={Clock3}
              label="学习时长"
              value={`${formatCount(profile.studyHours ?? 0)} 小时`}
            />
          </div>

          <section className="mt-7">
            <h2 className="text-xl font-extrabold tracking-tight">Posts</h2>

            {postsQuery.isLoading ? (
              <div className="mt-4">
                <PageSkeleton />
              </div>
            ) : posts.length ? (
              <div className="mt-4 space-y-4">
                {posts.map((post) => (
                  <div key={post.postId}>
                    <HitCard post={post} compact />
                  </div>
                ))}
                {postsQuery.hasNextPage ? (
                  <Button
                    className="w-full"
                    variant="secondary"
                    onClick={() => void postsQuery.fetchNextPage()}
                  >
                    加载更多
                  </Button>
                ) : null}
              </div>
            ) : (
              <Card className="mt-4">
                <EmptyState title="这个用户还没有发布 Post" />
              </Card>
            )}
          </section>
        </>
      ) : null}
    </div>
  )
}

function findSubTechDirectionName(
  directions: TechDirectionOption[],
  selectedId: number | null,
) {
  if (selectedId === null) return null
  for (const direction of directions) {
    const selected = direction.subTechDirectionTreeVOList.find(
      (item) => item.subTechDirectionId === selectedId,
    )
    if (selected) return selected.subTechDirectionName
  }
  return null
}

function ProfileStat({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof AtSign
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
