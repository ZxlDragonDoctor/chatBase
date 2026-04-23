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
        <div class="tab-bar">
          <button class="tab-btn" :class="{ active: activeTab === 'submit' }" @click="activeTab = 'submit'">
            提交反馈
          </button>
          <button class="tab-btn" :class="{ active: activeTab === 'history' }" @click="activeTab = 'history'; loadMyFeedback()">
            我的反馈
            <span v-if="unreadCount > 0" class="unread-badge">{{ unreadCount }}</span>
          </button>
        </div>

        <div v-if="activeTab === 'submit'">
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

        <div v-if="activeTab === 'history'">
          <div v-if="loadingHistory" class="anime-loading">加载中...</div>
          
          <div v-else-if="myFeedbacks.length === 0" class="anime-empty">
            <span class="anime-empty-icon">📭</span>
            <div>暂无反馈记录</div>
            <button class="anime-btn ghost" @click="activeTab = 'submit'">提交反馈</button>
          </div>

          <div v-else class="feedback-history-list">
            <div v-for="item in myFeedbacks" :key="item.id" class="feedback-history-item" :class="{ has_reply: item.adminReply }">
              <div class="history-header">
                <span class="history-rating">
                  <Star v-for="n in 5" :key="n" :size="14" :fill="item.rating >= n ? '#ffb7c5' : 'none'" />
                </span>
                <span class="history-type">{{ getTypeLabel(item.feedbackType) }}</span>
                <span class="history-status" :class="{ replied: item.adminReply }">
                  {{ item.adminReply ? '已回复' : '待处理' }}
                </span>
                <span class="history-time">{{ formatTime(item.createTime) }}</span>
              </div>
              <div class="history-content">{{ item.feedbackContent || '无内容' }}</div>
              
              <div v-if="item.adminReply" class="admin-reply-section">
                <div class="reply-label">管理员回复：</div>
                <div class="reply-content">{{ item.adminReply }}</div>
                <div class="reply-time">回复时间：{{ formatTime(item.replyTime) }}</div>
              </div>
            </div>

            <div class="pagination" v-if="historyTotal > historyPageSize">
              <button class="anime-btn ghost sm" :disabled="historyPageNum <= 1" @click="historyPageNum--; loadMyFeedback()">上一页</button>
              <span>第 {{ historyPageNum }} 页</span>
              <button class="anime-btn ghost sm" :disabled="historyPageNum * historyPageSize >= historyTotal" @click="historyPageNum++; loadMyFeedback()">下一页</button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Star, Send, RefreshCw, CheckCircle } from 'lucide-vue-next'
import { submitFeedbackForm, getUserFeedbackPage, type FeedbackItem } from '../api/feedbackForm'
import { getOrCreateUserId } from '../lib/user'

const userId = getOrCreateUserId()

const activeTab = ref<'submit' | 'history'>('submit')
const rating = ref(0)
const feedbackType = ref('other')
const content = ref('')
const contactInfo = ref('')
const contactError = ref('')
const submitting = ref(false)
const submitted = ref(false)
const error = ref<string | null>(null)

const myFeedbacks = ref<FeedbackItem[]>([])
const loadingHistory = ref(false)
const historyPageNum = ref(1)
const historyPageSize = ref(10)
const historyTotal = ref(0)

const feedbackTypes = [
  { value: 'accurate', label: '回答准确' },
  { value: 'inaccurate', label: '回答不准确' },
  { value: 'partial', label: '部分正确' },
  { value: 'off_topic', label: '偏离主题' },
  { value: 'slow', label: '响应太慢' },
  { value: 'other', label: '其他问题' },
]

const typeLabels: Record<string, string> = {
  accurate: '回答准确',
  inaccurate: '回答不准确',
  partial: '部分正确',
  off_topic: '偏离主题',
  slow: '响应太慢',
  other: '其他问题'
}

function getTypeLabel(type: string | null): string {
  return type ? typeLabels[type] || type : '未分类'
}

const ratingText = computed(() => {
  const texts = ['', '很不满意', '不满意', '一般', '满意', '很满意']
  return texts[rating.value] || ''
})

const unreadCount = computed(() => {
  return myFeedbacks.value.filter(f => f.adminReply).length
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

function formatTime(time: string | null): string {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
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

async function loadMyFeedback() {
  loadingHistory.value = true
  try {
    const res = await getUserFeedbackPage(userId, historyPageNum.value, historyPageSize.value)
    myFeedbacks.value = res.records
    historyTotal.value = res.total
  } catch (e) {
    console.error('加载反馈历史失败', e)
  } finally {
    loadingHistory.value = false
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

onMounted(() => {
  loadMyFeedback()
})
</script>

<style scoped>
.tab-bar {
  display: flex;
  gap: 0;
  margin-bottom: 24px;
  border-bottom: 2px solid var(--anime-border);
}

.tab-btn {
  padding: 12px 24px;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  font-size: 14px;
  font-weight: 600;
  color: var(--anime-text-muted);
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn:hover {
  color: var(--anime-text-primary);
}

.tab-btn.active {
  color: var(--anime-pink);
  border-bottom-color: var(--anime-pink);
}

.unread-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  background: var(--anime-pink);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  border-radius: 9px;
  margin-left: 8px;
}

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

.feedback-history-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feedback-history-item {
  padding: 16px;
  background: var(--anime-bg-card);
  border-radius: var(--anime-radius-lg);
  border: 1px solid var(--anime-border);
}

.feedback-history-item.has_reply {
  border-color: var(--anime-green);
  background: rgba(184, 233, 148, 0.05);
}

.history-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.history-rating {
  display: flex;
  gap: 2px;
}

.history-type {
  padding: 4px 12px;
  border-radius: var(--anime-radius);
  font-size: 12px;
  background: var(--anime-bg);
  color: var(--anime-text-secondary);
}

.history-status {
  padding: 4px 12px;
  border-radius: var(--anime-radius);
  font-size: 12px;
  background: rgba(255, 183, 197, 0.2);
  color: var(--anime-pink);
}

.history-status.replied {
  background: rgba(184, 233, 148, 0.2);
  color: var(--anime-green);
}

.history-time {
  font-size: 12px;
  color: var(--anime-text-muted);
}

.history-content {
  font-size: 14px;
  color: var(--anime-text-secondary);
  line-height: 1.5;
}

.admin-reply-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed var(--anime-border);
}

.reply-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--anime-green);
  margin-bottom: 8px;
}

.reply-content {
  padding: 12px;
  background: rgba(184, 233, 148, 0.1);
  border-radius: var(--anime-radius);
  font-size: 14px;
  color: var(--anime-text-primary);
  line-height: 1.6;
}

.reply-time {
  font-size: 12px;
  color: var(--anime-text-muted);
  margin-top: 8px;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 20px;
}
</style>