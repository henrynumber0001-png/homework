import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { server } from '../../../../tests/msw/server'
import { MembershipOrdersPage } from '@/features/membership/pages/MembershipOrdersPage'
import { MembershipPlansPage } from '@/features/membership/pages/MembershipPlansPage'

function renderPage(path: string, element: React.ReactNode) {
  const router = createMemoryRouter(
    [
      { path, element },
      { path: '/membership/center', element: <h1>会员中心</h1> },
    ],
    { initialEntries: [path] },
  )
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

describe('membership child page navigation', () => {
  it('allows returning from the upgrade options page', async () => {
    server.use(
      http.get('*/api/app/membership', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: {
            memberStatus: 0,
            currentMembershipType: null,
            currentExpireTime: null,
            baseFreezeExpireTime: null,
            fullPurchaseCards: [],
            diffUpgradeAvailable: false,
            maxDiffUpgradeMonths: 0,
            diffUpgradeOptions: [],
          },
        }),
      ),
    )

    renderPage('/membership', <MembershipPlansPage />)

    expect(
      await screen.findByRole('link', { name: '返回会员中心' }),
    ).toHaveAttribute('href', '/membership/center')
  })

  it('allows returning from the order history page', async () => {
    server.use(
      http.get('*/api/app/membership/orders', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: [],
        }),
      ),
    )

    renderPage('/membership/orders', <MembershipOrdersPage />)

    expect(
      await screen.findByRole('link', { name: '返回会员中心' }),
    ).toHaveAttribute('href', '/membership/center')
  })
})
