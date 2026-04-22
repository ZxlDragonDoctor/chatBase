<template>
  <div class="cyber-loader">
    <div class="cyber-loader-spinner"></div>
    <span class="cyber-loader-text" v-if="text">{{ text }}</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  text?: string
}>()
</script>

<style scoped>
.cyber-loader-text {
  color: var(--cyber-text-muted);
  font-family: var(--cyber-font-mono);
  font-size: 13px;
}
</style>