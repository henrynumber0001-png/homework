<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Collection, Connection, DataAnalysis, User } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getDashboard } from '@/api/admin'
import { showApiError } from '@/api/http'
import type { Dashboard } from '@/types/admin'
import { formatDateTime, formatNumber } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const dashboard = ref<Dashboard | null>(null)

const cards = computed(() => {
  if (!dashboard.value) return []
  return [
    { label: '今日题库浏览', value: dashboard.value.bankViews.daily, total: dashboard.value.bankViews.total, icon: Collection },
    { label: '今日完成题库', value: dashboard.value.bankCompletedUsers.daily, total: dashboard.value.bankCompletedUsers.total, icon: DataAnalysis },
    { label: '今日登录用户', value: dashboard.value.loginUsers.daily, total: dashboard.value.loginUsers.total, icon: Connection },
    { label: '今日注册用户', value: dashboard.value.registeredUsers.daily, total: dashboard.value.registeredUsers.total, icon: User },
  ]
})

onMounted(load)

async function load(): Promise<void> {
  loading.value = true
  try {
    dashboard.value = await getDashboard()
  } catch (error) {
    showApiError(error)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div v-loading="loading" class="page">
    <PageHeader
      title="数据概览"
      :description="dashboard ? `统计日期 ${dashboard.statDate} · 更新于 ${formatDateTime(dashboard.updatedTime)}` : '查看核心业务指标'"
    >
      <el-button @click="load">刷新数据</el-button>
      <el-button type="primary" @click="router.push('/question-banks')">进入题库</el-button>
    </PageHeader>

    <section class="metric-grid">
      <article v-for="card in cards" :key="card.label" class="metric-card">
        <div class="metric-icon"><component :is="card.icon" /></div>
        <span>{{ card.label }}</span>
        <strong>{{ formatNumber(card.value) }}</strong>
        <small>累计 {{ formatNumber(card.total) }}</small>
      </article>
    </section>

    <section v-if="dashboard" class="lower-grid">
      <article class="panel">
        <div class="panel-title">
          <div><h2>内容与社区</h2><p>今日内容活跃情况</p></div>
        </div>
        <div class="data-rows">
          <div><span>发帖用户</span><strong>{{ formatNumber(dashboard.postingUsers.daily) }}</strong></div>
          <div><span>累计发帖用户</span><strong>{{ formatNumber(dashboard.postingUsers.total) }}</strong></div>
        </div>
      </article>
      <article class="panel">
        <div class="panel-title">
          <div><h2>付费用户</h2><p>会员用户规模</p></div>
        </div>
        <div class="paid-grid">
          <div><span>今日 Premium</span><strong>{{ formatNumber(dashboard.paidUsers.premiumDaily) }}</strong></div>
          <div><span>Premium 总数</span><strong>{{ formatNumber(dashboard.paidUsers.premiumTotal) }}</strong></div>
          <div><span>今日 Premium Plus</span><strong>{{ formatNumber(dashboard.paidUsers.premiumPlusDaily) }}</strong></div>
          <div><span>Premium Plus 总数</span><strong>{{ formatNumber(dashboard.paidUsers.premiumPlusTotal) }}</strong></div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.metric-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: repeat(4, 1fr);
}

.metric-card {
  display: grid;
  gap: 5px;
  padding: 21px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  grid-template-columns: 46px 1fr;
}

.metric-icon {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  color: var(--brand);
  font-size: 21px;
  background: var(--brand-soft);
  border-radius: 10px;
  grid-row: span 3;
}

.metric-card span {
  color: var(--text-secondary);
  font-size: 12px;
}

.metric-card strong {
  font-size: 26px;
}

.metric-card small {
  color: #8c96a8;
  font-size: 11px;
}

.lower-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: 0.8fr 1.2fr;
}

.panel-title h2,
.panel-title p {
  margin: 0;
}

.panel-title h2 {
  font-size: 17px;
}

.panel-title p {
  margin-top: 5px;
  color: var(--text-secondary);
  font-size: 12px;
}

.data-rows,
.paid-grid {
  display: grid;
  gap: 12px;
  margin-top: 20px;
}

.data-rows {
  grid-template-columns: 1fr 1fr;
}

.paid-grid {
  grid-template-columns: repeat(4, 1fr);
}

.data-rows div,
.paid-grid div {
  padding: 16px;
  background: var(--surface-soft);
  border-radius: 9px;
}

.data-rows span,
.data-rows strong,
.paid-grid span,
.paid-grid strong {
  display: block;
}

.data-rows span,
.paid-grid span {
  color: var(--text-secondary);
  font-size: 11px;
}

.data-rows strong,
.paid-grid strong {
  margin-top: 7px;
  font-size: 20px;
}
</style>
