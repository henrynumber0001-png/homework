import { apiRequest } from '@/shared/api/client'
import type { HomePageData } from '@/features/home/types'

export function getHomePage() {
  return apiRequest<HomePageData>({
    url: '/app/home-page',
  })
}
