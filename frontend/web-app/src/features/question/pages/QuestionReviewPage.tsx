import { useQuery } from '@tanstack/react-query'
import { CheckCircle2, MessageSquareText, XCircle } from 'lucide-react'
import { useParams } from 'react-router-dom'
import {
  getCertificateReview,
  getInterviewReview,
} from '@/features/question/api'
import type {
  CertificateReview,
  InterviewReview,
} from '@/features/question/types'
import { Badge } from '@/shared/ui/Badge'
import { Card } from '@/shared/ui/Card'
import { EmptyState, ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

export function QuestionReviewPage() {
  const { groupType, bankId: rawBankId } = useParams()
  const bankId = Number(rawBankId)
  const interview = groupType === 'interview'
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

  return (
    <div className="reading-container py-8">
      <header>
        <p className="text-sm font-semibold text-brand">学习记录</p>
        <h1 className="mt-1 text-3xl font-extrabold">答案回顾</h1>
        <p className="mt-2 text-sm text-muted">
          回看自己的作答、正确答案与解析。
        </p>
      </header>

      {records.length ? (
        <div className="mt-7 space-y-5">
          {records.map((record, index) => (
            <Card key={record.questionId} className="p-5 sm:p-6">
              <div className="flex items-start gap-3">
                <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-[#eee7e1] text-xs font-bold text-brand">
                  {index + 1}
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="font-bold leading-7">{record.title}</h2>
                    {'isCorrect' in record && record.isCorrect != null ? (
                      record.isCorrect ? (
                        <Badge className="border-[#add0c3] bg-[#f0f7f4] text-success">
                          <CheckCircle2 className="mr-1 size-3.5" />
                          正确
                        </Badge>
                      ) : (
                        <Badge className="border-[#e3b9b9] bg-[#fbf0f0] text-danger">
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
                    <div className="mt-4 rounded-xl bg-[#edf3f1] p-4">
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
