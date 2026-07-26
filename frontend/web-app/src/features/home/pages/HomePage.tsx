import { useMutation, useQuery } from '@tanstack/react-query'
import {
  ArrowRight,
  BookOpen,
  CheckCircle2,
  GraduationCap,
  Sparkles,
  Users,
} from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getHomePage } from '@/features/home/api'
import type { HotQuestionBank } from '@/features/home/types'
import { HitCard } from '@/features/hit/components/HitCard'
import { startCertificateExam } from '@/features/question/api'
import { cn } from '@/shared/lib/cn'
import { formatCount, formatRate } from '@/shared/lib/format'
import { useCurrentUser } from '@/shared/queries/session'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { Dialog } from '@/shared/ui/Dialog'
import { EmptyState, ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

const bankSectionTones = {
  interview: {
    card: 'border-[#dbc5bc]',
    background: '#f1e5e0',
    border: '#dbc5bc',
    divider: 'divide-[#dcc9c2]',
    row: 'hover:bg-[#e2ccc4] focus-visible:bg-[#e2ccc4] active:bg-[#d4b8ae]',
    icon: 'bg-[#ad7765] text-white shadow-[#8b5f51]/25',
    title: 'group-hover:text-[#65483f] group-focus-visible:text-[#65483f]',
    accuracy:
      'border-[#cda99c] bg-[#fff9f6] text-[#73564c] group-hover:border-[#718981] group-hover:bg-[#718981] group-hover:text-white group-focus-visible:border-[#718981] group-focus-visible:bg-[#718981] group-focus-visible:text-white',
    rail: 'bg-[#718981]',
    heading: 'bg-[#ad7765]',
  },
  certification: {
    card: 'border-[#c4d1d4]',
    background: '#e6edef',
    border: '#c4d1d4',
    divider: 'divide-[#cbd6d8]',
    row: 'hover:bg-[#cedcdf] focus-visible:bg-[#cedcdf] active:bg-[#bdced2]',
    icon: 'bg-[#718895] text-white shadow-[#586d78]/25',
    title: 'group-hover:text-[#425965] group-focus-visible:text-[#425965]',
    accuracy:
      'border-[#c9b5c1] bg-[#fcf8fb] text-[#715a67] group-hover:border-[#8a7180] group-hover:bg-[#8a7180] group-hover:text-white group-focus-visible:border-[#8a7180] group-focus-visible:bg-[#8a7180] group-focus-visible:text-white',
    rail: 'bg-[#a87866]',
    heading: 'bg-[#718895]',
  },
} as const

const hitTones = [
  { background: '#fffaf8', border: '#ddc8bf' },
  { background: '#f9fbfc', border: '#c6d4d8' },
  { background: '#fafcf9', border: '#c8d4c7' },
  { background: '#fcf9fb', border: '#d5c6cf' },
] as const

export function HomePage() {
  const navigate = useNavigate()
  const currentUser = useCurrentUser()
  const [certificateBank, setCertificateBank] =
    useState<HotQuestionBank | null>(null)
  const examMutation = useMutation({
    mutationFn: startCertificateExam,
    onSuccess: (exam) => {
      setCertificateBank(null)
      navigate(`/banks/certification/exams/${exam.sessionId}`)
    },
  })
  const homeQuery = useQuery({
    queryKey: ['home'],
    queryFn: getHomePage,
  })

  if (homeQuery.isLoading) {
    return (
      <div className="app-container py-8">
        <PageSkeleton />
      </div>
    )
  }

  if (homeQuery.isError || !homeQuery.data) {
    return (
      <div className="app-container py-8">
        <ErrorState onRetry={() => void homeQuery.refetch()} />
      </div>
    )
  }

  const data = homeQuery.data

  return (
    <div className="app-container relative isolate overflow-x-clip py-7 sm:py-9">
      <span
        className="pointer-events-none absolute -right-28 top-72 -z-10 hidden size-72 rounded-full bg-[#cddbd7]/45 blur-3xl sm:block"
        aria-hidden="true"
      />
      <span
        className="pointer-events-none absolute -left-32 top-[38rem] -z-10 hidden size-64 rounded-full bg-[#dfc8bf]/35 blur-3xl sm:block"
        aria-hidden="true"
      />

      <section className="group relative overflow-hidden rounded-3xl border border-[#d2beb5] bg-[linear-gradient(125deg,#e4cec5_0%,#eee3da_52%,#d5e1dd_100%)] px-6 py-7 shadow-[0_18px_46px_rgba(82,65,57,0.10)] transition-all duration-500 hover:-translate-y-0.5 hover:shadow-[0_24px_58px_rgba(82,65,57,0.15)] motion-reduce:transition-none motion-reduce:hover:translate-y-0 sm:px-8 sm:py-9">
        <div className="absolute -right-10 -top-20 size-64 rounded-full border-[34px] border-white/25 transition-transform duration-700 group-hover:rotate-12 group-hover:scale-110 motion-reduce:transition-none" />
        <div className="absolute -bottom-20 right-44 hidden size-44 rounded-full bg-[#8fa49d]/20 blur-sm transition-transform duration-700 group-hover:-translate-x-4 group-hover:-translate-y-2 lg:block" />
        <div className="relative max-w-2xl">
          <p className="inline-flex items-center gap-2 rounded-full bg-white/60 px-3 py-1.5 text-xs font-bold text-[#72564c] shadow-sm backdrop-blur">
            <Sparkles className="size-3.5 text-[#9f6d5b]" />
            今天也向前一点
          </p>
          <h1 className="mt-2 text-2xl font-extrabold tracking-tight sm:text-3xl">
            {currentUser.data?.displayName
              ? `${currentUser.data.displayName}，欢迎回来`
              : '欢迎回到 HomeWork'}
          </h1>
          <p className="mt-3 max-w-xl text-sm leading-6 text-muted sm:text-base">
            从一道题开始，或者看看大家最近在讨论什么。
          </p>
          <div className="mt-5 flex flex-wrap gap-3">
            <Link
              to="/banks/interview"
              className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-[#9d6d5d] px-4 text-sm font-bold text-white shadow-sm transition-all duration-300 hover:-translate-y-0.5 hover:bg-[#855a4c] hover:shadow-lg active:translate-y-0 active:scale-[0.98] motion-reduce:transition-none"
            >
              <BookOpen className="size-4 transition-transform duration-300 group-hover:-rotate-3" />
              开始面试练习
            </Link>
            <Link
              to="/banks/certification"
              className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-[#82978f]/35 bg-white/65 px-4 text-sm font-bold text-[#536b63] backdrop-blur transition-all duration-300 hover:-translate-y-0.5 hover:border-[#758b83] hover:bg-[#758b83] hover:text-white hover:shadow-lg active:translate-y-0 active:scale-[0.98] motion-reduce:transition-none"
            >
              <GraduationCap className="size-4" />
              浏览认证题库
            </Link>
          </div>
        </div>
      </section>

      <div className="mt-7 grid gap-6 lg:grid-cols-2">
        <BankSection
          title="热门面试题库"
          icon={BookOpen}
          banks={data.interviewQuestionBankVOList.slice(0, 5)}
          moreTo="/banks/interview"
          tone="interview"
          onSelect={(bank) =>
            navigate(`/banks/interview/${bank.bankId}/practice`)
          }
        />
        <BankSection
          title="热门认证题库"
          icon={GraduationCap}
          banks={data.certificateQuestionBankVOList.slice(0, 5)}
          moreTo="/banks/certification"
          tone="certification"
          onSelect={setCertificateBank}
        />
      </div>

      <section className="relative mt-8 overflow-hidden rounded-3xl border border-[#c7d4d0] bg-[#e4ebe8]/80 p-4 shadow-[0_14px_38px_rgba(71,91,84,0.07)] sm:p-5">
        <span
          className="pointer-events-none absolute -right-12 -top-16 size-44 rounded-full border-[28px] border-white/30"
          aria-hidden="true"
        />
        <div className="relative">
          <SectionHeading
            title="最新 Hit"
            moreTo="/hits"
            headingTone="bg-[#778e86]"
          />
          {data.hotPostList.length ? (
            <div className="mt-4 grid gap-4 lg:grid-cols-2">
              {data.hotPostList.slice(0, 10).map((post, index) => (
                <HitCard
                  key={post.postId}
                  post={post}
                  compact
                  className="transition-all duration-300 hover:-translate-y-1 hover:shadow-xl motion-reduce:transition-none motion-reduce:hover:translate-y-0"
                  style={{
                    backgroundColor:
                      hitTones[index % hitTones.length].background,
                    borderColor: hitTones[index % hitTones.length].border,
                  }}
                />
              ))}
            </div>
          ) : (
            <Card className="mt-4 border-[#cad5d1] bg-white/65">
              <EmptyState
                title="还没有 Hit"
                description="第一条学习动态，或许就从你开始。"
              />
            </Card>
          )}
        </div>
      </section>

      <Dialog
        open={Boolean(certificateBank)}
        onOpenChange={(open) => {
          if (!open) setCertificateBank(null)
        }}
        title={certificateBank?.bankName || '选择答题模式'}
        description="练习模式逐题查看解析；考试模式会创建可恢复的限时场次。"
      >
        <div className="grid gap-3 sm:grid-cols-2">
          <button
            type="button"
            className="rounded-xl border border-line p-4 text-left transition hover:border-brand hover:bg-[#f8f4f0]"
            onClick={() =>
              navigate(
                `/banks/certification/${certificateBank?.bankId}/practice`,
              )
            }
          >
            <BookOpen className="size-5 text-accent" />
            <span className="mt-3 block font-bold">练习模式</span>
            <span className="mt-1 block text-xs leading-5 text-muted">
              逐题作答，提交后立即查看答案。
            </span>
          </button>
          <button
            type="button"
            disabled={examMutation.isPending}
            className="rounded-xl border border-line p-4 text-left transition hover:border-brand hover:bg-[#f8f4f0]"
            onClick={() => {
              if (certificateBank) examMutation.mutate(certificateBank.bankId)
            }}
          >
            <CheckCircle2 className="size-5 text-brand" />
            <span className="mt-3 block font-bold">考试模式</span>
            <span className="mt-1 block text-xs leading-5 text-muted">
              限时作答，统一交卷后查看成绩。
            </span>
          </button>
        </div>
        {examMutation.isError ? (
          <p className="mt-3 text-sm text-danger">
            考试场次创建失败，请稍后重试。
          </p>
        ) : null}
      </Dialog>
    </div>
  )
}

interface BankSectionProps {
  title: string
  icon: typeof BookOpen
  banks: HotQuestionBank[]
  moreTo: string
  tone: keyof typeof bankSectionTones
  onSelect: (bank: HotQuestionBank) => void
}

function BankSection({
  title,
  icon: Icon,
  banks,
  moreTo,
  tone,
  onSelect,
}: BankSectionProps) {
  const theme = bankSectionTones[tone]

  return (
    <section>
      <SectionHeading
        title={title}
        moreTo={moreTo}
        headingTone={theme.heading}
      />
      <Card
        className={cn(
          'mt-4 overflow-hidden shadow-[0_12px_30px_rgba(69,58,52,0.07)]',
          theme.card,
        )}
        style={{
          backgroundColor: theme.background,
          borderColor: theme.border,
        }}
      >
        {banks.length ? (
          <div className={cn('divide-y', theme.divider)}>
            {banks.map((bank) => (
              <button
                key={bank.bankId}
                type="button"
                className={cn(
                  'home-bank-row group relative flex w-full items-center gap-3 overflow-hidden px-4 py-4 text-left transition-all duration-300 active:scale-[0.99] motion-reduce:transition-none',
                  theme.row,
                )}
                onClick={() => onSelect(bank)}
              >
                <span
                  className={cn(
                    'absolute inset-y-2 left-0 w-1 origin-center scale-y-0 rounded-r-full transition-transform duration-300 group-hover:scale-y-100 group-focus-visible:scale-y-100 motion-reduce:transition-none',
                    theme.rail,
                  )}
                  aria-hidden="true"
                />
                <span
                  className={cn(
                    'flex size-10 shrink-0 items-center justify-center rounded-xl shadow-sm transition-all duration-300 group-hover:-rotate-6 group-hover:scale-110 motion-reduce:transition-none',
                    theme.icon,
                  )}
                >
                  <Icon className="size-5" />
                </span>
                <span className="min-w-0 flex-1">
                  <span
                    className={cn(
                      'block truncate text-sm font-bold transition-colors duration-300',
                      theme.title,
                    )}
                  >
                    {bank.bankName}
                  </span>
                  <span className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted">
                    <span>{bank.moduleName}</span>
                    <span className="inline-flex items-center gap-1">
                      <Users className="size-3.5" />
                      {formatCount(bank.completeCount)} 人完成
                    </span>
                  </span>
                </span>
                <span
                  className={cn(
                    'home-bank-accuracy shrink-0 whitespace-nowrap rounded-full border px-2.5 py-1.5 text-xs font-semibold transition-all duration-300 group-hover:scale-105 motion-reduce:transition-none',
                    theme.accuracy,
                  )}
                >
                  正确率{' '}
                  <strong className="font-extrabold">
                    {formatRate(bank.avgCorrectRate)}
                  </strong>
                </span>
              </button>
            ))}
          </div>
        ) : (
          <EmptyState title="暂无题库" />
        )}
      </Card>
    </section>
  )
}

function SectionHeading({
  title,
  moreTo,
  headingTone = 'bg-brand',
}: {
  title: string
  moreTo: string
  headingTone?: string
}) {
  return (
    <div className="flex items-center justify-between">
      <h2 className="flex items-center gap-2.5 text-lg font-extrabold tracking-tight">
        <span
          className={cn('size-2.5 rounded-full shadow-sm', headingTone)}
          aria-hidden="true"
        />
        {title}
      </h2>
      <Button asChild variant="ghost" size="sm">
        <Link
          to={moreTo}
          className="transition-all duration-300 hover:-translate-y-0.5"
        >
          查看更多
          <ArrowRight className="size-3.5" />
        </Link>
      </Button>
    </div>
  )
}
