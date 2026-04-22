<template>
  <div class="cyber-glow-card" :class="[color, { 'clickable': clickable }]" @click="clickable && $emit('click')">
    <div class="cyber-glow-card-header" v-if="$slots.header || title">
      <slot name="header">
        <span class="cyber-glow-card-title">{{ title }}</span>
      </slot>
    </div>
    <div class="cyber-glow-card-body">
      <slot></slot>
    </div>
    <div class="cyber-glow-card-footer" v-if="$slots.footer">
      <slot name="footer"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
  color?: 'green' | 'blue' | 'pink'
  clickable?: boolean
}>()

defineEmits<{
  (e: 'click'): void
}>()
</script>

<style scoped>
.cyber-glow-card.clickable {
  cursor: pointer;
}

.cyber-glow-card-header {
  margin-bottom: 12px;
}

.cyber-glow-card-title {
  font-family: var(--cyber-font-mono);
  font-size: 14px;
  color: var(--cyber-text-primary);
  font-weight: 600;
}

.cyber-glow-card-body {
  color: var(--cyber-text-primary);
}

.cyber-glow-card-footer {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}
</style>