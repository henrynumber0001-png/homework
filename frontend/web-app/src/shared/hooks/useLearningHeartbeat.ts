import { useEffect, useRef } from 'react'
import { useLocation } from 'react-router-dom'
import { apiRequest } from '@/shared/api/client'

const HEARTBEAT_INTERVAL_MS = 60_000
const ACTIVE_WINDOW_MS = 10 * 60_000

export function useLearningHeartbeat() {
  const location = useLocation()
  const lastActivityAt = useRef(Date.now())

  useEffect(() => {
    lastActivityAt.current = Date.now()
  }, [location.pathname, location.search])

  useEffect(() => {
    const markActivity = () => {
      lastActivityAt.current = Date.now()
    }
    const events: (keyof WindowEventMap)[] = [
      'click',
      'keydown',
      'scroll',
      'touchstart',
    ]

    events.forEach((event) =>
      window.addEventListener(event, markActivity, { passive: true }),
    )

    const intervalId = window.setInterval(() => {
      const recentlyActive =
        Date.now() - lastActivityAt.current < ACTIVE_WINDOW_MS
      if (document.visibilityState !== 'visible' || !recentlyActive) return

      void apiRequest<void>({
        url: '/app/learning-activity/heartbeat',
        method: 'POST',
      }).catch(() => {
        // Heartbeat failure must not interrupt the current learning flow.
      })
    }, HEARTBEAT_INTERVAL_MS)

    return () => {
      window.clearInterval(intervalId)
      events.forEach((event) => window.removeEventListener(event, markActivity))
    }
  }, [])
}
