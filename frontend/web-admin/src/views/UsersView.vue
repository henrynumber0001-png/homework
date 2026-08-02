<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import ReasonDialog from '@/components/ReasonDialog.vue'
import ReauthDialog from '@/components/ReauthDialog.vue'
import StatusTag from '@/components/StatusTag.vue'
import { actOnUser, getUser, listUsers, updateUserCommunityAccess } from '@/api/admin'
import { showApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import {
  CommunityRestrictionScope,
  UserAccountAction,
  UserInfoStatus,
  type CommunityRestrictionScope as CommunityRestrictionScopeValue,
  type UserAccountAction as UserAccountActionValue,
  type UserDetail,
  type UserInfoStatus as UserInfoStatusValue,
  type UserRow,
} from '@/types/admin'
import { formatDateTime } from '@/utils/format'
import {
  communityRestrictionScopeLabels,
  membershipTypeLabels,
  userIdentityProviderLabels,
  userIdentityStatusLabels,
  userStatusNames,
} from '@/utils/dictionaries'

const auth = useAuthStore()
const loading = ref(false)
const users = ref<UserRow[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<UserDetail | null>(null)
const reasonDialog = ref<InstanceType<typeof ReasonDialog>>()
const reauthDialog = ref<InstanceType<typeof ReauthDialog>>()
const pendingAction = ref<{ row: UserRow; action: UserAccountActionValue; reason?: string } | null>(null)
const communityVisible = ref(false)
const communitySaving = ref(false)
const query = reactive({
  keyword: '',
  status: '' as UserInfoStatusValue | '',
  pageNum: 1,
  pageSize: 20,
})
const communityForm = reactive({
  userId: 0,
  restricted: true,
  scope: CommunityRestrictionScope.BOTH as CommunityRestrictionScopeValue,
  endTime: '',
  reason: '',
  version: 0,
})

onMounted(load)

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await listUsers({
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    })
    users.value = result.records
    total.value = result.total
  } catch (error) {
    showApiError(error)
  } finally {
    loading.value = false
  }
}

function search(): void {
  query.pageNum = 1
  void load()
}

async function openDetail(user: UserRow): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await getUser(user.id)
  } catch (error) {
    showApiError(error)
  } finally {
    detailLoading.value = false
  }
}

function openAction(row: UserRow, action: UserAccountAction, title: string): void {
  pendingAction.value = { row, action }
  reasonDialog.value?.open({ title, description: `${row.displayName}（${row.accountNo}）` })
}

async function confirmReason(reason: string): Promise<void> {
  if (!pendingAction.value) return
  pendingAction.value.reason = reason
  reasonDialog.value?.close()
  if (
    pendingAction.value.action === UserAccountAction.BAN ||
    pendingAction.value.action === UserAccountAction.UNBAN
  ) {
    reauthDialog.value?.open('user:ban')
    return
  }
  await executeAction()
}

async function executeAction(reauthToken?: string): Promise<void> {
  if (!pendingAction.value?.reason) return
  try {
    await actOnUser(
      pendingAction.value.row.id,
      {
        action: pendingAction.value.action,
        reason: pendingAction.value.reason,
        version: pendingAction.value.row.version,
      },
      reauthToken,
    )
    ElMessage.success('用户状态已更新')
    await load()
  } catch (error) {
    showApiError(error)
  }
}

function openCommunity(row: UserRow, restricted: boolean): void {
  Object.assign(communityForm, {
    userId: row.id,
    restricted,
    scope: CommunityRestrictionScope.BOTH,
    endTime: '',
    reason: '',
    version: row.version,
  })
  communityVisible.value = true
}

async function saveCommunity(): Promise<void> {
  if (!communityForm.reason.trim()) return
  communitySaving.value = true
  try {
    await updateUserCommunityAccess(communityForm.userId, {
      restricted: communityForm.restricted,
      scope: communityForm.restricted ? communityForm.scope : undefined,
      endTime: communityForm.restricted ? communityForm.endTime || undefined : undefined,
      reason: communityForm.reason.trim(),
      version: communityForm.version,
    })
    ElMessage.success(communityForm.restricted ? '社区权限已限制' : '社区权限已恢复')
    communityVisible.value = false
    await load()
  } catch (error) {
    showApiError(error)
  } finally {
    communitySaving.value = false
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="用户管理" description="查询 App 用户，并处理基础账号状态和社区访问限制" />
    <section class="panel">
      <div class="filter-bar">
        <el-input v-model="query.keyword" clearable placeholder="账号、昵称或用户 ID" @keyup.enter="search" />
        <el-select v-model="query.status" clearable placeholder="账号状态">
          <el-option label="正常" :value="UserInfoStatus.ACTIVE" />
          <el-option label="禁用" :value="UserInfoStatus.DISABLED" />
          <el-option label="封禁" :value="UserInfoStatus.BANNED" />
        </el-select>
        <el-button type="primary" plain @click="search">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="users">
        <el-table-column prop="id" label="用户 ID" width="100" />
        <el-table-column label="用户" min-width="220">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :src="row.avatar">{{ row.displayName?.slice(0, 1) }}</el-avatar>
              <div><strong>{{ row.displayName }}</strong><span>{{ row.accountNo }}</span></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :value="userStatusNames[row.status]" /></template>
        </el-table-column>
        <el-table-column label="会员" width="130">
          <template #default="{ row }">{{ membershipTypeLabels[row.membershipType] || row.membershipType }}</template>
        </el-table-column>
        <el-table-column label="注册时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.registeredTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-dropdown v-if="auth.hasPermission('user:manage')" trigger="click">
              <el-button link>账号与社区</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-if="row.status === UserInfoStatus.ACTIVE"
                    @click="openAction(row, UserAccountAction.DISABLE, '临时禁用用户')"
                  >
                    临时禁用
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="row.status === UserInfoStatus.DISABLED"
                    @click="openAction(row, UserAccountAction.ACTIVATE, '启用用户')"
                  >
                    启用账号
                  </el-dropdown-item>
                  <el-dropdown-item @click="openCommunity(row, true)">限制社区发言</el-dropdown-item>
                  <el-dropdown-item @click="openCommunity(row, false)">恢复社区发言</el-dropdown-item>
                  <template v-if="auth.isSuperAdmin">
                    <el-dropdown-item
                      v-if="row.status !== UserInfoStatus.BANNED"
                      divided
                      @click="openAction(row, UserAccountAction.BAN, '永久封禁用户')"
                    >
                      永久封禁
                    </el-dropdown-item>
                    <el-dropdown-item
                      v-else
                      divided
                      @click="openAction(row, UserAccountAction.UNBAN, '解除永久封禁')"
                    >
                      解除封禁
                    </el-dropdown-item>
                  </template>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </section>

    <el-drawer v-model="detailVisible" title="用户详情" size="560px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户 ID">{{ detail.id }}</el-descriptions-item>
            <el-descriptions-item label="账号">{{ detail.accountNo }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ detail.displayName }}</el-descriptions-item>
            <el-descriptions-item label="内容数量">
              {{ detail.postCount }} 条动态 / {{ detail.commentCount }} 条评论
            </el-descriptions-item>
            <el-descriptions-item label="社区限制">
              <template v-if="detail.communityRestriction">
                {{ communityRestrictionScopeLabels[detail.communityRestriction.scope] }}，至
                {{ formatDateTime(detail.communityRestriction.endTime) }}
              </template>
              <span v-else>无</span>
            </el-descriptions-item>
          </el-descriptions>
          <h3>登录身份</h3>
          <el-table :data="detail.identities">
            <el-table-column label="渠道" width="110">
              <template #default="{ row }">{{ userIdentityProviderLabels[row.provider] }}</template>
            </el-table-column>
            <el-table-column prop="maskedIdentifier" label="脱敏标识" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">{{ userIdentityStatusLabels[row.status] }}</template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-drawer>

    <el-dialog v-model="communityVisible" :title="communityForm.restricted ? '限制社区发言' : '恢复社区发言'" width="500px">
      <el-form label-position="top">
        <template v-if="communityForm.restricted">
          <el-form-item label="限制范围">
            <el-radio-group v-model="communityForm.scope">
              <el-radio :value="CommunityRestrictionScope.POST">发帖</el-radio>
              <el-radio :value="CommunityRestrictionScope.COMMENT">评论</el-radio>
              <el-radio :value="CommunityRestrictionScope.BOTH">发帖和评论</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="结束时间">
            <el-date-picker
              v-model="communityForm.endTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="不填表示长期限制"
            />
          </el-form-item>
        </template>
        <el-form-item label="操作原因" required>
          <el-input v-model="communityForm.reason" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="communityVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="communitySaving"
          :disabled="!communityForm.reason.trim()"
          @click="saveCommunity"
        >
          确认
        </el-button>
      </template>
    </el-dialog>

    <ReasonDialog ref="reasonDialog" @confirm="confirmReason" />
    <ReauthDialog ref="reauthDialog" @success="executeAction" />
  </div>
</template>

<style scoped>
.panel > .filter-bar {
  margin-bottom: 18px;
}

.user-cell {
  display: flex;
  gap: 11px;
  align-items: center;
}

.user-cell strong,
.user-cell span {
  display: block;
}

.user-cell span {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 11px;
}

h3 {
  margin: 26px 0 10px;
  font-size: 15px;
}
</style>
