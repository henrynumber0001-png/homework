import { useQuery } from '@tanstack/react-query'
import {
  ArrowLeft,
  CheckCircle2,
  MessageSquareText,
  XCircle,
} from 'lucide-react'
import { Link, useLocation, useParams } from 'react-router-dom'
import {
  getCertificateReview,
  getInterviewReview,
} from '@/features/question/api'
import type {
  CertificateReview,
  InterviewReview,
  QuestionCount,
} from '@/features/question/types'
import { formatRate } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/Badge'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { EmptyState, ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

export function QuestionReviewPage() {
  const { groupType, bankId: rawBankId } = useParams()
  const location = useLocation()
  const bankId = Number(rawBankId)
  const interview = groupType === 'interview'
  const questionCount = (
    location.state as { questionCount?: QuestionCount } | null
  )?.questionCount
  const reviewQuery = useQuery<InterviewReview[] | CertificateReview[]>({
    queryKey: ['question-review', groupType, bankId],
    queryFn: () =>
      interview ? getInterviewReview(bankId) : getCertificateReview(bankId),
  })

  if (reviewQuery.isLoading) {
    return (
      <div className="reading-container py-8">
        <PageSkeleton />
      </div>
    )
  }

  if (reviewQuery.isError) {
    return (
      <div className="reading-container py-8">
        <ErrorState onRetry={() => void reviewQuery.refetch()} />
      </div>
    )
  }

  const records = reviewQuery.data || []
  const bankListPath = interview ? '/banks/interview' : '/banks/certification'
  const bankListLabel = interview ? '返回面试题库' : '返回认证题库'

  return (
    <div className="reading-container py-8">
      <Button asChild variant="ghost" className="mb-5 -ml-3">
        <Link to={bankListPath}>
          <ArrowLeft className="size-4" />
          {bankListLabel}
        </Link>
      </Button>
      <header>
        <p className="text-sm font-semibold text-brand">学习记录</p>
        <h1 className="mt-1 text-3xl font-extrabold">答案回顾</h1>
        <p className="mt-2 text-sm text-muted">
          回看自己的作答、正确答案与解析。
        </p>
      </header>

      {questionCount ? (
        <ReviewSummary interview={interview} count={questionCount} />
      ) : null}

      {records.length ? (
        <div className="mt-7 space-y-5">
          {records.map((record, index) => (
            <Card key={record.questionId} className="p-5 sm:p-6">
              <div className="flex items-start gap-3">
                <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-brand-soft text-xs font-bold text-brand">
                  {index + 1}
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="font-bold leading-7">{record.title}</h2>
                    {'isCorrect' in record && record.isCorrect != null ? (
                      record.isCorrect ? (
                        <Badge className="border-[#add0c3] bg-success-soft text-success">
                          <CheckCircle2 className="mr-1 size-3.5" />
                          正确
                        </Badge>
                      ) : (
                        <Badge className="border-[#e3b9b9] bg-danger-soft text-danger">
                          <XCircle className="mr-1 size-3.5" />
                          需复习
                        </Badge>
                      )
                    ) : null}
                  </div>

                  {interview && 'content' in record ? (
                    <ReviewBlock title="我的回答">
                      {record.content || '未作答'}
                    </ReviewBlock>
                  ) : null}
                  {!interview && 'chosenOptions' in record ? (
                    <>
                      <ReviewBlock title="我的选择">
                        {record.chosenOptions?.join('、') || '未作答'}
                      </ReviewBlock>
                      <ReviewBlock title="正确选项">
                        {record.correctAnswer.join('、')}
                      </ReviewBlock>
                    </>
                  ) : null}
                  <ReviewBlock title="答案解析">{record.analysis}</ReviewBlock>

                  {interview &&
                  'aiResult' in record &&
                  record.aiResult?.summary ? (
                    <div className="mt-4 rounded-xl bg-accent-soft p-4">
                      <p className="flex items-center gap-2 text-xs font-bold text-accent">
                        <MessageSquareText className="size-4" />
                        AI 反馈摘要
                      </p>
                      <p className="mt-2 text-sm leading-6 text-muted">
                        {record.aiResult.summary}
                      </p>
                    </div>
                  ) : null}
                </div>
              </div>
            </Card>
          ))}
        </div>
      ) : (
        <Card className="mt-7">
          <EmptyState
            title="暂无答题记录"
            description="完成题库后，可以在这里回顾答案。"
          />
        </Card>
      )}
    </div>
  )
}

function ReviewSummary({
  interview,
  count,
}: {
  interview: boolean
  count: QuestionCount
}) {
  return (
    <Card className="mt-7 overflow-hidden">
      <div className="flex flex-wrap items-end justify-between gap-4 bg-brand-soft px-5 py-6 sm:px-7">
        <div>
          <p className="text-sm font-semibold text-brand">本次完成结果</p>
          <h2 className="mt-1 text-lg font-extrabold text-ink">
            {interview ? '面试题平均正确率' : '认证题库正确率'}
          </h2>
        </div>
        <p className="text-4xl font-black text-brand">
          {formatRate(count.correctRate)}
        </p>
      </div>
      <div
        className={
          interview
            ? 'grid grid-cols-2 divide-x divide-line border-t border-line py-5 text-center'
            : 'grid grid-cols-3 divide-x divide-line border-t border-line py-5 text-center'
        }
      >
        <ReviewStat label="总题数" value={count.totalCount} />
        <ReviewStat label="已作答" value={count.answeredCount} />
        {interview ? null : (
          <ReviewStat label="答对" value={count.correctCount ?? 0} />
        )}
      </div>
    </Card>
  )
}

function ReviewStat({ label, value }: { label: string; value: number }) {
  return (
    <div>
      <p className="text-xl font-extrabold text-ink">{value}</p>
      <p className="mt-1 text-xs text-muted">{label}</p>
    </div>
  )
}

function ReviewBlock({
  title,
  children,
}: {
  title: string
  children: React.ReactNode
}) {
  return (
    <div className="mt-4">
      <p className="text-xs font-bold text-muted">{title}</p>
      <p className="mt-1.5 whitespace-pre-wrap text-sm leading-7 text-ink">
        {children}
      </p>
    </div>
  )
}
