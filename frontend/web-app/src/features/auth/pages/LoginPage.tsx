import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { ArrowRight } from 'lucide-react'
import { useCallback, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { z } from 'zod'
import { loginByEmail } from '@/features/auth/api'
import { AuthLayout } from '@/features/auth/components/AuthLayout'
import { OAuthButtons } from '@/features/auth/components/OAuthButtons'
import { Turnstile } from '@/features/auth/components/Turnstile'
import { authToken } from '@/features/auth/token'
import { getErrorMessage } from '@/shared/api/errors'
import { Button } from '@/shared/ui/Button'
import { Input } from '@/shared/ui/Input'

const loginSchema = z.object({
  email: z.email('请输入正确的邮箱地址'),
  password: z.string().min(1, '请输入密码'),
})

type LoginForm = z.infer<typeof loginSchema>

export function LoginPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [turnstileToken, setTurnstileToken] = useState('')
  const handleTurnstile = useCallback((token: string) => {
    setTurnstileToken(token)
  }, [])
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
  })

  const loginMutation = useMutation({
    mutationFn: (values: LoginForm) =>
      loginByEmail({ ...values, turnstileToken }),
    onSuccess: (token) => {
      authToken.set(token)
      const redirect = searchParams.get('redirect')
      navigate(redirect?.startsWith('/') ? redirect : '/home', {
        replace: true,
      })
    },
  })

  return (
    <AuthLayout
      title="欢迎回来"
      description="继续你的面试准备、认证练习和学习交流。"
      footer={
        <>
          还没有账号？
          <Link className="ml-1 font-semibold text-brand" to="/register">
            创建账号
          </Link>
        </>
      }
    >
      <form
        className="space-y-4"
        onSubmit={handleSubmit((values) => loginMutation.mutate(values))}
      >
        <label className="block text-sm font-semibold text-ink">
          邮箱
          <Input
            className="mt-2"
            type="email"
            autoComplete="email"
            placeholder="name@example.com"
            {...register('email')}
          />
          {errors.email ? (
            <span className="mt-1.5 block text-xs text-danger">
              {errors.email.message}
            </span>
          ) : null}
        </label>
        <label className="block text-sm font-semibold text-ink">
          密码
          <Input
            className="mt-2"
            type="password"
            autoComplete="current-password"
            placeholder="请输入密码"
            {...register('password')}
          />
          {errors.password ? (
            <span className="mt-1.5 block text-xs text-danger">
              {errors.password.message}
            </span>
          ) : null}
        </label>
        <Turnstile onTokenChange={handleTurnstile} />
        {loginMutation.isError ? (
          <p className="rounded-xl bg-[#f8eaea] px-3 py-2 text-sm text-danger">
            {getErrorMessage(loginMutation.error)}
          </p>
        ) : null}
        <Button
          className="w-full"
          size="lg"
          type="submit"
          disabled={loginMutation.isPending}
        >
          {loginMutation.isPending ? '正在登录…' : '登录'}
          <ArrowRight className="size-4" />
        </Button>
      </form>
      <OAuthButtons turnstileToken={turnstileToken} />
    </AuthLayout>
  )
}
