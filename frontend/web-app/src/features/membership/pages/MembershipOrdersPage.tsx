import { useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Clock3, ReceiptText } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  getAllMembershipOrders,
  getPendingMembershipOrders,
} from '@/features/membership/api'
import { clearPendingOrder } from '@/features/membership/order-storage'
import type {
  AllMembershipOrder,
  MembershipOrderListItem,
  PendingMembershipOrder,
} from '@/features/membership/types'
import {
  MembershipOrderStatus,
  MembershipType,
} from '@/shared/constants/domain'
import { formatCountdown, useCountdown } from '@/shared/hooks/useCountdown'
import { formatDateTime, formatMoney } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/Badge'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { EmptyState, ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

type OrderView = 'pending' | 'all'

const pendingOrdersQueryKey = ['membership', 'orders', 'pending'] as const
const allOrdersQueryKey = ['membership', 'orders', 'all'] as const

export function MembershipOrdersPage() {
  const queryClient = useQueryClient()
  const [view, setView] = useState<OrderView>('pending')
  const pendingQuery = useQuery({
    queryKey: pendingOrdersQueryKey,
    queryFn: getPendingMembershipOrders,
    enabled: view === 'pending',
  })
  const allQuery = useQuery({
    queryKey: allOrdersQueryKey,
    queryFn: getAllMembershipOrders,
    enabled: view === 'all',
  })
  const activeQuery = view === 'pending' ? pendingQuery : allQuery

  const handlePendingOrderExpired = useCallback(
    (orderNo: string) => {
      clearPendingOrder(orderNo)
      queryClient.setQueryData<PendingMembershipOrder[]>(
        pendingOrdersQueryKey,
        (orders) => orders?.filter((order) => order.orderNo !== orderNo),
      )
      void queryClient.invalidateQueries({ queryKey: pendingOrdersQueryKey })
      void queryClient.invalidateQueries({ queryKey: allOrdersQueryKey })
    },
    [queryClient],
  )

  return (
    <div className="reading-container py-8">
      <header>
        <Button asChild variant="ghost" size="sm" className="-ml-3 mb-4">
          <Link to="/membership/center">
            <ArrowLeft className="size-4" />
            返回会员中心
          </Link>
        </Button>
        <p className="text-sm font-semibold text-brand">Membership</p>
        <h1 className="mt-1 text-3xl font-extrabold">全部订单</h1>
        <p className="mt-2 text-sm text-muted">
          继续完成待支付订单，或查看此前的会员订单记录。
        </p>
      </header>

      <div
        className="mt-6 inline-flex rounded-xl border border-line bg-surface-muted p-1"
        role="tablist"
        aria-label="订单范围"
      >
        <OrderTab
          active={view === 'pending'}
          onClick={() => setView('pending')}
        >
          待支付
        </OrderTab>
        <OrderTab active={view === 'all'} onClick={() => setView('all')}>
          全部订单
        </OrderTab>
      </div>

      <section className="mt-5" role="tabpanel">
        {activeQuery.isLoading ? <PageSkeleton /> : null}

        {activeQuery.isError ? (
          <ErrorState onRetry={() => void activeQuery.refetch()} />
        ) : null}

        {!activeQuery.isLoading && !activeQuery.isError ? (
          view === 'pending' ? (
            <PendingOrders
              orders={pendingQuery.data || []}
              onExpired={handlePendingOrderExpired}
            />
          ) : (
            <AllOrders orders={allQuery.data || []} />
          )
        ) : null}
      </section>
    </div>
  )
}

function OrderTab({
  active,
  children,
  onClick,
}: {
  active: boolean
  children: React.ReactNode
  onClick: () => void
}) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={active}
      className={`rounded-lg px-4 py-2 text-sm font-bold transition-colors active:scale-[0.98] ${
        active ? 'bg-surface text-ink shadow-sm' : 'text-muted hover:text-ink'
      }`}
      onClick={onClick}
    >
      {children}
    </button>
  )
}

function PendingOrders({
  orders,
  onExpired,
}: {
  orders: PendingMembershipOrder[]
  onExpired: (orderNo: string) => void
}) {
  if (!orders.length) {
    return (
      <Card>
        <EmptyState
          title="没有待支付订单"
          description="新创建的会员订单会显示在这里。"
        />
      </Card>
    )
  }

  return (
    <div className="space-y-3">
      {orders.map((order) => (
        <PendingOrderCard
          key={order.orderNo}
          order={order}
          onExpired={onExpired}
        />
      ))}
    </div>
  )
}

function PendingOrderCard({
  order,
  onExpired,
}: {
  order: PendingMembershipOrder
  onExpired: (orderNo: string) => void
}) {
  const secondsLeft = useCountdown(order.paymentExpiredTime)
  const expired = secondsLeft <= 0

  useEffect(() => {
    if (expired) onExpired(order.orderNo)
  }, [expired, onExpired, order.orderNo])

  if (expired) return null

  return (
    <OrderCard order={order}>
      <div>
        <p className="text-lg font-extrabold">
          {formatMoney(order.payAmount, order.currency)}
        </p>
        <p className="mt-1 inline-flex items-center gap-1.5 text-xs font-semibold text-warning">
          <Clock3 className="size-3.5" />
          剩余 {formatCountdown(secondsLeft)}
        </p>
      </div>
      <Button asChild variant="secondary" size="sm">
        <Link to={`/membership/orders/${order.orderNo}`}>继续支付</Link>
      </Button>
    </OrderCard>
  )
}

function AllOrders({ orders }: { orders: AllMembershipOrder[] }) {
  if (!orders.length) {
    return (
      <Card>
        <EmptyState
          title="还没有会员订单"
          description="选择套餐后，订单记录会显示在这里。"
        />
      </Card>
    )
  }

  return (
    <div className="space-y-3">
      {orders.map((order) => (
        <OrderCard key={order.orderNo} order={order}>
          <div>
            <p className="text-lg font-extrabold">
              {formatMoney(order.payAmount, order.currency)}
            </p>
            <p className="mt-1 text-xs text-muted">
              {order.payTime
                ? `支付于 ${formatDateTime(order.payTime)}`
                : order.orderStatus === MembershipOrderStatus.PENDING
                  ? '尚未支付'
                  : '未完成支付'}
            </p>
          </div>
          {order.orderStatus === MembershipOrderStatus.PENDING ? (
            <Button asChild variant="secondary" size="sm">
              <Link to={`/membership/orders/${order.orderNo}`}>查看订单</Link>
            </Button>
          ) : null}
        </OrderCard>
      ))}
    </div>
  )
}

function OrderCard({
  order,
  children,
}: {
  order: MembershipOrderListItem
  children: React.ReactNode
}) {
  return (
    <Card className="p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex gap-3">
          <span className="flex size-10 items-center justify-center rounded-xl bg-brand-soft text-brand">
            <ReceiptText className="size-4" />
          </span>
          <div>
            <h2 className="font-bold">
              {order.membershipType === MembershipType.PREMIUM_PLUS
                ? 'Premium Plus'
                : 'Premium'}{' '}
              · {order.durationMonths} 个月
            </h2>
            <p className="mt-1 text-xs text-muted">{order.orderNo}</p>
          </div>
        </div>
        <StatusBadge status={order.orderStatus} />
      </div>
      <div className="mt-4 flex flex-wrap items-end justify-between gap-3 border-t border-line pt-4">
        {children}
      </div>
    </Card>
  )
}

function StatusBadge({ status }: { status: number }) {
  const copy = {
    [MembershipOrderStatus.PENDING]: [
      '待支付',
      'text-warning bg-warning-soft border-[#edcf9b]',
    ],
    [MembershipOrderStatus.PAID]: [
      '已支付',
      'text-success bg-success-soft border-[#add0c3]',
    ],
    [MembershipOrderStatus.EXPIRED]: [
      '已超时',
      'text-muted bg-surface-muted border-line',
    ],
    [MembershipOrderStatus.PAY_FAILED]: [
      '支付失败',
      'text-danger bg-danger-soft border-[#e3b9b9]',
    ],
  } as const
  const [label, className] = copy[status as keyof typeof copy] || [
    '未知状态',
    'text-muted',
  ]
  return <Badge className={className}>{label}</Badge>
}
