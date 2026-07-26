import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { server } from '../../tests/msw/server'
import { AppShell } from '@/layouts/AppShell'

describe('AppShell', () => {
  it('keeps all primary navigation entries around protected content', async () => {
    localStorage.setItem('homework_access_token', 'test-token')
    server.use(
      http.get('*/api/app/user/info', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: {
            accountNo: 'HW000001',
            displayName: 'Henry',
            avatar: null,
          },
        }),
      ),
      http.get('*/api/app/membership/center', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: {
            displayName: 'Henry',
            avatarUrl: null,
            membershipType: null,
            expiredTime: null,
            memberStatus: 0,
            baseFreezeExpireTime: null,
          },
        }),
      ),
      http.get('*/api/app/messages/unread-summary', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: {
            commentsAndMentions: 1,
            interactions: 2,
            system: 0,
            privateMessages: 0,
            total: 3,
          },
        }),
      ),
    )

    const router = createMemoryRouter(
      [
        {
          element: <AppShell />,
          children: [{ path: '/home', element: <h1>首页内容</h1> }],
        },
      ],
      { initialEntries: ['/home'] },
    )
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>,
    )

    expect(await screen.findByText('Henry')).toBeInTheDocument()
    expect(screen.getAllByText('首页').length).toBeGreaterThan(0)
    expect(screen.getAllByText('面试题库').length).toBeGreaterThan(0)
    expect(screen.getAllByText('认证题库').length).toBeGreaterThan(0)
    expect(screen.getAllByText('#Hit').length).toBeGreaterThan(0)
    expect(screen.getAllByText('个人中心').length).toBeGreaterThan(0)
    expect(screen.getByText('首页内容')).toBeInTheDocument()
  })
})
