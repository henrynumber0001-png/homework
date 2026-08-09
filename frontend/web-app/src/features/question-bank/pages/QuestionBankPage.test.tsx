import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { server } from '../../../../tests/msw/server'
import { QuestionBankPage } from '@/features/question-bank/pages/QuestionBankPage'

function renderPage(kind: 'interview' | 'certification') {
  const path =
    kind === 'interview' ? '/banks/interview' : '/banks/certification'
  const router = createMemoryRouter(
    [{ path, element: <QuestionBankPage kind={kind} /> }],
    { initialEntries: [path] },
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

describe('QuestionBankPage', () => {
  it('shows four primary modules and keeps accuracy beside completion metadata', async () => {
    server.use(
      http.get('*/api/app/question-banks/group-page', ({ request }) => {
        expect(new URL(request.url).searchParams.get('groupId')).toBe('1')
        return HttpResponse.json({
          code: 200,
          message: 'success',
          data: {
            firstModule: {
              id: 1,
              moduleName: '前端开发',
              sortOrder: 10,
            },
            firstSubModule: {
              id: 11,
              subModuleName: 'HTML & CSS',
              sortOrder: 10,
            },
            sort: 1,
            modules: [
              {
                id: 1,
                moduleName: '前端开发',
                sortOrder: 10,
              },
              {
                id: 2,
                moduleName: '后端开发',
                sortOrder: 20,
              },
              {
                id: 3,
                moduleName: '测试',
                sortOrder: 30,
              },
              {
                id: 4,
                moduleName: '运维',
                sortOrder: 40,
              },
            ],
            subModules: [
              {
                id: 11,
                subModuleName: 'HTML & CSS',
                sortOrder: 10,
              },
              {
                id: 12,
                subModuleName: 'JavaScript',
                sortOrder: 20,
              },
            ],
            banks: [
              {
                id: 101,
                bankName: '前端基础面试题',
                subModuleId: 11,
                completeCount: 128,
                avgCorrectRate: 0.76,
                tagNames: ['HTML', 'CSS'],
              },
            ],
          },
        })
      }),
    )

    renderPage('interview')

    const firstModule = await screen.findByRole('button', {
      name: /前端开发/,
    })
    expect(firstModule.parentElement).toHaveClass('lg:grid-cols-4')
    expect(screen.getByRole('button', { name: /后端开发/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /测试/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /运维/ })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: /HTML & CSS/ })).toHaveAttribute(
      'aria-selected',
      'true',
    )

    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument()
    expect(screen.getByText('平均正确率')).toBeInTheDocument()
    expect(screen.getByText('76%')).toBeInTheDocument()
    const action = screen.getByRole('button', { name: '开始答题' })
    expect(action).toHaveClass('bank-action')
    expect(action.closest('.bank-card')).toBeInTheDocument()
    expect(screen.queryByText(/undefined/)).not.toBeInTheDocument()
  })

  it('shows an actionable certification data message instead of a generic error', async () => {
    server.use(
      http.get('*/api/app/question-banks/group-page', () =>
        HttpResponse.json({
          code: 202,
          message: '数据错误',
          data: null,
        }),
      ),
    )

    renderPage('certification')

    expect(
      await screen.findByText(
        '认证题库的默认分类暂时没有可展示的题库，请配置题库数据后重试。',
      ),
    ).toBeInTheDocument()
  })
})
