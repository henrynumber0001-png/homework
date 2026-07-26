import { useInfiniteQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { listHits } from '@/features/hit/api'
import { HitCard } from '@/features/hit/components/HitCard'
import { HitComments } from '@/features/hit/components/HitComments'
import { HitComposer } from '@/features/hit/components/HitComposer'
import { Button } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { EmptyState, ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'

const PAGE_SIZE = 20

export function HitListPage() {
  const [openCommentsPostId, setOpenCommentsPostId] = useState<number | null>(
    null,
  )
  const hitsQuery = useInfiniteQuery({
    queryKey: ['hits'],
    queryFn: ({ pageParam }) => listHits(pageParam, PAGE_SIZE),
    initialPageParam: 1,
    getNextPageParam: (lastPage, pages) =>
      lastPage.length < PAGE_SIZE ? undefined : pages.length + 1,
  })
  const posts = hitsQuery.data?.pages.flat() || []

  return (
    <div className="reading-container py-8">
      <header>
        <p className="text-sm font-semibold text-brand">学习社区</p>
        <h1 className="mt-1 text-3xl font-extrabold tracking-tight">
          #Hit · 学习动态
        </h1>
        <p className="mt-2 text-sm leading-6 text-muted">
          分享刚刚学会的知识，也把好问题留给社区。
        </p>
      </header>

      <div className="mt-7">
        <HitComposer />
      </div>

      {hitsQuery.isLoading ? (
        <div className="mt-5">
          <PageSkeleton />
        </div>
      ) : hitsQuery.isError ? (
        <div className="mt-5">
          <ErrorState onRetry={() => void hitsQuery.refetch()} />
        </div>
      ) : posts.length ? (
        <div className="mt-5 space-y-4">
          {posts.map((post) => (
            <div key={post.postId}>
              <HitCard
                post={post}
                onToggleComments={() =>
                  setOpenCommentsPostId((current) =>
                    current === post.postId ? null : post.postId,
                  )
                }
              />
              {openCommentsPostId === post.postId ? (
                <Card className="-mt-4 rounded-t-none border-t-0 px-5 pb-5">
                  <HitComments postId={post.postId} />
                </Card>
              ) : null}
            </div>
          ))}
          {hitsQuery.hasNextPage ? (
            <Button
              className="w-full"
              variant="secondary"
              disabled={hitsQuery.isFetchingNextPage}
              onClick={() => void hitsQuery.fetchNextPage()}
            >
              {hitsQuery.isFetchingNextPage ? '加载中…' : '加载更多'}
            </Button>
          ) : (
            <p className="py-4 text-center text-xs text-muted">
              已经看到全部内容
            </p>
          )}
        </div>
      ) : (
        <Card className="mt-5">
          <EmptyState
            title="还没有 Hit"
            description="分享你的第一条学习动态吧。"
          />
        </Card>
      )}
    </div>
  )
}
