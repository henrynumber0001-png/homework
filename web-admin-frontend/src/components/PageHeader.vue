<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <header class="page-header">
    <div>
      <h1>{{ title }}</h1>
      <p v-if="description">{{ description }}</p>
    </div>
    <div class="page-actions">
      <slot />
    </div>
  </header>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  min-height: 52px;
}

h1 {
  margin: 0;
  color: var(--text-primary);
  font-size: 25px;
  font-weight: 680;
  letter-spacing: -0.02em;
}

p {
  margin: 7px 0 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.page-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>
