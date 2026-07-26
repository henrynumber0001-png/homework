import { describe, expect, it, vi } from 'vitest'
import { runBatchActions } from './batchActions'

describe('runBatchActions', () => {
  it('按顺序执行并汇总失败项', async () => {
    const progress = vi.fn()
    const result = await runBatchActions(
      [1, 2, 3],
      async (id) => {
        if (id === 2) throw new Error('版本冲突')
      },
      progress,
    )

    expect(result.succeeded).toEqual([1, 3])
    expect(result.failures).toEqual([{ item: 2, message: '版本冲突' }])
    expect(progress).toHaveBeenLastCalledWith(3, 3)
  })
})
