import { lazy, Suspense, type PropsWithChildren } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { TooltipProvider } from '@radix-ui/react-tooltip'
import { Toaster } from 'sonner'
import { queryClient } from '@/app/query-client'

const ReactQueryDevtools = lazy(() =>
  import('@tanstack/react-query-devtools').then((module) => ({
    default: module.ReactQueryDevtools,
  })),
)

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider delayDuration={300}>
        {children}
        <Toaster
          position="top-center"
          toastOptions={{
            style: {
              background: '#fffdfb',
              border: '1px solid #ded5ce',
              color: '#2f2925',
            },
          }}
        />
      </TooltipProvider>
      {import.meta.env.DEV ? (
        <Suspense fallback={null}>
          <ReactQueryDevtools initialIsOpen={false} />
        </Suspense>
      ) : null}
    </QueryClientProvider>
  )
}
