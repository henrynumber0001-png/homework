import {
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import {
  ArrowUpRight,
  BriefcaseBusiness,
  Bookmark,
  BookOpenCheck,
  Code2,
  Clock3,
  FileQuestion,
  Heart,
  ImagePlus,
  Mars,
  MessageCircle,
  MessageSquareText,
  NotebookPen,
  Pencil,
  Users,
  Venus,
} from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'
import {
  getUserCenter,
  getUserCenterActivities,
  getUserProfileOptions,
  replaceUserCenterImage,
} from '@/features/user-center/api'
import type {
  TechDirectionOption,
  UserImageType,
} from '@/features/user-center/types'
import { LearningCalendar } from '@/features/user-center/components/LearningCalendar'
import { HitCard } from '@/features/hit/components/HitCard'
import { MembershipType } from '@/shared/constants/domain'
import { cn } from '@/shared/lib/cn'
import { formatCount, formatRelativeTime } from '@/shared/lib/format'
import { Avatar } from '@/shared/ui/Avatar'
import { Badge } from '@/shared/ui/Badge'
import { Button } from '@/shared/ui/Button'
import { EmptyState, ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'
import { Card } from '@/shared/ui/Card'

const ACTIVITY_PAGE_SIZE = 20
const activityTabs = [
  { value: 'posts', label: 'Posts' },
  { value: 'commented', label: 'Commented' },
  { value: 'liked', label: 'Liked' },
  { value: 'favorite', label: 'Favorite' },
] as const

export function UserCenterPage() {
  const queryClient = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedActivityTab = searchParams.get('activityTab')
  const activeActivityTab = activityTabs.some(
    (tab) => tab.value === requestedActivityTab,
  )
    ? requestedActivityTab!
    : 'posts'
  const centerQuery = useQuery({
    queryKey: ['user-center'],
    queryFn: getUserCenter,
  })
  const profileOptionsQuery = useQuery({
    queryKey: ['user-profile-options'],
    queryFn: getUserProfileOptions,
    staleTime: 60 * 60 * 1000,
  })
  const activitiesQuery = useInfiniteQuery({
    queryKey: ['user-center', 'activities', activeActivityTab],
    queryFn: ({ pageParam }) =>
      getUserCenterActivities(activeActivityTab, pageParam, ACTIVITY_PAGE_SIZE),
    initialPageParam: 1,
    getNextPageParam: (lastPage, pages) =>
      lastPage.length < ACTIVITY_PAGE_SIZE ? undefined : pages.length + 1,
  })
  const imageMutation = useMutation({
    mutationFn: ({
      imageType,
      file,
    }: {
      imageType: UserImageType
      file: File
    }) => replaceUserCenterImage(imageType, file),
    onSuccess: async (_upload, { imageType }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['user-center'] }),
        queryClient.invalidateQueries({ queryKey: ['current-user'] }),
        queryClient.invalidateQueries({ queryKey: ['membership'] }),
      ])
      toast.success(imageType === 'avatar' ? '头像已更新' : '封面已更新')
    },
    onError: (error) => {
      toast.error('图片更新失败', {
        description: error instanceof Error ? error.message : '请稍后重试',
      })
    },
  })

  const submitImage = (imageType: UserImageType, file: File) => {
    const maxSize = imageType === 'avatar' ? 2 * 1024 * 1024 : 5 * 1024 * 1024
    if (file.size > maxSize) {
      toast.error(
        imageType === 'avatar' ? '头像不能超过 2MB' : '封面不能超过 5MB',
      )
      return
    }
    imageMutation.mutate({ imageType, file })
  }

  if (centerQuery.isLoading) {
    return (
      <div className="app-container py-8">
        <PageSkeleton />
      </div>
    )
  }

  if (centerQuery.isError || !centerQuery.data) {
    return (
      <div className="app-container py-8">
        <ErrorState onRetry={() => void centerQuery.refetch()} />
      </div>
    )
  }

  const data = centerQuery.data
  const counts = data.countsVO
  const bannerUrl = data.userInfoVO.bannerUrl
  const avatarUrl = data.userInfoVO.avatarUrl
  const updatingImageType = imageMutation.isPending
    ? imageMutation.variables?.imageType
    : null
  const techDirectionName = findSubTechDirectionName(
    profileOptionsQuery.data?.techDirectionTreeVOList ?? [],
    data.userInfoVO.subTechDirectionId,
  )
  const activities = activitiesQuery.data?.pages.flat() ?? []

  return (
    <div className="app-container py-7 sm:py-9">
      <section className="relative h-52 overflow-hidden rounded-[1.4rem] bg-brand-dark text-white sm:h-64">
        {bannerUrl ? (
          <img
            src={bannerUrl}
            alt="个人中心封面"
            className="absolute inset-0 size-full object-cover"
          />
        ) : (
          <>
            <span className="absolute -right-12 -top-20 size-64 rounded-full border-[36px] border-white/10" />
            <span className="absolute bottom-[-5rem] left-[18%] size-52 rounded-full bg-[#4f86b5]/35 blur-2xl" />
          </>
        )}
        <div className="absolute inset-0 bg-gradient-to-r from-[#0b2445]/80 via-[#123d69]/40 to-transparent" />
        <div className="absolute inset-x-0 bottom-0 h-28 bg-gradient-to-t from-[#091f3c]/65 to-transparent" />
        <div className="relative flex h-full items-start justify-end p-5 sm:p-7">
          <div>
            <label
              htmlFor="profile-banner-upload"
              className="inline-flex min-h-9 cursor-pointer items-center justify-center gap-2 rounded-xl border border-white/25 bg-black/15 px-3 text-xs font-semibold text-white backdrop-blur-md transition hover:bg-black/25 aria-disabled:pointer-events-none aria-disabled:opacity-60"
              aria-disabled={imageMutation.isPending}
            >
              <ImagePlus className="size-4" />
              {updatingImageType === 'banner' ? '上传中…' : '更换封面'}
            </label>
            <input
              id="profile-banner-upload"
              className="sr-only"
              type="file"
              accept="image/png,image/jpeg,image/webp"
              disabled={imageMutation.isPending}
              onChange={(event) => {
                const file = event.target.files?.[0]
                if (!file) return
                submitImage('banner', file)
                event.target.value = ''
              }}
            />
          </div>
        </div>
      </section>

      <section className="relative z-10 mx-3 -mt-7 rounded-2xl bg-surface px-5 py-5 shadow-[0_18px_45px_rgba(15,31,61,0.12)] sm:mx-7 sm:-mt-10 sm:px-7 sm:py-6">
        <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
          <div className="relative -mt-12 shrink-0 sm:-mt-14">
            <label
              htmlFor="profile-avatar-upload"
              aria-disabled={imageMutation.isPending}
              title="点击更换头像"
              className="group block cursor-pointer rounded-full aria-disabled:pointer-events-none aria-disabled:opacity-60"
            >
              <Avatar
                src={avatarUrl}
                name={data.userInfoVO.displayName}
                className="size-20 border-4 border-surface bg-surface shadow-md transition group-hover:ring-2 group-hover:ring-brand/35 sm:size-24"
              />
            </label>
            <input
              id="profile-avatar-upload"
              className="sr-only"
              type="file"
              aria-label={
                updatingImageType === 'avatar' ? '头像上传中' : '更换头像'
              }
              accept="image/png,image/jpeg,image/webp"
              disabled={imageMutation.isPending}
              onChange={(event) => {
                const file = event.target.files?.[0]
                if (!file) return
                submitImage('avatar', file)
                event.target.value = ''
              }}
            />
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="truncate text-2xl font-extrabold tracking-tight">
                {data.userInfoVO.displayName}
              </h1>
              <Link
                to={
                  data.membershipActive ? '/membership/center' : '/membership'
                }
                aria-label={
                  data.membershipActive ? '进入会员中心' : '了解 Premium 会员'
                }
              >
                {data.membershipActive ? (
                  <Badge className="border-[#dfc98f] bg-premium-soft text-[#77500d] transition hover:-translate-y-0.5">
                    {data.membershipType === MembershipType.PREMIUM_PLUS
                      ? 'Premium Plus'
                      : 'Premium'}
                  </Badge>
                ) : (
                  <Badge className="transition hover:-translate-y-0.5">
                    Free
                  </Badge>
                )}
              </Link>
            </div>
            <p className="mt-1 text-sm text-muted">
              @{data.userInfoVO.accountNo}
            </p>
            {data.userInfoVO.introduction ? (
              <p className="mt-2 max-w-2xl text-sm leading-6 text-ink/80">
                {data.userInfoVO.introduction}
              </p>
            ) : null}
            <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1.5 text-xs text-muted">
              {data.userInfoVO.gender ? (
                <span className="inline-flex items-center gap-1.5">
                  {data.userInfoVO.gender === 1 ? (
                    <Mars className="size-3.5 text-brand" />
                  ) : (
                    <Venus className="size-3.5 text-accent" />
                  )}
                  {data.userInfoVO.gender === 1 ? '男' : '女'}
                </span>
              ) : null}
              {techDirectionName ? (
                <span className="inline-flex items-center gap-1.5">
                  <Code2 className="size-3.5 text-brand" />
                  {techDirectionName}
                </span>
              ) : null}
              {data.userInfoVO.companyOrSchool ? (
                <span className="inline-flex items-center gap-1.5">
                  <BriefcaseBusiness className="size-3.5 text-brand" />
                  {data.userInfoVO.companyOrSchool}
                </span>
              ) : null}
            </div>
            <div className="mt-3 flex flex-wrap gap-x-5 gap-y-2 text-sm text-muted">
              <Link
                to="/me/followers"
                className="rounded-md transition hover:text-brand focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/35"
              >
                <strong className="text-ink">
                  {formatCount(counts.followerCount)}
                </strong>{' '}
                粉丝
              </Link>
              <Link
                to="/me/following"
                className="rounded-md transition hover:text-brand focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/35"
              >
                <strong className="text-ink">
                  {formatCount(counts.followingCount)}
                </strong>{' '}
                关注
              </Link>
            </div>
          </div>
          <Button asChild>
            <Link to="/me/edit">
              <Pencil className="size-4" />
              修改资料
            </Link>
          </Button>
        </div>
      </section>

      <dl className="mt-9 grid grid-cols-3 gap-x-3 gap-y-7 px-1 sm:grid-cols-6 sm:px-5">
        <StatItem
          icon={FileQuestion}
          label="累计作答"
          value={counts.answeredQuestionCount}
          suffix="题"
        />
        <StatItem
          icon={BookOpenCheck}
          label="学习题库"
          value={counts.learnedBankCount}
          suffix="个"
        />
        <StatItem
          icon={Clock3}
          label="学习时间"
          value={counts.studyHours}
          suffix="小时"
        />
        <StatItem
          icon={MessageSquareText}
          label="错题"
          value={counts.wrongQuestionCount}
          suffix="题"
        />
        <StatItem
          icon={Heart}
          label="收藏"
          value={counts.favoriteQuestionCount}
          suffix="题"
        />
        <StatItem
          icon={NotebookPen}
          label="笔记"
          value={counts.noteCount}
          suffix="条"
        />
      </dl>

      <section className="mt-10">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.14em] text-accent">
            Learning library
          </p>
          <h2 className="mt-1 text-xl font-extrabold tracking-tight">
            我的学习资料
          </h2>
        </div>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <LibraryCard
            to="/me/wrong-questions"
            icon={MessageSquareText}
            title="错题本"
            description="集中复习回答错误或掌握不稳的题目"
          />
          <LibraryCard
            to="/me/favorites"
            icon={Bookmark}
            title="题目收藏"
            description="保存值得反复阅读和练习的题目"
          />
          <LibraryCard
            to="/me/notes"
            icon={NotebookPen}
            title="我的笔记"
            description="回顾答题时记录的知识点和想法"
          />
        </div>
      </section>

      <div className="mt-7">
        <LearningCalendar />
      </div>

      <section className="mt-10">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.14em] text-accent">
            Hit activity
          </p>
          <h2 className="mt-1 text-xl font-extrabold tracking-tight">
            我的动态
          </h2>
        </div>

        <div className="mt-4 flex overflow-x-auto border-b border-line">
          {activityTabs.map((tab) => (
            <button
              key={tab.value}
              type="button"
              className={cn(
                'relative shrink-0 px-4 py-3 text-sm font-semibold text-muted',
                activeActivityTab === tab.value &&
                  'text-brand after:absolute after:inset-x-3 after:bottom-0 after:h-0.5 after:bg-brand',
              )}
              onClick={() => setSearchParams({ activityTab: tab.value })}
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
                  <p className="-mt-3 rounded-b-xl border border-t-0 border-line bg-surface-muted px-5 py-3 text-sm text-muted">
                    {activity.comment.comment}
                  </p>
                ) : null}
              </div>
            ))}
            {activitiesQuery.hasNextPage ? (
              <Button
                className="w-full"
                variant="secondary"
                disabled={activitiesQuery.isFetchingNextPage}
                onClick={() => void activitiesQuery.fetchNextPage()}
              >
                {activitiesQuery.isFetchingNextPage ? '加载中…' : '加载更多'}
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

function StatItem({
  icon: Icon,
  label,
  value,
  suffix,
}: {
  icon: typeof Users
  label: string
  value: number
  suffix: string
}) {
  return (
    <div className="text-center">
      <dt className="flex items-center justify-center gap-1.5 text-xs font-medium text-muted">
        <Icon className="size-3.5 text-accent" />
        {label}
      </dt>
      <dd className="mt-2 text-2xl font-black tracking-tight text-ink">
        {formatCount(value)}
        <span className="ml-1 text-[11px] font-medium tracking-normal text-muted">
          {suffix}
        </span>
      </dd>
    </div>
  )
}

function LibraryCard({
  to,
  icon: Icon,
  title,
  description,
}: {
  to: string
  icon: typeof Bookmark
  title: string
  description: string
}) {
  return (
    <Link
      to={to}
      className="group rounded-2xl bg-surface p-5 shadow-[0_10px_30px_rgba(15,31,61,0.06)] transition-[transform,box-shadow] duration-150 ease-[var(--ease-out-ui)] hover:-translate-y-0.5 hover:shadow-[0_15px_38px_rgba(15,31,61,0.11)]"
    >
      <div className="flex items-start justify-between gap-4">
        <span className="flex size-10 items-center justify-center rounded-xl bg-brand-soft text-brand">
          <Icon className="size-5" />
        </span>
        <ArrowUpRight className="size-4 text-placeholder transition group-hover:-translate-y-0.5 group-hover:translate-x-0.5 group-hover:text-brand" />
      </div>
      <h3 className="mt-5 font-bold group-hover:text-brand">{title}</h3>
      <p className="mt-2 text-sm leading-6 text-muted">{description}</p>
    </Link>
  )
}
