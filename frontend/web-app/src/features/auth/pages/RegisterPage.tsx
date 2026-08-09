import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { ArrowRight } from 'lucide-react'
import { useCallback, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { registerByEmail } from '@/features/auth/api'
import { AuthLayout } from '@/features/auth/components/AuthLayout'
import { OAuthButtons } from '@/features/auth/components/OAuthButtons'
import { Turnstile } from '@/features/auth/components/Turnstile'
import { authToken } from '@/features/auth/token'
import { getErrorMessage } from '@/shared/api/errors'
import { Button } from '@/shared/ui/Button'
import { Input } from '@/shared/ui/Input'

const registerSchema = z
  .object({
    displayName: z.string().trim().min(1, '请输入昵称'),
    email: z.email('请输入正确的邮箱地址'),
    password: z.string().min(1, '请输入密码'),
    passwordConfirm: z.string().min(1, '请再次输入密码'),
  })
  .refine((values) => values.password === values.passwordConfirm, {
    path: ['passwordConfirm'],
    message: '两次输入的密码不一致',
  })

type RegisterForm = z.infer<typeof registerSchema>

export function RegisterPage() {
  const navigate = useNavigate()
  const [turnstileToken, setTurnstileToken] = useState('')
  const handleTurnstile = useCallback((token: string) => {
    setTurnstileToken(token)
  }, [])
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
  })

  const registerMutation = useMutation({
    mutationFn: (values: RegisterForm) =>
      registerByEmail({ ...values, turnstileToken }),
    onSuccess: (token) => {
      authToken.set(token)
      navigate('/home', { replace: true })
    },
  })

  return (
    <AuthLayout
      title="创建 HomeWork 账号"
      description="用一个账号记录练习、错题、笔记和学习动态。"
      footer={
        <>
          已有账号？
          <Link className="ml-1 font-semibold text-brand" to="/login">
            返回登录
          </Link>
        </>
      }
    >
      <form
        className="space-y-4"
        onSubmit={handleSubmit((values) => registerMutation.mutate(values))}
      >
        <Field
          label="昵称"
          error={errors.displayName?.message}
          input={
            <Input
              autoComplete="nickname"
              placeholder="你的展示名称"
              {...register('displayName')}
            />
          }
        />
        <Field
          label="邮箱"
          error={errors.email?.message}
          input={
            <Input
              type="email"
              autoComplete="email"
              placeholder="name@example.com"
              {...register('email')}
            />
          }
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <Field
            label="密码"
            error={errors.password?.message}
            input={
              <Input
                type="password"
                autoComplete="new-password"
                placeholder="请输入密码"
                {...register('password')}
              />
            }
          />
          <Field
            label="确认密码"
            error={errors.passwordConfirm?.message}
            input={
              <Input
                type="password"
                autoComplete="new-password"
                placeholder="再次输入"
                {...register('passwordConfirm')}
              />
            }
          />
        </div>
        <Turnstile onTokenChange={handleTurnstile} />
        {registerMutation.isError ? (
          <p className="rounded-xl bg-danger-soft px-3 py-2 text-sm text-danger">
            {getErrorMessage(registerMutation.error)}
          </p>
        ) : null}
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
      <OAuthButtons turnstileToken={turnstileToken} />
    </AuthLayout>
  )
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
