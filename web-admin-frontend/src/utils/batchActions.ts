export interface BatchActionFailure<T> {
  item: T
  message: string
}

export interface BatchActionResult<T> {
  succeeded: T[]
  failures: Array<BatchActionFailure<T>>
}

export async function runBatchActions<T>(
  items: T[],
  action: (item: T) => Promise<unknown>,
  onProgress?: (completed: number, total: number) => void,
): Promise<BatchActionResult<T>> {
  const succeeded: T[] = []
  const failures: Array<BatchActionFailure<T>> = []

  for (const item of items) {
    try {
      await action(item)
      succeeded.push(item)
    } catch (error) {
      failures.push({
        item,
        message: error instanceof Error ? error.message : '操作失败',
      })
    }
    onProgress?.(succeeded.length + failures.length, items.length)
  }

  return { succeeded, failures }
}
