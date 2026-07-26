<template>
  <el-result icon="warning" title="无权访问" sub-title="当前管理员没有访问此页面所需的权限">
    <template #extra>
      <el-button type="primary" @click="$router.push('/question-banks')">返回题库</el-button>
    </template>
  </el-result>
</template>
