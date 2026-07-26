import { useQuery } from '@tanstack/react-query'
import {
  ArrowUpRight,
  Bookmark,
  BookOpenCheck,
  Clock3,
  FileQuestion,
  Heart,
  ImagePlus,
  MessageSquareText,
  NotebookPen,
  Sparkles,
  Users,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { getUserCenter } from '@/features/user-center/api'
import { LearningCalendar } from '@/features/user-center/components/LearningCalendar'
import { MembershipType } from '@/shared/constants/domain'
import { formatCount } from '@/shared/lib/format'
import { Avatar } from '@/shared/ui/Avatar'
import { Badge } from '@/shared/ui/Badge'
import { Button } from '@/shared/ui/Button'
import { ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

export function UserCenterPage() {
  const [bannerPreview, setBannerPreview] = useState<string | null>(null)
  const centerQuery = useQuery({
    queryKey: ['user-center'],
    queryFn: getUserCenter,
  })

  useEffect(
    () => () => {
      if (bannerPreview) URL.revokeObjectURL(bannerPreview)
    },
    [bannerPreview],
  )

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
  const bannerUrl = bannerPreview || data.graphInfoVO?.url

  return (
    <div className="app-container py-7 sm:py-9">
      <section className="relative h-52 overflow-hidden rounded-[1.4rem] bg-[#6d5a53] text-white sm:h-64">
        {bannerUrl ? (
          <img
            src={bannerUrl}
            alt={data.graphInfoVO?.name || ''}
            className="absolute inset-0 size-full object-cover"
          />
        ) : (
          <>
            <span className="absolute -right-12 -top-20 size-64 rounded-full border-[36px] border-white/10" />
            <span className="absolute bottom-[-5rem] left-[18%] size-52 rounded-full bg-[#879995]/30 blur-2xl" />
          </>
        )}
        <div className="absolute inset-0 bg-gradient-to-r from-[#302824]/80 via-[#493c37]/45 to-transparent" />
        <div className="absolute inset-x-0 bottom-0 h-28 bg-gradient-to-t from-[#2f2925]/65 to-transparent" />
        <div className="relative flex h-full flex-col justify-between p-5 sm:p-7">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="inline-flex items-center gap-2 text-xs font-bold uppercase tracking-[0.15em] text-white/75">
                <Sparkles className="size-3.5" />
                My learning space
              </p>
              {data.graphInfoVO?.name ? (
                <p className="mt-2 max-w-lg text-sm text-white/75">
                  {data.graphInfoVO.name}
                </p>
              ) : null}
            </div>
            <label
              htmlFor="profile-banner-upload"
              className="inline-flex min-h-9 cursor-pointer items-center justify-center gap-2 rounded-xl border border-white/25 bg-black/15 px-3 text-xs font-semibold text-white backdrop-blur-md transition hover:bg-black/25"
            >
              <ImagePlus className="size-4" />
              更换封面
            </label>
            <input
              id="profile-banner-upload"
              className="sr-only"
              type="file"
              accept="image/png,image/jpeg,image/webp"
              onChange={(event) => {
                const file = event.target.files?.[0]
                if (!file) return
                setBannerPreview(URL.createObjectURL(file))
                toast.info('封面预览已更新', {
                  description:
                    '当前后端仅提供 Banner 读取字段，刷新页面后将恢复。',
                })
                event.target.value = ''
              }}
            />
          </div>
          <p className="mb-7 max-w-xl text-sm leading-6 text-white/80 sm:mb-9">
            保持好奇，记录每一次进步。今天的积累，会成为下一次从容作答的底气。
          </p>
        </div>
      </section>

      <section className="relative z-10 mx-3 -mt-7 rounded-2xl bg-surface px-5 py-5 shadow-[0_18px_45px_rgba(58,47,41,0.12)] sm:mx-7 sm:-mt-10 sm:px-7 sm:py-6">
        <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
          <Avatar
            src={data.userInfoVO.avatar}
            name={data.userInfoVO.displayName}
            className="-mt-12 size-20 border-4 border-surface bg-surface shadow-md sm:-mt-14 sm:size-24"
          />
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="truncate text-2xl font-extrabold tracking-tight">
                {data.userInfoVO.displayName}
              </h1>
              {data.membershipActive ? (
                <Badge className="border-[#dfc98f] bg-[#fff7dd] text-[#7f5b15]">
                  {data.membershipType === MembershipType.PREMIUM_PLUS
                    ? 'Premium Plus'
                    : 'Premium'}
                </Badge>
              ) : (
                <Badge>Free</Badge>
              )}
            </div>
            <p className="mt-1 text-sm text-muted">
              @{data.userInfoVO.accountNo}
            </p>
            <div className="mt-3 flex flex-wrap gap-x-5 gap-y-2 text-sm text-muted">
              <span>
                <strong className="text-ink">
                  {formatCount(counts.followerCount)}
                </strong>{' '}
                粉丝
              </span>
              <span>
                <strong className="text-ink">
                  {formatCount(counts.followingCount)}
                </strong>{' '}
                关注
              </span>
              <span>
                <strong className="text-ink">
                  {formatCount(counts.postCount)}
                </strong>{' '}
                Hit
              </span>
            </div>
          </div>
          <Button asChild>
            <Link
              to={data.membershipActive ? '/membership/center' : '/membership'}
            >
              {data.membershipActive ? '会员中心' : '了解会员'}
              <ArrowUpRight className="size-4" />
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
    </div>
  )
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
      className="group rounded-2xl bg-surface p-5 shadow-[0_10px_30px_rgba(58,47,41,0.06)] transition hover:-translate-y-0.5 hover:shadow-[0_15px_38px_rgba(58,47,41,0.1)]"
    >
      <div className="flex items-start justify-between gap-4">
        <span className="flex size-10 items-center justify-center rounded-xl bg-[#eee7e1] text-brand">
          <Icon className="size-5" />
        </span>
        <ArrowUpRight className="size-4 text-[#b3a9a2] transition group-hover:-translate-y-0.5 group-hover:translate-x-0.5 group-hover:text-brand" />
      </div>
      <h3 className="mt-5 font-bold group-hover:text-brand">{title}</h3>
      <p className="mt-2 text-sm leading-6 text-muted">{description}</p>
    </Link>
  )
}
