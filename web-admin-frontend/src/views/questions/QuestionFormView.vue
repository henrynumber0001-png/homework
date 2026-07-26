<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Back, Delete, Picture, Plus, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, type UploadFile } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import {
  createQuestion,
  getQuestion,
  getQuestionBank,
  updateQuestion,
  uploadQuestionImage,
} from '@/api/admin'
import { showApiError } from '@/api/http'
import type { QuestionBankDetail, QuestionDetail, QuestionOption, QuestionType } from '@/types/admin'

const route = useRoute()
const router = useRouter()
const bankId = Number(route.params.bankId)
const questionId = route.params.questionId ? Number(route.params.questionId) : undefined
const editing = computed(() => questionId !== undefined)
const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const bank = ref<QuestionBankDetail | null>(null)
const original = ref<QuestionDetail | null>(null)
const imagePreview = ref('')
const uploadedImageId = ref<string>()
const removeImage = ref(false)

const form = reactive<{
  questionType: QuestionType
  title: string
  analysis: string
  options: QuestionOption[]
  correctAnswers: string[]
  reason: string
}>({
  questionType: 'ESSAY',
  title: '',
  analysis: '',
  options: [],
  correctAnswers: [],
  reason: '',
})

const isChoice = computed(() => form.questionType !== 'ESSAY')
const pageTitle = computed(() => (editing.value ? '编辑题目' : '创建题目'))

onMounted(load)

watch(
  () => form.questionType,
  (type) => {
    if (type === 'ESSAY') {
      form.options = []
      form.correctAnswers = []
      return
    }
    if (form.options.length < 2) {
      form.options = [
        { key: 'A', content: '' },
        { key: 'B', content: '' },
      ]
    }
    if (type === 'SINGLE_CHOICE' && form.correctAnswers.length > 1) {
      form.correctAnswers = form.correctAnswers.slice(0, 1)
    }
  },
)

async function load(): Promise<void> {
  loading.value = true
  try {
    bank.value = await getQuestionBank(bankId)
    if (!editing.value) {
      form.questionType = bank.value.groupType === 'INTERVIEW' ? 'ESSAY' : 'SINGLE_CHOICE'
      return
    }
    original.value = await getQuestion(bankId, questionId!)
    Object.assign(form, {
      questionType: original.value.questionType,
      title: original.value.title,
      analysis: original.value.analysis || '',
      options: original.value.options.map((option) => ({ ...option })),
      correctAnswers: [...original.value.correctAnswers],
      reason: '',
    })
    imagePreview.value = original.value.imageUrl || ''
  } catch (error) {
    showApiError(error)
  } finally {
    loading.value = false
  }
}

function addOption(): void {
  if (form.options.length >= 26) return
  form.options.push({
    key: String.fromCharCode(65 + form.options.length),
    content: '',
  })
}

function removeOption(index: number): void {
  if (form.options.length <= 2) return
  const removedKey = form.options[index].key
  const selectedKeys = new Set(form.correctAnswers)
  form.options.splice(index, 1)
  const nextAnswers: string[] = []
  form.options.forEach((option, optionIndex) => {
    const oldKey = option.key
    const nextKey = String.fromCharCode(65 + optionIndex)
    if (selectedKeys.has(oldKey) && oldKey !== removedKey) nextAnswers.push(nextKey)
    option.key = nextKey
  })
  form.correctAnswers = nextAnswers
}

function chooseSingleAnswer(value: string | number | boolean | undefined): void {
  form.correctAnswers = value ? [String(value)] : []
}

async function handleImage(file: UploadFile): Promise<void> {
  const raw = file.raw
  if (!raw) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(raw.type)) {
    ElMessage.warning('只支持 JPG、PNG 或 WebP 图片')
    return
  }
  if (raw.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 5MB')
    return
  }
  uploading.value = true
  try {
    const result = await uploadQuestionImage(raw)
    uploadedImageId.value = result.uploadId
    imagePreview.value = result.url
    removeImage.value = false
    ElMessage.success('图片已上传')
  } catch (error) {
    showApiError(error)
  } finally {
    uploading.value = false
  }
}

function clearImage(): void {
  imagePreview.value = ''
  uploadedImageId.value = undefined
  removeImage.value = Boolean(original.value?.imageUrl)
}

function validateForm(): string | null {
  if (!form.title.trim()) return '请输入题干'
  if (!isChoice.value) return null
  if (form.options.length < 2 || form.options.some((option) => !option.content.trim())) {
    return '请至少填写两个完整选项'
  }
  const uniqueContents = new Set(form.options.map((option) => option.content.trim()))
  if (uniqueContents.size !== form.options.length) return '选项内容不能重复'
  if (form.questionType === 'SINGLE_CHOICE' && form.correctAnswers.length !== 1) {
    return '单选题需要选择一个正确答案'
  }
  if (form.questionType === 'MULTIPLE' && form.correctAnswers.length < 2) {
    return '多选题需要选择至少两个正确答案'
  }
  return null
}

async function submit(): Promise<void> {
  const validationMessage = validateForm()
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }
  submitting.value = true
  const payload = {
    questionType: form.questionType,
    title: form.title.trim(),
    analysis: form.analysis.trim() || undefined,
    imageUploadId: uploadedImageId.value,
    removeImage: editing.value ? removeImage.value : undefined,
    options: isChoice.value
      ? form.options.map((option) => ({ key: option.key, content: option.content.trim() }))
      : [],
    correctAnswers: isChoice.value ? form.correctAnswers : [],
    reason: editing.value ? form.reason.trim() || undefined : undefined,
    version: original.value?.version,
  }

  try {
    if (editing.value) {
      await updateQuestion(bankId, questionId!, payload)
      ElMessage.success('题目已保存')
    } else {
      await createQuestion(bankId, payload)
      ElMessage.success('题目已创建，当前为未发布状态')
    }
    await router.push(`/question-banks/${bankId}`)
  } catch (error) {
    showApiError(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div v-loading="loading" class="page">
    <PageHeader
      :title="pageTitle"
      :description="bank ? `${bank.bankName} · 创建后默认未发布` : ''"
    >
      <el-button :icon="Back" @click="router.push(`/question-banks/${bankId}`)">返回工作台</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">
        {{ editing ? '保存题目' : '创建题目' }}
      </el-button>
    </PageHeader>

    <div class="editor-layout">
      <section class="panel content-panel">
        <div class="section-heading">
          <span>1</span>
          <div><h2>题目内容</h2><p>填写题干、题型和可选图片</p></div>
        </div>
        <el-form label-position="top" @submit.prevent="submit">
          <el-form-item label="题型" required>
            <el-radio-group v-model="form.questionType">
              <template v-if="bank?.groupType === 'INTERVIEW'">
                <el-radio-button value="ESSAY">简答题</el-radio-button>
              </template>
              <template v-else>
                <el-radio-button value="SINGLE_CHOICE">单选题</el-radio-button>
                <el-radio-button value="MULTIPLE">多选题</el-radio-button>
              </template>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="题干" required>
            <el-input
              v-model="form.title"
              type="textarea"
              :rows="6"
              maxlength="5000"
              show-word-limit
              placeholder="输入清晰、完整的题目描述"
            />
          </el-form-item>
          <el-form-item label="题目图片">
            <div class="image-field">
              <div v-if="imagePreview" class="image-preview">
                <el-image :src="imagePreview" fit="contain" :preview-src-list="[imagePreview]" preview-teleported />
                <el-button class="remove-image" circle type="danger" :icon="Delete" @click="clearImage" />
              </div>
              <el-upload
                v-else
                drag
                action="#"
                :auto-upload="false"
                :show-file-list="false"
                accept="image/jpeg,image/png,image/webp"
                :on-change="handleImage"
              >
                <el-icon class="upload-icon"><UploadFilled /></el-icon>
                <div>{{ uploading ? '正在上传…' : '拖入图片，或点击选择' }}</div>
                <small>JPG / PNG / WebP，最大 5MB</small>
              </el-upload>
              <el-upload
                v-if="imagePreview"
                action="#"
                :auto-upload="false"
                :show-file-list="false"
                accept="image/jpeg,image/png,image/webp"
                :on-change="handleImage"
              >
                <el-button :icon="Picture" :loading="uploading">替换图片</el-button>
              </el-upload>
            </div>
            <div v-if="editing" class="form-tip">
              不操作图片会保留原图；上传新图会替换；只有点击删除按钮才会删除原图。
            </div>
          </el-form-item>
        </el-form>
      </section>

      <section v-if="isChoice" class="panel content-panel">
        <div class="section-heading">
          <span>2</span>
          <div><h2>选项与答案</h2><p>选项键按 A、B、C 顺序自动生成</p></div>
        </div>
        <div class="option-list">
          <div v-for="(option, index) in form.options" :key="option.key" class="option-row">
            <el-radio
              v-if="form.questionType === 'SINGLE_CHOICE'"
              :model-value="form.correctAnswers[0]"
              :value="option.key"
              @change="chooseSingleAnswer"
            />
            <el-checkbox
              v-else
              v-model="form.correctAnswers"
              :value="option.key"
            />
            <span class="option-key">{{ option.key }}</span>
            <el-input v-model="option.content" maxlength="5000" :placeholder="`选项 ${option.key}`" />
            <el-button
              link
              type="danger"
              :icon="Delete"
              :disabled="form.options.length <= 2"
              @click="removeOption(index)"
            />
          </div>
        </div>
        <el-button :icon="Plus" :disabled="form.options.length >= 26" @click="addOption">添加选项</el-button>
        <div class="answer-hint">
          <strong>当前正确答案：</strong>
          <span v-if="form.correctAnswers.length">{{ form.correctAnswers.join('、') }}</span>
          <span v-else class="danger-text">尚未选择</span>
        </div>
      </section>

      <section class="panel content-panel">
        <div class="section-heading">
          <span>{{ isChoice ? 3 : 2 }}</span>
          <div><h2>答案解析</h2><p>简答题可在这里填写参考答案</p></div>
        </div>
        <el-input
          v-model="form.analysis"
          type="textarea"
          :rows="8"
          maxlength="20000"
          show-word-limit
          :placeholder="form.questionType === 'ESSAY' ? '填写参考答案、关键点或评分说明' : '解释正确答案及其他选项'"
        />
      </section>

      <section v-if="editing" class="panel content-panel">
        <div class="section-heading">
          <span>{{ isChoice ? 4 : 3 }}</span>
          <div><h2>修改说明</h2><p>已发布题目修改核心内容时必须填写原因</p></div>
        </div>
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="例如：修正标准答案或优化题干表达"
        />
      </section>

      <aside class="panel side-panel">
        <h3>保存说明</h3>
        <ul>
          <li>新建题目保存后默认为未发布。</li>
          <li>发布操作请回到题库工作台完成。</li>
          <li>题目编辑不会改变其在题库中的顺序。</li>
          <li v-if="original?.referencedBankCount && original.referencedBankCount > 1">
            此题被 {{ original.referencedBankCount }} 个题库引用，保存会修改共享题目内容。
          </li>
        </ul>
        <el-button type="primary" size="large" :loading="submitting" @click="submit">
          {{ editing ? '保存修改' : '创建未发布题目' }}
        </el-button>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.editor-layout {
  display: grid;
  gap: 18px;
  align-items: start;
  grid-template-columns: minmax(0, 1fr) 310px;
}

.content-panel {
  grid-column: 1;
}

.side-panel {
  position: sticky;
  top: 92px;
  grid-column: 2;
  grid-row: 1 / span 3;
}

.section-heading {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 24px;
}

.section-heading > span {
  display: grid;
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
  place-items: center;
  color: var(--brand);
  font-size: 13px;
  font-weight: 700;
  background: var(--brand-soft);
  border-radius: 8px;
}

h2,
h3,
p {
  margin: 0;
}

h2 {
  font-size: 17px;
}

.section-heading p {
  margin-top: 5px;
  color: var(--text-secondary);
  font-size: 12px;
}

.image-field {
  width: 100%;
}

.image-field :deep(.el-upload),
.image-field :deep(.el-upload-dragger) {
  width: 100%;
}

.image-field :deep(.el-upload-dragger) {
  padding: 26px;
}

.upload-icon {
  margin-bottom: 7px;
  color: var(--brand);
  font-size: 26px;
}

.image-field small {
  display: block;
  margin-top: 6px;
  color: var(--text-secondary);
}

.image-preview {
  position: relative;
  display: flex;
  width: 100%;
  height: 260px;
  margin-bottom: 12px;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #f5f7fa;
  border: 1px solid var(--line);
  border-radius: 10px;
}

.image-preview .el-image {
  width: 100%;
  height: 100%;
}

.remove-image {
  position: absolute;
  top: 12px;
  right: 12px;
}

.option-list {
  margin-bottom: 12px;
}

.option-row {
  display: grid;
  gap: 9px;
  align-items: center;
  margin-bottom: 10px;
  grid-template-columns: 22px 30px 1fr 28px;
}

.option-key {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  color: #35425d;
  font-size: 12px;
  font-weight: 650;
  background: #eef1f6;
  border-radius: 7px;
}

.answer-hint {
  margin-top: 18px;
  padding: 12px 14px;
  color: var(--text-secondary);
  font-size: 13px;
  background: var(--surface-soft);
  border-radius: 8px;
}

.side-panel h3 {
  margin-bottom: 12px;
}

.side-panel ul {
  margin: 0 0 22px;
  padding-left: 19px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.8;
}

.side-panel .el-button {
  width: 100%;
}
</style>
