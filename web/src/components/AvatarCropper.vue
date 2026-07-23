<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="visible" class="ac-overlay" @click.self="$emit('cancel')">
        <div class="ac-modal anime-card">
          <div class="ac-header">
            <span class="ac-title">✿ 裁切头像 ✿</span>
            <button class="ac-close" @click="$emit('cancel')">✕</button>
          </div>
          
          <div class="ac-body">
            <div class="ac-area" ref="containerRef">
              <img ref="imageRef" :src="imageUrl" alt="avatar" />
            </div>
            
            <div class="ac-preview-panel">
              <div class="ac-preview-label">预览</div>
              <div class="ac-preview-circle">
                <canvas ref="previewRef" width="150" height="150"></canvas>
              </div>
            </div>
          </div>
          
          <div class="ac-toolbar">
            <button class="ac-tbtn" @click="rotate(-90)" title="左旋90°">
              <span>↺</span>
              <span class="ac-tlabel">左旋</span>
            </button>
            <button class="ac-tbtn" @click="rotate(90)" title="右旋90°">
              <span>↻</span>
              <span class="ac-tlabel">右旋</span>
            </button>
            <button class="ac-tbtn" @click="doZoom(0.1)" title="放大">
              <span>🔍+</span>
              <span class="ac-tlabel">放大</span>
            </button>
            <button class="ac-tbtn" @click="doZoom(-0.1)" title="缩小">
              <span>🔍-</span>
              <span class="ac-tlabel">缩小</span>
            </button>
            <button class="ac-tbtn" @click="doReset" title="重置">
              <span>⟳</span>
              <span class="ac-tlabel">重置</span>
            </button>
          </div>
          
          <div class="ac-footer">
            <button class="anime-btn ghost" :disabled="processing" @click="$emit('cancel')">取消</button>
            <button class="anime-btn primary" :disabled="processing" @click="doConfirm">
              <span v-if="processing" class="anime-loader-spinner" style="width: 14px; height: 14px;"></span>
              <span v-else>确认裁切</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
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
const processing = ref(false)

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

onMounted(() => {
  if (props.visible) {
    initCropper()
  }
})

watch(() => props.visible, (val) => {
  if (val) {
    initCropper()
  } else {
    destroyCropper()
  }
})

watch(() => props.imageUrl, () => {
  if (props.visible) {
    initCropper()
  }
})

onBeforeUnmount(() => {
  destroyCropper()
})
</script>

<style>
.ac-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.ac-modal {
  width: 750px;
  max-width: 95vw;
  max-height: 90vh;
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.ac-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 2px solid var(--anime-border);
  background: var(--anime-bg);
}

.ac-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--anime-pink);
}

.ac-close {
  background: transparent;
  border: none;
  font-size: 20px;
  color: var(--anime-text-muted);
  cursor: pointer;
  padding: 4px 8px;
}

.ac-body {
  display: flex;
  gap: 20px;
  padding: 20px;
  flex: 1;
  min-height: 400px;
}

.ac-area {
  flex: 1;
  background: #000;
  border-radius: var(--anime-radius-lg);
  border: 2px solid var(--anime-border);
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
  color: var(--anime-text-secondary);
}

.ac-preview-circle {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  border: 3px solid var(--anime-pink);
  overflow: hidden;
  box-shadow: var(--anime-shadow-card);
  background: var(--anime-bg);
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
  border-top: 2px solid var(--anime-border);
  background: var(--anime-bg);
}

.ac-tbtn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  background: var(--anime-bg-card);
  border: 2px solid var(--anime-border);
  border-radius: var(--anime-radius-lg);
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 16px;
  color: var(--anime-text-primary);
}

.ac-tbtn:hover {
  border-color: var(--anime-pink);
  background: var(--anime-bg-hover);
}

.ac-tbtn:active {
  transform: scale(0.95);
}

.ac-tlabel {
  font-size: 11px;
  color: var(--anime-text-muted);
}

.ac-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 12px 20px;
  border-top: 2px solid var(--anime-border);
  background: var(--anime-bg);
}

.cropper-view-box {
  outline: 2px solid var(--anime-pink) !important;
  outline-color: rgba(255, 107, 157, 0.75) !important;
}

.cropper-face {
  background: none !important;
}

.cropper-line {
  background: var(--anime-pink) !important;
  opacity: 0.75 !important;
}

.cropper-point {
  background-color: var(--anime-pink) !important;
  opacity: 0.75 !important;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
