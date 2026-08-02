import * as domMatchers from '@testing-library/jest-dom/matchers'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll, expect } from 'vitest'
import { server } from './msw/server'

// The workspace contains two Vitest major versions. Register the DOM matchers
// against this application's Vitest instance instead of relying on package resolution.
expect.extend(domMatchers)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))

afterEach(() => {
  cleanup()
  server.resetHandlers()
  localStorage.clear()
  sessionStorage.clear()
})

afterAll(() => server.close())
