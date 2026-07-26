<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { changePassword } from '@/api/admin'
import { showApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const submitting = ref(false)
const form = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })

async function submit(): Promise<void> {
  if (form.newPassword.length < 12 || form.newPassword !== form.confirmPassword) return
  submitting.value = true
  try {
    await changePassword(form)
    ElMessage.success('密码已修改')
    Object.assign(form, { currentPassword: '', newPassword: '', confirmPassword: '' })
  } catch (error) {
    showApiError(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <PageHeader title="账号设置" description="查看管理员身份并维护登录密码" />
    <div class="settings-grid">
      <section class="panel profile">
        <div class="avatar">{{ auth.admin?.displayName?.slice(0, 1) }}</div>
        <h3>{{ auth.admin?.displayName }}</h3>
        <p>{{ auth.admin?.email }}</p>
        <el-tag>{{ auth.admin?.role === 'SUPER_ADMIN' ? '超级管理员' : '管理员' }}</el-tag>
      </section>
      <section class="panel password">
        <h3>修改密码</h3>
        <el-form label-position="top" @submit.prevent="submit">
          <el-form-item label="当前密码">
            <el-input v-model="form.currentPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="form.newPassword" type="password" show-password />
            <div class="form-tip">密码长度为 12–72 位。</div>
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="form.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="!form.currentPassword || form.newPassword.length < 12 || form.newPassword !== form.confirmPassword"
            @click="submit"
          >
            保存新密码
          </el-button>
        </el-form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.settings-grid {
  display: grid;
  gap: 20px;
  align-items: start;
  grid-template-columns: 300px 1fr;
}

.profile {
  text-align: center;
}

.profile .avatar {
  display: grid;
  width: 72px;
  height: 72px;
  margin: 4px auto 18px;
  place-items: center;
  color: var(--brand);
  font-size: 28px;
  font-weight: 700;
  background: var(--brand-soft);
  border-radius: 50%;
}

h3 {
  margin: 0 0 20px;
}

.profile h3 {
  margin-bottom: 6px;
}

.profile p {
  margin: 0 0 16px;
  color: var(--text-secondary);
}

.password {
  max-width: 650px;
}
</style>
