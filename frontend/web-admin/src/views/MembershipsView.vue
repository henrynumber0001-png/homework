<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import ReauthDialog from '@/components/ReauthDialog.vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  actOnMembership,
  createMembershipPlan,
  getMembership,
  listMembershipOrders,
  listMembershipPlans,
  listMemberships,
  updateMembershipPlan,
} from '@/api/admin'
import { showApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { MembershipDetail, MembershipOrder, MembershipPlan, MembershipRow } from '@/types/admin'
import { formatDateTime } from '@/utils/format'
import { membershipTypeLabels } from '@/utils/dictionaries'

const auth = useAuthStore()
const activeTab = ref('members')
const loading = ref(false)
const members = ref<MembershipRow[]>([])
const orders = ref<MembershipOrder[]>([])
const plans = ref<MembershipPlan[]>([])
const total = ref(0)
const actionVisible = ref(false)
const actionSaving = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<MembershipDetail | null>(null)
const planVisible = ref(false)
const reauthDialog = ref<InstanceType<typeof ReauthDialog>>()
const planMode = ref<'create' | 'edit'>('create')
const query = reactive({
  keyword: '',
  membershipType: '',
  orderStatus: '',
  pageNum: 1,
  pageSize: 20,
})
const actionForm = reactive({
  userId: 0,
  displayName: '',
  action: 'GRANT',
  membershipType: 'PREMIUM',
  durationMonths: 1,
  reason: '',
  ledgerVersion: 0,
})
const planForm = reactive({
  id: 0,
  membershipType: 'PREMIUM',
  purchaseType: 'FULL',
  durationMonths: 1,
  billingType: 'MONTHLY',
  price: 0,
  currency: 'CNY',
  enabled: false,
  reason: '',
  version: 0,
})

onMounted(load)

async function load(): Promise<void> {
  loading.value = true
  try {
    if (activeTab.value === 'members') {
      const result = await listMemberships({
        keyword: query.keyword || undefined,
        membershipType: query.membershipType || undefined,
        pageNum: query.pageNum,
        pageSize: query.pageSize,
      })
      members.value = result.records
      total.value = result.total
    } else if (activeTab.value === 'orders') {
      const result = await listMembershipOrders({
        keyword: query.keyword || undefined,
        orderStatus: query.orderStatus || undefined,
        pageNum: query.pageNum,
        pageSize: query.pageSize,
      })
      orders.value = result.records
      total.value = result.total
    } else {
      plans.value = await listMembershipPlans()
      total.value = plans.value.length
    }
  } catch (error) {
    showApiError(error)
  } finally {
    loading.value = false
  }
}

function switchTab(): void {
  Object.assign(query, { keyword: '', membershipType: '', orderStatus: '', pageNum: 1 })
  void load()
}

function search(): void {
  query.pageNum = 1
  void load()
}

function openAction(row: MembershipRow, action: string): void {
  Object.assign(actionForm, {
    userId: row.userId,
    displayName: row.displayName,
    action,
    membershipType: 'PREMIUM',
    durationMonths: 1,
    reason: '',
    ledgerVersion: row.ledgerVersion,
  })
  actionVisible.value = true
}

async function openDetail(row: MembershipRow): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await getMembership(row.userId)
  } catch (error) {
    showApiError(error)
  } finally {
    detailLoading.value = false
  }
}

async function saveAction(): Promise<void> {
  if (!actionForm.reason.trim()) return
  actionSaving.value = true
  try {
    await actOnMembership(actionForm.userId, {
      action: actionForm.action,
      membershipType: actionForm.action === 'GRANT' ? actionForm.membershipType : undefined,
      durationMonths: actionForm.action === 'GRANT' ? actionForm.durationMonths : undefined,
      reason: actionForm.reason.trim(),
      ledgerVersion: actionForm.ledgerVersion,
    })
    ElMessage.success('会员状态已更新')
    actionVisible.value = false
    await load()
  } catch (error) {
    showApiError(error)
  } finally {
    actionSaving.value = false
  }
}

function openPlanCreate(): void {
  planMode.value = 'create'
  Object.assign(planForm, {
    id: 0,
    membershipType: 'PREMIUM',
    purchaseType: 'FULL',
    durationMonths: 1,
    billingType: 'MONTHLY',
    price: 0,
    currency: 'CNY',
    enabled: false,
    reason: '',
    version: 0,
  })
  planVisible.value = true
}

function openPlanEdit(plan: MembershipPlan): void {
  planMode.value = 'edit'
  Object.assign(planForm, {
    id: plan.id,
    membershipType: plan.membershipType,
    purchaseType: plan.purchaseType,
    durationMonths: plan.durationMonths,
    billingType: plan.billingType || '',
    price: Number(plan.price),
    currency: plan.currency,
    enabled: plan.enabled,
    reason: '',
    version: plan.version,
  })
  planVisible.value = true
}

function requestPlanSave(): void {
  if (!planForm.reason.trim()) return
  reauthDialog.value?.open('membership:plan')
}

async function savePlan(reauthToken: string): Promise<void> {
  try {
    if (planMode.value === 'create') {
      await createMembershipPlan(
        {
          membershipType: planForm.membershipType,
          purchaseType: planForm.purchaseType,
          durationMonths: planForm.durationMonths,
          billingType: planForm.purchaseType === 'FULL' ? planForm.billingType || undefined : undefined,
          price: planForm.price,
          currency: planForm.currency,
          enabled: planForm.enabled,
          reason: planForm.reason.trim(),
        },
        reauthToken,
      )
      ElMessage.success('会员套餐已创建')
    } else {
      await updateMembershipPlan(
        planForm.id,
        {
          price: planForm.price,
          enabled: planForm.enabled,
          reason: planForm.reason.trim(),
          version: planForm.version,
        },
        reauthToken,
      )
      ElMessage.success('会员套餐已更新')
    }
    planVisible.value = false
    await load()
  } catch (error) {
    showApiError(error)
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="会员与订单" description="基础会员发放、暂停与恢复；订单仅查询，当前版本不开放退款" />
    <section class="panel">
      <el-tabs v-model="activeTab" @tab-change="switchTab">
        <el-tab-pane label="会员用户" name="members" />
        <el-tab-pane label="支付订单" name="orders" />
        <el-tab-pane label="套餐配置" name="plans" />
      </el-tabs>
      <div v-if="activeTab === 'plans' && auth.hasPermission('membership:plan')" class="plan-toolbar">
        <span>套餐变更需要二次认证。</span>
        <el-button type="primary" @click="openPlanCreate">创建套餐</el-button>
      </div>
      <div v-if="activeTab !== 'plans'" class="filter-bar">
        <el-input
          v-model="query.keyword"
          clearable
          :placeholder="activeTab === 'members' ? '账号、昵称或用户 ID' : '订单号或用户 ID'"
          @keyup.enter="search"
        />
        <el-select v-if="activeTab === 'members'" v-model="query.membershipType" clearable placeholder="会员等级">
          <el-option label="Premium" value="PREMIUM" />
          <el-option label="Premium Plus" value="PREMIUM_PLUS" />
        </el-select>
        <el-select v-else v-model="query.orderStatus" clearable placeholder="订单状态">
          <el-option label="已支付" value="PAID" />
          <el-option label="待支付" value="PENDING" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
        <el-button type="primary" plain @click="search">查询</el-button>
      </div>

      <el-table v-if="activeTab === 'members'" v-loading="loading" :data="members">
        <el-table-column prop="userId" label="用户 ID" width="100" />
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }"><strong>{{ row.displayName }}</strong><br /><small>{{ row.accountNo }}</small></template>
        </el-table-column>
        <el-table-column label="当前等级" width="150">
          <template #default="{ row }">{{ membershipTypeLabels[row.currentType] || row.currentType }}</template>
        </el-table-column>
        <el-table-column label="访问状态" width="120">
          <template #default="{ row }"><StatusTag :value="row.accessStatus" /></template>
        </el-table-column>
        <el-table-column label="Premium 到期" width="170">
          <template #default="{ row }">{{ formatDateTime(row.premiumExpireTime) }}</template>
        </el-table-column>
        <el-table-column label="Premium Plus 到期" width="180">
          <template #default="{ row }">{{ formatDateTime(row.premiumPlusExpireTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <template v-if="auth.hasPermission('membership:manage')">
              <el-button link type="primary" @click="openAction(row, 'GRANT')">发放</el-button>
              <el-button v-if="!row.suspended" link @click="openAction(row, 'SUSPEND')">暂停</el-button>
              <el-button v-else link @click="openAction(row, 'RESUME')">恢复</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-else-if="activeTab === 'orders'" v-loading="loading" :data="orders">
        <el-table-column prop="orderNo" label="订单号" min-width="200" />
        <el-table-column prop="userId" label="用户 ID" width="100" />
        <el-table-column label="套餐" width="150">
          <template #default="{ row }">{{ membershipTypeLabels[row.membershipType] || row.membershipType }} · {{ row.durationMonths }} 个月</template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="{ row }">{{ row.currency }} {{ Number(row.payAmount).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :value="row.orderStatus" /></template>
        </el-table-column>
        <el-table-column label="支付时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.payTime) }}</template>
        </el-table-column>
        <el-table-column label="退款" width="100">
          <template #default>不开放</template>
        </el-table-column>
      </el-table>

      <el-table v-else v-loading="loading" :data="plans">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="会员等级" min-width="160">
          <template #default="{ row }">{{ membershipTypeLabels[row.membershipType] || row.membershipType }}</template>
        </el-table-column>
        <el-table-column prop="purchaseType" label="购买类型" width="120" />
        <el-table-column prop="durationMonths" label="月数" width="90" />
        <el-table-column prop="billingType" label="计费类型" width="120" />
        <el-table-column label="价格" width="130">
          <template #default="{ row }">{{ row.currency }} {{ Number(row.price).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :value="row.enabled ? 'ACTIVE' : 'DISABLED'" /></template>
        </el-table-column>
        <el-table-column v-if="auth.hasPermission('membership:plan')" label="操作" width="100">
          <template #default="{ row }"><el-button link type="primary" @click="openPlanEdit(row)">编辑</el-button></template>
        </el-table-column>
      </el-table>

      <div v-if="activeTab !== 'plans'" class="table-footer">
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

    <el-dialog
      v-model="actionVisible"
      :title="actionForm.action === 'GRANT' ? '发放会员' : actionForm.action === 'SUSPEND' ? '暂停会员' : '恢复会员'"
      width="500px"
    >
      <p class="dialog-user">{{ actionForm.displayName }} · 用户 ID {{ actionForm.userId }}</p>
      <el-form label-position="top">
        <template v-if="actionForm.action === 'GRANT'">
          <el-form-item label="会员等级" required>
            <el-radio-group v-model="actionForm.membershipType">
              <el-radio-button value="PREMIUM">Premium</el-radio-button>
              <el-radio-button value="PREMIUM_PLUS">Premium Plus</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="发放月数" required>
            <el-input-number v-model="actionForm.durationMonths" :min="1" :max="120" />
          </el-form-item>
        </template>
        <el-form-item label="操作原因" required>
          <el-input v-model="actionForm.reason" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionSaving" :disabled="!actionForm.reason.trim()" @click="saveAction">
          确认
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="会员详情" size="600px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-alert
            title="会员暂停期间到期时间继续流逝，恢复后不会补回暂停时长。"
            type="info"
            :closable="false"
            show-icon
          />
          <el-descriptions class="membership-detail" :column="1" border>
            <el-descriptions-item label="用户">{{ detail.displayName }} · {{ detail.accountNo }}</el-descriptions-item>
            <el-descriptions-item label="当前等级">
              {{ membershipTypeLabels[detail.currentType] || detail.currentType }}
            </el-descriptions-item>
            <el-descriptions-item label="Premium 到期">{{ formatDateTime(detail.premiumExpireTime) }}</el-descriptions-item>
            <el-descriptions-item label="Premium Plus 到期">{{ formatDateTime(detail.premiumPlusExpireTime) }}</el-descriptions-item>
            <el-descriptions-item label="暂停状态">{{ detail.suspended ? '已暂停' : '正常' }}</el-descriptions-item>
          </el-descriptions>
          <h3>最近变更</h3>
          <el-table :data="detail.recentChanges" max-height="360">
            <el-table-column prop="changeType" label="变更" width="110" />
            <el-table-column prop="membershipType" label="等级" width="130" />
            <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
            <el-table-column label="时间" width="160">
              <template #default="{ row }">{{ formatDateTime(row.createdTime) }}</template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-drawer>

    <el-dialog v-model="planVisible" :title="planMode === 'create' ? '创建会员套餐' : '编辑会员套餐'" width="540px">
      <el-form label-position="top">
        <el-form-item label="会员等级" required>
          <el-select v-model="planForm.membershipType" :disabled="planMode === 'edit'">
            <el-option label="Premium" value="PREMIUM" />
            <el-option label="Premium Plus" value="PREMIUM_PLUS" />
          </el-select>
        </el-form-item>
        <div class="form-row">
          <el-form-item label="购买类型" required>
            <el-select v-model="planForm.purchaseType" :disabled="planMode === 'edit'">
              <el-option label="完整购买" value="FULL" />
              <el-option label="补差升级" value="DIFF" />
            </el-select>
          </el-form-item>
          <el-form-item label="时长（月）" required>
            <el-input-number v-model="planForm.durationMonths" :min="1" :max="12" :disabled="planMode === 'edit'" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="计费类型">
            <el-select
              v-model="planForm.billingType"
              :disabled="planMode === 'edit' || planForm.purchaseType === 'DIFF'"
            >
              <el-option label="按月" value="MONTHLY" />
              <el-option label="按季" value="QUARTERLY" />
              <el-option label="按年" value="YEARLY" />
            </el-select>
          </el-form-item>
          <el-form-item label="币种" required>
            <el-input v-model="planForm.currency" maxlength="10" :disabled="planMode === 'edit'" />
          </el-form-item>
        </div>
        <el-form-item label="价格" required>
          <el-input-number v-model="planForm.price" :min="0.01" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="planForm.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="操作原因" required>
          <el-input v-model="planForm.reason" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!planForm.reason.trim() || planForm.price < 0.01"
          @click="requestPlanSave"
        >
          验证并保存
        </el-button>
      </template>
    </el-dialog>
    <ReauthDialog ref="reauthDialog" @success="savePlan" />
  </div>
</template>

<style scoped>
.filter-bar {
  margin-bottom: 18px;
}

.plan-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.plan-toolbar span {
  margin-right: 12px;
  color: var(--text-secondary);
  font-size: 12px;
}

small {
  color: var(--text-secondary);
}

.dialog-user {
  margin: -5px 0 20px;
  color: var(--text-secondary);
}

.form-row {
  display: grid;
  gap: 14px;
  grid-template-columns: 1fr 1fr;
}

.form-row .el-select {
  width: 100%;
}

.membership-detail {
  margin-top: 18px;
}

h3 {
  margin: 24px 0 10px;
  font-size: 15px;
}
</style>
