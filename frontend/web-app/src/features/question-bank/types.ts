import type { SortTypeValue } from '@/shared/constants/domain'

export interface GraphInfo {
  url: string | null
  name: string | null
}

export interface CategoryModule {
  id: number
  moduleName: string
  sortOrder: number
  graphInfoVo: GraphInfo | null
}

export interface CategorySubModule {
  id: number
  subModuleName: string
  sortOrder: number
}

export interface QuestionBank {
  id: number
  bankName: string
  subModuleId: number
  completeCount: number
  avgCorrectRate: number | null
  /** 变更：原 priority 已统一命名为 sortOrder；App排序仍由后端完成。 */
  sortOrder?: number
  questionCount?: number
  tagNames: string[]
}

export interface GroupPageData {
  firstModule: CategoryModule
  firstSubModule: CategorySubModule
  sort: SortTypeValue
  modules: CategoryModule[]
  subModules: CategorySubModule[]
  banks: QuestionBank[]
}

export interface ModulePageData {
  firstSubModule: CategorySubModule
  sort: SortTypeValue
  subModules: CategorySubModule[]
  banks: QuestionBank[]
}

export interface SubModulePageData {
  sort: SortTypeValue
  banks: QuestionBank[]
}
