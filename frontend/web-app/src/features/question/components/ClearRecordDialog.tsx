import { AlertTriangle } from 'lucide-react'
import { Button } from '@/shared/ui/Button'
import { Dialog } from '@/shared/ui/Dialog'

interface ClearRecordDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  pending: boolean
  error: boolean
  onConfirm: () => void
}

export function ClearRecordDialog({
  open,
  onOpenChange,
  pending,
  error,
  onConfirm,
}: ClearRecordDialogProps) {
  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title="清空当前题库记录？"
      description="清空后，当前题库的全部作答记录和答题结果将不可恢复，但你可以重新作答。"
      footer={
        <>
          <Button
            type="button"
            variant="secondary"
            disabled={pending}
            onClick={() => onOpenChange(false)}
          >
            取消
          </Button>
          <Button
            type="button"
            variant="danger"
            disabled={pending}
            onClick={onConfirm}
          >
            {pending ? '正在清空…' : '确认清空'}
          </Button>
        </>
      }
    >
      <div className="flex items-start gap-3 rounded-xl bg-danger-soft p-4 text-sm leading-6 text-danger">
        <AlertTriangle className="mt-0.5 size-5 shrink-0" />
        收藏和笔记不会被删除，仅清空该题库在答题记录表中的数据。
      </div>
      {error ? (
        <p className="mt-3 text-sm text-danger">清空失败，请稍后重试。</p>
      ) : null}
    </Dialog>
  )
}
