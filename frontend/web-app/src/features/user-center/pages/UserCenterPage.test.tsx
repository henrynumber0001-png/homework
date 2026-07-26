import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { server } from '../../../../tests/msw/server'
import { UserCenterPage } from '@/features/user-center/pages/UserCenterPage'

describe('UserCenterPage', () => {
  it('uses graphInfoVO as an editable banner and keeps numeric stats borderless', async () => {
    server.use(
      http.get('*/api/app/user-center', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: {
            graphInfoVO: {
              url: 'https://example.com/banner.jpg',
              name: '把每次练习变成看得见的成长',
            },
            userInfoVO: {
              accountNo: 'HW000001',
              displayName: 'Henry',
              avatar: null,
            },
            membershipActive: true,
            membershipType: 1,
            aiFeaturesEnabled: false,
            countsVO: {
              followerCount: 12,
              followingCount: 8,
              postCount: 4,
              answeredQuestionCount: 168,
              learnedBankCount: 9,
              studyHours: 23,
              wrongQuestionCount: 18,
              favoriteQuestionCount: 21,
              noteCount: 7,
            },
          },
        }),
      ),
      http.get('*/api/app/learning-activity/calendar', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: [],
        }),
      ),
    )
    const router = createMemoryRouter(
      [{ path: '/me', element: <UserCenterPage /> }],
      { initialEntries: ['/me'] },
    )
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>,
    )

    const banner = await screen.findByRole('img', {
      name: '把每次练习变成看得见的成长',
    })
    expect(banner).toHaveAttribute('src', 'https://example.com/banner.jpg')
    expect(screen.getByText('更换封面')).toBeInTheDocument()
    expect(screen.getByLabelText('更换封面')).toHaveAttribute(
      'accept',
      'image/png,image/jpeg,image/webp',
    )
    expect(screen.getByRole('link', { name: /会员中心/ })).toHaveAttribute(
      'href',
      '/membership/center',
    )

    const labels = ['累计作答', '学习题库', '学习时间', '错题', '收藏', '笔记']
    for (const label of labels) {
      const stat = screen.getByText(label).closest('div')
      expect(stat).not.toHaveClass('surface-card')
      expect(stat).not.toHaveClass('border')
    }
  })
})
