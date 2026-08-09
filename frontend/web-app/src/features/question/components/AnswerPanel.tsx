import { Bot, CheckCircle2, Sparkles, XCircle } from 'lucide-react'
import type {
  CertificateAnswer,
  InterviewAnswer,
} from '@/features/question/types'
import { formatRate } from '@/shared/lib/format'
import { Card } from '@/shared/ui/Card'

export function InterviewAnswerPanel({
  answer,
  onAskAi,
}: {
  answer?: InterviewAnswer
  onAskAi: () => void
}) {
  if (!answer) {
    return (
      <Card className="p-5 text-sm leading-6 text-muted">
        提交回答后，这里会显示参考答案和可用的 AI 反馈。
      </Card>
    )
  }

  return (
    <div className="space-y-4">
      {answer.aiEvaluationEnabled && answer.aiResult ? (
        <Card className="p-5">
          <div className="flex items-center justify-between">
            <h3 className="flex items-center gap-2 font-bold">
              <Sparkles className="size-4 text-premium" />
              AI 反馈
            </h3>
            <span className="text-lg font-extrabold text-brand">
              {formatRate(answer.aiResult.scoreRate)}
            </span>
          </div>
          {answer.aiResult.summary ? (
            <p className="mt-3 text-sm leading-6 text-muted">
              {answer.aiResult.summary}
            </p>
          ) : null}
          <div className="mt-4 space-y-2 text-xs leading-5 text-muted">
            {answer.aiResult.accurateComment ? (
              <p>准确之处：{answer.aiResult.accurateComment}</p>
            ) : null}
            {answer.aiResult.missingComment ? (
              <p>可以补充：{answer.aiResult.missingComment}</p>
            ) : null}
            {answer.aiResult.wrongComment ? (
              <p>需要修正：{answer.aiResult.wrongComment}</p>
            ) : null}
          </div>
          <button
            type="button"
            className="mt-4 inline-flex items-center gap-1.5 text-xs font-bold text-accent hover:text-brand"
            onClick={onAskAi}
          >
            <Bot className="size-4" />
            追问 AI
          </button>
        </Card>
      ) : (
        <Card className="p-5">
          <h3 className="font-bold">AI 评分</h3>
          <p className="mt-2 text-sm leading-6 text-muted">
            当前账号暂未启用 AI 评分，参考答案仍可正常查看。
          </p>
        </Card>
      )}
      <Card className="p-5">
        <h3 className="font-bold">参考答案</h3>
        <p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-muted">
          {answer.analysis}
        </p>
      </Card>
    </div>
  )
}

export function CertificateAnswerPanel({
  answer,
}: {
  answer?: CertificateAnswer
}) {
  if (!answer) {
    return (
      <Card className="p-5 text-sm leading-6 text-muted">
        提交本题后显示正确选项和答案解析。
      </Card>
    )
  }

  return (
    <div className="space-y-4">
      <Card
        className={
          answer.correct
            ? 'border-[#add0c3] bg-success-soft p-5'
            : 'border-[#e3b9b9] bg-danger-soft p-5'
        }
      >
        <div className="flex items-center gap-2 font-bold">
          {answer.correct ? (
            <CheckCircle2 className="size-5 text-success" />
          ) : (
            <XCircle className="size-5 text-danger" />
          )}
          {answer.correct ? '回答正确' : '回答不正确'}
        </div>
        <p className="mt-3 text-sm text-muted">
          正确选项：{answer.correctAnswer.join('、')}
        </p>
      </Card>
      <Card className="p-5">
        <h3 className="font-bold">答案解析</h3>
        <p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-muted">
          {answer.analysis}
        </p>
      </Card>
    </div>
  )
}
