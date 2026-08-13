import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { ArrowRight, CheckCircle2 } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import {
  registerByEmail,
  sendEmailCode,
  verifyEmailCode,
} from '@/features/auth/api'
import { AuthLayout } from '@/features/auth/components/AuthLayout'
import { OAuthButtons } from '@/features/auth/components/OAuthButtons'
import { Turnstile } from '@/features/auth/components/Turnstile'
import { authToken } from '@/features/auth/token'
import { getErrorMessage } from '@/shared/api/errors'
import { Button } from '@/shared/ui/Button'
import { Input } from '@/shared/ui/Input'

const RESEND_COOLDOWN_SECONDS = 60

const emailSchema = z.object({
  email: z.email('请输入正确的邮箱地址'),
})

const codeSchema = z.object({
  code: z.string().regex(/^\d{6}$/, '请输入 6 位数字验证码'),
})

const credentialsSchema = z
  .object({
    displayName: z.string().trim().min(1, '请输入昵称'),
    password: z.string().min(1, '请输入密码'),
    passwordConfirm: z.string().min(1, '请再次输入密码'),
  })
  .refine((values) => values.password === values.passwordConfirm, {
    path: ['passwordConfirm'],
    message: '两次输入的密码不一致',
  })

type RegisterStep = 'email' | 'code' | 'credentials'
type EmailForm = z.infer<typeof emailSchema>
type CodeForm = z.infer<typeof codeSchema>
type CredentialsForm = z.infer<typeof credentialsSchema>

export function RegisterPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState<RegisterStep>('email')
  const [email, setEmail] = useState('')
  const [secureTicket, setSecureTicket] = useState('')
  const [turnstileToken, setTurnstileToken] = useState('')
  const [turnstileKey, setTurnstileKey] = useState(0)
  const [turnstileError, setTurnstileError] = useState('')
  const [resendSeconds, setResendSeconds] = useState(0)

  const handleTurnstile = useCallback((token: string) => {
    setTurnstileToken(token)
    if (token) setTurnstileError('')
  }, [])

  const emailForm = useForm<EmailForm>({
    resolver: zodResolver(emailSchema),
  })
  const codeForm = useForm<CodeForm>({
    resolver: zodResolver(codeSchema),
  })
  const credentialsForm = useForm<CredentialsForm>({
    resolver: zodResolver(credentialsSchema),
  })

  useEffect(() => {
    if (resendSeconds <= 0) return
    const timer = window.setTimeout(
      () => setResendSeconds((seconds) => seconds - 1),
      1000,
    )
    return () => window.clearTimeout(timer)
  }, [resendSeconds])

  const sendMutation = useMutation({
    mutationFn: (targetEmail: string) =>
      sendEmailCode({
        email: targetEmail,
        turnstileToken,
      }),
    onSuccess: (_, targetEmail) => {
      setEmail(targetEmail)
      setStep('code')
      setResendSeconds(RESEND_COOLDOWN_SECONDS)
      setTurnstileToken('')
      setTurnstileKey((key) => key + 1)
      setTurnstileError('')
      codeForm.reset()
    },
  })

  const verifyMutation = useMutation({
    mutationFn: (code: string) => verifyEmailCode({ email, code }),
    onSuccess: (ticket) => {
      setSecureTicket(ticket)
      setStep('credentials')
    },
  })

  const registerMutation = useMutation({
    mutationFn: (values: CredentialsForm) =>
      registerByEmail({
        ...values,
        email,
        secureTicket,
      }),
    onSuccess: (token) => {
      authToken.set(token)
      navigate('/home', { replace: true })
    },
  })

  function requireTurnstile() {
    if (turnstileToken) return true
    setTurnstileError('请先完成人机验证')
    return false
  }

  function handleInitialSend(values: EmailForm) {
    if (!requireTurnstile()) return
    sendMutation.mutate(values.email.trim().toLowerCase())
  }

  function handleResend() {
    if (!requireTurnstile()) return
    sendMutation.mutate(email)
  }

  function changeEmail() {
    setStep('email')
    setEmail('')
    setSecureTicket('')
    setResendSeconds(0)
    setTurnstileToken('')
    setTurnstileError('')
    setTurnstileKey((key) => key + 1)
    codeForm.reset()
    credentialsForm.reset()
    sendMutation.reset()
    verifyMutation.reset()
    registerMutation.reset()
  }

  return (
    <AuthLayout
      title="创建 HomeWork 账号"
      description="验证邮箱后，再设置你的昵称和密码。"
      footer={
        <>
          已有账号？
          <Link className="ml-1 font-semibold text-brand" to="/login">
            返回登录
          </Link>
        </>
      }
    >
      <StepIndicator step={step} />

      {step === 'email' ? (
        <form
          className="mt-6 space-y-4"
          onSubmit={emailForm.handleSubmit(handleInitialSend)}
        >
          <Field
            label="邮箱"
            error={emailForm.formState.errors.email?.message}
            input={
              <Input
                type="email"
                autoComplete="email"
                placeholder="name@example.com"
                {...emailForm.register('email')}
              />
            }
          />
          <Turnstile key={turnstileKey} onTokenChange={handleTurnstile} />
          <InlineError
            message={
              turnstileError ||
              (sendMutation.isError
                ? getErrorMessage(sendMutation.error)
                : undefined)
            }
          />
          <Button
            className="w-full"
            size="lg"
            type="submit"
            disabled={sendMutation.isPending}
          >
            {sendMutation.isPending ? '正在发送…' : '发送验证码'}
            <ArrowRight className="size-4" />
          </Button>
        </form>
      ) : null}

      {step === 'code' ? (
        <form
          className="mt-6 space-y-4"
          onSubmit={codeForm.handleSubmit((values) =>
            verifyMutation.mutate(values.code),
          )}
        >
          <VerifiedEmail email={email} onChange={changeEmail} />
          <Field
            label="邮箱验证码"
            error={codeForm.formState.errors.code?.message}
            input={
              <Input
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                placeholder="请输入 6 位验证码"
                {...codeForm.register('code')}
              />
            }
          />
          <InlineError
            message={
              verifyMutation.isError
                ? getErrorMessage(verifyMutation.error)
                : undefined
            }
          />
          <Button
            className="w-full"
            size="lg"
            type="submit"
            disabled={verifyMutation.isPending}
          >
            {verifyMutation.isPending ? '正在验证…' : '验证邮箱'}
            <ArrowRight className="size-4" />
          </Button>

          {resendSeconds > 0 ? (
            <p className="text-center text-xs text-muted">
              {resendSeconds} 秒后可重新发送
            </p>
          ) : (
            <div className="space-y-3 rounded-xl border border-line bg-surface-muted p-3">
              <Turnstile key={turnstileKey} onTokenChange={handleTurnstile} />
              <InlineError
                message={
                  turnstileError ||
                  (sendMutation.isError
                    ? getErrorMessage(sendMutation.error)
                    : undefined)
                }
              />
              <Button
                className="w-full"
                variant="secondary"
                type="button"
                disabled={sendMutation.isPending}
                onClick={handleResend}
              >
                {sendMutation.isPending ? '正在重发…' : '重新发送验证码'}
              </Button>
            </div>
          )}
        </form>
      ) : null}

      {step === 'credentials' ? (
        <form
          className="mt-6 space-y-4"
          onSubmit={credentialsForm.handleSubmit((values) =>
            registerMutation.mutate(values),
          )}
        >
          <VerifiedEmail email={email} onChange={changeEmail} verified />
          <Field
            label="昵称"
            error={credentialsForm.formState.errors.displayName?.message}
            input={
              <Input
                autoComplete="nickname"
                placeholder="你的展示名称"
                {...credentialsForm.register('displayName')}
              />
            }
          />
          <div className="grid gap-4 sm:grid-cols-2">
            <Field
              label="密码"
              error={credentialsForm.formState.errors.password?.message}
              input={
                <Input
                  type="password"
                  autoComplete="new-password"
                  placeholder="请输入密码"
                  {...credentialsForm.register('password')}
                />
              }
            />
            <Field
              label="确认密码"
              error={credentialsForm.formState.errors.passwordConfirm?.message}
              input={
                <Input
                  type="password"
                  autoComplete="new-password"
                  placeholder="再次输入"
                  {...credentialsForm.register('passwordConfirm')}
                />
              }
            />
          </div>
          <InlineError
            message={
              registerMutation.isError
                ? getErrorMessage(registerMutation.error)
                : undefined
            }
          />
          <Button
            className="w-full"
            size="lg"
            type="submit"
            disabled={registerMutation.isPending}
          >
            {registerMutation.isPending ? '正在创建…' : '创建账号'}
            <ArrowRight className="size-4" />
          </Button>
        </form>
      ) : null}

      {step === 'email' ? (
        <OAuthButtons turnstileToken={turnstileToken} />
      ) : null}
    </AuthLayout>
  )
}

function StepIndicator({ step }: { step: RegisterStep }) {
  const currentStep = step === 'email' ? 1 : step === 'code' ? 2 : 3

  return (
    <ol className="grid grid-cols-3 gap-2" aria-label="注册进度">
      {['填写邮箱', '验证邮箱', '设置密码'].map((label, index) => {
        const stepNumber = index + 1
        const active = stepNumber === currentStep
        const complete = stepNumber < currentStep
        return (
          <li
            key={label}
            className={`rounded-xl border px-2 py-2 text-center text-xs font-semibold ${
              active
                ? 'border-brand bg-brand-soft text-brand-dark'
                : complete
                  ? 'border-[#add0c3] bg-success-soft text-success'
                  : 'border-line text-muted'
            }`}
          >
            {complete ? '✓ ' : `${stepNumber}. `}
            {label}
          </li>
        )
      })}
    </ol>
  )
}

function VerifiedEmail({
  email,
  onChange,
  verified = false,
}: {
  email: string
  onChange: () => void
  verified?: boolean
}) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-xl bg-brand-soft px-3.5 py-3 text-sm">
      <span className="min-w-0 truncate text-ink">
        {verified ? (
          <CheckCircle2 className="mr-2 inline size-4 text-success" />
        ) : null}
        {email}
      </span>
      <button
        className="shrink-0 font-semibold text-brand-dark hover:text-brand"
        type="button"
        onClick={onChange}
      >
        更换邮箱
      </button>
    </div>
  )
}

function InlineError({ message }: { message?: string }) {
  return message ? (
    <p className="rounded-xl bg-danger-soft px-3 py-2 text-sm text-danger">
      {message}
    </p>
  ) : null
}

interface FieldProps {
  label: string
  error?: string
  input: React.ReactNode
}

function Field({ label, error, input }: FieldProps) {
  return (
    <label className="block text-sm font-semibold text-ink">
      {label}
      <span className="mt-2 block">{input}</span>
      {error ? (
        <span className="mt-1.5 block text-xs text-danger">{error}</span>
      ) : null}
    </label>
  )
}
