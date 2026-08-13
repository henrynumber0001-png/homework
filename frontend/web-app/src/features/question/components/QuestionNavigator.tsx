import { cn } from '@/shared/lib/cn'

interface QuestionNavigatorProps {
  questionIds: number[]
  currentId: number
  answeredIds: Set<number>
  onSelect: (questionId: number) => void
}

export function QuestionNavigator({
  questionIds,
  currentId,
  answeredIds,
  onSelect,
}: QuestionNavigatorProps) {
  return (
    <div className="surface-card h-fit p-4">
      <div className="mb-3 flex items-center justify-between text-xs text-muted">
        <span className="font-bold text-ink">题目导航</span>
        <span>
          {answeredIds.size}/{questionIds.length}
        </span>
      </div>
      <div className="grid grid-cols-5 gap-2 lg:grid-cols-4">
        {questionIds.map((id, index) => {
          const active = id === currentId
          const answered = answeredIds.has(id)
          return (
            <button
              key={id}
              type="button"
              className={cn(
                'flex aspect-square items-center justify-center rounded-lg border text-xs font-bold transition',
                answered
                  ? 'border-line bg-surface-muted text-muted'
                  : 'border-line bg-white text-ink hover:border-brand',
                active && 'border-brand text-brand ring-2 ring-brand/15',
              )}
              aria-label={`第${index + 1}题，${answered ? '已作答' : '未作答'}`}
              aria-current={active ? 'step' : undefined}
              onClick={() => onSelect(id)}
            >
              {index + 1}
            </button>
          )
        })}
      </div>
    </div>
  )
}
