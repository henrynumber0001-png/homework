import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../../tests/msw/server'
import { apiRequest } from '@/shared/api/client'

describe('apiRequest', () => {
  it('unwraps a successful Result response', async () => {
    server.use(
      http.get('*/api/test/success', () =>
        HttpResponse.json({
          code: 200,
          message: 'success',
          data: { value: 'ok' },
        }),
      ),
    )

    await expect(
      apiRequest<{ value: string }>({ url: '/test/success' }),
    ).resolves.toEqual({ value: 'ok' })
  })

  it('throws the backend business message for a failed Result', async () => {
    server.use(
      http.get('*/api/test/failure', () =>
        HttpResponse.json({
          code: 202,
          message: '参数不正确',
          data: null,
        }),
      ),
    )

    await expect(apiRequest({ url: '/test/failure' })).rejects.toMatchObject({
      code: 202,
      message: '参数不正确',
    })
  })
})
