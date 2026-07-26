import { apiRequest } from '@/shared/api/client'
import type { SortTypeValue } from '@/shared/constants/domain'
import type {
  GroupPageData,
  ModulePageData,
  QuestionBank,
  SubModulePageData,
} from '@/features/question-bank/types'

export function getQuestionBankGroupPage(groupId: number) {
  return apiRequest<GroupPageData>({
    url: '/app/question-banks/group-page',
    params: { groupId },
  })
}

export function getQuestionBankModulePage(
  currentGroupId: number,
  moduleId: number,
  currentModuleId: number,
) {
  return apiRequest<ModulePageData>({
    url: '/app/question-banks/group-page/module-page',
    params: { currentGroupId, moduleId, currentModuleId },
  })
}

export function getQuestionBankSubModulePage(
  currentGroupId: number,
  currentModuleId: number,
  subModuleId: number,
  currentSubModuleId: number,
) {
  return apiRequest<SubModulePageData>({
    url: '/app/question-banks/group-page/module-page/sub-module-page',
    params: {
      currentGroupId,
      currentModuleId,
      subModuleId,
      currentSubModuleId,
    },
  })
}

export function getQuestionBanksBySort(
  sortType: SortTypeValue,
  currentSubModuleId: number,
) {
  return apiRequest<QuestionBank[]>({
    url: '/app/question-banks/group-page/sort-type',
    params: { sortType, currentSubModuleId },
  })
}
