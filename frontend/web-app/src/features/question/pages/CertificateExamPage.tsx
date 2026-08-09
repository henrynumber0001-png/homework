import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  AlertTriangle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock3,
} from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { toast } from 'sonner'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import {
  getCertificateExam,
  saveCertificateExamAnswer,
  submitCertificateExam,
} from '@/features/question/api'
import { QuestionNavigator } from '@/features/question/components/QuestionNavigator'
import type { BankFinish } from '@/features/question/types'
import { ExamSessionStatus, QuestionType } from '@/shared/constants/domain'
import { cn } from '@/shared/lib/cn'
import { formatCountdown, useCountdown } from '@/shared/hooks/useCountdown'
import { formatRate } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/Badge'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { Dialog } from '@/shared/ui/Dialog'
import { ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

export function CertificateExamPage() {
  const queryClient = useQueryClient()
  const { sessionId: rawSessionId } = useParams()
  const sessionId = Number(rawSessionId)
  const [searchParams, setSearchParams] = useSearchParams()
  const [answers, setAnswers] = useState<Record<number, string[]>>({})
  const [savingQuestionIds, setSavingQuestionIds] = useState<Set<number>>(
    new Set(),
  )
  const [confirmSubmit, setConfirmSubmit] = useState(false)
  const [result, setResult] = useState<BankFinish | null>(null)
  const autoSubmitted = useRef(false)

  const examQuery = useQuery({
    queryKey: ['certificate-exam', sessionId],
    queryFn: () => getCertificateExam(sessionId),
    enabled: Number.isFinite(sessionId),
    refetchOnWindowFocus: true,
  })
  const secondsLeft = useCountdown(examQuery.data?.expiresAt)

  useEffect(() => {
    if (!examQuery.data) return
    setAnswers(
      Object.fromEntries(
        examQuery.data.questions.map((question) => [
          question.questionId,
          question.chosenOptions || [],
        ]),
      ),
    )
  }, [examQuery.data])

  const submitMutation = useMutation({
    mutationFn: () => submitCertificateExam(sessionId),
    onSuccess: (finish) => {
      setResult(finish)
      setConfirmSubmit(false)
      void queryClient.invalidateQueries({
        queryKey: ['certificate-exam', sessionId],
      })
      void queryClient.invalidateQueries({ queryKey: ['home'] })
      void queryClient.invalidateQueries({ queryKey: ['user-center'] })
    },
  })

  useEffect(() => {
    if (
      !examQuery.data ||
      examQuery.data.status !== ExamSessionStatus.IN_PROGRESS ||
      secondsLeft > 0 ||
      autoSubmitted.current
    ) {
      return
    }
    autoSubmitted.current = true
    submitMutation.mutate()
  }, [examQuery.data, secondsLeft, submitMutation])

  if (examQuery.isLoading) {
    return (
      <div className="app-container py-8">
        <PageSkeleton />
      </div>
    )
  }

  if (examQuery.isError || !examQuery.data) {
    return (
      <div className="app-container py-8">
        <ErrorState onRetry={() => void examQuery.refetch()} />
      </div>
    )
  }

  const exam = examQuery.data

  if (result) {
    return <ExamResult result={result} bankId={exam.bankId} />
  }

  if (exam.status !== ExamSessionStatus.IN_PROGRESS) {
    return (
      <div className="reading-container py-10">
        <Card className="p-8 text-center">
          <CheckCircle2 className="mx-auto size-9 text-success" />
          <h1 className="mt-4 text-2xl font-extrabold">本场考试已经结束</h1>
          <p className="mt-2 text-sm text-muted">
            你可以进入答案回顾查看已保存的作答记录。
          </p>
          <Button asChild className="mt-6">
            <Link to={`/banks/certification/${exam.bankId}/review`}>
              查看答案回顾
            </Link>
          </Button>
        </Card>
      </div>
    )
  }

  const questions = exam.questions
  const requestedQuestionId = Number(searchParams.get('question'))
  const currentQuestion =
    questions.find((item) => item.questionId === requestedQuestionId) ||
    questions[0]
  const currentIndex = questions.findIndex(
    (item) => item.questionId === currentQuestion.questionId,
  )
  const selected = answers[currentQuestion.questionId] || []
  const answeredIds = new Set(
    Object.entries(answers)
      .filter(([, options]) => options.length > 0)
      .map(([id]) => Number(id)),
  )
  const saving = savingQuestionIds.has(currentQuestion.questionId)

  const selectQuestion = (questionId: number) => {
    setSearchParams({ question: String(questionId) })
  }

  const persistOptions = async (
    questionId: number,
    chosenOptions: string[],
  ) => {
    if (savingQuestionIds.has(questionId)) return
    setSavingQuestionIds((current) => new Set(current).add(questionId))
    try {
      await saveCertificateExamAnswer({
        sessionId,
        questionId,
        chosenOptions,
      })
    } catch {
      toast.error('答案保存失败，请重新选择')
      void examQuery.refetch()
    } finally {
      setSavingQuestionIds((current) => {
        const next = new Set(current)
        next.delete(questionId)
        return next
      })
    }
  }

  const toggleOption = (option: string) => {
    if (saving) return
    const next =
      currentQuestion.questionType === QuestionType.SINGLE_CHOICE
        ? [option]
        : selected.includes(option)
          ? selected.filter((item) => item !== option)
          : [...selected, option]
    setAnswers((current) => ({
      ...current,
      [currentQuestion.questionId]: next,
    }))
    void persistOptions(currentQuestion.questionId, next)
  }

  return (
    <div className="app-container py-6">
      <div className="sticky top-16 z-30 -mx-2 mb-5 flex items-center justify-between gap-3 border-b border-line bg-canvas/95 px-2 py-3 backdrop-blur">
        <div>
          <p className="text-xs font-bold text-brand">认证考试</p>
          <p className="mt-1 text-sm font-semibold">
            已答 {answeredIds.size} / {questions.length}
          </p>
        </div>
        <div
          className={cn(
            'flex items-center gap-2 rounded-xl px-3 py-2 font-mono text-sm font-bold',
            secondsLeft <= 300
              ? 'bg-danger-soft text-danger'
              : 'bg-brand-soft text-brand',
          )}
        >
          <Clock3 className="size-4" />
          {formatCountdown(secondsLeft)}
        </div>
        <Button onClick={() => setConfirmSubmit(true)}>提交试卷</Button>
      </div>

      <div className="grid gap-5 lg:grid-cols-[190px_minmax(0,1fr)]">
        <QuestionNavigator
          questionIds={questions.map((item) => item.questionId)}
          currentId={currentQuestion.questionId}
          answeredIds={answeredIds}
          onSelect={selectQuestion}
        />

        <Card className="p-5 sm:p-7">
          <div className="flex items-center justify-between gap-3">
            <Badge>
              {currentQuestion.questionType === QuestionType.MULTIPLE
                ? '多选题'
                : '单选题'}
            </Badge>
            {saving ? (
              <span className="text-xs text-muted">正在保存…</span>
            ) : (
              <span className="text-xs text-success">答案自动保存</span>
            )}
          </div>
          <h1 className="mt-5 text-xl font-bold leading-8">
            {currentQuestion.title}
          </h1>
          {currentQuestion.imageUrl ? (
            <img
              src={currentQuestion.imageUrl}
              alt="题目配图"
              className="mt-5 max-h-72 w-full rounded-xl border border-line object-contain"
            />
          ) : null}
          <div className="mt-6 space-y-3">
            {currentQuestion.options.map((option, index) => {
              const active = selected.includes(option)
              return (
                <button
                  key={option}
                  type="button"
                  disabled={saving}
                  className={cn(
                    'flex w-full items-start gap-3 rounded-xl border p-4 text-left text-sm leading-6 transition',
                    active
                      ? 'border-brand bg-brand-soft'
                      : 'border-line bg-white hover:border-brand',
                  )}
                  onClick={() => toggleOption(option)}
                >
                  <span
                    className={cn(
                      'flex size-6 shrink-0 items-center justify-center text-xs font-bold',
                      currentQuestion.questionType === QuestionType.MULTIPLE
                        ? 'rounded-md'
                        : 'rounded-full',
                      active
                        ? 'bg-brand text-white'
                        : 'border border-line text-muted',
                    )}
                  >
                    {String.fromCharCode(65 + index)}
                  </span>
                  {option}
                </button>
              )
            })}
          </div>
          <div className="mt-7 flex justify-between">
            <Button
              variant="ghost"
              disabled={currentIndex <= 0}
              onClick={() =>
                selectQuestion(questions[currentIndex - 1].questionId)
              }
            >
              <ChevronLeft className="size-4" />
              上一题
            </Button>
            <Button
              variant="secondary"
              disabled={currentIndex >= questions.length - 1}
              onClick={() =>
                selectQuestion(questions[currentIndex + 1].questionId)
              }
            >
              下一题
              <ChevronRight className="size-4" />
            </Button>
          </div>
        </Card>
      </div>

      <Dialog
        open={confirmSubmit}
        onOpenChange={setConfirmSubmit}
        title="确认提交试卷？"
        description={`还有 ${questions.length - answeredIds.size} 道题未作答。提交后不能继续修改答案。`}
        footer={
          <>
            <Button variant="ghost" onClick={() => setConfirmSubmit(false)}>
              继续检查
            </Button>
            <Button
              disabled={submitMutation.isPending}
              onClick={() => submitMutation.mutate()}
            >
              确认提交
            </Button>
          </>
        }
      >
        <div className="flex items-start gap-3 rounded-xl bg-warning-soft p-4 text-sm leading-6 text-warning">
          <AlertTriangle className="mt-0.5 size-5 shrink-0" />
          请确认所有需要作答的题目已经保存。
        </div>
        {submitMutation.isError ? (
          <p className="mt-3 text-sm text-danger">
            提交失败，请检查网络后重试。
          </p>
        ) : null}
      </Dialog>
    </div>
  )
}

function ExamResult({
  result,
  bankId,
}: {
  result: BankFinish
  bankId: number
}) {
  const count = result.questionCount
  return (
    <div className="reading-container py-10">
      <Card className="overflow-hidden">
        <div className="bg-success-soft p-8 text-center">
          <CheckCircle2 className="mx-auto size-10 text-success" />
          <p className="mt-4 text-sm font-semibold text-success">考试已提交</p>
          <h1 className="mt-1 text-4xl font-black text-ink">
            {formatRate(count.correctRate)}
          </h1>
          <p className="mt-2 text-sm text-muted">本场正确率</p>
        </div>
        <div className="grid grid-cols-3 divide-x divide-line border-t border-line py-5 text-center">
          <ResultStat label="总题数" value={count.totalCount} />
          <ResultStat label="已作答" value={count.answeredCount} />
          <ResultStat label="答对" value={count.correctCount ?? 0} />
        </div>
        <div className="flex justify-center p-6">
          <Button asChild>
            <Link to={`/banks/certification/${bankId}/review`}>
              查看答案回顾
            </Link>
          </Button>
        </div>
      </Card>
    </div>
  )
}

function ResultStat({ label, value }: { label: string; value: number }) {
  return (
    <div>
      <p className="text-xl font-extrabold">{value}</p>
      <p className="mt-1 text-xs text-muted">{label}</p>
    </div>
  )
}
