<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { acceptInvitation, getInvitation } from '@/api/admin'
import { showApiError } from '@/api/http'
import type { InvitationPreview } from '@/types/admin'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const token = String(route.params.token || route.query.token || '')
const loading = ref(true)
const submitting = ref(false)
const preview = ref<InvitationPreview | null>(null)
const form = reactive({ password: '', confirmPassword: '' })

onMounted(loadPreview)

async function loadPreview(): Promise<void> {
  try {
    preview.value = await getInvitation(token)
  } catch (error) {
    showApiError(error)
  } finally {
    loading.value = false
  }
}

async function submit(): Promise<void> {
  if (form.password.length < 12 || form.password !== form.confirmPassword) return
  submitting.value = true
  try {
    await acceptInvitation(token, form)
    ElMessage.success('管理员账号已激活，请登录')
    await router.replace('/login')
  } catch (error) {
    showApiError(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="center-page">
    <div v-loading="loading" class="invitation-card">
      <template v-if="preview?.valid">
        <div class="brand-mark">H</div>
        <h1>加入 Homework 管理后台</h1>
        <p>{{ preview.displayName }}，邀请发送至 {{ preview.emailMasked }}</p>
        <el-alert
          type="info"
          :closable="false"
          :title="`邀请有效期至 ${formatDateTime(preview.expiresTime)}`"
        />
        <el-form label-position="top" class="form" @submit.prevent="submit">
          <el-form-item label="设置密码">
            <el-input v-model="form.password" type="password" show-password />
            <div class="form-tip">密码长度为 12–72 位。</div>
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="form.confirmPassword" type="password" show-password @keyup.enter="submit" />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            class="full"
            :loading="submitting"
            :disabled="form.password.length < 12 || form.password !== form.confirmPassword"
            @click="submit"
          >
            激活管理员账号
          </el-button>
        </el-form>
      </template>
      <el-result v-else-if="!loading" icon="error" title="邀请无效或已过期">
        <template #extra>
          <el-button type="primary" @click="router.push('/login')">返回登录</el-button>
        </template>
      </el-result>
    </div>
  </main>
</template>

<style scoped>
.center-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  background: #f5f7fb;
}

.invitation-card {
  width: 500px;
  min-height: 450px;
  padding: 42px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 16px;
  box-shadow: var(--shadow);
}

.brand-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  color: #fff;
  font-size: 20px;
  font-weight: 800;
  background: var(--brand);
  border-radius: 11px;
}

h1 {
  margin: 24px 0 8px;
  font-size: 26px;
}

p {
  margin: 0 0 24px;
  color: var(--text-secondary);
}

.form {
  margin-top: 26px;
}

.full {
  width: 100%;
}
</style>
