import type { HitPost } from '@/features/hit/types'

export interface HotQuestionBank {
  bankId: number
  bankName: string
  moduleName: string
  completeCount: number
  avgCorrectRate: number | null
}

export interface HomePageData {
  interviewQuestionBankVOList: HotQuestionBank[]
  certificateQuestionBankVOList: HotQuestionBank[]
  hotPostList: HitPost[]
}
