import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Bookmark,
  ChevronLeft,
  ChevronRight,
  StickyNote,
  Trash2,
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { toast } from 'sonner'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  finishQuestionBank,
  getInterviewQuestions,
  getInterviewRecord,
  clearQuestionBankRecord,
  saveQuestionNote,
  submitInterviewAnswer,
  updateQuestionFavorite,
} from '@/features/question/api'
import { AiChatDrawer } from '@/features/question/components/AiChatDialog'
import { ClearRecordDialog } from '@/features/question/components/ClearRecordDialog'
import { FinishBankDialog } from '@/features/question/components/FinishBankDialog'
import {
  InterviewAiPanel,
  InterviewReferenceAnswer,
} from '@/features/question/components/AnswerPanel'
import { QuestionNavigator } from '@/features/question/components/QuestionNavigator'
import type { InterviewAnswer } from '@/features/question/types'
import { ActionStatus, GroupType } from '@/shared/constants/domain'
import { cn } from '@/shared/lib/cn'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { Textarea } from '@/shared/ui/Input'
import { ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

export function InterviewPracticePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { bankId: rawBankId } = useParams()
  const bankId = Number(rawBankId)
  const [searchParams, setSearchParams] = useSearchParams()
  const [drafts, setDrafts] = useState<Record<number, string>>({})
  const [answers, setAnswers] = useState<Record<number, InterviewAnswer>>({})
  const [note, setNote] = useState('')
  const [aiOpen, setAiOpen] = useState(false)
  const [clearOpen, setClearOpen] = useState(false)
  const [finishOpen, setFinishOpen] = useState(false)

  const questionsQuery = useQuery({
    queryKey: ['interview-questions', bankId],
    queryFn: () => getInterviewQuestions(bankId),
    enabled: Number.isFinite(bankId),
  })
  const recordsQuery = useQuery({
    queryKey: ['interview-record', bankId],
    queryFn: () => getInterviewRecord(bankId),
    enabled: Number.isFinite(bankId),
  })

  useEffect(() => {
    if (!recordsQuery.data?.length) return
    setDrafts(
      Object.fromEntries(
        recordsQuery.data.map((record) => [
          record.questionId,
          record.content || '',
        ]),
      ),
    )
    setAnswers(
      Object.fromEntries(
        recordsQuery.data.map((record) => [
          record.questionId,
          {
            questionId: record.questionId,
            analysis: record.analysis,
            aiResult: record.aiResult,
            aiEvaluationEnabled: Boolean(record.aiResult),
            isFavorite: record.isFavorite,
            content: record.content,
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

  useEffect(() => {
    const hasUnsavedDraft = Object.entries(drafts).some(
      ([questionId, content]) => content.trim() && !answers[Number(questionId)],
    )
    if (!hasUnsavedDraft) return
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
    }
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [answers, drafts])

  const answerMutation = useMutation({
    mutationFn: submitInterviewAnswer,
    onSuccess: (answer) => {
      setAnswers((current) => ({ ...current, [answer.questionId]: answer }))
      void queryClient.invalidateQueries({
        queryKey: ['interview-record', bankId],
      })
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
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['interview-questions', bankId],
      })
      void queryClient.invalidateQueries({
        queryKey: ['interview-record', bankId],
      })
    },
  })
  const finishMutation = useMutation({
    mutationFn: () => finishQuestionBank(bankId, GroupType.INTERVIEW),
    onSuccess: (finish) => {
      setFinishOpen(false)
      void queryClient.invalidateQueries({ queryKey: ['home'] })
      void queryClient.invalidateQueries({ queryKey: ['user-center'] })
      navigate(`/banks/interview/${bankId}/review`, {
        state: { questionCount: finish.questionCount },
      })
    },
  })
  const clearMutation = useMutation({
    mutationFn: () => clearQuestionBankRecord(bankId, GroupType.INTERVIEW),
    onSuccess: () => {
      setDrafts({})
      setAnswers({})
      setNote('')
      setAiOpen(false)
      setClearOpen(false)
      const firstQuestion = questions[0]
      if (firstQuestion) {
        setSearchParams({ question: String(firstQuestion.questionId) })
      }
      queryClient.setQueryData(['interview-record', bankId], [])
      void queryClient.invalidateQueries({
        queryKey: ['question-review', 'interview', bankId],
      })
      void queryClient.invalidateQueries({ queryKey: ['home'] })
      void queryClient.invalidateQueries({ queryKey: ['user-center'] })
      toast.success('当前题库的答题记录已清空')
    },
  })

  const selectQuestion = (questionId: number) => {
    setSearchParams({ question: String(questionId) })
    setNote('')
    setAiOpen(false)
  }

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

  const currentAnswer = answers[currentQuestion.questionId]
  const favorite = currentAnswer?.isFavorite ?? currentQuestion.isFavorite

  return (
    <div
      className="app-container py-6"
      onPointerDown={() => {
        if (aiOpen) setAiOpen(false)
      }}
    >
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-xs font-bold text-brand">面试题库 · 作答中</p>
          <h1 className="mt-1 text-lg font-extrabold">
            第 {currentIndex + 1} / {questions.length} 题
          </h1>
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

      <div
        className={cn(
          'grid gap-5',
          currentAnswer
            ? 'lg:grid-cols-[180px_minmax(0,1fr)_340px]'
            : 'lg:grid-cols-[180px_minmax(0,1fr)]',
        )}
      >
        <aside className="space-y-3">
          <QuestionNavigator
            questionIds={questions.map((item) => item.questionId)}
            currentId={currentQuestion.questionId}
            answeredIds={answeredIds}
            onSelect={selectQuestion}
          />
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

        <div className="space-y-4">
          <Card className="p-5 sm:p-6">
            <div className="flex items-start justify-between gap-4">
              <h2 className="text-lg font-bold leading-8">
                {currentQuestion.title}
              </h2>
              <button
                type="button"
                aria-label={favorite ? '取消收藏' : '收藏题目'}
                className={cn(
                  'flex size-10 shrink-0 items-center justify-center rounded-xl border transition',
                  favorite
                    ? 'border-brand bg-brand-soft text-brand'
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
            <Textarea
              className="mt-5 min-h-52"
              placeholder="用自己的语言组织回答。可以先写结论，再补充原理、适用场景和取舍。"
              value={drafts[currentQuestion.questionId] || ''}
              onChange={(event) =>
                setDrafts((current) => ({
                  ...current,
                  [currentQuestion.questionId]: event.target.value,
                }))
              }
            />
            <div className="mt-4">
              <Button
                className="w-full"
                disabled={
                  answerMutation.isPending ||
                  !(drafts[currentQuestion.questionId] || '').trim()
                }
                onClick={() =>
                  answerMutation.mutate({
                    bankId,
                    questionId: currentQuestion.questionId,
                    content: drafts[currentQuestion.questionId].trim(),
                  })
                }
              >
                {answerMutation.isPending ? '正在提交…' : '提交回答'}
              </Button>
            </div>
          </Card>

          {currentAnswer ? (
            <InterviewReferenceAnswer answer={currentAnswer} />
          ) : null}
        </div>

        {currentAnswer ? (
          <aside className="space-y-4">
            <InterviewAiPanel
              answer={currentAnswer}
              onAskAi={() => setAiOpen(true)}
            />
            <Card className="p-5">
              <h3 className="flex items-center gap-2 font-bold">
                <StickyNote className="size-4 text-accent" />
                笔记本
              </h3>
              <Textarea
                className="mt-3 min-h-28"
                placeholder="记录容易忘记的概念、例子或自己的理解…"
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
          </aside>
        ) : null}
      </div>

      {currentAnswer ? (
        <AiChatDrawer
          open={aiOpen}
          onOpenChange={setAiOpen}
          bankId={bankId}
          questionId={currentQuestion.questionId}
          groupType={GroupType.INTERVIEW}
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
