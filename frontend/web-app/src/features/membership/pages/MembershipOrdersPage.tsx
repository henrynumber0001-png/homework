import { useQuery } from '@tanstack/react-query'
import { ReceiptText } from 'lucide-react'
import { Link } from 'react-router-dom'
import { getMembershipOrderHistory } from '@/features/membership/api'
import {
  MembershipOrderStatus,
  MembershipType,
} from '@/shared/constants/domain'
import { formatDateTime, formatMoney } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/Badge'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { EmptyState, ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

export function MembershipOrdersPage() {
  const ordersQuery = useQuery({
    queryKey: ['membership', 'orders'],
    queryFn: getMembershipOrderHistory,
  })

  if (ordersQuery.isLoading) {
    return (
      <div className="reading-container py-8">
        <PageSkeleton />
      </div>
    )
  }

  if (ordersQuery.isError) {
    return (
      <div className="reading-container py-8">
        <ErrorState onRetry={() => void ordersQuery.refetch()} />
      </div>
    )
  }

  const orders = ordersQuery.data || []

  return (
    <div className="reading-container py-8">
      <header>
        <p className="text-sm font-semibold text-brand">Membership</p>
        <h1 className="mt-1 text-3xl font-extrabold">订单历史</h1>
      </header>

      {orders.length ? (
        <div className="mt-6 space-y-3">
          {orders.map((order) => (
            <Card key={order.orderNo} className="p-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="flex gap-3">
                  <span className="flex size-10 items-center justify-center rounded-xl bg-[#eee7e1] text-brand">
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
                <div>
                  <p className="text-lg font-extrabold">
                    {formatMoney(order.payAmount, order.currency)}
                  </p>
                  <p className="mt-1 text-xs text-muted">
                    {order.payTime
                      ? `支付于 ${formatDateTime(order.payTime)}`
                      : '尚未支付'}
                  </p>
                </div>
                {order.orderStatus === MembershipOrderStatus.PENDING ? (
                  <Button asChild variant="secondary" size="sm">
                    <Link to={`/membership/orders/${order.orderNo}`}>
                      查看支付状态
                    </Link>
                  </Button>
                ) : null}
              </div>
            </Card>
          ))}
        </div>
      ) : (
        <Card className="mt-6">
          <EmptyState
            title="还没有会员订单"
            description="选择套餐后，订单记录会显示在这里。"
          />
        </Card>
      )}
    </div>
  )
}

function StatusBadge({ status }: { status: number }) {
  const copy = {
    [MembershipOrderStatus.PENDING]: [
      '待支付',
      'text-warning bg-[#fff4e3] border-[#edcf9b]',
    ],
    [MembershipOrderStatus.PAID]: [
      '已支付',
      'text-success bg-[#f0f7f4] border-[#add0c3]',
    ],
    [MembershipOrderStatus.EXPIRED]: [
      '已超时',
      'text-muted bg-[#f3efec] border-line',
    ],
    [MembershipOrderStatus.PAY_FAILED]: [
      '支付失败',
      'text-danger bg-[#fbf0f0] border-[#e3b9b9]',
    ],
  } as const
  const [label, className] = copy[status as keyof typeof copy] || [
    '未知状态',
    'text-muted',
  ]
  return <Badge className={className}>{label}</Badge>
}
