<template>
  <div
    class="cyber-glow-card"
    :class="[color, { clickable }]"
    :role="clickable ? 'button' : undefined"
    :tabindex="clickable ? 0 : undefined"
    @click="clickable && $emit('click')"
    @keydown="handleKeydown"
  >
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
const props = defineProps<{
  title?: string
  color?: 'green' | 'blue' | 'pink'
  clickable?: boolean
}>()

const emit = defineEmits<{
  (e: 'click'): void
}>()

function handleKeydown(event: KeyboardEvent) {
  if (!props.clickable || event.target !== event.currentTarget) return
  if (event.repeat) return
  if (event.key !== 'Enter' && event.key !== ' ') return

  if (event.key === ' ') event.preventDefault()
  emit('click')
}
</script>

<style scoped>
.cyber-glow-card {
  --glow-card-accent: var(--ui-primary);
  padding: 20px;
  position: relative;
  background: var(--ui-surface);
  border: 1px solid var(--ui-border);
  border-top: 2px solid var(--glow-card-accent);
  border-radius: var(--ui-radius-lg);
  box-shadow: var(--ui-shadow-card);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.cyber-glow-card.green { --glow-card-accent: var(--ui-success); }
.cyber-glow-card.blue { --glow-card-accent: var(--ui-primary); }
.cyber-glow-card.pink { --glow-card-accent: var(--ui-accent); }

.cyber-glow-card.clickable {
  cursor: pointer;
}

.cyber-glow-card.clickable:hover {
  border-color: var(--glow-card-accent);
}

.cyber-glow-card:focus-visible {
  outline: 2px solid var(--ui-focus);
  outline-offset: 2px;
}

.cyber-glow-card-header {
  margin-bottom: 12px;
}

.cyber-glow-card-title {
  font-size: 14px;
  color: var(--ui-text);
  font-weight: 600;
}

.cyber-glow-card-body {
  color: var(--ui-text);
}

.cyber-glow-card-footer {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

@media (prefers-reduced-motion: reduce) {
  .cyber-glow-card { transition: none; }
}
</style>
