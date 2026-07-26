import { useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, Clock3, QrCode, XCircle } from 'lucide-react'
import { QRCodeSVG } from 'qrcode.react'
import { useEffect } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getMembershipOrderStatus } from '@/features/membership/api'
import {
  clearPendingOrder,
  getPendingOrder,
} from '@/features/membership/order-storage'
import { MembershipOrderStatus } from '@/shared/constants/domain'
import { formatDateTime, formatMoney } from '@/shared/lib/format'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

export function MembershipOrderPage() {
  const queryClient = useQueryClient()
  const { orderNo = '' } = useParams()
  const order = getPendingOrder(orderNo)
  const statusQuery = useQuery({
    queryKey: ['membership', 'order', orderNo],
    queryFn: () => getMembershipOrderStatus(orderNo),
    refetchInterval: (query) => {
      const status = query.state.data
      return status == null || status === MembershipOrderStatus.PENDING
        ? 2_000
        : false
    },
  })

  useEffect(() => {
    if (statusQuery.data === MembershipOrderStatus.PAID) {
      clearPendingOrder(orderNo)
      void queryClient.invalidateQueries({ queryKey: ['membership'] })
      void queryClient.invalidateQueries({ queryKey: ['current-user'] })
      void queryClient.invalidateQueries({ queryKey: ['user-center'] })
    }
    if (
      statusQuery.data === MembershipOrderStatus.EXPIRED ||
      statusQuery.data === MembershipOrderStatus.PAY_FAILED
    ) {
      clearPendingOrder(orderNo)
    }
  }, [orderNo, queryClient, statusQuery.data])

  if (statusQuery.isLoading) {
    return (
      <div className="reading-container py-8">
        <PageSkeleton />
      </div>
    )
  }

  if (statusQuery.isError || statusQuery.data == null) {
    return (
      <div className="reading-container py-8">
        <ErrorState onRetry={() => void statusQuery.refetch()} />
      </div>
    )
  }

  const status = statusQuery.data

  if (status === MembershipOrderStatus.PAID) {
    return (
      <OrderResult
        success
        title="支付成功"
        description="会员权益已经由服务端确认并发放。"
      />
    )
  }

  if (
    status === MembershipOrderStatus.EXPIRED ||
    status === MembershipOrderStatus.PAY_FAILED
  ) {
    return (
      <OrderResult
        success={false}
        title={
          status === MembershipOrderStatus.EXPIRED ? '订单已超时' : '支付未完成'
        }
        description="请返回会员页重新选择套餐并创建订单。"
      />
    )
  }

  return (
    <div className="reading-container py-8">
      <header className="text-center">
        <p className="text-sm font-semibold text-brand">微信支付</p>
        <h1 className="mt-1 text-3xl font-extrabold">扫码完成支付</h1>
        <p className="mt-2 text-sm text-muted">订单号：{orderNo}</p>
      </header>

      <Card className="mt-6 p-6 text-center">
        {order?.codeUrl ? (
          <>
            <div className="mx-auto w-fit rounded-2xl border border-line bg-white p-4">
              <QRCodeSVG
                value={order.codeUrl}
                size={220}
                level="M"
                aria-label="微信支付二维码"
              />
            </div>
            <p className="mt-5 text-2xl font-extrabold">
              {formatMoney(order.amountDue, order.currency)}
            </p>
            <p className="mt-2 inline-flex items-center gap-1.5 text-sm text-warning">
              <Clock3 className="size-4" />
              请在 {formatDateTime(order.paymentExpiredTime)} 前完成
            </p>
          </>
        ) : (
          <div className="py-8">
            <QrCode className="mx-auto size-9 text-muted" />
            <p className="mt-4 font-bold">支付二维码不在当前浏览器会话中</p>
            <p className="mt-2 text-sm leading-6 text-muted">
              订单仍处于待支付状态，请从最初创建订单的页面完成扫码。
            </p>
          </div>
        )}
        <p className="mt-5 text-xs text-muted">
          页面每 2 秒自动查询支付状态，无需手动确认。
        </p>
      </Card>
    </div>
  )
}

function OrderResult({
  success,
  title,
  description,
}: {
  success: boolean
  title: string
  description: string
}) {
  return (
    <div className="reading-container py-10">
      <Card className="p-8 text-center">
        {success ? (
          <CheckCircle2 className="mx-auto size-11 text-success" />
        ) : (
          <XCircle className="mx-auto size-11 text-danger" />
        )}
        <h1 className="mt-4 text-2xl font-extrabold">{title}</h1>
        <p className="mt-2 text-sm text-muted">{description}</p>
        <div className="mt-6 flex justify-center gap-3">
          <Button asChild>
            <Link to={success ? '/membership/center' : '/membership'}>
              {success ? '进入会员中心' : '返回会员页'}
            </Link>
          </Button>
          <Button asChild variant="secondary">
            <Link to="/membership/orders">查看订单</Link>
          </Button>
        </div>
      </Card>
    </div>
  )
}
