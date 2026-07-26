<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Collection, UploadFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { showApiError } from '@/api/http'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const submitting = ref(false)
const form = reactive({ email: '', password: '' })

async function submit(): Promise<void> {
  if (!form.email || !form.password) return
  submitting.value = true
  try {
    await auth.login(form.email.trim(), form.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/question-banks'
    await router.replace(redirect)
  } catch (error) {
    showApiError(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-story">
      <div class="story-inner">
        <div class="brand"><span>H</span> Homework Admin</div>
        <h1>让题库维护<br />回到内容本身。</h1>
        <p>清晰的题库工作台、直观的题目编辑，以及可预检的 Excel 批量导入。</p>
        <div class="feature-grid">
          <div>
            <el-icon><Collection /></el-icon>
            <strong>先选题库</strong>
            <span>上下文清晰，操作不迷路</span>
          </div>
          <div>
            <el-icon><UploadFilled /></el-icon>
            <strong>导入可预检</strong>
            <span>错误定位到 Excel 行与字段</span>
          </div>
        </div>
      </div>
    </section>
    <section class="login-panel">
      <div class="login-box">
        <div class="mobile-brand">Homework 管理后台</div>
        <h2>欢迎回来</h2>
        <p>使用管理员账号登录后台</p>
        <el-form label-position="top" @submit.prevent="submit">
          <el-form-item label="邮箱">
            <el-input
              v-model="form.email"
              size="large"
              autocomplete="username"
              placeholder="admin@example.com"
            />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              type="password"
              size="large"
              show-password
              autocomplete="current-password"
              placeholder="请输入密码"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button
            class="submit-button"
            type="primary"
            size="large"
            :loading="submitting"
            :disabled="!form.email || !form.password"
            @click="submit"
          >
            登录
          </el-button>
        </el-form>
        <p class="security-tip">管理员会话与 App 用户会话完全独立。</p>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  grid-template-columns: 1.05fr 0.95fr;
  background: #fff;
}

.login-story {
  position: relative;
  display: grid;
  overflow: hidden;
  place-items: center;
  color: #fff;
  background:
    radial-gradient(circle at 75% 18%, rgb(89 120 233 / 35%), transparent 32%),
    radial-gradient(circle at 15% 80%, rgb(71 96 196 / 24%), transparent 30%), #17213a;
}

.login-story::after {
  position: absolute;
  right: -180px;
  bottom: -260px;
  width: 620px;
  height: 620px;
  content: "";
  border: 1px solid rgb(255 255 255 / 8%);
  border-radius: 50%;
  box-shadow:
    0 0 0 80px rgb(255 255 255 / 2%),
    0 0 0 160px rgb(255 255 255 / 2%);
}

.story-inner {
  z-index: 1;
  width: min(560px, 75%);
}

.brand {
  display: flex;
  gap: 12px;
  align-items: center;
  font-size: 15px;
  font-weight: 650;
}

.brand span {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  background: #4d72ee;
  border-radius: 10px;
}

h1 {
  margin: 80px 0 24px;
  font-size: 50px;
  line-height: 1.16;
  letter-spacing: -0.04em;
}

.story-inner > p {
  max-width: 480px;
  color: #aeb9cd;
  font-size: 17px;
  line-height: 1.8;
}

.feature-grid {
  display: grid;
  gap: 14px;
  margin-top: 64px;
  grid-template-columns: 1fr 1fr;
}

.feature-grid div {
  display: grid;
  grid-template-columns: 28px 1fr;
  padding: 18px;
  background: rgb(255 255 255 / 6%);
  border: 1px solid rgb(255 255 255 / 8%);
  border-radius: 12px;
}

.feature-grid .el-icon {
  margin-top: 2px;
  color: #86a0ff;
  font-size: 20px;
  grid-row: span 2;
}

.feature-grid strong {
  font-size: 14px;
}

.feature-grid span {
  margin-top: 5px;
  color: #8998b5;
  font-size: 12px;
}

.login-panel {
  display: grid;
  place-items: center;
}

.login-box {
  width: 390px;
}

.login-box h2 {
  margin: 0;
  color: #182136;
  font-size: 30px;
  letter-spacing: -0.02em;
}

.login-box > p {
  margin: 10px 0 34px;
  color: var(--text-secondary);
}

.mobile-brand {
  display: none;
}

.submit-button {
  width: 100%;
  margin-top: 8px;
}

.security-tip {
  margin-top: 26px !important;
  text-align: center;
  font-size: 12px;
}
</style>
