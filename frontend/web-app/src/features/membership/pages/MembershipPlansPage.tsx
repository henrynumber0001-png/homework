import { useMutation, useQuery } from '@tanstack/react-query'
import { ArrowRight, Check, Crown, ShieldCheck, Sparkles } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  createMembershipOrder,
  getMembershipPlans,
} from '@/features/membership/api'
import { savePendingOrder } from '@/features/membership/order-storage'
import type { MembershipSku } from '@/features/membership/types'
import { MembershipType } from '@/shared/constants/domain'
import { formatDateTime, formatMoney } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/Badge'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

interface PurchaseIntent {
  sku: MembershipSku
  idempotencyKey: string
}

export function MembershipPlansPage() {
  const navigate = useNavigate()
  const [intent, setIntent] = useState<PurchaseIntent | null>(null)
  const plansQuery = useQuery({
    queryKey: ['membership', 'plans'],
    queryFn: getMembershipPlans,
  })
  const orderMutation = useMutation({
    mutationFn: (purchaseIntent: PurchaseIntent) =>
      createMembershipOrder(
        purchaseIntent.sku.planId,
        purchaseIntent.idempotencyKey,
      ),
    onSuccess: (order) => {
      savePendingOrder(order)
      navigate(`/membership/orders/${order.orderNo}`)
    },
  })

  const chooseSku = (sku: MembershipSku) => {
    const nextIntent =
      intent?.sku.planId === sku.planId
        ? intent
        : { sku, idempotencyKey: crypto.randomUUID() }
    setIntent(nextIntent)
    orderMutation.mutate(nextIntent)
  }

  if (plansQuery.isLoading) {
    return (
      <div className="app-container py-8">
        <PageSkeleton />
      </div>
    )
  }

  if (plansQuery.isError || !plansQuery.data) {
    return (
      <div className="app-container py-8">
        <ErrorState onRetry={() => void plansQuery.refetch()} />
      </div>
    )
  }

  const data = plansQuery.data

  return (
    <div className="app-container py-8">
      <section className="relative overflow-hidden rounded-2xl border border-[#d8c18a] bg-[#fff8e7] px-6 py-9 text-center sm:px-8">
        <div className="absolute -left-10 -top-20 size-60 rounded-full border-[32px] border-[#d9bd77]/12" />
        <Crown className="relative mx-auto size-8 text-premium" />
        <p className="relative mt-3 text-sm font-bold text-premium">
          HomeWork Membership
        </p>
        <h1 className="relative mt-2 text-3xl font-black tracking-tight sm:text-4xl">
          选择适合当前阶段的会员
        </h1>
        <p className="relative mx-auto mt-3 max-w-2xl text-sm leading-6 text-muted">
          套餐、价格、购买月份与升级范围全部由当前后端配置实时提供。
        </p>
        {data.currentExpireTime ? (
          <p className="relative mt-4 text-xs text-muted">
            当前会员到期时间：{formatDateTime(data.currentExpireTime)}
          </p>
        ) : null}
      </section>

      <div className="mt-7 grid gap-5 lg:grid-cols-2">
        {data.fullPurchaseCards.map((card) => {
          const plus = card.membershipType === MembershipType.PREMIUM_PLUS
          return (
            <Card
              key={card.membershipType}
              className={plus ? 'border-[#d8c18a] p-6' : 'p-6'}
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-xs font-bold text-muted">会员等级</p>
                  <h2 className="mt-1 text-2xl font-extrabold">
                    {plus ? 'Premium Plus' : 'Premium'}
                  </h2>
                </div>
                <span
                  className={
                    plus
                      ? 'flex size-11 items-center justify-center rounded-xl bg-[#fff1c7] text-premium'
                      : 'flex size-11 items-center justify-center rounded-xl bg-[#e9f0ed] text-accent'
                  }
                >
                  {plus ? (
                    <Sparkles className="size-5" />
                  ) : (
                    <ShieldCheck className="size-5" />
                  )}
                </span>
              </div>
              <ul className="mt-5 space-y-2 text-sm text-muted">
                <li className="flex items-center gap-2">
                  <Check className="size-4 text-success" />
                  使用该等级当前已开放的学习能力
                </li>
                <li className="flex items-center gap-2">
                  <Check className="size-4 text-success" />
                  会员状态与权益由服务端统一判断
                </li>
              </ul>
              <div className="mt-6 grid gap-3 sm:grid-cols-3">
                {card.fullPurchaseOptions.map((sku) => (
                  <SkuButton
                    key={sku.planId}
                    sku={sku}
                    pending={
                      orderMutation.isPending &&
                      intent?.sku.planId === sku.planId
                    }
                    onClick={() => chooseSku(sku)}
                  />
                ))}
              </div>
            </Card>
          )
        })}
      </div>

      {data.diffUpgradeAvailable && data.diffUpgradeOptions.length ? (
        <Card className="mt-6 p-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <Badge className="border-[#d8c18a] bg-[#fff8e7] text-premium">
                补差升级
              </Badge>
              <h2 className="mt-3 text-xl font-extrabold">
                升级至 Premium Plus
              </h2>
              <p className="mt-2 text-sm text-muted">
                当前最多可选择 {data.maxDiffUpgradeMonths} 个月的补差档位。
              </p>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              {data.diffUpgradeOptions.map((sku) => (
                <SkuButton
                  key={sku.planId}
                  sku={sku}
                  pending={
                    orderMutation.isPending && intent?.sku.planId === sku.planId
                  }
                  onClick={() => chooseSku(sku)}
                />
              ))}
            </div>
          </div>
        </Card>
      ) : null}

      {orderMutation.isError ? (
        <Card className="mt-5 border-[#e3b9b9] bg-[#fbf0f0] p-4 text-sm text-danger">
          订单创建失败。再次点击同一套餐会复用本次购买标识，不会重复建单。
        </Card>
      ) : null}

      <div className="mt-6 flex justify-center">
        <Button asChild variant="ghost">
          <Link to="/membership/orders">
            查看历史订单
            <ArrowRight className="size-4" />
          </Link>
        </Button>
      </div>
    </div>
  )
}

function SkuButton({
  sku,
  pending,
  onClick,
}: {
  sku: MembershipSku
  pending: boolean
  onClick: () => void
}) {
  return (
    <button
      type="button"
      disabled={pending}
      className="rounded-xl border border-line bg-white p-3 text-left transition hover:border-premium hover:bg-[#fffcf3] disabled:opacity-60"
      onClick={onClick}
    >
      <span className="block text-xs text-muted">
        {sku.durationMonths} 个月
      </span>
      <span className="mt-1 block text-lg font-extrabold">
        {formatMoney(sku.price, sku.currency)}
      </span>
      <span className="mt-2 block text-xs font-semibold text-premium">
        {pending ? '正在创建订单…' : '选择套餐'}
      </span>
    </button>
  )
}
