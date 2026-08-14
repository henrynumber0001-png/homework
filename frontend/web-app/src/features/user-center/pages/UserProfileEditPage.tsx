import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
} from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ArrowLeft,
  Camera,
  Check,
  ChevronRight,
  Layers3,
  Mars,
  Save,
  UserRound,
  Venus,
} from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import {
  getUserProfile,
  getUserProfileOptions,
  replaceUserCenterImage,
  updateUserProfile,
} from '@/features/user-center/api'
import type {
  EditProfileInput,
  TechDirectionOption,
  UserProfile,
} from '@/features/user-center/types'
import { Gender, type GenderValue } from '@/shared/constants/domain'
import { Avatar } from '@/shared/ui/Avatar'
import { ErrorState, PageSkeleton } from '@/shared/ui/AsyncState'
import { Button } from '@/shared/ui/Button'
import { Input, Textarea } from '@/shared/ui/Input'

interface ProfileFormState {
  displayName: string
  companyOrSchool: string
  introduction: string
  gender: GenderValue | null
  directionId: number | null
  subTechDirectionId: number | null
}

const emptyForm: ProfileFormState = {
  displayName: '',
  companyOrSchool: '',
  introduction: '',
  gender: null,
  directionId: null,
  subTechDirectionId: null,
}

export function UserProfileEditPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [form, setForm] = useState<ProfileFormState>(emptyForm)
  const formInitialized = useRef(false)

  const profileQuery = useQuery({
    queryKey: ['user-profile'],
    queryFn: getUserProfile,
  })
  const optionsQuery = useQuery({
    queryKey: ['user-profile-options'],
    queryFn: getUserProfileOptions,
    staleTime: 60 * 60 * 1000,
  })

  useEffect(() => {
    if (formInitialized.current || !profileQuery.data || !optionsQuery.data) {
      return
    }
    const selectedDirection = findParentDirection(
      optionsQuery.data.techDirectionTreeVOList,
      profileQuery.data.subTechDirectionId,
    )
    setForm({
      displayName: profileQuery.data.displayName,
      companyOrSchool: profileQuery.data.companyOrSchool ?? '',
      introduction: profileQuery.data.introduction ?? '',
      gender: profileQuery.data.gender,
      directionId: selectedDirection,
      subTechDirectionId: profileQuery.data.subTechDirectionId,
    })
    formInitialized.current = true
  }, [optionsQuery.data, profileQuery.data])

  const selectedDirection = useMemo(
    () =>
      optionsQuery.data?.techDirectionTreeVOList.find(
        (direction) => direction.directionId === form.directionId,
      ) ?? null,
    [form.directionId, optionsQuery.data],
  )

  const profileMutation = useMutation({
    mutationFn: (input: EditProfileInput) => updateUserProfile(input),
    onSuccess: async (updated) => {
      queryClient.setQueryData<UserProfile>(['user-profile'], (current) =>
        current ? { ...current, ...updated } : current,
      )
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['user-center'] }),
        queryClient.invalidateQueries({ queryKey: ['current-user'] }),
      ])
      toast.success('个人资料已保存')
      navigate('/me')
    },
    onError: (error) => {
      toast.error('资料保存失败', {
        description: error instanceof Error ? error.message : '请稍后重试',
      })
    },
  })

  const avatarMutation = useMutation({
    mutationFn: (file: File) => replaceUserCenterImage('avatar', file),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['user-profile'] }),
        queryClient.invalidateQueries({ queryKey: ['user-center'] }),
        queryClient.invalidateQueries({ queryKey: ['current-user'] }),
        queryClient.invalidateQueries({ queryKey: ['membership'] }),
      ])
      toast.success('头像已更新')
    },
    onError: (error) => {
      toast.error('头像更新失败', {
        description: error instanceof Error ? error.message : '请稍后重试',
      })
    },
  })

  if (profileQuery.isLoading || optionsQuery.isLoading) {
    return (
      <div className="app-container py-8">
        <PageSkeleton />
      </div>
    )
  }

  if (
    profileQuery.isError ||
    optionsQuery.isError ||
    !profileQuery.data ||
    !optionsQuery.data
  ) {
    return (
      <div className="app-container py-8">
        <ErrorState
          message="修改资料页面加载失败，请稍后重试"
          onRetry={() => {
            void profileQuery.refetch()
            void optionsQuery.refetch()
          }}
        />
      </div>
    )
  }

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const displayName = form.displayName.trim()
    if (!displayName) {
      toast.error('请输入用户名')
      return
    }

    profileMutation.mutate({
      displayName,
      companyOrSchool: normalizeOptional(form.companyOrSchool),
      introduction: normalizeOptional(form.introduction),
      gender: form.gender,
      subTechDirectionId: form.subTechDirectionId,
      version: profileQuery.data.version,
    })
  }

  const uploadAvatar = (file: File) => {
    if (file.size > 2 * 1024 * 1024) {
      toast.error('头像不能超过 2MB')
      return
    }
    avatarMutation.mutate(file)
  }

  return (
    <div className="app-container py-7 sm:py-9">
      <div className="mb-6 flex items-center justify-between gap-4">
        <div>
          <Link
            to="/me"
            className="inline-flex items-center gap-1.5 text-sm font-semibold text-muted transition-colors hover:text-brand"
          >
            <ArrowLeft className="size-4" />
            返回个人中心
          </Link>
          <h1 className="mt-3 text-2xl font-extrabold tracking-tight sm:text-3xl">
            修改资料
          </h1>
          <p className="mt-1 text-sm text-muted">
            完善公开资料，让其他学习者更快认识你。
          </p>
        </div>
      </div>

      <form
        onSubmit={submit}
        className="grid overflow-hidden rounded-[1.4rem] bg-surface shadow-[0_18px_50px_rgba(15,31,61,0.1)] lg:grid-cols-[18rem_minmax(0,1fr)]"
      >
        <aside className="border-b border-line bg-gradient-to-b from-brand-soft/80 to-white p-7 text-center lg:border-b-0 lg:border-r">
          <p className="text-xs font-bold uppercase tracking-[0.15em] text-accent">
            Profile photo
          </p>
          <div className="mt-7 flex justify-center">
            <label
              htmlFor="edit-profile-avatar"
              title="点击更换头像"
              aria-disabled={avatarMutation.isPending}
              className="group relative cursor-pointer rounded-full aria-disabled:pointer-events-none aria-disabled:opacity-60"
            >
              <Avatar
                src={profileQuery.data.avatarUrl}
                name={form.displayName}
                className="size-32 border-4 border-white shadow-lg transition group-hover:ring-4 group-hover:ring-brand/20"
              />
              <span className="absolute bottom-1 right-1 flex size-9 items-center justify-center rounded-full border-2 border-white bg-brand text-white shadow-md">
                <Camera className="size-4" />
              </span>
            </label>
            <input
              id="edit-profile-avatar"
              className="sr-only"
              type="file"
              aria-label="更换头像"
              accept="image/png,image/jpeg,image/webp"
              disabled={avatarMutation.isPending}
              onChange={(event) => {
                const file = event.target.files?.[0]
                if (file) uploadAvatar(file)
                event.target.value = ''
              }}
            />
          </div>
          <p className="mt-5 font-bold text-ink">
            {avatarMutation.isPending ? '头像上传中…' : '点击头像更换'}
          </p>
          <p className="mt-2 text-xs leading-5 text-muted">
            支持 JPG、PNG、WebP，文件不超过 2MB。
          </p>
        </aside>

        <div className="p-6 sm:p-8 lg:p-10">
          <div className="grid gap-6 sm:grid-cols-2">
            <Field label="用户名" required className="sm:col-span-2">
              <Input
                aria-label="用户名"
                value={form.displayName}
                maxLength={50}
                autoComplete="nickname"
                placeholder="输入展示给其他用户的名字"
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    displayName: event.target.value,
                  }))
                }
              />
            </Field>

            <Field label="性别">
              <div className="grid grid-cols-3 gap-2">
                <GenderChoice
                  label="男"
                  icon={Mars}
                  selected={form.gender === Gender.MALE}
                  onClick={() =>
                    setForm((current) => ({
                      ...current,
                      gender: Gender.MALE,
                    }))
                  }
                />
                <GenderChoice
                  label="女"
                  icon={Venus}
                  selected={form.gender === Gender.FEMALE}
                  onClick={() =>
                    setForm((current) => ({
                      ...current,
                      gender: Gender.FEMALE,
                    }))
                  }
                />
                <GenderChoice
                  label="不设置"
                  icon={UserRound}
                  selected={form.gender === null}
                  onClick={() =>
                    setForm((current) => ({ ...current, gender: null }))
                  }
                />
              </div>
            </Field>

            <Field label="公司或学校">
              <Input
                aria-label="公司或学校"
                value={form.companyOrSchool}
                maxLength={50}
                placeholder="例如：HomeWork 大学"
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    companyOrSchool: event.target.value,
                  }))
                }
              />
            </Field>

            <Field label="技术方向" className="sm:col-span-2">
              <TechDirectionPicker
                directions={optionsQuery.data.techDirectionTreeVOList}
                selectedDirection={selectedDirection}
                selectedSubDirectionId={form.subTechDirectionId}
                onDirectionChange={(directionId) =>
                  setForm((current) => ({
                    ...current,
                    directionId,
                    subTechDirectionId: null,
                  }))
                }
                onSubDirectionChange={(subTechDirectionId) =>
                  setForm((current) => ({
                    ...current,
                    subTechDirectionId,
                  }))
                }
                onClear={() =>
                  setForm((current) => ({
                    ...current,
                    directionId: null,
                    subTechDirectionId: null,
                  }))
                }
              />
            </Field>

            <Field label="个人说明" className="sm:col-span-2">
              <Textarea
                aria-label="个人说明"
                value={form.introduction}
                maxLength={100}
                rows={4}
                placeholder="介绍一下你的学习方向、经验或正在挑战的目标"
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    introduction: event.target.value,
                  }))
                }
              />
              <p className="mt-1.5 text-right text-xs text-placeholder">
                {form.introduction.length}/100
              </p>
            </Field>
          </div>

          <div className="mt-8 flex flex-col-reverse gap-3 border-t border-line pt-6 sm:flex-row sm:justify-end">
            <Button
              type="button"
              variant="secondary"
              onClick={() => navigate('/me')}
            >
              取消
            </Button>
            <Button
              type="submit"
              disabled={profileMutation.isPending || avatarMutation.isPending}
            >
              <Save className="size-4" />
              {profileMutation.isPending ? '保存中…' : '保存资料'}
            </Button>
          </div>
        </div>
      </form>
    </div>
  )
}

function Field({
  label,
  required,
  className,
  children,
}: {
  label: string
  required?: boolean
  className?: string
  children: ReactNode
}) {
  return (
    <div className={className}>
      <span className="mb-2 block text-sm font-bold text-ink">
        {label}
        {required ? <span className="ml-1 text-danger">*</span> : null}
      </span>
      {children}
    </div>
  )
}

function GenderChoice({
  label,
  icon: Icon,
  selected,
  onClick,
}: {
  label: string
  icon: typeof Mars
  selected: boolean
  onClick: () => void
}) {
  return (
    <button
      type="button"
      aria-pressed={selected}
      onClick={onClick}
      className={
        selected
          ? 'flex min-h-11 items-center justify-center gap-1.5 rounded-xl border border-brand bg-brand-soft px-2 text-xs font-bold text-brand'
          : 'flex min-h-11 items-center justify-center gap-1.5 rounded-xl border border-line bg-white px-2 text-xs font-semibold text-muted transition hover:border-brand/45 hover:text-brand'
      }
    >
      <Icon className="size-4" />
      {label}
    </button>
  )
}

function TechDirectionPicker({
  directions,
  selectedDirection,
  selectedSubDirectionId,
  onDirectionChange,
  onSubDirectionChange,
  onClear,
}: {
  directions: TechDirectionOption[]
  selectedDirection: TechDirectionOption | null
  selectedSubDirectionId: number | null
  onDirectionChange: (directionId: number) => void
  onSubDirectionChange: (subDirectionId: number) => void
  onClear: () => void
}) {
  const selectedSubDirection =
    selectedDirection?.subTechDirectionTreeVOList.find(
      (item) => item.subTechDirectionId === selectedSubDirectionId,
    ) ?? null

  return (
    <div
      role="group"
      aria-label="技术方向"
      className="overflow-hidden rounded-2xl border border-line bg-[#f7faff]"
    >
      <div className="flex min-h-14 flex-wrap items-center gap-3 border-b border-line bg-white px-4 py-3">
        <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-brand-soft text-brand">
          <Layers3 className="size-4.5" />
        </span>
        <div className="min-w-0 flex-1">
          <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-placeholder">
            当前选择
          </p>
          <div className="mt-0.5 flex min-w-0 items-center gap-1 text-sm font-bold text-ink">
            {selectedDirection ? (
              <>
                <span className="truncate">
                  {selectedDirection.directionName}
                </span>
                <ChevronRight className="size-3.5 shrink-0 text-placeholder" />
                <span className="truncate text-brand">
                  {selectedSubDirection?.subTechDirectionName ??
                    '请选择具体方向'}
                </span>
              </>
            ) : (
              <span className="font-semibold text-muted">暂未设置</span>
            )}
          </div>
        </div>
        {selectedDirection ? (
          <button
            type="button"
            onClick={onClear}
            className="rounded-lg px-2.5 py-1.5 text-xs font-semibold text-muted transition hover:bg-brand-soft hover:text-brand"
          >
            暂不设置
          </button>
        ) : null}
      </div>

      <div className="grid min-h-52 sm:grid-cols-[12rem_minmax(0,1fr)]">
        <div
          className="border-b border-line bg-[#e8f0f9] p-2 sm:border-b-0 sm:border-r"
          aria-label="一级技术方向"
        >
          <p className="px-2 pb-2 pt-1 text-xs font-bold text-muted">
            选择方向
          </p>
          <div className="grid grid-cols-2 gap-1 sm:grid-cols-1">
            {directions.map((direction, index) => {
              const active =
                selectedDirection?.directionId === direction.directionId
              return (
                <button
                  key={direction.directionId}
                  type="button"
                  aria-pressed={active}
                  onClick={() => onDirectionChange(direction.directionId)}
                  className={
                    active
                      ? 'flex min-h-10 items-center gap-2 rounded-xl bg-white px-2.5 text-left text-sm font-bold text-ink shadow-sm'
                      : 'flex min-h-10 items-center gap-2 rounded-xl px-2.5 text-left text-sm font-semibold text-muted transition hover:bg-white/65 hover:text-ink'
                  }
                >
                  <span
                    className={
                      active
                        ? 'flex size-6 shrink-0 items-center justify-center rounded-lg bg-brand text-[10px] font-extrabold text-white'
                        : 'flex size-6 shrink-0 items-center justify-center rounded-lg bg-white/70 text-[10px] font-extrabold text-placeholder'
                    }
                  >
                    {String(index + 1).padStart(2, '0')}
                  </span>
                  <span className="truncate">{direction.directionName}</span>
                </button>
              )
            })}
          </div>
        </div>

        <div className="p-3" aria-label="二级技术方向">
          <p className="px-1 pb-3 pt-0.5 text-xs font-bold text-muted">
            {selectedDirection
              ? `选择${selectedDirection.directionName}的具体方向`
              : '请先选择左侧方向'}
          </p>
          {selectedDirection ? (
            <div className="grid gap-2 sm:grid-cols-2">
              {selectedDirection.subTechDirectionTreeVOList.map(
                (subDirection) => {
                  const active =
                    subDirection.subTechDirectionId === selectedSubDirectionId
                  return (
                    <button
                      key={subDirection.subTechDirectionId}
                      type="button"
                      aria-pressed={active}
                      onClick={() =>
                        onSubDirectionChange(subDirection.subTechDirectionId)
                      }
                      className={
                        active
                          ? 'flex min-h-11 items-center justify-between gap-3 rounded-xl border border-brand bg-white px-3 text-left text-sm font-bold text-brand shadow-sm'
                          : 'flex min-h-11 items-center justify-between gap-3 rounded-xl border border-transparent bg-white/75 px-3 text-left text-sm font-semibold text-ink transition hover:border-brand/35 hover:text-brand'
                      }
                    >
                      <span>{subDirection.subTechDirectionName}</span>
                      {active ? (
                        <Check className="size-4 shrink-0" />
                      ) : (
                        <ChevronRight className="size-3.5 shrink-0 text-placeholder" />
                      )}
                    </button>
                  )
                },
              )}
            </div>
          ) : (
            <div className="flex min-h-32 items-center justify-center rounded-xl border border-dashed border-line bg-white/55 px-4 text-center text-sm text-muted">
              选择一级方向后，这里会显示对应的具体方向
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function findParentDirection(
  directions: TechDirectionOption[],
  subTechDirectionId: number | null,
) {
  if (subTechDirectionId === null) return null
  return (
    directions.find((direction) =>
      direction.subTechDirectionTreeVOList.some(
        (item) => item.subTechDirectionId === subTechDirectionId,
      ),
    )?.directionId ?? null
  )
}

function normalizeOptional(value: string) {
  const normalized = value.trim()
  return normalized || null
}
