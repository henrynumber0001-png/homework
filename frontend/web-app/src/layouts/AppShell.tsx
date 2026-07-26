import {
  Bell,
  BookOpen,
  GraduationCap,
  Hash,
  Home,
  LogOut,
  Menu,
  UserRound,
} from 'lucide-react'
import * as DropdownMenu from '@radix-ui/react-dropdown-menu'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { queryClient } from '@/app/query-client'
import { authToken } from '@/features/auth/token'
import { cn } from '@/shared/lib/cn'
import { useLearningHeartbeat } from '@/shared/hooks/useLearningHeartbeat'
import {
  useCurrentUser,
  useMembershipCenter,
  useUnreadSummary,
} from '@/shared/queries/session'
import { Avatar } from '@/shared/ui/Avatar'
import { Badge } from '@/shared/ui/Badge'

const navItems = [
  { to: '/home', label: '首页', icon: Home },
  { to: '/banks/interview', label: '面试题库', icon: BookOpen },
  { to: '/banks/certification', label: '认证题库', icon: GraduationCap },
  { to: '/hits', label: '#Hit', icon: Hash },
  { to: '/me', label: '个人中心', icon: UserRound },
] as const

export function AppShell() {
  useLearningHeartbeat()
  const navigate = useNavigate()
  const userQuery = useCurrentUser()
  const membershipQuery = useMembershipCenter()
  const unreadQuery = useUnreadSummary()
  const unreadTotal = unreadQuery.data
    ? unreadQuery.data.commentsAndMentions +
      unreadQuery.data.interactions +
      unreadQuery.data.system +
      unreadQuery.data.privateMessages
    : 0

  const logout = () => {
    authToken.clear()
    queryClient.clear()
    navigate('/login', { replace: true })
  }

  return (
    <div className="min-h-screen">
      <header className="fixed inset-x-0 top-0 z-40 h-16 border-b border-line/90 bg-surface/95 backdrop-blur">
        <div className="app-container flex h-full items-center">
          <NavLink to="/home" className="flex shrink-0 items-center gap-2">
            <span className="flex size-9 items-center justify-center rounded-xl bg-brand text-lg font-black text-white">
              H
            </span>
            <span className="hidden text-lg font-extrabold tracking-tight sm:block">
              HomeWork
            </span>
          </NavLink>

          <nav
            className="ml-10 hidden h-full items-center gap-1 md:flex"
            aria-label="主导航"
          >
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  cn(
                    'relative flex h-full items-center px-3 text-sm font-semibold text-muted transition hover:text-ink',
                    isActive &&
                      'text-brand after:absolute after:inset-x-3 after:bottom-0 after:h-0.5 after:rounded-full after:bg-brand',
                  )
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="ml-auto flex items-center gap-2">
            <NavLink
              to="/messages"
              aria-label={
                unreadTotal ? `我的消息，${unreadTotal}条未读` : '我的消息'
              }
              className="relative flex size-10 items-center justify-center rounded-xl text-muted transition hover:bg-[#eee7e1] hover:text-ink"
            >
              <Bell className="size-5" />
              {unreadTotal ? (
                <span className="absolute right-1 top-1 min-w-4 rounded-full bg-danger px-1 text-center text-[10px] font-bold leading-4 text-white">
                  {unreadTotal > 99 ? '99+' : unreadTotal}
                </span>
              ) : null}
            </NavLink>

            <MembershipBadge
              status={membershipQuery.data?.memberStatus}
              isLoading={membershipQuery.isLoading}
            />

            <DropdownMenu.Root>
              <DropdownMenu.Trigger className="flex items-center gap-2 rounded-xl p-1.5 text-left transition hover:bg-[#eee7e1]">
                <Avatar
                  src={userQuery.data?.avatar}
                  name={userQuery.data?.displayName}
                  className="size-8"
                />
                <span className="hidden max-w-28 truncate text-sm font-semibold lg:block">
                  {userQuery.data?.displayName || '加载中…'}
                </span>
                <Menu className="hidden size-4 text-muted lg:block" />
              </DropdownMenu.Trigger>
              <DropdownMenu.Portal>
                <DropdownMenu.Content
                  align="end"
                  sideOffset={8}
                  className="z-50 min-w-48 rounded-xl border border-line bg-surface p-1.5 shadow-xl"
                >
                  <MenuLink to="/me">个人中心</MenuLink>
                  <MenuLink to="/membership/center">会员中心</MenuLink>
                  <MenuLink to="/messages">我的消息</MenuLink>
                  <MenuLink to="/membership/orders">订单历史</MenuLink>
                  <DropdownMenu.Separator className="my-1 h-px bg-line" />
                  <DropdownMenu.Item
                    onSelect={logout}
                    className="flex cursor-pointer items-center gap-2 rounded-lg px-3 py-2 text-sm text-danger outline-none hover:bg-[#f8eaea]"
                  >
                    <LogOut className="size-4" />
                    退出登录
                  </DropdownMenu.Item>
                </DropdownMenu.Content>
              </DropdownMenu.Portal>
            </DropdownMenu.Root>
          </div>
        </div>
      </header>

      <main className="pb-24 pt-16 md:pb-10">
        <Outlet />
      </main>

      <nav
        className="fixed inset-x-0 bottom-0 z-40 grid h-[4.5rem] grid-cols-5 border-t border-line bg-surface/97 px-1 pb-[env(safe-area-inset-bottom)] backdrop-blur md:hidden"
        aria-label="移动端主导航"
      >
        {navItems.map((item) => {
          const Icon = item.icon
          return (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                cn(
                  'flex flex-col items-center justify-center gap-1 text-[11px] font-medium text-muted',
                  isActive && 'text-brand',
                )
              }
            >
              <Icon className="size-5" />
              {item.label}
            </NavLink>
          )
        })}
      </nav>
    </div>
  )
}

function MenuLink({ to, children }: { to: string; children: React.ReactNode }) {
  const navigate = useNavigate()
  return (
    <DropdownMenu.Item
      onSelect={() => navigate(to)}
      className="cursor-pointer rounded-lg px-3 py-2 text-sm text-ink outline-none hover:bg-[#eee7e1]"
    >
      {children}
    </DropdownMenu.Item>
  )
}

function MembershipBadge({
  status,
  isLoading,
}: {
  status?: number
  isLoading: boolean
}) {
  if (isLoading) {
    return (
      <span className="h-7 w-16 animate-pulse rounded-full bg-[#eee7e1] sm:w-20" />
    )
  }

  if (status === 2) {
    return (
      <Badge className="border-[#d4b46d] bg-[#fff7dd] text-[#7f5b15]">
        Premium Plus
      </Badge>
    )
  }

  if (status === 1) {
    return (
      <Badge className="border-[#dfc98f] bg-[#fff8e8] text-premium">
        Premium
      </Badge>
    )
  }

  return <Badge>Free</Badge>
}
