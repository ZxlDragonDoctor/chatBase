<template>
  <div class="anime-page-shell">
    <section class="anime-card">
      <div class="anime-card-header">
        <div>
          <div class="anime-card-title">用户反馈</div>
          <div class="anime-card-desc">您的意见对我们很重要，帮助我们持续改进服务质量</div>
        </div>
      </div>

      <div class="anime-card-body">
        <form @submit.prevent="handleSubmit" class="feedback-form">
          <div class="form-section">
            <label class="form-label">满意度评分</label>
            <div class="rating-stars">
              <button
                v-for="n in 5"
                :key="n"
                type="button"
                class="star-btn"
                :class="{ active: rating >= n }"
                @click="rating = n"
              >
                <Star :size="24" :fill="rating >= n ? '#ffb7c5' : 'none'" />
              </button>
              <span class="rating-text">{{ ratingText }}</span>
            </div>
          </div>

          <div class="form-section">
            <label class="form-label">反馈类型</label>
            <div class="feedback-types">
              <button
                v-for="t in feedbackTypes"
                :key="t.value"
                type="button"
                class="type-btn"
                :class="{ active: feedbackType === t.value }"
                @click="feedbackType = t.value"
              >
                {{ t.label }}
              </button>
            </div>
          </div>

          <div class="form-section">
            <label class="form-label">详细描述</label>
            <textarea
              v-model="content"
              class="anime-input feedback-textarea"
              placeholder="请详细描述您的反馈内容，帮助我们更好地了解您的需求..."
              rows="6"
            ></textarea>
          </div>

          <div class="form-section">
            <label class="form-label">联系方式（可选）</label>
            <input
              v-model="contactInfo"
              type="text"
              class="anime-input"
              :class="{ 'input-error': contactError }"
              placeholder="邮箱或手机号，方便我们回复您"
              @blur="validateContact"
              @input="contactError = ''"
            />
            <div v-if="contactError" class="field-error">{{ contactError }}</div>
            <div class="field-hint">支持邮箱格式（如：user@example.com）或手机号（11位数字）</div>
          </div>

          <div v-if="error" class="anime-error">{{ error }}</div>

          <div class="form-actions">
            <button type="button" class="anime-btn ghost" @click="resetForm">
              <RefreshCw :size="18" />
              <span>重置</span>
            </button>
            <button type="submit" class="anime-btn primary" :disabled="submitting || !rating">
              <Send :size="18" />
              <span>{{ submitting ? '提交中...' : '提交反馈' }}</span>
            </button>
          </div>
        </form>

        <div v-if="submitted" class="success-message">
          <CheckCircle :size="48" style="color: var(--anime-green);" />
          <div class="success-title">感谢您的反馈！</div>
          <div class="success-desc">我们会认真对待每一条反馈，持续改进服务质量。</div>
          <button class="anime-btn ghost" @click="submitted = false; resetForm()">
            继续反馈
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Star, Send, RefreshCw, CheckCircle } from 'lucide-vue-next'
import { submitFeedbackForm } from '../api/feedbackForm'
import { getOrCreateUserId } from '../lib/user'

const userId = getOrCreateUserId()

const rating = ref(0)
const feedbackType = ref('other')
const content = ref('')
const contactInfo = ref('')
const contactError = ref('')
const submitting = ref(false)
const submitted = ref(false)
const error = ref<string | null>(null)

const feedbackTypes = [
  { value: 'accurate', label: '回答准确' },
  { value: 'inaccurate', label: '回答不准确' },
  { value: 'partial', label: '部分正确' },
  { value: 'off_topic', label: '偏离主题' },
  { value: 'slow', label: '响应太慢' },
  { value: 'other', label: '其他问题' },
]

const ratingText = computed(() => {
  const texts = ['', '很不满意', '不满意', '一般', '满意', '很满意']
  return texts[rating.value] || ''
})

const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
const PHONE_REGEX = /^1[3-9]\d{9}$/

function validateContact(): boolean {
  if (!contactInfo.value || contactInfo.value.trim() === '') {
    contactError.value = ''
    return true
  }
  
  const trimmed = contactInfo.value.trim()
  
  if (EMAIL_REGEX.test(trimmed)) {
    contactError.value = ''
    return true
  }
  
  if (PHONE_REGEX.test(trimmed)) {
    contactError.value = ''
    return true
  }
  
  contactError.value = '请输入有效的邮箱或手机号'
  return false
}

async function handleSubmit() {
  if (rating.value === 0) {
    error.value = '请选择满意度评分'
    return
  }

  if (!validateContact()) {
    error.value = '联系方式格式不正确'
    return
  }

  submitting.value = true
  error.value = null

  try {
    const result = await submitFeedbackForm({
      userId,
      rating: rating.value,
      feedbackType: feedbackType.value,
      content: content.value,
      contact: contactInfo.value.trim(),
    })

    if (result.success) {
      submitted.value = true
    } else {
      error.value = result.message
    }
  } catch (e: any) {
    error.value = e?.message || '提交失败，请稍后再试'
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  rating.value = 0
  feedbackType.value = 'other'
  content.value = ''
  contactInfo.value = ''
  contactError.value = ''
  error.value = null
}
</script>

<style scoped>
.feedback-form {
  max-width: 600px;
  margin: 0 auto;
}

.form-section {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--anime-text-primary);
  margin-bottom: 12px;
}

.rating-stars {
  display: flex;
  align-items: center;
  gap: 8px;
}

.star-btn {
  background: transparent;
  border: none;
  padding: 4px;
  cursor: pointer;
  transition: transform 0.2s;
}

.star-btn:hover {
  transform: scale(1.2);
}

.star-btn.active {
  color: var(--anime-pink);
}

.rating-text {
  margin-left: 16px;
  font-size: 14px;
  color: var(--anime-text-muted);
}

.feedback-types {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.type-btn {
  padding: 10px 20px;
  border: 2px solid var(--anime-border);
  border-radius: var(--anime-radius-lg);
  background: var(--anime-bg-card);
  color: var(--anime-text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
}

.type-btn:hover {
  border-color: var(--anime-pink);
}

.type-btn.active {
  border-color: var(--anime-pink);
  background: rgba(255, 183, 197, 0.15);
  color: var(--anime-pink);
}

.feedback-textarea {
  width: 100%;
  resize: vertical;
  min-height: 120px;
}

.input-error {
  border-color: var(--anime-pink) !important;
  box-shadow: 0 0 0 2px rgba(255, 183, 197, 0.2);
}

.field-error {
  font-size: 12px;
  color: var(--anime-pink);
  margin-top: 6px;
}

.field-hint {
  font-size: 12px;
  color: var(--anime-text-muted);
  margin-top: 6px;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.success-message {
  text-align: center;
  padding: 48px 24px;
}

.success-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--anime-green);
  margin-top: 16px;
}

.success-desc {
  font-size: 14px;
  color: var(--anime-text-muted);
  margin: 12px 0 24px;
}
</style>