<template>
  <div class="anime-page-shell">
    <section class="anime-card" :aria-busy="loading">
      <div class="anime-card-header">
        <div>
          <h1 class="anime-card-title">FAQ 常见问答管理</h1>
          <p class="anime-card-desc">自动提取高频问题 · 手动维护问答内容</p>
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
        <div class="faq-stats" role="group" aria-label="FAQ 状态概览">
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
              aria-label="搜索常见问题"
              type="search"
              placeholder="搜索问题..." 
              @input="handleSearch"
            />
            <button v-if="searchKeyword" class="search-clear" aria-label="清除问题搜索" @click="clearSearch">✕</button>
          </div>
        </div>

        <div v-if="loadError" class="anime-error faq-load-error" role="alert" aria-live="assertive">{{ loadError }}</div>

        <div class="faq-list">
          <div v-if="loading" class="anime-empty" role="status" aria-live="polite">
            <span class="anime-loader-spinner" aria-hidden="true"></span>
            <span class="anime-empty-text">加载中...</span>
          </div>
          <article v-for="faq in faqList" :key="faq.id" class="faq-item">
            <div class="faq-header">
              <div class="faq-question">
                <span class="faq-badge" :class="faq.status ? 'green' : 'muted'">{{ faq.status ? '启用' : '禁用' }}</span>
                <h2 class="faq-text">{{ faq.question }}</h2>
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
          </article>
          <div v-if="!loading && !loadError && faqList.length === 0" class="anime-empty">
            <span class="anime-empty-icon">📭</span>
            <span class="anime-empty-text">暂无FAQ数据</span>
          </div>
        </div>
      </div>
    </section>

    <div v-if="showExtractDialog" class="dialog-overlay" @click.self="closeExtractDialog">
      <div ref="extractDialogRef" class="dialog-content" role="dialog" aria-modal="true" aria-label="自动提取常见问题" tabindex="-1">
        <div class="dialog-header">
          <h3>自动提取FAQ</h3>
          <button class="dialog-close" aria-label="关闭自动提取窗口" @click="closeExtractDialog">✕</button>
        </div>
        <div class="dialog-body">
          <div class="form-section">
            <label class="form-label" for="faq-extract-min-count">最小出现次数</label>
            <input id="faq-extract-min-count" type="number" v-model="extractMinCount" class="anime-input" min="1" max="10" />
            <div class="field-hint">问题至少出现几次才会被提取</div>
          </div>
          <div class="form-section">
            <label class="form-label" for="faq-extract-days">统计天数</label>
            <input id="faq-extract-days" type="number" v-model="extractDays" class="anime-input" min="1" max="90" />
            <div class="field-hint">从最近N天的对话中提取</div>
          </div>
          <div class="dialog-footer">
            <button class="anime-btn ghost" @click="closeExtractDialog">取消</button>
            <button class="anime-btn primary" @click="doExtract" :disabled="extracting">
              <Sparkles :size="16" />
              <span>{{ extracting ? '提取中...' : '开始提取' }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showEditDialog" class="dialog-overlay" @click.self="closeEditDialog">
      <div ref="editDialogRef" class="dialog-content lg" role="dialog" aria-modal="true" tabindex="-1" :aria-label="editMode === 'create' ? '新增FAQ' : '编辑FAQ'">
        <div class="dialog-header">
          <h3>{{ editMode === 'create' ? '新增FAQ' : '编辑FAQ' }}</h3>
          <button class="dialog-close" aria-label="关闭FAQ编辑窗口" @click="closeEditDialog">✕</button>
        </div>
        <div class="dialog-body">
          <div class="form-section">
            <label class="form-label" for="faq-edit-question">问题</label>
            <input id="faq-edit-question" v-model="editForm.question" class="anime-input" placeholder="请输入问题" />
          </div>
          <div class="form-section">
            <label class="form-label" for="faq-edit-answer">答案</label>
            <textarea id="faq-edit-answer" v-model="editForm.answer" class="anime-input" rows="6" placeholder="请输入答案"></textarea>
          </div>
          <div class="form-section">
            <label class="form-label" for="faq-edit-keywords">关键词（逗号分隔）</label>
            <input id="faq-edit-keywords" v-model="editForm.keywords" class="anime-input" placeholder="关键词1,关键词2" />
          </div>
          <div class="form-section">
            <label class="form-label" for="faq-edit-priority">优先级</label>
            <input id="faq-edit-priority" type="number" v-model="editForm.priority" class="anime-input" min="0" max="100" />
          </div>
          <div class="dialog-footer">
            <button class="anime-btn ghost" @click="closeEditDialog">取消</button>
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
import { ref, computed, onMounted, watch } from 'vue'
import { Sparkles, RefreshCw, Edit, Trash2, ToggleLeft, ToggleRight, Save, Plus, Search } from 'lucide-vue-next'
import { getFaqPage, createFaq, updateFaq, deleteFaq, extractFaqFromConversations, getFaqStats } from '../api/faq'
import { renderMarkdown } from '../lib/markdown'
import { useModalDialog } from '../composables/useModalDialog'
import type { FaqItem, FaqStats } from '../api/faq'

const loading = ref(false)
const loadError = ref('')
const faqList = ref<FaqItem[]>([])
const faqStats = ref<FaqStats | null>(null)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const searchKeyword = ref('')
const searchTimer = ref<number | null>(null)

const showExtractDialog = ref(false)
const closeExtractDialog = () => { showExtractDialog.value = false }
const extractMinCount = ref(3)
const extractDays = ref(30)
const extracting = ref(false)

const showEditDialog = ref(false)
const closeEditDialog = () => { showEditDialog.value = false }
const editMode = ref<'create' | 'edit'>('edit')
const editForm = ref<Partial<FaqItem>>({})
const saving = ref(false)

const { dialogRef: extractDialogRef } = useModalDialog(showExtractDialog, closeExtractDialog)
const { dialogRef: editDialogRef } = useModalDialog(showEditDialog, closeEditDialog)

onMounted(() => {
  loadFaqList()
  loadFaqStats()
})

async function loadFaqList() {
  loading.value = true
  loadError.value = ''
  try {
    const keyword = searchKeyword.value.trim()
    const resp = await getFaqPage(undefined, keyword || undefined, pageNum.value, pageSize.value)
    faqList.value = resp.records || []
    total.value = resp.total || 0
  } catch (e: any) {
    console.error('加载FAQ失败', e)
    loadError.value = e?.response?.data?.message || '加载FAQ失败，请稍后重试'
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
@keyframes spin { to { transform: rotate(360deg); } }
.animate-spin { animation: spin 1s linear infinite; }
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

.stat-value.green { color: var(--ui-success); }
.stat-value.pink { color: var(--ui-danger); }

.stat-label {
  font-size: 12px;
  color: var(--anime-text-muted);
}

.faq-item {
  padding: 16px;
  box-shadow: var(--anime-shadow-sm);
  border-radius: var(--anime-radius-lg);
  margin-bottom: 12px;
  background: var(--anime-bg-card);
}

.faq-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  gap: 12px;
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

.faq-badge.green { background: var(--ui-success-soft); color: var(--ui-success); }
.faq-badge.muted { background: var(--ui-surface-muted); color: var(--ui-text-secondary); }

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
  flex-shrink: 0;
}

.faq-count { color: var(--ui-primary-hover); }
.faq-priority { color: var(--ui-text-secondary); }

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
  background: var(--ui-primary-soft);
  border-radius: var(--anime-radius-sm);
  font-size: 12px;
  color: var(--ui-primary);
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
  backdrop-filter: blur(4px);
}

.dialog-content {
  background: var(--anime-bg-card);
  box-shadow: var(--anime-shadow-lg);
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

.dialog-close:hover {
  color: var(--ui-primary);
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

.search-section {
  margin-bottom: 20px;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: var(--anime-bg);
  box-shadow: var(--anime-shadow-sm);
  border-radius: var(--anime-radius-lg);
  transition: all 0.3s ease;
}

.search-input-wrapper:focus-within {
  box-shadow: var(--anime-shadow-pink);
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
  color: var(--ui-primary);
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

<style scoped>
.faq-stats { display: flex; flex-wrap: wrap; gap: 14px; margin-bottom: 20px; }
.faq-load-error { margin: 0 0 14px; color: var(--ui-danger); background: var(--ui-danger-soft); border: 1px solid var(--ui-danger-border); }
.anime-card-title { margin: 0; }
.anime-card-desc { margin: 4px 0 0; }
.stat-item { min-width: 112px; padding: 12px 16px; text-align: left; background: var(--ui-surface-muted); border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); }
.stat-value { color: var(--ui-text); font-size: 24px; font-weight: 750; }
.stat-value.green { color: var(--ui-success); }
.stat-value.pink { color: var(--ui-danger); }
.stat-label { color: var(--ui-text-secondary); font-size: 12px; }
.search-section { margin-bottom: 18px; }
.search-input-wrapper { display: flex; align-items: center; gap: 10px; min-height: 44px; padding: 8px 12px; background: var(--ui-surface); border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); }
.search-input-wrapper:focus-within { border-color: var(--ui-focus); box-shadow: 0 0 0 3px color-mix(in srgb, var(--ui-focus) 18%, transparent); }
.search-icon { flex: 0 0 auto; color: var(--ui-text-muted); }
.search-input { min-width: 0; flex: 1; color: var(--ui-text); }
.search-input::placeholder { color: var(--ui-text-muted); }
.search-clear { min-width: 32px; min-height: 32px; border: 0; border-radius: var(--ui-radius-sm); background: transparent; color: var(--ui-text-secondary); cursor: pointer; }
.search-clear:hover { background: var(--ui-surface-muted); color: var(--ui-text); }
.faq-list { display: flex; flex-direction: column; gap: 12px; }
.faq-item { min-width: 0; padding: 18px; background: var(--ui-surface); border: 1px solid var(--ui-border); border-radius: var(--ui-radius-lg); box-shadow: var(--ui-shadow-card); }
.faq-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.faq-question { display: flex; align-items: flex-start; gap: 10px; min-width: 0; }
.faq-text { margin: 0; color: var(--ui-text); font-size: 16px; line-height: 1.45; font-weight: 700; overflow-wrap: anywhere; }
.faq-badge { flex: 0 0 auto; padding: 4px 9px; border: 1px solid var(--ui-border); border-radius: 999px; font-size: 12px; font-weight: 650; }
.faq-badge.green { background: var(--ui-success-soft); border-color: var(--ui-success-border); color: var(--ui-success); }
.faq-badge.muted { background: var(--ui-surface-muted); color: var(--ui-text-secondary); }
.faq-meta { display: flex; flex-wrap: wrap; gap: 12px; flex-shrink: 0; color: var(--ui-text-secondary); font-size: 12px; }
.faq-count { color: var(--ui-primary-hover); }
.faq-priority { color: var(--ui-text-secondary); }
.faq-answer { color: var(--ui-text-secondary); font-size: 14px; line-height: 1.65; margin-bottom: 12px; overflow-wrap: anywhere; }
.faq-keywords { display: flex; flex-wrap: wrap; gap: 7px; margin-bottom: 12px; }
.keyword-tag { padding: 4px 9px; background: var(--ui-primary-soft); border: 1px solid var(--ui-primary-border); border-radius: 999px; color: var(--ui-primary-hover); font-size: 12px; }
.faq-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.faq-actions .anime-btn.pink { color: var(--ui-danger) !important; }
.dialog-overlay { background: rgb(23 35 33 / 38%); backdrop-filter: blur(3px); }
.dialog-content { width: min(440px, 92vw); max-height: min(88vh, 760px); overflow-y: auto; padding: 22px; background: var(--ui-surface); border: 1px solid var(--ui-border); border-radius: var(--ui-radius-lg); box-shadow: var(--ui-shadow-card); }
.dialog-content.lg { width: min(640px, 92vw); }
.dialog-header { margin-bottom: 18px; }
.dialog-header h3 { margin: 0; color: var(--ui-text); font-size: 18px; }
.dialog-close { min-width: 36px; min-height: 36px; border-radius: var(--ui-radius-sm); color: var(--ui-text-secondary); }
.dialog-close:hover { color: var(--ui-text); background: var(--ui-surface-muted); }
.dialog-body { gap: 12px; }
.form-section { margin-bottom: 4px; }
.form-label { color: var(--ui-text-secondary); font-weight: 650; }
.field-hint { color: var(--ui-text-muted); }
.dialog-footer { flex-wrap: wrap; gap: 8px; margin-top: 14px; }
.faq-answer p { color: inherit; }
.faq-answer pre { background: var(--ui-surface-muted); border: 1px solid var(--ui-border); }
.faq-answer code { color: var(--ui-text); background: var(--ui-surface-muted); }
@media (max-width: 700px) {
  .faq-stats { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
  .stat-item { min-width: 0; padding: 10px; }
  .stat-value { font-size: 21px; }
  .faq-header { flex-direction: column; }
  .faq-meta { flex-shrink: 1; }
  .faq-item { padding: 14px; }
  .dialog-content, .dialog-content.lg { width: min(100%, 640px); max-width: 100%; padding: 18px; }
  .dialog-footer .anime-btn { flex: 1 1 auto; }
}
</style>
