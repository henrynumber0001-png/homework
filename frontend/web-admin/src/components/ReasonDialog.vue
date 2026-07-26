<script setup lang="ts">
import { reactive, ref } from 'vue'

const emit = defineEmits<{
  confirm: [reason: string]
}>()

const visible = ref(false)
const submitting = ref(false)
const title = ref('确认操作')
const description = ref('')
const form = reactive({ reason: '' })

function open(options: { title: string; description?: string }): void {
  title.value = options.title
  description.value = options.description || ''
  form.reason = ''
  submitting.value = false
  visible.value = true
}

function confirm(): void {
  if (!form.reason.trim()) return
  submitting.value = true
  emit('confirm', form.reason.trim())
}

function close(): void {
  visible.value = false
  submitting.value = false
}

defineExpose({ open, close })
</script>

<template>
  <el-dialog v-model="visible" :title="title" width="480px" :close-on-click-modal="!submitting">
    <p v-if="description" class="description">{{ description }}</p>
    <el-form label-position="top" @submit.prevent="confirm">
      <el-form-item label="操作原因" required>
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="填写原因，便于后续审计和追溯"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="submitting" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" :disabled="!form.reason.trim()" @click="confirm">
        确认
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.description {
  margin: -4px 0 18px;
  color: var(--text-secondary);
  line-height: 1.6;
}
</style>
