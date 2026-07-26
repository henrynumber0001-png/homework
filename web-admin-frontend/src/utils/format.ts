import dayjs from 'dayjs'

export function formatDateTime(value?: string): string {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '—'
}

export function formatNumber(value?: number): string {
  return new Intl.NumberFormat('zh-CN').format(value ?? 0)
}

export function saveBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = fileName
  anchor.click()
  URL.revokeObjectURL(url)
}
