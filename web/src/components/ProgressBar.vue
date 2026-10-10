<template>
  <div
    class="cyber-progress"
    role="progressbar"
    aria-label="进度"
    aria-valuemin="0"
    aria-valuemax="100"
    :aria-valuenow="percent"
  >
    <div class="cyber-progress-bar" :style="{ width: `${percent}%` }"></div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  value: number
  max?: number
}>()

const percent = computed(() => {
  const max = props.max || 100
  return Math.min(100, Math.max(0, (props.value / max) * 100))
})
</script>

<style scoped>
.cyber-progress {
  width: 100%;
  height: 6px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--ui-surface-muted);
}

.cyber-progress-bar {
  height: 100%;
  border-radius: inherit;
  background: var(--ui-primary);
  transition: width 0.3s ease;
}
</style>
