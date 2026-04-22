<template>
  <div class="anime-page-shell">
    <section class="anime-card">
      <div class="anime-card-header">
        <div>
          <div class="anime-card-title">知识库管理</div>
          <div class="anime-card-desc">知识库管理 · 文档同步 · FAQ配置</div>
        </div>
        <div class="anime-card-actions">
          <button class="anime-btn blue" @click="doSyncFromDify" :disabled="syncing">
            <Download :size="18" />
            <span v-if="syncing">同步中...</span>
            <span v-else>从Dify同步</span>
          </button>
          <button class="anime-btn primary" @click="showCreateKb = true">
            <Plus :size="18" />
            <span>新建知识库</span>
          </button>
        </div>
      </div>

      <div v-if="err" class="anime-error" style="margin: 16px 28px;">{{ err }}</div>

      <div class="anime-card-body">
        <div class="anime-tabs" style="margin-bottom: 16px;">
          <button v-for="t in tabs" :key="t.key" class="anime-tab" :class="{ active: activeTab === t.key }" @click="activeTab = t.key; loadTabData()">{{ t.label }}</button>
        </div>

        <div v-if="loading" class="anime-empty">
          <span class="anime-loader-spinner"></span>
          <span class="anime-empty-text">加载中...</span>
        </div>

        <div v-else-if="activeTab === 'kb'" class="kb-list">
          <div v-if="kbList.length === 0" class="anime-empty">
            <div class="anime-empty-icon">📚</div>
            <div class="anime-empty-text">暂无知识库</div>
          </div>
          <div class="kb-grid">
            <div v-for="kb in kbList" :key="kb.id" class="anime-card" style="padding: 20px;">
              <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
                <span style="font-size: 18px; font-weight: 700; color: var(--anime-pink);">{{ kb.name }}</span>
                <span class="anime-badge blue">{{ kb.docCount || 0 }} 文档</span>
              </div>
              <div style="font-size: 14px; color: var(--anime-text-muted); margin-bottom: 12px;">{{ kb.description || '无描述' }}</div>
              <div style="display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px;">
                <div style="display: flex; gap: 10px;">
                  <span class="anime-badge" :class="kb.status ? 'green' : 'pink'">{{ kb.status ? '启用' : '禁用' }}</span>
                  <span class="anime-code">{{ kb.sourceType }}</span>
                </div>
                <div style="font-size: 13px; color: var(--anime-text-muted);">创建时间: {{ kb.createTime }}</div>
              </div>
              <div style="display: flex; gap: 10px; flex-wrap: wrap;">
                <button class="anime-btn ghost" @click="viewDocs(kb)">
                  <BookOpen :size="16" />
                  <span>文档</span>
                </button>
                <button class="anime-btn primary" @click="openUploadModal(kb)">
                  <Upload :size="16" />
                  <span>上传</span>
                </button>
                <button class="anime-btn blue" @click="syncKb(kb)">
                  <RefreshCw :size="16" />
                  <span>同步</span>
                </button>
                <button class="anime-btn ghost" @click="editKb(kb)">
                  <Edit3 :size="16" />
                  <span>编辑</span>
                </button>
                <button class="anime-btn ghost" style="border-color: var(--anime-pink); color: var(--anime-pink);" @click="deleteKb(kb)">
                  <Trash2 :size="16" />
                  <span>删除</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="activeTab === 'doc'" class="doc-section">
          <div v-if="!selectedKb" class="anime-empty">
            <div class="anime-empty-icon">👈</div>
            <div class="anime-empty-text">请先选择一个知识库查看文档 ✿</div>
          </div>
          <template v-else>
            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;">
              <div style="display: flex; align-items: center; gap: 10px;">
                <span class="anime-badge green">{{ selectedKb.name }}</span>
                <span style="font-weight: 600; color: var(--anime-text-primary);">文档列表</span>
              </div>
              <div style="display: flex; gap: 12px;">
                <div class="doc-search-wrapper">
                  <Search :size="16" class="doc-search-icon" />
                  <input 
                    v-model="docSearchKeyword" 
                    class="doc-search-input" 
                    placeholder="搜索文档..." 
                    @input="handleDocSearch"
                  />
                  <button v-if="docSearchKeyword" class="doc-search-clear" @click="clearDocSearch">✕</button>
                </div>
                <button class="anime-btn primary" @click="showCreateDoc = true">
                  <Plus :size="18" />
                  <span>新增文档</span>
                </button>
              </div>
            </div>
            <div v-if="docLoading" class="anime-empty">
              <span class="anime-loader-spinner"></span>
              <span class="anime-empty-text">加载中...</span>
            </div>
            <div v-else-if="docList.length === 0" class="anime-empty">
              <div class="anime-empty-icon">📄</div>
              <div class="anime-empty-text">暂无文档</div>
            </div>
            <table v-else class="anime-table">
              <thead><tr><th>标题</th><th>状态</th><th>来源</th><th>同步</th><th>创建时间</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="doc in docList" :key="doc.id">
                  <td>{{ doc.title }}</td>
                  <td><span class="anime-badge" :class="doc.status ? 'green' : 'pink'">{{ doc.status ? '启用' : '禁用' }}</span></td>
                  <td class="anime-code">{{ doc.source }}</td>
                  <td><span class="anime-badge" :class="getSyncColor(doc.syncStatus)">{{ getSyncText(doc.syncStatus) }}</span></td>
                  <td>{{ doc.createTime }}</td>
                  <td style="display: flex; gap: 8px;">
                    <button class="anime-btn blue" style="padding: 6px 12px;" @click="syncDoc(doc)">同步</button>
                    <button class="anime-btn ghost" style="padding: 6px 12px; border-color: var(--anime-pink); color: var(--anime-pink);" @click="deleteDoc(doc)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </template>
        </div>

        <div v-else-if="activeTab === 'faq'" class="faq-section">
          <div v-if="faqList.length === 0" class="anime-empty">
            <div class="anime-empty-icon">❓</div>
            <div class="anime-empty-text">暂无FAQ</div>
          </div>
          <div class="faq-grid">
            <div v-for="faq in faqList" :key="faq.id" class="anime-card" style="padding: 20px;">
              <div style="font-size: 16px; font-weight: 600; color: var(--anime-blue); margin-bottom: 12px;">{{ faq.question }}</div>
              <div style="font-size: 14px; color: var(--anime-text-secondary); line-height: 1.6; margin-bottom: 16px;">{{ faq.answer }}</div>
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span class="anime-badge green">命中 {{ faq.hitCount || 0 }}</span>
                <button class="anime-btn ghost" style="padding: 6px 12px; border-color: var(--anime-pink); color: var(--anime-pink);" @click="deleteFaq(faq)">删除</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="showCreateKb" class="anime-modal-overlay" @click.self="showCreateKb = false">
        <div class="anime-modal">
          <div class="anime-modal-header">
            <span class="anime-modal-title">✿ 新建知识库 ✿</span>
            <button class="anime-modal-close" @click="showCreateKb = false">✕</button>
          </div>
          <div class="anime-modal-body">
            <div class="form-group">
              <label class="form-label">名称</label>
              <input v-model="formKb.name" class="anime-input" placeholder="知识库名称" />
            </div>
            <div class="form-group">
              <label class="form-label">描述</label>
              <textarea v-model="formKb.description" class="anime-textarea" placeholder="知识库描述" />
            </div>
          </div>
          <div class="anime-modal-footer">
            <button class="anime-btn primary" @click="createKbSubmit">确定</button>
            <button class="anime-btn ghost" @click="showCreateKb = false">取消</button>
          </div>
        </div>
      </div>

      <div v-if="showCreateDoc" class="anime-modal-overlay" @click.self="showCreateDoc = false">
        <div class="anime-modal">
          <div class="anime-modal-header">
            <span class="anime-modal-title">✿ 新增文档 ✿</span>
            <button class="anime-modal-close" @click="showCreateDoc = false">✕</button>
          </div>
          <div class="anime-modal-body">
            <div class="form-group">
              <label class="form-label">标题</label>
              <input v-model="formDoc.title" class="anime-input" placeholder="文档标题" />
            </div>
            <div class="form-group">
              <label class="form-label">内容</label>
              <textarea v-model="formDoc.content" class="anime-textarea" placeholder="文档内容" />
            </div>
          </div>
          <div class="anime-modal-footer">
            <button class="anime-btn primary" @click="createDocSubmit">确定</button>
            <button class="anime-btn ghost" @click="showCreateDoc = false">取消</button>
          </div>
        </div>
      </div>

      <div v-if="showUploadModal" class="anime-modal-overlay" @click.self="showUploadModal = false">
        <div class="anime-modal" style="max-width: 600px;">
          <div class="anime-modal-header">
            <span class="anime-modal-title">✿ 批量上传文件到知识库 ✿</span>
            <button class="anime-modal-close" @click="showUploadModal = false">✕</button>
          </div>
          <div class="anime-modal-body">
            <div style="margin-bottom: 16px;">
              <input ref="uploadInputRef" type="file" multiple style="display: none;" :disabled="uploadLoading" @change="handleUploadFileSelect" />
              <button class="anime-btn primary" @click="triggerUploadInput">
                <Upload :size="18" />
                <span>选择文件</span>
              </button>
              <span style="margin-left: 12px; color: var(--anime-text-muted); font-size: 13px;">支持 Word、PDF、Markdown 等</span>
            </div>
            <div v-if="uploadFiles.length > 0" style="margin-bottom: 16px;">
              <div style="color: var(--anime-text-muted); font-size: 13px; margin-bottom: 8px;">已选择 {{ uploadFiles.length }} 个文件:</div>
              <div style="display: flex; flex-direction: column; gap: 8px; max-height: 200px; overflow-y: auto;">
                <div v-for="(f, i) in uploadFiles" :key="i" style="display: flex; align-items: center; gap: 8px; padding: 6px 10px; background: var(--anime-bg); border-radius: 6px;">
                  <span style="font-weight: 600; flex: 1; overflow: hidden; text-overflow: ellipsis;">{{ f.name }}</span>
                  <span class="anime-code" style="font-size: 12px;">{{ formatSize(f.size) }}</span>
                  <button class="anime-btn ghost" style="padding: 2px 6px; font-size: 12px;" @click="removeUploadFile(i)">✕</button>
                </div>
              </div>
            </div>
            <div v-if="uploadProgress && uploadLoading" style="margin-bottom: 16px; padding: 16px; background: rgba(255, 183, 197, 0.1); border-radius: 8px;">
              <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 12px;">
                <span class="anime-badge pink">{{ uploadProgress.status === 'completed' ? '完成' : '上传中' }}</span>
                <span style="color: var(--anime-text-primary); font-weight: 600;">{{ uploadProgress.completedCount }} / {{ uploadProgress.totalCount }}</span>
              </div>
              <div class="anime-progress" style="margin-bottom: 12px;">
                <div class="anime-progress-bar" :style="{ width: uploadProgress.progressPercent + '%' }"></div>
              </div>
              <div v-if="uploadProgress.currentFile" style="color: var(--anime-text-muted); font-size: 13px; margin-bottom: 8px;">
                当前: {{ uploadProgress.currentFile }}
              </div>
              <div style="display: flex; flex-direction: column; gap: 4px; max-height: 150px; overflow-y: auto;">
                <div v-for="(fp, i) in uploadProgress.fileProgresses" :key="i" style="display: flex; align-items: center; gap: 8px; padding: 4px 8px; background: var(--anime-bg); border-radius: 4px; font-size: 13px;">
                  <span style="flex: 1; overflow: hidden; text-overflow: ellipsis;">{{ fp.fileName }}</span>
                  <span :style="{ color: fp.status === 'success' ? 'var(--anime-green)' : 'var(--anime-pink)' }">
                    {{ fp.status === 'success' ? '✓' : '✗' }}
                  </span>
                </div>
              </div>
            </div>
            <div v-if="uploadResult && !uploadLoading" style="padding: 12px; background: rgba(184, 233, 148, 0.1); border-radius: 8px;">
              <div style="color: var(--anime-green); font-weight: 600; margin-bottom: 8px;">上传完成: 成功 {{ uploadResult.successCount }} / {{ uploadResult.totalCount }}</div>
              <div style="display: flex; flex-direction: column; gap: 4px;">
                <div v-for="(r, i) in uploadResult.results" :key="i" style="display: flex; justify-content: space-between;">
                  <span>{{ r.fileName }}</span>
                  <span :style="{ color: r.success ? 'var(--anime-green)' : 'var(--anime-pink)' }">{{ r.message }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="anime-modal-footer">
            <button class="anime-btn primary" :disabled="uploadLoading || uploadFiles.length === 0" @click="doBatchUpload">
              <Upload :size="18" />
              <span v-if="uploadLoading">上传中...</span>
              <span v-else>开始上传</span>
            </button>
            <button class="anime-btn ghost" @click="showUploadModal = false">关闭</button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Plus, BookOpen, RefreshCw, Edit3, Trash2, Upload, Download, Search } from 'lucide-vue-next'
import { api } from '../api/client'
import { batchUploadToKb, syncFromDify } from '../api/kb'
import { subscribeUploadProgress } from '../api/progress'
import type { UploadProgress } from '../api/progress'
import type { KbKnowledgeBase, KbDocument } from '../api/kb'
import type { BatchUploadResponse } from '../types/dify'
import { getOrCreateUserId } from '../lib/user'

const userId = getOrCreateUserId()

const tabs = [{ key: 'kb' as const, label: '知识库' }, { key: 'doc' as const, label: '文档' }, { key: 'faq' as const, label: 'FAQ' }]
const activeTab = ref<'kb' | 'doc' | 'faq'>('kb')
const loading = ref(false)
const syncing = ref(false)
const err = ref<string | null>(null)
const kbList = ref<KbKnowledgeBase[]>([])
const docList = ref<KbDocument[]>([])
const faqList = ref<any[]>([])
const selectedKb = ref<KbKnowledgeBase | null>(null)
const docLoading = ref(false)
const docSearchKeyword = ref('')
const docSearchTimer = ref<number | null>(null)

const showCreateKb = ref(false)
const showCreateDoc = ref(false)
const showUploadModal = ref(false)
const formKb = ref({ name: '', description: '' })
const formDoc = ref({ title: '', content: '' })
const uploadFiles = ref<File[]>([])
const uploadLoading = ref(false)
const uploadResult = ref<BatchUploadResponse | null>(null)
const uploadKbId = ref<number | null>(null)
const uploadInputRef = ref<HTMLInputElement | null>(null)
const uploadProgress = ref<UploadProgress | null>(null)
const uploadEventSource = ref<EventSource | null>(null)

async function loadKbList() {
  loading.value = true
  err.value = null
  try {
    const res = await api.get('/kb/page', { params: { pageNum: 1, pageSize: 100 } })
    kbList.value = res.data.records || []
  } catch (e: any) { err.value = e?.message || '加载失败' }
  finally { loading.value = false }
}

async function doSyncFromDify() {
  syncing.value = true
  err.value = null
  try {
    const result = await syncFromDify()
    if (result.success) {
      await loadKbList()
    } else {
      err.value = result.message
    }
  } catch (e: any) {
    err.value = e?.message || '同步失败'
  } finally {
    syncing.value = false
  }
}

async function loadDocList(kbId: number) {
  docLoading.value = true
  try {
    const keyword = docSearchKeyword.value.trim()
    const res = await api.get(`/kb/${kbId}/document/page`, { 
      params: { 
        pageNum: 1, 
        pageSize: 100,
        title: keyword || undefined
      } 
    })
    docList.value = res.data.records || []
  } finally { docLoading.value = false }
}

function handleDocSearch() {
  if (docSearchTimer.value) {
    clearTimeout(docSearchTimer.value)
  }
  docSearchTimer.value = window.setTimeout(() => {
    if (selectedKb.value) {
      loadDocList(selectedKb.value.id)
    }
  }, 300)
}

function clearDocSearch() {
  docSearchKeyword.value = ''
  if (selectedKb.value) {
    loadDocList(selectedKb.value.id)
  }
}

async function loadFaqList() {
  try {
    const res = await api.get('/kb/conversation/faq/page', { params: { pageNum: 1, pageSize: 100 } })
    faqList.value = res.data.records || []
  } catch { faqList.value = [] }
}

function loadTabData() {
  if (activeTab.value === 'kb' && kbList.value.length === 0) loadKbList()
  else if (activeTab.value === 'faq') loadFaqList()
}

async function createKbSubmit() {
  try {
    await api.post('/kb', formKb.value)
    showCreateKb.value = false
    formKb.value = { name: '', description: '' }
    loadKbList()
  } catch (e: any) { err.value = e?.message || '创建失败' }
}

async function createDocSubmit() {
  if (!selectedKb.value) return
  try {
    await api.post('/kb/document', { ...formDoc.value, knowledgeBaseId: selectedKb.value.id })
    showCreateDoc.value = false
    formDoc.value = { title: '', content: '' }
    loadDocList(selectedKb.value.id)
  } catch (e: any) { err.value = e?.message || '创建失败' }
}

async function deleteKb(kb: KbKnowledgeBase) {
  if (!confirm(`确定删除知识库 "${kb.name}" 吗？`)) return
  try { await api.delete(`/kb/${kb.id}`); loadKbList() }
  catch (e: any) { err.value = e?.message || '删除失败' }
}

function viewDocs(kb: KbKnowledgeBase) { 
  selectedKb.value = kb
  docSearchKeyword.value = ''
  activeTab.value = 'doc'
  loadDocList(kb.id)
}

async function syncKb(kb: KbKnowledgeBase) {
  try {
    const res = await api.post(`/kb/${kb.id}/sync`)
    if (res.data?.success) alert('同步成功')
    else alert(res.data?.message || '同步失败')
  } catch { alert('同步失败') }
}

function editKb(kb: KbKnowledgeBase) { alert('编辑功能开发中') }

async function deleteDoc(doc: KbDocument) {
  if (!confirm(`确定删除文档 "${doc.title}" 吗？`)) return
  try { await api.delete(`/kb/document/${doc.id}`); if (selectedKb.value) loadDocList(selectedKb.value.id) }
  catch (e: any) { err.value = e?.message || '删除失败' }
}

async function syncDoc(doc: KbDocument) {
  try {
    const res = await api.post(`/kb/document/${doc.id}/sync`)
    if (res.data?.success) alert('同步成功')
    else alert(res.data?.message || '同步失败')
  } catch { alert('同步失败') }
}

async function deleteFaq(faq: any) {
  if (!confirm('确定删除FAQ吗？')) return
  try { await api.delete(`/kb/conversation/faq/${faq.id}`); loadFaqList() }
  catch (e: any) { err.value = e?.message || '删除失败' }
}

function getSyncColor(status: number): string { if (status === 1) return 'green'; if (status === 2) return 'pink'; return 'muted' }
function getSyncText(status: number): string { if (status === 1) return '已同步'; if (status === 2) return '失败'; return '未同步' }

function openUploadModal(kb: KbKnowledgeBase) {
  uploadKbId.value = kb.id
  uploadFiles.value = []
  uploadResult.value = null
  uploadProgress.value = null
  showUploadModal.value = true
}

function triggerUploadInput() {
  uploadInputRef.value?.click()
}

function handleUploadFileSelect(e: Event) {
  const el = e.target as HTMLInputElement
  const files = el.files
  if (files && files.length > 0) {
    uploadFiles.value = [...uploadFiles.value, ...Array.from(files)]
  }
  el.value = ''
}

function removeUploadFile(idx: number) {
  uploadFiles.value.splice(idx, 1)
}

async function doBatchUpload() {
  if (uploadFiles.value.length === 0 || !uploadKbId.value) return
  uploadLoading.value = true
  uploadResult.value = null
  uploadProgress.value = null
  
  const form = new FormData()
  uploadFiles.value.forEach(f => form.append('files', f))
  form.append('user', userId)
  
  try {
    const resp = await api.post<{ success: boolean; taskId: string; message: string }>(`/kb/${uploadKbId.value}/batch-upload`, form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    
    if (!resp.data.success || !resp.data.taskId) {
      err.value = resp.data.message || '创建上传任务失败'
      uploadLoading.value = false
      return
    }
    
    const taskId = resp.data.taskId
    uploadProgress.value = {
      taskId,
      totalCount: uploadFiles.value.length,
      completedCount: 0,
      successCount: 0,
      failedCount: 0,
      currentFile: '准备上传...',
      status: 'pending',
      fileProgresses: [],
      progressPercent: 0,
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    }
    
    uploadEventSource.value = subscribeUploadProgress(
      taskId,
      (progress) => {
        uploadProgress.value = progress
      },
      (progress) => {
        uploadProgress.value = progress
        uploadLoading.value = false
        
        uploadResult.value = {
          totalCount: progress.totalCount,
          successCount: progress.successCount,
          failedCount: progress.failedCount,
          results: progress.fileProgresses.map(fp => ({
            fileName: fp.fileName,
            success: fp.status === 'success',
            message: fp.message,
            difyFileId: fp.difyFileId || undefined
          }))
        }
        
        uploadFiles.value = []
        if (selectedKb.value) loadDocList(selectedKb.value.id)
      },
      (errorMsg) => {
        err.value = errorMsg
        uploadLoading.value = false
      }
    )
  } catch (e: any) {
    err.value = e?.message || '上传失败'
    uploadLoading.value = false
  }
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

onMounted(async () => {
  await loadKbList()
})

onUnmounted(() => {
  if (uploadEventSource.value) {
    uploadEventSource.value.close()
  }
})
</script>

<style scoped>
.kb-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.faq-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
.doc-section { min-height: 300px; }

.doc-search-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--anime-bg);
  border: 2px solid var(--anime-border);
  border-radius: var(--anime-radius-lg);
  transition: all 0.3s ease;
}

.doc-search-wrapper:focus-within {
  border-color: var(--anime-blue);
}

.doc-search-icon {
  color: var(--anime-text-muted);
}

.doc-search-input {
  border: none;
  background: transparent;
  font-size: 13px;
  color: var(--anime-text-primary);
  outline: none;
  width: 150px;
}

.doc-search-input::placeholder {
  color: var(--anime-text-muted);
}

.doc-search-clear {
  background: transparent;
  border: none;
  color: var(--anime-text-muted);
  cursor: pointer;
  padding: 2px 6px;
  font-size: 12px;
}

.doc-search-clear:hover {
  color: var(--anime-pink);
}
</style>