import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { server } from '../../../../tests/msw/server'
import { CertificatePracticePage } from '@/features/question/pages/CertificatePracticePage'
import { InterviewPracticePage } from '@/features/question/pages/InterviewPracticePage'
import { QuestionReviewPage } from '@/features/question/pages/QuestionReviewPage'
import type { QuestionCount } from '@/features/question/types'

function renderPracticePage(kind: 'interview' | 'certification') {
  const path = `/banks/${kind}/101/practice`
  const routePath = `/banks/${kind}/:bankId/practice`
  const Component =
    kind === 'interview' ? InterviewPracticePage : CertificatePracticePage
  const router = createMemoryRouter(
    [{ path: routePath, element: <Component /> }],
    {
      initialEntries: [path],
    },
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

function renderReviewPage(
  kind: 'interview' | 'certification',
  questionCount?: QuestionCount,
) {
  const router = createMemoryRouter(
    [
      {
        path: '/banks/:groupType/:bankId/review',
        element: <QuestionReviewPage />,
      },
    ],
    {
      initialEntries: [
        {
          pathname: `/banks/${kind}/101/review`,
          state: questionCount ? { questionCount } : undefined,
        },
      ],
    },
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

describe('practice page disclosure and layout', () => {
  it('reveals the interview reference, AI analysis, notebook and drawer only after submission', async () => {
    const user = userEvent.setup()
    server.use(
      http.get('*/api/app/bank/questions/interview/question', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: [
            {
              questionId: 11,
              title: '解释 Java Lambda 的工作原理',
              questionType: 3,
              isFavorite: false,
            },
            {
              questionId: 12,
              title: '解释 Java Stream 的惰性求值',
              questionType: 3,
              isFavorite: false,
            },
          ],
        }),
      ),
      http.get('*/api/app/bank/questions/interview/record', () =>
        HttpResponse.json({ code: 200, message: 'success', data: [] }),
      ),
      http.post('*/api/app/bank/questions/interview/answer', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: {
            questionId: 11,
            analysis: 'Lambda 会编译为 invokedynamic 调用点。',
            aiEvaluationEnabled: true,
            aiResult: {
              scoreRate: 0.8,
              accurateComment: '说明了动态调用。',
              innovativeComment: null,
              missingComment: '可以补充 LambdaMetafactory。',
              wrongComment: null,
              summary: '整体方向正确。',
              modelName: 'test',
            },
            isFavorite: false,
            content: '通过 invokedynamic 实现。',
          },
        }),
      ),
      http.get('*/api/app/bank/questions/ai/chat', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: { sessionId: null, bankId: 101, messages: [] },
        }),
      ),
      http.delete('*/api/app/bank/questions/clear/record', () =>
        HttpResponse.json({ code: 200, message: 'success', data: null }),
      ),
    )

    renderPracticePage('interview')

    const answerInput =
      await screen.findByPlaceholderText(/用自己的语言组织回答/)
    const previousButton = screen.getByRole('button', { name: '上一题' })
    expect(previousButton.parentElement).not.toHaveClass('lg:hidden')
    const nextButton = screen.getByRole('button', { name: '下一题' })
    expect(nextButton).toBeInTheDocument()
    expect(previousButton.closest('.surface-card')).toBeNull()
    expect(previousButton.parentElement).toBe(
      screen.getByRole('button', { name: '完成题库' }).parentElement,
    )
    expect(screen.getByRole('button', { name: '第2题，未作答' })).toHaveClass(
      'bg-white',
    )
    const submitButton = screen.getByRole('button', { name: '提交回答' })
    expect(submitButton).toHaveClass('w-full')
    const clearButton = screen.getByRole('button', { name: '清空记录' })
    expect(clearButton.closest('aside')).toHaveTextContent('题目导航')
    expect(screen.queryByText('参考答案')).not.toBeInTheDocument()
    expect(screen.queryByText('AI 解析')).not.toBeInTheDocument()
    expect(screen.queryByText('笔记本')).not.toBeInTheDocument()

    await user.type(answerInput, '通过 invokedynamic 实现。')
    await user.click(submitButton)

    expect(await screen.findByText('参考答案')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '第1题，已作答' })).toHaveClass(
      'bg-surface-muted',
    )
    expect(screen.getByText('AI 解析')).toBeInTheDocument()
    expect(screen.getByText('笔记本')).toBeInTheDocument()

    const askAiButton = screen.getByRole('button', { name: '追问 AI' })
    expect(askAiButton.querySelector('svg')).toHaveClass('size-3.5')
    await user.click(askAiButton)
    const drawer = screen.getByRole('dialog', { name: '追问 AI' })
    expect(drawer).toHaveAttribute('aria-hidden', 'false')

    await user.click(screen.getByText('解释 Java Lambda 的工作原理'))
    expect(drawer).toHaveAttribute('aria-hidden', 'true')
    await user.click(screen.getByRole('button', { name: '再次打开 AI 追问' }))
    expect(drawer).toHaveAttribute('aria-hidden', 'false')

    await user.click(nextButton)
    expect(
      await screen.findByText('解释 Java Stream 的惰性求值'),
    ).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '清空记录' }))
    expect(
      screen.getByRole('dialog', { name: '清空当前题库记录？' }),
    ).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '确认清空' }))
    await waitFor(() =>
      expect(screen.queryByText('参考答案')).not.toBeInTheDocument(),
    )
    expect(
      await screen.findByText('解释 Java Lambda 的工作原理'),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '完成题库' }))
    expect(
      screen.getByRole('dialog', { name: '确认完成当前题库？' }),
    ).toBeInTheDocument()
  })

  it('keeps certification analysis and notes hidden until an option is submitted', async () => {
    const user = userEvent.setup()
    server.use(
      http.get('*/api/app/bank/questions/certificate/question', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: [
            {
              questionId: 21,
              title: '哪个服务提供对象存储？',
              options: ['Amazon EBS', 'Amazon S3'],
              questionType: 1,
              imageUrl: null,
              isFavorite: false,
            },
            {
              questionId: 22,
              title: '哪个服务提供块存储？',
              options: ['Amazon EBS', 'Amazon S3'],
              questionType: 1,
              imageUrl: null,
              isFavorite: false,
            },
          ],
        }),
      ),
      http.get('*/api/app/bank/questions/certificate/record', () =>
        HttpResponse.json({ code: 200, message: 'success', data: [] }),
      ),
      http.post('*/api/app/bank/questions/certificate/practice/answer', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: {
            questionId: 21,
            correctAnswer: ['Amazon S3'],
            analysis: 'S3 是可扩展的对象存储服务。',
            correct: true,
            isFavorite: false,
          },
        }),
      ),
      http.delete('*/api/app/bank/questions/clear/record', () =>
        HttpResponse.json({ code: 200, message: 'success', data: null }),
      ),
    )

    renderPracticePage('certification')

    expect(
      await screen.findByText('哪个服务提供对象存储？'),
    ).toBeInTheDocument()
    const previousButton = screen.getByRole('button', { name: '上一题' })
    const nextButton = screen.getByRole('button', { name: '下一题' })
    expect(previousButton.closest('.surface-card')).toBeNull()
    expect(previousButton.parentElement).toBe(
      screen.getByRole('button', { name: '完成题库' }).parentElement,
    )
    expect(nextButton).toBeInTheDocument()
    const clearButton = screen.getByRole('button', { name: '清空记录' })
    expect(
      clearButton
        .closest('aside')
        ?.querySelector('nav[aria-label="认证题目导航"]'),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '第2题，未作答' })).toHaveClass(
      'bg-white',
    )
    expect(screen.queryByText('答案解析')).not.toBeInTheDocument()
    expect(screen.queryByText('我的笔记')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /B\. Amazon S3/ }))
    await user.click(screen.getByRole('button', { name: '提交答案' }))

    expect(await screen.findByText('回答正确')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '第1题，回答正确' })).toHaveClass(
      'bg-success-soft',
    )
    expect(screen.getByText('答案解析')).toBeInTheDocument()
    expect(screen.getByText('我的笔记')).toBeInTheDocument()
    expect(screen.getByText('S3 是可扩展的对象存储服务。')).toBeInTheDocument()

    await user.click(nextButton)
    expect(await screen.findByText('哪个服务提供块存储？')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '清空记录' }))
    await user.click(screen.getByRole('button', { name: '确认清空' }))
    expect(
      await screen.findByText('哪个服务提供对象存储？'),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '完成题库' }))
    expect(
      screen.getByRole('dialog', { name: '确认完成当前题库？' }),
    ).toBeInTheDocument()
  })

  it('marks an incorrectly answered certification question in red', async () => {
    const user = userEvent.setup()
    server.use(
      http.get('*/api/app/bank/questions/certificate/question', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: [
            {
              questionId: 31,
              title: '哪个服务提供对象存储？',
              options: ['Amazon EBS', 'Amazon S3'],
              questionType: 1,
              imageUrl: null,
              isFavorite: false,
            },
          ],
        }),
      ),
      http.get('*/api/app/bank/questions/certificate/record', () =>
        HttpResponse.json({ code: 200, message: 'success', data: [] }),
      ),
      http.post('*/api/app/bank/questions/certificate/practice/answer', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: {
            questionId: 31,
            correctAnswer: ['Amazon S3'],
            analysis: 'S3 是对象存储服务。',
            correct: false,
            isFavorite: false,
          },
        }),
      ),
    )

    renderPracticePage('certification')

    await screen.findByText('哪个服务提供对象存储？')
    await user.click(screen.getByRole('button', { name: /A\. Amazon EBS/ }))
    await user.click(screen.getByRole('button', { name: '提交答案' }))

    expect(
      await screen.findByRole('button', { name: '第1题，回答错误' }),
    ).toHaveClass('bg-danger-soft')
  })
})

describe('question review navigation', () => {
  it.each([
    ['interview', '返回面试题库'],
    ['certification', '返回认证题库'],
  ] as const)(
    'returns from %s review to its bank list',
    async (kind, label) => {
      const apiKind = kind === 'interview' ? 'interview' : 'certificate'
      server.use(
        http.get(`*/api/app/bank/questions/${apiKind}/review`, () =>
          HttpResponse.json({ code: 200, message: 'success', data: [] }),
        ),
      )

      renderReviewPage(kind)

      expect(await screen.findByRole('link', { name: label })).toHaveAttribute(
        'href',
        `/banks/${kind}`,
      )
    },
  )

  it('shows the interview average correct rate returned by finish', async () => {
    server.use(
      http.get('*/api/app/bank/questions/interview/review', () =>
        HttpResponse.json({ code: 200, message: 'success', data: [] }),
      ),
    )

    renderReviewPage('interview', {
      totalCount: 5,
      answeredCount: 4,
      correctCount: null,
      correctRate: 82.5,
    })

    expect(await screen.findByText('面试题平均正确率')).toBeInTheDocument()
    expect(screen.getByText('82.5%')).toBeInTheDocument()
    expect(screen.queryByText('答对')).not.toBeInTheDocument()
  })
})
