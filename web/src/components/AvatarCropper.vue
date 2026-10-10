<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="visible"
        ref="dialogRef"
        class="ac-overlay"
        role="dialog"
        aria-modal="true"
        aria-labelledby="avatar-cropper-title"
        tabindex="-1"
        @keydown="handleDialogKeydown"
        @click.self="$emit('cancel')"
      >
        <div class="ac-modal anime-card">
          <div class="ac-header">
            <h2 id="avatar-cropper-title" class="ac-title">裁切头像</h2>
            <button type="button" class="ac-close" aria-label="关闭头像裁切" @click="$emit('cancel')">✕</button>
          </div>
          
          <div class="ac-body">
            <div class="ac-area" ref="containerRef">
              <img ref="imageRef" :src="imageUrl" alt="待裁切头像" />
            </div>
            
            <div class="ac-preview-panel">
              <div class="ac-preview-label" id="avatar-crop-preview-label">预览</div>
              <div class="ac-preview-circle">
                <canvas ref="previewRef" width="150" height="150" role="img" aria-labelledby="avatar-crop-preview-label">头像裁切预览</canvas>
              </div>
            </div>
          </div>
          
          <div class="ac-toolbar">
            <button type="button" class="ac-tbtn" @click="rotate(-90)" title="左旋90°" aria-label="向左旋转 90 度">
              <span aria-hidden="true">↺</span>
              <span class="ac-tlabel">左旋</span>
            </button>
            <button type="button" class="ac-tbtn" @click="rotate(90)" title="右旋90°" aria-label="向右旋转 90 度">
              <span aria-hidden="true">↻</span>
              <span class="ac-tlabel">右旋</span>
            </button>
            <button type="button" class="ac-tbtn" @click="doZoom(0.1)" title="放大" aria-label="放大头像">
              <span aria-hidden="true">🔍+</span>
              <span class="ac-tlabel">放大</span>
            </button>
            <button type="button" class="ac-tbtn" @click="doZoom(-0.1)" title="缩小" aria-label="缩小头像">
              <span aria-hidden="true">🔍-</span>
              <span class="ac-tlabel">缩小</span>
            </button>
            <button type="button" class="ac-tbtn" @click="doReset" title="重置" aria-label="重置头像裁切">
              <span aria-hidden="true">⟳</span>
              <span class="ac-tlabel">重置</span>
            </button>
          </div>
          
          <div class="ac-footer">
            <button type="button" class="anime-btn ghost" :disabled="processing" @click="$emit('cancel')">取消</button>
            <button type="button" class="anime-btn primary" :disabled="processing" :aria-busy="processing" @click="doConfirm">
              <span v-if="processing" class="anime-loader-spinner" aria-hidden="true" style="width: 14px; height: 14px;"></span>
              <span v-else>确认裁切</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'

const props = defineProps<{
  imageUrl: string
  visible: boolean
}>()

const emit = defineEmits<{
  confirm: [blob: Blob]
  cancel: []
}>()

const containerRef = ref<HTMLDivElement>()
const imageRef = ref<HTMLImageElement>()
const previewRef = ref<HTMLCanvasElement>()
const dialogRef = ref<HTMLDivElement>()
const processing = ref(false)
let triggerElement: HTMLElement | null = null

let cropper: Cropper | null = null

function initCropper() {
  destroyCropper()

  if (!imageRef.value || !containerRef.value) {
    return
  }

  setTimeout(() => {
    if (!imageRef.value) return

    try {
      cropper = new Cropper(imageRef.value, {
        aspectRatio: 1,
        viewMode: 1,
        dragMode: 'move',
        autoCropArea: 0.8,
        responsive: true,
        guides: true,
        center: true,
        highlight: true,
        cropBoxMovable: true,
        cropBoxResizable: true,
        toggleDragModeOnDblclick: false,
        ready() {
          updatePreview()
        },
        crop() {
          updatePreview()
        }
      })
    } catch (e) {
      console.error('Cropper init error:', e)
    }
  }, 100)
}

function updatePreview() {
  if (!cropper || !previewRef.value) return

  try {
    const canvas = cropper.getCroppedCanvas({
      width: 200,
      height: 200,
      imageSmoothingEnabled: true,
      imageSmoothingQuality: 'high'
    })

    if (!canvas) return

    const ctx = previewRef.value.getContext('2d')
    if (!ctx) return

    ctx.clearRect(0, 0, 150, 150)
    ctx.save()
    ctx.beginPath()
    ctx.arc(75, 75, 75, 0, Math.PI * 2, true)
    ctx.closePath()
    ctx.clip()
    ctx.drawImage(canvas, 0, 0, 150, 150)
    ctx.restore()
  } catch (e) {
    console.error('Preview error:', e)
  }
}

function rotate(deg: number) {
  cropper?.rotate(deg)
}

function doZoom(ratio: number) {
  cropper?.zoom(ratio)
}

function doReset() {
  cropper?.reset()
}

function doConfirm() {
  if (!cropper || processing.value) return

  processing.value = true

  try {
    const canvas = cropper.getCroppedCanvas({
      width: 200,
      height: 200,
      imageSmoothingEnabled: true,
      imageSmoothingQuality: 'high'
    })

    if (!canvas) {
      processing.value = false
      return
    }

    canvas.toBlob((blob) => {
      if (blob) {
        emit('confirm', blob)
      }
      processing.value = false
    }, 'image/jpeg', 0.9)
  } catch (e) {
    console.error('Confirm error:', e)
    processing.value = false
  }
}

function destroyCropper() {
  if (cropper) {
    cropper.destroy()
    cropper = null
  }
}

function trapDialogFocus(event: KeyboardEvent) {
  if (event.key !== 'Tab' || !dialogRef.value) return

  const focusableElements = Array.from(dialogRef.value.querySelectorAll<HTMLElement>(
    'a[href], button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])'
  )).filter(element => element.getClientRects().length > 0)
  const first = focusableElements[0]
  const last = focusableElements[focusableElements.length - 1]

  if (!first || !last) {
    event.preventDefault()
    dialogRef.value.focus()
    return
  }

  const focusIsInside = dialogRef.value.contains(document.activeElement)
  if (event.shiftKey && (!focusIsInside || document.activeElement === first)) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && (!focusIsInside || document.activeElement === last)) {
    event.preventDefault()
    first.focus()
  }
}

function handleDialogKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && !processing.value) {
    event.preventDefault()
    emit('cancel')
    return
  }
  trapDialogFocus(event)
}

function focusDialog() {
  dialogRef.value?.querySelector<HTMLButtonElement>('button:not(:disabled)')?.focus()
}

onMounted(() => {
  if (props.visible) {
    triggerElement = document.activeElement instanceof HTMLElement && document.activeElement !== document.body
      ? document.activeElement
      : null
    initCropper()
    nextTick(focusDialog)
  }
})

watch(() => props.visible, (val) => {
  if (val) {
    initCropper()
    nextTick(focusDialog)
  } else {
    destroyCropper()
    if (triggerElement?.isConnected) triggerElement.focus()
  }
})

watch(() => props.imageUrl, () => {
  if (props.visible) {
    initCropper()
  }
})

onBeforeUnmount(() => {
  destroyCropper()
  if (triggerElement?.isConnected) triggerElement.focus()
})
</script>

<style scoped>
.ac-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16px;
  background: rgb(23 35 33 / 48%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.ac-modal {
  width: min(750px, calc(100vw - 32px));
  max-height: calc(100vh - 32px);
  padding: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  background: var(--ui-surface);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-lg);
  box-shadow: var(--ui-shadow-card);
}

.ac-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--ui-border);
  background: var(--ui-surface-muted);
}

.ac-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--ui-text);
}

.ac-close {
  background: transparent;
  border: none;
  min-width: 44px;
  min-height: 44px;
  font-size: 20px;
  color: var(--ui-text-muted);
  cursor: pointer;
  border-radius: var(--ui-radius-sm);
  background: transparent;
}

.ac-close:hover {
  color: var(--ui-primary-hover);
  background: var(--ui-primary-soft);
}

.ac-body {
  display: flex;
  gap: 20px;
  padding: 20px;
  flex: 1;
  min-height: 350px;
}

.ac-area {
  flex: 1;
  background: #000;
  border-radius: var(--ui-radius-md);
  border: 1px solid var(--ui-border);
  overflow: hidden;
  min-height: 350px;
}

.ac-area img {
  max-width: 100%;
  display: block;
}

.ac-preview-panel {
  width: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.ac-preview-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--ui-text-secondary);
}

.ac-preview-circle {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  border: 2px solid var(--ui-primary);
  overflow: hidden;
  box-shadow: var(--ui-shadow-card);
  background: var(--ui-surface);
}

.ac-preview-circle canvas {
  width: 100%;
  height: 100%;
}

.ac-toolbar {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 12px 20px;
  border-top: 1px solid var(--ui-border);
  background: var(--ui-surface-muted);
}

.ac-tbtn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 72px;
  min-height: 56px;
  padding: 8px 12px;
  background: var(--ui-surface);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 16px;
  color: var(--ui-text);
}

.ac-tbtn:hover {
  border-color: var(--ui-primary);
  background: var(--ui-primary-soft);
}

.ac-tbtn:active {
  transform: scale(0.95);
}

.ac-tlabel {
  font-size: 11px;
  color: var(--ui-text-secondary);
}

.ac-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 12px 20px;
  border-top: 1px solid var(--ui-border);
  background: var(--ui-surface);
}

.ac-footer .anime-btn { min-height: 44px; }

:deep(.cropper-view-box) {
  outline: 2px solid var(--ui-focus) !important;
}

:deep(.cropper-face) {
  background: none !important;
}

:deep(.cropper-line) {
  background: var(--ui-primary) !important;
  opacity: 0.75 !important;
}

:deep(.cropper-point) {
  background-color: var(--ui-primary) !important;
  opacity: 0.75 !important;
}

.ac-close:focus-visible,
.ac-tbtn:focus-visible {
  outline: 2px solid var(--ui-focus);
  outline-offset: 2px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 640px) {
  .ac-body {
    flex-direction: column;
    min-height: 0;
  }

  .ac-area { min-height: 300px; }

  .ac-preview-panel {
    width: 100%;
    flex-direction: row;
    justify-content: center;
  }

  .ac-preview-circle {
    width: 96px;
    height: 96px;
  }

  .ac-toolbar {
    flex-wrap: wrap;
    gap: 8px;
    padding: 12px;
  }

  .ac-tbtn {
    min-width: 56px;
    flex: 1 1 56px;
    padding: 8px;
  }

  .ac-footer { padding: 12px; }
  .ac-footer .anime-btn { flex: 1; }
}

@media (prefers-reduced-motion: reduce) {
  .ac-tbtn,
  .fade-enter-active,
  .fade-leave-active { transition: none; }
  .ac-tbtn:active { transform: none; }
}
</style>
