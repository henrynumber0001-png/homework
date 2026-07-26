<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { listAuditLogs } from '@/api/admin'
import { showApiError } from '@/api/http'
import type { AuditLog } from '@/types/admin'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const logs = ref<AuditLog[]>([])
const total = ref(0)
const detailVisible = ref(false)
const selectedLog = ref<AuditLog | null>(null)
const query = reactive({
  module: '',
  action: '',
  targetId: '',
  startTime: '',
  endTime: '',
  pageNum: 1,
  pageSize: 20,
})

onMounted(load)

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await listAuditLogs({
      module: query.module || undefined,
      action: query.action || undefined,
      targetId: query.targetId || undefined,
      startTime: query.startTime || undefined,
      endTime: query.endTime || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    })
    logs.value = result.records
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

function openDetail(row: AuditLog): void {
  selectedLog.value = row
  detailVisible.value = true
}

function formatSnapshot(value?: string): string {
  if (!value) return '—'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="操作日志" description="按操作人权限范围查询管理动作；V1 不提供导出" />
    <section class="panel">
      <div class="filter-bar">
        <el-select v-model="query.module" clearable placeholder="业务模块">
          <el-option label="题库" value="BANK" />
          <el-option label="题目" value="QUESTION" />
          <el-option label="用户" value="USER" />
          <el-option label="会员" value="MEMBERSHIP" />
          <el-option label="管理员" value="ADMIN" />
          <el-option label="社区" value="COMMUNITY" />
        </el-select>
        <el-input v-model="query.action" clearable placeholder="动作，如 UPDATE" />
        <el-input v-model="query.targetId" clearable placeholder="目标 ID" />
        <el-date-picker
          v-model="query.startTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="开始时间"
        />
        <el-date-picker
          v-model="query.endTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="结束时间"
        />
        <el-button type="primary" plain @click="search">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="logs">
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column label="操作者" min-width="150">
          <template #default="{ row }"><strong>{{ row.operatorName }}</strong><br /><small>#{{ row.operatorAdminId }}</small></template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="110" />
        <el-table-column prop="action" label="动作" width="110" />
        <el-table-column label="目标" min-width="150">
          <template #default="{ row }">{{ row.targetType }} #{{ row.targetId }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="230" show-overflow-tooltip />
        <el-table-column label="结果" width="90">
          <template #default="{ row }"><StatusTag :value="row.success ? 'ACTIVE' : 'FAILED'" /></template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">详情</el-button></template>
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

    <el-drawer v-model="detailVisible" title="操作日志详情" size="700px">
      <template v-if="selectedLog">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="Request ID">{{ selectedLog.requestId }}</el-descriptions-item>
          <el-descriptions-item label="操作者">{{ selectedLog.operatorName }} #{{ selectedLog.operatorAdminId }}</el-descriptions-item>
          <el-descriptions-item label="动作">{{ selectedLog.module }} / {{ selectedLog.action }}</el-descriptions-item>
          <el-descriptions-item label="目标">{{ selectedLog.targetType }} #{{ selectedLog.targetId }}</el-descriptions-item>
          <el-descriptions-item label="原因">{{ selectedLog.reason || '—' }}</el-descriptions-item>
          <el-descriptions-item label="来源 IP">{{ selectedLog.ip }}</el-descriptions-item>
          <el-descriptions-item label="失败信息">{{ selectedLog.failureMessage || '—' }}</el-descriptions-item>
        </el-descriptions>
        <h3>变更前</h3>
        <pre>{{ formatSnapshot(selectedLog.beforeSnapshot) }}</pre>
        <h3>变更后</h3>
        <pre>{{ formatSnapshot(selectedLog.afterSnapshot) }}</pre>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.filter-bar {
  margin-bottom: 18px;
}

.filter-bar :deep(.el-date-editor) {
  width: 190px;
}

small {
  color: var(--text-secondary);
}

h3 {
  margin: 24px 0 9px;
  font-size: 14px;
}

pre {
  max-height: 280px;
  padding: 14px;
  overflow: auto;
  color: #d9e2f2;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  background: #182136;
  border-radius: 9px;
}
</style>
