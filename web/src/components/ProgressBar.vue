<template>
  <div class="cyber-progress">
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