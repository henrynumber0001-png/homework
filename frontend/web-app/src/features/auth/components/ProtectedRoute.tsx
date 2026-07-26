import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { authToken } from '@/features/auth/token'

export function ProtectedRoute() {
  const location = useLocation()

  if (!authToken.get()) {
    const redirect = encodeURIComponent(
      `${location.pathname}${location.search}`,
    )
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }

  return <Outlet />
}
