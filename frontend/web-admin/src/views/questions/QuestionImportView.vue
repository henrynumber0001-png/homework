<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Back, CircleCheck, Download, Document, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, type UploadFile } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  commitQuestionImport,
  createQuestionImportTask,
  downloadQuestionImportTemplate,
  getQuestionBank,
  getQuestionImportTask,
  listQuestionImportErrors,
} from '@/api/admin'
import { showApiError } from '@/api/http'
import type {
  QuestionBank,
  QuestionImportError,
  QuestionImportTask,
} from '@/types/admin'
import { formatDateTime, saveBlob } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const bankId = Number(route.params.bankId)
const bank = ref<QuestionBank | null>(null)
const selectedFile = ref<File>()
const task = ref<QuestionImportTask | null>(null)
const errors = ref<QuestionImportError[]>([])
const loading = ref(false)
const downloading = ref(false)
const validating = ref(false)
const committing = ref(false)

const step = computed(() => {
  if (task.value?.status === 'COMPLETED') return 3
  if (task.value) return 2
  return 1
})

const canCommit = computed(() => task.value?.status === 'READY' && task.value.errorRows === 0)

onMounted(loadBank)

async function loadBank(): Promise<void> {
  loading.value = true
  try {
    bank.value = await getQuestionBank(bankId)
  } catch (error) {
    showApiError(error)
  } finally {
    loading.value = false
  }
}

async function downloadTemplate(): Promise<void> {
  downloading.value = true
  try {
    const blob = await downloadQuestionImportTemplate(bankId)
    saveBlob(blob, `${bank.value?.bankName || '题库'}-题目导入模板.xlsx`)
  } catch (error) {
    showApiError(error)
  } finally {
    downloading.value = false
  }
}

function chooseFile(uploadFile: UploadFile): void {
  const file = uploadFile.raw
  if (!file) return
  if (!file.name.toLowerCase().endsWith('.xlsx')) {
    ElMessage.warning('请选择 .xlsx 文件')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('Excel 文件不能超过 10MB')
    return
  }
  selectedFile.value = file
  task.value = null
  errors.value = []
}

function clearFile(): void {
  selectedFile.value = undefined
  task.value = null
  errors.value = []
}

async function validateFile(): Promise<void> {
  if (!selectedFile.value) return
  validating.value = true
  try {
    task.value = await createQuestionImportTask(bankId, selectedFile.value)
    await loadErrorsIfNeeded()
    if (task.value.status === 'READY') {
      ElMessage.success('预检通过，可以确认导入')
    }
  } catch (error) {
    showApiError(error)
  } finally {
    validating.value = false
  }
}

async function refreshTask(): Promise<void> {
  if (!task.value) return
  try {
    task.value = await getQuestionImportTask(task.value.taskId)
    await loadErrorsIfNeeded()
  } catch (error) {
    showApiError(error)
  }
}

async function loadErrorsIfNeeded(): Promise<void> {
  if (!task.value || task.value.errorRows <= 0) {
    errors.value = []
    return
  }
  errors.value = await listQuestionImportErrors(task.value.taskId)
}

async function commit(): Promise<void> {
  if (!task.value || !canCommit.value) return
  committing.value = true
  try {
    task.value = await commitQuestionImport(task.value.taskId, task.value.totalRows)
    ElMessage.success(`已导入 ${task.value.importedRows} 道未发布题目`)
  } catch (error) {
    showApiError(error)
  } finally {
    committing.value = false
  }
}
</script>

<template>
  <div v-loading="loading" class="page">
    <PageHeader
      title="Excel 导入题目"
      :description="bank ? `导入到：${bank.bankName}。导入成功的题目全部保持未发布状态。` : ''"
    >
      <el-button :icon="Back" @click="router.push(`/question-banks/${bankId}`)">返回工作台</el-button>
    </PageHeader>

    <section class="panel steps-panel">
      <el-steps :active="step" align-center finish-status="success">
        <el-step title="准备文件" description="下载模板并填写" />
        <el-step title="上传预检" description="检查每一行数据" />
        <el-step title="确认导入" description="写入为未发布题目" />
      </el-steps>
    </section>

    <div class="import-layout">
      <section class="panel import-main">
        <div class="block-heading">
          <div class="heading-icon"><Download /></div>
          <div>
            <h2>1. 下载并填写模板</h2>
            <p>模板已根据当前题库类型生成，请勿修改列名或工作表名称。</p>
          </div>
          <el-button :icon="Download" :loading="downloading" @click="downloadTemplate">
            下载 Excel 模板
          </el-button>
        </div>

        <el-divider />

        <div class="block-heading upload-heading">
          <div class="heading-icon"><UploadFilled /></div>
          <div>
            <h2>2. 上传文件进行预检</h2>
            <p>支持 .xlsx，单次最多 1,000 道题，文件最大 10MB。</p>
          </div>
        </div>

        <el-upload
          v-if="!selectedFile"
          drag
          action="#"
          :auto-upload="false"
          :show-file-list="false"
          accept=".xlsx"
          :on-change="chooseFile"
        >
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-title">把 Excel 文件拖到这里</div>
          <div class="upload-copy">或点击选择文件</div>
        </el-upload>

        <div v-else class="selected-file">
          <div class="file-icon"><Document /></div>
          <div>
            <strong>{{ selectedFile.name }}</strong>
            <span>{{ (selectedFile.size / 1024).toFixed(1) }} KB</span>
          </div>
          <el-button v-if="!validating && task?.status !== 'COMPLETED'" link @click="clearFile">重新选择</el-button>
        </div>

        <div v-if="selectedFile && !task" class="primary-action">
          <el-button type="primary" size="large" :loading="validating" @click="validateFile">
            上传并开始预检
          </el-button>
          <span>预检不会写入任何题目。</span>
        </div>

        <template v-if="task">
          <el-divider />
          <div class="result-header">
            <div>
              <h2>预检结果</h2>
              <p>任务 {{ task.taskId }} · 有效期至 {{ formatDateTime(task.expiresTime) }}</p>
            </div>
            <StatusTag :value="task.status" />
          </div>

          <div class="result-metrics">
            <div><strong>{{ task.totalRows }}</strong><span>数据行</span></div>
            <div class="success"><strong>{{ task.validRows }}</strong><span>有效</span></div>
            <div :class="{ danger: task.errorRows > 0 }"><strong>{{ task.errorRows }}</strong><span>错误</span></div>
            <div><strong>{{ task.importedRows }}</strong><span>已导入</span></div>
          </div>

          <el-alert
            v-if="task.failureReason"
            :title="task.failureReason"
            type="error"
            :closable="false"
            show-icon
          />

          <div v-if="errors.length" class="error-section">
            <div class="error-heading">
              <strong>请修正以下错误后重新上传</strong>
              <span>已列出全部 {{ errors.length }} 项错误</span>
            </div>
            <el-table :data="errors" max-height="340">
              <el-table-column prop="rowNumber" label="Excel 行" width="100" />
              <el-table-column prop="fieldName" label="字段" width="150" />
              <el-table-column prop="errorMessage" label="问题说明" min-width="300" />
            </el-table>
          </div>

          <div v-if="canCommit" class="commit-box">
            <el-icon><CircleCheck /></el-icon>
            <div>
              <strong>文件已通过预检</strong>
              <span>确认后将一次性导入 {{ task.totalRows }} 道题，全部为未发布状态。</span>
            </div>
            <el-button type="primary" size="large" :loading="committing" @click="commit">
              确认导入
            </el-button>
          </div>

          <div v-if="['VALIDATING', 'IMPORTING'].includes(task.status)" class="primary-action">
            <el-button type="primary" plain @click="refreshTask">刷新任务状态</el-button>
          </div>

          <el-result
            v-if="task.status === 'COMPLETED'"
            icon="success"
            title="题目导入完成"
            :sub-title="`${task.importedRows} 道题已进入题库，当前全部未发布`"
          >
            <template #extra>
              <el-button type="primary" @click="router.push(`/question-banks/${bankId}?released=false`)">
                返回工作台查看题目
              </el-button>
              <el-button @click="clearFile">继续导入</el-button>
            </template>
          </el-result>
        </template>
      </section>

      <aside class="panel guide">
        <h3>填写提示</h3>
        <ol>
          <li>一行代表一道题，空白行会被忽略。</li>
          <li>面试题库只接受简答题。</li>
          <li>认证题库只接受单选题和多选题。</li>
          <li>选择题选项从 A 开始连续填写。</li>
          <li>多选题正确答案至少包含两个选项。</li>
        </ol>
        <el-alert
          title="预检通过后仍需手动点击“确认导入”，避免误上传直接写入题库。"
          type="info"
          :closable="false"
        />
      </aside>
    </div>
  </div>
</template>

<style scoped>
.steps-panel {
  padding: 23px 40px;
}

.import-layout {
  display: grid;
  gap: 18px;
  align-items: start;
  grid-template-columns: minmax(0, 1fr) 310px;
}

.import-main {
  padding: 26px;
}

.block-heading {
  display: grid;
  gap: 14px;
  align-items: center;
  grid-template-columns: 42px 1fr auto;
}

.heading-icon {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  color: var(--brand);
  font-size: 21px;
  background: var(--brand-soft);
  border-radius: 10px;
}

h2,
h3,
p {
  margin: 0;
}

h2 {
  font-size: 17px;
}

.block-heading p,
.result-header p {
  margin-top: 5px;
  color: var(--text-secondary);
  font-size: 12px;
}

.upload-heading {
  margin-bottom: 18px;
  grid-template-columns: 42px 1fr;
}

.import-main :deep(.el-upload),
.import-main :deep(.el-upload-dragger) {
  width: 100%;
}

.import-main :deep(.el-upload-dragger) {
  padding: 42px;
  background: #fafbfe;
}

.upload-icon {
  color: var(--brand);
  font-size: 36px;
}

.upload-title {
  margin-top: 10px;
  color: #26334e;
  font-weight: 600;
}

.upload-copy {
  margin-top: 5px;
  color: var(--text-secondary);
  font-size: 12px;
}

.selected-file {
  display: grid;
  gap: 13px;
  align-items: center;
  padding: 16px;
  background: #f7f9fd;
  border: 1px solid #dfe4ee;
  border-radius: 10px;
  grid-template-columns: 42px 1fr auto;
}

.file-icon {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  color: #16855b;
  font-size: 22px;
  background: #e9f8f2;
  border-radius: 9px;
}

.selected-file strong,
.selected-file span {
  display: block;
}

.selected-file span {
  margin-top: 5px;
  color: var(--text-secondary);
  font-size: 12px;
}

.primary-action {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 18px;
}

.primary-action span {
  color: var(--text-secondary);
  font-size: 12px;
}

.result-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.result-metrics {
  display: grid;
  margin: 18px 0;
  padding: 16px;
  background: var(--surface-soft);
  border-radius: 9px;
  grid-template-columns: repeat(4, 1fr);
}

.result-metrics div {
  display: flex;
  flex-direction: column;
  padding-left: 16px;
  border-left: 1px solid var(--line);
}

.result-metrics div:first-child {
  padding-left: 0;
  border-left: 0;
}

.result-metrics strong {
  font-size: 21px;
}

.result-metrics span {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 11px;
}

.result-metrics .success strong {
  color: var(--success);
}

.result-metrics .danger strong {
  color: var(--danger);
}

.error-section {
  margin-top: 18px;
}

.error-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.error-heading span {
  color: var(--text-secondary);
  font-size: 12px;
}

.commit-box {
  display: grid;
  gap: 13px;
  align-items: center;
  margin-top: 20px;
  padding: 18px;
  color: #126746;
  background: #eefaf5;
  border: 1px solid #bfe9d8;
  border-radius: 10px;
  grid-template-columns: 34px 1fr auto;
}

.commit-box > .el-icon {
  font-size: 27px;
}

.commit-box strong,
.commit-box span {
  display: block;
}

.commit-box span {
  margin-top: 5px;
  color: #52836f;
  font-size: 12px;
}

.guide {
  position: sticky;
  top: 92px;
}

.guide h3 {
  margin-bottom: 12px;
}

.guide ol {
  margin: 0 0 20px;
  padding-left: 20px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.85;
}
</style>
