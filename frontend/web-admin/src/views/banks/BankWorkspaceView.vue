<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Back, Delete, Edit, Plus, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import ReasonDialog from '@/components/ReasonDialog.vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  actOnQuestion,
  actOnQuestionBank,
  getCategoryTree,
  getQuestionBank,
  listQuestions,
  updateQuestionBank,
  updateQuestionNo,
} from '@/api/admin'
import { showApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import {
  AdminSortMode,
  GroupType,
  QuestionAction,
  QuestionBankAction,
  QuestionBankStatus,
  QuestionType,
  type CategoryGroup,
  type AdminSortMode as AdminSortModeValue,
  type Question,
  type QuestionBank,
  type QuestionType as QuestionTypeValue,
} from '@/types/admin'
import { runBatchActions, type BatchActionFailure } from '@/utils/batchActions'
import { formatDateTime } from '@/utils/format'
import { bankStatusNames, groupTypeLabels, questionTypeLabels } from '@/utils/dictionaries'

type PendingOperation =
  | { kind: 'bank'; action: QuestionBankAction; title: string }
  | { kind: 'questions'; action: QuestionAction; title: string; rows: Question[] }

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const bankId = Number(route.params.bankId)
const bank = ref<QuestionBank | null>(null)
const categories = ref<CategoryGroup[]>([])
const questions = ref<Question[]>([])
const selectedQuestions = ref<Question[]>([])
const questionTable = ref<{
  clearSelection: () => void
  toggleRowSelection: (row: Question, selected: boolean) => void
}>()
const loadingBank = ref(false)
const loadingQuestions = ref(false)
const activeTab = ref('questions')
const total = ref(0)
const reasonDialog = ref<InstanceType<typeof ReasonDialog>>()
const pendingOperation = ref<PendingOperation | null>(null)
const batchDialogVisible = ref(false)
const batchCompleted = ref(0)
const batchTotal = ref(0)
const batchFailures = ref<Array<BatchActionFailure<Question>>>([])
const questionNoDialogVisible = ref(false)
const questionNoSaving = ref(false)
const questionNoForm = reactive({
  questionId: 0,
  title: '',
  currentQuestionNo: 0,
  targetQuestionNo: 1,
  reason: '',
})

const query = reactive({
  keyword: '',
  questionType: '' as QuestionTypeValue | '',
  released: (route.query.released === 'false' || route.query.released === 'true'
    ? route.query.released
    : '') as '' | 'true' | 'false',
  // 题目列表支持按更新时间或题目序号查看。
  sortMode: AdminSortMode.UPDATED_TIME_DESC as AdminSortModeValue,
  pageNum: 1,
  pageSize: 20,
})

const settings = reactive({
  subModuleId: undefined as number | undefined,
  bankName: '',
  tags: [] as string[],
  // 变更：原 priority 改为题库人工曝光权重。
  sortOrder: 10,
  reason: '',
})
const savingSettings = ref(false)

const allowedQuestionTypes = computed(() =>
  bank.value?.groupType === GroupType.INTERVIEW
    ? [{ label: '简答题', value: QuestionType.ESSAY }]
    : [
        { label: '单选题', value: QuestionType.SINGLE_CHOICE },
        { label: '多选题', value: QuestionType.MULTIPLE },
      ],
)

const categoryOptions = computed(() =>
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

onMounted(async () => {
  if (!Number.isFinite(bankId)) {
    await router.replace('/question-banks')
    return
  }
  await Promise.all([loadBank(), loadQuestions(), loadCategories()])
})

async function loadBank(): Promise<void> {
  loadingBank.value = true
  try {
    bank.value = await getQuestionBank(bankId)
    Object.assign(settings, {
      subModuleId: bank.value.subModule.id,
      bankName: bank.value.bankName,
      tags: [...(bank.value.tags || [])],
      sortOrder: bank.value.sortOrder,
      reason: '',
    })
  } catch (error) {
    showApiError(error)
  } finally {
    loadingBank.value = false
  }
}

async function loadCategories(): Promise<void> {
  try {
    categories.value = await getCategoryTree()
  } catch (error) {
    showApiError(error)
  }
}

async function loadQuestions(): Promise<void> {
  loadingQuestions.value = true
  try {
    const result = await listQuestions(bankId, {
      keyword: query.keyword || undefined,
      questionType: query.questionType || undefined,
      released: query.released === '' ? undefined : query.released === 'true',
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      // 变更：原 sortBy + sortDirection 合并为题目排序模式。
      sortMode: query.sortMode,
    })
    questions.value = result.records
    total.value = result.total
    selectedQuestions.value = []
  } catch (error) {
    showApiError(error)
  } finally {
    loadingQuestions.value = false
  }
}

function search(): void {
  query.pageNum = 1
  void loadQuestions()
}

function resetQuestionFilters(): void {
  Object.assign(query, {
    keyword: '',
    questionType: '',
    released: '',
    sortMode: AdminSortMode.UPDATED_TIME_DESC,
    pageNum: 1,
  })
  void loadQuestions()
}

function handleSelectionChange(rows: Question[]): void {
  selectedQuestions.value = rows
}

function openQuestionOperation(action: QuestionAction, rows: Question[], title: string): void {
  const allowedRows = rows.filter((row) => {
    if (action === QuestionAction.PUBLISH) return !row.released
    if (action === QuestionAction.OFFLINE) return row.released
    // 变更：一题只属于当前题库，删除不再受共享引用数量限制。
    if (action === QuestionAction.DELETE) return true
    return false
  })
  const excludedCount = rows.length - allowedRows.length
  if (!allowedRows.length) {
    ElMessage.warning('选中的题目当前状态不允许执行此操作')
    return
  }
  pendingOperation.value = { kind: 'questions', action, title, rows: allowedRows }
  reasonDialog.value?.open({
    title,
    description:
      allowedRows.length > 1 || excludedCount
        ? `将对 ${allowedRows.length} 道题逐条执行${excludedCount ? `，已自动排除 ${excludedCount} 道状态不匹配的题目` : ''}。失败项不会阻塞其他题目。`
        : undefined,
  })
}

function openBankOperation(action: QuestionBankAction, title: string): void {
  pendingOperation.value = { kind: 'bank', action, title }
  reasonDialog.value?.open({ title, description: `题库：${bank.value?.bankName}` })
}

async function confirmOperation(reason: string): Promise<void> {
  const operation = pendingOperation.value
  if (!operation || !bank.value) return

  if (operation.kind === 'bank') {
    try {
      await actOnQuestionBank(bankId, {
        action: operation.action,
        reason,
        version: bank.value.version,
      })
      reasonDialog.value?.close()
      ElMessage.success(`${operation.title}成功`)
      if (operation.action === QuestionBankAction.DELETE) {
        await router.replace('/question-banks')
        return
      }
      await loadBank()
    } catch (error) {
      reasonDialog.value?.close()
      showApiError(error)
    }
    return
  }

  reasonDialog.value?.close()
  batchCompleted.value = 0
  batchTotal.value = operation.rows.length
  batchFailures.value = []
  batchDialogVisible.value = true
  const result = await runBatchActions(
    operation.rows,
    (question) =>
      actOnQuestion(bankId, question.id, {
        action: operation.action,
        reason,
        version: question.version,
      }),
    (completed) => {
      batchCompleted.value = completed
    },
  )
  batchFailures.value = result.failures
  await Promise.all([loadQuestions(), loadBank()])
  await nextTick()
  const failedIds = new Set(result.failures.map((failure) => failure.item.id))
  questions.value.forEach((question) => {
    if (failedIds.has(question.id)) questionTable.value?.toggleRowSelection(question, true)
  })
  if (!result.failures.length) {
    ElMessage.success(`已完成 ${result.succeeded.length} 道题`)
  }
}

async function saveSettings(): Promise<void> {
  if (
    !bank.value ||
    !settings.subModuleId ||
    !settings.bankName.trim() ||
    !settings.tags.length ||
    !settings.reason.trim()
  ) return
  savingSettings.value = true
  try {
    bank.value = await updateQuestionBank(bankId, {
      subModuleId: settings.subModuleId,
      bankName: settings.bankName.trim(),
      tags: settings.tags,
      // 变更：保存题库设置时发送 sortOrder，不再发送 priority。
      sortOrder: settings.sortOrder,
      reason: settings.reason.trim(),
      version: bank.value.version,
    })
    settings.reason = ''
    ElMessage.success('题库设置已保存')
  } catch (error) {
    showApiError(error)
  } finally {
    savingSettings.value = false
  }
}

function selectSettingCategory(path: Array<string | number> | null): void {
  settings.subModuleId = path?.length ? Number(path.at(-1)) : undefined
}

function openQuestionNoDialog(question: Question): void {
  Object.assign(questionNoForm, {
    questionId: question.id,
    title: question.title,
    currentQuestionNo: question.questionNo,
    targetQuestionNo: question.questionNo,
    reason: '',
  })
  questionNoDialogVisible.value = true
}

async function saveQuestionNo(): Promise<void> {
  if (!bank.value) return
  const targetQuestionNo = questionNoForm.targetQuestionNo
  if (!Number.isInteger(targetQuestionNo)
    || targetQuestionNo < 1
    || targetQuestionNo > bank.value.questionCount) {
    ElMessage.warning(`题目序号必须在 1～${bank.value.questionCount} 之间`)
    return
  }
  if (!questionNoForm.reason.trim()) return
  if (targetQuestionNo === questionNoForm.currentQuestionNo) {
    questionNoDialogVisible.value = false
    return
  }

  questionNoSaving.value = true
  try {
    const result = await updateQuestionNo(bankId, questionNoForm.questionId, {
      questionNo: targetQuestionNo,
      bankQuestionOrderVersion: bank.value.version,
      reason: questionNoForm.reason.trim(),
    })
    bank.value.version = result.bankQuestionOrderVersion
    bank.value.updatedTime = result.updatedTime
    questionNoDialogVisible.value = false
    ElMessage.success(`题目已移动到第 ${result.questionNo} 题`)
    await loadQuestions()
  } catch (error) {
    showApiError(error)
  } finally {
    questionNoSaving.value = false
  }
}
</script>

<template>
  <div v-loading="loadingBank" class="page">
    <PageHeader
      :title="bank?.bankName || '题库工作台'"
      :description="bank ? `${groupTypeLabels[bank.groupType]} · ${bank.module.name} / ${bank.subModule.name}` : ''"
    >
      <el-button :icon="Back" @click="router.push('/question-banks')">返回题库</el-button>
      <el-dropdown v-if="bank" trigger="click">
        <el-button>题库操作</el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-if="bank.status !== QuestionBankStatus.PUBLISHED && auth.hasPermission('bank:publish')"
              @click="openBankOperation(QuestionBankAction.PUBLISH, '发布题库')"
            >
              发布题库
            </el-dropdown-item>
            <el-dropdown-item
              v-if="bank.status === QuestionBankStatus.PUBLISHED && auth.hasPermission('bank:publish')"
              @click="openBankOperation(QuestionBankAction.OFFLINE, '下架题库')"
            >
              下架题库
            </el-dropdown-item>
            <el-dropdown-item
              v-if="auth.hasPermission('bank:delete')"
              divided
              @click="openBankOperation(QuestionBankAction.DELETE, '删除题库')"
            >
              <span class="danger-text">删除题库</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-button
        v-if="auth.hasPermission('question:import')"
        :icon="UploadFilled"
        @click="router.push(`/question-banks/${bankId}/import`)"
      >
        Excel 导入
      </el-button>
      <el-button
        v-if="auth.hasPermission('question:create')"
        type="primary"
        :icon="Plus"
        @click="router.push(`/question-banks/${bankId}/questions/new`)"
      >
        创建题目
      </el-button>
    </PageHeader>

    <section v-if="bank" class="bank-summary">
      <div>
        <span>状态</span>
        <StatusTag :value="bankStatusNames[bank.status]" />
      </div>
      <div><span>题目总数</span><strong>{{ bank.questionCount }}</strong></div>
      <div><span>已发布</span><strong>{{ bank.releasedQuestionCount }}</strong></div>
      <div><span>浏览次数</span><strong>{{ bank.viewCount }}</strong></div>
      <div><span>完成人次</span><strong>{{ bank.completeCount }}</strong></div>
      <div><span>最后更新</span><strong class="time">{{ formatDateTime(bank.updatedTime) }}</strong></div>
    </section>

    <section class="panel workspace">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="题目管理" name="questions">
          <div class="question-toolbar">
            <div class="filter-bar">
              <el-input
                v-model="query.keyword"
                clearable
                placeholder="搜索题干或题目 ID"
                @keyup.enter="search"
                @clear="search"
              />
              <el-select v-model="query.questionType" clearable placeholder="题型">
                <el-option
                  v-for="type in allowedQuestionTypes"
                  :key="type.value"
                  :label="type.label"
                  :value="type.value"
                />
              </el-select>
              <el-select v-model="query.released" clearable placeholder="发布状态">
                <el-option label="未发布" value="false" />
                <el-option label="已发布" value="true" />
              </el-select>
              <!-- 更新时间与题目序号是两种明确的排序模式。 -->
              <el-select v-model="query.sortMode" placeholder="排序方式" @change="search">
                <el-option label="按更新时间降序" :value="AdminSortMode.UPDATED_TIME_DESC" />
                <el-option label="按题目序号升序" :value="AdminSortMode.QUESTION_NO_ASC" />
              </el-select>
              <el-button type="primary" plain @click="search">查询</el-button>
              <el-button @click="resetQuestionFilters">重置</el-button>
            </div>
          </div>

          <div v-if="selectedQuestions.length" class="batch-bar">
            <strong>已选择 {{ selectedQuestions.length }} 道题</strong>
            <el-button
              v-if="auth.hasPermission('question:publish')"
              type="primary"
              plain
              @click="openQuestionOperation(QuestionAction.PUBLISH, selectedQuestions, '批量发布题目')"
            >
              批量发布
            </el-button>
            <el-button
              v-if="auth.hasPermission('question:publish')"
              @click="openQuestionOperation(QuestionAction.OFFLINE, selectedQuestions, '批量下架题目')"
            >
              批量下架
            </el-button>
            <el-button
              v-if="auth.hasPermission('question:delete')"
              type="danger"
              plain
              @click="openQuestionOperation(QuestionAction.DELETE, selectedQuestions, '批量删除题目')"
            >
              批量删除
            </el-button>
            <el-button link @click="questionTable?.clearSelection()">取消选择</el-button>
          </div>

          <el-table
            ref="questionTable"
            v-loading="loadingQuestions"
            :data="questions"
            row-key="id"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="46" />
            <el-table-column label="题目序号" prop="questionNo" width="120" align="center">
              <template #default="{ row }">
                <div class="question-no-cell">
                  <strong>{{ row.questionNo }}</strong>
                  <el-button
                    v-if="auth.hasPermission('question:sort')"
                    link
                    type="primary"
                    @click="openQuestionNoDialog(row)"
                  >修改</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="题目" min-width="390">
              <template #default="{ row }">
                <div class="question-cell">
                  <el-image
                    v-if="row.imageUrl"
                    :src="row.imageUrl"
                    fit="cover"
                    class="question-thumb"
                    :preview-src-list="[row.imageUrl]"
                    preview-teleported
                    @click.stop
                  />
                  <div class="question-copy">
                    <button
                      class="question-title"
                      :disabled="!auth.hasPermission('question:update')"
                      @click="router.push(`/question-banks/${bankId}/questions/${row.id}/edit`)"
                    >
                      {{ row.title }}
                    </button>
                    <span>#{{ row.id }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="题型" width="110">
              <template #default="{ row }">{{ questionTypeLabels[row.questionType] }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <StatusTag :value="row.released ? 'PUBLISHED' : 'DRAFT'" />
              </template>
            </el-table-column>
            <el-table-column label="更新时间" width="156">
              <template #default="{ row }">{{ formatDateTime(row.updatedTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="auth.hasPermission('question:update')"
                  link
                  type="primary"
                  :icon="Edit"
                  @click="router.push(`/question-banks/${bankId}/questions/${row.id}/edit`)"
                >
                  编辑
                </el-button>
                <el-dropdown trigger="click">
                  <el-button link>更多</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item
                        v-if="!row.released && auth.hasPermission('question:publish')"
                        @click="openQuestionOperation(QuestionAction.PUBLISH, [row], '发布题目')"
                      >
                        发布
                      </el-dropdown-item>
                      <el-dropdown-item
                        v-if="row.released && auth.hasPermission('question:publish')"
                        @click="openQuestionOperation(QuestionAction.OFFLINE, [row], '下架题目')"
                      >
                        下架
                      </el-dropdown-item>
                      <el-dropdown-item
                        v-if="auth.hasPermission('question:delete')"
                        divided
                        @click="openQuestionOperation(QuestionAction.DELETE, [row], '删除题目')"
                      >
                        删除
                      </el-dropdown-item>
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
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              @current-change="loadQuestions"
              @size-change="search"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="题库设置" name="settings">
          <div class="settings-form">
            <el-form label-position="top" @submit.prevent="saveSettings">
              <el-form-item label="题库分类" required>
                <el-cascader
                  :model-value="[bank?.group.id, bank?.module.id, settings.subModuleId]"
                  :options="categoryOptions"
                  :props="{ expandTrigger: 'hover' }"
                  @change="selectSettingCategory"
                />
              </el-form-item>
              <el-form-item label="题库名称" required>
                <el-input v-model="settings.bankName" maxlength="100" show-word-limit />
              </el-form-item>
              <el-form-item label="标签" required>
                <el-select
                  v-model="settings.tags"
                  multiple
                  filterable
                  allow-create
                  default-first-option
                  :multiple-limit="10"
                />
              </el-form-item>
              <el-form-item label="题库权重">
                <el-input-number v-model="settings.sortOrder" :min="0" :max="9999" controls-position="right" />
                <div class="form-tip">默认10；需要优先曝光时才提高该值。</div>
              </el-form-item>
              <el-form-item label="修改原因" required>
                <el-input
                  v-model="settings.reason"
                  type="textarea"
                  :rows="3"
                  maxlength="500"
                  show-word-limit
                  placeholder="题库修改会记录到操作日志"
                />
              </el-form-item>
              <el-button
                v-if="auth.hasPermission('bank:update')"
                type="primary"
                :loading="savingSettings"
                :disabled="!settings.bankName.trim() || !settings.subModuleId || !settings.tags.length || !settings.reason.trim()"
                @click="saveSettings"
              >
                保存设置
              </el-button>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <ReasonDialog ref="reasonDialog" @confirm="confirmOperation" />

    <el-dialog v-model="questionNoDialogVisible" title="修改题目序号" width="480px">
      <p class="dialog-description">{{ questionNoForm.title }}</p>
      <el-form label-position="top" @submit.prevent="saveQuestionNo">
        <el-form-item label="目标序号" required>
          <el-input-number
            v-model="questionNoForm.targetQuestionNo"
            :min="1"
            :max="bank?.questionCount || 1"
            controls-position="right"
          />
          <div class="form-tip">
            当前为第 {{ questionNoForm.currentQuestionNo }} 题；中间题目将自动顺移。
          </div>
        </el-form-item>
        <el-form-item label="调整原因" required>
          <el-input
            v-model="questionNoForm.reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="本次修改会记录到操作日志"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="questionNoDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="questionNoSaving"
          :disabled="!questionNoForm.reason.trim()"
          @click="saveQuestionNo"
        >保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="batchDialogVisible"
      title="批量操作进度"
      width="560px"
      :close-on-click-modal="batchCompleted >= batchTotal"
      :show-close="batchCompleted >= batchTotal"
    >
      <el-progress
        :percentage="batchTotal ? Math.round((batchCompleted / batchTotal) * 100) : 0"
        :status="batchCompleted === batchTotal && !batchFailures.length ? 'success' : undefined"
      />
      <p class="progress-copy">已完成 {{ batchCompleted }} / {{ batchTotal }}</p>
      <el-alert
        v-if="batchCompleted === batchTotal && batchFailures.length"
        :title="`${batchFailures.length} 道题操作失败`"
        type="warning"
        :closable="false"
        show-icon
      />
      <div v-if="batchFailures.length" class="failure-list">
        <div v-for="failure in batchFailures" :key="failure.item.id">
          <strong>#{{ failure.item.id }}</strong>
          <span>{{ failure.message }}</span>
        </div>
      </div>
      <template #footer>
        <el-button
          type="primary"
          :disabled="batchCompleted < batchTotal"
          @click="batchDialogVisible = false"
        >
          完成
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<style scoped>
.bank-summary {
  display: grid;
  padding: 17px 22px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  grid-template-columns: 1.15fr repeat(5, 1fr);
}

.bank-summary > div {
  display: flex;
  min-height: 40px;
  flex-direction: column;
  justify-content: center;
  padding: 0 20px;
  border-right: 1px solid #edf0f4;
}

.bank-summary > div:first-child {
  padding-left: 0;
}

.bank-summary > div:last-child {
  border-right: 0;
}

.bank-summary span {
  margin-bottom: 5px;
  color: var(--text-secondary);
  font-size: 11px;
}

.bank-summary strong {
  font-size: 18px;
}

.bank-summary .time {
  font-size: 13px;
}

.workspace {
  min-height: 520px;
}

.question-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 5px 0 18px;
}

.batch-bar {
  display: flex;
  gap: 9px;
  align-items: center;
  margin-bottom: 12px;
  padding: 11px 14px;
  color: #2949b5;
  font-size: 13px;
  background: #f0f3ff;
  border: 1px solid #d9e0ff;
  border-radius: 9px;
}

.batch-bar strong {
  margin-right: 8px;
}

.question-cell {
  display: flex;
  gap: 11px;
  align-items: center;
  min-width: 0;
}

.question-thumb {
  width: 48px;
  height: 38px;
  flex: 0 0 auto;
  border-radius: 6px;
}

.question-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.question-title {
  overflow: hidden;
  padding: 0;
  color: #20315f;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.question-title:hover {
  color: var(--brand);
}

.question-title:disabled {
  color: var(--text-primary);
  cursor: default;
}

.question-copy span {
  color: var(--text-secondary);
  font-size: 11px;
}

.question-no-cell {
  display: flex;
  gap: 6px;
  align-items: center;
  justify-content: center;
}

.dialog-description {
  margin: 0 0 18px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.settings-form {
  max-width: 680px;
  padding: 18px 6px 28px;
}

.settings-form .el-cascader,
.settings-form .el-select {
  width: 100%;
}

.progress-copy {
  margin: 12px 0 18px;
  color: var(--text-secondary);
  text-align: center;
}

.failure-list {
  max-height: 230px;
  margin-top: 12px;
  overflow: auto;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.failure-list div {
  display: grid;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--line);
  grid-template-columns: 80px 1fr;
}

.failure-list div:last-child {
  border-bottom: 0;
}

.failure-list span {
  color: var(--danger);
  font-size: 13px;
}

</style>
