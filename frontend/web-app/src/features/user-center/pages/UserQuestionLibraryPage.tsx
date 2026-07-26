import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, ChevronRight, LibraryBig } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  getUserQuestionBanks,
  getUserQuestionDetail,
  getUserQuestionList,
} from '@/features/user-center/api'
import type { LibraryKind } from '@/features/user-center/types'
import { GroupType } from '@/shared/constants/domain'
import { cn } from '@/shared/lib/cn'
import { Badge } from '@/shared/ui/Badge'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { EmptyState, ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

const copy: Record<
  LibraryKind,
  { title: string; description: string; countKey: string }
> = {
  wrong: {
    title: '错题本',
    description: '回到没有掌握的地方，再做一次有针对性的复习。',
    countKey: 'wrongQuestionCount',
  },
  favorite: {
    title: '题目收藏',
    description: '集中查看你主动保存的重点题目。',
    countKey: 'favoriteQuestionCount',
  },
  note: {
    title: '我的笔记',
    description: '按题库回顾答题时留下的笔记。',
    countKey: 'noteCount',
  },
}

export function UserQuestionLibraryPage({ kind }: { kind: LibraryKind }) {
  const [searchParams, setSearchParams] = useSearchParams()
  const groupType =
    Number(searchParams.get('groupType')) === GroupType.CERTIFICATION
      ? GroupType.CERTIFICATION
      : GroupType.INTERVIEW
  const bankId = Number(searchParams.get('bankId')) || null
  const questionId = Number(searchParams.get('questionId')) || null
  const pageCopy = copy[kind]

  const banksQuery = useQuery({
    queryKey: ['user-library', kind, 'banks', groupType],
    queryFn: () => getUserQuestionBanks(kind, groupType),
  })
  const questionsQuery = useQuery({
    queryKey: ['user-library', kind, 'questions', bankId],
    queryFn: () => getUserQuestionList(kind, bankId!),
    enabled: Boolean(bankId),
  })
  const detailQuery = useQuery({
    queryKey: ['user-library', kind, 'detail', bankId, questionId],
    queryFn: () => getUserQuestionDetail(kind, bankId!, questionId!),
    enabled: Boolean(bankId && questionId),
  })

  const updateParams = (
    next: Partial<{
      groupType: number
      bankId: number | null
      questionId: number | null
    }>,
  ) => {
    const current = {
      groupType,
      bankId,
      questionId,
      ...next,
    }
    const params = new URLSearchParams()
    params.set('groupType', String(current.groupType))
    if (current.bankId) params.set('bankId', String(current.bankId))
    if (current.questionId) params.set('questionId', String(current.questionId))
    setSearchParams(params)
  }

  return (
    <div className="app-container py-8">
      <Button asChild variant="ghost" size="sm">
        <Link to="/me">
          <ArrowLeft className="size-4" />
          返回个人中心
        </Link>
      </Button>
      <header className="mt-4">
        <p className="text-sm font-semibold text-brand">个人学习资料</p>
        <h1 className="mt-1 text-3xl font-extrabold">{pageCopy.title}</h1>
        <p className="mt-2 text-sm text-muted">{pageCopy.description}</p>
      </header>

      <div className="mt-6 flex w-fit rounded-xl border border-line bg-surface p-1">
        <GroupButton
          active={groupType === GroupType.INTERVIEW}
          label="面试题库"
          onClick={() =>
            updateParams({
              groupType: GroupType.INTERVIEW,
              bankId: null,
              questionId: null,
            })
          }
        />
        <GroupButton
          active={groupType === GroupType.CERTIFICATION}
          label="认证题库"
          onClick={() =>
            updateParams({
              groupType: GroupType.CERTIFICATION,
              bankId: null,
              questionId: null,
            })
          }
        />
      </div>

      <div className="mt-6 grid gap-5 lg:grid-cols-[280px_minmax(0,1fr)]">
        <Card className="h-fit overflow-hidden">
          <div className="border-b border-line px-4 py-3 text-sm font-bold">
            题库
          </div>
          {banksQuery.isLoading ? (
            <div className="p-4">
              <PageSkeleton />
            </div>
          ) : banksQuery.data?.records.length ? (
            <div className="divide-y divide-line/70">
              {banksQuery.data.records.map((bank) => {
                const active = bank.bankId === bankId
                const count =
                  (bank as unknown as Record<string, number>)[
                    pageCopy.countKey
                  ] ?? 0
                return (
                  <button
                    key={bank.bankId}
                    type="button"
                    className={cn(
                      'flex w-full items-center gap-3 px-4 py-4 text-left transition',
                      active ? 'bg-[#eee7e1]' : 'hover:bg-[#f7f3ef]',
                    )}
                    onClick={() =>
                      updateParams({
                        bankId: bank.bankId,
                        questionId: null,
                      })
                    }
                  >
                    <LibraryBig className="size-4 shrink-0 text-brand" />
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-sm font-bold">
                        {bank.bankName}
                      </span>
                      <span className="mt-1 block text-xs text-muted">
                        {count} 条记录
                      </span>
                    </span>
                    <ChevronRight className="size-4 text-muted" />
                  </button>
                )
              })}
            </div>
          ) : (
            <EmptyState title="暂无相关题库" />
          )}
        </Card>

        <div>
          {!bankId ? (
            <Card>
              <EmptyState
                title="请选择一个题库"
                description="选择后查看具体题目。"
              />
            </Card>
          ) : questionsQuery.isLoading ? (
            <PageSkeleton />
          ) : questionsQuery.isError ? (
            <ErrorState onRetry={() => void questionsQuery.refetch()} />
          ) : questionsQuery.data?.records.length ? (
            <div className="space-y-3">
              {questionsQuery.data.records.map((question, index) => (
                <Card key={question.questionId} className="overflow-hidden">
                  <button
                    type="button"
                    disabled={!question.isAvailable}
                    className="flex w-full items-center gap-3 p-4 text-left hover:bg-[#f7f3ef] disabled:cursor-not-allowed disabled:opacity-55"
                    onClick={() =>
                      updateParams({ questionId: question.questionId })
                    }
                  >
                    <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-[#eee7e1] text-xs font-bold text-brand">
                      {index + 1}
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="block font-semibold">
                        {question.title}
                      </span>
                      {!question.isAvailable ? (
                        <span className="mt-1 text-xs text-muted">
                          该题目已不可用
                        </span>
                      ) : null}
                    </span>
                    <ChevronRight className="size-4 text-muted" />
                  </button>
                  {questionId === question.questionId ? (
                    <div className="border-t border-line bg-[#faf7f4] p-5">
                      {detailQuery.isLoading ? (
                        <p className="text-sm text-muted">加载详情中…</p>
                      ) : detailQuery.data ? (
                        <QuestionDetail kind={kind} detail={detailQuery.data} />
                      ) : (
                        <p className="text-sm text-danger">详情加载失败</p>
                      )}
                    </div>
                  ) : null}
                </Card>
              ))}
            </div>
          ) : (
            <Card>
              <EmptyState title="该题库暂无记录" />
            </Card>
          )}
        </div>
      </div>
    </div>
  )
}

function GroupButton({
  active,
  label,
  onClick,
}: {
  active: boolean
  label: string
  onClick: () => void
}) {
  return (
    <button
      type="button"
      className={cn(
        'rounded-lg px-4 py-2 text-sm font-semibold',
        active ? 'bg-[#eee7e1] text-brand' : 'text-muted',
      )}
      onClick={onClick}
    >
      {label}
    </button>
  )
}

function QuestionDetail({
  kind,
  detail,
}: {
  kind: LibraryKind
  detail: {
    noteContent?: string
    content?: string
    chosenOptions?: string[]
    correctAnswer?: string[]
    analysis?: string
  }
}) {
  return (
    <div className="space-y-4 text-sm leading-7">
      {kind === 'note' ? (
        <DetailBlock label="我的笔记">
          {detail.noteContent || '暂无笔记内容'}
        </DetailBlock>
      ) : null}
      {detail.content ? (
        <DetailBlock label="我的回答">{detail.content}</DetailBlock>
      ) : null}
      {detail.chosenOptions?.length ? (
        <DetailBlock label="我的选择">
          {detail.chosenOptions.join('、')}
        </DetailBlock>
      ) : null}
      {detail.correctAnswer?.length ? (
        <DetailBlock label="正确选项">
          {detail.correctAnswer.join('、')}
        </DetailBlock>
      ) : null}
      {detail.analysis ? (
        <DetailBlock label="答案解析">{detail.analysis}</DetailBlock>
      ) : null}
    </div>
  )
}

function DetailBlock({
  label,
  children,
}: {
  label: string
  children: React.ReactNode
}) {
  return (
    <div>
      <Badge>{label}</Badge>
      <p className="mt-2 whitespace-pre-wrap text-muted">{children}</p>
    </div>
  )
}
