<template>
  <main class="not-found">
    <el-result icon="info" title="页面不存在" sub-title="请检查地址，或返回题库管理继续操作">
      <template #extra>
        <el-button type="primary" @click="$router.push('/question-banks')">返回题库</el-button>
      </template>
    </el-result>
  </main>
</template>

<style scoped>
.not-found {
  display: grid;
  min-height: 100vh;
  place-items: center;
}
</style>
