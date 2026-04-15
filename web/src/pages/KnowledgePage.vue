<template>
  <div class="pageShell">
    <section class="card">
      <div class="cardHeader">
        <div>
          <div class="h1">知识库</div>
          <div class="muted">管理知识库和文档</div>
        </div>
        <div class="right">
          <button type="button" class="btn" @click="showCreateKb = true">新建知识库</button>
        </div>
      </div>

      <div v-if="err" class="error" style="margin: 0 18px 12px">{{ err }}</div>

      <div class="imToolbar">
        <div class="tabs">
          <button
            v-for="t in tabs"
            :key="t.key"
            type="button"
            class="tab"
            :class="{ active: activeTab === t.key }"
            @click="activeTab = t.key"
          >
            {{ t.label }}
          </button>
        </div>
      </div>

      <div v-if="loading" class="muted" style="padding: 16px">加载中…</div>

      <div v-else-if="activeTab === 'kb'" class="kbList">
        <div v-if="kbList.length === 0" class="empty" style="margin: 12px">暂无知识库</div>
        <div v-for="kb in kbList" :key="kb.id" class="kbCard">
          <div class="kbCardHead">
            <span class="h3">{{ kb.name }}</span>
            <span class="badge">{{ kb.docCount || 0 }} 文档</span>
          </div>
          <div class="kbCardDesc muted">{{ kb.description || '无描述' }}</div>
          <div class="kbCardMeta muted">
            状态：{{ kb.status ? '启用' : '禁用' }} |
            来源：{{ kb.sourceType }} |
            创建时间：{{ kb.createTime }}
          </div>
          <div class="kbCardActs">
            <button type="button" class="btn btnGhost" @click="viewDocs(kb)">文档</button>
            <button type="button" class="btn btnGhost" @click="syncKb(kb)">同步</button>
            <button type="button" class="btn btnGhost" @click="editKb(kb)">编辑</button>
            <button type="button" class="btn btnGhost" @click="deleteKb(kb)">删除</button>
          </div>
        </div>
      </div>

      <div v-else-if="activeTab === 'doc'" class="docList">
        <div v-if="!selectedKb" class="muted" style="padding: 20px">请先选择一个知识库</div>
        <template v-else>
          <div class="docListHead">
            <span class="h3">{{ selectedKb.name }} - 文档</span>
            <button type="button" class="btn" @click="showCreateDoc = true">新增文档</button>
          </div>
          <div v-if="docLoading" class="muted" style="padding: 12px">加载中…</div>
          <div v-else-if="docList.length === 0" class="empty" style="margin: 12px">暂无文档</div>
          <table v-else class="table">
            <thead>
              <tr>
                <th>标题</th>
                <th>状态</th>
                <th>来源</th>
                <th>同步状态</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="doc in docList" :key="doc.id">
                <td>{{ doc.title }}</td>
                <td>{{ doc.status ? '启用' : '禁用' }}</td>
                <td>{{ doc.source }}</td>
                <td>{{ doc.syncStatus === 1 ? '已同步' : doc.syncStatus === 2 ? '失败' : '未同步' }}</td>
                <td>{{ doc.createTime }}</td>
                <td>
                  <button type="button" class="btn btnGhost" @click="syncDoc(doc)">同步</button>
                  <button type="button" class="btn btnGhost" @click="deleteDoc(doc)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </template>
      </div>

      <div v-else-if="activeTab === 'faq'" class="faqList">
        <div v-if="faqList.length === 0" class="empty" style="margin: 12px">暂无FAQ</div>
        <div v-for="faq in faqList" :key="faq.id" class="faqItem">
          <div class="faqQ">{{ faq.question }}</div>
          <div class="faqA muted">{{ faq.answer }}</div>
          <div class="faqMeta">
            <span class="badge">命中 {{ faq.hitCount || 0 }}</span>
            <button type="button" class="btn btnGhost" @click="deleteFaq(faq)">删除</button>
          </div>
        </div>
      </div>
    </section>

    <div v-if="showCreateKb" class="modalOverlay" @click.self="showCreateKb = false">
      <div class="modal">
        <div class="modalHead">
          <span class="h3">新建知识库</span>
          <button type="button" class="btnGhost" @click="showCreateKb = false">✕</button>
        </div>
        <div class="modalBody">
          <div class="formGroup">
            <label>名称</label>
            <input v-model="formKb.name" class="input" placeholder="知识库名称" />
          </div>
          <div class="formGroup">
            <label>描述</label>
            <textarea v-model="formKb.description" class="input" placeholder="知识库描述" />
          </div>
        </div>
        <div class="modalFoot">
          <button type="button" class="btn" @click="createKbSubmit">确定</button>
          <button type="button" class="btn btnGhost" @click="showCreateKb = false">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '../api/client'

const tabs = [
  { key: 'kb' as const, label: '知识库' },
  { key: 'doc' as const, label: '文档' },
  { key: 'faq' as const, label: 'FAQ' },
]

const activeTab = ref<'kb' | 'doc' | 'faq'>('kb')
const loading = ref(false)
const err = ref<string | null>(null)
const kbList = ref<any[]>([])
const docList = ref<any[]>([])
const faqList = ref<any[]>([])
const selectedKb = ref<any>(null)
const docLoading = ref(false)

const showCreateKb = ref(false)
const showCreateDoc = ref(false)
const formKb = ref({ name: '', description: '' })

async function loadKbList() {
  loading.value = true
  err.value = null
  try {
    const res = await api.get('/kb/page', { params: { pageNum: 1, pageSize: 100 } })
    kbList.value = res.data.records || []
  } catch (e: any) {
    err.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadDocList(kbId: number) {
  docLoading.value = true
  try {
    const res = await api.get(`/kb/${kbId}/document/page`, { params: { pageNum: 1, pageSize: 100 } })
    docList.value = res.data.records || []
  } finally {
    docLoading.value = false
  }
}

async function loadFaqList() {
  try {
    const res = await api.get('/kb/conversation/faq/page', { params: { pageNum: 1, pageSize: 100 } })
    faqList.value = res.data.records || []
  } catch {
    faqList.value = []
  }
}

async function createKbSubmit() {
  try {
    await api.post('/kb', formKb.value)
    showCreateKb.value = false
    formKb.value = { name: '', description: '' }
    loadKbList()
  } catch (e: any) {
    err.value = e?.message || '创建失败'
  }
}

async function deleteKb(kb: any) {
  if (!confirm(`确定删除知识库 "${kb.name}" 吗？`)) return
  try {
    await api.delete(`/kb/${kb.id}`)
    loadKbList()
  } catch (e: any) {
    err.value = e?.message || '删除失败'
  }
}

function viewDocs(kb: any) {
  selectedKb.value = kb
  activeTab.value = 'doc'
  loadDocList(kb.id)
}

async function syncKb(kb: any) {
  try {
    await api.post(`/kb/${kb.id}/sync`)
    alert('同步成功')
  } catch {
    alert('同步失败')
  }
}

function editKb(kb: any) {
  alert('编辑功能开发中')
}

async function deleteDoc(doc: any) {
  if (!confirm(`确定删除文档 "${doc.title}" 吗？`)) return
  try {
    await api.delete(`/kb/document/${doc.id}`)
    if (selectedKb.value) loadDocList(selectedKb.value.id)
  } catch (e: any) {
    err.value = e?.message || '删除失败'
  }
}

async function syncDoc(doc: any) {
  try {
    await api.post(`/kb/document/${doc.id}/sync`)
    alert('同步成功')
  } catch {
    alert('同步失败')
  }
}

async function deleteFaq(faq: any) {
  if (!confirm(`确定删除FAQ吗？`)) return
  try {
    await api.delete(`/kb/conversation/faq/${faq.id}`)
    loadFaqList()
  } catch (e: any) {
    err.value = e?.message || '删除失败'
  }
}

async function loadPage() {
  await loadKbList()
  if (activeTab.value === 'faq') loadFaqList()
}

activeTab.value = 'kb'
onMounted(loadPage)
</script>