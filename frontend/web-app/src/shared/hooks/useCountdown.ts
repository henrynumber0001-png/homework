import { useCallback, useEffect, useState } from 'react'

export function useCountdown(expiresAt?: string) {
  const calculate = useCallback(() => {
    if (!expiresAt) return 0
    return Math.max(
      0,
      Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000),
    )
  }, [expiresAt])
  const [secondsLeft, setSecondsLeft] = useState(calculate)

  useEffect(() => {
    setSecondsLeft(calculate())
    const intervalId = window.setInterval(
      () => setSecondsLeft(calculate()),
      1000,
    )
    return () => window.clearInterval(intervalId)
  }, [calculate])

  return secondsLeft
}

export function formatCountdown(totalSeconds: number) {
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  return [hours, minutes, seconds]
    .map((value) => String(value).padStart(2, '0'))
    .join(':')
}
