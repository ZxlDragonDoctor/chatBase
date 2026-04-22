<template>
  <div class="anime-keyword-cloud">
    <span v-for="kw in keywords" :key="kw.keyword" class="anime-keyword" :class="getSizeClass(kw.rank)" :style="{ opacity: 1 - (kw.rank - 1) * 0.03 }">
      {{ kw.keyword }}
    </span>
    <div v-if="keywords.length === 0" class="anime-empty">
      <span class="anime-empty-text">暂无关键词数据</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { KeywordItem } from '../types/statistics'

defineProps<{
  keywords: KeywordItem[]
}>()

function getSizeClass(rank: number): string {
  if (rank <= 3) return 'large'
  if (rank <= 10) return 'medium'
  return 'small'
}
</script>