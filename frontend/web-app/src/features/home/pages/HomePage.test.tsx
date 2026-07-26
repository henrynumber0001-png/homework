import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { server } from '../../../../tests/msw/server'
import { HomePage } from '@/features/home/pages/HomePage'

function renderPage() {
  const router = createMemoryRouter(
    [
      { path: '/home', element: <HomePage /> },
      {
        path: '/banks/interview/:bankId/practice',
        element: <p>已进入面试题库</p>,
      },
    ],
    { initialEntries: ['/home'] },
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

describe('HomePage', () => {
  it('makes the whole hot bank row clickable and keeps accuracy on the right', async () => {
    server.use(
      http.get('*/api/app/user/info', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: { displayName: 'Henry' },
        }),
      ),
      http.get('*/api/app/home-page', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: {
            interviewQuestionBankVOList: [
              {
                bankId: 101,
                bankName: 'HTML5 面试题库',
                moduleName: '前端开发',
                completeCount: 128,
                avgCorrectRate: 0.763,
              },
            ],
            certificateQuestionBankVOList: [],
            hotPostList: [],
          },
        }),
      ),
    )

    renderPage()

    const bankRow = await screen.findByRole('button', {
      name: /HTML5 面试题库/,
    })
    expect(bankRow).toHaveClass('home-bank-row')
    expect(bankRow.querySelector('.lucide-arrow-right')).toBeNull()

    const accuracy = bankRow.querySelector('.home-bank-accuracy')
    expect(accuracy).toHaveTextContent('正确率 76.3%')
    expect(accuracy).toHaveClass('shrink-0')

    await userEvent.click(bankRow)
    expect(await screen.findByText('已进入面试题库')).toBeInTheDocument()
  })
})
