<template>
  <div class="anime-page-shell">
    <section class="anime-card">
      <div class="anime-card-header">
        <div>
          <div class="anime-card-title">FAQ 常见问答管理</div>
          <div class="anime-card-desc">自动提取高频问题 · 手动维护问答内容</div>
        </div>
        <div class="anime-card-actions">
          <button class="anime-btn primary" @click="showCreateDialog">
            <Plus :size="18" />
            <span>新增FAQ</span>
          </button>
          <button class="anime-btn ghost" @click="showExtractDialog = true">
            <Sparkles :size="18" />
            <span>自动提取</span>
          </button>
          <button class="anime-btn ghost" @click="loadFaqList">
            <RefreshCw :size="18" :class="{ 'animate-spin': loading }" />
            <span>刷新</span>
          </button>
        </div>
      </div>

      <div class="anime-card-body">
        <div class="faq-stats">
          <div class="stat-item">
            <div class="stat-value">{{ faqStats?.total || 0 }}</div>
            <div class="stat-label">总FAQ数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value green">{{ faqStats?.active || 0 }}</div>
            <div class="stat-label">启用</div>
          </div>
          <div class="stat-item">
            <div class="stat-value pink">{{ faqStats?.inactive || 0 }}</div>
            <div class="stat-label">禁用</div>
          </div>
        </div>

        <div class="anime-divider"></div>

        <div class="search-section">
          <div class="search-input-wrapper">
            <Search :size="18" class="search-icon" />
            <input 
              v-model="searchKeyword" 
              class="search-input" 
              placeholder="搜索问题..." 
              @input="handleSearch"
            />
            <button v-if="searchKeyword" class="search-clear" @click="clearSearch">✕</button>
          </div>
        </div>

        <div class="faq-list">
          <div v-for="faq in faqList" :key="faq.id" class="faq-item">
            <div class="faq-header">
              <div class="faq-question">
                <span class="faq-badge" :class="faq.status ? 'green' : 'muted'">{{ faq.status ? '启用' : '禁用' }}</span>
                <span class="faq-text">{{ faq.question }}</span>
              </div>
              <div class="faq-meta">
                <span class="faq-count">命中 {{ faq.hitCount }} 次</span>
                <span class="faq-priority">优先级: {{ faq.priority }}</span>
              </div>
            </div>
            <div class="faq-answer" v-html="renderAnswer(faq.answer)"></div>
            <div class="faq-keywords" v-if="faq.keywords">
              <span class="keyword-tag" v-for="kw in faq.keywords.split(',')" :key="kw">{{ kw }}</span>
            </div>
            <div class="faq-actions">
              <button class="anime-btn ghost sm" @click="editFaq(faq)">
                <Edit :size="14" />
                <span>编辑</span>
              </button>
              <button class="anime-btn ghost sm" :class="faq.status ? 'pink' : 'green'" @click="toggleStatus(faq)">
                <ToggleLeft v-if="faq.status" :size="14" />
                <ToggleRight v-else :size="14" />
                <span>{{ faq.status ? '禁用' : '启用' }}</span>
              </button>
              <button class="anime-btn ghost sm pink" @click="confirmDelete(faq)">
                <Trash2 :size="14" />
                <span>删除</span>
              </button>
            </div>
          </div>
          <div v-if="faqList.length === 0" class="anime-empty">
            <span class="anime-empty-icon">📭</span>
            <span class="anime-empty-text">暂无FAQ数据</span>
          </div>
        </div>
      </div>
    </section>

    <div v-if="showExtractDialog" class="dialog-overlay" @click.self="showExtractDialog = false">
      <div class="dialog-content">
        <div class="dialog-header">
          <h3>自动提取FAQ</h3>
          <button class="dialog-close" @click="showExtractDialog = false">✕</button>
        </div>
        <div class="dialog-body">
          <div class="form-section">
            <label class="form-label">最小出现次数</label>
            <input type="number" v-model="extractMinCount" class="anime-input" min="1" max="10" />
            <div class="field-hint">问题至少出现几次才会被提取</div>
          </div>
          <div class="form-section">
            <label class="form-label">统计天数</label>
            <input type="number" v-model="extractDays" class="anime-input" min="1" max="90" />
            <div class="field-hint">从最近N天的对话中提取</div>
          </div>
          <div class="dialog-footer">
            <button class="anime-btn ghost" @click="showExtractDialog = false">取消</button>
            <button class="anime-btn primary" @click="doExtract" :disabled="extracting">
              <Sparkles :size="16" />
              <span>{{ extracting ? '提取中...' : '开始提取' }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showEditDialog" class="dialog-overlay" @click.self="showEditDialog = false">
      <div class="dialog-content lg">
        <div class="dialog-header">
          <h3>{{ editMode === 'create' ? '新增FAQ' : '编辑FAQ' }}</h3>
          <button class="dialog-close" @click="showEditDialog = false">✕</button>
        </div>
        <div class="dialog-body">
          <div class="form-section">
            <label class="form-label">问题</label>
            <input v-model="editForm.question" class="anime-input" placeholder="请输入问题" />
          </div>
          <div class="form-section">
            <label class="form-label">答案</label>
            <textarea v-model="editForm.answer" class="anime-input" rows="6" placeholder="请输入答案"></textarea>
          </div>
          <div class="form-section">
            <label class="form-label">关键词（逗号分隔）</label>
            <input v-model="editForm.keywords" class="anime-input" placeholder="关键词1,关键词2" />
          </div>
          <div class="form-section">
            <label class="form-label">优先级</label>
            <input type="number" v-model="editForm.priority" class="anime-input" min="0" max="100" />
          </div>
          <div class="dialog-footer">
            <button class="anime-btn ghost" @click="showEditDialog = false">取消</button>
            <button class="anime-btn primary" @click="saveFaq" :disabled="saving">
              <Save :size="16" />
              <span>{{ saving ? '保存中...' : '保存' }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Sparkles, RefreshCw, Edit, Trash2, ToggleLeft, ToggleRight, Save, Plus, Search } from 'lucide-vue-next'
import { getFaqPage, createFaq, updateFaq, deleteFaq, extractFaqFromConversations, getFaqStats } from '../api/faq'
import { renderMarkdown } from '../lib/markdown'
import type { FaqItem, FaqStats } from '../api/faq'

const loading = ref(false)
const faqList = ref<FaqItem[]>([])
const faqStats = ref<FaqStats | null>(null)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const searchKeyword = ref('')
const searchTimer = ref<number | null>(null)

const showExtractDialog = ref(false)
const extractMinCount = ref(3)
const extractDays = ref(30)
const extracting = ref(false)

const showEditDialog = ref(false)
const editMode = ref<'create' | 'edit'>('edit')
const editForm = ref<Partial<FaqItem>>({})
const saving = ref(false)

onMounted(() => {
  loadFaqList()
  loadFaqStats()
})

async function loadFaqList() {
  loading.value = true
  try {
    const keyword = searchKeyword.value.trim()
    const resp = await getFaqPage(undefined, keyword || undefined, pageNum.value, pageSize.value)
    faqList.value = resp.records || []
    total.value = resp.total || 0
  } catch (e) {
    console.error('加载FAQ失败', e)
  }
  loading.value = false
}

function handleSearch() {
  if (searchTimer.value) {
    clearTimeout(searchTimer.value)
  }
  searchTimer.value = window.setTimeout(() => {
    pageNum.value = 1
    loadFaqList()
  }, 300)
}

function clearSearch() {
  searchKeyword.value = ''
  pageNum.value = 1
  loadFaqList()
}

async function loadFaqStats() {
  try {
    faqStats.value = await getFaqStats()
  } catch (e) {
    console.error('加载FAQ统计失败', e)
  }
}

async function doExtract() {
  extracting.value = true
  try {
    const result = await extractFaqFromConversations(1, extractMinCount.value, extractDays.value)
    if (result.success) {
      showExtractDialog.value = false
      await loadFaqList()
      await loadFaqStats()
    }
  } catch (e) {
    console.error('提取FAQ失败', e)
  }
  extracting.value = false
}

function editFaq(faq: FaqItem) {
  editMode.value = 'edit'
  editForm.value = { ...faq }
  showEditDialog.value = true
}

function showCreateDialog() {
  editMode.value = 'create'
  editForm.value = {
    knowledgeBaseId: 1,
    question: '',
    answer: '',
    keywords: '',
    status: true,
    priority: 0,
  }
  showEditDialog.value = true
}

async function saveFaq() {
  if (!editForm.value.question || !editForm.value.answer) return
  saving.value = true
  try {
    if (editMode.value === 'edit' && editForm.value.id) {
      await updateFaq(editForm.value)
    } else {
      await createFaq(editForm.value)
    }
    showEditDialog.value = false
    await loadFaqList()
    await loadFaqStats()
  } catch (e) {
    console.error('保存FAQ失败', e)
  }
  saving.value = false
}

async function toggleStatus(faq: FaqItem) {
  try {
    await updateFaq({ id: faq.id, status: !faq.status })
    await loadFaqList()
  } catch (e) {
    console.error('切换状态失败', e)
  }
}

async function confirmDelete(faq: FaqItem) {
  if (!confirm('确定删除此FAQ吗？')) return
  try {
    await deleteFaq(faq.id)
    await loadFaqList()
    await loadFaqStats()
  } catch (e) {
    console.error('删除FAQ失败', e)
  }
}

function truncate(s: string, max: number): string {
  return s && s.length > max ? s.substring(0, max) + '...' : s
}

function renderAnswer(answer: string): string {
  if (!answer) return ''
  const truncated = truncate(answer, 200)
  return renderMarkdown(truncated)
}
</script>

<style scoped>
.faq-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--anime-text-primary);
}

.stat-value.green { color: var(--anime-green); }
.stat-value.pink { color: var(--anime-pink); }

.stat-label {
  font-size: 12px;
  color: var(--anime-text-muted);
}

.faq-item {
  padding: 16px;
  border: 2px solid var(--anime-border);
  border-radius: var(--anime-radius-lg);
  margin-bottom: 12px;
  background: var(--anime-bg-card);
}

.faq-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.faq-question {
  display: flex;
  align-items: center;
  gap: 10px;
}

.faq-badge {
  padding: 4px 10px;
  border-radius: var(--anime-radius-sm);
  font-size: 12px;
  font-weight: 600;
}

.faq-badge.green { background: rgba(34, 197, 94, 0.2); color: var(--anime-green); }
.faq-badge.muted { background: rgba(128, 128, 128, 0.2); color: var(--anime-text-muted); }

.faq-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--anime-text-primary);
}

.faq-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--anime-text-muted);
}

.faq-count { color: var(--anime-blue); }
.faq-priority { color: var(--anime-purple); }

.faq-answer {
  font-size: 14px;
  color: var(--anime-text-secondary);
  margin-bottom: 12px;
  line-height: 1.6;
}

.faq-keywords {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.keyword-tag {
  padding: 4px 10px;
  background: rgba(255, 183, 197, 0.15);
  border-radius: var(--anime-radius-sm);
  font-size: 12px;
  color: var(--anime-pink);
}

.faq-actions {
  display: flex;
  gap: 8px;
}

.anime-btn.sm {
  padding: 6px 12px;
  font-size: 12px;
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.dialog-content {
  background: var(--anime-bg-card);
  border: 2px solid var(--anime-border);
  border-radius: var(--anime-radius-xl);
  padding: 24px;
  width: 400px;
  max-width: 90vw;
}

.dialog-content.lg {
  width: 600px;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.dialog-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--anime-text-primary);
}

.dialog-close {
  background: transparent;
  border: none;
  font-size: 20px;
  color: var(--anime-text-muted);
  cursor: pointer;
}

.dialog-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-section {
  margin-bottom: 8px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--anime-text-primary);
  margin-bottom: 8px;
}

.field-hint {
  font-size: 12px;
  color: var(--anime-text-muted);
  margin-top: 4px;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 16px;
}

.animate-spin { animation: spin 1s linear infinite; }

.search-section {
  margin-bottom: 20px;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: var(--anime-bg);
  border: 2px solid var(--anime-border);
  border-radius: var(--anime-radius-lg);
  transition: all 0.3s ease;
}

.search-input-wrapper:focus-within {
  border-color: var(--anime-pink);
}

.search-icon {
  color: var(--anime-text-muted);
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 14px;
  color: var(--anime-text-primary);
  outline: none;
}

.search-input::placeholder {
  color: var(--anime-text-muted);
}

.search-clear {
  background: transparent;
  border: none;
  color: var(--anime-text-muted);
  cursor: pointer;
  padding: 4px 8px;
  font-size: 14px;
}

.search-clear:hover {
  color: var(--anime-pink);
}

.faq-answer p {
  margin: 0;
}

.faq-answer pre {
  background: rgba(0, 0, 0, 0.05);
  padding: 8px;
  border-radius: var(--anime-radius-lg);
  overflow-x: auto;
  margin: 8px 0;
}

.faq-answer code {
  background: rgba(0, 0, 0, 0.08);
  padding: 2px 4px;
  border-radius: 4px;
  font-size: 13px;
}

.faq-answer pre code {
  background: transparent;
  padding: 0;
}

.faq-answer ul, .faq-answer ol {
  margin: 8px 0;
  padding-left: 20px;
}

.faq-answer li {
  margin-bottom: 4px;
}
</style>