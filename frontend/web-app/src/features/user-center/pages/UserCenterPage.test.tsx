import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { server } from '../../../../tests/msw/server'
import { UserCenterPage } from '@/features/user-center/pages/UserCenterPage'

describe('UserCenterPage', () => {
  it('uses user info as an editable banner and keeps numeric stats borderless', async () => {
    let bannerConfirmed = false
    server.use(
      http.get('*/api/app/user-center', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: {
            userInfoVO: {
              accountNo: 'HW000001',
              displayName: 'Henry',
              avatarUrl: null,
              bannerUrl: bannerConfirmed
                ? 'https://example.com/new-banner.webp'
                : 'https://example.com/banner.jpg',
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
      http.post('*/api/app/user-center/images/2', async ({ request }) => {
        const formData = await request.formData()
        const uploadedFile = formData.get('file') as Blob | null
        expect(uploadedFile?.size).toBe(3)
        expect(uploadedFile?.type).toBe('image/webp')
        return HttpResponse.json({
          code: 200,
          message: 'success',
          data: {
            imageObjectKey: 'temp/user/image/banner/example.webp',
            previewUrl: 'https://example.com/new-banner.webp',
          },
        })
      }),
      http.put('*/api/app/user-center/images/update', async ({ request }) => {
        expect(await request.json()).toEqual({
          imageObjectKey: 'temp/user/image/banner/example.webp',
          userImageType: 2,
        })
        bannerConfirmed = true
        return HttpResponse.json({
          code: 200,
          message: 'success',
          data: null,
        })
      }),
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
      name: '个人中心封面',
    })
    expect(banner).toHaveAttribute('src', 'https://example.com/banner.jpg')
    expect(screen.getByText('更换封面')).toBeInTheDocument()
    expect(screen.getByLabelText('更换封面')).toHaveAttribute(
      'accept',
      'image/png,image/jpeg,image/webp',
    )
    expect(screen.getByLabelText('更换头像')).toHaveAttribute(
      'accept',
      'image/png,image/jpeg,image/webp',
    )
    const avatarTrigger = screen.getByTitle('点击更换头像')
    expect(avatarTrigger).toHaveAttribute('for', 'profile-avatar-upload')
    expect(avatarTrigger.querySelector('svg')).toBeNull()
    expect(screen.queryByText('My learning space')).not.toBeInTheDocument()
    expect(
      screen.queryByText(/保持好奇，记录每一次进步/),
    ).not.toBeInTheDocument()
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

    const file = new File([new Uint8Array([1, 2, 3])], 'banner.webp', {
      type: 'image/webp',
    })
    await userEvent.upload(screen.getByLabelText('更换封面'), file)
    await waitFor(() =>
      expect(screen.getByRole('img', { name: '个人中心封面' })).toHaveAttribute(
        'src',
        'https://example.com/new-banner.webp',
      ),
    )
    expect(bannerConfirmed).toBe(true)
  })
})
