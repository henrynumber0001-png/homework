<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import ReauthDialog from '@/components/ReauthDialog.vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  actOnAdmin,
  createAdminInvitation,
  listAdmins,
  listQuestionBanks,
  updateAdminAccess,
} from '@/api/admin'
import { showApiError } from '@/api/http'
import {
  AdminAccountAction,
  AdminRole,
  AdminStatus,
  BankDataScope,
  type AdminAccountAction as AdminAccountActionValue,
  type AdminRow,
  type AdminStatus as AdminStatusValue,
  type BankDataScope as BankDataScopeValue,
  type QuestionBank,
} from '@/types/admin'
import { formatDateTime } from '@/utils/format'
import { adminAccountActionLabels, adminStatusNames, permissionOptions } from '@/utils/dictionaries'

type RiskOperation = 'invite' | 'access' | 'status'

const loading = ref(false)
const admins = ref<AdminRow[]>([])
const banks = ref<QuestionBank[]>([])
const total = ref(0)
const inviteVisible = ref(false)
const accessVisible = ref(false)
const statusVisible = ref(false)
const reauthDialog = ref<InstanceType<typeof ReauthDialog>>()
const pendingRiskOperation = ref<RiskOperation>()
const query = reactive({
  keyword: '',
  status: '' as AdminStatusValue | '',
  pageNum: 1,
  pageSize: 20,
})
const inviteForm = reactive({
  email: '',
  displayName: '',
  permissions: ['bank:view', 'question:view'] as string[],
  bankDataScope: BankDataScope.ASSIGNED_BANKS as BankDataScopeValue,
  assignedBankIds: [] as number[],
  reason: '',
})
const accessForm = reactive({
  adminId: 0,
  displayName: '',
  permissions: [] as string[],
  bankDataScope: BankDataScope.ASSIGNED_BANKS as BankDataScopeValue,
  assignedBankIds: [] as number[],
  reason: '',
  version: 0,
})
const statusForm = reactive<{
  adminId: number
  displayName: string
  action: AdminAccountActionValue
  reason: string
  version: number
}>({
  adminId: 0,
  displayName: '',
  action: AdminAccountAction.DISABLE,
  reason: '',
  version: 0,
})

onMounted(async () => {
  await Promise.all([load(), loadBanks()])
})

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await listAdmins({
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    })
    admins.value = result.records
    total.value = result.total
  } catch (error) {
    showApiError(error)
  } finally {
    loading.value = false
  }
}

async function loadBanks(): Promise<void> {
  try {
    const result = await listQuestionBanks({ pageNum: 1, pageSize: 100 })
    banks.value = result.records
  } catch {
    banks.value = []
  }
}

function search(): void {
  query.pageNum = 1
  void load()
}

function openInvite(): void {
  Object.assign(inviteForm, {
    email: '',
    displayName: '',
    permissions: ['bank:view', 'question:view'],
    bankDataScope: BankDataScope.ASSIGNED_BANKS,
    assignedBankIds: [],
    reason: '',
  })
  inviteVisible.value = true
}

function openAccess(row: AdminRow): void {
  Object.assign(accessForm, {
    adminId: row.id,
    displayName: row.displayName,
    permissions: [...row.permissions],
    bankDataScope: row.bankDataScope,
    assignedBankIds: [...row.assignedBankIds],
    reason: '',
    version: row.version,
  })
  accessVisible.value = true
}

function openStatus(row: AdminRow, action: AdminAccountAction): void {
  Object.assign(statusForm, {
    adminId: row.id,
    displayName: row.displayName,
    action,
    reason: '',
    version: row.version,
  })
  statusVisible.value = true
}

function requireReauth(operation: RiskOperation): void {
  pendingRiskOperation.value = operation
  reauthDialog.value?.open('admin:manage')
}

async function executeRiskOperation(reauthToken: string): Promise<void> {
  try {
    if (pendingRiskOperation.value === 'invite') {
      const result = await createAdminInvitation(
        {
          ...inviteForm,
          assignedBankIds:
            inviteForm.bankDataScope === BankDataScope.ALL_BANKS ? [] : inviteForm.assignedBankIds,
        },
        reauthToken,
      )
      try {
        await navigator.clipboard.writeText(result.invitationUrl)
        ElMessage.success('邀请已创建，链接已复制')
      } catch {
        ElMessage.success(`邀请已创建：${result.invitationUrl}`)
      }
      inviteVisible.value = false
    } else if (pendingRiskOperation.value === 'access') {
      await updateAdminAccess(
        accessForm.adminId,
        {
          permissions: accessForm.permissions,
          bankDataScope: accessForm.bankDataScope,
          assignedBankIds:
            accessForm.bankDataScope === BankDataScope.ALL_BANKS ? [] : accessForm.assignedBankIds,
          reason: accessForm.reason.trim(),
          version: accessForm.version,
        },
        reauthToken,
      )
      ElMessage.success('管理员权限已更新')
      accessVisible.value = false
    } else if (pendingRiskOperation.value === 'status') {
      await actOnAdmin(
        statusForm.adminId,
        {
          action: statusForm.action,
          reason: statusForm.reason.trim(),
          version: statusForm.version,
        },
        reauthToken,
      )
      ElMessage.success('管理员状态已更新')
      statusVisible.value = false
    }
    await load()
  } catch (error) {
    showApiError(error)
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="管理员" description="邀请普通管理员，并维护功能权限和可访问题库范围">
      <el-button type="primary" @click="openInvite">邀请管理员</el-button>
    </PageHeader>
    <section class="panel">
      <div class="filter-bar">
        <el-input v-model="query.keyword" clearable placeholder="邮箱或姓名" @keyup.enter="search" />
        <el-select v-model="query.status" clearable placeholder="账号状态">
          <el-option label="正常" :value="AdminStatus.ACTIVE" />
          <el-option label="禁用" :value="AdminStatus.DISABLED" />
          <el-option label="已归档" :value="AdminStatus.ARCHIVED" />
        </el-select>
        <el-button type="primary" plain @click="search">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="admins">
        <el-table-column label="管理员" min-width="220">
          <template #default="{ row }"><strong>{{ row.displayName }}</strong><br /><small>{{ row.email }}</small></template>
        </el-table-column>
        <el-table-column label="角色" width="130">
          <template #default="{ row }">{{ row.role === AdminRole.SUPER_ADMIN ? '超级管理员' : '管理员' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :value="adminStatusNames[row.status]" /></template>
        </el-table-column>
        <el-table-column label="权限数" width="90" align="center">
          <template #default="{ row }">{{ row.permissions.length }}</template>
        </el-table-column>
        <el-table-column label="题库范围" min-width="180">
          <template #default="{ row }">
            {{ row.bankDataScope === BankDataScope.ALL_BANKS ? '全部题库' : `指定 ${row.assignedBankIds.length} 个题库` }}
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="170">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.role !== AdminRole.SUPER_ADMIN">
              <el-button link type="primary" @click="openAccess(row)">权限</el-button>
              <el-dropdown trigger="click">
                <el-button link>状态</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="row.status === AdminStatus.ACTIVE" @click="openStatus(row, AdminAccountAction.DISABLE)">禁用</el-dropdown-item>
                    <el-dropdown-item v-if="row.status === AdminStatus.DISABLED" @click="openStatus(row, AdminAccountAction.ACTIVATE)">激活</el-dropdown-item>
                    <el-dropdown-item divided @click="openStatus(row, AdminAccountAction.ARCHIVE)">归档</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
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

    <el-drawer v-model="inviteVisible" title="邀请管理员" size="570px">
      <el-form label-position="top">
        <el-form-item label="邮箱" required><el-input v-model="inviteForm.email" /></el-form-item>
        <el-form-item label="显示名称" required><el-input v-model="inviteForm.displayName" maxlength="100" /></el-form-item>
        <el-form-item label="功能权限" required>
          <el-checkbox-group v-model="inviteForm.permissions" class="permission-grid">
            <el-checkbox v-for="[value, label] in permissionOptions" :key="value" :value="value">{{ label }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="题库范围" required>
          <el-radio-group v-model="inviteForm.bankDataScope">
            <el-radio :value="BankDataScope.ALL_BANKS">全部题库</el-radio>
            <el-radio :value="BankDataScope.ASSIGNED_BANKS">指定题库</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="inviteForm.bankDataScope === BankDataScope.ASSIGNED_BANKS" label="可访问题库" required>
          <el-select v-model="inviteForm.assignedBankIds" multiple filterable>
            <el-option v-for="bank in banks" :key="bank.id" :label="bank.bankName" :value="bank.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="邀请原因" required>
          <el-input v-model="inviteForm.reason" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inviteVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="
            !inviteForm.email ||
            !inviteForm.displayName ||
            !inviteForm.permissions.length ||
            !inviteForm.reason.trim() ||
            (inviteForm.bankDataScope === BankDataScope.ASSIGNED_BANKS && !inviteForm.assignedBankIds.length)
          "
          @click="requireReauth('invite')"
        >
          验证并创建邀请
        </el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="accessVisible" :title="`编辑权限 · ${accessForm.displayName}`" size="570px">
      <el-form label-position="top">
        <el-form-item label="功能权限" required>
          <el-checkbox-group v-model="accessForm.permissions" class="permission-grid">
            <el-checkbox v-for="[value, label] in permissionOptions" :key="value" :value="value">{{ label }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="题库范围">
          <el-radio-group v-model="accessForm.bankDataScope">
            <el-radio :value="BankDataScope.ALL_BANKS">全部题库</el-radio>
            <el-radio :value="BankDataScope.ASSIGNED_BANKS">指定题库</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="accessForm.bankDataScope === BankDataScope.ASSIGNED_BANKS" label="可访问题库">
          <el-select v-model="accessForm.assignedBankIds" multiple filterable>
            <el-option v-for="bank in banks" :key="bank.id" :label="bank.bankName" :value="bank.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="修改原因" required>
          <el-input v-model="accessForm.reason" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="accessVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="
            !accessForm.permissions.length ||
            !accessForm.reason.trim() ||
            (accessForm.bankDataScope === BankDataScope.ASSIGNED_BANKS && !accessForm.assignedBankIds.length)
          "
          @click="requireReauth('access')"
        >
          验证并保存
        </el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="statusVisible" title="修改管理员状态" width="480px">
      <p>{{ statusForm.displayName }} · {{ adminAccountActionLabels[statusForm.action] }}</p>
      <el-input
        v-model="statusForm.reason"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-word-limit
        placeholder="填写操作原因"
      />
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!statusForm.reason.trim()" @click="requireReauth('status')">
          验证并确认
        </el-button>
      </template>
    </el-dialog>

    <ReauthDialog ref="reauthDialog" @success="executeRiskOperation" />
  </div>
</template>

<style scoped>
.filter-bar {
  margin-bottom: 18px;
}

small {
  color: var(--text-secondary);
}

.permission-grid {
  display: grid;
  width: 100%;
  grid-template-columns: 1fr 1fr;
}

.el-drawer .el-select {
  width: 100%;
}
</style>
