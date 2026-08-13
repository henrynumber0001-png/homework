import { Bot, CheckCircle2, FileText, Sparkles, XCircle } from 'lucide-react'
import type {
  CertificateAnswer,
  InterviewAnswer,
} from '@/features/question/types'
import { formatRate } from '@/shared/lib/format'
import { Card } from '@/shared/ui/Card'

export function InterviewReferenceAnswer({
  answer,
}: {
  answer: InterviewAnswer
}) {
  return (
    <Card className="p-5 sm:p-6">
      <h3 className="flex items-center gap-2 font-bold">
        <FileText className="size-4 text-accent" />
        参考答案
      </h3>
      <p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-muted">
        {answer.analysis}
      </p>
    </Card>
  )
}

export function InterviewAiPanel({
  answer,
  onAskAi,
}: {
  answer: InterviewAnswer
  onAskAi: () => void
}) {
  return (
    <Card className="p-5">
      {answer.aiEvaluationEnabled && answer.aiResult ? (
        <>
          <div className="flex items-center justify-between gap-3">
            <h3 className="flex items-center gap-2 font-bold">
              <Sparkles className="size-4 text-premium" />
              AI 解析
            </h3>
            {answer.aiResult.scoreRate === null ? null : (
              <span className="text-lg font-extrabold text-brand">
                {formatRate(answer.aiResult.scoreRate)}
              </span>
            )}
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
            {answer.aiResult.innovativeComment ? (
              <p>亮点思路：{answer.aiResult.innovativeComment}</p>
            ) : null}
            {answer.aiResult.missingComment ? (
              <p>可以补充：{answer.aiResult.missingComment}</p>
            ) : null}
            {answer.aiResult.wrongComment ? (
              <p>需要修正：{answer.aiResult.wrongComment}</p>
            ) : null}
          </div>
        </>
      ) : (
        <>
          <h3 className="flex items-center gap-2 font-bold">
            <Sparkles className="size-4 text-premium" />
            AI 解析
          </h3>
          <p className="mt-2 text-sm leading-6 text-muted">
            当前账号暂未启用 AI 评分，你仍可以围绕参考答案继续追问。
          </p>
        </>
      )}

      <button
        type="button"
        className="mt-5 inline-flex min-h-8 items-center gap-1.5 rounded-lg bg-accent-soft px-2.5 text-xs font-bold text-accent transition-colors hover:bg-brand-soft hover:text-brand"
        onClick={onAskAi}
      >
        <Bot className="size-3.5" />
        追问 AI
      </button>
    </Card>
  )
}

export function CertificateResultBanner({
  answer,
}: {
  answer: CertificateAnswer
}) {
  return (
    <div
      className={
        answer.correct
          ? 'flex items-center gap-2 rounded-2xl border border-[#add0c3] bg-success-soft px-5 py-4 font-bold text-success'
          : 'flex items-center gap-2 rounded-2xl border border-[#e3b9b9] bg-danger-soft px-5 py-4 font-bold text-danger'
      }
      role="status"
    >
      {answer.correct ? (
        <CheckCircle2 className="size-5" />
      ) : (
        <XCircle className="size-5" />
      )}
      {answer.correct ? '回答正确' : '回答错误，请查看解析'}
    </div>
  )
}

export function CertificateAnswerPanel({
  answer,
  onAskAi,
}: {
  answer: CertificateAnswer
  onAskAi: () => void
}) {
  return (
    <Card className="p-5 sm:p-6">
      <h3 className="flex items-center gap-2 font-bold">
        <FileText className="size-4 text-accent" />
        答案解析
      </h3>
      <p className="mt-3 text-sm font-semibold text-ink">
        正确选项：{answer.correctAnswer.join('、')}
      </p>
      <p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-muted">
        {answer.analysis}
      </p>
      <div className="mt-5 border-t border-line pt-4">
        <button
          type="button"
          className="inline-flex min-h-8 items-center gap-1.5 rounded-lg bg-accent-soft px-2.5 text-xs font-bold text-accent transition-colors hover:bg-brand-soft hover:text-brand"
          onClick={onAskAi}
        >
          <Bot className="size-3.5" />
          追问 AI
        </button>
      </div>
    </Card>
  )
}
