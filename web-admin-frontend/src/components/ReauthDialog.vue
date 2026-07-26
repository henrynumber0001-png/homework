<script setup lang="ts">
import { reactive, ref } from 'vue'
import { reauthenticate } from '@/api/admin'
import { showApiError } from '@/api/http'

const emit = defineEmits<{
  success: [reauthToken: string]
}>()

const visible = ref(false)
const submitting = ref(false)
const actionScope = ref('')
const form = reactive({ password: '' })

function open(scope: string): void {
  actionScope.value = scope
  form.password = ''
  visible.value = true
}

async function confirm(): Promise<void> {
  if (!form.password) return
  submitting.value = true
  try {
    const result = await reauthenticate(form.password, actionScope.value)
    visible.value = false
    emit('success', result.reauthToken)
  } catch (error) {
    showApiError(error)
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" title="验证管理员身份" width="440px">
    <p class="description">这是高风险操作，请输入当前管理员密码后继续。</p>
    <el-form label-position="top" @submit.prevent="confirm">
      <el-form-item label="当前密码" required>
        <el-input
          v-model="form.password"
          type="password"
          show-password
          autocomplete="current-password"
          @keyup.enter="confirm"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" :disabled="!form.password" @click="confirm">
        验证并继续
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.description {
  margin: -4px 0 20px;
  color: var(--text-secondary);
}
</style>
