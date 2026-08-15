import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { server } from '../../../../tests/msw/server'
import { PublicUserProfilePage } from '@/features/user-profile/pages/PublicUserProfilePage'

function renderPage({
  self = false,
  blocked = false,
  followedByCurrentUser = false,
  mutualFollow = false,
}: {
  self?: boolean
  blocked?: boolean
  followedByCurrentUser?: boolean
  mutualFollow?: boolean
} = {}) {
  server.use(
    http.get('*/api/app/users/8/profile', () =>
      HttpResponse.json({
        code: 200,
        message: 'success',
        data: {
          userId: 8,
          userInfo: {
            accountNo: 'HW000008',
            displayName: '测试用户',
            avatarUrl: null,
            bannerUrl: 'https://example.com/public-banner.jpg',
            companyOrSchool: 'HomeWork 大学',
            subTechDirectionId: 101,
            gender: 1,
            introduction: '正在系统学习后端开发。',
          },
          membershipStatus: 1,
          membershipType: null,
          followerCount: 12,
          followingCount: 5,
          answeredQuestionCount: 20,
          learnedBankCount: 3,
          studyHours: 8,
          self,
          followedByCurrentUser: self ? null : followedByCurrentUser,
          mutualFollow,
          blocked,
          canSendPrivateMessage: !self && !blocked,
          chatboxId: null,
        },
      }),
    ),
    http.get('*/api/app/users/8/profile/posts', () =>
      HttpResponse.json({ code: 200, message: 'success', data: [] }),
    ),
    http.get('*/api/app/user-center/profile-info/options', () =>
      HttpResponse.json({
        code: 200,
        message: 'success',
        data: {
          techDirectionTreeVOList: [
            {
              directionId: 1,
              directionName: '后端开发',
              subTechDirectionTreeVOList: [
                {
                  subTechDirectionId: 101,
                  subTechDirectionName: 'Java后端',
                },
              ],
            },
          ],
        },
      }),
    ),
  )

  const router = createMemoryRouter(
    [{ path: '/users/:userId', element: <PublicUserProfilePage /> }],
    { initialEntries: ['/users/8'] },
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

describe('PublicUserProfilePage', () => {
  it('shows the public profile fields with the avatar above the banner layer', async () => {
    renderPage()

    expect(
      await screen.findByRole('heading', { name: '测试用户' }),
    ).toBeInTheDocument()
    expect(screen.getByText('@HW000008')).toBeInTheDocument()
    expect(screen.getByText('正在系统学习后端开发。')).toBeInTheDocument()
    expect(screen.getByText('男')).toBeInTheDocument()
    expect(await screen.findByText('Java后端')).toBeInTheDocument()
    expect(screen.getByText('HomeWork 大学')).toBeInTheDocument()
    expect(screen.getByRole('img', { name: '测试用户的主页封面' })).toHaveClass(
      'z-0',
    )
    expect(screen.getByTestId('profile-avatar-layer')).toHaveClass('z-20')
  })

  it('shows Follow when neither user has blocked the other', async () => {
    renderPage()

    expect(
      await screen.findByRole('button', { name: 'Follow' }),
    ).toBeInTheDocument()
  })

  it('shows Following when the current user already follows the profile user', async () => {
    renderPage({ followedByCurrentUser: true })

    expect(
      await screen.findByRole('button', { name: 'Following' }),
    ).toBeInTheDocument()
  })

  it('shows Mutual instead of Following when both users follow each other', async () => {
    renderPage({ followedByCurrentUser: true, mutualFollow: true })

    expect(
      await screen.findByRole('button', { name: 'Mutual' }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Following' }),
    ).not.toBeInTheDocument()
  })

  it('keeps Follow available but hides private and restricted content when blocked', async () => {
    renderPage({ blocked: true })

    expect(
      await screen.findByRole('button', { name: 'Follow' }),
    ).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: '私信' })).not.toBeInTheDocument()
    expect(screen.queryByText('累计作答')).not.toBeInTheDocument()
    expect(screen.queryByText('正在系统学习后端开发。')).not.toBeInTheDocument()
    expect(screen.queryByText('HomeWork 大学')).not.toBeInTheDocument()
    expect(
      screen.queryByRole('heading', { name: 'Posts' }),
    ).not.toBeInTheDocument()
  })

  it('keeps Mutual available when the users are blocked but still follow each other', async () => {
    renderPage({
      blocked: true,
      followedByCurrentUser: true,
      mutualFollow: true,
    })

    expect(
      await screen.findByRole('button', { name: 'Mutual' }),
    ).toBeInTheDocument()
  })

  it("hides Follow and Following on the current user's own profile", async () => {
    renderPage({ self: true })

    await screen.findByRole('heading', { name: '测试用户' })
    expect(
      screen.queryByRole('button', { name: 'Follow' }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Following' }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Mutual' }),
    ).not.toBeInTheDocument()
  })
})
