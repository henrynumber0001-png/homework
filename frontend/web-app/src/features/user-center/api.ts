import { apiRequest } from '@/shared/api/client'
import type { PageResult } from '@/shared/api/result'
import type { GroupTypeValue } from '@/shared/constants/domain'
import type {
  LearningCalendarItem,
  LibraryKind,
  EditProfileInput,
  EditedProfile,
  UserProfile,
  UserProfileOptions,
  UserCenterData,
  UserCenterActivity,
  FollowerListItem,
  FollowingListItem,
  UserImageType,
  UserImageUpload,
  UserQuestionBank,
  UserQuestionDetail,
  UserQuestionListItem,
} from '@/features/user-center/types'

const userImageTypeCodes: Record<UserImageType, 1 | 2> = {
  avatar: 1,
  banner: 2,
}

export function getUserCenter() {
  return apiRequest<UserCenterData>({
    url: '/app/user-center',
  })
}

export function getUserCenterActivities(
  tab: string,
  pageNum = 1,
  pageSize = 20,
) {
  return apiRequest<UserCenterActivity[]>({
    url: '/app/user-center/activities',
    params: { tab, pageNum, pageSize },
  })
}

export function getFollowers(pageNum = 1, pageSize = 20) {
  return apiRequest<FollowerListItem[]>({
    url: '/app/user-center/follower-list',
    params: { pageNum, pageSize },
  })
}

export function getFollowing(pageNum = 1, pageSize = 20) {
  return apiRequest<FollowingListItem[]>({
    url: '/app/user-center/following-list',
    params: { pageNum, pageSize },
  })
}

export function getUserProfile() {
  return apiRequest<UserProfile>({
    url: '/app/user-center/profile-info',
  })
}

export function getUserProfileOptions() {
  return apiRequest<UserProfileOptions>({
    url: '/app/user-center/profile-info/options',
  })
}

export function updateUserProfile(data: EditProfileInput) {
  return apiRequest<EditedProfile>({
    method: 'put',
    url: '/app/user-center/edit-profile',
    data,
  })
}

export async function replaceUserCenterImage(
  imageType: UserImageType,
  file: File,
) {
  const userImageType = userImageTypeCodes[imageType]
  const formData = new FormData()
  formData.append('file', file)
  const upload = await apiRequest<UserImageUpload>({
    method: 'post',
    url: `/app/user-center/images/${userImageType}`,
    data: formData,
  })
  await apiRequest<void>({
    method: 'put',
    url: '/app/user-center/images/update',
    data: {
      imageObjectKey: upload.imageObjectKey,
      userImageType,
    },
  })
  return upload
}

export function getLearningCalendar(year: number) {
  return apiRequest<LearningCalendarItem[]>({
    url: '/app/learning-activity/calendar',
    params: { year },
  })
}

const bankEndpoints: Record<LibraryKind, string> = {
  wrong: 'wrong-question-banks',
  favorite: 'favorite-question-banks',
  note: 'note-banks',
}

const listEndpoints: Record<LibraryKind, string> = {
  wrong: 'wrong-question-list',
  favorite: 'favorite-question-list',
  note: 'note-list',
}

const detailEndpoints: Record<LibraryKind, string> = {
  wrong: 'wrong-question',
  favorite: 'favorite-question',
  note: 'note-question',
}

export function getUserQuestionBanks(
  kind: LibraryKind,
  groupType: GroupTypeValue,
  pageNum = 1,
  pageSize = 20,
) {
  return apiRequest<PageResult<UserQuestionBank>>({
    url: `/app/user-center/${bankEndpoints[kind]}`,
    params: { groupType, pageNum, pageSize },
  })
}

export function getUserQuestionList(
  kind: LibraryKind,
  bankId: number,
  pageNum = 1,
  pageSize = 20,
) {
  return apiRequest<PageResult<UserQuestionListItem>>({
    url: `/app/user-center/${listEndpoints[kind]}`,
    params: { bankId, pageNum, pageSize },
  })
}

export function getUserQuestionDetail(
  kind: LibraryKind,
  bankId: number,
  questionId: number,
) {
  return apiRequest<UserQuestionDetail>({
    url: `/app/user-center/${detailEndpoints[kind]}`,
    params: { bankId, questionId },
  })
}
