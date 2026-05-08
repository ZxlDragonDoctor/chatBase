<template>
  <div class="anime-page-shell">
    <section class="anime-card">
      <div class="anime-card-header">
        <div>
          <div class="anime-card-title">知识库管理</div>
          <div class="anime-card-desc">全部知识库 · 含私有知识库 · 仅管理员可见</div>
        </div>
      </div>

      <div v-if="err" class="anime-error" style="margin: 16px 28px;">{{ err }}</div>

      <div class="anime-card-body">
        <div class="filter-bar">
          <Search :size="16" class="search-icon" />
          <input v-model="keyword" class="anime-input" placeholder="搜索知识库名称、描述、创建者..." style="max-width: 350px;" />
          <span class="filter-count">共 {{ filteredList.length }} 个知识库</span>
          <button class="anime-btn ghost" :disabled="loading" @click="loadKbList">
            <RefreshCw :size="16" />
            <span>刷新</span>
          </button>
        </div>

        <div v-if="loading" class="anime-empty">
          <span class="anime-loader-spinner"></span>
          <span class="anime-empty-text">加载中...</span>
        </div>

        <div v-else-if="filteredList.length === 0" class="anime-empty">
          <div class="anime-empty-icon">📚</div>
          <div class="anime-empty-text">{{ keyword ? '未找到匹配的知识库' : '暂无知识库' }}</div>
        </div>

        <div v-else class="kb-grid">
          <div v-for="kb in filteredList" :key="kb.id" class="anime-card kb-card" @click="openDetail(kb.id)">
            <div class="kb-header">
              <span class="kb-name">{{ kb.name }}</span>
              <div class="kb-badges">
                <span v-if="kb.categoryId" class="anime-badge purple">{{ getCategoryName(kb.categoryId) }}</span>
                <span class="anime-badge blue">{{ kb.docCount || 0 }} 文档</span>
              </div>
            </div>
            <div class="kb-desc">{{ kb.description || '无描述' }}</div>
            <div class="kb-meta">
              <span class="anime-badge" :class="kb.status ? 'green' : 'pink'">{{ kb.status ? '启用' : '禁用' }}</span>
              <span v-if="kb.isPublic !== undefined" class="anime-badge" :class="kb.isPublic ? 'green' : 'gray'">{{ kb.isPublic ? '公开' : '私有' }}</span>
              <span class="anime-code">{{ kb.sourceType || '手动' }}</span>
              <span class="kb-time">创建者: {{ kb.createBy || '-' }}</span>
              <span class="kb-time">创建: {{ kb.createTime }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <div v-if="showDetail && detail" class="anime-modal-overlay" @click.self="showDetail = false">
      <div class="anime-modal detail-modal">
        <div class="anime-modal-header">
          <span class="modal-title">知识库详情 #{{ detail.id }}</span>
          <button class="anime-modal-close" @click="showDetail = false">✕</button>
        </div>
        <div class="anime-tabs" style="padding: 0 24px; margin-bottom: 0;">
          <button class="anime-tab" :class="{ active: detailTab === 'info' }" @click="detailTab = 'info'">基本信息</button>
          <button class="anime-tab" :class="{ active: detailTab === 'docs' }" @click="switchDocTab">文档列表 ({{ detail.docCount || 0 }})</button>
        </div>
        <div class="anime-modal-body" style="border-top: 2px solid var(--anime-border); padding-top: 16px;">
          <template v-if="detailTab === 'info'">
            <table class="detail-table">
              <tr><td class="dt-label">名称</td><td class="dt-value">{{ detail.name }}</td></tr>
              <tr><td class="dt-label">描述</td><td class="dt-value">{{ detail.description || '无' }}</td></tr>
              <tr><td class="dt-label">分类</td><td class="dt-value">{{ getCategoryName(detail.categoryId) }}</td></tr>
              <tr><td class="dt-label">Dify Dataset ID</td><td class="dt-value anime-code">{{ detail.difyDatasetId || '-' }}</td></tr>
              <tr><td class="dt-label">Dify API Key</td><td class="dt-value anime-code">{{ maskKey(detail.difyApiKey) }}</td></tr>
              <tr><td class="dt-label">来源</td><td class="dt-value">{{ detail.sourceType || '手动' }}</td></tr>
              <tr><td class="dt-label">同步平台</td><td class="dt-value">{{ detail.syncPlatform || '-' }}</td></tr>
              <tr v-if="detail.syncGroupIds"><td class="dt-label">同步群组</td><td class="dt-value anime-code">{{ detail.syncGroupIds }}</td></tr>
              <tr><td class="dt-label">自动同步</td><td class="dt-value"><span class="anime-badge" :class="detail.autoSync ? 'green' : 'gray'">{{ detail.autoSync ? '开启' : '关闭' }}</span></td></tr>
              <tr v-if="detail.autoSync"><td class="dt-label">同步间隔</td><td class="dt-value">{{ detail.syncInterval || '-' }} 分钟</td></tr>
              <tr><td class="dt-label">公开</td><td class="dt-value"><span class="anime-badge" :class="detail.isPublic ? 'green' : 'gray'">{{ detail.isPublic ? '是' : '否' }}</span></td></tr>
              <tr><td class="dt-label">状态</td><td class="dt-value"><span class="anime-badge" :class="detail.status ? 'green' : 'pink'">{{ detail.status ? '启用' : '禁用' }}</span></td></tr>
              <tr><td class="dt-label">文档数</td><td class="dt-value">{{ detail.docCount || 0 }}</td></tr>
              <tr><td class="dt-label">片段数</td><td class="dt-value">{{ detail.chunkCount || 0 }}</td></tr>
              <tr><td class="dt-label">创建者</td><td class="dt-value">{{ detail.createBy || '-' }}</td></tr>
              <tr><td class="dt-label">创建时间</td><td class="dt-value">{{ detail.createTime || '-' }}</td></tr>
              <tr><td class="dt-label">更新时间</td><td class="dt-value">{{ detail.updateTime || '-' }}</td></tr>
            </table>
          </template>
          <template v-else>
            <div v-if="docLoading" class="anime-empty">
              <span class="anime-loader-spinner"></span>
              <span class="anime-empty-text">加载文档中...</span>
            </div>
            <div v-else-if="docList.length === 0" class="anime-empty">
              <div class="anime-empty-icon">📄</div>
              <div class="anime-empty-text">暂无文档</div>
            </div>
            <table v-else class="doc-table">
              <thead>
                <tr>
                  <th>标题</th>
                  <th>类型</th>
                  <th>大小</th>
                  <th>同步状态</th>
                  <th>创建时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="doc in docList" :key="doc.id">
                  <td class="doc-title-cell">{{ doc.title }}</td>
                  <td><span class="anime-badge muted">{{ doc.fileType || '文本' }}</span></td>
                  <td class="anime-code">{{ formatSize(doc.fileSize) }}</td>
                  <td>
                    <span class="anime-badge" :class="getSyncClass(doc)">{{ getSyncLabel(doc) }}</span>
                  </td>
                  <td class="doc-time">{{ doc.createTime }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="docTotal > docList.length" class="doc-pagination">
              <button class="anime-btn ghost sm" :disabled="docPage <= 1 || docLoading" @click="docPage--; loadDocuments()">上一页</button>
              <span class="anime-code">第 {{ docPage }} / {{ docTotalPages }} 页 · 共 {{ docTotal }} 条</span>
              <button class="anime-btn ghost sm" :disabled="docPage >= docTotalPages || docLoading" @click="docPage++; loadDocuments()">下一页</button>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Search, RefreshCw } from 'lucide-vue-next'
import { api } from '../api/client'

interface KbKnowledgeBaseItem {
  id: number
  name: string
  description: string
  categoryId: number | null
  docCount: number
  status: boolean
  isPublic: boolean
  sourceType: string
  createBy: string
  createTime: string
}

interface KbKnowledgeBaseDetail extends KbKnowledgeBaseItem {
  difyDatasetId: string
  difyApiKey: string
  syncPlatform: string
  syncGroupIds: string
  autoSync: boolean
  syncInterval: number
  chunkCount: number
  updateTime: string
}

interface KbDocument {
  id: number
  knowledgeBaseId: number
  title: string
  content: string
  fileUrl: string
  fileName: string
  fileSize: number
  fileType: string
  difyDocumentId: string
  difyStatus: string
  syncStatus: number
  createTime: string
}

interface KbCategory {
  id: number
  name: string
}

const kbList = ref<KbKnowledgeBaseItem[]>([])
const categoryList = ref<KbCategory[]>([])
const loading = ref(true)
const err = ref('')
const keyword = ref('')
const showDetail = ref(false)
const detail = ref<KbKnowledgeBaseDetail | null>(null)
const detailTab = ref<'info' | 'docs'>('info')
const docList = ref<KbDocument[]>([])
const docLoading = ref(false)
const docPage = ref(1)
const docPageSize = 10
const docTotal = ref(0)

const docTotalPages = computed(() => Math.ceil(docTotal.value / docPageSize))

const filteredList = computed(() => {
  const kw = keyword.value.toLowerCase().trim()
  if (!kw) return kbList.value
  return kbList.value.filter(kb =>
    kb.name.toLowerCase().includes(kw) ||
    kb.description?.toLowerCase().includes(kw) ||
    kb.createBy?.toLowerCase().includes(kw) ||
    getCategoryName(kb.categoryId).toLowerCase().includes(kw)
  )
})

function getCategoryName(id: number | null) {
  if (!id) return '无分类'
  const cat = categoryList.value.find(c => c.id === id)
  return cat?.name || '未知'
}

function maskKey(key: string | null): string {
  if (!key) return '-'
  if (key.length <= 8) return '****'
  return key.slice(0, 4) + '****' + key.slice(-4)
}

function formatSize(bytes: number | null): string {
  if (bytes == null) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function getSyncClass(doc: KbDocument): string {
  if (doc.syncStatus === 1) return 'green'
  if (doc.syncStatus === 2) return 'pink'
  return 'muted'
}

function getSyncLabel(doc: KbDocument): string {
  if (doc.syncStatus === 1) return '已同步'
  if (doc.syncStatus === 2) return '失败'
  return '未同步'
}

function switchDocTab() {
  detailTab.value = 'docs'
  if (docList.value.length === 0) {
    docPage.value = 1
    loadDocuments()
  }
}

async function loadDocuments() {
  if (!detail.value) return
  docLoading.value = true
  try {
    const res = await api.get(`/kb/${detail.value.id}/document/page`, {
      params: { pageNum: docPage.value, pageSize: docPageSize }
    })
    docList.value = res.data.records || []
    docTotal.value = res.data.total || 0
  } catch {
    docList.value = []
    docTotal.value = 0
  } finally {
    docLoading.value = false
  }
}

async function openDetail(id: number) {
  detailTab.value = 'info'
  docList.value = []
  docTotal.value = 0
  docPage.value = 1
  try {
    const res = await api.get(`/kb/${id}`)
    detail.value = res.data
    showDetail.value = true
  } catch {
    err.value = '加载详情失败'
  }
}

async function loadKbList() {
  loading.value = true
  err.value = ''
  try {
    const res = await api.get('/kb/admin/page', { params: { pageNum: 1, pageSize: 100 } })
    kbList.value = res.data.records || []
  } catch (e: any) {
    err.value = e.response?.data?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await api.get('/kb/category/tree')
    const flat: KbCategory[] = []
    const flatten = (items: any[]) => {
      items.forEach((item: any) => {
        flat.push({ id: item.id, name: item.name })
        if (item.children?.length) flatten(item.children)
      })
    }
    flatten(res.data || [])
    categoryList.value = flat
  } catch { categoryList.value = [] }
}

onMounted(async () => {
  await Promise.all([loadKbList(), loadCategories()])
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: rgba(168, 216, 234, 0.05);
  border-radius: var(--anime-radius-lg);
}
.search-icon { color: var(--anime-text-muted); flex-shrink: 0; }
.filter-count { font-size: 13px; color: var(--anime-text-muted); margin-left: auto; }
.kb-card { padding: 20px; border: 2px solid var(--anime-border); cursor: pointer; transition: all 0.2s; }
.kb-card:hover { border-color: var(--anime-pink); box-shadow: var(--anime-shadow-soft); }
.kb-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.kb-name { font-size: 18px; font-weight: 700; color: var(--anime-pink); }
.kb-badges { display: flex; gap: 6px; }
.kb-desc { font-size: 14px; color: var(--anime-text-muted); margin-bottom: 12px; }
.kb-meta { display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px; }
.kb-time { font-size: 13px; color: var(--anime-text-muted); }
.kb-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.detail-modal { max-width: 640px; }
.detail-table { width: 100%; border-collapse: collapse; }
.detail-table tr { border-bottom: 1px solid var(--anime-border); }
.detail-table tr:last-child { border-bottom: none; }
.detail-table td { padding: 10px 8px; font-size: 14px; vertical-align: top; }
.dt-label { color: var(--anime-text-muted); width: 120px; font-weight: 600; white-space: nowrap; }
.dt-value { color: var(--anime-text-primary); word-break: break-all; }
.doc-table { width: 100%; border-collapse: collapse; margin-top: 8px; }
.doc-table th { text-align: left; padding: 8px; font-size: 12px; color: var(--anime-text-muted); border-bottom: 2px solid var(--anime-border); }
.doc-table td { padding: 10px 8px; font-size: 13px; border-bottom: 1px solid var(--anime-border); }
.doc-title-cell { font-weight: 600; color: var(--anime-text-primary); max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.doc-time { font-size: 12px; color: var(--anime-text-muted); white-space: nowrap; }
.doc-pagination { display: flex; align-items: center; justify-content: center; gap: 12px; margin-top: 16px; padding-top: 12px; border-top: 2px solid var(--anime-border); }
</style>
