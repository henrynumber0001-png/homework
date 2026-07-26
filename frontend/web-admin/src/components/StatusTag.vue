<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ value: string | boolean }>()

const text = computed(() => {
  const labels: Record<string, string> = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    RELEASED: '已发布',
    OFFLINE: '已下架',
    HIDDEN: '已隐藏',
    DELETED: '已删除',
    ACTIVE: '正常',
    DISABLED: '已禁用',
    BANNED: '已封禁',
    PAID: '已支付',
    READY: '预检通过',
    COMPLETED: '已完成',
    VALIDATION_FAILED: '预检未通过',
    FAILED: '失败',
    VALIDATING: '预检中',
    IMPORTING: '导入中',
    true: '是',
    false: '否',
  }
  return labels[String(props.value)] || String(props.value)
})

const tagType = computed(() => {
  const value = String(props.value)
  if (['PUBLISHED', 'RELEASED', 'ACTIVE', 'PAID', 'READY', 'COMPLETED'].includes(value)) return 'success'
  if (['DRAFT', 'VALIDATING', 'IMPORTING'].includes(value)) return 'warning'
  if (['DELETED', 'DISABLED', 'BANNED', 'FAILED', 'VALIDATION_FAILED'].includes(value)) return 'danger'
  return 'info'
})
</script>

<template>
  <el-tag :type="tagType" effect="light" round>{{ text }}</el-tag>
</template>
