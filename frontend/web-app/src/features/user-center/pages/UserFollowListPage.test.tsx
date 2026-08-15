import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { server } from '../../../../tests/msw/server'
import { UserFollowListPage } from '@/features/user-center/pages/UserFollowListPage'

function renderList(kind: 'followers' | 'following') {
  const router = createMemoryRouter(
    [
      {
        path: `/me/${kind}`,
        element: <UserFollowListPage kind={kind} />,
      },
    ],
    { initialEntries: [`/me/${kind}`] },
  )
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

describe('UserFollowListPage', () => {
  it('shows a Follow action for a follower the current user does not follow', async () => {
    server.use(
      http.get('*/api/app/user-center/follower-list', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: [
            {
              followerUserId: 8,
              followerDisplayName: '新粉丝',
              followerAvatarUrl: null,
              mutualFollow: false,
              blocked: true,
            },
          ],
        }),
      ),
    )

    renderList('followers')

    expect(await screen.findByText('新粉丝')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Follow' })).toBeInTheDocument()
  })

  it('shows Mutual for a followee who follows the current user back', async () => {
    server.use(
      http.get('*/api/app/user-center/following-list', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: [
            {
              followeeUserId: 9,
              followeeDisplayName: '互关用户',
              followeeAvatarUrl: null,
              mutualFollow: true,
              blocked: true,
            },
          ],
        }),
      ),
    )

    renderList('following')

    expect(await screen.findByText('互关用户')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Mutual' })).toBeInTheDocument()
  })
})
