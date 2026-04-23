<template>
  <div class="anime-page-shell">
    <section class="anime-card">
      <div class="anime-card-header">
        <div>
          <div class="anime-card-title">反馈管理</div>
          <div class="anime-card-desc">查看用户反馈，回复并处理</div>
        </div>
        <div class="stats-overview" v-if="stats">
          <div class="stat-item">
            <span class="stat-label">总反馈</span>
            <span class="stat-value">{{ stats.total }}</span>
          </div>
          <div class="stat-item pending">
            <span class="stat-label">待处理</span>
            <span class="stat-value">{{ stats.pending }}</span>
          </div>
          <div class="stat-item done">
            <span class="stat-label">已处理</span>
            <span class="stat-value">{{ stats.processed }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">平均评分</span>
            <span class="stat-value">{{ stats.avgRating.toFixed(1) }}</span>
          </div>
        </div>
      </div>

      <div class="anime-card-body">
        <div class="filter-bar">
          <select v-model="filterStatus" class="anime-input" @change="loadList">
            <option :value="undefined">全部</option>
            <option :value="0">待处理</option>
            <option :value="1">已处理</option>
          </select>
          <button class="anime-btn ghost" @click="loadList">
            <RefreshCw :size="18" />
            <span>刷新</span>
          </button>
        </div>

        <div v-if="loading" class="anime-loading">加载中...</div>
        
        <div v-else-if="list.length === 0" class="anime-empty">
          <span class="anime-empty-icon">📭</span>
          <div>暂无反馈</div>
        </div>

        <div v-else class="feedback-list">
          <div v-for="item in list" :key="item.id" class="feedback-item" @click="openDetail(item)">
            <div class="feedback-header">
              <span class="feedback-id">#{{ item.id }}</span>
              <span class="feedback-rating">
                <Star v-for="n in 5" :key="n" :size="14" :fill="item.rating >= n ? '#ffb7c5' : 'none'" />
              </span>
              <span class="feedback-type-badge" :class="item.feedbackType">{{ getTypeLabel(item.feedbackType) }}</span>
              <span class="feedback-status" :class="{ pending: !item.status, done: item.status }">
                {{ item.status ? '已处理' : '待处理' }}
              </span>
            </div>
            <div class="feedback-content">{{ item.feedbackContent || '无内容' }}</div>
            <div class="feedback-meta">
              <span>用户: {{ item.userId }}</span>
              <span>{{ formatTime(item.createTime) }}</span>
            </div>
          </div>
        </div>

        <div class="pagination" v-if="total > pageSize">
          <button class="anime-btn ghost sm" :disabled="pageNum <= 1" @click="pageNum--; loadList()">上一页</button>
          <span>第 {{ pageNum }} / {{ totalPages }} 页</span>
          <button class="anime-btn ghost sm" :disabled="pageNum >= totalPages" @click="pageNum++; loadList()">下一页</button>
        </div>
      </div>
    </section>

    <div v-if="showDetail" class="anime-modal-overlay" @click.self="showDetail = false">
      <div class="anime-modal feedback-detail-modal">
        <div class="anime-modal-header">
          <span class="modal-title">反馈详情 #{{ detail?.id }}</span>
          <button class="anime-modal-close" @click="showDetail = false">✕</button>
        </div>
        <div class="anime-modal-body" v-if="detail">
          <div class="detail-section">
            <label>满意度评分</label>
            <div class="rating-stars">
              <Star v-for="n in 5" :key="n" :size="20" :fill="detail.rating >= n ? '#ffb7c5' : 'none'" />
              <span>{{ ratingTexts[detail.rating] }}</span>
            </div>
          </div>
          <div class="detail-section">
            <label>反馈类型</label>
            <span class="feedback-type-badge" :class="detail.feedbackType">{{ getTypeLabel(detail.feedbackType) }}</span>
          </div>
          <div class="detail-section">
            <label>反馈内容</label>
            <div class="detail-content-box">{{ detail.feedbackContent || '无内容' }}</div>
          </div>
          <div class="detail-section">
            <label>用户ID</label>
            <span>{{ detail.userId }}</span>
          </div>
          <div class="detail-section">
            <label>提交时间</label>
            <span>{{ formatTime(detail.createTime) }}</span>
          </div>
          <div class="detail-section" v-if="detail.adminReply">
            <label>管理员回复</label>
            <div class="detail-reply-box">{{ detail.adminReply }}</div>
            <div class="reply-meta">
              <span>回复时间: {{ formatTime(detail.replyTime) }}</span>
            </div>
          </div>

          <div class="reply-section" v-if="!detail.status">
            <label>回复用户</label>
            <textarea v-model="replyContent" class="anime-input reply-textarea" placeholder="输入回复内容..." rows="4"></textarea>
            <div class="reply-actions">
              <button class="anime-btn primary" :disabled="replying || !replyContent.trim()" @click="submitReply">
                <Send :size="18" />
                <span>{{ replying ? '提交中...' : '提交回复' }}</span>
              </button>
              <button class="anime-btn ghost" @click="markProcessed">
                <CheckCircle :size="18" />
                <span>标记已处理</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Star, RefreshCw, Send, CheckCircle } from 'lucide-vue-next'
import {
  fetchFeedbackPage,
  fetchFeedbackStats,
  replyFeedback,
  updateFeedbackStatus,
  type KbFeedback,
  type FeedbackStats
} from '../api/feedbackManage'

const list = ref<KbFeedback[]>([])
const stats = ref<FeedbackStats | null>(null)
const detail = ref<KbFeedback | null>(null)
const loading = ref(false)
const replying = ref(false)
const showDetail = ref(false)
const replyContent = ref('')
const filterStatus = ref<number | undefined>(undefined)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

const ratingTexts = ['', '很不满意', '不满意', '一般', '满意', '很满意']

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

function formatTime(time: string | null): string {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

async function loadList() {
  loading.value = true
  try {
    const res = await fetchFeedbackPage({
      status: filterStatus.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    list.value = res.records
    total.value = res.total
  } catch (e) {
    console.error('加载反馈列表失败', e)
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    stats.value = await fetchFeedbackStats()
  } catch (e) {
    console.error('加载统计失败', e)
  }
}

function openDetail(item: KbFeedback) {
  detail.value = item
  replyContent.value = item.adminReply || ''
  showDetail.value = true
}

async function submitReply() {
  if (!detail.value || !replyContent.value.trim()) return
  
  const adminId = Number(localStorage.getItem('chatbase_admin_id') || 1)
  replying.value = true
  try {
    const result = await replyFeedback(detail.value.id, adminId, replyContent.value.trim())
    if (result.success) {
      detail.value.status = true
      detail.value.adminReply = replyContent.value.trim()
      detail.value.replyTime = new Date().toISOString()
      await loadList()
      await loadStats()
    } else {
      alert(result.message)
    }
  } catch (e) {
    alert('回复失败')
  } finally {
    replying.value = false
  }
}

async function markProcessed() {
  if (!detail.value) return
  
  try {
    const result = await updateFeedbackStatus(detail.value.id, 1)
    if (result.success) {
      detail.value.status = true
      await loadList()
      await loadStats()
    } else {
      alert(result.message)
    }
  } catch (e) {
    alert('操作失败')
  }
}

onMounted(() => {
  loadList()
  loadStats()
})
</script>

<style scoped>
.stats-overview {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  flex-direction: column;
  padding: 12px 16px;
  background: var(--anime-bg-card);
  border-radius: var(--anime-radius-lg);
  border: 1px solid var(--anime-border);
}

.stat-item.pending .stat-value { color: var(--anime-pink); }
.stat-item.done .stat-value { color: var(--anime-green); }

.stat-label {
  font-size: 12px;
  color: var(--anime-text-muted);
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--anime-text-primary);
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.filter-bar select {
  width: 120px;
}

.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feedback-item {
  padding: 16px;
  background: var(--anime-bg-card);
  border-radius: var(--anime-radius-lg);
  border: 1px solid var(--anime-border);
  cursor: pointer;
  transition: all 0.2s;
}

.feedback-item:hover {
  border-color: var(--anime-pink);
  box-shadow: 0 2px 8px rgba(255, 183, 197, 0.2);
}

.feedback-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.feedback-id {
  font-weight: 600;
  color: var(--anime-text-primary);
}

.feedback-rating {
  display: flex;
  gap: 2px;
}

.feedback-type-badge {
  padding: 4px 12px;
  border-radius: var(--anime-radius);
  font-size: 12px;
  background: var(--anime-bg);
  color: var(--anime-text-secondary);
}

.feedback-type-badge.inaccurate { background: rgba(255, 183, 197, 0.2); color: var(--anime-pink); }
.feedback-type-badge.accurate { background: rgba(184, 233, 148, 0.2); color: var(--anime-green); }

.feedback-status {
  padding: 4px 12px;
  border-radius: var(--anime-radius);
  font-size: 12px;
}

.feedback-status.pending {
  background: rgba(255, 183, 197, 0.2);
  color: var(--anime-pink);
}

.feedback-status.done {
  background: rgba(184, 233, 148, 0.2);
  color: var(--anime-green);
}

.feedback-content {
  font-size: 14px;
  color: var(--anime-text-secondary);
  margin-bottom: 8px;
  line-height: 1.5;
  max-height: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.feedback-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--anime-text-muted);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 20px;
}

.feedback-detail-modal {
  max-width: 600px;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--anime-text-muted);
  margin-bottom: 8px;
}

.rating-stars {
  display: flex;
  align-items: center;
  gap: 4px;
}

.rating-stars span {
  margin-left: 8px;
  font-size: 14px;
  color: var(--anime-text-primary);
}

.detail-content-box,
.detail-reply-box {
  padding: 12px;
  background: var(--anime-bg);
  border-radius: var(--anime-radius);
  border: 1px solid var(--anime-border);
  font-size: 14px;
  line-height: 1.6;
}

.detail-reply-box {
  background: rgba(184, 233, 148, 0.1);
  border-color: rgba(184, 233, 148, 0.3);
}

.reply-meta {
  font-size: 12px;
  color: var(--anime-text-muted);
  margin-top: 8px;
}

.reply-section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--anime-border);
}

.reply-textarea {
  width: 100%;
  resize: vertical;
  min-height: 100px;
}

.reply-actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}
</style>