<template>
  <div class="cyber-loader" role="status" aria-live="polite" :aria-label="text ? undefined : '加载中'">
    <div class="cyber-loader-spinner" aria-hidden="true"></div>
    <span class="cyber-loader-text" v-if="text">{{ text }}</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  text?: string
}>()
</script>

<style scoped>
.cyber-loader {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.cyber-loader-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--ui-border);
  border-top-color: var(--ui-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.cyber-loader-text {
  color: var(--ui-text-secondary);
  font-size: 13px;
}

@media (prefers-reduced-motion: reduce) {
  .cyber-loader-spinner { animation: none; }
}
</style>
