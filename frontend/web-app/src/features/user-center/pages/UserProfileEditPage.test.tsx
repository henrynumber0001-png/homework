import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { server } from '../../../../tests/msw/server'
import { UserProfileEditPage } from '@/features/user-center/pages/UserProfileEditPage'

describe('UserProfileEditPage', () => {
  it('keeps edited fields after replacing the avatar and submits the selected secondary direction', async () => {
    let avatarUpdated = false
    let submittedBody: unknown

    server.use(
      http.get('*/api/app/user-center/profile-info', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: {
            avatarUrl: avatarUpdated
              ? 'https://example.com/new-avatar.webp'
              : 'https://example.com/avatar.webp',
            displayName: 'Henry',
            companyOrSchool: null,
            subTechDirectionId: 101,
            gender: 1,
            introduction: null,
            version: avatarUpdated ? 4 : 3,
          },
        }),
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
              {
                directionId: 2,
                directionName: '前端开发',
                subTechDirectionTreeVOList: [
                  {
                    subTechDirectionId: 201,
                    subTechDirectionName: '网站开发',
                  },
                ],
              },
            ],
          },
        }),
      ),
      http.post('*/api/app/user-center/images/1', async ({ request }) => {
        const formData = await request.formData()
        expect((formData.get('file') as Blob | null)?.size).toBe(3)
        return HttpResponse.json({
          code: 200,
          message: 'success',
          data: {
            imageObjectKey: 'temp/user/image/avatar/example.webp',
            previewUrl: 'https://example.com/new-avatar.webp',
          },
        })
      }),
      http.put('*/api/app/user-center/images/update', async ({ request }) => {
        expect(await request.json()).toEqual({
          imageObjectKey: 'temp/user/image/avatar/example.webp',
          userImageType: 1,
        })
        avatarUpdated = true
        return HttpResponse.json({
          code: 200,
          message: 'success',
          data: null,
        })
      }),
      http.put('*/api/app/user-center/edit-profile', async ({ request }) => {
        submittedBody = await request.json()
        return HttpResponse.json({
          code: 200,
          message: 'success',
          data: submittedBody,
        })
      }),
    )

    const router = createMemoryRouter(
      [
        { path: '/me/edit', element: <UserProfileEditPage /> },
        { path: '/me', element: <h1>个人中心</h1> },
      ],
      { initialEntries: ['/me/edit'] },
    )
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>,
    )

    const nameInput = await screen.findByLabelText('用户名')
    await userEvent.clear(nameInput)
    await userEvent.type(nameInput, 'Henry New')
    await userEvent.type(screen.getByLabelText('个人说明'), '持续学习中')

    const avatarFile = new File([new Uint8Array([1, 2, 3])], 'avatar.webp', {
      type: 'image/webp',
    })
    await userEvent.upload(screen.getByLabelText('更换头像'), avatarFile)
    await waitFor(() => expect(avatarUpdated).toBe(true))
    expect(screen.getByLabelText('用户名')).toHaveValue('Henry New')

    await userEvent.click(screen.getByRole('button', { name: /前端开发/ }))
    expect(screen.getByText('选择前端开发的具体方向')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /网站开发/ }))
    await userEvent.click(screen.getByRole('button', { name: '女' }))
    await userEvent.type(screen.getByLabelText('公司或学校'), 'HomeWork 大学')
    await userEvent.click(screen.getByRole('button', { name: '保存资料' }))

    expect(
      await screen.findByRole('heading', { name: '个人中心' }),
    ).toBeInTheDocument()
    expect(submittedBody).toEqual({
      displayName: 'Henry New',
      companyOrSchool: 'HomeWork 大学',
      subTechDirectionId: 201,
      gender: 2,
      introduction: '持续学习中',
      version: 4,
    })
  })
})
