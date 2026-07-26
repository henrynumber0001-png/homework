import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { server } from '../../../../tests/msw/server'
import { MembershipCenterPage } from '@/features/membership/pages/MembershipCenterPage'

describe('MembershipCenterPage', () => {
  it('provides an explicit way back to the personal center', async () => {
    server.use(
      http.get('*/api/app/membership/center', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: {
            displayName: 'Henry',
            avatarUrl: null,
            membershipType: 1,
            expiredTime: '2027-01-01T00:00:00',
            memberStatus: 1,
            baseFreezeExpireTime: null,
          },
        }),
      ),
    )
    const router = createMemoryRouter(
      [
        { path: '/membership/center', element: <MembershipCenterPage /> },
        { path: '/me', element: <h1>个人中心</h1> },
      ],
      { initialEntries: ['/membership/center'] },
    )
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>,
    )

    expect(
      await screen.findByRole('link', { name: '返回个人中心' }),
    ).toHaveAttribute('href', '/me')
  })
})
