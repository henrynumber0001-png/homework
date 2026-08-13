import { CheckCircle2 } from 'lucide-react'
import { Button } from '@/shared/ui/Button'
import { Dialog } from '@/shared/ui/Dialog'

interface FinishBankDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  pending: boolean
  error: boolean
  answeredCount: number
  totalCount: number
  onConfirm: () => void
}

export function FinishBankDialog({
  open,
  onOpenChange,
  pending,
  error,
  answeredCount,
  totalCount,
  onConfirm,
}: FinishBankDialogProps) {
  const unansweredCount = Math.max(totalCount - answeredCount, 0)

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title="确认完成当前题库？"
      description="完成后将生成本次正确率，并进入答案回顾页面。"
      footer={
        <>
          <Button
            type="button"
            variant="secondary"
            disabled={pending}
            onClick={() => onOpenChange(false)}
          >
            继续答题
          </Button>
          <Button type="button" disabled={pending} onClick={onConfirm}>
            {pending ? '正在完成…' : '确认完成'}
          </Button>
        </>
      }
    >
      <div className="flex items-start gap-3 rounded-xl bg-brand-soft p-4 text-sm leading-6 text-brand-dark">
        <CheckCircle2 className="mt-0.5 size-5 shrink-0 text-brand" />
        <span>
          已作答 {answeredCount} / {totalCount} 题
          {unansweredCount
            ? `，还有 ${unansweredCount} 题未作答。`
            : '，所有题目均已作答。'}
        </span>
      </div>
      {error ? (
        <p className="mt-3 text-sm text-danger">完成题库失败，请稍后重试。</p>
      ) : null}
    </Dialog>
  )
}
