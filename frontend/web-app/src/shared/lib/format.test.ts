import { describe, expect, it } from 'vitest'
import { formatCount, formatRate } from '@/shared/lib/format'

describe('format helpers', () => {
  it('keeps an unknown correct rate distinct from zero', () => {
    expect(formatRate(null)).toBe('--')
    expect(formatRate(0)).toBe('0%')
  })

  it('supports ratio and percentage responses', () => {
    expect(formatRate(0.856)).toBe('85.6%')
    expect(formatRate(85.6)).toBe('85.6%')
  })

  it('uses compact Chinese count formatting', () => {
    expect(formatCount(9999)).toBe('9999')
    expect(formatCount(12_300)).toBe('1.2万')
  })
})
