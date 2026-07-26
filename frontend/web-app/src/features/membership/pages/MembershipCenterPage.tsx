import { ArrowLeft, Crown, ShieldCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { MembershipType } from '@/shared/constants/domain'
import { formatDateTime } from '@/shared/lib/format'
import { useMembershipCenter } from '@/shared/queries/session'
import { Avatar } from '@/shared/ui/Avatar'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

export function MembershipCenterPage() {
  const membershipQuery = useMembershipCenter()

  if (membershipQuery.isLoading) {
    return (
      <div className="reading-container py-8">
        <PageSkeleton />
      </div>
    )
  }

  if (membershipQuery.isError || !membershipQuery.data) {
    return (
      <div className="reading-container py-8">
        <ErrorState onRetry={() => void membershipQuery.refetch()} />
      </div>
    )
  }

  const membership = membershipQuery.data
  const active = membership.memberStatus !== 0

  return (
    <div className="reading-container py-8">
      <header>
        <Button asChild variant="ghost" size="sm" className="-ml-3 mb-4">
          <Link to="/me">
            <ArrowLeft className="size-4" />
            返回个人中心
          </Link>
        </Button>
        <p className="text-sm font-semibold text-brand">Membership</p>
        <h1 className="mt-1 text-3xl font-extrabold">会员中心</h1>
      </header>
      <Card className="mt-6 overflow-hidden">
        <div className={active ? 'bg-[#fff6da] p-7' : 'bg-[#eee7e1] p-7'}>
          <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
            <Avatar
              src={membership.avatarUrl}
              name={membership.displayName}
              className="size-16"
            />
            <div className="flex-1">
              <p className="text-sm text-muted">{membership.displayName}</p>
              <h2 className="mt-1 flex items-center gap-2 text-2xl font-extrabold">
                {active ? (
                  <Crown className="size-6 text-premium" />
                ) : (
                  <ShieldCheck className="size-6 text-brand" />
                )}
                {membership.membershipType === MembershipType.PREMIUM_PLUS
                  ? 'Premium Plus'
                  : membership.membershipType === MembershipType.PREMIUM
                    ? 'Premium'
                    : 'Free'}
              </h2>
              <p className="mt-2 text-sm text-muted">
                {membership.expiredTime
                  ? `有效期至 ${formatDateTime(membership.expiredTime)}`
                  : '当前没有有效会员'}
              </p>
            </div>
          </div>
        </div>
        <div className="flex flex-wrap gap-3 p-6">
          <Button asChild>
            <Link to="/membership">
              {active ? '查看升级选项' : '选择会员套餐'}
            </Link>
          </Button>
          <Button asChild variant="secondary">
            <Link to="/membership/orders">订单历史</Link>
          </Button>
        </div>
      </Card>
    </div>
  )
}
