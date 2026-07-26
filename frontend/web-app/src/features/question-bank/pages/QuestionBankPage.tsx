import { useMutation, useQuery } from '@tanstack/react-query'
import {
  ArrowRight,
  BarChart3,
  BookOpen,
  CheckCircle2,
  Clock3,
  Flame,
  Layers3,
  Users,
} from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import {
  getQuestionBankGroupPage,
  getQuestionBankModulePage,
  getQuestionBanksBySort,
  getQuestionBankSubModulePage,
} from '@/features/question-bank/api'
import type {
  GroupPageData,
  QuestionBank,
} from '@/features/question-bank/types'
import { startCertificateExam } from '@/features/question/api'
import {
  QUESTION_BANK_GROUP_ID,
  SortType,
  type SortTypeValue,
} from '@/shared/constants/domain'
import { getErrorMessage } from '@/shared/api/errors'
import { cn } from '@/shared/lib/cn'
import { formatCount, formatRate } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/Badge'
import { Card } from '@/shared/ui/Card'
import { Dialog } from '@/shared/ui/Dialog'
import { EmptyState, ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

interface QuestionBankPageProps {
  kind: 'interview' | 'certification'
}

interface ViewState extends GroupPageData {
  selectedModuleId: number
  selectedSubModuleId: number
}

const moduleTones = [
  {
    active: 'border-[#9b776d] bg-[#9b776d] text-white shadow-[#80665d]/20',
    inactive:
      'border-[#d5beb6] bg-[#eadbd6] text-[#604b45] hover:border-[#c39e92] hover:bg-[#dfc6be]',
    panel: 'bg-[#e6d6d1]',
    accent: 'bg-[#9b776d]',
    icon: 'text-[#7f5e55]',
  },
  {
    active: 'border-[#748777] bg-[#748777] text-white shadow-[#5f7462]/20',
    inactive:
      'border-[#c5d0c4] bg-[#dce4da] text-[#465849] hover:border-[#9eb09d] hover:bg-[#cedaca]',
    panel: 'bg-[#d9e1d7]',
    accent: 'bg-[#748777]',
    icon: 'text-[#5f7462]',
  },
  {
    active: 'border-[#708594] bg-[#708594] text-white shadow-[#596f7e]/20',
    inactive:
      'border-[#c4d0d7] bg-[#dbe3e7] text-[#455966] hover:border-[#9fb1bc] hover:bg-[#cdd9df]',
    panel: 'bg-[#d9e1e5]',
    accent: 'bg-[#708594]',
    icon: 'text-[#586f7d]',
  },
  {
    active: 'border-[#8b7482] bg-[#8b7482] text-white shadow-[#755e6b]/20',
    inactive:
      'border-[#d3c5cd] bg-[#e6dce2] text-[#604d58] hover:border-[#b7a0ad] hover:bg-[#dacbd3]',
    panel: 'bg-[#e3d9df]',
    accent: 'bg-[#8b7482]',
    icon: 'text-[#725d69]',
  },
] as const

const bankTones = [
  {
    card: 'border-[#dfcbc3] bg-[#fffaf8] hover:border-[#bd8876]',
    marker: 'bg-[#bd8876]',
    action: 'bg-[#b87965] hover:bg-[#a76855]',
  },
  {
    card: 'border-[#cbd6db] bg-[#f9fbfc] hover:border-[#8096a3]',
    marker: 'bg-[#8096a3]',
    action: 'bg-[#708895] hover:bg-[#607985]',
  },
  {
    card: 'border-[#cbd5ca] bg-[#fafcf9] hover:border-[#829582]',
    marker: 'bg-[#829582]',
    action: 'bg-[#728874] hover:bg-[#617764]',
  },
  {
    card: 'border-[#d7c9d1] bg-[#fcf9fb] hover:border-[#987d8c]',
    marker: 'bg-[#987d8c]',
    action: 'bg-[#896f7e] hover:bg-[#775e6c]',
  },
] as const

const tagTones = [
  'border-[#dbc3ba] bg-[#edddd7] text-[#74564c]',
  'border-[#c6d4d9] bg-[#dde6e9] text-[#526a76]',
  'border-[#c9d5c8] bg-[#dfe7dc] text-[#506552]',
  'border-[#d8cdbd] bg-[#eae2d6] text-[#71614c]',
] as const

export function QuestionBankPage({ kind }: QuestionBankPageProps) {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const groupId =
    kind === 'interview'
      ? QUESTION_BANK_GROUP_ID.INTERVIEW
      : QUESTION_BANK_GROUP_ID.CERTIFICATION
  const title = kind === 'interview' ? '面试题库' : '认证题库'
  const description =
    kind === 'interview'
      ? '按技术方向选择题库，用表达和复盘建立稳定的面试思路。'
      : '通过练习或限时考试，系统准备专业认证。'
  const hydratedGroupId = useRef<number | null>(null)
  const [view, setView] = useState<ViewState | null>(null)
  const [selectedBank, setSelectedBank] = useState<QuestionBank | null>(null)

  const groupQuery = useQuery({
    queryKey: ['question-bank', 'group', groupId],
    queryFn: () => getQuestionBankGroupPage(groupId),
  })

  useEffect(() => {
    hydratedGroupId.current = null
    setView(null)
    setSelectedBank(null)
  }, [groupId])

  const selectionMutation = useMutation({
    mutationFn: async (
      action:
        | { type: 'module'; id: number }
        | { type: 'sub-module'; id: number }
        | { type: 'sort'; sort: SortTypeValue },
    ) => {
      if (!view) throw new Error('题库页面尚未初始化')

      if (action.type === 'module') {
        const data = await getQuestionBankModulePage(
          groupId,
          action.id,
          view.selectedModuleId,
        )
        return {
          ...view,
          ...data,
          selectedModuleId: action.id,
          selectedSubModuleId: data.firstSubModule.id,
        }
      }

      if (action.type === 'sub-module') {
        const data = await getQuestionBankSubModulePage(
          groupId,
          view.selectedModuleId,
          action.id,
          view.selectedSubModuleId,
        )
        return {
          ...view,
          ...data,
          selectedSubModuleId: action.id,
        }
      }

      const banks = await getQuestionBanksBySort(
        action.sort,
        view.selectedSubModuleId,
      )
      return { ...view, sort: action.sort, banks }
    },
    onSuccess: (nextView) => {
      setView(nextView)
      setSearchParams({
        moduleId: String(nextView.selectedModuleId),
        subModuleId: String(nextView.selectedSubModuleId),
        sort: String(nextView.sort),
      })
    },
  })

  useEffect(() => {
    if (!groupQuery.data || hydratedGroupId.current === groupId) return
    hydratedGroupId.current = groupId
    let cancelled = false

    const hydrate = async () => {
      const root = groupQuery.data
      let nextView: ViewState = {
        ...root,
        selectedModuleId: root.firstModule.id,
        selectedSubModuleId: root.firstSubModule.id,
      }
      const requestedModuleId = Number(searchParams.get('moduleId'))
      const requestedSubModuleId = Number(searchParams.get('subModuleId'))
      const requestedSort = Number(searchParams.get('sort')) as SortTypeValue

      if (
        requestedModuleId &&
        requestedModuleId !== nextView.selectedModuleId &&
        root.modules.some((module) => module.id === requestedModuleId)
      ) {
        const modulePage = await getQuestionBankModulePage(
          groupId,
          requestedModuleId,
          nextView.selectedModuleId,
        )
        nextView = {
          ...nextView,
          ...modulePage,
          selectedModuleId: requestedModuleId,
          selectedSubModuleId: modulePage.firstSubModule.id,
        }
      }

      if (
        requestedSubModuleId &&
        requestedSubModuleId !== nextView.selectedSubModuleId &&
        nextView.subModules.some(
          (subModule) => subModule.id === requestedSubModuleId,
        )
      ) {
        const subModulePage = await getQuestionBankSubModulePage(
          groupId,
          nextView.selectedModuleId,
          requestedSubModuleId,
          nextView.selectedSubModuleId,
        )
        nextView = {
          ...nextView,
          ...subModulePage,
          selectedSubModuleId: requestedSubModuleId,
        }
      }

      if (
        (requestedSort === SortType.HOT || requestedSort === SortType.LATEST) &&
        requestedSort !== nextView.sort
      ) {
        nextView = {
          ...nextView,
          sort: requestedSort,
          banks: await getQuestionBanksBySort(
            requestedSort,
            nextView.selectedSubModuleId,
          ),
        }
      }

      if (!cancelled) setView(nextView)
    }

    void hydrate().catch(() => {
      if (!cancelled) {
        setView({
          ...groupQuery.data,
          selectedModuleId: groupQuery.data.firstModule.id,
          selectedSubModuleId: groupQuery.data.firstSubModule.id,
        })
      }
    })

    return () => {
      cancelled = true
    }
  }, [groupId, groupQuery.data, searchParams])

  const examMutation = useMutation({
    mutationFn: startCertificateExam,
    onSuccess: (exam) => {
      setSelectedBank(null)
      navigate(`/banks/certification/exams/${exam.sessionId}`)
    },
  })

  if (groupQuery.isError) {
    return (
      <div className="app-container py-8">
        <ErrorState
          message={
            kind === 'certification'
              ? '认证题库的默认分类暂时没有可展示的题库，请配置题库数据后重试。'
              : getErrorMessage(groupQuery.error)
          }
          onRetry={() => void groupQuery.refetch()}
        />
      </div>
    )
  }

  if (groupQuery.isLoading || !view) {
    return (
      <div className="app-container py-8">
        <PageSkeleton />
      </div>
    )
  }

  const currentModule = view.modules.find(
    (module) => module.id === view.selectedModuleId,
  )
  const currentModuleIndex = Math.max(
    0,
    view.modules.findIndex((module) => module.id === view.selectedModuleId),
  )
  const currentTone = moduleTones[currentModuleIndex % moduleTones.length]

  const selectBank = (bank: QuestionBank) => {
    if (kind === 'interview') {
      navigate(`/banks/interview/${bank.id}/practice`)
    } else {
      setSelectedBank(bank)
    }
  }

  return (
    <div className="app-container relative isolate overflow-x-clip py-8">
      <span
        className="pointer-events-none absolute -right-24 top-4 -z-10 hidden size-72 rounded-full bg-[#d7dfe0]/45 blur-3xl sm:block"
        aria-hidden="true"
      />
      <span
        className="pointer-events-none absolute -left-28 top-48 -z-10 hidden size-64 rounded-full bg-[#e3d4ce]/40 blur-3xl sm:block"
        aria-hidden="true"
      />
      <header>
        <p className="inline-flex rounded-full bg-[#e4d6d1] px-3 py-1 text-xs font-bold tracking-wide text-[#755a51]">
          题库中心
        </p>
        <h1 className="mt-1 text-3xl font-extrabold tracking-tight">{title}</h1>
        <p className="mt-3 max-w-2xl text-sm leading-6 text-muted">
          {description}
        </p>
      </header>

      <div className="mt-7 grid grid-cols-2 gap-3 lg:grid-cols-4">
        {view.modules.map((module, index) => {
          const active = module.id === view.selectedModuleId
          const tone = moduleTones[index % moduleTones.length]
          return (
            <button
              key={module.id}
              type="button"
              aria-pressed={active}
              disabled={selectionMutation.isPending}
              className={cn(
                'group relative min-h-24 overflow-hidden rounded-2xl border px-4 py-4 text-left shadow-sm transition-all duration-300 motion-reduce:transition-none sm:min-h-28 sm:px-5',
                'hover:-translate-y-1 hover:shadow-lg motion-reduce:hover:translate-y-0',
                active ? tone.active : tone.inactive,
              )}
              onClick={() => {
                if (!active) {
                  selectionMutation.mutate({ type: 'module', id: module.id })
                }
              }}
            >
              <span className="relative block text-lg font-extrabold">
                {module.moduleName}
              </span>
              <span
                className={cn(
                  'relative mt-2 block text-xs transition-transform duration-300 group-hover:translate-x-0.5 motion-reduce:transition-none',
                  active ? 'text-white/75' : 'text-current opacity-70',
                )}
              >
                查看相关方向与题库
              </span>
              <span className="absolute -bottom-8 -right-6 size-24 rounded-full border-[18px] border-current opacity-10 transition-transform duration-500 group-hover:rotate-12 group-hover:scale-110 motion-reduce:transition-none" />
            </button>
          )
        })}
      </div>

      <div className="mt-7 grid gap-6 lg:grid-cols-[236px_minmax(0,1fr)]">
        <aside
          className={cn(
            'h-fit overflow-hidden rounded-2xl p-3 shadow-sm transition-colors duration-300',
            currentTone.panel,
          )}
        >
          <div className="flex items-center gap-3 px-2 pb-3 pt-1">
            <span
              className={cn(
                'flex size-9 shrink-0 items-center justify-center rounded-xl bg-white/75 shadow-sm',
                currentTone.icon,
              )}
            >
              <Layers3 className="size-4.5" />
            </span>
            <div className="min-w-0">
              <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#958a83]">
                学习方向
              </p>
              <p className="mt-0.5 truncate text-sm font-extrabold text-ink">
                {currentModule?.moduleName || '分类'}
              </p>
            </div>
          </div>
          <div className="space-y-1" role="tablist" aria-label="题库学习方向">
            {view.subModules.map((subModule, index) => {
              const active = subModule.id === view.selectedSubModuleId
              return (
                <button
                  key={subModule.id}
                  type="button"
                  role="tab"
                  aria-selected={active}
                  disabled={selectionMutation.isPending}
                  className={cn(
                    'group flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left text-sm font-semibold transition-all duration-200 motion-reduce:transition-none',
                    active
                      ? 'bg-white/90 text-ink shadow-sm'
                      : 'text-muted hover:translate-x-1 hover:bg-white/55 hover:text-ink motion-reduce:hover:translate-x-0',
                  )}
                  onClick={() => {
                    if (!active) {
                      selectionMutation.mutate({
                        type: 'sub-module',
                        id: subModule.id,
                      })
                    }
                  }}
                >
                  <span
                    className={cn(
                      'flex size-6 shrink-0 items-center justify-center rounded-lg text-[10px] font-extrabold',
                      active
                        ? cn(currentTone.accent, 'text-white')
                        : 'bg-white/65 text-[#958a83] group-hover:bg-white',
                    )}
                  >
                    {String(index + 1).padStart(2, '0')}
                  </span>
                  <span className="min-w-0 flex-1 truncate">
                    {subModule.subModuleName}
                  </span>
                  <ArrowRight
                    className={cn(
                      'size-3.5 shrink-0 transition',
                      active
                        ? 'translate-x-0 text-brand'
                        : '-translate-x-1 text-transparent group-hover:translate-x-0 group-hover:text-[#a69a92]',
                    )}
                  />
                </button>
              )
            })}
          </div>
        </aside>

        <section className="min-w-0">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-extrabold">题库列表</h2>
              <p className="mt-1 text-xs text-muted">
                共 {view.banks.length} 个题库
              </p>
            </div>
            <div className="flex rounded-xl border border-[#c8d3d5] bg-[#e5eaeb] p-1 shadow-inner">
              <SortButton
                active={view.sort === SortType.HOT}
                icon={Flame}
                label="热度"
                onClick={() => {
                  if (view.sort !== SortType.HOT) {
                    selectionMutation.mutate({
                      type: 'sort',
                      sort: SortType.HOT,
                    })
                  }
                }}
              />
              <SortButton
                active={view.sort === SortType.LATEST}
                icon={Clock3}
                label="最新"
                onClick={() => {
                  if (view.sort !== SortType.LATEST) {
                    selectionMutation.mutate({
                      type: 'sort',
                      sort: SortType.LATEST,
                    })
                  }
                }}
              />
            </div>
          </div>

          {selectionMutation.isError ? (
            <p
              className="mt-4 rounded-xl bg-[#f8eaea] px-4 py-3 text-sm text-danger"
              role="alert"
            >
              {getErrorMessage(selectionMutation.error)}
            </p>
          ) : null}

          <div className="mt-4 space-y-3">
            {view.banks.length ? (
              view.banks.map((bank, bankIndex) => {
                const tone = bankTones[bankIndex % bankTones.length]
                return (
                  <Card
                    key={bank.id}
                    className={cn(
                      'bank-card group relative isolate flex min-h-32 flex-col overflow-hidden p-0 transition-all duration-300 motion-reduce:transition-none sm:flex-row sm:items-stretch',
                      tone.card,
                    )}
                  >
                    <div className="bank-card-content min-w-0 flex-1 p-5 transition-all duration-300 motion-reduce:transition-none">
                      <div className="flex items-start gap-3">
                        <span
                          className={cn(
                            'mt-1 h-8 w-1 shrink-0 rounded-full transition-transform duration-300 group-hover:scale-y-125 motion-reduce:transition-none',
                            tone.marker,
                          )}
                          aria-hidden="true"
                        />
                        <div className="min-w-0 flex-1">
                          <h3 className="truncate text-base font-bold transition-colors group-hover:text-[#5d4d47]">
                            {bank.bankName}
                          </h3>
                          <div className="mt-2 flex flex-wrap gap-2">
                            {bank.tagNames.map((tag, tagIndex) => (
                              <Badge
                                key={tag}
                                className={cn(
                                  'transition-transform duration-200 group-hover:-translate-y-0.5 motion-reduce:transition-none',
                                  tagTones[tagIndex % tagTones.length],
                                )}
                              >
                                {tag}
                              </Badge>
                            ))}
                          </div>
                        </div>
                      </div>
                      <div className="mt-4 flex flex-wrap items-center gap-x-4 gap-y-2 pl-4 text-xs text-muted">
                        {bank.questionCount != null ? (
                          <span className="inline-flex items-center gap-1.5">
                            <BookOpen className="size-3.5 text-[#8c776f]" />
                            {bank.questionCount} 道题
                          </span>
                        ) : null}
                        <span className="inline-flex items-center gap-1.5">
                          <Users className="size-3.5 text-[#71858f]" />
                          {formatCount(bank.completeCount)} 人完成
                        </span>
                        <span className="inline-flex items-center gap-1.5">
                          <BarChart3 className="size-3.5 text-[#718675]" />
                          平均正确率
                          <strong className="font-extrabold text-ink">
                            {formatRate(bank.avgCorrectRate)}
                          </strong>
                        </span>
                      </div>
                    </div>
                    <button
                      type="button"
                      className={cn(
                        'bank-action relative flex min-h-14 w-full shrink-0 items-center justify-center gap-3 overflow-hidden px-5 text-sm font-extrabold text-white transition-all duration-300 motion-reduce:transition-none sm:absolute sm:inset-y-0 sm:right-0 sm:min-h-0 sm:w-40',
                        tone.action,
                      )}
                      onClick={() => selectBank(bank)}
                    >
                      <span className="absolute -bottom-9 -right-7 size-24 rounded-full border-[17px] border-white/10 transition-transform duration-500 group-hover:scale-125 motion-reduce:transition-none" />
                      <span className="relative">开始答题</span>
                      <span className="relative flex size-7 items-center justify-center rounded-full bg-white/18 transition-transform duration-300 group-hover:translate-x-1 motion-reduce:transition-none">
                        <ArrowRight className="size-4" />
                      </span>
                    </button>
                  </Card>
                )
              })
            ) : (
              <Card className="border-[#cbd5ca] bg-[#f7faf6]">
                <EmptyState title="当前分类暂无题库" />
              </Card>
            )}
          </div>
        </section>
      </div>

      <Dialog
        open={Boolean(selectedBank)}
        onOpenChange={(open) => {
          if (!open) setSelectedBank(null)
        }}
        title={selectedBank?.bankName || '选择答题模式'}
        description="练习模式逐题查看解析；考试模式会创建或恢复一个限时场次。"
      >
        <div className="grid gap-3 sm:grid-cols-2">
          <ModeButton
            icon={BookOpen}
            title="练习模式"
            description="逐题作答并立即查看答案解析"
            onClick={() =>
              navigate(`/banks/certification/${selectedBank?.id}/practice`)
            }
          />
          <ModeButton
            icon={CheckCircle2}
            title="考试模式"
            description="限时作答，统一交卷后查看成绩"
            disabled={examMutation.isPending}
            onClick={() => {
              if (selectedBank) examMutation.mutate(selectedBank.id)
            }}
          />
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

function SortButton({
  active,
  icon: Icon,
  label,
  onClick,
}: {
  active: boolean
  icon: typeof Flame
  label: string
  onClick: () => void
}) {
  return (
    <button
      type="button"
      className={cn(
        'flex items-center gap-1.5 rounded-lg px-3 py-2 text-xs font-semibold transition-all duration-200 motion-reduce:transition-none',
        active
          ? 'bg-[#708792] text-white shadow-sm'
          : 'text-[#5e7078] hover:-translate-y-0.5 hover:bg-white/75 hover:text-[#43545b] motion-reduce:hover:translate-y-0',
      )}
      onClick={onClick}
    >
      <Icon className="size-3.5" />
      {label}
    </button>
  )
}

function ModeButton({
  icon: Icon,
  title,
  description,
  disabled,
  onClick,
}: {
  icon: typeof BookOpen
  title: string
  description: string
  disabled?: boolean
  onClick: () => void
}) {
  return (
    <button
      type="button"
      disabled={disabled}
      className="rounded-xl border border-line p-4 text-left transition hover:border-brand hover:bg-[#f8f4f0] disabled:opacity-60"
      onClick={onClick}
    >
      <Icon className="size-5 text-brand" />
      <span className="mt-3 block font-bold">{title}</span>
      <span className="mt-1 block text-xs leading-5 text-muted">
        {description}
      </span>
    </button>
  )
}
