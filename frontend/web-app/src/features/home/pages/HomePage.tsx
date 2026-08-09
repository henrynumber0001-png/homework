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
    card: 'border-[#bfd0e4]',
    background: '#e6eef8',
    border: '#bfd0e4',
    divider: 'divide-[#cad8e8]',
    row: 'hover:bg-[#d8e5f3] focus-visible:bg-[#d8e5f3] active:bg-[#cadbee]',
    icon: 'bg-[#2f69a4] text-white shadow-[#173f73]/20',
    title: 'group-hover:text-[#153b68] group-focus-visible:text-[#153b68]',
    accuracy:
      'border-[#abc3dc] bg-white/75 text-[#234f7d] group-hover:border-[#245d96] group-hover:bg-[#245d96] group-hover:text-white group-focus-visible:border-[#245d96] group-focus-visible:bg-[#245d96] group-focus-visible:text-white',
    rail: 'bg-[#245d96]',
    heading: 'bg-[#2f69a4]',
  },
  certification: {
    card: 'border-[#cbd5e8]',
    background: '#edf1fa',
    border: '#cbd5e8',
    divider: 'divide-[#d5ddec]',
    row: 'hover:bg-[#dce4f2] focus-visible:bg-[#dce4f2] active:bg-[#ced9e9]',
    icon: 'bg-[#536fa4] text-white shadow-[#314b7c]/20',
    title: 'group-hover:text-[#2d4777] group-focus-visible:text-[#2d4777]',
    accuracy:
      'border-[#b8c8e0] bg-white/75 text-[#405985] group-hover:border-[#405d91] group-hover:bg-[#405d91] group-hover:text-white group-focus-visible:border-[#405d91] group-focus-visible:bg-[#405d91] group-focus-visible:text-white',
    rail: 'bg-[#405d91]',
    heading: 'bg-[#536fa4]',
  },
} as const

const hitTones = [
  { background: '#f6f9fd', border: '#d5e1ef' },
  { background: '#f2f7fc', border: '#ccdcec' },
  { background: '#f5f8fc', border: '#d8e2ef' },
  { background: '#f3f6fc', border: '#ced9ec' },
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
        className="pointer-events-none absolute -right-28 top-72 -z-10 hidden size-72 rounded-full bg-[#cbdcf1]/55 blur-3xl sm:block"
        aria-hidden="true"
      />
      <span
        className="pointer-events-none absolute -left-32 top-[38rem] -z-10 hidden size-64 rounded-full bg-[#d9e7f6]/55 blur-3xl sm:block"
        aria-hidden="true"
      />

      <section className="group relative overflow-hidden rounded-3xl border border-[#284f7c] bg-[linear-gradient(125deg,#102f59_0%,#174878_55%,#276b9e_100%)] px-6 py-7 text-white shadow-[0_18px_46px_rgba(13,45,86,0.18)] transition-[transform,box-shadow] duration-300 ease-[var(--ease-out-ui)] hover:-translate-y-0.5 hover:shadow-[0_24px_58px_rgba(13,45,86,0.24)] motion-reduce:transition-none motion-reduce:hover:translate-y-0 sm:px-8 sm:py-9">
        <div className="absolute -right-10 -top-20 size-64 rounded-full border-[34px] border-white/25 transition-transform duration-700 group-hover:rotate-12 group-hover:scale-110 motion-reduce:transition-none" />
        <div className="absolute -bottom-20 right-44 hidden size-44 rounded-full bg-[#72b7e8]/20 blur-sm transition-transform duration-300 group-hover:-translate-x-4 group-hover:-translate-y-2 lg:block" />
        <div className="relative max-w-2xl">
          <p className="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/10 px-3 py-1.5 text-xs font-bold text-[#dbeafe] shadow-sm backdrop-blur">
            <Sparkles className="size-3.5 text-[#7ec8ff]" />
            今天也向前一点
          </p>
          <h1 className="mt-2 text-2xl font-extrabold tracking-tight sm:text-3xl">
            {currentUser.data?.displayName
              ? `${currentUser.data.displayName}，欢迎回来`
              : '欢迎回到 HomeWork'}
          </h1>
          <p className="mt-3 max-w-xl text-sm leading-6 text-blue-100/80 sm:text-base">
            从一道题开始，或者看看大家最近在讨论什么。
          </p>
          <div className="mt-5 flex flex-wrap gap-3">
            <Link
              to="/banks/interview"
              className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-white px-4 text-sm font-bold text-brand-dark shadow-sm transition-[background-color,transform,box-shadow] duration-150 ease-[var(--ease-out-ui)] hover:-translate-y-0.5 hover:bg-[#eef6ff] hover:shadow-lg active:translate-y-0 active:scale-[0.97] motion-reduce:transition-none"
            >
              <BookOpen className="size-4 transition-transform duration-300 group-hover:-rotate-3" />
              开始面试练习
            </Link>
            <Link
              to="/banks/certification"
              className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-white/25 bg-white/10 px-4 text-sm font-bold text-white backdrop-blur transition-[background-color,border-color,transform,box-shadow] duration-150 ease-[var(--ease-out-ui)] hover:-translate-y-0.5 hover:border-white/45 hover:bg-white/18 hover:shadow-lg active:translate-y-0 active:scale-[0.97] motion-reduce:transition-none"
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

      <section className="relative mt-8 overflow-hidden rounded-3xl border border-[#c6d8ea] bg-[#eaf2fa]/85 p-4 shadow-[0_14px_38px_rgba(15,47,82,0.07)] sm:p-5">
        <span
          className="pointer-events-none absolute -right-12 -top-16 size-44 rounded-full border-[28px] border-white/30"
          aria-hidden="true"
        />
        <div className="relative">
          <SectionHeading
            title="最新 Hit"
            moreTo="/hits"
            headingTone="bg-[#2b6097]"
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
            <Card className="mt-4 border-[#ccdceb] bg-white/70">
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
            className="rounded-xl border border-line p-4 text-left transition-colors hover:border-brand hover:bg-brand-soft"
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
            className="rounded-xl border border-line p-4 text-left transition-colors hover:border-brand hover:bg-brand-soft"
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
          'mt-4 overflow-hidden shadow-[0_12px_30px_rgba(15,31,61,0.08)]',
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
