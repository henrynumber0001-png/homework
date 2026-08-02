<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Collection, Plus, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import { createQuestionBank, getCategoryTree, listQuestionBanks } from '@/api/admin'
import { showApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import {
  AdminSortMode,
  QuestionBankStatus,
  type AdminSortMode as AdminSortModeValue,
  type CategoryGroup,
  type QuestionBank,
  type QuestionBankStatus as QuestionBankStatusValue,
} from '@/types/admin'
import { formatDateTime } from '@/utils/format'
import { bankStatusNames, groupTypeLabels } from '@/utils/dictionaries'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const createVisible = ref(false)
const creating = ref(false)
const banks = ref<QuestionBank[]>([])
const categories = ref<CategoryGroup[]>([])
const total = ref(0)
const query = reactive({
  keyword: '',
  groupId: undefined as number | undefined,
  moduleId: undefined as number | undefined,
  subModuleId: undefined as number | undefined,
  sortMode: AdminSortMode.UPDATED_TIME_DESC as AdminSortModeValue,
  status: '' as QuestionBankStatusValue | '',
  pageNum: 1,
  pageSize: 12,
})
const createForm = reactive({
  subModuleId: undefined as number | undefined,
  bankName: '',
  tags: [] as string[],
  // 变更：原 priority 改为默认 10 的人工曝光权重。
  sortOrder: 10,
})

const cascaderOptions = computed(() =>
  categories.value.map((group) => ({
    value: group.id,
    label: group.groupName,
    children: group.modules.map((module) => ({
      value: module.id,
      label: module.moduleName,
      children: module.subModules.map((subModule) => ({
        value: subModule.id,
        label: subModule.subModuleName,
      })),
    })),
  })),
)

// 未选择 Group 时不提供 Module 选项；选择 Group 后只显示它的下级 Module。
const moduleOptions = computed(() =>
  query.groupId === undefined
    ? []
    : categories.value.find((group) => group.id === query.groupId)?.modules ?? [],
)

// 未选择 Module 时不提供 SubModule 选项；选择 Module 后只显示它的下级 SubModule。
const subModuleOptions = computed(() =>
  query.moduleId === undefined
    ? []
    : moduleOptions.value.find((module) => module.id === query.moduleId)?.subModules ?? [],
)

onMounted(async () => {
  await Promise.all([loadCategories(), loadBanks()])
})

async function loadCategories(): Promise<void> {
  try {
    categories.value = await getCategoryTree()
  } catch (error) {
    showApiError(error)
  }
}

async function loadBanks(): Promise<void> {
  loading.value = true
  try {
    const result = await listQuestionBanks({
      keyword: query.keyword || undefined,
      groupId: query.groupId,
      moduleId: query.moduleId,
      subModuleId: query.subModuleId,
      status: query.status || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      // 变更：原 sortBy + sortDirection 合并为一个明确的排序模式。
      sortMode: query.sortMode,
    })
    banks.value = result.records
    total.value = result.total
  } catch (error) {
    showApiError(error)
  } finally {
    loading.value = false
  }
}

function search(): void {
  query.pageNum = 1
  void loadBanks()
}

function resetFilters(): void {
  Object.assign(query, {
    keyword: '',
    groupId: undefined,
    moduleId: undefined,
    subModuleId: undefined,
    sortMode: AdminSortMode.UPDATED_TIME_DESC,
    status: '',
    pageNum: 1,
  })
  void loadBanks()
}

function openCreate(): void {
  Object.assign(createForm, { subModuleId: undefined, bankName: '', tags: [], sortOrder: 10 })
  createVisible.value = true
}

function selectCategory(path: Array<string | number> | null): void {
  createForm.subModuleId = path?.length ? Number(path.at(-1)) : undefined
}

async function submitCreate(): Promise<void> {
  if (!createForm.subModuleId || !createForm.bankName.trim() || !createForm.tags.length) return
  creating.value = true
  try {
    const bank = await createQuestionBank({
      subModuleId: createForm.subModuleId,
      bankName: createForm.bankName.trim(),
      tags: createForm.tags,
      // 变更：创建题库发送 sortOrder，普通题库保持默认权重 10。
      sortOrder: createForm.sortOrder,
    })
    ElMessage.success('题库已创建')
    createVisible.value = false
    await router.push(`/question-banks/${bank.id}`)
  } catch (error) {
    showApiError(error)
  } finally {
    creating.value = false
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="题库与题目" description="题库是题目的容器；先进入题库，再创建、导入和维护题目">
      <el-button v-if="auth.hasPermission('bank:create')" type="primary" :icon="Plus" @click="openCreate">
        创建题库
      </el-button>
    </PageHeader>

    <section class="panel filter-panel">
      <div class="filter-bar">
        <el-input
          class="filter-keyword"
          v-model="query.keyword"
          :prefix-icon="Search"
          clearable
          placeholder="搜索题库名称"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select
          class="filter-type"
          v-model="query.groupId"
          clearable
          placeholder="题库类型"
          @change="Object.assign(query, { moduleId: undefined, subModuleId: undefined })"
        >
          <el-option
            v-for="group in categories"
            :key="group.id"
            :label="group.groupName"
            :value="group.id"
          />
        </el-select>
        <!-- 严格三级联动：必须先选择 Group，才允许选择 Module。 -->
        <el-select
          class="filter-module"
          v-model="query.moduleId"
          clearable
          :disabled="query.groupId === undefined"
          placeholder="模块"
          @change="query.subModuleId = undefined"
        >
          <el-option
            v-for="module in moduleOptions"
            :key="module.id"
            :label="module.moduleName"
            :value="module.id"
          />
        </el-select>
        <!-- 必须先选择 Module，才允许选择 SubModule。 -->
        <el-select
          class="filter-submodule"
          v-model="query.subModuleId"
          clearable
          :disabled="query.moduleId === undefined"
          placeholder="子模块"
        >
          <el-option
            v-for="subModule in subModuleOptions"
            :key="subModule.id"
            :label="subModule.subModuleName"
            :value="subModule.id"
          />
        </el-select>
        <el-select class="filter-status" v-model="query.status" clearable placeholder="发布状态">
          <el-option label="草稿" :value="QuestionBankStatus.DRAFT" />
          <el-option label="已发布" :value="QuestionBankStatus.PUBLISHED" />
          <el-option label="已下架" :value="QuestionBankStatus.OFFLINE" />
        </el-select>
        <!-- 变更：排序模式是请求参数，不是新增数据库字段。 -->
        <el-select class="filter-sort" v-model="query.sortMode" placeholder="排序方式" @change="search">
          <el-option label="按更新时间降序" :value="AdminSortMode.UPDATED_TIME_DESC" />
          <el-option label="按题库权重降序" :value="AdminSortMode.SORT_ORDER_DESC" />
        </el-select>
        <el-button type="primary" plain @click="search">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
        <span class="result-count">共 {{ total }} 个题库</span>
      </div>
    </section>

    <section v-loading="loading">
      <div v-if="banks.length" class="bank-grid">
        <article
          v-for="bank in banks"
          :key="bank.id"
          class="bank-card"
          tabindex="0"
          @click="router.push(`/question-banks/${bank.id}`)"
          @keyup.enter="router.push(`/question-banks/${bank.id}`)"
        >
          <div class="card-top">
            <div class="bank-icon"><Collection /></div>
            <StatusTag :value="bankStatusNames[bank.status]" />
          </div>
          <div class="bank-type">{{ groupTypeLabels[bank.groupType] }}</div>
          <h2>{{ bank.bankName }}</h2>
          <p>{{ bank.module.name }} / {{ bank.subModule.name }}</p>
          <div v-if="bank.tags?.length" class="tags">
            <el-tag v-for="tag in bank.tags.slice(0, 3)" :key="tag" effect="plain" size="small">
              {{ tag }}
            </el-tag>
          </div>
          <div class="metrics">
            <div><strong>{{ bank.questionCount }}</strong><span>题目</span></div>
            <div><strong>{{ bank.releasedQuestionCount }}</strong><span>已发布</span></div>
            <div><strong>{{ bank.viewCount }}</strong><span>浏览</span></div>
          </div>
          <div class="card-footer">
            <span>更新于 {{ formatDateTime(bank.updatedTime) }}</span>
            <el-button link type="primary">进入工作台 →</el-button>
          </div>
        </article>
      </div>
      <el-empty v-else-if="!loading" description="没有符合条件的题库">
        <el-button v-if="auth.hasPermission('bank:create')" type="primary" @click="openCreate">
          创建第一个题库
        </el-button>
      </el-empty>
    </section>

    <div v-if="total > query.pageSize" class="pagination">
      <el-pagination
        v-model:current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadBanks"
      />
    </div>

    <el-drawer v-model="createVisible" title="创建题库" size="520px">
      <el-alert
        title="题库只能手动创建，创建后可在题库工作台中添加或导入题目。"
        type="info"
        :closable="false"
        show-icon
      />
      <el-form class="drawer-form" label-position="top" @submit.prevent="submitCreate">
        <el-form-item label="题库分类" required>
          <el-cascader
            :options="cascaderOptions"
            :props="{ expandTrigger: 'hover' }"
            clearable
            placeholder="选择类型、模块和子模块"
            @change="selectCategory"
          />
        </el-form-item>
        <el-form-item label="题库名称" required>
          <el-input v-model="createForm.bankName" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="标签" required>
          <el-select
            v-model="createForm.tags"
            multiple
            filterable
            allow-create
            default-first-option
            :multiple-limit="10"
            placeholder="输入标签后按回车"
          />
        </el-form-item>
        <el-form-item label="题库权重">
          <el-input-number v-model="createForm.sortOrder" :min="0" :max="9999" controls-position="right" />
          <div class="form-tip">默认10；只有需要优先曝光的题库才设置更高数值。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="creating"
          :disabled="!createForm.subModuleId || !createForm.bankName.trim() || !createForm.tags.length"
          @click="submitCreate"
        >
          创建并进入
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.filter-panel {
  padding: 16px 20px;
}

.filter-bar {
  gap: 8px;
}

.filter-bar .filter-keyword {
  width: 220px;
}

.filter-bar .filter-type,
.filter-bar .filter-module {
  width: 120px;
}

.filter-bar .filter-submodule {
  width: 128px;
}

.filter-bar .filter-status {
  width: 108px;
}

.filter-bar .filter-sort {
  width: 148px;
}

.filter-bar :deep(.el-button + .el-button) {
  margin-left: 0;
}

.result-count {
  margin-left: auto;
  color: var(--text-secondary);
  font-size: 13px;
}

.bank-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.bank-card {
  position: relative;
  display: flex;
  min-height: 294px;
  flex-direction: column;
  padding: 20px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 13px;
  cursor: pointer;
  transition:
    transform 160ms ease,
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.bank-card:hover {
  border-color: #b9c6f7;
  box-shadow: var(--shadow);
  transform: translateY(-2px);
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.bank-icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  color: var(--brand);
  font-size: 21px;
  background: var(--brand-soft);
  border-radius: 10px;
}

.bank-type {
  margin-top: 22px;
  color: var(--brand);
  font-size: 12px;
  font-weight: 650;
}

h2 {
  overflow: hidden;
  margin: 7px 0 6px;
  font-size: 18px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bank-card > p {
  overflow: hidden;
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tags {
  display: flex;
  gap: 6px;
  height: 24px;
  margin-top: 14px;
}

.metrics {
  display: grid;
  margin-top: auto;
  padding: 17px 0;
  border-top: 1px solid #edf0f4;
  grid-template-columns: repeat(3, 1fr);
}

.metrics div {
  display: flex;
  flex-direction: column;
}

.metrics strong {
  color: #182136;
  font-size: 18px;
}

.metrics span {
  margin-top: 3px;
  color: var(--text-secondary);
  font-size: 11px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #8a94a7;
  font-size: 11px;
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 10px 0;
}

.drawer-form {
  margin-top: 24px;
}

.drawer-form .el-cascader,
.drawer-form .el-select {
  width: 100%;
}
</style>
