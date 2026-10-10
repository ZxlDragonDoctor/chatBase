<template>
  <div class="anime-stat-card" :class="{ loading }" :aria-busy="loading">
    <div class="anime-stat-value" :class="colorClass">
      <template v-if="loading">
        <span class="anime-loader-spinner" aria-hidden="true"></span>
      </template>
      <template v-else>
        {{ formattedValue }}
      </template>
    </div>
    <div class="anime-stat-label">{{ label }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  value?: number | string
  label: string
  loading?: boolean
  color?: 'green' | 'blue' | 'purple' | 'yellow'
  format?: 'number' | 'tokens' | 'percent' | 'raw'
}>()

const colorClass = computed(() => {
  if (props.color) return props.color
  return ''
})

const formattedValue = computed(() => {
  if (props.value === undefined || props.value === null) return '—'
  const val = typeof props.value === 'string' ? parseFloat(props.value) : props.value
  
  switch (props.format) {
    case 'tokens':
      return formatTokens(val)
    case 'percent':
      return `${val.toFixed(1)}%`
    case 'raw':
      return props.value
    default:
      return formatNumber(val)
  }
})

function formatNumber(n: number): string {
  if (n >= 1000000) return `${(n / 1000000).toFixed(1)}M`
  if (n >= 1000) return `${(n / 1000).toFixed(1)}K`
  return n.toString()
}

function formatTokens(n: number): string {
  if (n >= 1000000) return `${(n / 1000000).toFixed(1)}M`
  if (n >= 1000) return `${(n / 1000).toFixed(1)}K`
  return n.toString()
}
</script>

<style scoped>
.anime-stat-card.loading { opacity: 0.78; }
.anime-stat-label { color: var(--ui-text-secondary); }

.anime-stat-value {
  background: none;
  color: var(--ui-primary);
  -webkit-text-fill-color: currentColor;
}

.anime-stat-value.blue { background: none; color: var(--ui-primary); }
.anime-stat-value.green { background: none; color: var(--ui-success); }
.anime-stat-value.purple { background: none; color: var(--ui-text-secondary); }
.anime-stat-value.yellow { background: none; color: var(--ui-accent); }
</style>
