import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ArrowLeft,
  Bookmark,
  Check,
  ChevronLeft,
  ChevronRight,
  StickyNote,
  Trash2,
  X,
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { toast } from 'sonner'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  clearQuestionBankRecord,
  finishQuestionBank,
  getCertificateQuestions,
  getCertificateRecord,
  saveQuestionNote,
  submitCertificatePracticeAnswer,
  updateQuestionFavorite,
} from '@/features/question/api'
import { AiChatDrawer } from '@/features/question/components/AiChatDialog'
import { ClearRecordDialog } from '@/features/question/components/ClearRecordDialog'
import { FinishBankDialog } from '@/features/question/components/FinishBankDialog'
import {
  CertificateAnswerPanel,
  CertificateResultBanner,
} from '@/features/question/components/AnswerPanel'
import type {
  CertificateAnswer,
  CertificateQuestion,
} from '@/features/question/types'
import {
  ActionStatus,
  GroupType,
  QuestionType,
} from '@/shared/constants/domain'
import { cn } from '@/shared/lib/cn'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'
import { Textarea } from '@/shared/ui/Input'

export function CertificatePracticePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { bankId: rawBankId } = useParams()
  const bankId = Number(rawBankId)
  const [searchParams, setSearchParams] = useSearchParams()
  const [selections, setSelections] = useState<Record<number, string[]>>({})
  const [answers, setAnswers] = useState<Record<number, CertificateAnswer>>({})
  const [note, setNote] = useState('')
  const [aiOpen, setAiOpen] = useState(false)
  const [clearOpen, setClearOpen] = useState(false)
  const [finishOpen, setFinishOpen] = useState(false)

  const questionsQuery = useQuery({
    queryKey: ['certificate-questions', bankId],
    queryFn: () => getCertificateQuestions(bankId),
  })
  const recordsQuery = useQuery({
    queryKey: ['certificate-record', bankId],
    queryFn: () => getCertificateRecord(bankId),
    enabled: Number.isFinite(bankId),
  })

  useEffect(() => {
    if (!recordsQuery.data?.length) return
    setSelections(
      Object.fromEntries(
        recordsQuery.data.map((record) => [
          record.questionId,
          record.chosenOptions || [],
        ]),
      ),
    )
    setAnswers(
      Object.fromEntries(
        recordsQuery.data.map((record) => [
          record.questionId,
          {
            questionId: record.questionId,
            correctAnswer: record.correctAnswer,
            analysis: record.analysis,
            correct: record.isCorrect,
            isFavorite: record.isFavorite,
          },
        ]),
      ),
    )
  }, [recordsQuery.data])

  const questions = questionsQuery.data || []
  const requestedQuestionId = Number(searchParams.get('question'))
  const currentQuestion =
    questions.find((item) => item.questionId === requestedQuestionId) ||
    questions[0]
  const currentIndex = currentQuestion
    ? questions.findIndex(
        (item) => item.questionId === currentQuestion.questionId,
      )
    : -1
  const answeredIds = useMemo(
    () => new Set(Object.keys(answers).map(Number)),
    [answers],
  )

  const answerMutation = useMutation({
    mutationFn: submitCertificatePracticeAnswer,
    onSuccess: (answer) => {
      setAnswers((current) => ({ ...current, [answer.questionId]: answer }))
    },
  })
  const noteMutation = useMutation({
    mutationFn: saveQuestionNote,
    onSuccess: () => {
      setNote('')
      toast.success('笔记已保存')
    },
  })
  const favoriteMutation = useMutation({
    mutationFn: ({
      questionId,
      active,
    }: {
      questionId: number
      active: boolean
    }) =>
      updateQuestionFavorite(
        bankId,
        questionId,
        active ? ActionStatus.ACTIVATE : ActionStatus.DEACTIVATE,
      ),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ['certificate-questions', bankId],
      }),
  })
  const finishMutation = useMutation({
    mutationFn: () => finishQuestionBank(bankId, GroupType.CERTIFICATION),
    onSuccess: (finish) => {
      setFinishOpen(false)
      void queryClient.invalidateQueries({ queryKey: ['home'] })
      void queryClient.invalidateQueries({ queryKey: ['user-center'] })
      navigate(`/banks/certification/${bankId}/review`, {
        state: { questionCount: finish.questionCount },
      })
    },
  })
  const clearMutation = useMutation({
    mutationFn: () => clearQuestionBankRecord(bankId, GroupType.CERTIFICATION),
    onSuccess: () => {
      setSelections({})
      setAnswers({})
      setNote('')
      setAiOpen(false)
      setClearOpen(false)
      const firstQuestion = questions[0]
      if (firstQuestion) {
        setSearchParams({ question: String(firstQuestion.questionId) })
      }
      queryClient.setQueryData(['certificate-record', bankId], [])
      void queryClient.invalidateQueries({
        queryKey: ['question-review', 'certification', bankId],
      })
      void queryClient.invalidateQueries({ queryKey: ['home'] })
      void queryClient.invalidateQueries({ queryKey: ['user-center'] })
      toast.success('当前题库的答题记录已清空')
    },
  })

  if (questionsQuery.isLoading) {
    return (
      <div className="app-container py-8">
        <PageSkeleton />
      </div>
    )
  }

  if (questionsQuery.isError || !currentQuestion) {
    return (
      <div className="app-container py-8">
        <ErrorState
          message={currentQuestion ? undefined : '该题库暂无可用题目'}
          onRetry={() => void questionsQuery.refetch()}
        />
      </div>
    )
  }

  const selected = selections[currentQuestion.questionId] || []
  const currentAnswer = answers[currentQuestion.questionId]
  const locked = Boolean(currentAnswer)
  const favorite = currentAnswer?.isFavorite ?? currentQuestion.isFavorite

  const toggleOption = (option: string) => {
    if (locked) return
    setSelections((current) => {
      const existing = current[currentQuestion.questionId] || []
      const next =
        currentQuestion.questionType === QuestionType.SINGLE_CHOICE
          ? [option]
          : existing.includes(option)
            ? existing.filter((item) => item !== option)
            : [...existing, option]
      return { ...current, [currentQuestion.questionId]: next }
    })
  }

  const selectQuestion = (questionId: number) => {
    setSearchParams({ question: String(questionId) })
    setNote('')
    setAiOpen(false)
  }

  return (
    <div
      className="app-container py-6"
      onPointerDown={() => {
        if (aiOpen) setAiOpen(false)
      }}
    >
      <div className="grid items-start gap-6 lg:grid-cols-[220px_minmax(0,1fr)]">
        <aside className="space-y-4 lg:sticky lg:top-20">
          <button
            type="button"
            className="inline-flex min-h-10 items-center gap-2 text-sm font-bold text-muted transition-colors hover:text-brand"
            onClick={() => navigate('/banks/certification')}
          >
            <ArrowLeft className="size-4" />
            返回题库
          </button>

          <Card className="overflow-hidden">
            <div className="border-b border-line px-4 py-4">
              <p className="text-xs font-bold text-accent">练习模式</p>
              <p className="mt-1 text-sm font-extrabold text-ink">认证题库</p>
              <p className="mt-1 text-xs text-muted">
                已完成 {answeredIds.size} / {questions.length} 题
              </p>
            </div>
            <CertificateQuestionNavigator
              questions={questions}
              currentId={currentQuestion.questionId}
              answers={answers}
              onSelect={selectQuestion}
            />
          </Card>
          <Button
            variant="ghost"
            size="sm"
            className="w-full justify-start text-danger hover:bg-danger-soft hover:text-danger"
            disabled={clearMutation.isPending}
            onClick={() => setClearOpen(true)}
          >
            <Trash2 className="size-4" />
            清空记录
          </Button>
        </aside>

        <main className="mx-auto w-full max-w-4xl">
          <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-3 text-sm">
              <span className="rounded-full bg-brand-soft px-3 py-1 text-xs font-bold text-brand">
                {currentQuestion.questionType === QuestionType.MULTIPLE
                  ? '多选题'
                  : '单选题'}
              </span>
              <span className="font-semibold text-muted">
                第 {currentIndex + 1} / {questions.length} 题
              </span>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <Button
                variant="secondary"
                size="sm"
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
                size="sm"
                disabled={currentIndex >= questions.length - 1}
                onClick={() =>
                  selectQuestion(questions[currentIndex + 1].questionId)
                }
              >
                下一题
                <ChevronRight className="size-4" />
              </Button>
              <Button
                variant="secondary"
                disabled={finishMutation.isPending}
                onClick={() => setFinishOpen(true)}
              >
                完成题库
              </Button>
            </div>
          </div>

          <Card className="p-5 sm:p-7">
            <div className="flex items-start justify-between gap-4">
              <h1 className="text-xl font-extrabold leading-8 text-ink">
                {currentQuestion.title}
              </h1>
              <button
                type="button"
                aria-label={favorite ? '取消收藏' : '收藏题目'}
                className={cn(
                  'flex size-10 shrink-0 items-center justify-center rounded-xl border transition-colors',
                  favorite
                    ? 'border-brand bg-brand-soft text-brand'
                    : 'border-line text-muted hover:border-brand hover:text-brand',
                )}
                disabled={favoriteMutation.isPending}
                onClick={() =>
                  favoriteMutation.mutate({
                    questionId: currentQuestion.questionId,
                    active: !favorite,
                  })
                }
              >
                <Bookmark
                  className={favorite ? 'size-4 fill-current' : 'size-4'}
                />
              </button>
            </div>

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
                const correct = Boolean(
                  currentAnswer?.correctAnswer.includes(option),
                )
                const chosenWrong = locked && active && !correct
                return (
                  <button
                    key={option}
                    type="button"
                    disabled={locked}
                    className={cn(
                      'flex min-h-16 w-full items-center gap-4 rounded-2xl border px-4 py-3.5 text-left text-sm font-semibold leading-6 transition-colors',
                      !locked && active && 'border-brand bg-brand-soft',
                      !locked &&
                        !active &&
                        'border-line bg-white hover:border-brand/70 hover:bg-brand-soft/40',
                      locked && correct && 'border-[#7fb3a8] bg-success-soft',
                      chosenWrong && 'border-[#d9949e] bg-danger-soft',
                      locked &&
                        !correct &&
                        !chosenWrong &&
                        'cursor-default border-line bg-surface-muted text-muted',
                    )}
                    onClick={() => toggleOption(option)}
                  >
                    <span
                      className={cn(
                        'flex size-8 shrink-0 items-center justify-center text-xs font-extrabold',
                        currentQuestion.questionType === QuestionType.MULTIPLE
                          ? 'rounded-lg'
                          : 'rounded-full',
                        !locked && active && 'bg-brand text-white',
                        !locked && !active && 'border border-line text-muted',
                        locked && correct && 'bg-success text-white',
                        chosenWrong && 'bg-danger text-white',
                        locked &&
                          !correct &&
                          !chosenWrong &&
                          'border border-line text-muted',
                      )}
                    >
                      {locked && correct ? (
                        <Check className="size-4" />
                      ) : chosenWrong ? (
                        <X className="size-4" />
                      ) : (
                        String.fromCharCode(65 + index)
                      )}
                    </span>
                    <span>
                      {String.fromCharCode(65 + index)}. {option}
                    </span>
                  </button>
                )
              })}
            </div>

            {!locked ? (
              <div className="mt-6 flex justify-end">
                <Button
                  disabled={!selected.length || answerMutation.isPending}
                  onClick={() =>
                    answerMutation.mutate({
                      bankId,
                      questionId: currentQuestion.questionId,
                      questionType: currentQuestion.questionType,
                      chosenOptions: selected,
                    })
                  }
                >
                  {answerMutation.isPending ? '正在提交…' : '提交答案'}
                </Button>
              </div>
            ) : null}
          </Card>

          {currentAnswer ? (
            <div className="mt-5 space-y-4">
              <CertificateResultBanner answer={currentAnswer} />
              <CertificateAnswerPanel
                answer={currentAnswer}
                onAskAi={() => setAiOpen(true)}
              />
              <Card className="p-5 sm:p-6">
                <h2 className="flex items-center gap-2 font-bold">
                  <StickyNote className="size-4 text-accent" />
                  我的笔记
                </h2>
                <Textarea
                  className="mt-3 min-h-28"
                  placeholder="写下你的学习笔记…"
                  value={note}
                  onChange={(event) => setNote(event.target.value)}
                />
                <div className="mt-3 flex justify-end">
                  <Button
                    variant="secondary"
                    size="sm"
                    disabled={!note.trim() || noteMutation.isPending}
                    onClick={() =>
                      noteMutation.mutate({
                        bankId,
                        questionId: currentQuestion.questionId,
                        noteContent: note.trim(),
                      })
                    }
                  >
                    保存笔记
                  </Button>
                </div>
              </Card>
            </div>
          ) : null}
        </main>
      </div>

      {currentAnswer ? (
        <AiChatDrawer
          open={aiOpen}
          onOpenChange={setAiOpen}
          bankId={bankId}
          questionId={currentQuestion.questionId}
          groupType={GroupType.CERTIFICATION}
        />
      ) : null}

      <ClearRecordDialog
        open={clearOpen}
        onOpenChange={setClearOpen}
        pending={clearMutation.isPending}
        error={clearMutation.isError}
        onConfirm={() => clearMutation.mutate()}
      />
      <FinishBankDialog
        open={finishOpen}
        onOpenChange={setFinishOpen}
        pending={finishMutation.isPending}
        error={finishMutation.isError}
        answeredCount={answeredIds.size}
        totalCount={questions.length}
        onConfirm={() => finishMutation.mutate()}
      />
    </div>
  )
}

function CertificateQuestionNavigator({
  questions,
  currentId,
  answers,
  onSelect,
}: {
  questions: CertificateQuestion[]
  currentId: number
  answers: Record<number, CertificateAnswer>
  onSelect: (questionId: number) => void
}) {
  const groups = [
    {
      type: QuestionType.SINGLE_CHOICE,
      label: '单选题',
    },
    {
      type: QuestionType.MULTIPLE,
      label: '多选题',
    },
  ]

  return (
    <nav className="space-y-5 p-4" aria-label="认证题目导航">
      {groups.map((group) => {
        const groupQuestions = questions
          .map((question, index) => ({ question, index }))
          .filter(({ question }) => question.questionType === group.type)
        if (!groupQuestions.length) return null
        return (
          <section key={group.type}>
            <h2 className="mb-2 text-xs font-bold text-muted">
              {group.label}（{groupQuestions.length}）
            </h2>
            <div className="flex flex-wrap gap-2">
              {groupQuestions.map(({ question, index }) => {
                const active = question.questionId === currentId
                const answer = answers[question.questionId]
                const answered = Boolean(answer)
                const answerLabel = !answer
                  ? '未作答'
                  : answer.correct
                    ? '回答正确'
                    : '回答错误'
                return (
                  <button
                    key={question.questionId}
                    type="button"
                    aria-current={active ? 'step' : undefined}
                    aria-label={`第${index + 1}题，${answerLabel}`}
                    className={cn(
                      'flex size-10 items-center justify-center rounded-xl border text-xs font-extrabold transition-colors',
                      answer?.correct &&
                        'border-[#a9c4bb] bg-success-soft text-success',
                      answer &&
                        !answer.correct &&
                        'border-[#d9a0a8] bg-danger-soft text-danger',
                      !answered &&
                        'border-line bg-white text-ink hover:border-brand hover:text-brand',
                      active && 'ring-2 ring-brand/20',
                    )}
                    onClick={() => onSelect(question.questionId)}
                  >
                    {index + 1}
                  </button>
                )
              })}
            </div>
          </section>
        )
      })}
    </nav>
  )
}
