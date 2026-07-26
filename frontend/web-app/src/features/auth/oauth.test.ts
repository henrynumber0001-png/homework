import { afterEach, describe, expect, it, vi } from 'vitest'
import { getConfiguredOAuthProviders } from '@/features/auth/oauth'

describe('OAuth release switch', () => {
  afterEach(() => {
    vi.unstubAllEnvs()
  })

  it('keeps every OAuth provider hidden when the release switch is off', () => {
    vi.stubEnv('VITE_OAUTH_ENABLED', 'false')

    expect(getConfiguredOAuthProviders()).toEqual([])
  })
})
