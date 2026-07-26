import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Bookmark, ChevronLeft, ChevronRight } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  finishQuestionBank,
  getCertificateQuestions,
  submitCertificatePracticeAnswer,
  updateQuestionFavorite,
} from '@/features/question/api'
import { CertificateAnswerPanel } from '@/features/question/components/AnswerPanel'
import { QuestionNavigator } from '@/features/question/components/QuestionNavigator'
import type { CertificateAnswer } from '@/features/question/types'
import {
  ActionStatus,
  GroupType,
  QuestionType,
} from '@/shared/constants/domain'
import { cn } from '@/shared/lib/cn'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

export function CertificatePracticePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { bankId: rawBankId } = useParams()
  const bankId = Number(rawBankId)
  const [searchParams, setSearchParams] = useSearchParams()
  const [selections, setSelections] = useState<Record<number, string[]>>({})
  const [answers, setAnswers] = useState<Record<number, CertificateAnswer>>({})

  const questionsQuery = useQuery({
    queryKey: ['certificate-questions', bankId],
    queryFn: () => getCertificateQuestions(bankId),
  })
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
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['home'] })
      void queryClient.invalidateQueries({ queryKey: ['user-center'] })
      navigate(`/banks/certification/${bankId}/review`)
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
  }

  return (
    <div className="app-container py-6">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-xs font-bold text-brand">认证题库 · 练习模式</p>
          <h1 className="mt-1 text-lg font-extrabold">
            第 {currentIndex + 1} / {questions.length} 题
          </h1>
        </div>
        <Button
          variant="secondary"
          disabled={finishMutation.isPending}
          onClick={() => finishMutation.mutate()}
        >
          完成练习
        </Button>
      </div>

      <div className="grid gap-5 lg:grid-cols-[180px_minmax(0,1fr)_340px]">
        <QuestionNavigator
          questionIds={questions.map((item) => item.questionId)}
          currentId={currentQuestion.questionId}
          answeredIds={answeredIds}
          onSelect={selectQuestion}
        />

        <Card className="h-fit p-5 sm:p-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="mb-2 text-xs font-semibold text-accent">
                {currentQuestion.questionType === QuestionType.MULTIPLE
                  ? '多选题'
                  : '单选题'}
              </p>
              <h2 className="text-lg font-bold leading-8">
                {currentQuestion.title}
              </h2>
            </div>
            <button
              type="button"
              aria-label={favorite ? '取消收藏' : '收藏题目'}
              className={cn(
                'flex size-10 shrink-0 items-center justify-center rounded-xl border transition',
                favorite
                  ? 'border-brand bg-[#eee7e1] text-brand'
                  : 'border-line text-muted hover:border-brand',
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

          <div className="mt-5 space-y-3">
            {currentQuestion.options.map((option, index) => {
              const active = selected.includes(option)
              return (
                <button
                  key={option}
                  type="button"
                  disabled={locked}
                  className={cn(
                    'flex w-full items-start gap-3 rounded-xl border p-4 text-left text-sm leading-6 transition',
                    active
                      ? 'border-brand bg-[#f3eeea]'
                      : 'border-line bg-white hover:border-brand',
                    locked && 'cursor-default',
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

          <div className="mt-6 flex flex-wrap items-center justify-between gap-3">
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
            {locked ? (
              <Button
                disabled={currentIndex >= questions.length - 1}
                onClick={() =>
                  selectQuestion(questions[currentIndex + 1].questionId)
                }
              >
                下一题
                <ChevronRight className="size-4" />
              </Button>
            ) : (
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
                提交答案
              </Button>
            )}
          </div>
        </Card>

        <CertificateAnswerPanel answer={currentAnswer} />
      </div>
    </div>
  )
}
