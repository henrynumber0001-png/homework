import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { server } from '../../../../tests/msw/server'
import { MessagesPage } from '@/features/messages/pages/MessagesPage'

function renderPage(initialEntry = '/messages') {
  const router = createMemoryRouter(
    [{ path: '/messages', element: <MessagesPage /> }],
    { initialEntries: [initialEntry] },
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

function emptyPage() {
  return {
    records: [],
    pageNum: 1,
    pageSize: 20,
    total: 0,
  }
}

describe('MessagesPage', () => {
  it('shows an unboxed text hint when a notification tab is empty', async () => {
    server.use(
      http.get('*/api/app/messages/unread-summary', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: {
            commentsAndMentions: 0,
            interactions: 0,
            system: 0,
            privateMessages: 0,
          },
        }),
      ),
      http.put('*/api/app/messages/notifications/open-tab', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: emptyPage(),
        }),
      ),
    )

    renderPage()

    const hint = await screen.findByText('这里暂时没有消息')
    expect(hint.closest('.surface-card')).toBeNull()
    expect(document.querySelector('.lucide-inbox')).toBeNull()
  })

  it('shows an unboxed text hint when there are no private conversations', async () => {
    server.use(
      http.get('*/api/app/messages/unread-summary', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: {
            commentsAndMentions: 0,
            interactions: 0,
            system: 0,
            privateMessages: 0,
          },
        }),
      ),
      http.get('*/api/app/messages/chatboxes', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: emptyPage(),
        }),
      ),
    )

    renderPage('/messages?tab=private')

    const hint = await screen.findByText('暂无私信会话')
    expect(hint.closest('.surface-card')).toBeNull()
    expect(document.querySelector('.lucide-inbox')).toBeNull()
  })
})
