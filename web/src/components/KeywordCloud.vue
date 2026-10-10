<template>
  <div class="anime-keyword-cloud" role="list" aria-label="关键词">
    <span v-for="kw in keywords" :key="kw.keyword" role="listitem" class="anime-keyword" :class="getSizeClass(kw.rank)" :style="{ opacity: 1 - (kw.rank - 1) * 0.03 }">
      {{ kw.keyword }}
    </span>
    <div v-if="keywords.length === 0" class="anime-empty" role="listitem">
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

<style scoped>
.anime-keyword-cloud {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px;
  background: var(--ui-surface-muted);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
}

.anime-keyword {
  display: inline-flex;
  padding: 6px 14px;
  border: 1px solid var(--ui-border);
  border-radius: 999px;
  background: var(--ui-surface);
  color: var(--ui-text-secondary);
  font-size: 13px;
  cursor: default;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.anime-keyword:hover {
  transform: translateY(-1px);
  border-color: var(--ui-primary);
  background: var(--ui-primary-soft);
  color: var(--ui-primary-hover);
}

.anime-keyword.large { font-size: 17px; }
.anime-keyword.medium { font-size: 14px; }
.anime-keyword.small { font-size: 12px; }
.anime-empty-text { color: var(--ui-text-secondary); }

@media (prefers-reduced-motion: reduce) {
  .anime-keyword { transition: none; }
  .anime-keyword:hover { transform: none; }
}
</style>
